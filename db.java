/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crawler;

import java.net.URL;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteResult;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ParallelScanOptions;
import java.net.UnknownHostException;
import java.util.Date;

import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 * @author turtlepool
 */
public class db {

    public final static String DefaultHost = "localhost";
    public final static int DefaultPort = 27017;
    public final static String DefaultDB = "default";
    public final static String DefaultCollection = "default";

    //singleton
    private static db instance = null;

    //mongo db object
    private MongoClient mongoClient = null;
    private DB db = null;
    private DBCollection coll = null;

    /**
     * get a default running instance
     */
    public static db getInstance() {
        return getInstance(DefaultHost, DefaultPort, DefaultDB, DefaultCollection);
    }

    /**
     * get a running instance
     */
    public static db getInstance(String host, int port, String DB, String collection) {
        //double-check locking
        synchronized (db.class) {
            if (instance == null) {
                instance = new db(host, port, DB, collection);
            }
        }
        return instance;
    }

    /**
     * private constructor for singleton
     */
    private db(String host, int port, String DB, String collection) {
        try {
            //connect to database
            mongoClient = new MongoClient(host, port);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
            return;
        }
        db = mongoClient.getDB(DB);
        coll = db.getCollection(collection);
    }

    public void close() {
        mongoClient.close();
    }

    public boolean update(URL url, String content) {
        if (!isVisited(url)) {
            BasicDBObject doc = new BasicDBObject("url", url.toString())
                    .append("type", url.getProtocol())
                    .append("tag", "html")
                    .append("content", content)
                    .append("time", new Date().getTime());
            coll.insert(doc);
            return true;
        }
        return false;
    }

    public boolean isVisited(URL url) {
        BasicDBObject query = new BasicDBObject("url", url.toString());
        DBCursor cursor = coll.find(query);
        try {
            while (cursor.hasNext()) {
                return true;
            }
        } finally {
            cursor.close();
        }
        return false;
    }

    public long count() {
        BasicDBObject query = new BasicDBObject("tag", "html");
        return coll.count(query);
    }

    public void show() {
        BasicDBObject query = new BasicDBObject("tag", "html");
        DBCursor cursor = coll.find(query);
        try {
            while (cursor.hasNext()) {
                System.out.println(cursor.next());
            }
        } finally {
            cursor.close();
        }
    }

    public void clear() {
        BasicDBObject query = new BasicDBObject("tag", "html");
        DBCursor cursor = coll.find(query);
        try {
            while (cursor.hasNext()) {
                coll.remove(cursor.next());
            }
        } finally {
            cursor.close();
        }
    }
}
