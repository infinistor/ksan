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
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;

/**
 *
 * @author legesse
 */


public class Objmanagertest {
    static OMLogger logger;
    /**
     * @param args the command line arguments
     */
    static void createFileTest() throws Exception{
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
                om.createBucket(bucket, "legesse1", "legesse1", "acl", "", "");
            } catch(ResourceAlreadyExistException ex){
                System.out.println("Bucket already exist!");
            }
            for(int idx=0; idx <100; idx++){
                Metadata md;
                path = tmp + idx; 
                System.out.println("create bucket : " + bucket + " path : " + path);   
                md =om.create(bucket, path); // to create on round robin osd disk
                om.close(bucket, path, md);
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
    
    static void createFileTestLocal() throws Exception{
        String path;
        String bucket = "testv1";
        String tmp = "test0003-0004-003";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        
        ObjManager om = new ObjManager();
        
        try{
            try{
                om.createBucket(bucket, "legesse1", "legesse1", "acl", "", "");
            } catch(ResourceAlreadyExistException ex){
                System.out.println("Buket already exist!");
            }
            for(int idx=0; idx <10; idx++){
                Metadata md;
                path = tmp + idx;
                md =om.createLocal(bucket, path); // to create file on local osd disk
                om.close(bucket, path, md);
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
    
    static void createFileOnDBPerformanceTest() throws Exception{
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
                om.createBucket(bucket, "user", "user", "acl", "", "");
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
                om.close(bucket, path,mt);
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
            //System.out.println("Disk exist : " + dp.diskExistInPool("0", "/DISK3"));
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
    
    static void testBucket() throws Exception{
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
        String diskid = "4";
        
        try {
            ObjManagerUtil omu = new ObjManagerUtil();
            List<Metadata> ml =omu.listObjects(bucketName, "", "", maxKeys);
            System.out.println(ml.toString());
            System.out.println("leng >> " + ml.size());
            /*ObjManagerConfig config = new ObjManagerConfig();
            ObjManagerCache  obmCache= new ObjManagerCache();
            config.loadDiskPools(obmCache);
            DataRepository dbm = new MysqlDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName);
            ListObject list = new ListObject(dbm, bucketName, diskid, 0, maxKeys);
            List<Metadata> ml = list.getUnformatedList();
            System.out.println(ml.toString());
            System.out.println("leng >> " + ml.size());*/
            /*ListObject lo1 = new ListObject(null, bucketName, delimiter, startAfter, maxKeys, prefix);
            ListObject lo2 = new ListObject(null, bucketName, "", startAfter, maxKeys, "");
            ListObject lo3 = new ListObject(null, bucketName, delimiter, startAfter, maxKeys, "");
            ListObject lo4 = new ListObject(null, bucketName, "", startAfter, maxKeys, prefix);*/
            /*ObjManager om = new ObjManager();
            s3ObjectList parm;
            = new s3ObjectList();
            om.listObject(bucketName, s3ObjectList);
            om.listObjects(bucketName, delimiter, startAfter, maxKeys, prefix);*/
        } catch (SQLException ex) {
            System.out.println("list sql error!");
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static void testDiskRecover(){
        try {
            String bucketName = "testvol1";
            String key = "parentDir1/subDir/test11";
            ObjManagerUtil om = new ObjManagerUtil();;
            Metadata md = om.getObjectWithPath(bucketName, key);
            DISK replica = om.allocReplicaDisk(bucketName, md);
            om.replaceDisk(bucketName, md.getObjId(), md.getVersionId(), md.getPrimaryDisk().getId(), replica.getId());
        } catch (ResourceNotFoundException | AllServiceOfflineException ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
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
    
    static void testVersiong() throws Exception{
        String path;
        String bucket = "testvol3vers";
        String tmp = "parentD1/subDir/test";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        String vers;
        int ret;
        
        ObjManager om;
        
        om = new ObjManager();
        
        S3ObjectList s3ObjectList = new S3ObjectList();
        
        try {
            sleep(3);
        } catch (InterruptedException ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try{
            try{
                om.createBucket(bucket, "legesse1", "legesse1", "acl", "", "");
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
                om.close(bucket, path, mt);
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
            s3ObjectList.setMaxKeys(String.valueOf(maxKeys));
            s3ObjectList.setDelimiter(delimiter);
            s3ObjectList.setPrefix(prefix);
            s3ObjectList.setStartAfter(startAfter);
            s3ObjectList.setVersionIdMarker("");
            s3ObjectList.setMarker(prefix);
            try {
                //om.listObjectsVersion(bucket, delimiter, startAfter, "", maxKeys, prefix);
                ObjectListParameter res =om.listObjectVersions(bucket, s3ObjectList);
                System.out.println("res >>" +res);
            } catch (SQLException ex) {
                Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch(IOException | InvalidParameterException | AllServiceOfflineException | ResourceNotFoundException ex){
            System.out.println("Error : " + ex.getMessage());
            //Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void testMultiPartUpload() throws Exception{
    String path;
        String bucket = "testvol1";
        String tmp = "parentD1/subDir/test";
        String etag = "etag_sample";
        String tag = "tag_sample";
        String meta = "meta_sample";
        String acl = "acl_sample";
        String vers;
        int ret;
        
        ObjManager om = new ObjManager();
        try {
            sleep(3);
        } catch (InterruptedException ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try{
            try{
                om.createBucket(bucket, "user", "user", "acl", "", "");
            } catch(ResourceAlreadyExistException ex){
                System.out.println("Bucket already exist!");
            }
            
            ObjMultipart mp = om.getMultipartInsatance(bucket);
            int idx;
            int idx1;
            for(idx=0; idx < 10; idx++){
                path = tmp + idx;    
               Metadata mt =om.create(bucket, path); 
                
                String uploadId = mp.createMultipartUpload(mt);
                for (idx1 =1; idx1 < 100; idx1++){
                    mp.startSingleUpload(mt, uploadId, idx1);
                    mp.finishSingleUpload(uploadId, idx1);
                }
                List<Integer> lst = mp.listParts(path, uploadId, 100, 0);
                System.out.println("key : " + path + " uploadId : " + uploadId + " parts : " + lst.toString());
                om.close(bucket, path, mt);
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
    
    static void testListObjectV3(){
        String bucketName = "my-test-java-v14lovqrhb1pd6g-fj4k6";//testvol3";
        String delimiter = "";//"/";
        String marker = "";//parentDir1/subDir_Thread_101/test26";
        int maxKeys = 2; 
        String prefix = "";//parentDir1/subDir";
        String diskid = "4";
        
        /*try {
            ObjManager om = new ObjManager();
            List<Metadata> ml =om.listObject(bucketName, delimiter, marker, null, null, maxKeys, prefix);
            System.out.println(ml.toString());
            System.out.println("leng >> " + ml.size());
        } 
        catch (SQLException ex) {
            System.out.println("list sql error!");
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Objmanagertest.class.getName()).log(Level.SEVERE, null, ex);
        }*/
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
       
       //testVersiong();
       
       //testMultiPartUpload();
       
       testListObjectV3();
    }
}
