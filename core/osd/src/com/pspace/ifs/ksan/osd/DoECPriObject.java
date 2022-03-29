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
package com.pspace.ifs.ksan.osd;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.pspace.ifs.ksan.osd.DISKPOOLLIST.DISKPOOL.SERVER;
import com.pspace.ifs.ksan.osd.DISKPOOLLIST.DISKPOOL.SERVER.DISK;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoECPriObject implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(DoECPriObject.class);
    private long fileLength;
    private int ecApplyMinutes;
    private DISKPOOLLIST diskPoolList;

    @Override
    public void run() {
        logger.info(OSDConstants.LOG_DO_EC_PRI_OBJECT_START);
        String ip = OSDUtils.getInstance().getLocalIP();
        fileLength = OSDUtils.getInstance().getECFileSize() * OSDConstants.MEGABYTES;
        ecApplyMinutes = OSDUtils.getInstance().getECApplyMinutes();

        List<String> diskList = new ArrayList<String>();
        logger.debug(OSDConstants.LOG_DO_EC_PRI_OBJECT_LOCAL_IP, ip);
        diskPoolList = OSDUtils.getInstance().getDiskPoolList();
        for (SERVER server : diskPoolList.getDiskpool().getServers()) {
            if (ip.equals(server.getIp())) {
                for (DISK disk : server.getDisks()) {
                    diskList.add(disk.getPath());
                }
            }
        }
        logger.debug(OSDConstants.LOG_DO_EC_PRI_OBJECT_DISKLIST_SIZE, diskList.size());
        for (String diskPath : diskList) {
            String objPath = diskPath + OSDConstants.SLASH + OSDConstants.OBJ_DIR;
            String ecPath = diskPath + OSDConstants.SLASH + OSDConstants.EC_DIR;
            logger.debug(OSDConstants.LOG_DO_EC_PRI_OBJECT_PATH, objPath, ecPath);
            
            check(objPath, ecPath);
        }
    }

    private void check(String dirPath, String ecPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        long now = Calendar.getInstance().getTimeInMillis();
        
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                check(files[i].getPath(), ecPath);
            }

            if (files[i].isFile()) {
                if (files[i].getName().startsWith(OSDConstants.POINT)) {
                    continue;
                }

                if (OSDConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY.equals(OSDUtils.getInstance().getAttributeFileReplication(files[i]))) {
                    long diff = (now - files[i].lastModified()) / OSDConstants.ONE_MINUTE_MILLISECONDS;

                    if (diff >= ecApplyMinutes) {
                        if (files[i].length() >= fileLength) {
                            ecEncode(files[i], ecPath);
                        }
                    }
                }
            }
        }
    }

    private void ecEncode(File file, String ecPath) {
        try {
            logger.info(OSDConstants.LOG_DO_EC_PRI_OBJECT_ENCODE_EC, file.getName());
            String path = OSDUtils.getInstance().makeECDirectory(file.getName(), ecPath);
            File ecFile = new File(path);
            com.google.common.io.Files.createParentDirs(ecFile);
            ecFile.mkdir();

            String command = OSDConstants.DO_EC_PRI_OBJECT_ZFEC
                            + path
                            + OSDConstants.DO_EC_PRI_OBJECT_ZFEC_PREFIX_OPTION
                            + OSDConstants.POINT + file.getName() 
                            + OSDConstants.DO_EC_PRI_OBJECT_ZFEC_TOTAL_NUMBER_OPTION + file.getAbsolutePath();
            logger.debug(OSDConstants.LOG_DO_EC_PRI_OBJECT_ZFEC_COMMAND, command);
            Process p = Runtime.getRuntime().exec(command);
            int exitCode = p.waitFor();
            p.destroy();
            logger.info(OSDConstants.LOG_DO_EC_PRI_OBJECT_ZFEC_EXIT_CODE, exitCode);
            createECTemp(file.getName(), ecPath);
            
            String replicaDiskID = OSDUtils.getInstance().getAttributeFileReplicaDiskID(file);
            if (!OSDConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL.equals(replicaDiskID)) {
                // delete replica disk
                logger.info(OSDConstants.LOG_DO_EC_PRI_OBJECT_REPLICA_DISK_ID, replicaDiskID);
                String ip = OSDUtils.getInstance().getOSDIP(diskPoolList, replicaDiskID);
                String diskPath = OSDUtils.getInstance().getPath(diskPoolList, replicaDiskID);
                String replicaPath = OSDUtils.getInstance().makePath(diskPath, file.getName());
                if (ip != null && diskPath != null) {
                    if (OSDUtils.getInstance().getLocalIP().equals(ip)) {
                        // local replica
                        File replicaFile = new File(replicaPath);
                        if (replicaFile.exists()) {
                            replicaFile.delete();
                        }
                    } else {
                        deleteReplica(ip, replicaPath);
                    }
                }
            }

            file.delete();
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    private void createECTemp(String fileName, String ecPath) {
        // create ec temp file
        File ecFile = new File(OSDUtils.getInstance().makeECTempPath(fileName, ecPath));
        try {
            ecFile.createNewFile();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void deleteReplica(String ipAddress, String replicaPath) {
		Socket socket = null;
        try {
            socket = new Socket(ipAddress, OSDUtils.getInstance().getPort());
            socket.setTcpNoDelay(true);
            String header = OSDConstants.DELETE_REPLICA + OSDConstants.DELIMITER + replicaPath;
            logger.debug(OSDConstants.LOG_DO_EC_PRI_OBJECT_HEADER, header);
            byte[] buffer = header.getBytes(OSDConstants.CHARSET_UTF_8);
            int size = buffer.length;
            
            DataOutputStream so = new DataOutputStream(socket.getOutputStream());
            so.writeInt(size);
            so.write(buffer, 0, size);
            so.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
	}
}
