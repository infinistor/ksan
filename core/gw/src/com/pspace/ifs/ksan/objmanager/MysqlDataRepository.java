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

import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.object.multipart.Multipart;
import com.pspace.ifs.ksan.gw.object.multipart.Part;
import com.pspace.ifs.ksan.gw.object.multipart.ResultParts;
import com.pspace.ifs.ksan.gw.object.multipart.ResultUploads;
import com.pspace.ifs.ksan.gw.object.multipart.Upload;

import java.util.Date;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author legesse
 */
public class MysqlDataRepository implements DataRepository{

    private String url;
    private String username;
    private String passwd;
    private Connection con;
    private ObjManagerCache  obmCache;
    
    // for objects
    private PreparedStatement pstCreate;
    private PreparedStatement pstInsert;
    private PreparedStatement pstDelete;
    private PreparedStatement pstupdateMetadata;
    private PreparedStatement pstSelectOne;
    private PreparedStatement pstSelectOneWithVersionId;
    private PreparedStatement pstSelectList;
    private PreparedStatement pstupdateDisks;
    private PreparedStatement pstupdateSizeTime;
    private PreparedStatement pstupdateLastVersion;
    private PreparedStatement pstupdateLastVersionDelete;
    private PreparedStatement pstUpdateObjectMeta;
    private PreparedStatement pstUpdateTagging;
    private PreparedStatement pstUpdateAcl;
    

    // for buckets
    private PreparedStatement pstCreateBucket;
    private PreparedStatement pstInsertBucket;
    private PreparedStatement pstDeleteBucket;
    private PreparedStatement pstSelectBucket;
    private PreparedStatement pstSelectAllBucket;
    private PreparedStatement pstUpdateBucket;
    //private PreparedStatement pstSelectUsedBucket;
    private PreparedStatement pstSelectUsedDisks;
    //private PreparedStatement pstSelectBucketByName;
    private PreparedStatement pstUpdateBucketAcl;
    private PreparedStatement pstUpdateBucketWeb;
    private PreparedStatement pstUpdateBucketCors;
    private PreparedStatement pstUpdateBucketLifecycle;
    private PreparedStatement pstUpdateBucketAccess;
    private PreparedStatement pstUpdateBucketTagging;
    private PreparedStatement pstUpdateBucketReplication;
    private PreparedStatement pstIsDeleteBucket;
    private PreparedStatement pstUpdateBucketEncryption;
    private PreparedStatement pstUpdateBucketObjectLock;
    private PreparedStatement pstUpdateBucketPolicy;
    private PreparedStatement pstUpdateBucketFilecount;
    private PreparedStatement pstUpdateBucketUsedSpace;
    
// for multipart upload
    private PreparedStatement pstCreateMultiPart;
    private PreparedStatement pstInsertMultiPart;
    private PreparedStatement pstUpdateMultiPart;
    private PreparedStatement pstDeleteMultiPart;
    private PreparedStatement pstSelectMultiPart;
    private PreparedStatement pstGetMultiPart;
    private PreparedStatement pstGetParts;
    private PreparedStatement pstGetPartsMax;
    private PreparedStatement pstGetUploads;
    private PreparedStatement pstIsUpload;
    private PreparedStatement pstIsUploadPartNo;
    
    // for utility
    private PreparedStatement pstCreateUJob;
    private PreparedStatement pstInsertUJob;
    private PreparedStatement pstUpdateUJob1;
    private PreparedStatement pstUpdateUJob2;
    private PreparedStatement pstSelectUJob;

    // for user disk pool map
    private PreparedStatement pstCreateUserDiskPool;
    private PreparedStatement pstInsertUserDiskPool;
    private PreparedStatement pstSelectUserDiskPool;
    private PreparedStatement pstDeleteUserDiskPool;
    
    public MysqlDataRepository(ObjManagerCache  obmCache, String host, String username, String passwd, String dbname) throws SQLException{
        this.obmCache = obmCache;
        this.passwd = passwd;
        this.username = username;
        this.url ="jdbc:mysql://"+ host+":3306/"+ dbname +"?useSSL=false";
        try{
            this.createDB(host, dbname);
            this.connect();
            pstCreate = con.prepareStatement("CREATE TABLE IF NOT EXISTS MDSDBTable("
                    + "bucket VARCHAR(256) NOT NULL, objKey VARCHAR(2048) NOT NULL, size BIGINT NOT NULL default 0,"
                    + "etag VARCHAR(1048) NOT NULL, meta VARCHAR(1048) NOT NULL, tag TEXT NOT NULL, "
                    + "pdiskid VARCHAR(80) NOT NULL, rdiskid VARCHAR(80) NOT NULL, objid VARCHAR(50) NOT NULL, "
                    + "acl VARCHAR(2048) NOT NULL, "
                    + "lastModified BIGINT DEFAULT 0, versionid VARCHAR(50) NOT NULL DEFAULT 'nil', deleteMarker VARCHAR(32) NOT NULL, lastversion BOOLEAN default true, "
                    + "PRIMARY KEY(objid, versionid, deleteMarker)) ENGINE=INNODB DEFAULT CHARSET=UTF8;");  
            pstInsert = con.prepareStatement("INSERT INTO MDSDBTable(bucket, objKey, etag, meta, tag, acl, size, lastModified, pdiskid, rdiskid, objid, versionid, deleteMarker, lastversion) VALUES(?, ?, ?, ?, ?, ?, ?,  9223372036854775808 - ?, ?, ?, ?, ?, ?, true)");
            pstDelete = con.prepareStatement("DELETE FROM MDSDBTable WHERE objid=? AND (versionid=? OR versionid is NULL)");
            //pstDeleteWithVersionId = con.prepareStatement("DELETE FROM MDSDBTable WHERE bucket=? AND objKey=? AND versionid=?");
            pstSelectOne = con.prepareStatement("SELECT bucket, objKey, size, objid, etag, tag, meta, acl, pdiskid, rdiskid, versionid, deleteMarker, lastversion FROM MDSDBTable WHERE objid=? AND lastversion=true");
            pstSelectOneWithVersionId = con.prepareStatement("SELECT bucket, objKey, size, objid, etag, tag, meta, acl, pdiskid, rdiskid, versionid, deleteMarker, lastversion FROM MDSDBTable WHERE objid=? AND versionid=?");
            pstupdateMetadata = con.prepareStatement("UPDATE MDSDBTable SET etag=?, meta=?, tag=?, acl=?, size=?, lastModified=?, pdiskid=?, rdiskid=?, versionid=?, deleteMarker=?, lastversion=? WHERE objid=?");
                    
            pstSelectList = con.prepareStatement("SELECT bucket, objid, etag, tag, meta, pdiskid, rdiskid FROM MDSDBTable WHERE objKey LIKE ?");
            pstupdateDisks = con.prepareStatement("UPDATE MDSDBTable SET pdiskid=?, rdiskid=? WHERE objid=?");
            pstupdateSizeTime = con.prepareStatement("UPDATE MDSDBTable SET size=?, lastModified=? WHERE objid=?");
            pstupdateLastVersion = con.prepareStatement("UPDATE MDSDBTable SET lastversion=false WHERE objid=? AND lastversion=true");
            pstupdateLastVersionDelete = con.prepareStatement("UPDATE MDSDBTable SET lastversion=true WHERE objid=? ORDER BY lastModified asc limit 1");
            pstSelectUsedDisks = con.prepareStatement("SELECT pdiskid as diskid FROM MDSDBTable UNION DISTINCT SELECT rdiskid FROM MDSDBTable;");
            pstIsDeleteBucket = con.prepareStatement("SELECT objKey FROM MDSDBTable WHERE bucket=? LIMIT 1");
            pstUpdateObjectMeta = con.prepareStatement("UPDATE MDSDBTable SET meta=? WHERE objid=? AND versionid=?");
            pstUpdateTagging = con.prepareStatement("UPDATE MDSDBTable SET tag=?, meta=? WHERE objid=? AND versionid=?");
            pstUpdateAcl = con.prepareStatement("UPDATE MDSDBTable SET acl=? WHERE objid=? AND versionid=?");

            // for bucket
            pstCreateBucket = con.prepareStatement("CREATE TABLE IF NOT EXISTS BUCKETS("
                    + "name VARCHAR(256) NOT NULL, "
                    + "id VARCHAR(80) NOT NULL, "
                    + "diskPoolId CHAR(36) NOT NULL, "
                    + " userId CHAR(32), userName VARCHAR(200), "
                    + "acl VARCHAR(2048), web VARCHAR(2048), cors VARCHAR(2048), "
                    + "lifecycle VARCHAR(2048), access VARCHAR(2048), "
                    + "tagging VARCHAR(2048), replication VARCHAR(2048), "
                    + "encryption VARCHAR(2048),   objectlock VARCHAR(2048),  policy VARCHAR(2048), "
                    + "versioning VARCHAR(50), MfaDelete VARCHAR(50), "
                    + "createTime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "replicaCount INT DEFAULT 2, "
                    + "usedSpace BIGINT  NOT NULL DEFAULT 0,  fileCount BIGINT  NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(id)) ENGINE=INNODB DEFAULT CHARSET=UTF8;");
            pstInsertBucket = con.prepareStatement("INSERT INTO BUCKETS(name, id, diskPoolId, userName, userId, acl, encryption, objectlock, replicaCount) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            pstDeleteBucket = con.prepareStatement("DELETE FROM BUCKETS WHERE id=?");
            pstSelectBucket = con.prepareStatement("SELECT name, id, diskPoolId, versioning, MfaDelete, userName, userId, acl, web, cors, lifecycle, access, tagging, replication, encryption, objectlock, policy, createTime, replicaCount, usedSpace, fileCount FROM BUCKETS WHERE id=?");
            pstSelectAllBucket = con.prepareStatement("SELECT name, id, diskPoolId, versioning, MfaDelete, userName, userId, acl, web, cors, lifecycle, access, tagging, replication, encryption, objectlock, policy, createTime, replicaCount, usedSpace, fileCount FROM BUCKETS");
            pstUpdateBucket = con.prepareStatement("UPDATE BUCKETS SET versioning=? WHERE id=?");
            
            pstUpdateBucketAcl = con.prepareStatement("UPDATE BUCKETS SET acl=? WHERE id=?");
            pstUpdateBucketWeb = con.prepareStatement("UPDATE BUCKETS SET web=? WHERE id=?");
            pstUpdateBucketCors = con.prepareStatement("UPDATE BUCKETS SET cors=? WHERE id=?");
            pstUpdateBucketLifecycle = con.prepareStatement("UPDATE BUCKETS SET lifecycle=? WHERE id=?");
            pstUpdateBucketAccess = con.prepareStatement("UPDATE BUCKETS SET access=? WHERE id=?");
            pstUpdateBucketTagging = con.prepareStatement("UPDATE BUCKETS SET tagging=? WHERE id=?");
            pstUpdateBucketReplication = con.prepareStatement("UPDATE BUCK SET replication=? WHERE id=?");
            pstUpdateBucketEncryption = con.prepareStatement("UPDATE BUCKETS SET encryption=? WHERE id=?");
            pstUpdateBucketObjectLock = con.prepareStatement("UPDATE BUCKETS SET objectlock=? WHERE id=?");
            pstUpdateBucketPolicy = con.prepareStatement("UPDATE BUCKETS SET policy=? WHERE id=?");
            pstUpdateBucketFilecount = con.prepareStatement("UPDATE BUCKETS SET fileCount = fileCount + ? WHERE id=?");
            pstUpdateBucketUsedSpace = con.prepareStatement("UPDATE BUCKETS SET usedSpace = usedSpace + ? WHERE id=?");
            
            // for multipart
            pstCreateMultiPart= con.prepareStatement("CREATE TABLE IF NOT EXISTS MULTIPARTS("
                    + " bucket VARCHAR(256) NOT NULL DEFAULT '' COMMENT 'bucket name',"
                    + " objKey VARCHAR(2048) COMMENT 'Object key'," 
                    + " changeTime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'time upload started',"
                    + " completed BOOLEAN DEFAULT false COMMENT 'job completed or in-progress',"
                    + " uploadid VARCHAR(80) NOT NULL COMMENT 'multi-part upload Id',"
                    + " acl VARCHAR(2048),"
                    + " meta VARCHAR(1024),"
                    + " etag VARCHAR(64),"
                    + " size bigint(20),"
                    + " partNo INT NOT NULL COMMENT 'part sequence number',"
                    + " pdiskid VARCHAR(80) NOT NULL, "
                    + " PRIMARY KEY(uploadid, partNo), INDEX index_objkey(objkey)) ENGINE=INNODB DEFAULT CHARSET=UTF8;");
            pstInsertMultiPart = con.prepareStatement("INSERT INTO MULTIPARTS(bucket, objKey, uploadid, partNo, acl, meta, etag, size, pdiskid, changeTime) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, now())");
            pstUpdateMultiPart = con.prepareStatement("UPDATE MULTIPARTS SET completed=?, changeTime=now() WHERE uploadid=? and partNo=?");
            pstDeleteMultiPart = con.prepareStatement("DELETE FROM MULTIPARTS WHERE uploadid=?");
            pstSelectMultiPart = con.prepareStatement("SELECT bucket, objKey, uploadid, partNo FROM MULTIPARTS WHERE uploadid=? AND  partNo > ? ORDER BY partNo LIMIT ? ");

            pstGetMultiPart = con.prepareStatement("SELECT bucket, objKey, changeTime, uploadid, acl, meta, pdiskid FROM MULTIPARTS WHERE uploadid=? AND  partNo = 0");
            pstGetParts = con.prepareStatement("SELECT changeTime, etag, size, partNo, pdiskid FROM MULTIPARTS WHERE uploadid=? AND  partNo != 0");
            pstGetPartsMax = con.prepareStatement("SELECT changeTime, etag, size, partNo, pdiskid FROM MULTIPARTS WHERE uploadid=? AND partNo > ? ORDER BY partNo LIMIT ?");
            pstGetUploads = con.prepareStatement("SELECT objKey, changeTime, uploadid, meta FROM MULTIPARTS WHERE bucket=? AND partNo = 0 AND completed=false ORDER BY partNo LIMIT ? ");
            pstIsUpload = con.prepareStatement("SELECT bucket FROM MULTIPARTS WHERE uploadid=?");
            pstIsUploadPartNo = con.prepareStatement("SELECT bucket, objKey FROM MULTIPARTS WHERE uploadid=? AND partNo=?");

           
            //String Id, String status, long TotalNumObject, boolean checkOnly, String utilName
            pstCreateUJob = con.prepareStatement("CREATE TABLE IF NOT EXISTS UTILJOBS(Id VARCHAR(15) NOT NULL PRIMARY KEY, "
                    + "status VARCHAR(20) NOT NULL, TotalNumObject BIGINT NOT NULL default 0, "
                    + "NumJobDone BIGINT NOT NULL default 0, checkOnly BOOLEAN DEFAULT false, "
                    + "utilName VARCHAR(256) NOT NULL, startTime DATETIME )");
            pstInsertUJob = con.prepareStatement("INSERT INTO UTILJOBS(Id, status, TotalNumObject, checkOnly, utilName, startTime) VALUES(?, ?, ?, ?, ?, now())");
            pstUpdateUJob1 = con.prepareStatement("UPDATE UTILJOBS SET status=? WHERE Id=?");
            pstUpdateUJob2 = con.prepareStatement("UPDATE UTILJOBS SET TotalNumObject=?, NumJobDone=? WHERE Id=?");
            pstSelectUJob = con.prepareStatement("SELECT status, TotalNumObject, NumJobDone, checkOnly, utilName, startTime FROM UTILJOBS WHERE Id=?");

            // for user disk pool table
            pstCreateUserDiskPool = con.prepareStatement("CREATE TABLE IF NOT EXISTS USERSDISKPOOL(userId VARCHAR(50) NOT NULL, "
                    + "credential VARCHAR(80) NOT NULL COMMENT 'access key _ secret key', diskpoolId VARCHAR(50) NOT NULL, "
                    + "replcaCount INT NOT NULL, PRIMARY KEY(userId, credential)) ENGINE=INNODB DEFAULT CHARSET=UTF8;");
            pstInsertUserDiskPool = con.prepareStatement("INSERT INTO USERSDISKPOOL(userId, credential, diskpoolId, replcaCount) VALUES(?, ?, ?, ?)");
            
            pstSelectUserDiskPool = con.prepareStatement("SELECT diskpoolId, replcaCount FROM USERSDISKPOOL WHERE userId=?");
            
            pstDeleteUserDiskPool = con.prepareStatement("DELETE FROM USERSDISKPOOL WHERE userId=? AND diskpoolId=?");
            
        } catch(SQLException ex){
            this.ex_message(ex);
        }
        
        this.createTable();
    }
    
    private int createDB(String host, String dbname){
        Connection connC = null;
        Statement stmt = null;
        try {    
            Class.forName("com.mysql.jdbc.Driver");
            connC= DriverManager.getConnection("jdbc:mysql://" + host+"/", this.username, this.passwd);
            stmt = connC.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS "+ dbname+ ";");
            return 0;
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            try {
                if (stmt != null)
                    stmt.close();
                if (connC != null)
                    connC.close();
            } catch (SQLException ex) {
                Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return -1;
    }
    
    private int connect(){
        try{
           Class.forName("com.mysql.jdbc.Driver");
           //System.out.println(">>>url" + this.url);
           this.con = DriverManager.getConnection(this.url, this.username, this.passwd); 
        } catch(SQLException ex){
            this.ex_message(ex);
            return -1;
        } catch (ClassNotFoundException ex) { 
            Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }
    
    private void ex_message(SQLException ex){
        Logger lgr = Logger.getLogger(MysqlDataRepository.class.getName());
        lgr.log(Level.SEVERE, ex.getMessage(), ex);
    }
    
    private int createTable() throws SQLException{
        //try{
            this.pstCreate.execute();
            this.pstCreateBucket.execute();
            this.pstCreateMultiPart.execute();
            pstCreateUJob.execute();
            pstCreateUserDiskPool.execute();
        /*} catch(SQLException ex){
            this.ex_message(ex);
            return -ex.getErrorCode();
        }*/
        return 0;
    }
    
    private int updateVersion(String bucketName, String key) throws SQLException{        
        pstupdateLastVersion.clearParameters();
        pstupdateLastVersion.setString(1, key);
        pstupdateLastVersion.execute();
        return 0;
    }
    

    private int updateMetadata(Metadata md){
        try {
            pstupdateMetadata.clearParameters();
            pstupdateMetadata.setString(1, md.getEtag());
            pstupdateMetadata.setString(2, md.getMeta());
            pstupdateMetadata.setString(3, md.getTag());
            pstupdateMetadata.setString(4, md.getAcl());
            pstupdateMetadata.setLong(5, md.getSize());
            pstupdateMetadata.setLong(6, md.getLastModified());
            pstupdateMetadata.setString(7, md.getPrimaryDisk().getId());
            try{
                if (md.isReplicaExist())
                    pstupdateMetadata.setString(8, md.getReplicaDisk().getId());
                else 
                   pstupdateMetadata.setString(8,  ""); 
            } catch(ResourceNotFoundException ex){
               pstupdateMetadata.setString(8,  "");
            }
            pstupdateMetadata.setString(9, md.getVersionId());
            pstupdateMetadata.setString(10, md.getDeleteMarker());
            pstupdateMetadata.setBoolean(11, md.getLastVersion());
            pstupdateMetadata.setString(12, md.getObjId());
            pstupdateMetadata.execute();
        } catch (SQLException ex) {
            ex_message( ex);
            return -1;
        }
        return 0;
    }
    
    private List<Object> getUtilJobObject(String Id, String status, long TotalNumObject, 
            long NumJobDone, boolean checkOnly, String utilName, String startTime){
        List<Object> res = new ArrayList<>();
        res.add(Id); // ID
        res.add(status); // status
        res.add(TotalNumObject);   // TotalNumObject
        res.add(NumJobDone);   // NumJobDone
        res.add(checkOnly);   // checkOnly
        res.add(utilName); // utilName
        res.add(startTime); // startTime
        return res;
    }
    
    private List<Object> insertUtilJob(String Id, String status, long TotalNumObject, boolean checkOnly, String utilName) throws SQLException{
        pstInsertUJob.clearParameters();
        pstInsertUJob.setString(1, Id);
        pstInsertUJob.setString(2, status);
        pstInsertUJob.setLong(3, TotalNumObject);
        pstInsertUJob.setBoolean(4, checkOnly);
        pstInsertUJob.setString(5, utilName);
        pstInsertUJob.execute();
        return getUtilJobObject(Id, status, TotalNumObject, 0, checkOnly, utilName, "");
    }
    
    //status, TotalNumObject, NumJobDone, checkOnly, utilName, startTime
    private List<Object> selectUtilJob(String Id) throws SQLException{
        List<Object> res = new ArrayList<>();
        pstSelectUJob.clearParameters();
        pstSelectUJob.setString(1, Id);
        ResultSet rs = pstSelectUJob.executeQuery();
        if(rs.next()){
            res = getUtilJobObject(Id, rs.getString(1), rs.getLong(2), rs.getLong(3), 
                    rs.getBoolean(4), rs.getString(5), rs.getString(6));
                System.out.format("==>Id : %s status : %s TotalNumObject : %d checkOnly : %s utilName : %s \n", Id, rs.getString(1), rs.getLong(2), rs.getBoolean(4), rs.getString(5)); 
        }
        return res;
    }
    
    private List<Object> updateStatusUtilJob(String Id, String status) throws SQLException{
        pstUpdateUJob1.clearParameters();
        pstUpdateUJob1.setString(1, status);
        pstUpdateUJob1.setString(2, Id);
        pstUpdateUJob1.execute();
        return getUtilJobObject(Id, status, 0, 0, false, " ", "");
    }
    
    private List<Object> updateNumberJobsUtilJob(String Id, long TotalNumObject, long NumJobDone) throws SQLException{
        pstUpdateUJob2.clearParameters();
        pstUpdateUJob2.setLong(1, TotalNumObject);
        pstUpdateUJob2.setLong(2, NumJobDone);
        pstUpdateUJob2.setString(3, Id);
        pstUpdateUJob2.execute();
        return getUtilJobObject(Id, "", TotalNumObject, NumJobDone, false, "", "");
    }
    
    @Override
    public synchronized int insertObject(Metadata md) throws ResourceNotFoundException{
        try{
            this.pstInsert.clearParameters();
            this.pstInsert.setString(1, md.getBucket());
            this.pstInsert.setString(2, md.getPath());
            this.pstInsert.setString(3, md.getEtag());
            this.pstInsert.setString(4, md.getMeta());
            this.pstInsert.setString(5, md.getTag());
            this.pstInsert.setString(6, md.getAcl());
            this.pstInsert.setLong(7, md.getSize());
            this.pstInsert.setLong(8, md.getLastModified());
            this.pstInsert.setString(9, md.getPrimaryDisk().getId());
            if (md.isReplicaExist()){
                this.pstInsert.setString(10, md.getReplicaDisk().getId());
            } else {
                this.pstInsert.setString(10, "");
            }
            this.pstInsert.setString(11, md.getObjId());
            this.pstInsert.setString(12, md.getVersionId());
            this.pstInsert.setString(13, md.getDeleteMarker());
            
            // If the same object exists, change lastversion to false.
            updateVersion(md.getBucket(), md.getObjId());
            
            //this.pstInsert.setBoolean(12, true);
            // if (md.getVersionId().isEmpty())
            //     updateVersion(md.getBucket(), md.getObjId());
            
            if (pstInsert.executeUpdate() > 0)
                updateBucketFileCount(md.getBucket(), 1);
            
        } catch(SQLException ex){
            if (ex.getErrorCode() == 1062)
                return updateMetadata(md);
            
            System.out.println(this.pstInsert.toString());
            System.out.println("=> Path : " + md.getPath() + " objid : " + md.getObjId() + " len " + md.getObjId().length());
            this.ex_message(ex);
            return -1;
        }
        return 0;
    }
    
    @Override
    public synchronized int updateDisks(Metadata md) {
        try {
            pstupdateDisks.clearParameters();
            pstupdateDisks.setString(1, md.getPrimaryDisk().getId());
            try {
                pstupdateDisks.setString(2, md.getReplicaDisk().getId());
            } catch (ResourceNotFoundException ex) {
                pstupdateDisks.setString(2, "");
            }
            pstupdateDisks.setString(3, md.getObjId());
            return pstupdateDisks.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    @Override
    public synchronized int updateSizeTime(Metadata md) {
        try {
            pstupdateSizeTime.clearParameters();
            pstupdateSizeTime.setLong(1, md.getSize());
            pstupdateSizeTime.setLong(2, md.getLastModified());
            pstupdateSizeTime.setString(3, md.getObjId());
            return pstupdateSizeTime.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public synchronized int selectMultiple(String bucketName, String path, String[] objid, String[] etag, String[] tag, String[] meta, long[] pdiskid, long[] rdiskid){
        int idx  = 0;
        try{
            this.pstSelectList.setString(1, path);
            ResultSet rs = this.pstSelectList.executeQuery();
            while(rs.next()){
                objid[idx]   = rs.getString(1);
                etag[idx]    = rs.getString(2);
                tag[idx]     = rs.getString(3);
                meta[idx]    = rs.getString(4);
                pdiskid[idx] = rs.getLong(5);
                rdiskid[idx] = rs.getLong(6);
                idx++;
            }
            
        } catch(SQLException ex){
            this.ex_message(ex);
           return -ex.getErrorCode();
        }
        return 0;
    }
    
    private Metadata getSelectObjectResult(String diskPoolId, ResultSet rs) throws SQLException, ResourceNotFoundException{
        Metadata mt;
        DISK pdsk;
        DISK rdsk;
        String rdiskPath;
        
        while(rs.next()){
            pdsk = this.obmCache.getDiskWithId(diskPoolId, rs.getString(9));
            try{
                rdiskPath = rs.getString(10);
                if (!rdiskPath.isEmpty())
                    rdsk = this.obmCache.getDiskWithId(diskPoolId, rs.getString(10));
                else
                    rdsk = new DISK();
            } catch(ResourceNotFoundException ex){
                rdsk = new DISK();
            }
            mt = new Metadata(rs.getString(1), rs.getString(2));
            mt.setSize(rs.getLong(3));
            mt.setEtag(rs.getString(5));
            mt.setTag(rs.getString(6));
            mt.setMeta(rs.getString(7));
            mt.setAcl(rs.getString(8));
            mt.setPrimaryDisk(pdsk);
            mt.setReplicaDISK(rdsk);
            mt.setVersionId(rs.getString(11), rs.getString(12), rs.getBoolean(13));
            return mt;
        }
        throw new ResourceNotFoundException("path not found in the db");
    }
    
    private synchronized Metadata selectSingleObjectInternal(String diskPoolId, String objId) throws ResourceNotFoundException {
        try{
            this.pstSelectOne.clearParameters();
            this.pstSelectOne.setString(1, objId);
            ResultSet rs = this.pstSelectOne.executeQuery();
            return getSelectObjectResult(diskPoolId, rs);
        } catch(SQLException ex){
            System.out.println(" error : " + ex.getMessage());
            this.ex_message(ex);
            throw new ResourceNotFoundException("path not found in the db : " + ex.getMessage());
        }
    }

    private synchronized Metadata selectSingleObjectInternal(String diskPoolId, String objId, String versionId) throws ResourceNotFoundException {
        try{
            this.pstSelectOneWithVersionId.clearParameters();
            this.pstSelectOneWithVersionId.setString(1, objId);
            this.pstSelectOneWithVersionId.setString(2, versionId);
            ResultSet rs = this.pstSelectOneWithVersionId.executeQuery();
            return getSelectObjectResult(diskPoolId, rs);      
        } catch(SQLException ex){
            System.out.println(" error : " + ex.getMessage());
            this.ex_message(ex);
            throw new ResourceNotFoundException("path not found in the db : " + ex.getMessage());
        }
    }
    
    @Override
    public synchronized Metadata selectSingleObject(String diskPoolId, String bucketName, String path) throws ResourceNotFoundException {
        Metadata mt = new Metadata(bucketName, path);
       return selectSingleObjectInternal(diskPoolId, mt.getObjId()); 
    }
    
    @Override
    public synchronized Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objid) throws ResourceNotFoundException {
       return selectSingleObjectInternal(diskPoolId, objid); 
    }
    
    @Override
    public synchronized void selectObjects(String bucketName, Object query, int maxKeys, DBCallBack callback) throws SQLException {
        String key;
        String etag;
        String lastModified;
        long size;
        String versionId;
        String pdiskid;
        String rdiskid;
        boolean lastVersion;
        int nunColumns;
        PreparedStatement stmt = this.con.prepareStatement(query.toString());
        //stmt.setLargeMaxRows(1000);
        ResultSet rs = stmt.executeQuery();
        
        while(rs.next()){
            nunColumns=(rs.getMetaData()).getColumnCount();
            key =          nunColumns < 1 ? " " : rs.getString(1) ;
            etag=          nunColumns < 2 ? " " : rs.getString(2);
            lastModified = nunColumns < 3 ? " " : rs.getString(3);
            size =         nunColumns < 4 ?  0  : rs.getLong(4);
            versionId =    nunColumns < 5 ? " " : rs.getString(5);
            pdiskid   =    nunColumns < 6 ? " " : rs.getString(6);
            rdiskid =      nunColumns < 7 ? " " : rs.getString(7);
            lastVersion=   nunColumns < 8 ? false : rs.getBoolean(8);
            callback.call(key,
                    etag, 
                    lastModified, 
                    size, 
                    versionId, 
                    pdiskid, 
                    rdiskid,
                    lastVersion);
        }
    }
         
    @Override
    public synchronized List<String> getAllUsedDiskId() throws SQLException{
        List<String> dList = new ArrayList();

        ResultSet rs = this.pstSelectUsedDisks.executeQuery();

        while(rs.next()){
            dList.add(rs.getString(1));
        }
        return dList;
    }
    
    @Override
    public void updateObjectTagging(Metadata mt) throws SQLException {
        pstUpdateTagging.clearParameters();
        pstUpdateTagging.setString(1, mt.getTag());
        pstUpdateTagging.setString(2, mt.getMeta());
        pstUpdateTagging.setString(3, mt.getObjId());
        pstUpdateTagging.setString(4, mt.getVersionId());
        pstUpdateTagging.executeUpdate();
    }
    
    @Override
    public void updateObjectAcl(Metadata mt) throws SQLException {
        pstUpdateAcl.clearParameters();
        pstUpdateAcl.setString(1, mt.getAcl());
        pstUpdateAcl.setString(2, mt.getObjId());
        pstUpdateAcl.setString(3, mt.getVersionId());
        pstUpdateAcl.executeUpdate();
    }

    @Override
    public synchronized Bucket insertBucket(Bucket bt) 
            throws ResourceAlreadyExistException{
        try{
            this.pstInsertBucket.clearParameters();
            this.pstInsertBucket.setString(1, bt.getName());
            this.pstInsertBucket.setString(2, bt.getId());
            this.pstInsertBucket.setString(3, bt.getDiskPoolId());
            this.pstInsertBucket.setString(4, bt.getUserName());
            this.pstInsertBucket.setString(5, bt.getUserId());
            this.pstInsertBucket.setString(6, bt.getAcl());
            this.pstInsertBucket.setString(7, bt.getEncryption());
            this.pstInsertBucket.setString(8, bt.getObjectLock());
            this.pstInsertBucket.setInt(9, bt.getReplicaCount());
            this.pstInsertBucket.executeUpdate();
            getUserDiskPool(bt); // get diskpoolId and replicaCount
        } catch(SQLException ex){
            throw new ResourceAlreadyExistException(String.format("Bucket(%s) is laready exist in the db!\n", bt.getName()), ex);
        }
        return bt;
    }
    
    private String getBucketId(String bucketName){
        //return new Metadata(bucketName, "/").getBucketId();
        Bucket bt = new Bucket();
        bt.setName(bucketName);
        return bt.getId();
    } 
    
    @Override
    public synchronized int deleteBucket(String bucketName){
        try{
            this.pstDeleteBucket.clearParameters();
            this.pstDeleteBucket.setString(1, getBucketId(bucketName));
            this.pstDeleteBucket.executeUpdate();
        } catch(SQLException ex){
            this.ex_message(ex);
            return -ex.getErrorCode();
        }
        return 0;
    }
    
    private Bucket parseBucket(ResultSet rs) throws SQLException{
        String name = rs.getString(1);
        String id = rs.getString(2); 
        String diskPoolId = rs.getString(3); 
        String versioning = rs.getString(4);
        String mfaDelete = rs.getString(5);
        String userName = rs.getString(6);
        String userId = rs.getString(7);
        String acl = rs.getString(8);
        String web = rs.getString(9);
        String cors = rs.getString(10);
        String lifecycle = rs.getString(11);
        String access = rs.getString(12);
        String tagging = rs.getString(13);
        String replication = rs.getString(14);
        String encryption = rs.getString(15);
        String objectlock = rs.getString(16);
        String policy = rs.getString(17);
        Date createTime = (Date)rs.getObject(18);
        int replicaCount = rs.getInt(19);
        long usedSpace = rs.getLong(20);
        long fileCount = rs.getLong(21);
        
        Bucket bt = new Bucket(name, id, diskPoolId, versioning, mfaDelete, userId, acl, createTime);
        bt.setUserName(userName);
        bt.setWeb(web);
        bt.setCors(cors);
        bt.setLifecycle(lifecycle);
        bt.setAccess(access);
        bt.setTagging(tagging);
        bt.setReplication(replication);
        bt.setEncryption(encryption);
        bt.setObjectLock(objectlock);
        bt.setPolicy(policy);
        bt.setReplicaCount(replicaCount);
        getUserDiskPool(bt); // get diskpoolId and replicaCount
        bt.setUsedSpace(usedSpace);
        bt.setFileCount(fileCount);
        return bt;
    }
    
    @Override
    public synchronized Bucket selectBucket(String bucketName) throws ResourceNotFoundException, SQLException{
        
        this.pstSelectBucket.clearParameters();
        this.pstSelectBucket.setString(1, getBucketId(bucketName));
        ResultSet rs = this.pstSelectBucket.executeQuery();

        while(rs.next()){
           return parseBucket(rs);
        }
        throw new ResourceNotFoundException("Bucket("+bucketName+") is not found in the db");
    }
    
    @Override
    public synchronized void loadBucketList() {
        try {
            Bucket bt;
            
            ResultSet rs = this.pstSelectAllBucket.executeQuery();
            
            while(rs.next()){
                bt = parseBucket(rs);
                System.out.println("bucketList>>" + bt);
                obmCache.setBucketInCache(bt);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, "failed to loadBucketList due to sql error!", ex);
        }
        
    }
    
    @Override
    public synchronized List<Bucket> getBucketList() {
        try {
            List<Bucket> btList = new ArrayList();
            
            ResultSet rs = this.pstSelectAllBucket.executeQuery();
            
            while(rs.next()){
                Bucket bt = parseBucket(rs);
                btList.add(bt);
            }
            return btList;
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, "failed to loadBucketList due to sql error!", ex);
        }
        return null;
    }
    
    @Override
    public synchronized int updateBucketVersioning(Bucket bt) {
        try {
            this.pstUpdateBucket.clearParameters();
            pstUpdateBucket.setString(1, bt.getVersioning());
            pstUpdateBucket.setString(2, getBucketId(bt.getName()));
            return pstUpdateBucket.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    @Override
    public  synchronized int insertMultipartUpload(String bucket, String objkey, String uploadid, int partNo, String acl, String meta, String etag, long size, String pdiskid) throws SQLException{
        pstInsertMultiPart.clearParameters();
        pstInsertMultiPart.setString(1, bucket);
        pstInsertMultiPart.setString(2, objkey);
        pstInsertMultiPart.setString(3, uploadid);
        pstInsertMultiPart.setInt(4, partNo);
        pstInsertMultiPart.setString(5, acl);
        pstInsertMultiPart.setString(6, meta);
        pstInsertMultiPart.setString(7, etag);
        pstInsertMultiPart.setLong(8, size);
        pstInsertMultiPart.setString(9, pdiskid);
        pstInsertMultiPart.execute();
        return 0;
    }
  
    @Override
    public synchronized int updateMultipartUpload(String bucket,  String uploadid, int partNo, boolean iscompleted) throws SQLException{
        pstUpdateMultiPart.clearParameters();
        pstUpdateMultiPart.setBoolean(1, iscompleted);
        pstUpdateMultiPart.setString(2, uploadid);
        pstUpdateMultiPart.setInt(3, partNo);
        pstUpdateMultiPart.execute();
        return 0;
    }
    
    @Override
    public synchronized int deleteMultipartUpload(String bucket,  String uploadid) throws SQLException{
        pstDeleteMultiPart.clearParameters();
        pstDeleteMultiPart.setString(1, uploadid);
        pstDeleteMultiPart.execute();
        return 0;
    }
      
    @Override
    public synchronized List<Integer> selectMultipart(String bucket, String uploadid, int maxParts, int partNoMarker) throws SQLException{
        List<Integer> list=new ArrayList<>();
        pstSelectMultiPart.clearParameters();
        pstSelectMultiPart.setString(1, uploadid);
        pstSelectMultiPart.setInt(2, partNoMarker);
        pstSelectMultiPart.setInt(3, maxParts + 1);
        ResultSet rs = pstSelectMultiPart.executeQuery();

        while(rs.next()){
            list.add(rs.getInt(4));
        }
        return list;
    }
    
    @Override
    public synchronized void selectMultipartUpload(String bucketName, Object query, int maxKeys, DBCallBack callback) throws SQLException {
        String key;
        String uploadid;
        long partNo;
        int counter = 0;
        boolean isTrancated =false;
        
        PreparedStatement stmt = this.con.prepareStatement(query.toString());
   
        ResultSet rs = stmt.executeQuery();
        while(rs.next()){
            key =          rs.getString(1) ;
            uploadid=      rs.getString(2);
            partNo =       rs.getInt(3);
            if (++counter == maxKeys)
                isTrancated = !rs.isLast();
            callback.call(key, uploadid, "", partNo, "", "", "", isTrancated);
        }
    }
    
    @Override
    public Metadata getObjectWithUploadIdPart(String diskPoolId, String uploadId, int partNo) throws SQLException{
        pstIsUploadPartNo.clearParameters();
        pstIsUploadPartNo.setString(1, uploadId);
        pstIsUploadPartNo.setInt(2, partNo);
        ResultSet rs = pstIsUploadPartNo.executeQuery();
        if (!rs.next()){
            return null;
        }
        
        String bucketName = rs.getString(1);
        String objkey     = rs.getString(2);
        Metadata mt;
        try {
            mt = selectSingleObject(diskPoolId, bucketName, objkey);
        } catch (ResourceNotFoundException ex) {
            return null;
        }
        return mt;
    }
    /*@Override
    public ObjectListParameter selectObjects(String bucketName, Object query, int maxKeys) throws SQLException {
        PreparedStatement stmt = this.con.prepareStatement(query.toString());
        //stmt.setLargeMaxRows(1000);
        ResultSet rs = stmt.executeQuery();
        
        int numRows = 0;
        ObjectListParameter list = new ObjectListParameter();

        list.setIstruncated(false);

        while(rs.next()){
            numRows++;
            if (numRows > maxKeys) {
                list.setNextMarker(rs.getString(1));
                list.setIstruncated(true);
                break;
            }
            
            S3Metadata meta = null;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                meta = objectMapper.readValue(rs.getString(9), S3Metadata.class);
            } catch (Exception e) {
                return null;
            }
            
            // S3Metadata meta = new S3Metadata();
            meta.setName(rs.getString(1));
            meta.setETag(rs.getString(2));
            meta.setLastModified((Date)rs.getObject(3));
            // meta.setSize(rs.getLong(4));
            meta.setVersionId(rs.getString(5));
            meta.setIsLatest(rs.getBoolean(8) ? "true" : "false");

            list.getObjects().add(meta);
        }

        return list;
    }*/

    private int updateVersionDelete(String objId) throws SQLException{        
        pstupdateLastVersionDelete.clearParameters();
        pstupdateLastVersionDelete.setString(1, objId);
        pstupdateLastVersionDelete.execute();
        return 0;
    }

    @Override
    public int deleteObject(String bucketName, String path, String versionId) {
        try{
            String objId = new Metadata(bucketName, path).getObjId();
            pstDelete.clearParameters();
            pstDelete.setString(1, objId);
            pstDelete.setString(2, versionId);
            if (pstDelete.executeUpdate()> 0)
                updateBucketFileCount(bucketName, -1);
            
            /*if (versionId != null){
                if (!versionId.isEmpty())
                    updateVersionDelete(objId);
            }*/
        } catch(SQLException ex){
            this.ex_message(ex);
            return -ex.getErrorCode();
        }
        return 0;
    }

    @Override
    public Metadata selectSingleObject(String diskPoolId, String bucketName, String path, String versionId)
            throws ResourceNotFoundException {
        Metadata mt = new Metadata(bucketName, path);
        return selectSingleObjectInternal(diskPoolId, mt.getObjId(), versionId); 
    }

    /***********************START*******************************************************************/
    // TO BE
    @Override
    public Multipart getMulipartUpload(String uploadid) throws SQLException {
        Multipart multipart = null;
        String pdiskid;

        this.pstGetMultiPart.clearParameters();
        this.pstGetMultiPart.setString(1, uploadid);
        ResultSet rs = this.pstGetMultiPart.executeQuery();
    
        if (rs.next()) {
            multipart = new Multipart(rs.getString(1), rs.getString(2), rs.getString(4));
            multipart.setLastModified((Date)rs.getObject(3));
            multipart.setAcl(rs.getString(5));
            multipart.setMeta(rs.getString(6));
            pdiskid = rs.getString(7);
            //multipart.setDiskID(pdiskid);
        }
        
        return multipart;
    }

    // TO BE
    @Override
    public SortedMap<Integer, Part> getParts(String uploadId) throws SQLException {
        SortedMap<Integer, Part> listPart = new TreeMap<>();
        this.pstGetParts.clearParameters();
        this.pstGetParts.setString(1, uploadId);
        ResultSet rs = this.pstGetParts.executeQuery();

        while (rs.next()) {
            Part part = new Part();
            part.setLastModified((Date)rs.getObject(1));
            part.setPartETag(rs.getString(2));
            part.setPartSize(rs.getLong(3));
            part.setPartNumber(rs.getInt(4));
            part.setDiskID(rs.getString(5));
            listPart.put(part.getPartNumber(), part);
        }

        return listPart;
    }

    // TO BE
    @Override
    public ResultParts getParts(String uploadId, int partNumberMarker, int maxParts) throws SQLException {
        ResultParts resultParts = new ResultParts(uploadId, maxParts);
        resultParts.setListPart(new TreeMap<Integer, Part>());
        
        pstGetPartsMax.clearParameters();
        pstGetPartsMax.setString(1, uploadId);
        /*if (Strings.isNullOrEmpty(partNumberMarker)) {
            pstGetPartsMax.setInt(2, 0);
        } else*/ {
            pstGetPartsMax.setInt(2, Integer.valueOf(partNumberMarker));
        }
        pstGetPartsMax.setInt(3, maxParts + 1);
        ResultSet rs = pstGetPartsMax.executeQuery();

        resultParts.setTruncated(false);
        int count = 0;
        while (rs.next()) {
            count++;
            if (count > maxParts) {
                resultParts.setPartNumberMarker(String.valueOf(rs.getInt(4)));
                resultParts.setTruncated(true);
                break;
            }
            Part part = new Part();
            part.setLastModified((Date)rs.getObject(1));
            part.setPartETag(rs.getString(2));
            part.setPartSize(rs.getLong(3));
            part.setPartNumber(rs.getInt(4));
            part.setDiskID(rs.getString(5));
            resultParts.getListPart().put(part.getPartNumber(), part);
        }

        return resultParts;
    }

    // TO BE
    @Override
    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException, GWException {
        ResultUploads resultUploads = new ResultUploads();
        resultUploads.setList(new ArrayList<Upload>());

        // need to make query with delimiter, prefix, keyMarker
        pstGetUploads.clearParameters();
        this.pstGetUploads.setString(1, bucket);
        this.pstGetUploads.setInt(2, maxUploads + 1);
        ResultSet rs = this.pstGetUploads.executeQuery();

        resultUploads.setTruncated(false);
        int count = 0;
        while (rs.next()) {
            count++;
            if (count > maxUploads) {
                resultUploads.setKeyMarker(rs.getString(1));
                resultUploads.setUploadIdMarker(rs.getString(3));
                resultUploads.setTruncated(true);
                break;
            }
            Upload upload = new Upload(rs.getString(1), (Date)rs.getObject(2), rs.getString(3), rs.getString(4));
            resultUploads.getList().add(upload);
        }
        return resultUploads;
    }
/***********************************************END*****************************************************************************/
    @Override
    public void updateObjectMeta(Metadata mt) throws SQLException {
        pstUpdateObjectMeta.clearParameters();
        pstUpdateObjectMeta.setString(1, mt.getMeta());
        pstUpdateObjectMeta.setString(2, mt.getObjId());
        pstUpdateObjectMeta.setString(3, mt.getVersionId());
        pstUpdateObjectMeta.executeUpdate();
    }

    @Override
    public void updateBucketAcl(Bucket bt) throws SQLException {
        pstUpdateBucketAcl.clearParameters();
        pstUpdateBucketAcl.setString(1, bt.getAcl());
        pstUpdateBucketAcl.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketAcl.executeUpdate();
       
    }

    @Override
    public void updateBucketCors(Bucket bt) throws SQLException {
        pstUpdateBucketCors.clearParameters();
        pstUpdateBucketCors.setString(1, bt.getCors());
        pstUpdateBucketCors.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketCors.executeUpdate();
    }

    @Override
    public void updateBucketWeb(Bucket bt) throws SQLException {
        pstUpdateBucketWeb.clearParameters();
        pstUpdateBucketWeb.setString(1, bt.getWeb());
        pstUpdateBucketWeb.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketWeb.executeUpdate();
      
    }

    @Override
    public void updateBucketLifecycle(Bucket bt) throws SQLException {
        pstUpdateBucketLifecycle.clearParameters();
        pstUpdateBucketLifecycle.setString(1, bt.getLifecycle());
        pstUpdateBucketLifecycle.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketLifecycle.executeUpdate();
        
    }

    @Override
    public void updateBucketAccess(Bucket bt) throws SQLException {
        pstUpdateBucketAccess.clearParameters();
        pstUpdateBucketAccess.setString(1, bt.getAccess());
        pstUpdateBucketAccess.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketAccess.executeUpdate();
        
    }

    @Override
    public void updateBucketTagging(Bucket bt) throws SQLException {
        pstUpdateBucketTagging.clearParameters();
        pstUpdateBucketTagging.setString(1, bt.getTagging());
        pstUpdateBucketTagging.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketTagging.executeUpdate();
        
    }

    @Override
    public void updateBucketReplication(Bucket bt) throws SQLException {
        pstUpdateBucketReplication.clearParameters();
        pstUpdateBucketReplication.setString(1, bt.getReplication());
        pstUpdateBucketReplication.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketReplication.executeUpdate();
       
    }
 
    @Override
    public void updateBucketEncryption(Bucket bt) throws SQLException {
        pstUpdateBucketEncryption.clearParameters();
        pstUpdateBucketEncryption.setString(1, bt.getEncryption());
        pstUpdateBucketEncryption.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketEncryption.executeUpdate();
    }

    @Override
    public void updateBucketObjectLock(Bucket bt) throws SQLException {
        pstUpdateBucketObjectLock.clearParameters();
        pstUpdateBucketObjectLock.setString(1, bt.getObjectLock());
        pstUpdateBucketObjectLock.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketObjectLock.executeUpdate();
        
    }
    
    @Override
    public void updateBucketPolicy(Bucket bt) throws SQLException {
        pstUpdateBucketPolicy.clearParameters();
        pstUpdateBucketPolicy.setString(1, bt.getPolicy());
        pstUpdateBucketPolicy.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketPolicy.executeUpdate();
    }
   
    private void updateBucketFileCount(String bucketName, long count) throws SQLException {
        pstUpdateBucketFilecount.clearParameters();
        pstUpdateBucketFilecount.setLong(1, count);
        pstUpdateBucketFilecount.setString(2, getBucketId(bucketName));
        pstUpdateBucketFilecount.executeUpdate();
    }

    @Override
    public void updateBucketUsedSpace(Bucket bt, long size) throws SQLException {
        pstUpdateBucketUsedSpace.clearParameters();
        pstUpdateBucketUsedSpace.setLong(1, size);
        pstUpdateBucketUsedSpace.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketUsedSpace.executeUpdate();
    }

    @Override
    public boolean isUploadId(String uploadid) throws SQLException {
        boolean success = false;
        this.pstIsUpload.clearParameters();
        this.pstIsUpload.setString(1, uploadid);
        ResultSet rs = this.pstIsUpload.executeQuery();
    
        if (rs.next()) {
            success = true;
        }
        return success;
    }

    /*@Override
    public ObjectListParameter listObject(String bucketName, String delimiter, String marker, int maxKeys, String prefix) throws SQLException {       
        ObjectListParameter objectListParameter;
        boolean bBucketListParameterPrefix = false;
		boolean bMarker = false;
		boolean bDelimiter = false;
		boolean bDelForceGte = false;
		boolean bDelimiterMarker = false;

		int prepareOrder = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		if (!Strings.isNullOrEmpty(prefix)) {
			bBucketListParameterPrefix = true;
		}

		if (!Strings.isNullOrEmpty(marker)) {
			bMarker = true;
		}

		if (!Strings.isNullOrEmpty(delimiter)) {
			bDelimiter = true;
		}

		if (bMarker && bDelimiter) {
			if (marker.substring(marker.length() - delimiter.length(), marker.length()).compareTo(delimiter) == 0) {
				StringBuilder delimiterp2 = new StringBuilder(); 
				delimiterp2.append(marker.substring(0, marker.length() - 1));
				delimiterp2.append(Character.getNumericValue(marker.charAt(marker.length() - 1)) + 1);
				marker = delimiterp2.toString();
				bDelimiterMarker = true;
			}
		}

		String delmarker = "";  //delimiter marker
		objectListParameter = new ObjectListParameter();
		objectListParameter.setIstruncated(true);

		// 1.1. Excute query
		try {
			
			if (maxKeys == 0) {
				objectListParameter.setIstruncated(false);
			}
			
			while (objectListParameter.getObjects().size() + objectListParameter.getCommonPrefixes().size() < maxKeys) {
				prepareOrder = 0;
				
				String query = "SELECT objKey, meta FROM MDSDBTable WHERE bucket='" + bucketName + "' AND lastversion=true AND deleteMarker <> 'mark' ";

				if (bBucketListParameterPrefix)
					query += " AND objKey LIKE ?";

				if (bMarker) {
					if (bDelimiterMarker) {
						query += " AND objKey >= ?";
					} else {
						query += " AND objKey > ?";
					}
				}

				if (bDelForceGte) {
					query += " AND objKey >= ?";
				}

				query  += " ORDER BY objKey ASC LIMIT " + (maxKeys + 1);

				pstmt = this.con.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

				if (bBucketListParameterPrefix) {
					pstmt.setString(++prepareOrder, prefix.replaceAll("\\%",  "\\\\%").replaceAll("\\_",  "\\\\_") + "%");
				}

				if (bMarker)
					pstmt.setString(++prepareOrder, marker);

				if (bDelForceGte) {
					pstmt.setString(++prepareOrder, delmarker);
					bDelForceGte = false;
				}

				System.out.println(pstmt.toString());
				rs = pstmt.executeQuery();

				if (!bDelimiter) {
					rs.last();     
					int rowcount = rs.getRow();
					rs.beforeFirst();

					if (rowcount < maxKeys + 1 || maxKeys == 0) {
						objectListParameter.setIstruncated(false);
					}

					while (rs.next()) {
						S3Metadata s3Metadata = new S3Metadata();
						ObjectMapper jsonMapper = new ObjectMapper();
						try {
							s3Metadata = jsonMapper.readValue(rs.getString(2), S3Metadata.class);
						} catch (Exception e) {
                            System.out.println(e.getMessage());
						}

						s3Metadata.setName(rs.getString("objKey"));

						objectListParameter.getObjects().add(s3Metadata);
						if (objectListParameter.getObjects().size() == Integer.valueOf(maxKeys)) {
							if (objectListParameter.isTruncated()) {
								objectListParameter.setNextMarker(rs.getString("objKey"));
							}

							break;
						}
					}

					if (!objectListParameter.isTruncated()) {
						break;
					}

				} else {
					rs.last();     
					int rowcount = rs.getRow();
					rs.beforeFirst();

					if (rowcount < maxKeys+1 || maxKeys == 0) {
						objectListParameter.setIstruncated(false);
					}

					int end = 0;
					if (bBucketListParameterPrefix) {
						end = prefix.length();
					}

					while (rs.next()) {
						String objectName = rs.getString("objKey");
						String subName = objectName.substring(end, objectName.length());

						String endPrefix = "";
						if (bBucketListParameterPrefix) {
							endPrefix = prefix;
						}

						int find = 0;
						int match = 0;

						// delimiter를 발견하면 common prefix
						// 아니라면 object
						while ((find = subName.indexOf(delimiter)) >= 0) {
							endPrefix += subName.substring(0, find + delimiter.length());
							match++;
							break;
						}

						// delimiter가 발견
						if (match > 0) {
							// common prefix 등록
							objectListParameter.getCommonPrefixes().put(endPrefix, endPrefix);

							if(objectListParameter.isTruncated()) {
								StringBuilder delimiterp1 = new StringBuilder(); 
								delimiterp1.append(endPrefix.substring(0, endPrefix.length()-1));
								delimiterp1.append(Character.getNumericValue(endPrefix.charAt(endPrefix.length()-1)) + 1);
								delmarker = delimiterp1.toString();
								bDelForceGte = true;
								
								if (objectListParameter.getObjects().size() + objectListParameter.getCommonPrefixes().size() == Integer.valueOf(maxKeys)) {
									int isTruncatePrepareOrder = 0;
									ResultSet truncateRS = null;
									PreparedStatement truncatePSTMT = null;
									
									// istruncate check
									String truncateQuery = "SELECT objKey, meta FROM MDSDBTable WHERE lastversion=true AND deleteMarker <> 'mark' ";

									if (bBucketListParameterPrefix)
										truncateQuery += " AND objKey LIKE ?";

									if (bMarker) {
										if (bDelimiterMarker) {
											truncateQuery += " AND objKey >= ?";
										} else {
											truncateQuery += " AND objKey > ?";
										}
									}

									if (bDelForceGte) {
										truncateQuery += " AND objKey >= ?";
									}

									truncateQuery  += " ORDER BY objKey ASC LIMIT " + (maxKeys+1);

									truncatePSTMT = this.con.prepareStatement(truncateQuery, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

									if (bBucketListParameterPrefix) {
										truncatePSTMT.setString(++isTruncatePrepareOrder, prefix.replaceAll("\\%",  "\\\\%").replaceAll("\\_",  "\\\\_") + "%");
									}

									if (bMarker)
										truncatePSTMT.setString(++isTruncatePrepareOrder, marker);

									if (bDelForceGte) {
										truncatePSTMT.setString(++isTruncatePrepareOrder, delmarker);
									}

									System.out.println(truncatePSTMT.toString());
									truncateRS = truncatePSTMT.executeQuery();
									int truncateMatchCount = 0;
									
									while (truncateRS.next()) {
										String truncateObjectName = rs.getString("objKey");
										String truncateSubName = truncateObjectName.substring(end, truncateObjectName.length());

										String truncateEndPrefix = "";
										if (bBucketListParameterPrefix) {
											truncateEndPrefix = prefix;
										}

										int istruncatefind = 0;
										int istruncatematch = 0;

										// delimiter를 발견하면 common prefix
										// 아니라면 object
										while ((istruncatefind = truncateSubName.indexOf(delimiter)) >= 0) {
											truncateEndPrefix += truncateSubName.substring(0, istruncatefind + delimiter.length());
											istruncatematch++;
											break;
										}
										
										if (istruncatematch > 0) {
											truncateMatchCount++;
											objectListParameter.setNextMarker(truncateEndPrefix);
											break;
										}
									}
									
									if (truncateMatchCount == 0) {
										objectListParameter.setIstruncated(false);
									}

                                    truncateRS.close();
                                    truncatePSTMT.close();
								}
								
								break;
							}
						} else {
							S3Metadata s3Metadata = new S3Metadata();
							ObjectMapper jsonMapper = new ObjectMapper();
							try {
								s3Metadata = jsonMapper.readValue(rs.getString(2), S3Metadata.class);
							} catch (Exception e) {
								System.out.println(e.getMessage());
							}

							s3Metadata.setName(rs.getString("objKey"));

							objectListParameter.getObjects().add(s3Metadata);
							if (objectListParameter.getObjects().size() + objectListParameter.getCommonPrefixes().size() == Integer.valueOf(maxKeys)) {
								if(objectListParameter.isTruncated()) {
									objectListParameter.setNextMarker(rs.getString("objKey"));
								}

								break;
							}
						}
					}

					if (!objectListParameter.isTruncated()) {
						break;
					}
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new SQLException();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new SQLException();
		} finally {
			try {
				if (rs != null)
					rs.close();

				if (pstmt != null)
					pstmt.close();

			} catch (Exception e) {
				System.out.println(e.getMessage());
			    throw new SQLException();
			}
		}

        return objectListParameter;
    }

    @Override
    public ObjectListParameter listObjectV2(String bucketName, String delimiter, String startAfter, String continuationToken, int maxKeys, String prefix) throws SQLException {
        ObjectListParameter objectListParameter;
        boolean bBucketListParameterRefix = false;
		boolean bMarker = false;
		boolean bContinuationToken = false;
		boolean bDelimiter = false;
		boolean bDelForceGte = false;
		boolean bDelimitermarker = false;
		boolean bDelimitertoken = false;

		int prepareOrder = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		if (!Strings.isNullOrEmpty(prefix)) {
			bBucketListParameterRefix = true;
		}

		if (!Strings.isNullOrEmpty(startAfter)) {
			bMarker = true;
		}

		if (!Strings.isNullOrEmpty(continuationToken)) {
			bContinuationToken = true;
		}

		if (!Strings.isNullOrEmpty(delimiter)) {
			bDelimiter = true;
		}
		
		if (bMarker && bDelimiter) {
			if (startAfter.substring(startAfter.length() - delimiter.length(), startAfter.length()).compareTo(delimiter) == 0) {
				StringBuilder delimiterp2 = new StringBuilder(); 
				delimiterp2.append(startAfter.substring(0, startAfter.length()-1));
				delimiterp2.append(Character.getNumericValue(startAfter.charAt(startAfter.length()-1)) + 1);
				startAfter = delimiterp2.toString();
				bDelimitermarker = true;
			}
		}
		
		if (bContinuationToken && bDelimiter) {
			if (continuationToken.substring(continuationToken.length() - delimiter.length(), continuationToken.length()).compareTo(delimiter) == 0) {
				StringBuilder delimiterp2 = new StringBuilder(); 
				delimiterp2.append(continuationToken.substring(0, continuationToken.length()-1));
				delimiterp2.append(Character.getNumericValue(continuationToken.charAt(continuationToken.length()-1)) + 1);
				continuationToken = delimiterp2.toString();
				bDelimitertoken = true;
			}
		}
		
		String delMarker = "";  //delimiter marker
		objectListParameter = new ObjectListParameter();
		objectListParameter.setIstruncated(true);

		// 1.1. Excute query
		try {			
			if (maxKeys == 0) {
				objectListParameter.setIstruncated(false);
			}
			
			while (objectListParameter.getObjects().size() + objectListParameter.getCommonPrefixes().size() < maxKeys) {
				prepareOrder = 0;
				String query = "SELECT objKey, meta FROM MDSDBTable WHERE bucket='" + bucketName + "' AND lastversion=true AND deleteMarker <> 'mark' ";

				if (bBucketListParameterRefix)
					query += " AND objKey LIKE ?";

				if (bMarker) {
					if (bDelimitermarker) {
						query += " AND objKey >= ?";
					} else {
						query += " AND objKey > ?";
					}
				}
				
				if (bContinuationToken) {
					if (bDelimitertoken) {
						query += " AND objKey >= ?";
					} else {
						query += " AND objKey > ?";
					}
				}
				
				if (bDelForceGte) {
					query += " AND objKey >= ?";
				}

				query  += " ORDER BY objKey ASC LIMIT " + (maxKeys + 1);

				pstmt = this.con.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

				if (bBucketListParameterRefix) {
					pstmt.setString(++prepareOrder, prefix.replaceAll("\\%",  "\\\\%").replaceAll("\\_",  "\\\\_") + "%");
				}

				if (bMarker)
					pstmt.setString(++prepareOrder, startAfter);
				
				if (bContinuationToken)
					pstmt.setString(++prepareOrder, continuationToken);

				if (bDelForceGte) {
					pstmt.setString(++prepareOrder, delMarker);
					bDelForceGte = false;
				}

				System.out.println(pstmt.toString());
				rs = pstmt.executeQuery();

				if (!bDelimiter) {
					rs.last();     
					int rowcount = rs.getRow();
					rs.beforeFirst();

					if (rowcount < maxKeys + 1 || maxKeys == 0) {
						objectListParameter.setIstruncated(false);
					}

					while (rs.next()) {
						S3Metadata s3Metadata = new S3Metadata();
						ObjectMapper jsonMapper = new ObjectMapper();
						try {
							s3Metadata = jsonMapper.readValue(rs.getString(2), S3Metadata.class);
						} catch (Exception e) {
							System.out.println(e.getMessage());
                            throw new Exception(e.getMessage());
						}
						
						s3Metadata.setName(rs.getString("objKey"));
						objectListParameter.getObjects().add(s3Metadata);
						if (objectListParameter.getObjects().size() == maxKeys) {
							if (objectListParameter.isTruncated()) {
								objectListParameter.setNextMarker(rs.getString("objKey")); 	
							}

							break;
						}
					}

					if (!objectListParameter.isTruncated()) {
						break;
					}

				} else {
					rs.last();     
					int rowCount = rs.getRow();
					rs.beforeFirst();

					if (rowCount < maxKeys + 1 || maxKeys == 0) {
						objectListParameter.setIstruncated(false);
					}

					int end = 0;
					if (bBucketListParameterRefix) {
						end = prefix.length();
					}

					while (rs.next()) {
						String objectName = rs.getString("objKey");
						String subName = objectName.substring(end, objectName.length());

						int find = 0;
						int match = 0;

						String endPrefix = "";
						if (bBucketListParameterRefix) {
							endPrefix = prefix;
						}

						// delimiter를 발견하면 common prefix
						// 아니라면 object
						while ((find = subName.indexOf(delimiter)) >= 0) {
							endPrefix += subName.substring(0, find + delimiter.length());
							match++;
							break;
						}

						// delimiter가 발견
						if (match > 0) {
							// common prefix 등록
							objectListParameter.getCommonPrefixes().put(endPrefix, endPrefix);

							// object 등록
							if (objectListParameter.isTruncated()) {
								StringBuilder delimiterp1 = new StringBuilder(); 
								delimiterp1.append(endPrefix.substring(0, endPrefix.length()-1));
								delimiterp1.append(Character.getNumericValue(endPrefix.charAt(endPrefix.length()-1)) + 1);
								delMarker = delimiterp1.toString();
								bDelForceGte = true;
								
								if (objectListParameter.getObjects().size() + objectListParameter.getCommonPrefixes().size() == maxKeys) {
									int truncatePrepareOrder = 0;
									ResultSet truncateRS = null;
									PreparedStatement truncatePSTMT = null;
									
									// istruncate check
									String truncateQuery = "SELECT objKey, meta FROM MDSDBTable WHERE bucket='" + bucketName + "' AND lastversion=true AND deleteMarker <> 'mark' ";

									if (bBucketListParameterRefix)
										truncateQuery += " AND objKey LIKE ?";

									if (bMarker) {
										if (bDelimitermarker) {
											truncateQuery += " AND objKey >= ?";
										} else {
											truncateQuery += " AND objKey > ?";
										}
									}
									
									if (bContinuationToken) {
										if (bDelimitertoken) {
											truncateQuery += " AND objKey >= ?";
										} else {
											truncateQuery += " AND objKey > ?";
										}
									}

									if (bDelForceGte) {
										truncateQuery += " AND objKey >= ?";
									}

									truncateQuery  += " ORDER BY objKey ASC LIMIT " + (maxKeys + 1);

									truncatePSTMT = this.con.prepareStatement(truncateQuery, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

									if (bBucketListParameterRefix) {
										truncatePSTMT.setString(++truncatePrepareOrder, prefix.replaceAll("\\%",  "\\\\%").replaceAll("\\_",  "\\\\_") + "%");
									}

									if (bMarker)
										truncatePSTMT.setString(++truncatePrepareOrder, startAfter);
									
									if (bContinuationToken)
										truncatePSTMT.setString(++truncatePrepareOrder, continuationToken);

									if (bDelForceGte) {
										truncatePSTMT.setString(++truncatePrepareOrder, delMarker);
									}
									
									System.out.println(truncatePSTMT.toString());
									truncateRS = truncatePSTMT.executeQuery();
									int truncateMatchCount=0;
									
									while (truncateRS.next()) {
										String truncateObjectName = rs.getString("objKey");
										String truncateSubName = truncateObjectName.substring(end, truncateObjectName.length());

										String truncateEndPrefix = "";
										if (bBucketListParameterRefix) {
											truncateEndPrefix = prefix;
										}

										int truncateFind = 0;
										int truncateMatch = 0;

										// delimiter를 발견하면 common prefix
										// 아니라면 object
										while ((truncateFind = truncateSubName.indexOf(delimiter)) >= 0) {
											truncateEndPrefix += truncateSubName.substring(0, truncateFind + delimiter.length());
											truncateMatch++;
											break;
										}
										
										if (truncateMatch > 0) {
											truncateMatchCount++;
											objectListParameter.setNextMarker(truncateEndPrefix);
											break;
										}
									}
									
                                    truncateRS.close();
                                    truncatePSTMT.close();

									if (truncateMatchCount == 0) {
										objectListParameter.setIstruncated(false);
									}
								}

								
								break;
							}

						} else {
							S3Metadata im = new S3Metadata();
							ObjectMapper jsonMapper = new ObjectMapper();
							try {
								im = jsonMapper.readValue(rs.getString(2), S3Metadata.class);
							} catch (Exception e) {
								System.out.println(e.getMessage());
			                    throw new SQLException(e.getMessage());
							}

							im.setName(rs.getString("objKey"));

							objectListParameter.getObjects().add(im);
							if (objectListParameter.getObjects().size() == maxKeys) {
								if (objectListParameter.isTruncated()) {
									objectListParameter.setNextMarker(rs.getString("objKey")); 	
								}

								break;
							}
						}
					}

					if (!objectListParameter.isTruncated()) {
						break;
					}
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new SQLException(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new SQLException(e.getMessage());
		} finally {
			try {
				if (rs != null)
					rs.close();

				if (pstmt != null)
					pstmt.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			    throw new SQLException(e.getMessage());
			}
		}

        return objectListParameter;
    }

    @Override
    public ObjectListParameter listObjectVersions(String bucketName, String delimiter, String keyMarker, String versionIdMarker, int maxKeys, String prefix) throws SQLException {
        ObjectListParameter objectListParameter;
        boolean bBucketListParameterRefix = false;
		boolean bKeyMarker = false;
		boolean bVersionIdMarker = false;
		boolean bDelimiter = false;
		boolean bDelForceGte = false;

		int prepareorder = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		if (!Strings.isNullOrEmpty(prefix)) {
			bBucketListParameterRefix = true;
		}

		if (!Strings.isNullOrEmpty(keyMarker)) {
			bKeyMarker = true;
		}

		if (!Strings.isNullOrEmpty(versionIdMarker))  {
			bVersionIdMarker = true;
		}

		if (!Strings.isNullOrEmpty(delimiter)) {
			bDelimiter = true;
		}

		String delMarker = "";  //delimiter marker
		objectListParameter = new ObjectListParameter();
		objectListParameter.setIstruncated(true);

		// 1.1. Excute query
		try {
			while (objectListParameter.getObjects().size() < maxKeys) {
				prepareorder = 0;
				
				String query = "SELECT objKey, meta, versionid, lastversion FROM MDSDBTable WHERE bucket='" + bucketName + "' ";

				if (bBucketListParameterRefix)
					query += " AND objKey LIKE ?";

				if (bKeyMarker && !bVersionIdMarker)
					query += " AND objKey > ?";

				if (bVersionIdMarker)
					query += " AND ( objKey > ? OR (objKey = ? AND versionid > ?)) ";

				if (bDelForceGte) {
					query += " AND objKey >= ?";
				}

				query  += " ORDER BY objKey ASC, lastModified DESC LIMIT " + (maxKeys + 1);

				pstmt = this.con.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

				if (bBucketListParameterRefix) {
					pstmt.setString(++prepareorder, prefix.replaceAll("\\%",  "\\\\%").replaceAll("\\_",  "\\\\_") + "%");
				}

				if (bKeyMarker)
					pstmt.setString(++prepareorder, keyMarker);

				if (bVersionIdMarker) {
					pstmt.setString(++prepareorder, keyMarker);
					pstmt.setString(++prepareorder, versionIdMarker);
				}

				if (bDelForceGte) {
					pstmt.setString(++prepareorder, delMarker);
					bDelForceGte = false;
				}

				System.out.println(pstmt.toString());
				rs = pstmt.executeQuery();

				if (!bDelimiter) {
					rs.last();     
					int rowCount = rs.getRow();
					rs.beforeFirst();

					if (rowCount < maxKeys + 1) {
						objectListParameter.setIstruncated(false);
					}

					while (rs.next()) {
						S3Metadata s3Metadata = new S3Metadata();
						ObjectMapper jsonMapper = new ObjectMapper();
						try {
							s3Metadata = jsonMapper.readValue(rs.getString(2), S3Metadata.class);
						} catch (Exception e) {
							System.out.println("mata : " + rs.getString(2));
							throw new Exception(e.getMessage());
						}

						s3Metadata.setName(rs.getString("objKey"));
						String versionid = rs.getString("versionid");
						s3Metadata.setVersionId(versionid);
						if(rs.getBoolean("lastversion")) {
							s3Metadata.setIsLatest("true");
						} else { 
							s3Metadata.setIsLatest("false");
						}

						objectListParameter.getObjects().add(s3Metadata);
						if(objectListParameter.getObjects().size() == maxKeys) {
							if(objectListParameter.isTruncated()) {
								objectListParameter.setNextMarker(rs.getString("objKey")); 	
								objectListParameter.setNextVersion(rs.getString("versionid"));
							}

							break;
						}
					}

					if (!objectListParameter.isTruncated()) {
						break;
					}

				} else {
					rs.last();     
					int rowCount = rs.getRow();
					rs.beforeFirst();

					if(rowCount < maxKeys + 1) {
						objectListParameter.setIstruncated(false);
					}

					int end=0;
					if (bBucketListParameterRefix) {
						end = prefix.length();
					}

					while (rs.next()) {
						String objectName = rs.getString("objKey");
						String subName = objectName.substring(end, objectName.length());

						int find = 0;
						int match = 0;

						String endPrefix = "";
						if(bBucketListParameterRefix) {
							endPrefix = prefix;
						}

						// delimiter를 발견하면 common prefix
						// 아니라면 object
						while ((find = subName.indexOf(delimiter)) >= 0) {
							endPrefix += subName.substring(0, find + delimiter.length());
							match++;
							break;
						}

						// delimiter가 발견
						if (match > 0) {
							// common prefix 등록
							objectListParameter.getCommonPrefixes().put(endPrefix, endPrefix);

							if (objectListParameter.isTruncated()) {
								StringBuilder delimiterp1 = new StringBuilder(); 
								delimiterp1.append(endPrefix.substring(0, endPrefix.length()-1));
								delimiterp1.append(endPrefix.charAt(endPrefix.length()) + 1);
								delMarker = delimiterp1.toString();
								bDelForceGte = true;
								break;
							}
                                                        // there is differce here

						} else {
							S3Metadata s3Metadata = new S3Metadata();
							ObjectMapper jsonMapper = new ObjectMapper();
							try {
								s3Metadata = jsonMapper.readValue(rs.getString(2), S3Metadata.class);
							} catch (Exception e) {
								System.out.println("mata : " + rs.getString(2));
							    throw new Exception(e.getMessage());
							}

							s3Metadata.setName(rs.getString("objKey"));
							String versionid = rs.getString("versionid");
							s3Metadata.setVersionId(versionid);
							if (rs.getBoolean("lastversion")) {
								s3Metadata.setIsLatest("true");
							} else { 
								s3Metadata.setIsLatest("false");
							}

							objectListParameter.getObjects().add(s3Metadata);
							if (objectListParameter.getObjects().size() == maxKeys) {
								if (objectListParameter.isTruncated()) {
									objectListParameter.setNextMarker(rs.getString("objKey")); 	
								}

								break;
							}
						}
					}

					if (!objectListParameter.isTruncated()) {
						break;
					}
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			throw new SQLException(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw new SQLException(e.getMessage());
		} finally {
			try {
				if (rs != null)
					rs.close();

				if (pstmt != null)
					pstmt.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			    throw new SQLException(e.getMessage());
			}
		}

        return objectListParameter;
    }*/
    
    @Override
    public  List<Object> utilJobMgt(String operation, List<Object> in){
        List<Object> ret;
        String status;
        long TotalNumObject;
        long NumJobDone;
        boolean checkOnly;
        String utilName;
        String Id = in.get(0).toString();
        
        try{
           // System.out.format("Operation : %s check : %s \n", operation, operation.equalsIgnoreCase("addJob"));
            if (operation.equalsIgnoreCase("addJob")){
                System.out.format("Operation : %s \n", operation);
                status = in.get(1).toString();
                TotalNumObject = Long.parseLong(in.get(2).toString());
                checkOnly = Boolean.getBoolean(in.get(4).toString());
                utilName = in.get(5).toString();
                //System.out.format("Id : %s status : %s TotalNumObject : %d checkOnly : %s utilName : %s \n", Id, status, TotalNumObject, checkOnly, utilName);
                ret = insertUtilJob(Id, status, TotalNumObject, checkOnly, utilName);
                //System.out.format("Id : %s status : %s TotalNumObject : %d checkOnly : %s utilName : %s \n", Id, status, TotalNumObject, checkOnly, utilName);
            } else if (operation.equalsIgnoreCase("getJob")){
                ret = selectUtilJob(Id);
            } else if (operation.equalsIgnoreCase("updateJobStatus")){
                status = in.get(1).toString();
                ret= updateStatusUtilJob(Id, status);
            } else if (operation.equalsIgnoreCase("updateJobNumber")){
                TotalNumObject = Long.parseLong(in.get(2).toString());
                NumJobDone = Long.parseLong(in.get(3).toString());
                ret= updateNumberJobsUtilJob(Id, TotalNumObject, NumJobDone);
            }
            else
                ret = new ArrayList<>();
        }catch(SQLException ex){
            ret = new ArrayList<>();;
        }
        return ret;
    }
    
    @Override
    public boolean isBucketDeleted(String bucket) throws SQLException {
        pstIsDeleteBucket.clearParameters();
        pstIsDeleteBucket.setString(1, bucket);
        ResultSet rs = pstIsDeleteBucket.executeQuery();
        
        if (rs == null) {
            return true;
        }

        if (rs.next()) {
            return false;
        }

        return true;
    }
    
    @Override
    public synchronized int insertUserDiskPool(String userId, String accessKey, String secretKey, String diskpoolId, int replicaCount) throws SQLException{
        pstInsertUserDiskPool.clearParameters();
        pstInsertUserDiskPool.setString(1, userId);
        pstInsertUserDiskPool.setString(2, secretKey + "_" + accessKey);
        pstInsertUserDiskPool.setString(3, diskpoolId);
        pstInsertUserDiskPool.setInt(4, replicaCount);
        pstInsertUserDiskPool.execute();
        return 0;
    }
    
    @Override
    public synchronized Bucket getUserDiskPool(Bucket bt) throws SQLException{
              
        pstSelectUserDiskPool.clearParameters();
        pstSelectUserDiskPool.setString(1, bt.getUserId());
   
        ResultSet rs = pstSelectUserDiskPool.executeQuery();
        
        while(rs.next()){
            bt.setDiskPoolId(rs.getString(1));
            bt.setReplicaCount(rs.getInt(2));
            break;
        }
        return bt;
    }
    
    @Override
    public synchronized int deleteUserDiskPool(String userId, String diskPoolId) throws SQLException{
        pstDeleteUserDiskPool.clearParameters();
        pstDeleteUserDiskPool.setString(1, userId);
        pstDeleteUserDiskPool.setString(2, diskPoolId);
        pstDeleteUserDiskPool.execute();
        return 0;
    }
    
    @Override
    public PreparedStatement getStatement(String query) throws SQLException{	
        return this.con.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
    }
    
    private LocalDateTime convertDate(String date2Convert){
        //System.out.println("date2Convert>>" + date2Convert);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
        LocalDateTime dateTime = LocalDateTime.parse(date2Convert, formatter);
        return dateTime;
    }
    
    @Override
    public List<Metadata> getObjectList(String bucketName, Object pstmt, int maxKeys) throws SQLException{
        List<Metadata> list = new ArrayList();
        ResultSet rs = null;
        DISK pdsk = null;
        DISK rdsk = null;
        String diskPoolId = "1";
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        obmCache.displayBucketList();
        obmCache.displayDiskPoolList();
        if (bt != null)
            //return list;
            diskPoolId = bt.getDiskPoolId();
        
        rs = ((PreparedStatement)pstmt).executeQuery();
        while (rs.next()) {
            String objKey = rs.getString("objKey");
            String meta = rs.getString("meta");
            String versionid = rs.getString("versionid"); 
            Boolean lastversion = rs.getBoolean("lastversion");
            String etag = rs.getString("etag");
            long lastModified = rs.getLong("lastModified");
            long size = rs.getLong("size");
            String tag = rs.getString("tag");
            String acl = rs.getString("acl");
            String pdiskid = rs.getString("pdiskid");
            String rdiskid = rs.getString("rdiskid");
            try {
                pdsk = obmCache.getDiskWithId(diskPoolId, pdiskid);
                if (!rdiskid.isEmpty())
                    rdsk = this.obmCache.getDiskWithId(diskPoolId, rdiskid);
                else
                    rdsk = new DISK();
            } catch (ResourceNotFoundException ex) {
                if ( pdsk == null)
                    pdsk = new DISK();
                rdsk = new DISK();
            }
            
            Metadata mt = new Metadata(bucketName, objKey);
            mt.setMeta(meta);
            mt.set(etag, tag, meta, acl, size);
            mt.setLastModified(lastModified);
            mt.setVersionId(versionid, "", lastversion);
            mt.setPrimaryDisk(pdsk);
            mt.setReplicaDISK(rdsk);
            list.add(mt);
        }
        
        rs.close();
        ((PreparedStatement)pstmt).close();
        
        return list;
    }
    
}
