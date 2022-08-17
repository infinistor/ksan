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
        this.url ="jdbc:mysql://"+ host+":3306/"+ dbname +"?useSSL=false&autoReconnect=true";
        try{
            this.createDB(host, dbname);
            this.connect();
           
            // for bucket
            pstCreateBucket = con.prepareStatement(DataRepositoryQuery.createBucketQuery);
            pstInsertBucket = con.prepareStatement(DataRepositoryQuery.insertBucketQuery);
            pstDeleteBucket = con.prepareStatement(DataRepositoryQuery.deleteBucketQuery);
            pstSelectBucket = con.prepareStatement(DataRepositoryQuery.selectBucketQuery);
            pstSelectAllBucket = con.prepareStatement(DataRepositoryQuery.selectAllBucketQuery);
            pstUpdateBucket = con.prepareStatement(DataRepositoryQuery.updateBucketQuery);
            
            pstUpdateBucketAcl = con.prepareStatement(DataRepositoryQuery.updateBucketAclQuery);
            pstUpdateBucketWeb = con.prepareStatement(DataRepositoryQuery.updateBucketWebQuery);
            pstUpdateBucketCors = con.prepareStatement(DataRepositoryQuery.updateBucketCorsQuery);
            pstUpdateBucketLifecycle = con.prepareStatement(DataRepositoryQuery.updateBucketLifecycleQuery);
            pstUpdateBucketAccess = con.prepareStatement(DataRepositoryQuery.updateBucketAccessQuery);
            pstUpdateBucketTagging = con.prepareStatement(DataRepositoryQuery.updateBucketTaggingQuery);
            pstUpdateBucketReplication = con.prepareStatement(DataRepositoryQuery.updateBucketReplicationQuery);
            pstUpdateBucketEncryption = con.prepareStatement(DataRepositoryQuery.updateBucketEncryptionQuery);
            pstUpdateBucketObjectLock = con.prepareStatement(DataRepositoryQuery.updateBucketObjectLockQuery);
            pstUpdateBucketPolicy = con.prepareStatement(DataRepositoryQuery.updateBucketPolicyQuery);
            pstUpdateBucketFilecount = con.prepareStatement(DataRepositoryQuery.updateBucketFilecountQuery);
            pstUpdateBucketUsedSpace = con.prepareStatement(DataRepositoryQuery.updateBucketUsedSpaceQuery);
            
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

           // for utility
            //String Id, String status, long TotalNumObject, boolean checkOnly, String utilName
            pstCreateUJob = con.prepareStatement(DataRepositoryQuery.createUJobQuery);
            pstInsertUJob = con.prepareStatement(DataRepositoryQuery.insertUJobQuery);
            pstUpdateUJob1 = con.prepareStatement(DataRepositoryQuery.updateUJob1Query);
            pstUpdateUJob2 = con.prepareStatement(DataRepositoryQuery.updateUJob2Query);
            pstSelectUJob = con.prepareStatement(DataRepositoryQuery.selectUJobQuery);

            // for user disk pool table
            pstCreateUserDiskPool = con.prepareStatement(DataRepositoryQuery.createUserDiskPoolQuery);
            pstInsertUserDiskPool = con.prepareStatement(DataRepositoryQuery.insertUserDiskPoolQuery);
            
            pstSelectUserDiskPool = con.prepareStatement(DataRepositoryQuery.selectUserDiskPoolQuery);
            
            pstDeleteUserDiskPool = con.prepareStatement(DataRepositoryQuery.deleteUserDiskPoolQuery);
            
        } catch(SQLException ex){
            this.ex_message(ex);
        }
        
        this.createTable();
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
           Class.forName("com.mysql.cj.jdbc.Driver");
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
        this.pstCreateBucket.execute();
        this.pstCreateMultiPart.execute();
        pstCreateUJob.execute();
        pstCreateUserDiskPool.execute();
        return 0;
    }
    
    private PreparedStatement getObjPreparedStmt(String bucketName, String format) throws SQLException{
        String query = String.format(format, "`" + bucketName + "`");
        PreparedStatement pstStmt = con.prepareStatement(query);
        //System.out.println("Query :>" + pstStmt);
        return pstStmt;
    }
    
    private void createObjectTable(String bucketName) throws SQLException{
        PreparedStatement pstStmt = getObjPreparedStmt(bucketName, DataRepositoryQuery.objCreateQuery);
        pstStmt.execute();
    }
    
    private int updateVersion(String bucketName, String key) throws SQLException{ 
        PreparedStatement pstupdateLastVersion = getObjPreparedStmt(bucketName, DataRepositoryQuery.objUpdateLastVersionQuery);
        pstupdateLastVersion.clearParameters();
        pstupdateLastVersion.setString(1, key);
        pstupdateLastVersion.execute();
        return 0;
    }
    

    private int updateMetadata(Metadata md){
        try {
            PreparedStatement pstupdateMetadata = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdateMetadataQuery);
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
            PreparedStatement pstStmt = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objInsertQuery);
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
            
        } catch(SQLException ex){
            if (ex.getErrorCode() == 1062)
                return updateMetadata(md);
            
            this.ex_message(ex);
            return -1;
        }
        return 0;
    }
    
    @Override
    public synchronized int updateDisks(Metadata md, boolean updatePrimary, DISK newDisk) {
        try {
            PreparedStatement pstupdatePDisks;
            PreparedStatement pstupdateRDisks;
            if (updatePrimary){
                pstupdatePDisks = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdatePDisksQuery);
                pstupdatePDisks.clearParameters();
                pstupdatePDisks.setString(1, newDisk.getId());
                pstupdatePDisks.setString(2, md.getObjId());
                pstupdatePDisks.setString(3, md.getVersionId());
                //pstupdatePDisks.setString(4, md.getPrimaryDisk().getId());
                return pstupdatePDisks.executeUpdate();
            }
            else{
                pstupdateRDisks = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdateRDisksQuery);
                pstupdateRDisks.clearParameters();
                pstupdateRDisks.setString(1, newDisk.getId());
                pstupdateRDisks.setString(2, md.getObjId());
                pstupdateRDisks.setString(3, md.getVersionId());
                return pstupdateRDisks.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(MysqlDataRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    @Override
    public synchronized int updateSizeTime(Metadata md) {
        try {
            PreparedStatement pstupdateSizeTime = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdateSizeTimeQuery);
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
    
    @Override
    public void updateObjectEtag(Metadata md, String etag) throws SQLException{
        PreparedStatement pstupdateEtag = getObjPreparedStmt(md.getBucket(), DataRepositoryQuery.objUpdateEtagQuery);
        pstupdateEtag.clearParameters();
        pstupdateEtag.setString(1, etag);
        pstupdateEtag.setString(2, md.getObjId());
        pstupdateEtag.setString(3, md.getVersionId());
    }
    
    private Metadata getSelectObjectResult(String diskPoolId, ResultSet rs) throws SQLException, ResourceNotFoundException{
        Metadata mt;
        DISK pdsk;
        DISK rdsk;
        String rdiskPath;
               
        Bucket bt  = obmCache.getBucketFromCache(rs.getString(1));
        if (bt == null)
            throw new ResourceNotFoundException("[getSelectObjectResult] bucket "+ rs.getString(1) +" not found in the db");
        
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
            mt = new Metadata(rs.getString(1), rs.getString(2));
            mt.setSize(rs.getLong(3));
            mt.setEtag(rs.getString(5));
            mt.setTag(rs.getString(6));
            mt.setMeta(rs.getString(7));
            mt.setAcl(rs.getString(8));
            mt.setPrimaryDisk(pdsk);
            mt.setReplicaDISK(rdsk);
            mt.setVersionId(rs.getString(11), rs.getString(12), rs.getBoolean(13));
            mt.setReplicaCount(bt.getReplicaCount());
            return mt;
        }
        throw new ResourceNotFoundException("[getSelectObjectResult] bucket: "+ rs.getString(1)+" key " + rs.getString(2)+ " not found in the db");
    }
    
    private synchronized Metadata selectSingleObjectInternal(String bucketName, String diskPoolId, String objId) throws ResourceNotFoundException {
        try{
            PreparedStatement pstStmt = getObjPreparedStmt(bucketName, DataRepositoryQuery.objSelectOneQuery);
            pstStmt.clearParameters();
            pstStmt.setString(1, objId);
            ResultSet rs = pstStmt.executeQuery();
            return getSelectObjectResult(diskPoolId, rs);
        } catch(SQLException ex){
            System.out.println(" error : " + ex.getMessage());
            this.ex_message(ex);
            throw new ResourceNotFoundException("path not found in the db : " + ex.getMessage());
        }
    }

    private synchronized Metadata selectSingleObjectInternal(String bucketName, String diskPoolId, String objId, String versionId) throws ResourceNotFoundException {
        try{
            PreparedStatement pstSelectOneWithVersionId = getObjPreparedStmt(bucketName, DataRepositoryQuery.objSelectOneWithVersionIdQuery);
            pstSelectOneWithVersionId.clearParameters();
            pstSelectOneWithVersionId.setString(1, objId);
            pstSelectOneWithVersionId.setString(2, versionId);
            ResultSet rs = pstSelectOneWithVersionId.executeQuery();
            return getSelectObjectResult(diskPoolId, rs);      
        } catch(SQLException ex){
            //System.out.println(" error : " + ex.getMessage());
            this.ex_message(ex);
            throw new ResourceNotFoundException("Object not found in the db : " + ex.getMessage());
        }
    }
    
    @Override
    public synchronized Metadata selectSingleObject(String diskPoolId, String bucketName, String path) throws ResourceNotFoundException {
        Metadata mt = new Metadata(bucketName, path);
       return selectSingleObjectInternal(bucketName, diskPoolId, mt.getObjId()); 
    }
    
    @Override
    public synchronized Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objid) throws ResourceNotFoundException {
       return selectSingleObjectInternal(bucketName, diskPoolId, objid); 
    }
    
    @Override
    public synchronized Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objid, String versionId) throws ResourceNotFoundException {
       return selectSingleObjectInternal(bucketName, diskPoolId, objid, versionId); 
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
        PreparedStatement pstUpdateTagging = getObjPreparedStmt(mt.getBucket(), DataRepositoryQuery.objUpdateTaggingQuery);
        pstUpdateTagging.clearParameters();
        pstUpdateTagging.setString(1, mt.getTag());
        pstUpdateTagging.setString(2, mt.getMeta());
        pstUpdateTagging.setString(3, mt.getObjId());
        pstUpdateTagging.setString(4, mt.getVersionId());
        pstUpdateTagging.executeUpdate();
    }
    
    @Override
    public void updateObjectAcl(Metadata mt) throws SQLException {
        PreparedStatement pstUpdateAcl = getObjPreparedStmt(mt.getBucket(), DataRepositoryQuery.objUpdateAclQuery);
        pstUpdateAcl.clearParameters();
        pstUpdateAcl.setString(1, mt.getAcl());
        pstUpdateAcl.setString(2, mt.getObjId());
        pstUpdateAcl.setString(3, mt.getVersionId());
        pstUpdateAcl.executeUpdate();
    }
    
    @Override
    public void updateObjectMeta(Metadata mt) throws SQLException {
        PreparedStatement pstUpdateObjectMeta = getObjPreparedStmt(mt.getBucket(), DataRepositoryQuery.objUpdateObjectMetaQuery);
        pstUpdateObjectMeta.clearParameters();
        pstUpdateObjectMeta.setString(1, mt.getMeta());
        pstUpdateObjectMeta.setString(2, mt.getObjId());
        pstUpdateObjectMeta.setString(3, mt.getVersionId());
        pstUpdateObjectMeta.executeUpdate();
    }
    
    private int updateVersionDelete(String bucketName, String objId) throws SQLException{ 
        PreparedStatement pstupdateLastVersionDelete = getObjPreparedStmt(bucketName, DataRepositoryQuery.objUpdateLastVersionDeleteQuery);
        pstupdateLastVersionDelete.clearParameters();
        pstupdateLastVersionDelete.setString(1, objId);
        pstupdateLastVersionDelete.execute();
        return 0;
    }

    @Override
    public int deleteObject(String bucketName, String path, String versionId) {
        try{
            String objId = new Metadata(bucketName, path).getObjId();
            PreparedStatement pststmt = getObjPreparedStmt(bucketName, DataRepositoryQuery.objDeleteQuery);
            pststmt.clearParameters();
            pststmt.setString(1, objId);
            pststmt.setString(2, versionId);
            if (pststmt.executeUpdate()> 0)
                updateBucketFileCount(bucketName, -1);
            
            if (versionId != null){
                if (!versionId.isEmpty())
                    updateVersionDelete(bucketName, objId);
            }
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
        PreparedStatement pstUpdateDeleteMarker = getObjPreparedStmt(bucketName, DataRepositoryQuery.objUpdateDeleteMarkerQuery);
        pstUpdateDeleteMarker.clearParameters();
        pstUpdateDeleteMarker.setString(1, markDelete);
        pstUpdateDeleteMarker.setBoolean(2, false);
        pstUpdateDeleteMarker.setString(3, objId);
        pstUpdateDeleteMarker.setString(4, versionId);
        ret= pstUpdateDeleteMarker.executeUpdate();
        if (ret  > 0){
            if (versionId != null){
                if (!versionId.isEmpty())
                    updateVersionDelete(bucketName, objId);
            }
        }
        return ret;
    }
    
    @Override
    public synchronized Bucket insertBucket(Bucket bt) 
            throws ResourceAlreadyExistException{
        try{
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
            this.pstInsertBucket.executeUpdate();
            //getUserDiskPool(bt); // get diskpoolId and replicaCount
        } catch(SQLException ex){
            System.out.println("SQLException:>" + ex);
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
        //getUserDiskPool(bt); // get diskpoolId and replicaCount
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
                //System.out.println("bucketList>>" + bt);
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
        return selectSingleObjectInternal(bucketName, diskPoolId, mt.getObjId(), versionId); 
    }

    /***********************START*******************************************************************/
    // TO BE
    @Override
    public Multipart getMulipartUpload(String uploadid) throws SQLException, ResourceNotFoundException  {
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
            DISK dsk = obmCache.getDiskWithId(pdiskid);
            multipart.setDiskPoolId(dsk.getDiskPoolId());
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
            part.setDiskID(rs.getString(5));
            resultParts.getListPart().put(part.getPartNumber(), part);
        }

        return resultParts;
    }

    // TO BE
    @Override
    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException {
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
    public List<Metadata> getObjectList(String bucketName, Object pstmt, int maxKeys, long offset) throws SQLException{
        List<Metadata> list = new ArrayList();
        ResultSet rs = null;
        DISK pdsk = null;
        DISK rdsk = null;
        String diskPoolId = "1";
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        //obmCache.displayBucketList();
        //obmCache.displayDiskPoolList();
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
                pdsk = obmCache.getDiskWithId( pdiskid);
                if (!rdiskid.isEmpty())
                    rdsk = this.obmCache.getDiskWithId( rdiskid);
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
            mt.setReplicaCount(bt.getReplicaCount());
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
}
