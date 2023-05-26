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

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author legesse
 */
public class DISKPOOL {
    private String id;
    private String name;
    private boolean isLocal;
    private int defualt_replica_count;
    private int currentServerIdx;
    private HashMap<String, SERVER> serverMap;
    private static Logger logger;
    private static final ReentrantLock lock = new ReentrantLock();
    
    public DISKPOOL(String id, String name){
        this.id = id;
        this.name = name;
        this.isLocal = false;
        serverMap = new HashMap<>();
        currentServerIdx = 0;
        defualt_replica_count = 1;
        logger = LoggerFactory.getLogger(DISKPOOL.class);
    }
    
    public DISKPOOL(){
        this.id = "";
        this.name = "";
        this.isLocal = false;
        serverMap = new HashMap<>();
        currentServerIdx = 0;
        defualt_replica_count = 0;
    }
    
    public void setId(String id){
        this.id = id;
    }
    
    public void setName(String name){
        this.name = name;
    }
    
    public void addServer(SERVER s){
        serverMap.putIfAbsent(s.getId(), s);
    }
    
    public void setDefaultReplicaCount(int rcount){
        defualt_replica_count = rcount;
    }
    
    public String getId(){
        return this.id;
    }
    
    public String getName(){
        return this.name;
    }
    
    public int getDefaultReplicaCount(){
        return defualt_replica_count;
    }
    
    public SERVER getLocalServer() throws ResourceNotFoundException{
         SERVER srv;
         
         for (String serverid : serverMap.keySet()){
             srv = serverMap.get(serverid);
             if (srv.isLocalServer())
                 return srv;
         }
         logger.error("[getLocalServer] Local server is not exist in the system!");
         throw new ResourceNotFoundException("[getLocalServer]Local server is not exist in the system!");
     }
      
     public SERVER getServerById(String serverid) throws ResourceNotFoundException{
         SERVER srv = serverMap.get(serverid);
         if (srv == null) {
             logger.error("There is no server in the the pool with id : {}!", serverid);
            throw new ResourceNotFoundException("There is no server in the the pool with id : " + serverid +"!");
         }
         return srv;
     }
     
     private String getNextServerId(){
         String serverid = "";
        try{
            lock.lock();
            Set<String> entry =  serverMap.keySet();
            List<String> keys = new ArrayList<>(entry);
            
            if (keys.isEmpty())
                return ""; 

            if (currentServerIdx >= keys.size())
                currentServerIdx = 0;

            try
            {
               serverid = keys.get(currentServerIdx);
               currentServerIdx++;
            }catch (IndexOutOfBoundsException e){
                logger.debug(" >>currentServerIdx: {} keySize {}", currentServerIdx, keys.size());
                currentServerIdx = 0;
            }
        }finally{
            lock.unlock();
        }
        
        return serverid;
     }
     
     public SERVER getNextServer()throws ResourceNotFoundException, AllServiceOfflineException{
        try{
            lock.lock();
            SERVER srv;
            int startIndex;
            int svrCounter = 0;
            boolean retried = false;

            if (serverMap.isEmpty()){
                logger.error("There is no server in the system!");
                throw new ResourceNotFoundException("There is no server in the system!"); 
            }

            startIndex = currentServerIdx;
            while((srv = serverMap.get(getNextServerId())) != null){
                if (svrCounter > serverMap.size()){
                    break;
                } 
                if (srv == null && retried == false){ // to rotate one more time
                    retried = true; 
                    svrCounter++;
                    continue;
                }

                if (startIndex == currentServerIdx) {
                    if (srv.getStatus() == ServerStatus.ONLINE && srv.possibleToAlloc() == true){ // to check last entry
                        return srv;
                    }
                    
                    if (serverMap.size() > 1){
                      logger.error("All OSD server are offline!");
                      throw new AllServiceOfflineException("All OSD server are offline!");
                    }
                    else if ( svrCounter > 0){
                      logger.error("There is no enough OSD server for all replica!");
                      throw new ResourceNotFoundException("There is no enough OSD server for all replica!"); 
                    }  
                }
                 
                logger.debug("ServerId : {} OSDIP : {} currentServerIdx : {} startIndex {} ", srv.getId(), srv.getName(), currentServerIdx, startIndex);
                if (srv.getStatus() != ServerStatus.ONLINE){
                    svrCounter++;
                    continue;
                }

                if (srv.possibleToAlloc() == false){ // to check disk avaliablity
                   svrCounter++;
                   continue; 
                }
                return srv;
            }
            logger.error("There is no server in the system!");
            throw new ResourceNotFoundException("There is no server in the system!"); 
        } finally{
            lock.unlock();
        } 
    }
    
    public boolean diskExistInPool(String diskid){
        SERVER srv;
        
        if (diskid.isEmpty()){
            return false;
        }
        
        for(String serverId : serverMap.keySet())
        {
            srv = serverMap.get(serverId);
            if ( !diskid.isEmpty() ){
                if (srv.diskExistWithId(diskid))
                    return true;
            }
        }
        return false;
    }
    
    public DISK getDisk( String diskid) throws ResourceNotFoundException{
        SERVER srv;
        DISK dsk;
        
        for(String serverId : serverMap.keySet()){
            srv = serverMap.get(serverId);
            if (srv == null)
                continue;
            
            if (!diskid.isEmpty()){
                if (srv.diskExistWithId(diskid)){
                    dsk = srv.getDiskById(diskid);
                    dsk.setOSDIP(srv.getName());
                    dsk.setOSDServerId(serverId);
                    return dsk;
                }
            }
             
        }
        logger.error("There is no disk in the the server with  diskid : {}!",  diskid);
        throw new ResourceNotFoundException("There is no disk in the the server with  diskid : "+ diskid +"!"); 
    }
    
    public SERVER getServer( String dpath, String diskid) throws ResourceNotFoundException{
        SERVER srv;
        logger.debug("-- getServer : path {}, disk id {}", dpath, diskid);
        logger.debug("server size : {}", serverMap.size());
        for(String serverId : serverMap.keySet()){
            srv = serverMap.get(serverId); 
            if (!dpath.isEmpty()){
                if (srv.diskExistWithPath(dpath) == true)
                    return srv;
            } else if (!diskid.isEmpty()){
                if (srv.diskExistWithId(diskid))
                    return srv;
            }
        }
        logger.error("There is no server in the the diskpool with path : {} or diskid : {}!", dpath, diskid);
        throw new ResourceNotFoundException("There is no server in the the diskpool with path : " + dpath +" or diskid : "+ diskid +"!"); 
    }
    
    public void setDiskStatus( String serverid, String diskid, DiskStatus st){
        SERVER srv;
       
        srv = serverMap.get(serverid);
        if (srv != null)
            srv.setDiskStatusById(diskid, st);
        
    }
    
    public void setDiskMode( String serverid, String diskid, DiskMode mode){
        SERVER srv;
        
        srv = serverMap.get(serverid);
        if ( srv != null)
           srv.setDiskModeById(diskid, mode);
    }
    
    public void addServer(String serverid, String ipaddr, String hostname, int rack){
        SERVER s = new SERVER(serverid, 0, hostname);
        s.setIpAddress(s.ipaddrToLong(ipaddr));
        s.setRack(rack);
        this.addServer(s);
    }
    
    public void removeServer(String serverid){
        serverMap.remove(serverid); 
    }
    
    
    public int getNumServers(){
        return serverMap.size();
    }
    
    public String displayServerList(){
        SERVER srv;
        String xmlData = "";
 
        for(String serverid : serverMap.keySet()){
            srv = serverMap.get(serverid);
            String eachSvr = String.format("\n\t<SERVER id=\"%s\" ip=\"%s\" status=\"%s\"  numDisk=\"%d\">", srv.getId() , srv.getName(), srv.getStatusInString(), srv.getNumDisk());
            // System.out.format("\t<SERVER id=\"%s\" ip=\"%s\" status=\"%s\"  numDisk=\"%s\">\n", srv.getId()
            //         , srv.getName(), srv.getStatus(), srv.getNumDisk());
            eachSvr = eachSvr + srv.displayDiksList() + "\n\t</SERVER>";
            //logger.debug("\t</SERVER>");
            // System.out.println("\t</SERVER>");
            xmlData= xmlData + eachSvr;
         }
        return xmlData;
    }
    
    public boolean isDiskPoolWithLocalServer(){
        return this.isLocal;
    }
    
    @Override
    public boolean equals(Object o){
        if (o == this)
            return true;
        
        if (!(o instanceof SERVER))
            return false;
        
        DISKPOOL s = (DISKPOOL)o; 
        
        return (this.id.compareTo(s.id) == 0 && this.name.compareTo(s.name) == 0);
    }
    
    @Override
    public String toString(){    
        return String.format("{ id : %s name : %s num_server : %d  default_replicaCount: %d}", 
                id, name, serverMap.size(), this.defualt_replica_count);
    }
}
