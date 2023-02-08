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
    public static String objCreateQuery = "CREATE TABLE IF NOT EXISTS %s("
            + "bucket VARCHAR(256) NOT NULL, objKey VARBINARY(2048) NOT NULL, size BIGINT NOT NULL default 0,"
            + "etag TEXT NOT NULL, meta TEXT NOT NULL, tag TEXT NOT NULL, "
            + "pdiskid VARCHAR(80) NOT NULL, rdiskid VARCHAR(80) NOT NULL, objid VARCHAR(50) NOT NULL, "
            + "acl TEXT NOT NULL, "
            + "lastModified BIGINT DEFAULT 0, versionid VARCHAR(50) NOT NULL DEFAULT 'nil', deleteMarker VARCHAR(32) NOT NULL, lastversion BOOLEAN default true, "
            + "PRIMARY KEY(objid, versionid, deleteMarker), INDEX index_objkey(objkey)) ENGINE=INNODB DEFAULT CHARSET=UTF8mb4 COLLATE=utf8mb4_unicode_ci;;";  
    
    public  static String objInsertQuery = "INSERT INTO %s(bucket, objKey, etag, meta, tag, acl, size, lastModified, pdiskid, rdiskid, objid, versionid, deleteMarker, lastversion) VALUES(?, ?, ?, ?, ?, ?, ?,  9223372036854775808 - ?, ?, ?, ?, ?, ?, true)";
    public  static String objDeleteQuery = "DELETE FROM %s WHERE objid=? AND (versionid=? OR versionid is NULL)";

    public  static  String objSelectOneQuery = "SELECT bucket, objKey, size, objid, etag, tag, meta, acl, pdiskid, rdiskid, versionid, deleteMarker, lastversion FROM %s WHERE objid=? AND lastversion=true";
    public  static String objSelectOneWithVersionIdQuery = "SELECT bucket, objKey, size, objid, etag, tag, meta, acl, pdiskid, rdiskid, versionid, deleteMarker, lastversion FROM %s WHERE objid=? AND versionid=?";
    public  static String objUpdateMetadataQuery = "UPDATE %s SET etag=?, meta=?, tag=?, acl=?, size=?, lastModified=?, pdiskid=?, rdiskid=?, versionid=?, deleteMarker=?, lastversion=? WHERE objid=?"; 
    public  static String objUpdateDeleteMarkerQuery ="UPDATE %s SET deleteMarker=?, lastversion=? WHERE objid=? AND versionid=? AND lastversion=true";

    public  static  String objSelectListQuery = "SELECT bucket, objid, etag, tag, meta, pdiskid, rdiskid FROM %s WHERE objKey LIKE ?";
    public  static String objUpdatePDisksQuery = "UPDATE %s SET pdiskid=? WHERE objid=? AND versionid=?";
    public  static String objUpdateRDisksQuery = "UPDATE %s SET rdiskid=? WHERE objid=? AND versionid=?";
    public  static  String objUpdateSizeTimeQuery = "UPDATE %s SET size=?, lastModified=? WHERE objid=?";
    public  static  String objUpdateLastVersionQuery = "UPDATE %s SET lastversion=false WHERE objid=? AND lastversion=true";
    public  static String objUpdateLastVersionDeleteQuery = "UPDATE %s SET lastversion=true WHERE objid=? AND deleteMarker <> 'mark' ORDER BY lastModified asc limit 1";
    //public  static String objSelectUsedDisksQuery = "SELECT pdiskid as diskid FROM %s UNION DISTINCT SELECT rdiskid FROM %s;";
    public  static String objSelectUsedDisksQuery = "SELECT pdiskid, rdiskid, objKey FROM %s WHERE objId > ? ORDER BY objKey LIMIT ?;";
    public  static String objIsDeleteBucketQuery = "SELECT objKey FROM %s LIMIT 1";
    public  static  String objUpdateObjectMetaQuery = "UPDATE %s SET meta=? WHERE objid=? AND versionid=?";
    public  static String objUpdateTaggingQuery = "UPDATE %s SET tag=?, meta=? WHERE objid=? AND versionid=?";
    public  static  String objUpdateAclQuery = "UPDATE %s SET acl=? WHERE objid=? AND versionid=?";
    public  static  String objUpdateEtagQuery = "UPDATE %s SET etag=? WHERE objid=? AND versionid=?";
    
    // for bucket
    public  static String createBucketQuery = "CREATE TABLE IF NOT EXISTS BUCKETS("
                    + "name VARCHAR(256) NOT NULL, "
                    + "id VARCHAR(80) NOT NULL, "
                    + "diskPoolId CHAR(36) NOT NULL, "
                    + " userId CHAR(36), userName VARCHAR(200), "
                    + "acl TEXT, web TEXT, cors TEXT, "
                    + "lifecycle TEXT, access TEXT, "
                    + "tagging TEXT, replication TEXT, logging TEXT, "
                    + "encryption TEXT,   objectlock TEXT,  policy TEXT, "
                    + "versioning VARCHAR(50), MfaDelete VARCHAR(50), "
                    + "createTime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "replicaCount INT DEFAULT 2, objTagIndexing BOOLEAN default false, "
                    + "usedSpace BIGINT  NOT NULL DEFAULT 0,  fileCount BIGINT  NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(id)) ENGINE=INNODB DEFAULT CHARSET=UTF8mb4 COLLATE=utf8mb4_unicode_ci;";
    
    public  static String  insertBucketQuery = "INSERT INTO BUCKETS(name, id, diskPoolId, userName, userId, acl, encryption, objectlock, replicaCount, objTagIndexing) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    public  static String  deleteBucketQuery = "DELETE FROM BUCKETS WHERE id=?";
    public  static String  selectBucketQuery = "SELECT name, id, diskPoolId, versioning, MfaDelete, userName, userId, acl, web, cors, lifecycle, access, tagging, replication, encryption, objectlock, policy, createTime, replicaCount, usedSpace, fileCount, logging, objTagIndexing FROM BUCKETS WHERE id=?";
    public  static String  selectAllBucketQuery = "SELECT name, id, diskPoolId, versioning, MfaDelete, userName, userId, acl, web, cors, lifecycle, access, tagging, replication, encryption, objectlock, policy, createTime, replicaCount, usedSpace, fileCount, logging, objTagIndexing FROM BUCKETS";
    public  static String  updateBucketQuery = "UPDATE BUCKETS SET versioning=? WHERE id=?";
            
    public  static String  updateBucketAclQuery = "UPDATE BUCKETS SET acl=? WHERE id=?";
    public  static String  updateBucketWebQuery = "UPDATE BUCKETS SET web=? WHERE id=?";
    public  static String  updateBucketCorsQuery = "UPDATE BUCKETS SET cors=? WHERE id=?";
    public  static String  updateBucketLifecycleQuery = "UPDATE BUCKETS SET lifecycle=? WHERE id=?";
    public  static String  updateBucketAccessQuery = "UPDATE BUCKETS SET access=? WHERE id=?";
    public  static String  updateBucketTaggingQuery = "UPDATE BUCKETS SET tagging=? WHERE id=?";
    public  static String  updateBucketReplicationQuery = "UPDATE BUCK SET replication=? WHERE id=?";
    public  static  String  updateBucketEncryptionQuery = "UPDATE BUCKETS SET encryption=? WHERE id=?";
    public  static String  updateBucketObjectLockQuery = "UPDATE BUCKETS SET objectlock=? WHERE id=?";
    public  static String  updateBucketPolicyQuery = "UPDATE BUCKETS SET policy=? WHERE id=?";
    public  static String  updateBucketFilecountQuery = "UPDATE BUCKETS SET fileCount = fileCount + ? WHERE id=?";
    public  static String  updateBucketUsedSpaceQuery = "UPDATE BUCKETS SET usedSpace = usedSpace + ? WHERE id=?";
    public  static String  updateBucketLoggingQuery = "UPDATE BUCK SET logging=? WHERE id=?";
    public  static String  updateBucketObjTagIndexingQuery = "UPDATE BUCK SET objTagIndexing=? WHERE id=?";
    
            // for multipart
     public  static String createMultiPartQuery= "CREATE TABLE IF NOT EXISTS MULTIPARTS("
                    + " bucket VARCHAR(256) NOT NULL DEFAULT '' COMMENT 'bucket name',"
                    + " objKey VARBINARY(2048) COMMENT 'Object key'," 
                    + " changeTime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'time upload started',"
                    + " completed BOOLEAN DEFAULT false COMMENT 'job completed or in-progress',"
                    + " uploadid VARCHAR(80) NOT NULL COMMENT 'multi-part upload Id',"
                    + " partRef TEXT,"
                    + " acl TEXT,"
                    + " meta TEXT,"
                    + " etag TEXT,"
                    + " size bigint(20),"
                    + " partNo INT NOT NULL COMMENT 'part sequence number',"
                    + " pdiskid VARCHAR(80) NOT NULL, "
                    + " PRIMARY KEY(uploadid, partNo), INDEX index_objkey(objkey)) ENGINE=INNODB DEFAULT CHARSET=UTF8mb4 COLLATE=utf8mb4_unicode_ci;";
     public  static String  insertMultiPartQuery = "INSERT INTO MULTIPARTS(bucket, objKey, uploadid, partNo, acl, meta, etag, size, pdiskid, changeTime) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, now())";
     public  static String  updateMultiPartQuery = "UPDATE MULTIPARTS SET completed=?, changeTime=now() WHERE uploadid=? and partNo=?";
     public  static String  deleteMultiPartQuery = "DELETE FROM MULTIPARTS WHERE uploadid=?";
     public  static String  selectMultiPartQuery = "SELECT bucket, objKey, uploadid, partNo FROM MULTIPARTS WHERE uploadid=? AND  partNo > ? ORDER BY partNo LIMIT ? ";

     public  static String  getMultiPartQuery = "SELECT bucket, objKey, changeTime, uploadid, acl, meta, pdiskid FROM MULTIPARTS WHERE uploadid=? AND  partNo = 0";
     public  static String  getPartsQuery = "SELECT changeTime, etag, size, partNo, pdiskid FROM MULTIPARTS WHERE uploadid=? AND  partNo != 0";
     public  static String  getPartsMaxQuery = "SELECT changeTime, etag, size, partNo, pdiskid FROM MULTIPARTS WHERE uploadid=? AND partNo > ? ORDER BY partNo LIMIT ?";
     public  static String  getUploadsQuery = "SELECT objKey, changeTime, uploadid, meta FROM MULTIPARTS WHERE bucket=? AND partNo = 0 AND completed=false ORDER BY partNo LIMIT ? ";
     public  static String  isUploadQuery = "SELECT bucket FROM MULTIPARTS WHERE uploadid=?";
     public  static String  isUploadPartNoQuery ="SELECT bucket, objKey, acl, meta, etag, size, pdiskid FROM MULTIPARTS WHERE uploadid=? AND partNo=?";
     public  static String  getPartRefQuery ="SELECT partRef FROM MULTIPARTS WHERE uploadid=? AND partNo=?";
     public  static String  updatePartRefQuery = "UPDATE MULTIPARTS SET partRef=? WHERE uploadid=? and partNo=?";
 
     // for utility
     public  static String  createUJobQuery = "CREATE TABLE IF NOT EXISTS UTILJOBS(Id VARCHAR(15) NOT NULL PRIMARY KEY, "
                    + "status VARCHAR(20) NOT NULL, TotalNumObject BIGINT NOT NULL default 0, "
                    + "NumJobDone BIGINT NOT NULL default 0, checkOnly BOOLEAN DEFAULT false, "
                    + "utilName VARCHAR(256) NOT NULL, startTime DATETIME )";
     public  static String  insertUJobQuery = "INSERT INTO UTILJOBS(Id, status, TotalNumObject, checkOnly, utilName, startTime) VALUES(?, ?, ?, ?, ?, now())";
     public  static String  updateUJob1Query = "UPDATE UTILJOBS SET status=? WHERE Id=?";
     public  static String  updateUJob2Query = "UPDATE UTILJOBS SET TotalNumObject=?, NumJobDone=? WHERE Id=?";
     public  static String  selectUJobQuery = "SELECT status, TotalNumObject, NumJobDone, checkOnly, utilName, startTime FROM UTILJOBS WHERE Id=?";
     
     // for LifeCycle
    public static String lifeCycleEventTableName = "LIFECYCLESEVENTS";
    public static String lifeCycleFailedEventTableName="LIFECYCLESFAILEDEVENTS";
    public  static String createLifeCycleQuery= "CREATE TABLE IF NOT EXISTS %s("
                    + " idx bigint(20),"
                    + " bucket VARCHAR(256) NOT NULL DEFAULT '' COMMENT 'bucket name',"
                    + " objKey VARBINARY(2048) COMMENT 'Object key'," 
                    + " objid VARCHAR(50) NOT NULL COMMENT 'md5 of bucket/objkey',"
                    + " versionid VARCHAR(50) NOT NULL DEFAULT 'null',"
                    + " uploadid VARCHAR(80) NOT NULL COMMENT 'LifeCycle upload Id',"
                    + " inDate timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'time data in ',"
                    + " log TEXT COMMENT 'failed log',"
                    + " PRIMARY KEY(objid, versionid, uploadid)) ENGINE=INNODB DEFAULT CHARSET=UTF8mb4 COLLATE=utf8mb4_unicode_ci;";
    public static String insertLifeCycleQuery = "INSERT INTO %s(idx, bucket, objKey, objid, versionid, uploadid, inDate, log) VALUES(?, ?, ?, ?, ?, ?, now(), ?)";
    public static String selectByUploadIdLifeCycleQuery = "SELECT idx, bucket, objKey, objid, versionid, uploadid, inDate, log FROM %s WHERE uploadid=?";
    public static String selectLifeCycleQuery = "SELECT idx, bucket, objKey, objid, versionid, uploadid, inDate, log FROM %s WHERE objid=? AND AND versionid=?";
    public static String selectAllLifeCycleQuery = "SELECT idx, bucket, objKey, versionid, uploadid, inDate, log FROM %s";
    public static String deleteLifeCycleQuery = "DELETE FROM %s WHERE objKey=? AND versionid=?";

    // for tags indexing
    public static String tagIndexingTablePrefexi = "_ObjTagIndex";
    
    public  static String createTagIndexingQuery= "CREATE TABLE IF NOT EXISTS %s("
            + " objid VARCHAR(50) NOT NULL, "
            + " versionid VARCHAR(50) NOT NULL DEFAULT 'nil',"
            + " TagKey VARCHAR(191) NOT NULL," 
            + " TagValue VARCHAR(256) NOT NULL,"
            + " PRIMARY KEY(objid, versionid, TagKey)) ENGINE=INNODB DEFAULT CHARSET=UTF8mb4 COLLATE=utf8mb4_unicode_ci;";
    public static String insertTagIndexingQuery = "INSERT INTO %s(objid, versionid, TagKey, TagValue) VALUES(?, ?, ?, ?)";
    public static String deleteTagIndexingQuery1 = "DELETE FROM %s WHERE objid=? AND versionid=?";
    public static String deleteTagIndexingQuery2 = "DELETE FROM %s WHERE objid=? AND versionid=? AND TagKey=?";
    public static String selectTagIndexingQuery = "SELECT objid, versionid, TagKey, TagValue FROM %s WHERE %s";
    
    // for restore objects
    public  static String createRestoreObjectsQuery= "CREATE TABLE IF NOT EXISTS RESTOREOBJECTS("
            + " bucket VARCHAR(256) NOT NULL DEFAULT '' COMMENT 'bucket name',"
            + " objKey VARBINARY(2048) COMMENT 'Object key',"
            + " objid VARCHAR(50) NOT NULL, "
            + " versionid VARCHAR(50) NOT NULL DEFAULT 'nil',"
            + " request TEXT,"
            + " PRIMARY KEY(objid, versionid)) ENGINE=INNODB DEFAULT CHARSET=UTF8mb4 COLLATE=utf8mb4_unicode_ci;";
    public static String insertRestoreObjectsQuery = "INSERT INTO RESTOREOBJECTS(bucket, objKey, objid, versionid, request) VALUES(?, ?, ?, ?, ?)";
    public static String selectRestoreObjectsQuery = "SELECT request FROM RESTOREOBJECTS WHERE objid=? AND versionid=?";
    public static String deleteRestoreObjectsQuery = "DELETE FROM RESTOREOBJECTS WHERE objid=? AND versionid=?";
}
