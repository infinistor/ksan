/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.objmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.gw.identity.S3BucketSimpleInfo;

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
