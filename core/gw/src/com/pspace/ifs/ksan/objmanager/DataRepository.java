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

import java.sql.SQLException;
import java.util.List;
import java.util.SortedMap;

import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.ObjectListParameter;
import com.pspace.ifs.ksan.gw.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.gw.object.multipart.Multipart;
import com.pspace.ifs.ksan.gw.object.multipart.Part;
import com.pspace.ifs.ksan.gw.object.multipart.ResultParts;
import com.pspace.ifs.ksan.gw.object.multipart.ResultUploads;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

/**
 *
 * @author legesse
 */
public interface DataRepository {
    public int insertObject(Metadata md) throws ResourceNotFoundException;
    public int updateDisks(Metadata md);
    public int updateSizeTime(Metadata md);
    public Metadata selectSingleObject(String bucketName, String path) throws ResourceNotFoundException;
    public Metadata selectSingleObject(String bucketName, String path, String versionId) throws ResourceNotFoundException;
    public Metadata selectSingleObjectWithObjId(String bucketName, String objid) throws ResourceNotFoundException; 
    public ObjectListParameter selectObjects(String bucketName, Object query, int maxKeys) throws SQLException;
    public void selectObjects(String bucketName, Object query, int maxKeys, DBCallBack callback) throws SQLException;
    public int deleteObject(String bucketName, String path, String versionId);
    public List<String> getAllUsedDiskId() throws SQLException;
    public Bucket insertBucket(String bucketName, String diskPoolId, String userName, String userId, String acl, String encryption, String objectlock)throws ResourceAlreadyExistException;
    public int deleteBucket(String bucketName);
    public Bucket selectBucket(String bucketName)throws ResourceNotFoundException, SQLException;
    public void loadBucketList(); 
    public List<Bucket> getBucketList();
    public int updateBucketVersioning(Bucket bt);
    
    public int insertMultipartUpload(String bucket, String objkey, String uploadid, int partNo, String acl, String meta, String etag, long size) throws SQLException;
    public int updateMultipartUpload(String bucket, String uploadid, int partNo, boolean iscompleted) throws SQLException;
    public int deleteMultipartUpload(String bucket,  String uploadid) throws SQLException;
    public List<Integer> selectMultipart(String bucket, String uploadid, int maxParts, int partNoMarker) throws SQLException;
    public void selectMultipartUpload(String bucketName, Object query, int maxKeys, DBCallBack callback) throws SQLException;

    public Multipart getMulipartUpload(String uploadid) throws SQLException;
    public SortedMap<Integer, Part> getParts(String uploadId) throws SQLException;
    public ResultParts getParts(String uploadId, String partNumberMarker, int maxParts) throws SQLException;
    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException, GWException;
    public boolean isUploadId(String uploadid) throws SQLException;
    
    public void updateBucketAcl(Bucket bt) throws SQLException;
    public void updateBucketCors(Bucket bt) throws SQLException;
    public void updateBucketWeb(Bucket bt) throws SQLException;
    public void updateBucketLifecycle(Bucket bt) throws SQLException;
    public void updateBucketAccess(Bucket bt) throws SQLException;
    public void updateBucketTagging(Bucket bt) throws SQLException;
    public void updateBucketReplication(Bucket bt) throws SQLException;
    // Add 2021-11-24
    public void updateBucketEncryption(Bucket bt) throws SQLException;
    public void updateBucketObjectLock(Bucket bt) throws SQLException;
    public void updateBucketPolicy(Bucket bt) throws SQLException;

    public ObjectListParameter listObject(String bucketName, String delimiter, String marker, int maxKeys, String prefix) throws SQLException;
    public ObjectListParameter listObjectV2(String bucketName, String delimiter, String startAfter, String continueationToken, int maxKeys, String prefix) throws SQLException;
    public ObjectListParameter listObjectVersions(String bucketName, String delimiter, String keyMarker, String versionIdMarker, int maxKeys, String prefix) throws SQLException;
    
    public List<Object> utilJobMgt(String operation, List<Object> in);

    // Add 2021-11-22
    public boolean isBucketDelete(String bucket) throws SQLException;
    public boolean isClosed() throws SQLException;
    public void close() throws SQLException;

    // Add 2021-11-29
    public void updateObjectMeta(Metadata meta) throws SQLException;
    public void updateObjectTagging(Metadata meta) throws SQLException;
    public void updateObjectAcl(Metadata meta) throws SQLException;

    public List<S3BucketSimpleInfo> listBuckets(String userName, String userId) throws SQLException;
    public void updateBucketUsed(String bucketName, long size);
}
