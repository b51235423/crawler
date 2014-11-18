/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 *
 * @author turtlepool
 */
public class Crawler implements Runnable {

    //
    public static final int Stages = 5;

    //singleton
    private static Crawler instance = null;

    //stage semaphores
    private Semaphore[] sems = new Semaphore[Stages];

    //queue
    private Queue<URL> queue = new LinkedList<URL>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //get singleton and run
        //(new Thread(getInstance())).start();

        //test
        worker w = new worker();
        try {
            //w.setTarget(new URL("http://stackoverflow.com/"));
            w.setTarget(new URL("http://disp.cc/b"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        w.work(1);
        //tags t = new tags("base",w.getContent());
        //t.list.forEach(s -> System.out.println(s.attribute("href")));
        w.work(2);
        System.out.println("base=" + w.getBase());
        System.out.println("redi=" + w.getRedirected());
        w.work(3);
        w.work(4);
        //w.getAnchors().list.forEach(s -> System.out.println(s.attribute("href")));
        getInstance().getQueue().forEach(s -> System.out.println(s.toString()));

    }

    /**
     * get a running instance
     */
    public static Crawler getInstance() {
        //double-check locking
        synchronized (Crawler.class) {
            if (instance == null) {
                instance = new Crawler();
            }
        }
        return instance;
    }

    /**
     * private constructor for singleton
     */
    private Crawler() {
        for (Semaphore s : sems) {
            s = new Semaphore(1, true);
        }
    }

    /**
     * run
     */
    public void run() {

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
    public Queue<URL> getQueue() {
        return queue;
    }
}
