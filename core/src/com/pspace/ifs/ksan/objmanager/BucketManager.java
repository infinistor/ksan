
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

import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class BucketManager {
    private final DataRepository dbm;
    private final ObjManagerCache  obmCache;
    private static Logger logger;
    
    public BucketManager(DataRepository dbm, ObjManagerCache  obmCache){
        logger = LoggerFactory.getLogger(BucketManager.class);
        this.dbm = dbm;
        this.obmCache = obmCache;
    }
    
    public int createBucket(Bucket bt) throws ResourceAlreadyExistException, SQLException, ResourceNotFoundException{
        DISKPOOL db;
        
        db = obmCache.getDiskPoolFromCache(bt.getDiskPoolId());
        try {
            dbm.selectBucket(bt.getName());
        } catch (ResourceNotFoundException ex) {
            bt.setReplicaCount(db.getDefaultReplicaCount());
            Bucket bt1 = dbm.insertBucket(bt);
            obmCache.setBucketInCache(bt1);
            return 0;
        }
        throw new ResourceAlreadyExistException("Bucket (" + bt.getName() + ") already exist in the system!");
    }
    
    public int createBucket(String bucketName, String userName, String userId, String acl, String  encryption, String objectlock) throws ResourceAlreadyExistException, ResourceNotFoundException{
       Bucket bt;
        
        try {
            bt = getBucket(bucketName);
            if (bt != null)
                throw new ResourceAlreadyExistException("[createBucket] Bucket (" +bucketName + ") already exist in the system!");
            
            bt = dbm.selectBucket(bucketName);
            if (bt != null){
                obmCache.setBucketInCache(bt);
                throw new ResourceAlreadyExistException("[createBucket] Bucket (" +bucketName + ") already exist in the system!");
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
                throw new ResourceNotFoundException("[createBucket] User("+ userId +") not assocated with diskpool. Please create user to diskpool assocation first!");
            }
            bt = dbm.insertBucket(bt);
            obmCache.setBucketInCache(bt);
        }
        return 0;
    }
    
    public int removeBucket(String bucketName){
        dbm.deleteBucket(bucketName);
        obmCache.removeBucketFromCache(bucketName);
        return 0;
    }
    
    public String[] listBucket(){
        return obmCache.getBucketNameList();
    }

    public List<S3BucketSimpleInfo> listBucketSimpleInfo(String userName, String userId) {
        return obmCache.getBucketSimpleList(userName, userId);
    }

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
       
    public int putBucketVersioning(String bucketName, String versionState) throws ResourceNotFoundException, SQLException{
        int ret;
        Bucket bt = dbm.selectBucket(bucketName);
        bt.setVersioning(versionState, "");
        ret = dbm.updateBucketVersioning(bt);
        obmCache.updateBucketInCache(bt);
        logger.debug("[putBucketVersioning] bucketName: {} status : {}", bucketName, versionState);
        return ret;
    }
    
    public String getBucketVersioning(String bucketName) throws ResourceNotFoundException, SQLException{
        Bucket bt = dbm.selectBucket(bucketName);
        return bt.getVersioning();
    }
    
    public Bucket getBucket(String bucketName) throws ResourceNotFoundException, SQLException {
        Bucket bt = dbm.selectBucket(bucketName);
        if (bt != null){
           obmCache.setBucketInCache(bt); 
        }
        /*Bucket bt = obmCache.getBucketFromCache(bucketName);
        if (bt == null){
            bt = dbm.selectBucket(bucketName);
            if (bt != null)
              obmCache.setBucketInCache(bt);
        }*/
        
        return bt;
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

    public boolean isBucketDeleted(String bucketName) throws SQLException {
        return dbm.isBucketDeleted(bucketName);
    }
}
