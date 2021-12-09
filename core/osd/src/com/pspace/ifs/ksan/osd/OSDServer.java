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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
    private static String objDir;
    private static String trashDir;
    private static String writeTempDir;
    private static boolean isRunning;
    private static DISKPOOLLIST diskpoolList;

    public static void main(String[] args) {
        OSDServer server = new OSDServer();
        server.start();
    }

    public void start() {
        logger.debug(OSDConstants.LOG_OSD_SERVER_START);
        OSDConfig config = new OSDConfig(OSDConstants.CONFIG_PATH);
        try {
            config.configure();
        } catch (URISyntaxException e) {
            logger.error(e.getMessage());
            System.exit(-1);
        }
        
        File file = new File(OSDConstants.PID_PATH);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
            int pid = 0;

            java.lang.management.RuntimeMXBean runtime = 
            java.lang.management.ManagementFactory.getRuntimeMXBean();
            java.lang.reflect.Field jvm;
            try {
                jvm = runtime.getClass().getDeclaredField(OSDConstants.JVM);
                jvm.setAccessible(true);
                sun.management.VMManagement mgmt =  (sun.management.VMManagement) jvm.get(runtime);
                java.lang.reflect.Method pid_method =  
                mgmt.getClass().getDeclaredMethod(OSDConstants.GET_PROCESS_ID);
                pid_method.setAccessible(true);

                pid = (Integer) pid_method.invoke(mgmt);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            logger.debug(OSDConstants.LOG_OSD_SERVER_PID, pid);
            fw.write(String.valueOf(pid));
            fw.flush();
            fw.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            System.exit(-1);
        }

        int poolSize = Integer.parseInt(config.getPoolSize());
        ip = config.getIP();
        port = Integer.parseInt(config.getPort());
        objDir = config.getObjDir();
        trashDir = config.getTrashDir();
        writeTempDir = config.getWriteTempDir();
        
        try {
            logger.debug(OSDConstants.LOG_OSD_SERVER_CONFIGURE_DISPOOLS);
			XmlMapper xmlMapper = new XmlMapper();
			InputStream is = new FileInputStream(OSDConstants.DISKPOOL_CONF_PATH);
			byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
			try {
				is.read(buffer, 0, OSDConstants.MAXBUFSIZE);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
			String xml = new String(buffer);
			
			logger.debug(xml);
			diskpoolList = xmlMapper.readValue(xml, DISKPOOLLIST.class);
			logger.debug(OSDConstants.LOG_OSD_SERVER_DISK_POOL_INFO, diskpoolList.getDiskpool().getId(), diskpoolList.getDiskpool().getName());
			logger.debug(OSDConstants.LOG_OSD_SERVER_SERVER_SIZE, diskpoolList.getDiskpool().getServers().size());
		} catch (JsonProcessingException | FileNotFoundException e) {
			logger.error(e.getMessage());
		} 

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
    
                    case OSDConstants.COPY:
                        copy(headers);
                        break;
    
                    case OSDConstants.PART:
                        part(headers);
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
                logger.error(e.getMessage());
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
            File file = new File(makeObjPath(path, objId, versionId));
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
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_INFO, path, objId, versionId, length);

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File file = new File(makeObjPath(path, objId, versionId));
            File tmpFile = new File(makeTempPath(path, objId, versionId));
            File trashFile = new File(makeTrashPath(path, objId, versionId));

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

            retryRenameTo(file, trashFile);
            retryRenameTo(tmpFile, file);
            
            logger.debug(OSDConstants.LOG_OSD_SERVER_PUT_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_PUT_SUCCESS_INFO, path, objId, versionId, length);
        }
    
        private void delete(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_START);
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String versionId = headers[OSDConstants.VERSIONID_INDEX];
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_INFO, path, objId, versionId);

            File file = new File(makeObjPath(path, objId, versionId));
            File trashFile = new File(makeTrashPath(path, objId, versionId));
            
            retryRenameTo(file, trashFile);
            logger.debug(OSDConstants.LOG_OSD_SERVER_DELETE_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_DELETE_SUCCESS_INFO, path, objId, versionId);
        }
    
        private void copy(String[] headers) throws IOException, NoSuchAlgorithmException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_COPY_START);
            String srcPath = headers[OSDConstants.PATH_INDEX];
            String srcObjId = headers[OSDConstants.OBJID_INDEX];
            String srcVersionId = headers[OSDConstants.VERSIONID_INDEX];
            String destPath = headers[OSDConstants.DEST_PATH_INDEX];
            String destObjId = headers[OSDConstants.DEST_OBJID_INDEX];
            String destVersionId = headers[OSDConstants.DEST_VERSIONID_INDEX];

            logger.debug(OSDConstants.LOG_OSD_SERVER_COPY_INFO, srcPath, srcObjId, srcVersionId, destPath, destObjId, destVersionId);

            String destIP = findOSD(destPath);
            if (destIP == null) {
                logger.error(OSDConstants.LOG_OSD_SERVER_CAN_NOT_FIND_OSD_IP, destPath);
                return;
            }

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File srcFile = new File(makeObjPath(srcPath, srcObjId, srcVersionId));
            try (FileInputStream fis = new FileInputStream(srcFile)) {
                if (ip.equals(destIP)) {
                    File file = new File(makeObjPath(destPath, destObjId, destVersionId));
                    File tmpFile = new File(makeTempPath(destPath, destObjId, destVersionId));
        
                    com.google.common.io.Files.createParentDirs(file);
                    com.google.common.io.Files.createParentDirs(tmpFile);
                    try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                        int readLength = 0;
                        while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
                            fos.write(buffer, 0, readLength);
                        }
                        fos.flush();
                    }
                    retryRenameTo(tmpFile, file);
                } else {
                    try (Socket destSocket = new Socket(destIP, port)) {
                        String header = OSDConstants.PUT + OSDConstants.DELIMITER + destPath + OSDConstants.DELIMITER + destObjId + OSDConstants.DELIMITER + destVersionId + OSDConstants.DELIMITER + String.valueOf(srcFile.length());
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
    
        private void part(String[] headers) throws IOException {
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_START);
            String path = headers[OSDConstants.PATH_INDEX];
            String objId = headers[OSDConstants.OBJID_INDEX];
            String partNo = headers[OSDConstants.PARTNO_INDEX];
            long length = Longs.tryParse(headers[OSDConstants.PUT_LENGTH_INDEX]);
            logger.debug(OSDConstants.LOG_OSD_SERVER_PART_INFO, path, objId, partNo, length);

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            File tmpFile = new File(makeTempPath(path, objId, partNo));

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
            File srcFile = new File(makeObjPath(srcPath, srcObjId, srcVersionId));
            long remainLength = 0L;
            int readLength = 0;
            int readBytes;
            String eTag = "";

            OSDData data = null;

            try (FileInputStream fis = new FileInputStream(srcFile)) {
                if (ip.equals(destIP)) {
                    File tmpFile = new File(makeTempPath(destPath, destObjId, destPartNo));
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
                } else {
                    try (Socket destSocket = new Socket(destIP, port)) {
                        String header = OSDConstants.PART + OSDConstants.DELIMITER + destPath + OSDConstants.DELIMITER + destObjId + OSDConstants.DELIMITER + destPartNo + OSDConstants.DELIMITER + String.valueOf(srcFile.length());
                        logger.debug(OSDConstants.LOG_OSD_SERVER_PART_COPY_RELAY_OSD, destIP, header);
                        sendHeader(destSocket, header);
    
                        if (Strings.isNullOrEmpty(copySourceRange)) {
                            while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
                                destSocket.getOutputStream().write(buffer, 0, readLength);
                                md5er.update(buffer, 0, readLength);
                            }
                            destSocket.getOutputStream().flush();
                        } else {
                            String[] ranges = copySourceRange.split(OSDConstants.SLASH);
                            for (String range : ranges) {
                                String[] rangeParts = range.split(OSDConstants.COMMA);
                                long offset = Longs.tryParse(rangeParts[0]);
                                long length = Longs.tryParse(rangeParts[1]);
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
                                    destSocket.getOutputStream().write(buffer, 0, readLength);
                                    md5er.update(buffer, 0, readLength);
                                    remainLength -= readLength;
                                }
                                destSocket.getOutputStream().flush();
                            }
                        }
                        
                        byte[] digest = md5er.digest();
                        eTag = base16().lowerCase().encode(digest);
    
                        data = receiveData(destSocket);
                        if (!eTag.equals(data.getETag())) {
                            logger.error(OSDConstants.LOG_OSD_SERVER_DIFFERENCE_ETAG, eTag, data.getETag());
                        }
                    }
                }
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

            File file = new File(makeObjPath(path, objId, versionId));
            File tmpFile = new File(makeTempPath(path, objId, versionId));
            File trashFile = new File(makeTrashPath(path, objId, versionId));

            byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
            MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5);
            long totalLength = 0L;

            try (FileOutputStream tmpOut = new FileOutputStream(tmpFile)) {
                com.google.common.io.Files.createParentDirs(file);
                com.google.common.io.Files.createParentDirs(tmpFile);
                
                for (String partNo : arrayPartNos) {
                    File partFile = new File(makeTempPath(path, objId, partNo));
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
            
            retryRenameTo(file, trashFile);
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
                File partFile = new File(makeTempPath(path, objId, partNo));
            
                if (!partFile.delete()) {
                    logger.error(OSDConstants.LOG_OSD_SERVER_FAILED_FILE_DELETE, partFile.getName());
                }
            }
            logger.debug(OSDConstants.LOG_OSD_SERVER_ABORE_MULTIPART_END);
            logger.info(OSDConstants.LOG_OSD_SERVER_ABORE_MULTIPART_SUCCESS_INFO, path, objId, partNos);
        }
    
        private String makeDirectoryName(String objId) {
            byte[] path = new byte[6];
            byte[] byteObjId = objId.getBytes();
    
            path[0] = OSDConstants.CHAR_SLASH;
            int index = 1;
            
            path[index++] = byteObjId[0];
            path[index++] = byteObjId[1];
            path[index++] = OSDConstants.CHAR_SLASH;
            path[index++] = byteObjId[2];
            path[index] = byteObjId[3];

            return new String(path);
        }
    
        private String makeObjPath(String path, String objId, String versionId) {
            String fullPath = path + OSDConstants.SLASH + objDir + makeDirectoryName(objId) + OSDConstants.SLASH + objId + OSDConstants.UNDERSCORE + versionId;
            return fullPath;
        }
    
        private String makeTempPath(String path, String objId, String versionId) {
            String fullPath = path + OSDConstants.SLASH + writeTempDir + OSDConstants.SLASH + objId + OSDConstants.UNDERSCORE + versionId;
            return fullPath;
        }
    
        private String makeTrashPath(String path, String objId, String versionId) {
            String uuid = UUID.randomUUID().toString();
            String fullPath = path + OSDConstants.SLASH + trashDir + OSDConstants.SLASH + objId + OSDConstants.UNDERSCORE + versionId + uuid;
            return fullPath;
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
