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

import com.google.common.base.Strings;
import com.mongodb.BasicDBObject;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
// import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.libs.multipart.Multipart;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.libs.multipart.ResultParts;
import com.pspace.ifs.ksan.libs.multipart.ResultUploads;
import com.pspace.ifs.ksan.libs.multipart.Upload;

import java.util.Date;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static Logger logger;
    // for buckets
    private PreparedStatement pstCreateBucket;
    private PreparedStatement pstInsertBucket;
    private PreparedStatement pstDeleteBucket;
    private PreparedStatement pstSelectBucket;
    private PreparedStatement pstSelectAllBucket;
    private PreparedStatement pstSelectAnalyticsBucket;
    private PreparedStatement pstUpdateBucket;
    //private PreparedStatement pstSelectUsedBucket;
    //private PreparedStatement pstSelectUsedDisks;
    //private PreparedStatement pstSelectBucketByName;
    private PreparedStatement pstUpdateBucketAcl;
    private PreparedStatement pstUpdateBucketWeb;
    private PreparedStatement pstUpdateBucketCors;
    private PreparedStatement pstUpdateBucketLifecycle;
    private PreparedStatement pstUpdateBucketAccess;
    private PreparedStatement pstUpdateBucketTagging;
    private PreparedStatement pstUpdateBucketReplication;
    //private PreparedStatement pstIsDeleteBucket;
    private PreparedStatement pstUpdateBucketEncryption;
    private PreparedStatement pstUpdateBucketLogging;
    private PreparedStatement pstUpdateBucketObjectLock;
    private PreparedStatement pstUpdateBucketPolicy;
    private PreparedStatement pstUpdateBucketFilecount;
    private PreparedStatement pstUpdateBucketUsedSpace;
    private PreparedStatement pstUpdateObjTagIndexBucket;
    private PreparedStatement pstUpdateAnalyticsBucket;
    private PreparedStatement pstUpdateAccelerateBucket;
    private PreparedStatement pstUpdatePaymentBucket;
    private PreparedStatement pstUpdateNotificationBucket;
    //private PreparedStatement pstSelectInventoryBucket;
    private PreparedStatement pstUpdateInventoryBucket;
    
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
    private PreparedStatement pstGetPartRef;
    private PreparedStatement pstSetPartRef;
    
    // for utility
    private PreparedStatement pstCreateUJob;
    private PreparedStatement pstInsertUJob;
    private PreparedStatement pstUpdateUJob1;
    private PreparedStatement pstUpdateUJob2;
    private PreparedStatement pstSelectUJob;
    
    
    // for restore object
    private PreparedStatement pstCreateRestoreObjects;
    private PreparedStatement pstInsertRestoreObjects;
    private PreparedStatement pstSelectRestoreObjects;
    private PreparedStatement pstDeleteRestoreObjects;
            
    public MysqlDataRepository(ObjManagerCache  obmCache, String host, String username, String passwd, String dbname) throws SQLException{
        logger = LoggerFactory.getLogger(MysqlDataRepository.class);
        this.obmCache = obmCache;
        this.passwd = passwd;
        this.username = username;
        this.url ="jdbc:mysql://"+ host+":3306/"+ dbname +"?useSSL=false&autoReconnect=true"; // autoReconnect option is depreciated in latest maraidb
        try{
            this.createDB(host, dbname);
            this.connect();
	    createPreparedStatements();
                        
        } catch(SQLException ex){
            this.ex_message(ex);
        }
        
        this.createTable();
    }

    private void createPreparedStatements() throws SQLException{
            // for bucket
            pstCreateBucket = con.prepareStatement(DataRepositoryQuery.createBucketQuery);
            pstInsertBucket = con.prepareStatement(DataRepositoryQuery.insertBucketQuery);
            pstDeleteBucket = con.prepareStatement(DataRepositoryQuery.deleteBucketQuery);
            pstSelectBucket = con.prepareStatement(DataRepositoryQuery.selectBucketQuery);
            pstSelectAllBucket = con.prepareStatement(DataRepositoryQuery.selectAllBucketQuery);
            pstSelectAnalyticsBucket= con.prepareStatement(DataRepositoryQuery.selectBucketAnalyticsQuery);
            pstUpdateBucket = con.prepareStatement(DataRepositoryQuery.updateBucketQuery);
            
            pstUpdateBucketAcl = con.prepareStatement(DataRepositoryQuery.updateBucketAclQuery);
            pstUpdateBucketWeb = con.prepareStatement(DataRepositoryQuery.updateBucketWebQuery);
            pstUpdateBucketCors = con.prepareStatement(DataRepositoryQuery.updateBucketCorsQuery);
            pstUpdateBucketLifecycle = con.prepareStatement(DataRepositoryQuery.updateBucketLifecycleQuery);
            pstUpdateBucketAccess = con.prepareStatement(DataRepositoryQuery.updateBucketAccessQuery);
            pstUpdateBucketTagging = con.prepareStatement(DataRepositoryQuery.updateBucketTaggingQuery);
            pstUpdateBucketReplication = con.prepareStatement(DataRepositoryQuery.updateBucketReplicationQuery);
            pstUpdateBucketEncryption = con.prepareStatement(DataRepositoryQuery.updateBucketEncryptionQuery);
	    pstUpdateBucketLogging = con.prepareStatement(DataRepositoryQuery.updateBucketLoggingQuery);
            pstUpdateBucketObjectLock = con.prepareStatement(DataRepositoryQuery.updateBucketObjectLockQuery);
            pstUpdateBucketPolicy = con.prepareStatement(DataRepositoryQuery.updateBucketPolicyQuery);
            pstUpdateBucketFilecount = con.prepareStatement(DataRepositoryQuery.updateBucketFilecountQuery);
            pstUpdateBucketUsedSpace = con.prepareStatement(DataRepositoryQuery.updateBucketUsedSpaceQuery);
            //pstIsDeleteBucket = con.prepareStatement(DataRepositoryQuery.objIsDeleteBucketQuery);
            pstUpdateObjTagIndexBucket = con.prepareStatement(DataRepositoryQuery.updateBucketObjTagIndexingQuery);
            pstUpdateAnalyticsBucket = con.prepareStatement(DataRepositoryQuery.updateBucketAnalyticsQuery);
            pstUpdateAccelerateBucket = con.prepareStatement(DataRepositoryQuery.updateBucketAccelerateQuery);
            pstUpdatePaymentBucket = con.prepareStatement(DataRepositoryQuery.updateBucketPaymentQuery);
            pstUpdateNotificationBucket = con.prepareStatement(DataRepositoryQuery.updateBucketNotificationQuery);
            pstUpdateInventoryBucket = con.prepareStatement(DataRepositoryQuery.updateBucketInventoryQuery);
            
            // for multipart
            pstCreateMultiPart= con.prepareStatement(DataRepositoryQuery.createMultiPartQuery);
            pstInsertMultiPart = con.prepareStatement(DataRepositoryQuery.insertMultiPartQuery);
            pstUpdateMultiPart = con.prepareStatement(DataRepositoryQuery.updateMultiPartQuery);
            pstDeleteMultiPart = con.prepareStatement(DataRepositoryQuery.deleteMultiPartQuery);
            pstSelectMultiPart = con.prepareStatement(DataRepositoryQuery.selectMultiPartQuery);

            pstGetMultiPart = con.prepareStatement(DataRepositoryQuery.getMultiPartQuery);
            pstGetParts = con.prepareStatement(DataRepositoryQuery.getPartsQuery);
            pstGetPartsMax = con.prepareStatement(DataRepositoryQuery.getPartsMaxQuery);
            pstGetUploads = con.prepareStatement(DataRepositoryQuery.getUploadsQuery);
            pstIsUpload = con.prepareStatement(DataRepositoryQuery.isUploadQuery);
            pstIsUploadPartNo = con.prepareStatement(DataRepositoryQuery.isUploadPartNoQuery);
            pstGetPartRef = con.prepareStatement(DataRepositoryQuery.getPartRefQuery);
            pstSetPartRef = con.prepareStatement(DataRepositoryQuery.updatePartRefQuery);
            
           // for utility
            //String Id, String status, long TotalNumObject, boolean checkOnly, String utilName
            pstCreateUJob = con.prepareStatement(DataRepositoryQuery.createUJobQuery);
            pstInsertUJob = con.prepareStatement(DataRepositoryQuery.insertUJobQuery);
            pstUpdateUJob1 = con.prepareStatement(DataRepositoryQuery.updateUJob1Query);
            pstUpdateUJob2 = con.prepareStatement(DataRepositoryQuery.updateUJob2Query);
            pstSelectUJob = con.prepareStatement(DataRepositoryQuery.selectUJobQuery);

            // for LifeCycle
            pstCreateRestoreObjects = con.prepareStatement(DataRepositoryQuery.createRestoreObjectsQuery);
            pstInsertRestoreObjects = con.prepareStatement(DataRepositoryQuery.insertRestoreObjectsQuery);
            pstSelectRestoreObjects = con.prepareStatement(DataRepositoryQuery.selectRestoreObjectsQuery);
            pstDeleteRestoreObjects = con.prepareStatement(DataRepositoryQuery.deleteRestoreObjectsQuery);

    }

    private int createDB(String host, String dbname){
        Connection connC = null;
        Statement stmt = null;
        try {    
            Class.forName("com.mysql.cj.jdbc.Driver");
            connC= DriverManager.getConnection("jdbc:mysql://" + host+"/", this.username, this.passwd);
            stmt = connC.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS "+ dbname+ ";");
            return 0;
        } catch (ClassNotFoundException | SQLException ex) {
            logger.error(ex.getMessage());
        }finally{
            try {
                if (stmt != null)
                    stmt.close();
                if (connC != null)
                    connC.close();
            } catch (SQLException ex) {
                logger.error(ex.getMessage());
            }
        }
        return -1;
    }
    
    private int connect(){
        try{
           Class.forName("com.mysql.cj.jdbc.Driver");
           Properties conProp = new Properties();
           conProp.setProperty("user", username);
           conProp.setProperty("password", passwd);
           conProp.setProperty("autoReconnectForPools", "true");
           conProp.setProperty("maxReconnects", "30");
           this.con = DriverManager.getConnection(this.url, conProp); 
        } catch(SQLException ex){
            this.ex_message(ex);
            return -1;
        } catch (ClassNotFoundException ex) { 
            logger.error(ex.getMessage());
        }
        return 0;
    }
   
    private void checkAndReconnect()throws SQLException{
	 if (this.con != null && !this.con.isClosed())
             return;

         this.connect();
         createPreparedStatements();
    } 

    private void ex_message(SQLException ex){
        logger.error(ex.getMessage());
    }
    
    private int createTable() throws SQLException{
        //System.out.format("[createTable] query : %s \n", pstCreateBucket.toString());
        this.pstCreateBucket.execute();
        this.pstCreateMultiPart.execute();
        pstCreateUJob.execute();
        //pstCreateLifeCycle.execute();
        createLifeCycleEventTables();
        createRestoreObjectTable();
        return 0;
    }
    
    private void createLifeCycleEventTables() throws SQLException{
        try (PreparedStatement pstCreateLifeCycle = getObjPreparedStmt(DataRepositoryQuery.lifeCycleEventTableName, DataRepositoryQuery.createLifeCycleQuery)){
            pstCreateLifeCycle.execute();
            pstCreateLifeCycle.close();
        }
        
        try (PreparedStatement pstCreateLifeCycle = getObjPreparedStmt(DataRepositoryQuery.lifeCycleFailedEventTableName, DataRepositoryQuery.createLifeCycleQuery)){
            pstCreateLifeCycle.execute();
            pstCreateLifeCycle.close();
    }   }
    
    private PreparedStatement getObjPreparedStmt(String bucketName, String format) throws SQLException{
        String query = String.format(format, "`" + bucketName + "`");
        PreparedStatement pstStmt = con.prepareStatement(query);
        //System.out.println("Query :>" + pstStmt);
        return pstStmt;
    }
    
    /*private PreparedStatement getObjPreparedStmt2(String bucketName, String format) throws SQLException{
        String query = String.format(format, "`" + bucketName + "`", "`" + bucketName + "`");
        PreparedStatement pstStmt = con.prepareStatement(query);
        return pstStmt;
    }*/
    
    private String convertXML2Json(String xmlstr){
        String tags[] = xmlstr.split("<Tagging>");
        if (tags.length > 1){
            String tag = tags[1].replaceAll("<Tagging>", "").replaceAll("</Tagging>", "");
            tag = tag.replaceAll("<TagSet>", "{");
            tag = tag.replaceAll("</TagSet>", "}");
            tag = tag.replaceAll("<Tag>", "");
            tag = tag.replaceAll("</Tag>", ",");
            tag = tag.replaceAll("<Key>", "\"");
            tag = tag.replaceAll("</Key>", "\": ");
            tag = tag.replaceAll("<Value>", " \"");
            tag = tag.replaceAll("</Value>", "\" ");
            return tag;
        }   
        return null;
    }
    
    private void createObjectTable(String bucketName) throws SQLException{
        try (PreparedStatement pstStmt = getObjPreparedStmt(bucketName, DataRepositoryQuery.objCreateQuery)){
            pstStmt.execute();
        }
    }
    
    private int updateVersion(String bucketName, String key) throws SQLException{ 
        try (PreparedStatement pstupdateLastVersion = getObjPreparedStmt(bucketName, DataRepositoryQuery.objUpdateLastVersionQuery)){
            pstupdateLastVersion.clearParameters();
            pstupdateLastVersion.setString(1, key);
            pstupdateLastVersion.execute();
        }
        return 0;
    }
    

    private int updateMetadata(Metadata md){
        try {
            try (PreparedStatement pstupdateMetadata = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdateMetadataQuery)){
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
            }
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
            //System.out.format("==>Id : %s status : %s TotalNumObject : %d checkOnly : %s utilName : %s \n", Id, rs.getString(1), rs.getLong(2), rs.getBoolean(4), rs.getString(5)); 
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
            logger.debug("[insertObject] Start bucketName : {} key : {}  versionId : {} ", md.getBucket(), md.getPath(), md.getVersionId());
	    checkAndReconnect();

            try (PreparedStatement pstStmt = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objInsertQuery)){
                pstStmt.clearParameters();
                pstStmt.setString(1, md.getBucket());
                pstStmt.setString(2, md.getPath());
                pstStmt.setString(3, md.getEtag());
                pstStmt.setString(4, md.getMeta());
                pstStmt.setString(5, md.getTag());
                pstStmt.setString(6, md.getAcl());
                pstStmt.setLong(7, md.getSize());
                pstStmt.setLong(8, md.getLastModified());
                pstStmt.setString(9, md.getPrimaryDisk().getId());
                if (md.isReplicaExist()){
                    pstStmt.setString(10, md.getReplicaDisk().getId());
                } else {
                    pstStmt.setString(10, "");
                }
                pstStmt.setString(11, md.getObjId());
                pstStmt.setString(12, md.getVersionId());
                pstStmt.setString(13, md.getDeleteMarker());

                // If the same object exists, change lastversion to false.
                updateVersion(md.getBucket(), md.getObjId());

                if (pstStmt.executeUpdate() > 0)
                    updateBucketFileCount(md.getBucket(), 1);
            }
        } catch(SQLException ex){
            if (ex.getErrorCode() == 1062)
                return updateMetadata(md);
            
            this.ex_message(ex);
            logger.debug("[insertObject] End bucketName : {} key : {}  versionId : {} ret : {}", md.getBucket(), md.getPath(), md.getVersionId(), -1);
            return -1;
        }
        logger.debug("[insertObject] End bucketName : {} key : {}  versionId : {} ret : {}", md.getBucket(), md.getPath(), md.getVersionId(), 0);
        return 0;
    }
    
    @Override
    public synchronized int updateDisks(Metadata md, boolean updatePrimary, DISK newDisk) {
        try {
	    checkAndReconnect();

            if (updatePrimary){
                try (PreparedStatement pstupdatePDisks = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdatePDisksQuery)){
                    pstupdatePDisks.clearParameters();
                    pstupdatePDisks.setString(1, newDisk.getId());
                    pstupdatePDisks.setString(2, md.getObjId());
                    pstupdatePDisks.setString(3, md.getVersionId());
                    //pstupdatePDisks.setString(4, md.getPrimaryDisk().getId());
                    return pstupdatePDisks.executeUpdate();
                }
            }
            else{
                try (PreparedStatement pstupdateRDisks = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdateRDisksQuery)){
                    pstupdateRDisks.clearParameters();
                    pstupdateRDisks.setString(1, newDisk.getId());
                    pstupdateRDisks.setString(2, md.getObjId());
                    pstupdateRDisks.setString(3, md.getVersionId());
                    return pstupdateRDisks.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
        }
        return -1;
    }
    
    @Override
    public synchronized int updateSizeTime(Metadata md) {
        try {
	    checkAndReconnect();

            try (PreparedStatement pstupdateSizeTime = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdateSizeTimeQuery)){
                pstupdateSizeTime.clearParameters();
                pstupdateSizeTime.setLong(1, md.getSize());
                pstupdateSizeTime.setLong(2, md.getLastModified());
                pstupdateSizeTime.setString(3, md.getObjId());
                return pstupdateSizeTime.executeUpdate();
            }
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
            //Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    @Override
    public void updateObjectEtag(Metadata md, String etag) throws SQLException{
	checkAndReconnect();
        try (PreparedStatement pstupdateEtag = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdateEtagQuery)){
            pstupdateEtag.clearParameters();
            pstupdateEtag.setString(1, etag);
            pstupdateEtag.setString(2, md.getObjId());
            pstupdateEtag.setString(3, md.getVersionId());
        }
    }
    
    private Metadata getSelectObjectResult(String bucketName, String objId, ResultSet rs) throws SQLException, ResourceNotFoundException{
        Metadata mt;
        DISK pdsk;
        DISK rdsk;
        String rdiskPath;
               
        Bucket bt  = obmCache.getBucketFromCache(bucketName);
        if (bt == null)
            throw new ResourceNotFoundException("[getSelectObjectResult] bucket "+ bucketName +" not found in the db");
        
        while(rs.next()){
            pdsk = this.obmCache.getDiskWithId(rs.getString(9));
            try{
                rdiskPath = rs.getString(10);
                if (!rdiskPath.isEmpty())
                    rdsk = this.obmCache.getDiskWithId(rs.getString(10));
                else
                    rdsk = new DISK();
            } catch(ResourceNotFoundException ex){
                rdsk = new DISK();
            }
            
            int replicaCount = obmCache.getDiskPoolFromCache(pdsk.getDiskPoolId()).getDefaultReplicaCount();
            mt = new Metadata(rs.getString(1), rs.getString(2));
            mt.setSize(rs.getLong(3));
            mt.setEtag(rs.getString(5));
            mt.setTag(rs.getString(6));
            mt.setMeta(rs.getString(7));
            mt.setAcl(rs.getString(8));
            mt.setPrimaryDisk(pdsk);
            mt.setReplicaDISK(rdsk);
            mt.setVersionId(rs.getString(11), rs.getString(12), rs.getBoolean(13));
            mt.setReplicaCount(replicaCount);
            return mt;
        }
        throw new ResourceNotFoundException("[getSelectObjectResult] bucket: "+ bucketName + " key " + objId + " not found in the db");
    }
    
    private synchronized Metadata selectSingleObjectInternal(String bucketName, String objId) throws ResourceNotFoundException {
        try{
            logger.debug("[selectSingleObjectInternal] Start bucketName : {} objId : {} ", bucketName, objId);
            try (PreparedStatement pstStmt = getObjPreparedStmt(bucketName, DataRepositoryQuery.objSelectOneQuery)){
                pstStmt.clearParameters();
                pstStmt.setString(1, objId);
                ResultSet rs = pstStmt.executeQuery();
                logger.debug("[selectSingleObjectInternal] End bucketName : {} objId : {} rowc: {}", bucketName, objId, rs.getRow());
                Metadata mt = getSelectObjectResult(bucketName, objId, rs);
                return mt; 
            }
        } catch(SQLException ex){
            //System.out.println(" error : " + ex.getMessage());
            this.ex_message(ex);
            throw new ResourceNotFoundException("path not found in the db : " + ex.getMessage());
        }
    }

    private synchronized Metadata selectSingleObjectInternal(String bucketName, String objId, String versionId) throws ResourceNotFoundException {
        try{
            logger.debug("[selectSingleObjectInternal] Start bucketName : {} objId : {}  versionId : {} ", bucketName, objId, versionId);
	    checkAndReconnect();
            try (PreparedStatement pstSelectOneWithVersionId = getObjPreparedStmt(bucketName, DataRepositoryQuery.objSelectOneWithVersionIdQuery)){
                pstSelectOneWithVersionId.clearParameters();
                pstSelectOneWithVersionId.setString(1, objId);
                pstSelectOneWithVersionId.setString(2, versionId);
                ResultSet rs = pstSelectOneWithVersionId.executeQuery();
                logger.debug("[selectSingleObjectInternal] End bucketName : {} objId : {} versionId {} rowc: {}", bucketName, objId, versionId, rs.getRow());
                return getSelectObjectResult(bucketName, objId, rs);
            }
        } catch(SQLException ex){
            //System.out.println(" error : " + ex.getMessage());
            this.ex_message(ex);
            throw new ResourceNotFoundException("Object not found in the db : " + ex.getMessage());
        }
    }
    
    @Override
    public synchronized Metadata selectSingleObject(String diskPoolId, String bucketName, String path) throws ResourceNotFoundException {
        Metadata mt = new Metadata(bucketName, path);
       return selectSingleObjectInternal(bucketName, mt.getObjId()); 
    }
    
    @Override
    public synchronized Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objid) throws ResourceNotFoundException {
       return selectSingleObjectInternal(bucketName, objid); 
    }
    
    @Override
    public synchronized Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objid, String versionId) throws ResourceNotFoundException {
       return selectSingleObjectInternal(bucketName, objid, versionId); 
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
        String lastObjId;
        boolean thereIsMore;
       
        checkAndReconnect();	
        String[] bList= obmCache.getBucketNameList();
        for (String bucketName : bList){
            lastObjId = "";
            while(true){
                try (PreparedStatement pstSelectUsedDisks1 = getObjPreparedStmt(bucketName, DataRepositoryQuery.objSelectUsedDisksQuery)){
                    pstSelectUsedDisks1.setString(1, lastObjId);
                    pstSelectUsedDisks1.setLong(2, 1000);
                    ResultSet rs = pstSelectUsedDisks1.executeQuery();

                    thereIsMore = false;
                    while(rs.next()){
                         thereIsMore = true;
                        if (!dList.contains(rs.getString(1)))
                            dList.add(rs.getString(1));

                        if (!dList.contains(rs.getString(2)))
                            dList.add(rs.getString(2));
                        lastObjId = rs.getString(3);
                    }
                }
            
                if (!thereIsMore)
                    break;
            }
        }
        System.out.println("[getAllUsedDiskId] size of used Disk >" + dList.size());
        return dList;
    }
    
    @Override
    public void updateObjectTagging(Metadata mt) throws SQLException {
	checkAndReconnect();
        try (PreparedStatement pstUpdateTagging = getObjPreparedStmt(mt.getBucket(), DataRepositoryQuery.objUpdateTaggingQuery)){
            pstUpdateTagging.clearParameters();
            pstUpdateTagging.setString(1, mt.getTag());
            pstUpdateTagging.setString(2, mt.getMeta());
            pstUpdateTagging.setString(3, mt.getObjId());
            pstUpdateTagging.setString(4, mt.getVersionId());
            pstUpdateTagging.executeUpdate();
            insertObjTag(mt.getBucket(), mt.getObjId(), mt.getVersionId(), mt.getTag());
        }
    }
    
    @Override
    public void updateObjectAcl(Metadata mt) throws SQLException {
	checkAndReconnect();
        try (PreparedStatement pstUpdateAcl = getObjPreparedStmt(mt.getBucket(), DataRepositoryQuery.objUpdateAclQuery)){
            pstUpdateAcl.clearParameters();
            pstUpdateAcl.setString(1, mt.getAcl());
            pstUpdateAcl.setString(2, mt.getObjId());
            pstUpdateAcl.setString(3, mt.getVersionId());
            pstUpdateAcl.executeUpdate();
        }
    }
    
    @Override
    public void updateObjectMeta(Metadata mt) throws SQLException {
	checkAndReconnect();
        try (PreparedStatement pstUpdateObjectMeta = getObjPreparedStmt(mt.getBucket(), DataRepositoryQuery.objUpdateObjectMetaQuery)){
            pstUpdateObjectMeta.clearParameters();
            pstUpdateObjectMeta.setString(1, mt.getMeta());
            pstUpdateObjectMeta.setString(2, mt.getObjId());
            pstUpdateObjectMeta.setString(3, mt.getVersionId());
            pstUpdateObjectMeta.executeUpdate();
        }
    }
    
    private int updateVersionDelete(String bucketName, String objId) throws SQLException{ 
        try (PreparedStatement pstupdateLastVersionDelete = getObjPreparedStmt(bucketName, DataRepositoryQuery.objUpdateLastVersionDeleteQuery)){
            pstupdateLastVersionDelete.clearParameters();
            pstupdateLastVersionDelete.setString(1, objId);
            pstupdateLastVersionDelete.execute();
            return 0;
        }
    }

    @Override
    public int deleteObject(String bucketName, String path, String versionId) {
        try{
            String objId = new Metadata(bucketName, path).getObjId();
	    checkAndReconnect();
            try (PreparedStatement pststmt = getObjPreparedStmt(bucketName, DataRepositoryQuery.objDeleteQuery)){
                pststmt.clearParameters();
                pststmt.setString(1, objId);
                pststmt.setString(2, versionId);
                if (pststmt.executeUpdate()> 0)
                    updateBucketFileCount(bucketName, -1);
            }
            if (versionId != null){
                if (!versionId.isEmpty())
                    updateVersionDelete(bucketName, objId);
            }
            removeObjTag(bucketName, objId, versionId);
        } catch(SQLException ex){
            this.ex_message(ex);
            return -ex.getErrorCode();
        }
        return 0;
    }

    @Override
    public int markDeletedObject(String bucketName, String path, String versionId, String markDelete) throws SQLException{
        int ret;
        String objId = new Metadata(bucketName, path).getObjId();
	checkAndReconnect();
        try (PreparedStatement pstUpdateDeleteMarker = getObjPreparedStmt(bucketName, DataRepositoryQuery.objUpdateDeleteMarkerQuery)){
            pstUpdateDeleteMarker.clearParameters();
            pstUpdateDeleteMarker.setString(1, markDelete);
            pstUpdateDeleteMarker.setBoolean(2, false);
            pstUpdateDeleteMarker.setString(3, objId);
            pstUpdateDeleteMarker.setString(4, versionId);
            ret= pstUpdateDeleteMarker.executeUpdate();
        }
        if (ret  > 0){
            if (versionId != null){
                if (!versionId.isEmpty())
                    updateVersionDelete(bucketName, objId);
            }
        }
        return ret;
    }
    
    @Override
    public boolean isBucketDeleted(String bucket) throws SQLException {
	checkAndReconnect();
        try (PreparedStatement pstIsDeleteBucket1 = getObjPreparedStmt(bucket, DataRepositoryQuery.objIsDeleteBucketQuery)){
            pstIsDeleteBucket1.clearParameters();
            //pstIsDeleteBucket1.setString(1, bucket);
            ResultSet rs = pstIsDeleteBucket1.executeQuery();
        
            if (rs == null) {
                return true;
            }

            if (rs.next()) {
                return false;
            }
        }
        return true;
    }
    //1 bucketName, 2 bucketId, 3 diskPoolId, 4 userName, 5 user, 6 acl, 7 encryption, 8 objectlock, 
    //9 replicaCount, 10 objTagIndexing, 11 accelerate, 12 payment, 13 notification
    @Override
    public synchronized Bucket insertBucket(Bucket bt) 
            throws ResourceAlreadyExistException{
        try{
            checkAndReconnect();
            createObjectTable(bt.getName());
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
            this.pstInsertBucket.setBoolean(10, bt.isObjectTagIndexEnabled());
            //this.pstInsertBucket.setString(11, bt.getAnalytics());
            this.pstInsertBucket.setString(11, bt.getAccelerate());
            this.pstInsertBucket.setString(12, bt.getPayment());
            this.pstInsertBucket.setString(13, bt.getNotification());
            //System.out.println(">>QUERY >>" + pstInsertBucket.toString());
            this.pstInsertBucket.executeUpdate();
        } catch(SQLException ex){
            System.out.println("SQLException:>" + ex);
            String str = String.format("name : %s, id : %s, diskPoolId : %s, userName : %s  userId : %s, acl : %s, encryption : %s, objectlock : %s, replicaCount : %s, objTagIndexing : %s"
                    , bt.getName(), bt.getId(), bt.getDiskPoolId(), bt.getUserName(), bt.getUserId(),  bt.getAcl(), bt.getEncryption(), bt.getObjectLock(), bt.getReplicaCount(), bt.isObjectTagIndexEnabled());
            throw new ResourceAlreadyExistException(String.format("Bucket(%s) is laready exist in the db!\n", str, ex));
        }
        try {
            createObjectTagIndexingTable(bt.getName());
        } catch (SQLException ex) {
           logger.error(ex.getMessage());
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
            checkAndReconnect();
            this.pstDeleteBucket.clearParameters();
            this.pstDeleteBucket.setString(1, getBucketId(bucketName));
            this.pstDeleteBucket.executeUpdate();
        } catch(SQLException ex){
            this.ex_message(ex);
            return -ex.getErrorCode();
        }
        return 0;
    }
    // 1bucketName, 2bucketId, 3diskPoolId, 4versioning, 5MfaDelete, 6userName, 7user, 8acl, 9web, 10cors, 11lifecycle, 12access, 13tagging, 
    //14replication, 15encryption, 16objectlock, 17policy, 18createTime, 19replicaCount, 20usedSpace, 21fileCount, 22logging, 23objTagIndexing, 24accelerate, 25payment, 26notification
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
        String logging = rs.getString(22);
        boolean isObjTagIndexing = rs.getBoolean(23);
        String accelerate = rs.getString(24);
        String payment = rs.getString(25);
        String notification = rs.getString(26);
        String analytics = rs.getString(27);
        String inventory = rs.getString(28);

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
        //getUserDiskPool(bt); // get diskpoolId and replicaCount
        bt.setUsedSpace(usedSpace);
        bt.setFileCount(fileCount);
        bt.setLogging(logging);
        bt.setObjectTagIndexEnabled(isObjTagIndexing);
        bt.setAnalytics(analytics);
        bt.setAccelerate(accelerate);
        bt.setPayment(payment);
        bt.setNotification(notification);
	bt.setInventory(inventory);
        return bt;
    }
    
    @Override
    public synchronized Bucket selectBucket(String bucketName) throws ResourceNotFoundException, SQLException{
        logger.debug("[selectBucket] Begin bucketName : {} ", bucketName);
	checkAndReconnect();
        this.pstSelectBucket.clearParameters();
        this.pstSelectBucket.setString(1, getBucketId(bucketName));
        ResultSet rs = this.pstSelectBucket.executeQuery();

        if(rs.next()){
           logger.debug("[selectBucket] End bucketName : {} ret :  0", bucketName);
           return parseBucket(rs);
        }
        logger.debug("[selectBucket] End bucketName : {} ret :  -2", bucketName);
        throw new ResourceNotFoundException("Bucket("+bucketName+") is not found in the db");
    }
    
    private void _loadBucketList()throws SQLException{
        Bucket bt;
           
        ResultSet rs = pstSelectAllBucket.executeQuery();

        while(rs.next()){
            bt = parseBucket(rs);

            obmCache.setBucketInCache(bt);
        }
    }
    
    @Override
    public synchronized void loadBucketList() {
        try {
	   checkAndReconnect();
           _loadBucketList(); 
        } catch (SQLException ex1) {
            try { 
                _loadBucketList(); // to fix connection reset by peer
            } catch (SQLException ex) {
                logger.error(ex.getMessage());
            }
        }
    }
    
    @Override
    public synchronized List<Bucket> getBucketList() {
        try {
            List<Bucket> btList = new ArrayList();
            checkAndReconnect(); 
            ResultSet rs = this.pstSelectAllBucket.executeQuery();
            
            while(rs.next()){
                Bucket bt = parseBucket(rs);
                btList.add(bt);
            }
            return btList;
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            logger.error(ex.getMessage());
            //Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, "failed to loadBucketList due to sql error!", ex);
        }
        return null;
    }
    
    @Override
    public synchronized int updateBucketVersioning(Bucket bt) {
        try {
	    checkAndReconnect();
            this.pstUpdateBucket.clearParameters();
            pstUpdateBucket.setString(1, bt.getVersioning());
            pstUpdateBucket.setString(2, getBucketId(bt.getName()));
            return pstUpdateBucket.executeUpdate();
        } catch (SQLException ex) {
            logger.error(ex.getMessage());
            //Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    @Override
    public synchronized void updateBucketObjTagIndexing(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateObjTagIndexBucket.clearParameters();
        pstUpdateObjTagIndexBucket.setBoolean(1, bt.isObjectTagIndexEnabled());
        pstUpdateObjTagIndexBucket.setString(2, getBucketId(bt.getName()));
        pstUpdateObjTagIndexBucket.executeUpdate();
    }
    
    @Override
    public  synchronized int insertMultipartUpload(Metadata mt, String uploadid, int partNo) throws SQLException{
	checkAndReconnect();
        pstInsertMultiPart.clearParameters();
        pstInsertMultiPart.setString(1, mt.getBucket());
        pstInsertMultiPart.setString(2, mt.getPath());
        pstInsertMultiPart.setString(3, uploadid);
        pstInsertMultiPart.setInt(4, partNo);
        pstInsertMultiPart.setString(5, mt.getAcl());
        pstInsertMultiPart.setString(6, mt.getMeta());
        pstInsertMultiPart.setString(7, mt.getEtag());
        pstInsertMultiPart.setLong(8, mt.getSize());
        pstInsertMultiPart.setString(9, mt.getPrimaryDisk().getId());
        try {
            pstInsertMultiPart.setString(10, mt.getReplicaDisk().getId());
        } catch (ResourceNotFoundException ex) {
            pstInsertMultiPart.setString(10, "");
        }
        pstInsertMultiPart.setString(11, ""); // for partRef
        pstInsertMultiPart.execute();
	logger.debug("[insertMultipartUpload] bucketName : {} uploadid :{} partNo :{}  iscompleted :{} etag :{}", mt.getBucket(), uploadid, partNo, false, mt.getEtag());
        return 0;
    }
  
    @Override
    public synchronized int updateMultipartUpload(Metadata mt,  String uploadid, int partNo, boolean iscompleted) throws SQLException{
	checkAndReconnect();
        pstUpdateMultiPart.clearParameters();
        pstUpdateMultiPart.setBoolean(1, iscompleted);
        pstUpdateMultiPart.setString( 2, mt.getMeta());
        pstUpdateMultiPart.setString( 3, mt.getEtag());
        pstUpdateMultiPart.setLong(   4, mt.getSize());
        pstUpdateMultiPart.setString(5, uploadid);
        pstUpdateMultiPart.setInt(6, partNo);
        pstUpdateMultiPart.execute();
	logger.debug("[updateMultipartUpload] bucketName : {} uploadid :{} partNo :{}  iscompleted :{} etag :{}", mt.getBucket(), uploadid, partNo, false, mt.getEtag());
        return 0;
    }
    
    @Override
    public synchronized int deleteMultipartUpload(String bucket,  String uploadid) throws SQLException{
	checkAndReconnect();
        pstDeleteMultiPart.clearParameters();
        pstDeleteMultiPart.setString(1, uploadid);
        pstDeleteMultiPart.execute();
        return 0;
    }
      
    @Override
    public synchronized List<Integer> selectMultipart(String bucket, String uploadid, int maxParts, int partNoMarker) throws SQLException{
        List<Integer> list=new ArrayList<>();
	checkAndReconnect();
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
        Date lastModified;
        boolean isTrancated =false;
        
        checkAndReconnect();	
        PreparedStatement stmt = this.con.prepareStatement(query.toString());
   
        ResultSet rs = stmt.executeQuery();
        while(rs.next()){
            key =          rs.getString(1) ;
            uploadid=      rs.getString(2);
            partNo =       rs.getInt(3);
            lastModified = (Date)rs.getObject(4);

            if (++counter == maxKeys)
                isTrancated = !rs.isLast();
            
            callback.call(key, uploadid, lastModified == null ? "" : lastModified.toString(), partNo, "", "", "", isTrancated);
        }
    }
        
    @Override
    public Metadata getObjectWithUploadIdPart(String diskPoolId, String uploadId, int partNo) throws SQLException{
        checkAndReconnect();	
        pstIsUploadPartNo.clearParameters();
        pstIsUploadPartNo.setString(1, uploadId);
        pstIsUploadPartNo.setInt(2, partNo);
        ResultSet rs = pstIsUploadPartNo.executeQuery();
        if (!rs.next()){
            return null;
        }
        
        String bucketName = rs.getString(1);
        String objkey     = rs.getString(2);
        String acl        = rs.getString(3);
        String meta       = rs.getString(4);
        String etag       = rs.getString(5);
        long size         = rs.getLong(6);
        String pdiskId    = rs.getString(7);
        
        Metadata mt;
        DISK pdsk;
        try {
            pdsk = pdiskId != null ? obmCache.getDiskWithId(pdiskId) : new DISK();
        } catch (ResourceNotFoundException ex) {
             pdsk = new DISK();
        }
        
        mt = new Metadata(bucketName, objkey);
        mt.set(etag, "", meta, acl, size);
        mt.setPrimaryDisk(pdsk);
        mt.setReplicaDISK(new DISK());
        
        return mt;
    }
    
    @Override
    public Metadata selectSingleObject(String diskPoolId, String bucketName, String path, String versionId)
            throws ResourceNotFoundException {
        Metadata mt = new Metadata(bucketName, path);
        return selectSingleObjectInternal(bucketName, mt.getObjId(), versionId); 
    }

    /***********************START*******************************************************************/
    // TO BE
    @Override
    public Multipart getMulipartUpload(String uploadid) throws SQLException, ResourceNotFoundException  {
        Multipart multipart = null;
        String pdiskid;

        checkAndReconnect();
        this.pstGetMultiPart.clearParameters();
        this.pstGetMultiPart.setString(1, uploadid);
        ResultSet rs = this.pstGetMultiPart.executeQuery();
    
        if (rs.next()) {
            multipart = new Multipart(rs.getString(1), rs.getString(2), rs.getString(4));
            multipart.setLastModified((Date)rs.getObject(3));
            multipart.setAcl(rs.getString(5));
            multipart.setMeta(rs.getString(6));
            pdiskid = rs.getString(7);
            DISK dsk = obmCache.getDiskWithId(pdiskid);
            multipart.setDiskPoolId(dsk.getDiskPoolId());
        }
        
        return multipart;
    }

    // TO BE
    @Override
    public SortedMap<Integer, Part> getParts(String uploadId) throws SQLException {
        SortedMap<Integer, Part> listPart = new TreeMap<>();
	checkAndReconnect();
        this.pstGetParts.clearParameters();
        this.pstGetParts.setString(1, uploadId);
        ResultSet rs = this.pstGetParts.executeQuery();

        while (rs.next()) {
            Part part = new Part();
            part.setLastModified((Date)rs.getObject(1));
            part.setPartETag(rs.getString(2));
            part.setPartSize(rs.getLong(3));
            part.setPartNumber(rs.getInt(4));
            part.setPrimaryDiskId(rs.getString(5));
            part.setReplicaDiskId(rs.getString(6));
            listPart.put(part.getPartNumber(), part);
        }

        return listPart;
    }

    // TO BE
    @Override
    public ResultParts getParts(String uploadId, int partNumberMarker, int maxParts) throws SQLException {
	checkAndReconnect();
        ResultParts resultParts = new ResultParts(uploadId, maxParts);
        resultParts.setListPart(new TreeMap<Integer, Part>());
        
        pstGetPartsMax.clearParameters();
        pstGetPartsMax.setString(1, uploadId);
        pstGetPartsMax.setInt(2, partNumberMarker);
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
            part.setPrimaryDiskId(rs.getString(5));
            part.setReplicaDiskId(rs.getString(6));
            resultParts.getListPart().put(part.getPartNumber(), part);
        }

        return resultParts;
    }

    // TO BE
    @Override
    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException {
	checkAndReconnect();
        ResultUploads resultUploads = new ResultUploads();
        resultUploads.setList(new ArrayList<>());

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
    
    @Override
    public String getPartRef(String uploadId, int partNo) throws SQLException, ResourceNotFoundException {
        String partRef = " ";
	checkAndReconnect();
        pstGetPartRef.clearParameters();
        pstGetPartRef.setString(1, uploadId);
        pstGetPartRef.setInt(2, partNo);
        ResultSet rs = pstGetPartRef.executeQuery();

        while (rs.next()) {
            partRef = rs.getString(1);
          
        }
        return partRef;
    }
    
    @Override
    public int setPartRef(String uploadId, int partNo, String partRef) throws SQLException, ResourceNotFoundException {
        checkAndReconnect();
	pstSetPartRef.clearParameters();
        pstSetPartRef.setString(1, uploadId);
        pstSetPartRef.setInt(2, partNo);
        pstSetPartRef.setString(3, partRef);
        pstSetPartRef.execute();
        return 0;
    }
    
/***********************************************END*****************************************************************************/


    @Override
    public void updateBucketAcl(Bucket bt) throws SQLException {
        checkAndReconnect();
	pstUpdateBucketAcl.clearParameters();
        pstUpdateBucketAcl.setString(1, bt.getAcl());
        pstUpdateBucketAcl.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketAcl.executeUpdate();
       
    }

    @Override
    public void updateBucketCors(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketCors.clearParameters();
        pstUpdateBucketCors.setString(1, bt.getCors());
        pstUpdateBucketCors.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketCors.executeUpdate();
    }

    @Override
    public void updateBucketWeb(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketWeb.clearParameters();
        pstUpdateBucketWeb.setString(1, bt.getWeb());
        pstUpdateBucketWeb.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketWeb.executeUpdate();
      
    }

    @Override
    public void updateBucketLifecycle(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketLifecycle.clearParameters();
        pstUpdateBucketLifecycle.setString(1, bt.getLifecycle());
        pstUpdateBucketLifecycle.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketLifecycle.executeUpdate();
        
    }

    @Override
    public void updateBucketAccess(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketAccess.clearParameters();
        pstUpdateBucketAccess.setString(1, bt.getAccess());
        pstUpdateBucketAccess.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketAccess.executeUpdate();
        
    }

    @Override
    public void updateBucketTagging(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketTagging.clearParameters();
        pstUpdateBucketTagging.setString(1, bt.getTagging());
        pstUpdateBucketTagging.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketTagging.executeUpdate();
        
    }

    @Override
    public void updateBucketReplication(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketReplication.clearParameters();
        pstUpdateBucketReplication.setString(1, bt.getReplication());
        pstUpdateBucketReplication.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketReplication.executeUpdate();
       
    }
 
    @Override
    public void updateBucketEncryption(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketEncryption.clearParameters();
        pstUpdateBucketEncryption.setString(1, bt.getEncryption());
        pstUpdateBucketEncryption.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketEncryption.executeUpdate();
    }

    @Override
    public void updateBucketObjectLock(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketObjectLock.clearParameters();
        pstUpdateBucketObjectLock.setString(1, bt.getObjectLock());
        pstUpdateBucketObjectLock.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketObjectLock.executeUpdate();
        
    }
    
    @Override
    public void updateBucketPolicy(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketPolicy.clearParameters();
        pstUpdateBucketPolicy.setString(1, bt.getPolicy());
        pstUpdateBucketPolicy.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketPolicy.executeUpdate();
    }
   
    private void updateBucketFileCount(String bucketName, long count) throws SQLException {
        /*pstUpdateBucketFilecount.clearParameters();
        pstUpdateBucketFilecount.setLong(1, count);
        pstUpdateBucketFilecount.setString(2, getBucketId(bucketName));
        pstUpdateBucketFilecount.executeUpdate();*/
    }

    @Override
    public void updateBucketUsedSpace(Bucket bt, long size) throws SQLException {
        pstUpdateBucketUsedSpace.clearParameters();
        pstUpdateBucketUsedSpace.setLong(1, size);
        pstUpdateBucketUsedSpace.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketUsedSpace.executeUpdate();
    }
 
    @Override
    public void updateBucketLogging(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateBucketLogging.clearParameters();
        pstUpdateBucketLogging.setString(1, bt.getLogging());
        pstUpdateBucketLogging.setString(2, getBucketId(bt.getName()));
        pstUpdateBucketLogging.executeUpdate();
    }
    
    /*private String getBucketAnalyticsConfigurationsAsString(String bucketName) throws SQLException{
        String analytics = "";
        pstSelectAnalyticsBucket.clearParameters();
        pstSelectAnalyticsBucket.setString(1, getBucketId(bucketName));
        ResultSet rs = this.pstSelectAnalyticsBucket.executeQuery();

        if  (rs.next()) {
            analytics = rs.getString(1);
        }
        return analytics;
    }*/
    
    @Override
    public void updateBucketAnalyticsConfiguration(Bucket bt ) throws SQLException {
	checkAndReconnect();
        pstUpdateAnalyticsBucket.clearParameters();
        pstUpdateAnalyticsBucket.setString(1, bt.getAnalytics());
        pstUpdateAnalyticsBucket.setString(2, getBucketId(bt.getName()));
        pstUpdateAnalyticsBucket.executeUpdate();
    }
    
    /*@Override
    public int putBucketAnalyticsConfiguration(String bucketName, String id, String analytics) throws SQLException {
        List<BasicDBObject> configList;
        BasicDBObject config = new BasicDBObject("config", analytics);
        config.append("id", id);
        
        String configListstr = getBucketAnalyticsConfigurationsAsString(bucketName);
        if (configListstr.isEmpty())
            configList = new ArrayList();
        else{
            configList = (List<BasicDBObject>) BasicDBObject.parse(configListstr);
            if (configList.size() >= 1000)
                return -500;
            
            if (configList.contains(config))
                return -17;
        }
        configList.add(config);
        updateBucketAnalytics(bucketName, Arrays.toString(configList.toArray()));
        return 0;
    }
    
    @Override
    public BucketAnalytics listBucketAnalyticsConfiguration(String bucketName, String lastId) throws SQLException{
        String configListstr = getBucketAnalyticsConfigurationsAsString(bucketName);
        boolean foundStart = false;
        BucketAnalytics lst = new BucketAnalytics();
        
        if (Strings.isNullOrEmpty(lastId))
            foundStart = true;
        
        if (!configListstr.isEmpty()){
            List<BasicDBObject> configList = (List<BasicDBObject>) BasicDBObject.parse(configListstr);
            for (BasicDBObject cnf :configList ){
                if (foundStart){
                    if (lst.getConfig().size() == 100){
                        lst.setLastId(cnf.getString("id"));
                        lst.setTruncated(true);
                        break;
                    }
                    lst.getConfig().add(cnf.getString("config"));
                }
            
                if (foundStart == false) 
                   if (cnf.getString("id").equals(lastId))
                       foundStart = true;
            }
        }
        return lst;
    }
    
    @Override
    public String getBucketAnalyticsConfiguration(String bucketName, String id) throws SQLException{
        String config = "";
        String configListstr = getBucketAnalyticsConfigurationsAsString(bucketName);
        if (!configListstr.isEmpty()){
            List<BasicDBObject> configList = (List<BasicDBObject>) BasicDBObject.parse(configListstr);
            for (BasicDBObject cnf :configList ){
                if (cnf.getString("id").equals(id))
                    config = cnf.getString("config");
            }
        }
        return config;
    }
    
    @Override
    public void deleteBucketAnalyticsConfiguration(String bucketName, String id) throws SQLException{
        String configListstr = getBucketAnalyticsConfigurationsAsString(bucketName);
        if (!configListstr.isEmpty()){
            List<BasicDBObject> configList = (List<BasicDBObject>) BasicDBObject.parse(configListstr);
            for (BasicDBObject cnf :configList ){
                if (cnf.getString("id").equals(id)){
                    configList.remove(cnf);
                    updateBucketAnalytics(bucketName, Arrays.toString(configList.toArray()));
                    break;
                }
            }
        }
    }*/
    
    @Override
    public void updateBucketAccelerate(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateAccelerateBucket.clearParameters();
        pstUpdateAccelerateBucket.setString(1, bt.getAccelerate());
        pstUpdateAccelerateBucket.setString(2, getBucketId(bt.getName()));
        pstUpdateAccelerateBucket.executeUpdate();
    }
    
    @Override
    public void updateBucketPayment(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdatePaymentBucket.clearParameters();
        pstUpdatePaymentBucket.setString(1, bt.getPayment());
        pstUpdatePaymentBucket.setString(2, getBucketId(bt.getName()));
        pstUpdatePaymentBucket.executeUpdate();
    }
    
    @Override
    public void updateBucketNotification(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateNotificationBucket.clearParameters();
        pstUpdateNotificationBucket.setString(1, bt.getNotification());
        pstUpdateNotificationBucket.setString(2, getBucketId(bt.getName()));
        pstUpdateNotificationBucket.executeUpdate();
    }
    
    /*private String getBucketInventoryConfigurationsAsString(String bucketName) throws SQLException{
        String inventory = "";
        pstSelectInventoryBucket.clearParameters();
        pstSelectInventoryBucket.setString(1, getBucketId(bucketName));
        ResultSet rs = this.pstSelectInventoryBucket.executeQuery();

        if  (rs.next()) {
            inventory = rs.getString(1);
        }
        return inventory;
    }*/
   
    @Override
    public void updateBucketInventoryConfiguration(Bucket bt) throws SQLException {
	checkAndReconnect();
        pstUpdateInventoryBucket.clearParameters();
        pstUpdateInventoryBucket.setString(1, bt.getInventory());
        pstUpdateInventoryBucket.setString(2, getBucketId(bt.getName()));
        pstUpdateInventoryBucket.executeUpdate();
    }
    
    /* @Override
    public int putBucketInventoryConfiguration(String bucketName, String id, String analytics) throws SQLException {
        List<BasicDBObject> configList;
        BasicDBObject config = new BasicDBObject("config", analytics);
        config.append("id", id);
        
        String configListstr = getBucketInventoryConfigurationsAsString(bucketName);
        if (configListstr.isEmpty())
            configList = new ArrayList();
        else{
            configList = (List<BasicDBObject>) BasicDBObject.parse(configListstr);
            if (configList.size() >= 1000)
                return -500;
            
            if (configList.contains(config))
                return -17;
        }
        configList.add(config);
        updateBucketInventory(bucketName, Arrays.toString(configList.toArray()));
        return 0;
    }
    
    @Override
    public BucketAnalytics listBucketInventoryConfiguration(String bucketName, String lastId) throws SQLException{
        String configListstr = getBucketInventoryConfigurationsAsString(bucketName);
        boolean foundStart = false;
        BucketAnalytics lst = new BucketAnalytics();
        
        if (Strings.isNullOrEmpty(lastId))
            foundStart = true;
        
        if (!configListstr.isEmpty()){
            List<BasicDBObject> configList = (List<BasicDBObject>) BasicDBObject.parse(configListstr);
            for (BasicDBObject cnf :configList ){
                if (foundStart){
                    if (lst.getConfig().size() == 100){
                        lst.setLastId(cnf.getString("id"));
                        lst.setTruncated(true);
                        break;
                    }
                    lst.getConfig().add(cnf.getString("config"));
                }
            
                if (foundStart == false) 
                   if (cnf.getString("id").equals(lastId))
                       foundStart = true;
            }
        }
        return lst;
    }
    
    @Override
    public String getBucketInventoryConfiguration(String bucketName, String id) throws SQLException{
        String config = "";
        String configListstr = getBucketInventoryConfigurationsAsString(bucketName);
        if (!configListstr.isEmpty()){
            List<BasicDBObject> configList = (List<BasicDBObject>) BasicDBObject.parse(configListstr);
            for (BasicDBObject cnf :configList ){
                if (cnf.getString("id").equals(id))
                    config = cnf.getString("config");
            }
        }
        return config;
    }
    
    @Override
    public void deleteBucketInventoryConfiguration(String bucketName, String id) throws SQLException{
        String configListstr = getBucketInventoryConfigurationsAsString(bucketName);
        if (!configListstr.isEmpty()){
            List<BasicDBObject> configList = (List<BasicDBObject>) BasicDBObject.parse(configListstr);
            for (BasicDBObject cnf :configList ){
                if (cnf.getString("id").equals(id)){
                    configList.remove(cnf);
                    updateBucketInventory(bucketName, Arrays.toString(configList.toArray()));
                    break;
                }
            }
        }
    }*/
    
    @Override
    public boolean isUploadId(String uploadid) throws SQLException {
        boolean success = false;
	checkAndReconnect();
        this.pstIsUpload.clearParameters();
        this.pstIsUpload.setString(1, uploadid);
        ResultSet rs = this.pstIsUpload.executeQuery();
    
        if (rs.next()) {
            success = true;
        }
        return success;
    }
    
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
           checkAndReconnect();
           // System.out.format("Operation : %s check : %s \n", operation, operation.equalsIgnoreCase("addJob"));
            if (operation.equalsIgnoreCase("addJob")){
                //System.out.format("Operation : %s \n", operation);
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
    public PreparedStatement getStatement(String query) throws SQLException{
        checkAndReconnect();	    
        return this.con.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
    }
    
    private LocalDateTime convertDate(String date2Convert){
        //System.out.println("date2Convert>>" + date2Convert);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
        LocalDateTime dateTime = LocalDateTime.parse(date2Convert, formatter);
        return dateTime;
    }
    
    @Override
    public List<Metadata> getObjectList(String bucketName, Object pstmt, int maxKeys, long offset) throws SQLException{
        List<Metadata> list = new ArrayList();
        ResultSet rs;
        DISK pdsk = null;
        DISK rdsk;
        int default_replicaCount;
       
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
                pdsk = obmCache.getDiskWithId( pdiskid);
                if (!rdiskid.isEmpty())
                    rdsk = this.obmCache.getDiskWithId( rdiskid);
                else
                    rdsk = new DISK();
                default_replicaCount = 2;
            } catch (ResourceNotFoundException ex) {
                if ( pdsk == null)
                    pdsk = new DISK();
                rdsk = new DISK();
                 default_replicaCount = 1;
            }
            
            int replicaCount;
            try {
                replicaCount = obmCache.getDiskPoolFromCache(pdsk.getDiskPoolId()).getDefaultReplicaCount();
            } catch (ResourceNotFoundException ex) {
                replicaCount = default_replicaCount;
            }
            Metadata mt = new Metadata(bucketName, objKey);
            mt.setMeta(meta);
            mt.set(etag, tag, meta, acl, size);
            mt.setLastModified(lastModified);
            mt.setVersionId(versionid, "", lastversion);
            mt.setPrimaryDisk(pdsk);
            mt.setReplicaDISK(rdsk);
            mt.setReplicaCount(replicaCount);
            list.add(mt);
        }
        
        rs.close();
        ((PreparedStatement)pstmt).close();
        
        return list;
    }
    
    @Override
    public long getObjectListCount(String bucketName, Object pstmt) throws SQLException {
        ResultSet rs = null;
        long rowCount = 0;
        
        rs = ((PreparedStatement)pstmt).executeQuery();
        if (rs.next()) {
           rowCount = rs.getLong(0); 
        }
        return rowCount;
    }

    private void insertLifeCycle(String eventName, LifeCycle lc) throws SQLException{
        try (PreparedStatement pstInsertLifeCycle = this.getObjPreparedStmt(eventName, DataRepositoryQuery.insertLifeCycleQuery)){
            pstInsertLifeCycle.clearParameters();
            pstInsertLifeCycle.setLong(  1, lc.getIndex());
            pstInsertLifeCycle.setString(2, lc.getBucketName());
            pstInsertLifeCycle.setString(3, lc.getKey());
            pstInsertLifeCycle.setString(4, lc.getObjId());
            pstInsertLifeCycle.setString(5, lc.getVersionId());
            pstInsertLifeCycle.setString(6, lc.getUploadId());
            pstInsertLifeCycle.setString(7, lc.getLog());
            pstInsertLifeCycle.execute();
        }
    }
    
 
    private List<LifeCycle> parseSelectLifeCycle(ResultSet rs, boolean isFailed) throws SQLException{
        List<LifeCycle> list = new ArrayList();
        while (rs.next()) {
            long idx = rs.getLong("idx");
            String bucket = rs.getString("bucket");
            String objKey = rs.getString("objKey");
            String versionid = rs.getString("versionid");
            String uploadid = rs.getString("uploadid");
            Date inDate = rs.getDate("inDate");
            String log = rs.getString("log");
            //boolean isfailed = rs.getBoolean("isFailed");
            LifeCycle slf = new LifeCycle(idx, bucket, objKey, versionid, uploadid, log);
            slf.setFailedEvent(isFailed);
            slf.setInDate(inDate);
            list.add(slf);
        }
        return list;
    }
    
    
    private LifeCycle selectLifeCycle(String eventName, LifeCycle lc) throws SQLException{
        try (PreparedStatement pstSelectLifeCycle = this.getObjPreparedStmt(eventName, DataRepositoryQuery.selectLifeCycleQuery)){
            pstSelectLifeCycle.setString(1, lc.getObjId());
            pstSelectLifeCycle.setString(2, lc.getVersionId());
            ResultSet rs = pstSelectLifeCycle.executeQuery();
            List<LifeCycle> list = parseSelectLifeCycle(rs, eventName.equals(DataRepositoryQuery.lifeCycleFailedEventTableName));
            if (list.isEmpty())
                return null;

            return list.get(0);
        }
    }
    
    private LifeCycle selectByUploadIdLifeCycle(String eventName, String uploadId) throws SQLException{
        try (PreparedStatement pstSelectByUploadIdLifeCycle = this.getObjPreparedStmt(eventName, DataRepositoryQuery.selectByUploadIdLifeCycleQuery)){
            pstSelectByUploadIdLifeCycle.setString(1, uploadId);
            ResultSet rs = pstSelectByUploadIdLifeCycle.executeQuery();
            List<LifeCycle> list = parseSelectLifeCycle(rs, eventName.equals(DataRepositoryQuery.lifeCycleFailedEventTableName));
            if (list.isEmpty())
                return null;

            return list.get(0);
        }
    }
   
    private List<LifeCycle> selectAllLifeCycle(String eventName) throws SQLException{
        try (PreparedStatement pstSelectAllLifeCycle = this.getObjPreparedStmt(eventName, DataRepositoryQuery.selectAllLifeCycleQuery)){
            ResultSet rs = pstSelectAllLifeCycle.executeQuery();
            return parseSelectLifeCycle(rs, eventName.equals(DataRepositoryQuery.lifeCycleFailedEventTableName));
        }
    }
    
    private int deleteLifeCycle(String eventName, LifeCycle lc) throws SQLException{
        try (PreparedStatement pstDeleteLifeCycle = this.getObjPreparedStmt(eventName, DataRepositoryQuery.deleteLifeCycleQuery)){
            pstDeleteLifeCycle.setString(1, lc.getObjId());
            pstDeleteLifeCycle.setString(2, lc.getVersionId());
            return pstDeleteLifeCycle.executeUpdate();
        }
    }
    
    @Override
    public void insertLifeCycle(LifeCycle lc) throws SQLException{
	checkAndReconnect();
        insertLifeCycle(DataRepositoryQuery.lifeCycleEventTableName, lc);
    }
    
    @Override
    public void insertFailedLifeCycle(LifeCycle lc) throws SQLException{
	checkAndReconnect();
        insertLifeCycle(DataRepositoryQuery.lifeCycleFailedEventTableName, lc);
    }
    
    @Override
    public LifeCycle selectLifeCycle(LifeCycle lc) throws SQLException{
	checkAndReconnect();
        return selectLifeCycle(DataRepositoryQuery.lifeCycleEventTableName, lc);
    }
    
    @Override
    public LifeCycle selectFailedLifeCycle(LifeCycle lc) throws SQLException{
	checkAndReconnect();
        return selectLifeCycle(DataRepositoryQuery.lifeCycleFailedEventTableName, lc);
    }
    
    @Override
    public LifeCycle selectByUploadIdLifeCycle(String uploadId) throws SQLException{
	checkAndReconnect();
        return selectByUploadIdLifeCycle(DataRepositoryQuery.lifeCycleEventTableName, uploadId);
    }
    
    @Override
    public LifeCycle selectByUploadIdFailedLifeCycle(String uploadId) throws SQLException{
	checkAndReconnect();
        return selectByUploadIdLifeCycle(DataRepositoryQuery.lifeCycleFailedEventTableName, uploadId);
    }
    
    @Override
    public List<LifeCycle> selectAllLifeCycle() throws SQLException{
	checkAndReconnect();
        return selectAllLifeCycle(DataRepositoryQuery.lifeCycleEventTableName);
    }
    
    @Override
    public List<LifeCycle> selectAllFailedLifeCycle() throws SQLException{
	checkAndReconnect();
        return selectAllLifeCycle(DataRepositoryQuery.lifeCycleFailedEventTableName);
    }
    
    @Override
    public int deleteLifeCycle(LifeCycle lc) throws SQLException{
	checkAndReconnect();
        return deleteLifeCycle(DataRepositoryQuery.lifeCycleEventTableName, lc);
    }
    
    @Override
    public int deleteFailedLifeCycle(LifeCycle lc) throws SQLException{
	checkAndReconnect();
        return deleteLifeCycle(DataRepositoryQuery.lifeCycleFailedEventTableName, lc);
    }
    
    private void createObjectTagIndexingTable(String bucketName) throws SQLException{
        try (PreparedStatement pstStmt = getObjPreparedStmt(bucketName + DataRepositoryQuery.tagIndexingTablePrefexi, DataRepositoryQuery.createTagIndexingQuery)){
            pstStmt.execute();
        }
    }
    
    private int insertObjTag(String bucketName, String objId, String versionId, String tags) {
        
        try {
            String tkey;
            String tvalue;
            Bucket bt;
            int ret = 0;
            
            String tagsJson = convertXML2Json(tags);
            if (tagsJson == null)
                return 0; // ignore
            
            if (tagsJson.isEmpty())
                return 0; // ignore
            
            try {
                bt = selectBucket(bucketName);
                if (!bt.isObjectTagIndexEnabled())
                    return 0;
            } catch (ResourceNotFoundException | SQLException  ex) {
                return 0;
            }
            
            //removeObjTag(bucketName, objId, versionId, tags);
            try (PreparedStatement pstInsertStmt = getObjPreparedStmt(bucketName + DataRepositoryQuery.tagIndexingTablePrefexi, DataRepositoryQuery.insertTagIndexingQuery)){
            
                pstInsertStmt.clearParameters();
                pstInsertStmt.setString(1, objId);
                pstInsertStmt.setString(2, versionId);
                Document tagDoc = Document.parse(tagsJson);
                Iterator it = tagDoc.keySet().iterator();
                while(it.hasNext()){
                    tkey = (String)it.next();
                    tvalue = tagDoc.getString(tkey);
                    pstInsertStmt.setString(3, tkey);
                    pstInsertStmt.setString(4, tvalue);
                    if (pstInsertStmt.execute())
                        ret++;
                }
                return ret;
            }
        } catch (SQLException  ex) {
            logger.error(ex.getMessage());
            //Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
    private int removeObjTag(String bucketName, String objId, String versionId) throws SQLException{
        try (PreparedStatement pstdeleteStmt = getObjPreparedStmt(bucketName + DataRepositoryQuery.tagIndexingTablePrefexi, DataRepositoryQuery.deleteTagIndexingQuery1)){
            pstdeleteStmt.clearParameters();
            pstdeleteStmt.setString(1, objId);
            pstdeleteStmt.setString(2, versionId);
            return pstdeleteStmt.executeUpdate();
        }
    }
    
    private int removeObjTag(String bucketName, String objId, String versionId, String tags) throws SQLException{
        String key;
        String tagsJson = convertXML2Json(tags);
        if (tagsJson == null)
            return  0;
        
        if (tagsJson.isEmpty())
            return 0;
        
        try (PreparedStatement pstdeleteStmt = getObjPreparedStmt(bucketName + DataRepositoryQuery.tagIndexingTablePrefexi, DataRepositoryQuery.deleteTagIndexingQuery2)){
            pstdeleteStmt.clearParameters();
            pstdeleteStmt.setString(1, objId);
            pstdeleteStmt.setString(2, versionId);
            Document doc = Document.parse(tagsJson);
            Iterator it = doc.keySet().iterator();
            if (it.hasNext()){
                while(it.hasNext()){
                    key = (String)it.next();
                    pstdeleteStmt.setString(3, key);
                }
            }
            return pstdeleteStmt.executeUpdate();
        }
    }
    
    private List<Metadata> parseSelectListObjectwithTags(String bucketName, ResultSet rs) throws SQLException{
        List<Metadata> list = new ArrayList();
        while(rs.next()){
            String objid = rs.getString("objid");
            String versionId = rs.getString("versionid");
            try {
                list.add(this.selectSingleObjectInternal(bucketName, objid, versionId));
            } catch (ResourceNotFoundException ex) {
                // skipe
            }
        }
        return list;
    }
    
    @Override
    public List<Metadata> listObjectWithTags(String bucketName, Object query, int maxKeys) throws SQLException{
	checkAndReconnect();
        try (PreparedStatement pstselectStmt = getObjPreparedStmt(bucketName+ DataRepositoryQuery.tagIndexingTablePrefexi, DataRepositoryQuery.selectTagIndexingQuery + (String)query)){  
            ResultSet rs = pstselectStmt.executeQuery();
            return parseSelectListObjectwithTags(bucketName, rs);
        }
    }

    private void createRestoreObjectTable() throws SQLException{
        pstCreateRestoreObjects.execute();
    }
    
    private int insertRestoreObject(String bucketName, String objKey, String objId, String versionId, String request) throws SQLException {
        
        if (request == null)
            return -1; // ignore

        if (request.isEmpty())
            return -1; // ignore

        pstInsertRestoreObjects.clearParameters();
        pstInsertRestoreObjects.setString(1, bucketName); 
        pstInsertRestoreObjects.setString(2, objKey);
        pstInsertRestoreObjects.setString(3, objId);
        pstInsertRestoreObjects.setString(4, versionId);
        pstInsertRestoreObjects.setString(5, request);
        pstInsertRestoreObjects.execute();
        return 0;    
    }
    
    private int removeRestoreObject(String bucketName, String objId, String versionId) throws SQLException{
        pstDeleteRestoreObjects.clearParameters();
        pstDeleteRestoreObjects.setString(1, objId);
        pstDeleteRestoreObjects.setString(2, versionId);
        return pstDeleteRestoreObjects.executeUpdate();
    }
    
    private String selectRestoreObject(String bucketName, String objId, String versionId) throws SQLException{
        String request = null;
        pstSelectRestoreObjects.setString(1, objId);
        pstSelectRestoreObjects.setString(2, versionId);
        ResultSet rs = pstSelectRestoreObjects.executeQuery();
        if (rs.next()){
            request = rs.getString("request");
        }
   
        return request;
    }
    
    @Override
    public int insertRestoreObjectRequest(String bucketName, String key, String objId, String versionId, String request) throws SQLException {
	checkAndReconnect();
        return insertRestoreObject(bucketName, key, objId, versionId, request);
    }

    @Override
    public String getRestoreObjectRequest(String bucketName, String objId, String versionId)  throws SQLException{
	 checkAndReconnect();
         return selectRestoreObject(bucketName, objId, versionId);
    }

    @Override
    public void deleteRestoreObjectRequest(String bucketName, String objId, String versionId)  throws SQLException{
	 checkAndReconnect();
         removeRestoreObject(bucketName, objId, versionId);
    }
    
    @Override
    public long getEstimatedCount(String bucketName){
        return 0;
    }
}
