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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.pspace.ifs.ksan.osd.DISKPOOLLIST.DISKPOOL.SERVER;
import com.pspace.ifs.ksan.osd.DISKPOOLLIST.DISKPOOL.SERVER.DISK;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSDServer {
    private final static Logger logger = LoggerFactory.getLogger(OSDServer.class);
    private static String ip;
    private static int port;
    private static boolean isRunning;
    private static DISKPOOLLIST diskpoolList;

    public static void main(String[] args) {
        OSDServer server = new OSDServer();
        server.start();
    }

    public void start() {
        logger.debug(OSDConstants.LOG_OSD_SERVER_START);
        
        OSDUtils.getInstance().writePID();

        int poolSize = OSDUtils.getInstance().getPoolSize();
        ip = OSDUtils.getInstance().getIP();
        port = OSDUtils.getInstance().getPort();
        
        diskpoolList = OSDUtils.getInstance().getDiskPoolList();

        ScheduledExecutorService serviceEC = Executors.newSingleThreadScheduledExecutor();
        serviceEC.scheduleAtFixedRate(new DoECPriObject(), OSDUtils.getInstance().getECScheduleMinutes(), OSDUtils.getInstance().getECScheduleMinutes(), TimeUnit.MINUTES);

        ScheduledExecutorService serviceEmptyTrash = Executors.newSingleThreadScheduledExecutor();
        serviceEmptyTrash.scheduleAtFixedRate(new DoEmptyTrash(), OSDUtils.getInstance().getTrashScheduleMinutes(), OSDUtils.getInstance().getTrashScheduleMinutes(), TimeUnit.MINUTES);

        ScheduledExecutorService serviceMoveCacheToDisk = Executors.newSingleThreadScheduledExecutor();
        serviceMoveCacheToDisk.scheduleAtFixedRate(new DoMoveCacheToDisk(), OSDUtils.getInstance().getCacheScheduleMinutes(), OSDUtils.getInstance().getCacheScheduleMinutes(), TimeUnit.MINUTES);

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
                while (true) {
                    int length = di.readInt();
                    di.read(buffer, 0, length);
                    String indicator = new String(buffer, 0, OSDConstants.INDICATOR_SIZE);
                    String header = new String(buffer, 0, length);
                    String[] headers = header.split(OSDConstants.DELIMITER, -1);
    
                    switch (indicator) {
                    case OSDConstants.GET:
                        get(headers);
                        break;
                    
                    case OSDConstants.PUT:
                        put(headers);
                        break;
                    
                    case OSDConstants.DELETE:
                        delete(headers);
                        break;

                    case OSDConstants.DELETE_REPLICA:
                        deleteReplica(headers);
                        break;
    
                    case OSDConstants.COPY:
                        copy(headers);
                        break;

                    case OSDConstants.GET_PART:
                        getPart(headers);
    
                    case OSDConstants.PART:
                        part(headers);
                        break;

                    case OSDConstants.DELETE_PART:
                        deletePart(headers);
                        break;
    
                    case OSDConstants.PART_COPY:
                        partCopy(headers);
                        break;
    
                    case OSDConstants.COMPLETE_MULTIPART:
                        completeMultipart(headers);
                        break;
    
                    case OSDConstants.ABORT_MULTIPART:
                        abortMultipart(headers);
                        break;
    
                    case OSDConstants.STOP:
                        isRunning = false;
                        break;
    
                    default:
                        logger.error(OSDConstants.LOG_OSD_SERVER_UNKNOWN_INDICATOR, indicator);
                    }
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                if (e.getMessage() != null) {
                    logger.error(e.getMessage());
                }

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
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String versionId = headers[OSDConstants.VERSIONID_INDEX];
            String sourceRange = headers[OSDConstants.SOURCE_RANGE_INDEX];
            long readTotal = 0L;
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_INFO, path, objId, versionId, sourceRange);
    
            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
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
            socket.getOutputStream().flush();
            
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_END, readTotal);
            logger.info(OSDConstants.LOG_OSD_SERVER_GET_SUCCESS_INFO, path, objId, versionId, sourceRange);
        }
    
        private void put(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_START);
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String versionId = headers[OSDConstants.VERSIONID_INDEX];
            long length = Longs.tryParse(headers[OSDConstants.PUT_LENGTH_INDEX]);
            String replication = headers[OSDConstants.PUT_REPLICATION_INDEX];
            String replicaDiskID = headers[OSDConstants.PUT_REPLICA_DISK_ID_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_INFO, path, objId, versionId, length, replication);

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = null;
            File tmpFile = null;
            File trashFile = null;

            if (OSDUtils.getInstance().getCacheDisk() != null && length <= (OSDUtils.getInstance().getCacheFileSize() * OSDConstants.MEGABYTES)) {
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
                while ((readLength = socket.getInputStream().read(buffer, 0, readMax)) > 0) {
                    remainLength -= readLength;
                    fos.write(buffer, 0, readLength);
                    if (remainLength <= 0) {
                        break;
                    }
                    readMax = (int) (remainLength < OSDConstants.MAXBUFSIZE ? remainLength : OSDConstants.MAXBUFSIZE);
                }
                fos.flush();
            }

            if (file.exists()) {
                retryRenameTo(file, trashFile);
            }
            OSDUtils.getInstance().setAttributeFileReplication(tmpFile, replication, replicaDiskID);
            retryRenameTo(tmpFile, file);
            if (OSDUtils.getInstance().getCacheDisk() != null && length <= (OSDUtils.getInstance().getCacheFileSize() * OSDConstants.MEGABYTES)) {
                String fullPath = OSDUtils.getInstance().makeObjPath(path, objId, versionId);
                String command = "ln -s " + file.getAbsolutePath() + " " + fullPath;

                logger.debug("{}", command);
                Process p = Runtime.getRuntime().exec(command);
                int exitCode;
                try {
                    exitCode = p.waitFor();
                    logger.info("ln : {}", exitCode);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_PUT_SUCCESS_INFO, path, objId, versionId, length);
        }
    
        private void delete(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_START);
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String versionId = headers[OSDConstants.VERSIONID_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_INFO, path, objId, versionId);
            boolean isCache = false;
            File file = null;
            File trashFile = null;
            if (OSDUtils.getInstance().getCacheDisk() != null) {
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
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String partNo = headers[OSDConstants.PARTNO_INDEX];
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
            String path = headers[OSDConstants.PATH_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_REPLICA_PATH, path);

            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
            
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_REPLICA_END);
        }
    
        private void copy(String[] headers) throws IOException, NoSuchAlgorithmException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_COPY_START);
            String srcPath = headers[OSDConstants.PATH_INDEX];
            String srcObjId = headers[OSDConstants.OBJID_INDEX];
            String srcVersionId = headers[OSDConstants.VERSIONID_INDEX];
            String destPath = headers[OSDConstants.DEST_PATH_INDEX];
            String destObjId = headers[OSDConstants.DEST_OBJID_INDEX];
            String destVersionId = headers[OSDConstants.DEST_VERSIONID_INDEX];
            String replication = headers[OSDConstants.COPY_REPLICATION_INDED];
            String replicaDiskID = headers[OSDConstants.COPY_REPLICA_DISK_ID_INDEX];

            logger.debug(OSDConstants.LOG_OSD_SERVER_COPY_INFO, srcPath, srcObjId, srcVersionId, destPath, destObjId, destVersionId);

            String destIP = findOSD(destPath);
            if (destIP == null) {
                logger.error(OSDConstants.LOG_OSD_SERVER_CAN_NOT_FIND_OSD_IP, destPath);
                return;
            }

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File srcFile = new File(OSDUtils.getInstance().makeObjPath(srcPath, srcObjId, srcVersionId));
            try (FileInputStream fis = new FileInputStream(srcFile)) {
                if (ip.equals(destIP)) {
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
                        retryRenameTo(file, trashFile);
                    }
                    OSDUtils.getInstance().setAttributeFileReplication(file, replication, replicaDiskID);
                    retryRenameTo(tmpFile, file);
                } else {
                    try (Socket destSocket = new Socket(destIP, port)) {
                        String header = OSDConstants.PUT 
                                        + OSDConstants.DELIMITER + destPath 
                                        + OSDConstants.DELIMITER + destObjId 
                                        + OSDConstants.DELIMITER + destVersionId 
                                        + OSDConstants.DELIMITER + String.valueOf(srcFile.length())
                                        + OSDConstants.DELIMITER + replication
                                        + OSDConstants.DELIMITER + replicaDiskID;
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
    
                        OSDData data = receiveData(destSocket);
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
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String partNo = headers[OSDConstants.PART_NO_INDEX];

            long readTotal = 0L;
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_PART_INFO, path, objId, partNo);
    
            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = new File(OSDUtils.getInstance().makeTempPath(path, objId, partNo));
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
            socket.getOutputStream().flush();
            if (!file.delete()) {
                logger.error(OSDConstants.LOG_OSD_SERVER_FAILED_FILE_DELETE, file.getName());
            }
            
            logger.debug(OSDConstants.LOG_OSD_SERVER_GET_PART_END, readTotal);
            logger.info(OSDConstants.LOG_OSD_SERVER_GET_PART_SUCCESS_INFO, path, objId, partNo);
        }

        private void part(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_START);
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String partNo = headers[OSDConstants.PARTNO_INDEX];
            long length = Longs.tryParse(headers[OSDConstants.PUT_LENGTH_INDEX]);
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_INFO, path, objId, partNo, length);

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File tmpFile = new File(OSDUtils.getInstance().makeTempPath(path, objId, partNo));

            com.google.common.io.Files.createParentDirs(tmpFile);
            try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                int readLength = 0;
                long remainLength = length;
                int readMax = (int) (length < OSDConstants.MAXBUFSIZE ? length : OSDConstants.MAXBUFSIZE);
                while ((readLength = socket.getInputStream().read(buffer, 0, readMax)) > 0) {
                    remainLength -= readLength;
                    fos.write(buffer, 0, readLength);
                    if (remainLength <= 0) {
                        break;
                    }
                    readMax = (int) (remainLength < OSDConstants.MAXBUFSIZE ? remainLength : OSDConstants.MAXBUFSIZE);
                }
                fos.flush();
            }

            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_PART_SUCCESS_INFO, path, objId, partNo, length);
        }
    
        private void partCopy(String[] headers) throws IOException, NoSuchAlgorithmException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_COPY_START);
            String srcPath = headers[OSDConstants.PATH_INDEX];
            String srcObjId = headers[OSDConstants.OBJID_INDEX];
            String srcVersionId = headers[OSDConstants.VERSIONID_INDEX];
            String destPath = headers[OSDConstants.DEST_PATH_INDEX];
            String destObjId = headers[OSDConstants.DEST_OBJID_INDEX];
            String destPartNo = headers[OSDConstants.DEST_PARTNO_INDEX];
            String copySourceRange = headers[OSDConstants.SRC_RANAGE_INDEX];

            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_COPY_INFO, srcPath, srcObjId, srcVersionId, destPath, destObjId, destPartNo, copySourceRange);

            String destIP = findOSD(destPath);
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

            OSDData data = null;

            try (FileInputStream fis = new FileInputStream(srcFile)) {
                File tmpFile = new File(OSDUtils.getInstance().makeTempPath(destPath, destObjId, destPartNo));
                com.google.common.io.Files.createParentDirs(tmpFile);
                try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                    data = new OSDData();
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
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String versionId = headers[OSDConstants.VERSIONID_INDEX];
            String partNos = headers[OSDConstants.COMPLETE_MULTIPART_PARTNOS];
            logger.debug(OSDConstants.LOG_OSD_SERVER_COMPLETE_MULTIPART_INFO, path, objId, partNos);
            String[] arrayPartNos = partNos.split(OSDConstants.COMMA);
            Arrays.sort(arrayPartNos);

            File file = new File(OSDUtils.getInstance().makeObjPath(path, objId, versionId));
            File tmpFile = new File(OSDUtils.getInstance().makeTempPath(path, objId, versionId));
            File trashFile = new File(OSDUtils.getInstance().makeTrashPath(path, objId, versionId));

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5);
            long totalLength = 0L;

            try (FileOutputStream tmpOut = new FileOutputStream(tmpFile)) {
                com.google.common.io.Files.createParentDirs(file);
                com.google.common.io.Files.createParentDirs(tmpFile);
                
                for (String partNo : arrayPartNos) {
                    File partFile = new File(OSDUtils.getInstance().makeTempPath(path, objId, partNo));
                    try (FileInputStream fis = new FileInputStream(partFile)) {
                        int readLength = 0;
                        while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
                            totalLength += readLength;
                            tmpOut.write(buffer, 0, readLength);
                            md5er.update(buffer, 0, readLength);
                        }
                        tmpOut.flush();
                        if (!partFile.delete()) {
                            logger.error(OSDConstants.LOG_OSD_SERVER_FAILED_FILE_DELETE, partFile.getName());
                        }
                    }
                }
            }
            if (file.exists()) {
                retryRenameTo(file, trashFile);
            }
            
            retryRenameTo(tmpFile, file);

            byte[] digest = md5er.digest();
			String eTag = base16().lowerCase().encode(digest);

            sendData(eTag, totalLength);
            logger.debug(OSDConstants.LOG_OSD_SERVER_COMPLETE_MULTIPART_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_COMPLETE_MULTIPART_SUCCESS_INFO, path, objId, partNos);
        }
    
        private void abortMultipart(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_ABORE_MULTIPART_START);
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String partNos = headers[OSDConstants.ABORT_MULTIPART_PARTNOS];
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

        private String findOSD(String path) {
            for (SERVER server : diskpoolList.getDiskpool().getServers()) {
                for (DISK disk : server.getDisks()) {
                    if (path.equals(disk.getPath())) {
                        return server.getIp();
                    }
                }
            }

            return null;
        }

        private void sendHeader(Socket socket, String header) throws IOException {
            byte[] buffer = header.getBytes(OSDConstants.CHARSET_UTF_8);
            int size = buffer.length;
            
            DataOutputStream so = new DataOutputStream(socket.getOutputStream());
            
            so.writeInt(size);
            so.write(buffer, 0, size);
            so.flush();
        }

        private void sendData(String ETag, long fileSize) throws IOException {
            String tail = OSDConstants.FILE + OSDConstants.DELIMITER + ETag + OSDConstants.DELIMITER + String.valueOf(fileSize);
            byte[] buffer = tail.getBytes(OSDConstants.CHARSET_UTF_8);
            int size = buffer.length;
            
            DataOutputStream so = new DataOutputStream(socket.getOutputStream());
            
            so.writeInt(size);
            so.write(buffer, 0, size);
            so.flush();
        }      

        private OSDData receiveData(Socket socket) throws IOException {
            DataInputStream si = new DataInputStream(socket.getInputStream());
            int size = si.readInt();
            byte[] buffer = new byte[size];
            si.read(buffer, 0, size);
            String result = new String(buffer, 0, size);
            String[] ArrayResult = result.split(OSDConstants.DELIMITER, -1);

            OSDData data = new OSDData();
            switch (ArrayResult[0]) {
            case OSDConstants.FILE:
                data.setETag(ArrayResult[1]);
                data.setFileSize(Long.parseLong(ArrayResult[2]));
                return data;
            default:
                logger.error(OSDConstants.LOG_OSD_SERVER_UNKNOWN_DATA, ArrayResult[1]);
            }

            return null;
        }

        private void retryRenameTo(File tempFile, File destFile) throws IOException {
            if (tempFile.exists()) {
                for (int i = 0; i < OSDConstants.RETRY_COUNT; i++) {
                    if (tempFile.renameTo(destFile)) {
                        return;
                    }
                }
                logger.error(OSDConstants.LOG_OSD_SERVER_FAILED_FILE_RENAME, destFile.getName());
            }
        }
    }
}
