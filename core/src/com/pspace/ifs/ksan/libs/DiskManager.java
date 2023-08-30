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
package com.pspace.ifs.ksan.libs;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Map;

import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DiskManager {
    public static final String DATA = "Data";
    public static final String ITEMS = "Items";

    public static final String FILE_DISKPOOL_LIST_START = "<DISKPOOLLIST>\n";
    public static final String FILE_DISKPOOL_LIST_END = "</DISKPOOLLIST>";
    public static final String FILE_DISKPOOL_ID = "\t<DISKPOOL id=\"";
    public static final String FILE_DISKPOOL_END = "\t</DISKPOOL>\n";
    public static final String FILE_DISKPOOL_NAME = "\" name=\"";
    public static final String FILE_DISKPOOL_REPLICATION_TYPE =  "\" replicationType=\"";
    public static final String FILE_DISKPOOL_NEWLINE = "\">\n";
    public static final String FILE_SERVER_ID = "\t\t<SERVER id=\"";
    public static final String FILE_SERVER_IP = "\" ip=\"";
    public static final String FILE_SERVER_STATUS = "\" status=\"";
    public static final String FILE_SERVER_END = "\t\t</SERVER>\n";
    public static final String FILE_DISK_ID = "\t\t\t<DISK id=\"";
    public static final String FILE_DISK_PATH = "\" path=\"";
    public static final String FILE_DISK_MODE = "\" mode=\"";
    public static final String FILE_DISK_STATUS = "\" status=\"";
    public static final String FILE_DISK_END = "\" />\n";

    public static final String DISK_MODE_READONLY = "ReadOnly";
    public static final String DISK_MODE_READWRITE = "ReadWrite";
    public static final String DISK_MODE_MAINTENANCE = "Maintenance";
    public static final String DISK_STATUS_STOP = "Stop";
    public static final String DISK_STATUS_WEAK = "Weak";
    public static final String DISK_STATUS_GOOD = "Good";
    public static final String DISK_STATUS_BAD = "Bad";
    public static final String DISK_STATUS_DISABLE = "Disable";

    private static final Logger logger = LoggerFactory.getLogger(DiskManager.class);
    private List<DiskPool> diskPoolList;
    private String localHost;
    private HashMap<String, String> localDiskInfoMap;
    
    public static DiskManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static DiskManager INSTANCE = new DiskManager();
    }

    private DiskManager() {
        localHost = KsanUtils.getLocalIP();
        diskPoolList = new ArrayList<DiskPool>();
        localDiskInfoMap = new HashMap<String, String>();
    }

    public void configure() {
        for (DiskPool pool : diskPoolList) {
            for (Server server : pool.getServerList()) {
                if (localHost.equals(server.getIp())) {
                    for (Disk disk : server.getDiskList()) {
                        localDiskInfoMap.put(disk.getId(), disk.getPath());
                    }
                }
            }
        }
    }

    public HashMap<String, String> getLocalDiskInfo() {
        HashMap<String, String> map = new HashMap<String, String>(localDiskInfoMap);
        return map;
    }

    public String getLocalPath(String diskID) {
        return localDiskInfoMap.get(diskID);
    }

    public String getLocalPath() {
        if (!localDiskInfoMap.isEmpty()) {
            Map.Entry<String, String> entry = localDiskInfoMap.entrySet().iterator().next();
            return entry.getValue();
        }
        return null;
    }

    public String getLocalDiskId() {
        if (!localDiskInfoMap.isEmpty()) {
            String key = localDiskInfoMap.keySet().iterator().next();
            return key;
        }
        return null;
    }

    public String getPath(String diskID) {
        for (DiskPool pool : diskPoolList) {
            for (Server server : pool.getServerList()) {
                for (Disk disk : server.getDiskList()) {
                    if (diskID.equals(disk.getId())) {
                        return disk.getPath();
                    }
                }
            }
        }

        return null;
    }

    public String getOSDIP(String diskID) {
        for (DiskPool pool : diskPoolList) {
            for (Server server : pool.getServerList()) {
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
        localDiskInfoMap.clear();
    }

    public List<DiskPool> getDiskPoolList() {
        return new ArrayList<DiskPool>(diskPoolList);
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

    public int getECM(String diskID) {
        for (DiskPool pool : diskPoolList) {
            for (Server server : pool.getServerList()) {
                for (Disk disk : server.getDiskList()) {
                    if (diskID.equals(disk.getId())) {
                        return pool.getErasureCodeM();
                    }
                }
            }
        }
        return 0;
    }

    public int getECK(String diskID) {
        for (DiskPool pool : diskPoolList) {
            for (Server server : pool.getServerList()) {
                for (Disk disk : server.getDiskList()) {
                    if (diskID.equals(disk.getId())) {
                        return pool.getErasureCodeK();
                    }
                }
            }
        }
        return 0;
    }

    public void saveFile() throws IOException {
        try {
            com.google.common.io.Files.createParentDirs(new File(Constants.DISKPOOL_CONF_PATH));
            try (FileWriter fileWriter = new FileWriter(Constants.DISKPOOL_CONF_PATH, StandardCharsets.UTF_8)) {
                fileWriter.write(FILE_DISKPOOL_LIST_START);
                for (DiskPool diskPool : diskPoolList) {
                    fileWriter.write(FILE_DISKPOOL_ID + diskPool.getId() + FILE_DISKPOOL_NAME + diskPool.getName() + FILE_DISKPOOL_REPLICATION_TYPE + diskPool.getReplicationType() + FILE_DISKPOOL_NEWLINE);
                    for (Server server : diskPool.getServerList()) {
                        fileWriter.write(FILE_SERVER_ID + server.getId() + FILE_SERVER_IP + server.getIp() + FILE_SERVER_STATUS + server.getStatus() + FILE_DISKPOOL_NEWLINE);
                        for (Disk disk : server.getDiskList()) {
                            fileWriter.write(FILE_DISK_ID + disk.getId() + FILE_DISK_PATH + disk.getPath() + FILE_DISK_MODE + disk.getMode() + FILE_DISK_STATUS + disk.getStatus() + FILE_DISK_END);
                        }
                        fileWriter.write(FILE_SERVER_END);
                    }
                    fileWriter.write(FILE_DISKPOOL_END);
                }
                fileWriter.write(FILE_DISKPOOL_LIST_END);
            } catch (IOException e) {
                throw new IOException(e);
            }
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    public boolean isAvailableDiskForRead(String diskID) {
        for (DiskPool pool : diskPoolList) {
            for (Server server : pool.getServerList()) {
                for (Disk disk : server.getDiskList()) {
                    if (diskID.equals(disk.getId()) && !disk.getMode().equals(DISK_MODE_MAINTENANCE) && disk.getStatus().equals(DISK_STATUS_GOOD)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isAvailableDiskForWrite(String diskID) {
        for (DiskPool pool : diskPoolList) {
            for (Server server : pool.getServerList()) {
                for (Disk disk : server.getDiskList()) {
                    if (diskID.equals(disk.getId()) && disk.getMode().equals(DISK_MODE_READWRITE) && disk.getStatus().equals(DISK_STATUS_GOOD)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
