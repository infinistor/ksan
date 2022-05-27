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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;
import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  Store diskpool and list of bucket in memory
 * @author legesse
 */
public class ObjManagerCache {
    private final HashMap<String, DISKPOOL> diskPoolMap;
    private final HashMap<String, Bucket> bucketMap;
    private DataRepository dbm;
    private static Logger logger;
    
    public ObjManagerCache(){
        diskPoolMap = new HashMap<>();
        bucketMap   = new HashMap<>();
        logger = LoggerFactory.getLogger(ObjManager.class);
        dbm = null;
    }
    
    // the db manager will be used to reload data incase not exist in memory
    public void setDBManager(DataRepository dbm){
        this.dbm = dbm;
    }
    
    public void setDiskPoolInCache(DISKPOOL dskPool){
        if (dskPool != null)
            diskPoolMap.putIfAbsent(dskPool.getId(), dskPool);
    }
    
    public void removeDiskPoolFromCache(String diskPoolId){
        diskPoolMap.remove(diskPoolId);
    }
    
    private DISKPOOL getDiskPool(String diskPoolId){
        DISKPOOL dskPool;
        dskPool = diskPoolMap.get(diskPoolId);
        if (dskPool == null) {
            this.loadDiskPools();
            dskPool = diskPoolMap.get(diskPoolId);
        }
        
        return dskPool;
    }
    
    public DISKPOOL getDiskPoolFromCache(String diskPoolId) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        dskPool = getDiskPool(diskPoolId);
        if (dskPool == null) {
            //here is no diskpool with id : " + diskPoolId +"!");
            throw new ResourceNotFoundException("There is no diskpool wih id : " + diskPoolId + " !");
        }
        return dskPool;
    }
    
    public DISKPOOL getDiskPoolFromCacheWithServerId(String serverId) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        
        for(String diskPoolId : diskPoolMap.keySet()){
            dskPool = getDiskPool(diskPoolId);
            try {
                dskPool.getServerById(serverId);
                return dskPool;
            } catch (ResourceNotFoundException ex) {
                // check the next diskPool   
            }
        }
        logger.error("There is no server in the disk pool with that serverid : {}!", serverId);
       throw new ResourceNotFoundException("There is no server in the disk pool with that serverid : " + serverId +"!"); 
    }
    
    public void setBucketInCache(Bucket bt){
        bucketMap.putIfAbsent(bt.getName(), bt);
    }
    
    public void updateBucketInCache(Bucket bt){
        bucketMap.replace(bt.getName(), bt);
    }
    
    public void removeBucketFromCache(String bucketName){
        bucketMap.remove(bucketName);
    }
    
    public Bucket getBucketFromCache(String bucketName){
        Bucket bt = bucketMap.get(bucketName);
        if (bt == null){
            try {
                bt = dbm.selectBucket(bucketName);
                if (bt != null)
                    setBucketInCache(bt);
            } catch (SQLException | ResourceNotFoundException ex) {
                return null;
            }
        }
        return bt;
    }
   
    public String[] getBucketNameList(){
    
        return bucketMap.keySet().toArray(new String[0]);
    }
    
    public List<S3BucketSimpleInfo> getBucketSimpleList() {
        List<S3BucketSimpleInfo> btList = new ArrayList<S3BucketSimpleInfo>();

        for (String key : bucketMap.keySet()) {
            Bucket bt = bucketMap.get(key);
            S3BucketSimpleInfo bsi = new S3BucketSimpleInfo();
            bsi.setBucketName(bt.getName());
            bsi.setCreateDate(bt.getCreateTime());

            btList.add(bsi);
        }

        return btList;
    }

    public boolean bucketExist(String bucketName){
        return bucketMap.containsKey(bucketName);
    }
    
    public DISK getDiskWithPath( String dskPoolId, String dpath) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        DISK dsk;
        
        dskPool = getDiskPool(dskPoolId);
        if (dskPool != null){
            dsk = dskPool.getDisk(dpath, "");
            return dsk;
        }
        logger.error("There is no disk in the the server with path : {} and disk pool id : {}!", dpath, dskPoolId);
        throw new ResourceNotFoundException("There is no disk in the the server with path : " + 
                dpath +" and disk pool id : " + dskPoolId + "!"); 
    }
    
    public DISK getDiskWithId( String dskPoolId, String diskid) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        DISK dsk;
        
        dskPool = getDiskPool(dskPoolId);
        if (dskPool != null){
            dsk = dskPool.getDisk("", diskid);
            return dsk;
        }
        logger.error("There is no disk in the the server with diskid : {} at disk pool id : {}!", diskid, dskPoolId);
        throw new ResourceNotFoundException("There is no disk in the the server with diskid : " + 
                diskid +" at diskpool id : "+ dskPoolId +"!"); 
    }
    
    public boolean validateDisk(String dskPoolId, String diskid, String dskPath){
         DISKPOOL dskPool;
         
         dskPool = getDiskPool(dskPoolId);
         if (dskPool == null)
             return false;
         
         return dskPool.diskExistInPool(diskid, dskPath);
    }
    
    public boolean isDiskSeparatedAndValid(String dskPoolId, Metadata mt){
        DISKPOOL dskPool;
         
        dskPool = getDiskPool(dskPoolId);
        if (dskPool == null)
            return false; // diskpool not exist
        
        if (!dskPool.diskExistInPool(mt.getPrimaryDisk().getId(), ""))
            return false; // primary disk not exist
        
        if (!mt.isReplicaExist())
           return true;
                
        
        
        try {
            if (!dskPool.diskExistInPool(mt.getReplicaDisk().getId(), ""))
                return false; // replica disk not exist
            
            if ((mt.getPrimaryDisk().getOsdIp()).equals(mt.getReplicaDisk().getOsdIp()))
                return false;
        } catch (ResourceNotFoundException ex) {
            return true;
        }
       
        return true;
    }
    
    public SERVER getServerWithDiskPath( String dskPoolId, String dpath) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        
        dskPool = getDiskPool(dskPoolId);
        if (dskPool != null){
            return dskPool.getServer(dpath, "");
        }
        logger.error("There is no server in the the pool with disk path : {} and disk pool id : {}!", dpath, dskPoolId);
        throw new ResourceNotFoundException("There is no server in the the pool with disk path : " + 
                dpath +" and disk pool id : " + dskPoolId + "!"); 
    }
    
    public void displayBucketList(){
        /*Bucket bt;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("<BUCKETLIST>");
        for(String bucketName : bucketMap.keySet()){
            bt = bucketMap.get(bucketName);
            String formatTime = formatter.format(bt.getCreateTime());
            System.out.format("   <BUCKET id=\"%s\"  name=\"%s\" diskPoolId=\"%s\" Versioning=\"%s\" createTime=\"%s\" >\n", 
                    bt.getId(), bt.getName(), bt.getDiskPoolId(), bt.getVersioning(), formatTime);
        }
        System.out.println("</BUCKETLIST>");*/
    }
    
    public void displayDiskPoolList(){
        /*DISKPOOL dskPool;
        
        System.out.println("<DISKPOOLLIST>");
        for(String dskPoolId : diskPoolMap.keySet()){
             dskPool = diskPoolMap.get(dskPoolId);
             System.out.format("   <DISKPOOL id=\"%s\"  name=\"%s\" numServer=\"%s\" >\n"
                     , dskPool.getId(), dskPool.getName(), dskPool.getNumServers());
             dskPool.displayServerList();
             System.out.println("   </DISKPOOL>");
         }
         System.out.println("</DISKPOOLLIST>");*/
    }

    public void resetBucketList() {
        bucketMap.clear();
    }
    
    public void loadDiskPools(){
        try{ 
            File fXmlFile = new File("/usr/local/ksan/etc/diskpools.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("DISKPOOL");
            for (int idx = 0; idx < nList.getLength(); idx++) {
                DISKPOOL dp;
                //SERVER s;
                Node dpoolNode = nList.item(idx);
                dp = new DISKPOOL(((Element)dpoolNode).getAttribute("id"), ((Element)dpoolNode).getAttribute("name"));
                logger.debug("disk pool : id {}, name {}", dp.getId(), dp.getName());
                NodeList serverNodeList = ((Element)dpoolNode).getElementsByTagName("SERVER");
                int sidx = 0;
                SERVER s[] = new SERVER[serverNodeList.getLength()];
                while (sidx < serverNodeList.getLength()){
                    
                    Element elemS = (Element)((Element)serverNodeList.item(sidx));
                    s[sidx] = new SERVER(elemS.getAttribute("id"), 0, elemS.getAttribute("ip"));

                    NodeList diskNodeList = elemS.getElementsByTagName("DISK");
                    int didx = 0;
                    while(didx < diskNodeList.getLength()){
                        Element elemD = ((Element)diskNodeList.item(didx));
                        s[sidx].addDisk(elemD.getAttribute("path"), elemD.getAttribute("id"), 0, DiskStatus.GOOD);
                        //System.out.format("Disk id : %s path : %s status : %s\n",  elemD.getAttribute("id"), elemD.getAttribute("path"), elemD.getAttribute("status"));
                        logger.debug("disk id : {}, path : {}", elemD.getAttribute("id"), elemD.getAttribute("path"));
                        didx++; 
                    }
                    dp.addServer(s[sidx]);
                    sidx++;
                }
               this.setDiskPoolInCache(dp);
            }
        }catch (Exception e){
            System.out.println("Error loading diskpool-->" + e);
        }
    }
}
