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
import java.sql.SQLException;
import java.util.List;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;
import java.util.Map;

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
    //private OSDClient osdc;
    private ObjManagerSharedResource obmsr;
    private ObjMultipart multipart;
    private BucketManager bucketMGT;
    private Objects objectMGT;
    private ObjTagsIndexing objectIndexing;
    private RestoreObjects restoreObj;
    
    private static Logger logger;
   
    private void init(ObjManagerConfig config) throws Exception {
        logger = LoggerFactory.getLogger(ObjManager.class);

        this.config = config;

        obmsr = ObjManagerSharedResource.getInstance(config, true);

        obmCache = obmsr.getCache();

        dbm = new DataRepositoryLoader(config, obmCache).getDataRepository();

        logger.debug(config.toString());

        dAlloc = new DiskAllocation(obmCache);

        //osdc = new OSDClient(config);
        
        multipart = new ObjMultipart(dbm);
        
        bucketMGT = new BucketManager(dbm, obmCache);
        
        objectMGT = new Objects(dbm, dAlloc, obmCache, bucketMGT);
        
        objectIndexing = new ObjTagsIndexing(bucketMGT, objectMGT);
        
        restoreObj = new RestoreObjects(dbm);
    }
    
    public ObjManager() throws Exception {
        init(new ObjManagerConfig());
    }
    
    public ObjManager(ObjManagerConfig config) throws Exception {
        init(config);
    }
  
    /**
     * Return a new primary and replica disk mount path allocated for the path provided.Unless the close method is called, the allocated disk mount path is not stored permanently. 
     * @param bucketName   bucket name
     * @param key          the key name of the object
     * @return    metadata associated with the object requested 
     * @throws ResourceNotFoundException 
     */

    public Metadata open(String bucketName, String key) 
            throws ResourceNotFoundException{
        return objectMGT.open(bucketName, key, null);
    }

    /**
     * Return a new primary and replica disk mount path allocated for the path provided.Unless the close method is called, the allocated disk mount path is not stored permanently. 
     * @param bucketName   bucket name
     * @param key          key name of the object 
     * @param versionId    the version Id of the object
     * @return  metadata associated with the object requested 
     * @throws ResourceNotFoundException 
     */
    public Metadata open(String bucketName, String key, String versionId) 
            throws ResourceNotFoundException{
        return objectMGT.open(bucketName, key, versionId);
    }
   
    public Metadata getObjectWithVersionId(String bucketName, String key, String versionId) throws ResourceNotFoundException, SQLException{
        return open(bucketName, key, versionId);
    } 
        
    /**
     * creates a metadata of an object with disk information allocated with round robin algorithm.
     * @param bucketName  bucket name
     * @param key         the key name of the object
     * @return metadata of an object with allocated disk information 
     * @throws IOException 
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     */
    public Metadata create(String bucketName, String key)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return objectMGT.create(null, bucketName, key, "null");
    }
    
    /**
     *  creates a metadata of an object with disk information allocated with round robin algorithm.
     * @param bucketName  bucket name
     * @param key         the key name of the object
     * @param versionId   the version of the object
     * @return a basics of metadata object with allocated disk information 
     * @throws IOException 
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     */
    public Metadata create(String bucketName, String key, String versionId)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return objectMGT.create(null, bucketName, key, versionId);
    }
    
    /**
     * creates a metadata of an object with disk information allocated with round robin algorithm.
     * @param diskpoolId  disk pool Id number
     * @param bucketName  bucket name
     * @param key         the key name of an object
     * @param versionId   the versionId of the object
     * @return a basics of metadata object with allocated disk information 
     * @throws IOException 
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     */
    public Metadata create(String diskpoolId, String bucketName, String key, String versionId)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return objectMGT.create(diskpoolId, bucketName, key, versionId);
    }
    
    /**
     * creates a metadata of an object with disk information allocated primary disk to local disk if it exist and otherwise both primary and replica with round robin algorithm.
     * @param bucketName  bucket name 
     * @param key         the key name of an object
     * @return a basics of metadata object with allocated disk information 
     * @throws IOException 
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     */
    public Metadata createLocal(String bucketName, String key)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return objectMGT.createLocal(null, bucketName, key, "null");
    }
    
    /**
     * creates a metadata of an object with disk information allocated primary disk to local disk if it exist and otherwise both primary and replica with round robin algorithm.
     * @param bucketName  bucket name 
     * @param key         the key name of an object
     * @param versionId the versionId of the object
     * @return a basics of metadata object with allocated disk information  
     * @throws IOException , AllServiceOfflineException, ResourceNotFoundException
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     */
    public Metadata createLocal(String bucketName, String key, String versionId)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return objectMGT.createLocal(null, bucketName, key, versionId);
    }
    /**
     * creates a metadata of an object with disk information allocated primary disk to local disk if it exist and otherwise both primary and replica with round robin algorithm.
     * @param diskPoolId  the diskpoolId used to create object
     * @param bucketName  bucket name 
     * @param key         the key name of an object
     * @param versionId   the versionId of the object
     * @return a basics of metadata object with allocated disk information with primary on local osd 
     * @throws IOException , AllServiceOfflineException, ResourceNotFoundException
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     */
    public Metadata createLocal(String diskPoolId, String bucketName, String key, String versionId)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return objectMGT.createLocal(diskPoolId, bucketName, key, versionId);
    }
    
    /**
     * Return the same primary and replica disk to copy the the object and reside in the same osd.
     * @param bucketName    bucket name 
     * @param toBucketName  bucket name of destination
     * @param from          key name of the source metadata
     * @param to            key name of the destination metadata
     * @param versionId  version id of the source object
     * @return a basics of metadata object with allocated disk information 
     * @throws IOException 
     * @throws AllServiceOfflineException 
     * @throws ResourceNotFoundException 
     * @throws java.sql.SQLException 
     */
    public Metadata createCopy(String bucketName, String from, String versionId, String toBucketName, String to) throws IOException, AllServiceOfflineException, ResourceNotFoundException, SQLException{
        return objectMGT.createCopy(bucketName, from, versionId, toBucketName, to);
    }
   
    /**
     * Remove the metadata associated with the key if it existed 
     * @param bucketName  bucket name
     * @param key         the key name of an object
     * @return 
     */
    public int remove(String bucketName, String key){
        return objectMGT.remove(bucketName, key, "null"); 
    }

    /**
     * Remove the metadata associated with key if it existed 
     * @param bucketName  bucket name
     * @param key         key name of an object
     * @param versionId   the version Id
     * @return 
     */
    public int remove(String bucketName, String key, String  versionId){
        return objectMGT.remove(bucketName, key, versionId);  
    }
    
    public int close(String bucketName, String key, Metadata mt) throws ResourceNotFoundException, SQLException{
        return objectMGT.close(bucketName, key, mt);
    }
    
    public void updateObjectMeta(Metadata mt) throws SQLException {
        objectMGT.updateObjectMeta(mt);
    }
    
    public void updateObjectTagging(Metadata mt) throws SQLException {
        objectMGT.updateObjectTagging(mt);
    }

    public void updateObjectAcl(Metadata mt) throws SQLException {
        objectMGT.updateObjectAcl(mt);
    }
    
    public ObjectListParameter listObject(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        return objectMGT.listObject(bucketName, s3ObjectList);
    }

    public ObjectListParameter listObjectV2(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        return objectMGT.listObjectV2(bucketName, s3ObjectList);
    }

    public ObjectListParameter listObjectVersions(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        return objectMGT.listObjectVersions(bucketName, s3ObjectList);
    }
    
    /**
     * Create bucket if it is not exist
     * @param bt a Bucket class instance with basic information        
     * @return  0 when the operation is successful        
     * @throws ResourceAlreadyExistException     
     * @throws java.sql.SQLException     
     * @throws ResourceNotFoundException     
     */
    public int createBucket(Bucket bt) throws ResourceAlreadyExistException, SQLException, ResourceNotFoundException{
        return bucketMGT.createBucket(bt);
    }
    
    /**
     * Create bucket if it is not exist
     * @param bucketName bucket name
     * @param userName
     * @param userId the user uniquely identifying name
     * @param acl    
     * @param encryption    
     * @param objectlock    
     * @return 0 for success or negative number for error     
     * @throws ResourceAlreadyExistException     
     * @throws ResourceNotFoundException     
     */
    public int createBucket(String bucketName, String userName, String userId, String acl, String  encryption, String objectlock) throws ResourceAlreadyExistException, ResourceNotFoundException{
        return bucketMGT.createBucket(bucketName, userName, userId, acl, encryption, objectlock);
    }
    
    /**
     * Remove bucket if it is exist
     * @param bucketName bucket name
     * @return 0 for success or negative number for error     
     */
    public int removeBucket(String bucketName){
        return bucketMGT.removeBucket(bucketName);
    }
    
    /**
     * List all bucket exist in the system
     * @return list of bucket names or null if no bucket exist     
     */
    public String[] listBucket(){
        return bucketMGT.listBucket();
    }

    /**
     * List all bucket exist in the system
     * @return list of bucket names or null if no bucket exist     
     */
    public List<S3BucketSimpleInfo> listBucketSimpleInfo(String userName, String userId) {
        return bucketMGT.listBucketSimpleInfo(userName, userId);
    }

    /**
     * Check existence of a bucket 
     * @param bucketName bucket name
     * @return true for existence or false for absent     
     */
    public boolean isBucketExist(String bucketName){
        return bucketMGT.isBucketExist(bucketName);
    }
       
    public int putBucketVersioning(String bucketName, String versionState) throws ResourceNotFoundException, SQLException{
        return bucketMGT.putBucketVersioning(bucketName, versionState);
    }
    
    public String getBucketVersioning(String bucketName) throws ResourceNotFoundException, SQLException{
        return bucketMGT.getBucketVersioning(bucketName);
    }
    
    public Bucket getBucket(String bucketName) throws ResourceNotFoundException, SQLException {
        return bucketMGT.getBucket(bucketName);
    }

    public void updateBucketAcl(String bucketName, String acl) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketAcl(bucketName, acl);
    }

    public void updateBucketCors(String bucketName, String cors) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketCors(bucketName, cors);
    }
   
    public void updateBucketEncryption(String bucketName, String encryption) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketEncryption(bucketName, encryption);
    }
    
    public void updateBucketWeb(String bucketName, String web) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketWeb(bucketName, web);
    }

    public void updateBucketLifecycle(String bucketName, String lifecycle) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketLifecycle(bucketName, lifecycle);
    }

    public void updateBucketAccess(String bucketName, String access) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketAccess(bucketName, access);
    }

    public void updateBucketTagging(String bucketName, String tagging) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketTagging(bucketName, tagging);
    }

    public void updateBucketReplication(String bucketName, String replicationXml) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketReplication(bucketName, replicationXml);
    }

    public void updateBucketObjectLock(String bucketName, String lock) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketObjectLock(bucketName, lock);
    }

    public void updateBucketPolicy(String bucketName, String policy) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketPolicy(bucketName, policy);
    }

    public void updateBucketUsed(String bucketName, long size) throws SQLException, ResourceNotFoundException {
        bucketMGT.updateBucketUsed(bucketName, size);
    }

    public boolean isBucketDelete(String bucketName) throws SQLException {
        return bucketMGT.isBucketDeleted(bucketName);
    }
    
    public void updateBucketLogging(String bucketName, String logging) throws ResourceNotFoundException, SQLException{
        bucketMGT.updateBucketLogging(bucketName, logging);
    }
    
    public ObjMultipart getMultipartInsatance(String Bucket){
        multipart.setBucket(Bucket);
        return multipart;
    }
    
    public void updateDiskpools(String routingKey, String body){
        this.obmsr.getDiskMonitor().update(routingKey, body);
    }
    
    public ObjTagsIndexing getObjectTagsIndexing(){
        return this.objectIndexing;
    }
    
    public RestoreObjects getRestoreObjects(){
        return this.restoreObj;
    }
    
    public void updateBucketAnalyticsConfiguration(String bucketName, String analytics) throws ResourceNotFoundException, SQLException{
        bucketMGT.updateBucketAnalyticsConfiguration(bucketName, analytics);
    }
    
    public void updateBucketAccelerateConfiguration(String bucketName, String accelerate) throws ResourceNotFoundException, SQLException{
        bucketMGT.updateBucketAccelerateConfiguration(bucketName, accelerate);
    }
    
    public void updateBucketPayment(String bucketName, String payment) throws ResourceNotFoundException, SQLException{
        bucketMGT.updateBucketPayment(bucketName, payment);
    }
    
    public List<Map<String, String>> listBucketAnalyticsConfiguration() {
        return bucketMGT.listBucketAnalyticsConfiguration();
    }
    
    // for pool
    public boolean isValid(){ return true;}
    
    public void close() throws SQLException {}
    
    public void activate(){}
    
    public void deactivate(){}
   
}
