/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

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
        //test
        try {
            crawler.getInstance().q.offer(new URL("http://www.mobile01.com/"));
            crawler.getInstance().q.offer(new URL("http://disp.cc/b"));
            crawler.getInstance().q.offer(new URL("http://www.msn.com/zh-tw"));
            crawler.getInstance().q.offer(new URL("http://chinese.engadget.com/"));
            crawler.getInstance().q.offer(new URL("http://www.techbang.com/"));
            crawler.getInstance().q.offer(new URL("https://www.pixnet.net/"));
            crawler.getInstance().q.offer(new URL("http://www.pttbook.com/"));
            crawler.getInstance().q.offer(new URL("http://www.teepr.com/"));
            crawler.getInstance().q.offer(new URL("http://www.juksy.com/"));
            crawler.getInstance().q.offer(new URL("http://janettoer.pixnet.net/blog"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        (new Thread(new worker())).start();
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
