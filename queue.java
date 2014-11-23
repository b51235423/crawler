/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 *
 * @author turtlepool
 */
public class queue {

    Random r = new Random();

    //queue
    private LinkedList<Queue<URL>> queue = new LinkedList<>();

    public void offer(URL url) {
        for (Queue<URL> q : queue) {
            if (q.peek().getHost().toString().equals(url.getHost().toString())) {
                q.offer(url);
                return;
            }
        }
        Queue<URL> q = new LinkedList<URL>();
        q.add(url);
        queue.add(q);
    }

    public URL poll() {
        URL url = null;
        if (!queue.isEmpty()) {
            int x = r.nextInt(queue.size());
            Queue<URL> q = queue.get(x);
            url = q.poll();
            if (q.isEmpty()) {
                queue.remove(q);
            }
        }
        return url;
    }

    public int count() {
        return queue.size();
    }

    public void show() {
        queue.forEach(q -> System.out.println("host=" + q.peek().getHost().toString() + " size=" + q.size()));
    }
}
