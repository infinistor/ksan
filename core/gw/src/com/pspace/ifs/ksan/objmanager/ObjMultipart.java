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

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.object.multipart.Multipart;
import com.pspace.ifs.ksan.gw.object.multipart.Part;
import com.pspace.ifs.ksan.gw.object.multipart.ResultParts;
import com.pspace.ifs.ksan.gw.object.multipart.ResultUploads;
import com.pspace.ifs.ksan.gw.object.multipart.Upload;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.UUID;
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
    private ObjManagerConfig config;
    private List<ListResult> list;
    
    class ListMultipartCallBack implements DBCallBack{
        @Override
        public void call(String key, String uploadid, String unused1, long partNo, 
                    String unused2, String unused3, String unused4, boolean isTrancated) {
            ListResult lr = new ListResult();
            lr.set(bucket, key, uploadid, (int)partNo);
            if (isTrancated)
                lr.setTruncated();
            if (list.size() >= defaultMaxUpkoads)
                lr.setTruncated();
            list.add(lr);
        }  
    }
    
    public ObjMultipart(String bucket) throws UnknownHostException, Exception{
        config = new ObjManagerConfig();
        if (config.dbRepository.equalsIgnoreCase("MYSQL"))
             dbm = new MysqlDataRepository(null, config.dbHost, config.dbUsername, config.dbPassword, config.dbName);
        else if(config.dbRepository.equalsIgnoreCase("MONGO"))
             dbm = new MongoDataRepository(null, config.dbHost, config.dbUsername, config.dbPassword, config.dbName, 27017);
        else 
            System.out.println("ObjManger initalization error :  there is no db storage configured!");
        list = new ArrayList<>();
        defaultMaxUpkoads = 1000;
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
    
    public String createMultipartUpload(String bucket, String objkey, String acl, String meta){
        String id;
        while (true){
            id = getNewUploadId();
            if (id.isEmpty())
                return null;
            
            try {
                dbm.insertMultipartUpload(bucket, objkey, id, 0, acl, meta, "", 0L);
                return id;
            } catch (SQLException ex) {
                Logger.getLogger(ObjMultipart.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public int startSingleUpload(String objkey, String uploadId, int partNo, String acl, String meta, String etag, long size){
        try {
            return dbm.insertMultipartUpload(bucket, objkey, uploadId, partNo, acl, meta, etag, size);
        } catch (SQLException ex) {
            Logger.getLogger(ObjMultipart.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public int finishSingleUpload(String uploadId, int partNo){
        try {
            return dbm.updateMultipartUpload(bucket, uploadId, partNo, true);
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
           listObjQuery.put("key", new BasicDBObject("$regex", prefix).append("$options", "i"));
        else
           listObjQuery.put("key", new BasicDBObject("$regex", prefix + ".*" + delimiter).append("$options", "i")); // prefix like
        
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
        String sql = "SELECT objKey, uploadid, partNo FROM MULTIPARTS WHERE bucket= '"+ bucket + "' AND partNo=0 AND completed=false";
        
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

    public Multipart getMultipart(String uploadId) throws SQLException {
        return dbm.getMulipartUpload(uploadId);
    }

    /*public int UploadPart(String objkey, String uploadId, int partNo, String acl, String meta, String etag, long size){
        try {
            return dbm.insertMultipartUpload(bucket, objkey, uploadId, partNo, acl, meta, etag, size);
        } catch (SQLException ex) {
            Logger.getLogger(ObjMultipart.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }*/

    public SortedMap<Integer, Part> getParts(String uploadId) throws SQLException {
        return dbm.getParts(uploadId);
    }

    public ResultParts getParts(String uploadId, String partNumberMarker, int maxParts) throws SQLException {
        return dbm.getParts(uploadId, partNumberMarker, maxParts);
    }

    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException, GWException {
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
}
