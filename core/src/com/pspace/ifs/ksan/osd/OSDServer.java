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

import static com.google.common.io.BaseEncoding.base16;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.FileReader;
import org.json.simple.JSONObject;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.pspace.ifs.ksan.osd.DoEmptyTrash;
import com.pspace.ifs.ksan.osd.utils.OSDConfig;
import com.pspace.ifs.ksan.osd.utils.OSDConstants;
import com.pspace.ifs.ksan.osd.utils.OSDUtils;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.KsanUtils;
import com.pspace.ifs.ksan.libs.data.OsdData;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.HeartbeatManager;
import com.pspace.ifs.ksan.libs.config.AgentConfig;
import com.pspace.ifs.ksan.libs.OSDClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.crypto.stream.CtrCryptoInputStream;
import org.apache.commons.crypto.stream.CtrCryptoOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OSDServer {
    private final static Logger logger = LoggerFactory.getLogger(OSDServer.class);
    private static String localIP;
    private static int port;
    private static boolean isRunning;
    private static ScheduledExecutorService serviceEmptyTrash = null;
    private static ScheduledExecutorService serviceMoveCacheToDisk = null;
    private static ScheduledExecutorService serviceEC = null;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new HookThread());
        OSDServer server = new OSDServer();
        server.start();
    }

    public void start() {
        logger.info(OSDConstants.LOG_OSD_SERVER_START);
        KsanUtils.writePID(OSDConstants.PID_PATH);
        try {
            OSDPortal.getInstance().getConfig();
            OSDPortal.getInstance().getDiskPoolsDetails();
            while (!OSDPortal.getInstance().isAppliedDiskpools()) {
                Thread.sleep(1000);
                OSDPortal.getInstance().getDiskPoolsDetails();
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            System.exit(1);
        }

        try {
            EventObject.getInstance().regist();
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }

        int poolSize = OSDConfig.getInstance().getPoolSize();
        localIP = KsanUtils.getLocalIP();
        port = OSDConfig.getInstance().getPort();

        ExecutorService pool = Executors.newFixedThreadPool(poolSize);

        isRunning = true;
        try (ServerSocket server = new ServerSocket(port)) {
            while (isRunning) {
                try {
                    Socket socket = server.accept();
                    logger.info(OSDConstants.LOG_OSD_SERVER_CONNECTED_INFO, socket.getRemoteSocketAddress().toString());
                    pool.execute(new Worker(socket));
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private class Worker implements Runnable {
        private Socket socket;
        
        Worker(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[OSDConstants.HEADERSIZE];
                DataInputStream di = new DataInputStream(socket.getInputStream());
                boolean flag = false;

                while (true) {
                    int length = socket.getInputStream().read();
                    if (length == -1) {
                        logger.info("socket {} EOF ...", socket.getRemoteSocketAddress().toString());
                        break;
                    }

                    logger.info("header length : {}", length);
                    // if (length > OSDConstants.HEADERSIZE) {
                    //     logger.error("Header size is too big : {}", length);
                    //     break;
                    // }

                    socket.getInputStream().read(buffer, 0, length);
                    String indicator = new String(buffer, 0, OsdData.INDICATOR_SIZE);
                    String header = new String(buffer, 0, length);
                    String[] headers = header.split(OsdData.DELIMITER, -1);
                    
                    switch (indicator) {
                    case OsdData.GET:
                        get(headers);
                        break;
                    
                    case OsdData.PUT:
                        put(headers);
                        break;
                    
                    case OsdData.DELETE:
                        delete(headers);
                        break;

                    case OsdData.DELETE_REPLICA:
                        deleteReplica(headers);
                        break;
    
                    case OsdData.COPY:
                        copy(headers);
                        break;

                    case OsdData.GET_PART:
                        getPart(headers);
                        break;
    
                    case OsdData.PART:
                        part(headers);
                        break;

                    case OsdData.DELETE_PART:
                        deletePart(headers);
                        break;
    
                    case OsdData.PART_COPY:
                        partCopy(headers);
                        break;
    
                    case OsdData.COMPLETE_MULTIPART:
                        completeMultipart(headers);
                        break;
    
                    case OsdData.ABORT_MULTIPART:
                        abortMultipart(headers);
                        break;
    
                    case OsdData.STOP:
                        isRunning = false;
                        break;
    
                    default:
                        logger.error(OSDConstants.LOG_OSD_SERVER_UNKNOWN_INDICATOR, indicator);
                        flag = true;
                    }
                    
                    if (flag) {
                        break;
                    }
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                logger.info("socket : {} - {}", socket.getRemoteSocketAddress().toString(), e.getMessage());

                if (socket.isClosed()) {
                    logger.error("Socket is closed");
                }   
                if (!socket.isConnected()) {
                    logger.error("Socket is not connected");
                }
                if (socket.isInputShutdown()) {
                    logger.error("Socket input is shutdown");
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        private void get(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String versionId = headers[OsdData.VERSIONID_INDEX];
            String sourceRange = headers[OsdData.SOURCE_RANGE_INDEX];
            String key = headers[OsdData.KEY_INDEX];
            long readTotal = 0L;
            CtrCryptoInputStream encryptIS = null;
            
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_INFO, path, objId, versionId, sourceRange);
    
            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
            if (!Strings.isNullOrEmpty(key)) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    long remainLength = 0L;
                    int readLength = 0;
                    int readBytes;
        
                    encryptIS = OSDUtils.initCtrDecrypt(fis, key);

                    if (Strings.isNullOrEmpty(sourceRange)) {
                        remainLength = file.length();
                        while ((readLength = encryptIS.read(buffer, 0, OSDConstants.BUFSIZE)) != -1) {
                            readTotal += readLength;
                            socket.getOutputStream().write(buffer, 0, readLength);
                        }
                    } else {
                        String[] ranges = sourceRange.split(OSDConstants.SLASH);
                        for (String range : ranges) {
                            String[] rangeParts = range.split(OSDConstants.COMMA);
                            long offset = Longs.tryParse(rangeParts[OSDConstants.RANGE_OFFSET_INDEX]);
                            long length = Longs.tryParse(rangeParts[OSDConstants.RANGE_LENGTH_INDEX]);
                            logger.debug(OSDConstants.LOG_OSD_SERVER_RANGE_INFO, offset, length);
        
                            if (offset > 0) {
                                encryptIS.skip(offset);
                            }
                            remainLength = length;
                            while (remainLength > 0) {
                                readBytes = 0;
                                if (remainLength < OSDConstants.MAXBUFSIZE) {
                                    readBytes = (int)remainLength;
                                } else {
                                    readBytes = OSDConstants.MAXBUFSIZE;
                                }
    
                                readLength = encryptIS.read(buffer, 0, readBytes);
                                
                                readTotal += readLength;
                                socket.getOutputStream().write(buffer, 0, readLength);
                                remainLength -= readLength;
                            }
                        }
                    }
                }
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    long remainLength = 0L;
                    int readLength = 0;
                    int readBytes;
        
                    if (Strings.isNullOrEmpty(sourceRange)) {
                        remainLength = file.length();
                        while (remainLength > 0) {
                            readBytes = 0;
                            if (remainLength < OSDConstants.MAXBUFSIZE) {
                                readBytes = (int)remainLength;
                            } else {
                                readBytes = OSDConstants.MAXBUFSIZE;
                            }
    
                            readLength = fis.read(buffer, 0, readBytes);
                            
                            readTotal += readLength;
                            socket.getOutputStream().write(buffer, 0, readLength);
                            remainLength -= readLength;
                        }
                    } else {
                        String[] ranges = sourceRange.split(OSDConstants.SLASH);
                        for (String range : ranges) {
                            String[] rangeParts = range.split(OSDConstants.COMMA);
                            long offset = Longs.tryParse(rangeParts[OSDConstants.RANGE_OFFSET_INDEX]);
                            long length = Longs.tryParse(rangeParts[OSDConstants.RANGE_LENGTH_INDEX]);
                            logger.debug(OSDConstants.LOG_OSD_SERVER_RANGE_INFO, offset, length);
        
                            if (offset > 0) {
                                fis.skip(offset);
                            }
                            remainLength = length;
                            while (remainLength > 0) {
                                readBytes = 0;
                                if (remainLength < OSDConstants.MAXBUFSIZE) {
                                    readBytes = (int)remainLength;
                                } else {
                                    readBytes = OSDConstants.MAXBUFSIZE;
                                }
    
                                readLength = fis.read(buffer, 0, readBytes);
                                
                                readTotal += readLength;
                                socket.getOutputStream().write(buffer, 0, readLength);
                                remainLength -= readLength;
                            }
                        }
                    }
                }
            }
            
            socket.getOutputStream().flush();
            socket.getOutputStream().close();
            
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_END, readTotal);
            logger.info("from : {}", socket.getRemoteSocketAddress().toString());
            logger.info(OSDConstants.LOG_OSD_SERVER_GET_SUCCESS_INFO, path, objId, versionId, sourceRange);
        }
    
        private void put(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String versionId = headers[OsdData.VERSIONID_INDEX];
            long length = Longs.tryParse(headers[OsdData.PUT_LENGTH_INDEX]);
            String replication = headers[OsdData.PUT_REPLICATION_INDEX];
            String replicaDiskID = headers[OsdData.PUT_REPLICA_DISK_ID_INDEX];
            String key = headers[OsdData.PUT_KEY_INDEX];
            String mode  = headers[OsdData.PUT_MODE_INDEX];
            CtrCryptoOutputStream encryptOS = null;

            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_INFO, path, objId, versionId, length, replication, mode);

            boolean isNoDisk = false;
            if (mode != null) {
                if (mode.equals(OSDConstants.PERFORMANCE_MODE_NO_DISK)) {
                    isNoDisk = true;
                }
            }

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = null;
            File tmpFile = null;
            File trashFile = null;

            if (!Strings.isNullOrEmpty(key)) {
                if (OSDConfig.getInstance().isCacheDiskpath()) {
                    file = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeObjPath(path, objId, versionId)));
                    tmpFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTempPath(path, objId, versionId)));
                    trashFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTrashPath(path, objId, versionId)));
                    File linkFile = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
                    com.google.common.io.Files.createParentDirs(linkFile);
                } else {
                    file = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
                    tmpFile = new File(OSDUtils.getInstance().makeTempPath(path, objId, versionId));
                    trashFile = new File(OSDUtils.getInstance().makeTrashPath(path, objId, versionId));
                }

                com.google.common.io.Files.createParentDirs(file);
                com.google.common.io.Files.createParentDirs(tmpFile);
                try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                    int readLength = 0;
                    long remainLength = length;
                    int readMax = (int) (length < OSDConstants.MAXBUFSIZE ? length : OSDConstants.MAXBUFSIZE);
                    encryptOS = OSDUtils.initCtrEncrypt(fos, key);
                    while ((readLength = socket.getInputStream().read(buffer, 0, readMax)) != -1) {
                        remainLength -= readLength;
                        if (!isNoDisk) {
                            encryptOS.write(buffer, 0, readLength);
                        }
                        if (remainLength <= 0) {
                            break;
                        }
                        readMax = (int) (remainLength < OSDConstants.MAXBUFSIZE ? remainLength : OSDConstants.MAXBUFSIZE);
                    }
                    if (!isNoDisk) {
                        encryptOS.flush();
                    }
                }
            } else {
                if (OSDConfig.getInstance().isCacheDiskpath()) {
                    file = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeObjPath(path, objId, versionId)));
                    tmpFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTempPath(path, objId, versionId)));
                    trashFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTrashPath(path, objId, versionId)));
                    File linkFile = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
                    com.google.common.io.Files.createParentDirs(linkFile);
                } else {
                    file = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
                    tmpFile = new File(OSDUtils.getInstance().makeTempPath(path, objId, versionId));
                    trashFile = new File(OSDUtils.getInstance().makeTrashPath(path, objId, versionId));
                }

                com.google.common.io.Files.createParentDirs(file);
                com.google.common.io.Files.createParentDirs(tmpFile);
                try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                    int readLength = 0;
                    long remainLength = length;
                    int readMax = (int) (length < OSDConstants.MAXBUFSIZE ? length : OSDConstants.MAXBUFSIZE);
                    while ((readLength = socket.getInputStream().read(buffer, 0, readMax)) != -1) {
                        remainLength -= readLength;
                        if (!isNoDisk) {
                            fos.write(buffer, 0, readLength);
                        }
                        if (remainLength <= 0) {
                            break;
                        }
                        readMax = (int) (remainLength < OSDConstants.MAXBUFSIZE ? remainLength : OSDConstants.MAXBUFSIZE);
                    }
                    if (!isNoDisk) {
                        fos.flush();
                    }
                }
            }

            if (file.exists()) {
                File temp = new File(file.getAbsolutePath());
                logger.info("file is already exists : {}", file.getAbsolutePath());
                retryRenameTo(temp, trashFile);
            }

            // OSDUtils.getInstance().setAttributeFileReplication(tmpFile, replication, replicaDiskID);
            retryRenameTo(tmpFile, file);
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                String fullPath = OSDUtils.getInstance().makeObjPath(path, objId, versionId);
                Files.createSymbolicLink(Paths.get(fullPath), Paths.get(file.getAbsolutePath()));
            }
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_END);
            logger.info("from : {}", socket.getRemoteSocketAddress().toString());
            logger.info(OSDConstants.LOG_OSD_SERVER_PUT_SUCCESS_INFO, path, objId, versionId, length);
        }
    
        private void delete(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String versionId = headers[OsdData.VERSIONID_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_INFO, path, objId, versionId);
            boolean isCache = false;
            File file = null;
            File trashFile = null;
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                file = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeObjPath(path, objId, versionId)));
                if (file.exists()) {
                    isCache = true;
                    trashFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTrashPath(path, objId, versionId)));
                } else {
                    file = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
                    trashFile = new File(OSDUtils.getInstance().makeTrashPath(path, objId, versionId));
                }
            } else {
                file = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
                trashFile = new File(OSDUtils.getInstance().makeTrashPath(path, objId, versionId));
            }
            
            retryRenameTo(file, trashFile);
            if (isCache) {
                File link = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
                link.delete();
            }

            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_DELETE_SUCCESS_INFO, path, objId, versionId);
        }

        private void deletePart(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_PART_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String partNo = headers[OsdData.PARTNO_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_PART_INFO, path, objId, partNo);

            File file = new File(OSDUtils.getInstance().makeTempPath(path, objId, partNo));
            if (file.exists()) {
                file.delete();
            }

            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_PART_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_DELETE_PART_SUCCESS_INFO, path, objId, partNo);
        }

        private void deleteReplica(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_REPLICA_START);
            String path = headers[OsdData.PATH_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_REPLICA_PATH, path);

            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
            
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_REPLICA_END);
        }
    
        private void copy(String[] headers) throws IOException, NoSuchAlgorithmException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_COPY_START);
            String srcPath = headers[OsdData.PATH_INDEX];
            String srcObjId = headers[OsdData.OBJID_INDEX];
            String srcVersionId = headers[OsdData.VERSIONID_INDEX];
            String destPath = headers[OsdData.DEST_PATH_INDEX];
            String destObjId = headers[OsdData.DEST_OBJID_INDEX];
            String destVersionId = headers[OsdData.DEST_VERSIONID_INDEX];
            String replication = headers[OsdData.COPY_REPLICATION_INDED];
            String replicaDiskID = headers[OsdData.COPY_REPLICA_DISK_ID_INDEX];

            logger.debug(OSDConstants.LOG_OSD_SERVER_COPY_INFO, srcPath, srcObjId, srcVersionId, destPath, destObjId, destVersionId);

            String destIP = null; //DiskManager.getInstance().getOSDIP(destDiskId);
            if (destIP == null) {
                logger.error(OSDConstants.LOG_OSD_SERVER_CAN_NOT_FIND_OSD_IP, destPath);
                return;
            }

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File srcFile = new File(OSDUtils.getInstance().makeObjPath(srcPath, srcObjId, srcVersionId));
            try (FileInputStream fis = new FileInputStream(srcFile)) {
                if (localIP.equals(destIP)) {
                    File file = new File(OSDUtils.getInstance().makeObjPath(destPath, destObjId, destVersionId));
                    File tmpFile = new File(OSDUtils.getInstance().makeTempPath(destPath, destObjId, destVersionId));
                    File trashFile = new File(OSDUtils.getInstance().makeTrashPath(destPath, destObjId, destVersionId));
        
                    com.google.common.io.Files.createParentDirs(file);
                    com.google.common.io.Files.createParentDirs(tmpFile);
                    try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                        int readLength = 0;
                        while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
                            fos.write(buffer, 0, readLength);
                        }
                        fos.flush();
                    }
                    if (file.exists()) {
                        File temp = new File(file.getAbsolutePath());
                        retryRenameTo(temp, trashFile);
                    }
                    // OSDUtils.getInstance().setAttributeFileReplication(file, replication, replicaDiskID);
                    retryRenameTo(tmpFile, file);
                } else {
                    try (Socket destSocket = new Socket(destIP, port)) {
                        String header = OsdData.PUT 
                                        + OsdData.DELIMITER + destPath 
                                        + OsdData.DELIMITER + destObjId 
                                        + OsdData.DELIMITER + destVersionId 
                                        + OsdData.DELIMITER + String.valueOf(srcFile.length())
                                        + OsdData.DELIMITER + replication
                                        + OsdData.DELIMITER + replicaDiskID;
                        logger.debug(OSDConstants.LOG_OSD_SERVER_COPY_RELAY_OSD, destIP, header);
                        sendHeader(destSocket, header);
                        MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5);
    
                        int readLength = 0;
                        while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
                            destSocket.getOutputStream().write(buffer, 0, readLength);
                            md5er.update(buffer, 0, readLength);
                        }
                        destSocket.getOutputStream().flush();
    
                        byte[] digest = md5er.digest();
                        String eTag = base16().lowerCase().encode(digest);
    
                        OsdData data = receiveData(destSocket);
                        if (!eTag.equals(data.getETag())) {
                            logger.error(OSDConstants.LOG_OSD_SERVER_DIFFERENCE_ETAG, eTag, data.getETag());
                        }
                    }
                }
            }

            logger.info(OSDConstants.LOG_OSD_SERVER_COPY_SUCCESS_INFO, srcPath, srcObjId, srcVersionId, destPath, destObjId, destVersionId);
        }
    
        private void getPart(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_PART_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String partNo = headers[OsdData.PART_NO_INDEX];

            long readTotal = 0L;
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_PART_INFO, path, objId, partNo);
    
            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = null;
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                file = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTempPartPath(path, objId, partNo)));
            } else {
                file = new File(OSDUtils.getInstance().makeTempPartPath(path, objId, partNo));
            }
            try (FileInputStream fis = new FileInputStream(file)) {
                long remainLength = 0L;
                int readLength = 0;
                int readBytes;

                remainLength = file.length();
                
                while (remainLength > 0) {
                    readBytes = 0;
                    if (remainLength < OSDConstants.MAXBUFSIZE) {
                        readBytes = (int)remainLength;
                    } else {
                        readBytes = OSDConstants.MAXBUFSIZE;
                    }
                    readLength = fis.read(buffer, 0, readBytes);
                    readTotal += readLength;
                    socket.getOutputStream().write(buffer, 0, readLength);
                    remainLength -= readLength;
                }
            }
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_PART_END, readTotal);
            logger.info(OSDConstants.LOG_OSD_SERVER_GET_PART_SUCCESS_INFO, path, objId, partNo);
            socket.getOutputStream().flush();
            
            if (!file.delete()) {
                logger.error(OSDConstants.LOG_OSD_SERVER_FAILED_FILE_DELETE, file.getName());
            }
        }

        private void part(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String partNo = headers[OsdData.PARTNO_INDEX];
            long length = Longs.tryParse(headers[OsdData.PUT_LENGTH_INDEX]);
            String key = headers[OsdData.PARTKEY_INDEX];
            CtrCryptoOutputStream encryptOS = null;

            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_INFO, path, objId, partNo, length, key);

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File tmpFile = null;
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                tmpFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTempPartPath(path, objId, partNo)));
            } else {
                tmpFile = new File(OSDUtils.getInstance().makeTempPartPath(path, objId, partNo));
            }

            com.google.common.io.Files.createParentDirs(tmpFile);
            if (!Strings.isNullOrEmpty(key)) {
                try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                    int readLength = 0;
                    long remainLength = length;
                    int readMax = (int) (length < OSDConstants.MAXBUFSIZE ? length : OSDConstants.MAXBUFSIZE);
                    encryptOS = OSDUtils.initCtrEncrypt(fos, key);
                    while ((readLength = socket.getInputStream().read(buffer, 0, readMax)) != -1) {
                        remainLength -= readLength;
                        encryptOS.write(buffer, 0, readLength);
                        if (remainLength <= 0) {
                            break;
                        }
                        readMax = (int) (remainLength < OSDConstants.MAXBUFSIZE ? remainLength : OSDConstants.MAXBUFSIZE);
                    }
                    encryptOS.flush();
                }
            } else {
                try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                    int readLength = 0;
                    long remainLength = length;
                    int readMax = (int) (length < OSDConstants.MAXBUFSIZE ? length : OSDConstants.MAXBUFSIZE);
                    while ((readLength = socket.getInputStream().read(buffer, 0, readMax)) != -1) {
                        remainLength -= readLength;
                        fos.write(buffer, 0, readLength);
                        if (remainLength <= 0) {
                            break;
                        }
                        readMax = (int) (remainLength < OSDConstants.MAXBUFSIZE ? remainLength : OSDConstants.MAXBUFSIZE);
                    }
                    fos.flush();
                }
            }

            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_PART_SUCCESS_INFO, path, objId, partNo, length);
        }
    
        private void partCopy(String[] headers) throws IOException, NoSuchAlgorithmException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_COPY_START);
            String srcPath = headers[OsdData.PATH_INDEX];
            String srcObjId = headers[OsdData.OBJID_INDEX];
            String srcVersionId = headers[OsdData.VERSIONID_INDEX];
            String destPath = headers[OsdData.DEST_PATH_INDEX];
            String destObjId = headers[OsdData.DEST_OBJID_INDEX];
            String destPartNo = headers[OsdData.DEST_PARTNO_INDEX];
            String copySourceRange = headers[OsdData.SRC_RANAGE_INDEX];

            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_COPY_INFO, srcPath, srcObjId, srcVersionId, destPath, destObjId, destPartNo, copySourceRange);

            String destIP = null; //findOSD(destPath);
            if (destIP == null) {
                logger.error(OSDConstants.LOG_OSD_SERVER_CAN_NOT_FIND_OSD_IP, destPath);
                return;
            }

            MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5);
            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File srcFile = new File(OSDUtils.getInstance().makeObjPath(srcPath, srcObjId, srcVersionId));
            long remainLength = 0L;
            int readLength = 0;
            int readBytes;
            String eTag = "";

            OsdData data = null;

            try (FileInputStream fis = new FileInputStream(srcFile)) {
                File tmpFile = null;
                if (OSDConfig.getInstance().isCacheDiskpath()) {
                    tmpFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTempPath(destPath, destObjId, destPartNo)));
                } else {
                    tmpFile = new File(OSDUtils.getInstance().makeTempPath(destPath, destObjId, destPartNo));
                }
                com.google.common.io.Files.createParentDirs(tmpFile);
                try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                    data = new OsdData();
                    if (Strings.isNullOrEmpty(copySourceRange)) {
                        remainLength = srcFile.length();
                        data.setFileSize(remainLength);
                        while (remainLength > 0) {
                            readBytes = 0;
                            if (remainLength < OSDConstants.MAXBUFSIZE) {
                                readBytes = (int)remainLength;
                            } else {
                                readBytes = OSDConstants.MAXBUFSIZE;
                            }
                            readLength = fis.read(buffer, 0, readBytes);
                            fos.write(buffer, 0, readLength);
                            md5er.update(buffer, 0, readLength);
                            remainLength -= readLength;
                        }
                        fos.flush();
                    } else {
                        String[] ranges = copySourceRange.split(OSDConstants.SLASH);
                        long totalLength = 0L;
                        for (String range : ranges) {
                            String[] rangeParts = range.split(OSDConstants.COMMA);
                            long offset = Longs.tryParse(rangeParts[OSDConstants.RANGE_OFFSET_INDEX]);
                            long length = Longs.tryParse(rangeParts[OSDConstants.RANGE_LENGTH_INDEX]);
                            logger.debug(OSDConstants.LOG_OSD_SERVER_RANGE_INFO, offset, length);
    
                            if (offset > 0) {
                                fis.skip(offset);
                            }
                            remainLength = length;
                            totalLength += length;
                            while (remainLength > 0) {
                                readBytes = 0;
                                if (remainLength < OSDConstants.MAXBUFSIZE) {
                                    readBytes = (int)remainLength;
                                } else {
                                    readBytes = OSDConstants.MAXBUFSIZE;
                                }
                                readLength = fis.read(buffer, 0, readBytes);
                                fos.write(buffer, 0, readLength);
                                md5er.update(buffer, 0, readLength);
                                remainLength -= readLength;
                            }
                            fos.flush();
    
                            data.setFileSize(totalLength);
                        }
                    }
                }
                byte[] digest = md5er.digest();
                eTag = base16().lowerCase().encode(digest);
            }

            sendData(data.getETag(), data.getFileSize());
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_COPY_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_PART_COPY_SUCCESS_INFO, srcPath, srcObjId, srcVersionId, destPath, destObjId, destPartNo, copySourceRange);
        }
    
        private void completeMultipart(String[] headers) throws IOException, NoSuchAlgorithmException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_COMPLETE_MULTIPART_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String versionId = headers[OsdData.VERSIONID_INDEX];
            String key = headers[OsdData.COMPLETE_MULTIPART_KEY_INDEX];
            String partInfos = headers[OsdData.COMPLETE_MULTIPART_PARTNOS_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_COMPLETE_MULTIPART_INFO, path, objId, partInfos);
            
            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5);
            long totalLength = 0L;
            long existFileSize = 0L;
            long putSize = 0L;
            long calSize = 0L;
            CtrCryptoOutputStream encryptOS = null;
            CtrCryptoInputStream encryptIS = null;
            String eTag;
            
            String[] arrayPartInfo = partInfos.split(OSDConstants.COMMA);

            SortedMap<Integer, Part> listPart = null;
            for (int i = 0; i < arrayPartInfo.length; i++) {
                Part part = new Part();
                String[] info = arrayPartInfo[i].split(OSDConstants.SLASH);
                logger.debug("part no : {}, diskId : {}, size : {}", info[0], info[1], info[2]);
                part.setPartNumber(Integer.parseInt(info[0]));
                part.setDiskID(info[1]);
                part.setPartSize(Long.parseLong(info[2]));
                listPart.put(part.getPartNumber(), part);
            }

            File tmpFile = null;
            File file = null;
            File trashFile = null;

            if (OSDConfig.getInstance().isCacheDiskpath()) {
                file = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeObjPath(path, objId, versionId)));
                tmpFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTempPath(path, objId, versionId)));
                trashFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTrashPath(path, objId, versionId)));
            } else {
                file = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
                tmpFile = new File(OSDUtils.getInstance().makeTempPath(path, objId, versionId));
                trashFile = new File(OSDUtils.getInstance().makeTrashPath(path, objId, versionId));
            }
            com.google.common.io.Files.createParentDirs(tmpFile);
            com.google.common.io.Files.createParentDirs(file);

            try (FileOutputStream tmpOut = new FileOutputStream(tmpFile)) {
                if (!Strings.isNullOrEmpty(key)) {
                    encryptOS = OSDUtils.initCtrEncrypt(tmpOut, key);
                    // for each part object
                    for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<Integer, Part> entry = it.next();
                        String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getDiskID());
                        if (!Strings.isNullOrEmpty(partPath)) {
                            // part is in local disk
                            logger.debug("part : {}, diskID : {}, part path : {}", entry.getKey(), entry.getValue().getDiskID(), partPath);
                            File partFile = null;
                            if (OSDConfig.getInstance().isCacheDiskpath()) {
                                partFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTempPartPath(partPath, objId, String.valueOf(entry.getValue().getPartNumber()))));
                            } else {
                                partFile = new File(OSDUtils.getInstance().makeTempPartPath(partPath, objId, String.valueOf(entry.getValue().getPartNumber())));
                            }
    
                            try (FileInputStream fis = new FileInputStream(partFile)) {
                                encryptIS = OSDUtils.initCtrDecrypt(fis, key);
                                int readLength = 0;
                                while ((readLength = encryptIS.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
                                    totalLength += readLength;
                                    encryptOS.write(buffer, 0, readLength);
                                    md5er.update(buffer, 0, readLength);
                                }
                                encryptOS.flush();
                            }
                        } else {
                            partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                            String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                            OSDClient client = new OSDClient(host, OSDConfig.getInstance().getPort());
                            client.getPartInit(partPath, objId, String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), encryptOS, md5er);
                            totalLength += client.getPart();
                        }
                    }
                } else {
                    // for each part object
                    for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<Integer, Part> entry = it.next();
                        String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getDiskID());
                        if (!Strings.isNullOrEmpty(partPath)) {
                            // part is in local disk
                            logger.debug("part : {}, diskID : {}, part path : {}", entry.getKey(), entry.getValue().getDiskID(), partPath);
                            File partFile = null;
                            if (OSDConfig.getInstance().isCacheDiskpath()) {
                                partFile = new File(OSDUtils.getInstance().makeCachePath(OSDUtils.getInstance().makeTempPartPath(partPath, objId, String.valueOf(entry.getValue().getPartNumber()))));
                            } else {
                                partFile = new File(OSDUtils.getInstance().makeTempPartPath(partPath, objId, String.valueOf(entry.getValue().getPartNumber())));
                            }
                            try (FileInputStream fis = new FileInputStream(partFile)) {
                                int readLength = 0;
                                while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
                                    totalLength += readLength;
                                    tmpOut.write(buffer, 0, readLength);
                                    md5er.update(buffer, 0, readLength);
                                }
                                tmpOut.flush();
                            }
                        } else {
                            partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                            String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                            OSDClient client = new OSDClient(host, OSDConfig.getInstance().getPort());
                            client.getPartInit(partPath, objId, String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), tmpOut, md5er);
                            totalLength += client.getPart();
                        }
                    }
                }

                byte[] digest = md5er.digest();
                eTag = base16().lowerCase().encode(digest);
            }

            if (file.exists()) {
                File temp = new File(file.getAbsolutePath());
                logger.info("file is already exists : {}", file.getAbsolutePath());
                retryRenameTo(temp, trashFile);
            }

            retryRenameTo(tmpFile, file);
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                String fullPath = OSDUtils.getInstance().makeObjPath(path, objId, versionId);
                Files.createSymbolicLink(Paths.get(fullPath), Paths.get(file.getAbsolutePath()));
            }

            sendData(eTag, totalLength);
            logger.debug(OSDConstants.LOG_OSD_SERVER_COMPLETE_MULTIPART_END);
        }
    
        private void abortMultipart(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_ABORE_MULTIPART_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String partNos = headers[OsdData.ABORT_MULTIPART_PARTNOS];
            logger.debug(OSDConstants.LOG_OSD_SERVER_ABORE_MULTIPART_INFO, path, objId, partNos);
            String[] arrayPartNos = partNos.split(OSDConstants.COMMA);

            for (String partNo : arrayPartNos) {
                File partFile = new File(OSDUtils.getInstance().makeTempPath(path, objId, partNo));
            
                if (!partFile.delete()) {
                    logger.error(OSDConstants.LOG_OSD_SERVER_FAILED_FILE_DELETE, partFile.getName());
                }
            }
            logger.debug(OSDConstants.LOG_OSD_SERVER_ABORE_MULTIPART_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_ABORE_MULTIPART_SUCCESS_INFO, path, objId, partNos);
        }

        // private String findOSD(String path) {
        //     for (SERVER server : diskpoolList.getDiskpool().getServers()) {
        //         for (DISK disk : server.getDisks()) {
        //             if (path.equals(disk.getPath())) {
        //                 return server.getIp();
        //             }
        //         }
        //     }

        //     return null;
        // }

        private void sendHeader(Socket socket, String header) throws IOException {
            byte[] buffer = header.getBytes(OSDConstants.CHARSET_UTF_8);
            int size = buffer.length;
            
            DataOutputStream so = new DataOutputStream(socket.getOutputStream());
            
            so.writeInt(size);
            so.write(buffer, 0, size);
            so.flush();
        }

        private void sendData(String ETag, long fileSize) throws IOException {
            String tail = OsdData.FILE + OsdData.DELIMITER + ETag + OsdData.DELIMITER + String.valueOf(fileSize);
            byte[] buffer = tail.getBytes(OSDConstants.CHARSET_UTF_8);
            int size = buffer.length;
            
            DataOutputStream so = new DataOutputStream(socket.getOutputStream());
            
            so.writeInt(size);
            so.write(buffer, 0, size);
            so.flush();
        }      

        private OsdData receiveData(Socket socket) throws IOException {
            DataInputStream si = new DataInputStream(socket.getInputStream());
            int size = si.readInt();
            byte[] buffer = new byte[size];
            si.read(buffer, 0, size);
            String result = new String(buffer, 0, size);
            String[] ArrayResult = result.split(OsdData.DELIMITER, -1);

            OsdData data = new OsdData();
            switch (ArrayResult[0]) {
            case OsdData.FILE:
                data.setETag(ArrayResult[1]);
                data.setFileSize(Long.parseLong(ArrayResult[2]));
                return data;
            default:
                logger.error(OSDConstants.LOG_OSD_SERVER_UNKNOWN_DATA, ArrayResult[1]);
            }

            return null;
        }

        private void retryRenameTo(File srcFile, File destFile) throws IOException {
            if (srcFile.exists()) {
                for (int i = 0; i < OSDConstants.RETRY_COUNT; i++) {
                    if (srcFile.renameTo(destFile)) {
                        return;
                    }
                }
                logger.error(OSDConstants.LOG_OSD_SERVER_FAILED_FILE_RENAME, srcFile.getAbsolutePath(), destFile.getAbsolutePath());
            }
        }
    }

    public static void startEmptyTrash() {
        if (serviceEmptyTrash != null) {
            serviceEmptyTrash.shutdownNow();
        } else {
            serviceEmptyTrash = Executors.newSingleThreadScheduledExecutor();
        }
        serviceEmptyTrash.scheduleAtFixedRate(new DoEmptyTrash(), 1000, OSDConfig.getInstance().getTrashCheckInterval(), TimeUnit.MILLISECONDS);
    }

    public static void startMoveCacheToDisk() {
        if (OSDConfig.getInstance().isCacheDiskpath()) {
            if (serviceMoveCacheToDisk != null) {
                serviceMoveCacheToDisk.shutdownNow();
            } else {
                serviceMoveCacheToDisk = Executors.newSingleThreadScheduledExecutor();
            }
            serviceMoveCacheToDisk.scheduleAtFixedRate(new DoMoveCacheToDisk(), 1000, OSDConfig.getInstance().getCacheCheckInterval(), TimeUnit.MILLISECONDS);
        }
    }

    public static void startECThread() {
        if (serviceEC != null) {
            serviceEC.shutdownNow();
        } else {
            serviceEC = Executors.newSingleThreadScheduledExecutor();
        }
        serviceEC.scheduleAtFixedRate(new DoECPriObject(), 1000, OSDConfig.getInstance().getECCheckInterval(), TimeUnit.MILLISECONDS);
    }

    static class HookThread extends Thread {
		private static final Logger logger = LoggerFactory.getLogger(HookThread.class);
		
		@Override
		public void run() {
			// kill -TERM pid
			try {
                logger.info(OSDConstants.HOOK_THREAD_INFO);
                OSDPortal.getInstance().postGWEvent(false);
			} catch (Exception e) {
				PrintStack.logging(logger, e);
			}
            logger.info(OSDConstants.STOP_KSAN_OSD);
		}
	}
}