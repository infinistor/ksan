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
import com.mongodb.MongoCredential;
import com.mongodb.client.FindIterable;
import org.bson.Document; 
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.ObjectListParameter;
import com.pspace.ifs.ksan.gw.identity.S3BucketSimpleInfo;
import com.pspace.ifs.ksan.gw.object.multipart.Multipart;
import com.pspace.ifs.ksan.gw.object.multipart.Part;
import com.pspace.ifs.ksan.gw.object.multipart.ResultParts;
import com.pspace.ifs.ksan.gw.object.multipart.ResultUploads;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.Indexes;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;


/**
 *
 * @author legesse
 */
public class MongoDataRepository implements DataRepository{
    private String url;
    private String username;
    private String passwd;
    private String dbname;
    private ObjManagerCache obmCache;
    private MongoDatabase database; 
    private MongoCollection<Document> buckets;
    
    public MongoDataRepository(ObjManagerCache  obmCache, String host, String username, String passwd, String dbname, int port) throws UnknownHostException{
        this.url = "mongodb://" + host + ":" + port;
        this.username = username;
        this.passwd = passwd;
        this.dbname = dbname;
        this.obmCache = obmCache;
        connect();
        createBucketsHolder();
    }
    
    private void connect() throws UnknownHostException{
        MongoClient mongo;
        
        mongo = MongoClients.create(url);
        
        MongoCredential credential = MongoCredential.createCredential(username, dbname, passwd.toCharArray()); 
        
        database = mongo.getDatabase(dbname);
    }
    
    private void createBucketsHolder(){
        Document index;
        
        index = new Document("bucketName", 1);
        buckets = database.getCollection("BUCKETS");
        if (buckets == null){
            database.createCollection("BUCKETS");
            buckets = database.getCollection("BUCKETS");
            buckets.createIndex(index);
        }
    }
    
    private String getMultiPartUploadCollName(String bucketName){
        return "MULTIPARTUPLOAD_" + bucketName;
    }
    
    private int createMultipartUploadCollection(String bucketName){
        //Document index;
        String collName;
        MongoCollection<Document> coll;
        
        collName = getMultiPartUploadCollName(bucketName);
        if (database.getCollection(collName) == null){
            database.createCollection(collName);
            coll = database.getCollection(collName);
            coll.createIndex(Indexes.ascending("uploadId", "partNo", "key"));
            //coll.createIndexes(list);
        }
        return 0;
    }
    
    private String getCurrentDateTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
        Date date = new Date();
        return formatter.format(date);
    }
    
    @Override
    public int insertObject(Metadata md) throws ResourceNotFoundException {
        MongoCollection<Document> objects;
        Document doc;
        objects = this.database.getCollection(md.getBucket());
        doc = new Document("key", md.getPath());
        doc.append("bucket", md.getBucket());
        doc.append("key", md.getPath());
        doc.append("lastModified", md.getLastModified());
        doc.append("objId", md.getObjId());
        doc.append("etag", md.getEtag());
        doc.append("meta", md.getMeta());
        doc.append("tag", md.getTag());
        doc.append("size", md.getSize());
        doc.append("versionid", md.getVersionId());
        doc.append("deleteMarker", md.getDeleteMarker());
        doc.append("lastversion", true);
        doc.append("pdiskId", md.getPrimaryDisk().getId());
        if (md.isReplicaExist())
            doc.append("rdiskId", md.getReplicaDisk().getId());
        if (!(md.getVersionId()).isEmpty())
            objects.updateMany(Filters.eq("objId", md.getObjId()), Updates.set("lastversion", false));
        objects.insertOne(doc);
        return 0;
    }

    @Override
    public int updateDisks(Metadata md) {
       try {
            MongoCollection<Document> objects;
            objects = database.getCollection(md.getBucket());
            objects.updateOne(Filters.eq("objId", md.getObjId()), Updates.set("pdiskPath", md.getPrimaryDisk().getId()));
            objects.updateOne(Filters.eq("objId", md.getObjId()), Updates.set("rdiskPath", md.getReplicaDisk().getId()));
            return 0;
        } catch (ResourceNotFoundException ex) {
            Logger.getLogger(MongoDataRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0; 
    }

    @Override
    public int updateSizeTime(Metadata md) {
        MongoCollection<Document> objects;
        objects = database.getCollection(md.getBucket());
        objects.updateOne(Filters.eq("objId", md.getObjId()), Updates.set("lastModified", md.getLastModified()));
        objects.updateOne(Filters.eq("objId", md.getObjId()), Updates.set("size", md.getSize()));
        return 0;
    }
 
    private Metadata selectSingleObjectInternal(String bucketName, String objId) throws ResourceNotFoundException {
        MongoCollection<Document> objects;
        Metadata mt;
        
        objects = database.getCollection(bucketName);
        FindIterable fit = objects.find(Filters.and(eq("objId", objId), eq("lastversion", true)));
        Document doc =(Document)fit.first();
        if (doc == null)
          throw new   ResourceNotFoundException("There is not object with a bucket name " + bucketName + " and objid " + objId);
        
        Date lastModified = doc.getDate("lastModified");
        String key       = (String)doc.get("key");
        String etag         = doc.getString("etag");
        String meta         = doc.getString("meta");
        String tag          = doc.getString("tag");
        long size           = doc.getLong("size");
        String versionid    = doc.getString("versionid");
        String deleteMarker = doc.getString("deleteMarker");
        boolean lastversion = doc.getBoolean("lastversion");
        Bucket bt           = obmCache.getBucketFromCache(bucketName);
        String pdiskId      = doc.getString("pdiskId");
        DISK pdsk           = obmCache.getDiskWithId(bt.getDiskPoolId(), pdiskId);
        String rdiskId      = doc.getString("rdiskId");
        DISK rdsk           = obmCache.getDiskWithId(bt.getDiskPoolId(), rdiskId);
        
        mt = new Metadata( bucketName, key);
        mt.set(etag, tag, meta);
        mt.setPrimaryDisk(pdsk);
        mt.setReplicaDISK(rdsk);
        mt.setLastModified(lastModified.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        mt.setSize(size);
        mt.setVersionId(versionid, deleteMarker, lastversion);
        return mt;
    }
 
    @Override
    public Metadata selectSingleObject(String bucketName, String key) throws ResourceNotFoundException {
        Metadata mt;
        
        mt = new Metadata( bucketName, key);
        return selectSingleObjectInternal(bucketName, mt.getObjId()); 
    }
    @Override
    public Metadata selectSingleObjectWithObjId(String bucketName, String objId) throws ResourceNotFoundException {
        return selectSingleObjectInternal(bucketName, objId); 
    }
    
    @Override
    public void selectObjects(String bucketName, Object query, int maxKeys, DBCallBack callback) throws SQLException {
        MongoCollection<Document> objects;
        objects = database.getCollection(bucketName);
        FindIterable<Document> oit = objects.find((BasicDBObject)query).limit(maxKeys).sort(new BasicDBObject("key", 1 ));
        Iterator it = oit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            String key         = doc.getString("key");
            String etag        = doc.getString("etag");
            String pdiskid     = doc.getString("pdiskId");
            String rdiskid     = doc.getString("rdiskId");
            Date lastModified  = doc.getDate("lastModified");
            String lastModifiedStr = lastModified.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime().toString();
            long size           = doc.getLong("size");
           callback.call(key, etag, lastModifiedStr, size, null, pdiskid, rdiskid, true);
        }
    }
 
    @Override
    public List<String> getAllUsedDiskId() throws SQLException{
        List<String> dList = new ArrayList();
        MongoCollection<Document> objects;
        
        FindIterable<Document> fit = buckets.find();
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            String bucketName = (String)doc.get("bucketName");
            objects = database.getCollection(bucketName);
            String diskid;
            MongoCursor<String> cdisk = objects.distinct("pdiskId", String.class).iterator();
            while(cdisk.hasNext()){
                diskid = cdisk.next();
                if (!dList.contains(diskid))
                    dList.add(diskid);
            }
            
            cdisk = objects.distinct("rdiskId", String.class).iterator();
            while(cdisk.hasNext()){
                diskid = cdisk.next();
                if (!dList.contains(diskid))
                    dList.add(diskid);
            }

        }
        return dList;
    }
    
    // 2021-11-24 add username
    @Override
    public Bucket insertBucket(String bucketName, String diskPoolId, String userName, String versioning, String MfaDelete, String encryption, String objectlock) throws ResourceAlreadyExistException {
        Document doc;
        Document index;
        
        index = new Document("objId", 1);
        doc = new Document("bucketName", bucketName);
        doc.append("bucketName", bucketName);
        doc.append("diskPoolId", diskPoolId);
        doc.append("bucketId", new Metadata(bucketName, "/").getBucketId());
        doc.append("versioning", versioning);
        buckets.insertOne(doc);
        database.createCollection(bucketName);
        database.getCollection(bucketName).createIndex(index);
        Bucket bt = new Bucket(bucketName, bucketName, diskPoolId);
        return bt;
    }

    @Override
    public int deleteBucket(String bucketName) {
        buckets.deleteOne(Filters.eq("bucketName", bucketName));
        this.database.getCollection(bucketName).drop();
        return 0;
    }

    @Override
    public Bucket selectBucket(String bucketName) throws ResourceNotFoundException, SQLException {
        FindIterable fit = buckets.find(eq("bucketName", bucketName));
        Document doc =(Document)fit.first();
        if (doc == null)
          throw new   ResourceNotFoundException("There is not bucket with a name " + bucketName);
        
        String diskPoolId = doc.getString("diskPoolId");
        String bucketId = doc.getString("bucketId");
        return new Bucket(bucketName, bucketId, diskPoolId);
    }

    @Override
    public void loadBucketList() {
        Bucket bt;
        FindIterable<Document> fit = buckets.find();
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            String bucketName = (String)doc.get("bucketName");
            String diskPoolId = (String)doc.get("diskPoolId");
            String bucketId   = (String)doc.get("bucketId");
            bt = new Bucket(bucketName, bucketId, diskPoolId);
            obmCache.setBucketInCache(bt);
        }
    } 
    
    @Override
    public List<Bucket> getBucketList() {
        List<Bucket> btList = new ArrayList();
        FindIterable<Document> fit = buckets.find();
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            String bucketName = (String)doc.get("bucketName");
            String diskPoolId = (String)doc.get("diskPoolId");
            String bucketId   = (String)doc.get("bucketId");
            Bucket bt = new Bucket(bucketName, bucketId, diskPoolId);
            btList.add(bt);
        } 
        return btList;
    }
    
    @Override
    public int updateBucketVersioning(Bucket bt){
        FindIterable fit = buckets.find(eq("bucketName", bt.getName()));
        Document doc =(Document)fit.first();
        if (doc == null)
          return -1;
        buckets.updateOne(Filters.eq("bucketName", bt.getName()), Updates.set("versioning", bt.getVersioning()));
        return 0;    
    }
    
    @Override
    public int insertMultipartUpload(String bucket, String objkey, String uploadid, int partNo, String acl, String meta, String etag, long size) throws SQLException{
        MongoCollection<Document> multip;
        String collName;
        Document doc;
        
        collName = getMultiPartUploadCollName(bucket);
        multip = this.database.getCollection(collName);
        if (multip == null)
            return -1;
        
        doc = new Document("key", objkey);
        //doc.append("key", objkey);
        doc.append("uploadId", uploadid);
        doc.append("partNo", partNo);
        doc.append("completed", false);
        doc.append("changeTime", getCurrentDateTime());
        doc.append("acl", acl);
        doc.append("meta", meta);
        doc.append("etag", etag);
        doc.append("size", size);
     
        multip.insertOne(doc);
        return 0;
    }
    
    @Override
    public int updateMultipartUpload(String bucket,  String uploadid, int partNo, boolean iscompleted) throws SQLException{
        MongoCollection<Document> multip;
        String collName;
        
        collName = getMultiPartUploadCollName(bucket);
        multip = this.database.getCollection(collName);
        if (multip == null)
            return -1;
        
        multip.updateOne(Filters.and(eq("uploadId", uploadid), eq("partNo", partNo)), Updates.set("completed", iscompleted));
        return 0;
    }
    
    @Override
    public int deleteMultipartUpload(String bucket,  String uploadid) throws SQLException{
        MongoCollection<Document> multip;
        String collName;
        
        collName = getMultiPartUploadCollName(bucket);
        multip = this.database.getCollection(collName);
        if (multip == null)
            return -1;
        
        multip.deleteOne(Filters.eq("uploadId", uploadid));
        return 0;
    }
    
    @Override
    public List<Integer> selectMultipart(String bucket, String uploadid, int maxParts, int partNoMarker) throws SQLException{
        List<Integer> list=new ArrayList<>();
        MongoCollection<Document> multip;
        String collName;
        
        collName = getMultiPartUploadCollName(bucket);
        multip = this.database.getCollection(collName);
        if (multip == null)
            return null;
        
        FindIterable fit = multip.find(Filters.and(eq("uploadId", uploadid), Filters.gt("partNo", partNoMarker)))
                .limit(maxParts).sort(new BasicDBObject("partNo", 1 ));
     
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            list.add(doc.getInteger("partNo"));
        }
        return list;
    }
    
    @Override
    public void selectMultipartUpload(String bucket, Object query, int maxKeys, DBCallBack callback) throws SQLException {
        MongoCollection<Document> multip;
        String collName;
        String key;
        String uploadid;
        long partNo;
        int counter = 0;
        boolean isTruncated = false;
        
        collName = getMultiPartUploadCollName(bucket);
        multip = this.database.getCollection(collName);
        
        BasicDBObject sortList = new BasicDBObject("key", 1 );
        sortList.append("uploadId", 1);
        sortList.append("partNo", 1);
        FindIterable fit = multip.find((BasicDBObject)query).limit(maxKeys + 1)
                .sort(sortList);
     
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            key      = doc.getString("key");
            uploadid = doc.getString("uploadId");
            partNo   = (long )doc.getInteger("partNo");
            ++counter;
            if (counter == maxKeys){
                isTruncated =it.hasNext();
                System.out.println("counter : "+ counter + " max :" + maxKeys + " next :" + isTruncated);
            }
            
            callback.call(key, uploadid, "", partNo, "", "", "", isTruncated);
            if (isTruncated == true)
                break;
        }
    }

    @Override
    public ObjectListParameter selectObjects(String bucketName, Object query, int maxKeys) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int deleteObject(String bucketName, String path, String versionId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Metadata selectSingleObject(String bucketName, String path, String versionId)
            throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Multipart getMulipartUpload(String uploadid) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SortedMap<Integer, Part> getParts(String uploadId) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultParts getParts(String uploadId, String partNumberMarker, int maxParts) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException, GWException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateBucketAcl(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        return;
    }

    @Override
    public void updateBucketCors(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        return;
    }

    @Override
    public void updateBucketWeb(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        return;
    }

    @Override
    public void updateBucketLifecycle(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        return;
    }

    @Override
    public void updateBucketAccess(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        return;
    }

    @Override
    public void updateBucketTagging(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        return;
    }

    @Override
    public void updateBucketReplication(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isUploadId(String uploadid) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ObjectListParameter listObject(String bucketName, String delimiter, String marker, int maxKeys, String prefix) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectListParameter listObjectV2(String bucketName, String delimiter, String startAfter,
            String continueationToken, int maxKeys, String prefix) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectListParameter listObjectVersions(String bucketName, String delimiter, String keyMarker,
            String versionIdMarker, int maxKeys, String prefix) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public List<Object> utilJobMgt(String operation, List<Object> in) {
        return new ArrayList<>();
    }

    @Override
    public boolean isBucketDelete(String bucket) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateBucketEncryption(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateBucketObjectLock(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateBucketPolicy(Bucket bt) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateObjectTagging(Metadata meta) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<S3BucketSimpleInfo> listBuckets(String userName, String userId) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateObjectAcl(Metadata meta) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateObjectMeta(Metadata meta) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateBucketUsed(String bucketName, long size) {
        // TODO Auto-generated method stub
        
    }
}
