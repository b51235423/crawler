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
import javax.swing.JOptionPane;

/**
 *
 * @author turtlepool
 */
public class crawler implements Runnable {

    //
    public static final int Stages = 5;

    //singleton
    private static crawler instance = null;
    private static Object p;

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
            //w.setTarget(new URL("http://www.mobile01.com/"));
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
        System.out.println("body=" + w.getBody());
        BufferedWriter bufWriter;
        File file;
        try {
            file = new File("test.txt");
            bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false)));
            bufWriter.write(w.getBody());
            bufWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        w.work(3);
        w.work(4);
        //w.getAnchors().list.forEach(s -> System.out.println(s.attribute("href")));
        getInstance().getQueue().forEach(s -> System.out.println(s.toString()));

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
