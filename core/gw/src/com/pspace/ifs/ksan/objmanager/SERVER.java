/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pspace.ifs.KSAN.ObjManger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

import com.pspace.ifs.KSAN.ObjManger.ObjManagerException.ResourceNotFoundException;

/**
 *
 * @author legesse
 */
public class SERVER {
    private String id;
    private long ipaddr;
    private String name;
    private ServerStatus status; // ONLINE or OFFLINE
    private int rack;
    private boolean isLocal;
    private int currentDiskIdx;
    private HashMap<String, DISK> diskMap;
    private static Logger logger;
    
    public SERVER(String id, long ip, String name){
        this.id = id;
        this.ipaddr = ip; // IP address in decimal
        this.name = name; //IP address in string
        this.status = ServerStatus.ONLINE;
        currentDiskIdx = 0;
        diskMap = new HashMap();
        this.isLocal=this.isLocalIpaddres();
        logger = LoggerFactory.getLogger(SERVER.class);
    }
    
    private boolean isLocalIpaddres(){
        InetAddress ip;
        try{
            if (!this.name.isEmpty())
                ip = InetAddress.getByName(this.name);
            else{
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.putLong(this.ipaddr);
                ip = InetAddress.getByAddress(buffer.array());
            }
           if (ip.isAnyLocalAddress() || ip.isLoopbackAddress())
               return true;
           
           return NetworkInterface.getByInetAddress(ip) != null;
        } catch(UnknownHostException e){
            logger.error(e.getMessage());
            e.printStackTrace();
            return false;
        } catch(SocketException e){
            logger.error(e.getMessage());
            return false;
        }
    }
    
    public long ipaddrToLong(String ipaddr){
        long res = 0;
        long ip;
        String[] ipAddressInArray = ipaddr.split("\\.");
        
        for (int i = 3; i >= 0; i--) {
           ip = Long.parseLong(ipAddressInArray[3 - i]); 
           res |= ip << (i * 8);
        }
        return res;
    }
    
    public String ipaddrToString(long ip) {
        StringBuilder result = new StringBuilder(15);
        for (int i = 0; i < 4; i++) {
            result.insert(0,Long.toString(ip & 0xff));
            if (i < 3) {
                result.insert(0,'.');
            }

            ip = ip >> 8;
        }
        return result.toString();
    }
    
    public String getId(){
        return this.id;
    }
    
    public long getIpAddress(){
        return this.ipaddr;
    }
    
    public String getName(){
        return this.name;
    }
    
    public int getRack(){
        return this.rack;
    }
    
    public boolean diskExistWithId(String diskid){
        return diskMap.containsKey(diskid);
    }
    
    public boolean diskExistWithPath(String diskPath){
        DISK dsk;
        for(String diskid : diskMap.keySet()){
            dsk = diskMap.get(diskid);
            if (dsk != null){
                if (dsk.getPath().equals(diskPath))
                    return true;
            }
        }
        return false;
    }
    
    public DISK getDiskById(String diskid) throws ResourceNotFoundException{
        logger.debug("--getDiskById {} : diskMap size {}", diskid, diskMap.size());
        DISK dsk = diskMap.get(diskid);
        if (dsk == null) {
            logger.error("There is no disk in the the server with id : {}!", diskid);
            throw new ResourceNotFoundException("There is no disk in the the server with id : " + diskid +"!"); 
        }
        return dsk;
    }
    
    public DISK getDiskByPath(String path) throws ResourceNotFoundException{
        DISK dsk;
        logger.debug("--getDiskByPath {} : diskMap size {}", path, diskMap.size());
        for(String diskid : diskMap.keySet()){
            dsk = diskMap.get(diskid);
            if (dsk != null){
                if (dsk.getPath().equals(path))
                    return dsk;
            }
            
        }
        logger.error("There is no disk in the the server with path : {}!", path);
        throw new ResourceNotFoundException("There is no disk in the the server with path : " + path +"!"); 
    }
    
    public void addDisk(String path, String diskid, int role, DiskStatus status){
        DISK dsk = new DISK();
        dsk.setPath(path);
        dsk.setId(diskid);
        dsk.setRole(role);
        dsk.setStatus(status);
        dsk.setSpace(0.0, 0.0, 0.0);
        diskMap.putIfAbsent(diskid, dsk);
    }
    
    public void removeDisk(String path, String diskid) throws ResourceNotFoundException{
        DISK dsk;
        
        if (!path.isEmpty() && diskid.isEmpty()){
            dsk = getDiskByPath(path);
            diskMap.remove(dsk.getId());
        } else
            diskMap.remove(diskid);
    }
    
    /*public void setDiskSpace(String diskid, long freeSpace, long freeInode) throws ResourceNotFoundException{
        this.getDiskById(diskid).setFreeSpace(freeSpace);
        this.getDiskById(diskid).setFreeInode(freeInode);
    }*/
    
    public void setName(String name){
        this.name = name;
    }
    
    public void setRack(int rack){
        this.rack = rack;
    }
    
    public void setId(String id){
        this.id = id;
    }
    
    public void setIpAddress(long ip){
        this.ipaddr = ip;
    }
    
    public void setDiskStatusById(String diskid, DiskStatus st) {
        DISK dsk;
        
        dsk = diskMap.get(diskid);
        if (dsk != null)
            dsk.setStatus(st);
    }
    
    public void setDiskModeById(String diskid, DiskMode mode) {
        DISK dsk;
        
        dsk = diskMap.get(diskid);
        if (dsk != null)
            dsk.setMode(mode);
    }
    
    public void setStatus(ServerStatus status){
        this.status = status;
    }
    
    public boolean isLocalServer(){
        return this.isLocal;
    }
    
    private String getNextDiskId(){
        Set<String> entry =  diskMap.keySet();
        String []keys = entry.toArray( new String[entry.size()]);
          
        if (currentDiskIdx >= diskMap.size())
            currentDiskIdx = 0;

        String diskid = keys[currentDiskIdx];
        currentDiskIdx++;
        return diskid;
    }
    
    public DISK getNextDisk()  throws ResourceNotFoundException{
        DISK dsk;
        int startIndex;
        
        if (diskMap.isEmpty()){
            logger.error("There is no disk in the server!");
            throw new ResourceNotFoundException("There is no disk in the server!"); 
        }
        logger.debug("diskMap size : {}", diskMap.size());
        startIndex = currentDiskIdx;
        while((dsk = diskMap.get(getNextDiskId())) != null){
            if (dsk == null)
                continue;
            
            // if (startIndex == currentDiskIdx)
            //     break;
            
            if (dsk.getStatus() != DiskStatus.GOOD){
                continue;
            }
            
            if (dsk.getMode() != DiskMode.READWRITE)
                continue;
            
            return dsk;
        }
        logger.error("There is no disk the server!"); 
        throw new ResourceNotFoundException("There is no disk the server!");
    }
    
    public ServerStatus getStatus(){
        return this.status;
    }
    
    public int getNumDisk(){
        return diskMap.size();
    }
    
    public void displayDiksList(){
        DISK dsk;
        
        for(String diskid : diskMap.keySet()){
             dsk = diskMap.get(diskid);
             logger.debug("\t   <DISK id=\"{}\"  path=\"{}\" freeSpace=\"{}\" freeInode=\"{}\" mode=\"{}\" status=\"{}\">",
               dsk.getId(), dsk.getPath(), dsk.getFreeSpace(), dsk.getFreeInode(), dsk.getMode(), dsk.getStatus());
            //  System.out.format(
            //    "\t   <DISK id=\"%s\"  path=\"%s\" freeSpace=\"%f\" freeInode=\"%f\" mode=\"%s\" status=\"%s\"> \n",
            //    dsk.getId(), dsk.getPath(), dsk.getFreeSpace(), dsk.getFreeInode(), dsk.getMode(), dsk.getStatus());
        }
    }

    @Override
    public boolean equals(Object o){
        if (o == this)
            return true;
        
        if (!(o instanceof SERVER))
            return false;
        
        SERVER s = (SERVER)o; 
        return (this.id.compareTo(s.getId()) == 0 && 
                this.ipaddr == s.getIpAddress() && 
                this.name.compareTo(s.getName()) == 0);
    }
    
    @Override
    public String toString(){    
        return String.format(
                "{id : %s ipaddr : %d hostname : %s isLocal : %s status : %d  diskCount : %d}", 
                getId(), getIpAddress(), getName(), isLocal, getStatus(), getNumDisk() );
    } 
}
