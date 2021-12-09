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
package com.pspace.ifs.ksan.objmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pspace.ifs.ksan.gw.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

/**
 *  Store diskpool and list of bucket in memory
 * @author legesse
 */
public class ObjManagerCache {
    private final HashMap<String, DISKPOOL> diskPoolMap;
    private final HashMap<String, Bucket> bucketMap;
    private static Logger logger;
    
    public ObjManagerCache(){
        diskPoolMap = new HashMap<>();
        bucketMap   = new HashMap<>();
        logger = LoggerFactory.getLogger(ObjManager.class);
    }
    
    public void setDiskPoolInCache(DISKPOOL dskPool){
        if (dskPool != null)
            diskPoolMap.putIfAbsent(dskPool.getId(), dskPool);
    }
    
    public void removeDiskPoolFromCache(String diskPoolId){
        diskPoolMap.remove(diskPoolId);
    }
    
    public DISKPOOL getDiskPoolFromCache(String diskPoolId) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        dskPool = diskPoolMap.get(diskPoolId);
        if (dskPool == null) {
            logger.error("diskPool is null");
            logger.error("There is no diskpool with id : {}!", diskPoolId);
             throw new ResourceNotFoundException("There is no diskpool with id : " + diskPoolId +"!");
        }
        return dskPool;
    }
    
    public DISKPOOL getDiskPoolFromCacheWithServerId(String serverId) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        
        for(String diskPoolId : diskPoolMap.keySet()){
            dskPool = diskPoolMap.get(diskPoolId);
            try {
                dskPool.getServerById(serverId);
                return dskPool;
            } catch (ResourceNotFoundException ex) {
                // check the next diskPool   
            }
        }
        logger.error("There is no server in the disk pool with that serverid : {}!", serverId);
       throw new ResourceNotFoundException("There is no server in the disk pool with that serverid : " + serverId +"!"); 
    }
    
    public void setBucketInCache(Bucket bt){
        bucketMap.putIfAbsent(bt.getName(), bt);
    }
    
    public void removeBucketFromCache(String bucketName){
        bucketMap.remove(bucketName);
    }
    
    public Bucket getBucketFromCache(String bucketName){
        Bucket bt = bucketMap.get(bucketName);
        return bt;
    }
   
    public String[] getBucketNameList(){
    
        return bucketMap.keySet().toArray(new String[0]);
    }
    
    public List<S3BucketSimpleInfo> getBucketSimpleList() {
        List<S3BucketSimpleInfo> btList = new ArrayList<S3BucketSimpleInfo>();

        for (String key : bucketMap.keySet()) {
            Bucket bt = bucketMap.get(key);
            S3BucketSimpleInfo bsi = new S3BucketSimpleInfo();
            bsi.setBucketName(bt.getName());
            bsi.setCreateDate(bt.getCreateTime());

            btList.add(bsi);
        }

        return btList;
    }

    public boolean bucketExist(String bucketName){
        return bucketMap.containsKey(bucketName);
    }
    
    public DISK getDiskWithPath( String dskPoolId, String dpath) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        DISK dsk;
        
        dskPool = diskPoolMap.get(dskPoolId);
        if (dskPool != null){
            dsk = dskPool.getDisk(dpath, "");
            return dsk;
        }
        logger.error("There is no disk in the the server with path : {} and disk pool id : {}!", dpath, dskPoolId);
        throw new ResourceNotFoundException("There is no disk in the the server with path : " + 
                dpath +" and disk pool id : " + dskPoolId + "!"); 
    }
    
    public DISK getDiskWithId( String dskPoolId, String diskid) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        DISK dsk;
        
        dskPool = diskPoolMap.get(dskPoolId);
        if (dskPool != null){
            dsk = dskPool.getDisk("", diskid);
            return dsk;
        }
        logger.error("There is no disk in the the server with diskid : {} at disk pool id : {}!", diskid, dskPoolId);
        throw new ResourceNotFoundException("There is no disk in the the server with diskid : " + 
                diskid +" at diskpool id : "+ dskPoolId +"!"); 
    }
    
    public boolean validateDisk(String dskPoolId, String diskid, String dskPath){
         DISKPOOL dskPool;
         
         dskPool = diskPoolMap.get(dskPoolId);
         if (dskPool == null)
             return false;
         
         return dskPool.diskExistInPool(diskid, dskPath);
    }
    
    public SERVER getServerWithDiskPath( String dskPoolId, String dpath) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        
        dskPool = diskPoolMap.get(dskPoolId);
        if (dskPool != null){
            return dskPool.getServer(dpath, "");
        }
        logger.error("There is no server in the the pool with disk path : {} and disk pool id : {}!", dpath, dskPoolId);
        throw new ResourceNotFoundException("There is no server in the the pool with disk path : " + 
                dpath +" and disk pool id : " + dskPoolId + "!"); 
    }
    
    public void displayBucketList(){
        Bucket bt;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("<BUCKETLIST>");
        for(String bucketName : bucketMap.keySet()){
            bt = bucketMap.get(bucketName);
            String formatTime = formatter.format(bt.getCreateTime());
            System.out.format("   <BUCKET id=\"%s\"  name=\"%s\" diskPoolId=\"%s\" Versioning=\"%s\" createTime=\"%s\" >\n", 
                    bt.getId(), bt.getName(), bt.getDiskPoolId(), bt.getVersioning(), formatTime);
        }
        System.out.println("</BUCKETLIST>");
    }
    
    public void displayDiskPoolList(){
        DISKPOOL dskPool;
        
        
        System.out.println("<DISKPOOLLIST>");
        for(String dskPoolId : diskPoolMap.keySet()){
             dskPool = diskPoolMap.get(dskPoolId);
             System.out.format("   <DISKPOOL id=\"%s\"  name=\"%s\" numServer=\"%s\" >\n"
                     , dskPool.getId(), dskPool.getName(), dskPool.getNumServers());
             dskPool.displayServerList();
             System.out.println("   </DISKPOOL>");
         }
         System.out.println("</DISKPOOLLIST>");
    }

    public void resetBucketList() {
        bucketMap.clear();
    }
}
