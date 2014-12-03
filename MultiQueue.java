/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 *
 * @author turtlepool
 */
public class MultiQueue {

    public static final int DefaultPriority = 1000, PopLimit = 10, QueueLimit = 512, SubQueueLimit = 512;

    //random
    private Random r = new Random();

    //queue
    private LinkedList<subqueue<URL>> queue = new LinkedList<>();

    public MultiQueue() {
        //test
        try {
            offer(new URL("http://www.mobile01.com/"));
            offer(new URL("http://disp.cc/b"));
            offer(new URL("http://www.msn.com/zh-tw"));
            offer(new URL("http://chinese.engadget.com/"));
            offer(new URL("http://www.techbang.com/"));
            offer(new URL("https://www.pixnet.net/"));
            offer(new URL("http://www.pttbook.com/"));
            offer(new URL("http://www.teepr.com/"));
            offer(new URL("http://www.juksy.com/"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void offer(URL url) {
        for (Queue<URL> q : queue) {
            if (q.peek().getHost().toString().equals(url.getHost().toString())) {
                q.offer(url);
                return;
            }
        }
        subqueue<URL> q = new subqueue<URL>();
        q.add(url);
        q.setPriority(0, Math.floor(r.nextDouble() * DefaultPriority));
        if (queue.size() < QueueLimit) {
            queue.add(q);
        } else {
            queue.remove(queue.size() - 1);
            queue.add(q);
        }
        sort(0, queue.size() - 1);
    }

    public URL poll() {
        URL url = null;
        if (!queue.isEmpty()) {
            int x = r.nextInt(Math.min(queue.size(), PopLimit));
            //int x = r.nextInt(Math.max(Queue.size() / 10, 1));
            Queue<URL> q = queue.get(x);
            url = q.poll();
            if (q.isEmpty()) {
                queue.remove(q);
            }
        }
        return url;
    }

    public void setPriority(URL url, int d, double v) {
        if (!queue.isEmpty()) {
            return;
        }
        int i = 0;
        for (subqueue<URL> q : queue) {
            if (q.peek().getHost().toString().equals(url.getHost().toString())) {
                q.setPriority(d, v);
                sort(0, queue.size() - 1);
                return;
            }
            ++i;
        }
    }

    public double getPriority(URL url) {
        int i = 0;
        for (subqueue<URL> q : queue) {
            if (q.peek().getHost().toString().equals(url.getHost().toString())) {
                return q.getPriority();
            }
            ++i;
        }
        return 0.5 * DefaultPriority;
    }

    public double getPriority(URL url, int d) {
        int i = 0;
        for (subqueue<URL> q : queue) {
            if (q.peek().getHost().toString().equals(url.getHost().toString())) {
                return q.getPriority(d);
            }
            ++i;
        }
        return 0.5 * DefaultPriority;
    }

    public void sort(int from, int to) {
        //System.out.println("sort to=" + to);
        double pivot = queue.get(to).getPriority();
        int m = 0, n = 0;
        for (int i = from; i < to; ++i) {
            if (pivot > queue.get(i).getPriority()) {
                queue.add(from + m, queue.remove(i));
                ++m;
            } else {
                queue.add(from + m + n, queue.remove(i));
                ++n;
            }
        }
        queue.add(from + m, queue.remove(to));
        if (m > 2) {
            sort(from, from + m - 1);
        }
        if (n > 2) {
            sort(from + m + 1, to);
        }
    }

    public int count() {
        return queue.size();
    }

    public void show() {
        int i = 0;
        System.out.println("queues=" + queue.size());
        for (Queue<URL> q : queue) {
            System.out.println(i + " p=" + queue.get(i).getPriority() + " host=" + q.peek().getHost().toString() + " size=" + q.size());
            ++i;
        }
    }

    public String showToLimit() {
        int i = 0;
        String s = "queues=" + queue.size();
        for (Iterator<subqueue<URL>> it = queue.iterator(); it.hasNext();) {
            Queue<URL> q = it.next();
            s += "\n" + i + " p=" + queue.get(i).getPriority() + " host=" + q.peek().getHost().toString() + " size=" + q.size();
            if (i == PopLimit) {
                break;
            }
            ++i;
        }
        return s;
    }

    public enum priority {

        PrFetchTime, PrExceptions
    }

    public class subqueue<E> extends LinkedList<E> implements Queue<E> {

        private double[] p = new double[priority.values().length];

        public void setPriority(int d, double v) {
            p[d] = v;
        }

        public double getPriority() {
            return p[priority.PrFetchTime.ordinal()] + p[priority.PrExceptions.ordinal()];
        }

        public double getPriority(int d) {
            return p[d];
        }

        public boolean offer(E element) {
            //element exists in sub Queue
            for (Iterator<E> it = iterator(); it.hasNext();) {
                if (it.next().toString().equals(element.toString())) {
                    return false;
                }
            }
            //size of sub Queue reaches the limit
            if (size() >= SubQueueLimit) {
                poll();
            }
            return super.offer(element);
        }
    }
}
