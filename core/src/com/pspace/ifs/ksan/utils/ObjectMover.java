/*
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
package com.pspace.ifs.ksan.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pspace.ifs.ksan.objmanager.DISK;
import com.pspace.ifs.ksan.objmanager.DataRepository;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.MongoDataRepository;
import com.pspace.ifs.ksan.objmanager.MysqlDataRepository;
import com.pspace.ifs.ksan.objmanager.OSDClient;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

/**
 *
 * @author legesse
 */
public class ObjectMover {
    private String Id;
    private boolean checkOnly;
    private String Status;
    private String utilName;
    private long TotalNumObject;
    private long NumJobDone;
    private String startTime;
    private DataRepository dbm;
    private ObjManagerUtil obmu;
    private OSDClient osdc;
    
    private int initDB(){
        try {
            ObjManagerConfig config = new ObjManagerConfig();
            if (config.dbRepository.equalsIgnoreCase("MYSQL")){
                 dbm = new MysqlDataRepository(null, config.dbHost, config.dbUsername, config.dbPassword, config.dbName);
            } else if(config.dbRepository.equalsIgnoreCase("MONGO"))
                 dbm = new MongoDataRepository(null, config.dbHost, config.dbUsername, config.dbPassword, config.dbName, 27017);
        } catch (Exception ex) {
            Logger.getLogger(ObjectMover.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }
    
    public ObjectMover(boolean checkOnly, String utilName) throws Exception{
        initDB();
        this.checkOnly = checkOnly;
        this.utilName = utilName;
        TotalNumObject = 0;
        NumJobDone = 0;
        Status = "INIT";
        Id = getNewId();
        List<Object> in = encode();
        List<Object> out= dbm.utilJobMgt("addJob", in);
        obmu = new ObjManagerUtil();
        osdc = new OSDClient();
    }
    
    public ObjectMover(String id) throws Exception{
        List<Object> in;
        List<Object> out;
        Id = id;
        checkOnly = false;
        Status = "";
        TotalNumObject = 0;
        NumJobDone = 0;
        in = encode();
        initDB();
        out = dbm.utilJobMgt("getJob", in);
        decode(out);
        obmu = new ObjManagerUtil();
        osdc = new OSDClient();
        System.out.format(">>>Id : %s status : %s TotalNumObject : %d checkOnly : %s utilName : %s \n", Id, Status, TotalNumObject, checkOnly, utilName);
    }
    
    private List<Object> encode(){
        List<Object> en = new ArrayList<>();
        en.add(Id);
        en.add(Status);
        en.add(TotalNumObject);
        en.add(NumJobDone);
        en.add(checkOnly);
        en.add(utilName);
        en.add(startTime);
        return en;
    }
    
    private void decode(List<Object> en){
        if (en.isEmpty())
            return;
        
        if (en.size() != 7 )
            return;
        
        Id = en.get(0).toString();
        Status = en.get(1).toString();
        TotalNumObject = Long.decode(en.get(2).toString());
        NumJobDone = Long.decode(en.get(3).toString());
        checkOnly = Boolean.getBoolean(en.get(4).toString());
        utilName = en.get(5).toString();
        startTime = en.get(6).toString();
        System.out.format("<<<Id : %s status : %s TotalNumObject : %d checkOnly : %s utilName : %s \n", Id, Status, TotalNumObject, checkOnly, utilName);
    }
    
    private String getNewId(){
        int retry =100;
        int leftLimit = 65; // letter 'A'
        int rightLimit = 122; // letter 'z'
        String id;
    
        List<Object> in = new ArrayList<>();
        List<Object> out;
        Random rand = new Random();
       
        do {
            id = rand.ints(leftLimit, rightLimit)
                    .limit(15)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
            in.add(id);
            out = dbm.utilJobMgt("getJob", in);
            if (--retry < 0)
                break;
             
             if (out == null)
                 break;
            //System.out.format("Test -->id : %s ret : %d\n", id, ret);
        }while(!out.isEmpty());
        return id;
    }
    
    private int getJobs(){
        return 0;
    }
    
    private int setJobs(){
        return 0;
    }
    
    private int updateJobsStatus(String status){
        Status = status;
        List<Object> in = encode();
        List<Object> out= dbm.utilJobMgt("updateJobStatus", in);
        if (out.isEmpty())
            return -1;
        return 0;
    }
    
    public int updateNumberObjectsProcessed(long totalChecked, long totalFixed){
        TotalNumObject = totalChecked;
        NumJobDone = totalFixed;
        List<Object> in = encode();
        List<Object> out= dbm.utilJobMgt("updateJobNumber", in);
        if (out.isEmpty())
            return -1;
        return 0;
    }
    
    public String getJobId(){
        return Id;
    }
    
    public String getJobStatus(){
        return Status;
    }
    
    public int moveObject(String bucket, String objId, String versionId, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        System.out.println("[moveObject] objId " + objId + " versionId : " + versionId);
        Metadata mt = obmu.getObject(bucket, objId, versionId);
        DISK replica = obmu.allocReplicaDisk(bucket, mt.getPrimaryDisk().getId(), mt.isReplicaExist() ? mt.getReplicaDisk().getId() : null);
        return moveObject(bucket, objId, versionId, srcDiskId, replica);
    }
    
    public int moveObject1(String bucket, String key, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        System.out.println("[moveObject1] key " + key );
        Metadata mt = obmu.getObjectWithPath(bucket, key);
        System.out.println("[moveObject1] mt " + mt.getObjId() + " versionId :" + mt.getVersionId());
        DISK replica = obmu.allocReplicaDisk(bucket, mt.getPrimaryDisk().getId(), mt.isReplicaExist() ? mt.getReplicaDisk().getId() : null);
        System.out.println("srcDiskId >" + srcDiskId + " newDiskid >" + replica.getId());
        return moveObject(bucket, mt.getObjId(), mt.getVersionId(), srcDiskId, replica);
    }
    
    public int moveObject(String bucket, String objId, String versionId, String srcDiskId, DISK desDisk) throws ResourceNotFoundException, Exception{
        // move object in osd
        osdc.moveObject(bucket, objId, srcDiskId, desDisk);
        System.out.println("bucketName :>" + bucket);
       return obmu.replaceDisk(bucket, objId, versionId, srcDiskId, desDisk.getId()); 
    }
    
    public int moveObject1(String bucket, String key, String srcDiskId, String desDiskId) throws ResourceNotFoundException, Exception{
       return moveObject(bucket, new Metadata(bucket, key).getObjId(), srcDiskId, desDiskId);
    }
    
    public int startJob(){
        return updateJobsStatus("RUNNING");
    }
    
    public int stopJob(){
        return updateJobsStatus("STOPPED");
    }
    
    public int pauseJob(){
        return updateJobsStatus("PAUSE");
    }
    
    public int finishedJob(){
        return updateJobsStatus("DONE");
    }
    
    public boolean ISJobRunning(){
        return Status.contains("RUNNING");
    }
}