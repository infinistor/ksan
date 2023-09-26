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
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */

enum LISTTYPE {
    FORUTILITY, LISTOBJECT, LISTOBJECTVERSION; 
}

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
    //private long offset;
    private String lastObjId;
  
    private BasicDBObject mongoQuery;
    
    private boolean bBucketListParameterPrefix;
    private boolean bMarker;
    private boolean bDelimiter;
    private boolean bDelForceGte;
    private boolean bDelimiterMarker;
   
    private boolean bContinuationToken;
    private boolean bDelimitertoken;
    private boolean bVersionIdMarker;
    
    private long expiredTime;
    
    private ObjectListParameter objectListParameter;
    private DataRepository dbm;
    private static Logger logger  = LoggerFactory.getLogger(ListObject.class);
    
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

        listType = "listObject";
        initParameters(dbm, bucketName, delimiter, marker, null, null, maxKeys, prefix);
        
       this.listObjectAndParse();
    }
    
    // for listObjectV2
    public ListObject(DataRepository dbm, String bucketName, String delimiter, String marker, String continuationToken, int maxKeys, String prefix) throws SQLException{
   
        listType = "listObjectV2";
        initParameters(dbm, bucketName, delimiter, marker, null, continuationToken, maxKeys, prefix);
        
        this.listObjectAndParse();
    }
    
    // for listObjectVersion
    public ListObject(DataRepository dbm, String bucketName, String delimiter, String marker, String versionIdMarker, int maxKeys, String prefix, boolean versionOn) throws SQLException{
        
        listType = "listObjectVersion";
        initParameters(dbm, bucketName, delimiter, marker, versionIdMarker, null, maxKeys, prefix);
                
        this.listObjectAndParse();
    }
    
     // for utility
    public ListObject(DataRepository dbm, String bucketName, String diskid, String lastObjId, int maxKeys) throws SQLException{
        this.dbm = dbm;
        this.bucketName = bucketName;
        this.diskid = diskid;
        this.lastObjId = lastObjId;
        this.maxKeys = maxKeys;
        this.versionIdMarker = null;
        listType = "utility";
        mongoQuery = null;
    }
    
    // new format
    public ListObject(DataRepository dbm, String bucketName, String delimiter, String keyMarker, String versionIdMarker, String continuationToken, int maxKeys, String prefix) throws SQLException{

        if (!Strings.isNullOrEmpty(continuationToken))
            listType = "listObjectV2";
        else if(!Strings.isNullOrEmpty(versionIdMarker))
            listType = "listObjectVersion";
        else
            listType = "listObject";
        
        initParameters(dbm, bucketName, delimiter, keyMarker, versionIdMarker, continuationToken, maxKeys, prefix);
    }
    
    public ObjectListParameter getList(){
        return objectListParameter;
    }
    
    public void setExpiredTime(long expiredTime, String type){
        this.expiredTime = expiredTime;
        listType = type;
    }
    
    public List<Metadata> getUnformatedList() throws SQLException{
        Object query1;
        
        if (!listType.equalsIgnoreCase("utility") && !listType.startsWith("listExpired"))
            return this.fetchList();

        if (listType.equalsIgnoreCase("utility"))
            query1 = makeQueryWithDiskId(diskid, lastObjId, versionIdMarker);
        else{
            query1=makeQuery4ExpiredObject();
            if (dbm instanceof MongoDataRepository)
                return dbm.getObjectList(bucketName, query1, maxKeys, 0);
            else
                return this.bindExcuteExpiredObjectQuery();
        }
        
        if (query1 != null){
            if (dbm instanceof MongoDataRepository)
                return dbm.getObjectList(bucketName, query1, maxKeys, 0);
            else{
                 String sql = (String)query1;
                PreparedStatement stmt = (PreparedStatement)dbm.getStatement(sql);
                return dbm.getObjectList(bucketName, stmt, maxKeys, 0);
            }
        }
        throw new SQLException("[ListObject] supported data storage not exist! check objmanager repostory config");
    }
    
    public long getUnformatedListCount() throws SQLException{
        Object queryStmt;
        
        queryStmt = makeQueryWithDiskIdToCount(diskid);
        return dbm.getObjectListCount(bucketName, queryStmt);
    }
    
    public void updateOffset(String diskid, String lastObjId, String lastVersionId){
        if (!listType.equalsIgnoreCase("utility"))
            return;
        
        this.diskid = diskid;
        this.lastObjId = lastObjId;
        this.versionIdMarker = lastVersionId;
    }
    
    private Object makeQueryWithDiskId(String diskid, String lastObjId, String lastVersionId){
            boolean isVersionOn = true;
            
            if (lastVersionId == null)
                isVersionOn = false;
            else if (lastVersionId.equalsIgnoreCase("null") == true)
                isVersionOn = false;
            else if(lastVersionId.isEmpty())
                isVersionOn = false;
            
            if (dbm instanceof MongoDataRepository){
             BasicDBObject orObjQuery;
               
             if (!diskid.isEmpty()){
                BasicDBObject or[] = new BasicDBObject[2];
                BasicDBObject and1[] = isVersionOn == false ? new BasicDBObject[2] : new BasicDBObject[3];

                or[0] = new BasicDBObject("pdiskid", new BasicDBObject("$eq", diskid));
                or[1] = new BasicDBObject("rdiskid", new BasicDBObject("$eq", diskid));
               
                //orObjQuery  = new BasicDBObject("$or", or);
                and1[0] =  new BasicDBObject("$or", or);
                and1[1] = new BasicDBObject("objId", new BasicDBObject("$gt", lastObjId));
                if (isVersionOn)
                    and1[2] = new BasicDBObject("versionid", new BasicDBObject("$lt", lastVersionId));
                orObjQuery  = new BasicDBObject("$and", and1);
             }
             else {
                if (!isVersionOn)
                    orObjQuery = new BasicDBObject("objId", new BasicDBObject("$gt", lastObjId));
                else{
                   BasicDBObject and1[] = new BasicDBObject[2];
                   and1[0] = new BasicDBObject("objId", new BasicDBObject("$gt", lastObjId));
                   and1[1] = new BasicDBObject("versionid", new BasicDBObject("$lt", lastVersionId));
                   orObjQuery  = new BasicDBObject("$and", and1);
                }
                 //orObjQuery.put("bucketName", new BasicDBObject("$regex", bucketName).append("$options", "i"));
             }
             //System.out.println(" orObjQuery>>" +orObjQuery);
            return orObjQuery;
         }else{
            String sql; 
            if (!diskid.isEmpty()){
                if (!isVersionOn){
                    sql = "SELECT * FROM `" + bucketName + "`"
                            + " WHERE (pdiskid like '" + diskid 
                            + "' OR rdiskid like '" + diskid + "') AND objId > '"+ lastObjId + "' ORDER BY objId LIMIT " 
                            + maxKeys ;
                }
                else {
                    sql = "SELECT * FROM `" + bucketName + "`"
                            + " WHERE (pdiskid like '" + diskid 
                            + "' OR rdiskid like '" + diskid + "') AND objId > '"+ lastObjId + "' AND versionid < '"+ lastVersionId +"' ORDER BY objId LIMIT " 
                            + maxKeys ;
                }
            }
            else {
                if (!isVersionOn){
                    sql = "SELECT * FROM `" + bucketName + "`"
                            + " WHERE objId > '"+ lastObjId +  "' ORDER BY objId LIMIT " 
                            + maxKeys;
                }
                else{
                    sql = "SELECT * FROM `" + bucketName + "`"
                            + " WHERE objId > '"+ lastObjId +  "' AND versionid < '"+ lastVersionId +"' ORDER BY objId LIMIT " 
                            + maxKeys; 
                }
            }
            System.out.println(" SqlQuery>>" + sql);
            return sql;
         }   
    }
    
    private Object makeQueryWithDiskIdToCount(String diskid){
        if (dbm instanceof MongoDataRepository){
            return makeQueryWithDiskId(diskid, " ", null);
        }
        
        String sql;
        if (!diskid.isEmpty()){
            sql = "SELECT count(*) FROM `" + bucketName + "`"
                        + "WHERE bucket='" + bucketName + "' AND (pdiskid like '" + diskid 
                        + "' OR rdiskid like '" + diskid + "')";
        }
        else {
            sql = "SELECT count(*) FROM '" + bucketName + "'"; 
        }
        return sql;
    }
    
    private void makeQueryV1(){
        
        query = "SELECT * FROM `" + bucketName + "` WHERE bucket='" + bucketName + "' AND lastversion=true AND deleteMarker <> 'mark' ";

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
        
       query = "SELECT * FROM `"+ bucketName +"` WHERE bucket='" + bucketName + "' AND lastversion=true AND deleteMarker <> 'mark' ";
       
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
        
        query = "SELECT * FROM `"+ bucketName +"` WHERE bucket='" + bucketName + "' ";

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
           BasicDBObject objQuery;
           List<BasicDBObject> and = new ArrayList();
           
           if (!listType.equalsIgnoreCase("listObjectVersion")){
               and.add(new BasicDBObject("lastversion", true));
               and.add(new BasicDBObject("deleteMarker", new BasicDBObject("$ne", "mark")));
           }
           
           prefixStr = prefix.replaceAll("\\%",  "\\\\/")
                   .replaceAll("\\_",  "\\\\_")
                   .replaceAll("\\(", "\\\\(")
                   .replaceAll("\\)", "\\\\)");
           //prefixStr = prefix.replace("/[.*+?^${}()|[\]\\]/g", '\\$&');
           if (bBucketListParameterPrefix){   
                if (!bDelimiterMarker)
                    and.add(new BasicDBObject("objKey", new BasicDBObject("$regex", "^" + prefixStr)));//.append("$options", "i")
                else{
                    List<BasicDBObject> lor = new ArrayList();
                    lor.add(new BasicDBObject("objKey", new BasicDBObject("$regex", "^" + prefixStr + "[^/]*$")));
                    lor.add(new BasicDBObject("objKey", new BasicDBObject("$regex", "^" + prefixStr + "[^/]*/+$")));
                    and.add(new BasicDBObject("objKey", new BasicDBObject("$or", lor.toArray())));
                }
           }
           
           if (listType.equalsIgnoreCase("listObjectVersion")){
               
                if (bMarker && !bVersionIdMarker){
                    //objQuery = new BasicDBObject("objKey", marker);
                    and.add(new BasicDBObject("objKey", new BasicDBObject("$gt", marker)));//objkey
                }

                if (bVersionIdMarker){
                    
                    /*and.add(new BasicDBObject("objKey", new BasicDBObject("$gte", marker)));//objkey
                    if (!versionIdMarker.equalsIgnoreCase("null"))
                        and.add(new BasicDBObject("versionid", new BasicDBObject("$gte", versionIdMarker)));
                    else
                        and.add(new BasicDBObject("versionid", new BasicDBObject("$gte", "")));*/
                    if (!versionIdMarker.equalsIgnoreCase("null")){
                        objQuery = new BasicDBObject("objKey", marker);
                        objQuery.append("versionid", new BasicDBObject("$lt", versionIdMarker));
                        and.add(objQuery);
                    } 
                    else{
                        and.add(new BasicDBObject("versionid", versionIdMarker));
                    }
                    
                    and.add(new BasicDBObject("objKey", new BasicDBObject("$gt", marker)));
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
           
           if (bDelForceGte){
               and.add(new BasicDBObject("objKey", new BasicDBObject("$gte", delmarker))); //objkey
               bDelForceGte = false;
           }
 
           if (and.isEmpty())
               andObjQuery = new BasicDBObject();
           else if (listType.equalsIgnoreCase("listObjectVersion")){
               if (!versionIdMarker.equalsIgnoreCase("null"))
                    andObjQuery = new BasicDBObject("$or", and.toArray());
               else
                    andObjQuery = new BasicDBObject("$and", and.toArray());  
           } else
               andObjQuery = new BasicDBObject("$and", and.toArray());
           
           //System.out.println( "andObjQuery >>" + andObjQuery);
           return andObjQuery;
       }
       return null;
    }
    
    private List<Metadata> bindExcuteExpiredObjectQuery() throws SQLException{
        int prepareOrder = 0;
        PreparedStatement pstmt;
        
        if (query.isEmpty())
            return null;
        
	pstmt = (PreparedStatement)dbm.getStatement(query);
        if(!listType.equalsIgnoreCase("listExpiredDeletedObject"))
            pstmt.setLong(++prepareOrder, expiredTime);
        
        if (bBucketListParameterPrefix) {
            pstmt.setString(++prepareOrder, prefix.replaceAll("\\%",  "\\\\%").replaceAll("\\_",  "\\\\_") + "%");
        }

	if (bMarker && !bVersionIdMarker)
            pstmt.setString(++prepareOrder, marker);
        
        if (bMarker && bVersionIdMarker) {
            pstmt.setString(++prepareOrder, marker);
            pstmt.setString(++prepareOrder, versionIdMarker);
        }

        if (bDelForceGte) {
            pstmt.setString(++prepareOrder, delmarker);
            bDelForceGte = false;
        }

        return dbm.getObjectList(bucketName, pstmt, maxKeys, 0);
    }
    
    private Object makeQuery4ExpiredObject(){
        if (dbm instanceof MongoDataRepository){
           String prefixStr;
           BasicDBObject andObjQuery;
           BasicDBObject objQuery;
           List<BasicDBObject> and = new ArrayList();
           
           if (listType.equalsIgnoreCase("listExpiredObject")){
               and.add(new BasicDBObject("lastversion", true));
               and.add(new BasicDBObject("deleteMarker", new BasicDBObject("$ne", "mark")));
           } 
           else if(listType.equalsIgnoreCase("listExpiredObjectVersion")){
               and.add(new BasicDBObject("lastversion", false));
           } 
           else if(listType.equalsIgnoreCase("listExpiredDeletedObject")){
               and.add(new BasicDBObject("lastversion", true));
               and.add(new BasicDBObject("deleteMarker",  "mark"));
           } 
           else{
               return null;
           }
           
           if (bBucketListParameterPrefix){    
               prefixStr = prefix.replaceAll("\\%",  "\\\\/")
                   .replaceAll("\\_",  "\\\\_")
                   .replaceAll("\\(", "\\\\(")
                   .replaceAll("\\)", "\\\\)");
               and.add(new BasicDBObject("objKey", new BasicDBObject("$regex", "^" + prefixStr)));//.append("$options", "i")
           }
           
           if (bMarker && !bVersionIdMarker){
               and.add(new BasicDBObject("objKey", new BasicDBObject("$gt", marker)));//objkey
           }
           
           if (bMarker && bVersionIdMarker)
               if (bDelimiterMarker)
                   and.add(new BasicDBObject("objKey", new BasicDBObject("$gte", marker))); //objkey
               else
                   and.add(new BasicDBObject("objKey", new BasicDBObject("$gt", marker)));//objkey
           
           if (bVersionIdMarker){
               if (!versionIdMarker.equalsIgnoreCase("null")){
                    objQuery = new BasicDBObject("objKey", marker);
                    objQuery.append("versionid", new BasicDBObject("$lt", versionIdMarker));
                    and.add(objQuery);
               } 
               else{
                   and.add(new BasicDBObject("versionid", versionIdMarker));
               }
               and.add(new BasicDBObject("objKey", new BasicDBObject("$gt", marker)));
           }
           
           if (bDelForceGte){
               and.add(new BasicDBObject("objKey", new BasicDBObject("$gte", delmarker))); //objkey
               bDelForceGte = false;
           }
 
           if (and.isEmpty())
               andObjQuery = new BasicDBObject();
           else {
               if (!versionIdMarker.equalsIgnoreCase("null"))
                   andObjQuery = new BasicDBObject("$or", and.toArray());
               else
                   andObjQuery = new BasicDBObject("$and", and.toArray());  
           } 
           System.out.println( "andObjQuery >>" + andObjQuery);
           return andObjQuery;
       }
       else{
           if (listType.equalsIgnoreCase("listExpiredObject")){
               query = "SELECT * FROM `"+ bucketName +"` WHERE lastversion=true AND deleteMarker <> 'mark' AND lastModified < ?";
           } 
           else if(listType.equalsIgnoreCase("listExpiredObjectVersion")){
               query = "SELECT * FROM `"+ bucketName +"` WHERE lastversion=false AND lastModified < ? ";
           } 
           else if(listType.equalsIgnoreCase("listExpiredDeletedObject")){
               query = "SELECT * FROM `"+ bucketName +"` WHERE lastversion=true AND deleteMarker='mark' ";
           } 
           else{
               return null;
           }
           
           if (bBucketListParameterPrefix)
                query += " AND objKey LIKE ?";

           if (bMarker && !bVersionIdMarker)
                query += " AND objKey > ?";

           if (bMarker && bVersionIdMarker)
                query += " AND ( objKey > ? OR (objKey = ? AND versionid > ?)) ";

           if (bDelForceGte) {
                query += " AND objKey >= ?";
           }

           query  += " ORDER BY objKey ASC, lastModified ASC LIMIT " + (maxKeys + 1); 
           System.out.println( "query >>" + query);
           return null;
       }
    }
     
    private void makeQuery(){
        mongoQuery = makeMongoQuery();
        if (mongoQuery != null){
            logger.debug(" >>mongo query : {}", mongoQuery.toString());
            if (listType.equalsIgnoreCase("listObjectVersion")) 
                logger.error(" >>mongo query : {}", mongoQuery.toString());
            return;
        }
        
        if (listType.equalsIgnoreCase("listObject")) 
            makeQueryV1();
        else if (listType.equalsIgnoreCase("listObjectV2")) 
            makeQueryV2();
        else if (listType.equalsIgnoreCase("listObjectVersion")) 
            makeQueryWithVersion();
    }
    
    private int setObject(String objKey, Metadata mt, int offset) throws Exception{
        //S3Metadata s3Metadata = new S3Metadata();
       S3Metadata s3Metadata = S3Metadata.getS3Metadata(mt.getMeta());
        
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
        return dbm.getObjectList(bucketName, pstmt, maxKeys, 0);
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
        return dbm.getObjectList(bucketName, pstmt, maxKeys, 0);
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
        return dbm.getObjectList(bucketName, pstmt, maxKeys, 0);
    }
    
    private List<Metadata> bindAndExcute() throws SQLException{
        if (dbm instanceof MongoDataRepository){
            logger.debug(">> bucketName : {} >>mongo query : {}  maxKeys : {}", bucketName, mongoQuery.toString(), maxKeys);
            return dbm.getObjectList(bucketName, mongoQuery, maxKeys + 1, 0);
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
            e.printStackTrace();
            //System.out.println(e.getMessage());
            throw new SQLException(e);
        } catch (Exception e) {
            //System.out.println(e.getMessage());
            e.printStackTrace();
            throw new SQLException(e);
        }
    }
}
