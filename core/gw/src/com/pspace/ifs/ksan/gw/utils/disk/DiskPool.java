package com.pspace.ifs.ksan.gw.utils.disk;

import java.util.ArrayList;
import java.util.List;

public class DiskPool {
    private String id;
    private String name;
    private String classTypeId; // "STANDARD", "ARCHIVE"
    private String replicationType; // "OnePlusZero", "OnePlusOne", "OnePlusTwo"
    private List<Server> serverList;

    public DiskPool() {
        id = "";
        name = "";
        classTypeId = "";
        replicationType = "";
        serverList = new ArrayList<Server>();
    }

    public DiskPool(String id, String name, String classTypeId, String replicationType) {
        this.id = id;
        this.name = name;
        this.classTypeId = classTypeId;
        this.replicationType = replicationType;
        serverList = new ArrayList<Server>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassTypeId() {
        return classTypeId;
    }

    public void setClassTypeId(String classTypeId) {
        this.classTypeId = classTypeId;
    }

    public String getReplicationType() {
        return replicationType;
    }

    public void setReplicationType(String replicationType) {
        this.replicationType = replicationType;
    }

    public List<Server> getServerList() {
        return serverList;
    }

    public void setServerList(List<Server> serverList) {
        this.serverList = serverList;
    }

    public void addServer(Server server) {
        serverList.add(server);
    }
}
