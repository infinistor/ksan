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

import static com.google.common.io.BaseEncoding.base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.Collection;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.pspace.ifs.ksan.gw.exception.AzuErrorCode;
import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagerHelper;
import com.pspace.ifs.ksan.gw.object.osdclient.OSDClientManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.OSDClient;
import com.pspace.ifs.ksan.libs.data.OsdData;
import com.pspace.ifs.ksan.libs.KsanUtils;

import de.sfuhrm.openssl4j.OpenSSL4JProvider;

import org.apache.commons.crypto.stream.CtrCryptoInputStream;
import org.apache.commons.crypto.stream.CtrCryptoOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzuObjectOperation {
    private Metadata objMeta;
    private S3Metadata s3Meta;
    private AzuParameter azuParameter;
    private String versionId;
    private ObjManager objManager;
    private static final Logger logger = LoggerFactory.getLogger(AzuObjectOperation.class);

    public AzuObjectOperation(Metadata objMeta, S3Metadata s3Meta, AzuParameter azuParameter, String versionId) {
        this.objMeta = objMeta;
        this.s3Meta = s3Meta;
        this.azuParameter = azuParameter;

        if (Strings.isNullOrEmpty(versionId)) {
            this.versionId = GWConstants.VERSIONING_DISABLE_TAIL;
        } else {
            this.versionId = versionId;
        }
    }

    public void getObject() throws Exception {       
        OSDClient client = null;
        String sourceRange = AzuConstants.EMPTY_STRING;
        long actualSize = 0L;
        long fileSize = objMeta.getSize();
        
        try {
            if (objMeta.getReplicaCount() > 1) {
                logger.debug("bucket : {}, object : {}", objMeta.getBucket(), objMeta.getPath());
                logger.debug("primary disk id : {}, osd ip : {}", objMeta.getPrimaryDisk().getId(), objMeta.getPrimaryDisk().getOsdIp());
                if (objMeta.isReplicaExist()) {
                    logger.debug("replica disk id : {}, osd ip : {}", objMeta.getReplicaDisk().getId(), objMeta.getReplicaDisk().getOsdIp());
                }

                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    logger.debug("get local - objid : {}, primary get - path : {}", objMeta.getObjId(), objMeta.getPrimaryDisk().getPath());
                    actualSize = getObjectLocal(azuParameter.getResponse().getOutputStream(), objMeta.getPrimaryDisk().getPath(), objMeta.getObjId());
                    azuParameter.addResponseSize(actualSize);
                } else if (objMeta.isReplicaExist() && GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    logger.debug("get local - objid : {}, replica get - path : {}", objMeta.getObjId(), objMeta.getReplicaDisk().getPath());
                    actualSize = getObjectLocal(azuParameter.getResponse().getOutputStream(), objMeta.getReplicaDisk().getPath(), objMeta.getObjId());
                    azuParameter.addResponseSize(actualSize);
                } else {
                    logger.debug("get osd - objid : {}, primary osd : {}", objMeta.getObjId(), objMeta.getPrimaryDisk().getOsdIp());
                    try {
                        // client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        // if (client == null) {
                        //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                        //     client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        // }
                        client = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        client.getInit(objMeta.getPrimaryDisk().getPath(), 
                                       objMeta.getObjId(), 
                                       objMeta.getVersionId(), 
                                       fileSize, 
                                       sourceRange, 
                                       azuParameter.getResponse().getOutputStream(),
                                       AzuConstants.EMPTY_STRING);
                        actualSize = client.get();
                        // OSDClientManager.getInstance().returnOSDClient(client);
                        azuParameter.addResponseSize(actualSize);
                    } catch (Exception e) {
                        PrintStack.logging(logger, e);
                        if (objMeta.isReplicaExist()) {
                            try {
                                // client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                                // if (client == null) {
                                //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getReplicaDisk().getOsdIp());
                                //     client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                                // }
                                client = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                                client.getInit(objMeta.getReplicaDisk().getPath(), 
                                               objMeta.getObjId(), 
                                               objMeta.getVersionId(), 
                                               fileSize, 
                                               sourceRange, 
                                               azuParameter.getResponse().getOutputStream(),
                                               AzuConstants.EMPTY_STRING);
                                actualSize = client.get();
                                // OSDClientManager.getInstance().returnOSDClient(client);
                                client = null;
                                azuParameter.addResponseSize(actualSize);
                            } catch (Exception e1) {
                                PrintStack.logging(logger, e);
                                throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
                            }
                        } else {
                            // Can't find the object
                            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
                        }
                    }
                }
            } else {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    actualSize = getObjectLocal(azuParameter.getResponse().getOutputStream(), objMeta.getPrimaryDisk().getPath(), objMeta.getObjId());
                    azuParameter.addResponseSize(actualSize);
                } else {
                    try {
                        // client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        // if (client == null) {
                        //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                        //     client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        // }
                        client = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        client.getInit(objMeta.getPrimaryDisk().getPath(), 
                                       objMeta.getObjId(), 
                                       objMeta.getVersionId(), 
                                       fileSize, 
                                       sourceRange, 
                                       azuParameter.getResponse().getOutputStream(),
                                       AzuConstants.EMPTY_STRING);
                        actualSize = client.get();
                        // OSDClientManager.getInstance().returnOSDClient(client);
                        client = null;
                        azuParameter.addResponseSize(actualSize);
                    } catch (Exception e) {
                        PrintStack.logging(logger, e);
                        throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
                    }
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            if (client != null) {
                // OSDClientManager.getInstance().returnOSDClient(client);
            }
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        } 

        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_FILE_SIZE, actualSize);
    }

    private long getObjectLocal(OutputStream outputStream, String path, String objId) throws IOException, AzuException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File file = null;

        file = new File(KsanUtils.makeObjPath(path, objId, versionId));

        logger.info("obj path : {}", file.getAbsolutePath());
        long actualSize = 0L;
        try (FileInputStream fis = new FileInputStream(file)) {
            long remaingLength = 0L;
            int readLength = 0;
            int readBytes;

            remaingLength = file.length();
            while (remaingLength > 0) {
                readBytes = 0;
                if (remaingLength < GWConstants.MAXBUFSIZE) {
                    readBytes = (int)remaingLength;
                } else {
                    readBytes = GWConstants.MAXBUFSIZE;
                }

                if (remaingLength >= GWConstants.MAXBUFSIZE) {
                        readLength = GWConstants.MAXBUFSIZE;
                } else {
                    readLength = (int)remaingLength;
                }
                readLength = fis.read(buffer, 0, readBytes);
                
                actualSize += readLength;
                outputStream.write(buffer, 0, readLength);
                // s3Parameter.addResponseSize(readLength);
                remaingLength -= readLength;
            }
        }

        outputStream.flush();
        outputStream.close();

        return actualSize;
    }

    public S3Object putObject() throws AzuException {
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
        long length = s3Meta.getContentLength();
        long totalReads = 0L;
        long existFileSize = 0L;
        long putSize = 0L;
        long calSize = 0L;

        try {
            MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            
            existFileSize = objMeta.getSize();
            putSize = length;
            boolean isPrimaryCache = false;
            boolean isReplicaCache = false;

            logger.debug("performance mode : {}", GWConfig.getInstance().getPerformanceMode());
            logger.debug("objMeta - replicaCount : {}", objMeta.getReplicaCount());


            if (objMeta.getReplicaCount() > 1) {
                existFileSize *= objMeta.getReplicaCount();
                putSize *= objMeta.getReplicaCount();
                logger.debug("bucket : {}, object : {}", objMeta.getBucket(), objMeta.getPath());
                logger.debug("primary disk id : {}, osd ip : {}", objMeta.getPrimaryDisk().getId(), objMeta.getPrimaryDisk().getOsdIp());
                if (objMeta.isReplicaExist()) {
                    logger.debug("replica disk id : {}, osd ip : {}", objMeta.getReplicaDisk().getId(), objMeta.getReplicaDisk().getOsdIp());
                }
                
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        tmpFilePrimary = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        trashPrimary = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        isPrimaryCache = true;
                    } else {
                        filePrimary = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        tmpFilePrimary = new File(KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        trashPrimary = new File(KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                    }
                    com.google.common.io.Files.createParentDirs(filePrimary);
                    com.google.common.io.Files.createParentDirs(tmpFilePrimary);
                    fosPrimary = new FileOutputStream(tmpFilePrimary, false);
                } else {
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // if (clientPrimary == null) {
                    //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                    //     clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // }
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    logger.info("osd - {},{}", clientPrimary.getSocket().getRemoteSocketAddress().toString(), clientPrimary.getSocket().getLocalPort());
                    clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), 
                                            objMeta.getObjId(), 
                                            versionId, 
                                            length, 
                                            Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                            objMeta.getReplicaDisk().getId(), 
                                            GWConstants.EMPTY_STRING,
                                            GWConfig.getInstance().getPerformanceMode());
                }
                
                if (objMeta.isReplicaExist()) {
                    if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                        if (GWConfig.getInstance().isCacheDiskpath()) {
                            fileReplica = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId)));
                            tmpFileReplica = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId)));
                            trashReplica = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId)));
                            isReplicaCache = true;
                        } else {
                            fileReplica = new File(KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                            tmpFileReplica = new File(KsanUtils.makeTempPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                            trashReplica = new File(KsanUtils.makeTrashPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                        }
                        com.google.common.io.Files.createParentDirs(fileReplica);
                        com.google.common.io.Files.createParentDirs(tmpFileReplica);
                        fosReplica = new FileOutputStream(tmpFileReplica, false);
                    } else {
                        // clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        // if (clientReplica == null) {
                        //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getReplicaDisk().getOsdIp());
                        //     clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        // }
                        clientReplica = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        logger.info("osd - {},{}", clientReplica.getSocket().getRemoteSocketAddress().toString(), clientReplica.getSocket().getLocalPort());
                        clientReplica.putInit(objMeta.getReplicaDisk().getPath(), 
                                            objMeta.getObjId(), 
                                            versionId, 
                                            length, 
                                            Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, 
                                            Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, 
                                            GWConstants.EMPTY_STRING,
                                            GWConfig.getInstance().getPerformanceMode());
                    }
                }
    
                while ((readLength = azuParameter.getInputStream().read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (filePrimary == null) {
                        clientPrimary.put(buffer, 0, readLength);
                    } else {
                        fosPrimary.write(buffer, 0, readLength);
                    }

                    if (objMeta.isReplicaExist()) {
                        if (fileReplica == null) {
                            clientReplica.put(buffer, 0, readLength);
                        } else {
                            fosReplica.write(buffer, 0, readLength);
                        }
                    }

                    md5er.update(buffer, 0, readLength);
                    
                    if (totalReads >= length) {
                        break;
                    }
                }
                logger.info("total read : {}", totalReads);

                if (filePrimary == null) {
                    clientPrimary.putFlush();
                    // OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                    clientPrimary = null;
                } else {
                    fosPrimary.flush();
                    fosPrimary.close();

                    if (filePrimary.exists()) {
                        File tempFile = new File(filePrimary.getAbsolutePath());
                        logger.debug("filePrimary is already exists : {}", filePrimary.getAbsolutePath());
                        retryRenameTo(tempFile, trashPrimary);
                    }

                    // if (objMeta.getReplicaDisk() != null && !Strings.isNullOrEmpty(objMeta.getReplicaDisk().getId())) {
                    //     setAttributeFileReplication(tmpFilePrimary, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                    // } else {
                        // setAttributeFileReplication(tmpFilePrimary, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                    // }

                    retryRenameTo(tmpFilePrimary, filePrimary);
                    if (isPrimaryCache) {
                        String path = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                        com.google.common.io.Files.createParentDirs(new File(path));
                        logger.debug("path : {}, primary path : {}", path, filePrimary.getAbsolutePath());
                        Files.createSymbolicLink(Paths.get(path), Paths.get(filePrimary.getAbsolutePath()));
                    }
                }
                if (objMeta.isReplicaExist()) {
                    if (fileReplica == null) {
                        clientReplica.putFlush();
                        // OSDClientManager.getInstance().returnOSDClient(clientReplica);
                        clientReplica = null;
                    } else {
                        fosReplica.flush();
                        fosReplica.close();
                        
                        if (fileReplica.exists()) {
                            File tempFile = new File(fileReplica.getAbsolutePath());
                            logger.debug("fileReplica is already exists : {}", fileReplica.getAbsolutePath());
                            retryRenameTo(tempFile, trashReplica);
                        }
                        // setAttributeFileReplication(tmpFileReplica, GWConstants.FILE_ATTRIBUTE_REPLICATION_REPLICA, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        retryRenameTo(tmpFileReplica, fileReplica);
                        if (isReplicaCache) {
                            String path = KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId);
                            com.google.common.io.Files.createParentDirs(new File(path));
                            logger.debug("path : {}, primary path : {}", path, fileReplica.getAbsolutePath());
                            Files.createSymbolicLink(Paths.get(path), Paths.get(fileReplica.getAbsolutePath()));
                        }
                    }
                }
            } else {
                File file = null;
                File tmpFile = null;
                File trashFile = null;
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        file = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        tmpFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        trashFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        File link = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        com.google.common.io.Files.createParentDirs(file);
                        com.google.common.io.Files.createParentDirs(tmpFile);
                        com.google.common.io.Files.createParentDirs(link);
                        fosPrimary = new FileOutputStream(tmpFile, false);
                        isPrimaryCache = true;
                    } else {
                        file = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        tmpFile = new File(KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        trashFile = new File(KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        com.google.common.io.Files.createParentDirs(file);
                        com.google.common.io.Files.createParentDirs(tmpFile);
                        fosPrimary = new FileOutputStream(tmpFile, false);
                    }
                    
                } else {
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // if (clientPrimary == null) {
                    //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                    //     clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // }
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());

                    logger.info("osd - {},{}", clientPrimary.getSocket().getRemoteSocketAddress().toString(), clientPrimary.getSocket().getLocalPort());
                    clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), 
                                            objMeta.getObjId(), 
                                            versionId, 
                                            length, 
                                            Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                            GWConstants.EMPTY_STRING,//objMeta.getReplicaDisk().getId(), 
                                            GWConstants.EMPTY_STRING,
                                            GWConfig.getInstance().getPerformanceMode());
                }
                
                while ((readLength = azuParameter.getInputStream().read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (file == null) {
                        clientPrimary.put(buffer, 0, readLength);
                    } else {
                        fosPrimary.write(buffer, 0, readLength);
                    }

                    md5er.update(buffer, 0, readLength);

                    if (totalReads >= length) {
                        break;
                    }
                }
                logger.info("total read : {}", totalReads);

                if (file == null) {
                    clientPrimary.putFlush();
                    // OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                    clientPrimary = null;
                } else {
                    fosPrimary.flush();
                    fosPrimary.close();

                    if (file.exists()) {
                        File tempFile = new File(file.getAbsolutePath());
                        logger.debug("file is already exists : {}", file.getAbsolutePath());
                        retryRenameTo(tempFile, trashFile);
                    }

                    // setAttributeFileReplication(tmpFile, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                    
                    retryRenameTo(tmpFile, file);
                    if (isPrimaryCache) {
                        String path = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                        Files.createSymbolicLink(Paths.get(path), Paths.get(file.getAbsolutePath()));
                    }
                }
            }

            byte[] digest = md5er.digest();
			String eTag = base64().encode(digest);

            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(totalReads);
            s3Object.setVersionId(versionId);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);

            calSize = putSize - existFileSize;
            if (GWConfig.getInstance().isNoOption()) {
                updateBucketUsed(objMeta.getBucket(), calSize);
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        } catch (ResourceNotFoundException e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.NO_SUCH_KEY, azuParameter);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        } finally {
            if (objMeta.getReplicaCount() > 1) {
                if (fosPrimary != null) {
                    try {
                        fosPrimary.close();
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
                    }
                }
                if (fosReplica != null) {
                    try {
                        fosReplica.close();
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
                    }
                }
            }
            try {
                if (clientPrimary != null) {
                    // OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                }
                if (clientReplica != null) {
                    // OSDClientManager.getInstance().returnOSDClient(clientReplica);
                }
            } catch (Exception e) {
                PrintStack.logging(logger, e);
            }
        }
        return s3Object;
    }

    public boolean deleteObject() throws AzuException {
        OSDClient client = null;
        try {
            if (objMeta.getReplicaCount() > 1) {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    deleteObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId());
                } else {
                    client = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.delete(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId());
                    // OSDClientManager.getInstance().returnOSDClient(client);
                    client = null;
                } 
                
                if (objMeta.isReplicaExist()) {
                    if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                        deleteObjectLocal(objMeta.getReplicaDisk().getPath(), objMeta.getObjId());
                    } else {
                        client = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        // client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        client.delete(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId());
                        // OSDClientManager.getInstance().returnOSDClient(client);
                        client = null;
                    }
                }
                
            } else {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    deleteObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId());
                } else {
                    client = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.delete(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId());
                    // OSDClientManager.getInstance().returnOSDClient(client);
                    client = null;
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        } finally {
            if (client != null) {
                try {
                    // OSDClientManager.getInstance().returnOSDClient(client);
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                    throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
                }
            }
        }
        logger.info(AzuConstants.LOG_S3OBJECT_OPERATION_DELETE, objMeta.getBucket(), objMeta.getPath());
        return true;
    }

    private void deleteObjectLocal(String path, String objId) throws IOException, AzuException {
        File file = null;
        File trashFile = null;
        if (GWConfig.getInstance().isCacheDiskpath()) {
            file = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeObjPath(path, objId, versionId)));
            trashFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(path, objId, versionId)));
        } else {
            file = new File(KsanUtils.makeObjPath(path, objId, versionId));
            trashFile = new File(KsanUtils.makeTrashPath(path, objId, versionId));
        }

        updateBucketUsed(objMeta.getBucket(), file.length() * objMeta.getReplicaCount() * -1);
        if (file.exists()) {
            retryRenameTo(file, trashFile);
            if (GWConfig.getInstance().isCacheDiskpath()) {
                File link = new File(KsanUtils.makeObjPath(path, objId, versionId));
                link.delete();
            }
        }
    }

    public S3Object uploadBlock(String blockId, long length) throws AzuException {
        S3Object s3Object = new S3Object();
        MessageDigest md5er = null;
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        int readLength = 0;
        long totalReads = 0L;
        File filePrimary = null;
        File fileReplica = null;
        FileOutputStream fosPrimary = null;
        FileOutputStream fosReplica = null;
        OSDClient clientPrimary = null;
        OSDClient clientReplica = null;

        try {
            md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            if (objMeta.getReplicaCount() > 1) {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPartPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), blockId)));
                    } else {
                        filePrimary = new File(KsanUtils.makeTempPartPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), blockId));
                    }
                    com.google.common.io.Files.createParentDirs(filePrimary);
                    fosPrimary = new FileOutputStream(filePrimary, false);
                } else {
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // if (clientPrimary == null) {
                    //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                    //     clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // }
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    logger.info("osd - {},{}", clientPrimary.getSocket().getRemoteSocketAddress().toString(), clientPrimary.getSocket().getLocalPort());
                    clientPrimary.partInit(objMeta.getPrimaryDisk().getPath(), 
                                           objMeta.getObjId(), 
                                           blockId, 
                                           length,
                                           GWConstants.EMPTY_STRING);
                }
                
                if (objMeta.isReplicaExist()) {
                    if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                        if (GWConfig.getInstance().isCacheDiskpath()) {
                            fileReplica = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPartPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), blockId)));
                        } else {
                            fileReplica = new File(KsanUtils.makeTempPartPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), blockId));
                        }
                        com.google.common.io.Files.createParentDirs(fileReplica);
                        fosReplica = new FileOutputStream(fileReplica, false);
                    } else {
                        // clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        // if (clientReplica == null) {
                        //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getReplicaDisk().getOsdIp());
                        //     clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        // }
                        clientReplica = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        logger.info("osd - {},{}", clientReplica.getSocket().getRemoteSocketAddress().toString(), clientReplica.getSocket().getLocalPort());
                        clientReplica.partInit(objMeta.getReplicaDisk().getPath(), 
                                           objMeta.getObjId(), 
                                           blockId, 
                                           length,
                                           GWConstants.EMPTY_STRING);
                    }
                }
    
                while ((readLength = azuParameter.getInputStream().read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (filePrimary == null) {
                        clientPrimary.put(buffer, 0, readLength);
                    } else {
                        fosPrimary.write(buffer, 0, readLength);
                    }
    
                    if (objMeta.isReplicaExist()) {
                        if (fileReplica == null) {
                            clientReplica.put(buffer, 0, readLength);
                        } else {
                            fosReplica.write(buffer, 0, readLength);
                        }
                    }
    
                    md5er.update(buffer, 0, readLength);
                    if (totalReads >= length) {
                        break;
                    }
                }

                if (filePrimary == null) {
                    clientPrimary.putFlush();
                    clientPrimary = null;
                } else {
                    fosPrimary.flush();
                    fosPrimary.close();
                }

                if (objMeta.isReplicaExist()) {
                    if (fileReplica == null) {
                        clientReplica.putFlush();
                        clientReplica = null;
                    } else {
                        fosReplica.flush();
                        fosReplica.close();
                    }
                }
            } else {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPartPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), blockId)));
                    } else {
                        filePrimary = new File(KsanUtils.makeTempPartPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), blockId));
                    }
                    com.google.common.io.Files.createParentDirs(filePrimary);
                    fosPrimary = new FileOutputStream(filePrimary, false);
                } else {
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // if (clientPrimary == null) {
                    //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                    //     clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // }
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    logger.info("osd - {},{}", clientPrimary.getSocket().getRemoteSocketAddress().toString(), clientPrimary.getSocket().getLocalPort());
                    clientPrimary.partInit(objMeta.getPrimaryDisk().getPath(), 
                                           objMeta.getObjId(), 
                                           blockId, 
                                           length,
                                           GWConstants.EMPTY_STRING);
                }

                while ((readLength = azuParameter.getInputStream().read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (filePrimary == null) {
                        clientPrimary.put(buffer, 0, readLength);
                    } else {
                        fosPrimary.write(buffer, 0, readLength);
                    }
    
                    md5er.update(buffer, 0, readLength);
                    if (totalReads >= length) {
                        break;
                    }
                }

                if (filePrimary == null) {
                    clientPrimary.putFlush();
                    clientPrimary = null;
                } else {
                    fosPrimary.flush();
                    fosPrimary.close();
                }
            }

            logger.debug("Total read : {}", totalReads);

            byte[] digest = md5er.digest();
            String eTag = base64().encode(digest);

            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(length);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        }

        return s3Object;
    }

    public S3Object completeBlockList(Collection<String> blockList) throws AzuException {
        S3Object s3Object = new S3Object();
        MessageDigest md5er = null;
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File file = null;
        File tmpFile = null;
        File trashFile = null;
        File block = null;
        int readLength = 0;
        long totalLength = 0L;
        long existFileSize = 0L;
        long putSize = 0L;
        long calSize = 0L;
        boolean isCacheDiskpath = false;

        if (GWConfig.getInstance().isCacheDiskpath()) {
            isCacheDiskpath = true;
            file = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
            tmpFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempCompleteMultipartPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
            trashFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
        } else {
            file = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
            tmpFile = new File(KsanUtils.makeTempCompleteMultipartPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
            trashFile = new File(KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
        }
        
        try (FileOutputStream tmpOut = new FileOutputStream(tmpFile)) {
            com.google.common.io.Files.createParentDirs(file);
            com.google.common.io.Files.createParentDirs(tmpFile);
            md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            for (String blockId: blockList) {
                if (GWConfig.getInstance().isCacheDiskpath()) {
                    block = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPartPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), blockId)));
                } else {
                    block = new File(KsanUtils.makeTempPartPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), blockId));
                }

                FileInputStream fis = new FileInputStream(block);
                while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                    totalLength += readLength;
                    tmpOut.write(buffer, 0, readLength);
                    md5er.update(buffer, 0, readLength);
                }
                fis.close();

                block.delete();
            }

            if (file.exists()) {
                File tempFile = new File(file.getAbsolutePath());
                retryRenameTo(tempFile, trashFile);
            }
            // if (objMeta.getReplicaDisk() != null && !Strings.isNullOrEmpty(objMeta.getReplicaDisk().getId())) {
            //     setAttributeFileReplication(tmpFilePrimary, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
            // } else {
            //     setAttributeFileReplication(tmpFilePrimary, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
            // }
            retryRenameTo(tmpFile, file);
            if (isCacheDiskpath) {
                String filePath = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                Files.createSymbolicLink(Paths.get(filePath), Paths.get(file.getAbsolutePath()));
            }

            byte[] digest = md5er.digest();
            String eTag = base64().encode(digest);
            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(totalLength);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (NoSuchAlgorithmException | IOException e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        }

        return s3Object;
    }

    private void setObjManager() throws Exception {
		objManager = ObjManagerHelper.getInstance().getObjManager();
	}

	private void releaseObjManager() throws Exception {
		ObjManagerHelper.getInstance().returnObjManager(objManager);
	}

    private void updateBucketUsed(String bucketName, long size) throws AzuException {
		try {
			setObjManager();
			objManager.updateBucketUsed(bucketName, size);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		} finally {
			try {
				releaseObjManager();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
				throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
			}
		}
	}

    private void retryRenameTo(File srcFile, File destFile) throws IOException {
        if (srcFile.exists()) {
            for (int i = 0; i < GWConstants.RETRY_COUNT; i++) {
                if (srcFile.renameTo(destFile)) {
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            logger.error(GWConstants.LOG_S3OBJECT_OPERATION_FAILED_FILE_RENAME, srcFile.getAbsolutePath(), destFile.getAbsolutePath());
        }
    }
}
