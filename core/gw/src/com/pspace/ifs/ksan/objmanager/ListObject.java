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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.mongodb.BasicDBObject;
import com.pspace.ifs.ksan.gw.identity.ObjectListParameter;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author legesse
 */

enum LISTTYPE {
    FORUTILITY, LISTOBJECT, LISTOBJECTVERSION; 
}
    
/*public class ListObject {
    private String bucketName;
    private String delimiter;
    private String startAfterKey;
    private String startAfterVersionId;
    private String prefix;
    private String commonPrefix;
    private String contents;
    private String lastKey;
    private String nextVersionMarker;
    private String diskid;
    private StringBuilder xmlStringBuilder;
    private int maxKeys;
    private int keyCounts;
    private long startOffset;
    private LISTTYPE listType;
    private List<Metadata> metaList;
    private DataRepository dbm;
    private ObjManagerCache  obmCache;
    
    class ListObjectCallBack implements DBCallBack{
        @Override
        public void call(String key, String etag, String lastModified, long size, 
                String versionId, String pdiskid, String rdiskid, boolean lastVersion) {
            String content;

            if (listType == LISTTYPE.FORUTILITY){
                Bucket bt = obmCache.getBucketFromCache(bucketName);
                Metadata mt = new Metadata(bucketName, key, etag, "", "", pdiskid, "", rdiskid, "");
                mt.setSize(size);
                try {
                    mt.setPrimaryDisk(obmCache.getDiskWithId(bt.getDiskPoolId(), pdiskid));
                    mt.setReplicaDISK(obmCache.getDiskWithId(bt.getDiskPoolId(), rdiskid));
                } catch (ResourceNotFoundException ex) {
                    //Logger.getLogger(ListObject.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                //mt.setLastModified(lastModified);
                metaList.add(mt);
                return;
            }
            
            setCommonPrefix(key);
            if (listType == LISTTYPE.LISTOBJECT)
                content = setContents(key, etag, lastModified, size);
            else //LISTTYPE.LISTOBJECTVERSION
                content = setContents(key, etag, lastModified, size, versionId, lastVersion);
            if (content == null);
            else if(content.isEmpty());
            else {
                if (contents == null)
                    contents = content;
                else if (contents.isEmpty())
                    contents = content;
                else
                    contents = contents + content;
            }
            lastKey = key;
        }
    }
    
    public ListObject(DataRepository dbm, String bucketName, String delimiter, String startAfterKey, int maxKeys, String prefix){
        this.dbm = dbm;
        this.bucketName = bucketName;
        this.delimiter = delimiter;
        this.startAfterKey = startAfterKey;
        this.startAfterVersionId = "";
        this.maxKeys = maxKeys;
        this.prefix = prefix;
        this.startOffset = 0;
        this.keyCounts = 0;
        this.contents = " ";
        this.commonPrefix = "";
        this.lastKey = "";
        listType = LISTTYPE.LISTOBJECT;
        xmlStringBuilder = new StringBuilder();
    }
    
    public ListObject(DataRepository dbm, String bucketName, String delimiter, String startAfterKey, String startAfterVersionId, int maxKeys, String prefix){
        this.dbm = dbm;
        this.bucketName = bucketName;
        this.delimiter = delimiter;
        this.startAfterKey = startAfterKey;
        this.startAfterVersionId = startAfterVersionId;
        this.maxKeys = maxKeys;
        this.prefix = prefix;
        this.startOffset = 0;
        this.keyCounts = 0;
        this.contents = " ";
        this.commonPrefix = "";
        this.lastKey = "";
        listType = LISTTYPE.LISTOBJECTVERSION;
        xmlStringBuilder = new StringBuilder();
    }
    public ListObject(DataRepository dbm, ObjManagerCache  obmCache, String bucketName, String diskid, long offset, int maxKeys){
        this.dbm = dbm;
        this.obmCache = obmCache;
        this.bucketName = bucketName;
        this.maxKeys = maxKeys;
        startOffset=offset;
        this.diskid = diskid;
        metaList = new ArrayList();
        listType = LISTTYPE.FORUTILITY;
        xmlStringBuilder = new StringBuilder();
    }
    
    // for ListObject 
    private String setContents(String key, String etag, String lastModified, long size){
        String content = String.format("     <Contents> \n"
                        + "\t <Key>%s</Key> \n"
                        + "\t <LastModified>%s</LastModified>\n"
                        + "\t <ETag>%s</ETag>\n"
                        + "\t <Size>%d</Size>\n"
                        + "\t <StorageClass>STANDARD</StorageClass>\n"
                        + "     </Contents>\n", key, lastModified, etag, size);
        
        this.keyCounts++;
        return content;
    }
    
    // for ListObjectVersion
    private String setContents(String key, String etag, String lastModified, long size, String versionId, boolean lastVersion){
        String content = String.format("     <Version> \n"
                        + "\t<IsLatest>%s</IsLatest>\n"
                        + "\t <Key>%s</Key> \n"
                        + "\t <LastModified>%s</LastModified>\n"
                        + "\t <ETag>%s</ETag>\n"
                        + "\t <Size>%d</Size>\n"
                        + "\t <StorageClass>STANDARD</StorageClass>\n"
                        + "\t <VersionId>%s</VersionId>\n"
                        + "     </Version>\n", lastVersion, key, lastModified, etag, size, versionId);
        
        this.keyCounts++;
        return content;
    }
    
    private String setHeaderListObjects(String common){
        return String.format(
                "\n<ListBucketResult>\n"
                        + "  <Name>%s</Name>\n"
                        + "  <Prefix>%s</Prefix>\n"
                        + "  <KeyCount>%d</KeyCount>\n"
                        + "  <MaxKeys>%s</MaxKeys>\n"
                        + "  <Delimiter>%s</Delimiter>\n"
                        + "  <IsTruncated>%s</IsTruncated>\n"
                        + "  %s"
                        + "  %s"
                        + "\n</ListBucketResult>\n"
                , bucketName, prefix, keyCounts, maxKeys, delimiter, false, this.contents, common);
    }
    
    private String setHeaderListObjectsVersion(String common){
        return String.format(
                "\n<ListVersionsResult>\n"
                        + "  <Name>%s</Name>\n"
                        + "  <Prefix>%s</Prefix>\n"
                        + "  <KeyCount>%d</KeyCount>\n"
                        + "  <MaxKeys>%s</MaxKeys>\n"
                        + "  <Delimiter>%s</Delimiter>\n"
                        + "  <IsTruncated>%s</IsTruncated>\n"
                        + "  <KeyMarker>%s</KeyMarker>\n"
                        + "  <VersionIdMarker>%s</VersionIdMarker>\n"
                        + "  <NextKeyMarker>%s</NextKeyMarker>\n"
                        + "  <NextVersionIdMarker>%s</NextVersionIdMarker>\n"
                        + "  %s"
                        + "  %s"
                        + "\n</ListVersionsResult>\n"
                , bucketName, prefix, keyCounts, maxKeys, delimiter, false, 
                startAfterKey, startAfterVersionId, startAfterKey, startAfterVersionId, this.contents, common);
    }
    
    private Document getResult(){
        String common = "";
        String res = "";
        
        if (this.contents == null);
        else if (!(this.contents.isEmpty())){
           if (!this.commonPrefix.isEmpty())
                common = String.format(
                   "   <CommonPrefixes>\n"
                 + "         <Prefix>%s</Prefix>\n"
                 + "     </CommonPrefixes>\n", commonPrefix);
        }
        
        if (!this.contents.isEmpty()){
            if (listType == LISTTYPE.LISTOBJECT)
                res = setHeaderListObjects(common);
            else
                res = setHeaderListObjectsVersion(common);
        }
            
        xmlStringBuilder.append(res);
        return stringToXML();
    }
    
    public void setStartOffset(long offset){
        this.startOffset = offset;
    }
     
    private long getStartOffset(){
        return this.startOffset;
    }
    
    private Object makeMongoQuery(){
        BasicDBObject listObjQuery = new BasicDBObject();
        if (delimiter.isEmpty())
           listObjQuery.put("key", new BasicDBObject("$regex", prefix).append("$options", "i"));
        else
           listObjQuery.put("key", new BasicDBObject("$regex", prefix + ".*" + delimiter).append("$options", "i")); // prefix like
        
        if (!startAfterKey.isEmpty())
            listObjQuery.append("$gt", startAfterKey);
        
        return listObjQuery;
    }
    
    private String makeQuery() {
        String sql;
        
        sql = "SELECT objKey, etag, lastModified, size, versionId, pdiskid, rdiskid, lastversion, meta FROM MDSDBTable WHERE bucket='" + bucketName + "'";

        if (this.listType == LISTTYPE.LISTOBJECT) {
            sql = sql +" AND lastversion=true AND deleteMarker <> 'mark'";
        } 

        // if (!prefix.isEmpty() || !delimiter.isEmpty())
        //     sql = sql +" WHERE";
        
        if (!startAfterKey.isEmpty()){
            //startAfterKey.replaceAll("/", "\/");
            sql = sql + " AND objKey > '" + startAfterKey.replaceAll("/", "\\/") + "' ";
        }
        
        if (!startAfterVersionId.isEmpty())
             sql = sql + " AND versionId > '" + startAfterVersionId + "' ";
        
        if (!prefix.isEmpty())
           sql = sql + " AND objKey LIKE '"+ prefix + "%'";
        
        // if (!prefix.isEmpty() && !delimiter.isEmpty())
        //     sql = sql + " AND";
        
        if (!delimiter.isEmpty()){
           sql = sql + " AND objKey NOT LIKE '%" + delimiter + "_%'"; 
        }
        
        sql = sql + " ORDER BY objKey asc, lastModified desc  LIMIT " + (maxKeys+1) + " OFFSET " + getStartOffset() + ";";
        
        System.out.println("listObject query : " + sql);

        return sql;
    }
    
    private Object makeQueryWithDiskId(String diskid, long offset){
         if (dbm instanceof MongoDataRepository){
             BasicDBObject orObjQuery;
             
             if (!diskid.isEmpty()){
                BasicDBObject or = new BasicDBObject();

                or.put("pdiskid", new BasicDBObject("$regex", diskid).append("$options", "i"));
                or.put("rdiskid", new BasicDBObject("$regex", diskid).append("$options", "i"));

                orObjQuery  = new BasicDBObject("$or", or);
             }
             else {
                 orObjQuery = new BasicDBObject();
                 orObjQuery.put("bucketName", new BasicDBObject("$regex", bucketName).append("$options", "i"));
             }
            return orObjQuery;
         }else{
            String sql; 
            if (!diskid.isEmpty()){
                sql = "SELECT objKey, etag, lastModified, size, versionid, pdiskid, rdiskid FROM MDSDBTable "
                        + "WHERE bucket='" + bucketName + "' AND (pdiskid like '" + diskid 
                        + "' OR rdiskid like '" + diskid + "') ORDER BY objKey LIMIT " 
                        + maxKeys + " OFFSET " + offset;
            }
            else {
                sql = "SELECT objKey, etag, lastModified, size, versionid, pdiskid, rdiskid FROM MDSDBTable "
                        + "WHERE bucket='" + bucketName +  "' ORDER BY objKey LIMIT " 
                        + maxKeys + " OFFSET " + offset;
            }
            return sql;
         }   
    }
    
    private void setCommonPrefix(String key){
        if (delimiter == null)
            return;
        
        if (!(delimiter.isEmpty())){
            if (commonPrefix.isEmpty())
                commonPrefix = key;
            else{
                while (!key.startsWith(commonPrefix)){
                   commonPrefix = commonPrefix
                           .substring( 0, commonPrefix.lastIndexOf(delimiter));
                }
            }
        }
        //System.out.println("commonPrefix>>>>>" + this.commonPrefix);
    }
    
    private Document stringToXML(){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc;
        
         try
         {
            ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("UTF-8"));
            builder = factory.newDocumentBuilder();
            doc = builder.parse(input);
      
            return doc;
         }catch (ParserConfigurationException | SAXException | IOException ex){
              Logger.getLogger(ListObject.class.getName()).log(Level.SEVERE, null, ex);
         }
         
         return null;
    }
    
    public Document excute(){
        DBCallBack cb = new ListObjectCallBack();
        Object query;
        try {
            if (listType == LISTTYPE.FORUTILITY)
                return null;
            
            if (dbm instanceof MongoDataRepository)
                query = makeMongoQuery();
            else 
                query = makeQuery();
            
            this.dbm.selectObjects(bucketName, query, this.maxKeys, cb);
        } catch (SQLException ex) {
            Logger.getLogger(ListObject.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return getResult();
    }

    public ObjectListParameter executeDirect() {
        ObjectListParameter objectListParameter = null;
        Object query;
        try {
            if (listType == LISTTYPE.FORUTILITY)
                return null;
            
            if (dbm instanceof MongoDataRepository)
                query = makeMongoQuery();
            else 
                query = makeQuery();
            
                objectListParameter = this.dbm.selectObjects(bucketName, query, this.maxKeys);
        } catch (SQLException ex) {
            Logger.getLogger(ListObject.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        return objectListParameter;
    }
    
    public List<Metadata> excute1(){
        DBCallBack cb = new ListObjectCallBack();
        Object query;
        try {
            if (listType != LISTTYPE.FORUTILITY)
                return null;
            query = makeQueryWithDiskId(this.diskid, this.startOffset);
            System.out.println("query : " + query);
            this.dbm.selectObjects(bucketName, query, this.maxKeys, cb);
        } catch (SQLException ex) {
            Logger.getLogger(ListObject.class.getName()).log(Level.SEVERE, null, ex);
        }
        return this.metaList;
    }
    
    @Override
    public String toString(){
        return xmlStringBuilder.toString();
    }
    
}*/

public class ListObject{
    private String bucketName;
    private String delimiter;
    private String marker;
    private int maxKeys;
    private String prefix;
    private String query;
    private String delmarker;
    private String versionIdMarker;
    private String continuationToken;
    private String listType;
    private String diskid;
    private long offset;
  
    private BasicDBObject mongoQuery;
    
    private boolean bBucketListParameterPrefix;
    private boolean bMarker;
    private boolean bDelimiter;
    private boolean bDelForceGte;
    private boolean bDelimiterMarker;
   
    private boolean bContinuationToken;
    private boolean bDelimitertoken;
    private boolean bVersionIdMarker;
    
    private ObjectListParameter objectListParameter;
    private DataRepository dbm;
    
    private void initParameters(DataRepository dbm, String bucketName, String delimiter, String marker, String versionIdMarker, String continuationToken, int maxKeys, String prefix){
        this.dbm = dbm;
        this.bucketName = bucketName;
        this.delimiter = delimiter;
        this.marker = marker;
        this.maxKeys = maxKeys;
        this.prefix = prefix;
        this.versionIdMarker = versionIdMarker;
        this.continuationToken = continuationToken;
        
        objectListParameter = new ObjectListParameter();
        mongoQuery = null;
        bBucketListParameterPrefix = false;
        bMarker = false;
        bDelimiter = false;
	bDelForceGte = false;
	bDelimiterMarker = false;
        bContinuationToken = false;
        bDelimitertoken = false;
        bVersionIdMarker = false;
        query = "";
        delmarker = ""; 
        
        if (!Strings.isNullOrEmpty(prefix)) {
            bBucketListParameterPrefix = true;
        }
        
        if (!Strings.isNullOrEmpty(marker)) {
            bMarker = true;
        }
        
        if (!Strings.isNullOrEmpty(continuationToken)) {
            bContinuationToken = true;
        }
        
        if (!Strings.isNullOrEmpty(delimiter)) {
            bDelimiter = true;
        }
        
	if (!Strings.isNullOrEmpty(versionIdMarker))  {
            bVersionIdMarker = true;
        }
        
        if (bMarker && bDelimiter) {
            if (marker.substring(marker.length() - delimiter.length(), marker.length()).compareTo(delimiter) == 0) {
                StringBuilder delimiterp2 = new StringBuilder(); 
                delimiterp2.append(marker.substring(0, marker.length()-1));
                delimiterp2.append(Character.getNumericValue(marker.charAt(marker.length()-1)) + 1);
                this.marker = delimiterp2.toString();
                bDelimiterMarker = true;
            }
	}
		
        if (bContinuationToken && bDelimiter) {
            if (continuationToken.substring(continuationToken.length() - delimiter.length(), continuationToken.length()).compareTo(delimiter) == 0) {
                StringBuilder delimiterp2 = new StringBuilder(); 
                delimiterp2.append(continuationToken.substring(0, continuationToken.length()-1));
                delimiterp2.append(Character.getNumericValue(continuationToken.charAt(continuationToken.length()-1)) + 1);
                this.continuationToken = delimiterp2.toString();
                bDelimitertoken = true;
            }
        }  
        
    }
    // for listObject
    public ListObject(DataRepository dbm, String bucketName, String delimiter, String marker, int maxKeys, String prefix) throws SQLException{
        /*this.dbm = dbm;
        this.bucketName = bucketName;
        this.delimiter = delimiter;
        this.marker = marker;
        this.maxKeys = maxKeys;
        this.prefix = prefix;
        objectListParameter = new ObjectListParameter();
        
        mongoQuery = null;
        bBucketListParameterPrefix = false;
        bMarker = false;
        bDelimiter = false;
	bDelForceGte = false;
	bDelimiterMarker = false;
        query = "";
        delmarker = "";*/
        listType = "listObject";
        initParameters(dbm, bucketName, delimiter, marker, null, null, maxKeys, prefix);
        
        /*if (!Strings.isNullOrEmpty(prefix)) 
            bBucketListParameterPrefix = true;
		
        if (!Strings.isNullOrEmpty(marker)) 
	    bMarker = true;
		
        if (!Strings.isNullOrEmpty(delimiter)) 
            bDelimiter = true;
	
        if (bMarker && bDelimiter) {
            if (marker.substring(marker.length() - delimiter.length(), marker.length()).compareTo(delimiter) == 0) {
                    StringBuilder delimiterp2 = new StringBuilder(); 
                    delimiterp2.append(marker.substring(0, marker.length() - 1));
                    delimiterp2.append(Character.getNumericValue(marker.charAt(marker.length() - 1)) + 1);
                    marker = delimiterp2.toString();
                    bDelimiterMarker = true;
            }
	}*/
       this.listObjectAndParse();
    }
    
    // for listObjectV2
    public ListObject(DataRepository dbm, String bucketName, String delimiter, String marker, String continuationToken, int maxKeys, String prefix) throws SQLException{
        /*this.dbm = dbm;
        this.bucketName = bucketName;
        this.delimiter = delimiter;
        this.marker = marker;
        this.maxKeys = maxKeys;
        this.prefix = prefix;
        this.continuationToken = continuationToken;
        objectListParameter = new ObjectListParameter();
        
        mongoQuery = null;
        bBucketListParameterPrefix = false;
        bMarker = false;
        bDelimiter = false;
	bDelForceGte = false;
	bDelimiterMarker = false;
        bContinuationToken = false;
        bDelimitertoken = false;
        query = "";
        delmarker = "";*/
        listType = "listObjectV2";
        initParameters(dbm, bucketName, delimiter, marker, null, continuationToken, maxKeys, prefix);
        
        /*if (!Strings.isNullOrEmpty(prefix)) {
            bBucketListParameterPrefix = true;
        }
        
        if (!Strings.isNullOrEmpty(marker)) {
            bMarker = true;
        }
        
        if (!Strings.isNullOrEmpty(continuationToken)) {
            bContinuationToken = true;
        }
        
        if (!Strings.isNullOrEmpty(delimiter)) {
            bDelimiter = true;
        }
	
        if (bMarker && bDelimiter) {
            if (marker.substring(marker.length() - delimiter.length(), marker.length()).compareTo(delimiter) == 0) {
                    StringBuilder delimiterp2 = new StringBuilder(); 
                    delimiterp2.append(marker.substring(0, marker.length()-1));
                    delimiterp2.append(Character.getNumericValue(marker.charAt(marker.length()-1)) + 1);
                    marker = delimiterp2.toString();
                    bDelimiterMarker = true;
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
        }*/
        this.listObjectAndParse();
    }
    
    // for listObjectVersion
    public ListObject(DataRepository dbm, String bucketName, String delimiter, String marker, String versionIdMarker, int maxKeys, String prefix, boolean versionOn) throws SQLException{
        /*this.dbm = dbm;
        this.bucketName = bucketName;
        this.delimiter = delimiter;
        this.marker = marker;
        this.maxKeys = maxKeys;
        this.prefix = prefix;
        this.versionIdMarker = versionIdMarker;
        objectListParameter = new ObjectListParameter();
        
        mongoQuery = null;
        bBucketListParameterPrefix = false;
        bMarker = false;
        bDelimiter = false;
	bDelForceGte = false;
	bDelimiterMarker = false;
        bVersionIdMarker = false;
        bDelimitertoken = false;
        query = "";
        delmarker = "";*/
        
        listType = "listObjectVersion";
        initParameters(dbm, bucketName, delimiter, marker, versionIdMarker, null, maxKeys, prefix);
        
        /*if (!Strings.isNullOrEmpty(prefix)) {
            bBucketListParameterPrefix = true;
        }
        
        if (!Strings.isNullOrEmpty(marker)) {
            bMarker = true;
        }

        if (!Strings.isNullOrEmpty(versionIdMarker))  {
            bVersionIdMarker = true;
        }

        if (!Strings.isNullOrEmpty(delimiter)) {
                bDelimiter = true;
        }
        
        if (!Strings.isNullOrEmpty(continuationToken)) {
            bContinuationToken = true;
        }*/
        
        this.listObjectAndParse();
    }
    
     // for utility
    public ListObject(DataRepository dbm, String bucketName, String diskid, long offset, int maxKeys) throws SQLException{
        this.dbm = dbm;
        this.bucketName = bucketName;
        this.diskid = diskid;
        this.offset = offset;
        this.maxKeys = maxKeys;
        listType = "utility";
        mongoQuery = null;
    }
    
    // new format
    public ListObject(DataRepository dbm, String bucketName, String delimiter, String keyMarker, String versionIdMarker, String continuationToken, int maxKeys, String prefix) throws SQLException{
        /*this.dbm = dbm;
        this.bucketName = bucketName;
        this.delimiter = delimiter;
        this.marker = keyMarker;
        this.maxKeys = maxKeys;
        this.prefix = prefix;
        this.versionIdMarker = versionIdMarker;
        this.continuationToken = continuationToken;
        objectListParameter = new ObjectListParameter();
        
        mongoQuery = null;
        bBucketListParameterPrefix = false;
        bMarker = false;
        bDelimiter = false;
	bDelForceGte = false;
	bDelimiterMarker = false;
        bVersionIdMarker = false;
        bDelimitertoken = false;
        bContinuationToken = false;
        query = "";
        delmarker = "";*/
        if (!Strings.isNullOrEmpty(continuationToken))
            listType = "listObjectV2";
        else if(!Strings.isNullOrEmpty(versionIdMarker))
            listType = "listObjectVersion";
        else
            listType = "listObject";
        
        initParameters(dbm, bucketName, delimiter, keyMarker, versionIdMarker, continuationToken, maxKeys, prefix);
        
        /*if (!Strings.isNullOrEmpty(prefix)) {
            bBucketListParameterPrefix = true;
        }
        
        if (!Strings.isNullOrEmpty(marker)) {
            bMarker = true;
        }

        if (!Strings.isNullOrEmpty(versionIdMarker))  {
                bVersionIdMarker = true;
        }

        if (!Strings.isNullOrEmpty(delimiter)) {
                bDelimiter = true;
        }*/
        //this.listObjectAndParse();
    }
    
    public ObjectListParameter getList(){
        return objectListParameter;
    }
    
    public List<Metadata> getUnformatedList() throws SQLException{
        Object query1;
        
        if (!listType.equalsIgnoreCase("utility"))
            return this.fetchList();

        query1 = makeQueryWithDiskId(diskid, offset);
        if (query1 != null){
            return dbm.getObjectList(bucketName, query1, maxKeys);
        }
        else{
            String sql = (String)query1;
            PreparedStatement stmt = (PreparedStatement)dbm.getStatement(sql);
            return dbm.getObjectList(bucketName, stmt, maxKeys);
        }
    }
    
    public void updateOffset(String diskid, long offset){
        if (!listType.equalsIgnoreCase("utility"))
            return;
        
        this.diskid = diskid;
        this.offset = offset;
    }
    
    private Object makeQueryWithDiskId(String diskid, long offset){
         if (dbm instanceof MongoDataRepository){
             BasicDBObject orObjQuery;
             
             if (!diskid.isEmpty()){
                BasicDBObject or[] = new BasicDBObject[2];

                or[0] = new BasicDBObject("pdiskId", new BasicDBObject("$eq", diskid));
                or[1] = new BasicDBObject("rdiskId", new BasicDBObject("$eq", diskid));
               
                orObjQuery  = new BasicDBObject("$or", or);   
             }
             else {
                 orObjQuery = new BasicDBObject();
                 //orObjQuery.put("bucketName", new BasicDBObject("$regex", bucketName).append("$options", "i"));
             }
             //System.out.println(" orObjQuery>>" +orObjQuery);
            return orObjQuery;
         }else{
            String sql; 
            if (!diskid.isEmpty()){
                sql = "SELECT * FROM MDSDBTable "
                        + "WHERE bucket='" + bucketName + "' AND (pdiskid like '" + diskid 
                        + "' OR rdiskid like '" + diskid + "') ORDER BY objKey LIMIT " 
                        + maxKeys + " OFFSET " + offset;
            }
            else {
                sql = "SELECT * FROM MDSDBTable "
                        + "WHERE bucket='" + bucketName +  "' ORDER BY objKey LIMIT " 
                        + maxKeys + " OFFSET " + offset;
            }
            return sql;
         }   
    }
    
    private void makeQueryV1(){
        
        query = "SELECT * FROM MDSDBTable WHERE bucket='" + bucketName + "' AND lastversion=true AND deleteMarker <> 'mark' ";

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
    }
    
    private void makeQueryV2(){
        
       query = "SELECT * FROM MDSDBTable WHERE bucket='" + bucketName + "' AND lastversion=true AND deleteMarker <> 'mark' ";
       
       if (bBucketListParameterPrefix)
           query += " AND objKey LIKE ?";
       
       if (bMarker) {
           if (bDelimiterMarker) {
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
    }
    
    private void makeQueryWithVersion(){
        
        query = "SELECT * FROM MDSDBTable WHERE bucket='" + bucketName + "' ";

	if (bBucketListParameterPrefix)
            query += " AND objKey LIKE ?";

	if (bMarker && !bVersionIdMarker)
	    query += " AND objKey > ?";

	if (bVersionIdMarker)
	    query += " AND ( objKey > ? OR (objKey = ? AND versionid > ?)) ";

	if (bDelForceGte) {
	    query += " AND objKey >= ?";
	}

        query  += " ORDER BY objKey ASC, lastModified ASC LIMIT " + (maxKeys + 1); 
    }
    
    private BasicDBObject makeMongoQuery(){
        if (dbm instanceof MongoDataRepository){
           String prefixStr;
           BasicDBObject andObjQuery;
           List<BasicDBObject> and = new ArrayList();
           
           and.add(new BasicDBObject("lastversion", "true"));
           and.add(new BasicDBObject("deleteMarker", new BasicDBObject("$ne", "mark")));
           prefixStr = prefix.replaceAll("\\%",  "\\\\/").replaceAll("\\_",  "\\\\_");
           //prefixStr = prefix.replace("/[.*+?^${}()|[\]\\]/g", '\\$&');
           if (bBucketListParameterPrefix){    
               and.add(new BasicDBObject("objKey", new BasicDBObject("$regex", prefixStr).append("$options", "i")));
           }
           
           if (listType.equalsIgnoreCase("listObjectVersion")){
                if (bMarker && !bVersionIdMarker)
                    and.add(new BasicDBObject("objKey", new BasicDBObject("$gt", marker)));//objkey

                if (bVersionIdMarker){
                    and.add(new BasicDBObject("objKey", new BasicDBObject("$gte", marker)));//objkey
                    and.add(new BasicDBObject("versionid", new BasicDBObject("$gt", versionIdMarker).append("$options", "i")));
                }
           }
           else{
               if (bMarker)
                   if (bDelimiterMarker)
                        and.add(new BasicDBObject("objKey", new BasicDBObject("$gte", marker))); //objkey
                    else
                        and.add(new BasicDBObject("objKey", new BasicDBObject("$gt", marker)));//objkey
               
               if (listType.equalsIgnoreCase("listObjectV2")){
                   if (bContinuationToken){ 
                        if (bDelimitertoken) 
                            and.add(new BasicDBObject("objKey", new BasicDBObject("$gte", continuationToken)));//objkey
                        else
                            and.add(new BasicDBObject("objKey", new BasicDBObject("$gt", continuationToken)));//objkey
                    }
               }
           }
           
           if (bDelForceGte)
               and.add(new BasicDBObject("$gte", delmarker)); //objkey
 
           
           andObjQuery = new BasicDBObject("$and", and.toArray());
           
           //System.out.println( "andObjQuery >>" + andObjQuery);
           return andObjQuery;
       }
       return null;
    }
    
    private void makeQuery(){
        mongoQuery = makeMongoQuery();
        if (mongoQuery != null)
            return;
        
        if (listType.equalsIgnoreCase("listObject")) 
            makeQueryV1();
        else if (listType.equalsIgnoreCase("listObjectV2")) 
            makeQueryV2();
        else if (listType.equalsIgnoreCase("listObjectVersion")) 
            makeQueryWithVersion();
    }
    
    private int setObject(String objKey, Metadata mt, int offset) throws Exception{
        S3Metadata s3Metadata = new S3Metadata();
        ObjectMapper jsonMapper = new ObjectMapper();
        
        try {
            s3Metadata = jsonMapper.readValue(mt.getMeta(), S3Metadata.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new Exception(e.getMessage());  
        }

        s3Metadata.setName(objKey);
        if (listType.equalsIgnoreCase("listObjectVersion")){
           s3Metadata.setVersionId(mt.getVersionId());
           s3Metadata.setIsLatest(mt.getLastVersion() ? "true" : "false"); 
        }

        objectListParameter.getObjects().add(s3Metadata);
        if ((objectListParameter.getObjects().size() + offset)== maxKeys) {
            if (objectListParameter.isTruncated()) {
                objectListParameter.setNextMarker(objKey);
                if (listType.equalsIgnoreCase("listObjectVersion"))
                   objectListParameter.setNextVersion(mt.getVersionId()); 
            }
            return 1; //break;
        }
        return 0;
    }
    
    /***************************************************************************************/
    
    private List<Metadata> bindAndExcuteV1() throws SQLException{
        int prepareOrder = 0;
        PreparedStatement pstmt = (PreparedStatement)dbm.getStatement(query);
	
        if (bBucketListParameterPrefix) {
            pstmt.setString(++prepareOrder, prefix.replaceAll("\\%",  "\\\\%").replaceAll("\\_",  "\\\\_") + "%");
        }

        if (bMarker)
            pstmt.setString(++prepareOrder, marker);

        if (bDelForceGte) {
            pstmt.setString(++prepareOrder, delmarker);
            bDelForceGte = false;
        }

        //System.out.println(pstmt.toString());
        return dbm.getObjectList(bucketName, pstmt, maxKeys);
    }
    
    private List<Metadata> bindAndExcuteV2() throws SQLException{
        int prepareOrder = 0;
        PreparedStatement pstmt = (PreparedStatement)dbm.getStatement(query);
	
        if (bBucketListParameterPrefix) {
            pstmt.setString(++prepareOrder, prefix.replaceAll("\\%",  "\\\\%").replaceAll("\\_",  "\\\\_") + "%");
	}

	if (bMarker)
            pstmt.setString(++prepareOrder, marker);
				
	if (bContinuationToken)
            pstmt.setString(++prepareOrder, continuationToken);

	if (bDelForceGte) {
            pstmt.setString(++prepareOrder, delmarker);
            bDelForceGte = false;
	}
        
        //System.out.println(pstmt.toString());
        return dbm.getObjectList(bucketName, pstmt, maxKeys);
    }
    
    private List<Metadata> bindAndExcuteVersion() throws SQLException{
        int prepareOrder = 0;
        PreparedStatement pstmt = (PreparedStatement)dbm.getStatement(query);
	
        if (bBucketListParameterPrefix) {
            pstmt.setString(++prepareOrder, prefix.replaceAll("\\%",  "\\\\%").replaceAll("\\_",  "\\\\_") + "%");
        }

	if (bMarker)
            pstmt.setString(++prepareOrder, marker);
        
        if (bVersionIdMarker) {
            pstmt.setString(++prepareOrder, marker);
            pstmt.setString(++prepareOrder, versionIdMarker);
        }

        if (bDelForceGte) {
            pstmt.setString(++prepareOrder, delmarker);
            bDelForceGte = false;
        }

        //System.out.println(pstmt.toString());
        return dbm.getObjectList(bucketName, pstmt, maxKeys);
    }
    
    private List<Metadata> bindAndExcute() throws SQLException{
        if (dbm instanceof MongoDataRepository){
            return dbm.getObjectList(bucketName, mongoQuery, maxKeys);
        }
        if (listType.equalsIgnoreCase("listObject")) 
            return bindAndExcuteV1();
        else if (listType.equalsIgnoreCase("listObjectV2")) 
            return bindAndExcuteV2();
        else if (listType.equalsIgnoreCase("listObjectVersion")) 
            return bindAndExcuteVersion();
        
        return new ArrayList();
    }
    
    private List<Metadata> fetchList() throws SQLException{
       makeQuery();
                                
       List<Metadata> list = bindAndExcute();
       return list;
    }
/*********************************************************************************************************************************/
    private int getCommonPrefixCount(){
       
        if (listType.equalsIgnoreCase("listObject"))
            return objectListParameter.getCommonPrefixes().size();
        
        if (listType.equalsIgnoreCase("listObjectV2"))
            return objectListParameter.getCommonPrefixes().size();
        
        return 0;
    }
    
    private void listObjectAndParse() throws SQLException {       
        int rowcount;
        
	objectListParameter.setIstruncated(true);	
	try {
            if (maxKeys == 0) {
                objectListParameter.setIstruncated(false);
            }
			
	    while ((objectListParameter.getObjects().size() + getCommonPrefixCount()) < maxKeys) {                                
                makeQuery();
                                
                List<Metadata> list = bindAndExcute();
                Iterator itr = list.iterator();
                rowcount = list.size();
                
                if (rowcount < (maxKeys + 1) || maxKeys == 0) {
                    objectListParameter.setIstruncated(false);
                }
                
                if (!bDelimiter) {
        
                    while (itr.hasNext()) {
                        Metadata mt = (Metadata)itr.next();
                        if (setObject(mt.getPath(), mt, 0) == 1)
                            break;
                    }

                    if (!objectListParameter.isTruncated()) {
                        break;
                    }

                } else {
                                        
                    int end = 0;
                    if (bBucketListParameterPrefix) {
                        end = prefix.length();
                    }

                    while (itr.hasNext()) {
                        Metadata mt = (Metadata)itr.next();
                        String objectName = mt.getPath();
                        String metaValue = mt.getMeta();
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

                                if (((objectListParameter.getObjects().size() + objectListParameter.getCommonPrefixes().size()) == maxKeys) 
                                        && !(listType.equalsIgnoreCase("listObjectVersion"))) {
                                    makeQuery();
                                    List<Metadata> truncateList = bindAndExcute();
                                    Iterator truncateItr = truncateList.iterator();
                         
                                    int truncateMatchCount = 0;

                                    while (truncateItr.hasNext()) {
                                        //mt = (Metadata)truncateItr.next();
                                        String truncateObjectName = objectName;//mt.getPath();
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
                                }
                                break;
                            }
                        } else {
                            int ret;
                            
                            if (listType.equalsIgnoreCase("listObject") || listType.equalsIgnoreCase("listObjectV2")) 
                                ret = setObject(objectName, mt, objectListParameter.getCommonPrefixes().size());
                            else  
                                ret = setObject(objectName, mt, 0);
                            
                            if (ret == 1)
                                break;
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
        }
    }
}
