package com.pspace.ifs.ksan.gw.utils.disk;

import java.util.ArrayList;
import java.util.List;

public class Server {
    private String id;
    private String ip;
    private String status;  // "Unknown", "Timeout", "Offline", "Online"
    private List<Disk> diskList;

    public Server() {
        id = "";
        ip = "";
        status = "";
        diskList = new ArrayList<Disk>();
    }

    public Server(String id, String ip, String status) {
        this.id = id;
        this.ip = ip;
        this.status = status;
        diskList = new ArrayList<Disk>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Disk> getDiskList() {
        return diskList;
    }

    public void setDiskList(List<Disk> diskList) {
        this.diskList = diskList;
    }

    public void addDisk(Disk disk) {
        diskList.add(disk);
    }
}
