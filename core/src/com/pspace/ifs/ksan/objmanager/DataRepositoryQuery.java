/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.ksan.objmanager;

/**
 *
 * @author legesse
 */
public final class DataRepositoryQuery {
    // for objects
    public String objCreateQuery = "CREATE TABLE IF NOT EXISTS '%s'("
            + "bucket VARCHAR(256) NOT NULL, objKey VARCHAR(2048) NOT NULL, size BIGINT NOT NULL default 0,"
            + "etag TEXT NOT NULL, meta TEXT NOT NULL, tag TEXT NOT NULL, "
            + "pdiskid VARCHAR(80) NOT NULL, rdiskid VARCHAR(80) NOT NULL, objid VARCHAR(50) NOT NULL, "
            + "acl TEXT NOT NULL, "
            + "lastModified BIGINT DEFAULT 0, versionid VARCHAR(50) NOT NULL DEFAULT 'nil', deleteMarker VARCHAR(32) NOT NULL, lastversion BOOLEAN default true, "
            + "PRIMARY KEY(objid, versionid, deleteMarker), INDEX index_objkey(objkey)) ENGINE=INNODB DEFAULT CHARSET=UTF8;";  
    
    public String objInsertQuery = "INSERT INTO '%s'(bucket, objKey, etag, meta, tag, acl, size, lastModified, pdiskid, rdiskid, objid, versionid, deleteMarker, lastversion) VALUES(?, ?, ?, ?, ?, ?, ?,  9223372036854775808 - ?, ?, ?, ?, ?, ?, true)";
    public String objDeleteQuery = "DELETE FROM '%s' WHERE objid=? AND (versionid=? OR versionid is NULL)";

    public String objSelectOneQuery = "SELECT bucket, objKey, size, objid, etag, tag, meta, acl, pdiskid, rdiskid, versionid, deleteMarker, lastversion FROM '%s' WHERE objid=? AND lastversion=true";
    public String objSelectOneWithVersionIdQuery = "SELECT bucket, objKey, size, objid, etag, tag, meta, acl, pdiskid, rdiskid, versionid, deleteMarker, lastversion FROM '%s' WHERE objid=? AND versionid=?";
    public String objUpdateMetadataQuery = "UPDATE '%s' SET etag=?, meta=?, tag=?, acl=?, size=?, lastModified=?, pdiskid=?, rdiskid=?, versionid=?, deleteMarker=?, lastversion=? WHERE objid=?"; 
    public String objUpdateDeleteMarkerQuery ="UPDATE '%s' SET deleteMarker=?, lastversion=? WHERE objid=? AND versionid=? AND lastversion=true";

    public String objSelectListQuery = "SELECT bucket, objid, etag, tag, meta, pdiskid, rdiskid FROM '%s' WHERE objKey LIKE ?";
    public String objUpdatePDisksQuery = "UPDATE '%s' SET pdiskid=? WHERE objid=? AND versionid=?";
    public String objUpdateRDisksQuery = "UPDATE '%s' SET rdiskid=? WHERE objid=? AND versionid=?";
    public String objUpdateSizeTimeQuery = "UPDATE '%s' SET size=?, lastModified=? WHERE objid=?";
    public String objUpdateLastVersionQuery = "UPDATE '%s' SET lastversion=false WHERE objid=? AND lastversion=true";
    public String objUpdateLastVersionDeleteQuery = "UPDATE '%s' SET lastversion=true WHERE objid=? AND deleteMarker <> 'mark' ORDER BY lastModified asc limit 1";
    public String objSelectUsedDisksQuery = "SELECT pdiskid as diskid FROM '%s' UNION DISTINCT SELECT rdiskid FROM '%s';";
    public String objIsDeleteBucketQuery = "SELECT objKey FROM '%s' WHERE bucket=? LIMIT 1";
    public String objUpdateObjectMetaQuery = "UPDATE '%s' SET meta=? WHERE objid=? AND versionid=?";
    public String objUpdateTaggingQuery = "UPDATE '%s' SET tag=?, meta=? WHERE objid=? AND versionid=?";
    public String objUpdateAclQuery = "UPDATE '%s' SET acl=? WHERE objid=? AND versionid=?";
    
    // for bucket
    public String createBucketQuery = "CREATE TABLE IF NOT EXISTS BUCKETS("
                    + "name VARCHAR(256) NOT NULL, "
                    + "id VARCHAR(80) NOT NULL, "
                    + "diskPoolId CHAR(36) NOT NULL, "
                    + " userId CHAR(36), userName VARCHAR(200), "
                    + "acl TEXT, web TEXT, cors TEXT, "
                    + "lifecycle TEXT, access TEXT, "
                    + "tagging TEXT, replication TEXT, "
                    + "encryption TEXT,   objectlock TEXT,  policy TEXT, "
                    + "versioning VARCHAR(50), MfaDelete VARCHAR(50), "
                    + "createTime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "replicaCount INT DEFAULT 2, "
                    + "usedSpace BIGINT  NOT NULL DEFAULT 0,  fileCount BIGINT  NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(id)) ENGINE=INNODB DEFAULT CHARSET=UTF8;";
    
    public String  insertBucketQuery = "INSERT INTO BUCKETS(name, id, diskPoolId, userName, userId, acl, encryption, objectlock, replicaCount) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public String  deleteBucketQuery = "DELETE FROM BUCKETS WHERE id=?";
    public String  selectBucketQuery = "SELECT name, id, diskPoolId, versioning, MfaDelete, userName, userId, acl, web, cors, lifecycle, access, tagging, replication, encryption, objectlock, policy, createTime, replicaCount, usedSpace, fileCount FROM BUCKETS WHERE id=?";
    public String  selectAllBucketQuery = "SELECT name, id, diskPoolId, versioning, MfaDelete, userName, userId, acl, web, cors, lifecycle, access, tagging, replication, encryption, objectlock, policy, createTime, replicaCount, usedSpace, fileCount FROM BUCKETS";
    public String  updateBucketQuery = "UPDATE BUCKETS SET versioning=? WHERE id=?";
            
    public String  updateBucketAclQuery = "UPDATE BUCKETS SET acl=? WHERE id=?";
    public String  updateBucketWebQuery = "UPDATE BUCKETS SET web=? WHERE id=?";
    public String  updateBucketCorsQuery = "UPDATE BUCKETS SET cors=? WHERE id=?";
    public String  updateBucketLifecycleQuery = "UPDATE BUCKETS SET lifecycle=? WHERE id=?";
    public String  updateBucketAccessQuery = "UPDATE BUCKETS SET access=? WHERE id=?";
    public String  updateBucketTaggingQuery = "UPDATE BUCKETS SET tagging=? WHERE id=?";
    public String  updateBucketReplicationQuery = "UPDATE BUCK SET replication=? WHERE id=?";
    public String  updateBucketEncryptionQuery = "UPDATE BUCKETS SET encryption=? WHERE id=?";
    public String  updateBucketObjectLockQuery = "UPDATE BUCKETS SET objectlock=? WHERE id=?";
    public String  updateBucketPolicyQuery = "UPDATE BUCKETS SET policy=? WHERE id=?";
    public String  updateBucketFilecountQuery = "UPDATE BUCKETS SET fileCount = fileCount + ? WHERE id=?";
    public String  updateBucketUsedSpaceQuery = "UPDATE BUCKETS SET usedSpace = usedSpace + ? WHERE id=?";
            
            // for multipart
     public String createMultiPartQuery= "CREATE TABLE IF NOT EXISTS MULTIPARTS("
                    + " bucket VARCHAR(256) NOT NULL DEFAULT '' COMMENT 'bucket name',"
                    + " objKey VARCHAR(2048) COMMENT 'Object key'," 
                    + " changeTime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'time upload started',"
                    + " completed BOOLEAN DEFAULT false COMMENT 'job completed or in-progress',"
                    + " uploadid VARCHAR(80) NOT NULL COMMENT 'multi-part upload Id',"
                    + " acl TEXT,"
                    + " meta TEXT,"
                    + " etag TEXT,"
                    + " size bigint(20),"
                    + " partNo INT NOT NULL COMMENT 'part sequence number',"
                    + " pdiskid VARCHAR(80) NOT NULL, "
                    + " PRIMARY KEY(uploadid, partNo), INDEX index_objkey(objkey)) ENGINE=INNODB DEFAULT CHARSET=UTF8;";
     public String  insertMultiPartQuery = "INSERT INTO MULTIPARTS(bucket, objKey, uploadid, partNo, acl, meta, etag, size, pdiskid, changeTime) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
     public String  updateMultiPartQuery = "UPDATE MULTIPARTS SET completed=?, changeTime=now() WHERE uploadid=? and partNo=?";
     public String  deleteMultiPartQuery = "DELETE FROM MULTIPARTS WHERE uploadid=?";
     public String  selectMultiPartQuery = "SELECT bucket, objKey, uploadid, partNo FROM MULTIPARTS WHERE uploadid=? AND  partNo > ? ORDER BY partNo LIMIT ? ";

     public String  getMultiPartQuery = "SELECT bucket, objKey, changeTime, uploadid, acl, meta, pdiskid FROM MULTIPARTS WHERE uploadid=? AND  partNo = 0";
     public String  getPartsQuery = "SELECT changeTime, etag, size, partNo, pdiskid FROM MULTIPARTS WHERE uploadid=? AND  partNo != 0";
     public String  getPartsMaxQuery = "SELECT changeTime, etag, size, partNo, pdiskid FROM MULTIPARTS WHERE uploadid=? AND partNo > ? ORDER BY partNo LIMIT ?";
     public String  getUploadsQuery = "SELECT objKey, changeTime, uploadid, meta FROM MULTIPARTS WHERE bucket=? AND partNo = 0 AND completed=false ORDER BY partNo LIMIT ? ";
     public String  isUploadQuery = "SELECT bucket FROM MULTIPARTS WHERE uploadid=?";
     public String  isUploadPartNoQuery ="SELECT bucket, objKey, acl, meta, etag, size, pdiskid FROM MULTIPARTS WHERE uploadid=? AND partNo=?";

     // for utility
     public String  createUJobQuery = "CREATE TABLE IF NOT EXISTS UTILJOBS(Id VARCHAR(15) NOT NULL PRIMARY KEY, "
                    + "status VARCHAR(20) NOT NULL, TotalNumObject BIGINT NOT NULL default 0, "
                    + "NumJobDone BIGINT NOT NULL default 0, checkOnly BOOLEAN DEFAULT false, "
                    + "utilName VARCHAR(256) NOT NULL, startTime DATETIME )";
     public String  insertUJobQuery = "INSERT INTO UTILJOBS(Id, status, TotalNumObject, checkOnly, utilName, startTime) VALUES(?, ?, ?, ?, ?, now())";
     public String  updateUJob1Query = "UPDATE UTILJOBS SET status=? WHERE Id=?";
     public String  updateUJob2Query = "UPDATE UTILJOBS SET TotalNumObject=?, NumJobDone=? WHERE Id=?";
     public String  selectUJobQuery = "SELECT status, TotalNumObject, NumJobDone, checkOnly, utilName, startTime FROM UTILJOBS WHERE Id=?";

            // for user disk pool table
     public String  createUserDiskPoolQuery = "CREATE TABLE IF NOT EXISTS USERSDISKPOOL(userId VARCHAR(50) NOT NULL, "
                    + "credential VARCHAR(80) NOT NULL COMMENT 'access key _ secret key', diskpoolId VARCHAR(50) NOT NULL, "
                    + "replcaCount INT NOT NULL, PRIMARY KEY(userId, credential)) ENGINE=INNODB DEFAULT CHARSET=UTF8;";
     public String  insertUserDiskPoolQuery = "INSERT INTO USERSDISKPOOL(userId, credential, diskpoolId, replcaCount) VALUES(?, ?, ?, ?)";
            
     public String  selectUserDiskPoolQuery = "SELECT diskpoolId, replcaCount FROM USERSDISKPOOL WHERE userId=?";
            
     public String  deleteUserDiskPoolQuery = "DELETE FROM USERSDISKPOOL WHERE userId=? AND diskpoolId=?";
}
