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
import java.util.SortedMap;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
// import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.libs.multipart.Multipart;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.libs.multipart.ResultParts;
import com.pspace.ifs.ksan.libs.multipart.ResultUploads;

/**
 *
 * @author legesse
 */
public interface DataRepository {
    // for object
    public int insertObject(Metadata md) throws ResourceNotFoundException;
    public int updateDisks(Metadata md, boolean updatePrimary, DISK newDisk);
    public int updateSizeTime(Metadata md);
    public Metadata selectSingleObject(String diskPoolId, String bucketName, String path) throws ResourceNotFoundException;
    public Metadata selectSingleObject(String diskPoolId, String bucketName, String path, String versionId) throws ResourceNotFoundException;
    public Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objid) throws ResourceNotFoundException; 
    public Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objid, String versionId) throws ResourceNotFoundException; 
    public void selectObjects(String bucketName, Object query, int maxKeys, DBCallBack callback) throws SQLException;
    public int deleteObject(String bucketName, String path, String versionId);
    public int markDeletedObject(String bucketName, String path, String versionId, String markDelete) throws SQLException;
    public List<String> getAllUsedDiskId() throws SQLException;
    public Object getStatement(String query) throws SQLException;
    public List<Metadata> getObjectList(String bucketName, Object pstmt, int maxKeys, long offset) throws SQLException;
    public long getObjectListCount(String bucketName, Object pstmt) throws SQLException;
    //public void updateObjectMeta(String bucket, String objkey, String versionid, String meta) throws SQLException;
    public void updateObjectMeta(Metadata mt) throws SQLException;
    public void updateObjectTagging(Metadata mt) throws SQLException;
    public void updateObjectAcl(Metadata mt) throws SQLException;
    public void updateObjectEtag(Metadata mt, String etag) throws SQLException;
    
    // for bucket
    public Bucket insertBucket(Bucket bt)throws ResourceAlreadyExistException;
    public int deleteBucket(String bucketName);
    public Bucket selectBucket(String bucketName)throws ResourceNotFoundException, SQLException;
    public void loadBucketList(); 
    public List<Bucket> getBucketList();
    public int updateBucketVersioning(Bucket bt);
    public void updateBucketAcl(Bucket bt) throws SQLException;
    public void updateBucketCors(Bucket bt) throws SQLException;
    public void updateBucketWeb(Bucket bt) throws SQLException;
    public void updateBucketLifecycle(Bucket bt) throws SQLException;
    public void updateBucketAccess(Bucket bt) throws SQLException;
    public void updateBucketTagging(Bucket bt) throws SQLException;
    public void updateBucketReplication(Bucket bt) throws SQLException;
    public boolean isBucketDeleted(String bucket) throws SQLException;
    public void updateBucketEncryption(Bucket bt) throws SQLException;
    public void updateBucketObjectLock(Bucket bt) throws SQLException;
    public void updateBucketPolicy(Bucket bt) throws SQLException;
    public void updateBucketUsedSpace(Bucket bt, long size) throws SQLException ;
    //public void updateBucketFileCount(Bucket bt) throws SQLException ;
    public void updateBucketLogging(Bucket bt) throws SQLException;
    public void updateBucketObjTagIndexing(Bucket bt) throws SQLException;
    
    // for multipart upload
    public int insertMultipartUpload(String bucket, String objkey, String uploadid, int partNo, String acl, String meta, String etag, long size, String pdiskid) throws SQLException;
    public int updateMultipartUpload(String bucket, String uploadid, int partNo, boolean iscompleted) throws SQLException;
    public int deleteMultipartUpload(String bucket,  String uploadid) throws SQLException;
    public List<Integer> selectMultipart(String bucket, String uploadid, int maxParts, int partNoMarker) throws SQLException;
    public void selectMultipartUpload(String bucketName, Object query, int maxKeys, DBCallBack callback) throws SQLException;
    public Multipart getMulipartUpload(String uploadid) throws SQLException, ResourceNotFoundException ;
    public SortedMap<Integer, Part> getParts(String uploadId) throws SQLException;
    public ResultParts getParts(String uploadId, int partNumberMarker, int maxParts) throws SQLException;
    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException;
    public boolean isUploadId(String uploadid) throws SQLException;
    public Metadata getObjectWithUploadIdPart(String diskPoolId, String uploadId, int partNo) throws SQLException;
    
    // for utility 
    public List<Object> utilJobMgt(String operation, List<Object> in);
   
    // for Lifecycle
    public void insertLifeCycle(LifeCycle lc) throws SQLException;
    public void insertFailedLifeCycle(LifeCycle lc) throws SQLException; 
    public LifeCycle selectLifeCycle(LifeCycle lc) throws SQLException;
    public LifeCycle selectFailedLifeCycle(LifeCycle lc) throws SQLException;
    public LifeCycle selectByUploadIdLifeCycle(String uploadId) throws SQLException;
    public LifeCycle selectByUploadIdFailedLifeCycle(String uploadId) throws SQLException;
    public List<LifeCycle> selectAllLifeCycle() throws SQLException;
    public List<LifeCycle> selectAllFailedLifeCycle() throws SQLException;
    public int deleteLifeCycle(LifeCycle lc) throws SQLException;
    public int deleteFailedLifeCycle(LifeCycle lc) throws SQLException;
    
    // for object tags indexing
     public List<Metadata> listObjectWithTags(String bucketName, Object query, int maxKeys) throws SQLException;
     
     // for restore object
     public int insertRestoreObjectRequest(String bucketName, String key, String objId, String versionId, String request) throws SQLException;
     public String getRestoreObjectRequest(String bucketName, String objId, String versionId) throws SQLException;
     public void deleteRestoreObjectRequest(String bucketName, String objId, String versionId) throws SQLException;
}
