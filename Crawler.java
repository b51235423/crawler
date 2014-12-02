/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.net.URL;
import java.util.concurrent.Semaphore;

/**
 *
 * @author turtlepool
 */
public class crawler implements Runnable {

    //
    public static final int Stages = 4;

    //singleton
    private static crawler instance = null;

    //stage semaphores
    private Semaphore[] sems = new Semaphore[Stages];

    //queue
    private queue q = new queue();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //get singleton and run
        //(new Thread(getInstance())).start();

        db.getInstance().clear();
        worker w = new worker();
        (new Thread(w)).start();
        crawler.getInstance().startListener(w);

        //test();
    }

    public static void test() {
        worker w = new worker();
        w.work();
        w.work();
        tags t = new tags("base", w.getContent());
        t.list.forEach(s -> System.out.println(s.attribute("href")));
        w.work();
        System.out.println("base=" + w.getBase());
        System.out.println("redi=" + w.getRedirected());
        System.out.println("body=" + w.getBody());
        w.work();
        w.getAnchors().list.forEach(s -> System.out.println(s.attribute("href")));
        w.work();
        //getInstance().getQueue().forEach(s -> System.out.println(s.toString()));
        //db.getInstance().show();
        //crawler.getInstance().q.show();
    }

    /**
     * get a running instance
     */
    public static crawler getInstance() {
        //double-check locking
        synchronized (crawler.class) {
            if (instance == null) {
                instance = new crawler();
            }
        }
        return instance;
    }

    /**
     * private constructor for singleton
     */
    private crawler() {
        //semaphore
        for (int i = 0; i < Stages; ++i) {
            sems[i] = new Semaphore(1, true);
        }
    }

    /**
     * run
     */
    public void run() {
        //initial site
        try {
            q.offer(new URL("http://www.mobile01.com/"));
            q.offer(new URL("http://disp.cc/b"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //start workers
        worker[] workers = new worker[5];
        for (int i = 0; i < workers.length; ++i) {
            workers[i] = new worker();
        }
    }

    public void startListener(worker w) {
        debugger d = new debugger();
        d.setVisible(true);
        new Thread(new Runnable() {
            public void run() {
                long t = System.currentTimeMillis();
                while (w.isRunning()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    double s = r((System.currentTimeMillis() - t) / 1000);
                    d.updateTitle(w.hashCode() + " t=" + s + "s fetched=" + w.getFetched() + " avg=" + r(w.getFetched() / s) + "pages/s");
                    for (int i = crawler.Stages - 1; i >= 0; --i) {
                        d.updateSMsg("\tS" + i + " c=" + w.getStageCount(i) + " avg=" + r(w.getAverageStageDelay(i)) + "ms");
                    }
                    d.updateSMsg(w.hashCode() + " t=" + s + "s fetched=" + w.getFetched() + " avg=" + r(w.getFetched() / s) + "pages/s");
                    d.updateSMsg("\n\n");
                    d.updateQMsg(w.queuestr + "\n\n");
                    d.updateQMsg(w.hashCode() + " t=" + s + "\n");
                    d.updateEMsg(w.hashCode() + " t=" + s + "s " + w.popException() + "\n\n");
                }
                System.out.println(w.hashCode() + " Summary");
                System.out.println(w.hashCode() + " DB.count=" + db.getInstance().count());
                q.show();
            }
        }).start();
    }

    public double r(double d) {
        return Math.round(d * 100) * 1.0 / 100;
    }

    /**
     * getSemaphore
     */
    public Semaphore[] getSemaphore() {
        return sems;
    }

    /**
     * getQueue
     */
    public queue getQueue() {
        return q;
    }
}
