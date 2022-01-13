package com.pspace.ifs.ksan.objmanager;

public class DISK{
    private String path;
    private String diskid;
    private double totalSpace;
    private double usedSpace;
    private double reservedSpace;
    private double totalInode;
    private double usedInode;
    private String osdIp;
    private int role; // primary or replica
    private DiskMode mode;
    private DiskStatus status; // GOOD, STOPPED, BROKEN, UNKNOWN;
    
    public DISK(){
        this.path = "";
        this.diskid = "";
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
    
    @Override
    public String toString(){
        return String.format(
                "{ path : %s diskid : %s mode : %s  status : %s }", 
                this.getPath(), this.getId(), this.getMode(), this.getStatus());
    }
}
