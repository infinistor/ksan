/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.ObjManger;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pspace.ifs.KSAN.ObjManger.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.KSAN.ObjManger.ObjManagerException.ResourceNotFoundException;
import java.util.ArrayList;

/**
 *
 * @author legesse
 */
public class ObjManagerUtil {
    private DataRepository dbm;
    private DiskAllocation dAlloc;
    private ObjManagerCache  obmCache;
    private ObjManagerConfig config;
    //private DiskMonitor diskM;
    //private MQSender mqSender;
    private OMLogger logger;
    
    public ObjManagerUtil() throws Exception{
            
            config = new ObjManagerConfig();
            
            obmCache = new ObjManagerCache();
          
            //System.out.println(">>dbmCache : " + obmCache);
            if (config.dbRepository.equalsIgnoreCase("MYSQL"))
                 dbm = new MysqlDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName);
            else if(config.dbRepository.equalsIgnoreCase("MONGO"))
                 dbm = new MongoDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName, 27017);
            else 
                System.out.println("ObjManger initalization error :  there is no db storage configured!");
            
            //System.out.println(">>dbm : " + dbm);
            config.loadDiskPools(obmCache);
           
            dbm.loadBucketList();
            
            //obmCache.displayBucketList();
            
            //obmCache.displayDiskPoolList();
            
            dAlloc = new DiskAllocation(obmCache);
            
            logger = new OMLogger(ObjManager.class.getName());
    }
    
    private Bucket getBucket(String bucketName) throws ResourceNotFoundException, SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        if (bt == null)
            bt = dbm.selectBucket(bucketName);
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
    
    public Metadata getObjectWithPath(String bucketName, String key) throws ResourceNotFoundException{
        Bucket bt;
        try {
            bt = this.getBucket(bucketName);
        } catch (SQLException ex) {
            throw new ResourceNotFoundException(ex.getMessage());
        }
        return dbm.selectSingleObject(bt.getDiskPoolId(), bucketName, key);
    }
    
    public int updateObject(String bucketName, String objid, Metadata mt, int updateCtrl){
        int ret = 0;
        
        if (updateCtrl == 1)
            ret = dbm.updateDisks(mt);
        else if (updateCtrl == 2)
            ret= dbm.updateSizeTime(mt);
        
        return ret;
    }
    
    public List<Metadata> listObjects(String bucketName, String diskid, int listCtrl, long offset, long numObjects){
        try {
            ListObject lo = new ListObject(dbm, bucketName, diskid, offset, (int)numObjects);
            return lo.getUnformatedList();
        } catch (SQLException ex) {
            return new ArrayList();
        }
    }
    /**
     * It will allocate a replica disk for recovery of failed replica object
     * @param bucketName   bucket name
     * @param dpath        primary disk path 
     * @param diskid       primary disk disk Id number
     * @return new DISK object
     * @throws ResourceNotFoundException if there is no server or disk available
     * @throws AllServiceOfflineException if all server are offline 
     *                                   or if all DISK are not Good state
     */
    public DISK allocReplicaDisk(String bucketName, String dpath, String diskid) throws ResourceNotFoundException, AllServiceOfflineException{
        DISK primary = new DISK();
        
        if (dpath == null && diskid == null)
            return null;

        if (dpath != null && diskid != null){
            if (dpath.isEmpty() && diskid.isEmpty())
                return null;
        }
        
        primary.setPath(dpath);
        primary.setId(diskid);
        String dskPoolId = obmCache.getBucketFromCache(bucketName).getDiskPoolId();
        return dAlloc.allocDisk(dskPoolId, primary);
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
     * @param pdiskid     primary disk id
     * @param rdiskid     new replica disk id
     * @return -1 if it is failed, 0 if nothing is updated or 1 if it is successful
     * @throws ResourceNotFoundException if the disk provided is not valid or exist in the system
     */
    public int replaceDisk(String bucketName, String objid, String pdiskid, String rdiskid) throws ResourceNotFoundException{
        String diskpoolid;
        DISK primary;
        DISK replica;
        diskpoolid = obmCache.getBucketFromCache(bucketName).getDiskPoolId();
        primary = obmCache.getDiskWithId(diskpoolid, pdiskid);
        replica = obmCache.getDiskWithId(diskpoolid, rdiskid);
        System.out.println(" PRIMARY > " + pdiskid + ", " + primary + " REPLICA >" + rdiskid + " ," + replica);
        Metadata md = new Metadata();
        md.setObjid(objid);
        md.setPrimaryDisk(primary);
        md.setReplicaDISK(replica);
        return dbm.updateDisks(md);
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
}
