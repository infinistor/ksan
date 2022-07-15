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

import java.sql.SQLException;
import java.util.List;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class ObjManagerUtil {
    private DataRepository dbm;
    private DiskAllocation dAlloc;
    private ObjManagerCache  obmCache;
    private ObjManagerConfig config;
    private OSDClient osdc;
    private ObjManagerSharedResource omsr;
    private static Logger logger;
    
    public ObjManagerUtil() throws Exception{
            
            config = new ObjManagerConfig();
            
            omsr = ObjManagerSharedResource.getInstance(config, false);
            
            obmCache = omsr.getCache();
          
            dbm = new DataRepositoryLoader(config, obmCache).getDataRepository();
            
            osdc = new OSDClient(config);
            //config.loadDiskPools(obmCache);
           
            //dbm.loadBucketList();
            
            obmCache.setDBManager(dbm);
                
            dAlloc = new DiskAllocation(obmCache);
            
            logger =  LoggerFactory.getLogger(ObjManagerUtil.class);
    }
    
    private Bucket getBucket(String bucketName) throws ResourceNotFoundException, SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        if (bt == null)
            bt = dbm.selectBucket(bucketName);
        
        if (bt == null)
            throw new ResourceNotFoundException( "BucketName : " + bucketName + " not exist in the system!");  
        
        return bt;
    }
    
    public Metadata getObject(String bucketName, String objid) throws ResourceNotFoundException{
        Bucket bt;
        
        try {
            bt = this.getBucket(bucketName);
        } catch (SQLException ex) {
            throw new ResourceNotFoundException(ex.getMessage());
        }
        return dbm.selectSingleObjectWithObjId(bt.getDiskPoolId(), bucketName, objid);
    }
    
    public Metadata getObject(String bucketName, String objid, String versionId) throws ResourceNotFoundException{
        Bucket bt;
        
        try {
            bt = this.getBucket(bucketName);
        } catch (SQLException ex) {
            throw new ResourceNotFoundException(ex.getMessage());
        }
        
        return dbm.selectSingleObjectWithObjId(bt.getDiskPoolId(), bucketName, objid, versionId);
    }
    
    public Metadata getObjectWithPath(String bucketName, String key, String versionId) throws ResourceNotFoundException{
        Bucket bt;
        try {
            bt = this.getBucket(bucketName);
        } catch (SQLException ex) {
            throw new ResourceNotFoundException(ex.getMessage());
        }
        return dbm.selectSingleObject(bt.getDiskPoolId(), bucketName, key, versionId);
    }
    
    public Metadata getObjectWithPath(String bucketName, String key) throws ResourceNotFoundException{
        return getObjectWithPath(bucketName, key, "null");
    }
    
    public int updateObject(String bucketName, String objid, Metadata mt, int updateCtrl){
        int ret = 0;
        
        if (updateCtrl == 1)
            ret = 0; //dbm.updateDisks(mt);
        else if (updateCtrl == 2)
            ret= dbm.updateSizeTime(mt);
        
        return ret;
    }
    
    public List<Metadata> listObjects(String bucketName, String diskid, long offset, long numObjects){
        try {
            ListObject lo = new ListObject(dbm, bucketName, diskid, offset, (int)numObjects);
            return lo.getUnformatedList();
        } catch (SQLException ex) {
            return new ArrayList();
        }
    }
    
    public List<Metadata> listObjects(String bucketName, long offset, long numObjects){
        try {
            ListObject lo = new ListObject(dbm, bucketName, "", offset, (int)numObjects);
            return lo.getUnformatedList();
        } catch (SQLException ex) {
            return new ArrayList();
        }
    }
    
    /**
     * It will allocate a replica disk for recovery of failed replica object
     * @param bucketName   bucket name
     * @param pdiskId   primary diskid
     * @param rdiskId   replica diskid
     * @return new DISK object
     * @throws ResourceNotFoundException if there is no server or disk available
     * @throws AllServiceOfflineException if all server are offline 
     *                                   or if all DISK are not Good state
     */
    public DISK allocReplicaDisk(String bucketName, Metadata mt) throws ResourceNotFoundException, AllServiceOfflineException{
       
        if (mt == null)
             throw new ResourceNotFoundException("null metadata are provided!");

        /*if (pdiskId != null && rdiskId != null){
            if (pdiskId.isEmpty() && rdiskId.isEmpty())
                throw new ResourceNotFoundException("empty diskid provided!");
        }*/
        
        //DISK rsrcDisk = null;
        String dskPoolId = obmCache.getBucketFromCache(bucketName).getDiskPoolId();
       /* DISK psrcDisk = obmCache.getDiskWithId(dskPoolId, pdiskId);
        if (rdiskId != null)
            rsrcDisk = obmCache.getDiskWithId(dskPoolId, rdiskId);*/
        return dAlloc.allocDisk(dskPoolId, mt);
    }
    
    public boolean allowedToReplicate(String bucketName, DISK primary,  DISK replica, String DstDiskId){
        String dskPoolId = obmCache.getBucketFromCache(bucketName).getDiskPoolId();
        if (dskPoolId == null)
            return false;
        
        return dAlloc.isReplicationAllowedInDisk(dskPoolId, primary, replica, DstDiskId);
    }
    /**
     * Replace replica disk with new one after recovery
     * @param bucketName  bucket name
     * @param objid       object id
     * @param versionId   the version id of the object
     * @param oldDiskid   the diskId going to be replaced
     * @param newDiskid   the new diskId
     * @return -1 if it is failed, 0 if nothing is updated or 1 if it is successful
     * @throws ResourceNotFoundException if the disk provided is not valid or exist in the system
     */
    public int replaceDisk(String bucketName, String objid, String versionId, String oldDiskid, String newDiskid) throws ResourceNotFoundException{
        String diskpoolid;
        DISK newDisk;
        boolean updatePrimary = false;
        diskpoolid = obmCache.getBucketFromCache(bucketName).getDiskPoolId();
        newDisk = obmCache.getDiskWithId(diskpoolid, newDiskid);
       
        Metadata md = dbm.selectSingleObjectWithObjId(diskpoolid, bucketName, objid, versionId);
        if (md.getPrimaryDisk().getId().equals(oldDiskid))
            updatePrimary = true;
        else if(md.isReplicaExist()){
            if (!md.getReplicaDisk().getId().equals(oldDiskid)){
                System.out.println("Replica problem");
                return -1; // invalide update
            }
        }
        else {
             System.out.println("Replica problem 2");
            return -1; // invalide update
        }
        System.out.println("call update db");
        return dbm.updateDisks(md, updatePrimary, newDisk);
    }
    
    public List<Bucket> getExistedBucketList(){
        return dbm.getBucketList();
    }
    
    public List<String> getExistedDiskList(){
        try {
            return dbm.getAllUsedDiskId();
        } catch (SQLException ex) {
            return null;
        }
    }
    
    public int addUserDiskPool(String userId, String diskPoolId, int replicaCount){
  
        try {
            DISKPOOL dp = obmCache.getDiskPoolFromCache(diskPoolId);
            if (dp == null)
                return -2;
            return dbm.insertUserDiskPool(userId, "", "", diskPoolId, replicaCount);
        } catch (ResourceNotFoundException e) {
            return -2; 
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062)
                return -17;
            System.out.println(ex);
            return -1;
        } 
    }
    
    public int removeUserDiskPool(String userId, String diskPoolId){
        try {
            return dbm.deleteUserDiskPool(userId, diskPoolId);
        } catch (SQLException ex) {
            return -1;
        }
    }
    
    public OSDClient getOSDClient(){
        return osdc;
    }
    
    public DataRepository getDBRepository(){
        return dbm;
    }
}
