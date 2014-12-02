/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import crawler.tags.tag;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/**
 *
 * @author turtlepool
 */
public class worker implements Runnable {

    public static final int FetchLimit = 1000, ConnTimeOut = 200, ReadTimeOut = 500;
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36";

    //queue
    private queue q;

    //stage related
    private boolean run = true;
    private int stage = 0;
    private long fetchtime = 0;
    private Semaphore[] sems;
    public String queuestr = "";

    //page related
    private int[] count = new int[crawler.Stages];
    private double[] delta = new double[crawler.Stages];
    private long fetched = 0;
    private String base = "", body = "", content = "", title = "";
    private tags anchors = null;
    private URL target = null, redirected = null, fail = null;

    //Exceptions
    private LinkedList<ex> exlist = new LinkedList<>();

    public class ex {

        public URL url;
        public long time;
        public int stage;
        public Exception e;

        public ex(URL url, long time, int stage, Exception e) {
            this.url = url;
            this.stage = stage;
            this.time = time;
            this.e = e;
        }
    }

    /**
     * worker constructor
     */
    public worker() {
        sems = crawler.getInstance().getSemaphore();
        q = crawler.getInstance().getQueue();
    }

    /**
     * do works with the stage order
     */
    public void run() {
        while (run) {
            try {
                sems[stage].acquire();
                work();
                sems[stage].release();
            } catch (InterruptedException e) {
                addException(e);
                run = false;
            }
            if (fetched >= FetchLimit) {
                run = false;
            }
        }
    }

    /**
     * do work work of stage s
     */
    public void work() {
        boolean b = false;
        long t = System.currentTimeMillis();
        switch (stage) {
            case 0:
                b = poll();
                break;
            case 1:
                b = fetch();
                break;
            case 2:
                b = process();
                break;
            case 3:
                b = store();
                break;
        }
        t = System.currentTimeMillis() - t;
        if (stage == 1) {
            fetchtime = t;
        }
        delta[stage] += t;
        ++count[stage];
        stage = b ? (stage + 1) % crawler.Stages : 0;
    }

    /**
     * poll a url from the queue
     */
    public boolean poll() {
        //queue operations
        //push urls into queue if this worker is already worked for a full cycle 
        if (target != null) {
            anchors.list.forEach(s -> {
                URL u = parseHttpRef(s.attribute("href"));
                if (u != null) {
                    q.offer(u);
                }
            });

            //update fetch time
            System.out.println(hashCode() + " S" + stage + " clear=" + target.toString() + " ft=" + fetchtime);
            q.setPriority(target, (int) fetchtime);
            clear(null);
        } else if (fail != null) {
            q.setPriority(fail, q.getPriority(fail) * 2);
            fail = null;
        }

        //pop a url from queue
        target = q.poll();
        if (target == null || db.getInstance().isVisited(target)) {
            clear(new Exception("Target Fetched Exception"));
            return false;
        }

        queuestr = q.showToLimit();
        return true;
    }

    /**
     * fetch page from the Internet
     */
    public boolean fetch() {
        //network operations
        int response = 0;
        String type = "", charset = "UTF-8";
        HttpURLConnection conn = null;

        //open connection
        try {
            conn = (HttpURLConnection) target.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", UserAgent);
            conn.setConnectTimeout(ConnTimeOut);
            conn.setReadTimeout(ReadTimeOut);
            conn.connect();
            response = conn.getResponseCode();
        } catch (Exception e) {
            clear(e);
            return false;
        }

        //get charset
        type = conn.getContentType() == null ? "" : conn.getContentType();
        if (!type.startsWith("text/html")) {
            clear(new Exception("Content Type Exception:" + type));
            return false;
        }
        if (type.indexOf("charset=") >= 0) {
            charset = type.substring(type.indexOf("charset=") + 8, type.length());
        }

        //get content type
        if (response == 200) {
            //read page
            try {
                InputStream in = conn.getInputStream();
                content = new String(readStream(in), charset);
            } catch (Exception e) {
                clear(e);
                return false;
            }
        } else {
            clear(new Exception("Response Code Exception:" + response));
            return false;
        }

        //get redirected url
        redirected = conn.getURL();

        return true;
    }

    /**
     * read stream
     */
    public byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        inStream.close();
        return outStream.toByteArray();
    }

    /**
     * clear
     */
    public void clear(Exception e) {
        System.out.println(hashCode() + " S" + stage + " clear=" + target.toString() + " e=" + (e != null) + " ft=" + fetchtime);
        if (e != null) {
            fail = parseHttpRef(target.toString());
            addException(e);
        }
        target = redirected = null;
        anchors = null;
        base = body = content = title = "";
    }

    /**
     * process the fetched page
     */
    public boolean process() {
        //cpu operations
        //get base of the page
        tags t = new tags("base", content);
        t.list.forEach(s -> base = s.attribute("href"));
        if (base.equals("")) {
            base = redirected.getProtocol() + "://" + redirected.getAuthority() + "/";
            String a[] = redirected.getFile().split("/");
            int len = ((a.length > 0 && !redirected.getFile().endsWith("/")) ? a.length - 1 : a.length);
            for (int i = 1; i < len; ++i) {
                base += a[i] + "/";
            }
        }

        //get title
        t = new tags("title", content);
        t.list.forEach(s -> title = s.getText());

        //get anchors
        anchors = new tags("a", content);
        if (anchors.list.isEmpty()) {
            clear(new Exception("Empty Link Exception"));
            return false;
        }

        //get pure text body of the page
        //body = content;
        tags bodytags = new tags("body", content);
        if (bodytags.list.isEmpty()) {
            body = content;
        } else {
            body = bodytags.getFirstTag().filter();
        }

        return true;
    }

    /**
     * store the content of the fetched page into database
     */
    public boolean store() {
        //disk operations
        //store
        if (db.getInstance().update(target, body)) {
            ++fetched;
        }
        if (!redirected.toString().equals(target.toString())) {
            db.getInstance().update(redirected, body);
        }

        //delete visited link
        for (Iterator<tag> it = anchors.list.iterator(); it.hasNext();) {
            tag t = it.next();
            URL u = parseHttpRef(t.attribute("href"));
            if (u == null || db.getInstance().isVisited(u)) {
                it.remove();
            }
        }

        return true;
    }

    /**
     * parse URL or relative URL
     */
    public URL parseHttpRef(String href) {
        try {
            if (href.startsWith("//")) {
                return new URL(redirected.getProtocol() + ":" + href);
            } else if (href.startsWith("/")) {
                return new URL(redirected.getProtocol() + "://" + redirected.getAuthority() + href);
            } else if (href.startsWith("http")) {
                return new URL(href);
            } else if (href.startsWith("jav") || href.startsWith("mail")) {
                return null;
            } else {
                if (href.endsWith("htm") || href.endsWith("htm") || href.endsWith("asp") || href.endsWith("aspx") || href.endsWith("php") || href.endsWith("jsp")) {
                    return new URL(redirected, href);
                } else {
                    return null;
                }
            }
        } catch (MalformedURLException e) {
            addException(e);
            return null;
        }
    }

    private void addException(Exception exception) {
        ex e = new ex(target, System.currentTimeMillis(), stage, exception);
        exlist.add(e);
    }

    /**
     * set fetch target
     */
    public void setTarget(URL url) {
        this.target = url;
    }

    /**
     * get redirected URL
     */
    public URL getRedirected() {
        return redirected;
    }

    /**
     * get the base of the fetched page
     */
    public String getBase() {
        return base;
    }

    /**
     * get body of the fetched page
     */
    public String getBody() {
        return body;
    }

    /**
     * get the content of the fetched page
     */
    public String getContent() {
        return content;
    }

    /**
     * get the title of the fetched page
     */
    public String getTitle() {
        return title;
    }

    /**
     * get anchors of the fetched page
     */
    public tags getAnchors() {
        return anchors;
    }

    /**
     * getFetched
     */
    public long getFetched() {
        return fetched;
    }

    /**
     * isRunning
     */
    public boolean isRunning() {
        return run;
    }

    /**
     * getAverageStageDelay
     */
    public double getAverageStageDelay(int s) {
        return delta[s] / count[s];
    }

    /**
     * getAverageStageDelay
     */
    public int getStageCount(int s) {
        return count[s];
    }

    public String popException() {
        String s = "";
        while (!exlist.isEmpty()) {
            s = "\t" + exlist.remove(0).e.toString() + s;
        }
        return s;
    }
}
