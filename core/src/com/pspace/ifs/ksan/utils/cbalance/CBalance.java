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
package com.pspace.ifs.ksan.utils.cbalance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.utils.ObjectMover;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author legesse
 */
public class CBalance {
    private ObjManagerUtil obmu;
    //private boolean checkOnly;
    //private MQSender mqSender;
    private List<Bucket> bukList;
    //private List<String> dskList;
    private ObjectMover objm;
    private long totalChecked;
    private long totalFixed;
    private boolean isallowedToMoveTolocalDisk;
    
    public CBalance(boolean isallowedToMoveTolocalDisk) throws Exception{
        obmu = new ObjManagerUtil();
        //dskList = obmu.getExistedDiskList();
        bukList = obmu.getExistedBucketList();
        objm = new ObjectMover(obmu, false, "CBalance");
        objm.enableDisableLocalDiskMove(isallowedToMoveTolocalDisk);
        totalChecked = 0;
        totalFixed = 0;
        this.isallowedToMoveTolocalDisk = isallowedToMoveTolocalDisk;
    }
    
    private List<Metadata> getListOfObjects(String bucketName, String SrcDiskId, long size, String lastObjId, String DstDiskId){
        List<Metadata> res;
        List<Metadata> list;
        //long offset = 0;
        long numObjects = 100;
        //long job_done = 0;
        long size_counter = 0;
        int ret;
        
        res = new ArrayList();
        ret = objm.startJob();
        if (ret != 0)
            return res;
        
        do{
            list = obmu.listObjects(bucketName, SrcDiskId, lastObjId, numObjects *2 );
            if (list == null)
                return res;
            
            objm.log("[getListOfObjects] bucketName : %s list.size : %d res.size : %d  size : %d \n",  bucketName, list.size(), res.size(), size);
            if (list.isEmpty())
                return res;
            
            Iterator<Metadata> it = list.iterator();
            while(it.hasNext())
            {
                totalChecked++;
                Metadata mt = it.next();
                lastObjId = mt.getObjId();
                
                if (mt.getSize() > size || mt.getSize() <= 0)
                    continue;
                
               objm.log("[getListOfObjects] objid: %s size : %d \n", mt.getObjId(), mt.getSize());
                try {
                    if(!obmu.allowedToReplicate(bucketName, mt.getPrimaryDisk(), mt.getReplicaDisk(), DstDiskId, isallowedToMoveTolocalDisk)){
                       objm.log("[getListOfObjects] objid: %s pdisk : %s  rdisk : %s  newdisk : %s\n", mt.getObjId(), mt.getPrimaryDisk().getId(), mt.getReplicaDisk().getId(), DstDiskId);
                        continue;
                    }
                    
                } catch (ResourceNotFoundException ex) {
                     if(!obmu.allowedToReplicate(bucketName, mt.getPrimaryDisk(), null, DstDiskId)){
                         objm.log("[getListOfObjects] objid: %s size : %d Ex skipped\n", mt.getObjId(), mt.getSize());
                          continue;
                     }
                }
                /*try {
                   objm.log("[getListOfObjects] bucketName : %s primary : %s replica : %s new : %s \n", bucketName, mt.getPrimaryDisk().getId(), mt.getReplicaDisk().getId(), DstDiskId);
                } catch (ResourceNotFoundException ex) {
                  objm.log("[getListOfObjects] bucketName : %s primary : %s  new : %s \n", bucketName, mt.getPrimaryDisk().getId(),  DstDiskId);
                }*/
                size_counter = size_counter + mt.getSize();
                //System.out.format("[getListOfObjects] path :  %s size : %d counter : %d  expected : %d\n", mt.getPath(), mt.getSize(), size_counter, size);
                res.add(mt);
                if (size_counter > size)
                    return res;
                
                if (res.size() == numObjects)
                   return res; 
            }
            
        } while(res.size() < numObjects );        
        return res;  
    } 
        
    private List<Bucket> getBucketList(String bucketName){
        List<Bucket> bList;
        
        if (bucketName.isEmpty())
            bList= bukList;
        else{
            bList = new ArrayList();
            for (Bucket bt : bukList)
                 bList.add(bt);
            /*bukList.stream().filter(bucket -> (bucket.getName().equals(bucketName))).forEachOrdered(bucket -> {
                bList.add(bucket);
            });*/
        }
        return bList;
    }
    
    public void setDebugModeOn(){
        objm.setDebugModeOn();
    }
    
    public String getDiskIdWithName(String diskName) throws ResourceNotFoundException{
       
        if (diskName.isEmpty())
            return "";
        
        return obmu.getObjManagerConfig().getDiskIdWithName(diskName);
    }
    
    public long moveWithSize(String bucketName, String srcDiskId, long amountToMove, String dstDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        int ret;
        String lastObjId;
        long size_counter = 0;
        long total_object;
        long checked_object = 0;
        List<Metadata> list;
        List<Bucket> bList;
        HashMap<String, String> lastObjectMap = new HashMap();;
        
        bList = getBucketList(bucketName);
        total_object = countEntry(bList,  srcDiskId); 
        objm.log("total_job :> " + total_object + "\n");
        if (total_object == 0)
            return 0;
        
        do{     
            for(Bucket bucket : bList){
                lastObjId = lastObjectMap.get(bucket.getName());
                if (lastObjId == null)
                    lastObjId = " ";
                
                list = obmu.listObjects(bucket.getName(), srcDiskId, lastObjId, 100);
                //list = getListOfObjects(bucket.getName(), srcDiskId, size,  lastObjId, dstDiskId);
                //System.out.format("[moveWithSize] list_size : %d  bucketName : %s \n", list.size(), bucket.getName());
                Iterator<Metadata> it = list.iterator();
                while(it.hasNext())
                {
                    Metadata mt = it.next();
                    lastObjId = mt.getObjId();
                    checked_object++;
                    
                    if (mt.getSize() == 0 || mt.getSize() > amountToMove)
                        continue;
                    
                    //ret = 0;
                    objm.log("[moveWithSize] objid: %s size : %d amountToMove : %d size_counter : %d\n", mt.getObjId(), mt.getSize(), amountToMove, size_counter);
                    try{
                        
                        if (dstDiskId.isEmpty()){
                            ret = moveSingleObject(bucket.getName(), mt.getObjId(), mt.getVersionId(), srcDiskId);
                        } else{
                            if (!obmu.allowedToReplicate(bucket.getName(), mt.getPrimaryDisk(), mt.getReplicaDisk(), dstDiskId, isallowedToMoveTolocalDisk)){
                                continue;
                            }
                            ret = moveSingleObject(bucket.getName(), mt.getObjId(), mt.getVersionId(), srcDiskId, dstDiskId);
                        }
                    } catch( ResourceNotFoundException ex){
                        continue; //ignore
                    }
                    
                    /*try {
                       objm.log("[moveWithSize] bucketName : %s primary : %s replica : %s new : %s size : %d  amountToMove : %d  size_counter : %d \n", bucketName, mt.getPrimaryDisk().getId(), mt.getReplicaDisk().getId(), dstDiskId, mt.getSize(), amountToMove, size_counter );
                    } catch (ResourceNotFoundException ex) {
                       objm.log("[getListOfObjects] bucketName : %s primary : %s  new : %s  size_counter : %d \n", bucketName, mt.getPrimaryDisk().getId(),  dstDiskId, size_counter);
                    }*/
                    if (ret == 0)
                        size_counter = size_counter + mt.getSize();

                    if (size_counter >= amountToMove){
                       objm.log("[moveWithSize] size_counter : %d amountToMove : %d end \n", size_counter, amountToMove);
                        return size_counter;
                    }
                }
               lastObjectMap.put(bucket.getName(), lastObjId);
            }
            
            //if (lastObjId.isEmpty())
            //    break;
            //offset = offset + 100;
            //if (offset > total_object)
            //    break;
           objm.log("[moveWithSize] total_object : %d checked_object : %d\n", total_object, checked_object);
        }while(checked_object < total_object);
        
        return size_counter;
    }
   
    private long countEntry(List<Bucket> bList, String srcDiskId){
       long total_job = 0;
       
       for(Bucket bucket : bList){
           total_job += obmu.listObjectsCount(bucket.getName(), srcDiskId);
           //System.out.format("[countEntry] bucket : %s total_job : %d\n", bucket.getName(), total_job);
       }
       return total_job;
    }
    
    public long emptyDisk(String srcDiskId){
        List<Metadata> list;
        List<Bucket> bList;
        long numObjects = 1000;
        //long offset = 0;
        long job_done = 0;
        long not_processed = 0;
        long direct_count = 0;
        String lastObjId;
        
        bList = getBucketList("");
        long total_job = countEntry(bList,  srcDiskId); 
       objm.log("total_job :> " + total_job);
        if (total_job == 0)
            return total_job;
        
        //do{
        for(Bucket bucket : bList){
            lastObjId = " ";
            do{
                list = obmu.listObjects(bucket.getName(), srcDiskId, lastObjId, numObjects);
               objm.log("[emptyDisk] bucket : %s diskid : %s lastObjId : %s numObject: %d listSize : %d empty : %b \n", bucket.getName(), srcDiskId, lastObjId, numObjects, list.size(), list.isEmpty());
                Iterator<Metadata> it = list.iterator();
                while(it.hasNext()){
                    Metadata mt = it.next();
                    lastObjId = mt.getObjId();
                    if (mt.getPath().endsWith("/")){
                       objm.log("[emptyDisk] DIR bucket : %s path : %s objid : %s skipped\n",
                                mt.getBucket(), mt.getPath(), mt.getObjId());
                        direct_count++;
                        continue;
                    }
                    try {
                       objm.log("[emptyDisk] bucket : %s path : %s objid : %s version : %s pdiskid : %s (%s) rdiskid: %s (%s)\n",
                                mt.getBucket(), mt.getPath(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.getPrimaryDisk().getOsdIp()
                                , mt.getReplicaDisk().getId(), mt.getReplicaDisk().getOsdIp());
                    } catch (ResourceNotFoundException ex) {
                       objm.log("[emptyDisk] bucket : %s path : %s objid : %s version : %s pdiskid : %s (%s) \n",
                                mt.getBucket(), mt.getPath(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.getPrimaryDisk().getOsdIp());
                    }
                    
                    try {
                        if (moveSingleObject(bucket.getName(), mt.getObjId(), mt.getVersionId(), srcDiskId) == 0)
                            job_done++;
                        else
                            not_processed++;
                    } catch ( Exception ex) {
                        if (objm.isDebugModeOn())
                            Logger.getLogger(CBalance.class.getName()).log(Level.SEVERE, null, ex);
                    } 
                }
            } while (!list.isEmpty());
        }
        
       objm.log("[emptyDisk] total_job : %d job_done : %d  failed : %d directory_count : %d\n", total_job, job_done, not_processed, direct_count);
        return job_done;
    }
    
    public long moveWithSize(String srcDiskId, long amountToMove) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return moveWithSize("", srcDiskId, amountToMove, "");
    }
    
    public long moveWithSize(String bucketName, String srcDiskId, long amountToMove) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return moveWithSize(bucketName, srcDiskId, amountToMove, "");
    }
    
    public int moveSingleObjectWithKey(String bucket, String key, String versionId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        Metadata mt = obmu.getObjectWithPath(bucket, key, versionId);
        if (mt == null)
            return -1;
        
        return objm.moveObject1(bucket, key, versionId, mt.getReplicaDisk().getId());
    }
    
    public int moveSingleObjectWithKey(String bucket, String key, String versionId, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return objm.moveObject1(bucket, key, versionId, srcDiskId);
    }
    
    public int moveSingleObjectWithKey(String bucket, String key, String versionId, String srcDiskId, String dstDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return objm.moveObject1(bucket, key, versionId, srcDiskId, dstDiskId);
    }
    
    public int moveSingleObject(String bucket, String objId, String versionId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        Metadata mt = obmu.getObject(bucket, objId);
        return objm.moveObject(bucket, objId,  versionId, mt.getPrimaryDisk().getId());
    }
    
    public int moveSingleObject(String bucket, String objId, String versionId, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return objm.moveObject(bucket, objId, versionId, srcDiskId);
    }
    
    public int moveSingleObject(String bucket, String objId, String versionId, String srcDiskId, String dstDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return objm.moveObject(bucket, objId, versionId, srcDiskId, dstDiskId);
    }
}
