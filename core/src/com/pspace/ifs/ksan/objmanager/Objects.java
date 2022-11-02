
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
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */
public class Objects {
    private final DataRepository dbm;
    private final DiskAllocation dAlloc;
    private final ObjManagerCache  obmCache;
    private BucketManager bucketMGT;
    private static Logger logger;
    
    public Objects(DataRepository dbm, DiskAllocation dAlloc, ObjManagerCache  obmCache, BucketManager bucketMGT){
        logger = LoggerFactory.getLogger(Objects.class);
        this.dbm = dbm;
        this.dAlloc = dAlloc;
        this.obmCache = obmCache;
        this.bucketMGT = bucketMGT;
    }
       
    private Metadata _open(String bucketName, String key, String versionId)
            throws ResourceNotFoundException{
        Metadata mt;
        Bucket bt;
        logger.debug("[_open] Begin bucketName : {} key : {}  versionId : {} ", bucketName, key, versionId == null ? "null" : versionId);
        try {
            bt = bucketMGT.getBucket(bucketName);
        } catch (SQLException ex) {
            throw new ResourceNotFoundException("[_open] Bucket (" + bucketName +") failed to reterive in the system due to :" + ex);
        }
        
        if (bt == null){
            throw new ResourceNotFoundException("[_open] Bucket(" + bucketName +") not exist in the system!");
        }
        
        if (versionId == null)
            mt = dbm.selectSingleObject(bt.getDiskPoolId(), bucketName, key);
        else
            mt = dbm.selectSingleObject(bt.getDiskPoolId(), bucketName, key, versionId);
        
        logger.debug("[_open] End bucketName : {} key : {}  versionId : {} ", bucketName, key, versionId == null ? "null" : versionId);
        return mt;
    }
    
    private Metadata _create(String bucketName, String key, String versionId, String diskPoolId, int algorithm)
            throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        Bucket bt;
        logger.debug("[_create] Begin bucketName : {} key : {}  versionId : {} alg : {} ", bucketName, key, versionId, algorithm == AllocAlgorithm.LOCALPRIMARY? "LOCAL" : "ROUNDROBIN");
        if (dAlloc == null){
            throw new ResourceNotFoundException("[_create] No disk to allocate!");
        }
        
        try { 
            bt = bucketMGT.getBucket(bucketName);
            logger.debug("[_create] in_diskPoolId {} default_diskpoolId {}", diskPoolId == null ? "null" : diskPoolId, bt.getDiskPoolId());
            if (diskPoolId == null)
               diskPoolId = bt.getDiskPoolId();
            else if (diskPoolId.isEmpty())
               diskPoolId = bt.getDiskPoolId(); 
        } catch (SQLException ex) {
            throw new ResourceNotFoundException("[_create] Bucket(" + bucketName +") not found due to " + ex);
        }
        
        DISKPOOL dp = obmCache.getDiskPoolFromCache(diskPoolId);
        if (dp == null){
            throw new ResourceNotFoundException("[_create] Diskpool with id "+ diskPoolId +" not found!");
        }
        
        // create meta
        Metadata mt = new Metadata(bucketName, key);
        mt.setVersionId(versionId, "", Boolean.TRUE);

        // allocate disk
        dAlloc.allocDisk(mt, diskPoolId, dp.getDefaultReplicaCount(), algorithm); // FIXME replace bucket id
        
        if (mt.isReplicaExist())
            logger.debug("[_create] End bucketName : {} key : {} versionId : {} replicaCount : {} pdsik : {}({}) rdisk : {}({})", bucketName, key, versionId, 
                dp.getDefaultReplicaCount(), mt.getPrimaryDisk().getPath(), mt.getPrimaryDisk().getId(),  mt.getReplicaDisk().getPath(),  mt.getReplicaDisk().getId());
        else
            logger.debug("[_create] End bucketName : {} key : {} versionId : {} replicaCount : {} pdsik : {}({}) rdisk : {}", bucketName, key, versionId, 
                dp.getDefaultReplicaCount(), mt.getPrimaryDisk().getPath(), mt.getPrimaryDisk().getId(), ""); 
        return mt;
    }
    
    private Metadata create(String bucketName, String key, String versionId, String diskPoolId, int algorithm)
            throws IOException, AllServiceOfflineException , ResourceNotFoundException {
        Metadata mt;
        
        try {
            mt = _open(bucketName, key, versionId);
            return mt; 
        } catch (ResourceNotFoundException ex) {
            mt = _create(bucketName, key, versionId, diskPoolId, algorithm); 
            return mt; 
        }
    }
    
    private int _removeObject(String bucketName, String key, String  versionId) {
        logger.debug("[_removeObject] Begin bucketName : {} key : {}  versionId : {} ", bucketName, key, versionId == null ? "null" : versionId);
        try {
            Metadata mt;
            Bucket bt = bucketMGT.getBucket(bucketName);
            mt = dbm.selectSingleObject(bt.getDiskPoolId(), bucketName, key, versionId);
            // remove from DB or mark
            dbm.deleteObject(bucketName, key, versionId);
        } catch (ResourceNotFoundException | SQLException ex) {
            logger.debug(ex.getMessage());
            return -1;
        }
        logger.debug("[_removeObject] End bucketName : {} key : {}  versionId : {} ", bucketName, key, versionId == null ? "null" : versionId);
        return 0;
    }
    
    private Metadata _createCopy(String bucketName, String from, String versionId, String toBucketName, String to) throws IOException, AllServiceOfflineException, ResourceNotFoundException, SQLException{
        Metadata mt;
        Metadata cpy_mt;
        
        logger.debug("[_createCopy] Begin from (bucketName : {} key : {}  versionId : {}) to (bucketName : {} key : {})", bucketName, from, versionId, toBucketName, to);
        Bucket bt = bucketMGT.getBucket(bucketName);
        mt = dbm.selectSingleObject(bt.getDiskPoolId(), bucketName, from, versionId);
        if (mt == null)
            throw new ResourceNotFoundException("[_createCopy] Bucket(" + bucketName +")  and key("+ from +") not exist!");
        
        cpy_mt = new Metadata(toBucketName, to);
        cpy_mt.setPrimaryDisk(mt.getPrimaryDisk());
        cpy_mt.setReplicaCount(mt.getReplicaCount());
        if (mt.isReplicaExist())
            cpy_mt.setReplicaDISK(mt.getReplicaDisk());
        
        logger.debug("[_createCopy] End from (bucketName : {} key : {}  versionId : {}) to (bucketName : {} key : {})", bucketName, from, versionId, toBucketName, to);
        return cpy_mt;
    }
    
    private int _close(String bucketName, String key, Metadata mt) throws ResourceNotFoundException, SQLException{
        Bucket bt;
        
        logger.debug("[_close] Begin bucketName : {} key : {}  versionId : {} ", bucketName, key, mt.getVersionId());
        try {
            bt = bucketMGT.getBucket(bucketName);
        } catch (SQLException ex) {
           throw new ResourceNotFoundException("[_close] Bucket(" + bucketName +") not exist ! >" + ex); 
        }
        
        if (bt == null)
           throw new ResourceNotFoundException("[_close] Bucket(" + bucketName +") not exist!");
        try{
            Metadata mt2 = dbm.selectSingleObject(bt.getDiskPoolId(), bucketName, key, mt.getVersionId());
            if (!bucketMGT.getBucketVersioning(bucketName).equalsIgnoreCase("Enabled")){
               logger.debug("[OVERWRITE OBJECT] {}", mt2);
               dbm.deleteObject(bucketName, key, mt.getVersionId()); 
            }
        } catch(ResourceNotFoundException ex){
            
        }
        
        if (!obmCache.isDiskSeparatedAndValid(bt.getDiskPoolId(), mt)){
            String err = String.format("[_close] Bucket : %s key : %s disk information pdiskid : %s rdiskid : %s is not separated or not exist in the syystem",
                    bucketName, key, mt.getPrimaryDisk().getId(), mt.isReplicaExist()? mt.getReplicaDisk().getId()  : "");
            logger.debug(err);
            throw new InvalidParameterException(err);
        }
        
        mt.updateLastmodified(); // to update last modified time to now
        logger.debug("[_close] End bucketName : {} key : {}  versionId : {} ", bucketName, key, mt.getVersionId());
        return dbm.insertObject(mt); 
    }
    
    private HashMap<String, String> getTagsKeyValue(String tagsList){
        String arr[];
        HashMap<String, String> tags = new HashMap<>();
        
        if (tagsList.isEmpty())
            return tags;
        
        if (tagsList.contains(","))
            arr = tagsList.split(",");
        else{
           arr = new String[1]; 
           arr[0]= tagsList;
        }
        
        for (String tag : arr){
            if (tag.contains(":")){
               String kv[] = tag.split(":");
               tags.put(kv[0], kv[1]);
            }
            else
              tags.put(tag, " ");  
        }
        return tags;
    }
    
    private Object getListWithTagQuery(String tagsList){
        String sql = "";
        List<BasicDBObject> and = new ArrayList();
        HashMap<String, String> pair = getTagsKeyValue(tagsList);
        if (pair.isEmpty())
            return null;
        if (dbm instanceof MongoDataRepository){
            for (String key : pair.keySet()){
               and.add(new BasicDBObject("TagKey", new BasicDBObject("$eq", key)));
               String value = pair.get(key);
               if (value.isEmpty())
                   and.add(new BasicDBObject("TagValue", value));
            }

            if (pair.size() == 1)
                return and.get(0);
            else
                return new BasicDBObject("$and", and.toArray());
        }
        else {
            for (String key : pair.keySet()){
                String value = pair.get(key);
                if (sql.isEmpty())
                    sql = sql + "WHERE TagKey=" + key;
                else{
                    sql = sql + " AND TagKey=" + key;  
                } 

                if (!value.isEmpty())
                    sql = sql + " AND TagValue=" + value;
            }
            return sql;
        }
    }
    
    private String convertXML2Json(String xmlstr){
        String tags[] = xmlstr.split("<Tagging>");
        if (tags.length > 0){
            String tag = tags[1].replaceAll("<Tagging>", "").replaceAll("</Tagging>", "");
            tag = tag.replaceAll("<TagSet>", "{");
            tag = tag.replaceAll("</TagSet>", "}");
            tag = tag.replaceAll("<Tag>", "");
            tag = tag.replaceAll("</Tag>", ",");
            tag = tag.replaceAll("<Key>", "\"");
            tag = tag.replaceAll("</Key>", "\": ");
            tag = tag.replaceAll("<Value>", " \"");
            tag = tag.replaceAll("</Value>", "\" ");
            logger.debug("[convertXML2Json] xml : {} => json : {}", xmlstr, tag);
            return tag;
        }   
        return null;
    }
    
    /******************************************************************/
    
    public Metadata open(String bucketName, String key, String versionId) throws ResourceNotFoundException{
        return _open(bucketName, key, versionId);
    }
    
    public Metadata create(String diskpoolId, String bucketName, String key, String versionId)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return create(bucketName, key, versionId, diskpoolId, AllocAlgorithm.ROUNDROBIN);
    }
    
    public Metadata createLocal(String diskpoolId, String bucketName, String key, String versionId)throws IOException, AllServiceOfflineException, ResourceNotFoundException{
        return create(bucketName, key, versionId, diskpoolId, AllocAlgorithm.LOCALPRIMARY);
    }
    
    public Metadata createCopy(String bucketName, String from, String versionId, String toBucket, String to) throws IOException, AllServiceOfflineException, ResourceNotFoundException, SQLException{
       return _createCopy(bucketName, from, versionId, toBucket, to);
    } 
    
    public int remove(String bucket, String key, String  versionId) {
        return _removeObject(bucket, key, versionId); 
    }
    
    public int close(String bucketName, String key, Metadata mt) throws ResourceNotFoundException, SQLException{
        return _close(bucketName, key, mt);
    }
     
    public void updateObjectMeta(Metadata mt) throws SQLException {
        dbm.updateObjectMeta(mt);
    }
    
    public void updateObjectTagging(Metadata mt) throws SQLException {
        String tags = mt.getTag();
        dbm.updateObjectTagging(mt, convertXML2Json(tags));
    }

    public void updateObjectAcl(Metadata mt) throws SQLException {
        dbm.updateObjectAcl(mt);
    }
    
    private static int parseMaxKeys(String maxKeysStr){
        int maxKeys;
        try{
            maxKeys = Integer.parseInt(maxKeysStr);
        } catch(NumberFormatException ex){
            maxKeys = 0;
        }
        return maxKeys;
    }
    
    public ObjectListParameter listObject(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        ListObject list = new ListObject(dbm, bucketName, s3ObjectList.getDelimiter(), s3ObjectList.getMarker(), parseMaxKeys(s3ObjectList.getMaxKeys()), s3ObjectList.getPrefix());
        return list.getList();
    }

    public ObjectListParameter listObjectV2(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        ListObject list = new ListObject(dbm, bucketName, s3ObjectList.getDelimiter(), s3ObjectList.getStartAfter(), s3ObjectList.getContinuationToken(), parseMaxKeys(s3ObjectList.getMaxKeys()), s3ObjectList.getPrefix());
        return list.getList();
    }

    public ObjectListParameter listObjectVersions(String bucketName, S3ObjectList s3ObjectList) throws SQLException {
        ListObject list = new ListObject(dbm, bucketName, s3ObjectList.getDelimiter(), s3ObjectList.getKeyMarker(), s3ObjectList.getVersionIdMarker(), parseMaxKeys(s3ObjectList.getMaxKeys()), s3ObjectList.getPrefix(), true);
        return list.getList();
    }
    
    public List<Metadata> listObjectWithTags(String bucketName, String tagsList, int maxObjects) throws SQLException{
        Object query = getListWithTagQuery(tagsList);
        logger.debug("[listObjectWithTags] query> {}",  query.toString());
        return dbm.listObjectWithTags(bucketName, query, maxObjects);
    }
}
