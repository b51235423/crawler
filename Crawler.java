/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.net.URL;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        new Thread(new Runnable() {
            public void run() {
                long t = System.currentTimeMillis();
                while (w.isRunning()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    double s = (System.currentTimeMillis() - t) / 1000;
                    System.out.printf("%d fetched=%8d t=%1fs avg=%8fpages/s", w.hashCode(), w.getFetched(), s, w.getFetched() / s);
                    for (int i = 0; i < crawler.Stages; ++i) {
                        System.out.printf("\tS%d c=%5d avg=%5fms", i, w.getStageCount(i), (float) w.getAverageStageDelay(i));
                    }
                    System.out.println(w.queuestr);
                }
                System.out.println(w.hashCode() + " Summary");
                System.out.println(w.hashCode() + " DB.count=" + db.getInstance().count());
                q.show();
            }
        }).start();
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
