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
package com.pspace.ifs.ksan.libs.disk;

import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Strings;

public class DiskPool {
    public static final String ID = "Id";
    public static final String NAME = "Name";
    public static final String DISK_POOL_TYPE = "DiskPoolType";
    public static final String REPLICATION_TYPE = "ReplicationType";
    public static final String REPLICATION_TYPE_ONE_PLUS_ZERO = "OnePlusZero";
    public static final String REPLICATION_TYPE_ONE_PLUS_ONE = "OnePlusOne";
    public static final String REPLICATION_TYPE_ONE_PLUS_TWO = "OnePlusTwo";
    public static final String REPLICATION_TYPE_ERASURE_CODE = "ErasureCode";
    public static final String SERVERS = "Servers";
    public static final String EC = "EC";
    public static final String EC_M = "M";  // the number of coding chunks
    public static final String EC_K = "K";  // the number of data chunks

    private String id;
    private String name;
    private String classTypeId; // "STANDARD", "ARCHIVE"
    private String replicationType; // "OnePlusZero", "OnePlusOne", "OnePlusTwo", "ErasureCode"
    private int replicaCount;
    private int ecM;
    private int ecK;
    private List<Server> serverList;

    public DiskPool() {
        id = "";
        name = "";
        classTypeId = "";
        replicationType = "";
        ecM = 0;
        ecK = 0;
        serverList = new ArrayList<Server>();
    }

    public DiskPool(String id, String name, String classTypeId, String replicationType) {
        this.id = id;
        this.name = name;
        this.classTypeId = classTypeId;
        this.replicationType = replicationType;
        ecM = 0;
        ecK = 0;
        serverList = new ArrayList<Server>();
    }

    public DiskPool(String id, String name, String classTypeId, String replicationType, int ecM, int ecK) {
        this.id = id;
        this.name = name;
        this.classTypeId = classTypeId;
        this.replicationType = replicationType;
        this.ecM = ecM;
        this.ecK = ecK;
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
        if (!Strings.isNullOrEmpty(this.replicationType)) {
            if (this.replicationType.equalsIgnoreCase(REPLICATION_TYPE_ONE_PLUS_ONE)) {
                replicaCount = 2;
            } else if (this.replicationType.equalsIgnoreCase(REPLICATION_TYPE_ONE_PLUS_ZERO)) {
                replicaCount = 1;
            } else if (this.replicationType.equalsIgnoreCase(REPLICATION_TYPE_ONE_PLUS_TWO)) {
                replicaCount = 3;
            } else if (this.replicationType.equalsIgnoreCase(REPLICATION_TYPE_ERASURE_CODE)) {
                replicaCount = 2;
            } else {
                replicaCount = 0;
            }
        }
    }

    public List<Server> getServerList() {
        return new ArrayList<Server>(serverList);
    }

    public void setServerList(List<Server> serverList) {
        this.serverList = new ArrayList<Server>(serverList);
    }

    public void addServer(Server server) {
        serverList.add(server);
    }

    public int getReplicaCount() {
        return replicaCount;
    }

    public int getErasureCodeM() {
        return ecM;
    }

    public int getErasureCodeK() {
        return ecK;
    }
}
