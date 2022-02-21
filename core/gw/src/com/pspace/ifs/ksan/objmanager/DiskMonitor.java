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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pspace.ifs.ksan.mq.MQCallback;
import com.pspace.ifs.ksan.mq.MQReceiver;
import com.pspace.ifs.ksan.mq.MQResponse;
import com.pspace.ifs.ksan.mq.MQResponseType;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import org.json.simple.JSONArray;

//import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
    TOTALINODE("TotalInode"),
    USEDINODE("UsedInode"),
    MODE("RwMode"),
    STATUS("State"),
    START("start"),
    STOP("stop"),
    RW("ReadWrite"),
    RO("ReadOnly"),
    ADD("Add"),       /*for add disk, server, diskpool and server*/
    REMOVE("Remove"), /*to remove disk, server, diskpool and server*/
    TIMEOUT("timeout"), /* for server timout */
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

    class MQReader implements MQCallback{

        @Override
        public MQResponse call(String routingKey, String body) {
            MQResponse ret;
            JsonOutput jo;
            DISKPOOL dskPool = null;
            if (routingKey.contains(".servers.disks.size"))
                return new MQResponse(MQResponseType.SUCCESS, "", "", 0); 
            
            System.out.format("BiningKey : %s body : %s\n", routingKey, body);
            
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
                            return new MQResponse(MQResponseType.ERROR,  "-22", ex.getMessage(),  0); 
                    }
                }
            } catch (ParseException ex) {
                System.out.println("body :>" + body);
                Logger.getLogger(DiskMonitor.class.getName()).log(Level.SEVERE, null, ex);
                return new MQResponse(MQResponseType.ERROR, ex.getErrorType(), ex.getMessage(), 0);
            }
            
            if (routingKey.contains("servers.disks.")){
                if (routingKey.contains(".added"))
                    ret =addRemoveDisk(dskPool, jo);
                else if (routingKey.contains(".removed"))
                    ret =addRemoveDisk(dskPool, jo);
                else if (routingKey.contains(".state")){
                    ret =startStopDisk(dskPool, jo);
                } else if (routingKey.contains(".rwmode"))
                    ret =updateDiskMode(dskPool, jo);
                else if (routingKey.contains(".updated") || routingKey.contains(".size"))
                    ret =updateDiskSpace(dskPool, jo);
                else 
                    ret = new MQResponse(MQResponseType.WARNING, -22, "ObjManger not supported the request!", 0);
            }  
            else if (routingKey.contains("servers.diskpools.")){
                if (routingKey.contains(".added"))
                    ret =addRemoveDiskPool(KEYS.ADD.label, jo, body);
                else if (routingKey.contains(".removed"))
                    ret =addRemoveDiskPool(KEYS.REMOVE.label, jo, body);
                else if (routingKey.contains(".updated"))
                    ret= updateDiskPool(dskPool, jo,  body);
                else 
                    ret = new MQResponse(MQResponseType.WARNING, -22, "ObjManger not supported the request!", 0);
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
                    ret = new MQResponse(MQResponseType.WARNING, -22, "ObjManger not supported the request!", 0);
            }     
            else 
                ret = new MQResponse(MQResponseType.WARNING, -22, "ObjManger not supported the request!", 0);
            
            return ret; 
        }  
    }
    
    private ObjManagerCache obmCache;
    private MQReceiver mq1ton;
    private JSONParser parser;
    
    public DiskMonitor(ObjManagerCache obmCache, String mqHost, String mqQueue, String mqExchange)
            throws Exception{
        this.obmCache = obmCache;
        MQCallback callback = new MQReader();
        mq1ton = new MQReceiver(mqHost, mqQueue, mqExchange, false, "topic", "*.servers.*.*", callback);
        //mq1ton.addCallback(callback);
        parser = new JSONParser();
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
        } if (jO.containsKey(KEYS.HOSTNAME.label))
            res.hostname = (String)jO.get(KEYS.HOSTNAME.label);
        if (jO.containsKey(KEYS.IPADD.label))
            res.IPaddr = (String)jO.get(KEYS.IPADD.label);
        if (jO.containsKey(KEYS.RACK.label))
            res.rack = Integer.getInteger((String)jO.get(KEYS.RACK.label));
        
        if (jO.containsKey(KEYS.STATUS.label))
            res.status = (String)jO.get(KEYS.STATUS.label);
        
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
    
    private MQResponse startStopDisk(DISKPOOL dskPool, JsonOutput jo){
        MQResponse res;
        
        if (jo.status.equalsIgnoreCase("Good")){
            System.out.println("Start Disk>>" + jo);
            dskPool.setDiskStatus(jo.serverid, jo.id, DiskStatus.GOOD);
        }
        else if (jo.status.equalsIgnoreCase("Stop")){
            System.out.println("Stop Disk>>" + jo);
            dskPool.setDiskStatus(jo.serverid, jo.id, DiskStatus.STOPPED);
        }
        res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
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
        res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
        obmCache.displayDiskPoolList();
        
        return res;
    }
    
    private MQResponse updateDiskSpace(DISKPOOL dskPool, JsonOutput jo){
        MQResponse res;
        DISK dsk;
  
        try {
            //System.out.println("objetc>>" + jo);
            dsk = dskPool.getDisk("", jo.diskid);
            dsk.setSpace(jo.totalSpace, jo.usedSpace, jo.reservedSpace);
            dsk.setInode(jo.totalInode, jo.usedInode);
            res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
            //obmCache.displayDiskPoolList();
        } catch (ResourceNotFoundException ex) {
            Logger.getLogger(DiskMonitor.class.getName()).log(Level.SEVERE, null, ex);
            res = new MQResponse(MQResponseType.ERROR, "disk not exist", "", 0);
        }
        
        return res;
    }
    
    private MQResponse addRemoveDisk(DISKPOOL dskPool, JsonOutput jo){
        MQResponse res;
       try {
            if (jo.action.equalsIgnoreCase(KEYS.ADD.label)){
                    dskPool.getServerById(jo.serverid)
                            .addDisk(jo.mpath, jo.diskid, 0, DiskStatus.STOPPED);
            }
            else if (jo.action.equalsIgnoreCase(KEYS.REMOVE.label)){
                dskPool.getServerById(jo.serverid)
                        .removeDisk(jo.mpath, jo.diskid);
            }
       } catch (ResourceNotFoundException ex) {
           Logger.getLogger(DiskMonitor.class.getName()).log(Level.SEVERE, null, ex);
       }
        res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
        obmCache.displayDiskPoolList();
        
        return res;
    }
    
    /*private MQResponse updateDiskSpace(DISKPOOL dskPool, JsonOutput jo) {
       MQResponse res;
       try{
           dskPool.getServerById(jo.serverid)
                  .setDiskSpace(jo.diskid, jo.fr, jo.freeInode);
           res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
       } catch(ResourceNotFoundException ex){
           System.out.println(ex);
           res = new MQResponse(MQResponseType.ERROR, -1, ex.getMessage(), 0);
       }
       return res;
    }*/
    
    private MQResponse addRemoveServer(DISKPOOL dskPool, JsonOutput jo){
        MQResponse res;
        
        if (jo.action.equalsIgnoreCase(KEYS.ADD.label)){
            dskPool.addServer(jo.serverid, jo.IPaddr, jo.hostname, jo.rack);
        }
        else if (jo.action.equalsIgnoreCase(KEYS.REMOVE.label)){
            dskPool.removeServer(jo.serverid);
        }
        res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
        
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
           res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
        } catch( ResourceNotFoundException ex){
           System.out.println(ex);
           res = new MQResponse(MQResponseType.ERROR, -1, ex.getMessage(), 0);
        }
        return res;
    }
    
    private MQResponse addRemoveDiskPool(String action, JsonOutput jo, String msg){
        MQResponse res;
    
        //System.out.println("action >>" + action);
        if (action.equalsIgnoreCase(KEYS.ADD.label)){
            DISKPOOL dskPool1 = new DISKPOOL(jo.id, jo.diskPoolName);
            /*JSONObject jsonObject;
            JSONObject jsonDskObject;
            JSONArray jsonDisk;
            try {
                jsonObject = (JSONObject)parser.parse(msg);
                jsonDisk = (JSONArray)jsonObject.get("Disks");
                for(int idx=0; idx < jsonDisk.size(); idx++){
                    jsonDskObject = (JSONObject)jsonDisk.get(idx);
                    SERVER srv = new SERVER();
                }
            } catch (ParseException ex) {
                Logger.getLogger(DiskMonitor.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            
            this.obmCache.setDiskPoolInCache(dskPool1);
            this.obmCache.displayDiskPoolList();
        }
        else if (action.equalsIgnoreCase(KEYS.REMOVE.label)){
            if (!(jo.id.isEmpty() && jo.diskPoolName.isEmpty()))
                this.obmCache.removeDiskPoolFromCache(jo.id);
            this.obmCache.displayDiskPoolList();
        }
        res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
        
        return res;
    }
    
    private MQResponse updateDiskPool(DISKPOOL dskPool, JsonOutput jo, String msg){
        MQResponse res;
        JSONObject dsk;
        //DISKPOOL dskPool1 = obmCache.getDiskPoolFromCache(jo.id);
        JSONArray dskArray = decodeJsonArray(msg, "Disks");
        Iterator it = dskArray.iterator();
        while(it.hasNext()){
            dsk = (JSONObject)it.next();
            String diskId =(String)dsk.get("Id");
            String serverId = (String)dsk.get("ServerId");
            String dskPoolId = (String)dsk.get("DiskPoolId");
            String mpath = (String)dsk.get("Path");
            //dskPool1.
            System.out.format("DISK to add: { diskid : %s serverId : %s dskPoolId : %s mpath : %s}", 
                    diskId, serverId, dskPoolId, mpath);
        }
        
        res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
        return res;
    }
    
    private MQResponse volumeMGNT(DISKPOOL dskPool, JsonOutput jo, String msg){
        MQResponse res;
        System.out.println("[volumeMNT : 345] " + msg);
        res = new MQResponse(MQResponseType.SUCCESS, "", "", 0);
        return res;
    }
}
