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

import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
// import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.libs.multipart.Multipart;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.libs.multipart.ResultParts;
import com.pspace.ifs.ksan.libs.multipart.ResultUploads;
import com.pspace.ifs.ksan.libs.multipart.Upload;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;


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
    private static Logger mongoLogger;
    // constant for data elements
    // for object collection
    //private static final String BUCKET="bucket";
    
    // for collection names
    private static final String BUCKETSCOLLECTION="BUCKETS";
    private static final String USERSDISKPOOL="USERSDISKPOOL";
    private static final String MULTIPARTUPLOAD ="MULTIPARTUPLOAD";
    private static final String UTILJOBS = "UTILJOBS";
    
    // for object collection
    private static final String OBJKEY="objKey";
    private static final String OBJID="objId";
    private static final String SIZE="size";
    private static final String ETAG="etag";
    private static final String META="meta";
    private static final String TAG="tag";
    private static final String PDISKID="pdiskid";
    private static final String RDISKID="rdiskid";
    private static final String ACL="acl";
    private static final String LASTMODIFIED="lastModified";
    private static final String VERSIONID="versionid";
    private static final String DELETEMARKER="deleteMarker";
    private static final String LASTVERSION="lastversion";
    
    // for bucket
    private static final String BUCKETNAME="bucketName";
    private static final String BUCKETID="bucketId";
    private static final String DISKPOOLID="diskPoolId";
    private static final String USERID="userId";
    private static final String USERNAME="userName";
    private static final String WEB="web";
    private static final String CORS="cors";
    private static final String LIFECYCLE="lifecycle";
    private static final String ACCESS="access";
    private static final String TAGGING="tagging";
    private static final String REPLICATION="replication";
    private static final String VERSIONING="versioning";
    private static final String MFADELETE="MfaDelete";
    private static final String CREATETIME="createTime";
    private static final String REPLICACOUNT="replicaCount";
    private static final String CREDENTIAL="credential";
    private static final String ENCRYPTION="encryption";
    private static final String OBJECTLOCK="objectlock";
    private static final String POLICY="policy";
    private static final String FILECOUNT="fileCount";
    private static final String USEDSPACE="usedSpace";
    
    // for multipart upload
    private static final String UPLOADID="uploadId";
    private static final String PARTNO ="partNo";
    private static final String COMPLETED = "completed";
    private static final String CHANGETIME = "changeTime";
    
    // for utility
    private static final String ID = "Id";
    private static final String STATUS = "status";
    private static final String TOTALNUMBEROFOBJECTS = "TotalNumObject";
    private static final String NUMJOBDONE = "NumJobDone";
    private static final String CHECKONLY = "checkOnly";
    private static final String UTILNAME = "utilName";
    private static final String STARTTIME = "startTime";
   
    public MongoDataRepository(ObjManagerCache  obmCache, String hosts, String username, String passwd, String dbname, int port) throws UnknownHostException{
        //System.out.format(">>[MongoDataRepository] hosts : %s username : %s dbname : %s\n", hosts, username, dbname);
        parseDBHostNames2URL(hosts, port);
        this.username = username;
        this.passwd = passwd;
        this.dbname = dbname;
        this.obmCache = obmCache;
        connect();
        createBucketsHolder();
        createUserDiskPoolHolder();
    }
    
    private void parseDBHostNames2URL(String hosts, int port){
        if (hosts.contains(",")){
            if (hosts.contains(":"))
                url = "mongodb://" + hosts;
            else{
                String hostList[] = hosts.split(",");
                url = "mongodb://";
                for( String host: hostList)
                    url = url + host + ":" + port + ",";
            }
        } else
            this.url = "mongodb://" + hosts + ":" + port;
    }
    
    private void connect() throws UnknownHostException{
        MongoClient mongo;
        
        mongo = MongoClients.create(url);
        
        MongoCredential credential = MongoCredential.createCredential(username, dbname, passwd.toCharArray()); 
        
        database = mongo.getDatabase(dbname);
    }
    
    private void createBucketsHolder(){
        Document index;
        
        index = new Document(BUCKETNAME, 1);
        buckets = database.getCollection(BUCKETSCOLLECTION);
        if (buckets == null){
            database.createCollection(BUCKETSCOLLECTION);
            buckets = database.getCollection(BUCKETSCOLLECTION);
            buckets.createIndex(index, new IndexOptions().unique(true));
        }
    }
    
    private void createUserDiskPoolHolder(){
        Document index;
        
        index = new Document(USERID, 1);
        index.append(CREDENTIAL, 1);
        MongoCollection<Document> userDiskPool = database.getCollection(USERSDISKPOOL);
        if (userDiskPool == null){
            database.createCollection(USERSDISKPOOL);
            userDiskPool = database.getCollection(USERSDISKPOOL);
            userDiskPool.createIndex(index, new IndexOptions().unique(true));
        }
    }
    
    private MongoCollection<Document> getMultiPartUploadCollection(){
        MongoCollection<Document> multip;
        
        multip = this.database.getCollection(MULTIPARTUPLOAD);
        if (multip == null){
            database.createCollection(MULTIPARTUPLOAD);
            multip = database.getCollection(MULTIPARTUPLOAD);
            multip.createIndex(Indexes.ascending(UPLOADID, PARTNO, OBJKEY, BUCKETNAME), new IndexOptions().unique(true));
        }
        
        return multip;
    }
    
    private String getCurrentDateTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
        Date date = new Date();
        return formatter.format(date);
    }
    
    private Date convertString2Date(String dateStr){
        Date date;
        try {  
            date=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(dateStr);
        } catch (ParseException ex) {
            date = new Date();
        }
        return date;
    }
    
    @Override
    public int insertObject(Metadata md) throws ResourceNotFoundException {
        MongoCollection<Document> objects;
        Document doc;
        objects = this.database.getCollection(md.getBucket());
        doc = new Document(OBJKEY, md.getPath());
        doc.append(BUCKETNAME, md.getBucket());
        doc.append(OBJKEY, md.getPath());
        doc.append(LASTMODIFIED, md.getLastModified());
        doc.append(OBJID, md.getObjId());
        doc.append(ETAG, md.getEtag());
        doc.append(META, md.getMeta());
        doc.append(TAG, md.getTag());
        doc.append(SIZE, md.getSize());
        doc.append(ACL, md.getAcl());
        doc.append(VERSIONID, md.getVersionId());
        doc.append(DELETEMARKER, md.getDeleteMarker());
        doc.append(LASTVERSION, true);
        doc.append(PDISKID, md.getPrimaryDisk().getId());
        if (md.isReplicaExist())
            doc.append(RDISKID, md.getReplicaDisk().getId());
        if (!(md.getVersionId()).isEmpty())
            objects.updateMany(Filters.eq(OBJID, md.getObjId()), Updates.set(LASTVERSION, false));
        objects.insertOne(doc);
        updateBucketObjectCount(md.getBucket(), 1);
        return 0;
    }

    @Override
    public int updateDisks(Metadata md, boolean updatePrimary, DISK newDisk) {
        MongoCollection<Document> objects;
        objects = database.getCollection(md.getBucket());
        System.out.format("objId :%s versionid : %s pdiskid : %s newDiskid : %s \n", md.getObjId(), md.getVersionId(), md.getPrimaryDisk().getId(), newDisk.getId());
       
        UpdateResult res = objects.updateOne(Filters.and(Filters.eq(OBJID, md.getObjId()), eq(VERSIONID, md.getVersionId())), Updates.set(updatePrimary ? PDISKID : RDISKID, newDisk.getId()));
       System.out.println("after update!");
       
       return (int)res.getModifiedCount(); 
    }

    @Override
    public int updateSizeTime(Metadata md) {
        MongoCollection<Document> objects;
        objects = database.getCollection(md.getBucket());
        objects.updateOne(Filters.eq(OBJID, md.getObjId()), Updates.set(LASTMODIFIED, md.getLastModified()));
        objects.updateOne(Filters.eq(OBJID, md.getObjId()), Updates.set(SIZE, md.getSize()));
        return 0;
    }
 
    private Metadata selectSingleObjectInternal(String bucketName, String objId, String versionId) throws ResourceNotFoundException {
        MongoCollection<Document> objects;
        FindIterable fit;
        Metadata mt;
        
        objects = database.getCollection(bucketName);
        if (versionId.isEmpty())
            fit = objects.find(Filters.and(eq(OBJID, objId), eq(LASTVERSION, true)));
        else
            fit = objects.find(Filters.and(eq(OBJID, objId), eq(VERSIONID, versionId)));
        
        Bucket bt  = obmCache.getBucketFromCache(bucketName);
        if (bt == null)
            throw new   ResourceNotFoundException("There is not bucket with a bucket name " + bucketName + " and contain object with objid " + objId);
        
        Document doc =(Document)fit.first();
        if (doc == null)
          throw new   ResourceNotFoundException("There is not object with a bucket name " + bucketName + " and objid " + objId);
        
        long lastModified = doc.getLong(LASTMODIFIED);
        String key       = (String)doc.get(OBJKEY);
        String etag         = doc.getString(ETAG);
        String meta         = doc.getString(META);
        String tag          = doc.getString(TAG);
        long size           = doc.getLong(SIZE);
        String acl          = doc.getString(ACL);
        String versionid    = doc.getString(VERSIONID);
        String deleteMarker = doc.getString(DELETEMARKER);
        boolean lastversion = doc.getBoolean(LASTVERSION);
        String pdiskId      = doc.getString(PDISKID);
        DISK pdsk           = pdiskId != null ? obmCache.getDiskWithId(bt.getDiskPoolId(), pdiskId) : new DISK();
        String rdiskId      = doc.getString(RDISKID);
        DISK rdsk;
        if (rdiskId == null)
            rdsk = new DISK();
        else if (rdiskId.isEmpty())
            rdsk = new DISK();
        else
            rdsk = obmCache.getDiskWithId(bt.getDiskPoolId(), rdiskId);
        
        mt = new Metadata( bucketName, key);
        mt.set(etag, tag, meta, acl, size);
        mt.setPrimaryDisk(pdsk);
        mt.setReplicaDISK(rdsk);
        mt.setLastModified(lastModified);
        //mt.setSize(size);
        mt.setVersionId(versionid, deleteMarker, lastversion);
        return mt;
    }
 
    @Override
    public Metadata selectSingleObject(String diskPoolId, String bucketName, String key) throws ResourceNotFoundException {
        Metadata mt;
        
        mt = new Metadata( bucketName, key);
        //System.out.format("[selectSingleObject ] bucket : %s path : %s objid : %s\n", mt.getBucket(), mt.getPath(), mt.getObjId());
        return selectSingleObjectInternal(bucketName, mt.getObjId(), ""); 
    }
    @Override
    public Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objId) throws ResourceNotFoundException {
        return selectSingleObjectInternal(bucketName, objId, ""); 
    }
    
    @Override
    public Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objId, String versionId) throws ResourceNotFoundException {
        return selectSingleObjectInternal(bucketName, objId, versionId); 
    }
    
    @Override
    public void selectObjects(String bucketName, Object query, int maxKeys, DBCallBack callback) throws SQLException {
        MongoCollection<Document> objects;
        objects = database.getCollection(bucketName);
        FindIterable<Document> oit = objects.find((BasicDBObject)query).limit(maxKeys).sort(new BasicDBObject(OBJKEY, 1 ));
        Iterator it = oit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            String key         = doc.getString(OBJKEY);
            String etag        = doc.getString(ETAG);
            String pdiskid     = doc.getString(PDISKID);
            String rdiskid     = doc.getString(RDISKID);
            long lastModified  = doc.getLong(LASTMODIFIED);
            Date dt = new Date(TimeUnit.MILLISECONDS.convert(lastModified, TimeUnit.NANOSECONDS));
            String lastModifiedStr = dt.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime().toString();
            long size           = doc.getLong(SIZE);
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
            String bucketName = (String)doc.get(BUCKETNAME);
            objects = database.getCollection(bucketName);
            String diskid;
            MongoCursor<String> cdisk = objects.distinct(PDISKID, String.class).iterator();
            while(cdisk.hasNext()){
                diskid = cdisk.next();
                if (!dList.contains(diskid))
                    dList.add(diskid);
            }
            
            cdisk = objects.distinct(RDISKID, String.class).iterator();
            while(cdisk.hasNext()){
                diskid = cdisk.next();
                if (!dList.contains(diskid))
                    dList.add(diskid);
            }

        }
        return dList;
    }
   
    @Override
    public Bucket insertBucket(Bucket bt) throws ResourceAlreadyExistException{
        Document doc;
        Document index;
        try{ 
            doc = new Document(BUCKETNAME, bt.getName());
            //doc.append(BUCKETNAME, bucketName);
            doc.append(DISKPOOLID, bt.getDiskPoolId());
            doc.append(BUCKETID, new Metadata(bt.getName(), "/").getBucketId());
            doc.append(USERID, bt.getUserId());
            doc.append(ACL, bt.getAcl());
            doc.append(REPLICACOUNT, bt.getReplicaCount());
            
            doc.append(USERNAME, bt.getUserName());
            doc.append(WEB, "");
            doc.append(CORS, "");
            doc.append(LIFECYCLE, "");
            doc.append(ACCESS, "");
            doc.append(TAGGING, "");
            doc.append(REPLICATION, "");
            doc.append(VERSIONING, "");
            doc.append(MFADELETE, "");
            
            doc.append(ENCRYPTION, bt.getEncryption());
            doc.append(OBJECTLOCK, bt.getObjectLock());
            doc.append(POLICY, "");
            doc.append(FILECOUNT, 0L);
            doc.append(USEDSPACE, 0L);
            doc.append(CREATETIME, getCurrentDateTime());
            
            buckets.insertOne(doc);
            database.createCollection(bt.getName());
            // for index for object collection
            index = new Document(OBJID, 1);
            index.append(VERSIONID, 1);
            index.append(LASTVERSION, 1);
            index.append(DELETEMARKER, 1);
            //index.append(OBJKEY, 1);
            database.getCollection(bt.getName()).createIndex(index, new IndexOptions().unique(true)); 
            // wild index for listobjects
            Document wildIndex = new Document(OBJID + ".$**", 1);
            database.getCollection(bt.getName()).createIndex(wildIndex); 
            //bt = new Bucket(bucketName, bucketName, diskPoolId);
            getUserDiskPool(bt);
        } catch(SQLException ex){
            throw new ResourceAlreadyExistException(String.format("Bucket(%s) is laready exist in the db!", bt.getName()), ex);
        }
        return bt;
    }

    @Override
    public int deleteBucket(String bucketName) {
        buckets.deleteOne(Filters.eq(BUCKETNAME, bucketName));
        this.database.getCollection(bucketName).drop();
        return 0;
    }

    private long getParseLong(Document doc, String key){
        long value = 0;
        Object tmp = doc.get(key);
        
        if (tmp != null)
            value = Long.valueOf(tmp.toString());
        
        return value;
    }
    
    private Bucket parseBucket(String bucketName, Document doc) throws ResourceNotFoundException, SQLException{
        if (doc == null)
          throw new   ResourceNotFoundException("There is not bucket with a name " + bucketName);
        
        String diskPoolId = doc.getString(DISKPOOLID);
        String bucketId   = doc.getString(BUCKETID);
        String userId     = doc.getString(USERID);
        String userName   = doc.getString(USERNAME);
        String web        = doc.getString(WEB);
        String acl        = doc.getString(ACL);
        String cors       = doc.getString(CORS);
        String lifecycle  = doc.getString(LIFECYCLE);
        String access     = doc.getString(ACCESS);
        String tagging    = doc.getString(TAGGING);
        String replication= doc.getString(REPLICATION);
        int replicaCount  = doc.getInteger(REPLICACOUNT);
        String objectlock = doc.getString(OBJECTLOCK);
        String encryption = doc.getString(ENCRYPTION);
        String policy     = doc.getString(POLICY);
        long usedSpace = getParseLong(doc, USEDSPACE);
        long fileCount = getParseLong(doc, FILECOUNT);
        //long usedSpace = Long.valueOf(doc.get(USEDSPACE).toString());//doc.getLong(USEDSPACE);
        //long fileCount = Long.valueOf(doc.get(FILECOUNT).toString());//doc.getLong(FILECOUNT);
        
        Date createTime;
        try {
            String createTimeStr = doc.getString(CREATETIME);
      
            if (createTimeStr == null)
                createTime = new Date(0);
            else
                createTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(createTimeStr);
        } catch (ParseException ex) {
            createTime = new Date(0);
        }
        String versioning = doc.getString(VERSIONING);
        String mfdelete   = doc.getString(MFADELETE);
        Bucket bt = new Bucket(bucketName, bucketId, diskPoolId);
        
        bt.setUserId(userId);
        bt.setWeb(web);
        bt.setAccess(access);
        bt.setAcl(acl);
        bt.setCors(cors);
        bt.setLifecycle(lifecycle);
        bt.setTagging(tagging);
        bt.setReplication(replication);
        bt.setCreateTime(createTime);
        bt.setVersioning(versioning, mfdelete);
        bt.setReplicaCount(replicaCount);
        bt.setObjectLock(objectlock);
        bt.setEncryption(encryption);
        bt.setUserName(userName);
        bt.setPolicy(policy);
        bt.setFileCount(fileCount);
        bt.setUsedSpace(usedSpace);
        //getUserDiskPool(bt);
        //System.out.println(">>" + bt);
        return bt;
    }
    
    @Override
    public Bucket selectBucket(String bucketName) throws ResourceNotFoundException, SQLException {
        FindIterable fit = buckets.find(eq(BUCKETNAME, bucketName));
        Document doc =(Document)fit.first();
        return parseBucket(bucketName, doc);
    }

    @Override
    public void loadBucketList() {
        Bucket bt;
        FindIterable<Document> fit = buckets.find();
        Iterator it = fit.iterator();
        while((it.hasNext())){
            try {
                Document doc = (Document)it.next();
                String bucketName = (String)doc.get(BUCKETNAME);
                bt =parseBucket(bucketName, doc);
                obmCache.setBucketInCache(bt);
            } catch (ResourceNotFoundException | SQLException ex) {
                Logger.getLogger(MongoDataRepository.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    } 
    
    @Override
    public List<Bucket> getBucketList() {
        List<Bucket> btList = new ArrayList();
        FindIterable<Document> fit = buckets.find();
        Iterator it = fit.iterator();
        while((it.hasNext())){
            try {
                Document doc = (Document)it.next();
                String bucketName = (String)doc.get(BUCKETNAME);
                //String diskPoolId = (String)doc.get("diskPoolId");
                //String bucketId   = (String)doc.get("bucketId");
                Bucket bt = parseBucket(bucketName, doc);//new Bucket(bucketName, bucketId, diskPoolId);
                btList.add(bt);
            } catch (ResourceNotFoundException | SQLException ex) {
                Logger.getLogger(MongoDataRepository.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
        return btList;
    }
    
    @Override
    public int updateBucketVersioning(Bucket bt){
        return updateBucket(bt.getName(), VERSIONING, bt.getVersioning());  
    }
    
    @Override
    public int insertMultipartUpload(String bucket, String objkey, String uploadid, int partNo, String acl, String meta, String etag, long size, String pdiskid) throws SQLException{
        MongoCollection<Document> multip;
        Document doc;
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return -1;
        
        doc = new Document(OBJKEY, objkey);
        doc.append(BUCKETNAME, bucket);
        doc.append(UPLOADID, uploadid);
        doc.append(PARTNO, partNo);
        doc.append(COMPLETED, false);
        doc.append(CHANGETIME, getCurrentDateTime());
        doc.append(ACL, acl);
        doc.append(META, meta);
        doc.append(ETAG, etag);
        doc.append(SIZE, size);
        doc.append(PDISKID, pdiskid);
        
        multip.insertOne(doc);
        return 0;
    }
    
    @Override
    public int updateMultipartUpload(String bucket,  String uploadid, int partNo, boolean iscompleted) throws SQLException{
        MongoCollection<Document> multip;
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return -1;
        
        multip.updateOne(Filters.and(eq(BUCKETNAME, bucket), eq(UPLOADID, uploadid), eq(PARTNO, partNo)), Updates.set(COMPLETED, iscompleted));
        return 0;
    }
    
    @Override
    public int deleteMultipartUpload(String bucket,  String uploadid) throws SQLException{
        MongoCollection<Document> multip;
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return -1;
        
        multip.deleteOne(Filters.eq(UPLOADID, uploadid));
        return 0;
    }
    
    @Override
    public List<Integer> selectMultipart(String bucket, String uploadid, int maxParts, int partNoMarker) throws SQLException{
        List<Integer> list=new ArrayList<>();
        /*MongoCollection<Document> multip;
        String collName;
        
        collName = getMultiPartUploadCollName(bucket);
        multip = this.database.getCollection(collName);
        if (multip == null)
            return null;
        
        FindIterable fit = multip.find(Filters.and(eq(UPLOADID, uploadid), Filters.gt(PARTNO, partNoMarker)))
                .limit(maxParts).sort(new BasicDBObject(PARTNO, 1 ));
     
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            list.add(doc.getInteger(PARTNO));
        }*/
        return list;
    }
    
    @Override
    public void selectMultipartUpload(String bucket, Object query, int maxKeys, DBCallBack callback) throws SQLException {
        /*MongoCollection<Document> multip;
        String collName;
        String key;
        String uploadid;
        long partNo;
        int counter = 0;
        boolean isTruncated = false;
        
        collName = getMultiPartUploadCollName(bucket);
        multip = this.database.getCollection(collName);
        
        BasicDBObject sortList = new BasicDBObject(OBJKEY, 1 );
        sortList.append(UPLOADID, 1);
        sortList.append(PARTNO, 1);
        FindIterable fit = multip.find((BasicDBObject)query).limit(maxKeys + 1)
                .sort(sortList);
     
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            key      = doc.getString(OBJKEY);
            uploadid = doc.getString(UPLOADID);
            partNo   = (long )doc.getInteger(PARTNO);
            ++counter;
            if (counter == maxKeys){
                isTruncated =it.hasNext();
                System.out.println("counter : "+ counter + " max :" + maxKeys + " next :" + isTruncated);
            }
            
            callback.call(key, uploadid, "", partNo, "", "", "", isTruncated);
            if (isTruncated == true)
                break;
        }*/
    }

    /*@Override
    public ObjectListParameter selectObjects(String bucketName, Object query, int maxKeys) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }*/
    
    private int updateVersionDelete(String bucketName, String objId){
        MongoCollection<Document> objects;
        long lastmodfiedT =0;
        String versionId = "null";
        String deletemaker;
        UpdateResult ures;
        
        objects = database.getCollection(bucketName);
        
        FindIterable fit = objects.find(Filters.eq(OBJID, objId));
     
        Iterator it = fit.iterator();
        while ((it.hasNext())){
            Document doc = (Document)it.next();
            deletemaker = doc.getString(DELETEMARKER);
            if (deletemaker != null){
                if (deletemaker.equalsIgnoreCase("mark"))
                    continue;
            }
            if (doc.getLong(LASTMODIFIED) > lastmodfiedT){
                lastmodfiedT = doc.getLong(LASTMODIFIED);
                versionId = doc.getString(VERSIONID);
            }
        }
        
        ures = objects.updateOne(Filters.and(Filters.eq(OBJID, objId), 
                Filters.eq(VERSIONID, versionId)), Updates.set(LASTVERSION, true));
        if (ures == null)
            return -1;
        
        if (ures.getModifiedCount() > 0)
            return 0;
        
        return -1;
    }
    
    @Override
    public int deleteObject(String bucketName, String objKey, String versionId) {
        MongoCollection<Document> objects;
        DeleteResult dres;
        String objId = new Metadata(bucketName, objKey).getObjId();
        objects = database.getCollection(bucketName);
        
        dres = objects.deleteOne(Filters.and(Filters.eq(OBJID, objId), Filters.eq(VERSIONID, versionId)));
        
        if (dres == null)
            return -1;
        
        int nchange = (int)dres.getDeletedCount();
        if (nchange > 0){
            if (!versionId.equalsIgnoreCase("null"))
                updateVersionDelete(bucketName, objId);
            
            updateBucketObjectCount(bucketName, -1);
        }
        return nchange;
    }
    
    @Override
    public int markDeletedObject(String bucketName, String path, String versionId, String markDelete) 
            throws SQLException {
        int ret;
        String objId = new Metadata(bucketName, path).getObjId();
        ret = updateObject(bucketName,  objId, versionId, DELETEMARKER, markDelete);
        if (ret == 0)
            ret = updateObject(bucketName,  objId, versionId, LASTVERSION, false);
        
        if (ret == 0)
            updateVersionDelete(bucketName, objId);
        return ret;
    }
    
    @Override
    public Metadata selectSingleObject(String diskPoolId, String bucketName, String objKey, String versionId)
            throws ResourceNotFoundException {
        Metadata mt;
      
        mt = new Metadata( bucketName, objKey);
        return selectSingleObjectInternal(bucketName, mt.getObjId(), versionId); 
    }

    @Override
    public Multipart getMulipartUpload(String uploadId) throws SQLException {
        MongoCollection<Document> multip;
        Multipart mpart = null;
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return null;
        
        FindIterable fit = multip.find(Filters.eq(UPLOADID, uploadId));
     
        Iterator it = fit.iterator();
        if ((it.hasNext())){
            Document doc = (Document)it.next();
            mpart = new Multipart(doc.getString(BUCKETNAME), doc.getString(OBJKEY), uploadId);
            mpart.setLastModified((Date)doc.getDate(LASTMODIFIED));
            mpart.setAcl(doc.getString(ACL));
            mpart.setMeta(doc.getString(META));
        }
        
        return mpart;
    }

    @Override
    public SortedMap<Integer, Part> getParts(String uploadId) throws SQLException {
        SortedMap<Integer, Part> listPart = new TreeMap<>();
        MongoCollection<Document> multip;
        Multipart mpart = null;
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return null;
        
        FindIterable fit = multip.find(Filters.and(Filters.eq(UPLOADID, uploadId), Filters.ne(PARTNO, 0)));
     
        Iterator it = fit.iterator();
        while ((it.hasNext())){
            Document doc = (Document)it.next();
            Part part = new Part();
            part.setLastModified((Date)doc.getDate(LASTMODIFIED));
            part.setPartETag(doc.getString(ETAG));
            part.setPartSize(doc.getLong(SIZE));
            part.setPartNumber(doc.getInteger(PARTNO));
            part.setDiskID(doc.getString(PDISKID));
            listPart.put(part.getPartNumber(), part);
        }
        
        return listPart;
    }

    //SELECT changeTime, etag, size, partNo FROM MULTIPARTS WHERE uploadid=? AND partNo > ? ORDER BY partNo LIMIT ?")
    @Override
    public ResultParts getParts(String uploadId, int partNumberMarker, int maxParts) throws SQLException {
        MongoCollection<Document> multip;
        ResultParts resultParts = new ResultParts(uploadId, maxParts);
        resultParts.setListPart(new TreeMap<>());
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return resultParts;
        
        BasicDBObject sortList = new BasicDBObject(PARTNO, 1 );
        
        FindIterable fit = multip.find(
                Filters.and(Filters.eq(UPLOADID, uploadId), Filters.gt(PARTNO, partNumberMarker)))
                .limit(maxParts + 1)
                .sort(sortList);
     
        Iterator it = fit.iterator();
        int count = 0;
        resultParts.setTruncated(false);
        while((it.hasNext())){
            Document doc = (Document)it.next();
            count++;
            if (count > maxParts) {
                resultParts.setPartNumberMarker(doc.getString(PARTNO));
                resultParts.setTruncated(true);
                break;
            }
            Part part = new Part();
            part.setLastModified((Date)doc.getDate(LASTMODIFIED));
            part.setPartETag(doc.getString(ETAG));
            part.setPartSize(doc.getLong(SIZE));
            part.setPartNumber(doc.getInteger(PARTNO));
            part.setDiskID(doc.getString(PDISKID));
            resultParts.getListPart().put(part.getPartNumber(), part);
        }
        
        return resultParts;
    }

    // SELECT objKey, changeTime, uploadid, meta FROM MULTIPARTS WHERE bucket=? AND partNo = 0 AND completed=false ORDER BY partNo LIMIT ? 
    @Override
    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException {
        ResultUploads resultUploads = new ResultUploads();
        resultUploads.setList(new ArrayList<>());
        resultUploads.setTruncated(false);
        
        MongoCollection<Document> multip = getMultiPartUploadCollection();
        if (multip == null)
            return resultUploads;
        
        BasicDBObject sortList = new BasicDBObject(PARTNO, 1 );
        
        FindIterable fit = multip.find(
                Filters.and(Filters.eq(BUCKETNAME, bucket), Filters.eq(PARTNO, 0), Filters.eq(COMPLETED, false)))
                .limit(maxUploads + 1)
                .sort(sortList);
        
        int count = 0;
        Iterator it = fit.iterator();
        resultUploads.setTruncated(false);
        while (it.hasNext()) {
            Document doc = (Document)it.next();
            count++;
            if (count > maxUploads) {
                resultUploads.setKeyMarker(doc.getString(OBJKEY));
                resultUploads.setUploadIdMarker(doc.getString(UPLOADID));
                resultUploads.setTruncated(true);
                break;
            }
            Date changeTime = convertString2Date(doc.getString(CHANGETIME));
            Upload upload = new Upload(doc.getString(OBJKEY), changeTime, doc.getString(UPLOADID), doc.getString(META));
            resultUploads.getList().add(upload);
        }
        return resultUploads;
    }

    @Override
    public boolean isUploadId(String uploadid) throws SQLException {
        MongoCollection<Document> multip = getMultiPartUploadCollection();
        if (multip == null){
            return false;
        }
        
        FindIterable fit = multip.find(Filters.eq(UPLOADID, uploadid)); 
        Iterator it = fit.iterator();
        return it.hasNext();
    }
    
    private int updateObject(String bucketName,  String objId, String versionId, String key, String value){
        MongoCollection<Document> objects;
        objects = database.getCollection(bucketName);
        UpdateResult res = objects.updateOne(Filters.and(Filters.eq(OBJID, objId), Filters.eq(VERSIONID, versionId)), Updates.set(key, value));
        return res.getModifiedCount() > 0 ? 0 : -1;
    }
    
    private int updateObject(String bucketName,  String objId, String versionId, String key, boolean value){
        MongoCollection<Document> objects;
        objects = database.getCollection(bucketName);
        UpdateResult res = objects.updateOne(Filters.and(Filters.eq(OBJID, objId), Filters.eq(VERSIONID, versionId)), Updates.set(key, value));
        return res.getModifiedCount() > 0 ? 0 : -1;
    }
    
    @Override
    public void updateObjectMeta(Metadata mt) throws SQLException {
        updateObject(mt.getBucket(),  mt.getObjId(), mt.getVersionId(), META, mt.getMeta());
    }

    private int updateBucket(String bucketName, String key, String value){
        FindIterable fit = buckets.find(eq(BUCKETNAME, bucketName));
        Document doc =(Document)fit.first();
        if (doc == null)
          return -1;
        
        buckets.updateOne(Filters.eq(BUCKETNAME, bucketName), Updates.set(key, value));
        return 0;
    }
    
    private int updateBucketObjectSpaceCount(String bucketName, String key, long value){
        FindIterable fit = buckets.find(eq(BUCKETNAME, bucketName));
        Document doc =(Document)fit.first();
        if (doc == null)
          return -1;
        
        buckets.updateOne(Filters.eq(BUCKETNAME, bucketName), Updates.set(key, value));
        return 0;
    }
    
    @Override
    public void updateBucketAcl(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), ACL, bt.getAcl());
    }

    @Override
    public void updateBucketCors(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), CORS, bt.getCors());
    }

    @Override
    public void updateBucketWeb(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), WEB, bt.getWeb());
    }

    @Override
    public void updateBucketLifecycle(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), LIFECYCLE, bt.getLifecycle());
    }

    @Override
    public void updateBucketAccess(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), ACCESS, bt.getAccess());
    }

    @Override
    public void updateBucketTagging(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), TAGGING, bt.getTagging());
    }

    @Override
    public void updateBucketReplication(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), REPLICATION, bt.getReplication());
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

    private List<Object> insertUtilJob(String Id, String status, long TotalNumObject, boolean checkOnly, String utilName){
        MongoCollection<Document> utilJob;
        Document doc;
        String startTime = getCurrentDateTime();
        
        utilJob = this.database.getCollection(UTILJOBS);
        if (utilJob == null)
            return new ArrayList<>();
        
        doc = new Document(ID, Id);
        doc.append(STATUS, status);
        doc.append(TOTALNUMBEROFOBJECTS, TotalNumObject);
        doc.append(NUMJOBDONE, 0);
        doc.append(CHECKONLY, checkOnly);
        doc.append(UTILNAME, utilName);
        doc.append(STARTTIME, startTime);
        utilJob.insertOne(doc);
        return getUtilJobObject(Id, status, 0, 0, false, " ", startTime);
    }
    private List<Object> selectUtilJob(String Id){
        MongoCollection<Document> utilJob;
        utilJob = this.database.getCollection(UTILJOBS);
        
        FindIterable fit = utilJob.find(eq(ID, Id));
        
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            return getUtilJobObject(Id, doc.getString(STATUS), 
                    doc.getLong(TOTALNUMBEROFOBJECTS), doc.getLong(NUMJOBDONE), 
                    doc.getBoolean(CHECKONLY), doc.getString(UTILNAME), 
                    doc.getString(STARTTIME));
        }
        return null;
    }
    
    private List<Object> updateStatusUtilJob(String Id, String status){
        MongoCollection<Document> utilJob;
        utilJob = this.database.getCollection(UTILJOBS);
        FindIterable fit = utilJob.find(eq(ID, Id));
        Document doc =(Document)fit.first();
        if (doc == null)
          return new ArrayList<>();
        
        utilJob.updateOne(Filters.eq(ID, Id), Updates.set(STATUS, status));
        return getUtilJobObject(Id, status, 0, 0, false, " ", "");
    }
    
    private List<Object> updateNumberJobsUtilJob(String Id, long TotalNumObject, long NumJobDone){
        MongoCollection<Document> utilJob;
        utilJob = this.database.getCollection(UTILJOBS);
        FindIterable fit = utilJob.find(eq(ID, Id));
        Document doc =(Document)fit.first();
        if (doc == null)
          return new ArrayList<>();
        
        utilJob.updateOne(Filters.eq(ID, Id), Updates.set(TOTALNUMBEROFOBJECTS, TotalNumObject));
        utilJob.updateOne(Filters.eq(ID, Id), Updates.set(NUMJOBDONE, NumJobDone));
        return getUtilJobObject(Id, "", TotalNumObject, NumJobDone, false, "", "");
    }
    
    @Override
    public List<Object> utilJobMgt(String operation, List<Object> in) {
                List<Object> ret;
        String status;
        long TotalNumObject;
        long NumJobDone;
        boolean checkOnly;
        String utilName;
        String Id = in.get(0).toString();
           
        if (operation.equalsIgnoreCase("addJob")){
            status = in.get(1).toString();
            TotalNumObject = Long.parseLong(in.get(2).toString());
            checkOnly = Boolean.getBoolean(in.get(4).toString());
            utilName = in.get(5).toString();
            ret = insertUtilJob(Id, status, TotalNumObject, checkOnly, utilName);
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
        
        return ret;
    }

    @Override
    public boolean isBucketDeleted(String bucketName) throws SQLException {
        MongoCollection<Document> bkt;
        
        bkt = database.getCollection(bucketName);
        return bkt.countDocuments() <= 0;
    }
    
    @Override
    public int insertUserDiskPool(String userId, String accessKey, String secretKey, String diskpoolId, int replicaCount) throws SQLException {
        MongoCollection<Document> userDiskPool;
        Document doc;
        
        userDiskPool = this.database.getCollection(USERSDISKPOOL);
        if (userDiskPool == null)
            return -1;
        
        doc = new Document(USERID, userId);
        doc.append(CREDENTIAL, secretKey + "_" + accessKey);
        doc.append(DISKPOOLID, diskpoolId);
        doc.append(REPLICACOUNT, replicaCount);
        userDiskPool.insertOne(doc);
        
        // check inserted or not
        FindIterable fit = userDiskPool.find(Filters.and(eq(USERID, userId), Filters.eq(CREDENTIAL, secretKey + "_" + accessKey)));
        Iterator it = fit.iterator();
        if((it.hasNext()))
            return 0;
        
        return -1;
    }

    @Override
    public Bucket getUserDiskPool(Bucket bt) throws SQLException {
        MongoCollection<Document> userDiskPool;
        String diskPoolId;
        int replicaCount;
        
        //System.out.println("userId : " + bt.getUserId());
        userDiskPool = this.database.getCollection(USERSDISKPOOL);
        
        FindIterable fit = userDiskPool.find(Filters.and(eq(USERID, bt.getUserId()), Filters.eq(CREDENTIAL, "_")));
        
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            diskPoolId      = doc.getString(DISKPOOLID);
            //System.out.println("doc >>" + doc);
            replicaCount   = doc.getInteger(REPLICACOUNT);
            bt.setDiskPoolId(diskPoolId);
            bt.setReplicaCount(replicaCount);
            break;
        }
        return bt;
    }

    @Override
    public int deleteUserDiskPool(String userId, String diskPoolId) throws SQLException {
       MongoCollection<Document> userDiskPool;
        
        userDiskPool = this.database.getCollection(USERSDISKPOOL);
        if (userDiskPool == null)
            return -1;
        
        userDiskPool.deleteOne(Filters.and(eq(USERID, userId), eq(CREDENTIAL, diskPoolId)));
        return 0;
    }

    @Override
    public Object getStatement(String query) throws SQLException {
        throw new SQLException("mongod is not supported mysql like statements!");
        //return null;
    }

    @Override
    public List<Metadata> getObjectList(String bucketName, Object query, int maxKeys, long offset) throws SQLException {
        String diskPoolId = "1";
        MongoCollection<Document> objects;
        objects = database.getCollection(bucketName);
        BasicDBObject sortBy = new BasicDBObject(OBJKEY, 1 );
        BasicDBObject mongoQuery =(BasicDBObject)query;
        
        if (!mongoQuery.containsField(LASTVERSION)){
            sortBy.append(LASTMODIFIED, -1);
            sortBy.append("_id", -1);
        }
        
        FindIterable<Document> oit = objects.find(mongoQuery).limit(maxKeys).sort(sortBy).skip((int)offset);
        Iterator it = oit.iterator();
        List<Metadata> list = new ArrayList();
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        if (bt != null)
            diskPoolId = bt.getDiskPoolId();
        
        while((it.hasNext())){
            Document doc = (Document)it.next();
            String key         = doc.getString(OBJKEY);
            String etag        = doc.getString(ETAG);
            String tag         = doc.getString(TAG);
            String meta        = doc.getString(META);
            String acl         = doc.getString(ACL);
            String versionid   = doc.getString(VERSIONID);
            String deletem     = doc.getString(DELETEMARKER);
            boolean lastversion = doc.getBoolean(LASTVERSION);
            String pdiskid     = doc.getString(PDISKID);
            String rdiskid     = doc.getString(RDISKID);
            long lastModified  = doc.getLong(LASTMODIFIED);
            long size           = doc.getLong(SIZE);
            Metadata mt = new Metadata(bucketName, key);
            mt.setVersionId(versionid, deletem, lastversion);
            mt.set(etag, tag, meta, acl, size);
            mt.setLastModified(lastModified);
                
            try {
                mt.setPrimaryDisk(obmCache.getDiskWithId(diskPoolId, pdiskid));
            } catch (ResourceNotFoundException ex) {
                mt.setPrimaryDisk(new DISK());
            }
            
            try {
                if (rdiskid == null)
                    mt.setReplicaDISK(new DISK());
                else if(rdiskid.isEmpty())
                    mt.setReplicaDISK(new DISK());
                else
                    mt.setReplicaDISK(obmCache.getDiskWithId(diskPoolId, rdiskid));
            } catch (ResourceNotFoundException ex) {
                mt.setReplicaDISK(new DISK());
            }
            list.add(mt);
        }
        return list;
    }

    @Override
    public void updateObjectTagging(Metadata mt) throws SQLException {
        updateObject(mt.getBucket(),  mt.getObjId(), mt.getVersionId(), TAG, mt.getTag());
        updateObject(mt.getBucket(),  mt.getObjId(), mt.getVersionId(), META, mt.getMeta());
    }

    @Override
    public void updateObjectAcl(Metadata mt) throws SQLException {
        updateObject(mt.getBucket(),  mt.getObjId(), mt.getVersionId(), ACL, mt.getAcl());
    }

    @Override
    public void updateBucketEncryption(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), ENCRYPTION, bt.getEncryption());
    }

    @Override
    public void updateBucketObjectLock(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), OBJECTLOCK, bt.getObjectLock());
    }

    @Override
    public void updateBucketPolicy(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), POLICY, bt.getPolicy());
    }

    @Override
    public void updateBucketUsedSpace(Bucket bt, long size) throws SQLException {
        updateBucketObjectSpaceCount(bt.getName(), USEDSPACE, size);
    }

    private void updateBucketObjectCount(String bucketName, long fileCount){
        updateBucketObjectSpaceCount(bucketName, USEDSPACE, fileCount);
    }
    
    @Override
    public Metadata getObjectWithUploadIdPart(String diskPoolId, String uploadId, int partNo) throws SQLException {
        MongoCollection<Document> multip;
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return null;
        
        FindIterable fit = multip.find(Filters.and(Filters.eq(UPLOADID, uploadId), Filters.eq(PARTNO, partNo)));
     
        Iterator it = fit.iterator();
        
        if (!(it.hasNext()))
            return null;
        
        Metadata mt; 
        DISK pdsk;
        
        Document doc = (Document)it.next();
        String bucketName = doc.getString(BUCKETNAME);
        String objkey     = doc.getString(OBJKEY);
        String acl        = doc.getString(ACL);
        String meta       = doc.getString(META);
        String etag       = doc.getString(ETAG);
        long size         = doc.getLong(SIZE);
        String pdiskId    = doc.getString(PDISKID);
       
        try {
            if (pdiskId == null || obmCache == null)
                pdsk = new DISK();
            else 
                pdsk = obmCache.getDiskWithId(diskPoolId, pdiskId);
        } catch (ResourceNotFoundException ex) {
            pdsk = new DISK();
        }
        
        mt = new Metadata(bucketName, objkey);
        mt.set(etag, "", meta, acl, size);
        mt.setPrimaryDisk(pdsk);
        mt.setReplicaDISK(new DISK());
        return mt;
    }
}
