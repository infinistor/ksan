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

import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.List;

import com.pspace.ifs.ksan.mq.MQSender;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.gw.identity.ObjectListParameter;
import com.pspace.ifs.ksan.gw.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.gw.identity.S3ObjectList;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class ObjManager {
    
    private DataRepository dbm;
    private DiskAllocation dAlloc;
    private static ObjManagerCache  obmCache;
    private ObjManagerConfig config;
    private DiskMonitor diskM;
    private MQSender mqSender;
    private OSDClient osdc;
    private static Logger logger;
   
    public ObjManager() throws Exception {
       
            logger = LoggerFactory.getLogger(ObjManager.class);

            config = new ObjManagerConfig();
            obmCache = new ObjManagerCache();
            
            if (config.dbRepository.equalsIgnoreCase("MYSQL"))
                dbm = new MysqlDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName);
            else if(config.dbRepository.equalsIgnoreCase("MONGO"))
                dbm = new MongoDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName, 27017);
            else 
                logger.debug("ObjManger initalization error :  there is no db storage configured!");

            obmCache.setDBManager(dbm);
            
            config.loadDiskPools(obmCache);
            //config.loadBucketList(obmCache);
            logger.debug(config.toString());

            dbm.loadBucketList();
            //obmCache.displayBucketList();
            
            //obmCache.displayDiskPoolList();
            dAlloc = new DiskAllocation(obmCache);
            
            diskM = new DiskMonitor(obmCache, config.mqHost, config.mqQueeuname, config.mqExchangename);
            
            mqSender = new MQSender(config.mqHost, config.mqOsdExchangename, "topic", ""); 
            
            osdc = new OSDClient(mqSender);
       
    }
   private Metadata _open(String bucket, String path, String versionId)
            throws ResourceNotFoundException{
        Metadata mt;
        Bucket bt;
        
        try {
            bt = getBucket(bucket);
        } catch (SQLException ex) {
            throw new ResourceNotFoundException("Bucket(" + bucket +") failed to reterive in the system due to :" + ex);
        }
        
        if (bt == null){
            throw new ResourceNotFoundException("Bucket(" + bucket +") not exist in the system!");
        }
        
        if (versionId == null)
            mt = dbm.selectSingleObject(bt.getDiskPoolId(), bucket, path);
        else
            mt = dbm.selectSingleObject(bt.getDiskPoolId(), bucket, path, versionId);
       
        mt.setReplicaCount(bt.getReplicaCount());
        
        return mt;
    }
    /**
     * Return a new primary and replica disk mount path allocated for the path provided.Unless the close method is called, the allocated disk mount path is not stored permanently. 
     * @param bucket   bucket name
     * @param path     the path or key of the metadata is going to be opened
     * @return  metadata associated with the object requested 
     * @throws ResourceNotFoundException 
     */

    public Metadata open(String bucket, String path) 
            throws ResourceNotFoundException{
        return _open(bucket, path, null);
    }

    /**
     * Return a new primary and replica disk mount path allocated for the path provided.Unless the close method is called, the allocated disk mount path is not stored permanently. 
     * @param bucket   bucket name
     * @param path     the path or key of the metadata is going to be opened
     * @param versionId specific versionId
     * @return  metadata associated with the object requested 
     * @throws ResourceNotFoundException 
     */
    public Metadata open(String bucket, String path, String versionId) 
            throws ResourceNotFoundException{
        
        return _open(bucket, path, versionId);
    }
    
    /**
     * Return a new primary and replica disk mount path allocated for the path provided.
     * Unless the close method is called, the allocated disk mount path is not stored permanently. 
     * @param bucket bucket name
     * @param path     the path or key of the metadata is going to be created
     * @param algorithm the algorithm used to allocate osd disk
     * @return  return the metadata object with allocated primary and replica disk 
     * @throws IOException 
     */
    
    private Metadata create(String bucketName, String path, int algorithm)
            throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        Bucket bt;
        
        logger.debug("Begin bucketName : {} key : {} alg : {} ", bucketName, path, algorithm == AllocAlgorithm.LOCALPRIMARY? "LOCAL" : "ROUNDROBIN");
        if (dAlloc == null){
            throw new ResourceNotFoundException("No disk to allocate!");
        }

        // create meta
        try { 
            bt = getBucket(bucketName);
        } catch (SQLException ex) {
            throw new ResourceNotFoundException("Bucket(" + bucketName +") not found due to " + ex);
        }
        
        logger.debug(" bucket : {} bucketId : {} diskPoolId : {} ", bt.getName(), bt.getId(), bt.getDiskPoolId());
        Metadata mt = new Metadata(bucketName, path);

        // allocate disk
        dAlloc.allocDisk(mt, bt.getDiskPoolId(), bt.getReplicaCount(), algorithm); // FIXME replace bucket id
        
        logger.debug("End bucketName : {} key : {} pdiks : {} rdisk : {}", bucketName, path, 
                mt.getPrimaryDisk().getPath(), mt.isReplicaExist() ? mt.getReplicaDisk().getPath() : "");
        return mt;
    }
    
 
    /**
     * Return a new primary and replica disk mount path allocated for the path provided with round robin algorithm.
     * @param bucket bucket name
     * @param path the path or key of the metadata is going to be created
     * @return a basics of metadata object with allocated disk information 
     * @throws IOException 
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     */
    public Metadata create(String bucket, String path)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return create(bucket, path, AllocAlgorithm.ROUNDROBIN);
    }
    
    /**
     * Return a new primary and replica disk mount path allocated for the path provided with primary object at local osd algorithm.
     * @param bucket bucket name 
     * @param path the path or key of the metadata is going to be created
     * @return a basics of metadata object with allocated disk information with primary on local osd 
     * @throws IOException 
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     */
    public Metadata createLocal(String bucket, String path)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return create(bucket, path, AllocAlgorithm.LOCALPRIMARY);
    }
    
    /**
     * Return the same primary and replica disk to copy the the object and reside in the same osd.
     * @param bucket bucket name 
     * @param toBucket bucket name of destination
     * @param from  key of the source metadata
     * @param to  key of the destination metadata
     * @param versionId  version id of the source object
     * @return a basics of metadata object with allocated disk information 
     * @throws IOException 
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     */
    public Metadata createCopy(String bucket, String from, String versionId, String toBucket, String to) throws IOException, AllServiceOfflineException, ResourceNotFoundException, SQLException{
        Metadata mt;
        Metadata cpy_mt;
        
        Bucket bt = getBucket(bucket);
        mt = dbm.selectSingleObject(bt.getDiskPoolId(), bucket, from, versionId);
        if (mt == null)
            throw new ResourceNotFoundException("Bucket(" + bucket +")  and key("+ from +") not exist!");
        
        cpy_mt = new Metadata(toBucket, to);
        cpy_mt.setPrimaryDisk(mt.getPrimaryDisk());
        cpy_mt.setReplicaCount(bt.getReplicaCount());
        if (mt.isReplicaExist())
            cpy_mt.setReplicaDISK(mt.getReplicaDisk());
        return cpy_mt;
    }
   
    private int removeObject(String bucket, String path, String  versionId){
             
        try {
            Metadata mt;
            Bucket bt = getBucket(bucket);
            mt = dbm.selectSingleObject(bt.getDiskPoolId(), bucket, path, versionId);
            // remove from DB or mark
            dbm.deleteObject(bucket, path, versionId);
            //osdc.removeObject(mt);
            return 0;
        } catch (ResourceNotFoundException ex) {
            logger.debug(ex.getMessage());
        }
        catch (Exception ex) {
            logger.debug(ex.getMessage());
        }
        
        return -1;
    }
    /**
     * Remove the metadata associated with path if it existed 
     * @param bucket bucket name
     * @param path  path or key of the metadata is going to be removed
     * @return 
     */
    public int remove(String bucket, String path) {
        return removeObject(bucket, path, "null"); 
    }

    /**
     * Remove the metadata associated with path if it existed 
     * @param bucket bucket name
     * @param path  path or key of the metadata is going to be removed
     * @param versionId the version Id
     * @return 
     */
    public int remove(String bucket, String path, String  versionId){
        return removeObject(bucket, path, versionId); 
    }

    /**
     * Store the metadata for the file associated with path
     * @param bucketName
     * @param path path or key of the metadata is going to be stored
     * @param etag
     * @param meta
     * @param tag
     * @param pdskPath primary disk path 
     * @param rdskPath replica disk path
     * @return
     * @throws InvalidParameterException 
     * @throws ResourceNotFoundException 
     */
    public int close(String bucketName, String path, String etag, String meta, String tag, long size, String acl, String pdskPath, String rdskPath, String versionId, String deleteMarker) 
            throws InvalidParameterException, ResourceNotFoundException{
        Metadata mt;
        Bucket bt;
        DISK pdsk;
        DISK rdsk;
        boolean isreplicaExist;
        
        mt = new Metadata(bucketName, path);
        mt.set(etag, tag, meta, acl, size);
        mt.setVersionId(versionId, deleteMarker, true);
        
        try {
            bt = this.getBucket(bucketName);
        } catch (SQLException ex) {
            throw new ResourceNotFoundException("Bucket(" + bucketName +") not exist ! >" + ex);
        }
        
        if (bt == null)
           throw new ResourceNotFoundException("Bucket(" + bucketName +") not exist!");
        
        pdsk = obmCache.getDiskWithPath(bt.getDiskPoolId(), pdskPath);
        isreplicaExist = false;
        mt.setPrimaryDisk(pdsk);
        if (rdskPath != null){
            if (!rdskPath.isEmpty()){
                rdsk = obmCache.getDiskWithPath(bt.getDiskPoolId(), rdskPath);
                isreplicaExist = true;
                mt.setReplicaDISK(rdsk);
            } 
        }
     
        mt.setReplicaCount(bt.getReplicaCount());
        return this.close(bucketName, path, mt);
    }
    
    public int close(String bucketName, String path, Metadata mt) throws ResourceNotFoundException{
        Bucket bt;
        
        try {
            bt = getBucket(bucketName);
        } catch (SQLException ex) {
           throw new ResourceNotFoundException("Bucket(" + bucketName +") not exist ! >" + ex); 
        }
        
        if (bt == null)
           throw new ResourceNotFoundException("Bucket(" + bucketName +") not exist!");
        try{
            Metadata mtd = dbm.selectSingleObject(bt.getDiskPoolId(), bucketName, path);
            if (!bt.getVersioning().equalsIgnoreCase("Enabled"))
                dbm.deleteObject(bucketName, path, "null");
        } catch(ResourceNotFoundException ex){
            
        }
        
        if (!obmCache.isDiskSeparatedAndValid(bt.getDiskPoolId(), mt)){
            String err = String.format("Bucket : %s key : %s disk information pdiskid : %s rdiskid : %s is not separated or not exist in the syystem",
                    bucketName, path, mt.getPrimaryDisk().getId(), mt.isReplicaExist()? mt.getReplicaDisk().getId()  : "");
            logger.debug(err);
            throw new InvalidParameterException(err);
        }
        //System.out.format("[close ] bucket : %s path : %s objid : %s\n", mt.getBucket(), mt.getPath(), mt.getObjId());
        
        mt.updateLastmodified(); // to update last modified time to now
        return dbm.insertObject(mt); 
    }
    
    /**
     * Create bucket if it is not exist
     * @param bucketName bucket name
     * @param userId the user uniquely identifying name
     * @param acl    
     * @return 0 for success or negative number for error     
     * @throws ResourceAlreadyExistException     
     * @throws ResourceNotFoundException     
     */
    
    public int createBucket(String bucketName, String userName, String userId, String acl, String  encryption, String objectlock) throws ResourceAlreadyExistException, ResourceNotFoundException{
       Bucket bt;
        
        try {
            bt = getBucket(bucketName);
            if (bt != null)
                throw new ResourceAlreadyExistException("Bucket (" +bucketName + ") already exist in the system!");
            
            bt = dbm.selectBucket(bucketName);
            if (bt != null){
                obmCache.setBucketInCache(bt);
                throw new ResourceAlreadyExistException("Bucket (" +bucketName + ") already exist in the system!");
            }
        } catch (SQLException | ResourceNotFoundException ex) {
            bt = new Bucket();
            bt.setName(bucketName);
            bt.setUserId(userId);
            bt.setAcl(acl);
            bt.setEncryption(encryption);
            bt.setObjectLock(objectlock);
            
            try {
                bt = dbm.getUserDiskPool(bt);
            } catch (SQLException ex1) {
                throw new ResourceNotFoundException("User("+ userId +") not assocated with diskpool. Please create user to diskpool assocation first!");
            }
            bt = dbm.insertBucket(bt);
            obmCache.setBucketInCache(bt);
        }
        return 0;
    }
    
    /**
     * Remove bucket if it is exist
     * @param bucketName bucket name
     * @return 0 for success or negative number for error     
     */
    public int removeBucket(String bucketName){
        dbm.deleteBucket(bucketName);
        obmCache.removeBucketFromCache(bucketName);
        return 0;
    }
    
    /**
     * List all bucket exist in the system
     * @return list of bucket names or null if no bucket exist     
     */
    public String[] listBucket(){
        return obmCache.getBucketNameList();
    }

    /**
     * List all bucket exist in the system
     * @return list of bucket names or null if no bucket exist     
     */
    public List<S3BucketSimpleInfo> listBucketSimpleInfo(String userName, String userId) {
        return obmCache.getBucketSimpleList();
    }

    /**
     * Check existence of a bucket 
     * @param bucketName bucket name
     * @return true for existence or false for absent     
     */
    public boolean isBucketExist(String bucketName){
        boolean isExist = obmCache.bucketExist(bucketName);
        if (isExist == false){
            try {
                Bucket bt = dbm.selectBucket(bucketName);
                obmCache.setBucketInCache(bt);
            } catch (ResourceNotFoundException | SQLException ex) {
                return false;
            }
            return true;
        }
        return isExist;
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
        Bucket bt;
        try {
            bt = getBucket(bucketName);
        } catch (SQLException ex) {
            throw new ResourceNotFoundException("unable to get buckt " + bucketName + " in the system!");
        }
        
        String dskPoolId = bt.getDiskPoolId();
        return dAlloc.allocDisk(dskPoolId, primary, null);
    }
    
    public int putBucketVersioning(String bucketName, String versionState) throws ResourceNotFoundException, SQLException{
        int ret;
        Bucket bt = dbm.selectBucket(bucketName);
        bt.setVersioning(versionState, "");
        ret = dbm.updateBucketVersioning(bt);
        obmCache.updateBucketInCache(bt);
        return ret;
    }
    
    public String getBucketVersioning(String bucketName) throws ResourceNotFoundException, SQLException{
        Bucket bt = dbm.selectBucket(bucketName);
        return bt.getVersioning();
    }
    
    public Metadata getObjectWithPath(String bucketName, String key) throws ResourceNotFoundException, SQLException{
        return _open(bucketName, key, null); 
    }

    public Metadata getObjectWithVersionId(String bucketName, String key, String versionId) throws ResourceNotFoundException, SQLException{
        return _open(bucketName, key, versionId);
    }

    public Bucket getBucket(String bucketName) throws ResourceNotFoundException, SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        if (bt == null){
            bt = dbm.selectBucket(bucketName);
            if (bt != null)
              obmCache.setBucketInCache(bt);
        }
        
        return bt;
    }

    public void updateObjectMeta(Metadata mt) throws SQLException {
        dbm.updateObjectMeta(mt);
    }
    
    public void updateObjectTagging(Metadata mt) throws SQLException {
        dbm.updateObjectTagging(mt);
    }

    public void updateObjectAcl(Metadata mt) throws SQLException {
        dbm.updateObjectAcl(mt);
    }

    public void updateBucketAcl(String bucketName, String acl) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setAcl(acl);
        dbm.updateBucketAcl(bt);
        obmCache.updateBucketInCache(bt);
    }

    public void updateBucketCors(String bucketName, String cors) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setCors(cors);
        dbm.updateBucketCors(bt);
        obmCache.updateBucketInCache(bt);
    }
   
    public void updateBucketEncryption(String bucketName, String encryption) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setEncryption(encryption);
        dbm.updateBucketEncryption(bt);
        obmCache.updateBucketInCache(bt);
    }
    
    public void updateBucketWeb(String bucketName, String web) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setWeb(web);
        dbm.updateBucketWeb(bt);
        obmCache.updateBucketInCache(bt);
    }

    public void updateBucketLifecycle(String bucketName, String lifecycle) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setLifecycle(lifecycle);
        dbm.updateBucketLifecycle(bt);
        obmCache.updateBucketInCache(bt);
    }

    public void updateBucketAccess(String bucketName, String access) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setAccess(access);
        dbm.updateBucketAccess(bt);
        obmCache.updateBucketInCache(bt);
    }

    public void updateBucketTagging(String bucketName, String tagging) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setTagging(tagging);
        dbm.updateBucketTagging(bt);
        obmCache.updateBucketInCache(bt);
    }

    public void updateBucketReplication(String bucketName, String replicationXml) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setReplication(replicationXml);
        dbm.updateBucketReplication(bt);
        obmCache.updateBucketInCache(bt);
    }

    public void updateBucketObjectLock(String bucketName, String lock) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setObjectLock(lock);
        dbm.updateBucketObjectLock(bt);
        obmCache.updateBucketInCache(bt);
    }

    public void updateBucketPolicy(String bucketName, String policy) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        bt.setPolicy(policy);
        dbm.updateBucketPolicy(bt);
        obmCache.updateBucketInCache(bt);
    }

    public void updateBucketUsed(String bucketName, long size) throws SQLException, ResourceNotFoundException {
        Bucket bt = getBucket(bucketName);
        dbm.updateBucketUsedSpace(bt, size);
        bt.setUsedSpace(bt.getUsedSpace() + size);
        obmCache.updateBucketInCache(bt);
    }

    public boolean isBucketDelete(String bucketName) throws SQLException {
        return dbm.isBucketDeleted(bucketName);
    }
    
    private static int parseMaxKeys(String maxKeysStr){
        int maxKeys;
        try{
            maxKeys = Integer.parseInt(maxKeysStr);
        } catch(NumberFormatException ex){
            maxKeys = 0;
        }
        return maxKeys;
    }
    
    public ObjectListParameter listObject(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
       
        ListObject list = new ListObject(dbm, bucketName, s3ObjectList.getDelimiter(), s3ObjectList.getMarker(), parseMaxKeys(s3ObjectList.getMaxKeys()), s3ObjectList.getPrefix());
        return list.getList();
    }

    public ObjectListParameter listObjectV2(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        ListObject list = new ListObject(dbm, bucketName, s3ObjectList.getDelimiter(), s3ObjectList.getStartAfter(), s3ObjectList.getContinuationToken(), parseMaxKeys(s3ObjectList.getMaxKeys()), s3ObjectList.getPrefix());
        return list.getList();
    }

    public ObjectListParameter listObjectVersions(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        ListObject list = new ListObject(dbm, bucketName, s3ObjectList.getDelimiter(), s3ObjectList.getKeyMarker(), s3ObjectList.getVersionIdMarker(), parseMaxKeys(s3ObjectList.getMaxKeys()), s3ObjectList.getPrefix(), true);
        return list.getList();
    }

    public List<Metadata> listObject(String bucketName, String delimiter, String keyMarker, String versionIdMarker, String continuationToken, int maxKeys, String prefix) throws SQLException{
        ListObject list = new ListObject(dbm, bucketName, delimiter, keyMarker, versionIdMarker, continuationToken, maxKeys, prefix);
        return list.getUnformatedList();
    }
    
    // for pool
    public boolean isValid(){
        return true;
    }
    
    public void close() throws SQLException {}
    
    public void activate(){}
    
    public void deactivate(){}

    public void updateConfig(){}

    public void updateDiskpools(){}
}