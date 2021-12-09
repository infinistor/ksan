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

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.pspace.ifs.ksan.objmanager.ObjManagerException.AllServiceOfflineException;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

/**
 *
 * @author legesse
 */
public class DISKPOOL {
    private String id;
    private String name;
    private boolean isLocal;
    private int currentServerIdx;
    private HashMap<String, SERVER> serverMap;
    private static Logger logger;
    
    public DISKPOOL(String id, String name){
        this.id = id;
        this.name = name;
        this.isLocal = false;
        serverMap = new HashMap<>();
        currentServerIdx = 0;
        logger = LoggerFactory.getLogger(DISKPOOL.class);
    }
    
    public DISKPOOL(){
        this.id = "";
        this.name = "";
        this.isLocal = false;
        serverMap = new HashMap<>();
        currentServerIdx = 0;
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
    
    public String getId(){
        return this.id;
    }
    
    public String getName(){
        return this.name;
    }
    
    public SERVER getLocalServer() throws NoSuchElementException{
         SERVER srv;
         
         for (String serverid : serverMap.keySet()){
             srv = serverMap.get(serverid);
             if (srv.isLocalServer())
                 return srv;
         }
         logger.error("Local server is not exist in the system!");
         throw new NoSuchElementException("Local server is not exist in the system!");
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
        Set<String> entry =  serverMap.keySet();
        List<String> keys = new ArrayList<>(entry);
        String serverid = "";
        
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
           
        return serverid;
     }
     
     public SERVER getNextServer()throws ResourceNotFoundException, AllServiceOfflineException{
         SERVER srv;
         int startIndex;
         boolean retried = false;
        
         if (serverMap.isEmpty()){
             logger.error("There is no server in the system!");
             throw new ResourceNotFoundException("There is no server in the system!"); 
         }
         
         startIndex = currentServerIdx;
         while((srv = serverMap.get(getNextServerId())) != null){
              if (srv == null && retried == false){ // to rotate one more time
                  retried = true; 
                  continue;
              }
              
              if (startIndex == currentServerIdx) {
                  if (serverMap.size() > 1){
                    logger.error("All OSD server are offline!");
                    throw new AllServiceOfflineException("All OSD server are offline!");
                  }
                  else{
                    logger.error("There is no enough OSD server for all replica!");
                    throw new ResourceNotFoundException("There is no enough OSD server for all replica!"); 
                  }  
              }
              
              if (srv.getStatus() != ServerStatus.ONLINE){
                  continue;
              }
              return srv;
         }
         logger.error("There is no server in the system!");
         throw new ResourceNotFoundException("There is no server in the system!"); 
    }
    
    public boolean diskExistInPool(String diskid, String path){
        SERVER srv;
        
        if (path.isEmpty() && diskid.isEmpty()){
            return false;
        }
        
        for(String serverId : serverMap.keySet())
        {
            srv = serverMap.get(serverId);
            if (!path.isEmpty()){
                if (srv.diskExistWithPath(path)){
                    return true;
                }
            }
            if ( !diskid.isEmpty() ){
                if (srv.diskExistWithId(diskid))
                    return true;
            }
        }
        return false;
    }
    
    public DISK getDisk( String dpath, String diskid) throws ResourceNotFoundException{
        SERVER srv;
        DISK dsk;
        
        /*if (!(dpath == null && diskid == null)){
            
        }*/
        logger.debug("-- getDisk : path {}, disk id {}", dpath, diskid);
        for(String serverId : serverMap.keySet()){
            srv = serverMap.get(serverId);
            if (srv == null)
                continue;
            
            if (!dpath.isEmpty()){
                if (srv.diskExistWithPath(dpath)){
                    dsk = srv.getDiskByPath(dpath);
                    dsk.setOSDIP(srv.getName());
                    return dsk;
                }
            }
            else if (!diskid.isEmpty()){
                if (srv.diskExistWithId(diskid)){
                    dsk = srv.getDiskById(diskid);
                    dsk.setOSDIP(srv.getName());
                    return dsk;
                }
            }
             
        }
        logger.error("There is no disk in the the server with path : {} or diskid : {}!", dpath, diskid);
        throw new ResourceNotFoundException("There is no disk in the the server with path : " + dpath +" or diskid : "+ diskid +"!"); 
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
    
    public void displayServerList(){
        SERVER srv;
 
        for(String serverid : serverMap.keySet()){
            srv = serverMap.get(serverid);
            logger.debug("\t<SERVER id=\"{}\" ip=\"{}\" status=\"{}\"  numDisk=\"{}\">", srv.getId() , srv.getName(), srv.getStatus(), srv.getNumDisk());
            // System.out.format("\t<SERVER id=\"%s\" ip=\"%s\" status=\"%s\"  numDisk=\"%s\">\n", srv.getId()
            //         , srv.getName(), srv.getStatus(), srv.getNumDisk());
            srv.displayDiksList();
            logger.debug("\t</SERVER>");
            // System.out.println("\t</SERVER>");
         }
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
        return String.format("{ id : %s name : %s num_server : %d }", 
                id, name, serverMap.size());
    }
}