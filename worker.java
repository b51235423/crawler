/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import crawler.Tag.tag;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
public class Worker implements Runnable {

    public static final int FetchLimit = 1000, ConnTimeOut = 200, ReadTimeOut = 500;
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36";

    //queue
    private MultiQueue q;

    //stage related
    private boolean run = true;
    private double[] delay = new double[stage.values().length];
    private double[] delaysum = new double[stage.values().length];
    private stage currentstage = stage.StagePoll;
    private int[] count = new int[stage.values().length];
    private long fetched = 0;
    private Semaphore[] sems;
    public String queuestr = "";

    //page related
    private String base = "", body = "", content = "", title = "";
    private Tag anchors = null;
    private URL target = null, redirected = null, fail = null;

    //Exceptions
    private LinkedList<ex> exlist = new LinkedList<>();

    /**
     * worker constructor
     */
    public Worker() {
        sems = Crawler.getInstance().getSemaphore();
        q = Crawler.getInstance().getQueue();
    }

    /**
     * do works with the stage order
     */
    public void run() {
        while (run) {
            try {
                sems[currentstage.ordinal()].acquire();
                work();
                sems[currentstage.ordinal()].release();
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
        boolean b = true;
        long t = System.currentTimeMillis();
        try {
            switch (currentstage) {
                case StagePoll:
                    poll();
                    break;
                case StageFetch:
                    fetch();
                    break;
                case StageProcess:
                    process();
                    break;
                case StageStore:
                    store();
                    break;
            }
        } catch (Exception ex) {
            b = false;
            clear(ex);
        }
        t = System.currentTimeMillis() - t;
        delay[currentstage.ordinal()] = t;
        delaysum[currentstage.ordinal()] += t;
        ++count[currentstage.ordinal()];
        currentstage = stage.values()[b ? (currentstage.ordinal() + 1) % stage.values().length : 0];
    }

    /**
     * poll a url from the Queue
     */
    public void poll() throws Exception {
        //queue operations
        //push urls into Queue if this Worker is already worked for a full cycle 
        if (target != null) {
            anchors.list.forEach(s -> {
                URL u = parseHttpRef(s.attribute("href"));
                if (u != null) {
                    q.offer(u);
                }
            });

            //update fetch time
            q.setPriority(target, MultiQueue.priority.PrFetchTime.ordinal(), Math.floor(delay[stage.StageFetch.ordinal()]));
            clear(null);
        } else if (fail != null) {
            q.setPriority(fail, MultiQueue.priority.PrExceptions.ordinal(), q.getPriority(fail, MultiQueue.priority.PrExceptions.ordinal() * 2));
            fail = null;
        }

        //pop a url from MultiQueue
        target = q.poll();
        if (target == null || Database.getInstance().isVisited(target)) {
            throw new Exception("Target Fetched Exception");
        }

        queuestr = q.showToLimit();
    }

    /**
     * fetch page from the Internet
     */
    public void fetch() throws Exception {
        //network operations
        int response = 0;
        String type = "", charset = "UTF-8";
        HttpURLConnection conn = null;

        //open connection
        conn = (HttpURLConnection) target.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", UserAgent);
        conn.setConnectTimeout(ConnTimeOut);
        conn.setReadTimeout(ReadTimeOut);
        conn.connect();
        response = conn.getResponseCode();

        //get charset
        type = conn.getContentType() == null ? "" : conn.getContentType();
        if (!type.startsWith("text/html")) {
            throw new Exception("Content Type Exception:" + type);
        }
        if (type.indexOf("charset=") >= 0) {
            charset = type.substring(type.indexOf("charset=") + 8, type.length());
        }

        //get content type
        if (response == 200) {
            //read page
            InputStream in = conn.getInputStream();
            content = new String(readStream(in), charset);
        } else {
            throw new Exception("Response Code Exception:" + response);
        }

        //get redirected url
        redirected = conn.getURL();
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
        System.out.println(hashCode() + " S" + currentstage + " clear=" + target.toString() + " e=" + (e != null) + " ft=" + delay[stage.StageFetch.ordinal()]);
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
    public void process() throws Exception {
        //cpu operations
        //get base of the page
        Tag t = new Tag("base", content);
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
        t = new Tag("title", content);
        t.list.forEach(s -> title = s.getText());

        //get anchors
        anchors = new Tag("a", content);
        if (anchors.list.isEmpty()) {
            throw new Exception("Empty Link Exception");
        }

        //get pure text body of the page
        Tag bodytags = new Tag("body", content);
        if (bodytags.list.isEmpty()) {
            body = content;
        } else {
            body = bodytags.getFirstTag().filter();
        }
    }

    /**
     * store the content of the fetched page into database
     */
    public void store() throws Exception {
        //disk operations
        //store
        if (Database.getInstance().update(target, body)) {
            ++fetched;
        }
        if (!redirected.toString().equals(target.toString())) {
            Database.getInstance().update(redirected, body);
        }

        //delete visited link
        for (Iterator<tag> it = anchors.list.iterator(); it.hasNext();) {
            tag t = it.next();
            URL u = parseHttpRef(t.attribute("href"));
            if (u == null || Database.getInstance().isVisited(u)) {
                it.remove();
            }
        }
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
        ex e = new ex(target, System.currentTimeMillis(), currentstage.ordinal(), exception);
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
    public Tag getAnchors() {
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
        return delaysum[s] / count[s];
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
            ex e = exlist.remove();
            s = "\nt=" + e.time + " url=" + e.url.getHost().toString() + " ex=" + e.e.toString() + s;
        }
        return s;
    }

    public enum stage {

        StagePoll, StageFetch, StageProcess, StageStore
    }

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
}
