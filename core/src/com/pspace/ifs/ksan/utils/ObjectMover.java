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
import com.pspace.ifs.ksan.objmanager.DataRepositoryLoader;
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
    private boolean debugOn;
    private boolean checkOnly;
    private boolean allowedToMoveLocalDisk;
    private String Status;
    private String utilName;
    private long TotalNumObject;
    private long NumJobDone;
    private String startTime;
    private DataRepository dbm;
    private ObjManagerUtil obmu;
    private OSDClient osdc;
     
    public ObjectMover(boolean checkOnly, String utilName) throws Exception{
        this.debugOn = false;
        obmu = new ObjManagerUtil();
        dbm = obmu.getDBRepository();
        osdc = obmu.getOSDClient();
        this.checkOnly = checkOnly;
        this.utilName = utilName;
        TotalNumObject = 0;
        NumJobDone = 0;
        allowedToMoveLocalDisk = false;
        Status = "INIT";
        Id = getNewId();
        List<Object> in = encode();
        List<Object> out= dbm.utilJobMgt("addJob", in);
    }
    
    public ObjectMover(ObjManagerUtil obmu, boolean checkOnly, String utilName) throws Exception{
        this.debugOn = false;
        this.obmu = obmu;
        dbm = obmu.getDBRepository();
        osdc = obmu.getOSDClient();
        this.checkOnly = checkOnly;
        this.utilName = utilName;
        TotalNumObject = 0;
        NumJobDone = 0;
        Status = "INIT";
        allowedToMoveLocalDisk = false;
        Id = getNewId();
        List<Object> in = encode();
        List<Object> out= dbm.utilJobMgt("addJob", in);
    }
    
    public ObjectMover(String id) throws Exception{
        this.debugOn = false;
        List<Object> in;
        List<Object> out;
        Id = id;
        checkOnly = false;
        Status = "";
        TotalNumObject = 0;
        NumJobDone = 0;
        allowedToMoveLocalDisk = false;
        in = encode();
        obmu = new ObjManagerUtil();
        dbm = obmu.getDBRepository();
        osdc = obmu.getOSDClient();
        out = dbm.utilJobMgt("getJob", in);
        decode(out);
        log(">>>Id : %s status : %s TotalNumObject : %d checkOnly : %s utilName : %s \n", Id, Status, TotalNumObject, checkOnly, utilName);
    }
    
    public void setDebugModeOn(){
        this.debugOn = true;
        osdc.setDebugModeOn();
    }
    
    public boolean isDebugModeOn(){
        return debugOn;
    }
    
    public void log(String format, Object... args ){
        if (debugOn)
            System.out.format(format, args);
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
       log("<<<Id : %s status : %s TotalNumObject : %d checkOnly : %s utilName : %s \n", Id, Status, TotalNumObject, checkOnly, utilName);
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
    
    public ObjManagerUtil getObjManagerUtil(){
        return obmu;
    }
    
    public void enableDisableLocalDiskMove(boolean allowedToMoveLocalDisk){
        this.allowedToMoveLocalDisk = allowedToMoveLocalDisk;
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
    
    private DISK getDisk(Metadata mt, String diskID) throws ResourceNotFoundException{
        if (mt.getPrimaryDisk().getId().equalsIgnoreCase(diskID))
            return mt.getPrimaryDisk();
        
        if (!mt.isReplicaExist())
           throw new ResourceNotFoundException("There is no disk with ID " + diskID + " in the metadata!");
        
         if (mt.getReplicaDisk().getId().equalsIgnoreCase(diskID))
            return mt.getReplicaDisk();
         
         throw new ResourceNotFoundException("There is no disk with ID " + diskID + " in the metadata!");
    }
   
    private int moveObject(String bucket, String objId, String versionId, DISK srcDisk, DISK desDisk, DISK diskToChange)throws ResourceNotFoundException, Exception{
        System.out.format("Source : %s/%s%s Des : %s/%s/%s \n", srcDisk.getDiskPoolId(), srcDisk.getOsdIp(), srcDisk.getPath(), desDisk.getDiskPoolId(), desDisk.getOsdIp(), desDisk.getPath());
        if (osdc.copyObject(bucket, objId, versionId, srcDisk, desDisk) != 0)
            return -1;
        
        obmu.replaceDisk(bucket, objId, versionId,  diskToChange.getId(), desDisk.getId());
        
        osdc.removeObject(objId, versionId, diskToChange);
       log("********************************End****************************************************************\n");
        return 0;
    }
    
    public int moveObject(String bucket, String objId, String versionId, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
      log("********************************start****************************************************************\n");
        //log("[moveObject] bucket : %s objId : %s  versionId : %s", bucket,  objId,  versionId);
        Metadata mt = obmu.getObject(bucket, objId, versionId); 
        DISK srcDisk = getDisk(mt, srcDiskId);
        DISK replica = obmu.allocReplicaDisk(bucket, mt);
        checkDiskDistance(mt,  replica);
        //System.out.format("[moveObject] Bucket : %s objId : %s versionId : %s sourceD : %s Des_D : %s \n", mt.getBucket(), objId, versionId, srcDiskId, replica.getId());
        //if (!mt.getPrimaryDisk().getId().equals(srcDiskId))
        return moveObject(bucket, objId, versionId, srcDisk, replica, srcDisk);
        
        //return moveObject(bucket, objId, versionId, mt.getPrimaryDisk(), replica, srcDisk);
    }
    
    public int moveObject(String bucket, String objId, String versionId, String srcDiskId, String desDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        //System.out.println("[moveObject] bucket : "+ bucket +" objId " + objId + " versionId : " + versionId);
        Metadata mt = obmu.getObject(bucket, objId, versionId);
        DISK srcDisk = getDisk(mt, srcDiskId);
        DISK desDisk = obmu.getDISK(desDiskId);
        checkDiskDistance(mt,  desDisk);
        //return moveObject(bucket, objId, versionId, mt.getPrimaryDisk(), desDisk, srcDisk);
        return moveObject(bucket, objId, versionId, srcDisk, desDisk, srcDisk);
    }
    
    public int copyObject(String bucket, String objId, String versionId, String srcDiskId, String desDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        Metadata mt = obmu.getObject(bucket, objId, versionId);
        DISK srcDisk = getDisk(mt, srcDiskId);
        DISK desDisk = obmu.getDISK(desDiskId);
        
        if (!(mt.getPrimaryDisk().getId().equals(srcDisk.getId())) && !(mt.getPrimaryDisk().getId().equals(desDisk.getId())))
            return -1;
        
        if (!(mt.getReplicaDisk().getId().equals(srcDisk.getId())) && !(mt.getReplicaDisk().getId().equals(desDisk.getId())))
            return -1;
        
        if (osdc.copyObject(bucket, objId, versionId, srcDisk, desDisk) != 0)
            return -1;
        
        return 0;
    }
    
    public int moveObject1(String bucket, String key, String versionId, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        //System.out.format("[moveObject1] bucket : %s key : %s versionId : %s \n", bucket, key,  versionId);
        Metadata mt = obmu.getObjectWithPath(bucket, key, versionId);
        DISK srcDisk = getDisk(mt, srcDiskId);
        //System.out.println("[moveObject1] mt " + mt.getObjId() + " versionId :" + mt.getVersionId());
        DISK replica = obmu.allocReplicaDisk(bucket, mt);
        checkDiskDistance(mt,  replica);
        //System.out.println("srcDiskId >" + srcDiskId + " newDiskid >" + replica.getId());
        return moveObject(bucket, mt.getObjId(), mt.getVersionId(), srcDisk, replica, srcDisk);
    }
    
    public int moveObject1(String bucket, String key, String versionId, String srcDiskId, String desDiskId) throws ResourceNotFoundException, Exception{
       Metadata mt = obmu.getObjectWithPath(bucket, key, versionId);
       DISK srcDisk = getDisk(mt, srcDiskId);
       DISK desDisk = getDisk(mt, desDiskId);
       checkDiskDistance(mt,  desDisk);
       return moveObject(bucket, mt.getObjId(), versionId, srcDisk, desDisk, srcDisk);
    }
    
    private void checkDiskDistance(Metadata mt, DISK newDisk) throws ResourceNotFoundException {
        boolean check;
        
        try {
            check = obmu.allowedToReplicate(mt.getBucket(), mt.getPrimaryDisk(), mt.getReplicaDisk(), newDisk.getId(), allowedToMoveLocalDisk);
        } catch (ResourceNotFoundException ex) {
            check = obmu.allowedToReplicate(mt.getBucket(), mt.getPrimaryDisk(), null, newDisk.getId(), allowedToMoveLocalDisk);
        }
        
        if (!check)
            throw new ResourceNotFoundException("[checkDiskDistance] It is not allowd to put primary and replica in the same osd!");
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
