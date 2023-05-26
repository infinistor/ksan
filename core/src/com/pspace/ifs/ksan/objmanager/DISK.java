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

import org.json.simple.JSONObject;

public class DISK{
    private String path;
    private String diskid;
    private String diskName;
    private double totalSpace;
    private double usedSpace;
    private double reservedSpace;
    private double totalInode;
    private double usedInode;
    private String osdIp;
    private String hostname;
    private String osdServerId;
    private String diskPoolId;
    private int role; // primary or replica
    private DiskMode mode;
    private DiskStatus status; // GOOD, STOPPED, BROKEN, UNKNOWN;
    
    public DISK(){
        hostname = "";
        this.path = "";
        this.diskid = "";
        diskName = "";
        this.role = 0;
        this.totalSpace = 0;
        this.usedSpace = 0;
        this.reservedSpace = 0;
        this.totalInode = 0;
        this.usedInode = 0;
        this.mode = DiskMode.READWRITE;
        this.status = DiskStatus.UNKNOWN;
    }
    
    public String getId(){
        return this.diskid;
    }
    
    public int getRole(){
        return this.role;
    }
    
    public DiskStatus getStatus(){
        return this.status;
    }
    
    public String getOsdIp(){
        return this.osdIp;
    }
    
    public String getPath(){
        return this.path;
    }
    
    public DiskMode getMode(){
        return this.mode;
    }
    
    public double getFreeSpace(){
        return (totalSpace - (usedSpace + reservedSpace));
    }
    
    public double getFreeInode(){
        return totalInode - usedInode;
    }
    
    public String getOSDServerId(){
        return osdServerId;
    }
    
    public String getDiskPoolId(){
        return diskPoolId;
    }
    
    public String getHostName(){
        return hostname;
    }
    
    public String getDiskName(){
        return diskName;
    }
    
    public void setOSDServerId(String osdServerId){
        this.osdServerId = osdServerId;
    }
    
    public void setId(String diskid){
        this.diskid = diskid;
    }
    
    public void setStatus(DiskStatus st){
        this.status = st;
    }
    
    public void setRole(int rol){
        this.role = rol;
    }
    
    public void setPath(String path){
        this.path = path;
    }
    
    public void setMode(DiskMode mode){
        this.mode = mode;
    }
    
    public void setSpace(double totalSpace, double usedSpace, double reserverdSpace){
        this.totalSpace = totalSpace;
        this.usedSpace = usedSpace;
        this.reservedSpace = reserverdSpace;
    }
    
    public void setInode(double totalInode, double usedInode){
        this.totalInode = totalInode;
        this.usedInode = usedInode;
    }
    
    public void setOSDIP(String osdIP){
        this.osdIp = osdIP;
    }
    
    public void setDiskPoolId(String diskPoolId){
        this.diskPoolId = diskPoolId;
    }
    
    public void setHostName(String hostname){
        this.hostname = hostname;
    }
    
    public void setDiskName(String diskName){
        this.diskName = diskName;
    }
    
    @Override
    public String toString(){
         JSONObject jsonStr = new JSONObject();
         jsonStr.put("hostname",getHostName());
         jsonStr.put("Path", getPath());
         jsonStr.put("OsdIP", getOsdIp());
         jsonStr.put("diskPoolId", getDiskPoolId());
         jsonStr.put("diskId", getId());
         jsonStr.put("mode", getMode());
         jsonStr.put("role", getRole());
         jsonStr.put("status", getStatus());
         return jsonStr.toJSONString();
        /*return String.format(
                "{ OsdIP : %s, diskPoolId : %s, path : %s, diskid : %s, mode : %s,  status : %s }", 
                getOsdIp(), getDiskPoolId(), getPath(), getId(), getMode(), getStatus());*/
    }
}
