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
package com.pspace.ifs.ksan.objmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Set;

/**
 *  Store diskpool and list of bucket in memory
 * @author legesse
 */
public class ObjManagerCache {
    private final HashMap<String, DISKPOOL> diskPoolMap;
    private final HashMap<String, Bucket> bucketMap;
    private DataRepository dbm;
    private GetFromPortal portal;
    private static Logger logger;
    
    public ObjManagerCache() throws IOException{
        diskPoolMap = new HashMap<>();
        bucketMap   = new HashMap<>();
        logger = LoggerFactory.getLogger(ObjManager.class);
        dbm = null;
        portal = new GetFromPortal();
    }
    
    // the db manager will be used to reload data incase not exist in memory
    public void setDBManager(DataRepository dbm){
        this.dbm = dbm;
    }
    
    public void setDiskPoolInCache(DISKPOOL dskPool){
        if (dskPool != null)
            diskPoolMap.putIfAbsent(dskPool.getId(), dskPool);
    }
    
    public void removeDiskPoolFromCache(String diskPoolId){
        diskPoolMap.remove(diskPoolId);
    }
    
    private DISKPOOL getDiskPool(String diskPoolId){
        DISKPOOL dskPool;
        dskPool = diskPoolMap.get(diskPoolId);
        if (dskPool == null) {
            this.reloadDiskPoolList();
            dskPool = diskPoolMap.get(diskPoolId);
        }
        
        return dskPool;
    }
    
    private DISKPOOL getDiskPoolWithName(String diskPoolName){
        DISKPOOL dskPool;
        for(String diskPoolId : diskPoolMap.keySet()){
            dskPool = diskPoolMap.get(diskPoolId);
            if (dskPool == null)
                continue;
            
            if (dskPool.getName() == null)
                continue;
            
            if (dskPool.getName().equals(diskPoolName)){
                return dskPool;
            }   
        }
        
        return null;
    }
    
    private DISKPOOL getDiskPoolfromMetadata(Metadata mt, String default_diskPoolId, boolean fromPrimary) throws ResourceNotFoundException{
        String diskPoolId;
        
        if (fromPrimary){
            diskPoolId = mt.getPrimaryDisk().getDiskPoolId();
        }
        else{
            diskPoolId = mt.getReplicaDisk().getDiskPoolId();
        }
        
        if (diskPoolId == null)
            diskPoolId = default_diskPoolId;
        else if (diskPoolId.isEmpty())
            diskPoolId = default_diskPoolId;
        
        DISKPOOL tmp = getDiskPool(diskPoolId);
        logger.debug("[getDiskPoolfromMetadata] key : {} primary {} def_diskpoolId : {} sel_diskPoolId : {} diskPool : {}", mt.getPath(), fromPrimary, default_diskPoolId, diskPoolId, tmp);
        return tmp;
    }
    
    public DISKPOOL getDiskPoolFromCache(String diskPoolId) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        dskPool = getDiskPool(diskPoolId);
        if (dskPool == null) {
            //here is no diskpool with id : " + diskPoolId +"!");
            throw new ResourceNotFoundException("There is no diskpool wih id : " + diskPoolId + " !");
        }
        return dskPool;
    }
    
    public DISKPOOL getDiskPoolFromCacheWithName(String diskPoolName) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        dskPool = this.getDiskPoolWithName(diskPoolName);
        if (dskPool == null) {
            throw new ResourceNotFoundException("There is no diskpool wih name : " + diskPoolName + " !");
        }
        return dskPool;
    }
    
    public DISKPOOL getDiskPoolFromCacheWithServerId(String serverId) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        
        for(String diskPoolId : diskPoolMap.keySet()){
            dskPool = getDiskPool(diskPoolId);
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
    
    public DISKPOOL getDiskPoolFromCacheWithDiskId(String diskId) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        
        for(String diskPoolId : diskPoolMap.keySet()){
            dskPool = getDiskPool(diskPoolId);
            if (dskPool.diskExistInPool(diskId)) {
                return dskPool;
            } // check the next diskPool
        }
        logger.error("[getDiskPoolFromCacheWithDiskId] There is no disk pool that hold a disk with that Id  : {}!", diskId);
       throw new ResourceNotFoundException("[getDiskPoolFromCacheWithDiskId] There is no disk pool that hold a disk with that Id  : " + diskId +"!"); 
    }
    
    public void setBucketInCache(Bucket bt){
        bucketMap.putIfAbsent(bt.getName(), bt);
    }
    
    public void updateBucketInCache(Bucket bt){
        bucketMap.replace(bt.getName(), bt);
    }
    
    public void removeBucketFromCache(String bucketName){
        bucketMap.remove(bucketName);
    }
    
    public Bucket getBucketFromCache(String bucketName){
        Bucket bt = bucketMap.get(bucketName);
        if (bt == null){
            try {
                bt = dbm.selectBucket(bucketName);
                if (bt != null)
                    setBucketInCache(bt);
            } catch (SQLException | ResourceNotFoundException ex) {
                return null;
            }
        }
        return bt;
    }
   
    private void reloadBucketList(){
        bucketMap.clear();
        dbm.loadBucketList();
    }
    
    public String[] getBucketNameList(){
        reloadBucketList();
        
        return bucketMap.keySet().toArray(new String[0]);
    }
    
    public List<S3BucketSimpleInfo> getBucketSimpleList( String userName, String userId) {
        List<S3BucketSimpleInfo> btList = new ArrayList<S3BucketSimpleInfo>();

        reloadBucketList(); // get list always bucket from db 
        
        for (String key : bucketMap.keySet()) {
            Bucket bt = bucketMap.get(key);
            if (bt.getUserId().equals(userId) || bt.getUserName().equals(userName)){
            	S3BucketSimpleInfo bsi = new S3BucketSimpleInfo();
            	bsi.setBucketName(bt.getName());
            	bsi.setCreateDate(bt.getCreateTime());
            	btList.add(bsi);
	    }
        }
         
        return btList;
    }

    public boolean bucketExist(String bucketName){
        if (bucketMap.containsKey(bucketName))
            return true;
        
        dbm.loadBucketList();
        return bucketMap.containsKey(bucketName);
    }
    
    /*public DISK getDiskWithPath( String dskPoolId, String dpath) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        DISK dsk;
        
        dskPool = getDiskPool(dskPoolId);
        if (dskPool != null){
            dsk = dskPool.getDisk(dpath, "");
            return dsk;
        }
        logger.error("There is no disk in the the server with path : {} and disk pool id : {}!", dpath, dskPoolId);
        throw new ResourceNotFoundException("There is no disk in the the server with path : " + 
                dpath +" and disk pool id : " + dskPoolId + "!"); 
    }*/
    
    public DISK getDiskWithId( String dskPoolId, String diskid) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        DISK dsk;
        
        dskPool = getDiskPool(dskPoolId);
        if (dskPool != null){
            dsk = dskPool.getDisk(diskid);
            dsk.setDiskPoolId(dskPool.getId());
            return dsk;
        }
        logger.error("There is no disk in the the server with diskid : {} at disk pool id : {}!", diskid, dskPoolId);
        throw new ResourceNotFoundException("There is no disk in the the server with diskid : " + 
                diskid +" at diskpool id : "+ dskPoolId +"!"); 
    }
    
    public DISK getDiskWithId(String diskid) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        DISK dsk;
        
        dskPool = getDiskPoolFromCacheWithDiskId(diskid);
        if (dskPool != null){
            dsk = dskPool.getDisk( diskid);
            dsk.setDiskPoolId(dskPool.getId());
            return dsk;
        }
        logger.error("[getDiskWithId] There is no disk in the the server with diskid : {}!", diskid);
        throw new ResourceNotFoundException("[getDiskWithId] There is no disk in the the server with diskid :" + diskid +"!"); 
    }
    
    public boolean validateDisk(String dskPoolId, String diskid, String dskPath){
         DISKPOOL dskPool;
         
         dskPool = getDiskPool(dskPoolId);
         if (dskPool == null)
             return false;
         
         return dskPool.diskExistInPool(diskid);
    }
    
    public boolean isDiskSeparatedAndValid(String dskPoolId, Metadata mt){
        DISKPOOL dskPool;
            
        try {
            dskPool = getDiskPoolfromMetadata(mt, dskPoolId, true);
            if (dskPool == null){
                //logger.debug("[isDiskSeparatedAndValid] key : {} diskpool not found primaryDiskPool : {} diskId : {}", mt.getPath(), mt.getPrimaryDisk().getDiskPoolId(), mt.getPrimaryDisk().getId());
                return false; // diskpool not exist
            }
        
            if (!dskPool.diskExistInPool(mt.getPrimaryDisk().getId())){
                //logger.debug("[isDiskSeparatedAndValid] key : {} primary disk not exist in diskpoolId : {} diskId : {}", mt.getPath(), dskPool.getId(), mt.getPrimaryDisk().getId());
                return false; // primary disk not exist
            }
        
            if (!mt.isReplicaExist())
                return true;
            
            dskPool = getDiskPoolfromMetadata(mt, dskPoolId, false);
            if (dskPool == null){
                //logger.debug("[isDiskSeparatedAndValid] key : {} diskpool not found replicaDiskPool : {} diskId : {}", mt.getPath(), mt.getReplicaDisk().getDiskPoolId(), mt.getReplicaDisk().getId());
                return false; // diskpool not exist
            }
            
            if (!dskPool.diskExistInPool(mt.getReplicaDisk().getId())){
                //logger.debug("[isDiskSeparatedAndValid] key : {} replica disk not exist in diskpoolId : {} diskId : {}", mt.getPath(), dskPool.getId(), mt.getReplicaDisk().getId());
                return false; // replica disk not exist
            }
            
            if ((mt.getPrimaryDisk().getOsdIp()).equals(mt.getReplicaDisk().getOsdIp())){
                //logger.debug("[isDiskSeparatedAndValid] key : {} posd_IP : {} rosd IP : {}", mt.getPath(), mt.getPrimaryDisk().getOsdIp(), mt.getReplicaDisk().getOsdIp());
                return false;
            }
            
        } catch (ResourceNotFoundException ex) {
            return true;
        }
       
        return true;
    }
    
    public SERVER getServerWithDiskPath( String dskPoolId, String dpath) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        
        dskPool = getDiskPool(dskPoolId);
        if (dskPool != null){
            return dskPool.getServer(dpath, "");
        }
        logger.error("There is no server in the the pool with disk path : {} and disk pool id : {}!", dpath, dskPoolId);
        throw new ResourceNotFoundException("There is no server in the the pool with disk path : " + 
                dpath +" and disk pool id : " + dskPoolId + "!"); 
    }
    
    public void reloadDiskPoolList() {
        try{
            portal.loadDiskPoolList(this);
        } catch (Exception ex){
           logger.error("failed to reload diskpools :> {} ", ex);
        }
    }
    
    public void displayBucketList(){
        /*Bucket bt;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("<BUCKETLIST>");
        for(String bucketName : bucketMap.keySet()){
            bt = bucketMap.get(bucketName);
            String formatTime = formatter.format(bt.getCreateTime());
            System.out.format("   <BUCKET id=\"%s\"  name=\"%s\" diskPoolId=\"%s\" Versioning=\"%s\" createTime=\"%s\" >\n", 
                    bt.getId(), bt.getName(), bt.getDiskPoolId(), bt.getVersioning(), formatTime);
        }
        System.out.println("</BUCKETLIST>");*/
    }
    
    public String displayDiskPoolList(){
        DISKPOOL dskPool;
        String dskpoolXml;
        
        dskpoolXml = "<DISKPOOLLIST>";
        for(String dskPoolId : diskPoolMap.keySet()){
             dskPool = diskPoolMap.get(dskPoolId);
             String dskpoolStr = String.format("\n   <DISKPOOL id=\"%s\"  name=\"%s\" defaultReplicaCount=%d numServer=\"%s\" >\n"
                     , dskPool.getId(), dskPool.getName(), dskPool.getDefaultReplicaCount(), dskPool.getNumServers());
             dskpoolStr = dskpoolStr + dskPool.displayServerList() + "\n   </DISKPOOL>";
             dskpoolXml = dskpoolXml + dskpoolStr;
         }
         dskpoolXml = dskpoolXml + "\n</DISKPOOLLIST>\n";
         return dskpoolXml;
    }

    public void resetBucketList() {
        bucketMap.clear();
    }
    
    public void dumpCacheInFile() throws IOException{
        try (PrintWriter printWriter = new PrintWriter(new FileWriter("/var/log/ksan/objmanager/diskpools_dump.xml"))) {
            printWriter.print(displayDiskPoolList()); 
            printWriter.close();
        } 
    }
}
