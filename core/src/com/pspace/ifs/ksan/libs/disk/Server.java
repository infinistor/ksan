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

public class Server {
    public static final String ID = "Id";
    public static final String STATE = "State";
    public static final String DISKS = "Disks";
    public static final String NETWORK_INTERFACES = "NetworkInterfaces";
    public static final String IP_ADDRESS = "IpAddress";

    private String id;
    private String ip;
    private String status;  // "Unknown", "Timeout", "Offline", "Online"
    private List<Disk> diskList;

    public static final String STATUS_UNKNOWN = "Unknown";
    public static final String STATUS_TIMEOUT = "Timeout";
    public static final String STATUS_OFFLINE = "Offline";
    public static final String STATUS_ONLINE = "Online";

    public Server() {
        id = "";
        ip = "";
        status = "";
        diskList = null;
    }

    public Server(String id, String ip, String status) {
        this.id = id;
        this.ip = ip;
        this.status = status;
        diskList = null;
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
        return new ArrayList<Disk>(diskList);
    }

    public void setDiskList(List<Disk> diskList) {
        this.diskList = new ArrayList<Disk>(diskList);
    }

    public void addDisk(Disk disk) {
        diskList.add(disk);
    }
}
