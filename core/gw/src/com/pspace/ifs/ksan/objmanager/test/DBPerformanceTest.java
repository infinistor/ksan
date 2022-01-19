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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author legesse
 */
public class DBPerformanceTest extends Thread{

    static int mcount = 0;
    static long reportPUnit = 1000000;
    static long maxNumJob = 5000000;
    static long num_entry = 0;
    static boolean isFinished;
    static long startTime = System.nanoTime();
    static long startTime1m = System.nanoTime();
    static String bucket = "testvol3";
    static String userName="user1";
    static int idx_job = 0;
    static int notDone_job = 0;
    static boolean timeInitOn=false;
    
    // for read entry
    static int mReadCount = 0;
    static long num_ReadEntry = 0;
    static long startReadTime = 0;
    static long startReadTime1m = 0;
    static boolean isReadFinished;

    static long numJobPerT = 125000; // 125000

    static ReentrantLock lock = new ReentrantLock();

    static ReentrantLock pathlock = new ReentrantLock();
    
    static ReentrantLock indexLock = new ReentrantLock();
    
    static CountDownLatch latch = new CountDownLatch(40);
    
    static int getIndex(String str){
        indexLock.lock();
        try {
            if (idx_job >= maxNumJob)
                return -1;
            idx_job++;
            notDone_job++;
            //System.out.format("ThreadId : %s index : %d num_entry : %d\n",  str, idx_job, num_entry);
            return idx_job;
        } finally {
            indexLock.unlock();
        }
    }
    
    static void returnIndex(){
        indexLock.lock();
        try {
            /*if (idx_job > maxNumJob)
               return;
            if (idx_job <= 0)
                return;
            
            idx_job--;*/
            if (notDone_job <= 0)
                return;
            
            notDone_job--;

        } finally {
            indexLock.unlock();
        }
    }
    static void getConfig(){
        InputStream is = null;
        try {
            Properties prop = new Properties();
            is = new FileInputStream("/usr/local/ksan/etc/dbtest.conf");
            prop.load(is);
            userName   = prop.getProperty("test.user");
            bucket     = prop.getProperty("test.bucketName");
            maxNumJob = Long.parseLong(prop.getProperty("test.number"));
            //System.out.format(" >>>> userName : %s  bucket : %s numJobPerT : %d \n", userName, bucket, maxNumJob);
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }
    
    static void countEntry() {
        long diff;

        lock.lock();
        try {
            if (((num_entry + 1) % reportPUnit) == 0) {
                diff = System.nanoTime() - startTime1m;
                System.out.format("<> %d  : Objects(%d/%d) create excution time  : %s \n", mcount, reportPUnit, num_entry + 1, getInSecond(diff));
                startTime1m = System.nanoTime();
                mcount++;
            }
            num_entry++;
        } finally {
            lock.unlock();
        }
    }

    static void initWriteStartTime(){
        lock.lock();
        try {
            if (timeInitOn == false){
               startTime = System.nanoTime();
               startTime1m = System.nanoTime();
               isFinished = false; 
               timeInitOn = true;
            }
        } finally {
            lock.unlock();
        }
    }
    static void initReadStartTime(){
        
        if (startReadTime > 0)
            return;
        
        lock.lock();
        try {
            if (startReadTime == 0){
                startReadTime = System.nanoTime();
                startReadTime1m = System.nanoTime();
                idx_job = 0;
            }
        } finally {
            lock.unlock();
        }
    }
    
    static String getInSecond(long elapsedTime){
        String timeStr;
        //long elapsedTimeSec;
        //long elapsedTimeDo;
        double seconds = (double)elapsedTime / 1_000_000_000.0;
        timeStr = seconds + " second";
        /*elapsedTimeSec = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        if (elapsedTimeSec > 0)
           timeStr = elapsedTimeSec + " second";
        else{
            elapsedTimeDo = elapsedTime % 1000000000;
            timeStr = "0." +elapsedTimeDo + " second";
        }*/
        return timeStr;
    }
    
    static void countGetEntry() {
        long elapsedTime;
        
        lock.lock();
        try {
            if (((num_ReadEntry + 1) % reportPUnit) == 0) {
                elapsedTime = System.nanoTime() - startReadTime1m;
                System.out.format("<> %d  : Objects(%d/%d) read excution time  : %s \n", mReadCount, reportPUnit, num_ReadEntry + 1,  getInSecond(elapsedTime));
                startReadTime1m = System.nanoTime();
                mReadCount++;
            }
            num_ReadEntry++;
        } finally {
            lock.unlock();
        }
    }

    static void createBucket() {
       
        try { 
            ObjManager om = new ObjManager();
            try {
                System.out.println("bucketName :>" + bucket);
                om.createBucket(bucket,  userName, "acl");
            } catch (ResourceNotFoundException ex) {
                Logger.getLogger(DBPerformanceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ResourceAlreadyExistException ex) {
            System.out.println("Bucket already exist!");
        } catch (Exception ex) {
            System.out.println("OBJMANAGER INIT ERROR : " + ex);
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
    
    static void createFileOnDBPerformanceTest( ObjManager om, String name) {
        int idx = 0;
        String path;
        String tmp = bucket + "testDir_" + name + "/test";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        String deleteMarker = "dmarker";
        //long tm = System.nanoTime();
        //long diff;
        //long count = 0;
        
        try {
            //ObjManager om = new ObjManager();
            //for (idx = 0; idx < (int) numJobPerT; idx++) { //500000
            while((idx = getIndex(name + "_create"))!= -1) { 
                path = getPath(tmp, idx);
                Metadata mt = om.create(bucket, path); // to create on round robin osd disk
                mt.setMeta(meta);
                mt.setEtag(etag);
                mt.setTag(tag);
                mt.setAcl(acl);
                mt.setVersionId("nil", deleteMarker, true);
                om.close(bucket, path, mt);
                //count++;
                countEntry();
            }
            //System.out.format("End num_entry : %d  idx : %d  thread  : %s \n",  num_entry, idx, name);
        } catch (IOException | InvalidParameterException | AllServiceOfflineException | ResourceNotFoundException ex) {
            System.out.println("Error : " + ex.getMessage());
            System.out.println("<><><> idx :" + idx + " thread > " + name );

        } /*catch (Exception ex) {
            System.out.println(" OBJMANAGER INIT Error : " + ex.getMessage());
            Logger.getLogger(DBPerformanceTest.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        
        //diff = System.nanoTime() - tm;
        //System.out.format(" > name : %s time : %s jobDone : %d\n", name, getInSecond(diff), count);
        latch.countDown();
    }
    
    static void readFileFromDBPerformanceTest(ObjManager om, String name) {
        int idx = 0;
        String path;
        String tmp = bucket + "testDir_" + name + "/test";
       
        try {
            //ObjManager om = new ObjManager();
            latch.await();
            initReadStartTime();
            //for (idx = 0; idx < (int) numJobPerT; idx++) { //500000
            while((idx = getIndex(name + "_read"))!= -1){
                path = getPath(tmp, idx);
                try{
                    Metadata mt = om.open(bucket, path, "nil");
                } catch (ResourceNotFoundException ex) {
                    countGetEntry();
                    continue;
                }
                countGetEntry();
            }
            //System.out.format("End num_entry : %d  idx : %d  thread  : %s \n",  num_entry, idx, name);
        } catch (InvalidParameterException ex) {
            System.out.println("Error : " + ex.getMessage());
        } catch (InterruptedException ex) {
            System.out.println("Thread interrupted : " + ex);
        } 
    }
    
    @Override
    public void run(){
       long diff;
       try {
            ObjManager om = new ObjManager();
            System.out.print("\033[H\033[2J");
            System.out.flush();
            //System.out.println("Thread: " + getId() + " running");
            
            initWriteStartTime();
            
            createFileOnDBPerformanceTest(om, "Thread_" + getId());

            //System.out.format("maxNumJob : %d  num_entry : %d  isFinished : %s \n", maxNumJob,num_entry ,  isFinished);
            if ((maxNumJob == num_entry )&& (isFinished == false)){
                diff = System.nanoTime() - startTime;
                System.out.println("Total " + num_entry + " create excution time  : " + getInSecond(diff));
                isFinished = true;
                //System.exit(0); 
            }
             
            readFileFromDBPerformanceTest(om, "Thread_" + getId());
            if ((maxNumJob == num_ReadEntry )&& (isReadFinished == false)){
                diff = System.nanoTime() - startReadTime;
                System.out.println("Total " + num_ReadEntry + " read excution time  : " + getInSecond(diff));
                isReadFinished = true;
                //System.exit(0); 
            } 
       } catch (Exception ex) {
           latch.countDown();
           ex.printStackTrace();
           System.out.println(" [Thread : " + getId()+"] OBJMANAGER INIT Error : " + ex);
       }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        if (args.length == 0)
            getConfig();
        else {
            if (args.length >= 1)
                bucket=args[0];

            if (args.length >=2){
                maxNumJob = Long.parseLong(args[1]);        
            }
        }
        
        if (maxNumJob > 0){
            numJobPerT = maxNumJob / 40;
            if (maxNumJob > 1000000)
                reportPUnit = 1000000;
            else if(maxNumJob <= 1000000 && maxNumJob > 100000)
                reportPUnit=100000;
            else if(maxNumJob <= 100000 && maxNumJob > 10000)
                reportPUnit=10000;
            else if (maxNumJob <= 10000 && maxNumJob > 1000)
                reportPUnit=1000;
            else
                reportPUnit=maxNumJob;
        }
        
        System.out.println("Bucket Name : " + bucket + " len " + args.length);
        System.out.format("bucket : %s user : %s maxNumJob : %d reportPUnit : %d \n", bucket, userName, maxNumJob, reportPUnit);
        createBucket();
        
        DBPerformanceTest dbT[] = new DBPerformanceTest[40];
        
        for (int tidx = 1; tidx <= 40; tidx++) {
            dbT[tidx - 1] = new DBPerformanceTest();
            dbT[tidx - 1].start();
            /*new Thread("" + tidx) {
                @Override
                public void run() {
                    long diff;
                    System.out.println("Thread: " + getId() + " running");
                    createFileOnDBPerformanceTest("Thread_" + getId());
                    
                    System.out.format("maxNumJob : %d  num_entry : %d  isFinished : %s \n", maxNumJob,num_entry ,  isFinished);
                    if ((maxNumJob == num_entry )&& (isFinished == false)){
                        diff = System.nanoTime() - startTime;
                        System.out.println("Total " + num_entry + " create excution time  : " + diff + " nanosecond ");
                        isFinished = true;
                        //System.exit(0); 
                    }
                    
                }
               
            }.start();*/
            System.out.format(">>Thread Index : %d\n", tidx);
        }
        
        if (isFinished == false){
            long diff = System.nanoTime() - startTime;
            System.out.println("Total " + num_entry + " create excution time  : " + diff + " nanosecond ");
        }
    }
    
}