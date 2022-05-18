/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.util.fsck;


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
import com.pspace.ifs.ksan.util.ObjectMover;

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
                            objm.moveObject(bucketName, mt.getObjId(), mt.getVersionId(), mt.getPrimaryDisk().getId());
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
    
    public long checkEachDisk(String bucketName) throws Exception{
        long job_done = 0;
        
        for(String dsk : dskList){
            if (!dsk.isEmpty())
                job_done =job_done + checkEachObject(bucketName, dsk);
        }
    
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
       return checkEachObject(bucket, diskId);
    }
}
