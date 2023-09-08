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
    private LifeCycleManagment lfm;
    private ObjMultipart multipart;
    private RestoreObjects restoreObj;
    private BucketManager bucketMGT;
    private Objects objectMGT;
    private static Logger logger;
    
    public ObjManagerUtil(ObjManagerConfig config) throws Exception{
            
            this.config = config;
            
            omsr = ObjManagerSharedResource.getInstance(config, false);
            
            obmCache = omsr.getCache();
          
            dbm = new DataRepositoryLoader(config, obmCache).getDataRepository();
            
            osdc = new OSDClient(config);
            
            obmCache.setDBManager(dbm);
                
            dAlloc = new DiskAllocation(obmCache);
            
            bucketMGT = new BucketManager(dbm, obmCache);
        
            objectMGT = new Objects(dbm, dAlloc, obmCache, bucketMGT);
            
            lfm = new LifeCycleManagment(dbm);
            
            multipart = new ObjMultipart(dbm);
              
            restoreObj = new RestoreObjects(dbm);
            
            logger =  LoggerFactory.getLogger(ObjManagerUtil.class);
    }
    
    public ObjManagerUtil() throws Exception{
            this(new ObjManagerConfig());
    }
    
    public Bucket getBucket(String bucketName) throws ResourceNotFoundException, SQLException {
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
    
    public void updateObjectEtag(String bucketName, Metadata mt, String newEtag) throws SQLException{
        dbm.updateObjectEtag(mt, newEtag);
    }
    
    public List<Metadata> listObjects(String bucketName, String diskid, String lastObjId, long numObjects){
        try {
            ListObject lo = new ListObject(dbm, bucketName, diskid, lastObjId, (int)numObjects);
            return lo.getUnformatedList();
        } catch (SQLException ex) {
            return new ArrayList();
        }
    }
    
    public List<Metadata> listObjectsVersion(String bucketName, String diskid, String lastObjId, String lastVersionid, long numObjects){
        try {
            ListObject lo = new ListObject(dbm, bucketName, diskid, lastObjId, (int)numObjects);
            lo.updateOffset(diskid, lastObjId, lastVersionid);
            return lo.getUnformatedList();
        } catch (SQLException ex) {
            return new ArrayList();
        }
    }
    
    public List<Metadata> listObjects(String bucketName, String lastObjId, long numObjects){
        try {
            ListObject lo = new ListObject(dbm, bucketName, "", lastObjId, (int)numObjects);
            return lo.getUnformatedList();
        } catch (SQLException ex) {
            return new ArrayList();
        }
    }
    
    public long listObjectsCount(String bucketName, String diskid){
        try {
            ListObject lo = new ListObject(dbm, bucketName, diskid, "", 0);
            return lo.getUnformatedListCount();
        } catch (SQLException ex) {
            return 0;
        }
    }
    
    /**
     * It will allocate a replica disk for recovery of failed replica object
     * @param bucketName   bucket name
     * @param mt
     * @return new DISK object
     * @throws ResourceNotFoundException if there is no server or disk available
     * @throws AllServiceOfflineException if all server are offline 
     *                                   or if all DISK are not Good state
     */
    public DISK allocReplicaDisk(String bucketName, Metadata mt) throws ResourceNotFoundException, AllServiceOfflineException{
        if (mt == null)
             throw new ResourceNotFoundException("null metadata are provided!");
        
        return dAlloc.allocDisk(mt);
    }
    
    public DISK allocReplicaDisk(String bucketName, String diskPoolName, Metadata mt) throws ResourceNotFoundException, AllServiceOfflineException{
        if (mt == null)
             throw new ResourceNotFoundException("null metadata are provided!");
        
        return dAlloc.allocDisk(diskPoolName, mt);
    }
    
    public boolean allowedToReplicate(String bucketName, DISK primary,  DISK replica, String DstDiskId, boolean allowedToMoveToLocalDisk){
        
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        if (bt == null){
            System.out.format("[allowedToReplicate] buckeName : %s \n", bucketName);
            return false;
        }
        
        String dskPoolId = bt.getDiskPoolId();
        if (dskPoolId == null)
            return false;
        
        return dAlloc.isReplicationAllowedInDisk(primary, replica, DstDiskId, allowedToMoveToLocalDisk);
    }
    
    public boolean allowedToReplicate(String bucketName, DISK primary,  DISK replica, String DstDiskId){
        return allowedToReplicate(bucketName, primary,  replica, DstDiskId, false);
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
        //System.out.println("call update db");
        return dbm.updateDisks(md, updatePrimary, newDisk);
    }
    
    public List<Bucket> getExistedBucketList(){
        return getBucketList();
    }
    
    public List<Bucket> getBucketList(){
        return dbm.getBucketList();
    }
    
    public List<String> getExistedDiskList(){
        try {
            return dbm.getAllUsedDiskId();
        } catch (SQLException ex) {
            return null;
        }
    }
    
    public DISK getDISK(String diskId) throws ResourceNotFoundException{
        return obmCache.getDiskWithId(diskId);
    }
   
    public OSDClient getOSDClient(){
        return osdc;
    }
    
    public DataRepository getDBRepository(){
        return dbm;
    }
    
    public ObjManagerConfig getObjManagerConfig(){
        return config;
    }
    
    public LifeCycleManagment getLifeCycleManagmentInsatance(){
        return lfm;
    }
    
    public ObjMultipart getMultipartInsatance(String Bucket){
        multipart.setBucket(Bucket);
        return multipart;
    }
    
    public RestoreObjects getRestoreObjects(){
        return this.restoreObj;
    }
    
    public List<Metadata> listExpiredObjects(String bucketName, String prefix, String nextMarker, int maxKeys, long expiredTime) throws SQLException{
        return objectMGT.listExpiredObjects(bucketName, prefix, nextMarker, maxKeys, expiredTime);
    }
    public List<Metadata> listExpiredObjectVersions(String bucketName, String prefix, String nextMarker, String nextVersionId, int maxKeys, long expiredTime) throws SQLException{
        return objectMGT.listExpiredObjectVersions(bucketName, prefix, nextMarker, nextVersionId, maxKeys, expiredTime);
    }
    
    public List<Metadata> listDeleteMarkedObjects(String bucketName, String prefix, String nextMarker, int maxKeys) throws SQLException{
        return objectMGT.listDeleteMarkedObjects(bucketName, prefix, nextMarker, maxKeys);
    }
}
