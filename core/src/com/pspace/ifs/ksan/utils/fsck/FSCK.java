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

import com.pspace.ifs.ksan.libs.mq.MQSender;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.OSDClient;
import com.pspace.ifs.ksan.objmanager.OSDResponseParser;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.utils.ObjectMover;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author legesse
 */

public class FSCK {
    private ObjManagerUtil obmu;
    private boolean checkOnly;
    private OSDClient osdc;
    private List<Bucket> bukList;
    private List<String> dskList;
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
        dskList = obmu.getExistedDiskList();
        bukList = obmu.getExistedBucketList();
        
    }
      
    private OSDResponseParser getAttr(String bucket, String objId, String versionId, String diskId, String mpath, String serverId) throws Exception{
        String attr;
        attr = osdc.getObjectAttr(bucket, objId, versionId, diskId, mpath, serverId);
        return new OSDResponseParser(attr);
    }
    
    private int checkObjectCorrectness(Metadata mt, OSDResponseParser primary, OSDResponseParser replica) throws Exception{
        
        if (mt.getReplicaCount() > 1 ){ // when only one object exist
            if (!mt.isPrimaryExist()){
                problemType1++;
                return 1; // copy replica to primary
            }
            
            if (!mt.isReplicaExist()){
                problemType2++;
                return 2; // copy primary to replica
            }
            
            primary = getAttr(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.getPrimaryDisk().getPath(), mt.getPrimaryDisk().getOSDServerId());
            replica = getAttr(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getReplicaDisk().getId(), mt.getReplicaDisk().getPath(), mt.getReplicaDisk().getOSDServerId()); 
            if (primary.md5.equals(replica.md5) && primary.md5.equals(mt.getEtag())){ 
                if (primary.size == replica.size && primary.size == mt.getSize())
                    return 0;
                problemType3++;
                return 3; // fix meta size
            }
            else if (!primary.md5.equals(replica.md5)){
                if (primary.md5.equals(mt.getEtag())){
                    problemType2++;
                    return 2; //cpy primary
                }else if(replica.md5.equals(mt.getEtag())){
                    problemType1++;
                    return 1; // cpy relica
                } else{
                    problemType5++;
                    return 5; // difficut to fix
                }
            }
            else {
                problemType4++;
                return 4; // fix md5 of meta
            }
        }
        
        return 0;
    }
    
    private int fixObject(Metadata mt) throws Exception{
        int ret = -1;
        int check = 0;
        OSDResponseParser primary = null, replica = null;
        
        try {
            check = checkObjectCorrectness(mt, primary, replica);
            if (check == 0)
                return 0;
        } catch (Exception ex) {
            System.out.format("[fixObject] bucket : %s objId : %s versionId : %s unable to check due to %s", mt.getBucket(), mt.getObjId(), mt.getVersionId(), ex.getMessage());
            return -1;
        }
        
        if (checkOnly){
            return check > 0 ? -1 : 0;
        }
            
        switch(check){
            case 1:
                ret = objm.moveObject(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId());
                break;
            case 2:
                ret = objm.moveObject(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getReplicaDisk().getId());
                break;
            case 3: // update meta size
                obmu.updateObject(mt.getBucket(), mt.getObjId(), mt, 2);
                break;
            case 4: // update meta md5
                obmu.updateObject(mt.getBucket(), mt.getObjId(), mt, 1);
                break;
            case 5:
                System.out.format("[fixObject] bucket : %s objId : %s versionId : %s  md5{meta, primary, replica} : {%s, %s, %s} all are different\n", 
                        mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getEtag(), primary.md5, replica.md5);
                break;
        }
        
        if (ret == 0)
            totalFixed++;
        
        return ret;
    }
    
    private long checkEachObject(String bucketName, String diskid) throws Exception{
        List<Metadata> list;
        long offset = 0;
        long numObjects = 100;
        long job_done = 0;
        int ret;
        
        
        ret = objm.startJob();
        if (ret != 0)
            return 0;
        
        do{
            list = obmu.listObjects(bucketName, diskid, offset, numObjects);
            if (list == null)
                return 0;
            
            if (list.isEmpty())
                return 0;
            
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
                try {
                    /*System.out.format("[checkEachObject] bucket : %s path : %s versionId : %s pdiskid : %s rpdiskid : %s \n", 
                            mt.getBucket(), mt.getPath(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.getReplicaDisk().getId());*/
                    if ((ret = fixObject(mt))== 0)
                        job_done++;
                
                } catch (ResourceNotFoundException | AllServiceOfflineException ex) {
                    System.out.format(" Bucket : %s path : %s pdisk : %s \n",
                            mt.getBucket(), mt.getPath(),
                            mt.isPrimaryExist() ? mt.getPrimaryDisk().getPath() : "");
                }    
            }
            
            offset = offset + numObjects;
            //System.out.println("----------------------------------------------------------->" + job_done);
        } while(list.size() == numObjects );
        //totalFixed = totalFixed + job_done;
        objm.updateNumberObjectsProcessed(totalChecked, totalFixed);
        objm.finishedJob();
        
        return job_done;  
    }
      
    private long checkEachObject(String bucketName) throws Exception{
        List<Metadata> list;
        long offset = 0;
        long numObjects = 100;
        long job_done = 0;
        int ret;
        
        
        ret = objm.startJob();
        //System.out.format("[checkEachObject] bucket : %s  diskid : %s ret : %d\n", bucketName, diskid, ret);
        if (ret != 0)
            return 0;
        
        do{
            list = obmu.listObjects(bucketName,  offset, numObjects);
            if (list == null)
                return 0;
            
            //System.out.format("[checkEachObject] bucket : %s  diskid : %s list# : %d\n", bucketName, diskid, list.size());
            if (list.isEmpty())
                return 0;
            
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
                try {
                    /*System.out.format("[checkEachObject] bucket : %s path : %s versionId : %s pdiskid : %s rpdiskid : %s \n", 
                            mt.getBucket(), mt.getPath(), mt.getVersionId(), mt.getPrimaryDisk().getId(), mt.getReplicaDisk().getId());*/
                    if ((ret = fixObject(mt))== 0)
                       job_done++;
                    /*if (!mt.isReplicaExist()){
                        System.out.format(">> Bucket : %s path : %s pdisk : %s rdisk : %s \n",
                                mt.getBucket(), mt.getPath(),
                                mt.isPrimaryExist() ? mt.getPrimaryDisk().getPath() : " Empty",
                                mt.isReplicaExist() ? mt.getReplicaDisk().getPath() : " Empty");
                        if (!checkOnly)
                            objm.moveObject(bucketName, mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId());
                        job_done++;
                    }*/
                } catch (ResourceNotFoundException | AllServiceOfflineException ex) {
                    System.out.format(" Bucket : %s path : %s pdisk : %s \n",
                            mt.getBucket(), mt.getPath(),
                            mt.isPrimaryExist() ? mt.getPrimaryDisk().getPath() : "");
                }    
            }
            
            offset = offset + numObjects;
            //System.out.println("----------------------------------------------------------->" + job_done);
        } while(list.size() == numObjects );
        //totalFixed = totalFixed + job_done;
        objm.updateNumberObjectsProcessed(totalChecked, totalFixed);
        objm.finishedJob();
        
        return job_done;  
    }
    
    public long checkEachDisk(String bucketName) throws Exception{
        long job_done = 0;
        job_done =checkEachObject(bucketName);
  
        return job_done; 
    }
    
    public long checkEachOneDiskAllBucket(String diskId) throws Exception{
        long job_done = 0;
        
        for(Bucket bucket : bukList)
            job_done =job_done + checkEachObject(bucket.getName(), diskId);
  
        return job_done;
    }
    
    public long checkEachBucket() throws Exception{
        long job_done = 0;

        for(Bucket bucket : bukList)
           job_done =job_done + checkEachDisk(bucket.getName());
        
        return job_done;
    }
    
    public long checkBucketDisk(String bucket, String diskId) throws Exception{
       System.out.format("[checkBucketDisk] bucket : %s  \n", bucket);
       return checkEachObject(bucket, diskId);
    }
    
    public void getResultSummary(){
        System.out.println("|----------------------------------------------------------|");
        System.out.println("|         \t SUMMARY                                   |");
        System.out.println("|----------------------------------------------------------|");
        System.out.format("| Total Checked                   \t | %-15d |\n" , this.totalChecked);
        System.out.format("| Total Fixed                     \t | %-15d |\n" , this.totalFixed);
        System.out.format("| Total Problem with replica      \t | %-15d |\n" , this.problemType1);
        System.out.format("| Total Problem with primary      \t | %-15d |\n" , this.problemType2);
        System.out.format("| Total Problem with meta size    \t | %-15d |\n" , this.problemType3);
        System.out.format("| Total Problem with meta md5     \t | %-15d |\n" , this.problemType4);
        System.out.format("| Total Problem with all different\t | %-15d |\n" , this.problemType4);
        System.out.println("|----------------------------------------------------------|");
    }
}
