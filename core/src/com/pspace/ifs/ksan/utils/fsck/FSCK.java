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
package com.pspace.ifs.ksan.utils.fsck;


import java.util.Iterator;
import java.util.List;

import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.OSDClient;
import com.pspace.ifs.ksan.objmanager.OSDResponseParser;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.utils.ObjectMover;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author legesse
 */

public class FSCK {
    private ObjManagerUtil obmu;
    private boolean checkOnly;
    private OSDClient osdc;
    private List<Bucket> bukList;
    //private List<String> dskList;
    private ObjectMover objm;
    private long totalChecked;
    private long totalFixed;
    private long problemType1; // primary problem
    private long problemType2; // replica problem
    private long problemType3; // meta size problem
    private long problemType4; // meta md5 problem
    private long problemType5; // All are different
    
    public FSCK(boolean checkonly) throws Exception{
        checkOnly = checkonly;
        totalChecked = 0;
        totalFixed = 0;
        problemType1 = 0;
        problemType2 = 0;
        problemType3 = 0;
        problemType4 = 0;
        problemType5 = 0;
        objm = new ObjectMover(checkOnly, "FSCK");
        obmu = objm.getObjManagerUtil();
        osdc = obmu.getOSDClient();
        //dskList = obmu.getExistedDiskList();
        bukList = obmu.getExistedBucketList();
        
    }
    
    class Response{
        public OSDResponseParser primary;
        public OSDResponseParser replica;
        public String actionMessage;
        int ret;
    }
    
    private OSDResponseParser getAttr(String bucket, String objId, String versionId, String diskId, String mpath, String serverId) throws IOException, InterruptedException, TimeoutException {
        
        return osdc.getObjectAttr(bucket, objId, versionId, diskId, mpath, serverId);
    }
    
    private Response checkObjectCorrectness(Metadata mt ) throws IOException, InterruptedException, TimeoutException, ResourceNotFoundException {
        OSDResponseParser primary;
        OSDResponseParser replica;
        Response res = new Response();
        
        res.ret = 0;
        if (mt.getPath().endsWith("/"))
            return res; // ignore directory
        
        if (mt.getReplicaCount() > 1 ){ // when only one object exist
            if (!mt.isPrimaryExist()){
                problemType1++;
                res.ret = 6;// copy replica to primary
                res.actionMessage = "copy replica to primary";
                return res; 
            }
            
            if (!mt.isReplicaExist()){
                problemType2++;
                res.ret = 7; // copy primary to replica
                res.actionMessage = "copy primary to replica";
                return res;
            }
            objm.log("[checkObjectCorrectness] Before getAttr request from OSD objId : %s versionId : %s \n", mt.getObjId(), mt.getVersionId());
            primary = getAttr(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.getPrimaryDisk().getPath(), mt.getPrimaryDisk().getOSDServerId());
            replica = getAttr(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getReplicaDisk().getId(), mt.getReplicaDisk().getPath(), mt.getReplicaDisk().getOSDServerId()); 
            objm.log("[checkObjectCorrectness] bucket : %s objId : %s msize : %d psize : %d rsize : %d\n", mt.getBucket(), mt.getObjId(),  mt.getSize(), primary.size, replica.size);
            res.primary = primary;
            res.replica = replica;
            res.ret = 0;
            if (primary.errorCode.contains("MQ_OBJECT_NOT_FOUND")){
                problemType1++;
                res.ret = 6;// copy replica to primary
                res.actionMessage = "copy replica to primary";
                return res;  
            }
            
            if (replica.errorCode.contains("MQ_OBJECT_NOT_FOUND")){
                problemType2++;
                res.ret = 7; // copy primary to replica
                res.actionMessage = "copy primary to replica";
                return res;
            }
            
            if (primary.md5.equals(replica.md5) && primary.md5.equals(mt.getEtag())){ 
                if (primary.size == replica.size && primary.size == mt.getSize())
                    return res;
                problemType3++;
                res.ret = 3; // fix meta size
                 res.actionMessage = "fix meta size";
                return res;
            }
            else if (!primary.md5.equals(replica.md5)){
                if (primary.md5.equals(mt.getEtag())){
                    problemType2++;
                    //System.out.println("copy primary -> replica");
                    res.ret = 2; //cpy primary
                    res.actionMessage = "copy primary to replica";
                    return res;
                }else if(replica.md5.equals(mt.getEtag())){
                    problemType1++;
                    //System.out.println("copy replica -> primary");
                    res.ret = 1; // cpy relica
                    res.actionMessage = "copy replica to primary ";
                    return res;
                } else{
                    problemType5++;
                    res.ret = 5; // difficut to fix
                    res.actionMessage = "all are different difficult to fix";
                    return res; 
                }
            }
            else {
                problemType4++;
                res.ret = 4; // fix md5 of meta
                 res.actionMessage = "fix md5 of meta(Etag) ";
                return res;
            }
        } else{
            primary = getAttr(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.getPrimaryDisk().getPath(), mt.getPrimaryDisk().getOSDServerId());
            //System.out.println(" objId >> " + mt.getObjId() +" primary  >> "+ primary);
            if (!primary.errorCode.contains("MQ_SUCESS")){
                objm.log("ObjId >>" +  mt.getObjId()+" primary md5 >> "+ primary.md5 + " size >" + primary.size + " errocode >>" + primary.errorCode);
                problemType1++;
                return res;
            }
            
            if (!primary.md5.equals(mt.getEtag())){
                //System.out.println(" primary md5 >> "+ primary.md5 + " size >" + primary.size);
                problemType4++;
            }
            
            if (primary.size != mt.getSize()){
                //System.out.println(" primary md5 >> "+ primary.md5 + " size >" + primary.size);
                problemType3++;
            }
        }
        
        return res;
    }
    
    private int takeAction(Metadata mt, Response res) throws ResourceNotFoundException, Exception{
        int ret = 0;
        switch(res.ret){
            case 1:
                ret = objm.copyObject(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getReplicaDisk().getId(), mt.getPrimaryDisk().getId());
                if (ret != 0 )
                    ret = objm.moveObject(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getReplicaDisk().getId());
                break;
            case 2:
                ret = objm.copyObject(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.getReplicaDisk().getId());
                if (ret != 0 )
                    ret = objm.moveObject(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId());
                break;
            case 6:
                ret = objm.moveObject(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getReplicaDisk().getId());
                break;
            case 7:
                ret = objm.moveObject(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId());
                break;
            case 3: // update meta size
                obmu.updateObject(mt.getBucket(), mt.getObjId(), mt, 2);
                break;
            case 4: // update meta md5
                //System.out.println(" obmu >> " + obmu + " primary >>" + res.primary);
                obmu.updateObjectEtag(mt.getBucket(), mt, res.primary.md5);
                break;
            case 5:
               objm.log("[takeAction] bucket : %s objId : %s versionId : %s  md5{meta, primary, replica} : {%s, %s, %s} all are different\n", 
                        mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getEtag(), res.primary.md5, res.replica.md5);
               ret = -1;
                break;
        }
        return ret;
    }
    
    private int fixObject(Metadata mt) throws Exception{
        int ret = -1;
        
        try {
            Response res = checkObjectCorrectness(mt);
            if (res.ret == 0){
                objm.log("[fixObject] bucket : %s objId : %s versionId : %s NORMAL\n", mt.getBucket(), mt.getObjId(), mt.getVersionId());
                return 0;
            }
            
            objm.log("[fixObject] ERROR bucket : %s objId : %s versionId : %s %s \n", mt.getBucket(), mt.getObjId(), mt.getVersionId(), res.actionMessage);
            if (checkOnly){
                return res.ret > 0 ? -1 : 0;
            }
            
            ret = takeAction(mt, res); 
            if (ret == 0)
                totalFixed++;
        } 
        catch (ResourceNotFoundException | IOException ex) {
            objm.log("[fixObject] failed bucket : %s objId : %s versionId : %s unable to check due to %s \n", mt.getBucket(), mt.getObjId(), mt.getVersionId(), ex.fillInStackTrace());
            return -1;
        }
        catch (InterruptedException | TimeoutException ex) {
            objm.log("[fixObject] failed bucket : %s objId : %s versionId : %s unable to check due to timeout  %s \n", mt.getBucket(), mt.getObjId(), mt.getVersionId(), ex.getMessage());
            return -1;
        }
        
        return ret;
    }
    
    private long checkEachObject(String bucketName, String diskid) throws Exception{
        List<Metadata> list;
        String lastObjId = " ";
        long numObjects = 100;
        long job_done = 0;
        int ret;
        
        
        ret = objm.startJob();
        if (ret != 0){
             objm.log("[checkEachObject ] bucket : %s diskid : %s unable to register job!\n", bucketName, diskid);
            return 0;
        }
        
        do{ 
            //objm.log("[checkEachObject] Before bucketName: %s diskid : %s lastObjId : %s \n", bucketName, diskid, lastObjId);
            list = obmu.listObjects(bucketName, diskid, lastObjId, numObjects);
            if (list == null){
                 objm.log("[checkEachObject -1] bucket : %s diskid : %s lastObjId : %s is empty \n", bucketName, diskid, lastObjId);
                return 0;
            }
            
            //objm.log("[checkEachObject] After bucketName: %s diskid : %s lastObjId : %s size : %d\n", bucketName, diskid, lastObjId, list.size());
            if (list.isEmpty()){
                objm.log("[checkEachObject -2] bucket : %s diskid : %s lastObjId : %s is empty \n", bucketName, diskid, lastObjId);
                return 0;
            }
            
            Iterator<Metadata> it = list.iterator();
            while(it.hasNext())
            {
                totalChecked++;
                if (!objm.ISJobRunning()){
                    //totalFixed = totalFixed + job_done;
                     objm.updateNumberObjectsProcessed(totalChecked, totalFixed); 
                    return job_done;
                }
                
                Metadata mt = it.next();
                lastObjId = mt.getObjId();
                try {
                    objm.log("[checkEachObject] bucket : %s key : %s objId : %s versionId : %s pdiskid : %s rpdiskid : %s \n", 
                            mt.getBucket(), mt.getPath(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.isReplicaExist() ? mt.getReplicaDisk().getId(): "");
                    if (fixObject(mt)== 0)
                        job_done++;
                
                } catch (ResourceNotFoundException | AllServiceOfflineException ex) {
                   objm.log("[checkEachObject] not found Bucket : %s path : %s pdisk : %s \n",
                            mt.getBucket(), mt.getPath(),
                            mt.isPrimaryExist() ? mt.getPrimaryDisk().getPath() : "");
                }    
            }
            
            //offset = offset + numObjects;
            //System.out.println("----------------------------------------------------------->" + job_done);
        } while(list.size() == numObjects );
        //totalFixed = totalFixed + job_done;
        objm.updateNumberObjectsProcessed(totalChecked, totalFixed);
        objm.finishedJob();
        
        return job_done;  
    }
      
    private long checkEachObject(String bucketName) throws Exception{
        List<Metadata> list;
        String lastObjId = " ";
        long numObjects = 100;
        long job_done = 0;
        int ret;
        
        
        ret = objm.startJob();
        if (ret != 0){
             objm.log("[checkEachObject -1] bucket : %s unable to register job!\n", bucketName);
            return 0;
        }
        
        do{
            list = obmu.listObjects(bucketName, lastObjId, numObjects);
            if (list == null){
                objm.log("[checkEachObject -1] bucket : %s lastObjId : %s is empty \n", bucketName, lastObjId);
                return 0;
            }
            
            //System.out.format("[checkEachObject] bucket : %s  diskid : %s list# : %d\n", bucketName, diskid, list.size());
            if (list.isEmpty()){
                objm.log("[checkEachObject -2] bucket : %s lastObjId : %s is empty \n", bucketName, lastObjId);
                return 0;
            }
            
            Iterator<Metadata> it = list.iterator();
            while(it.hasNext())
            {
                totalChecked++;
                if (!objm.ISJobRunning()){
                    //totalFixed = totalFixed + job_done;
                    objm.updateNumberObjectsProcessed(totalChecked, totalFixed); 
                    return job_done;
                }
                
                Metadata mt = it.next();
                //System.out.format("[CheckList] objId : %s lastObjId : %s  nChecked : %d\n", mt.getObjId(), lastObjId, totalChecked);
                lastObjId = mt.getObjId();
                try {
                    objm.log("[checkEachObject] bucket : %s key : %s objId : %s versionId : %s pdiskid : %s rpdiskid : %s \n", 
                            mt.getBucket(), mt.getPath(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.isReplicaExist() ? mt.getReplicaDisk().getId() :  "");
                    if (fixObject(mt)== 0)
                       job_done++;
             
                } catch (ResourceNotFoundException | AllServiceOfflineException ex) {
                   objm.log(" [checkEachObject] Not found Bucket : %s key : %s objId : %s pdisk : %s \n",
                            mt.getBucket(), mt.getPath(), mt.getObjId(), 
                            mt.isPrimaryExist() ? mt.getPrimaryDisk().getPath() : "");
                }    
            }
            
            //offset = offset + numObjects;
            //System.out.println("----------------------------------------------------------->" + job_done);
        } while(list.size() == numObjects );
        //totalFixed = totalFixed + job_done;
        objm.updateNumberObjectsProcessed(totalChecked, totalFixed);
        objm.finishedJob();
        
        return job_done;  
    }
    
    public void setDebugModeOn(boolean debugOn){
        if (debugOn)
            objm.setDebugModeOn();
    }
    
    public String getDiskId(String diskName) throws ResourceNotFoundException{
        if (diskName.isEmpty())
            return diskName;
        
        return objm.getObjManagerUtil().getObjManagerConfig().getDiskIdWithName(diskName);
    }
    
    public long checkEachDisk(String bucketName) throws Exception{
        long job_done = 0;
        job_done =checkEachObject(bucketName);
  
        return job_done; 
    }
    
    public long checkEachOneDiskAllBucket(String diskId) throws Exception{
        long job_done = 0;
        
        for(Bucket bucket : bukList){
           objm.log("[checkEachDisk] bucketName : %s diskId : %s \n", bucket.getName(), diskId);
            job_done =job_done + checkEachObject(bucket.getName(), diskId);
            objm.log("[checkEachDisk] bucketName : %s diskId : %s job_done : %d\n", bucket.getName(), diskId, job_done);
        }
  
        return job_done;
    }
    
    public long checkEachBucket() throws Exception{
        long job_done = 0;

        for(Bucket bucket : bukList)
           job_done =job_done + checkEachDisk(bucket.getName());
        
        return job_done;
    }
    
    public long checkBucketDisk(String bucket, String diskId) throws Exception{
      objm.log("[checkBucketDisk] bucket : %s  \n", bucket);
       return checkEachObject(bucket, diskId);
    }
    
    public void getResultSummary(){
        System.out.println("|----------------------------------------------------------|");
        System.out.println("|         \t SUMMARY                                   |");
        System.out.println("|----------------------------------------------------------|");
        System.out.format("| Total Checked                   \t | %-15d |\n" , this.totalChecked);
        System.out.format("| Total Fixed                     \t | %-15d |\n" , this.totalFixed);
        System.out.format("| Total Problem with primary      \t | %-15d |\n" , this.problemType1);
        System.out.format("| Total Problem with replica      \t | %-15d |\n" , this.problemType2);
        System.out.format("| Total Problem with meta size    \t | %-15d |\n" , this.problemType3);
        System.out.format("| Total Problem with meta md5     \t | %-15d |\n" , this.problemType4);
        System.out.format("| Total Problem with all different\t | %-15d |\n" , this.problemType5);
        System.out.println("|----------------------------------------------------------|");
    }
}
