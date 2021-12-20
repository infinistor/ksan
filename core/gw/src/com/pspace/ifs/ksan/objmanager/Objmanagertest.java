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
package com.pspace.ifs.ksan.objmanager;

import java.io.IOException;
import static java.lang.Thread.sleep;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author legesse
 */


public class Objmanagertest {
    static OMLogger logger;
    /**
     * @param args the command line arguments
     */
    static void createFileTest(){
        String path;
        String bucket = "testvol2";
        String tmp = "parentDir1/subDir/test";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        
        ObjManager om = new ObjManager();//new ObjManager();;
        try {
            sleep(3);
        } catch (InterruptedException ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try{
            try{
                om.createBucket(bucket, "1", "username", "user", "acl", "encryption", "objectlock");
            } catch(ResourceAlreadyExistException ex){
                System.out.println("Bucket already exist!");
            }
            for(int idx=0; idx <100; idx++){
                Metadata md;
                path = tmp + idx; 
                System.out.println("create bucket : " + bucket + " path : " + path);   
                md =om.create(bucket, path); // to create on round robin osd disk
                om.close(bucket, path, etag, meta, tag, 0L, acl, md.getPrimaryDisk().getPath(), md.getReplicaDisk().getPath(), "", "file");
                md = om.open(bucket, path);
                System.out.println(md);
                Thread.sleep(10000);
                om.listBucket();
                om.remove(bucket, path);
            }
            om.removeBucket(bucket);
           
        } catch(IOException | InvalidParameterException | InterruptedException | AllServiceOfflineException | ResourceNotFoundException ex){
            System.out.println("Error : " + ex.getMessage());
            //Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
 
    }
    
    static void createFileTestLocal(){
        String path;
        String bucket = "testv1";
        String tmp = "test0003-0004-003";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        
        ObjManager om = new ObjManager();;
        
        try{
            try{
                om.createBucket(bucket, "1", "username", "user", "acl", "encryption", "objectlock");
            } catch(ResourceAlreadyExistException ex){
                System.out.println("Buket already exist!");
            }
            for(int idx=0; idx <10; idx++){
                Metadata md;
                path = tmp + idx;
                md =om.createLocal(bucket, path); // to create file on local osd disk
                om.close(bucket, path, etag, meta, tag, 0L, acl, md.getPrimaryDisk().getPath(), md.getReplicaDisk().getPath(), "", "file");
                md = om.open(bucket, path);
                
                System.out.println(md);
                System.out.println("path : " +path+" => objid : " + md.getObjId() + "  pdiskid : " + md.getPrimaryDisk().getId() 
                       + " rdiskid : " + md.getReplicaDisk().getId() + "   pdisk path :" + md.getPrimaryDisk().getPath() 
                       + "   Rdisk path :" + md.getReplicaDisk().getPath());
                om.remove(bucket, path);
            }
  
        } catch(IOException | InvalidParameterException | AllServiceOfflineException | ResourceNotFoundException ex){
            System.out.println("Error : " + ex.getMessage());
            //Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    static void createFileOnDBPerformanceTest(){
        String path;
        String bucket = "testvol3";
        String tmp = "parentDir1/subDir/test";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        
        ObjManager om = new ObjManager();;
        try {
            sleep(3);
        } catch (InterruptedException ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try{
            try{
                om.createBucket(bucket, "1", "username", "user", "acl", "encryption", "objectlock");
            } catch(ResourceAlreadyExistException ex){
                System.out.println("Bucket already exist!");
            }
            
            long startTime = System.nanoTime();
            long startTime1m = System.nanoTime();
            long diff = 0;
            int mcount =1;
            int idx;
            Metadata mt;
            
            for(idx=0; idx < 20000000; idx++){
                path = tmp + idx;    
                mt =om.create(bucket, path); // to create on round robin osd disk
                om.close(bucket, path, etag, meta, tag, 0L, acl, mt.getPrimaryDisk().getPath(), mt.getReplicaDisk().getPath(), "", "file");
                if ((idx + 1) % 1000000 == 0){
                    diff = System.nanoTime() - startTime1m;
                    System.out.println(mcount + ": Millon create excution time  : " + diff + "nanosecond");
                    startTime1m = System.nanoTime();
                    mcount++;
                }
            }
            
            diff = System.nanoTime() - startTime;
            System.out.println("Total " + idx + " create excution time  : " + diff + "nanosecond");
        } catch(IOException | InvalidParameterException | AllServiceOfflineException | ResourceNotFoundException ex){
            System.out.println("Error : " + ex.getMessage());
            //Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static void testDiskPool() throws AllServiceOfflineException{
        try{
            DISKPOOL dp = new DISKPOOL();
            dp.setId("1");
            dp.setName("dpool1");

            SERVER s = new SERVER("1", 12312311, "192.168.17.81");
            s.addDisk("/DISK1", "1", 0, DiskStatus.GOOD);
            dp.addServer(s);

            SERVER s1 = new SERVER("2", 12314413, "192.168.17.82");
            s1.addDisk("/DISK2", "2", 0, DiskStatus.GOOD);
            dp.addServer(s1);
            dp.displayServerList();
            System.out.println(dp);
            System.out.println("Disk exist : " + dp.diskExistInPool("0", "/DISK3"));
            System.out.println(dp.getNextServer());
            System.out.println(dp.getNextServer());
        } catch (ResourceNotFoundException ex) {
            System.out.println("Error : " + ex.getMessage());
            //OMLogger.getInstance().log(Objmanagertest.class.getName(), Level.SEVERE, null, ex);
            //Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static void testConfig(){
        try {
            ObjManagerConfig conf = new ObjManagerConfig();
            //conf.loadDiskPools();
        } catch (Exception ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static void testLogger(){
        logger.log(OMLoggerLevel.DEBUG, " Debug->test message");
        logger.log(OMLoggerLevel.ERROR, " ERROR->test message");
        logger.log(OMLoggerLevel.INFO, " INFO->test message");
        logger.log(OMLoggerLevel.WARN, " WARN->test message");
        logger.log(OMLoggerLevel.TREACE, " INFO->test message");
    }
    
    static void testBucket(){
        ObjManager om = new ObjManager();;
        String [] lst = om.listBucket();
        System.out.println("length : " + lst.length + "list : " + Arrays.toString(lst));
        System.out.println(om.isBucketExist("testvol2"));
        System.out.println(om.isBucketExist("testvol12"));
    }
    
    static void listObjectTest(){
        String bucketName = "testvol3";
        String delimiter = "/";
        String startAfter = "";
        int maxKeys = 10; 
        String prefix = "parentDir1/subDir";
        /*ListObject lo1 = new ListObject(null, bucketName, delimiter, startAfter, maxKeys, prefix);
        ListObject lo2 = new ListObject(null, bucketName, "", startAfter, maxKeys, "");
        ListObject lo3 = new ListObject(null, bucketName, delimiter, startAfter, maxKeys, "");
        ListObject lo4 = new ListObject(null, bucketName, "", startAfter, maxKeys, prefix);*/
        ObjManager om = new ObjManager();;
        om.listObjects(bucketName, delimiter, startAfter, maxKeys, prefix);
    }
    
    static void testDiskRecover(){
        try {
            String bucketName = "testvol1";
            String key = "parentDir1/subDir/test11";
            ObjManager om = new ObjManager();;
            Metadata md = om.open(bucketName, key);
            DISK replica = om.allocReplicaDisk(bucketName, md.getPrimaryDisk().getPath(), md.getPrimaryDisk().getId());
            om.replaceDisk(bucketName, md.getObjId(), md.getPrimaryDisk().getId(), replica.getId());
        } catch (ResourceNotFoundException | AllServiceOfflineException ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static void testParsing(){
        String body = "{\"DiskPool\":{\"Id\":\"7990e59b-6582-489e-9bf6-71fa165444b5\",\"Name\":\"pool6\",\"Description\":null,\"ModDate\":\"2021-04-22T11:21:03\",\"ModId\":\"48168f1f-bfc7-4d18-965e-db96bd6ff5ad\",\"ModName\":\"string\",\"RegDate\":\"2021-04-20T20:02:28\",\"RegId\":\"48168f1f-bfc7-4d18-965e-db96bd6ff5ad\",\"RegName\":\"InternalService\"},\"Id\":\"012795f1-42f4-47cb-a03e-a8bc0bfb47cd\",\"DiskPoolId\":\"7990e59b-6582-489e-9bf6-71fa165444b5\",\"Name\":\"volume13\",\"Description\":null,\"Password\":\"c53255317bb11707d0f614696b3ce6f221d0e2f2\",\"State\":\"Offline\",\"TotalSize\":102400.0,\"UsedSize\":0.0,\"UsageRate\":0.0,\"UsedInode\":0.0,\"ReplicationType\":\"OnePlusTwo\",\"EnableDrService\":false,\"Permission\":\"Private\",\"OwnerId\":0,\"GroupId\":0,\"ModDate\":\"2021-04-22T16:26:34\",\"ModId\":\"48168f1f-bfc7-4d18-965e-db96bd6ff5ad\",\"ModName\":\"string\",\"RegDate\":\"2021-04-22T16:26:34\",\"RegId\":\"48168f1f-bfc7-4d18-965e-db96bd6ff5ad\",\"RegName\":null}";
        JSONParser parser = new JSONParser();
        JSONObject jO;
         JsonOutput res = new JsonOutput();
        
        try {
            jO =(JSONObject)parser.parse(body);
            System.out.println("Volume Id >" + (String)jO.get("Id"));
            System.out.println("DiskPool Id >" + (String)((JSONObject)jO.get("DiskPool")).get("Id"));
            if (jO.containsKey(KEYS.ACTION.label)){
            res.action = (String)jO.get(KEYS.ACTION.label);
            System.out.println("action >" + res.action);
        } if (jO.containsKey(KEYS.MODE.label))
            res.action = (String)jO.get(KEYS.MODE.label);
        if (jO.containsKey(KEYS.DISKID.label))
            res.diskid   = (String)jO.get(KEYS.DISKID.label);
        if (jO.containsKey(KEYS.SERVERID.label))
            res.serverid = (String)jO.get(KEYS.SERVERID.label);
        if (jO.containsKey(KEYS.MOUNTPATH.label))
            res.mpath = (String)jO.get(KEYS.MOUNTPATH.label);
        if (jO.containsKey(KEYS.TOTALSPACE.label))
            res.totalSpace =  (double) jO.get(KEYS.TOTALSPACE.label);
        if (jO.containsKey(KEYS.USEDSPACE.label))
            res.usedSpace = (double)jO.get(KEYS.USEDSPACE.label);
        if (jO.containsKey(KEYS.RESERVEDSPACE.label))
            res.reservedSpace = (double)jO.get(KEYS.RESERVEDSPACE.label);
        if (jO.containsKey(KEYS.USEDINODE.label)){
            System.out.println("keys==>" + KEYS.USEDINODE.label);
            System.out.println("values==>" + jO.get(KEYS.USEDINODE.label));
            res.usedInode = (double)jO.get(KEYS.USEDINODE.label);
        }
        if (jO.containsKey(KEYS.TOTALINODE.label))
            res.totalInode = (double)jO.get(KEYS.TOTALINODE.label);
        if (jO.containsKey(KEYS.DISKPOOLID.label)){
            res.dpoolid = (String)jO.get(KEYS.DISKPOOLID.label);
            System.out.println("dskpoolId >" + res.dpoolid);
        } if (jO.containsKey(KEYS.HOSTNAME.label))
            res.hostname = (String)jO.get(KEYS.HOSTNAME.label);
        if (jO.containsKey(KEYS.IPADD.label))
            res.IPaddr = (String)jO.get(KEYS.IPADD.label);
        if (jO.containsKey(KEYS.RACK.label))
            res.rack = Integer.getInteger((String)jO.get(KEYS.RACK.label));
        System.out.println("=>" + res);
        } catch (ParseException ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    static void testVersiong(){
        String path;
        String bucket = "testvol4vers";
        String tmp = "parentD1/subDir/test";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        String vers;
        int ret;
        
        ObjManager om = new ObjManager();;
        try {
            sleep(3);
        } catch (InterruptedException ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try{
            try{
                om.createBucket(bucket, "1", "username", "user", "acl", "encryption", "objectlock");
            } catch(ResourceAlreadyExistException ex){
                System.out.println("Bucket already exist!");
            }
            
            try {
                vers = om.getBucketVersioning(bucket);
                if (vers.isEmpty())
                    om.putBucketVersioning(bucket, "Enabled");
                System.out.println("111Versioin ret :> " + vers);
            } catch (ResourceNotFoundException | SQLException ex) {
                try {
                    ret = om.putBucketVersioning(bucket, "Enabled");
                    System.out.println("222Versioin ret :> " + ret);
                } catch (SQLException ex1) {
                    Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            long startTime = System.nanoTime();
            long startTime1m = System.nanoTime();
            long diff = 0;
            int mcount =1;
            int idx;
            Metadata mt;
            for(idx=0; idx < 200; idx++){
                path = tmp + idx;    
                mt =om.create(bucket, path); // to create on round robin osd disk
                om.close(bucket, path, etag, meta, tag, 0L, acl, mt.getPrimaryDisk().getPath(),mt.getReplicaDisk().getPath(), Long.toString(startTime), "file");
                if ((idx + 1) % 1000000 == 0){
                    diff = System.nanoTime() - startTime1m;
                    System.out.println(mcount + ": Millon create excution time  : " + diff + "nanosecond");
                    startTime1m = System.nanoTime();
                    mcount++;
                }
            }
            
            diff = System.nanoTime() - startTime;
            System.out.println("Total " + idx + " create excution time  : " + diff + "nanosecond");
            String delimiter = "/";
            String startAfter = "";
            int maxKeys = 10; 
            String prefix = "parentD1/subDir";
            om.listObjectsVersion(bucket, delimiter, startAfter, "", maxKeys, prefix);
        } catch(IOException | InvalidParameterException | AllServiceOfflineException | ResourceNotFoundException ex){
            System.out.println("Error : " + ex.getMessage());
            //Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void testMultiPartUpload(){
    String path;
        String bucket = "testvol4MultPart";
        String tmp = "parentD1/subDir/test";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        String vers;
        int ret;
        
        ObjManager om = new ObjManager();;
        try {
            sleep(3);
        } catch (InterruptedException ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try{
            try{
                om.createBucket(bucket, "1", "username", "user", "acl", "encryption", "objectlock");
            } catch(ResourceAlreadyExistException ex){
                System.out.println("Bucket already exist!");
            }
            
            ObjMultipart mp = new ObjMultipart(bucket);
            int idx;
            int idx1;
            for(idx=0; idx < 10; idx++){
                path = tmp + idx;    
               Metadata mt =om.create(bucket, path); 
                
                String uploadId = mp.createMultipartUpload(bucket, path, "acl", "meta");
                for (idx1 =1; idx1 < 100; idx1++){
                    mp.startSingleUpload(path, uploadId, idx1, "acl", "meta", "etag", 0, "");
                    mp.finishSingleUpload(uploadId, idx1);
                }
                List<Integer> lst = mp.listParts(path, uploadId, 100, 0);
                System.out.println("key : " + path + " uploadId : " + uploadId + " parts : " + lst.toString());
                om.close(bucket, path, etag, meta, tag, 0L, acl, mt.getPrimaryDisk().getPath(), mt.getReplicaDisk().getPath(), Long.toString(System.nanoTime()), "file");
            }
            
            String delimiter = "/";
            String startAfter = "";
            int maxKeys = 10; 
            String prefix = "parentD1/subDir";
            List<ListResult> lsu =mp.listUploads(delimiter, prefix, startAfter, "", maxKeys);
            System.out.println(" upload list : >" + lsu.toString());
        } catch(IOException | InvalidParameterException | AllServiceOfflineException | ResourceNotFoundException ex){
            System.out.println("Error : " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("Error : " + ex.getMessage());
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
    
    public static void main(String[] args) {
        logger = new OMLogger(Objmanagertest.class.getName());
       
       //createFileTest();  // create with round robin disk allocation
       
       //createFileTestLocal(); // create primary object at local osd disk
       
       //testDiskPool();
       
       //testConfig();
       
       //testLogger();  // test logger
       
       //testBucket(); // to test bucket
       
       //listObjectTest();
       
       //testDiskRecover();
       
       //testParsing();
       
       //createFileOnDBPerformanceTest();
       
       // testVersiong();
       
       testMultiPartUpload();
    }
}
