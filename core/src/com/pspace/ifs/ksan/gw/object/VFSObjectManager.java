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

import com.pspace.ifs.ksan.gw.encryption.S3Encryption;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.object.S3Range;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.mq.MQSender;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.config.AgentConfig;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.KsanUtils;
import com.pspace.ifs.ksan.libs.osd.OSDClientManager;
import com.pspace.ifs.ksan.libs.osd.OSDClient;
import com.pspace.ifs.ksan.libs.data.ECPart;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;

import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.DISK;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Set;

import org.json.simple.JSONObject;

import de.sfuhrm.openssl4j.OpenSSL4JProvider;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import org.apache.commons.io.FileUtils;
import org.apache.commons.crypto.stream.CtrCryptoInputStream;
import org.apache.commons.crypto.stream.CtrCryptoOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VFSObjectManager implements IObjectManager {
    private static final Logger logger = LoggerFactory.getLogger(VFSObjectManager.class);

    @Override
    public void getObject(S3Parameter param, Metadata meta, S3Encryption encryption, S3Range s3Range) throws GWException {
        // check size 0
        if (meta.getSize() == 0) {
            param.addResponseSize(0L);
            return;
        }

        // check disk
        boolean isAvailablePrimary = meta.isPrimaryExist() && isAvailableDiskForRead(meta.getPrimaryDisk().getId());
        boolean isAvailableReplica = false;
        DISK replicaDISK = null;
        if (meta.isReplicaExist()) {
            try {
                replicaDISK = meta.getReplicaDisk();
                isAvailableReplica = isAvailableDiskForRead(replicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.error("Replica is null");
            }
        }
        if (!isAvailablePrimary && !isAvailableReplica) {
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }

        // check encryption
        String key = encryption.isEnabledEncryption() ? encryption.getEncryptionKey() : GWConstants.EMPTY_STRING;

        // check range
        String sourceRange = GWConstants.EMPTY_STRING;
        long fileSize = 0L;
        if (s3Range != null && s3Range.getListRange().size() > 0) {
            for (S3Range.Range range : s3Range.getListRange()) {
                if (Strings.isNullOrEmpty(sourceRange)) {
                    sourceRange = String.valueOf(range.getOffset()) + GWConstants.COMMA + String.valueOf(range.getLength());
                } else {
                    sourceRange += GWConstants.SLASH + String.valueOf(range.getOffset()) + GWConstants.COMMA + String.valueOf(range.getLength());
                }
                fileSize += range.getLength();
            }
        }

        // check EC
        long actualSize = getECObject(param, meta, key, sourceRange);
        if (actualSize > 0) {
            param.addResponseSize(actualSize);
            return;
        }

        // check multipart
        S3Metadata s3Metadata = S3Metadata.getS3Metadata(meta.getMeta());
        String uploadId = s3Metadata.getUploadId();
        if (!Strings.isNullOrEmpty(uploadId)) {
            actualSize = getMultipart(isAvailablePrimary, isAvailableReplica, param, meta, key, sourceRange);
            logger.debug("multipart actualSize : {}", actualSize);
            param.addResponseSize(actualSize);
            return;
        }

        // check primary local
        String localPath = null;
        if (isAvailablePrimary && (localPath = DiskManager.getInstance().getLocalPath(meta.getPrimaryDisk().getId())) != null) {
            File file = new File(KsanUtils.makeObjPathForOpen(localPath, meta.getObjId(), meta.getVersionId()));
            actualSize = getObjectLocal(param, file, sourceRange, key);
        } 
        // check : replica local
        else if (isAvailableReplica && (localPath = DiskManager.getInstance().getLocalPath(replicaDISK.getId())) != null) {
            File file = new File(KsanUtils.makeObjPathForOpen(localPath, meta.getObjId(), meta.getVersionId()));
            actualSize = getObjectLocal(param, file, sourceRange, key);
        } else if (isAvailablePrimary) {
            actualSize = getObjectOSD(param, meta, meta.getPrimaryDisk(), sourceRange, key);
        } else if (isAvailableReplica) {
            actualSize = getObjectOSD(param, meta, replicaDISK, sourceRange, key);
        }

        param.addResponseSize(actualSize);
    }

    @Override
    public S3Object putObject(S3Parameter param, Metadata meta, S3Encryption encryption) throws GWException {
        // check disk
        boolean isAvailablePrimary = meta.isPrimaryExist() && isAvailableDiskForWrite(meta.getPrimaryDisk().getId());
        boolean isAvailableReplica = false;
        DISK replicaDISK = null;
        if (meta.isReplicaExist()) {
            try {
                replicaDISK = meta.getReplicaDisk();
                isAvailableReplica = isAvailableDiskForWrite(replicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.error("Replica is null");
            } 
        }
        if (!isAvailablePrimary && !isAvailableReplica) {
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }
        logger.debug("isAvailablePrimary : {}, isAvailableReplica : {}", isAvailablePrimary, isAvailableReplica);
        logger.debug("objId : {}, versionId : {}, size : {}", meta.getObjId(), meta.getVersionId(), meta.getSize());
        // check encryption
        String key = encryption.isEnabledEncryption() ? encryption.getEncryptionKey() : GWConstants.EMPTY_STRING;

        long length = meta.getSize();
        S3Object s3Object = new S3Object();
        
        InputStream is = param.getInputStream();
        File filePrimary = null;
        File fileReplica = null;
        File fileTempPrimary = null;
        File fileTempReplica = null;
        File fileTrashPrimary = null;
        File fileTrashReplica = null;
        FileOutputStream fosPrimary = null;
        FileOutputStream fosReplica = null;
        CtrCryptoOutputStream ctrPrimary = null;
        CtrCryptoOutputStream ctrReplica = null;
        OSDClient osdClientPrimary = null;
        OSDClient osdClientReplica = null;
        boolean isCachePrimary = false;
        boolean isCacheReplica = false;
        boolean isBorrowOsdPrimary = false;
        boolean isBorrowOsdReplica = false;

        try {
            // MD5
            MessageDigest md5er = null;
            // if (length < 100 * GWConstants.MEGABYTES) {
                md5er = MessageDigest.getInstance(GWConstants.MD5);
            // } else {
            //     md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            // }
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            long totalReads = 0L;

            // check primary
            if (isAvailablePrimary) {
                if (GWUtils.getLocalIP().equals(meta.getPrimaryDisk().getOsdIp())) {
                    String objPath = null;
                    String tempPath = null;
                    String trashPath = null;
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        objPath = KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                        tempPath = KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                        trashPath = KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                        isCachePrimary = true;
                    } else {
                        objPath = KsanUtils.makeObjPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                        tempPath = KsanUtils.makeTempPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                        trashPath = KsanUtils.makeTrashPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                    }
                    // check unmount disk path
                    if (objPath == null || tempPath == null || trashPath == null) {
                        throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
                    }
                    filePrimary = new File(objPath);
                    fileTempPrimary = new File(tempPath);
                    fileTrashPrimary = new File(trashPath);
                    fosPrimary = new FileOutputStream(fileTempPrimary, false);
                } else {
                    // osdClientPrimary = OSDClientManager.getInstance().getOSDClient(meta.getPrimaryDisk().getOsdIp());
                    osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientPrimary == null) {
                    //     osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdPrimary = true;
                    // }
                    osdClientPrimary.putInit(meta.getPrimaryDisk().getPath(),
                        meta.getObjId(),
                        param.getVersionId(),
                        length,
                        Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY,
                        replicaDISK != null? replicaDISK.getId() : Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL,
                        key,
                        "");
                }
            }

            // check replica
            if (isAvailableReplica) {
                logger.debug("replica osd ip : {}", replicaDISK.getOsdIp());
                if (GWUtils.getLocalIP().equals(replicaDISK.getOsdIp())) {
                    String objPath = null;
                    String tempPath = null;
                    String trashPath = null;
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        objPath = KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                        tempPath = KsanUtils.makeTempPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                        trashPath = KsanUtils.makeTrashPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                        isCacheReplica = true;
                    } else {
                        objPath = KsanUtils.makeObjPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                        tempPath = KsanUtils.makeTempPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                        trashPath = KsanUtils.makeTrashPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                    }
                    // check unmount disk path
                    if (objPath == null || tempPath == null || trashPath == null) {
                        throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
                    }
                    fileReplica = new File(objPath);
                    fileTempReplica = new File(tempPath);
                    fileTrashReplica = new File(trashPath);
                    fosReplica = new FileOutputStream(fileTempReplica, false);
                } else {
                    // osdClientReplica = OSDClientManager.getInstance().getOSDClient(replicaDISK.getOsdIp());
                    osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientReplica == null) {
                    //     osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdReplica = true;
                    // }
                    osdClientReplica.putInit(replicaDISK.getPath(),
                        meta.getObjId(),
                        param.getVersionId(),
                        length,
                        Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA,
                        Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL,
                        key,
                        "");
                }
            }

            // check encryption
            if (!Strings.isNullOrEmpty(key)) {
                if (fosPrimary != null) {
                    ctrPrimary = GWUtils.initCtrEncrypt(fosPrimary, key);
                }
                if (fosReplica != null) {
                    ctrReplica = GWUtils.initCtrEncrypt(fosReplica, key);
                }

                while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (isAvailablePrimary) {
                        if (filePrimary == null) {
                            osdClientPrimary.put(buffer, 0, readLength);
                        } else {
                            ctrPrimary.write(buffer, 0, readLength);
                        }
                    }
                    if (isAvailableReplica) {
                        if (fileReplica == null) {
                            osdClientReplica.put(buffer, 0, readLength);
                        } else {
                            ctrReplica.write(buffer, 0, readLength);
                        }
                    }
                    md5er.update(buffer, 0, readLength);
                    if (totalReads >= length) {
                        break;
                    }
                }
            } else {
                // no encryption
                while ((readLength = is.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (isAvailablePrimary) {
                        if (filePrimary == null) {
                            osdClientPrimary.put(buffer, 0, readLength);
                        } else {
                            fosPrimary.write(buffer, 0, readLength);
                        }
                    }
                    if (isAvailableReplica) {
                        if (fileReplica == null) {
                            osdClientReplica.put(buffer, 0, readLength);
                        } else {
                            fosReplica.write(buffer, 0, readLength);
                        }
                    }
                    md5er.update(buffer, 0, readLength);
                    if (totalReads >= length) {
                        break;
                    }
                }
            }
            
            byte[] digest = md5er.digest();
            String eTag = base16().lowerCase().encode(digest);
            // for GCS md5hash
            String md5Hash = Base64.getEncoder().encodeToString(digest);          
            s3Object.setMd5hash(md5Hash);

            s3Object.setEtag(eTag);
            s3Object.setFileSize(totalReads);
            s3Object.setLastModified(new Date());
            s3Object.setVersionId(param.getVersionId());
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
            s3Object.setSavePrimary(isAvailablePrimary);
            s3Object.setSaveReplica(isAvailableReplica);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (NoSuchAlgorithmException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } finally {
            if (isAvailablePrimary) {
                try {
                    if (filePrimary == null) {
                        osdClientPrimary.putFlush();
                    } else {
                        if (!Strings.isNullOrEmpty(key)) {
                            ctrPrimary.flush();
                            ctrPrimary.close();
                        } 
                        if (fosPrimary != null) {
                            fosPrimary.flush();
                            fosPrimary.close();
                        }

                        if (filePrimary.exists()) {
                            File tmpFile = new File(filePrimary.getAbsolutePath());
                            retryRenameTo(tmpFile, fileTrashPrimary);
                        }
                        if (meta.isReplicaExist()) {
                            KsanUtils.setAttributeFileReplication(fileTempPrimary, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, replicaDISK.getId());
                        }
                        retryRenameTo(fileTempPrimary, filePrimary);
                        if (isCachePrimary) {
                            String path = KsanUtils.makeObjPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                            Files.createSymbolicLink(Paths.get(path), Paths.get(filePrimary.getAbsolutePath()));
                        }
                    }
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
            if (isAvailableReplica) {
                try {
                    if (fileReplica == null) {
                        osdClientReplica.putFlush();
                    } else {
                        if (!Strings.isNullOrEmpty(key)) {
                            ctrReplica.flush();
                            ctrReplica.close();
                        } 
                        if (fosReplica != null) {
                            fosReplica.flush();
                            fosReplica.close();
                        }

                        if (fileReplica.exists()) {
                            File tmpFile = new File(fileReplica.getAbsolutePath());
                            retryRenameTo(tmpFile, fileTrashReplica);
                        }
                        KsanUtils.setAttributeFileReplication(fileTempReplica, Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        retryRenameTo(fileTempReplica, fileReplica);
                        if (isCacheReplica) {
                            String path = KsanUtils.makeObjPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                            Files.createSymbolicLink(Paths.get(path), Paths.get(fileReplica.getAbsolutePath()));
                        }
                    }
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
        }

        return s3Object;
    }

    @Override
    public S3Object copyObject(S3Parameter param, Metadata srcMeta, S3Encryption srcEncryption, Metadata meta,
            S3Encryption encryption) throws GWException {
        // check src disk
        boolean isAvailableSrcPrimary = srcMeta.isPrimaryExist() && isAvailableDiskForRead(srcMeta.getPrimaryDisk().getId());
        boolean isAvailableSrcReplica = false;
        DISK srcReplicaDISK = null;
        if (srcMeta.isReplicaExist()) {
            try {
                srcReplicaDISK = srcMeta.getReplicaDisk();
                isAvailableSrcReplica = isAvailableDiskForRead(srcReplicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.info("src object replica is null");
            }
        }
        if (!isAvailableSrcPrimary && !isAvailableSrcReplica) {
            logger.error("src object disk is not available.");
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }
        logger.info("isAvailableSrcPrimary : {}, isAvailableSrcReplica : {}", isAvailableSrcPrimary, isAvailableSrcReplica);
        
        // check disk
        boolean isAvailablePrimary = meta.isPrimaryExist() && isAvailableDiskForWrite(meta.getPrimaryDisk().getId());
        boolean isAvailableReplica = false;
        DISK replicaDISK = null;
        if (meta.isReplicaExist()) {
            try {
                replicaDISK = meta.getReplicaDisk();
                isAvailableReplica = isAvailableDiskForWrite(replicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.info("replica is null");
            }
        }
        if (!isAvailablePrimary && !isAvailableReplica) {
            logger.error("object disk is not available.");
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }
        logger.debug("isAvailablePrimary : {}, isAvailableReplica : {}", isAvailablePrimary, isAvailableReplica);

        // check multipart
        S3Metadata s3Metadata = S3Metadata.getS3Metadata(srcMeta.getMeta());
        String uploadId = s3Metadata.getUploadId();
        boolean isSrcMultipart = !Strings.isNullOrEmpty(uploadId);

        S3Object s3Object = new S3Object();
        String srcKey = srcEncryption.isEnabledEncryption() ? srcEncryption.getEncryptionKey() : GWConstants.EMPTY_STRING;
        String key = encryption.isEnabledEncryption() ? encryption.getEncryptionKey() : GWConstants.EMPTY_STRING;
        logger.info("srcKey : {}, key : {}", srcKey, key);
        boolean isBorrowSrcOsd = false;
        OSDClient srcClient = null;
        boolean isBorrowOsdPrimary = false;
        boolean isBorrowOsdReplica = false;
        OSDClient osdClientPrimary = null;
        OSDClient osdClientReplica = null;
        File filePrimary = null;
        File fileReplica = null;
        File fileTempPrimary = null;
        File fileTempReplica = null;
        File fileTrashPrimary = null;
        File fileTrashReplica = null;
        FileOutputStream fosPrimary = null;
        FileOutputStream fosReplica = null;
        CtrCryptoOutputStream ctrPrimary = null;
        CtrCryptoOutputStream ctrReplica = null;
        boolean isCachePrimary = false;
        boolean isCacheReplica = false;
        InputStream is = null;
        CtrCryptoInputStream encryptIS = null;

        try {
            File srcFile = null;
            String localPath = GWConstants.EMPTY_STRING;

            if (isAvailableSrcPrimary && (localPath = DiskManager.getInstance().getLocalPath(srcMeta.getPrimaryDisk().getId())) != null) {
                srcFile = new File(KsanUtils.makeObjPathForOpen(localPath, srcMeta.getObjId(), srcMeta.getVersionId()));
                is = new FileInputStream(srcFile);
                if (!Strings.isNullOrEmpty(srcKey)) {
                    encryptIS = GWUtils.initCtrDecrypt(is, srcKey);
                }
            } 
            // check : replica local
            else if (isAvailableSrcReplica && (localPath = DiskManager.getInstance().getLocalPath(srcReplicaDISK.getId())) != null) {
                srcFile = new File(KsanUtils.makeObjPathForOpen(localPath, srcMeta.getObjId(), srcMeta.getVersionId()));
                is = new FileInputStream(srcFile);
                if (!Strings.isNullOrEmpty(srcKey)) {
                    encryptIS = GWUtils.initCtrDecrypt(is, srcKey);
                }
            } else if (isAvailableSrcPrimary) {
                // get src primary from OSD
                // try {
                //     // srcClient = OSDClientManager.getInstance().getOSDClient(srcMeta.getPrimaryDisk().getOsdIp());
                // } catch (Exception e) {
                //     srcClient = null;
                // }
                srcClient = new OSDClient(srcMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                // if (srcClient == null) {
                //     srcClient = new OSDClient(srcMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                // } else {
                //     isBorrowSrcOsd = true;
                // }
                srcClient.getInit(srcMeta.getPrimaryDisk().getPath(),
                    srcMeta.getObjId(),
                    srcMeta.getVersionId(),
                    srcMeta.getSize(),
                    GWConstants.EMPTY_STRING,
                    null,
                    srcKey);
                is = srcClient.getInputStream();
            } else if (isAvailableSrcReplica) {
                // get src replica from OSD
                // try {
                //     // srcClient = OSDClientManager.getInstance().getOSDClient(srcReplicaDISK.getOsdIp());
                // } catch (Exception e) {
                //     srcClient = null;
                // }
                srcClient = new OSDClient(srcReplicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                // if (srcClient == null) {
                //     srcClient = new OSDClient(srcReplicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                // } else {
                //     isBorrowSrcOsd = true;
                // }
                srcClient.getInit(srcReplicaDISK.getPath(),
                    srcMeta.getObjId(),
                    srcMeta.getVersionId(),
                    srcMeta.getSize(),
                    GWConstants.EMPTY_STRING,
                    null,
                    srcKey);
                is = srcClient.getInputStream();
            }
            
            // localPath = GWConstants.EMPTY_STRING;
            // check primary
            if (isAvailablePrimary) {
                if (GWUtils.getLocalIP().equals(meta.getPrimaryDisk().getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                        fileTempPrimary = new File(KsanUtils.makeTempPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                        fileTrashPrimary = new File(KsanUtils.makeTrashPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                        isCachePrimary = true;
                    } else {
                        filePrimary = new File(KsanUtils.makeObjPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                        fileTempPrimary = new File(KsanUtils.makeTempPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                        fileTrashPrimary = new File(KsanUtils.makeTrashPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                    }
                    fosPrimary = new FileOutputStream(fileTempPrimary, false);
                } else {
                    // try {
                    //     // osdClientPrimary = OSDClientManager.getInstance().getOSDClient(meta.getPrimaryDisk().getOsdIp());
                    // } catch (Exception e) {
                    //     osdClientPrimary = null;
                    // }
                    osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientPrimary == null) {
                    //     osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdPrimary = true;
                    // }
                    osdClientPrimary.putInit(meta.getPrimaryDisk().getPath(),
                        meta.getObjId(),
                        param.getVersionId(),
                        srcMeta.getSize(),
                        Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY,
                        replicaDISK != null? replicaDISK.getId() : Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL,
                        key,
                        "");
                }                
            }
            // check replica
            if (isAvailableReplica) {
                if (GWUtils.getLocalIP().equals(replicaDISK.getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        fileReplica = new File(KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                        fileTempReplica = new File(KsanUtils.makeTempPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                        fileTrashReplica = new File(KsanUtils.makeTrashPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                        isCacheReplica = true;
                    } else {
                        fileReplica = new File(KsanUtils.makeObjPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                        fileTempReplica = new File(KsanUtils.makeTempPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                        fileTrashReplica = new File(KsanUtils.makeTrashPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                    }
                    fosReplica = new FileOutputStream(fileTempReplica, false);
                } else {
                    // try {
                    //     // osdClientReplica = OSDClientManager.getInstance().getOSDClient(replicaDISK.getOsdIp());
                    // } catch (Exception e) {
                    //     osdClientReplica = null;
                    // }
                    osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientReplica == null) {
                    //     osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdReplica = true;
                    // }
                    osdClientReplica.putInit(replicaDISK.getPath(),
                        meta.getObjId(),
                        param.getVersionId(),
                        srcMeta.getSize(),
                        Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA,
                        Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL,
                        key,
                        "");
                }
            }

            // MD5
            MessageDigest md5er = null;
            // if (srcMeta.getSize() < 100 * GWConstants.MEGABYTES) {
                md5er = MessageDigest.getInstance(GWConstants.MD5);
            // } else {
            //     md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            // }
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            long totalReads = 0L;

            // copy contents
            if (!srcEncryption.isEnabledEncryption() && !encryption.isEnabledEncryption()) {
                while ((readLength = is.read(buffer, 0, GWConstants.MAXBUFSIZE))!= -1) {
                    totalReads += readLength;
                    if (isAvailablePrimary) {
                        if (fosPrimary != null) {
                            fosPrimary.write(buffer, 0, readLength);
                        } else {
                            osdClientPrimary.put(buffer, 0, readLength);
                        }
                    }
                    if (isAvailableReplica) {
                        if (fosReplica != null) {
                            fosReplica.write(buffer, 0, readLength);
                        } else {
                            osdClientReplica.put(buffer, 0, readLength);
                        }
                    }
                    md5er.update(buffer, 0, readLength);
                }
            } else if (srcEncryption.isEnabledEncryption() && encryption.isEnabledEncryption()) {
                if (fosPrimary != null) {
                    ctrPrimary = GWUtils.initCtrEncrypt(fosPrimary, key);
                }
                if (fosReplica != null) {
                    ctrReplica = GWUtils.initCtrEncrypt(fosReplica, key);
                }
                while ((readLength = encryptIS.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (isAvailablePrimary) {
                        if (ctrPrimary != null) {
                            ctrPrimary.write(buffer, 0, readLength);
                        } else {
                            osdClientPrimary.put(buffer, 0, readLength);
                        }
                    }
                    if (isAvailableReplica) {
                        if (ctrReplica!= null) {
                            ctrReplica.write(buffer, 0, readLength);
                        } else {
                            osdClientReplica.put(buffer, 0, readLength);
                        }
                    }
                    md5er.update(buffer, 0, readLength);
                }
            } else if (!srcEncryption.isEnabledEncryption() && encryption.isEnabledEncryption()) {
                if (fosPrimary != null) {
                    ctrPrimary = GWUtils.initCtrEncrypt(fosPrimary, key);
                }
                if (fosReplica != null) {
                    ctrReplica = GWUtils.initCtrEncrypt(fosReplica, key);
                }
                while ((readLength = is.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (isAvailablePrimary) {
                        if (ctrPrimary != null) {
                            ctrPrimary.write(buffer, 0, readLength);
                        } else {
                            osdClientPrimary.put(buffer, 0, readLength);
                        }
                    }
                    if (isAvailableReplica) {
                        if (ctrReplica!= null) {
                            ctrReplica.write(buffer, 0, readLength);
                        } else {
                            osdClientReplica.put(buffer, 0, readLength);
                        }
                    }
                    md5er.update(buffer, 0, readLength);
                }
            } else {
                // srcEncryption.isEnabledEncryption() && !encryption.isEnabledEncryption()
                while ((readLength = encryptIS.read(buffer, 0, GWConstants.BUFSIZE))!= -1) {
                    totalReads += readLength;
                    if (isAvailablePrimary) {
                        if (fosPrimary != null) {
                            fosPrimary.write(buffer, 0, readLength);
                        } else {
                            osdClientPrimary.put(buffer, 0, readLength);
                        }
                    }
                    if (isAvailableReplica) {
                        if (fosReplica != null) {
                            fosReplica.write(buffer, 0, readLength);
                        } else {
                            osdClientReplica.put(buffer, 0, readLength);
                        }
                    }
                    md5er.update(buffer, 0, readLength);
                }
            }

            byte[] digest = md5er.digest();
            String eTag = base16().lowerCase().encode(digest);

            if (isSrcMultipart) {
                s3Object.setEtag(srcMeta.getEtag());
                s3Object.setFileSize(srcMeta.getSize());
                s3Object.setLastModified(new Date());
                s3Object.setVersionId(param.getVersionId());
                s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
            } else {
                s3Object.setEtag(eTag);
                s3Object.setFileSize(totalReads);
                s3Object.setLastModified(new Date());
                s3Object.setVersionId(param.getVersionId());
                s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
            }
        } catch (FileNotFoundException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (NoSuchAlgorithmException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } finally {
            try {
                if (is != null) {
                    if (srcEncryption.isEnabledEncryption() && encryptIS != null) {
                        encryptIS.close();
                    } else {
                        is.close();
                    }
                } else {
                    if (srcClient != null) {
                        srcClient.close();
                    }
                }
            } catch (IOException e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, param);
            }

            if (isAvailablePrimary) {
                try {
                    if (filePrimary == null) {
                        osdClientPrimary.putFlush();
                        if (isBorrowOsdPrimary) {
                            try {
                                // OSDClientManager.getInstance().releaseOSDClient(osdClientPrimary);
                            } catch (Exception e) {
                                logger.error("release OSDClient error", e);
                            }
                        } else {
                            osdClientPrimary.close();
                        }
                    } else {
                        if (!Strings.isNullOrEmpty(key)) {
                            ctrPrimary.flush();
                            ctrPrimary.close();
                        } else {
                            fosPrimary.flush();
                            fosPrimary.close();
                        }

                        if (filePrimary.exists()) {
                            File tmpFile = new File(filePrimary.getAbsolutePath());
                            retryRenameTo(tmpFile, fileTrashPrimary);
                        }
                        if (meta.isReplicaExist()) {
                            KsanUtils.setAttributeFileReplication(fileTempPrimary, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, replicaDISK.getId());
                        }
                        retryRenameTo(fileTempPrimary, filePrimary);
                        if (isCachePrimary) {
                            String path = KsanUtils.makeObjPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                            Files.createSymbolicLink(Paths.get(path), Paths.get(filePrimary.getAbsolutePath()));
                        }
                    }
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
            if (isAvailableReplica) {
                try {
                    if (fileReplica == null) {
                        osdClientReplica.putFlush();
                        if (isBorrowOsdReplica) {
                            try {
                                // OSDClientManager.getInstance().releaseOSDClient(osdClientReplica);
                            } catch (Exception e) {
                                logger.error("release OSDClient error", e);
                            }
                        } else {
                            osdClientReplica.close();
                        }
                    } else {
                        if (!Strings.isNullOrEmpty(key)) {
                            ctrReplica.flush();
                            ctrReplica.close();
                        } else {
                            fosReplica.flush();
                            fosReplica.close();
                        }

                        if (fileReplica.exists()) {
                            File tmpFile = new File(fileReplica.getAbsolutePath());
                            retryRenameTo(tmpFile, fileTrashReplica);
                        }
                        KsanUtils.setAttributeFileReplication(fileTempReplica, Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        retryRenameTo(fileTempReplica, fileReplica);
                        if (isCacheReplica) {
                            String path = KsanUtils.makeObjPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                            Files.createSymbolicLink(Paths.get(path), Paths.get(fileReplica.getAbsolutePath()));
                        }
                    }
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
        }
        
        return s3Object;
    }

    @Override
    public boolean deleteObject(S3Parameter param, Metadata meta) throws GWException {
        OSDClient client = null;

        File ecFile = new File(KsanUtils.makeECPathForOpen(meta.getPrimaryDisk().getPath(), meta.getObjId(), meta.getVersionId()));
        if (ecFile.exists()) {
            logger.debug("ec exist : {}", ecFile.getAbsolutePath());
            List<ECPart> ecList = new ArrayList<ECPart>();
            for (DiskPool pool : DiskManager.getInstance().getDiskPoolList()) {
                for (Server server : pool.getServerList()) {
                    for (Disk disk : server.getDiskList()) {
                        ECPart ecPart = new ECPart(server.getIp(), disk.getId(), disk.getPath(), false);
                        ecList.add(ecPart);
                    }
                }
            }

            for (ECPart ecPart : ecList) {
                String getPath = KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), meta.getObjId(), meta.getVersionId());
                if (ecPart.getServerIP().equals(GWUtils.getLocalIP())) {
                    File file = new File(getPath);
                    if (file.exists()) {
                        if (file.delete()) {
                            logger.debug("delete ec part : {}", getPath);
                        } else {
                            logger.debug("fail to delete ec part : {}", getPath);
                        }
                    } else {
                        logger.debug("ec part does not exist.", getPath);
                    }
                } else {
                    try {
                        OSDClient ecClient = new OSDClient(ecPart.getServerIP(), (int)GWConfig.getInstance().getOsdPort());
                        logger.debug("delete ec part file : {}, to : {}, {}", getPath, ecPart.getServerIP(), ecPart.getDiskPath());
                        ecClient.deleteECPart(getPath);
                    } catch (Exception e) {
                        PrintStack.logging(logger, e);
                    }
                }
            }
        }

        // delete multipart
        S3Metadata s3Metadata = S3Metadata.getS3Metadata(meta.getMeta());
        if (!Strings.isNullOrEmpty(s3Metadata.getUploadId())) {
            try {
                File multipartFile = null;
                if (GWConfig.getInstance().isCacheDiskpath()) {
                    multipartFile = new File(GWConfig.getInstance().getCacheDiskpath() + KsanUtils.makeObjPathForOpen(meta.getPrimaryDisk().getPath(), meta.getObjId(), meta.getVersionId()));
                } else {
                    multipartFile = new File(KsanUtils.makeObjPathForOpen(meta.getPrimaryDisk().getPath(), meta.getObjId(), meta.getVersionId()));
                }

                if (multipartFile.exists()) {
                    // Thread thread = new Thread() {
                    //     @Override
                    //     public void run() {
                    //         BufferedReader srcBR = new BufferedReader(new FileReader(multipartFile));
                    //         String line = null;
                    //         while ((line = srcBR.readLine()) != null) {
                    //             String[] infos = line.split(GWConstants.COLON);
                    //             String[] uploadInfos = infos[1].split(GWConstants.UNDERSCORE);
                    //             File partFile = new File(infos[1]);
                    //             File trashFile = new File(KsanUtils.makeTrashPath(path, objId, versionId));
                    //             if (partFile.exists()) {
                    //                 logger.error("{} is deleted.", partFile.getAbsolutePath());
                    //                 partFile.delete();
                    //             }
                    //         }
                    //     }
                    // };
                    // thread.start();
                } else {
                    logger.error("{} is not exist.", multipartFile.getAbsolutePath());
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            } catch (Exception e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, param);
            }
            return true;
        }

        try {
            if (meta.getReplicaCount() > 1) {
                if (GWUtils.getLocalIP().equals(meta.getPrimaryDisk().getOsdIp())) {
                    deleteObjectLocal(meta.getPrimaryDisk().getPath(), meta.getObjId(), meta.getVersionId());
                } else {
                    client = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.delete(meta.getPrimaryDisk().getPath(), meta.getObjId(), meta.getVersionId());
                    // OSDClientManager.getInstance().returnOSDClient(client);
                    // client = null;
                } 
                
                if (meta.isReplicaExist()) {
                    if (GWUtils.getLocalIP().equals(meta.getReplicaDisk().getOsdIp())) {
                        deleteObjectLocal(meta.getReplicaDisk().getPath(), meta.getObjId(), meta.getVersionId());
                    } else {
                        client = new OSDClient(meta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        // client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        client.delete(meta.getReplicaDisk().getPath(), meta.getObjId(), meta.getVersionId());
                        // OSDClientManager.getInstance().returnOSDClient(client);
                        // client = null;
                    }
                }
                
            } else {
                if (GWUtils.getLocalIP().equals(meta.getPrimaryDisk().getOsdIp())) {
                    deleteObjectLocal(meta.getPrimaryDisk().getPath(), meta.getObjId(), meta.getVersionId());
                } else {
                    client = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.delete(meta.getPrimaryDisk().getPath(), meta.getObjId(), meta.getVersionId());
                    // OSDClientManager.getInstance().returnOSDClient(client);
                    // client = null;
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } finally {
            if (client != null) {
                try {
                    // OSDClientManager.getInstance().returnOSDClient(client);
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
        }
        logger.info(GWConstants.LOG_S3OBJECT_OPERATION_DELETE, meta.getBucket(), meta.getPath(), meta.getVersionId());
        return true;
    }

    private void deleteObjectLocal(String path, String objId, String versionId) throws IOException, GWException {
        File file = null;
        File trashFile = null;
        if (GWConfig.getInstance().isCacheDiskpath()) {
            file = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeObjPathForOpen(path, objId, versionId)));
            trashFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(path, objId, versionId)));
        } else {
            file = new File(KsanUtils.makeObjPathForOpen(path, objId, versionId));
            trashFile = new File(KsanUtils.makeTrashPath(path, objId, versionId));
        }

        if (file.exists()) {
            retryRenameTo(file, trashFile);
            if (GWConfig.getInstance().isCacheDiskpath()) {
                File link = new File(KsanUtils.makeObjPathForOpen(path, objId, versionId));
                if (!link.delete()) {
                    logger.error("delete link file failed. {}", link.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public S3Object uploadPart(S3Parameter param, Metadata meta) throws GWException {
        // check disk
        boolean isAvailablePrimary = meta.isPrimaryExist() && isAvailableDiskForWrite(meta.getPrimaryDisk().getId());
        boolean isAvailableReplica = false;
        DISK replicaDISK = null;
        if (meta.isReplicaExist()) {
            try {
                replicaDISK = meta.getReplicaDisk();
                isAvailableReplica = isAvailableDiskForWrite(replicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.error("Replica is null");
            }
        }
        if (!isAvailablePrimary && !isAvailableReplica) {
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }

        long length = meta.getSize();
        S3Object s3Object = new S3Object();
        
        InputStream is = param.getInputStream();
        File filePrimary = null;
        File fileReplica = null;
        FileOutputStream fosPrimary = null;
        FileOutputStream fosReplica = null;
        // CtrCryptoOutputStream ctrPrimary = null;
        // CtrCryptoOutputStream ctrReplica = null;
        OSDClient osdClientPrimary = null;
        OSDClient osdClientReplica = null;
        boolean isCachePrimary = false;
        boolean isCacheReplica = false;
        boolean isBorrowOsdPrimary = false;
        boolean isBorrowOsdReplica = false;

        try {
            // MD5
            MessageDigest md5er = null;
            // if (length < 100 * GWConstants.MEGABYTES) {
                md5er = MessageDigest.getInstance(GWConstants.MD5);
            // } else {
            //     md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            // }
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            long totalReads = 0L;

            // check primary
            if (isAvailablePrimary) {
                if (GWUtils.getLocalIP().equals(meta.getPrimaryDisk().getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
                        isCachePrimary = true;
                    } else {
                        filePrimary = new File(KsanUtils.makePartPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
                    }
                    fosPrimary = new FileOutputStream(filePrimary, false);
                } else {
                    // osdClientPrimary = OSDClientManager.getInstance().getOSDClient(meta.getPrimaryDisk().getOsdIp());
                    logger.debug("osd client primary : {}", meta.getPrimaryDisk().getOsdIp());
                    osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientPrimary == null) {
                    //     osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdPrimary = true;
                    // }
                    osdClientPrimary.partInit(meta.getPrimaryDisk().getPath(),
                        meta.getObjId(),
                        param.getUploadId(),
                        String.valueOf(param.getPartNumber()),
                        length,
                        "");
                }
            }

            // check replica
            if (isAvailableReplica) {
                if (GWUtils.getLocalIP().equals(replicaDISK.getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        fileReplica = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
                        isCacheReplica = true;
                    } else {
                        fileReplica = new File(KsanUtils.makePartPath(replicaDISK.getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
                    }
                    fosReplica = new FileOutputStream(fileReplica, false);
                } else {
                    // osdClientReplica = OSDClientManager.getInstance().getOSDClient(replicaDISK.getOsdIp());
                    logger.debug("osd client replica : {}", replicaDISK.getOsdIp());
                    osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientReplica == null) {
                    //     osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdReplica = true;
                    // }
                    osdClientReplica.partInit(replicaDISK.getPath(),
                        meta.getObjId(),
                        param.getUploadId(),
                        String.valueOf(param.getPartNumber()),
                        length,
                        "");
                }
            }

            while ((readLength = is.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                totalReads += readLength;
                if (isAvailablePrimary) {
                    if (fosPrimary == null) {
                        osdClientPrimary.put(buffer, 0, readLength);
                    } else {
                        fosPrimary.write(buffer, 0, readLength);
                    }
                }
                if (isAvailableReplica) {
                    if (fosReplica == null) {
                        osdClientReplica.put(buffer, 0, readLength);
                    } else {
                        fosReplica.write(buffer, 0, readLength);
                    }
                }
                md5er.update(buffer, 0, readLength);
                if (totalReads >= length) {
                    break;
                }
            }

            byte[] digest = md5er.digest();
            String eTag = base16().lowerCase().encode(digest);
            s3Object.setEtag(eTag);
            s3Object.setFileSize(totalReads);
            s3Object.setLastModified(new Date());
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (NoSuchAlgorithmException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } finally {
            if (isAvailablePrimary) {
                try {
                    if (fosPrimary == null) {
                        osdClientPrimary.putFlush();
                    } else {
                        fosPrimary.flush();
                        fosPrimary.close();

                        if (meta.isReplicaExist()) {
                            KsanUtils.setAttributeFileReplication(filePrimary, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, replicaDISK.getId());
                        }
                        
                        if (isCachePrimary) {
                            String path = KsanUtils.makePartPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber());
                            Files.createSymbolicLink(Paths.get(path), Paths.get(filePrimary.getAbsolutePath()));
                        }
                    }
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
            if (isAvailableReplica) {
                try {
                    if (fosReplica == null) {
                        osdClientReplica.putFlush();
                    } else {
                        fosReplica.flush();
                        fosReplica.close();

                        KsanUtils.setAttributeFileReplication(fileReplica, Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        if (isCacheReplica) {
                            String path = KsanUtils.makePartPath(replicaDISK.getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber());
                            Files.createSymbolicLink(Paths.get(path), Paths.get(fileReplica.getAbsolutePath()));
                        }
                    }
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
        }

        return s3Object;
    }

    @Override
    public S3Object uploadPartCopy(S3Parameter param, Metadata srcMeta, S3Encryption srcEncryption, S3Range s3Range,
            Metadata meta) throws GWException {
        // check src disk
        if (srcMeta == null) {
            logger.error("src medata is null.");
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        }

        boolean isAvailableSrcPrimary = srcMeta.isPrimaryExist() && isAvailableDiskForRead(srcMeta.getPrimaryDisk().getId());
        boolean isAvailableSrcReplica = false;
        DISK srcReplicaDISK = null;
        if (srcMeta.isReplicaExist()) {
            try {
                srcReplicaDISK = srcMeta.getReplicaDisk();
                isAvailableSrcReplica = isAvailableDiskForRead(srcReplicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.info("src object replica is null");
            }
        }
        if (!isAvailableSrcPrimary && !isAvailableSrcReplica) {
            logger.error("src object disk is not available.");
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }
        
        // check disk
        boolean isAvailablePrimary = meta.isPrimaryExist() && isAvailableDiskForWrite(meta.getPrimaryDisk().getId());
        boolean isAvailableReplica = false;
        DISK replicaDISK = null;
        if (meta.isReplicaExist()) {
            try {
                replicaDISK = meta.getReplicaDisk();
                isAvailableReplica = isAvailableDiskForWrite(replicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.info("replica is null");
            }
        }
        if (!isAvailablePrimary && !isAvailableReplica) {
            logger.error("object disk is not available.");
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }

        String sourceRange = GWConstants.EMPTY_STRING;
        long length = 0L;
        long offset = 0L;
        long last = 0L;
        boolean hasRange = false;
        if (s3Range != null && !s3Range.getListRange().isEmpty()) {
            for (S3Range.Range range : s3Range.getListRange()) {
                offset = range.getOffset();
                last = range.getLast();
                if (Strings.isNullOrEmpty(sourceRange)) {
                    sourceRange = String.valueOf(offset) + GWConstants.COMMA + String.valueOf(last);
                } else {
                    sourceRange += GWConstants.SLASH + String.valueOf(offset) + GWConstants.COMMA + String.valueOf(last);
                }
                length += last - offset + 1;
            }
            hasRange = true;
        } else {
            length = srcMeta.getSize();
        }

        // check src object is multipart and objId is same
        boolean isMultipart = false;
        S3Metadata srcMetadata = S3Metadata.getS3Metadata(srcMeta.getMeta());
        if (!Strings.isNullOrEmpty(srcMetadata.getUploadId())) { // && srcMeta.getObjId().equals(meta.getObjId())) {
            logger.debug("src object is multipart ...");
            isMultipart = true;
        }

        S3Object s3Object = new S3Object();
        boolean isBorrowSrcOsd = false;
        OSDClient srcClient = null;
        boolean isBorrowOsdPrimary = false;
        boolean isBorrowOsdReplica = false;
        OSDClient osdClientPrimary = null;
        OSDClient osdClientReplica = null;
        InputStream is = null;
        CtrCryptoInputStream encryptIS = null;
        File filePrimary = null;
        File fileReplica = null;
        FileOutputStream fosPrimary = null;
        FileOutputStream fosReplica = null;
        OutputStream osPrimary = null;
        OutputStream osReplica = null;
        boolean isCachePrimary = false;
        boolean isCacheReplica = false;
        
        try {
            File srcFile = null;
            
            
            String localPath = GWConstants.EMPTY_STRING;
            String srcKey = srcEncryption.isEnabledEncryption() ? srcEncryption.getEncryptionKey() : GWConstants.EMPTY_STRING;

            if (isAvailableSrcPrimary && (localPath = DiskManager.getInstance().getLocalPath(srcMeta.getPrimaryDisk().getId())) != null) {
                srcFile = new File(KsanUtils.makeObjPathForOpen(localPath, srcMeta.getObjId(), srcMeta.getVersionId()));
                logger.debug("srcFile : {}", srcFile.getAbsolutePath());
                is = new FileInputStream(srcFile);
                if (!Strings.isNullOrEmpty(srcKey)) {
                    encryptIS = GWUtils.initCtrDecrypt(is, srcKey);
                }
            } 
            // check : replica local
            else if (isAvailableSrcReplica && (localPath = DiskManager.getInstance().getLocalPath(srcReplicaDISK.getId())) != null) {
                srcFile = new File(KsanUtils.makeObjPathForOpen(localPath, srcMeta.getObjId(), srcMeta.getVersionId()));
                is = new FileInputStream(srcFile);
                if (!Strings.isNullOrEmpty(srcKey)) {
                    encryptIS = GWUtils.initCtrDecrypt(is, srcKey);
                }
            } else if (isAvailableSrcPrimary) {
                // get src primary from OSD
                // try {
                //     // srcClient = OSDClientManager.getInstance().getOSDClient(srcMeta.getPrimaryDisk().getOsdIp());
                // } catch (Exception e) {
                //     srcClient = null;
                // }
                srcClient = new OSDClient(srcMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                // if (srcClient == null) {
                //     srcClient = new OSDClient(srcMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                // } else {
                //     isBorrowSrcOsd = true;
                // }
                srcClient.getInit(srcMeta.getPrimaryDisk().getPath(),
                    srcMeta.getObjId(),
                    srcMeta.getVersionId(),
                    srcMeta.getSize(),
                    sourceRange,
                    null,
                    srcKey);
                is = srcClient.getInputStream();
            } else if (isAvailableSrcReplica) {
                // get src replica from OSD
                // try {
                //     // srcClient = OSDClientManager.getInstance().getOSDClient(srcReplicaDISK.getOsdIp());
                // } catch (Exception e) {
                //     srcClient = null;
                // }
                srcClient = new OSDClient(srcReplicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                // if (srcClient == null) {
                //     srcClient = new OSDClient(srcReplicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                // } else {
                //     isBorrowSrcOsd = true;
                // }
                srcClient.getInit(srcReplicaDISK.getPath(),
                    srcMeta.getObjId(),
                    srcMeta.getVersionId(),
                    srcMeta.getSize(),
                    sourceRange,
                    null,
                    srcKey);
                is = srcClient.getInputStream();                    
            }
            
            // String key = encryption.isEnabledEncryption()? encryption.getEncryptionKey() : GWConstants.EMPTY;
            String key = GWConstants.EMPTY_STRING;
            
            // check primary
            if (isAvailablePrimary) {
                if (GWUtils.getLocalIP().equals(meta.getPrimaryDisk().getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
                        isCachePrimary = true;
                    } else {
                        filePrimary = new File(KsanUtils.makePartPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
                    }
                    logger.debug("filePrimary : " + filePrimary.getAbsolutePath());
                    fosPrimary = new FileOutputStream(filePrimary, false);
                    osPrimary = fosPrimary;
                } else {
                    // try {
                    //     // osdClientPrimary = OSDClientManager.getInstance().getOSDClient(meta.getPrimaryDisk().getOsdIp());
                    // } catch (Exception e) {
                    //     osdClientPrimary = null;
                    // }
                    // logger.debug("upload part copy osd : {}", meta.getPrimaryDisk().getOsdIp());
                    osdClientPrimary = new OSDClient(srcReplicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientPrimary == null) {
                    //     osdClientPrimary = new OSDClient(srcReplicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdPrimary = true;
                    // }
                    osdClientPrimary.partInit(meta.getPrimaryDisk().getPath(),
                        meta.getObjId(),
                        param.getUploadId(),
                        String.valueOf(param.getPartNumber()),
                        length,
                        key);
                    osPrimary = osdClientPrimary.getOutputStream();
                }
            }

            // check replica
            if (isAvailableReplica) {
                if (GWUtils.getLocalIP().equals(replicaDISK.getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        fileReplica = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
                        isCacheReplica = true;
                    } else {
                        fileReplica = new File(KsanUtils.makePartPath(replicaDISK.getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
                    }
                    fosReplica = new FileOutputStream(fileReplica, false);
                    osReplica = fosReplica;
                } else {
                    // try {
                    //     // osdClientReplica = OSDClientManager.getInstance().getOSDClient(replicaDISK.getOsdIp());
                    // } catch (Exception e) {
                    //     osdClientReplica = null;
                    // }
                    // logger.debug("upload part copy osd : {}",replicaDISK.getOsdIp());
                    osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientReplica == null) {
                    //     osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdReplica = true;
                    // }
                    osdClientReplica.partInit(replicaDISK.getPath(),
                        meta.getObjId(),
                        param.getUploadId(),
                        String.valueOf(param.getPartNumber()),
                        length,
                        key);
                    osReplica = osdClientReplica.getOutputStream();
                }
            }

            // src object is multipart
            if (isMultipart) {
                BufferedReader br = null;
                br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                // if (!Strings.isNullOrEmpty(srcKey)) {
                //     br = new BufferedReader(new InputStreamReader(encryptIS));
                // } else {
                //     br = new BufferedReader(new InputStreamReader(is));
                // }
                
                String line = null;
                long accOffset = 0L;
                long partLength = 0L;
                Long objOffset = 0L;
                Long objLast = 0L;
                long newOffset = 0L;
                long newLast = 0L;
                boolean isPartRange = false;
                StringBuilder sb = new StringBuilder();

                logger.debug("offset : {}, length : {}", offset, length);
                while ((line = br.readLine())!= null) {
                    logger.debug("read line : " + line);
                    objOffset = 0L;
                    objLast = 0L;
                    
                    String[] infos = line.split(GWConstants.COLON);
                    String objDiskId = infos[0];
                    String objPath = infos[1];
                    Long objSize = Long.parseLong(infos[2]);
                    if (infos.length > 3) {
                        String[] objRanges = infos[3].split(GWConstants.DASH);
                        objOffset = Long.parseLong(objRanges[0]);
                        objLast = Long.parseLong(objRanges[1]);
                        partLength = objLast - objOffset + 1;
                        isPartRange = true;
                    } else {
                        partLength = objSize;
                        isPartRange = false;
                    }
                    logger.debug("accOffset : {}, offset : {}, partLength : {}", accOffset, offset, partLength);
                    if (accOffset < offset) {
                        if (accOffset + partLength <= offset) {
                            accOffset += partLength;
                            continue;
                        }
                    }
                    
                    newOffset = offset - accOffset;
                    if (accOffset + partLength < last) {
                        if (newOffset > 0 && newOffset < partLength) {
                            sb.setLength(0);
                            sb.append(objDiskId);
                            sb.append(GWConstants.COLON);
                            sb.append(objPath);
                            sb.append(GWConstants.COLON);
                            sb.append(objSize);
                            sb.append(GWConstants.COLON);
                            sb.append(newOffset);
                            sb.append(GWConstants.DASH);
                            sb.append(partLength - 1);
                            // if (isPartRange) {
                            //     sb.append(objLast);
                            // } else {
                            //     sb.append(objSize - 1);
                            // }
                            sb.append(System.lineSeparator());
                            if (isAvailablePrimary) {
                                osPrimary.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                            }
                            if (isAvailableReplica) {
                                osReplica.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                            }
                        } else {
                            line += System.lineSeparator();
                            sb.setLength(0);
                            sb.append(line);
                            if (isAvailablePrimary) {
                                osPrimary.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                            }
                            if (isAvailableReplica) {
                                osReplica.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    } else if ((accOffset + partLength - 1) == last) {
                        line += System.lineSeparator();
                        sb.setLength(0);
                        sb.append(line);
                        if (isAvailablePrimary) {
                            osPrimary.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                        }
                        if (isAvailableReplica) {
                            osReplica.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                        }
                        break;
                    } else {
                        // newLast = last - accOffset + 1;
                        newLast = last - accOffset;
                        if (newLast - objOffset + 1 == objSize) {
                            line += System.lineSeparator();
                            sb.setLength(0);
                            sb.append(line);
                            if (isAvailablePrimary) {
                                osPrimary.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                            }
                            if (isAvailableReplica) {
                                osReplica.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                            }
                        } else {
                            sb.setLength(0);
                            sb.append(objDiskId);
                            sb.append(GWConstants.COLON);
                            sb.append(objPath);
                            sb.append(GWConstants.COLON);
                            sb.append(objSize);
                            sb.append(GWConstants.COLON);
                            sb.append(objOffset);
                            sb.append(GWConstants.DASH);
                            sb.append(newLast);
                            sb.append(System.lineSeparator());
                            if (isAvailablePrimary) {
                                osPrimary.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                            }
                            if (isAvailableReplica) {
                                osReplica.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                            }
                        }
                        break;
                    }
                    accOffset += partLength;
                }
                br.close();
                s3Object.setEtag(GWConstants.PARTCOPY_MD5);
            } else {
                // src object is not multipart
                byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
                int readLength = 0;
                int readByte = 0;
                MessageDigest md5er = null;

                // if (length < 100 * GWConstants.MEGABYTES) {
                    md5er = MessageDigest.getInstance(GWConstants.MD5);
                // } else {
                    // md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
                // }
                long remainLength = length;
                if (!Strings.isNullOrEmpty(srcKey)) {
                    if (hasRange) {
                        encryptIS.skip(offset);
                    }
                    
                    while (remainLength > 0L) {
                        if (remainLength > GWConstants.BUFSIZE) {
                            readByte = GWConstants.BUFSIZE;
                        } else {
                            readByte = (int)remainLength;
                        }
                        if ((readLength = encryptIS.read(buffer, 0, readByte)) != -1) {
                            if (isAvailablePrimary) {
                                osPrimary.write(buffer, 0, readByte);
                                osPrimary.flush();
                            }
                            if (isAvailableReplica) {
                                osReplica.write(buffer, 0, readByte);
                                osReplica.flush();
                            }
                            md5er.update(buffer, 0, readLength);
                        } else {
                            break;
                        }
                        remainLength -= readLength;
                    }
                    encryptIS.close();
                } else {
                    if (hasRange) {
                        is.skip(offset);
                    }

                    while (remainLength > 0L) {
                        if (remainLength > GWConstants.MAXBUFSIZE) {
                            readByte = GWConstants.MAXBUFSIZE;
                        } else {
                            readByte = (int)remainLength;
                        }
                        if ((readLength = is.read(buffer, 0, readByte)) != -1) {

                            if (isAvailablePrimary) {
                                osPrimary.write(buffer, 0, readByte);
                                osPrimary.flush();
                            }
                            if (isAvailableReplica) {
                                osReplica.write(buffer, 0, readByte);
                                osReplica.flush();
                            }
                            md5er.update(buffer, 0, readLength);
                        } else {
                            break;
                        }
                        remainLength -= readLength;
                    }
                }
                byte[] digest = md5er.digest();
                String eTag = base16().lowerCase().encode(digest);
                s3Object.setEtag(eTag);
            }
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(length);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (NoSuchAlgorithmException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } finally {
            if (isAvailablePrimary) {
                try {
                    if (filePrimary == null) {
                        osdClientPrimary.putFlush();
                    } else {
                        osPrimary.flush();
                        osPrimary.close();
                        fosPrimary.flush();
                        fosPrimary.close();

                        if (meta.isReplicaExist()) {
                            KsanUtils.setAttributeFileReplication(filePrimary, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, replicaDISK.getId());
                        }
                        
                        if (isCachePrimary) {
                            String path = KsanUtils.makePartPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber());
                            Files.createSymbolicLink(Paths.get(path), Paths.get(filePrimary.getAbsolutePath()));
                        }
                    }
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
            if (isAvailableReplica) {
                try {
                    if (fileReplica == null) {
                        osdClientReplica.putFlush();
                    } else {
                        osReplica.flush();
                        osReplica.close();
                        fosReplica.flush();
                        fosReplica.close();

                        KsanUtils.setAttributeFileReplication(fileReplica, Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        if (isCacheReplica) {
                            String path = KsanUtils.makePartPath(replicaDISK.getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber());
                            Files.createSymbolicLink(Paths.get(path), Paths.get(fileReplica.getAbsolutePath()));
                        }
                    }
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
        }
        logger.debug("s3Object : {}, {}", s3Object.getEtag(), s3Object.getLastModified());
        return s3Object;
    }

    @Override
    public S3Object completeMultipart(S3Parameter param, Metadata meta, S3Encryption encryption,
            SortedMap<Integer, Part> listPart) throws GWException {       
        // check disk
        boolean isAvailablePrimary = meta.isPrimaryExist() && isAvailableDiskForWrite(meta.getPrimaryDisk().getId());
        boolean isAvailableReplica = false;
        DISK replicaDISK = null;
        if (meta.isReplicaExist()) {
            try {
                replicaDISK = meta.getReplicaDisk();
                isAvailableReplica = isAvailableDiskForWrite(replicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.info("replica is null");
            }
        }
        if (!isAvailablePrimary && !isAvailableReplica) {
            logger.error("object disk is not available.");
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }

        S3Object s3Object = new S3Object();
        // save file to local and rename, osd put
        // osd에 put하기 위해서는 length를 알아야 함. 

        String localPath = DiskManager.getInstance().getLocalPath(meta.getPrimaryDisk().getId());
        if (localPath == null) {
            logger.error("local path is null");
            return null;
        }

        OSDClient clientPartPrimary = null;
        OSDClient clientPartReplica = null;
        boolean isBorrowOsdPartPrimary = false;
        boolean isBorrowOsdPartReplica = false;
        OSDClient osdClientPrimary = null;
        OSDClient osdClientReplica = null;
        boolean isBorrowOsdPrimary = false;
        boolean isBorrowOsdReplica = false;
        boolean isCachePrimary = false;
        boolean isCacheReplica = false;
        File partPrimary = null;
        File partReplica = null;
        FileInputStream fisPrimary = null;
        FileInputStream fisReplica = null;
        InputStream isPartPrimary = null;
        InputStream isPartReplica = null;
        BufferedWriter bwPrimary = null;
        BufferedWriter bwReplica = null;

        try {
            File tempPrimary = new File(KsanUtils.makeTempPath(localPath, meta.getObjId(), param.getVersionId()) + ".primary");
            File tempReplica = null;
            bwPrimary = new BufferedWriter(new FileWriter(tempPrimary, StandardCharsets.UTF_8));
            
            if (meta.isReplicaExist()) {
                tempReplica = new File(KsanUtils.makeTempPath(localPath, meta.getObjId(), param.getVersionId()) + ".replica");
                bwReplica = new BufferedWriter(new FileWriter(tempReplica, StandardCharsets.UTF_8));
            }
            
            // create temp file
            long totalLength = 0L;
            String primaryDiskId = null;
            String replicaDiskId = null;
            String primaryPartPath = null;
            String replicaPartPath = null;
            StringBuilder sbPrimary = new StringBuilder();
            StringBuilder sbReplica = new StringBuilder();

            for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, Part> entry = it.next();
                totalLength += entry.getValue().getPartSize();
                primaryDiskId = entry.getValue().getPrimaryDiskId();
                replicaDiskId = entry.getValue().getReplicaDiskId();
                if (primaryDiskId != null) {
                    primaryPartPath = DiskManager.getInstance().getPath(primaryDiskId);
                }
                if (replicaDiskId != null) {
                    replicaPartPath = DiskManager.getInstance().getPath(replicaDiskId);
                }
                logger.debug("primaryDiskId : {} - {}, replicaDiskId : {} - {}", primaryDiskId, DiskManager.getInstance().getOSDIP(primaryDiskId), replicaDiskId, DiskManager.getInstance().getOSDIP(replicaDiskId));
                // part copy file
                if (entry.getValue().getPartETag().equals(GWConstants.PARTCOPY_MD5)) {
                    boolean isAvailablePartPrimary = primaryDiskId != null && isAvailableDiskForRead(primaryDiskId);
                    boolean isAvailablePartReplica = replicaDiskId != null && isAvailableDiskForRead(replicaDiskId);
                    
                    if (isAvailablePartPrimary) {
                        if ((localPath = DiskManager.getInstance().getLocalPath(primaryDiskId)) != null) {
                            partPrimary = new File(KsanUtils.makePartPath(localPath, meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                            fisPrimary = new FileInputStream(partPrimary);
                            isPartPrimary = fisPrimary;
                        } else {
                            // try {
                            //     // clientPartPrimary = OSDClientManager.getInstance().getOSDClient(DiskManager.getInstance().getOSDIP(primaryDiskId));
                            // } catch (Exception e) {
                            //     clientPartPrimary = null;
                            // }
                            clientPartPrimary = new OSDClient(DiskManager.getInstance().getOSDIP(primaryDiskId), (int)GWConfig.getInstance().getOsdPort());
                            // if (clientPartPrimary == null) {
                            //     clientPartPrimary = new OSDClient(DiskManager.getInstance().getOSDIP(primaryDiskId), (int)GWConfig.getInstance().getOsdPort());
                            // } else {
                            //     isBorrowOsdPartPrimary = true;
                            // }
                            // client.getPartInit(objPath, objSize, partRange, os);
                            String path = KsanUtils.makePartPath(DiskManager.getInstance().getPath(primaryDiskId), meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber()));
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            clientPartPrimary.getPartInit(path, entry.getValue().getPartSize(), GWConstants.EMPTY_STRING, baos);
                            isPartPrimary = new ByteArrayInputStream(baos.toByteArray());
                        }
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(isPartPrimary, StandardCharsets.UTF_8))) {
                            String line = null;
                            while ((line = br.readLine()) != null) {
                                line += System.lineSeparator();
                                if (bwPrimary != null) {
                                    bwPrimary.write(line);
                                }
                            }
                        } catch (IOException e) {
                            PrintStack.logging(logger, e);
                            throw new GWException(GWErrorCode.SERVER_ERROR, param);
                        }
                    }
                    if (isAvailablePartReplica) {
                        if ((localPath = DiskManager.getInstance().getLocalPath(replicaDiskId)) != null) {
                            partReplica = new File(KsanUtils.makePartPath(localPath, meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                            fisReplica = new FileInputStream(partReplica);
                            isPartReplica = fisReplica;
                        } else {
                            // try {
                            //     // clientPartReplica = OSDClientManager.getInstance().getOSDClient(DiskManager.getInstance().getOSDIP(replicaDiskId));
                            // } catch (Exception e) {
                            //     clientPartReplica = null;
                            // }
                            clientPartReplica = new OSDClient(DiskManager.getInstance().getOSDIP(replicaDiskId), (int)GWConfig.getInstance().getOsdPort());
                            // if (clientPartReplica == null) {
                            //     clientPartReplica = new OSDClient(DiskManager.getInstance().getOSDIP(replicaDiskId), (int)GWConfig.getInstance().getOsdPort());
                            // } else {
                            //     isBorrowOsdPartReplica = true;
                            // }
                            String path = KsanUtils.makePartPath(DiskManager.getInstance().getPath(replicaDiskId), meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber()));
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            clientPartReplica.getPartInit(path, entry.getValue().getPartSize(), GWConstants.EMPTY_STRING, baos);
                            isPartReplica = new ByteArrayInputStream(baos.toByteArray());
                        }
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(isPartReplica, StandardCharsets.UTF_8))) {
                            String line = null;
                            while ((line = br.readLine()) != null) {
                                line += System.lineSeparator();
                                if (bwReplica != null) {
                                    bwReplica.write(line);
                                }
                            }
                        } catch (IOException e) {
                            PrintStack.logging(logger, e);
                            throw new GWException(GWErrorCode.SERVER_ERROR, param);
                        }
                    }
                } else {
                    if (primaryPartPath != null) {
                        sbPrimary.setLength(0);
                        sbPrimary.append(primaryDiskId);
                        sbPrimary.append(GWConstants.COLON);
                        sbPrimary.append(KsanUtils.makePartPath(primaryPartPath, meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                        sbPrimary.append(GWConstants.COLON);
                        sbPrimary.append(entry.getValue().getPartSize());
                        sbPrimary.append(GWConstants.COLON);
                        sbPrimary.append(System.lineSeparator());
                        if (bwPrimary != null) {
                            bwPrimary.write(sbPrimary.toString());
                        }
                    }
                    if (replicaPartPath != null) {
                        sbReplica.setLength(0);
                        sbReplica.append(replicaDiskId);
                        sbReplica.append(GWConstants.COLON);
                        sbReplica.append(KsanUtils.makePartPath(replicaPartPath, meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                        sbReplica.append(GWConstants.COLON);
                        sbReplica.append(entry.getValue().getPartSize());
                        sbReplica.append(GWConstants.COLON);
                        sbReplica.append(System.lineSeparator());
                        if (bwReplica != null) {
                            bwReplica.write(sbReplica.toString());
                        }
                    }
                }
            }
                
            // rename or osd put
            // String key = encryption.isEnabledEncryption() ? encryption.getEncryptionKey() : GWConstants.EMPTY_STRING;
            String key = GWConstants.EMPTY_STRING;
            File filePrimary = null;
            File fileReplica = null;
            File fileTrashPrimary = null;
            File fileTrashReplica = null;

            if (isAvailablePrimary) {
                if (GWUtils.getLocalIP().equals(meta.getPrimaryDisk().getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                        fileTrashPrimary = new File(KsanUtils.makeTrashPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                        isCachePrimary = true;
                    } else {
                        filePrimary = new File(KsanUtils.makeObjPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                        fileTrashPrimary = new File(KsanUtils.makeTrashPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
                    }
                    // rename
                    if (filePrimary.exists()) {
                        File tmpFile = new File(filePrimary.getAbsolutePath());
                        retryRenameTo(tmpFile, fileTrashPrimary);
                    }
                    if (meta.isReplicaExist()) {
                        KsanUtils.setAttributeFileReplication(tempPrimary, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, replicaDISK.getId());
                    }
                    retryRenameTo(tempPrimary, filePrimary);
                    if (isCachePrimary) {
                        String path = KsanUtils.makeObjPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                        Files.createSymbolicLink(Paths.get(path), Paths.get(filePrimary.getAbsolutePath()));
                    }
                } else {
                    // try {
                    //     // osdClientPrimary = OSDClientManager.getInstance().getOSDClient(meta.getPrimaryDisk().getOsdIp());
                    // } catch (Exception e) {
                    //     osdClientPrimary = null;
                    // }
                    osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientPrimary == null) {
                    //     osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdPrimary = true;
                    // }
                    logger.debug("temp file length : {}", tempPrimary.length());
                    osdClientPrimary.putInit(meta.getPrimaryDisk().getPath(),
                        meta.getObjId(),
                        param.getVersionId(),
                        tempPrimary.length(),
                        Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY,
                        replicaDISK != null? replicaDISK.getId() : Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL,
                        key,
                        "");
                    try (FileInputStream fis = new FileInputStream(tempPrimary)) {
                        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
                        int readLength = 0;
                        while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE))!= -1) {
                            osdClientPrimary.put(buffer, 0, readLength);
                        }
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR, param);
                    } finally {
                        if (!tempPrimary.delete()) {
                            logger.error("temp file delete fail : {}", tempPrimary.getAbsolutePath());
                        }
                    }
                }
            }
            if (isAvailableReplica) {
                if (GWUtils.getLocalIP().equals(replicaDISK.getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        fileReplica = new File(KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                        fileTrashReplica = new File(KsanUtils.makeTrashPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                        isCacheReplica = true;
                    } else {
                        fileReplica = new File(KsanUtils.makeObjPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                        fileTrashReplica = new File(KsanUtils.makeTrashPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId()));
                    }
                    // rename
                    if (fileReplica.exists()) {
                        File tmpFile = new File(fileReplica.getAbsolutePath());
                        retryRenameTo(tmpFile, fileTrashReplica);
                    }
                    if (meta.isReplicaExist()) {
                        KsanUtils.setAttributeFileReplication(tempPrimary, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, replicaDISK.getId());
                    }
                    retryRenameTo(tempReplica, fileReplica);
                    if (isCacheReplica) {
                        String path = KsanUtils.makeObjPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                        Files.createSymbolicLink(Paths.get(path), Paths.get(fileReplica.getAbsolutePath()));
                    }
                } else {
                    // try {
                    //     // osdClientReplica = OSDClientManager.getInstance().getOSDClient(replicaDISK.getOsdIp());
                    // } catch (Exception e) {
                    //     osdClientReplica = null;
                    // }
                    osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientReplica == null) {
                    //     osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdReplica = true;
                    // }
                    logger.debug("temp file length : {}", tempReplica.length());
                    osdClientReplica.putInit(replicaDISK.getPath(),
                        meta.getObjId(),
                        param.getVersionId(),
                        tempReplica.length(),
                        Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY,
                        Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL,
                        key,
                        "");
                    try (FileInputStream fis = new FileInputStream(tempReplica)) {
                        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
                        int readLength = 0;
                        while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE))!= -1) {
                            osdClientReplica.put(buffer, 0, readLength);
                        }
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR, param);
                    } finally {
                        if (!tempReplica.delete()) {
                            logger.error("tempReplica delete fail : {}", tempReplica.getAbsolutePath());
                        }
                    }
                }
            }

            s3Object.setLastModified(new Date());
            s3Object.setFileSize(totalLength);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } finally {
            if (bwPrimary != null) {
                try {
                    bwPrimary.flush();
                    bwPrimary.close();
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }
            if (bwReplica != null) {
                try {
                    bwReplica.flush();
                    bwReplica.close();
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                }
            }

            if (isBorrowOsdPartPrimary) {
                try {
                    // OSDClientManager.getInstance().releaseOSDClient(clientPartPrimary);
                } catch (Exception e) {
                    logger.error("release osdClientPartPrimary error : {}", e.getMessage());
                }
            } else {
                if (clientPartPrimary != null) {
                    clientPartPrimary.close();
                }
            }
            if (isBorrowOsdPartReplica) {
                try {
                    // OSDClientManager.getInstance().releaseOSDClient(clientPartReplica);
                } catch (Exception e) {
                    logger.error("release osdClientPartReplica error : {}", e.getMessage());
                }
            } else {
                if (clientPartReplica != null) {
                    clientPartReplica.close();
                }
            }
            if (isBorrowOsdPrimary) {
                try {
                    // OSDClientManager.getInstance().releaseOSDClient(osdClientPrimary);
                } catch (Exception e) {
                    logger.error("release osdClientPrimary error : {}", e.getMessage());
                }
            } else {
                if (osdClientPrimary != null) {
                    osdClientPrimary.close();
                }
            }
            if (isBorrowOsdReplica) {
                try {
                    // OSDClientManager.getInstance().releaseOSDClient(osdClientReplica);
                } catch (Exception e) {
                    logger.error("release osdClientReplica error : {}", e.getMessage());
                }
            } else {
                if (osdClientReplica != null) {
                    osdClientReplica.close();
                }
            }
        }

        return s3Object;
    }

    @Override
    public void abortMultipart(S3Parameter param, Metadata meta, SortedMap<Integer, Part> listPart) throws GWException {
        long totalLength = 0L;
        String primaryDiskId = null;
        String replicaDiskId = null;
        String primaryPartPath = null;
        String replicaPartPath = null;
        File partFilePrimary = null;
        File partFileReplica = null;
        File partFileTrashPrimary = null;
        File partFileTrashReplica = null;
        OSDClient clientPartPrimary = null;
        OSDClient clientPartReplica = null;
        boolean isBorrowOsdPartPrimary = false;
        boolean isBorrowOsdPartReplica = false;
        String localPath = null;

        try {
            for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, Part> entry = it.next();
                primaryDiskId = entry.getValue().getPrimaryDiskId();
                replicaDiskId = entry.getValue().getReplicaDiskId();
                if (primaryDiskId != null) {
                    primaryPartPath = DiskManager.getInstance().getPath(primaryDiskId);
                }
                if (replicaDiskId != null) {
                    replicaPartPath = DiskManager.getInstance().getPath(replicaDiskId);
                }

                // primary
                if ((localPath = DiskManager.getInstance().getLocalPath(primaryDiskId)) != null) {
                    partFilePrimary = new File(KsanUtils.makePartPath(localPath, meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                    partFileTrashPrimary = new File(KsanUtils.makeTrashPath(localPath, meta.getObjId(), param.getUploadId()));
                    if (!partFilePrimary.exists()) {
                        retryRenameTo(partFilePrimary, partFileTrashPrimary);
                    }
                } else {
                    // clientPartPrimary = OSDClientManager.getInstance().getOSDClient(DiskManager.getInstance().getOSDIP(primaryDiskId));
                    clientPartPrimary = new OSDClient(DiskManager.getInstance().getOSDIP(primaryDiskId), (int)GWConfig.getInstance().getOsdPort());
                    // if (clientPartPrimary == null) {
                    //     clientPartPrimary = new OSDClient(DiskManager.getInstance().getOSDIP(primaryDiskId), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdPartPrimary = true;
                    // }
                    clientPartPrimary.deletePart(DiskManager.getInstance().getPath(primaryDiskId), meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber()));
                    if (isBorrowOsdPartPrimary) {
                        // OSDClientManager.getInstance().releaseOSDClient(clientPartPrimary);
                    } else {
                        clientPartPrimary.close();
                    }
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        }
    }
    
    private boolean isAvailableDiskForRead(String diskId) {
        return DiskManager.getInstance().isAvailableDiskForRead(diskId);
    }

    private boolean isAvailableDiskForWrite(String diskId) {
        return DiskManager.getInstance().isAvailableDiskForWrite(diskId);
    }

    private long getECObject(S3Parameter param, Metadata meta, String key, String range) throws GWException {
        String versionId = param.getVersionId();
        String localPath = null;

        HashMap<String, String> localDiskInfo = DiskManager.getInstance().getLocalDiskInfo();
        if (localDiskInfo == null || localDiskInfo.size() == 0) {
            logger.info("local disk info list is null");
            return -1;
        } else {
            for (Map.Entry<String, String> entry : localDiskInfo.entrySet()) {
                if (isAvailableDiskForRead(entry.getKey())) {
                    localPath = entry.getValue();
                    break;
                }
            }
            // Set<String> diskIds = localDiskInfo.keySet();
            // for (String diskId : diskIds) {
            //     if (isAvailableDiskForRead(diskId)) {
            //         localPath = localDiskInfo.get(diskId);
            //         break;
            //     }
            // }
            // for (int i = 0; i < diskIds.size(); i++) {
            //     String diskId = diskIds.iterator().next();
            //     if (isAvailableDiskForRead(diskId)) {
            //         localPath = localDiskInfo.get(diskId);
            //         break;
            //     }
            // }
            if (localPath == null) {
                logger.info("local disk is not available for read");
                return -1;
            }
        }

        // EC가 적용된 파일은 모든 disk에 분산 되어 있기 때문에 LocalDisk path에서 파일을 찾는다.
        File file = new File(KsanUtils.makeECPathForOpen(localPath, meta.getObjId(), versionId));
        if (!file.exists()) {
            return -1;
        }

        List<ECPart> ecList = new ArrayList<ECPart>();
        for (DiskPool pool : DiskManager.getInstance().getDiskPoolList()) {
            for (Server server : pool.getServerList()) {
                for (Disk disk : server.getDiskList()) {
                    if (isAvailableDiskForRead(disk.getId())) {
                        ECPart ecPart = new ECPart(server.getIp(), disk.getId(), disk.getPath(), false);
                        ecList.add(ecPart);
                    }
                }
            }
        }
        
        int numberOfCodingChunks = DiskManager.getInstance().getECM(meta.getPrimaryDisk().getId());
        int numberOfDataChunks = DiskManager.getInstance().getECK(meta.getPrimaryDisk().getId());
        
        int getECPartCount = 0;
        try {
            for (ECPart ecPart : ecList) {
                String newECPartPath = file.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
                File newECPartFile = new File(newECPartPath);
                if (ecPart.getServerIP().equals(GWUtils.getLocalIP())) {
                    // if local disk, move file
                    File sourceECPartFile = new File(KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), meta.getObjId(), versionId));
                    if (sourceECPartFile.exists()) {
                        FileUtils.copyFile(sourceECPartFile, newECPartFile);
                        ecPart.setProcessed(true);
                    } else {
                        logger.info("ec part does not exist. {}", sourceECPartFile.getAbsolutePath());
                    }
                } else {
                    try (FileOutputStream fos = new FileOutputStream(newECPartFile)) {
                        String getPath = KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), meta.getObjId(), versionId);
                        // OSDClient client = OSDClientManager.getInstance().getOSDClient(ecPart.getServerIP());
                        OSDClient client = new OSDClient(ecPart.getServerIP(), (int)GWConfig.getInstance().getOsdPort());
                        logger.debug("get ec part file : {}, to : {}, {}", getPath, ecPart.getServerIP(), ecPart.getDiskPath());
                        client.getECPartInit(getPath, fos);
                        client.getECPart();
                        client.close();
                        ecPart.setProcessed(true);
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                    }
                }
                getECPartCount++;
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        }
        logger.debug("numberOfCodingChunks : {}, numberOfDataChunks : {}, numberOfECPart : {}", numberOfCodingChunks, numberOfDataChunks, getECPartCount);
        
        // zunfec
        String ecAllFilePath = KsanUtils.makeECPathForOpen(localPath, meta.getObjId(), versionId);
        String command = "";
        getECPartCount = 0;
        StringBuffer buf = new StringBuffer();
        buf.append(Constants.ZUNFEC + ecAllFilePath);
        for (ECPart ecPart : ecList) {
            String ecPartPath = file.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
            if (ecPart.isProcessed()) {
                buf.append(Constants.SPACE + ecPartPath);
                getECPartCount++;
            }
        }
        command = buf.toString();
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_ZUNFEC_COMMAND, command);
        
        try {
            Process p = Runtime.getRuntime().exec(command);
            int exitCode = p.waitFor();
            p.destroy();
            logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_ZUNFEC_DECODE_EXIT_VALUE, exitCode);
        } catch (InterruptedException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        }

        // delete junk file
        String ecDir = KsanUtils.makeECDirectoryPath(localPath, meta.getObjId());
        File dir = new File(ecDir);
        File[] ecFiles = dir.listFiles();
        if (ecFiles != null) {
            for (int i = 0; i < ecFiles.length; i++) {
                if (ecFiles[i].getName().startsWith(Constants.POINT) && ecFiles[i].getName().charAt(ecFiles[i].getName().length() - 2) == Constants.CHAR_POINT) {
                    String trashPath = KsanUtils.makeTrashPath(localPath, ecFiles[i].getName());
                    File trashECPart = new File(trashPath);
                    retryRenameTo(ecFiles[i], trashECPart);
                    // ecFiles[i].delete();
                }
            }
        }

        File ecAllFile = new File(ecAllFilePath);
        return getObjectLocal(param, ecAllFile, range, key);
    }

    private long getObjectLocal(S3Parameter param, File file, String sourceRange, String key) throws GWException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        CtrCryptoInputStream encryptIS = null;
        long actualSize = 0L;
        long remaingLength = 0L;
        int readLength = 0;
        int readBytes;

        try (FileInputStream fis = new FileInputStream(file);
            OutputStream outputStream = param.getResponse().getOutputStream();) {
            
            if (Strings.isNullOrEmpty(sourceRange)) {
                if (!Strings.isNullOrEmpty(key)) {
                    encryptIS = GWUtils.initCtrDecrypt(fis, key);
                    while ((readLength = encryptIS.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                        actualSize += readLength;
                        outputStream.write(buffer, 0, readLength);
                    }
                } else {
                    remaingLength = file.length();
                    while (remaingLength > 0) {
                        readBytes = 0;
                        if (remaingLength < GWConstants.MAXBUFSIZE) {
                            readBytes = (int)remaingLength;
                        } else {
                            readBytes = GWConstants.MAXBUFSIZE;
                        }
                        if ((readLength = fis.read(buffer, 0, readBytes)) != -1) {
                            actualSize += readLength;
                            outputStream.write(buffer, 0, readLength);
                            remaingLength -= readLength;
                        } else {
                            break;
                        }
                    }

                }
            } else {
                String[] ranges = sourceRange.split(GWConstants.SLASH);
                long offset = 0L;
                long length = 0L;
                for (String range : ranges) {
                    String[] rangeParts = range.split(GWConstants.COMMA);
                    Long offsetLong = Longs.tryParse(rangeParts[0]);
                    Long lengthLong = Longs.tryParse(rangeParts[1]);
                    offset = (offsetLong == null) ? 0L : offsetLong;
                    length = (lengthLong == null) ? 0L : lengthLong;
                    logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_RANGE, offset, length);

                    remaingLength = length;
                    
                    if (!Strings.isNullOrEmpty(key)) {
                        encryptIS = GWUtils.initCtrDecrypt(fis, key);
                        if (offset > 0) {
                            encryptIS.skip(offset);
                        }
                        while (remaingLength > 0) {
                            readBytes = 0;
                            if (remaingLength < GWConstants.MAXBUFSIZE) {
                                readBytes = (int)remaingLength;
                            } else {
                                readBytes = GWConstants.MAXBUFSIZE;
                            }
                            if ((readLength = encryptIS.read(buffer, 0, readBytes)) != -1) {
                                actualSize += readLength;
                                outputStream.write(buffer, 0, readLength);
                                remaingLength -= readLength;
                            } else {
                                break;
                            }
                        }
                    } else {
                        if (offset > 0) {
                            fis.skip(offset);
                        }
                        while (remaingLength > 0) {
                            readBytes = 0;
                            if (remaingLength < GWConstants.MAXBUFSIZE) {
                                readBytes = (int)remaingLength;
                            } else {
                                readBytes = GWConstants.MAXBUFSIZE;
                            }
                            if ((readLength = fis.read(buffer, 0, readBytes)) != -1) {
                                actualSize += readLength;
                                outputStream.write(buffer, 0, readLength);
                                remaingLength -= readLength;
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            outputStream.flush();
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } finally {
            try {
                if (encryptIS != null) {
                    encryptIS.close();
                }
            } catch (IOException e) {
                PrintStack.logging(logger, e);
            }
        }

        return actualSize;
    }

    private long getObjectOSD(S3Parameter param, Metadata meta, DISK disk, String sourceRange, String key) throws GWException {
        long actualSize = 0L;
        OSDClient client = null;
        boolean isBorrowOsd = false;
        try {
            // try {
            //     // client = OSDClientManager.getInstance().getOSDClient(disk.getOsdIp());
            // } catch (Exception e) {
            //     client = null;
            // }
            client = new OSDClient(disk.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
            // if (client == null) {
            //     client = new OSDClient(disk.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
            // } else {
            //     isBorrowOsd = true;
            // }

            client.getInit(disk.getPath(),
                meta.getObjId(),
                param.getVersionId(),
                meta.getSize(),
                sourceRange,
                param.getResponse().getOutputStream(),
                key);
                
            actualSize = client.get();
            if (isBorrowOsd) {
                try {
                    // OSDClientManager.getInstance().releaseOSDClient(client);
                } catch (Exception e) {
                    logger.error("release OSDClient error : {}", e.getMessage());
                }
            } else {
                client.close();
            }
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        }

        return actualSize;
    }

    private long getMultipart(boolean isAvailablePrimary, boolean isAvailableReplica, S3Parameter param, Metadata meta, String key, String range) throws GWException {
        logger.debug("getMultipart ... : {}, {}", meta.getPath(), meta.getObjId());
        String localPath = null;
        long actualSize = -1;
        DISK replicaDISK = null;
        if (isAvailableReplica) {
            try {
                replicaDISK = meta.getReplicaDisk();
            } catch (ResourceNotFoundException e) {
                logger.debug("Replica is null");
            }
        }

        // check : primary local
        if (isAvailablePrimary && (localPath = DiskManager.getInstance().getLocalPath(meta.getPrimaryDisk().getId())) != null) {
            logger.debug("getMultipart : primary local : {}", localPath);
            return getMultipartLocal(param, localPath, meta, key, range);
        } 
        // check : replica local
        else if (isAvailableReplica && (replicaDISK != null) && (localPath = DiskManager.getInstance().getLocalPath(replicaDISK.getId())) != null) {
            logger.debug("getMultipart : replica local : {}", localPath);
            return getMultipartLocal(param, localPath, meta, key, range);
        } else if (isAvailablePrimary) {
            logger.debug("getMultipart : primary osd : {}:{}", meta.getPrimaryDisk().getOsdIp(), meta.getPrimaryDisk().getPath());
            return getMultipartOSD(param, meta, meta.getPrimaryDisk(), key, range);
        } else if (isAvailableReplica) {
            logger.debug("getMultipart : replica osd : {}:{}", replicaDISK.getOsdIp(), meta.getPrimaryDisk().getPath());
            return getMultipartOSD(param, meta, replicaDISK, key, range);
        }

        return actualSize;
    }

    private long getMultipartLocal(S3Parameter param, String localPath, Metadata meta, String key, String sourceRange) throws GWException {
        logger.debug("getMultipartLocal... : {}, {}", meta.getPath(), meta.getObjId());
        long actualSize = 0L;
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File file = new File(KsanUtils.makeObjPathForOpen(localPath, meta.getObjId(), meta.getVersionId()));
        boolean isBorrowOsd = false;
        logger.debug("getMultipartLocal file : {}", file.getAbsolutePath());
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file,StandardCharsets.UTF_8));
                OutputStream os = param.getResponse().getOutputStream()) {
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
                    String[] ranges = sourceRange.split(GWConstants.SLASH);
                    long offset = 0L;
                    long length = 0L;
                    for (String range : ranges) {
                        long accOffset = 0L;
                        String[] rangeParts = range.split(GWConstants.COMMA);
                        Long offsetLong = Longs.tryParse(rangeParts[0]);
                        Long lengthLong = Longs.tryParse(rangeParts[1]);
                        offset = (offsetLong == null) ? 0L : offsetLong;
                        length = (lengthLong == null) ? 0L : lengthLong;
                        logger.debug("getMultipartLocal : range : offset={}, length={}", offset, length);
                        br.reset();
                        while ((line = br.readLine()) != null) {
                            String[] infos = line.split(GWConstants.COLON);
                            objDiskId = infos[0];
                            objPath = infos[1];
                            objSize = Long.parseLong(infos[2]);
                            long saveRemaingLength = 0L;
                            objOffset = 0L;
                            objLast = 0L;

                            if (infos.length > 3) {
                                String[] objRanges = infos[3].split(GWConstants.DASH);
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
                                logger.debug("from local : {}, length : {}, remaingLength : {}, objLength : {}", partFile.getAbsolutePath(), length, remaingLength, objLength);
                                try (FileInputStream fis = new FileInputStream(partFile)) {
                                    if (isRange) {
                                        fis.skip(objOffset);
                                    }
                                    while (remaingLength > 0) {
                                        readBytes = 0;
                                        if (remaingLength < GWConstants.MAXBUFSIZE) {
                                            readBytes = (int)remaingLength;
                                        } else {
                                            readBytes = GWConstants.MAXBUFSIZE;
                                        }
                                        if ((readLength = fis.read(buffer, 0, readBytes)) != -1) {
                                            actualSize += readLength;
                                            os.write(buffer, 0, readLength);
                                            remaingLength -= readLength;
                                        } else {
                                            break;
                                        }
                                    }
                                } catch (Exception e1) {
                                    PrintStack.logging(logger, e1);
                                    throw new GWException(GWErrorCode.SERVER_ERROR, param);
                                }
                            } else {
                                // get osd
                                logger.debug("from osd : {}, length : {}, remaingLength : {}, objLength : {}", DiskManager.getInstance().getOSDIP(objDiskId), length, remaingLength, objLength);
                                // OSDClient client = OSDClientManager.getInstance().getOSDClient(DiskManager.getInstance().getOSDIP(objDiskId));
                                OSDClient client = new OSDClient(DiskManager.getInstance().getOSDIP(objDiskId), (int)GWConfig.getInstance().getOsdPort());
                                isBorrowOsd = false;
                                // if (client == null) {
                                //     client = new OSDClient(DiskManager.getInstance().getOSDIP(objDiskId), (int)GWConfig.getInstance().getOsdPort());
                                //     isBorrowOsd = false;
                                // } else {
                                //     isBorrowOsd = true;
                                // }
                                String partRange = null;
                                if (isRange) {
                                    partRange = objOffset + GWConstants.DASH + (objOffset + objLength - 1);
                                } else {
                                    partRange = GWConstants.EMPTY_STRING;
                                }
                                if (remaingLength < objLength) {
                                    partRange = objOffset + GWConstants.DASH + (objOffset + remaingLength - 1);
                                }

                                client.getPartInit(objPath, remaingLength, partRange, os);
                                actualSize += client.getPart();
                                if (isBorrowOsd) {
                                    try {
                                        // OSDClientManager.getInstance().releaseOSDClient(client);
                                    } catch (Exception e) {
                                        PrintStack.logging(logger, e);
                                    }
                                } else {
                                    client.close();
                                }
                            }

                            length -= saveRemaingLength;
                            if (length <= 0) {
                                break;
                            }
                            logger.debug("actualSize : {}, length : {}", actualSize, length);
                        }
                    }
                } else {
                    while ((line = br.readLine()) != null) {
                        String[] infos = line.split(GWConstants.COLON);
                        objDiskId = infos[0];
                        objPath = infos[1];
                        objSize = Long.parseLong(infos[2]);
                        if (infos.length > 3) {
                            String[] objRanges = infos[3].split(GWConstants.DASH);
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
                            logger.info("partFile : {}, file size : {}, remaingLength : {}", partFile.getAbsolutePath(), partFile.length(), remaingLength);
                            try (FileInputStream fis = new FileInputStream(partFile)) {
                                if (isRange) {
                                    fis.skip(objOffset);
                                }

                                while (remaingLength > 0) {
                                    readBytes = 0;
                                    if (remaingLength < GWConstants.MAXBUFSIZE) {
                                        readBytes = (int)remaingLength;
                                    } else {
                                        readBytes = GWConstants.MAXBUFSIZE;
                                    }
                                    if ((readLength = fis.read(buffer, 0, readBytes)) != -1) {
                                        actualSize += readLength;
                                        os.write(buffer, 0, readLength);
                                        remaingLength -= readLength;
                                    } else {
                                        break;
                                    }
                                }
                            } catch (Exception e1) {
                                PrintStack.logging(logger, e1);
                                throw new GWException(GWErrorCode.SERVER_ERROR, param);
                            }
                        } else {
                            // get osd
                            // OSDClient client = OSDClientManager.getInstance().getOSDClient(DiskManager.getInstance().getOSDIP(objDiskId));
                            OSDClient client = new OSDClient(DiskManager.getInstance().getOSDIP(objDiskId), (int)GWConfig.getInstance().getOsdPort());
                            isBorrowOsd = false;
                            // if (client == null) {
                            //     client = new OSDClient(DiskManager.getInstance().getOSDIP(objDiskId), (int)GWConfig.getInstance().getOsdPort());
                            //     isBorrowOsd = false;
                            // } else {
                            //     isBorrowOsd = true;
                            // }
                            String partRange = null;
                            if (isRange) {
                                partRange = infos[3];
                            } else {
                                partRange = GWConstants.EMPTY_STRING;
                            }
                            client.getPartInit(objPath, objSize, partRange, os);
                            actualSize += client.getPart();
                            if (isBorrowOsd) {
                                try {
                                    // OSDClientManager.getInstance().releaseOSDClient(client);
                                } catch (Exception e) {
                                    PrintStack.logging(logger, e);
                                }
                            } else {
                                client.close();
                            }
                        }
                    }
                }
            } catch (RuntimeException e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, param);
            } catch (Exception e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, param);
            }
        } else {
            logger.error("not found multipart file : " + file.getAbsolutePath());
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        }

        return actualSize;
    }

    private long getMultipartOSD(S3Parameter param, Metadata meta, DISK disk, String key, String range) throws GWException {
        long actualSize = 0L;
        boolean isBorrowOsdPrimary = false;
        String path = DiskManager.getInstance().getPath(disk.getId());
        OSDClient osdClient = null;
        logger.debug("osd ip : {}", disk.getOsdIp());
        
        // try {
        //     // osdClient = OSDClientManager.getInstance().getOSDClient(disk.getOsdIp());
        //     logger.debug("get osd client from pool");
        // } catch (Exception e) {
        //     osdClient = null;
        // }

        // try {
        //     if (osdClient == null) {
        //         osdClient = new OSDClient(disk.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
        //     } else {
        //         isBorrowOsdPrimary = true;
        //     }
        // } catch (Exception e) {
        //     PrintStack.logging(logger, e);
        //     throw new GWException(GWErrorCode.SERVER_ERROR, param);
        // }
        
        try {
            osdClient = new OSDClient(disk.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
            osdClient.getMultipartInit(path, meta.getObjId(), meta.getVersionId(), meta.getSize(), range, param.getResponse().getOutputStream(), key);
            actualSize = osdClient.getMultipart();
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } finally {
            if (isBorrowOsdPrimary) {
                try {
                    // OSDClientManager.getInstance().releaseOSDClient(osdClient);
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                }
            } else {
                if (osdClient != null)
                    osdClient.close();
            }
        }
        
        return actualSize;
    }

    private void retryRenameTo(File srcFile, File destFile) {
        if (srcFile.exists()) {
            if (destFile == null) {
                logger.error("destFile is null");
                return;
            }
            for (int i = 0; i < GWConstants.RETRY_COUNT; i++) {
                if (srcFile.renameTo(destFile)) {
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
            logger.error(GWConstants.LOG_S3OBJECT_OPERATION_FAILED_FILE_RENAME, srcFile.getAbsolutePath(), destFile.getAbsolutePath());
        }
    }

    @Override
    public boolean deletePart(S3Parameter param, Metadata meta) throws GWException {
        // check disk
        boolean isAvailablePrimary = meta.isPrimaryExist() && isAvailableDiskForWrite(meta.getPrimaryDisk().getId());
        boolean isAvailableReplica = false;
        DISK replicaDISK = null;
        if (meta.isReplicaExist()) {
            try {
                replicaDISK = meta.getReplicaDisk();
                isAvailableReplica = isAvailableDiskForWrite(replicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.error("Replica is null");
            }
        }
        if (!isAvailablePrimary && !isAvailableReplica) {
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }

        File filePrimary = null;
        File fileReplica = null;
        File fileTrashPrimary = null;
        File fileTrashReplica = null;

        if (isAvailableReplica) {
            // if (GWUtils.getLocalIP().equals(meta.getPrimaryDisk().getOsdIp())) {
            //     // check Cache
            //     if (GWConfig.getInstance().isCacheDiskpath()) {
            //         filePrimary = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
            //         isCachePrimary = true;
            //     } else {
            //         filePrimary = new File(KsanUtils.makePartPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getUploadId(), param.getPartNumber()));
            //     }
            //     fosPrimary = new FileOutputStream(filePrimary, false);
            // } else {
            //     // osdClientPrimary = OSDClientManager.getInstance().getOSDClient(meta.getPrimaryDisk().getOsdIp());
            //     logger.debug("osd client primary : {}", meta.getPrimaryDisk().getOsdIp());
            //     if (osdClientPrimary == null) {
            //         osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
            //     } else {
            //         isBorrowOsdPrimary = true;
            //     }
            //     clientPartPrimary.deletePart(DiskManager.getInstance().getPath(primaryDiskId), meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber()));
            //     if (isBorrowOsdPrimary) {
            //         // OSDClientManager.getInstance().releaseOSDClient(clientPartPrimary);
            //     } else {
            //         clientPartPrimary.close();
            //     }
            // }

            // if ((localPath = DiskManager.getInstance().getLocalPath(primaryDiskId)) != null) {
            //     partFilePrimary = new File(KsanUtils.makePartPath(localPath, meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
            //     partFileTrashPrimary = new File(KsanUtils.makeTrashPath(localPath, meta.getObjId(), param.getUploadId()));
            //     if (!partFilePrimary.exists()) {
            //         retryRenameTo(partFilePrimary, partFileTrashPrimary);
            //     }
            // } else {
            //     // clientPartPrimary = OSDClientManager.getInstance().getOSDClient(DiskManager.getInstance().getOSDIP(primaryDiskId));
            //     if (clientPartPrimary == null) {
            //         clientPartPrimary = new OSDClient(DiskManager.getInstance().getOSDIP(primaryDiskId), (int)GWConfig.getInstance().getOsdPort());
            //     } else {
            //         isBorrowOsdPartPrimary = true;
            //     }
            //     clientPartPrimary.deletePart(DiskManager.getInstance().getPath(primaryDiskId), meta.getObjId(), param.getUploadId(), String.valueOf(entry.getValue().getPartNumber()));
            //     if (isBorrowOsdPartPrimary) {
            //         // OSDClientManager.getInstance().releaseOSDClient(clientPartPrimary);
            //     } else {
            //         clientPartPrimary.close();
            //     }
            // }
        }
        if (isAvailableReplica) {

        }

        return true;
    }

    @Override
    public void restoreObject(S3Parameter param, Metadata meta, Metadata restoreMeta) throws GWException {
        try {
            File srcFile = new File(KsanUtils.makeObjPathForOpen(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
            File destFile = new File(KsanUtils.makeObjPath(restoreMeta.getPrimaryDisk().getPath(), restoreMeta.getObjId(), param.getVersionId()));

            if (destFile.exists()) {
                if (!destFile.delete()) {
                    logger.error(GWConstants.LOG_S3OBJECT_OPERATION_FAILED_FILE_DELETE, destFile.getAbsolutePath());
                }
            }

            retryRenameTo(srcFile, destFile);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        }
    }

    @Override
    public void storageMove(S3Parameter param, Metadata meta, Metadata restoreMeta) throws GWException {
        try {
            File srcFile = new File(KsanUtils.makeObjPathForOpen(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId()));
            File destFile = new File(KsanUtils.makeObjPath(restoreMeta.getPrimaryDisk().getPath(), restoreMeta.getObjId(), param.getVersionId()));

            if (destFile.exists()) {
                if (!destFile.delete()) {
                    logger.error(GWConstants.LOG_S3OBJECT_OPERATION_FAILED_FILE_DELETE, destFile.getAbsolutePath());
                }
            }

            retryRenameTo(srcFile, destFile);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        }
    }

    @Override
    public S3Object putObjectRange(S3Parameter param, Metadata meta, long offset, long length) throws GWException {
        // check disk
        boolean isAvailablePrimary = meta.isPrimaryExist() && isAvailableDiskForWrite(meta.getPrimaryDisk().getId());
        boolean isAvailableReplica = false;
        DISK replicaDISK = null;
        if (meta.isReplicaExist()) {
            try {
                replicaDISK = meta.getReplicaDisk();
                isAvailableReplica = isAvailableDiskForWrite(replicaDISK.getId());
            } catch (ResourceNotFoundException e) {
                logger.error("Replica is null");
            }
        }
        if (!isAvailablePrimary && !isAvailableReplica) {
            throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
        }
        logger.debug("isAvailablePrimary : {}, isAvailableReplica : {}", isAvailablePrimary, isAvailableReplica);
        logger.debug("objId : {}, versionId : {}, offset : {}, length : {}", meta.getObjId(), meta.getVersionId(), offset, length);

        // check encryption
        String key = GWConstants.EMPTY_STRING;//encryption.isEnabledEncryption() ? encryption.getEncryptionKey() : GWConstants.EMPTY_STRING;

        S3Object s3Object = new S3Object();
        
        InputStream is = param.getInputStream();
        RandomAccessFile filePrimary = null;
        RandomAccessFile fileReplica = null;
        OSDClient osdClientPrimary = null;
        OSDClient osdClientReplica = null;
        boolean isCachePrimary = false;
        boolean isCacheReplica = false;
        boolean isBorrowOsdPrimary = false;
        boolean isBorrowOsdReplica = false;
        long fileSize = 0L;

        try {
            // MD5
            MessageDigest md5er = null;
            // if (length < 100 * GWConstants.MEGABYTES) {
                md5er = MessageDigest.getInstance(GWConstants.MD5);
            // } else {
            //     md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            // }
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            long totalReads = 0L;

            String objPath = null;
            // check primary
            if (isAvailablePrimary) {
                if (GWUtils.getLocalIP().equals(meta.getPrimaryDisk().getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        objPath = KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                        isCachePrimary = true;
                    } else {
                        objPath = KsanUtils.makeObjPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                    }
                    // check unmount disk path
                    if (objPath == null) {
                        throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
                    }
                    filePrimary = new RandomAccessFile(objPath, "rw");
                    filePrimary.seek(offset);
                    logger.debug("file pointer : {}", filePrimary.getFilePointer());
                } else {
                    // osdClientPrimary = OSDClientManager.getInstance().getOSDClient(meta.getPrimaryDisk().getOsdIp());
                    osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientPrimary == null) {
                    //     osdClientPrimary = new OSDClient(meta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdPrimary = true;
                    // }
                    osdClientPrimary.putRangeInit(meta.getPrimaryDisk().getPath(),
                        meta.getObjId(),
                        param.getVersionId(),
                        offset,
                        length,
                        Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY,
                        replicaDISK != null? replicaDISK.getId() : Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL,
                        key,
                        "");
                }
            }

            // check replica
            if (isAvailableReplica) {
                logger.debug("replica osd ip : {}", replicaDISK.getOsdIp());
                if (GWUtils.getLocalIP().equals(replicaDISK.getOsdIp())) {
                    // check Cache
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        objPath = KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                        isCacheReplica = true;
                    } else {
                        objPath = KsanUtils.makeObjPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                    }
                    // check unmount disk path
                    if (objPath == null) {
                        throw new GWException(GWErrorCode.INTERNAL_SERVER_DISK_ERROR, param);
                    }
                    fileReplica = new RandomAccessFile(objPath, "rw");
                    fileReplica.seek(offset);
                    logger.debug("file pointer : {}", fileReplica.getFilePointer());
                } else {
                    // osdClientReplica = OSDClientManager.getInstance().getOSDClient(replicaDISK.getOsdIp());
                    osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // if (osdClientReplica == null) {
                    //     osdClientReplica = new OSDClient(replicaDISK.getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // } else {
                    //     isBorrowOsdReplica = true;
                    // }
                    osdClientReplica.putRangeInit(replicaDISK.getPath(),
                        meta.getObjId(),
                        param.getVersionId(),
                        offset,
                        length,
                        Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA,
                        Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL,
                        key,
                        "");
                }
            }

            while ((readLength = is.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                totalReads += readLength;
                if (isAvailablePrimary) {
                    if (filePrimary == null) {
                        osdClientPrimary.put(buffer, 0, readLength);
                    } else {
                        filePrimary.write(buffer, 0, readLength);
                    }
                }
                if (isAvailableReplica) {
                    if (fileReplica == null) {
                        osdClientReplica.put(buffer, 0, readLength);
                    } else {
                        fileReplica.write(buffer, 0, readLength);
                    }
                }
                md5er.update(buffer, 0, readLength);
                if (totalReads >= length) {
                    break;
                }
            }

            if (isAvailablePrimary) {
                if (filePrimary == null) {
                    osdClientPrimary.putFlush();
                } else {
                    filePrimary.close();
                    File file = new File(objPath);
                    fileSize = file.length();
                    if (meta.isReplicaExist()) {
                        KsanUtils.setAttributeFileReplication(file, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, replicaDISK.getId());
                    }
                    
                    if (isCachePrimary) {
                        String path = KsanUtils.makeObjPath(meta.getPrimaryDisk().getPath(), meta.getObjId(), param.getVersionId());
                        Files.createSymbolicLink(Paths.get(path), Paths.get(file.getAbsolutePath()));
                    }
                }
            }
            if (isAvailableReplica) {
                if (fileReplica == null) {
                    osdClientReplica.putFlush();
                } else {
                    fileReplica.close();
                    File file = new File(objPath);
                    fileSize = fileReplica.length();
                    KsanUtils.setAttributeFileReplication(file, Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                    
                    if (isCacheReplica) {
                        String path = KsanUtils.makeObjPath(replicaDISK.getPath(), meta.getObjId(), param.getVersionId());
                        Files.createSymbolicLink(Paths.get(path), Paths.get(file.getAbsolutePath()));
                    }
                }
            }

            byte[] digest = md5er.digest();
            String eTag = base16().lowerCase().encode(digest);
            // for GCS md5hash
            String md5Hash = Base64.getEncoder().encodeToString(digest);          
            s3Object.setMd5hash(md5Hash);

            s3Object.setEtag(eTag);
            // s3Object.setFileSize(totalReads);
            logger.debug("fileSize : {}", fileSize);
            s3Object.setFileSize(fileSize);
            s3Object.setLastModified(new Date());
            s3Object.setVersionId(param.getVersionId());
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (NoSuchAlgorithmException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, param);
        } finally {
            if (isBorrowOsdPrimary) {
                try {
                    // OSDClientManager.getInstance().releaseOSDClient(osdClientPrimary);
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                }
            } else {
                if (osdClientPrimary != null) {
                    osdClientPrimary.close();
                }
            }
            if (isBorrowOsdReplica) {
                try {
                    // OSDClientManager.getInstance().releaseOSDClient(osdClientReplica);
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                }
            } else {
                if (osdClientReplica != null) {
                    osdClientReplica.close();
                }
            }
        }

        return s3Object;
    }
}
