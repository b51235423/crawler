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
public class queue {

    public static final int DefaultPriority = 500, PopLimit = 10;
    //public static final double R = 0.1;

    Random r = new Random();

    //queue
    private LinkedList<Queue<URL>> queue = new LinkedList<>();
    private LinkedList<Integer> priority = new LinkedList<>();

    public class pqueue<T> extends LinkedList<T> implements Queue<T> {

        private double[] vector;

        public int getPriority() {
            return 0;
        }
    }

    public queue() {
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
            offer(new URL("http://janettoer.pixnet.net/blog"));
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
        Queue<URL> q = new LinkedList<URL>();
        q.add(url);
        int x = r.nextInt(Math.max(queue.size(), 1));
        queue.add(x, q);
        priority.add(x, r.nextInt(DefaultPriority));
        sort(0, queue.size() - 1);
    }

    public URL poll() {
        URL url = null;
        if (!queue.isEmpty()) {
            int x = r.nextInt(Math.min(queue.size(), PopLimit));
            //int x = r.nextInt(Math.max(queue.size() / 10, 1));
            Queue<URL> q = queue.get(x);
            url = q.poll();
            if (q.isEmpty()) {
                queue.remove(q);
            }
        }
        return url;
    }

    public void setPriority(URL url, int p) {
        int i = 0;
        for (Queue<URL> q : queue) {
            if (q.peek().getHost().toString().equals(url.getHost().toString())) {
                priority.set(i, p);
                if (!priority.isEmpty()) {
                    sort(0, queue.size() - 1);
                }
                return;
            }
            ++i;
        }
    }

    public void fadeout(URL url) {
        for (int i = 0; i < queue.size() - PopLimit; ++i) {
            Queue<URL> q = queue.get(i);
            if (q.peek().toString().equals(url.toString())) {
                //queue.add(i + PopLimit, queue.remove(i));
                queue.remove(i);
                return;
            }
        }
    }

    public int getPriority(URL url) {
        int i = 0;
        for (Queue<URL> q : queue) {
            if (q.peek().getHost().toString().equals(url.getHost().toString())) {
                return priority.get(i);
            }
            ++i;
        }
        return getMidPriority();
    }

    public int getMidPriority() {
        if (priority.isEmpty()) {
            return DefaultPriority;
        } else {
            return priority.get(priority.size() / 2);
        }
    }

    public void sort(int from, int to) {
        //System.out.println("sort to=" + to);
        int m = 0, n = 0, pivot = priority.get(to);
        for (int i = from; i < to; ++i) {
            if (pivot > priority.get(i)) {
                queue.add(from + m, queue.remove(i));
                priority.add(from + m, priority.remove(i));
                ++m;
            } else {
                queue.add(from + m + n, queue.remove(i));
                priority.add(from + m + n, priority.remove(i));
                ++n;
            }
        }
        queue.add(from + m, queue.remove(to));
        priority.add(from + m, priority.remove(to));
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
        for (Queue<URL> q : queue) {
            System.out.println(i + "\tp=" + priority.get(i) + " host=" + q.peek().getHost().toString() + " size=" + q.size());
            ++i;
        }
    }

    public String showToLimit() {
        int i = 0;
        String s = "";
        for (Iterator<Queue<URL>> it = queue.iterator(); it.hasNext();) {
            Queue<URL> q = it.next();
            s += i + "\n\thost=" + q.peek().getHost().toString() + " size=" + q.size();
            if (i == PopLimit) {
                break;
            }
            ++i;
        }
        return s;
    }
}
