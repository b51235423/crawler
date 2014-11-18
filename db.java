/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.net.URL;

/**
 *
 * @author turtlepool
 */
public class db {

    //singleton
    private static db instance = null;

    /**
     * get a running instance
     */
    public static db getInstance() {
        //double-check locking
        synchronized (db.class) {
            if (instance == null) {
                instance = new db();
            }
        }
        return instance;
    }

    /**
     * private constructor for singleton
     */
    private db() {
        //connect to database
    }

    public void update(URL url, String content) {

    }

    public boolean isVisited(URL url) {
        return false;
    }
}
