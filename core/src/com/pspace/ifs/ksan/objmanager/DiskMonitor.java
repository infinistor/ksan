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

import java.util.Iterator;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import org.json.simple.JSONArray;

//import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author legesse
 */

enum KEYS{
    ID("Id"),
    DISKPOOLNAME("Name"), //DiskPoolName
    DISKPOOLID("DiskPoolId"),
    IPADD("IPaddr"),
    HOSTNAME("Name"),
    MOUNTPATH("MountPath"),
    SERVERID("ServerId"),
    RACK("Rack"),
    DISKID("DiskNo"),
    ACTION("Mode"),
    TOTALSPACE("TotalSize"),
    USEDSPACE("UsedSize"),
    RESERVEDSPACE("ReservedSize"),
    RESERVEDINODE("ReservedInode"),
    TOTALINODE("TotalInode"),
    USEDINODE("UsedInode"),
    MODE("RwMode"),
    STATUS("State"),
    START("start"),
    STOP("stop"),
    RW("ReadWrite"),
    RO("ReadOnly"),
    MAINTENACE("Maintenance"),
    ADD("Add"),       /*for add disk, server, diskpool and server*/
    REMOVE("Remove"), /*to remove disk, server, diskpool and server*/
    TIMEOUT("timeout"), /* for server timout */
    REPLICACOUNT("ReplicationType"),
    ;
    public final String label;
    
    private KEYS(String label) {
        this.label = label;
    }
}

class JsonOutput{
    String id;
    String diskid;
    String serverid;
    String dpoolid;
    double totalSpace;
    double reservedSpace;
    double usedSpace;
    double totalInode;
    double usedInode;
    String hostname;
    String IPaddr;
    String diskPoolName;
    String mpath;
    String action;
    String mode;
    String status;
    int rack;
    int replicaCount;
    
    public JsonOutput(){
        id = "";
        diskid = "";
        serverid = "";
        dpoolid = "";
        totalSpace = 0;
        reservedSpace = 0;
        usedSpace = 0;
        totalInode = 0;
        usedInode = 0;
        mpath = "";
        action = "";
        IPaddr = "";
        hostname = "";
        diskPoolName= "";
        status = "";
        mode = "";
        rack = -1;
        replicaCount = 0;
    }
    
    @Override
    public String toString(){
        return String.format(
                "action : %s diskid : %s serverid : %s "
                        + "dpoolid: %s totalSpace : %f "
                        + "usedSpace: %f reservedSpace : %f "
                        + "totalInode : %f usedInode : %f "
                        + "mpath : %s IPaddr : %s hostname : %s "
                        + "diskpoolName : %s mode : %s rack : %d"
                        + "status : %s", 
                action, diskid, serverid, dpoolid, totalSpace, usedSpace, 
                reservedSpace, totalInode, usedInode, mpath, IPaddr, 
                hostname, diskPoolName, mode, rack, status);
    }
}

public class DiskMonitor {

    /*class MQReader implements MQCallback{

        @Override
        public MQResponse call(String routingKey, String body) {
            MQResponse res = update(routingKey, body);
        
            if (res.getResult().compareToIgnoreCase("Success") == 0) {
                try {
                    obmCache.dumpCacheInFile();
                } catch (IOException ex) {
                   logger.debug("failed to  dump diskpool cache {}", ex);
                }
            }
            return res;
        }  
    }*/
    
    private ObjManagerCache obmCache;
    //private MQReceiver mq1ton;
    private JSONParser parser;
    private static Logger logger;
    private ObjManagerConfig config;
    
    public DiskMonitor(ObjManagerCache obmCache, ObjManagerConfig config)
            throws Exception{
        logger = LoggerFactory.getLogger(DiskMonitor.class);
        this.obmCache = obmCache;
        this.config = config;
        //MQCallback callback = new MQReader();
        //mq1ton = new MQReceiver(config.mqHost, config.mqQueue, config.mqExchange, false, "topic", "*.servers.*.*", callback);
        parser = new JSONParser();
    }
    
    public MQResponse update(String routingKey, String body) {
        MQResponse ret;
        JsonOutput jo;
        DISKPOOL dskPool = null;
        
        //if (routingKey.contains(".servers.disks.size"))
         //   return new MQResponse(MQResponseType.SUCCESS, "", "", 0); 

        logger.debug("BiningKey : {} body : {}\n", routingKey, body);

        try {
            jo = decodeJsonData(body);
            System.out.println("jo value >>" + jo);
            try {
                dskPool = obmCache.getDiskPoolFromCache(jo.dpoolid);
            } catch (ResourceNotFoundException e){
                try { 
                    dskPool = obmCache.getDiskPoolFromCacheWithServerId(jo.serverid);
                } catch (ResourceNotFoundException ex) {
                     if (!routingKey.contains("servers.diskpools."))
                        return new MQResponse(MQResponseType.ERROR,  MQResponseCode.MQ_OBJECT_NOT_FOUND, ex.getMessage(),  0); 
                }
            }
        } catch (ParseException ex) {
            System.out.println("body :>" + body);
            logger.debug("parsing error " + ex);
            return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_INVALID_REQUEST, ex.getMessage(), 0);
        }

        if (routingKey.contains("servers.disks.")){
            if (routingKey.contains(".added"))
                ret =addRemoveDisk(dskPool, jo, body);
            else if (routingKey.contains(".removed"))
                ret =addRemoveDisk(dskPool, jo, body);
            else if (routingKey.contains(".state")){
                ret =startStopDisk(dskPool, jo);
            } else if (routingKey.contains(".rwmode"))
                ret =updateDiskMode(dskPool, jo);
            else if (routingKey.contains(".updated") || routingKey.contains(".size"))
                ret =updateDiskSpace(dskPool, jo);
            else 
                ret = new MQResponse(MQResponseType.WARNING, MQResponseCode.MQ_INVALID_REQUEST, "ObjManager not supported the request!", 0);
        }  
        else if (routingKey.contains("servers.diskpools.")){
            if (routingKey.contains(".added"))
                ret =addRemoveDiskPool(KEYS.ADD.label, jo, body);
            else if (routingKey.contains(".removed"))
                ret =addRemoveDiskPool(KEYS.REMOVE.label, jo, body);
            else if (routingKey.contains(".updated")){
                ret= updateDiskPool(dskPool, jo,  body);
            } else 
                ret = new MQResponse(MQResponseType.WARNING, MQResponseCode.MQ_INVALID_REQUEST, "ObjManager not supported the request!", 0);
        }
        else if (routingKey.contains("servers.volumes.")){
            ret = volumeMGNT(dskPool, jo, body);
        }
        else if(routingKey.contains("servers.")){
            if (routingKey.contains(".added"))
                ret =addRemoveServer(dskPool, jo);
            else if (routingKey.contains(".removed"))
                ret =addRemoveServer(dskPool, jo);
            else if (routingKey.contains(".state"))
                ret =updateServerStatus(dskPool, jo);

            else 
                ret = new MQResponse(MQResponseType.WARNING, MQResponseCode.MQ_INVALID_REQUEST, "ObjManager not supported the request!", 0);
        }     
        else 
            ret = new MQResponse(MQResponseType.WARNING, MQResponseCode.MQ_INVALID_REQUEST, "ObjManager not supported the request!", 0);
        
        try {
            obmCache.dumpCacheInFile(); // dump to file memory
        } catch (IOException ex) {
            logger.debug("failed to update memory in to a file {}", ex);
        }
        return ret; 
    }  
       
    private JsonOutput decodeJsonData(String msg)throws ParseException{
        JSONObject jO;
        JsonOutput res = new JsonOutput();
        
        //jO = (JSONObject)JSONValue.parse(msg);
        try{
           jO =(JSONObject)parser.parse(msg);
        } catch(NullPointerException ex){
            System.out.println("EError : ->" + ex + "msg =>" + msg);
            return res; // return empty parsing
        }
        
        if (jO.containsKey(KEYS.ID.label))
            res.id  = (String)jO.get(KEYS.ID.label);
        
        if (jO.containsKey(KEYS.ACTION.label)){
            res.action = (String)jO.get(KEYS.ACTION.label);
            //System.out.println("action >" + res.action);
        } if (jO.containsKey(KEYS.MODE.label))
            res.mode = (String)jO.get(KEYS.MODE.label);
        if (jO.containsKey(KEYS.DISKID.label))
            res.diskid   = (String)jO.get(KEYS.DISKID.label);
        if (jO.containsKey(KEYS.SERVERID.label))
            res.serverid = (String)jO.get(KEYS.SERVERID.label);
        if (jO.containsKey(KEYS.MOUNTPATH.label))
            res.mpath = (String)jO.get(KEYS.MOUNTPATH.label);
        if (jO.containsKey(KEYS.TOTALSPACE.label))
            res.totalSpace = Double.valueOf(jO.get(KEYS.TOTALSPACE.label).toString());
        if (jO.containsKey(KEYS.USEDSPACE.label))
            res.usedSpace = Double.valueOf(jO.get(KEYS.USEDSPACE.label).toString());
        if (jO.containsKey(KEYS.RESERVEDSPACE.label))
            res.reservedSpace = Double.valueOf(jO.get(KEYS.RESERVEDSPACE.label).toString());
        if (jO.containsKey(KEYS.TOTALINODE.label))
            res.totalInode = Double.valueOf(jO.get(KEYS.TOTALINODE.label).toString());
        if (jO.containsKey(KEYS.USEDINODE.label))
            res.usedInode = Double.valueOf(jO.get(KEYS.USEDINODE.label).toString());
        
        if (jO.containsKey(KEYS.DISKPOOLNAME.label))
            res.diskPoolName = (String)jO.get(KEYS.DISKPOOLNAME.label);
        
        if (jO.containsKey(KEYS.DISKPOOLID.label)){
            res.dpoolid = (String)jO.get(KEYS.DISKPOOLID.label);
            //System.out.println("dskpoolId >" + res.dpoolid);
        }
        else{
            res.dpoolid = res.id;
        }
        
        if (jO.containsKey(KEYS.HOSTNAME.label))
            res.hostname = (String)jO.get(KEYS.HOSTNAME.label);
        if (jO.containsKey(KEYS.IPADD.label))
            res.IPaddr = (String)jO.get(KEYS.IPADD.label);
        
        if (jO.containsKey(KEYS.RACK.label))
            res.rack = Integer.getInteger((String)jO.get(KEYS.RACK.label));
        
        if (jO.containsKey(KEYS.STATUS.label))
            res.status = (String)jO.get(KEYS.STATUS.label);
        
        if (jO.containsKey(KEYS.REPLICACOUNT.label)){
            String status = (String)jO.get(KEYS.REPLICACOUNT.label);
            if (status.equalsIgnoreCase("OnePlusOne"))
                res.replicaCount = 2;
            else if (status.equalsIgnoreCase("ErasureCode"))
                res.replicaCount = 2;
            else
                res.replicaCount = 1;
            logger.error("diskpoolId : {} src_replicaCount :{} replicaCount : {}", res.dpoolid, status, res.replicaCount);
        }
        
        return res;
    }
    
    private JSONArray decodeJsonArray(String msg, String tag){
        JSONObject jsonObject;
        JSONArray list;
        try {
            jsonObject =(JSONObject)parser.parse(msg);
            list = (JSONArray) jsonObject.get(tag);
            return list;
        } catch (ParseException ex) {
            return new JSONArray();
        }
    }
    
    private JsonOutput decodeSubJsonObject(String msg, String tag) throws ParseException{
        JSONObject jsonObject;
        JSONObject subObject;
        
        jsonObject =(JSONObject)parser.parse(msg);
        subObject = (JSONObject)jsonObject.get(tag);
        return decodeJsonData(subObject.toJSONString()); 
    }
    
    private MQResponse startStopDisk(DISKPOOL dskPool, JsonOutput jo){
        MQResponse res;
        
        if (jo.status.equalsIgnoreCase("Good")){
            logger.debug("Start Disk {}" , jo);
            dskPool.setDiskStatus(jo.serverid, jo.id, DiskStatus.GOOD);
        }
        else if (jo.status.equalsIgnoreCase("Stop")){
            logger.debug("Stop Disk>> {}",  jo);
            dskPool.setDiskStatus(jo.serverid, jo.id, DiskStatus.STOPPED);
        }
        res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        obmCache.displayDiskPoolList();
        
        return res;
    }
   
    private MQResponse updateDiskMode(DISKPOOL dskPool, JsonOutput jo){
        MQResponse res;
  
        if (jo.mode.equalsIgnoreCase(KEYS.RW.label)){
            dskPool.setDiskMode(jo.serverid, jo.id, DiskMode.READWRITE);
        }
        else if (jo.mode.equalsIgnoreCase(KEYS.RO.label)){
            dskPool.setDiskMode(jo.serverid, jo.id, DiskMode.READONLY);
        }
        else if (jo.mode.equalsIgnoreCase(KEYS.MAINTENACE.label))
            dskPool.setDiskMode(jo.serverid, jo.id, DiskMode.MAINTENANCE);
        
        res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        obmCache.displayDiskPoolList();
        
        return res;
    }
    
    private MQResponse updateDiskSpace(DISKPOOL dskPool, JsonOutput jo){
        MQResponse res;
        DISK dsk;
  
        try {
            //System.out.println("objetc>>" + jo);
            dsk = dskPool.getDisk(jo.diskid);
            dsk.setSpace(jo.totalSpace, jo.usedSpace, jo.reservedSpace);
            dsk.setInode(jo.totalInode, jo.usedInode);
            res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
            //obmCache.displayDiskPoolList();
        } catch (ResourceNotFoundException ex) {
            //Logger.getLogger(DiskMonitor.class.getName()).log(Level.SEVERE, null, ex);
            res = new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_DISK_NOTFOUND, "disk not exist", 0);
        }
        
        return res;
    }
    
    private MQResponse addRemoveDisk(DISKPOOL dskPool, JsonOutput jo, String body){
        MQResponse res;
        SERVER svr = null;
        try {
            if (jo.action.equalsIgnoreCase(KEYS.ADD.label)){
                    svr = dskPool.getServerById(jo.serverid);
                    svr.addDisk(jo.mpath, jo.diskid, 0, DiskStatus.STOPPED);
                    logger.debug("[addRemoveDisk] disk added {}", body);
            }
            else if (jo.action.equalsIgnoreCase(KEYS.REMOVE.label)){
                dskPool.getServerById(jo.serverid)
                        .removeDisk(jo.mpath, jo.diskid);
                logger.debug("[addRemoveDisk] disk removed {}", body);
            }
        } catch (ResourceNotFoundException ex) {
           if (jo.action.equalsIgnoreCase(KEYS.ADD.label)){
                try {
                    JsonOutput jsonSvr = this.decodeSubJsonObject(body, "Server");
                    dskPool.addServer(jsonSvr.serverid, jsonSvr.IPaddr, jsonSvr.hostname, jsonSvr.rack);
                    svr = dskPool.getServerById(jo.serverid);
                    svr.setRack(jsonSvr.rack);
                    svr.setStatus(ServerStatus.ONLINE);
                    svr.addDisk(jo.mpath, jo.diskid, 0, DiskStatus.STOPPED);
                    logger.debug("[addRemoveDisk] OSD server and disk added {}", body);
                } catch (ParseException | ResourceNotFoundException ex1) {
                   logger.debug("[addRemoveDisk] unable to add disk because the server not exist request {}", body);
                } 
           }
        }
        res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        obmCache.displayDiskPoolList();
 
        return res;
    }
    
    private int addDiskNotExist(String diskPoolId, String serverId, String mpath, String diskId) throws ResourceNotFoundException{
        DISKPOOL dskPool;
        try {
            dskPool = obmCache.getDiskPoolFromCache(diskPoolId);
            dskPool.getDisk(diskId);
            return 0;
        } catch (ResourceNotFoundException ex) {
            dskPool = obmCache.getDiskPoolFromCache(diskPoolId);
            dskPool.getServerById(serverId).addDisk(mpath, diskId, 0, DiskStatus.STOPPED);
        }
        return 0;
    }
    
    private MQResponse addRemoveServer(DISKPOOL dskPool, JsonOutput jo){
        MQResponse res;
        
        if (jo.action.equalsIgnoreCase(KEYS.ADD.label)){
            dskPool.addServer(jo.serverid, jo.IPaddr, jo.hostname, jo.rack);
        }
        else if (jo.action.equalsIgnoreCase(KEYS.REMOVE.label)){
            dskPool.removeServer(jo.serverid);
        }
        res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        
        return res;
    }
    
    private MQResponse updateServerStatus(DISKPOOL dskPool, JsonOutput jo){
        MQResponse res;
        try{ 
            if (jo.action.equalsIgnoreCase(KEYS.START.label)){
                dskPool.getServerById(jo.serverid)
                       .setStatus(ServerStatus.ONLINE);
            }
            else if (jo.action.equalsIgnoreCase(KEYS.STOP.label)){
                dskPool.getServerById(jo.serverid)
                       .setStatus(ServerStatus.OFFLINE);
            }
            else if (jo.action.equalsIgnoreCase(KEYS.TIMEOUT.label)){
                dskPool.getServerById(jo.serverid)
                       .setStatus(ServerStatus.TIMEOUT);
            }
           res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        } catch( ResourceNotFoundException ex){
           System.out.println(ex);
           res = new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_INVALID_REQUEST, ex.getMessage(), 0);
        }
        return res;
    }
    
    private MQResponse addRemoveDiskPool(String action, JsonOutput jo, String msg){
        MQResponse res;
        try{
            if (action.equalsIgnoreCase(KEYS.ADD.label)){
                DISKPOOL dskPool1 = new DISKPOOL(jo.id, jo.diskPoolName); 
                dskPool1.setDefaultReplicaCount(jo.replicaCount);
                this.obmCache.setDiskPoolInCache(dskPool1);
                this.obmCache.displayDiskPoolList();
                obmCache.dumpCacheInFile();
                logger.debug("[addDiskPool] diskpool name : {} Id : {} added", jo.diskPoolName, jo.id);
            }
            else if (action.equalsIgnoreCase(KEYS.REMOVE.label)){
                if (!(jo.id.isEmpty() && jo.diskPoolName.isEmpty()))
                    this.obmCache.removeDiskPoolFromCache(jo.id);
                this.obmCache.displayDiskPoolList();
                obmCache.dumpCacheInFile();
                logger.debug("[removeDiskPool] diskpool name : {} Id : {} removed", jo.diskPoolName, jo.id);
            }
            res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        } catch (IOException  ex) {
            logger.debug("[addRemoveDiskPool] unable add or remove diskpool  msg {}", msg);
            res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        }
        return res;
    }
    
    private void updateReplicaCount(JsonOutput jo){
        if (jo.replicaCount == 1 || jo.replicaCount == 2){
            DISKPOOL dskPool1; 
            try {
                dskPool1 = obmCache.getDiskPoolFromCache(jo.dpoolid);
                dskPool1.setDefaultReplicaCount(jo.replicaCount);
            } catch (ResourceNotFoundException ex) {
                dskPool1 = new DISKPOOL(jo.id, jo.diskPoolName);
                dskPool1.setDefaultReplicaCount(jo.replicaCount); 
            }
        }
    }
    
    private MQResponse updateDiskPool(DISKPOOL dskPool, JsonOutput jo, String msg) {
        MQResponse res;
        try {
            /*JSONObject dsk;
            JSONArray dskArray = decodeJsonArray(msg, "Disks");
            Iterator it = dskArray.iterator();
            while(it.hasNext()){
            dsk = (JSONObject)it.next();
            String diskId =(String)dsk.get("Id");
            String serverId = (String)dsk.get("ServerId");
            String dskPoolId = (String)dsk.get("DiskPoolId");
            String mpath = (String)dsk.get("Path");
            String status = (String)dsk.get("State");
            DiskStatus st;
            if (status.equalsIgnoreCase("Good"))
            st = DiskStatus.GOOD;
            else if(status.equalsIgnoreCase("stop"))
            st = DiskStatus.STOPPED;
            else if (status.equalsIgnoreCase("broken"))
            st = DiskStatus.BROKEN;
            else
            st = DiskStatus.UNKNOWN;
            
            String diskMood = (String)dsk.get("RwMode");
            DiskMode mode;
            if (diskMood.equalsIgnoreCase(KEYS.RO.label))
            mode = DiskMode.READONLY;
            else
            mode = DiskMode.READWRITE;
            
            double totalInode = Double.valueOf(dsk.get(KEYS.TOTALINODE.label).toString());
            //double reservedInode = Double.valueOf(dsk.get(KEYS.RESERVEDINODE.label).toString());
            double userInode = Double.valueOf(dsk.get(KEYS.USEDINODE.label).toString());
            double totalSize = Double.valueOf(dsk.get(KEYS.TOTALSPACE.label).toString());
            double reservedSize = Double.valueOf(dsk.get(KEYS.RESERVEDSPACE.label).toString());
            double usedSize = Double.valueOf(dsk.get(KEYS.USEDSPACE.label).toString());
            
            logger.debug("DISK to add: { diskid : {} serverId : {} dskPoolId : {} mpath : {} status : {} diskMood: {} totalInode : {} userInode {} totalSize : {} usedSize {}}",
            diskId, serverId, dskPoolId, mpath, status, diskMood, totalInode,  userInode, totalSize, usedSize);
            DISKPOOL dskPool1 = null;
            try {
            addDiskNotExist(dskPoolId, serverId, mpath, diskId);
            dskPool1 = obmCache.getDiskPoolFromCache(dskPoolId);
            dskPool1.setDiskMode(serverId, diskId, mode);
            dskPool1.setDiskStatus(serverId, diskId, st);
            dskPool1.getDisk(diskId).setInode(totalInode, userInode);
            dskPool1.getDisk(diskId).setSpace(totalSize, usedSize, reservedSize);
            
            logger.debug("DISK to update: { diskid : {} serverId : {} dskPoolId : {} mpath : {} update Applied!",
            diskId, serverId, dskPoolId, mpath);
            } catch (ResourceNotFoundException ex) { // add disk if not exist
            logger.debug(" NEW DISK to add: { diskid : {} serverId : {} dskPoolId : {} mpath : {} new disk Applied!",
            diskId, serverId, dskPoolId, mpath);
            
            SERVER svr;
            try {
            dskPool1 = obmCache.getDiskPoolFromCache(dskPoolId);
            svr = dskPool1.getServerById(serverId);
            svr.addDisk(mpath, diskId, 0, DiskStatus.GOOD);
            } catch (ResourceNotFoundException ex1) {
            try {
            svr = config.getPortalHandel().loadOSDserver(serverId, dskPoolId);
            if (svr == null){
            logger.debug("OSD identfied with serverId {} not exist in the system!", serverId);
            updateReplicaCount(jo);
            return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0); 
            }
            dskPool1 = obmCache.getDiskPoolFromCache(dskPoolId);
            dskPool1.addServer(svr);
            } catch (ResourceNotFoundException ex2) {
            logger.debug("OSD identfied with serverId {} not exist in the system!", serverId);
            updateReplicaCount(jo);
            return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
            }
            }
            logger.debug("DISK to add: diskid : {} serverId : {} dskPoolId : {} mpath : {} new disk Applied!",
            diskId, serverId, dskPoolId, mpath);
            }
            }
            updateReplicaCount(jo);*/
            if (config.getPortalHandel().removeDiskFromDiskPool(obmCache, msg, dskPool.getId()) != 0)
                 config.getPortalHandel().loadDiskPoolList(obmCache);
            updateReplicaCount(jo);
            obmCache.dumpCacheInFile();
            res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException | ParseException ex) {
            logger.debug("[updateDiskPool] unable apply diskpool update msg {}", msg);
            res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        }
        
        return res;
    }
    
    private MQResponse volumeMGNT(DISKPOOL dskPool, JsonOutput jo, String msg){
        MQResponse res;
        System.out.println("[volumeMNT : 345] " + msg);
        res = new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        return res;
    }
}
