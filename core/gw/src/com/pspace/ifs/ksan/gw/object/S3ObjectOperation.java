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
package com.pspace.ifs.ksan.gw.object;

import static com.google.common.io.BaseEncoding.base16;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.multipart.Part;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagerHelper;
import com.pspace.ifs.ksan.gw.object.osdclient.OSDClient;
import com.pspace.ifs.ksan.gw.object.osdclient.OSDClientManager;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWDiskConfig;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.osd.OSDData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3ObjectOperation {
    private Metadata objMeta;
    private S3Metadata s3Meta;
    private S3Parameter s3Parameter;
    private String versionId;
    private S3ServerSideEncryption s3ServerSideEncryption;
    private ObjManager objManager;
    private static final Logger logger = LoggerFactory.getLogger(S3ObjectOperation.class);

    public S3ObjectOperation(Metadata objMeta, S3Metadata s3Meta, S3Parameter s3Parameter, String versionId, S3ServerSideEncryption s3ServerSideEncryption) {
        this.objMeta = objMeta;
        this.s3Meta = s3Meta;
        this.s3Parameter = s3Parameter;
        this.versionId = versionId;
        this.s3ServerSideEncryption = s3ServerSideEncryption;
    }

    private String getDirectoryBlobSuffix(String key) {
		if (key.endsWith(GWConstants.DIRECTORY_SUFFIX)) {
			return GWConstants.DIRECTORY_SUFFIX;
		}
		return null;
	}

    public void getObject(S3Range s3Range) throws Exception {       
        OSDClient client = null;
        String sourceRange = "";
        long actualSize = 0L;
        long fileSize = objMeta.getSize();

        if (GWConfig.getPerformanceMode().equals(GWConstants.PERFORMANCE_MODE_NO_REPLICA)) {
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int sendSize = 0;
            long remainingLength = fileSize;

            while (remainingLength > 0) {
                if (remainingLength > GWConstants.MAXBUFSIZE) {
                    sendSize = GWConstants.MAXBUFSIZE;
                } else {
                    sendSize = (int)remainingLength;
                }
                s3Parameter.getResponse().getOutputStream().write(buffer, 0, sendSize);
                remainingLength -= sendSize;
            }
        }

        if (s3Range != null && s3Range.getListRange().size() > 0) {
            fileSize = 0L;
            for (S3Range.Range range : s3Range.getListRange()) {
                if (Strings.isNullOrEmpty(sourceRange)) {
                    sourceRange = String.valueOf(range.getOffset()) + GWConstants.COMMA + String.valueOf(range.getLength());
                } else {
                    sourceRange += GWConstants.SLASH + String.valueOf(range.getOffset()) + GWConstants.COMMA + String.valueOf(range.getLength());
                }
                fileSize += range.getLength();
            }
        }
        
        try {
            if (objMeta.getReplicaCount() > 1) {
                logger.debug("bucket : {}, object : {}", objMeta.getBucket(), objMeta.getPath());
                logger.debug("primary disk id : {}, osd ip : {}", objMeta.getPrimaryDisk().getId(), objMeta.getPrimaryDisk().getOsdIp());
                logger.debug("replica disk id : {}, osd ip : {}", objMeta.getReplicaDisk().getId(), objMeta.getReplicaDisk().getOsdIp());
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    actualSize = getObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), sourceRange);
                    s3Parameter.addResponseSize(actualSize);
                } else if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    actualSize = getObjectLocal(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), sourceRange);
                    s3Parameter.addResponseSize(actualSize);
                } else {
                    try {
                        client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        client.getInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId(), fileSize, sourceRange, s3Parameter.getResponse().getOutputStream());
                        actualSize = client.get();
                        logger.debug("end ..... client get : actualSize : {}", actualSize);
                        OSDClientManager.getInstance().returnOSDClient(client);
                        s3Parameter.addResponseSize(actualSize);
                    } catch (Exception e) {
                        client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        client.getInit(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId(), fileSize, sourceRange, s3Parameter.getResponse().getOutputStream());
                        actualSize = client.get();
                        OSDClientManager.getInstance().returnOSDClient(client);
                        s3Parameter.addResponseSize(actualSize);
                    }
                }
            } else {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    actualSize = getObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), sourceRange);
                } else {
                    try {
                        client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        client.getInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId(), fileSize, sourceRange, s3Parameter.getResponse().getOutputStream());
                        actualSize = client.get();
                        OSDClientManager.getInstance().returnOSDClient(client);
                        s3Parameter.addResponseSize(actualSize);
                    } catch (Exception e) {
                        client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        client.getInit(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId(), fileSize, sourceRange, s3Parameter.getResponse().getOutputStream());
                        actualSize = client.get();
                        OSDClientManager.getInstance().returnOSDClient(client);
                        s3Parameter.addResponseSize(actualSize);
                    }
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } 

        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_FILE_SIZE, actualSize);
    }

    private long getObjectLocal(String path, String objId, String sourceRange) throws IOException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File ecFile = new File(makeECPath(path, objId, versionId));
        File file = null;

        if (ecFile.exists()) {
            logger.info(GWConstants.LOG_S3OBJECT_OPERATION_ZUNFEC_DECODE, ecFile.getName());
            String command = GWConstants.ZUNFEC
                            + makeECDecodePath(path, objId, versionId)
                            + GWConstants.SPACE + ecFile + GWConstants.ZFEC_0
                            + GWConstants.SPACE + ecFile + GWConstants.ZFEC_1
                            + GWConstants.SPACE + ecFile + GWConstants.ZFEC_2
                            + GWConstants.SPACE + ecFile + GWConstants.ZFEC_3;
            logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_ZUNFEC_COMMAND, command);
            Process p = Runtime.getRuntime().exec(command);
            try {
                int exitCode = p.waitFor();
                logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_ZUNFEC_DECODE_EXIT_VALUE, exitCode);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

            file = new File(makeECDecodePath(path, objId, versionId));
        } else {
            file = new File(makeObjPath(path, objId, versionId));
        }

        long actualSize = 0L;
        try (FileInputStream fis = new FileInputStream(file)) {
            long remaingLength = 0L;
            int readLength = 0;
            int readBytes;

            if (Strings.isNullOrEmpty(sourceRange)) {
                remaingLength = file.length();
                while (remaingLength > 0) {
                    readBytes = 0;
                    if (remaingLength < GWConstants.MAXBUFSIZE) {
                        readBytes = (int)remaingLength;
                    } else {
                        readBytes = GWConstants.MAXBUFSIZE;
                    }

                    if (GWConfig.getPerformanceMode().equals(GWConstants.PERFORMANCE_MODE_NO_DISK)) {
                        if (remaingLength >= GWConstants.MAXBUFSIZE) {
                            readLength = GWConstants.MAXBUFSIZE;
                        } else {
                            readLength = (int)remaingLength;
                        }
                    } else {
                        readLength = fis.read(buffer, 0, readBytes);
                    }
                    
                    actualSize += readLength;
                    s3Parameter.getResponse().getOutputStream().write(buffer, 0, readLength);
                    remaingLength -= readLength;
                }
            } else {
                String[] ranges = sourceRange.split(GWConstants.SLASH);
                for (String range : ranges) {
                    String[] rangeParts = range.split(",");
                    long offset = Longs.tryParse(rangeParts[0]);
                    long length = Longs.tryParse(rangeParts[1]);
                    logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_RANGE, offset, length);

                    if (offset > 0) {
                        fis.skip(offset);
                    }
                    remaingLength = length;
                    while (remaingLength > 0) {
                        readBytes = 0;
                        if (remaingLength < GWConstants.MAXBUFSIZE) {
                            readBytes = (int)remaingLength;
                        } else {
                            readBytes = GWConstants.MAXBUFSIZE;
                        }

                        if (GWConfig.getPerformanceMode().equals(GWConstants.PERFORMANCE_MODE_NO_DISK)) {
                            readLength = readBytes;
                        } else {
                            readLength = fis.read(buffer, 0, readBytes);
                        }
                        
                        actualSize += readLength;
                        s3Parameter.getResponse().getOutputStream().write(buffer, 0, readLength);
                        remaingLength -= readLength;
                    }
                }
            }
        }

        if (ecFile.exists()) {
            file.delete();
        }

        return actualSize;
    }
    
    public S3Object putObject() throws GWException {
        S3Object s3Object = null;
        String objectName = s3Parameter.getObjectName();

        if (getDirectoryBlobSuffix(objectName) != null) {
            s3Object = new S3Object();
			s3Object.setVersionId(GWConstants.VERSIONING_DISABLE_TAIL);
			s3Object.setEtag(GWConstants.DIRECTORY_MD5);
			s3Object.setLastModified(new Date());
			s3Object.setFileSize(0);
			s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
			return s3Object;
		}

        s3Object = putObjectNormal(s3Meta.getContentLength(), s3Parameter.getInputStream());
        s3Parameter.addRequestSize(s3Object.getFileSize());
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_ETAG_AND_VERSION_ID, s3Object.getEtag(), s3Object.getVersionId());

        return s3Object;
    }

    private S3Object putObjectNormal(long length, InputStream is) throws GWException {
        S3Object s3Object = new S3Object();

        File filePrimary = null;
        File tmpFilePrimary = null;
        FileOutputStream fosPrimary = null;
        File fileReplica = null;
        File tmpFileReplica = null;
        FileOutputStream fosReplica = null;
        File trashPrimary = null;
        File trashReplica = null;
        OSDClient clientPrimary = null;
        OSDClient clientReplica = null;
        long existFileSize = 0L;
        long putSize = 0L;
        long calSize = 0L;

        try {
            MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5);
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            long remainLength = length;
            int bufferSize = (int) (remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
            
            existFileSize = objMeta.getSize();
            putSize = length;
            boolean isPrimaryCache = false;
            boolean isReplicaCache = false;

            logger.info("performance mode : {}", GWConfig.getPerformanceMode());
            logger.info("objMeta - replicaCount : {}", objMeta.getReplicaCount());

            // No option
            if (GWConfig.isNoOption()) {
                if (objMeta.getReplicaCount() > 1) {
                    existFileSize *= objMeta.getReplicaCount();
                    putSize *= objMeta.getReplicaCount();
                    logger.debug("bucket : {}, object : {}", objMeta.getBucket(), objMeta.getPath());
                    logger.debug("primary disk id : {}, osd ip : {}", objMeta.getPrimaryDisk().getId(), objMeta.getPrimaryDisk().getOsdIp());
                    logger.debug("replica disk id : {}, osd ip : {}", objMeta.getReplicaDisk().getId(), objMeta.getReplicaDisk().getOsdIp());
                    if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                        if (!Strings.isNullOrEmpty(GWConfig.getCacheDisk()) && length <= (GWConfig.getCacheFileSize() * GWConstants.MEGABYTES)) {
                            filePrimary = new File(makeCachePath(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                            tmpFilePrimary = new File(makeCachePath(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                            trashPrimary = new File(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            File file = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            com.google.common.io.Files.createParentDirs(file);
                            com.google.common.io.Files.createParentDirs(filePrimary);
                            com.google.common.io.Files.createParentDirs(tmpFilePrimary);
                            logger.debug("filePrimary path : {}", filePrimary.getAbsolutePath());
                            logger.debug("tmpFilePrimary path : {}", tmpFilePrimary.getAbsolutePath());
                            fosPrimary = new FileOutputStream(tmpFilePrimary, false);
                            isPrimaryCache = true;
                        } else {
                            filePrimary = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            tmpFilePrimary = new File(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            trashPrimary = new File(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            com.google.common.io.Files.createParentDirs(filePrimary);
                            com.google.common.io.Files.createParentDirs(tmpFilePrimary);
                            fosPrimary = new FileOutputStream(tmpFilePrimary, false);
                        }
                    } else {
                        clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, length, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId(), GWConfig.getPerformanceMode());
                    }
                    
                    if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                        if (!Strings.isNullOrEmpty(GWConfig.getCacheDisk()) && length <= GWConfig.getCacheFileSize() * GWConstants.MEGABYTES) {
                            fileReplica = new File(makeCachePath(makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId)));
                            tmpFileReplica = new File(makeCachePath(makeTempPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId)));
                            trashReplica = new File(makeCachePath(makeTrashPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId)));
                            File file = new File(makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                            com.google.common.io.Files.createParentDirs(file);
                            com.google.common.io.Files.createParentDirs(fileReplica);
                            com.google.common.io.Files.createParentDirs(tmpFileReplica);
                            logger.debug("fileReplica path : {}", fileReplica.getAbsolutePath());
                            logger.debug("tmpFileReplica path : {}", tmpFileReplica.getAbsolutePath());
                            fosReplica = new FileOutputStream(tmpFileReplica, false);
                            isReplicaCache = true;
                        } else {
                            fileReplica = new File(makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                            tmpFileReplica = new File(makeTempPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                            trashReplica = new File(makeTrashPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                            com.google.common.io.Files.createParentDirs(fileReplica);
                            com.google.common.io.Files.createParentDirs(tmpFileReplica);
                            fosReplica = new FileOutputStream(tmpFileReplica, false);
                        }
                    } else {
                        clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        clientReplica.putInit(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId, length, GWConstants.FILE_ATTRIBUTE_REPLICATION_REPLICA, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, GWConfig.getPerformanceMode());
                    }
        
                    while ((readLength = is.read(buffer, 0, bufferSize)) > 0) {
                        remainLength -= readLength;
                        
                        if (filePrimary == null) {
                            clientPrimary.put(buffer, 0, readLength);
                        } else {
                            fosPrimary.write(buffer, 0, readLength);
                        }
    
                        if (fileReplica == null) {
                            clientReplica.put(buffer, 0, readLength);
                        } else {
                            fosReplica.write(buffer, 0, readLength);
                        }
    
                        md5er.update(buffer, 0, readLength);
                        if (remainLength <= 0) {
                            break;
                        }
                        bufferSize = (int) (remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
                    }
    
                    if (filePrimary == null) {
                        clientPrimary.putFlush();
                        OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                    } else {
                        fosPrimary.flush();
                        if (filePrimary.exists()) {
                            retryRenameTo(filePrimary, trashPrimary);
                        }
                        setAttributeFileReplication(tmpFilePrimary, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                        retryRenameTo(tmpFilePrimary, filePrimary);
                        if (isPrimaryCache) {
                            String path = makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                            String command = "ln -s " + filePrimary.getAbsolutePath() + " " + path;
    
                            logger.debug("{}", command);
                            Process p = Runtime.getRuntime().exec(command);
                            int exitCode = p.waitFor();
                            logger.info("ln : {}", exitCode);
                        }
                    }
                    if (fileReplica == null) {
                        clientReplica.putFlush();
                        OSDClientManager.getInstance().returnOSDClient(clientReplica);
                    } else {
                        fosReplica.flush();
                        if (fileReplica.exists()) {
                            retryRenameTo(fileReplica, trashReplica);
                        }
                        setAttributeFileReplication(tmpFileReplica, GWConstants.FILE_ATTRIBUTE_REPLICATION_REPLICA, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        retryRenameTo(tmpFileReplica, fileReplica);
                        if (isReplicaCache) {
                            String path = makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId);
                            String command = "ln -s " + fileReplica.getAbsolutePath() + " " + path;
    
                            logger.debug("{}", command);
                            Process p = Runtime.getRuntime().exec(command);
                            int exitCode = p.waitFor();
                            logger.info("ln : {}", exitCode);
                        }
                    }
                } else {
                    File file = null;
                    File tmpFile = null;
                    File trashFile = null;
                    if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                        if (!Strings.isNullOrEmpty(GWConfig.getCacheDisk()) && length <= GWConfig.getCacheFileSize() * GWConstants.MEGABYTES) {
                            file = new File(makeCachePath(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                            tmpFile = new File(makeCachePath(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                            trashFile = new File(makeCachePath(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                            File link = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            com.google.common.io.Files.createParentDirs(file);
                            com.google.common.io.Files.createParentDirs(tmpFile);
                            com.google.common.io.Files.createParentDirs(link);
                            fosPrimary = new FileOutputStream(tmpFile, false);
                            isPrimaryCache = true;
                        } else {
                            file = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            tmpFile = new File(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            trashFile = new File(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            com.google.common.io.Files.createParentDirs(file);
                            com.google.common.io.Files.createParentDirs(tmpFile);
                            fosPrimary = new FileOutputStream(tmpFile, false);
                        }
                        
                    } else {
                        clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, length, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId(), GWConfig.getPerformanceMode());
                    }
                    
    
                    while ((readLength = is.read(buffer, 0, bufferSize)) > 0) {
                        remainLength -= readLength;
    
                        if (file == null) {
                            clientPrimary.put(buffer, 0, readLength);
                        } else {
                            fosPrimary.write(buffer, 0, readLength);
                        }
    
                        md5er.update(buffer, 0, readLength);
                        if (remainLength <= 0) {
                            break;
                        }
                        bufferSize = (int) (remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
                    }
    
                    if (file == null) {
                        clientPrimary.putFlush();
                        OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                    } else {
                        fosPrimary.flush();
                        if (file.exists()) {
                            retryRenameTo(file, trashFile);
                        }

                        if (objMeta.getReplicaDisk() != null) {
                            setAttributeFileReplication(tmpFile, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                        }
                        
                        retryRenameTo(tmpFile, file);
                        if (isPrimaryCache) {
                            String path = makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                            String command = "ln -s " + file.getAbsolutePath() + " " + path;
    
                            logger.debug("{}", command);
                            Process p = Runtime.getRuntime().exec(command);
                            int exitCode = p.waitFor();
                            logger.info("ln : {}", exitCode);
                        }
                    }
                }
            }
            // No replication option
            else if (GWConfig.isNoReplica()) {
                File file = null;
                File tmpFile = null;
                File trashFile = null;
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    if (!Strings.isNullOrEmpty(GWConfig.getCacheDisk()) && length <= GWConfig.getCacheFileSize() * GWConstants.MEGABYTES) {
                        file = new File(makeCachePath(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        tmpFile = new File(makeCachePath(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        trashFile = new File(makeCachePath(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        File link = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        com.google.common.io.Files.createParentDirs(file);
                        com.google.common.io.Files.createParentDirs(tmpFile);
                        com.google.common.io.Files.createParentDirs(link);
                        fosPrimary = new FileOutputStream(tmpFile, false);
                        isPrimaryCache = true;
                    } else {
                        file = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        tmpFile = new File(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        trashFile = new File(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        com.google.common.io.Files.createParentDirs(file);
                        com.google.common.io.Files.createParentDirs(tmpFile);
                        fosPrimary = new FileOutputStream(tmpFile, false);
                    }
                    
                } else {
                    clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, length, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId(), GWConfig.getPerformanceMode());
                }
                
                while ((readLength = is.read(buffer, 0, bufferSize)) > 0) {
                    remainLength -= readLength;

                    if (file == null) {
                        clientPrimary.put(buffer, 0, readLength);
                    } else {
                        fosPrimary.write(buffer, 0, readLength);
                    }

                    md5er.update(buffer, 0, readLength);
                    if (remainLength <= 0) {
                        break;
                    }
                    bufferSize = (int) (remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
                }

                if (file == null) {
                    clientPrimary.putFlush();
                    OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                } else {
                    fosPrimary.flush();
                    if (file.exists()) {
                        retryRenameTo(file, trashFile);
                    }

                    if (objMeta.getReplicaDisk() != null) {
                        setAttributeFileReplication(tmpFile, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                    }
                    
                    retryRenameTo(tmpFile, file);
                    if (isPrimaryCache) {
                        String path = makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                        String command = "ln -s " + file.getAbsolutePath() + " " + path;

                        logger.debug("{}", command);
                        Process p = Runtime.getRuntime().exec(command);
                        int exitCode = p.waitFor();
                        logger.info("ln : {}", exitCode);
                    }
                }
            }
            // No IO option
            else if (GWConfig.isNoIO()) {
                while ((readLength = is.read(buffer, 0, bufferSize)) > 0) {
                    remainLength -= readLength;
                    md5er.update(buffer, 0, readLength);
                    if (remainLength <= 0) {
                        break;
                    }
                    bufferSize = (int) (remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
                }
            }
            // No disk option
            else if (GWConfig.isNoDisk()) {
                if (!GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, length, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId(), GWConfig.getPerformanceMode());
                }
                if (objMeta.getReplicaDisk() != null && !GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    clientReplica.putInit(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId, length, GWConstants.FILE_ATTRIBUTE_REPLICATION_REPLICA, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, GWConfig.getPerformanceMode());
                }

                while ((readLength = is.read(buffer, 0, bufferSize)) > 0) {
                    remainLength -= readLength;
                    
                    if (clientPrimary != null) {
                        clientPrimary.put(buffer, 0, readLength);
                    }

                    if (clientReplica != null) {
                        clientReplica.put(buffer, 0, readLength);
                    }

                    md5er.update(buffer, 0, readLength);
                    if (remainLength <= 0) {
                        break;
                    }
                    bufferSize = (int) (remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
                }

                if (clientPrimary != null) {
                    clientPrimary.putFlush();
                    OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                }
                if (clientReplica != null) {
                    clientReplica.putFlush();
                    OSDClientManager.getInstance().returnOSDClient(clientReplica);
                }
            } else {
                logger.error(GWConstants.LOG_S3OBJECT_OPERATION_OPTION_NO_CASE, GWConfig.getPerformanceMode());
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            }

            byte[] digest = md5er.digest();
			String eTag = base16().lowerCase().encode(digest);

            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(length);
            s3Object.setVersionId(versionId);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);

            calSize = putSize - existFileSize;
            if (GWConfig.isNoOption()) {
                updateBucketUsed(objMeta.getBucket(), calSize);
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } catch (ResourceNotFoundException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.NO_SUCH_KEY, s3Parameter);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
            if (objMeta.getReplicaCount() > 1) {
                if (fosPrimary != null) {
                    try {
                        fosPrimary.close();
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                    }
                }
                if (fosReplica != null) {
                    try {
                        fosReplica.close();
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                    }
                }
            }
        }
        return s3Object;
    }

    public boolean deleteObject() throws GWException {
        try {
            if (objMeta.getReplicaCount() > 1) {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    deleteObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId());
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.delete(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId());
                    OSDClientManager.getInstance().returnOSDClient(client);
                } 
                
                if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    deleteObjectLocal(objMeta.getReplicaDisk().getPath(), objMeta.getObjId());
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    client.delete(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId());
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            } else {
                deleteObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId());
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
        logger.info(GWConstants.LOG_S3OBJECT_OPERATION_DELETE, objMeta.getBucket(), objMeta.getPath(), versionId);
        return true;
    }

    private void deleteObjectLocal(String path, String objId) throws IOException, GWException {
        File file = null;
        File trashFile = null;
        if (!Strings.isNullOrEmpty(GWConfig.getCacheDisk()) && objMeta.getSize() <= (GWConfig.getCacheFileSize() * GWConstants.MEGABYTES)) {
            file = new File(makeCachePath(makeObjPath(path, objId, versionId)));
            trashFile = new File(makeCachePath(makeTrashPath(path, objId, versionId)));
        } else {
            file = new File(makeObjPath(path, objId, versionId));
            trashFile = new File(makeTrashPath(path, objId, versionId));
        }

        updateBucketUsed(objMeta.getBucket(), file.length() * objMeta.getReplicaCount() * -1);
        if (file.exists()) {
            retryRenameTo(file, trashFile);
            if (!Strings.isNullOrEmpty(GWConfig.getCacheDisk()) && objMeta.getSize() <= (GWConfig.getCacheFileSize() * GWConstants.MEGABYTES)) {
                File link = new File(makeObjPath(path, objId, versionId));
                link.delete();
            }
        }
    }

    public S3Object uploadPart(String path, long length) throws GWException {
        S3Object s3Object = new S3Object();

        try {
            MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5);
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            long remainLength = length;
            int bufferSize = (int)(remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);

            File tmpFile = new File(makeTempPath(path, objMeta.getObjId(), s3Parameter.getPartNumber()));
            com.google.common.io.Files.createParentDirs(tmpFile);
            try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                while ((readLength = s3Parameter.getInputStream().read(buffer, 0, bufferSize)) > 0) {
                    remainLength -= readLength;
                    fos.write(buffer, 0, readLength);
                    md5er.update(buffer, 0, readLength);
                    if (remainLength <= 0) {
                        break;
                    }
                    bufferSize = (int)(remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
                }
                fos.flush();
            }

            byte[] digest = md5er.digest();
			String eTag = base16().lowerCase().encode(digest);

            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(length);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } 
        
        return s3Object;
    }

    public void deletePart(String diskID) throws Exception {
        String host = GWDiskConfig.getInstance().getOSDIP(diskID);
        if (host == null) {
            logger.error(GWConstants.LOG_S3OBJECT_OPERATION_DISK_IP_NULL, diskID);
            return;
        }

        String path = GWDiskConfig.getInstance().getPath(diskID);
        if (path == null) {
            logger.error(GWConstants.LOG_S3OBJECT_OPERATION_DISK_PATH_NULL, diskID);
            return;
        }

        if (GWUtils.getLocalIP().equals(host)) {
            File tmpFile = new File(makeTempPath(path, objMeta.getObjId(), s3Parameter.getPartNumber()));
            tmpFile.delete();
        } else {
            OSDClient client = OSDClientManager.getInstance().getOSDClient(host);
            client.deletePart(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber());
            OSDClientManager.getInstance().returnOSDClient(client);
        }
    }

    public S3Object completeMultipart(SortedMap<Integer, Part> listPart) throws Exception {
        S3Object s3Object = new S3Object();
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5);
        long totalLength = 0L;
        long existFileSize = 0L;
        long putSize = 0L;
        long calSize = 0L;

        File tmpFile = new File(makeTempPath(GWDiskConfig.getInstance().getLocalPath(), objMeta.getObjId(), versionId));
        try (FileOutputStream tmpOut = new FileOutputStream(tmpFile)) {
            com.google.common.io.Files.createParentDirs(tmpFile);
            // for each part object
            for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, Part> entry = it.next();
                if (GWDiskConfig.getInstance().getLocalDiskID().equals(entry.getValue().getDiskID())) {
                    // part is in local disk
                    File partFile = new File(makeTempPath(GWDiskConfig.getInstance().getLocalPath(), objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber())));
                    try (FileInputStream fis = new FileInputStream(partFile)) {
                        int readLength = 0;
                        while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                            totalLength += readLength;
                            tmpOut.write(buffer, 0, readLength);
                            md5er.update(buffer, 0, readLength);
                        }
                        tmpOut.flush();
                        if (!partFile.delete()) {
                            logger.error(GWConstants.LOG_S3OBJECT_OPERATION_FAILED_FILE_DELETE, partFile.getName());
                        }
                    }
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(entry.getValue().getDiskID());
                    client.getPartInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), tmpOut, md5er);
                    totalLength += client.getPart();
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            }
            byte[] digest = md5er.digest();
			String eTag = base16().lowerCase().encode(digest);

            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(totalLength);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        }

        OSDClient clientPrimary = null;
        OSDClient clientReplica = null;
        int readLength = 0;
        
        existFileSize = objMeta.getSize();
        putSize = totalLength;

        if (objMeta.getReplicaCount() > 1) {
            existFileSize *= objMeta.getReplicaCount();
            putSize *= objMeta.getReplicaCount();
            if (!GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, totalLength, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId(), GWConfig.getPerformanceMode());
            }

            if (!GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                clientReplica.putInit(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId, totalLength, GWConstants.FILE_ATTRIBUTE_REPLICATION_REPLICA, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, GWConfig.getPerformanceMode());
            }
            
            if (clientPrimary != null || clientReplica != null) {
                try (FileInputStream fis = new FileInputStream(tmpFile)) {
                    readLength = 0;
                    while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                        if (clientPrimary != null) {
                            clientPrimary.put(buffer, 0, readLength);
                        }
                        if (clientReplica != null) {
                            clientReplica.put(buffer, 0, readLength);
                        }
                    }
                }
            }

            if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                File file = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                File trashFile = new File(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                com.google.common.io.Files.createParentDirs(file);
                if (file.exists()) {
                    retryRenameTo(file, trashFile);
                }
                setAttributeFileReplication(tmpFile, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                retryRenameTo(tmpFile, file);
            }

            if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                File file = new File(makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                File trashFile = new File(makeTrashPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                com.google.common.io.Files.createParentDirs(file);
                if (file.exists()) {
                    retryRenameTo(file, trashFile);
                }
                setAttributeFileReplication(tmpFile, GWConstants.FILE_ATTRIBUTE_REPLICATION_REPLICA, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                retryRenameTo(tmpFile, file);
            }
        } else {
            File file = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
            File trashFile = new File(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
            com.google.common.io.Files.createParentDirs(file);
            if (file.exists()) {
                retryRenameTo(file, trashFile);
            }
            setAttributeFileReplication(tmpFile, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
            retryRenameTo(tmpFile, file);
        }

        calSize = putSize - existFileSize;
        updateBucketUsed(objMeta.getBucket(), calSize);

        return s3Object;
    }

    public void abortMultipart(SortedMap<Integer, Part> listPart) throws GWException {
        try {
            for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, Part> entry = it.next();
                logger.info("key : {}, diskId : {}", entry.getKey(), objMeta.getPrimaryDisk().getId());
                if (GWDiskConfig.getInstance().getLocalDiskID().equals(entry.getValue().getDiskID())) {
                    // part is in local disk
                    File partFile = new File(makeTempPath(GWDiskConfig.getInstance().getLocalPath(), objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber())));
                    partFile.delete();
                } else {
                    String host = GWDiskConfig.getInstance().getOSDIP(entry.getValue().getDiskID());
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(host);
                    client.deletePart(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber());
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            }
        } catch (Exception e) {
             PrintStack.logging(logger, e);
             throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
    }

    public S3Object uploadPartCopy(String path, Metadata srcObjMeta, S3Range s3Range) throws GWException {
        S3Object s3Object = new S3Object();
        try {
            String copySourceRange = "";
            if (s3Range != null && s3Range.getListRange().size() > 0) {
                for (S3Range.Range range : s3Range.getListRange()) {
                    if (Strings.isNullOrEmpty(copySourceRange)) {
                        copySourceRange = String.valueOf(range.getOffset()) + GWConstants.COMMA + String.valueOf(range.getLength());
                    } else {
                        copySourceRange += GWConstants.SLASH + String.valueOf(range.getOffset()) + GWConstants.COMMA + String.valueOf(range.getLength());
                    }
                }
            }
            logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_COPY_SOURCE_RANGE, copySourceRange);
            OSDData osdData = null;
            if (objMeta.getReplicaCount() > 1) {
                if (GWUtils.getLocalIP().equals(srcObjMeta.getPrimaryDisk().getOsdIp())) {
                    osdData = uploadPartCopyLocal(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, path, objMeta.getObjId(), s3Parameter.getPartNumber());
                } else if (GWUtils.getLocalIP().equals(srcObjMeta.getReplicaDisk().getOsdIp())) {
                    osdData = uploadPartCopyLocal(srcObjMeta.getReplicaDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, path, objMeta.getObjId(), s3Parameter.getPartNumber());
                } else {
                    try {
                        OSDClient client = OSDClientManager.getInstance().getOSDClient(srcObjMeta.getPrimaryDisk().getOsdIp());
                        osdData = client.partCopy(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, path, objMeta.getObjId(), s3Parameter.getPartNumber());
                        OSDClientManager.getInstance().returnOSDClient(client);
                    } catch (Exception e) {
                        OSDClient client = OSDClientManager.getInstance().getOSDClient(srcObjMeta.getReplicaDisk().getOsdIp());
                        osdData = client.partCopy(srcObjMeta.getReplicaDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, path, objMeta.getObjId(), s3Parameter.getPartNumber());
                        OSDClientManager.getInstance().returnOSDClient(client);
                    }
                }
            } else {
                osdData = uploadPartCopyLocal(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, path, objMeta.getObjId(), s3Parameter.getPartNumber());
            }

            if (osdData != null) {
                s3Object.setEtag(osdData.getETag());
                s3Object.setFileSize(osdData.getFileSize());
            } else {
                logger.error(GWConstants.LOG_S3OBJECT_OPERATION_OSD_ERROR);
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            }

            s3Object.setLastModified(new Date());
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        return s3Object;
    }

    private OSDData uploadPartCopyLocal(String srcPath, String srcObjId, String srcVersionId, String copySourceRange, String path, String objId, String partNo) throws IOException, NoSuchAlgorithmException {
        OSDData osdData = new OSDData();

        MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5);
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File srcFile = new File(makeObjPath(srcPath, srcObjId, srcVersionId));
        long remainLength = 0L;
        int readLength = 0;
        int readBytes;
        String eTag = "";
        long totalLength = 0L;

        OSDData data = null;

        try (FileInputStream fis = new FileInputStream(srcFile)) {
            File tmpFile = new File(makeTempPath(path, objId, partNo));
            com.google.common.io.Files.createParentDirs(tmpFile);
            try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                data = new OSDData();
                if (Strings.isNullOrEmpty(copySourceRange)) {
                    remainLength = srcFile.length();
                    data.setFileSize(remainLength);
                    while (remainLength > 0) {
                        readBytes = 0;
                        if (remainLength < GWConstants.MAXBUFSIZE) {
                            readBytes = (int)remainLength;
                        } else {
                            readBytes = GWConstants.MAXBUFSIZE;
                        }
                        readLength = fis.read(buffer, 0, readBytes);
                        fos.write(buffer, 0, readLength);
                        md5er.update(buffer, 0, readLength);
                        remainLength -= readLength;
                        totalLength += readLength;
                    }
                    fos.flush();
                } else {
                    String[] ranges = copySourceRange.split(GWConstants.SLASH);
                    totalLength = 0L;
                    for (String range : ranges) {
                        String[] rangeParts = range.split(GWConstants.COMMA);
                        long offset = Longs.tryParse(rangeParts[GWConstants.RANGE_OFFSET_INDEX]);
                        long length = Longs.tryParse(rangeParts[GWConstants.RANGE_LENGTH_INDEX]);
                        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_RANGE, offset, length);

                        if (offset > 0) {
                            fis.skip(offset);
                        }
                        remainLength = length;
                        totalLength += length;
                        while (remainLength > 0) {
                            readBytes = 0;
                            if (remainLength < GWConstants.MAXBUFSIZE) {
                                readBytes = (int)remainLength;
                            } else {
                                readBytes = GWConstants.MAXBUFSIZE;
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
            osdData.setETag(eTag);
            osdData.setFileSize(totalLength);
        }

        return osdData;
    }

    public S3Object copyObject(Metadata srcObjMeta) throws GWException {
        S3Object s3Object = new S3Object();
        try {            
            logger.info(GWConstants.LOG_S3OBJECT_OPERATION_LOCAL_IP, GWUtils.getLocalIP());
            logger.info(GWConstants.LOG_S3OBJECT_OPERATION_OBJ_PRIMARY_IP, objMeta.getPrimaryDisk().getOsdIp());
            if (objMeta.getReplicaDisk() != null) {
                logger.info(GWConstants.LOG_S3OBJECT_OPERATION_OBJ_REPLICA_IP, objMeta.getReplicaDisk().getOsdIp());
            }

            if (objMeta.getReplicaCount() > 1) {
                // check primary local src, obj
                if (GWUtils.getLocalIP().equals(srcObjMeta.getPrimaryDisk().getOsdIp())) {
                    if (srcObjMeta.getPrimaryDisk().getOsdIp().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                        copyObjectLocal(srcObjMeta.getPrimaryDisk().getPath(), 
                                        srcObjMeta.getObjId(), 
                                        srcObjMeta.getVersionId(), 
                                        objMeta.getPrimaryDisk().getPath(), 
                                        objMeta.getObjId(), 
                                        versionId, 
                                        GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                        objMeta.getReplicaDisk().getId());
                    } else {
                        // src local, obj replica
                        // put src to replica
                        copyObjectLocalToOSD(srcObjMeta.getPrimaryDisk().getPath(), 
                                             srcObjMeta.getObjId(), 
                                             srcObjMeta.getVersionId(), 
                                             objMeta.getPrimaryDisk().getOsdIp(), 
                                             objMeta.getPrimaryDisk().getPath(), 
                                             objMeta.getObjId(), 
                                             versionId, 
                                             GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                             objMeta.getReplicaDisk().getId());
                    }
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.copy(srcObjMeta.getPrimaryDisk().getPath(), 
                                srcObjMeta.getObjId(), 
                                srcObjMeta.getVersionId(), 
                                objMeta.getPrimaryDisk().getPath(), 
                                objMeta.getObjId(), 
                                versionId,
                                GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY,
                                objMeta.getReplicaDisk().getId());
                    OSDClientManager.getInstance().returnOSDClient(client);
                }

                // check replica local src, obj
                if (GWUtils.getLocalIP().equals(srcObjMeta.getReplicaDisk().getOsdIp())) {
                    if (srcObjMeta.getReplicaDisk().getOsdIp().equals(objMeta.getReplicaDisk().getOsdIp())) {
                        copyObjectLocal(srcObjMeta.getReplicaDisk().getPath(), 
                                        srcObjMeta.getObjId(), 
                                        srcObjMeta.getVersionId(), 
                                        objMeta.getReplicaDisk().getPath(), 
                                        objMeta.getObjId(), 
                                        versionId, 
                                        GWConstants.FILE_ATTRIBUTE_REPLICATION_REPLICA, 
                                        GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                    } else {
                        // src local, obj replica
                        // put src to replica
                        copyObjectLocalToOSD(srcObjMeta.getReplicaDisk().getPath(), 
                                             srcObjMeta.getObjId(), 
                                             srcObjMeta.getVersionId(), 
                                             objMeta.getReplicaDisk().getOsdIp(), 
                                             objMeta.getReplicaDisk().getPath(), 
                                             objMeta.getObjId(), 
                                             versionId,
                                             GWConstants.FILE_ATTRIBUTE_REPLICATION_REPLICA, 
                                             GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                    }
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    client.copy(srcObjMeta.getReplicaDisk().getPath(), 
                                srcObjMeta.getObjId(), 
                                srcObjMeta.getVersionId(), 
                                objMeta.getReplicaDisk().getPath(), 
                                objMeta.getObjId(), 
                                versionId,
                                GWConstants.FILE_ATTRIBUTE_REPLICATION_REPLICA, 
                                GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            } else {
                copyObjectLocal(srcObjMeta.getPrimaryDisk().getPath(), 
                                srcObjMeta.getObjId(), 
                                srcObjMeta.getVersionId(), 
                                objMeta.getPrimaryDisk().getPath(), 
                                objMeta.getObjId(), 
                                versionId, 
                                GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
            }

            s3Object.setEtag(srcObjMeta.getEtag());
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(srcObjMeta.getSize());
            s3Object.setVersionId(versionId);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
      
        return s3Object;
    }

    private void copyObjectLocal(String srcPath, String srcObjId, String srcVersionId, String path, String objId, String versionId, String replication, String replicaDiskID) throws IOException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File srcFile = new File(makeObjPath(srcPath, srcObjId, srcVersionId));
        try (FileInputStream fis = new FileInputStream(srcFile)) {
            File file = new File(makeObjPath(path, objId, versionId));
            File tmpFile = new File(makeTempPath(path, objId, versionId));
            File trashFile = new File(makeTrashPath(path, objId, versionId));

            com.google.common.io.Files.createParentDirs(file);
            com.google.common.io.Files.createParentDirs(tmpFile);
            try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                int readLength = 0;
                while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                    fos.write(buffer, 0, readLength);
                }
                fos.flush();
            }
            if (file.exists()) {
                retryRenameTo(file, trashFile);
            }
            setAttributeFileReplication(tmpFile, replication, replicaDiskID);
            retryRenameTo(tmpFile, file);
        }
    }

    private void copyObjectLocalToOSD(String srcPath, String srcObjId, String srcVersionId, String osdIP, String path, String objId, String versionId, String replication, String replicaDiskID) throws GWException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File srcFile = new File(makeObjPath(srcPath, srcObjId, srcVersionId));
        OSDClient client = null;
        try {
            client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
            client.putInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, s3Meta.getContentLength(), replication, replicaDiskID, "");
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } 

        try (FileInputStream fis = new FileInputStream(srcFile)) {
            int readLength = 0;
            while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                client.put(buffer, 0, readLength);
            }

            client.putFlush();
            OSDClientManager.getInstance().returnOSDClient(client);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
    }

    private String makeDirectoryName(String objId) {
        byte[] path = new byte[6];
        byte[] byteObjId = objId.getBytes();

        path[0] = GWConstants.CHAR_SLASH;
        int index = 1;
        
        path[index++] = byteObjId[0];
        path[index++] = byteObjId[1];
        path[index++] = GWConstants.CHAR_SLASH;
        path[index++] = byteObjId[2];
        path[index] = byteObjId[3];

        return new String(path);
    }

    private void setObjManager() throws Exception {
		objManager = ObjManagerHelper.getInstance().getObjManager();
	}

	private void releaseObjManager() throws Exception {
		ObjManagerHelper.getInstance().returnObjManager(objManager);
	}

    private void updateBucketUsed(String bucketName, long size) throws GWException {
		try {
			setObjManager();
			objManager.updateBucketUsed(bucketName, size);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
	}

    private String makeObjPath(String path, String objId, String versionId) {
        String fullPath = path + GWConstants.SLASH + GWConstants.OBJ_DIR + makeDirectoryName(objId) + GWConstants.SLASH + objId + GWConstants.UNDERSCORE + versionId;
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_OBJ_PATH, fullPath);
        return fullPath;
    }

    private String makeTempPath(String path, String objId, String versionId) {
        String fullPath = path + GWConstants.SLASH + GWConstants.TEMP_DIR + GWConstants.SLASH + objId + GWConstants.UNDERSCORE + versionId;
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_TEMP_PATH, fullPath);
        return fullPath;
    }

    private String makeTrashPath(String path, String objId, String versionId) {
        String uuid = UUID.randomUUID().toString();
        String fullPath = path + GWConstants.SLASH + GWConstants.TRASH_DIR + GWConstants.SLASH + objId + GWConstants.UNDERSCORE + versionId + GWConstants.DASH + uuid;
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_TRASH_PATH, fullPath);
        return fullPath;
    }

    private String makeECPath(String path, String objId, String versionId) {
        String fullPath = path + GWConstants.SLASH + GWConstants.EC_DIR + makeDirectoryName(objId) + GWConstants.SLASH + GWConstants.POINT + objId + GWConstants.UNDERSCORE + versionId;
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_EC_PATH, fullPath);
        return fullPath;
    }

    private String makeECDecodePath(String path, String objId, String versionId) {
        String fullPath = path + GWConstants.SLASH + GWConstants.EC_DIR + makeDirectoryName(objId) + GWConstants.SLASH + objId + GWConstants.UNDERSCORE + versionId;
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_EC_PATH, fullPath);
        return fullPath;
    }

    private String makeCachePath(String path) {
        String fullPath = GWConfig.getCacheDisk() + path;
        return fullPath;
    }

    private void retryRenameTo(File tempFile, File destFile) {
        if (tempFile.exists()) {
            for (int i = 0; i < GWConstants.RETRY_COUNT; i++) {
                if (tempFile.renameTo(destFile)) {
                    return;
                }
            }
            logger.error(GWConstants.LOG_S3OBJECT_OPERATION_FAILED_FILE_RENAME, tempFile.getAbsolutePath(), destFile.getAbsolutePath());
        }
    }

    public void setAttributeFileReplication(File file, String value, String replicaDiskID) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        try {
            view.write(GWConstants.FILE_ATTRIBUTE_REPLICATION, Charset.defaultCharset().encode(value));
            view.write(GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID, Charset.defaultCharset().encode(replicaDiskID));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public String getAttributeFileReplication(File file) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        ByteBuffer buf = null;
        try {
            buf = ByteBuffer.allocate(view.size(GWConstants.FILE_ATTRIBUTE_REPLICATION));
            view.read(GWConstants.FILE_ATTRIBUTE_REPLICATION, buf);
            buf.flip();
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (IllegalArgumentException iae) {
            logger.error(iae.getMessage());
        } catch (SecurityException e) {
            logger.error(e.getMessage());
        }

        return Charset.defaultCharset().decode(buf).toString();
    }

    public String getAttributeFileReplicaDiskID(File file) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        ByteBuffer buf = null;
        try {
            buf = ByteBuffer.allocate(view.size(GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID));
            view.read(GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID, buf);
            buf.flip();
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (IllegalArgumentException iae) {
            logger.error(iae.getMessage());
        } catch (SecurityException e) {
            logger.error(e.getMessage());
        }

        return Charset.defaultCharset().decode(buf).toString();
    }
}
