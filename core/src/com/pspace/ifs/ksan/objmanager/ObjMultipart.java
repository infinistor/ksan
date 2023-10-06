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

// import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.libs.multipart.Multipart;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.libs.multipart.ResultParts;
import com.pspace.ifs.ksan.libs.multipart.ResultUploads;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author legesse
 */
public class ObjMultipart{
    private String bucket;
    //private String key;
    //private String uploadId;
    private int defaultMaxUpkoads;
    private DataRepository dbm;
    //private ObjManagerConfig config;
    private List<ListResult> list;
    
    class ListMultipartCallBack implements DBCallBack{
        @Override
        public void call(String key, String uploadid, String lastModified, long partNo, 
                    String unused2, String unused3, String unused4, boolean isTrancated) {
            ListResult lr = new ListResult();
            lr.set(bucket, key, uploadid, (int)partNo, lastModified);
            if (isTrancated)
                lr.setTruncated();
            if (list.size() >= defaultMaxUpkoads)
                lr.setTruncated();
            list.add(lr);
        }  
    }
    
    public ObjMultipart(DataRepository dbm) throws UnknownHostException, Exception{
        this.dbm = dbm;
        list = new ArrayList<>();
        defaultMaxUpkoads = 1000;
    }
 
    public void setBucket(String bucket){
        this.bucket=bucket;
    }
    
    private String getNewUploadId(){
        int leftLimit = 48; // number '0'
        int rightLimit = 122; // letter 'z'
        String id;
        
        Random rand = new Random();
        id = rand.ints(leftLimit, rightLimit)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(15)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return id;
    }
    
    public String createMultipartUpload(Metadata mt){
        String id;
        while (true){
            id = getNewUploadId();
            if (id.isEmpty())
                return null;
            
            try {
                dbm.insertMultipartUpload(mt, id, 0);
                return id;
            } catch (SQLException ex) {
                Logger.getLogger(ObjMultipart.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public int startSingleUpload(Metadata mt, String uploadId, int partNo){
        try {
            mt.setEtag("");
            //mt.setMeta("");
            return dbm.insertMultipartUpload(mt, uploadId, partNo);
        } catch (SQLException ex) {
            Logger.getLogger(ObjMultipart.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public int finishSingleUpload(Metadata mt, String uploadId, int partNo){
        try {
            return dbm.updateMultipartUpload(mt, uploadId, partNo, true);
        } catch (SQLException ex) {
            Logger.getLogger(ObjMultipart.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public int abortMultipartUpload(String uploadId){
        try {
            return dbm.deleteMultipartUpload(bucket, uploadId);
        } catch (SQLException ex) {
            Logger.getLogger(ObjMultipart.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public List<Integer> listParts(String key, String uploadId, int maxParts, int partNoMarker) throws SQLException{
        
        if (maxParts > 1000)
            maxParts = 0;
        
        return dbm.selectMultipart(bucket, uploadId, maxParts, partNoMarker);
    }
    
    private Object makeMongoQuery(String delimiter, String prefix, String keyMarker, String uploadIdMarker){
        BasicDBObject listObjQuery = new BasicDBObject();
        if (delimiter.isEmpty())
           listObjQuery.put("key", new BasicDBObject("$regex", prefix));
        else
           listObjQuery.put("key", new BasicDBObject("$regex", prefix + ".*" + delimiter)); // prefix like
        
        if (!keyMarker.isEmpty()){
            listObjQuery.append("$gt", keyMarker);
            if (!uploadIdMarker.isEmpty())
                listObjQuery.append("$gt", uploadIdMarker);
        }
        listObjQuery.append("completed", false);
        listObjQuery.append("partNo", new BasicDBObject("$ne", 0));
        return listObjQuery;
    }
    
    private Object makeMysqlQuery(String delimiter, String prefix, String keyMarker, String uploadIdMarker){
        String sql = "SELECT objKey, uploadid, partNo, changeTime FROM MULTIPARTS WHERE bucket= '"+ bucket + "' AND partNo=0 AND completed=false";
        
        if (!prefix.isEmpty())
           sql = sql + " AND objKey LIKE '"+ prefix + "%'";
        
        if (!delimiter.isEmpty()){
           sql = sql + " AND objKey LIKE '" + prefix + "%" + delimiter + "%'"; 
        }
        
        if (!keyMarker.isEmpty()){
             sql = sql + " AND objKey > '" + keyMarker.replaceAll("/", "\\/") + "'";
             if (!uploadIdMarker.isEmpty())
                 sql = sql + " AND uploadid > " + uploadIdMarker;
        }
        
        sql = sql + " ORDER BY bucket, objKey, uploadid LIMIT " + defaultMaxUpkoads;
        return sql;
    }
    
    public List<ListResult> listUploads(String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException{
        Object sql;
        
        if (maxUploads < 1000)
            defaultMaxUpkoads = maxUploads;
         
        if (dbm instanceof MongoDataRepository)
            sql = makeMongoQuery(delimiter, prefix, keyMarker, uploadIdMarker);
        else 
            sql = makeMysqlQuery(delimiter, prefix, keyMarker, uploadIdMarker);
        
        DBCallBack cb = new ListMultipartCallBack(); 
        //System.out.println("sql : " + sql);
        dbm.selectMultipartUpload(bucket, sql, maxUploads, cb);      
        return this.list;
    }

    public Multipart getMultipart(String uploadId) throws SQLException, ResourceNotFoundException {
        return dbm.getMulipartUpload(uploadId);
    }

    public SortedMap<Integer, Part> getParts(String uploadId) throws SQLException {
        return dbm.getParts(uploadId);
    }

    public ResultParts getParts(String uploadId, String partNumberMarker, int maxParts) throws SQLException {
        int partNumberMarkerDec = 0;
        
        if (!Strings.isNullOrEmpty(partNumberMarker))
            partNumberMarkerDec = Integer.valueOf(partNumberMarker);
        
        return dbm.getParts(uploadId, partNumberMarkerDec, maxParts);
    }

    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException {
        try {
            return dbm.getUploads(bucket, delimiter, prefix, keyMarker, uploadIdMarker, maxUploads);
        } catch (SQLException e) {
            Logger.getLogger(ObjMultipart.class.getName()).log(Level.SEVERE, null, e);
            throw new SQLException(e);
        }
    }

    public boolean isUploadId(String uploadid) throws SQLException {
        return dbm.isUploadId(uploadid);
    }
    
    public Metadata getObjectWithUploadIdPartNo(String uploadId, int partNo) throws SQLException {
        try {
            Bucket bt = dbm.selectBucket(bucket);
            return dbm.getObjectWithUploadIdPart(bt.getDiskPoolId(), uploadId, partNo);
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }
    
    public int putPartRef(String uploadId, int partNo, String ref) throws SQLException, ResourceNotFoundException{
        return dbm.setPartRef(uploadId, partNo, ref);
    }
    
    public String getPartRef(String uploadId, int partNo) throws SQLException, ResourceNotFoundException{
        return dbm.getPartRef(uploadId, partNo);
    }
}
