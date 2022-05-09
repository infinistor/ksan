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
package com.pspace.ifs.ksan.gw.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pspace.ifs.ksan.gw.utils.disk.Disk;
import com.pspace.ifs.ksan.gw.utils.disk.DiskPool;
import com.pspace.ifs.ksan.gw.utils.disk.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskManager {
    private static final Logger logger = LoggerFactory.getLogger(DiskManager.class);
    private List<DiskPool> diskPoolList;
    private String localHost;
    private HashMap<String, String> localDiskInfoMap = new HashMap<String, String>();
    
    public static DiskManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static DiskManager INSTANCE = new DiskManager();
    }

    private DiskManager() {
        localHost = GWUtils.getLocalIP();
        diskPoolList = new ArrayList<DiskPool>();
    }

    public void configure() {
        for (DiskPool pool : diskPoolList) {
            for (Server server : diskPoolList.get(0).getServerList()) {
                if (localHost.equals(server.getIp())) {
                    for (Disk disk : server.getDiskList()) {
                        localDiskInfoMap.put(disk.getId(), disk.getPath());
                    }
                }
            }
        }
    }

    public String getLocalPath(String diskID) {
        return localDiskInfoMap.get(diskID);
    }

    public String getOSDIP(String diskID) {
        for (DiskPool pool : diskPoolList) {
            for (Server server : diskPoolList.get(0).getServerList()) {
                for (Disk disk : server.getDiskList()) {
                    if (diskID.equals(disk.getId())) {
                        return server.getIp();
                    }
                }
            }
        }

        return null;
    }

    public void addDiskPool(DiskPool diskPool) {
        diskPoolList.add(diskPool);
    }

    public void clearDiskPoolList() {
        diskPoolList.clear();
    }

    public List<DiskPool> getDiskPoolList() {
        return diskPoolList;
    }

    public DiskPool getDiskPool() {
        return diskPoolList.get(0);
    }

    public DiskPool getDiskPool(String id) {
        for (DiskPool pool : diskPoolList) {
            if (id.equals(pool.getId())) {
                return pool;
            }
        }

        return null;
    }

    public void saveFile() throws IOException {
        try {
            FileWriter fileWriter = new FileWriter("/usr/local/ksan/etc/diskpools.xml2", false);
            fileWriter.write("<DISKPOOLLIST>" + "\n");
            fileWriter.write("\t<DISKPOOL id=\"" + diskPoolList.get(0).getId() + "\" name=\"" + diskPoolList.get(0).getName() + "\" replicationType=\"" + diskPoolList.get(0).getReplicationType() + "\">\n");
            for (Server server : diskPoolList.get(0).getServerList()) {
                fileWriter.write("\t\t<SERVER id=\"" + server.getId() + "\" ip=\"" + server.getIp() + "\" status=\"" + server.getStatus() + "\">\n");
                for (Disk disk : server.getDiskList()) {
                    fileWriter.write("\t\t\t<DISK id=\"" + disk.getId() + "\" path=\"" + disk.getPath() + "\" mode=\"" + disk.getMode() + "\" status=\"" + disk.getStatus() + "\" />\n");
                }
                fileWriter.write("\t\t</SERVER>\n");
            }
            fileWriter.write("\t</DISKPOOL>\n");
            fileWriter.write("</DISKPOOLLIST>");
            fileWriter.close();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
