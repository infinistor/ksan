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
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.List;
import java.util.ArrayList;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.pspace.ifs.ksan.gw.encryption.S3Encryption;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
import com.pspace.ifs.ksan.gw.object.osdclient.OSDClientManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.osd.OSDClient;
import com.pspace.ifs.ksan.libs.data.OsdData;
import com.pspace.ifs.ksan.libs.KsanUtils;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;
import com.pspace.ifs.ksan.libs.data.ECPart;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;

import de.sfuhrm.openssl4j.OpenSSL4JProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.crypto.stream.CtrCryptoInputStream;
import org.apache.commons.crypto.stream.CtrCryptoOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3ObjectOperation {
    private Metadata objMeta;
    private S3Metadata s3Meta;
    private S3Parameter s3Parameter;
    private String versionId;
    private S3Encryption s3Encryption;
    private ObjManager objManager;
    private static final Logger logger = LoggerFactory.getLogger(S3ObjectOperation.class);

    public S3ObjectOperation(Metadata objMeta, S3Metadata s3Meta, S3Parameter s3Parameter, String versionId, S3Encryption s3Encryption) {
        this.objMeta = objMeta;
        this.s3Meta = s3Meta;
        this.s3Parameter = s3Parameter;
        if (Strings.isNullOrEmpty(versionId)) {
            this.versionId = GWConstants.VERSIONING_DISABLE_TAIL;
        } else {
            this.versionId = versionId;
        }
        
        this.s3Encryption = s3Encryption;
    }

    private String getDirectoryBlobSuffix(String key) {
		if (key.endsWith(GWConstants.DIRECTORY_SUFFIX)) {
			return GWConstants.DIRECTORY_SUFFIX;
		}
		return null;
	}

    public void getObject(S3Range s3Range) throws Exception {       
        OSDClient client = null;
        String sourceRange = GWConstants.EMPTY_STRING;
        long actualSize = 0L;
        long fileSize = objMeta.getSize();
        String key = Strings.isNullOrEmpty(s3Encryption.getCustomerKey()) ? GWConstants.EMPTY_STRING : s3Encryption.getCustomerKey();

        if (GWConfig.getInstance().isNoReplica() || GWConfig.getInstance().isNoIO()) {
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

            return;
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

        // check multipart
        String uploadId = s3Meta.getUploadId();
        if (!Strings.isNullOrEmpty(uploadId)) {
            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            File file = new File(KsanUtils.makeObjMultipartPathForOpen(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId(), uploadId));
            
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file));
                     OutputStream os = s3Parameter.getResponse().getOutputStream()) {
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
                        File partFile = new File(objPath);
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
        
                                readLength = fis.read(buffer, 0, readBytes);
                                
                                actualSize += readLength;
                                os.write(buffer, 0, readLength);
                                remaingLength -= readLength;
                            }
                        } catch (Exception e1) {
                            PrintStack.logging(logger, e1);
                            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                        }
                    }
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                }

                s3Parameter.addResponseSize(actualSize);
                return;
            }
        }

        // check EC exists
        File ecFile = new File(KsanUtils.makeECPathForOpen(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
        logger.debug("ecfile : {}", ecFile.getAbsolutePath());
        if (ecFile.exists()) {
            List<ECPart> ecList = new ArrayList<ECPart>();
            for (DiskPool pool : DiskManager.getInstance().getDiskPoolList()) {
                for (Server server : pool.getServerList()) {
                    for (Disk disk : server.getDiskList()) {
                        ECPart ecPart = new ECPart(server.getIp(), disk.getId(), disk.getPath(), false);
                        ecList.add(ecPart);
                    }
                }
            }
            int numberOfCodingChunks = DiskManager.getInstance().getECM(objMeta.getPrimaryDisk().getId());
            int numberOfDataChunks = DiskManager.getInstance().getECK(objMeta.getPrimaryDisk().getId());
            logger.debug("numberOfCodingChunks : {}, numberOfDataChunks : {}", numberOfCodingChunks, numberOfDataChunks);
            int getECPartCount = 0;
            for (ECPart ecPart : ecList) {
                String newECPartPath = ecFile.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
                File newECPartFile = new File(newECPartPath);
                if (ecPart.getServerIP().equals(GWUtils.getLocalIP())) {
                    // if local disk, move file
                    File sourceECPartFile = new File(KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objMeta.getObjId(), versionId));
                    if (sourceECPartFile.exists()) {
                        FileUtils.copyFile(sourceECPartFile, newECPartFile);
                        ecPart.setProcessed(true);
                    } else {
                        logger.info("ec part does not exist. {}", sourceECPartFile.getAbsolutePath());
                    }
                } else {
                    try (FileOutputStream fos = new FileOutputStream(newECPartFile)) {
                        String getPath = KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objMeta.getObjId(), versionId);
                        OSDClient ecClient = new OSDClient(ecPart.getServerIP(), (int)GWConfig.getInstance().getOsdPort());
                        logger.debug("get ec part file : {}, to : {}, {}", getPath, ecPart.getServerIP(), ecPart.getDiskPath());
                        ecClient.getECPartInit(getPath, fos);
                        ecClient.getECPart();
                        ecPart.setProcessed(true);
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                    }
                }
                getECPartCount++;
            }
            // zunfec
            String ecAllFilePath = KsanUtils.makeECPathForOpen(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
            String command = Constants.ZUNFEC + ecAllFilePath;
            getECPartCount = 0;
            for (ECPart ecPart : ecList) {
                String ecPartPath = ecFile.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
                if (ecPart.isProcessed()) {
                    command += Constants.SPACE + ecPartPath;
                    getECPartCount++;
                }
            }
            logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_ZUNFEC_COMMAND, command);
            Process p = Runtime.getRuntime().exec(command);
            try {
                int exitCode = p.waitFor();
                p.destroy();
                logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_ZUNFEC_DECODE_EXIT_VALUE, exitCode);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

            // delete junk file
            String ecDir = KsanUtils.makeECDirectoryPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId());
            File dir = new File(ecDir);
            File[] ecFiles = dir.listFiles();
            for (int i = 0; i < ecFiles.length; i++) {
                if (ecFiles[i].getName().startsWith(Constants.POINT)) {
                    if (ecFiles[i].getName().charAt(ecFiles[i].getName().length() - 2) == Constants.CHAR_POINT) {
                        ecFiles[i].delete();
                    }
                }
            }

            File ecAllFile = new File(ecAllFilePath);
            if (ecAllFile.exists()) {
                logger.info("zunfec result : {}, {}", ecAllFile.getAbsolutePath(), ecAllFile.length());
                byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
                CtrCryptoInputStream encryptIS = null;

                try (FileInputStream fis = new FileInputStream(ecAllFile); OutputStream os = s3Parameter.getResponse().getOutputStream()) {
                    long remaingLength = 0L;
                    int readLength = 0;
                    int readBytes;

                    if (Strings.isNullOrEmpty(sourceRange)) {
                        if (!Strings.isNullOrEmpty(key)) {
                            encryptIS = GWUtils.initCtrDecrypt(fis, key);
                            while ((readLength = encryptIS.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                                actualSize += readLength;
                                os.write(buffer, 0, readLength);
                                logger.debug("read length : {}", readLength);
                            }
                        } else {
                            remaingLength = ecAllFile.length();
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
                                logger.debug("read length : {}", readLength);
                                actualSize += readLength;
                                os.write(buffer, 0, readLength);
                                remaingLength -= readLength;
                            }
                        }
                    } else {
                        String[] ranges = sourceRange.split(GWConstants.SLASH);
                        for (String range : ranges) {
                            String[] rangeParts = range.split(GWConstants.COMMA);
                            long offset = Longs.tryParse(rangeParts[0]);
                            long length = Longs.tryParse(rangeParts[1]);
                            logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_RANGE, offset, length);

                            remaingLength = length;
                            
                            if (!Strings.isNullOrEmpty(key)) {
                                long skipOffset = 0;
                                encryptIS = GWUtils.initCtrDecrypt(fis, key);

                                if (offset > 0) {
                                    long skip = encryptIS.skip(offset);
                                    logger.debug("skip : {}", skip);
                                }
                                while (remaingLength > 0) {
                                    readBytes = 0;
                                    if (remaingLength < GWConstants.MAXBUFSIZE) {
                                        readBytes = (int)remaingLength;
                                    } else {
                                        readBytes = GWConstants.MAXBUFSIZE;
                                    }

                                    if ((readLength = encryptIS.read(buffer, 0, readBytes)) != -1) {
                                        skipOffset += readLength;
                                        logger.debug("read {} bytes", readLength);
                                        actualSize += readLength;
                                        os.write(buffer, 0, readLength);
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
            
                                    readLength = fis.read(buffer, 0, readBytes);
                                    
                                    actualSize += readLength;
                                    os.write(buffer, 0, readLength);
                                    remaingLength -= readLength;
                                }
                            }
                        }
                    }
                    os.flush();
                } catch (IOException e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                }

                if (encryptIS != null) {
                    encryptIS.close();
                }

                s3Parameter.addResponseSize(actualSize);
                return;
            }
        }

        try {
            if (objMeta.getReplicaCount() > 1) {
                logger.debug("bucket : {}, object : {}", objMeta.getBucket(), objMeta.getPath());
                logger.debug("primary disk id : {}, osd ip : {}", objMeta.getPrimaryDisk().getId(), objMeta.getPrimaryDisk().getOsdIp());
                if (objMeta.isReplicaExist()) {
                    logger.debug("replica disk id : {}, osd ip : {}", objMeta.getReplicaDisk().getId(), objMeta.getReplicaDisk().getOsdIp());
                }

                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    logger.debug("get local - objid : {}, primary get - path : {}", objMeta.getObjId(), objMeta.getPrimaryDisk().getPath());
                    actualSize = getObjectLocal(s3Parameter.getResponse().getOutputStream(), objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), sourceRange, key);
                    s3Parameter.addResponseSize(actualSize);
                } else if (objMeta.isReplicaExist() && GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    logger.debug("get local - objid : {}, replica get - path : {}", objMeta.getObjId(), objMeta.getReplicaDisk().getPath());
                    actualSize = getObjectLocal(s3Parameter.getResponse().getOutputStream(), objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), sourceRange, key);
                    s3Parameter.addResponseSize(actualSize);
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
                                       s3Parameter.getResponse().getOutputStream(),
                                       key);
                        actualSize = client.get();
                        // OSDClientManager.getInstance().returnOSDClient(client);
                        s3Parameter.addResponseSize(actualSize);
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
                                               s3Parameter.getResponse().getOutputStream(),
                                               key);
                                actualSize = client.get();
                                // OSDClientManager.getInstance().returnOSDClient(client);
                                client = null;
                                s3Parameter.addResponseSize(actualSize);
                            } catch (Exception e1) {
                                PrintStack.logging(logger, e);
                                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                            }
                        } else {
                            // Can't find the object
                            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                        }
                    }
                }
            } else {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    actualSize = getObjectLocal(s3Parameter.getResponse().getOutputStream(), objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), sourceRange, key);
                    s3Parameter.addResponseSize(actualSize);
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
                                       s3Parameter.getResponse().getOutputStream(),
                                       key);
                        actualSize = client.get();
                        // OSDClientManager.getInstance().returnOSDClient(client);
                        // client = null;
                        s3Parameter.addResponseSize(actualSize);
                    } catch (Exception e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                    }
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            if (client != null) {
                // OSDClientManager.getInstance().returnOSDClient(client);
            }
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } 

        logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_FILE_SIZE, actualSize);
    }

    private long getObjectLocal(OutputStream outputStream, String path, String objId, String sourceRange, String key) throws IOException, GWException, InvalidKeyException, NoSuchAlgorithmException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File file = new File(KsanUtils.makeObjPathForOpen(path, objId, versionId));
        CtrCryptoInputStream encryptIS = null;

        if (GWConfig.getInstance().isNoIO()) {
            outputStream.write(buffer, 0, 100 * 1000);
            outputStream.flush();
            outputStream.close();

            return 100*1000;
        } 

        // logger.info("obj path : {}", file.getAbsolutePath());
        long actualSize = 0L;
        try (FileInputStream fis = new FileInputStream(file)) {
            long remaingLength = 0L;
            int readLength = 0;
            int readBytes;

            if (Strings.isNullOrEmpty(sourceRange)) {
                if (!Strings.isNullOrEmpty(key)) {
                    encryptIS = GWUtils.initCtrDecrypt(fis, key);
                    while ((readLength = encryptIS.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                        actualSize += readLength;
                        outputStream.write(buffer, 0, readLength);
                        logger.debug("read length : {}", readLength);
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
    
                        if (remaingLength >= GWConstants.MAXBUFSIZE) {
                                readLength = GWConstants.MAXBUFSIZE;
                        } else {
                            readLength = (int)remaingLength;
                        }
                        readLength = fis.read(buffer, 0, readBytes);
                        
                        actualSize += readLength;
                        outputStream.write(buffer, 0, readLength);
                        remaingLength -= readLength;
                    }
                }
            } else {
                String[] ranges = sourceRange.split(GWConstants.SLASH);
                for (String range : ranges) {
                    String[] rangeParts = range.split(GWConstants.COMMA);
                    long offset = Longs.tryParse(rangeParts[0]);
                    long length = Longs.tryParse(rangeParts[1]);
                    logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_RANGE, offset, length);

                    remaingLength = length;
                    
                    if (!Strings.isNullOrEmpty(key)) {
                        long skipOffset = 0;
                        encryptIS = GWUtils.initCtrDecrypt(fis, key);

                        if (offset > 0) {
                            long skip = encryptIS.skip(offset);
                            logger.debug("skip : {}", skip);
                        }
                        while (remaingLength > 0) {
                            readBytes = 0;
                            if (remaingLength < GWConstants.MAXBUFSIZE) {
                                readBytes = (int)remaingLength;
                            } else {
                                readBytes = GWConstants.MAXBUFSIZE;
                            }

                            if ((readLength = encryptIS.read(buffer, 0, readBytes)) != -1) {
                                skipOffset += readLength;
                                logger.debug("read {} bytes", readLength);
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
    
                            readLength = fis.read(buffer, 0, readBytes);
                            
                            actualSize += readLength;
                            outputStream.write(buffer, 0, readLength);
                            remaingLength -= readLength;
                        }
                    }
                }
            }
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        if (encryptIS != null) {
            encryptIS.close();
        }
        outputStream.flush();
        outputStream.close();

        return actualSize;
    }

    private long getObjectLocal(OutputStream outputStream, String path, String objId, String versionId, String sourceRange, String key) throws IOException, GWException, InvalidKeyException, NoSuchAlgorithmException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File file = new File(KsanUtils.makeObjPathForOpen(path, objId, versionId));;
        CtrCryptoInputStream encryptIS = null;

        long actualSize = 0L;
        try (FileInputStream fis = new FileInputStream(file)) {
            long remaingLength = 0L;
            int readLength = 0;
            int readBytes;

            if (Strings.isNullOrEmpty(sourceRange)) {
                if (!Strings.isNullOrEmpty(key)) {
                    encryptIS = GWUtils.initCtrDecrypt(fis, key);
                    while ((readLength = encryptIS.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
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
    
                        if (remaingLength >= GWConstants.MAXBUFSIZE) {
                                readLength = GWConstants.MAXBUFSIZE;
                        } else {
                            readLength = (int)remaingLength;
                        }
                        readLength = fis.read(buffer, 0, readBytes);
                        
                        actualSize += readLength;
                        outputStream.write(buffer, 0, readLength);
                        remaingLength -= readLength;
                    }
                }
            } else {
                String[] ranges = sourceRange.split(GWConstants.SLASH);
                for (String range : ranges) {
                    String[] rangeParts = range.split(GWConstants.COMMA);
                    long offset = Longs.tryParse(rangeParts[0]);
                    long length = Longs.tryParse(rangeParts[1]);
                    logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_RANGE, offset, length);

                    remaingLength = length;

                    if (!Strings.isNullOrEmpty(key)) {
                        long skipOffset = 0;
                        encryptIS = GWUtils.initCtrDecrypt(fis, key);

                        if (offset > 0) {
                            long skip = encryptIS.skip(offset);
                            logger.debug("skip : {}", skip);
                        }
                        while (remaingLength > 0) {
                            readBytes = 0;
                            if (remaingLength < GWConstants.MAXBUFSIZE) {
                                readBytes = (int)remaingLength;
                            } else {
                                readBytes = GWConstants.MAXBUFSIZE;
                            }

                            if ((readLength = encryptIS.read(buffer, 0, readBytes)) != -1) {
                                skipOffset += readLength;
                                logger.debug("read {} bytes", readLength);
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
    
                            readLength = fis.read(buffer, 0, readBytes);
                            
                            actualSize += readLength;
                            outputStream.write(buffer, 0, readLength);
                            remaingLength -= readLength;
                        }
                    }
                }
            }
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        if (encryptIS != null) {
            encryptIS.close();
        }
        outputStream.flush();
        outputStream.close();

        return actualSize;
    }

    private long getObjectLocal(OutputStream outputStream, String path, String objId, String versionId, String sourceRange, MessageDigest md5er, String key) throws IOException, GWException, InvalidKeyException, NoSuchAlgorithmException {
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        File file = new File(KsanUtils.makeObjPathForOpen(path, objId, versionId));
        CtrCryptoInputStream encryptIS = null;

        long actualSize = 0L;
        try (FileInputStream fis = new FileInputStream(file)) {
            long remaingLength = 0L;
            int readLength = 0;
            int readBytes;

            if (Strings.isNullOrEmpty(sourceRange)) {
                if (!Strings.isNullOrEmpty(key)) {
                    encryptIS = GWUtils.initCtrDecrypt(fis, key);
                    while ((readLength = encryptIS.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                        actualSize += readLength;
                        outputStream.write(buffer, 0, readLength);
                        md5er.update(buffer, 0, readLength);
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
    
                        if (remaingLength >= GWConstants.MAXBUFSIZE) {
                                readLength = GWConstants.MAXBUFSIZE;
                        } else {
                            readLength = (int)remaingLength;
                        }
                        readLength = fis.read(buffer, 0, readBytes);
                        
                        actualSize += readLength;
                        outputStream.write(buffer, 0, readLength);
                        md5er.update(buffer, 0, readLength);
                        remaingLength -= readLength;
                    }
                }
            } else {
                String[] ranges = sourceRange.split(GWConstants.SLASH);
                for (String range : ranges) {
                    String[] rangeParts = range.split(GWConstants.COMMA);
                    long offset = Longs.tryParse(rangeParts[0]);
                    long length = Longs.tryParse(rangeParts[1]);
                    logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_RANGE, offset, length);

                    remaingLength = length;

                    if (!Strings.isNullOrEmpty(key)) {
                        long skipOffset = 0;
                        encryptIS = GWUtils.initCtrDecrypt(fis, key);

                        if (offset > 0) {
                            long skip = encryptIS.skip(offset);
                            logger.debug("skip : {}", skip);
                        }
                        while (remaingLength > 0) {
                            readBytes = 0;
                            if (remaingLength < GWConstants.MAXBUFSIZE) {
                                readBytes = (int)remaingLength;
                            } else {
                                readBytes = GWConstants.MAXBUFSIZE;
                            }

                            if ((readLength = encryptIS.read(buffer, 0, readBytes)) != -1) {
                                skipOffset += readLength;
                                logger.debug("read {} bytes", readLength);
                                actualSize += readLength;
                                outputStream.write(buffer, 0, readLength);
                                md5er.update(buffer, 0, readLength);
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
    
                            readLength = fis.read(buffer, 0, readBytes);
                            
                            actualSize += readLength;
                            outputStream.write(buffer, 0, readLength);
                            md5er.update(buffer, 0, readLength);
                            remaingLength -= readLength;
                        }
                    }
                }
            }
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        if (encryptIS != null) {
            encryptIS.close();
        }
        outputStream.flush();
        outputStream.close();

        return actualSize;
    }
    
    public S3Object putObject() throws GWException {
        S3Object s3Object = null;

        if (s3Encryption.isEnabledEncryption()) {
            s3Object = putObjectEncryption(s3Meta.getContentLength(), s3Parameter.getInputStream());
        } else {
            s3Object = putObjectNormal(s3Meta.getContentLength(), s3Parameter.getInputStream());
        }
        
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
        long totalReads = 0L;

        try {
            MessageDigest md5er = null;
            if (length < 100 * GWConstants.MEGABYTES) {
                md5er = MessageDigest.getInstance(GWConstants.MD5);
            } else {
                md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            }

            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            
            boolean isPrimaryCache = false;
            boolean isReplicaCache = false;

            // logger.debug("performance mode : {}", GWConfig.getInstance().getPerformanceMode());
            logger.debug("objMeta - replicaCount : {}", objMeta.getReplicaCount());

            // No option
            if (GWConfig.getInstance().isNoOption()) {
                if (objMeta.getReplicaCount() > 1) {
                    logger.debug("bucket : {}, object : {}", objMeta.getBucket(), objMeta.getPath());
                    logger.debug("primary disk id : {}, osd ip : {}", objMeta.getPrimaryDisk().getId(), objMeta.getPrimaryDisk().getOsdIp());
                    if (objMeta.isReplicaExist()) {
                        logger.debug("replica disk id : {}, osd ip : {}", objMeta.getReplicaDisk().getId(), objMeta.getReplicaDisk().getOsdIp());
                    }
                    
                    if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                        if (GWConfig.getInstance().isCacheDiskpath()) {
                            filePrimary = new File(KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            tmpFilePrimary = new File(KsanUtils.makeTempPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            trashPrimary = new File(KsanUtils.makeTrashPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            isPrimaryCache = true;
                        } else {
                            filePrimary = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            tmpFilePrimary = new File(KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            trashPrimary = new File(KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        }
                        // com.google.common.io.Files.createParentDirs(filePrimary);
                        // com.google.common.io.Files.createParentDirs(tmpFilePrimary);
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
                                fileReplica = new File(KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                                tmpFileReplica = new File(KsanUtils.makeTempPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                                trashReplica = new File(KsanUtils.makeTrashPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                                isReplicaCache = true;
                            } else {
                                fileReplica = new File(KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                                tmpFileReplica = new File(KsanUtils.makeTempPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                                trashReplica = new File(KsanUtils.makeTrashPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                            }
                            // com.google.common.io.Files.createParentDirs(fileReplica);
                            // com.google.common.io.Files.createParentDirs(tmpFileReplica);
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
        
                    while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
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

                        KsanUtils.setAttributeFileReplication(tmpFilePrimary, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                        // if (objMeta.getReplicaDisk() != null && !Strings.isNullOrEmpty(objMeta.getReplicaDisk().getId())) {
                        //     setAttributeFileReplication(tmpFilePrimary, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                        // } else {
                            // setAttributeFileReplication(tmpFilePrimary, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        // }

                        retryRenameTo(tmpFilePrimary, filePrimary);
                        if (isPrimaryCache) {
                            String path = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                            // com.google.common.io.Files.createParentDirs(new File(path));
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
                            KsanUtils.setAttributeFileReplication(tmpFileReplica, Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);

                            retryRenameTo(tmpFileReplica, fileReplica);
                            if (isReplicaCache) {
                                String path = KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId);
                                // com.google.common.io.Files.createParentDirs(new File(path));
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
                            file = new File(KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            tmpFile = new File(KsanUtils.makeTempPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            trashFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                            File link = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            // com.google.common.io.Files.createParentDirs(file);
                            // com.google.common.io.Files.createParentDirs(tmpFile);
                            // com.google.common.io.Files.createParentDirs(link);
                            fosPrimary = new FileOutputStream(tmpFile, false);
                            isPrimaryCache = true;
                        } else {
                            file = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            tmpFile = new File(KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            trashFile = new File(KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            // com.google.common.io.Files.createParentDirs(file);
                            // com.google.common.io.Files.createParentDirs(tmpFile);
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
                    // long opStart = System.currentTimeMillis();
                    // logger.info("put pre : {}", opStart - putStart);
                    while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
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

                        KsanUtils.setAttributeFileReplication(tmpFile, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        
                        retryRenameTo(tmpFile, file);
                        if (isPrimaryCache) {
                            String path = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                            Files.createSymbolicLink(Paths.get(path), Paths.get(file.getAbsolutePath()));
                        }
                    }
                }
            }
            // No replication option
            else if (GWConfig.getInstance().isNoReplica()) {
                File file = null;
                File tmpFile = null;
                File trashFile = null;
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        file = new File(KsanUtils.makeObjPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        tmpFile = new File(KsanUtils.makeTempPath(GWConfig.getInstance().getCacheDiskpath() + objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        trashFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId)));
                        File link = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        // com.google.common.io.Files.createParentDirs(file);
                        // com.google.common.io.Files.createParentDirs(tmpFile);
                        // com.google.common.io.Files.createParentDirs(link);
                        fosPrimary = new FileOutputStream(tmpFile, false);
                        isPrimaryCache = true;
                    } else {
                        file = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        tmpFile = new File(KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        trashFile = new File(KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                        // com.google.common.io.Files.createParentDirs(file);
                        // com.google.common.io.Files.createParentDirs(tmpFile);
                        fosPrimary = new FileOutputStream(tmpFile, false);
                    }
                    
                } else {
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    logger.info("osd - {},{}", clientPrimary.getSocket().getRemoteSocketAddress().toString(), clientPrimary.getSocket().getLocalPort());
                    clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), 
                                          objMeta.getObjId(), 
                                          versionId, 
                                          length, 
                                          Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                          Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, //objMeta.getReplicaDisk().getId(), 
                                          GWConstants.EMPTY_STRING,
                                          GWConfig.getInstance().getPerformanceMode());
                }
                
                while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
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

                    KsanUtils.setAttributeFileReplication(tmpFile, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                    
                    retryRenameTo(tmpFile, file);
                    if (isPrimaryCache) {
                        String path = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                        Files.createSymbolicLink(Paths.get(path), Paths.get(file.getAbsolutePath()));
                    }
                }
            }
            // No IO option
            else if (GWConfig.getInstance().isNoIO()) {
                while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                    totalReads += readLength;
                    md5er.update(buffer, 0, readLength);
                }
            }
            // No disk option
            else if (GWConfig.getInstance().isNoDisk()) {
                if (!GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
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
                if (objMeta.getReplicaDisk() != null && !GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    clientReplica = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
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

                while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (clientPrimary != null) {
                        clientPrimary.put(buffer, 0, readLength);
                    }

                    if (clientReplica != null) {
                        clientReplica.put(buffer, 0, readLength);
                    }

                    md5er.update(buffer, 0, readLength);

                    if (totalReads >= length) {
                        break;
                    }
                }
                logger.info("total read : {}", totalReads);

                if (clientPrimary != null) {
                    clientPrimary.putFlush();
                    // OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                    clientPrimary = null;
                }
                if (clientReplica != null) {
                    clientReplica.putFlush();
                    // OSDClientManager.getInstance().returnOSDClient(clientReplica);
                    clientReplica = null;
                }
            } 
            // else {
                // logger.error(GWConstants.LOG_S3OBJECT_OPERATION_OPTION_NO_CASE, GWConfig.getInstance().getPerformanceMode());
                // throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            // }

            byte[] digest = md5er.digest();
			String eTag = base16().lowerCase().encode(digest);

            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(totalReads);
            s3Object.setVersionId(versionId);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (IOException e) { //catch (NoSuchAlgorithmException | IOException e) {
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

        // long putEnd = System.currentTimeMillis();
        // logger.info("put after : {}", putEnd - opEnd);
        return s3Object;
    }

    private S3Object putObjectEncryption(long length, InputStream is) throws GWException {
        S3Object s3Object = new S3Object();

        File filePrimary = null;
        File tmpFilePrimary = null;
        FileOutputStream fosPrimary = null;
        CtrCryptoOutputStream encryptPrimary = null;
        File fileReplica = null;
        File tmpFileReplica = null;
        FileOutputStream fosReplica = null;
        CtrCryptoOutputStream encryptReplica = null;
        File trashPrimary = null;
        File trashReplica = null;
        OSDClient clientPrimary = null;
        OSDClient clientReplica = null;
        long totalReads = 0L;
        long existFileSize = 0L;
        long putSize = 0L;

        try {
            MessageDigest md5er = null;
            if (length < 100 * GWConstants.MEGABYTES) {
                md5er = MessageDigest.getInstance(GWConstants.MD5);
            } else {
                md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            }

            byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
            int readLength = 0;
            
            existFileSize = objMeta.getSize();
            putSize = length;
            boolean isPrimaryCache = false;
            boolean isReplicaCache = false;

            logger.debug("putObjectEncryption ...");
            logger.debug("performance mode : {}", GWConfig.getInstance().getPerformanceMode());
            logger.debug("objMeta - replicaCount : {}", objMeta.getReplicaCount());

            // No option
            if (GWConfig.getInstance().isNoOption()) {
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
                            File file = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            com.google.common.io.Files.createParentDirs(file);
                            com.google.common.io.Files.createParentDirs(filePrimary);
                            com.google.common.io.Files.createParentDirs(tmpFilePrimary);
                            logger.debug("filePrimary path : {}", filePrimary.getAbsolutePath());
                            logger.debug("tmpFilePrimary path : {}", tmpFilePrimary.getAbsolutePath());
                            fosPrimary = new FileOutputStream(tmpFilePrimary, false);
                            isPrimaryCache = true;
                        } else {
                            filePrimary = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            tmpFilePrimary = new File(KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            trashPrimary = new File(KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                            com.google.common.io.Files.createParentDirs(filePrimary);
                            com.google.common.io.Files.createParentDirs(tmpFilePrimary);
                            fosPrimary = new FileOutputStream(tmpFilePrimary, false);
                        }
                        encryptPrimary = GWUtils.initCtrEncrypt(fosPrimary, s3Encryption.getCustomerKey());
                    } else {
                        clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), 
                                              objMeta.getObjId(), 
                                              versionId, 
                                              length, 
                                              Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                              objMeta.getReplicaDisk().getId(), 
                                              s3Encryption.getCustomerKey(),
                                              GWConfig.getInstance().getPerformanceMode());
                    }
                    
                    if (objMeta.isReplicaExist()) {
                        if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                            if (GWConfig.getInstance().isCacheDiskpath()) {
                                fileReplica = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId)));
                                tmpFileReplica = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId)));
                                trashReplica = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId)));
                                File file = new File(KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                                com.google.common.io.Files.createParentDirs(file);
                                com.google.common.io.Files.createParentDirs(fileReplica);
                                com.google.common.io.Files.createParentDirs(tmpFileReplica);
                                logger.debug("fileReplica path : {}", fileReplica.getAbsolutePath());
                                logger.debug("tmpFileReplica path : {}", tmpFileReplica.getAbsolutePath());
                                fosReplica = new FileOutputStream(tmpFileReplica, false);
                                isReplicaCache = true;
                            } else {
                                fileReplica = new File(KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                                tmpFileReplica = new File(KsanUtils.makeTempPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                                trashReplica = new File(KsanUtils.makeTrashPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId));
                                com.google.common.io.Files.createParentDirs(fileReplica);
                                com.google.common.io.Files.createParentDirs(tmpFileReplica);
                                fosReplica = new FileOutputStream(tmpFileReplica, false);
                            }
                            encryptReplica = GWUtils.initCtrEncrypt(fosReplica, s3Encryption.getCustomerKey());
                        } else {
                            clientReplica = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                            // clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                            clientReplica.putInit(objMeta.getReplicaDisk().getPath(), 
                                                objMeta.getObjId(), 
                                                versionId, 
                                                length, 
                                                Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, 
                                                Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, 
                                                s3Encryption.getCustomerKey(),
                                                GWConfig.getInstance().getPerformanceMode());
                        }
                    }

                    while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                        totalReads += readLength;
                        if (filePrimary == null) {
                            clientPrimary.put(buffer, 0, readLength);
                        } else {
                            encryptPrimary.write(buffer, 0, readLength);
                        }
    
                        if (objMeta.isReplicaExist()) {
                            if (fileReplica == null) {
                                clientReplica.put(buffer, 0, readLength);
                            } else {
                                encryptReplica.write(buffer, 0, readLength);
                            }
                        }
    
                        md5er.update(buffer, 0, readLength);

                        if (totalReads >= length) {
                            break;
                        }
                    }
    
                    if (filePrimary == null) {
                        clientPrimary.putFlush();
                        // OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                        clientPrimary = null;
                    } else {
                        encryptPrimary.flush();
                        encryptPrimary.close();

                        if (filePrimary.exists()) {
                            File tempFile = new File(filePrimary.getAbsolutePath());
                            retryRenameTo(tempFile, trashPrimary);
                        }
                        // if (objMeta.getReplicaDisk() != null && !Strings.isNullOrEmpty(objMeta.getReplicaDisk().getId())) {
                        //     setAttributeFileReplication(tmpFilePrimary, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                        // } else {
                        //     setAttributeFileReplication(tmpFilePrimary, GWConstants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, GWConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        // }
                        KsanUtils.setAttributeFileReplication(tmpFilePrimary, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                        retryRenameTo(tmpFilePrimary, filePrimary);
                        if (isPrimaryCache) {
                            String path = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                            Files.createSymbolicLink(Paths.get(path), Paths.get(filePrimary.getAbsolutePath()));
                        }
                    }
                    if (objMeta.isReplicaExist()) {
                        if (fileReplica == null) {
                            clientReplica.putFlush();
                            // OSDClientManager.getInstance().returnOSDClient(clientReplica);
                            clientReplica = null;
                        } else {
                            encryptReplica.flush();
                            encryptReplica.close();

                            if (fileReplica.exists()) {
                                File tempFile = new File(fileReplica.getAbsolutePath());
                                retryRenameTo(tempFile, trashReplica);
                            }
                            KsanUtils.setAttributeFileReplication(tmpFileReplica, Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                            retryRenameTo(tmpFileReplica, fileReplica);
                            if (isReplicaCache) {
                                String path = KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId);
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
                        encryptPrimary = GWUtils.initCtrEncrypt(fosPrimary, s3Encryption.getCustomerKey());
                    } else {
                        clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                        clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), 
                                              objMeta.getObjId(), 
                                              versionId, 
                                              length, 
                                              Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                              Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL,//objMeta.getReplicaDisk().getId(), 
                                              GWConstants.EMPTY_STRING,
                                              GWConfig.getInstance().getPerformanceMode());
                    }
                    
    
                    while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                        totalReads += readLength;
                        if (file == null) {
                            clientPrimary.put(buffer, 0, readLength);
                        } else {
                            encryptPrimary.write(buffer, 0, readLength);
                        }
    
                        md5er.update(buffer, 0, readLength);

                        if (totalReads >= length) {
                            break;
                        }
                    }
    
                    if (file == null) {
                        clientPrimary.putFlush();
                        // OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                        clientPrimary = null;
                    } else {
                        encryptPrimary.flush();
                        encryptPrimary.close();

                        if (file.exists()) {
                            File tempFile = new File(file.getAbsolutePath());
                            retryRenameTo(tempFile, trashFile);
                        }

                        KsanUtils.setAttributeFileReplication(tmpFile, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                        
                        retryRenameTo(tmpFile, file);
                        if (isPrimaryCache) {
                            String path = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                            Files.createSymbolicLink(Paths.get(path), Paths.get(file.getAbsolutePath()));
                        }
                    }
                }
            }
            // No replication option
            else if (GWConfig.getInstance().isNoReplica()) {
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
                    encryptPrimary = GWUtils.initCtrEncrypt(fosPrimary, s3Encryption.getCustomerKey());
                } else {
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), 
                                          objMeta.getObjId(), 
                                          versionId, 
                                          length, 
                                          Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                          Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, //objMeta.getReplicaDisk().getId(), 
                                          s3Encryption.getCustomerKey(), 
                                          GWConfig.getInstance().getPerformanceMode());
                }
                
                while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (file == null) {
                        clientPrimary.put(buffer, 0, readLength);
                    } else {
                        encryptPrimary.write(buffer, 0, readLength);
                    }

                    md5er.update(buffer, 0, readLength);

                    if (totalReads >= length) {
                        break;
                    }
                }

                if (file == null) {
                    clientPrimary.putFlush();
                    // OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                    clientPrimary = null;
                } else {
                    encryptPrimary.flush();
                    encryptPrimary.close();

                    if (file.exists()) {
                        File tempFile = new File(file.getAbsolutePath());
                        retryRenameTo(tempFile, trashFile);
                    }

                    KsanUtils.setAttributeFileReplication(tmpFile, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                    
                    retryRenameTo(tmpFile, file);
                    if (isPrimaryCache) {
                        String path = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                        Files.createSymbolicLink(Paths.get(path), Paths.get(file.getAbsolutePath()));
                    }
                }
            }
            // No IO option
            else if (GWConfig.getInstance().isNoIO()) {
                while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) > 0) {
                    totalReads += readLength;
                    md5er.update(buffer, 0, readLength);
                }
            }
            // No disk option
            else if (GWConfig.getInstance().isNoDisk()) {
                if (!GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    clientPrimary.putInit(objMeta.getPrimaryDisk().getPath(), 
                                          objMeta.getObjId(), 
                                          versionId, 
                                          length, 
                                          Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, 
                                          objMeta.getReplicaDisk().getId(), 
                                          s3Encryption.getCustomerKey(),
                                          GWConfig.getInstance().getPerformanceMode());
                }
                if (objMeta.getReplicaDisk() != null && !GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                    clientReplica = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                    clientReplica.putInit(objMeta.getReplicaDisk().getPath(), 
                                          objMeta.getObjId(), 
                                          versionId, 
                                          length, 
                                          Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, 
                                          Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, 
                                          s3Encryption.getCustomerKey(),
                                          GWConfig.getInstance().getPerformanceMode());
                }

                while ((readLength = is.read(buffer, 0, GWConstants.BUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (clientPrimary != null) {
                        clientPrimary.put(buffer, 0, readLength);
                    }

                    if (clientReplica != null) {
                        clientReplica.put(buffer, 0, readLength);
                    }

                    md5er.update(buffer, 0, readLength);

                    if (totalReads >= length) {
                        break;
                    }
                }

                if (clientPrimary != null) {
                    clientPrimary.putFlush();
                    // OSDClientManager.getInstance().returnOSDClient(clientPrimary);
                    clientPrimary = null;
                }
                if (clientReplica != null) {
                    clientReplica.putFlush();
                    // OSDClientManager.getInstance().returnOSDClient(clientReplica);
                    clientReplica = null;
                }
            } else {
                logger.error(GWConstants.LOG_S3OBJECT_OPERATION_OPTION_NO_CASE, GWConfig.getInstance().getPerformanceMode());
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            }

            byte[] digest = md5er.digest();
			String eTag = base16().lowerCase().encode(digest);

            s3Object.setEtag(eTag);
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(totalReads);
            s3Object.setVersionId(versionId);
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
        } catch (IOException e) {
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

    public boolean deleteObject() throws GWException {
        OSDClient client = null;

        // check EC exists
        File ecFile = new File(KsanUtils.makeECPathForOpen(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
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
                String getPath = KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objMeta.getObjId(), versionId);
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
        S3Metadata s3Metadata = S3Metadata.getS3Metadata(objMeta.getMeta());
        if (!Strings.isNullOrEmpty(s3Metadata.getUploadId())) {
            try {
                File multipartFile = null;
                if (GWConfig.getInstance().isCacheDiskpath()) {
                    multipartFile = new File(GWConfig.getInstance().getCacheDiskpath() + KsanUtils.makeObjPathForOpen(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
                } else {
                    multipartFile = new File(KsanUtils.makeObjPathForOpen(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
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
                    throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                }
            } catch (Exception e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            }
            return true;
        }

        try {
            if (objMeta.getReplicaCount() > 1) {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    deleteObjectLocal(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId());
                } else {
                    client = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    // client = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    client.delete(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId());
                    // OSDClientManager.getInstance().returnOSDClient(client);
                    // client = null;
                } 
                
                if (objMeta.isReplicaExist()) {
                    if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                        deleteObjectLocal(objMeta.getReplicaDisk().getPath(), objMeta.getObjId());
                    } else {
                        client = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        // client = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        client.delete(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId());
                        // OSDClientManager.getInstance().returnOSDClient(client);
                        // client = null;
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
                    // client = null;
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } finally {
            if (client != null) {
                try {
                    // OSDClientManager.getInstance().returnOSDClient(client);
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                    throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                }
            }
        }
        logger.info(GWConstants.LOG_S3OBJECT_OPERATION_DELETE, objMeta.getBucket(), objMeta.getPath(), versionId);
        return true;
    }

    private void deleteObjectLocal(String path, String objId) throws IOException, GWException {
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
                link.delete();
            }
        }
    }

    public S3Object uploadPart(String path, long length) throws GWException {
        S3Object s3Object = null;

        if (s3Encryption.isEnabledEncryption()) {
            s3Object = uploadPartEncryption(path, length, s3Parameter.getInputStream());
        } else {
            s3Object = uploadPartNormal(path, length, s3Parameter.getInputStream());
        }
        
        return s3Object;
    }

    private S3Object uploadPartNormal(String path, long length, InputStream is) throws GWException {
        S3Object s3Object = new S3Object();
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
            MessageDigest md5er = null;
            if (length < 100 * GWConstants.MEGABYTES) {
                md5er = MessageDigest.getInstance(GWConstants.MD5);
            } else {
                md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            }

            logger.debug("upload part ... path:{}, partNumber:{}, length:{}", path, s3Parameter.getPartNumber(), length);
            if (objMeta.getReplicaCount() > 1) {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                    } else {
                        filePrimary = new File(KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                    }
                    fosPrimary = new FileOutputStream(filePrimary, false);
                } else {
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // if (clientPrimary == null) {
                    //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                    //     clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // }
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    logger.info("osd - {},{}", clientPrimary.getSocket().getRemoteSocketAddress().toString(), clientPrimary.getSocket().getLocalPort());
                    // clientPrimary.partInit(objMeta.getPrimaryDisk().getPath(), 
                    //                        objMeta.getObjId(), 
                    //                        s3Parameter.getPartNumber(), 
                    //                        length,
                    //                        GWConstants.EMPTY_STRING);
                }
                
                if (objMeta.isReplicaExist()) {
                    if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                        if (GWConfig.getInstance().isCacheDiskpath()) {
                            fileReplica = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber())));
                        } else {
                            fileReplica = new File(KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                        }
                        fosReplica = new FileOutputStream(fileReplica, false);
                    } else {
                        // clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        // if (clientReplica == null) {
                        //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getReplicaDisk().getOsdIp());
                        //     clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        // }
                        clientReplica = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        logger.info("osd - {},{}", clientReplica.getSocket().getRemoteSocketAddress().toString(), clientReplica.getSocket().getLocalPort());
                        // clientReplica.partInit(objMeta.getReplicaDisk().getPath(), 
                        //                    objMeta.getObjId(), 
                        //                    s3Parameter.getPartNumber(), 
                        //                    length,
                        //                    GWConstants.EMPTY_STRING);
                    }
                }
    
                while ((readLength = is.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
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
                        filePrimary = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                    } else {
                        filePrimary = new File(KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                    }
                    fosPrimary = new FileOutputStream(filePrimary, false);
                } else {
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // if (clientPrimary == null) {
                    //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                    //     clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // }
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    logger.info("osd - {},{}", clientPrimary.getSocket().getRemoteSocketAddress().toString(), clientPrimary.getSocket().getLocalPort());
                    // clientPrimary.partInit(objMeta.getPrimaryDisk().getPath(), 
                    //                        objMeta.getObjId(), 
                    //                        s3Parameter.getPartNumber(), 
                    //                        length,
                    //                        GWConstants.EMPTY_STRING);
                }

                while ((readLength = is.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
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

    private S3Object uploadPartEncryption(String path, long length, InputStream is) throws GWException {
        S3Object s3Object = new S3Object();
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        int readLength = 0;
        long totalReads = 0L;
        File filePrimary = null;
        File fileReplica = null;
        FileOutputStream fosPrimary = null;
        FileOutputStream fosReplica = null;
        OSDClient clientPrimary = null;
        OSDClient clientReplica = null;
        CtrCryptoOutputStream encryptPrimary = null;
        CtrCryptoOutputStream encryptReplica = null;

        try {
            MessageDigest md5er = null;
            if (length < 100 * GWConstants.MEGABYTES) {
                md5er = MessageDigest.getInstance(GWConstants.MD5);
            } else {
                md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
            }

            if (objMeta.getReplicaCount() > 1) {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                    } else {
                        filePrimary = new File(KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                    }
                    fosPrimary = new FileOutputStream(filePrimary, false);
                    encryptPrimary = GWUtils.initCtrEncrypt(fosPrimary, s3Encryption.getCustomerKey());
                } else {
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // if (clientPrimary == null) {
                    //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                    //     clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // }
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    logger.info("osd - {},{}", clientPrimary.getSocket().getRemoteSocketAddress().toString(), clientPrimary.getSocket().getLocalPort());
                    // clientPrimary.partInit(objMeta.getPrimaryDisk().getPath(), 
                    //                        objMeta.getObjId(), 
                    //                        s3Parameter.getPartNumber(), 
                    //                        length,
                    //                        s3Encryption.getCustomerKey());
                }
                
                if (objMeta.isReplicaExist()) {
                    if (GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                        if (GWConfig.getInstance().isCacheDiskpath()) {
                            fileReplica = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                        } else {
                            fileReplica = new File(KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                        }
                        fosReplica = new FileOutputStream(fileReplica, false);
                        encryptReplica = GWUtils.initCtrEncrypt(fosReplica, s3Encryption.getCustomerKey());
                    } else {
                        // clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        // if (clientReplica == null) {
                        //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getReplicaDisk().getOsdIp());
                        //     clientReplica = OSDClientManager.getInstance().getOSDClient(objMeta.getReplicaDisk().getOsdIp());
                        // }
                        clientReplica = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                        logger.info("osd - {},{}", clientReplica.getSocket().getRemoteSocketAddress().toString(), clientReplica.getSocket().getLocalPort());
                        // clientReplica.partInit(objMeta.getReplicaDisk().getPath(), 
                        //                    objMeta.getObjId(), 
                        //                    s3Parameter.getPartNumber(), 
                        //                    length,
                        //                    s3Encryption.getCustomerKey());
                    }
                }
    
                while ((readLength = is.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (filePrimary == null) {
                        clientPrimary.put(buffer, 0, readLength);
                    } else {
                        encryptPrimary.write(buffer, 0, readLength);
                    }
    
                    if (objMeta.isReplicaExist()) {
                        if (fileReplica == null) {
                            clientReplica.put(buffer, 0, readLength);
                        } else {
                            encryptReplica.write(buffer, 0, readLength);
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
                    encryptPrimary.flush();
                    encryptPrimary.close();
                }

                if (objMeta.isReplicaExist()) {
                    if (fileReplica == null) {
                        clientReplica.putFlush();
                        clientReplica = null;
                    } else {
                        encryptReplica.flush();
                        encryptReplica.close();
                    }
                }
            } else {
                if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        filePrimary = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                    } else {
                        filePrimary = new File(KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
                    }
                    fosPrimary = new FileOutputStream(filePrimary, false);
                    encryptPrimary = GWUtils.initCtrEncrypt(fosPrimary, s3Encryption.getCustomerKey());
                } else {
                    // clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // if (clientPrimary == null) {
                    //     OSDClientManager.getInstance().addClient((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount(), objMeta.getPrimaryDisk().getOsdIp());
                    //     clientPrimary = OSDClientManager.getInstance().getOSDClient(objMeta.getPrimaryDisk().getOsdIp());
                    // }
                    clientPrimary = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                    logger.info("osd - {},{}", clientPrimary.getSocket().getRemoteSocketAddress().toString(), clientPrimary.getSocket().getLocalPort());
                    // clientPrimary.partInit(objMeta.getPrimaryDisk().getPath(), 
                    //                        objMeta.getObjId(), 
                    //                        s3Parameter.getPartNumber(), 
                    //                        length,
                    //                        s3Encryption.getCustomerKey());
                }

                while ((readLength = is.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                    totalReads += readLength;
                    if (filePrimary == null) {
                        clientPrimary.put(buffer, 0, readLength);
                    } else {
                        encryptPrimary.write(buffer, 0, readLength);
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
                    encryptPrimary.flush();
                    encryptPrimary.close();
                }
            }

            logger.debug("Total read : {}", totalReads);

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
        String host = DiskManager.getInstance().getOSDIP(diskID);
        if (host == null) {
            logger.error(GWConstants.LOG_S3OBJECT_OPERATION_DISK_IP_NULL, diskID);
            return;
        }

        String path = objMeta.getPrimaryDisk().getPath();
        if (!GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
            // OSDClient client = OSDClientManager.getInstance().getOSDClient(host);
            OSDClient client = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
            // client.deletePart(path, objMeta.getObjId(), s3Parameter.getPartNumber());
            // OSDClientManager.getInstance().returnOSDClient(client);
        } else {
            File tmpFile = null;
            if (GWConfig.getInstance().isCacheDiskpath()) {
                tmpFile = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
            } else {
                tmpFile = new File(KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
            }

            if (tmpFile.exists()) {
                tmpFile.delete();
            } else {
                logger.error("part file not exists : {}" + tmpFile.getAbsolutePath());
            }            
        }
    }

    // private boolean checkPartRef(ObjMultipart objMultipart, String uploadId, int partNumber) {
    //     try {
    //         String reference = objMultipart.getPartRef(uploadId, partNumber);
    //         logger.error("{},{} reference : {}", uploadId, partNumber, reference);
    //         if (!Strings.isNullOrEmpty(reference)) {
    //             String[] infos = reference.split(GWConstants.COLON);
    //             if (infos.length == 1) {
    //                 if (infos[0].equals(objMeta.getObjId() + versionId)) {
    //                     return true;
    //                 }
    //             } else if (infos.length >= 2) {
    //                 int index = 0;
    //                 String newReference = null;
    //                 while (index < infos.length) {
    //                     if (infos[index].equals(objMeta.getObjId() + versionId)) {
    //                         index++;
    //                         continue;
    //                     } else {
    //                         if (newReference == null) {
    //                             newReference = infos[index];
    //                         } else {
    //                             newReference += GWConstants.COLON + infos[index];
    //                         }
    //                     }
    //                     index++;
    //                 }
    //                 objMultipart.putPartRef(uploadId, partNumber, newReference);
    //             }
    //         }
    //     } catch (Exception e) {
    //         PrintStack.logging(logger, e);
    //     }
    //     return false;
    // }

    // private void setPartRef(ObjMultipart objMultipart, String uploadId, int partNumber) {
    //     try {
    //         String reference = objMultipart.getPartRef(uploadId, partNumber);
    //         if (Strings.isNullOrEmpty(reference)) {
    //             reference = objMeta.getObjId() + versionId;
    //             objMultipart.putPartRef(uploadId, partNumber, reference);
    //         } else {
    //             if (!reference.contains(objMeta.getObjId() + versionId)) {
    //                 reference += GWConstants.COLON + objMeta.getObjId() + versionId;
    //                 objMultipart.putPartRef(uploadId, partNumber, reference);
    //             }
    //         }
    //     } catch (Exception e) {
    //         PrintStack.logging(logger, e);
    //     }
    // }

    public S3Object completeMultipart(SortedMap<Integer, Part> listPart) throws Exception {
        S3Object s3Object = new S3Object();
        byte[] buffer = new byte[GWConstants.MAXBUFSIZE];
        // MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
        // MessageDigest md5er = MessageDigest.getInstance(GWConstants.MD5);
        
        long totalLength = 0L;
        long existFileSize = 0L;
        long putSize = 0L;
        CtrCryptoOutputStream encryptOS = null;
        CtrCryptoInputStream encryptIS = null;
        File file = null;
        File tmpFile = null;
        File trashFile = null;
        boolean isCacheDiskpath = false;

        String path = objMeta.getPrimaryDisk().getPath();

        // if (GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
            file = new File(KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
            // file = new File(KsanUtils.makeObjMultipartPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId, s3Parameter.getUploadId()));
            tmpFile = new File(KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
            trashFile = new File(KsanUtils.makeTrashPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
        // }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile))) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, Part> entry = it.next();
                totalLength += entry.getValue().getPartSize();
                // String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getDiskID());
                String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getPrimaryDiskId());
                if (entry.getValue().getPartETag().equals(GWConstants.DIRECTORY_MD5)) {
                    // partCopy
                    logger.debug("partCopy...");
                    File copyPartFile = new File(KsanUtils.makePartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                    try (BufferedReader br = new BufferedReader(new FileReader(copyPartFile))) {
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            // // check reference
                            // String[] infos = line.split(GWConstants.COLON);
                            // String[] uploadInfos = infos[1].split(GWConstants.UNDERSCORE);
                            // setPartRef(objMultipart, uploadInfos[1], Integer.parseInt(uploadInfos[2]));
                            line += System.lineSeparator();
                            bw.write(line);
                        }
                    } catch(Exception e) {
                        PrintStack.logging(logger, e);
                        throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                    }
                } else {
                    sb.setLength(0);
                    // sb.append(entry.getValue().getDiskID());
                    sb.append(entry.getValue().getPrimaryDiskId());
                    sb.append(GWConstants.COLON);
                    sb.append(KsanUtils.makePartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                    sb.append(GWConstants.COLON);
                    sb.append(entry.getValue().getPartSize());
                    sb.append(GWConstants.COLON);
                    sb.append(System.lineSeparator());
                    bw.write(sb.toString());
                    // // check reference
                    // setPartRef(objMultipart, s3Parameter.getUploadId(), entry.getValue().getPartNumber());
                }
            }
        } catch(Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        s3Object.setLastModified(new Date());
        s3Object.setFileSize(totalLength);
        s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);

        if (file.exists()) {
            File tempFile = new File(file.getAbsolutePath());
            retryRenameTo(tempFile, trashFile);
        }
        retryRenameTo(tmpFile, file);
        

        /*
        if (!GWUtils.getLocalIP().equals(objMeta.getPrimaryDisk().getOsdIp())) {
            logger.info(GWConstants.LOG_CANNOT_FIND_LOCAL_PATH, objMeta.getPrimaryDisk().getId());
            String partInfos = GWConstants.EMPTY_STRING;
            for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, Part> entry = it.next();
                partInfos += entry.getValue().getPartNumber() + GWConstants.SLASH 
                            + entry.getValue().getDiskID() + GWConstants.SLASH 
                            + entry.getValue().getPartSize() + GWConstants.COMMA;
            }
            logger.debug("partInfos : {}", partInfos);
            OSDClient client = new OSDClient(objMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
            OsdData data = null;
            if (objMeta.getReplicaCount() > 1) {
                data = client.completeMultipart(path, objMeta.getObjId(), versionId, s3Encryption.getCustomerKey(), Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId(), partInfos);
            } else {
                data = client.completeMultipart(path, objMeta.getObjId(), versionId, s3Encryption.getCustomerKey(), Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, partInfos);
            }
            
            s3Object.setEtag(data.getETag());
            s3Object.setLastModified(new Date());
            s3Object.setFileSize(data.getFileSize());
            s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
            // return s3Object;
        } else {
            if (GWConfig.getInstance().isCacheDiskpath()) {
                isCacheDiskpath = true;
                file = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeObjPath(path, objMeta.getObjId(), versionId)));
                tmpFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempCompleteMultipartPath(path, objMeta.getObjId(), versionId)));
                trashFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(path, objMeta.getObjId(), versionId)));
            } else {
                file = new File(KsanUtils.makeObjPath(path, objMeta.getObjId(), versionId));
                tmpFile = new File(KsanUtils.makeTempCompleteMultipartPath(path, objMeta.getObjId(), versionId));
                trashFile = new File(KsanUtils.makeTrashPath(path, objMeta.getObjId(), versionId));
            }
            com.google.common.io.Files.createParentDirs(file);
            com.google.common.io.Files.createParentDirs(tmpFile);
    
            try (FileOutputStream tmpOut = new FileOutputStream(tmpFile)) {
                if (s3Encryption.isEnabledEncryption()) {
                    encryptOS = GWUtils.initCtrEncrypt(tmpOut, s3Encryption.getCustomerKey());
                    // for each part object
                    for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<Integer, Part> entry = it.next();
                        String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getDiskID());
                        if (!Strings.isNullOrEmpty(partPath)) {
                            // part is in local disk
                            logger.debug("part : {}, diskID : {}, part path : {}", entry.getKey(), entry.getValue().getDiskID(), partPath);
                            File partFile = null;
                            if (GWConfig.getInstance().isCacheDiskpath()) {
                                partFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber()))));
                            } else {
                                partFile = new File(KsanUtils.makeTempPartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                            }
    
                            totalLength += partFile.length();
                            logger.debug("totalLength: {}", totalLength);
                            try (FileInputStream fis = new FileInputStream(partFile)) {
                                // encryptIS = GWUtils.initCtrDecrypt(fis, s3Encryption.getCustomerKey());
                                int readLength = 0;
                                while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                                    // totalLength += readLength;
                                    encryptOS.write(buffer, 0, readLength);
                                    // md5er.update(buffer, 0, readLength);
                                }
                                encryptOS.flush();
                                // encryptIS.close();
                            }
                        } else {
                            partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                            String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                            OSDClient client = new OSDClient(host, (int)GWConfig.getInstance().getOsdPort());
                            // client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), encryptOS, md5er);
                            client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), encryptOS);
                            totalLength += client.getPart();
    
                            // partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                            // String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                            // OSDClient client = new OSDClient(host, (int)GWConfig.getInstance().getOsdPort());
                            // // OSDClient client = OSDClientManager.getInstance().getOSDClient(host);
                            // client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), encryptOS, md5er);
                            // totalLength += client.getPart();
                            // OSDClientManager.getInstance().returnOSDClient(client);
                        }
                    }
                    encryptOS.close();
                } else {
                    // for each part object
                    for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<Integer, Part> entry = it.next();
                        String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getDiskID());
                        if (!Strings.isNullOrEmpty(partPath)) {
                            // part is in local disk
                            logger.debug("part : {}, diskID : {}, part path : {}", entry.getKey(), entry.getValue().getDiskID(), partPath);
                            File partFile = null;
                            if (GWConfig.getInstance().isCacheDiskpath()) {
                                partFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber()))));
                            } else {
                                partFile = new File(KsanUtils.makeTempPartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                            }
                            totalLength += partFile.length();
                            logger.debug("totalLength: {}", totalLength);
                            try (FileInputStream fis = new FileInputStream(partFile)) {
                                int readLength = 0;
                                while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                                    // totalLength += readLength;
                                    tmpOut.write(buffer, 0, readLength);
                                    // md5er.update(buffer, 0, readLength);
                                }
                                tmpOut.flush();
                            }
                        } else {
                            partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                            String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                            OSDClient client = new OSDClient(host, (int)GWConfig.getInstance().getOsdPort());
                            // client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), tmpOut, md5er);
                            client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), tmpOut);
                            totalLength += client.getPart();
    
                            // partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                            // String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                            // OSDClient client = new OSDClient(host, (int)GWConfig.getInstance().getOsdPort());
                            // // OSDClient client = OSDClientManager.getInstance().getOSDClient(host);
                            // client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), tmpOut, md5er);
                            // totalLength += client.getPart();
                            // OSDClientManager.getInstance().returnOSDClient(client);
                        }
                    }
                    tmpOut.close();
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
                if (objMeta.getReplicaCount() > 1) {
                    KsanUtils.setAttributeFileReplication(tmpFile, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, objMeta.getReplicaDisk().getId());
                } else {
                    KsanUtils.setAttributeFileReplication(tmpFile, Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                }
                retryRenameTo(tmpFile, file);
                if (isCacheDiskpath) {
                    String filePath = KsanUtils.makeObjPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId);
                    Files.createSymbolicLink(Paths.get(filePath), Paths.get(file.getAbsolutePath()));
                }
    
                // s3Object.setEtag(eTag);
                s3Object.setLastModified(new Date());
                s3Object.setFileSize(totalLength);
                s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
            }
        }

        if (objMeta.getReplicaCount() > 1) {
            if (!GWUtils.getLocalIP().equals(objMeta.getReplicaDisk().getOsdIp())) {
                logger.info(GWConstants.LOG_CANNOT_FIND_LOCAL_PATH, objMeta.getPrimaryDisk().getId());
                String partInfos = GWConstants.EMPTY_STRING;
                for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<Integer, Part> entry = it.next();
                    partInfos += entry.getValue().getPartNumber() + GWConstants.SLASH 
                                + entry.getValue().getDiskID() + GWConstants.SLASH 
                                + entry.getValue().getPartSize() + GWConstants.COMMA;
                }
                logger.debug("partInfos : {}", partInfos);
                OSDClient client = new OSDClient(objMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                // OsdData data = client.completeMultipart(path, objMeta.getObjId(), versionId, s3Encryption.getCustomerKey(), partInfos);
                OsdData data = client.completeMultipart(path, objMeta.getObjId(), versionId, s3Encryption.getCustomerKey(), Constants.FILE_ATTRUBUTE_REPLICATION_PRIMARY, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL, partInfos);
                s3Object.setEtag(data.getETag());
                s3Object.setLastModified(new Date());
                s3Object.setFileSize(data.getFileSize());
                s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
                // return s3Object;
            } else {
                if (GWConfig.getInstance().isCacheDiskpath()) {
                    isCacheDiskpath = true;
                    file = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeObjPath(path, objMeta.getObjId(), versionId)));
                    tmpFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempCompleteMultipartPath(path, objMeta.getObjId(), versionId)));
                    trashFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTrashPath(path, objMeta.getObjId(), versionId)));
                } else {
                    file = new File(KsanUtils.makeObjPath(path, objMeta.getObjId(), versionId));
                    tmpFile = new File(KsanUtils.makeTempCompleteMultipartPath(path, objMeta.getObjId(), versionId));
                    trashFile = new File(KsanUtils.makeTrashPath(path, objMeta.getObjId(), versionId));
                }
                com.google.common.io.Files.createParentDirs(file);
                com.google.common.io.Files.createParentDirs(tmpFile);
        
                try (FileOutputStream tmpOut = new FileOutputStream(tmpFile)) {
                    if (s3Encryption.isEnabledEncryption()) {
                        encryptOS = GWUtils.initCtrEncrypt(tmpOut, s3Encryption.getCustomerKey());
                        // for each part object
                        for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                            Map.Entry<Integer, Part> entry = it.next();
                            String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getDiskID());
                            if (!Strings.isNullOrEmpty(partPath)) {
                                // part is in local disk
                                logger.debug("part : {}, diskID : {}, part path : {}", entry.getKey(), entry.getValue().getDiskID(), partPath);
                                File partFile = null;
                                if (GWConfig.getInstance().isCacheDiskpath()) {
                                    partFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber()))));
                                } else {
                                    partFile = new File(KsanUtils.makeTempPartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                                }
        
                                totalLength += partFile.length();
                                logger.debug("totalLength: {}", totalLength);
                                try (FileInputStream fis = new FileInputStream(partFile)) {
                                    // encryptIS = GWUtils.initCtrDecrypt(fis, s3Encryption.getCustomerKey());
                                    int readLength = 0;
                                    while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                                        // totalLength += readLength;
                                        encryptOS.write(buffer, 0, readLength);
                                        // md5er.update(buffer, 0, readLength);
                                    }
                                    encryptOS.flush();
                                    // encryptIS.close();
                                }
                            } else {
                                partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                                String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                                OSDClient client = new OSDClient(host, (int)GWConfig.getInstance().getOsdPort());
                                // client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), encryptOS, md5er);
                                client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), encryptOS);
                                totalLength += client.getPart();
        
                                // partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                                // String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                                // OSDClient client = new OSDClient(host, (int)GWConfig.getInstance().getOsdPort());
                                // // OSDClient client = OSDClientManager.getInstance().getOSDClient(host);
                                // client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), encryptOS, md5er);
                                // totalLength += client.getPart();
                                // OSDClientManager.getInstance().returnOSDClient(client);
                            }
                        }
                        encryptOS.close();
                    } else {
                        // for each part object
                        for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                            Map.Entry<Integer, Part> entry = it.next();
                            String partPath = DiskManager.getInstance().getLocalPath(entry.getValue().getDiskID());
                            if (!Strings.isNullOrEmpty(partPath)) {
                                // part is in local disk
                                logger.debug("part : {}, diskID : {}, part path : {}", entry.getKey(), entry.getValue().getDiskID(), partPath);
                                File partFile = null;
                                if (GWConfig.getInstance().isCacheDiskpath()) {
                                    partFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makeTempPartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber()))));
                                } else {
                                    partFile = new File(KsanUtils.makeTempPartPath(partPath, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                                }
                                totalLength += partFile.length();
                                logger.debug("totalLength: {}", totalLength);
                                try (FileInputStream fis = new FileInputStream(partFile)) {
                                    int readLength = 0;
                                    while ((readLength = fis.read(buffer, 0, GWConstants.MAXBUFSIZE)) != -1) {
                                        // totalLength += readLength;
                                        tmpOut.write(buffer, 0, readLength);
                                        // md5er.update(buffer, 0, readLength);
                                    }
                                    tmpOut.flush();
                                }
                            } else {
                                partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                                String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                                OSDClient client = new OSDClient(host, (int)GWConfig.getInstance().getOsdPort());
                                // client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), tmpOut, md5er);
                                client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), tmpOut);
                                totalLength += client.getPart();
        
                                // partPath = DiskManager.getInstance().getPath(entry.getValue().getDiskID());
                                // String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                                // OSDClient client = new OSDClient(host, (int)GWConfig.getInstance().getOsdPort());
                                // // OSDClient client = OSDClientManager.getInstance().getOSDClient(host);
                                // client.getPartInit(partPath, objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()), entry.getValue().getPartSize(), tmpOut, md5er);
                                // totalLength += client.getPart();
                                // OSDClientManager.getInstance().returnOSDClient(client);
                            }
                        }
                        tmpOut.close();
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
                    KsanUtils.setAttributeFileReplication(tmpFile, Constants.FILE_ATTRIBUTE_REPLICATION_REPLICA, Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID_NULL);
                    retryRenameTo(tmpFile, file);
                    if (isCacheDiskpath) {
                        String filePath = KsanUtils.makeObjPath(objMeta.getReplicaDisk().getPath(), objMeta.getObjId(), versionId);
                        Files.createSymbolicLink(Paths.get(filePath), Paths.get(file.getAbsolutePath()));
                    }
        
                    // s3Object.setEtag(eTag);
                    s3Object.setLastModified(new Date());
                    s3Object.setFileSize(totalLength);
                    s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);
                }
            }
        } */
        
        // delete part file
        // abortMultipart(listPart);
        // tmpFile.delete();

        return s3Object;
    }

    public void abortMultipart(SortedMap<Integer, Part> listPart) throws GWException {
        try {
            String path = null;
            for (Iterator<Map.Entry<Integer, Part>> it = listPart.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, Part> entry = it.next();
                // logger.info("key : {}, diskId : {}", entry.getKey(), entry.getValue().getDiskID());
                // path = DiskManager.getInstance().getLocalPath(entry.getValue().getDiskID());
                logger.info("key : {}, diskId : {}", entry.getKey(), entry.getValue().getPrimaryDiskId());
                path = DiskManager.getInstance().getLocalPath(entry.getValue().getPrimaryDiskId());

                if (!Strings.isNullOrEmpty(path)) {
                    // part is in local disk
                    File partFile = null;
                    if (GWConfig.getInstance().isCacheDiskpath()) {
                        partFile = new File(KsanUtils.makePartPath(GWConfig.getInstance().getCacheDiskpath() + path, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                    } else {
                        partFile = new File(KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), String.valueOf(entry.getValue().getPartNumber())));
                    }
                    partFile.delete();
                } else {
                    // String host = DiskManager.getInstance().getOSDIP(entry.getValue().getDiskID());
                    String host = DiskManager.getInstance().getOSDIP(entry.getValue().getPrimaryDiskId());
                    OSDClient client = new OSDClient(host, (int)GWConfig.getInstance().getOsdPort());
                    // OSDClient client = OSDClientManager.getInstance().getOSDClient(host);
                    // client.deletePart(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), String.valueOf(entry.getValue().getPartNumber()));
                    // OSDClientManager.getInstance().returnOSDClient(client);
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
    }

    public S3Object uploadPartCopy(String path, Metadata srcObjMeta, S3Range s3Range, S3Encryption srcEncryption) throws ResourceNotFoundException, Exception {
        S3Object s3Object = new S3Object();
        OSDClient client = null;
        String srcKey = srcEncryption.getCustomerKey();
        long actualSize = 0L;
        CtrCryptoOutputStream encryptOS = null;
        OutputStream outputStream = null;

        long offset = 0L;
        long last = 0L;
        
        String copySourceRange = GWConstants.EMPTY_STRING;
        if (s3Range != null && s3Range.getListRange().size() > 0) {
            for (S3Range.Range range : s3Range.getListRange()) {
                offset = range.getOffset();
                last = range.getLast();
                if (Strings.isNullOrEmpty(copySourceRange)) {
                    copySourceRange = String.valueOf(offset) + GWConstants.COMMA + String.valueOf(last);
                } else {
                    copySourceRange += GWConstants.SLASH + String.valueOf(offset) + GWConstants.COMMA + String.valueOf(last);
                }
            }
            logger.debug(GWConstants.LOG_S3OBJECT_OPERATION_COPY_SOURCE_RANGE, copySourceRange);
            actualSize = last - offset + 1;
            logger.debug("size : {}, offset : {}, last : {}", actualSize, offset, last);
        } else {
            actualSize = srcObjMeta.getSize();
        }
        
        File srcFile = null;
        File tmpFile = null;
        boolean isSrcMultipart = false;
        BufferedWriter bw = null;

        try {
            // srcFile check
            S3Metadata s3Metadata = S3Metadata.getS3Metadata(srcObjMeta.getMeta());
            logger.debug("src has uploadId:{}", s3Metadata.getUploadId());

            if (!Strings.isNullOrEmpty(s3Metadata.getUploadId())) {
                isSrcMultipart = true;
            }

            if (GWConfig.getInstance().isCacheDiskpath()) {
                srcFile = new File(GWConfig.getInstance().getCacheDiskpath() + KsanUtils.makeObjPathForOpen(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId()));
                tmpFile = new File(GWConfig.getInstance().getCacheDiskpath() + (KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber())));
            } else {
                srcFile = new File(KsanUtils.makeObjPathForOpen(srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId()));
                tmpFile = new File(KsanUtils.makePartPath(path, objMeta.getObjId(), s3Parameter.getUploadId(), s3Parameter.getPartNumber()));
            }

            bw = new BufferedWriter(new FileWriter(tmpFile));

            if (isSrcMultipart) {
                BufferedReader srcBR = new BufferedReader(new FileReader(srcFile));
                String line = null;
                long accOffset = 0L;
                long partLength = 0L;
                Long objOffset = 0L;
                Long objLast = 0L;
                long newOffset = 0L;
                long newLast = 0L;
                boolean isPartRange = false;
                StringBuilder sb = new StringBuilder();

                while ((line = srcBR.readLine()) != null) {
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
                            if (isPartRange) {
                                sb.append(objLast); // this part Range
                            } else {
                                sb.append(objSize - 1);
                            }
                            sb.append(System.lineSeparator());
                            bw.write(sb.toString());
                        } else {
                            line += System.lineSeparator();
                            bw.write(line);
                        }
                    } else if ((accOffset + partLength - 1) == last) {
                        line += System.lineSeparator();
                        bw.write(line);
                        break;
                    } else {
                        newLast = last - accOffset;
                        if (newLast - objOffset + 1 == objSize) {
                            line += System.lineSeparator();
                            bw.write(line);
                        } else {
                            sb.setLength(0);
                        
                            sb.append(objDiskId);
                            sb.append(GWConstants.COLON);
                            sb.append(objPath);
                            sb.append(GWConstants.COLON);
                            sb.append(objSize);
                            sb.append(GWConstants.COLON);
                            sb.append(objOffset);   // this part Range
                            sb.append(GWConstants.DASH);
                            sb.append(newLast);
                            sb.append(System.lineSeparator());
                            bw.write(sb.toString());
                        }
                        break;
                    }
                    accOffset += partLength;
                }
                srcBR.close();
                s3Object.setEtag(GWConstants.DIRECTORY_MD5);
            } else {
                // copy range and make part
                FileInputStream is = new FileInputStream(srcFile);
			    FileOutputStream os = new FileOutputStream(tmpFile);
                CtrCryptoInputStream enfin = null;
                MessageDigest md5er = null;
                byte[] byteArray = new byte[GWConstants.MAXBUFSIZE];
			    int readLength = 0;
                int readByte = GWConstants.BUFSIZE;

                try {
                    if (actualSize < 100 * GWConstants.MEGABYTES) {
                        md5er = MessageDigest.getInstance(GWConstants.MD5);
                    } else {
                        md5er = MessageDigest.getInstance(GWConstants.MD5, new OpenSSL4JProvider());
                    }
                } catch (NoSuchAlgorithmException e) {
                    PrintStack.logging(logger, e);
                    is.close();
                    os.close();
                    throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                }

                // check range
                if (last == 0) {
                    // whole file
                    if (srcEncryption.isEnabledEncryption()) {
                        // enfin = GWUtils.initCtrEncrypt(is, s3Encryption.getCustomerKey());
                        // while ((readLength = enfin.read(byteArray, 0, GWConstants.MAXBUFSIZE)) != -1) {
                        //     os.write(byteArray, 0, readLength);
                        //     md5er.update(byteArray, 0, readLength);
                        // }
                        // enfin.close();
                        // os.flush();
                        // os.close();
                    } else {
                        while ((readLength = is.read(byteArray, 0, GWConstants.MAXBUFSIZE))!= -1) {
                            os.write(byteArray, 0, readLength);
                            md5er.update(byteArray, 0, readLength);
                        }
                        is.close();
                        os.flush();
                        os.close();
                    }
                } else {
                    if (srcEncryption.isEnabledEncryption()) {
                        // enfin = GWUtils.initCtrEncrypt(is, s3Encryption.getCustomerKey());
                        // enfin.skip(offset);
                        // long remainLength = actualSize;
                        // while (remainLength > 0) {
                        //     if (remainLength > GWConstants.BUFSIZE) {
                        //         readByte = GWConstants.BUFSIZE;
                        //     } else {
                        //         readByte = (int)remainLength;
                        //     }

                        //     if ((readLength = enfin.read(byteArray, 0, readByte)) == -1) {
                        //         os.write(byteArray, 0, readLength);
                        //         md5er.update(byteArray, 0, readLength);
                        //     } else {
                        //         break;
                        //     }

                        //     remainLength -= readLength;
                        // }
                        // enfin.close();
                        // os.flush();
                        // os.close();
                    } else {
                        is.skip(offset);
                        long remainLength = actualSize;
                        while (remainLength > 0) {
                            if (remainLength > GWConstants.BUFSIZE) {
                                readByte = GWConstants.BUFSIZE;
                            } else {
                                readByte = (int)remainLength;
                            }

                            if ((readLength = is.read(byteArray, 0, readByte)) == -1) {
                                os.write(byteArray, 0, readLength);
                                md5er.update(byteArray, 0, readLength);
                            } else {
                                break;
                            }

                            remainLength -= readLength;
                        }
                        is.close();
                        os.flush();
                        os.close();
                    }
                }

                byte[] digest = md5er.digest();
                String eTag = base16().lowerCase().encode(digest);
                
                s3Object.setEtag(eTag);
            }
            bw.close();
        } catch(FileNotFoundException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        } catch(IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        s3Object.setLastModified(new Date());
        s3Object.setFileSize(actualSize);
        s3Object.setDeleteMarker(GWConstants.OBJECT_TYPE_FILE);

        return s3Object;
    }

    public S3Object copyObject(Metadata srcObjMeta, S3Encryption srcEncryption) throws GWException {
        S3Object s3Object = new S3Object();
        long fileSize = srcObjMeta.getSize();
        String key = Strings.isNullOrEmpty(srcEncryption.getCustomerKey()) ? GWConstants.EMPTY_STRING : srcEncryption.getCustomerKey();
        long actualSize = 0L;
        OSDClient client = null;

        try {            
            logger.info(GWConstants.LOG_S3OBJECT_OPERATION_LOCAL_IP, GWUtils.getLocalIP());
            logger.info(GWConstants.LOG_S3OBJECT_OPERATION_OBJ_PRIMARY_IP, objMeta.getPrimaryDisk().getOsdIp());

            if (srcObjMeta.getReplicaCount() > 1 && objMeta.getReplicaDisk() != null) {
                logger.info(GWConstants.LOG_S3OBJECT_OPERATION_OBJ_REPLICA_IP, objMeta.getReplicaDisk().getOsdIp());
            }

            // tmp src object - get src object data
            File tmpFile = new File(KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId()));
            com.google.common.io.Files.createParentDirs(tmpFile);
            try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
                if (srcObjMeta.getReplicaCount() > 1) {
                    if (GWUtils.getLocalIP().equals(srcObjMeta.getPrimaryDisk().getOsdIp())) {
                        actualSize = getObjectLocal(fos, srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), GWConstants.EMPTY_STRING, key);
                    } else if (GWUtils.getLocalIP().equals(srcObjMeta.getReplicaDisk().getOsdIp())) {
                        actualSize = getObjectLocal(fos, srcObjMeta.getReplicaDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), GWConstants.EMPTY_STRING, key);
                    } else {
                        try {
                            client = new OSDClient(srcObjMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                            // client = OSDClientManager.getInstance().getOSDClient(srcObjMeta.getPrimaryDisk().getOsdIp());
                            client.getInit(srcObjMeta.getPrimaryDisk().getPath(), 
                                           srcObjMeta.getObjId(), 
                                           srcObjMeta.getVersionId(), 
                                           fileSize, 
                                           GWConstants.EMPTY_STRING, 
                                           fos,
                                           key);
                            actualSize = client.get();
                            // OSDClientManager.getInstance().returnOSDClient(client);
                        } catch (Exception e) {
                            client = new OSDClient(srcObjMeta.getReplicaDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                            // client = OSDClientManager.getInstance().getOSDClient(srcObjMeta.getReplicaDisk().getOsdIp());
                            client.getInit(srcObjMeta.getReplicaDisk().getPath(), 
                                           srcObjMeta.getObjId(), 
                                           srcObjMeta.getVersionId(), 
                                           fileSize, 
                                           GWConstants.EMPTY_STRING, 
                                           fos, 
                                           key);
                            actualSize = client.get();
                            // OSDClientManager.getInstance().returnOSDClient(client);
                        }
                    }
                } else {
                    if (GWUtils.getLocalIP().equals(srcObjMeta.getPrimaryDisk().getOsdIp())) {
                        actualSize = getObjectLocal(fos, srcObjMeta.getPrimaryDisk().getPath(), srcObjMeta.getObjId(), srcObjMeta.getVersionId(), GWConstants.EMPTY_STRING, key);
                    } else {
                        try {
                            client = new OSDClient(srcObjMeta.getPrimaryDisk().getOsdIp(), (int)GWConfig.getInstance().getOsdPort());
                            // client = OSDClientManager.getInstance().getOSDClient(srcObjMeta.getPrimaryDisk().getOsdIp());
                            client.getInit(srcObjMeta.getPrimaryDisk().getPath(), 
                                           srcObjMeta.getObjId(), 
                                           srcObjMeta.getVersionId(), 
                                           fileSize, 
                                           GWConstants.EMPTY_STRING, 
                                           fos,
                                           key);
                            actualSize = client.get();
                            // OSDClientManager.getInstance().returnOSDClient(client);
                        } catch (Exception e) {
                            PrintStack.logging(logger, e);
                            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
                        }
                    }
                }
            } catch (Exception e) {
                PrintStack.logging(logger, e);
                throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
            }

            tmpFile = new File(KsanUtils.makeTempPath(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), objMeta.getVersionId()));
            s3Meta.setContentLength(tmpFile.length());
            logger.info("tmpfile path : {}", tmpFile.getAbsolutePath());
            
            try (FileInputStream fis = new FileInputStream(tmpFile);) {
                if (s3Encryption.isEnabledEncryption()) {
                    s3Object = putObjectEncryption(s3Meta.getContentLength(), fis);
                } else {
                    s3Object = putObjectNormal(s3Meta.getContentLength(), fis);
                }
            } catch (Exception e) {
                PrintStack.logging(logger, e);
            }
            tmpFile.delete();
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        return s3Object;
    }

    public void restoreObject(Metadata restoreObjMeta) throws GWException {
        try {
            File srcFile = new File(KsanUtils.makeObjPathForOpen(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
            File destFile = new File(KsanUtils.makeObjPath(restoreObjMeta.getPrimaryDisk().getPath(), restoreObjMeta.getObjId(), versionId));

            if (destFile.exists()) {
                destFile.delete();
            }

            retryRenameTo(srcFile, destFile);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }
    }

    public void storageMove(Metadata targetObjMeta) throws GWException {
        try {
            File srcFile = new File(KsanUtils.makeObjPathForOpen(objMeta.getPrimaryDisk().getPath(), objMeta.getObjId(), versionId));
            File destFile = new File(KsanUtils.makeObjPath(targetObjMeta.getPrimaryDisk().getPath(), targetObjMeta.getObjId(), versionId));

            if (destFile.exists()) {
                destFile.delete();
            }

            retryRenameTo(srcFile, destFile);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
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
                    e.printStackTrace();
                }
            }
            logger.error(GWConstants.LOG_S3OBJECT_OPERATION_FAILED_FILE_RENAME, srcFile.getAbsolutePath(), destFile.getAbsolutePath());
        }
    }

}
