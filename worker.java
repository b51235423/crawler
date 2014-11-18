/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 *
 * @author turtlepool
 */
public class worker implements Runnable {

    public static final int ConnTimeOut = 8000;
    public static final int ReadTimeOut = 8000;
    public static final String UserAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36";

    private boolean run = true;
    private int stage = 0;
    private Semaphore[] sems;
    private Queue<URL> queue;
    private URL target = null;
    private URL redirected = null;
    private String content = "", title = "", base = "";
    private tags anchors = null;

    public worker() {
        this.sems = Crawler.getInstance().getSemaphore();
        this.queue = Crawler.getInstance().getQueue();
    }

    public void run() {
        while (run) {
            try {
                sems[stage].acquire();
                boolean b = work(stage);
                sems[stage].release();
                stage = b ? stage + 1 : stage;
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public boolean work(int s) {
        boolean b = false;
        long t = System.currentTimeMillis();
        switch (s) {
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
                b = offer();
                break;
        }
        t = System.currentTimeMillis() - t;
        System.out.println("Stage " + s + " time=" + t);
        return b;
    }

    public boolean poll() {
        target = queue.poll();
        return target != null;
    }

    public boolean fetch() {
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
            return false;
        }

        //get charset
        type = conn.getContentType() == null ? "" : conn.getContentType();
        if (type.indexOf("charset=") >= 0) {
            charset = type.substring(type.indexOf("charset=") + 8, type.length());
        }
        System.out.println("type=" + type + " charset=" + charset + " response=" + response);

        //get content type
        if (response == 200) {
            //read page
            try {
                InputStream in = conn.getInputStream();
                byte[] out = readStream(in);
                content = new String(out, charset);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }

        //get redirected url
        redirected = conn.getURL();

        //try to do garbage collection 
        conn = null;

        return true;
    }

    public byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = inStream.read(buffer)) != -1) {
            outSteam.write(buffer, 0, len);
        }
        outSteam.close();
        inStream.close();
        return outSteam.toByteArray();
    }

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

        //store
        return true;
    }

    public boolean offer() {
        anchors.list.forEach(s -> {
            URL u = parseHttpRef(s.attribute("href"));
            if (u != null && !isVisited(u)) {
                queue.offer(u);
            }
        });

        //
        target = redirected = null;
        content = title = base = "";
        anchors = null;

        return true;
    }

    public URL parseHttpRef(String href) {
        try {
            if (href.startsWith("//")) {
                return new URL(redirected.getProtocol() + ":" + href);
            } else if (href.startsWith("/")) {
                return new URL(redirected.getProtocol() + "://" + redirected.getAuthority() + href);
            } else if (href.startsWith("http")) {
                return new URL(href);
            } else if (href.startsWith("java")) {
                return null;
            } else if (href.startsWith("mail")) {
                return null;
            } else {
                return new URL(redirected, href);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isVisited(URL url) {
        return false;
    }

    public void setTarget(URL url) {
        this.target = url;
    }

    public URL getRedirected() {
        return redirected;
    }

    public String getContent() {
        return content;
    }

    public String getBase() {
        return base;
    }

    public String getTitle() {
        return title;
    }

    public tags getAnchors() {
        return anchors;
    }
}
