/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
         String serverid = "";
        //try{
        //    lock.lock();
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
            /*}finally{
                lock.unlock();
            }*/
           
        return serverid;
     }
     
     public SERVER getNextServer()throws ResourceNotFoundException, AllServiceOfflineException{
        try{
            lock.lock();
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
        } finally{
            lock.unlock();
        } 
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