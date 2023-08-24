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
import java.util.HashMap;
import java.util.List;
import java.io.FileInputStream;

import org.apache.commons.io.FileUtils;

import com.pspace.ifs.ksan.osd.utils.OSDConfig;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.osd.utils.OSDUtils;
import com.pspace.ifs.ksan.osd.utils.OSDConstants;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.KsanUtils;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.data.OsdData;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;
import com.pspace.ifs.ksan.libs.osd.OSDClient;
import com.pspace.ifs.ksan.libs.data.ECPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoECPriObject implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(DoECPriObject.class);
    private String localIP = KsanUtils.getLocalIP();
    private long fileLength;
    private int ecWaitTime;
    private int numberOfCodingChunks;
    private int numberOfDataChunks;
    HashMap<String, String> localDiskInfoMap;

    @Override
    public void run() {
        logger.info(OSDConstants.LOG_DO_EC_PRI_OBJECT_START);
        fileLength = OSDConfig.getInstance().getECMinSize() * OSDConstants.MEGABYTES;
        ecWaitTime = OSDConfig.getInstance().getECWaitTime();

        localDiskInfoMap = DiskManager.getInstance().getLocalDiskInfo();
        logger.debug("local disk info size : {}", localDiskInfoMap.size());
        localDiskInfoMap.forEach((diskId, diskPath) -> {
            logger.debug("diskId: {}, diskPath: {}", diskId, diskPath);
            numberOfCodingChunks = DiskManager.getInstance().getECM(diskId);
            numberOfDataChunks = DiskManager.getInstance().getECK(diskId);
            logger.debug("number of coding chunks: {}, number of data chunks: {}", numberOfCodingChunks, numberOfDataChunks);
            if (numberOfCodingChunks > 0 && numberOfDataChunks > 0) {
                // check EC
                check(diskPath + Constants.SLASH + Constants.OBJ_DIR, diskPath + Constants.SLASH + Constants.EC_DIR);
            }
        });
    }

    private void check(String dirPath, String ecPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        long now = Calendar.getInstance().getTimeInMillis();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                check(files[i].getAbsolutePath(), ecPath);
                continue;
            }
            logger.debug("file : {}", files[i].getName());
            if (files[i].isFile()) {
                if (files[i].getName().startsWith(OSDConstants.POINT)) {
                    continue;
                }
                String replica = KsanUtils.getAttributeFileReplication(files[i]);
                logger.debug("replica : -{}-", replica);
                if (Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY.equals(replica)) {
                    long diff = (now - files[i].lastModified());
                    if (diff >= ecWaitTime) {
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
            String path = KsanUtils.makeECDirectory(file.getName(), ecPath);
            File ecFile = new File(path);
            com.google.common.io.Files.createParentDirs(ecFile);
            ecFile.mkdir();

            String command = Constants.ZFEC
                            + path
                            + Constants.ZFEC_PREFIX_OPTION
                            + OSDConstants.POINT + file.getName() 
                            + Constants.ZFEC_TOTAL_SHARES_OPTION + Integer.toString(numberOfCodingChunks + numberOfDataChunks)
                            + Constants.ZFEC_REQUIRED_SHARES_OPTION + Integer.toString(numberOfDataChunks) + Constants.SPACE
                            + file.getAbsolutePath();
            logger.debug(OSDConstants.LOG_DO_EC_PRI_OBJECT_ZFEC_COMMAND, command);
            Process p = Runtime.getRuntime().exec(command);
            int exitCode = p.waitFor();
            p.destroy();
            logger.info(OSDConstants.LOG_DO_EC_PRI_OBJECT_ZFEC_EXIT_CODE, exitCode);
            
            // spread ec file
            spreadEC(path, file.getName());
            
            String replicaDiskID = KsanUtils.getAttributeFileReplicaDiskID(file);
            logger.debug("replica diskID : {}", replicaDiskID);
            if (!Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL.equals(replicaDiskID)) {
                // delete replica disk
                logger.info(OSDConstants.LOG_DO_EC_PRI_OBJECT_REPLICA_DISK_ID, replicaDiskID);
                String ip = DiskManager.getInstance().getOSDIP(replicaDiskID);
                String diskPath = DiskManager.getInstance().getPath(replicaDiskID);
                String replicaPath = KsanUtils.makePath(diskPath, file.getName());
                if (ip != null && diskPath != null) {
                    if (localIP.equals(ip)) {
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

    private void deleteReplica(String ipAddress, String replicaPath) {
		Socket socket = null;
        try {
            OSDClient client = new OSDClient(ipAddress, OSDConfig.getInstance().getPort());
            client.deleteReplica(replicaPath);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
	}

    private void spreadEC(String path, String fileName) {
        File dir = new File(path);
        File[] files = dir.listFiles();
        String[] ends = new String[files.length];
        // File dest = new File(path + Constants.SLASH + Constants.POINT + fileName);

        logger.debug("ec parts : {}", files.length);
        for (int i = 0; i < files.length; i++) {
            ends[i] = Integer.toString(i) + Constants.UNDERSCORE + Integer.toString(numberOfCodingChunks + numberOfDataChunks) + Constants.ZFEC_SUFFIX;
        }

        try {
            List<ECPart> sendList = new ArrayList<ECPart>();
            for (DiskPool pool : DiskManager.getInstance().getDiskPoolList()) {
                for (Server server : pool.getServerList()) {
                    for (Disk disk : server.getDiskList()) {
                        ECPart sendECPart = new ECPart(server.getIp(), disk.getId(), disk.getPath(), false);
                        sendList.add(sendECPart);
                    }
                }
            }

            byte[] buffer = new byte[Constants.MAXBUFSIZE];
            for (int i = 0, j = 1; i < ends.length; i++) {
                logger.debug("file : {}", files[i].getName());
                if (!files[i].getName().endsWith(".fec")) {
                    files[i].delete();
                    continue;
                }
                for (ECPart sendECPart : sendList) {
                    String ecPartPath = KsanUtils.makeECDirectory(files[i].getName().substring(1), sendECPart.getDiskPath() + Constants.SLASH + Constants.EC_DIR) + Constants.SLASH + Constants.POINT + fileName;
                    if (!sendECPart.isProcessed()) {
                        if (sendECPart.getServerIP().equals(localIP)) {
                            // if local disk, move file
                            try {
                                String copyPath = KsanUtils.makeECDirectory(files[i].getName().substring(1), sendECPart.getDiskPath() + Constants.SLASH + Constants.EC_DIR);
                                File file = new File(ecPartPath);
                                logger.debug("move file : {}, to : {}", files[i].getName(), file.getAbsolutePath());
                                FileUtils.moveFile(files[i], file);
                            } catch (IOException e) {
                                PrintStack.logging(logger, e);
                            }
                            sendECPart.setProcessed(true);
                        } else {
                            long fileLength = files[i].length();
                            OSDClient client = new OSDClient(sendECPart.getServerIP(), OSDConfig.getInstance().getPort());
                            logger.debug("send file : {}, to : {}, {}, {}", files[i].getName(), sendECPart.getServerIP(), sendECPart.getDiskPath(), ecPartPath);
                            client.putECPartInit(ecPartPath, fileLength);
                            int readLength = 0;
                            long totalReads = 0L;
                            try (FileInputStream fis = new FileInputStream(files[i])) {
                                while ((readLength = fis.read(buffer, 0, Constants.MAXBUFSIZE)) != -1) {
                                    totalReads += readLength;
                                    client.putECPart(buffer, 0, readLength);
                                    if (totalReads >= fileLength) {
                                        break;
                                    }
                                }
                            }
                            client.putECPartFlush();
                            sendECPart.setProcessed(true);
                            files[i].delete();
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }
    }
}

