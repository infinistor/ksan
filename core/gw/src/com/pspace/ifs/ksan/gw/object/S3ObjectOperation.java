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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
import com.pspace.ifs.ksan.gw.object.osdclient.OSDClient;
import com.pspace.ifs.ksan.gw.object.osdclient.OSDClientManager;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.objmanager.Metadata;
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
            if (GWConfig.getInstance().replicationCount() > 1) {
                if (GWConfig.getInstance().localIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    actualSize = getObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), sourceRange);
                } else if (GWConfig.getInstance().localIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    actualSize = getObjectLocal(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), sourceRange);
                } else {
                    client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.getInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId(), fileSize, sourceRange, s3Parameter.getResponse().getOutputStream());
                    actualSize = client.get();
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            } else {
                if (GWConfig.getInstance().localIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    actualSize = getObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), sourceRange);
                } else {
                    client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.getInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId(), fileSize, sourceRange, s3Parameter.getResponse().getOutputStream());
                    actualSize = client.get();
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            }
        } catch (Exception e) {
            if (client != null && client.getSocket() != null && client.getSocket().isClosed()) {
                try {
                    client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    client.getInit(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId(), fileSize, sourceRange, s3Parameter.getResponse().getOutputStream());
                    actualSize = client.get();
                    OSDClientManager.getInstance().returnOSDClient(client);
                } catch (IOException | ResourceNotFoundException e1) {
                    PrintStack.logging(logger, e1);
                    throw new GWException(GWErrorCode.SERVER_ERROR);
                }
            }
        } 

        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_FILE_SIZE, actualSize);
    }

    private long getObjectLocal(String path, String objId, String sourceRange) throws IOException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File file = new File(makeObjPath(path, objId, versionId));
        long actualSize = 0L;
        try (FileInputStream fis = new FileInputStream(file)) {
            long remainLength = 0L;
            int readLength = 0;
            int readBytes;

            if (Strings.isNullOrEmpty(sourceRange)) {
                remainLength = file.length();
                while (remainLength > 0) {
                    readBytes = 0;
                    if (remainLength < GWConstants.MAXBUFSIZE) {
                        readBytes = (int)remainLength;
                    } else {
                        readBytes = GWConstants.MAXBUFSIZE;
                    }
                    readLength = fis.read(buffer, 0, readBytes);
                    actualSize += readLength;
                    s3Parameter.getResponse().getOutputStream().write(buffer, 0, readLength);
                    remainLength -= readLength;
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
                    remainLength = length;
                    while (remainLength > 0) {
                        readBytes = 0;
                        if (remainLength < GWConstants.MAXBUFSIZE) {
                            readBytes = (int)remainLength;
                        } else {
                            readBytes = GWConstants.MAXBUFSIZE;
                        }
                        readLength = fis.read(buffer, 0, readBytes);
                        actualSize += readLength;
                        s3Parameter.getResponse().getOutputStream().write(buffer, 0, readLength);
                        remainLength -= readLength;
                    }
                }
            }
        }

        return actualSize;
    }

    
    public S3Object putObject() throws GWException {
        S3Object s3Object = null;
        String objectName = s3Parameter.getObjectName();

        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_OBJECT_PRIMARY_INFO, objMeta.getPrimaryDisk().getOsdIp() + objMeta.getPrimaryDisk().getPath());
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
        try {
            MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5);
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            long remainLength = length;
            int bufferSize = (int) (remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
            
            if (GWConfig.getInstance().replicationCount() > 1) {
                // check local / OSD server
                logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_PRIMARY_IP, GWConfig.getInstance().localIP(), objMeta.getPrimaryDisk().getOsdIp());
                logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_REPLICA_IP, GWConfig.getInstance().localIP(), objMeta.getReplicaDisk().getOsdIp());
                if (GWConfig.getInstance().localIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    filePrimary = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                    tmpFilePrimary = new File(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                    trashPrimary = new File(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                    com.google.common.io.Files.createParentDirs(filePrimary);
                    com.google.common.io.Files.createParentDirs(tmpFilePrimary);
                    fosPrimary = new FileOutputStream(tmpFilePrimary, false);
                } else {
                    clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, length);
                }
                
                if (GWConfig.getInstance().localIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    fileReplica = new File(makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                    tmpFileReplica = new File(makeTempPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                    trashReplica = new File(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                    com.google.common.io.Files.createParentDirs(fileReplica);
                    com.google.common.io.Files.createParentDirs(tmpFileReplica);
                    fosReplica = new FileOutputStream(tmpFileReplica, false);
                } else {
                    clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    clientReplica.putInit(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId, length);
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
                    retryRenameTo(filePrimary, trashPrimary);
                    retryRenameTo(tmpFilePrimary, filePrimary);
                }
                if (fileReplica == null) {
                    clientReplica.putFlush();
                    OSDClientManager.getInstance().returnOSDClient(clientReplica);
                } else {
                    fosReplica.flush();
                    retryRenameTo(fileReplica, trashReplica);
                    retryRenameTo(tmpFileReplica, fileReplica);
                }
            } else {
                File file = new File(makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                File tmpFile = new File(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                File trashFile = new File(makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                com.google.common.io.Files.createParentDirs(file);
                com.google.common.io.Files.createParentDirs(tmpFile);
                try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                    while ((readLength = is.read(buffer, 0, bufferSize)) > 0) {
                        remainLength -= readLength;
                        fos.write(buffer, 0, readLength);
                        md5er.update(buffer, 0, readLength);
                        if (remainLength <= 0) {
                            break;
                        }
                        bufferSize = (int) (remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
                    }
                    fos.flush();
                }
                retryRenameTo(file, trashFile);
                retryRenameTo(tmpFile, file);
            }

            byte[] digest = md5er.digest();
			String eTag = base16().lowerCase().encode(digest);

            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(length);
            s3Object.setVersionId(versionId);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (NoSuchAlgorithmException | IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR);
        } catch (ResourceNotFoundException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.NO_SUCH_KEY);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR);
        } finally {
            if (GWConfig.getInstance().replicationCount() > 1) {
                if (fosPrimary != null) {
                    try {
                        fosPrimary.close();
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR);
                    }
                }
                if (fosReplica != null) {
                    try {
                        fosReplica.close();
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR);
                    }
                }
            }
        }
        return s3Object;
    }

    public boolean deleteObject() throws GWException {
        try {
            if (GWConfig.getInstance().replicationCount() > 1) {
                if (GWConfig.getInstance().localIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    deleteObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId());
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.delete(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId());
                    OSDClientManager.getInstance().returnOSDClient(client);
                } 
                
                if (GWConfig.getInstance().localIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
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
            throw new GWException(GWErrorCode.SERVER_ERROR);
        }
        logger.info(GWConstants.LOG_S3OBJECT_OPERATION_DELETE, objMeta.getBucket(), objMeta.getPath(), versionId);
        return true;
    }

    private void deleteObjectLocal(String path, String objId) throws IOException {
        File file = new File(makeObjPath(path, objId, versionId));
        File trashFile = new File(makeTrashPath(path, objId, versionId));

        retryRenameTo(file, trashFile);
    }

    public S3Object uploadPart(long length) throws GWException {
        S3Object s3Object = new S3Object();
        File tmpFilePrimary = null;
        FileOutputStream fosPrimary = null;
        File tmpFileReplica = null;
        FileOutputStream fosReplica = null;
        OSDClient clientPrimary = null;
        OSDClient clientReplica = null;

        try {
            MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5);
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            long remainLength = length;
            int bufferSize = (int)(remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
            
            if (GWConfig.getInstance().replicationCount() > 1) {
                if (GWConfig.getInstance().localIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    tmpFilePrimary = new File(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber()));
                    com.google.common.io.Files.createParentDirs(tmpFilePrimary);
                    fosPrimary = new FileOutputStream(tmpFilePrimary, false);
                } else {
                    clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    clientPrimary.partInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber(), s3Meta.getContentLength());
                }

                if (GWConfig.getInstance().localIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    tmpFileReplica = new File(makeTempPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber()));
                    com.google.common.io.Files.createParentDirs(tmpFileReplica);
                    fosReplica = new FileOutputStream(tmpFileReplica, false);
                } else {
                    clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    clientReplica.partInit(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber(), s3Meta.getContentLength());
                }

                while ((readLength = s3Parameter.getInputStream().read(buffer, 0, bufferSize)) >= 0) {
                    remainLength -= readLength;
                    if (tmpFilePrimary == null) {
                        clientPrimary.put(buffer, 0, readLength);
                    } else {
                        fosPrimary.write(buffer, 0, readLength);
                    }

                    if (tmpFileReplica == null) {
                        clientReplica.put(buffer, 0, readLength);
                    } else {
                        fosReplica.write(buffer, 0, readLength);
                    }

                    md5er.update(buffer, 0, readLength);
                    if (remainLength <= 0) {
                        break;
                    }
                    bufferSize = (int)(remainLength < GWConstants.BUFSIZE ? remainLength : GWConstants.BUFSIZE);
                }

                if (tmpFilePrimary == null) {
                    clientPrimary.putFlush();
                    OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                } else {
                    fosPrimary.flush();
                }
                if (tmpFileReplica == null) {
                    clientReplica.putFlush();
                    OSDClientManager.getInstance().returnOSDClient(clientReplica);
                } else {
                    fosReplica.flush();
                }
            } else {
                File tmpFile = new File(makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber()));
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
            }

            byte[] digest = md5er.digest();
			String eTag = base16().lowerCase().encode(digest);

            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(length);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR);
        } finally {
            if (GWConfig.getInstance().replicationCount() > 1) {
                if (fosPrimary != null) {
                    try {
                        fosPrimary.close();
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR);
                    }
                }
                if (fosReplica != null) {
                    try {
                        fosReplica.close();
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR);
                    }
                }
            }
        }
        
        return s3Object;
    }

    public S3Object completeMultipart(SortedMap<Integer, Part> listPart) throws Exception {
        S3Object s3Object = new S3Object();
        try {
            StringBuilder bld = new StringBuilder();
            for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, Part> entry = it.next();
                if (bld.length() == 0) {
                    bld.append(String.valueOf(entry.getValue().getPartNumber()));
                } else {
                    bld.append(GWConstants.COMMA);
                    bld.append(String.valueOf(entry.getValue().getPartNumber()));
                }
            }
            String partNos = bld.toString();
            OSDData osdData = null;
            OSDData dataPrimary = null;
            OSDData dataReplica = null;
            if (GWConfig.getInstance().replicationCount() > 1) {
                if (GWConfig.getInstance().localIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    osdData = completeMultipartLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), partNos);
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    dataPrimary = client.completeMultipart(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, partNos);
                    OSDClientManager.getInstance().returnOSDClient(client);
                }

                if (GWConfig.getInstance().localIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    osdData = completeMultipartLocal(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), partNos);
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    dataReplica = client.completeMultipart(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId, partNos);
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            } else {
                osdData = completeMultipartLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), partNos);
            }

            if (osdData != null) {
                s3Object.setEtag(osdData.getETag());
                s3Object.setFileSize(osdData.getFileSize());
            } else if (dataPrimary != null) {
                s3Object.setEtag(dataPrimary.getETag());
                s3Object.setFileSize(dataPrimary.getFileSize());
            } else if (dataReplica != null) {
                s3Object.setEtag(dataReplica.getETag());
                s3Object.setFileSize(dataReplica.getFileSize());
            } else {
                logger.error(GWConstants.LOG_S3OBJECT_OPERATION_OSD_ERROR);
                throw new GWException(GWErrorCode.SERVER_ERROR);
            }

            s3Object.setVersionId(versionId);
            s3Object.setLastModified(new Date());
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (IOException | ResourceNotFoundException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR);
        } 

        return s3Object;
    }

    private OSDData completeMultipartLocal(String path, String objId, String partNos) throws NoSuchAlgorithmException, IOException {
        String[] arrayPartNos = partNos.split(GWConstants.COMMA);
        Arrays.sort(arrayPartNos);

        OSDData osdData = new OSDData();
        File file = new File(makeObjPath(path, objId, versionId));
        File tmpFile = new File(makeTempPath(path, objId, versionId));
        File trashFile = new File(makeTrashPath(path, objId, versionId));

        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5);
        long totalLength = 0L;

        try (FileOutputStream tmpOut = new FileOutputStream(tmpFile)) {
            com.google.common.io.Files.createParentDirs(file);
            com.google.common.io.Files.createParentDirs(tmpFile);

            for (String partNo : arrayPartNos) {
                File partFile = new File(makeTempPath(path, objId, partNo));
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
            }
        }
        
        retryRenameTo(file, trashFile);
        retryRenameTo(tmpFile, file);

        byte[] digest = md5er.digest();
        String eTag = base16().lowerCase().encode(digest);
        osdData.setETag(eTag);
        osdData.setFileSize(totalLength);

        return osdData;
    }

    public void abortMultipart(SortedMap<Integer, Part> listPart) throws GWException {
        try {
            StringBuilder bld = new StringBuilder();
            for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, Part> entry = it.next();
                if (bld.length() == 0) {
                    bld.append(String.valueOf(entry.getValue().getPartNumber()));
                } else {
                    bld.append(GWConstants.COMMA);
                    bld.append(String.valueOf(entry.getValue().getPartNumber()));
                }
            }
            String partNos = bld.toString();

            if (GWConfig.getInstance().replicationCount() > 1) {
                if (GWConfig.getInstance().localIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    abortMultipartLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), partNos);
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.abortMultipart(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), partNos);
                    OSDClientManager.getInstance().returnOSDClient(client);
                }

                if (GWConfig.getInstance().localIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    abortMultipartLocal(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), partNos);
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    client.abortMultipart(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), partNos);
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            } else {
                abortMultipartLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), partNos);
            }
        } catch (Exception e) {
             PrintStack.logging(logger, e);
             throw new GWException(GWErrorCode.SERVER_ERROR);
        }
    }

    private void abortMultipartLocal(String path, String objId, String partNos) {
        String[] arrayPartNos = partNos.split(GWConstants.COMMA);

        for (String partNo : arrayPartNos) {
            File partFile = new File(makeTempPath(path, objId, partNo));
        
            if (!partFile.delete()) {
                logger.error(GWConstants.LOG_S3OBJECT_OPERATION_FAILED_FILE_DELETE, partFile.getName());
            }
        }
    }

    public S3Object uploadPartCopy(Metadata srcObjMeta, S3Range s3Range) throws GWException {
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
            logger.debug("copySourceRange : {}", copySourceRange);
            OSDData osdData = null;
            OSDData dataPrimary = null;
            OSDData dataReplica = null;
            if (GWConfig.getInstance().replicationCount() > 1) {
                if (GWConfig.getInstance().localIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    osdData = uploadPartCopyLocal(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber());
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    dataPrimary = client.partCopy(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber());
                    OSDClientManager.getInstance().returnOSDClient(client);
                }

                if (GWConfig.getInstance().localIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    osdData = uploadPartCopyLocal(srcObjMeta.getReplicaDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber());
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    dataReplica = client.partCopy(srcObjMeta.getReplicaDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber());
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            } else {
                osdData = uploadPartCopyLocal(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), copySourceRange, objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), s3Parameter.getPartNumber());
            }

            if (osdData != null) {
                s3Object.setEtag(osdData.getETag());
                s3Object.setFileSize(osdData.getFileSize());
            } else if (dataPrimary != null) {
                s3Object.setEtag(dataPrimary.getETag());
                s3Object.setFileSize(dataPrimary.getFileSize());
            } else if (dataReplica != null) {
                s3Object.setEtag(dataReplica.getETag());
                s3Object.setFileSize(dataReplica.getFileSize());
            } else {
                logger.error(GWConstants.LOG_S3OBJECT_OPERATION_OSD_ERROR);
                throw new GWException(GWErrorCode.SERVER_ERROR);
            }

            s3Object.setLastModified(new Date());
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new GWException(GWErrorCode.SERVER_ERROR);
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
            logger.info(GWConstants.LOG_S3OBJECT_OPERATION_LOCAL_IP, GWConfig.getInstance().localIP());
            logger.info(GWConstants.LOG_S3OBJECT_OPERATION_OBJ_PRIMARY_IP, objMeta.getPrimaryDisk().getOsdIp());
            logger.info(GWConstants.LOG_S3OBJECT_OPERATION_OBJ_REPLICA_IP, objMeta.getReplicaDisk().getOsdIp());

            if (GWConfig.getInstance().replicationCount() > 1) {
                // check primary local src, obj
                if (GWConfig.getInstance().localIP().equals(srcObjMeta.getPrimaryDisk().getOsdIp())) {
                    if (srcObjMeta.getPrimaryDisk().getOsdIp().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                        copyObjectLocal(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(),  objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                    } else {
                        // src local, obj replica
                        // put src to replica
                        copyObjectLocalToOSD(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), objMeta.getPrimaryDisk().getOsdIp(), objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                    }
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.copy(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(),  objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                    OSDClientManager.getInstance().returnOSDClient(client);
                }

                // check replica local src, obj
                if (GWConfig.getInstance().localIP().equals(srcObjMeta.getReplicaDisk().getOsdIp())) {
                    if (srcObjMeta.getReplicaDisk().getOsdIp().equals(objMeta.getReplicaDisk().getOsdIp())) {
                        copyObjectLocal(srcObjMeta.getReplicaDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(),  objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId);
                    } else {
                        // src local, obj replica
                        // put src to replica
                        copyObjectLocalToOSD(srcObjMeta.getReplicaDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), objMeta.getReplicaDisk().getOsdIp(), objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId);
                    }
                } else {
                    OSDClient client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    client.copy(srcObjMeta.getReplicaDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(),  objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId);
                    OSDClientManager.getInstance().returnOSDClient(client);
                }
            } else {
                copyObjectLocal(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(),  objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
            }

            s3Object.setEtag(srcObjMeta.getEtag());
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(srcObjMeta.getSize());
            s3Object.setVersionId(versionId);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR);
        }
      
        return s3Object;
    }

    private void copyObjectLocal(String srcPath, String srcObjId, String srcVersionId, String path, String objId, String versionId) throws IOException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File srcFile = new File(makeObjPath(srcPath, srcObjId, srcVersionId));
        try (FileInputStream fis = new FileInputStream(srcFile)) {
            File file = new File(makeObjPath(path, objId, versionId));
            File tmpFile = new File(makeTempPath(path, objId, versionId));

            com.google.common.io.Files.createParentDirs(file);
            com.google.common.io.Files.createParentDirs(tmpFile);
            try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                int readLength = 0;
                while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                    fos.write(buffer, 0, readLength);
                }
                fos.flush();
            }

            retryRenameTo(tmpFile, file);
        }
    }

    private void copyObjectLocalToOSD(String srcPath, String srcObjId, String srcVersionId, String osdIP, String path, String objId, String versionId) throws GWException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File srcFile = new File(makeObjPath(srcPath, srcObjId, srcVersionId));
        OSDClient client = null;
        try {
            client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
            client.putInit(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, s3Meta.getContentLength());
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR);
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
            throw new GWException(GWErrorCode.SERVER_ERROR);
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

    private String makeObjPath(String path, String objId, String versionId) {
        String fullPath = path + GWConstants.SLASH + GWConstants.OBJ_DIR + makeDirectoryName(objId) + GWConstants.SLASH + objId + GWConstants.LOW_LINE + versionId;
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_OBJ_PATH, fullPath);
        return fullPath;
    }

    private String makeTempPath(String path, String objId, String versionId) {
        String fullPath = path + GWConstants.SLASH + GWConstants.TEMP_DIR + GWConstants.SLASH + objId + GWConstants.LOW_LINE + versionId;
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_TEMP_PATH, fullPath);
        return fullPath;
    }

    private String makeTrashPath(String path, String objId, String versionId) {
        String uuid = UUID.randomUUID().toString();
        String fullPath = path + GWConstants.SLASH + GWConstants.TRASH_DIR + GWConstants.SLASH + objId + GWConstants.LOW_LINE + versionId + GWConstants.DASH + uuid;
        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_TRASH_PATH, fullPath);
        return fullPath;
    }

    private void retryRenameTo(File tempFile, File destFile) {
        if (tempFile.exists()) {
            for (int i = 0; i < GWConstants.RETRY_COUNT; i++) {
                if (tempFile.renameTo(destFile)) {
                    return;
                }
            }
            logger.error(GWConstants.LOG_S3OBJECT_OPERATION_FAILED_FILE_RENAME, destFile.getName());
        }
    }
}
