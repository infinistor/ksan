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

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.pspace.ifs.ksan.gw.identity.ObjectListParameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author legesse
 */

enum LISTTYPE {
    FORUTILITY, LISTOBJECT, LISTOBJECTVERSION; 
}
    
public class ListObject {
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
                        + "\t <StorageClass>" + GWConstants.AWS_TIER_STANTARD + "</StorageClass>\n"
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
                        + "\t <StorageClass>" + GWConstants.AWS_TIER_STANTARD + "</StorageClass>\n"
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
    
    /*private void parseParameter() {
        
    }*/
    
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
    
}
