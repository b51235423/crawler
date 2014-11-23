/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import crawler.tags.tag;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 *
 * @author turtlepool
 */
public class worker implements Runnable {

    public static final int Limit = 512, ConnTimeOut = 100, ReadTimeOut = 500, MaxContentSize = 100000;
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36";

    //queue
    private queue q;

    //stage related
    private boolean run = true;
    private int stage = 0;
    private Semaphore[] sems;

    //page related
    private int fetched = 0;
    private double[] delta = new double[crawler.Stages], count = new double[crawler.Stages];
    private String base = "", body = "", content = "", title = "";
    private tags anchors = null;
    private URL target = null;
    private URL redirected = null;

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
        long t = System.currentTimeMillis();
        while (run) {
            try {
                sems[stage].acquire();
                work();
                sems[stage].release();
            } catch (InterruptedException e) {
                run = false;
                e.printStackTrace();
            }
            if (fetched >= Limit) {
                run = false;
            }
        }
        t = System.currentTimeMillis() - t;
        System.out.println(hashCode() + " fetched= " + fetched + " time=" + t + " avg=" + (fetched * 1.0 / t) * 1000 + "pages/s");
        for (int i = 0; i < delta.length; ++i) {
            System.out.println("stage " + i + " count=" + count[i] + " avg=" + delta[i] / count[i]);
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
                t = System.currentTimeMillis() - t;
                delta[stage] += t;
                ++count[stage];
                //System.out.println(hashCode() + " Stage " + stage + " time=" + t);
                stage = b ? stage + 1 : stage;
                break;
            case 1:
                b = fetch();
                t = System.currentTimeMillis() - t;
                delta[stage] += t;
                ++count[stage];
                //System.out.println(hashCode() + " Stage " + stage + " time=" + t);
                stage = b ? stage + 1 : 0;
                break;
            case 2:
                b = process();
                t = System.currentTimeMillis() - t;
                delta[stage] += t;
                ++count[stage];
                //System.out.println(hashCode() + " Stage " + stage + " time=" + t);
                stage = b ? stage + 1 : stage;
                break;
            case 3:
                b = store();
                t = System.currentTimeMillis() - t;
                delta[stage] += t;
                ++count[stage];
                //System.out.println(hashCode() + " Stage " + stage + " time=" + t);
                stage = b ? stage + 1 : stage;
                break;
        }
        stage = stage % crawler.Stages;
    }

    /**
     * poll a url from the queue
     */
    public boolean poll() {
        if (target != null) {
            anchors.list.forEach(s -> {
                URL u = parseHttpRef(s.attribute("href"));
                if (u != null) {
                    q.offer(u);
                }
            });
            clear();
        }
        target = q.poll();
        return target != null;
    }

    /**
     * fetch page from the Internet
     */
    public boolean fetch() {
        System.out.print(hashCode() + " " + target.toString());
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
            e.printStackTrace();
            clear();
            return false;
        }

        //get charset
        type = conn.getContentType() == null ? "" : conn.getContentType();
        if (!type.startsWith("text/html")) {
            clear();
            return false;
        }
        if (type.indexOf("charset=") >= 0) {
            charset = type.substring(type.indexOf("charset=") + 8, type.length());
        }
        System.out.print(" type=" + type + " charset=" + charset + " response=" + response + " len=" + conn.getContentLength());

        //get content type
        if (response == 200) {
            //read page
            try {
                InputStream in = conn.getInputStream();
                content = new String(readStream(in), charset);
            } catch (Exception e) {
                e.printStackTrace();
                clear();
                return false;
            }
        } else {
            clear();
            return false;
        }
        //System.out.println("len=" + content.length());

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
        while ((len = inStream.read(buffer)) != -1 && outStream.size() < MaxContentSize) {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        inStream.close();
        return outStream.toByteArray();
    }

    /**
     * clear
     */
    public void clear() {
        target = redirected = null;
        anchors = null;
        base = body = content = title = "";
    }

    /**
     * process the fetched page
     */
    public boolean process() {
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

        //get pure text body of the page
        body = content;
//        tags bodytags = new tags("body", content);
//        if (bodytags.list.isEmpty()) {
//            body = content;
//        } else {
//            body = bodytags.getFirstTag().filter();
//        }

        return true;
    }

    /**
     * store the content of the fetched page into database
     */
    public boolean store() {
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

        //count
        //System.out.println(hashCode() + " fetched=" + fetched);
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
            e.printStackTrace();
            return null;
        }
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
}
