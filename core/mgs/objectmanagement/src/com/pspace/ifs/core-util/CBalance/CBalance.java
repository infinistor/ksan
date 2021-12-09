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
package com.pspace.ifs.ksan.utility.CBalance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.pspace.ifs.ksan.mq.MQSender;
import com.pspace.ifs.ksan.objmanager.Bucket;
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
public class CBalance {
    private ObjManagerUtil obmu;
    //private boolean checkOnly;
    private MQSender mqSender;
    private List<Bucket> bukList;
    private List<String> dskList;
    private ObjectMover objm;
    private long totalChecked;
    private long totalFixed;
    
    public CBalance() throws Exception{
        ObjManagerConfig config = new ObjManagerConfig();
        obmu = new ObjManagerUtil();
        mqSender = new MQSender(config.mqHost, config.mqOsdExchangename, "topic", ""); 
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
    
    public long moveWithSize(String bucketName, String srcDiskId, long amountToMove, String dstDiskId) throws ResourceNotFoundException, AllServiceOfflineException{
        int ret;
        int retry = 3;
        long offset = 0;
        long size;
        long size_counter = 0;
        List<Metadata> list;
        List<Bucket> bList;
        
        bList = getBucketList(bucketName);
        do{
            size = amountToMove / bList.size();
            for(Bucket bucket : bList){    
                list = getListOfObjects(bucket.getName(), srcDiskId, size,  offset, dstDiskId);

                Iterator<Metadata> it = list.iterator();
                while(it.hasNext())
                {
                    Metadata mt = it.next();
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
            retry--;
        }while(retry > 0);
        
        return size_counter;
    }
    
    public long moveWithSize(String srcDiskId, long amountToMove) throws ResourceNotFoundException, AllServiceOfflineException{
        return moveWithSize("", srcDiskId, amountToMove, "");
    }
    
    public long moveWithSize(String bucketName, String srcDiskId, long amountToMove) throws ResourceNotFoundException, AllServiceOfflineException{
        return moveWithSize(bucketName, srcDiskId, amountToMove, "");
    }
    
    public int moveSingleObjectWithKey(String bucket, String key) throws ResourceNotFoundException, AllServiceOfflineException{
        Metadata mt = obmu.getObjectWithPath(bucket, key);
        if (mt == null)
            return -1;
        
        return objm.moveObject1(bucket, key, mt.getPrimaryDisk().getId());
    }
    
    public int moveSingleObjectWithKey(String bucket, String key, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException{
        return objm.moveObject1(bucket, key, srcDiskId);
    }
    
    public int moveSingleObjectWithKey(String bucket, String key, String srcDiskId, String dstDiskId) throws ResourceNotFoundException, AllServiceOfflineException{
        return objm.moveObject1(bucket, key, srcDiskId, dstDiskId);
    }
    
    public int moveSingleObject(String bucket, String objId) throws ResourceNotFoundException, AllServiceOfflineException{
        Metadata mt = obmu.getObject(bucket, objId);
        return objm.moveObject(bucket, objId, mt.getPrimaryDisk().getId());
    }
    
    public int moveSingleObject(String bucket, String objId, String srcDiskId) throws ResourceNotFoundException, AllServiceOfflineException{
        return objm.moveObject(bucket, objId, srcDiskId);
    }
    
    public int moveSingleObject(String bucket, String objId, String srcDiskId, String dstDiskId) throws ResourceNotFoundException, AllServiceOfflineException{
        return objm.moveObject(bucket, objId, srcDiskId, dstDiskId);
    }
}
