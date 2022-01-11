package com.pspace.ifs.KSAN.ObjManger;

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
import com.pspace.ifs.KSAN.ObjManger.ObjManagerException.ResourceAlreadyExistException;
import com.pspace.ifs.KSAN.ObjManger.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.KSAN.s3gw.exception.S3Exception;
import com.pspace.ifs.KSAN.s3gw.identity.ObjectListParameter;
import com.pspace.ifs.KSAN.s3gw.multipart.Multipart;
import com.pspace.ifs.KSAN.s3gw.multipart.Part;
import com.pspace.ifs.KSAN.s3gw.multipart.ResultParts;
import com.pspace.ifs.KSAN.s3gw.multipart.ResultUploads;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
    
    // constant for data elements
    // for object collection
    //private static final String BUCKET="bucket";
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
        
        index = new Document(BUCKETNAME, 1);
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
            coll.createIndex(Indexes.ascending("uploadId", "partNo", OBJKEY));
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
        doc.append(DELETEMARKER, true);
        doc.append(PDISKID, md.getPrimaryDisk().getId());
        if (md.isReplicaExist())
            doc.append(RDISKID, md.getReplicaDisk().getId());
        if (!(md.getVersionId()).isEmpty())
            objects.updateMany(Filters.eq(OBJID, md.getObjId()), Updates.set(LASTVERSION, false));
        objects.insertOne(doc);
        return 0;
    }

    @Override
    public int updateDisks(Metadata md) {
       try {
            MongoCollection<Document> objects;
            objects = database.getCollection(md.getBucket());
            objects.updateOne(Filters.eq(OBJID, md.getObjId()), Updates.set(PDISKID, md.getPrimaryDisk().getId()));
            objects.updateOne(Filters.eq(OBJID, md.getObjId()), Updates.set(RDISKID, md.getReplicaDisk().getId()));
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
        
        Document doc =(Document)fit.first();
        if (doc == null)
          throw new   ResourceNotFoundException("There is not object with a bucket name " + bucketName + " and objid " + objId);
        
        Date lastModified = doc.getDate(LASTMODIFIED);
        String key       = (String)doc.get(OBJKEY);
        String etag         = doc.getString(ETAG);
        String meta         = doc.getString(META);
        String tag          = doc.getString(TAG);
        long size           = doc.getLong(SIZE);
        String acl          = doc.getString(ACL);
        String versionid    = doc.getString(VERSIONID);
        String deleteMarker = doc.getString(DELETEMARKER);
        boolean lastversion = doc.getBoolean(LASTVERSION);
        Bucket bt           = obmCache.getBucketFromCache(bucketName);
        String pdiskId      = doc.getString(PDISKID);
        DISK pdsk           = obmCache.getDiskWithId(bt.getDiskPoolId(), pdiskId);
        String rdiskId      = doc.getString(RDISKID);
        DISK rdsk           = obmCache.getDiskWithId(bt.getDiskPoolId(), rdiskId);
        
        mt = new Metadata( bucketName, key);
        mt.set(etag, tag, meta, acl, size);
        mt.setPrimaryDisk(pdsk);
        mt.setReplicaDISK(rdsk);
        mt.setLastModified(lastModified.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        //mt.setSize(size);
        mt.setVersionId(versionid, deleteMarker, lastversion);
        return mt;
    }
 
    @Override
    public Metadata selectSingleObject(String diskPoolId, String bucketName, String key) throws ResourceNotFoundException {
        Metadata mt;
        
        mt = new Metadata( bucketName, key);
        return selectSingleObjectInternal(bucketName, mt.getObjId(), ""); 
    }
    @Override
    public Metadata selectSingleObjectWithObjId(String diskPoolId, String bucketName, String objId) throws ResourceNotFoundException {
        return selectSingleObjectInternal(bucketName, objId, ""); 
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
            Date lastModified  = doc.getDate(LASTMODIFIED);
            String lastModifiedStr = lastModified.toInstant()
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
    public Bucket insertBucket(String bucketName, String diskPoolId, String userId, String acl, int replicaCount) throws ResourceAlreadyExistException{
        Document doc;
        Document index;
        Bucket bt = null;
        try{
            index = new Document(BUCKETNAME, 1);
            doc = new Document(BUCKETNAME, bucketName);
            doc.append(BUCKETNAME, bucketName);
            doc.append(DISKPOOLID, diskPoolId);
            doc.append(BUCKETID, new Metadata(bucketName, "/").getBucketId());
            doc.append(USERID, userId);
            doc.append(ACL, acl);
            doc.append(REPLICACOUNT, replicaCount);
            
            doc.append(WEB, "");
            doc.append(CORS, "");
            doc.append(LIFECYCLE, "");
            doc.append(ACCESS, "");
            doc.append(TAGGING, "");
            doc.append(REPLICATION, "");
            doc.append(VERSIONING, "");
            doc.append(MFADELETE, "");
            doc.append(CREATETIME, getCurrentDateTime());
            
            buckets.insertOne(doc);
            database.createCollection(bucketName);
            database.getCollection(bucketName).createIndex(index);
            bt = new Bucket(bucketName, bucketName, diskPoolId);
            getUserDiskPool(bt);
        } catch(SQLException ex){
            throw new ResourceAlreadyExistException(String.format("Bucket(%s) is laready exist in the db!", bucketName), ex);
        }
        return bt;
    }

    @Override
    public int deleteBucket(String bucketName) {
        buckets.deleteOne(Filters.eq(BUCKETNAME, bucketName));
        this.database.getCollection(bucketName).drop();
        return 0;
    }

    private Bucket parseBucket(String bucketName, Document doc) throws ResourceNotFoundException, SQLException{
        if (doc == null)
          throw new   ResourceNotFoundException("There is not bucket with a name " + bucketName);
        
        String diskPoolId = doc.getString(DISKPOOLID);
        String bucketId   = doc.getString(BUCKETID);
        String userId     = doc.getString(USERID);
        String web        = doc.getString(WEB);
        String acl        = doc.getString(ACL);
        String cors       = doc.getString(CORS);
        String lifecycle  = doc.getString(LIFECYCLE);
        String access     = doc.getString(ACCESS);
        String tagging    = doc.getString(TAGGING);
        String replication= doc.getString(REPLICATION);
        Date createTime = doc.getDate(CREATETIME);
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
        getUserDiskPool(bt);
        System.out.println(">>" + bt);
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
        FindIterable fit = buckets.find(eq(BUCKETNAME, bt.getName()));
        Document doc =(Document)fit.first();
        if (doc == null)
          return -1;
        buckets.updateOne(Filters.eq(BUCKETNAME, bt.getName()), Updates.set(VERSIONING, bt.getVersioning()));
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
        
        doc = new Document(OBJKEY, objkey);
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
        
        BasicDBObject sortList = new BasicDBObject(OBJKEY, 1 );
        sortList.append("uploadId", 1);
        sortList.append("partNo", 1);
        FindIterable fit = multip.find((BasicDBObject)query).limit(maxKeys + 1)
                .sort(sortList);
     
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            key      = doc.getString(OBJKEY);
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

    /*@Override
    public ObjectListParameter selectObjects(String bucketName, Object query, int maxKeys) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }*/

    @Override
    public int deleteObject(String bucketName, String objKey, String versionId) {
        MongoCollection<Document> objects;
        DeleteResult dres;
        objects = database.getCollection(bucketName);
        if (versionId.equalsIgnoreCase("null"))
            dres = objects.deleteOne(Filters.eq(OBJKEY, objKey));
        else
            dres = objects.deleteOne(Filters.and(Filters.eq(OBJKEY, objKey), Filters.eq(VERSIONID, versionId)));
        
        if (dres == null)
            return -1;
        
        return (int)dres.getDeletedCount();
    }

    @Override
    public Metadata selectSingleObject(String diskPoolId, String bucketName, String objKey, String versionId)
            throws ResourceNotFoundException {
        Metadata mt;
      
        mt = new Metadata( bucketName, objKey);
        return selectSingleObjectInternal(bucketName, mt.getObjId(), versionId); 
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
    public ResultUploads getUploads(String bucket, String delimiter, String prefix, String keyMarker, String uploadIdMarker, int maxUploads) throws SQLException, S3Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateObjectMeta(String bucketName, String objId, String versionid, String meta) throws SQLException {
        MongoCollection<Document> objects;
        objects = database.getCollection(bucketName);
        objects.updateOne(Filters.and(Filters.eq(OBJID, objId), Filters.eq("versionid", versionid)), Updates.set("meta", meta));
    }

    private int updateBucket(String bucketName, String key, String value){
        FindIterable fit = buckets.find(eq(BUCKETNAME, bucketName));
        Document doc =(Document)fit.first();
        if (doc == null)
          return -1;
        
        buckets.updateOne(Filters.eq(BUCKETNAME, bucketName), Updates.set(key, value));
        return 0;
    }
    
    @Override
    public void updateBucketAcl(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), "acl", bt.getAcl());
    }

    @Override
    public void updateBucketCors(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), "cors", bt.getCors());
    }

    @Override
    public void updateBucketWeb(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), "web", bt.getWeb());
    }

    @Override
    public void updateBucketLifecycle(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), "lifecycle", bt.getLifecycle());
    }

    @Override
    public void updateBucketAccess(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), "access", bt.getAccess());
    }

    @Override
    public void updateBucketTagging(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), "tagging", bt.getTagging());
    }

    @Override
    public void updateBucketReplication(Bucket bt) throws SQLException {
        updateBucket(bt.getName(), "replication", bt.getReplication());
    }

    @Override
    public boolean isUploadId(String uploadid) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /*@Override
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
    }*/
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
        
        utilJob = this.database.getCollection("UTILJOBS");
        if (utilJob == null)
            return new ArrayList<>();
        
        doc = new Document("Id", Id);
        doc.append("status", status);
        doc.append("TotalNumObject", TotalNumObject);
        doc.append("NumJobDone", 0);
        doc.append("checkOnly", checkOnly);
        doc.append("utilName", utilName);
        doc.append("startTime", startTime);
        utilJob.insertOne(doc);
        return getUtilJobObject(Id, status, 0, 0, false, " ", startTime);
    }
    private List<Object> selectUtilJob(String Id){
        MongoCollection<Document> utilJob;
        utilJob = this.database.getCollection("UTILJOBS");
        
        FindIterable fit = utilJob.find(eq("Id", Id));
        
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            return getUtilJobObject(Id, doc.getString("status"), 
                    doc.getLong("TotalNumObject"), doc.getLong("NumJobDone"), 
                    doc.getBoolean("checkOnly"), doc.getString("utilName"), 
                    doc.getString("startTime"));
        }
        return null;
    }
    
    private List<Object> updateStatusUtilJob(String Id, String status){
        MongoCollection<Document> utilJob;
        utilJob = this.database.getCollection("UTILJOBS");
        FindIterable fit = utilJob.find(eq("Id", Id));
        Document doc =(Document)fit.first();
        if (doc == null)
          return new ArrayList<>();
        
        utilJob.updateOne(Filters.eq("Id", Id), Updates.set("status", status));
        return getUtilJobObject(Id, status, 0, 0, false, " ", "");
    }
    
    private List<Object> updateNumberJobsUtilJob(String Id, long TotalNumObject, long NumJobDone){
        MongoCollection<Document> utilJob;
        utilJob = this.database.getCollection("UTILJOBS");
        FindIterable fit = utilJob.find(eq("Id", Id));
        Document doc =(Document)fit.first();
        if (doc == null)
          return new ArrayList<>();
        
        utilJob.updateOne(Filters.eq("Id", Id), Updates.set("TotalNumObject", TotalNumObject));
        utilJob.updateOne(Filters.eq("Id", Id), Updates.set("NumJobDone", NumJobDone));
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
        
        userDiskPool = this.database.getCollection("USERSDISKPOOL");
        if (userDiskPool == null)
            return -1;
        
        doc = new Document("userId", userId);
        doc.append("credential", secretKey + "_" + accessKey);
        doc.append("diskpoolId", diskpoolId);
        doc.append("replcaCount", replicaCount);
  
        userDiskPool.insertOne(doc);
        return 0;
    }

    @Override
    public Bucket getUserDiskPool(Bucket bt) throws SQLException {
        MongoCollection<Document> userDiskPool;
        String diskPoolId;
        int replicaCount;
        
        System.out.printf("userId : " + bt.getUserId());
        userDiskPool = this.database.getCollection("USERSDISKPOOL");
        
        FindIterable fit = userDiskPool.find(Filters.and(eq("userId", bt.getUserId()), Filters.eq("credential", "_")));
        
        Iterator it = fit.iterator();
        while((it.hasNext())){
            Document doc = (Document)it.next();
            diskPoolId      = doc.getString("diskpoolId");
            replicaCount   = doc.getInteger("replcaCount");
            bt.setDiskPoolId(diskPoolId);
            bt.setReplicaCount(replicaCount);
            break;
        }
        return bt;
    }

    @Override
    public int deleteUserDiskPool(String userId, String diskPoolId) throws SQLException {
       MongoCollection<Document> userDiskPool;
        
        userDiskPool = this.database.getCollection("USERSDISKPOOL");
        if (userDiskPool == null)
            return -1;
        
        userDiskPool.deleteOne(Filters.and(eq("userId", userId), eq("diskPoolId", diskPoolId)));
        return 0;
    }

    @Override
    public Object getStatement(String query) throws SQLException {
        return null;
    }

    @Override
    public List<Metadata> getObjectList(String bucketName, Object query, int maxKeys) throws SQLException {
        String diskPoolId = "1";
        MongoCollection<Document> objects;
        objects = database.getCollection(bucketName);
        BasicDBObject sortBy = new BasicDBObject(OBJKEY, 1 );
        BasicDBObject mongoQuery =(BasicDBObject)query;
        
        if (mongoQuery.containsField("versionid"))
            sortBy.append("lastModified", -1);
        
        FindIterable<Document> oit = objects.find(mongoQuery).limit(maxKeys).sort(sortBy);
        Iterator it = oit.iterator();
        List<Metadata> list = new ArrayList();
        Bucket bt = obmCache.getBucketFromCache(bucketName);
        if (bt != null)
            diskPoolId = bt.getDiskPoolId();
        
        while((it.hasNext())){
            Document doc = (Document)it.next();
            String key         = doc.getString(OBJKEY);
            String etag        = doc.getString("etag");
            String tag         = doc.getString("tag");
            String meta        = doc.getString("meta");
            String acl         = doc.getString("acl");
            String pdiskid     = doc.getString("pdiskId");
            String rdiskid     = doc.getString("rdiskId");
            Date lastModified  = doc.getDate("lastModified");
            LocalDateTime lastModifiedStr = lastModified.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            long size           = doc.getLong("size");
            Metadata mt = new Metadata(bucketName, key);
            mt.set(etag, tag, meta, acl, size);
            mt.setLastModified(lastModifiedStr);
                
            try {
                mt.setPrimaryDisk(obmCache.getDiskWithId(diskPoolId, pdiskid));
            } catch (ResourceNotFoundException ex) {
                mt.setPrimaryDisk(new DISK());
            }
            
            try {
                mt.setReplicaDISK(obmCache.getDiskWithId(diskPoolId, rdiskid));
            } catch (ResourceNotFoundException ex) {
                mt.setReplicaDISK(new DISK());
            }
            list.add(mt);
        }
        return list;
    }
}
