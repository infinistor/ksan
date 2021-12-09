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

import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.List;

import com.pspace.ifs.ksan.gw.identity.ObjectListParameter;
import com.pspace.ifs.ksan.gw.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.gw.identity.S3ObjectList;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.mq.MQSender;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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
    // private OMLogger logger;
    private static Logger logger;
   
    // public static ObjManager getInstance() {
    //     return LazyHolder.INSTANCE;
    // }

    // private static class LazyHolder {
    //     private static ObjManager INSTANCE = new ObjManager();
    // }

    public ObjManager() {
        try {
            logger = LoggerFactory.getLogger(ObjManager.class);

            config = new ObjManagerConfig();
            obmCache = new ObjManagerCache();
            //dbm = new MysqlDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName);
            if (config.dbRepository.equalsIgnoreCase("MYSQL"))
                 dbm = new MysqlDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName);
            else if(config.dbRepository.equalsIgnoreCase("MONGO"))
                 dbm = new MongoDataRepository(obmCache, config.dbHost, config.dbUsername, config.dbPassword, config.dbName, 27017);
            else 
                logger.debug("ObjManger initalization error :  there is no db storage configured!");

            config.loadDiskPools(obmCache);
            //config.loadBucketList(obmCache);
            logger.debug(config.toString());

            dbm.loadBucketList();
            obmCache.displayBucketList();
            
            obmCache.displayDiskPoolList();
            dAlloc = new DiskAllocation(obmCache);
            diskM = new DiskMonitor(obmCache, config.mqHost, config.mqQueeuname, config.mqExchangename);
            // logger = new OMLogger(ObjManager.class.getName());
            mqSender = new MQSender(config.mqHost, config.mqOsdExchangename, "topic", ""); 
        } catch(Exception e){
            logger.error("ObjManger initalization error : " + e);
        }
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
        Metadata mt;
        
        mt = dbm.selectSingleObject(bucket, path);
       
        return mt;
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
        Metadata mt;
        
        mt = dbm.selectSingleObject(bucket, path, versionId);
       
        return mt;
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
        
        logger.debug("Begin bucketName : {} key : {} alg : {} ", bucketName, path, algorithm == AllocAlgorithm.LOCALPRIMARY? "LOCAL" : "ROUNDROBIN");
        if (dAlloc == null){
            throw new ResourceNotFoundException("No disk to allocate!");
        }

        // create meta
        Bucket bt = this.obmCache.getBucketFromCache(bucketName);

        if (bt == null){
            throw new ResourceNotFoundException("Bucket(" + bucketName +") not exist!");
        }

        logger.debug(" bucket : {} bucketId : {} diskPoolId : {} ", bt.getName(), bt.getId(), bt.getDiskPoolId());
        Metadata mt = new Metadata(bucketName, path);

        // allocate disk
        dAlloc.allocDisk(mt, bt.getDiskPoolId(), algorithm); // FIXME replace bucket id
        
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
     * @throws com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException 
     * @throws com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException 
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
     * @throws com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException 
     * @throws com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException 
     */
    public Metadata createCopy(String bucket, String from, String versionId, String toBucket, String to) throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        Metadata mt;
        Metadata cpy_mt;
        
        mt = dbm.selectSingleObject(bucket, from, versionId);
        if (mt == null)
            throw new ResourceNotFoundException("Bucket(" + bucket +")  and key("+ from +") not exist!");
        
        cpy_mt = new Metadata(toBucket, to);
        cpy_mt.setPrimaryDisk(mt.getPrimaryDisk());
        if (mt.getReplicaDisk() != null) {
            cpy_mt.setReplicaDISK(mt.getReplicaDisk());
        }
        
        return cpy_mt;
    }
    
    /**
     * Remove the metadata associated with path if it existed 
     * @param bucket bucket name
     * @param path  path or key of the metadata is going to be removed
     * @return 
     */
    public int remove(String bucket, String path) {
        dbm.deleteObject(bucket, path, "null");
        return 0;
        // Metadata mt;
        // JSONObject obj;
        // String bindingKey;
        // String bindingKeyPref = "*.servers.unlink.";
        // try {
        //     mt = dbm.selectSingleObject(bucket, path);
        //     // remove from DB
        //     dbm.deleteObject(bucket, path, "null");
            
        //     obj = new JSONObject();
        //     obj.put("ObjId", mt.getObjId());
        //     obj.put("Path", mt.getPath());
        //     obj.put("DiskId", mt.getPrimaryDisk().getId());
        //     obj.put("DiskPath", mt.getPrimaryDisk().getPath());
        //     bindingKey = bindingKeyPref + mt.getPrimaryDisk().getId();
        //     if (mqSender != null){ // FIXME until MQ intergation working well
        //         mqSender.send(obj.toString(), bindingKey);

        //         obj.replace("DiskId", mt.getReplicaDisk().getId());
        //         obj.replace("DiskPath", mt.getReplicaDisk().getPath());
        //         bindingKey = bindingKeyPref + mt.getReplicaDisk().getId();
        //         mqSender.send(obj.toString(), bindingKey);
        //     }
        //     return 0;
        // } catch (ResourceNotFoundException ex) {
        //     // logger.log(OMLoggerLevel.ERROR, ex, "[remove ] bucket : %s path : %s failed", bucket, path);
        //     logger.debug(ex.getMessage());
        // }
        // catch (Exception ex) {
        //     // logger.log(OMLoggerLevel.ERROR, ex, "[remove ] bucket : %s path : %s failed", bucket, path);
        //     logger.debug(ex.getMessage());
        // }
        // return -1;
    }

    /**
     * Remove the metadata associated with path if it existed 
     * @param bucket bucket name
     * @param path  path or key of the metadata is going to be removed
     * @param versionId the version Id
     * @return 
     */
    public int remove(String bucket, String path, String  versionId){
        Metadata mt;
        JSONObject obj;
        String bindingKey;
        String bindingKeyPref = "*.servers.unlink.";
        try {
            mt = dbm.selectSingleObject(bucket, path, versionId);
            // remove from DB
            dbm.deleteObject(bucket, path, versionId);
            
            obj = new JSONObject();
            obj.put("ObjId", mt.getObjId());
            obj.put("Path", mt.getPath());
            obj.put("DiskId", mt.getPrimaryDisk().getId());
            obj.put("DiskPath", mt.getPrimaryDisk().getPath());
            bindingKey = bindingKeyPref + mt.getPrimaryDisk().getId();
            mqSender.send(obj.toString(), bindingKey);
            
            obj.replace("DiskId", mt.getReplicaDisk().getId());
            obj.replace("DiskPath", mt.getReplicaDisk().getPath());
            bindingKey = bindingKeyPref + mt.getReplicaDisk().getId();
            mqSender.send(obj.toString(), bindingKey);
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
        DISK rdsk = null;
        boolean isreplicaExist;
        
        bt = obmCache.getBucketFromCache(bucketName);
        if (bt == null)
           throw new ResourceNotFoundException("Bucket(" + bucketName +") not exist!");
        
        pdsk = obmCache.getDiskWithPath(bt.getDiskPoolId(), pdskPath);
        isreplicaExist = false;
        if (rdskPath != null){
            if (!rdskPath.isEmpty()){
                rdsk = obmCache.getDiskWithPath(bt.getDiskPoolId(), rdskPath);
                isreplicaExist = true;
            } 
        }
              
        try {
            if (path.isEmpty())
               throw new InvalidParameterException("empty path not allowed!"); 
            
            if (!obmCache.validateDisk(bt.getDiskPoolId(), "", pdskPath))
                throw new InvalidParameterException("primary disk("+pdskPath+") not exist in the system!");
            
            if (!obmCache.validateDisk(bt.getDiskPoolId(), "", rdskPath)){
                logger.debug("bucket : {} path : {} there replica disk provided", bucketName, path);
                //throw new InvalidParameterException("Replica disk not exist in the system!");
            }
            
            mt = dbm.selectSingleObject(bucketName, path);
            if (!bt.getVersioning().equalsIgnoreCase("Enabled"))
                dbm.deleteObject(bucketName, path, "null");
            mt.setPrimaryDisk(pdsk);
            if (isreplicaExist)
                mt.setReplicaDISK(rdsk);
            mt.setEtag(etag);
            mt.setTag(tag);
            mt.setMeta(meta); 
            mt.setSize(size);
            mt.setAcl(acl);
            mt.setVersionId(versionId, deleteMarker, true);
        } catch(ResourceNotFoundException ex){
            // create meta
            mt = new Metadata(bucketName, path, etag, meta, tag, size, acl, pdsk.getId(), 
                    pdsk.getPath(), rdsk == null ? "" : rdsk.getId(), 

                    rdsk == null ? "" : rdsk.getPath(), versionId, deleteMarker); 
 
            mt.setVersionId(versionId, deleteMarker, true);
        }
    
        // inseret to db
        return dbm.insertObject(mt);
    }
    
    public int close(String bucketName, String key, Metadata mt) throws ResourceNotFoundException{
        Bucket bt;
        Metadata mtd;
        
        bt = obmCache.getBucketFromCache(bucketName);
        if (bt == null)
           throw new ResourceNotFoundException("Bucket(" + bucketName +") not exist!");
        try{
            mtd = dbm.selectSingleObject(bucketName, key);
            if (!bt.getVersioning().equalsIgnoreCase("Enabled"))
                dbm.deleteObject(bucketName, key, "null");
        } catch(ResourceNotFoundException ex){
            
        }
        
        return dbm.insertObject(mt); 
    }
    /**
     *
     * @param bucketName  name of the bucket containing the objects. 
     * @param delimiter   A string used to group objects keys
     * @param startAfter  key to start with when listing objects in a bucket.
     * @param maxKeys     maximum number of keys returned.
     * @param prefix      return keys that begin with the prefix
     * @return
     */
    public Document listObjects(String bucketName, String delimiter, String startAfter, int maxKeys, String prefix){
        Document res;
        ListObject lo = new ListObject(dbm, bucketName, delimiter, startAfter, maxKeys, prefix);
        res = lo.excute();
        //Element root = res.getDocumentElement();
        //logger.debug("res : >" + root.getTagName());
        logger.debug("result : >" + lo);
        return res;
    }

    /**
     *
     * @param bucketName  name of the bucket containing the objects. 
     * @param delimiter   A string used to group objects keys
     * @param startAfter  key to start with when listing objects in a bucket.
     * @param maxKeys     maximum number of keys returned.
     * @param prefix      return keys that begin with the prefix
     * @return
     */
    public ObjectListParameter getListObjects(String bucketName, String delimiter, String startAfter, int maxKeys, String prefix) {
        ObjectListParameter objectListParameter = null;
        
        ListObject lo = new ListObject(dbm, bucketName, delimiter, startAfter, maxKeys, prefix);
        objectListParameter = lo.executeDirect();
        
        return objectListParameter;
    }
    
    /**
     *
     * @param bucketName  name of the bucket containing the objects. 
     * @param delimiter   A string used to group objects keys
     * @param startAfter  key to start with when listing objects in a bucket.
     * @param maxKeys     maximum number of keys returned.
     * @param prefix      return keys that begin with the prefix
     * @return
     */
    public ObjectListParameter getListObjectsVersions(String bucketName, String delimiter, String startAfter, String startAfterVersionId, int maxKeys, String prefix) {
        ObjectListParameter objectListParameter = null;
        
        ListObject lo = new ListObject(dbm, bucketName, delimiter, startAfter, startAfterVersionId, maxKeys, prefix);
        objectListParameter = lo.executeDirect();
        
        return objectListParameter;
    }


    /**
     * Create bucket if it is not exist
     * @param bucketName bucket name
     * @param diskPoolId the diskPoolId assigned to the bucket
     * @return 0 for success or negative number for error     
     * @throws com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException     
     * @throws com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException     
     */
    
    public int createBucket(String bucketName, String diskPoolId, String userName, String userId, String acl, String encryption, String objectlock) throws ResourceAlreadyExistException, ResourceNotFoundException{
       
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        if (bt != null)
            throw new ResourceAlreadyExistException("Bucket (" +bucketName + ") already exist in the system!");
        
        try {
            bt = dbm.selectBucket(bucketName);
            if (bt != null){
                obmCache.setBucketInCache(bt);
                throw new ResourceAlreadyExistException("Bucket (" +bucketName + ") already exist in the system!");
            }
        } catch (SQLException | ResourceNotFoundException ex) {
            DISKPOOL dp = obmCache.getDiskPoolFromCache(diskPoolId);
            if (dp == null)
                throw new ResourceNotFoundException("Disk pool(" + diskPoolId +") not exist in the system!");

            bt = dbm.insertBucket(bucketName, diskPoolId, userName, userId, acl, encryption, objectlock);

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
     * @throws SQLException
     */
    public List<S3BucketSimpleInfo> listBucketSimpleInfo(String userName, String userId) throws SQLException {
        return dbm.listBuckets(userName, userId);
        // return obmCache.getBucketSimpleList();
        
        // 2021-11-18 temporary fix
        // obmCache.resetBucketList();
        // dbm.loadBucketList();
        // List<S3BucketSimpleInfo> list = obmCache.getBucketSimpleList();

        // return list;
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
        // try {
        //     Bucket bt = dbm.selectBucket(bucketName);
        // } catch (ResourceNotFoundException | SQLException e) {
        //     return false;
        // }

        // return true;
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
        logger.debug(" PRIMARY > " + pdiskid + ", " + primary + " REPLICA >" + rdiskid + " ," + replica);
        Metadata md = new Metadata();
        md.setObjid(objid);
        md.setPrimaryDisk(primary);
        md.setReplicaDISK(replica);
        return dbm.updateDisks(md);
    }
    
    public int putBucketVersioning(String bucketName, String versionState) throws ResourceNotFoundException, SQLException{
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setVersioning(versionState, "");
        dbm.updateBucketPolicy(bt);

        return dbm.updateBucketVersioning(bt);
    }
    
    public String getBucketVersioning(String bucketName) throws ResourceNotFoundException, SQLException{
        Bucket bt = dbm.selectBucket(bucketName);
        return bt.getVersioning();
    }
    
    public Document listObjectsVersion(String bucketName, String delimiter, String startAfter, String startAfterVersionId, int maxKeys, String prefix){
        Document res;
        ListObject lo = new ListObject(dbm, bucketName, delimiter, startAfter, startAfterVersionId, maxKeys, prefix);
        res = lo.excute();
        //Element root = res.getDocumentElement();
        //logger.debug("res : >" + root.getTagName());
        logger.debug("result : >" + lo);
        return res;
    }

    public ObjectListParameter getListObjectsVersion(String bucketName, String delimiter, String startAfter, String startAfterVersionId, int maxKeys, String prefix){
        ObjectListParameter objectListParameter = null;
        ListObject lo = new ListObject(dbm, bucketName, delimiter, startAfter, startAfterVersionId, maxKeys, prefix);
        objectListParameter = lo.executeDirect();

        return objectListParameter;
    }

    public Metadata getObjectWithPath(String bucketName, String key) throws ResourceNotFoundException{
        return dbm.selectSingleObject(bucketName, key);
    }

    public Metadata getObjectWithVersionId(String bucketName, String key, String versionId) throws ResourceNotFoundException{
        return dbm.selectSingleObject(bucketName, key, versionId);
    }

    public Bucket getBucket(String bucketName) throws ResourceNotFoundException, SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        if (bt == null)
            bt = dbm.selectBucket(bucketName);

        return bt;
    }

    public void updateObjectMeta(Metadata meta) throws SQLException {
        dbm.updateObjectMeta(meta);
    }
    
    public void updateBucketAcl(String bucketName, String acl) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setAcl(acl);
        dbm.updateBucketAcl(bt);
    }

    public void updateBucketCors(String bucketName, String cors) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setCors(cors);
        dbm.updateBucketCors(bt);
    }

    public void updateBucketEncryption(String bucketName, String encryption) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setEncryption(encryption);
        dbm.updateBucketEncryption(bt);
    }

    public void updateBucketWeb(String bucketName, String web) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setWeb(web);
        dbm.updateBucketWeb(bt);
    }

    public void updateBucketLifecycle(String bucketName, String lifecycle) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setLifecycle(lifecycle);
        dbm.updateBucketLifecycle(bt);
    }

    public void updateBucketAccess(String bucketName, String access) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setAccess(access);
        dbm.updateBucketAccess(bt);
    }

    public void updateBucketTagging(String bucketName, String tagging) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setTagging(tagging);
        dbm.updateBucketTagging(bt);
    }

    public void updateBucketReplication(String bucketName, String replicationXml) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setReplication(replicationXml);
        dbm.updateBucketReplication(bt);
    }

    public void updateBucketObjectLock(String bucketName, String lock) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setObjectlock(lock);
        dbm.updateBucketObjectLock(bt);
    }

    public void updateBucketPolicy(String bucketName, String policy) throws SQLException {
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        bt.setPolicy(policy);
        dbm.updateBucketPolicy(bt);
    }

    public ObjectListParameter listObject(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        return dbm.listObject(bucketName, s3ObjectList.getDelimiter(), s3ObjectList.getMarker(), Integer.parseInt(s3ObjectList.getMaxKeys()), s3ObjectList.getPrefix());
    }

    public ObjectListParameter listObjectV2(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        return dbm.listObjectV2(bucketName, s3ObjectList.getDelimiter(), s3ObjectList.getStartAfter(), s3ObjectList.getContinuationToken(), Integer.parseInt(s3ObjectList.getMaxKeys()), s3ObjectList.getPrefix());
    }

    public ObjectListParameter listObjectVersions(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        return dbm.listObjectVersions(bucketName, s3ObjectList.getDelimiter(), s3ObjectList.getKeyMarker(), s3ObjectList.getVersionIdMarker(), Integer.parseInt(s3ObjectList.getMaxKeys()), s3ObjectList.getPrefix());
    }

    public void updateObjectTagging(Metadata meta) throws SQLException {
        dbm.updateObjectTagging(meta);
    }

    public void updateObjectAcl(Metadata meta) throws SQLException {
        dbm.updateObjectAcl(meta);
    }

    // Add 2021-11-22
    public boolean isBucketDelete(String bucketName) throws SQLException {
        return dbm.isBucketDelete(bucketName);
    }

    public boolean isValid() {
		if (this.dbm != null) {
			try {
                return this.dbm.isClosed();
            } catch (SQLException e) {
                logger.error(e.getMessage());
                return true;
            }
		}
		return false;
	}

    public void close() throws SQLException {
        if (this.dbm != null) {
            this.dbm.close();
        }
    }

	public void activate() {
		logger.debug("activate db connect...");
	}

	public void desactivate() {
		logger.debug("desactivate db connect...");
	}
}
