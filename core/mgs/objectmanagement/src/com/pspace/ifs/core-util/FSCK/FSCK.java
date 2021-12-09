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
package com.pspace.ifs.ksan.utility.FSCK;


import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pspace.ifs.ksan.mq.MQSender;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.DISK;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.utility.ObjectMover;

/**
 *
 * @author legesse
 */

public class FSCK {
    private ObjManagerUtil obmu;
    private boolean checkOnly;
    private MQSender mqSender;
    private List<Bucket> bukList;
    private List<String> dskList;
    private ObjectMover objm;
    private long totalChecked;
    private long totalFixed;
    
    public FSCK(boolean checkonly) throws Exception{
        ObjManagerConfig config = new ObjManagerConfig();
        obmu = new ObjManagerUtil();
        mqSender = new MQSender(config.mqHost, config.mqOsdExchangename, "topic", ""); 
        dskList = obmu.getExistedDiskList();
        bukList = obmu.getExistedBucketList();
        checkOnly = checkonly;
        objm = new ObjectMover(checkOnly, "FSCK");
        totalChecked = 0;
        totalFixed = 0;
    }
    
    /*public void howtouse(String progName){
        System.out.format("%s FSCK --action=<check|fix> --type=<Server|Disk|Object> --serverid=<OsdServerId> --diskid=<OsdDiskId> --objid=<ObjectId>\n"
                + "  Where: "
                + " --type  : server if the whole server failed, Disk if a disk failed and otherwise Object if a single object failed ", progName);
    }
    private int parseArgs(String [] args){
        String argInLower;
        String tmp;
        for(String arg : args){
            argInLower = arg.toLowerCase();
            if (argInLower.startsWith("--action")){ 
                tmp =argInLower.split("=")[1];
                if (tmp.equalsIgnoreCase("fix"))
                    isFixOn = true;      
            }
            if (argInLower.startsWith("--type")) {
                type = argInLower.split("=")[1].toLowerCase();
            }
            if (argInLower.startsWith("--serverid")) serverId = argInLower.split("=")[1];
            if (argInLower.startsWith("--diskid")) diskid = argInLower.split("=")[1];
            if (argInLower.startsWith("--objid")) objid = argInLower.split("=")[1];
        }
        
        return 0;
    }
    
    private DISK getNewDisk(String bucketName, String pdpath, String pdiskid) throws ResourceNotFoundException, AllServiceOfflineException{
        return obmu.allocReplicaDisk(bucketName, pdpath, pdiskid);
    }
    
    private int getObjectAttr(){
        return 0;
    }
    
    private int replicate(String objid, DISK from, DISK to) throws Exception{
        String bindingKey="recoverObject";
        String msg2send = String.format("{ from: %s to : %s }", from.toString(), to.toString());
        
        mqSender.send(msg2send, bindingKey);
        return 0;
    }
    
    private int repairObject(Metadata mt, String path, String diskid){
        obmu.updateObject(mt.getBucket(), mt.getObjId(),  mt, 1);
        return 0;
    }*/
    
    private long checkEachObject(String bucketName, String diskid){
        List<Metadata> list;
        long offset = 0;
        long numObjects = 100;
        long job_done = 0;
        int ret;
        
        ret = objm.startJob();
        if (ret != 0)
            return 0;
        
        do{
            list = obmu.listObjects(bucketName, diskid, 1, offset, numObjects);
            if (list == null)
                return 0;
            
            if (list.isEmpty())
                return 0;
            
            Iterator<Metadata> it = list.iterator();
            while(it.hasNext())
            {
                totalChecked++;
                if (!objm.ISJobRunning()){
                    totalFixed = totalFixed + job_done;
                     objm.updateNumberObjectsProcessed(totalChecked, totalFixed); 
                    return job_done;
                }
                
                Metadata mt = it.next();
                try {
                    if (!mt.isReplicaExist()){
                        System.out.format(">> Bucket : %s path : %s pdisk : %s rdisk : %s \n",
                                mt.getBucket(), mt.getPath(),
                                mt.isPrimaryExist() ? mt.getPrimaryDisk().getPath() : " Empty",
                                mt.isReplicaExist() ? mt.getReplicaDisk().getPath() : " Empty");
                        if (!checkOnly)
                            objm.moveObject(bucketName, mt.getObjId(), mt.getPrimaryDisk().getId());
                        job_done++;
                    }
                } catch (ResourceNotFoundException | AllServiceOfflineException ex) {
                    System.out.format(" Bucket : %s path : %s pdisk : %s \n",
                            mt.getBucket(), mt.getPath(),
                            mt.isPrimaryExist() ? mt.getPrimaryDisk().getPath() : "");
                }    
            }
            
            offset = offset + numObjects;
            System.out.println("----------------------------------------------------------->" + job_done);
        } while(list.size() == numObjects );
        totalFixed = totalFixed + job_done;
        objm.updateNumberObjectsProcessed(totalChecked, totalFixed);
        objm.finishedJob();
        
        return job_done;  
    }
    
    public long checkEachDisk(String bucketName){
        long job_done = 0;
        
        for(String dsk : dskList){
            if (!dsk.isEmpty())
                job_done =job_done + checkEachObject(bucketName, dsk);
        }
    
        return job_done; 
    }
    
    public long checkEachOneDiskAllBucket(String diskId){
        long job_done = 0;
        
        for(Bucket bucket : bukList)
            job_done =job_done + checkEachObject(bucket.getName(), diskId);
  
        return job_done;
    }
    
    public long checkEachBucket(){
        long job_done = 0;

        for(Bucket bucket : bukList)
           job_done =job_done + checkEachDisk(bucket.getName());
        
        return job_done;
    }
    
    public long checkBucketDisk(String bucket, String diskId){
       return checkEachObject(bucket, diskId);
    }
}
