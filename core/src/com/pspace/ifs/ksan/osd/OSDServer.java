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

import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
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
import com.pspace.ifs.ksan.libs.osd.OSDClient;
import com.pspace.ifs.ksan.libs.Constants;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;

import de.sfuhrm.openssl4j.OpenSSL4JProvider;
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

        // startEmptyTrash();
        // startMoveCacheToDisk();
        startECThread();

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
                // DataInputStream di = new DataInputStream(socket.getInputStream());
                boolean flag = false;

                while (true) {                    
                    int length = socket.getInputStream().read();
                    if (length == -1) {
                        logger.info("socket {} EOF ... socket close.", socket.getRemoteSocketAddress().toString());
                        if (socket != null) {
                            socket.close();
                        }
                        break;
                    }
                    byte[] lengthBuffer = new byte[length];
                    socket.getInputStream().read(lengthBuffer, 0, length);
                    String strLength = new String(lengthBuffer, StandardCharsets.UTF_8);

                    length = Integer.parseInt(strLength);

                    logger.info("header length : {}", length);
                    // if (length > OSDConstants.HEADERSIZE) {
                    //     logger.error("Header size is too big : {}", length);
                    //     break;
                    // }

                    socket.getInputStream().read(buffer, 0, length);
                    String indicator = new String(buffer, 0, OsdData.INDICATOR_SIZE, StandardCharsets.UTF_8);
                    String header = new String(buffer, 0, length, StandardCharsets.UTF_8);
                    logger.debug("read header : {}", header);
                    String[] headers = header.split(OsdData.DELIMITER, -1);
                    
                    switch (indicator) {
                    case OsdData.GET:
                        get(headers);
                        break;
                    
                    case OsdData.PUT:
                        put(headers);
                        break;

                    case OsdData.PUT_RANGE:
                        putRange(headers);
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

                    case OsdData.GET_EC_PART:
                        getECPart(headers);
                        break;

                    case OsdData.PUT_EC_PART:
                        putECPart(headers);
                        break;

                    case OsdData.DELETE_EC_PART:
                        deleteECPart(headers);
                        break;

                    case OsdData.GET_MULTIPART:
                        getMuiltipart(headers);
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
                PrintStack.logging(logger, e);
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
            
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_INFO, path, objId, versionId, sourceRange, key);
    
            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = new File(KsanUtils.makeObjPath(path, objId, versionId));
            logger.debug("file : {}", file.getAbsolutePath());

            if (key.equalsIgnoreCase(OSDConstants.STR_NULL)) {
                key = null;
            }

            try {
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
                            long offset = 0L;
                            long length = 0L;
                            for (String range : ranges) {
                                String[] rangeParts = range.split(OSDConstants.COMMA);
                                Long offsetLong = Longs.tryParse(rangeParts[OSDConstants.RANGE_OFFSET_INDEX]);
                                Long lengthLong = Longs.tryParse(rangeParts[OSDConstants.RANGE_LENGTH_INDEX]);
                                offset = offsetLong == null ? 0L : offsetLong;
                                length = lengthLong == null ? 0L : lengthLong;

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
                            long offset = 0L;
                            long length = 0L;
                            for (String range : ranges) {
                                String[] rangeParts = range.split(OSDConstants.COMMA);
                                Long offsetLong = Longs.tryParse(rangeParts[OSDConstants.RANGE_OFFSET_INDEX]);
                                Long lengthLong = Longs.tryParse(rangeParts[OSDConstants.RANGE_LENGTH_INDEX]);
                                offset = offsetLong == null ? 0L : offsetLong;
                                length = lengthLong == null ? file.length() : lengthLong;
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
            } catch (Exception e) {
                PrintStack.logging(logger, e);
                socket.close();
            }
            
            socket.getOutputStream().flush();
            logger.info(OSDConstants.LOG_OSD_SERVER_GET_SUCCESS_INFO, path, objId, versionId, sourceRange, readTotal);
        }

        private void getECPart(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_EC_PART_START);
            String path = headers[OsdData.PATH_INDEX];
            logger.debug("path : {}", path);
            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = new File(path);

            try (FileInputStream fis = new FileInputStream(file)) {
                long remainLength = file.length();
                int readLength = 0;
                int readBytes;

                while (remainLength > 0) {
                    readBytes = 0;
                    if (remainLength < OSDConstants.MAXBUFSIZE) {
                        readBytes = (int)remainLength;
                    } else {
                        readBytes = OSDConstants.MAXBUFSIZE;
                    }
                    readLength = fis.read(buffer, 0, readBytes);
                    socket.getOutputStream().write(buffer, 0, readLength);
                    remainLength -= readLength;
                }
                socket.getOutputStream().flush();
            } catch (IOException e) {
                PrintStack.logging(logger, e);
                socket.close();
            }

            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_EC_PART_END);
        }
    
        private void put(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String versionId = headers[OsdData.VERSIONID_INDEX];
            Long lengthLong = Longs.tryParse(headers[OsdData.PUT_LENGTH_INDEX]);
            long length = lengthLong == null ? 0L : lengthLong;
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

            if (key.equalsIgnoreCase(OSDConstants.STR_NULL)) {
                key = null;
            }
            if (!Strings.isNullOrEmpty(key)) {
                if (OSDConfig.getInstance().isCacheDiskpath()) {
                    file = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeObjPath(path, objId, versionId));
                    tmpFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeTempPath(path, objId, versionId));
                    trashFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeTrashPath(path, objId, versionId));
                    File linkFile = new File(KsanUtils.makeObjPath(path, objId, versionId));
                } else {
                    file = new File(KsanUtils.makeObjPath(path, objId, versionId));
                    tmpFile = new File(KsanUtils.makeTempPath(path, objId, versionId));
                    trashFile = new File(KsanUtils.makeTrashPath(path, objId, versionId));
                }

                try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                    int readLength = 0;
                    long remainLength = length;
                    int readMax = (int) (length < OSDConstants.BUFSIZE ? length : OSDConstants.BUFSIZE);
                    encryptOS = OSDUtils.initCtrEncrypt(fos, key);
                    while ((readLength = socket.getInputStream().read(buffer, 0, readMax)) != -1) {
                        remainLength -= readLength;
                        if (!isNoDisk) {
                            encryptOS.write(buffer, 0, readLength);
                        }
                        if (remainLength <= 0) {
                            break;
                        }
                        readMax = (int) (remainLength < OSDConstants.BUFSIZE ? remainLength : OSDConstants.BUFSIZE);
                    }
                    if (!isNoDisk) {
                        encryptOS.flush();
                    }
                }
            } else {
                if (OSDConfig.getInstance().isCacheDiskpath()) {
                    file = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeObjPath(path, objId, versionId));
                    tmpFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeTempPath(path, objId, versionId));
                    trashFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeTrashPath(path, objId, versionId));
                    File linkFile = new File(KsanUtils.makeObjPath(path, objId, versionId));
                } else {
                    file = new File(KsanUtils.makeObjPath(path, objId, versionId));
                    tmpFile = new File(KsanUtils.makeTempPath(path, objId, versionId));
                    trashFile = new File(KsanUtils.makeTrashPath(path, objId, versionId));
                }

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

            KsanUtils.setAttributeFileReplication(tmpFile, replication, replicaDiskID);
            retryRenameTo(tmpFile, file);
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                String fullPath = KsanUtils.makeObjPath(path, objId, versionId);
                Files.createSymbolicLink(Paths.get(fullPath), Paths.get(file.getAbsolutePath()));
            }

            logger.info(OSDConstants.LOG_OSD_SERVER_PUT_SUCCESS_INFO, path, objId, versionId, length);
        }

        private void putECPart(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_EC_PART_START);
            String path = headers[OsdData.PATH_INDEX];
            Long lengthLong = Longs.tryParse(headers[OsdData.PUT_EC_LENGTH_INDEX]);
            long length = lengthLong == null ? 0L : lengthLong;

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = new File(path);
            com.google.common.io.Files.createParentDirs(file);
            try (FileOutputStream fos = new FileOutputStream(file, false)) {
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
            }
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_EC_PART_END);
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
                file = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeObjPath(path, objId, versionId));
                if (file.exists()) {
                    isCache = true;
                    trashFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeTrashPath(path, objId, versionId));
                } else {
                    file = new File(KsanUtils.makeObjPath(path, objId, versionId));
                    trashFile = new File(KsanUtils.makeTrashPath(path, objId, versionId));
                }
            } else {
                file = new File(KsanUtils.makeObjPath(path, objId, versionId));
                trashFile = new File(KsanUtils.makeTrashPath(path, objId, versionId));
            }
            
            retryRenameTo(file, trashFile);
            if (isCache) {
                File link = new File(KsanUtils.makeObjPath(path, objId, versionId));
                link.delete();
            }

            logger.info(OSDConstants.LOG_OSD_SERVER_DELETE_SUCCESS_INFO, path, objId, versionId);
        }

        private void deletePart(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_PART_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String uploadId = headers[OsdData.PART_DELETE_UPLOADID_INDEX];
            String partNumber = headers[OsdData.PART_DELETE_PARTNUMBER_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_PART_INFO, path, objId, uploadId, partNumber);

            File file = new File(KsanUtils.makePartPath(path, objId, uploadId, partNumber));
            File trashFile = new File(KsanUtils.makeTrashPath(path, objId, uploadId));
            if (file.exists()) {
                retryRenameTo(file, trashFile);
            } else {
                logger.error("does not exist: {}", file.getAbsolutePath());
            }

            logger.info(OSDConstants.LOG_OSD_SERVER_DELETE_PART_SUCCESS_INFO, path, objId, uploadId, partNumber);
        }

        private void deleteECPart(String[] headers) {
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_EC_PART_START);
            String path = headers[OsdData.PATH_INDEX];
            
            File file = new File(path);
            if (file.exists()) {
                file.delete();
                logger.debug("ec part delete : {}", path);
            } else {
                logger.debug("file does not exist. : {}", path);
            }
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_EC_PART_END);
        }

        private void deleteReplica(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_REPLICA_START);
            String path = headers[OsdData.PATH_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_REPLICA_PATH, path);

            File file = new File(path);
            if (file.exists()) {
                file.delete();
            } else {
                logger.info("deleteReplica, does not exist: {}", path);
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
            File srcFile = new File(KsanUtils.makeObjPath(srcPath, srcObjId, srcVersionId));
            try (FileInputStream fis = new FileInputStream(srcFile)) {
                if (localIP.equals(destIP)) {
                    File file = new File(KsanUtils.makeObjPath(destPath, destObjId, destVersionId));
                    File tmpFile = new File(KsanUtils.makeTempPath(destPath, destObjId, destVersionId));
                    File trashFile = new File(KsanUtils.makeTrashPath(destPath, destObjId, destVersionId));
        
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
                    KsanUtils.setAttributeFileReplication(file, replication, replicaDiskID);
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
                        MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5, new OpenSSL4JProvider());
    
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
            String range = headers[OsdData.PART_RANGE_INDEX];

            long readTotal = 0L;
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_PART_INFO, path, range);

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = null;
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                file = new File(OSDConfig.getInstance().getCacheDiskpath() + path);
            } else {
                file = new File(path);
            }
            try (FileInputStream fis = new FileInputStream(file)) {
                long remainLength = 0L;
                int readLength = 0;
                int readBytes;
                long offset = 0L;
                long last = 0L;

                if (!Strings.isNullOrEmpty(range)) {
                    String[] rangeInfo = range.split(OSDConstants.DASH);
                    offset = Long.parseLong(rangeInfo[0]);
                    last = Long.parseLong(rangeInfo[1]);
                    remainLength = last - offset + 1;
                    fis.skip(offset);
                } else {
                    remainLength = file.length();
                }
                
                logger.debug("file length : {}", remainLength);
                
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
            logger.info(OSDConstants.LOG_OSD_SERVER_GET_PART_SUCCESS_INFO, path, range);
            socket.getOutputStream().flush();
        }

        private void part(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String uploadId = headers[OsdData.UPLOAD_KEY_INDEX];
            String partNo = headers[OsdData.PART_NO_INDEX];
            Long lengthLong = Longs.tryParse(headers[OsdData.PART_LENGTH_INDEX]);
            long length = lengthLong == null ? 0L : lengthLong;
            String key = headers[OsdData.PART_KEY_INDEX];
            CtrCryptoOutputStream encryptOS = null;

            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_INFO, path, objId, uploadId, partNo, length, key);

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File tmpFile = null;
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                tmpFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makePartPath(path, objId, uploadId, partNo));
            } else {
                tmpFile = new File(KsanUtils.makePartPath(path, objId, uploadId, partNo));
            }

            if (key.equalsIgnoreCase(OSDConstants.STR_NULL)) {
                key = null;
            }
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
/*            
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_COPY_START);
            String srcDiskId = headers[OsdData.PATH_INDEX];
            String srcObjId = headers[OsdData.OBJID_INDEX];
            String srcVersionId = headers[OsdData.VERSIONID_INDEX];
            String destPath = headers[OsdData.DEST_PATH_INDEX];
            String destObjId = headers[OsdData.DEST_OBJID_INDEX];
            String destPartNo = headers[OsdData.DEST_PARTNO_INDEX];
            String copySourceRange = headers[OsdData.SRC_RANAGE_INDEX];
            String srcLength = headers[OsdData.SRC_LENGTH_INDEX];

            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_COPY_INFO, srcDiskId, srcObjId, srcVersionId, destPath, destObjId, destPartNo, copySourceRange);

            // String destIP = null; //findOSD(destPath);
            // if (destIP == null) {
            //     logger.error(OSDConstants.LOG_OSD_SERVER_CAN_NOT_FIND_OSD_IP, destPath);
            //     return;
            // }

            MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5, new OpenSSL4JProvider());
            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File srcFile = null; //new File(KsanUtils.makeObjPath(srcPath, srcObjId, srcVersionId));
            long remainLength = 0L;
            int readLength = 0;
            int readBytes = 0;
            long readTotal = 0L;
            String eTag = "";

            OsdData data = null;

            File tmpFile = null;
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                tmpFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makePartPath(destPath, destObjId, destPartNo));
            } else {
                tmpFile = new File(KsanUtils.makePartPath(destPath, destObjId, destPartNo));
            }

            try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                String srcPath = DiskManager.getInstance().getLocalPath(srcDiskId);
                if (srcPath != null) {
                    srcFile = new File(KsanUtils.makeObjPath(srcPath, srcObjId, srcVersionId));
                    try (FileInputStream fis = new FileInputStream(srcFile)) {            
                        if (Strings.isNullOrEmpty(copySourceRange)) {
                            remainLength = srcFile.length();
                            while (remainLength > 0) {
                                readBytes = 0;
                                if (remainLength < OSDConstants.MAXBUFSIZE) {
                                    readBytes = (int)remainLength;
                                } else {
                                    readBytes = OSDConstants.MAXBUFSIZE;
                                }
        
                                readLength = fis.read(buffer, 0, readBytes);
                                readTotal += readLength;
                                fos.write(buffer, 0, readLength);
                                md5er.update(buffer, 0, readLength);
                                remainLength -= readLength;
                            }
                        } else {
                            String[] ranges = copySourceRange.split(OSDConstants.SLASH);
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
                                    fos.write(buffer, 0, readLength);
                                    md5er.update(buffer, 0, readLength);
                                    remainLength -= readLength;
                                }
                            }
                        }
                    }
                } else {
                    OSDClient client = new OSDClient(DiskManager.getInstance().getOSDIP(srcDiskId), OSDConfig.getInstance().getPort());
                    client.getInitWithMD5(DiskManager.getInstance().getPath(srcDiskId), 
                                         srcObjId, 
                                         srcVersionId, 
                                         Long.parseLong(srcLength), 
                                         copySourceRange, 
                                         fos,
                                         md5er,
                                         "");
                    readTotal = client.getWithMD5();
                }
            }

            byte[] digest = md5er.digest();
            eTag = base16().lowerCase().encode(digest);

            // try (FileInputStream fis = new FileInputStream(srcFile)) {
            //     File tmpFile = null;
            //     if (OSDConfig.getInstance().isCacheDiskpath()) {
            //         tmpFile = new File(KsanUtils.makeCachePath(KsanUtils.makeTempPath(destPath, destObjId, destPartNo)));
            //     } else {
            //         tmpFile = new File(KsanUtils.makeTempPath(destPath, destObjId, destPartNo));
            //     }
            //     com.google.common.io.Files.createParentDirs(tmpFile);
            //     try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
            //         data = new OsdData();
            //         if (Strings.isNullOrEmpty(copySourceRange)) {
            //             remainLength = srcFile.length();
            //             data.setFileSize(remainLength);
            //             while (remainLength > 0) {
            //                 readBytes = 0;
            //                 if (remainLength < OSDConstants.MAXBUFSIZE) {
            //                     readBytes = (int)remainLength;
            //                 } else {
            //                     readBytes = OSDConstants.MAXBUFSIZE;
            //                 }
            //                 readLength = fis.read(buffer, 0, readBytes);
            //                 fos.write(buffer, 0, readLength);
            //                 md5er.update(buffer, 0, readLength);
            //                 remainLength -= readLength;
            //             }
            //             fos.flush();
            //         } else {
            //             String[] ranges = copySourceRange.split(OSDConstants.SLASH);
            //             long totalLength = 0L;
            //             for (String range : ranges) {
            //                 String[] rangeParts = range.split(OSDConstants.COMMA);
            //                 long offset = Longs.tryParse(rangeParts[OSDConstants.RANGE_OFFSET_INDEX]);
            //                 long length = Longs.tryParse(rangeParts[OSDConstants.RANGE_LENGTH_INDEX]);
            //                 logger.debug(OSDConstants.LOG_OSD_SERVER_RANGE_INFO, offset, length);
    
            //                 if (offset > 0) {
            //                     fis.skip(offset);
            //                 }
            //                 remainLength = length;
            //                 totalLength += length;
            //                 while (remainLength > 0) {
            //                     readBytes = 0;
            //                     if (remainLength < OSDConstants.MAXBUFSIZE) {
            //                         readBytes = (int)remainLength;
            //                     } else {
            //                         readBytes = OSDConstants.MAXBUFSIZE;
            //                     }
            //                     readLength = fis.read(buffer, 0, readBytes);
            //                     fos.write(buffer, 0, readLength);
            //                     md5er.update(buffer, 0, readLength);
            //                     remainLength -= readLength;
            //                 }
            //                 fos.flush();
    
            //                 data.setFileSize(totalLength);
            //             }
            //         }
            //     }
            //     byte[] digest = md5er.digest();
            //     eTag = base16().lowerCase().encode(digest);
            // }
            logger.debug("etag : {}, readTotal : {}", eTag, readTotal);
            sendData(eTag, readTotal);
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_COPY_END);
            // logger.info(OSDConstants.LOG_OSD_SERVER_PART_COPY_SUCCESS_INFO, srcPath, srcObjId, srcVersionId, destPath, destObjId, destPartNo, copySourceRange);
*/        }

        private void getMuiltipart(String[] headers) throws IOException {
            try {
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_MULTIPART_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String versionId = headers[OsdData.VERSIONID_INDEX];
            String sourceRange = headers[OsdData.SOURCE_RANGE_INDEX];
            String key = headers[OsdData.KEY_INDEX];
            logger.debug("path : {}, objId : {}, versionId : {}, sourceRange : {}, key : {}", path, objId, versionId, sourceRange, key);
            long readTotal = 0L;
            CtrCryptoInputStream encryptIS = null;
            
            long actualSize = 0L;

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = new File(KsanUtils.makeObjPathForOpen(path, objId, versionId));
            boolean isBorrowOsd = false;
            if (file.exists()) {
                OutputStream os = socket.getOutputStream();
                try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    String line = null;
                    String objDiskId = null;
                    String objPath = null;
                    long objSize = 0L;
                    long objOffset = 0L;
                    long objLast = 0L;
                    long objLength = 0L;
                    long remaingLength = 0L;
                    int readBytes = 0;
                    int readLength = 0;
                    boolean isRange = false;

                    // check range
                    if (!Strings.isNullOrEmpty(sourceRange)) {
                        br.mark(0);
                        String[] ranges = sourceRange.split(OSDConstants.SLASH);
                        long offset = 0L;
                        long length = 0L;
                        for (String range : ranges) {
                            long accOffset = 0L;
                            String[] rangeParts = range.split(OSDConstants.COMMA);
                            Long offsetLong = Longs.tryParse(rangeParts[0]);
                            Long lengthLong = Longs.tryParse(rangeParts[1]);
                            offset = offsetLong == null ? 0L : offsetLong;
                            length = lengthLong == null ? 0L : lengthLong;
                            br.reset();
                            while ((line = br.readLine()) != null) {
                                String[] infos = line.split(OSDConstants.COLON);
                                objDiskId = infos[0];
                                objPath = infos[1];
                                objSize = Long.parseLong(infos[2]);
                                long saveRemaingLength = 0L;
                                objOffset = 0L;
                                objLast = 0L;

                                if (infos.length > 3) {
                                    String[] objRanges = infos[3].split(OSDConstants.DASH);
                                    objOffset = Long.parseLong(objRanges[0]);
                                    objLast = Long.parseLong(objRanges[1]);
                                    objLength = objLast - objOffset + 1;
                                    isRange = true;
                                } else {
                                    objLength = objSize;
                                    isRange = false;
                                }

                                if (accOffset < offset) {
                                    if (accOffset + objLength <= offset) {
                                        accOffset += objLength;
                                        continue;
                                    } else {
                                        objOffset += offset - accOffset;
                                        objLength -= offset - accOffset;
                                        accOffset += objLength;
                                        isRange = true;
                                    }
                                }

                                if (objLength > length) {
                                    remaingLength = length;
                                } else {
                                    remaingLength = objLength;
                                }
                                saveRemaingLength = remaingLength;
                                
                                // check local path
                                if (DiskManager.getInstance().getLocalPath(objDiskId) != null) {
                                    // get local
                                    File partFile = new File(objPath);
                                    try (FileInputStream fis = new FileInputStream(partFile)) {
                                        if (isRange) {
                                            fis.skip(objOffset);
                                        }
                                        while (remaingLength > 0) {
                                            readBytes = 0;
                                            if (remaingLength < OSDConstants.MAXBUFSIZE) {
                                                readBytes = (int)remaingLength;
                                            } else {
                                                readBytes = OSDConstants.MAXBUFSIZE;
                                            }
                                            readLength = fis.read(buffer, 0, readBytes);
                                            
                                            actualSize += readLength;
                                            os.write(buffer, 0, readLength);
                                            remaingLength -= readLength;
                                        }
                                        os.flush();
                                    } catch (Exception e1) {
                                        PrintStack.logging(logger, e1);
                                    }
                                } else {
                                    // get osd
                                    OSDClient client = null; //OSDClientManager.getInstance().getOSDClient(DiskManager.getInstance().getOSDIP(objDiskId));
                                    if (client == null) {
                                        client = new OSDClient(DiskManager.getInstance().getOSDIP(objDiskId), OSDConfig.getInstance().getPort());
                                        isBorrowOsd = false;
                                    } else {
                                        isBorrowOsd = true;
                                    }
                                    String partRange = null;
                                    if (isRange) {
                                        partRange = objOffset + OSDConstants.DASH + (objOffset + objLength - 1);
                                    } else {
                                        partRange = OSDConstants.EMPTY_STRING;
                                    }
                                    if (remaingLength < objLength) {
                                        partRange = objOffset + OSDConstants.DASH + (objOffset + remaingLength - 1);
                                    }
                                    client.getPartInit(objPath, remaingLength, partRange, os);
                                    client.getPart();
                                    // if (isBorrowOsd) {
                                    //     OSDClientManager.getInstance().releaseOSDClient(client);
                                    // }
                                    client.close();
                                }
                                length -= saveRemaingLength;
                                if (length <= 0) {
                                    break;
                                }
                            }
                        }
                    } else {
                        while ((line = br.readLine()) != null) {
                            String[] infos = line.split(OSDConstants.COLON);
                            objDiskId = infos[0];
                            objPath = infos[1];
                            objSize = Long.parseLong(infos[2]);
                            if (infos.length > 3) {
                                String[] objRanges = infos[3].split(OSDConstants.DASH);
                                objOffset = Long.parseLong(objRanges[0]);
                                objLast = Long.parseLong(objRanges[1]);
                                objLength = objLast - objOffset + 1;
                                isRange = true;
                            } else {
                                objLength = objSize;
                                isRange = false;
                            }
                            remaingLength = objLength;
                            // check local path
                            if (DiskManager.getInstance().getLocalPath(objDiskId) != null) {
                                // get local
                                File partFile = new File(objPath);
                                try (FileInputStream fis = new FileInputStream(partFile)) {
                                    if (isRange) {
                                        fis.skip(objOffset);
                                    }
                                    while (remaingLength > 0) {
                                        readBytes = 0;
                                        if (remaingLength < OSDConstants.MAXBUFSIZE) {
                                            readBytes = (int)remaingLength;
                                        } else {
                                            readBytes = OSDConstants.MAXBUFSIZE;
                                        }
                                        readLength = fis.read(buffer, 0, readBytes);
                                        
                                        actualSize += readLength;
                                        os.write(buffer, 0, readLength);
                                        remaingLength -= readLength;
                                    }
                                    os.flush();
                                } catch (Exception e1) {
                                    PrintStack.logging(logger, e1);
                                }
                            } else {
                                // get osd
                                OSDClient client = null; //OSDClientManager.getInstance().getOSDClient(DiskManager.getInstance().getOSDIP(objDiskId));
                                if (client == null) {
                                    client = new OSDClient(DiskManager.getInstance().getOSDIP(objDiskId), OSDConfig.getInstance().getPort());
                                    isBorrowOsd = false;
                                } else {
                                    isBorrowOsd = true;
                                }
                                String partRange = null;
                                if (isRange) {
                                    partRange = infos[3];
                                } else {
                                    partRange = OSDConstants.EMPTY_STRING;
                                }
                                client.getPartInit(objPath, objSize, partRange, os);
                                client.getPart();
                                client.close();
                                // if (isBorrowOsd) {
                                //     OSDClientManager.getInstance().releaseOSDClient(client);
                                // }
                            }
                        }
                    }
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                }
            } else {
                logger.error("not found multipart file : " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }
            // logger.debug(OSDConstants.LOG_OSD_SERVER_GET_INFO, path, objId, versionId, sourceRange);
    
            // byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            // File file = new File(KsanUtils.makeObjPath(path, objId, versionId));

            // if (file.exists()) {
            //     try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            //         String line = null;
            //         String objDiskId = null;
            //         String objPath = null;
            //         long objSize = 0L;
            //         long objOffset = 0L;
            //         long objLast = 0L;
            //         long objLength = 0L;
            //         long remaingLength = 0L;
            //         int readBytes = 0;
            //         int readLength = 0;
            //         boolean isRange = false;
    
            //         // check range
            //         if (!Strings.isNullOrEmpty(sourceRange)) {
            //             long accOffset = 0L;
            //             br.mark(0);
            //             String[] ranges = sourceRange.split(OSDConstants.SLASH);
            //             for (String range : ranges) {
            //                 String[] rangeParts = range.split(OSDConstants.COMMA);
            //                 long offset = Longs.tryParse(rangeParts[0]);
            //                 long length = Longs.tryParse(rangeParts[1]);
    
            //                 long newOffset = 0L;
            //                 long newLength = 0L;
            //                 br.reset();
            //                 while ((line = br.readLine()) != null) {
            //                     String[] infos = line.split(OSDConstants.COLON);
            //                     objDiskId = infos[0];
            //                     objPath = infos[1];
            //                     objSize = Long.parseLong(infos[2]);
            //                     if (infos.length > 3) {
            //                         String[] objRanges = infos[3].split(OSDConstants.DASH);
            //                         objOffset = Long.parseLong(objRanges[0]);
            //                         objLast = Long.parseLong(objRanges[1]);
            //                         objLength = objLast - objOffset + 1;
            //                         isRange = true;
            //                     } else {
            //                         objLength = objSize;
            //                         isRange = false;
            //                     }
    
            //                     if (accOffset < offset) {
            //                         if (accOffset + objLength <= offset) {
            //                             accOffset += objLength;
            //                             continue;
            //                         }
            //                     }
                                
            //                     // check local path
            //                     if (DiskManager.getInstance().getLocalPath(objDiskId) != null) {
            //                         // get local
            //                         File partFile = new File(objPath);
            //                         try (FileInputStream fis = new FileInputStream(partFile)) {
            //                             if (isRange) {
            //                                 fis.skip(objOffset);
            //                             }
            //                             while (remaingLength > 0) {
            //                                 readBytes = 0;
            //                                 if (remaingLength < OSDConstants.MAXBUFSIZE) {
            //                                     readBytes = (int)remaingLength;
            //                                 } else {
            //                                     readBytes = OSDConstants.MAXBUFSIZE;
            //                                 }
            //                                 readLength = fis.read(buffer, 0, readBytes);

            //                                 socket.getOutputStream().write(buffer, 0, readLength);
            //                                 remaingLength -= readLength;
            //                             }
            //                         } catch (Exception e1) {
            //                             PrintStack.logging(logger, e1);
            //                         }
            //                     } else {
            //                         // get osd
            //                         OSDClient client = new OSDClient(DiskManager.getInstance().getOSDIP(objDiskId), OSDConfig.getInstance().getPort());
            //                         String partRange = null;
            //                         if (isRange) {
            //                             partRange = infos[3];
            //                         } else {
            //                             partRange = OSDConstants.EMPTY_STRING;
            //                         }
            //                         client.getPartInit(objPath, objSize, partRange, socket.getOutputStream());
            //                         client.getPart();
            //                     }
            //                 }
            //             }
            //         } else {
            //             while ((line = br.readLine()) != null) {
            //                 String[] infos = line.split(OSDConstants.COLON);
            //                 objDiskId = infos[0];
            //                 objPath = infos[1];
            //                 objSize = Long.parseLong(infos[2]);
            //                 if (infos.length > 3) {
            //                     String[] objRanges = infos[3].split(OSDConstants.DASH);
            //                     objOffset = Long.parseLong(objRanges[0]);
            //                     objLast = Long.parseLong(objRanges[1]);
            //                     objLength = objLast - objOffset + 1;
            //                     isRange = true;
            //                 } else {
            //                     objLength = objSize;
            //                     isRange = false;
            //                 }
            //                 remaingLength = objLength;
            //                 // check local path
            //                 if (DiskManager.getInstance().getLocalPath(objDiskId) != null) {
            //                     // get local
            //                     File partFile = new File(objPath);
            //                     try (FileInputStream fis = new FileInputStream(partFile)) {
            //                         if (isRange) {
            //                             fis.skip(objOffset);
            //                         }
            //                         while (remaingLength > 0) {
            //                             readBytes = 0;
            //                             if (remaingLength < OSDConstants.MAXBUFSIZE) {
            //                                 readBytes = (int)remaingLength;
            //                             } else {
            //                                 readBytes = OSDConstants.MAXBUFSIZE;
            //                             }
            //                             readLength = fis.read(buffer, 0, readBytes);
                                        
            //                             socket.getOutputStream().write(buffer, 0, readLength);
            //                             remaingLength -= readLength;
            //                         }
            //                     } catch (Exception e1) {
            //                         PrintStack.logging(logger, e1);
            //                     }
            //                 } else {
            //                     // get osd
            //                     OSDClient client = new OSDClient(DiskManager.getInstance().getOSDIP(objDiskId), OSDConfig.getInstance().getPort());
            //                     String partRange = null;
            //                     if (isRange) {
            //                         partRange = infos[3];
            //                     } else {
            //                         partRange = OSDConstants.EMPTY_STRING;
            //                     }
            //                     client.getPartInit(objPath, objSize, partRange, socket.getOutputStream());
            //                     client.getPart();
            //                 }
            //             }
            //         }
            //     } catch (Exception e) {
            //         PrintStack.logging(logger, e);
            //     }
            // }
        }
    
        private void completeMultipart(String[] headers) throws IOException, NoSuchAlgorithmException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_COMPLETE_MULTIPART_START);
            // String path = headers[OsdData.PATH_INDEX];
            // String objId = headers[OsdData.OBJID_INDEX];
            // String versionId = headers[OsdData.VERSIONID_INDEX];
            // String key = headers[OsdData.COMPLETE_MULTIPART_KEY_INDEX];
            // String replication = headers[OsdData.COMPLETE_MULTIPART_REPLICATION_INDEX];
            // String replicaDiskId = headers[OsdData.COMPLETE_MULTIPART_REPLICA_DISKID_INDEX];
            // String partInfos = headers[OsdData.COMPLETE_MULTIPART_PARTNOS_INDEX];
            // logger.debug(OSDConstants.LOG_OSD_SERVER_COMPLETE_MULTIPART_INFO, path, objId, versionId, key, partInfos);
            
            // byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            // MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5, new OpenSSL4JProvider());
            // long totalLength = 0L;
            // long existFileSize = 0L;
            // long putSize = 0L;
            // long calSize = 0L;
            // CtrCryptoOutputStream encryptOS = null;
            // // CtrCryptoInputStream encryptIS = null;
            // String eTag = "";
            
            // String[] arrayPartInfo = partInfos.split(OSDConstants.COMMA);

            // SortedMap<Integer, Part> listPart = new TreeMap<Integer, Part>();
            // for (int i = 0; i < arrayPartInfo.length; i++) {
            //     if (Strings.isNullOrEmpty(arrayPartInfo[i])) {
            //         break;
            //     }
            //     Part part = new Part();
            //     String[] info = arrayPartInfo[i].split(OSDConstants.SLASH);
            //     logger.debug("part no : {}, diskId : {}, size : {}", info[0], info[1], info[2]);
            //     part.setPartNumber(Integer.parseInt(info[0]));
            //     part.setPrimaryDiskId(info[1]);
            //     part.setPartSize(Long.parseLong(info[2]));
            //     listPart.put(part.getPartNumber(), part);
            // }

            // File tmpFile = null;
            // File file = null;
            // File trashFile = null;

            // if (OSDConfig.getInstance().isCacheDiskpath()) {
            //     file = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeObjPath(path, objId, versionId));
            //     tmpFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeTempPath(path, objId, versionId));
            //     trashFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeTrashPath(path, objId, versionId));
            // } else {
            //     file = new File(KsanUtils.makeObjPath(path, objId, versionId));
            //     tmpFile = new File(KsanUtils.makeTempPath(path, objId, versionId));
            //     trashFile = new File(KsanUtils.makeTrashPath(path, objId, versionId));
            // }
            // com.google.common.io.Files.createParentDirs(tmpFile);
            // com.google.common.io.Files.createParentDirs(file);

            // try (FileOutputStream tmpOut = new FileOutputStream(tmpFile)) {
            //     logger.debug("key : {}", key);
            //     if (key.equalsIgnoreCase(OSDConstants.STR_NULL)) {
            //         key = null;
            //     }
            //     if (!Strings.isNullOrEmpty(key)) {
            //         encryptOS = OSDUtils.initCtrEncrypt(tmpOut, key);
            //         // for each part object
            //         for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
            //             Map.Entry<Integer, Part> entry = it.next();
            //             String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getPrimaryDiskId());
            //             if (!Strings.isNullOrEmpty(partPath)) {
            //                 // part is in local disk
            //                 logger.debug("key : {}, part : {}, diskID : {}, part path : {}", key, entry.getKey(), entry.getValue().getPrimaryDiskId(), partPath);
            //                 File partFile = null;
            //                 if (OSDConfig.getInstance().isCacheDiskpath()) {
            //                     partFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makePartPath(partPath, objId, String.valueOf(entry.getValue().getPartNumber())));
            //                 } else {
            //                     partFile = new File(KsanUtils.makePartPath(partPath, objId, String.valueOf(entry.getValue().getPartNumber())));
            //                 }
    
            //                 try (FileInputStream fis = new FileInputStream(partFile)) {
            //                     // encryptIS = OSDUtils.initCtrDecrypt(fis, key);
            //                     int readLength = 0;
            //                     while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
            //                         totalLength += readLength;
            //                         encryptOS.write(buffer, 0, readLength);
            //                         // md5er.update(buffer, 0, readLength);
            //                     }
            //                     encryptOS.flush();
            //                 }
            //             } else {
            //                 partPath = DiskManager.getInstance().getPath(entry.getValue().getPrimaryDiskId());
            //                 String host = DiskManager.getInstance().getOSDIP(entry.getValue().getPrimaryDiskId());
            //                 OSDClient client = new OSDClient(host, OSDConfig.getInstance().getPort());
            //                 client.getPartInit(partPath, objId, String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), encryptOS/*, md5er*/);
            //                 totalLength += client.getPart();
            //             }
            //         }
            //     } else {
            //         // for each part object
            //         for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
            //             Map.Entry<Integer, Part> entry = it.next();
            //             String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getPrimaryDiskId());
            //             if (!Strings.isNullOrEmpty(partPath)) {
            //                 // part is in local disk
            //                 logger.debug("part : {}, diskID : {}, part path : {}", entry.getKey(), entry.getValue().getPrimaryDiskId(), partPath);
            //                 File partFile = null;
            //                 if (OSDConfig.getInstance().isCacheDiskpath()) {
            //                     partFile = new File(OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makePartPath(partPath, objId, String.valueOf(entry.getValue().getPartNumber())));
            //                 } else {
            //                     partFile = new File(KsanUtils.makePartPath(partPath, objId, String.valueOf(entry.getValue().getPartNumber())));
            //                 }
            //                 try (FileInputStream fis = new FileInputStream(partFile)) {
            //                     int readLength = 0;
            //                     while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
            //                         totalLength += readLength;
            //                         tmpOut.write(buffer, 0, readLength);
            //                         // md5er.update(buffer, 0, readLength);
            //                     }
            //                     tmpOut.flush();
            //                 }
            //                 partFile.delete();
            //             } else {
            //                 partPath = DiskManager.getInstance().getPath(entry.getValue().getPrimaryDiskId());
            //                 String host = DiskManager.getInstance().getOSDIP(entry.getValue().getPrimaryDiskId());
            //                 OSDClient client = new OSDClient(host, OSDConfig.getInstance().getPort());
            //                 client.getPartInit(partPath, objId, String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), tmpOut/*, md5er*/);
            //                 totalLength += client.getPart();
            //             }
            //         }
            //     }

            //     byte[] digest = md5er.digest();
            //     eTag = base16().lowerCase().encode(digest);
            // } catch (Exception e) {
            //     PrintStack.logging(logger, e);
            // }

            // logger.info("total length : {}", totalLength);

            // if (file.exists()) {
            //     File temp = new File(file.getAbsolutePath());
            //     logger.info("file is already exists : {}", file.getAbsolutePath());
            //     retryRenameTo(temp, trashFile);
            // }

            // KsanUtils.setAttributeFileReplication(tmpFile, replication, replicaDiskId);
            // retryRenameTo(tmpFile, file);
            // if (OSDConfig.getInstance().isCacheDiskpath()) {
            //     String fullPath = KsanUtils.makeObjPath(path, objId, versionId);
            //     Files.createSymbolicLink(Paths.get(fullPath), Paths.get(file.getAbsolutePath()));
            // }

            // sendData(eTag, totalLength);
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
                File partFile = new File(KsanUtils.makeTempPath(path, objId, partNo));
            
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

        private void putRange(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_START);
            String path = headers[OsdData.PATH_INDEX];
            String objId = headers[OsdData.OBJID_INDEX];
            String versionId = headers[OsdData.VERSIONID_INDEX];
            Long offsetLong = Longs.tryParse(headers[OsdData.PUT_RANGE_OFFSEET_INDEX]);
            Long lengthLong = Longs.tryParse(headers[OsdData.PUT_RANGE_LENGTH_INDEX]);
            long offset = offsetLong == null ? 0 : offsetLong;
            long length = lengthLong == null ? 0 : lengthLong;
            String replication = headers[OsdData.PUT_RANGE_REPLICATION_INDEX];
            String replicaDiskID = headers[OsdData.PUT_RANGE_REPLICA_DISK_ID_INDEX];
            // String key = headers[OsdData.PUT_RANGE_KEY_INDEX];
            String mode  = headers[OsdData.PUT_RANGE_MODE_INDEX];

            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_RANGE_INFO, path, objId, versionId, offset, length, replication, mode);

            boolean isNoDisk = false;
            if (mode != null) {
                if (mode.equals(OSDConstants.PERFORMANCE_MODE_NO_DISK)) {
                    isNoDisk = true;
                }
            }

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];

            RandomAccessFile randomFile = null;
            String filePath = null;

            if (OSDConfig.getInstance().isCacheDiskpath()) {
                filePath = OSDConfig.getInstance().getCacheDiskpath() + KsanUtils.makeObjPath(path, objId, versionId);
                // File linkFile = new File(KsanUtils.makeObjPath(path, objId, versionId));
            } else {
                filePath = KsanUtils.makeObjPath(path, objId, versionId);
            }
            randomFile = new RandomAccessFile(filePath, "rw");
            if (offset > 0) {
                randomFile.seek(offset);
            }

            int readLength = 0;
            long remainLength = length;
            int readMax = (int) (length < OSDConstants.MAXBUFSIZE ? length : OSDConstants.MAXBUFSIZE);

            while ((readLength = socket.getInputStream().read(buffer, 0, readMax)) != -1) {
                remainLength -= readLength;
                if (!isNoDisk) {
                    randomFile.write(buffer, 0, readLength);
                }
                if (remainLength <= 0) {
                    break;
                }
                readMax = (int) (remainLength < OSDConstants.MAXBUFSIZE ? remainLength : OSDConstants.MAXBUFSIZE);
            }
            randomFile.close();
            File file = new File(filePath);
            KsanUtils.setAttributeFileReplication(file, replication, replicaDiskID);
            
            if (OSDConfig.getInstance().isCacheDiskpath()) {
                String fullPath = KsanUtils.makeObjPath(path, objId, versionId);
                Files.createSymbolicLink(Paths.get(fullPath), Paths.get(file.getAbsolutePath()));
            }

            logger.info(OSDConstants.LOG_OSD_SERVER_PUT_SUCCESS_INFO, path, objId, versionId, length);
        }

        private void sendHeader(Socket socket, String header) throws IOException {
            // byte[] buffer = header.getBytes(Constants.CHARSET_UTF_8);
            // int size = buffer.length;
            
            // DataOutputStream so = new DataOutputStream(socket.getOutputStream());
            
            // so.writeInt(size);
            // so.write(buffer, 0, size);
            // so.flush();

            byte[] buffer = header.getBytes(Constants.CHARSET_UTF_8);
            String strLength = Integer.toString(buffer.length);
            byte[] lengthBuffer = strLength.getBytes(Constants.CHARSET_UTF_8);

            byte length = (byte)lengthBuffer.length;
            socket.getOutputStream().write(length);
            socket.getOutputStream().write(lengthBuffer, 0, length);
            
            socket.getOutputStream().write(buffer, 0, buffer.length);
            socket.getOutputStream().flush();
            logger.info("send header size : {}", buffer.length);
        }

        private void sendData(String ETag, long fileSize) throws IOException {
            String tail = OsdData.FILE + OsdData.DELIMITER + ETag + OsdData.DELIMITER + String.valueOf(fileSize);
            byte[] buffer = tail.getBytes(Constants.CHARSET_UTF_8);
            String strLength = Integer.toString(buffer.length);
            byte[] lengthBuffer = strLength.getBytes(Constants.CHARSET_UTF_8);

            byte length = (byte)lengthBuffer.length;
            socket.getOutputStream().write(length);
            socket.getOutputStream().write(lengthBuffer, 0, length);
            
            socket.getOutputStream().write(buffer, 0, buffer.length);
            socket.getOutputStream().flush();
            logger.debug("sendData : {}", tail);

            // int size = buffer.length;
            // logger.debug("sendData : {}", tail);
            // DataOutputStream so = new DataOutputStream(socket.getOutputStream());
            // so.writeInt(size);
            // so.write(buffer, 0, size);
            // so.flush();
        }      

        private OsdData receiveData(Socket socket) throws IOException {
            int length = socket.getInputStream().read();
            if (length == -1) {
                logger.info("socket {} EOF ...", socket.getRemoteSocketAddress().toString());
                return null;
            }
            byte[] lengthBuffer = new byte[length];
            socket.getInputStream().read(lengthBuffer, 0, length);
            String strLength = new String(lengthBuffer, StandardCharsets.UTF_8);

            length = Integer.parseInt(strLength);
            byte[] buffer = new byte[length];
            socket.getInputStream().read(buffer, 0, length);
            String result = new String(buffer, 0, length, StandardCharsets.UTF_8);
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
            // DataInputStream si = new DataInputStream(socket.getInputStream());
            // int size = si.readInt();
            // byte[] buffer = new byte[size];
            // si.read(buffer, 0, size);
            // String result = new String(buffer, 0, size);
            // String[] ArrayResult = result.split(OsdData.DELIMITER, -1);

            // OsdData data = new OsdData();
            // switch (ArrayResult[0]) {
            // case OsdData.FILE:
            //     data.setETag(ArrayResult[1]);
            //     data.setFileSize(Long.parseLong(ArrayResult[2]));
            //     return data;
            // default:
            //     logger.error(OSDConstants.LOG_OSD_SERVER_UNKNOWN_DATA, ArrayResult[1]);
            // }

            // return null;
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
            while (!serviceEmptyTrash.isTerminated()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
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
        logger.info("start ec thread, interval : {} ms", OSDConfig.getInstance().getECCheckInterval());
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