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
import com.pspace.ifs.ksan.util.ObjectMover;
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
    private List<String> dskList;
    private ObjectMover objm;
    private long totalChecked;
    private long totalFixed;
    
    public CBalance() throws Exception{
        //ObjManagerConfig config = new ObjManagerConfig();
        obmu = new ObjManagerUtil();
        //mqSender = new MQSender(config.mqHost, config.mqOsdExchangename, "topic", ""); 
        dskList = obmu.getExistedDiskList();
        bukList = obmu.getExistedBucketList();
        objm = new ObjectMover(false, "CBalance");
        totalChecked = 0;
        totalFixed = 0;
    }
    
    private List<Metadata> getListOfObjects(String bucketName, String SrcDiskId, long size, long offset, String DstDiskId){
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
            list = obmu.listObjects(bucketName, SrcDiskId, 1, offset, numObjects);
            if (list == null)
                return res;
            
            if (list.isEmpty())
                return res;
            
            Iterator<Metadata> it = list.iterator();
            while(it.hasNext())
            {
                totalChecked++;
                Metadata mt = it.next();
                if (mt.getSize() > size || mt.getSize() <= 0)
                    continue;
                
                try {
                    if(!obmu.allowedToReplicate(bucketName, mt.getPrimaryDisk(), mt.getReplicaDisk(), DstDiskId))
                        continue;
                    
                } catch (ResourceNotFoundException ex) {
                     if(!obmu.allowedToReplicate(bucketName, mt.getPrimaryDisk(), null, DstDiskId))
                        continue;
                }
                
                size_counter = size_counter + mt.getSize();    
                res.add(mt);
                if (size_counter > size)
                    return res;
            }
            
            offset = offset + numObjects;
        } while(list.size() == numObjects );        
        return res;  
    } 
        
    private List<Bucket> getBucketList(String bucketName){
        List<Bucket> bList;
        
        if (bucketName.isEmpty())
            bList= bukList;
        else{
            bList = new ArrayList();
            bukList.stream().filter(bucket -> (bucket.getName().equals(bucketName))).forEachOrdered(bucket -> {
                bList.add(bucket);
            });
        }
        return bList;
    }
    
    public long moveWithSize(String bucketName, String srcDiskId, long amountToMove, String dstDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        int ret;
        long offset = 0;
        long size;
        long size_counter = 0;
        long total_object;
        long checked_object = 0;
        List<Metadata> list;
        List<Bucket> bList;
        
        bList = getBucketList(bucketName);
        total_object = countEntry(bList,  srcDiskId); 
        System.out.println("total_job :> " + total_object);
        if (total_object == 0)
            return 0;
        
        size = amountToMove / bList.size();
        do{    
            for(Bucket bucket : bList){    
                list = getListOfObjects(bucket.getName(), srcDiskId, size,  offset, dstDiskId);
                //System.out.format("[moveWithSize] list_size : %d  bucketName : %s \n", list.size(), bucket.getName());
                Iterator<Metadata> it = list.iterator();
                while(it.hasNext())
                {
                    Metadata mt = it.next();
                    checked_object++;
                    if (mt.getSize() == 0)
                        continue;
                    
                    if (dstDiskId.isEmpty())
                        ret = moveSingleObject(bucket.getName(), mt.getObjId(), srcDiskId);
                    else
                        ret = moveSingleObject(bucket.getName(), mt.getObjId(), srcDiskId, dstDiskId);
                    
                    if (ret > 0)
                        size_counter = size_counter + mt.getSize();

                    if (size_counter >= amountToMove)
                        return size_counter;
                }    
            }
            offset = offset + 100;
            if (offset > total_object)
                break;
            //System.out.format("[moveWithSize] total_object : %d offset : %d  checked_object : %d\n", total_object, offset, checked_object);
        }while(checked_object < total_object);
        
        return size_counter;
    }
   
    private long countEntry(List<Bucket> bList, String srcDiskId){
       List<Metadata> list;
       long offset = 0; 
       long numObjects = 1000;
       long total_job = 0;
       long one_round = 0;
       
       do{
            one_round = 0;
            for(Bucket bucket : bList){
               list = obmu.listObjects(bucket.getName(), srcDiskId, 1, offset, numObjects); 
               Iterator<Metadata> it = list.iterator();
                while(it.hasNext()){
                    it.next();
                    total_job++;
                    one_round++;
                }
            }
            offset += numObjects;
            if (one_round == 0)
                break;
       }
       while(true);
       return total_job;
    }
    
    public long emptyDisk(String srcDiskId){
        List<Metadata> list;
        List<Bucket> bList;
        long numObjects = 100;
        long offset = 0;
        long job_done = 0;
        
        bList = getBucketList("");
        long total_job = countEntry(bList,  srcDiskId); 
        System.out.println("total_job :> " + total_job);
        if (total_job == 0)
            return total_job;
        
        do{
            for(Bucket bucket : bList){
                list = obmu.listObjects(bucket.getName(), srcDiskId, 1, offset, numObjects);
                Iterator<Metadata> it = list.iterator();
                while(it.hasNext()){
                    Metadata mt = it.next();
                    try {
                        objm.moveObject(bucket.getName(), mt.getObjId(), mt.getVersionId(), srcDiskId);
                        job_done++;
                    } catch ( Exception ex) {
                        Logger.getLogger(CBalance.class.getName()).log(Level.SEVERE, null, ex);
                    } 
                }
            }
        } 
        while(job_done < total_job);
        
        //System.out.format("[emptyDisk-2] total_job : %d job_done : %d\n", total_job, job_done);
        return job_done;
    }
    
    public long moveWithSize(String srcDiskId, long amountToMove) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return moveWithSize("", srcDiskId, amountToMove, "");
    }
    
    public long moveWithSize(String bucketName, String srcDiskId, long amountToMove) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return moveWithSize(bucketName, srcDiskId, amountToMove, "");
    }
    
    public int moveSingleObjectWithKey(String bucket, String key) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        Metadata mt = obmu.getObjectWithPath(bucket, key);
        if (mt == null)
            return -1;
        
        return objm.moveObject1(bucket, key, mt.getPrimaryDisk().getId());
    }
    
    public int moveSingleObjectWithKey(String bucket, String key, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return objm.moveObject1(bucket, key, srcDiskId);
    }
    
    public int moveSingleObjectWithKey(String bucket, String key, String srcDiskId, String dstDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return objm.moveObject1(bucket, key, srcDiskId, dstDiskId);
    }
    
    public int moveSingleObject(String bucket, String objId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        Metadata mt = obmu.getObject(bucket, objId);
        return objm.moveObject(bucket, objId, "", mt.getPrimaryDisk().getId());
    }
    
    public int moveSingleObject(String bucket, String objId, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return objm.moveObject(bucket, objId, "", srcDiskId);
    }
    
    public int moveSingleObject(String bucket, String objId, String srcDiskId, String dstDiskId) throws ResourceNotFoundException, AllServiceOfflineException, Exception{
        return objm.moveObject(bucket, objId, srcDiskId, dstDiskId);
    }
}
