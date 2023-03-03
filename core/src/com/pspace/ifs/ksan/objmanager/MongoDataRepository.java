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
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.orderBy;
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
import org.bson.conversions.Bson;


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
    private static final String LIFECYCLESEVENTS ="LIFECYCLESEVENTS";
    private static final String LIFECYCLESFAILEDEVENTS ="LIFECYCLESFAILEDEVENTS";
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
    private static final String LOGGING="logging";
    private static final String OBJECTTAG_INDEXING = "objTagIndexing";
    
    // for multipart upload
    private static final String UPLOADID="uploadId";
    private static final String PARTNO ="partNo";
    private static final String COMPLETED = "completed";
    private static final String CHANGETIME = "changeTime";
    private static final String PARTREF = "partRef";
    
    // for utility
    private static final String ID = "Id";
    private static final String STATUS = "status";
    private static final String TOTALNUMBEROFOBJECTS = "TotalNumObject";
    private static final String NUMJOBDONE = "NumJobDone";
    private static final String CHECKONLY = "checkOnly";
    private static final String UTILNAME = "utilName";
    private static final String STARTTIME = "startTime";
    
    // for lifcycle
    private static final String INDATE = "inDate";
    private static final String LOGMSG = "log";
    private static final String LIFECYCLEID= "idx";
    private static final String ISFAILED = "isFailed";
    
    // for Object Tag indexing
    private static final String OBJTAGINDEX_TABLE = "ObjTagIndex";
    private static final String TAG_KEY = "TagKey";
    private static final String TAG_VALUE = "TagValue";
    
    // for restore objects
    private static final String OBJRESTORE_TABLE = "RESTOREOBJECTS";
    private static final String OBJRESTORE_REQUEST = "request";
   
    public MongoDataRepository(ObjManagerCache  obmCache, String hosts, String username, String passwd, String dbname, int port) throws UnknownHostException{
        this.username = username;
        this.passwd = passwd;
        this.dbname = dbname;
        this.obmCache = obmCache;
        parseDBHostNames2URL(hosts, port);
        connect();
        createBucketsHolder();
        createLifCycleHolder(LIFECYCLESEVENTS);
        createLifCycleHolder(LIFECYCLESFAILEDEVENTS);
        createRestoreObjHolder();
    }
    
    private void parseDBHostNames2URL(String hosts, int port){
        String credential;
        String authSRC;
        
        if (hosts.contains("mongodb://")){
            url = hosts;
            return;
        }
        
        if (!username.isEmpty()){
            credential = String.format("%s:%s@", username, passwd);
            authSRC = String.format("/?authSource=%s", dbname);
        }
        else{
           credential = "";
           authSRC = "";
        }
        
        if (hosts.contains(",")){
            if (hosts.contains(":"))
                url = "mongodb://" + credential + hosts;
            else{
                String hostList[] = hosts.split(",");
                url = "mongodb://" + credential;
                for( String host: hostList)
                    url = url + host + ":" + port + ",";
            }
        } else
            url = "mongodb://" + credential + hosts + ":" + port;
        url = url + authSRC;
        
        //System.out.println("url >>" + url);
    }

    private void connect() throws UnknownHostException{
        MongoClient mongo;
        
        mongo = MongoClients.create(url);
                
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
    
    private void createLifCycleHolder(String collectionName){
        Document index;
        //objid, versionid, uploadid
        index = new Document(OBJID, 1);
        index.append(VERSIONID, 1);
        index.append(UPLOADID, 1);
        MongoCollection<Document> lifeCycle = database.getCollection(collectionName);
        if (lifeCycle == null){
            database.createCollection(collectionName);
            lifeCycle = database.getCollection(collectionName);
            lifeCycle.createIndex(index, new IndexOptions().unique(true));
        }
    }
    
    private void createObjTagHolder(String bucketName){
        Document index;
        String collectionName = bucketName +  "_" + OBJTAGINDEX_TABLE;
        //objid, versionid, uploadid
        index = new Document(OBJID, 1);
        index.append(VERSIONID, 1);
        index.append(TAG_KEY, 1);
        MongoCollection<Document> objTag = database.getCollection(collectionName);
        if (objTag == null){
            database.createCollection(collectionName);
            objTag = database.getCollection(collectionName);
            objTag.createIndex(index, new IndexOptions().unique(true));
        }
    }
    
    private MongoCollection<Document> getObjTagIndexCollection(String bucketName){
        MongoCollection<Document> objTag;
        String collectionName = bucketName +  "_" + OBJTAGINDEX_TABLE;
        
        objTag = this.database.getCollection(collectionName);
        if (objTag == null){
            database.createCollection(collectionName);
            objTag = database.getCollection(collectionName);
            objTag.createIndex(Indexes.ascending(OBJID, VERSIONID, TAG_KEY), new IndexOptions().unique(true));
        }
        
        return objTag;
    }
    
    private MongoCollection<Document> getLifCyclesCollection(String collectionName){
        MongoCollection<Document> lifeCycle;
        
        lifeCycle = this.database.getCollection(collectionName);
        if (lifeCycle == null){
            database.createCollection(collectionName);
            lifeCycle = database.getCollection(collectionName);
            lifeCycle.createIndex(Indexes.ascending(OBJID, VERSIONID, UPLOADID), new IndexOptions().unique(true));
        }
        
        return lifeCycle;
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
    
    private void createRestoreObjHolder(){
        Document index;
    
        index = new Document(OBJID, 1);
        index.append(VERSIONID, 1);
        //index.append(BUCKETNAME, 1);
        MongoCollection<Document> restoreObj = database.getCollection(OBJRESTORE_TABLE);
        if (restoreObj == null){
            database.createCollection(OBJRESTORE_TABLE);
            restoreObj = database.getCollection(OBJRESTORE_TABLE);
            restoreObj.createIndex(index, new IndexOptions().unique(true));
        }
    }
    
    private MongoCollection<Document> getRestoreObjCollection(){
        MongoCollection<Document> restoreObj;
            
        restoreObj = this.database.getCollection(OBJRESTORE_TABLE);
        if (restoreObj == null){
            database.createCollection(OBJRESTORE_TABLE);
            restoreObj = database.getCollection(OBJRESTORE_TABLE);
            restoreObj.createIndex(Indexes.ascending(OBJID, VERSIONID), new IndexOptions().unique(true));
        }       
        return restoreObj;
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
    
        insertObjTag(md.getBucket(), md.getObjId(), md.getVersionId(), md.getTag());    
        return 0;
    }

    @Override
    public int updateDisks(Metadata md, boolean updatePrimary, DISK newDisk) {
        MongoCollection<Document> objects;
        objects = database.getCollection(md.getBucket());
        //System.out.format("objId :%s versionid : %s pdiskid : %s newDiskid : %s \n", md.getObjId(), md.getVersionId(), md.getPrimaryDisk().getId(), newDisk.getId());
       
        UpdateResult res = objects.updateOne(Filters.and(Filters.eq(OBJID, md.getObjId()), eq(VERSIONID, md.getVersionId())), Updates.set(updatePrimary ? PDISKID : RDISKID, newDisk.getId()));
       //System.out.println("after update!");
       
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
        DISK pdsk           = pdiskId != null ? obmCache.getDiskWithId(pdiskId) : new DISK();
        String rdiskId      = doc.getString(RDISKID);
        DISK rdsk;
        if (rdiskId == null)
            rdsk = new DISK();
        else if (rdiskId.isEmpty())
            rdsk = new DISK();
        else
            rdsk = obmCache.getDiskWithId(rdiskId);
        
        int replicaCount = obmCache.getDiskPoolFromCache(pdsk.getDiskPoolId()).getDefaultReplicaCount();
        mt = new Metadata( bucketName, key);
        mt.set(etag, tag, meta, acl, size);
        mt.setPrimaryDisk(pdsk);
        mt.setReplicaDISK(rdsk);
        mt.setLastModified(lastModified);
        mt.setReplicaCount(replicaCount);
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
        doc.append(TAGGING, bt.getTagging());
        doc.append(REPLICATION, "");
        doc.append(VERSIONING, "");
        doc.append(MFADELETE, "");

        doc.append(ENCRYPTION, bt.getEncryption());
        doc.append(OBJECTLOCK, bt.getObjectLock());
        doc.append(POLICY, "");
        doc.append(FILECOUNT, 0L);
        doc.append(USEDSPACE, 0L);
        doc.append(CREATETIME, getCurrentDateTime());
        doc.append(LOGGING, bt.getLogging());
        doc.append(OBJECTTAG_INDEXING, bt.isObjectTagIndexEnabled());

        buckets.insertOne(doc);
        database.createCollection(bt.getName());
        // for index for object collection
        index = new Document(OBJID, 1);
        index.append(VERSIONID, 1);
        index.append(LASTVERSION, 1);
        index.append(DELETEMARKER, 1);
        //index.append(OBJKEY, 1);
        database.getCollection(bt.getName()).createIndex(index, new IndexOptions().unique(true)); 
        
        Document objkeyIndex = new Document(OBJKEY, 1); // for listobject sorting
        database.getCollection(bt.getName()).createIndex(objkeyIndex);
        
        Document lastModfiedIndex = new Document(LASTMODIFIED, -1); // for listobjectversion sorting
        database.getCollection(bt.getName()).createIndex(lastModfiedIndex);
        
        // wild index for listobjects
        Document wildIndex = new Document(OBJKEY + ".$**", 1);
        database.getCollection(bt.getName()).createIndex(wildIndex); 
         
        createObjTagHolder(bt.getName());
        return bt;
    }

    @Override
    public int deleteBucket(String bucketName) {
        buckets.deleteOne(Filters.eq(BUCKETNAME, bucketName));
        this.database.getCollection(bucketName).drop();
        getObjTagIndexCollection(bucketName).drop();
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
        String logging  = doc.getString(LOGGING);
        boolean objTagIndexing = false;
        if (doc.containsKey(OBJECTTAG_INDEXING))
            objTagIndexing = doc.getBoolean(OBJECTTAG_INDEXING);
        
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
        bt.setLogging(logging);
        bt.setObjectTagIndexEnabled(objTagIndexing);
        return bt;
    }
    
    @Override
    public Bucket selectBucket(String bucketName) throws ResourceNotFoundException, SQLException {
        //FindIterable fit = buckets.find(eq(BUCKETNAME, bucketName));
        FindIterable fit = buckets.find(eq(BUCKETID, new Metadata(bucketName, "/").getBucketId()));
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
    public int insertMultipartUpload(Metadata mt, String uploadid, int partNo) throws SQLException{
        MongoCollection<Document> multip;
        Document doc;
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return -1;
        
        doc = new Document(OBJKEY, mt.getPath());
        doc.append(BUCKETNAME, mt.getBucket());
        doc.append(UPLOADID, uploadid);
        doc.append(PARTNO, partNo);
        doc.append(COMPLETED, false);
        doc.append(CHANGETIME, getCurrentDateTime());
        doc.append(ACL, mt.getAcl());
        doc.append(META, mt.getMeta());
        doc.append(ETAG, mt.getEtag());
        doc.append(SIZE, mt.getSize());
        doc.append(PDISKID, mt.getPrimaryDisk().getId());
        try {
            doc.append(RDISKID, mt.getReplicaDisk().getId());
        } catch (ResourceNotFoundException ex) {
            doc.append(RDISKID, "");
        }
        doc.append(PARTREF, "");
        
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
        return list;
    }
    
    @Override
    public void selectMultipartUpload(String bucket, Object query, int maxKeys, DBCallBack callback) throws SQLException {
 
    }
    
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
           
            removeObjTag(bucketName, objId, versionId, "");
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
    public Multipart getMulipartUpload(String uploadId) throws SQLException, ResourceNotFoundException {
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
            DISK dsk = obmCache.getDiskWithId(doc.getString(PDISKID));
            mpart.setDiskPoolId(dsk.getDiskPoolId());
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
            part.setPrimaryDiskId(doc.getString(PDISKID));
            part.setReplicaDiskId(doc.getString(RDISKID));
            listPart.put(part.getPartNumber(), part);
        }
        
        return listPart;
    }

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
                resultParts.setPartNumberMarker((doc.getInteger(PARTNO)).toString());
                resultParts.setTruncated(true);
                break;
            }
            Part part = new Part();
            part.setLastModified((Date)doc.getDate(LASTMODIFIED));
            part.setPartETag(doc.getString(ETAG));
            part.setPartSize(doc.getLong(SIZE));
            part.setPartNumber(doc.getInteger(PARTNO));
            part.setPrimaryDiskId(doc.getString(PDISKID));
            part.setReplicaDiskId(doc.getString(RDISKID));
            resultParts.getListPart().put(part.getPartNumber(), part);
        }
        
        return resultParts;
    }

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
    
    @Override
    public String getPartRef(String uploadId, int partNo) throws SQLException, ResourceNotFoundException {
        MongoCollection<Document> multip;
        String partRef = null;
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return null;
        
        FindIterable fit = multip.find(Filters.and(Filters.eq(PARTNO, partNo), Filters.eq(UPLOADID, uploadId)));
     
        Iterator it = fit.iterator();
        if ((it.hasNext())){
            Document doc = (Document)it.next();
            partRef=doc.getString(PARTREF);
        }
        
        return partRef;
    }
    
    @Override
    public int setPartRef(String uploadId, int partNo, String partRef) throws SQLException, ResourceNotFoundException {
        MongoCollection<Document> multip;
        
        multip = getMultiPartUploadCollection();
        if (multip == null)
            return -1;
        
        multip.updateOne(Filters.and(eq(UPLOADID, uploadId), eq(PARTNO, partNo)), Updates.set(PARTREF, partRef));
        
        return 0;
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
    
    @Override
    public void updateObjectEtag(Metadata mt, String etag) throws SQLException{
        updateObject(mt.getBucket(),  mt.getObjId(), mt.getVersionId(), ETAG, etag);
    }
    
    private int updateBucket(String bucketName, String key, String value){
        FindIterable fit = buckets.find(eq(BUCKETNAME, bucketName));
        Document doc =(Document)fit.first();
        if (doc == null)
          return -1;
        
        buckets.updateOne(Filters.eq(BUCKETNAME, bucketName), Updates.set(key, value));
        return 0;
    }
    
    private int updateBucket(String bucketName, String key, boolean value){
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

    @Override
    public void updateBucketLogging(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), LOGGING, bt.getLogging());
    }
    
    @Override
    public void updateBucketObjTagIndexing(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), OBJECTTAG_INDEXING, bt.isObjectTagIndexEnabled());
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
    public Object getStatement(String query) throws SQLException {
        throw new SQLException("mongod is not supported mysql like statements!");
        //return null;
    }

    @Override
    public List<Metadata> getObjectList(String bucketName, Object query, int maxKeys, long offset) throws SQLException {
        int def_replicaCount = 0;
        int replicaCount;
        MongoCollection<Document> objects;
        objects = database.getCollection(bucketName);
        List<Bson> sortList= new ArrayList();
       // List<BasicDBObject> sortBy = new ArrayList();
        BasicDBObject sortBy;
        //BasicDBObject sortBy1 = null;
        BasicDBObject mongoQuery =(BasicDBObject)query;
        String queryString = mongoQuery.toJson();
        Bson orderBySort;
        
        //Bson orderBySort = orderBy(descending(OBJKEY), ascending(LASTMODIFIED));
        if (queryString.contains(OBJID)){ // for utlity list 
            sortList.add(new BasicDBObject(OBJID, 1));
            //sortList.add(ascending(OBJID));
            //sortBy = new BasicDBObject(OBJID, 1); 
            //sortBy.add(new BasicDBObject(OBJID, 1 ));
        } else{
            sortList.add(new BasicDBObject(OBJKEY, 1 ));
            //sortList.add(ascending(OBJKEY));
            //sortBy = new BasicDBObject(OBJKEY, 1 );
            //sortBy.add(new BasicDBObject(OBJKEY, 1 ));
        }
            
        if (!queryString.contains(LASTVERSION)){
            sortList.add(new BasicDBObject(LASTMODIFIED, -1 ));
            //sortList.add(descending(LASTMODIFIED));
            //sortBy.append(LASTMODIFIED, -1);
            //sortBy.add(new BasicDBObject(LASTMODIFIED, -1));
            //sortBy.add(new BasicDBObject("_id", -1));
        }
        
        if (sortList.size() > 1)
            orderBySort = orderBy(sortList);
        else if (sortList.size() == 1)
            orderBySort = sortList.get(0);
        else
            orderBySort = ascending(OBJID);
        
        FindIterable<Document> oit = objects.find(mongoQuery).limit(maxKeys).sort(orderBySort).skip((int)offset);
        Iterator it = oit.iterator();
        List<Metadata> list = new ArrayList();
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
                mt.setPrimaryDisk(obmCache.getDiskWithId(pdiskid));
                def_replicaCount = 1;
            } catch (ResourceNotFoundException ex) {
                mt.setPrimaryDisk(new DISK());
            }
            
            try {
                if (rdiskid == null)
                    mt.setReplicaDISK(new DISK());
                else if(rdiskid.isEmpty())
                    mt.setReplicaDISK(new DISK());
                else{
                    mt.setReplicaDISK(obmCache.getDiskWithId(rdiskid));
                    def_replicaCount = 2;
                }
            } catch (ResourceNotFoundException ex) {
                mt.setReplicaDISK(new DISK());
            }
            
            try {
                replicaCount = obmCache.getDiskPoolFromCache(mt.getPrimaryDisk().getDiskPoolId()).getDefaultReplicaCount();
            } catch (ResourceNotFoundException ex) {
               replicaCount = def_replicaCount;
            }
            
            mt.setReplicaCount(replicaCount);
            list.add(mt);
        }
        return list;
    }
    
    @Override
    public long getObjectListCount(String bucketName, Object query) throws SQLException {
        MongoCollection<Document> objects;
        objects = database.getCollection(bucketName);
        BasicDBObject mongoQuery =(BasicDBObject)query;
        
        return objects.countDocuments(mongoQuery);
    }
    
    @Override
    public void updateObjectTagging(Metadata mt) throws SQLException {
        updateObject(mt.getBucket(),  mt.getObjId(), mt.getVersionId(), TAG, mt.getTag());
        updateObject(mt.getBucket(),  mt.getObjId(), mt.getVersionId(), META, mt.getMeta());
        insertObjTag(mt.getBucket(),  mt.getObjId(), mt.getVersionId(), mt.getTag());
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
                pdsk = obmCache.getDiskWithId(pdiskId);
        } catch (ResourceNotFoundException ex) {
            pdsk = new DISK();
        }
        
        mt = new Metadata(bucketName, objkey);
        mt.set(etag, "", meta, acl, size);
        mt.setPrimaryDisk(pdsk);
        mt.setReplicaDISK(new DISK());
        return mt;
    }

    private void insertLifeCycle(String collectionName, LifeCycle lc) throws SQLException {
        MongoCollection<Document> lifecycle = getLifCyclesCollection(collectionName);
        if (lifecycle == null)
            throw new SQLException("[insertLifeCycle] mongo db holder for lifcycle not found!");
        
        Document doc = new Document(OBJID, lc.getObjId());
        doc.append(BUCKETNAME, lc.getBucketName());
        doc.append(OBJKEY, lc.getKey());
        doc.append(VERSIONID, lc.getVersionId());
        doc.append(UPLOADID, lc.getUploadId());
        doc.append(INDATE, lc.getInDate());
        doc.append(LOGMSG, lc.getLog());
        doc.append(LIFECYCLEID, lc.getIndex());
        //doc.append(ISFAILED, lc.isFailed());
        lifecycle.insertOne(doc);
    }

    private List<LifeCycle> parseSelectLifeCycle(Iterator it, boolean isFailed){
        List<LifeCycle> list = new ArrayList();
        
        if (!(it.hasNext()))
            return list;
        
        while(it.hasNext()){
            Document doc = (Document)it.next();
            String bucketName = doc.getString(BUCKETNAME);
            String objKey     = doc.getString(OBJKEY);
            String versionId  = doc.getString(VERSIONID);
            String uploadId   = doc.getString(UPLOADID);
            String log        = doc.getString(LOGMSG);
            Date inDate       = doc.getDate(INDATE);
            long idx          = doc.getLong(LIFECYCLEID);
            //boolean isFailed  = doc.getBoolean(ISFAILED);
            LifeCycle slf = new LifeCycle(idx, bucketName, objKey, versionId, uploadId, log);
            slf.setInDate(inDate);
            slf.setFailedEvent(isFailed);
            list.add(slf);
        }
        return list;
    }
    
    private LifeCycle selectLifeCycle(String collectionName, LifeCycle lc) throws SQLException {
        MongoCollection<Document> lifecycle = getLifCyclesCollection(collectionName);
        FindIterable fit = lifecycle.find(Filters.and(Filters.eq(OBJID, lc.getObjId()), Filters.eq(VERSIONID, lc.getVersionId())));
     
        Iterator it = fit.iterator();
        if (!it.hasNext())
            return null;
        
        return parseSelectLifeCycle(it, collectionName.equals(LIFECYCLESFAILEDEVENTS)).get(0);
    }

    private LifeCycle selectByUploadIdLifeCycle(String collectionName, String uploadId) throws SQLException {
        MongoCollection<Document> lifecycle = getLifCyclesCollection(collectionName);
        FindIterable fit = lifecycle.find(Filters.eq(UPLOADID, uploadId));
     
        Iterator it = fit.iterator();
        if (!it.hasNext())
            return null;
        
        return parseSelectLifeCycle(it, collectionName.equals(LIFECYCLESFAILEDEVENTS)).get(0);
    }

    private List<LifeCycle> selectAllLifeCycle(String collectionName) throws SQLException {
        MongoCollection<Document> lifecycle = getLifCyclesCollection(collectionName);
        FindIterable fit = lifecycle.find();
   
        Iterator it = fit.iterator();
        return parseSelectLifeCycle(it, collectionName.equals(LIFECYCLESFAILEDEVENTS));
    }

    private int deleteLifeCycle(String collectionName, LifeCycle lc) throws SQLException {
        DeleteResult dres;
        MongoCollection<Document> lifecycle = getLifCyclesCollection(collectionName);
        
        dres = lifecycle.deleteOne(Filters.and(Filters.eq(OBJID, lc.getObjId()), Filters.eq(VERSIONID, lc.getVersionId())));
        
        if (dres == null)
            return -1;
        
        return (int)dres.getDeletedCount();
    }
    
    @Override
    public void insertLifeCycle(LifeCycle lc) throws SQLException {
        insertLifeCycle(LIFECYCLESEVENTS, lc);
    }
    
    @Override
    public void insertFailedLifeCycle(LifeCycle lc) throws SQLException {
        insertLifeCycle(LIFECYCLESFAILEDEVENTS, lc);
    }
    
    @Override
    public LifeCycle selectLifeCycle(LifeCycle lc) throws SQLException {
        return selectLifeCycle(LIFECYCLESEVENTS, lc);
    }
    
    @Override
    public LifeCycle selectFailedLifeCycle(LifeCycle lc) throws SQLException {
        return selectLifeCycle(LIFECYCLESFAILEDEVENTS, lc);
    }
    
    @Override
    public LifeCycle selectByUploadIdLifeCycle(String uploadId) throws SQLException {
        return selectByUploadIdLifeCycle(LIFECYCLESEVENTS, uploadId);
    }
    
    @Override
    public LifeCycle selectByUploadIdFailedLifeCycle(String uploadId) throws SQLException {
        return selectByUploadIdLifeCycle(LIFECYCLESFAILEDEVENTS, uploadId);
    }
    
    @Override
    public List<LifeCycle> selectAllLifeCycle() throws SQLException {
        return selectAllLifeCycle(LIFECYCLESEVENTS);
    }
    
    @Override
    public List<LifeCycle> selectAllFailedLifeCycle() throws SQLException {
        return selectAllLifeCycle(LIFECYCLESFAILEDEVENTS);
    }
    
    @Override
    public int deleteLifeCycle(LifeCycle lc) throws SQLException {
        return deleteLifeCycle(LIFECYCLESEVENTS, lc);
    }
    
    @Override
    public int deleteFailedLifeCycle(LifeCycle lc) throws SQLException {
        return deleteLifeCycle(LIFECYCLESFAILEDEVENTS, lc);
    }
    
    private int insertObjTag(String bucketName, String objId, String versionId, String tags) {
        Bucket bt;
        MongoCollection<Document> objTags = getObjTagIndexCollection(bucketName);
        
        if (tags == null)
            return 0;
        
        if (tags.isEmpty())
            return 0;
        
        try {
            bt = selectBucket(bucketName);
            if (!bt.isObjectTagIndexEnabled())
                return 0;
        } catch (ResourceNotFoundException | SQLException ex) {
            return 0;
        } 
        
        if (objTags == null){
            //throw new SQLException("[insertObjTag] mongo db holder for object Tags indexing not found!");
            return 0;
        }
        
        String tagsJson = convertXML2Json(tags);
        if (tagsJson == null)
            return 0; // ignore
        
        if (tagsJson.isEmpty())
            return 0; // ignore
        
        Document doc = new Document(OBJID, objId);        
        doc.append(VERSIONID, versionId);
        
        
        Document tagDoc = Document.parse(tagsJson);
        String key, value;
        Iterator it = tagDoc.keySet().iterator();
        while(it.hasNext()){
            key = (String)it.next();
            value = tagDoc.getString(key);
            if (doc.containsKey(TAG_KEY))
                doc.replace(TAG_KEY, key);
            else
                doc.append(TAG_KEY, key);
            if (doc.containsKey(TAG_VALUE))
                doc.replace(TAG_VALUE, value);
            else
                doc.append(TAG_VALUE, value);

            objTags.insertOne(doc);
        }
        return 0;
    }
    
    private int removeObjTag(String bucketName, String objId, String versionId, String tags){
        MongoCollection<Document> objTags = getObjTagIndexCollection(bucketName);
        int ret = 0;
        
        if (objTags == null){
            return 0;
            //throw new SQLException("[removeObjTag] mongo db holder for object Tags indexing not found!");
        }
        
        String tagsJson = convertXML2Json(tags);
        if (tagsJson == null){
            objTags.findOneAndDelete(Filters.and(eq(OBJID, objId), eq(VERSIONID, versionId)));
            ret++;
            return ret;
        }
        
        if (tagsJson.isEmpty()){
            objTags.findOneAndDelete(Filters.and(eq(OBJID, objId), eq(VERSIONID, versionId)));
            ret++;
            return ret;
        }
             
        Document doc = Document.parse(tagsJson);
        String key;
        Iterator it = doc.keySet().iterator();
        if (it.hasNext()){
            while(it.hasNext()){
                key = (String)it.next();
                objTags.findOneAndDelete(Filters.and(eq(OBJID, objId), eq(VERSIONID, versionId), eq(TAG_KEY, key)));
                ret++;
            }
        }
        else{
            objTags.findOneAndDelete(Filters.and(eq(OBJID, objId), eq(VERSIONID, versionId)));
            ret++;
        }
        return ret;
    }
  
    @Override 
    public List<Metadata> listObjectWithTags(String bucketName, Object query, int maxObjects) throws SQLException{
        MongoCollection<Document> objectsTags;
        objectsTags = getObjTagIndexCollection(bucketName);
        BasicDBObject mongoQuery =(BasicDBObject)query;
       
        FindIterable<Document> oit = objectsTags.find(mongoQuery).limit(maxObjects);
        Iterator it = oit.iterator();
        List<Metadata> list = new ArrayList();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            String objId         = doc.getString(OBJID);
            String versionid   = doc.getString(VERSIONID);
            Metadata mt;
            try {
                mt = selectSingleObjectInternal(bucketName, objId, versionid);
                list.add(mt);
            } catch (ResourceNotFoundException ex) {
                //Logger.getLogger(MongoDataRepository.class.getName()).log(Level.SEVERE, null, ex);
            }  
        }
        return list;
    }
    
    @Override
    public int insertRestoreObjectRequest(String bucketName, String key, String objId, String versionId, String request) throws SQLException{
        MongoCollection<Document> objRestore = getRestoreObjCollection();
        
        if (request == null)
            return -1;
        
        if (request.isEmpty())
            return -1;
         
        if (objRestore == null){
            //throw new SQLException("[insertRestoreObjectRequest] mongo db holder for restore objects  not found!");
            return -1;
        }
        
        Document doc = new Document(BUCKETNAME, bucketName);        
        doc.append(OBJID, objId);
        doc.append(OBJKEY, key);
        doc.append(VERSIONID, versionId);
        doc.append(OBJRESTORE_REQUEST, request);
        objRestore.insertOne(doc);
    
        return 0;
    }
    
    @Override
    public String getRestoreObjectRequest(String bucketName, String objId, String versionId) throws SQLException{
        MongoCollection<Document> objRestore;
        String request=null;
        
        objRestore = getRestoreObjCollection();
        FindIterable<Document> oit = objRestore.find(Filters.and(eq(OBJID, objId), eq(VERSIONID, versionId)));
        Iterator it = oit.iterator();
        if((it.hasNext())){
            Document doc = (Document)it.next();
            request      = doc.getString(OBJRESTORE_REQUEST);
        }
        return request; 
    }
    
    @Override
    public void deleteRestoreObjectRequest(String bucketName, String objId, String versionId) throws SQLException{
        MongoCollection<Document> objRestore;
               
        objRestore = getRestoreObjCollection();
        objRestore.findOneAndDelete(Filters.and(eq(OBJID, objId), eq(VERSIONID, versionId)));
    }
}
