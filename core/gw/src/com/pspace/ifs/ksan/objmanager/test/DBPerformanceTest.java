/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
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
                om.createBucket(bucket, "1", "username", "user", "acl", "encryption", "objectlock");
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