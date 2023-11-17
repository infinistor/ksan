/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License. See LICENSE for details
*
* All materials such as this program, related source codes, and documents are provided as they are.
* Developers and developers of the KSAN project are not responsible for the results of using this program.
* The KSAN development team has the right to change the LICENSE method for all outcomes related to KSAN development without prior notice, permission, or consent.
*/
package com.pspace.ifs.ksan.objmanager.test;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package com.pspace.ifs.DSAN.ObjManger;

import ch.qos.logback.classic.LoggerContext;
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
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.Objmanagertest;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class DBPerformanceTest extends Thread{

    static int mcount = 0;
    static long reportPUnit = 1000000;
    static long maxNumJob = 5000000;
    static long lineNum = 0;
    static long processNum = 0;
    static long year = 0;
    static long month = 0;
    public long days1 = 0;
    static long num_entry = 0;
    static boolean isFinished;
    static long startTime = System.nanoTime();
    static long startTime1m = System.nanoTime();
    static String bucket = "testvol3";
    static String userName="user1";
    static List<String> opList = new ArrayList();
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
    static void getConfig(String tconfpath){
        InputStream is = null;
        String op;
        int idx =0;
        try {
            Properties prop = new Properties();
            //is = new FileInputStream("/usr/local/ksan/etc/dbtest.conf");
            is = new FileInputStream(tconfpath);
            prop.load(is);
            userName   = prop.getProperty("test.user");
            bucket     = prop.getProperty("test.bucketName");
            maxNumJob = 1000; //Long.parseLong(prop.getProperty("test.number"));
            lineNum = Long.parseLong(prop.getProperty("test.line"));
            processNum = Long.parseLong(prop.getProperty("test.process"));
            year = Long.parseLong(prop.getProperty("test.year"));
            //month = Long.parseLong(prop.getProperty("test.month"));
            do {
                op = prop.getProperty("test.operations" + idx);
                idx++;
                if (op == null)
                    break;
                
                if (!op.isEmpty()){
                    opList.add(op);
                    System.out.println("test.operations : " + op);
                }
            } while(true);
           
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
                om.createBucket(bucket,  userName,  userName, "acl", "", "");
            } catch (ResourceNotFoundException ex) {
                Logger.getLogger(DBPerformanceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ResourceAlreadyExistException ex) {
            System.out.println("Bucket already exist!");
        } catch (Exception ex) {
            System.out.println("[createBucket] OBJMANAGER INIT ERROR : " + ex);
        }
    }

    static String getPath(String subPath, long idx ){
        String path;
        pathlock.lock();
        try {
            path = subPath + "File" + idx;
        }
        finally{
            pathlock.unlock();
        }
        return path;
    }
    
    static void createFileOnDBPerformanceTest( ObjManager om, String name) throws SQLException {  
        int idx = 0;
        String path;
        //String tmp = bucket + "testDir_" + name + "/test";
        String etag = "0b4117540b9b10b43853078b87532c24";
        String tag = "tag_sample";
        String meta = "{\"key\":\"2.pptx\",\"uM\":[{\"x-amz-meta-s3b-last-modified\":\"20230323T085209Z\"},{\"x-amz-meta-sha256\":\"ff9e10be07c2b040b95cb84740bcdece139cf4dc5bdd70193557d826e191e8e3\"}],\"eT\":\"0b4117540b9b10b43853078b87532c24\",\"lM\":\"Fri Jun 30 19:17:43 KST 2023\",\"tier\":\"STANDARD\",\"dM\":\"file\",\"cL\":\"441559\",\"cT\":\"application/vnd.openxmlformats-officedocument.presentationml.presentation\",\"oI\":\"5ee7bc18-9c05-4092-8b27-94b22ba68012\",\"oN\":\"ksanuser\",\"tC\":\"0\",\"vId\":\"null\"}";
        String acl = "acl_sample";
        String deleteMarker = "file";
        //long tm = System.nanoTime();
        //long diff;
        //long count = 0;
        
        try {
            //ObjManager om = new ObjManager();
            for (idx = 0; idx < 1000; idx++) { //500000
            //while((getIndex(name + "_create"))!= -1) { 
                path = getPath(name, idx);
                //System.out.println(">>" +path);
                Metadata mt = om.create(bucket, path); // to create on round robin osd disk
                mt.setMeta(meta);
                mt.setEtag(etag);
                mt.setTag(tag);
                mt.setAcl(acl);
                mt.setVersionId("nil", deleteMarker, true);
                om.close(bucket, path, mt);
                //count++;
                countEntry();
            //}
           }
            //System.out.format("End num_entry : %d  idx : %d  thread  : %s \n",  num_entry, idx, name);
        } catch (IOException | InvalidParameterException | AllServiceOfflineException | ResourceNotFoundException ex) {
            System.out.println("[createFileOnDBPerformanceTest]Error : " + ex.getMessage());
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
    
    static void listExpiredObject(ObjManagerUtil om, String bucketName){
        String marker = "";
        int maxKeys = 1000; 
        String prefix = "/";
        long expiredTime = 10000;
        int idx;
        int execute_time = 2;
        
        try {
            //ObjManagerUtil om = new ObjManagerUtil();
            while(true){
                List<Metadata> ml =om.listExpiredObjects(bucketName, prefix, marker, maxKeys, expiredTime);
                for(idx = 0; idx < ml.size(); idx++){
                    System.out.println("idx :>" + idx + " " + ml.get(idx).toString());
                    //System.out.println("leng >> " + ml.size());
                    if ((ml.size() == maxKeys) && (idx == (maxKeys - 1))){
                        marker = ml.get(idx).getPath();
                    }
                }
                
                if (marker.isEmpty())
                    break;
                
                if (execute_time-- > 0)
                    break;
            }
        } 
        catch (SQLException ex) {
            System.out.println("list sql error!");
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void run_io(){
       long diff;
       String dirpath;
       try {
            ObjManager om = new ObjManager();
            System.out.print("\033[H\033[2J");
            System.out.flush();
            //System.out.println("Thread: " + getId() + " running");
            
            initWriteStartTime();
            
            for ( int months=0; months < 12; months++){
                for (int hours = 0; hours < 24; hours++){
                    dirpath = String.format("Line%d/공정%d/%d/%d/%d/%d/", lineNum , processNum, year, months, days1, hours);
                    //System.out.println(dirpath);
                    createFileOnDBPerformanceTest(om, dirpath);
                }
            }
            

            //System.out.format("maxNumJob : %d  num_entry : %d  isFinished : %s \n", maxNumJob,num_entry ,  isFinished);
            if ((maxNumJob == num_entry )&& (isFinished == false)){
                diff = System.nanoTime() - startTime;
                System.out.println("Total " + num_entry + " create excution time  : " + getInSecond(diff));
                isFinished = true;
                //System.exit(0); 
            }
             
            /*readFileFromDBPerformanceTest(om, "Thread_" + getId());
            if ((maxNumJob == num_ReadEntry )&& (isReadFinished == false)){
                diff = System.nanoTime() - startReadTime;
                System.out.println("Total " + num_ReadEntry + " read excution time  : " + getInSecond(diff));
                isReadFinished = true;
                //System.exit(0); 
            } */
       } catch (Exception ex) {
           latch.countDown();
           ex.printStackTrace();
           System.out.println(" [Thread : " + getId()+"] OBJMANAGER INIT Error : " + ex);
       }
    }
    
    void run_list_objects(){
       long diff;
       try {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            //System.out.println("Thread: " + getId() + " running");
            ObjManagerUtil om = new ObjManagerUtil();
            initWriteStartTime();
            
            listExpiredObject(om, bucket);

            System.out.format("maxNumJob : %d  num_entry : %d  isFinished : %s \n", maxNumJob,num_entry ,  isFinished);
            if ((maxNumJob == num_entry )&& (isFinished == false)){
                diff = System.nanoTime() - startTime;
                System.out.println("Total " + num_entry + "  listExpiredObjects excution time  : " + getInSecond(diff));
                isFinished = true;
                //System.exit(0); 
            }
       } catch (Exception ex) {
           latch.countDown();
           ex.printStackTrace();
       }
    }
    
    @Override
    public void run(){
        run_io();
        /*for (String op : opList){
           if (op.equalsIgnoreCase("io"))
               run_io();
           if (op.equalsIgnoreCase("list"))
               run_list_objects();
        }*/     
    }
    
    static void disableDebuglog(String driver){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(driver);
        rootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        disableDebuglog("org.mongodb.driver");
        disableDebuglog("com.pspace.ifs.ksan.objmanager");
        
        if (args.length == 1)
            getConfig(args[0]);
        else {
            if (args.length != 4 ){
                System.out.println("Wrong paramters! expected bucketName  ineNum 공정 Year!");
                System.exit(-1);
            }
            /*if (args.length >= 1)
                bucket=args[0];

            if (args.length >=2){
                maxNumJob = Long.parseLong(args[1]);        
            }*/
            bucket=args[0];
            //userName   = args[1];
            maxNumJob = 1000; 
            lineNum = Long.parseLong(args[1]);
            processNum = Long.parseLong(args[2]);
            year = Long.parseLong(args[3]);
            System.out.format("Args: > bucket : %s numFileinEachDir : %d lineNum : %d 공정 : %d year : %d \n", bucket, maxNumJob, lineNum, processNum, year);
            //System.exit(1); //FIXME
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
            else{
                //reportPUnit=maxNumJob;// FIXME
                reportPUnit=100000;
            }
        }
        
        System.out.println("Bucket Name : " + bucket + " len " + args.length);
        System.out.format("bucket : %s user : %s maxNumJob : %d reportPUnit : %d \n", bucket, userName, maxNumJob, reportPUnit);
        //createBucket();
        
        DBPerformanceTest dbT[] = new DBPerformanceTest[30];
        
        for (int tidx = 1; tidx <= 30; tidx++) {
            dbT[tidx - 1] = new DBPerformanceTest();
            dbT[tidx - 1].days1 = tidx;
            dbT[tidx - 1].start();

            System.out.format(">>Thread Index(days) : %d\n", tidx);
        }
        
        if (isFinished == false){
            long diff = System.nanoTime() - startTime;
            System.out.println("Total " + num_entry + " create excution time  : " + diff + " nanosecond ");
        }
        //System.exit(0);  
    }
    
}
