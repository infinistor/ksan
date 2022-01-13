/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.objmanager.test;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package com.pspace.ifs.DSAN.ObjManger;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

/**
 *
 * @author legesse
 */
public class DBPerformanceTest {

    static int mcount = 0;
    static long reportPUnit = 1000000;
    static long maxNumJob = 5000000;
    static long num_entry = 0;
    static boolean isFinished;
    static long startTime = System.nanoTime();
    static long startTime1m = System.nanoTime();
    static String bucket = "testvol3";

    static long numJobPerT = 125000; // 125000

    static ReentrantLock lock = new ReentrantLock();

    static ReentrantLock pathlock = new ReentrantLock();
    
    static void countEntry() {
        long diff;

        lock.lock();
        try {
            if (((num_entry + 1) % reportPUnit) == 0) {
                diff = System.nanoTime() - startTime1m;
                System.out.format("> %d  : Objects(%d/%d) create excution time  : %d nanosecond \n", mcount, reportPUnit, num_entry + 1, diff);
                startTime1m = System.nanoTime();
                mcount++;
            }
            num_entry++;
        } finally {
            lock.unlock();
        }
    }

    static void createBucket() {
        ObjManager om = new ObjManager();;
        try {
            try {
                om.createBucket(bucket,  "user", "acl");
            } catch (ResourceNotFoundException ex) {
                Logger.getLogger(DBPerformanceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ResourceAlreadyExistException ex) {
            System.out.println("Bucket already exist!");
        }
    }

    static String getPath(String subPath, long idx ){
        String path;
        pathlock.lock();
        try {
            path = subPath + idx;
        }
        finally{
            pathlock.unlock();
        }
        return path;
    }
    
    static void createFileOnDBPerformanceTest(String name) {
        String path;
        String tmp = bucket + "testDir_" + name + "/test";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        String deleteMarker = "dmarker";

        ObjManager om = new ObjManager();

        try {

            for (int idx = 0; idx < (int) numJobPerT; idx++) { //500000
                path = getPath(tmp, idx);
                Metadata mt = om.create(bucket, path); // to create on round robin osd disk
                mt.setMeta(meta);
                mt.setEtag(etag);
                mt.setTag(tag);
                mt.setAcl(acl);
                mt.setVersionId("nil", deleteMarker, true);
                om.close(bucket, path, mt);
                countEntry();
            }
            //System.out.println("End idx : " + num_entry);
        } catch (IOException | InvalidParameterException | AllServiceOfflineException | ResourceNotFoundException ex) {
            System.out.println("Error : " + ex.getMessage());

        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        
        if (args.length >= 1)
            bucket=args[0];
        
        if (args.length >=2){
            maxNumJob = Long.parseLong(args[1]);
            numJobPerT = maxNumJob / 40;
            if (maxNumJob > 1000000)
                reportPUnit = 1000000;
            else if(maxNumJob <= 1000000 && maxNumJob > 100000)
                reportPUnit=100000;
            else if(maxNumJob <= 100000 && maxNumJob > 10000)
                reportPUnit=10000;
            else
                reportPUnit=1000;
        }
        
        System.out.println("Bucket Name : " + bucket + " len " + args.length);
        createBucket();
        startTime = System.nanoTime();
        startTime1m = System.nanoTime();
        isFinished = false;
        
        for (int tidx = 1; tidx <= 40; tidx++) {
            new Thread("" + tidx) {
                @Override
                public void run() {
                    long diff;
                    System.out.println("Thread: " + getId() + " running");
                    createFileOnDBPerformanceTest("Thread_" + getId());
                    
                    //System.out.format("maxNumJob : %d  num_entry : %d  isFinished : %s \n", maxNumJob,num_entry ,  isFinished);
                    if ((maxNumJob == num_entry )&& (isFinished == false)){
                        diff = System.nanoTime() - startTime;
                        System.out.println("Total " + num_entry + " create excution time  : " + diff + " nanosecond ");
                        isFinished = true;
                        System.exit(0); 
                    }
                }
               
            }.start();
        }

        
    }
    
}
