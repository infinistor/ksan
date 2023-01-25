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
package com.pspace.ifs.ksan.libs;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.pspace.ifs.ksan.libs.data.ECPart;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;

import com.google.common.base.Strings;

import com.google.common.primitives.Longs;
import org.apache.commons.io.FileUtils;
import org.apache.commons.crypto.stream.CtrCryptoInputStream;
import org.apache.commons.crypto.stream.CtrCryptoOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KsanUtils {
    private static final Logger logger = LoggerFactory.getLogger(KsanUtils.class);
    private static String localIP = null;
    private static int RETRY_COUNT = 3;
    private static final String LOG_OSD_SERVER_FAILED_FILE_RENAME = "failed file rename {} -> {}";

    private KsanUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    public static void writePID(String path) {
        File file = new File(path);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
			Long pid = ProcessHandle.current().pid();

            fw.write(String.valueOf(pid));
            fw.flush();
            fw.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            System.exit(-1);
        }
    }

    public static String getLocalIP() {
		if (!Strings.isNullOrEmpty(localIP)) {
			return localIP;
		} else {
			InetAddress local = null;
			try {
				local = InetAddress.getLocalHost();
				localIP = local.getHostAddress();
			} catch (UnknownHostException e) {
				logger.error(e.getMessage());
			}
			return localIP;
		}
	}

    public static void retryRenameTo(File srcFile, File destFile) throws IOException {
        if (srcFile.exists()) {
            for (int i = 0; i < RETRY_COUNT; i++) {
                if (srcFile.renameTo(destFile)) {
                    return;
                }
            }
            logger.error(LOG_OSD_SERVER_FAILED_FILE_RENAME, srcFile.getAbsolutePath(), destFile.getAbsolutePath());
        } else {
            logger.info("{}, File does not exist. ", srcFile.getAbsolutePath());
        }
    }

    private static String makeDirectorySub(String objId) {
        byte[] path = new byte[3];
        byte[] byteObjId = objId.getBytes();
        path[0] = Constants.CHAR_SLASH;
        path[1] = byteObjId[0];
        path[2] = byteObjId[1];

        return new String(path);
    }

    private static String makeDirectoryName(String objId) {
        byte[] path = new byte[6];
        byte[] byteObjId = objId.getBytes();

        path[0] = Constants.CHAR_SLASH;
        path[1] = byteObjId[0];
        path[2] = byteObjId[1];
        path[3] = Constants.CHAR_SLASH;
        path[4] = byteObjId[2];
        path[5] = byteObjId[3];

        return new String(path);
    }

    public static String makePath(String path, String fileName) {
        String fullPath = path + Constants.SLASH + Constants.OBJ_DIR + makeDirectoryName(fileName) + Constants.SLASH + fileName;
        return fullPath;
    }

    public static String makeObjPath(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.OBJ_DIR);
        sb.append(makeDirectorySub(objId.substring(0, 2)));

        File objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }
        sb.append(makeDirectorySub(objId.substring(2, 4)));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }

        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
    }

    public static String makeObjPathForOpen(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.OBJ_DIR);
        sb.append(makeDirectoryName(objId.substring(0, 4)));
        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
    }

    public static String makeTempPath(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        String uuid = UUID.randomUUID().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.TEMP_DIR);
        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(uuid);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
    }

    public static String makeTempPartPath(String path, String objId, String partNo) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.TEMP_DIR);
        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(partNo);

        return sb.toString();
    }

    public static String makeTrashPath(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        String uuid = UUID.randomUUID().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.TRASH_DIR);
        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(uuid);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
    }

    public static String makeECPath(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.EC_DIR);
        sb.append(makeDirectorySub(objId.substring(0, 2)));

        File objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }
        sb.append(makeDirectorySub(objId.substring(2, 4)));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }

        sb.append(Constants.SLASH);
        sb.append(Constants.POINT);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
    }

    public static String makeECPathForOpen(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.EC_DIR);
        sb.append(makeDirectoryName(objId.substring(0, 4)));
        sb.append(Constants.SLASH);
        sb.append(Constants.POINT);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
    }

    public static String makeTempCompleteMultipartPath(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.TEMP_DIR);
        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
    }

    public static String makeTempCopyPath(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.TEMP_DIR);
        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
    }

    public static String makeECDirectoryPath(String path, String objId) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.EC_DIR);
        sb.append(makeDirectoryName(objId.substring(0, 4)));

        return sb.toString();
    }

    public static String makeECDirectory(String fileName, String ecPath) {
        StringBuilder sb = new StringBuilder();
        sb.append(ecPath);
        sb.append(makeDirectorySub(fileName.substring(0, 2)));

        File objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }

        sb.append(makeDirectorySub(fileName.substring(2, 4)));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }

        return sb.toString();
    }

    public static void setAttributeFileReplication(File file, String replica, String diskID) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        try {
            view.write(Constants.FILE_ATTRIBUTE_REPLICATION, Charset.defaultCharset().encode(replica));
            view.write(Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID, Charset.defaultCharset().encode(diskID));
            logger.debug("Set attribute file replication {}, {}", replica, diskID);
        } catch (IOException e) {
            PrintStack.logging(logger, e);
        }
    }

    public static String getAttributeFileReplication(File file) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        ByteBuffer buf = null;
        try {
            buf = ByteBuffer.allocateDirect(Constants.FILE_ATTRIBUTE_REPLICATION_SIZE);
            view.read(Constants.FILE_ATTRIBUTE_REPLICATION, buf);
            buf.flip();
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (IllegalArgumentException iae) {
            logger.error(iae.getMessage());
        } catch (SecurityException e) {
            logger.error(e.getMessage());
        }
        String replica = Charset.defaultCharset().decode(buf).toString();
        logger.debug("get replica : {}", replica);
        return replica;
    }

    public static String getAttributeFileReplicaDiskID(File file) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        ByteBuffer buf = null;
        try {
            buf = ByteBuffer.allocateDirect(Constants.FILE_ATTRIBUTE_REPLICATION_DISK_ID_SIZE);
            view.read(Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID, buf);
            buf.flip();
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (IllegalArgumentException iae) {
            logger.error(iae.getMessage());
        } catch (SecurityException e) {
            logger.error(e.getMessage());
        }

        String diskID = Charset.defaultCharset().decode(buf).toString();
        logger.debug("file replicaDiskID : {}", diskID);
        return diskID;
    }

    public static boolean isECObject(String path, String objId, String versionId) {
        // check EC exists
        File ecFile = new File(KsanUtils.makeECPathForOpen(path, objId, versionId));
        if (ecFile.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static long getECObject(String diskId, String path, String objId, String versionId, OutputStream os, String sourceRange, String key, int osdPort) {
        long actualSize = 0L;
        File ecFile = new File(KsanUtils.makeECPathForOpen(path, objId, versionId));
        if (!ecFile.exists()) {
            return actualSize;
        }

        if (ecFile.exists()) {
            List<ECPart> ecList = new ArrayList<ECPart>();
            for (DiskPool pool : DiskManager.getInstance().getDiskPoolList()) {
                for (Server server : pool.getServerList()) {
                    for (Disk disk : server.getDiskList()) {
                        ECPart ecPart = new ECPart(server.getIp(), disk.getPath(), false);
                        ecList.add(ecPart);
                    }
                }
            }
            int numberOfCodingChunks = DiskManager.getInstance().getECM(diskId);
            int numberOfDataChunks = DiskManager.getInstance().getECK(diskId);

            int getECPartCount = 0;
            StringBuilder sb = new StringBuilder();
            for (ECPart ecPart : ecList) {
                sb.setLength(0);
                sb.append(ecFile.getAbsolutePath());
                sb.append(Constants.POINT);
                sb.append(Integer.toString(getECPartCount));
                String newECPartPath = sb.toString();
                File newECPartFile = new File(newECPartPath);

                if (ecPart.getServerIP().equals(getLocalIP())) {
                    // if local disk, move file
                    File sourceECPartFile = new File(KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objId, versionId));
                    if (sourceECPartFile.exists()) {
                        try {
                            FileUtils.copyFile(sourceECPartFile, newECPartFile);
                        } catch (IOException e) {
                            PrintStack.logging(logger, e);
                        }
                        ecPart.setProcessed(true);
                    }
                } else {
                    try (FileOutputStream fos = new FileOutputStream(newECPartFile)) {
                        String getPath = KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objId, versionId);
                        OSDClient ecClient = new OSDClient(ecPart.getServerIP(), osdPort);
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
            String ecAllFilePath = KsanUtils.makeECPathForOpen(path, objId, versionId);
            String command = Constants.ZUNFEC + ecAllFilePath;
            getECPartCount = 0;
            for (ECPart ecPart : ecList) {
                sb.setLength(0);
                sb.append(ecFile.getAbsolutePath());
                sb.append(Constants.POINT);
                sb.append(Integer.toString(getECPartCount));
                String ecPartPath = sb.toString();
                if (ecPart.isProcessed()) {
                    command += Constants.SPACE + ecPartPath;
                    getECPartCount++;
                }
            }

            try {
                Process p = Runtime.getRuntime().exec(command);
                int exitCode = p.waitFor();
                p.destroy();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                return -1;
            } catch (IOException e) {
                logger.error(e.getMessage());
                return -1;
            }

            // delete junk file
            String ecDir = KsanUtils.makeECDirectoryPath(path, objId);
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
                byte[] buffer = new byte[Constants.MAXBUFSIZE];
                CtrCryptoInputStream encryptIS = null;

                try (FileInputStream fis = new FileInputStream(ecAllFile)) {
                    long remaingLength = 0L;
                    int readLength = 0;
                    int readBytes;

                    if (Strings.isNullOrEmpty(sourceRange)) {
                        if (!Strings.isNullOrEmpty(key)) {
                            encryptIS = initCtrDecrypt(fis, key);
                            while ((readLength = encryptIS.read(buffer, 0, Constants.MAXBUFSIZE)) != -1) {
                                actualSize += readLength;
                                os.write(buffer, 0, readLength);
                                logger.debug("read length : {}", readLength);
                            }
                        } else {
                            remaingLength = ecAllFile.length();
                            while (remaingLength > 0) {
                                readBytes = 0;
                                if (remaingLength < Constants.MAXBUFSIZE) {
                                    readBytes = (int)remaingLength;
                                } else {
                                    readBytes = Constants.MAXBUFSIZE;
                                }
            
                                if (remaingLength >= Constants.MAXBUFSIZE) {
                                        readLength = Constants.MAXBUFSIZE;
                                } else {
                                    readLength = (int)remaingLength;
                                }
                                readLength = fis.read(buffer, 0, readBytes);
                                actualSize += readLength;
                                os.write(buffer, 0, readLength);
                                remaingLength -= readLength;
                            }
                        }
                    } else {
                        String[] ranges = sourceRange.split(Constants.SLASH);
                        for (String range : ranges) {
                            String[] rangeParts = range.split(Constants.COMMA);
                            long offset = Longs.tryParse(rangeParts[0]);
                            long length = Longs.tryParse(rangeParts[1]);

                            remaingLength = length;
                            
                            if (!Strings.isNullOrEmpty(key)) {
                                long skipOffset = 0;
                                encryptIS = initCtrDecrypt(fis, key);

                                if (offset > 0) {
                                    long skip = encryptIS.skip(offset);
                                    logger.debug("skip : {}", skip);
                                }
                                while (remaingLength > 0) {
                                    readBytes = 0;
                                    if (remaingLength < Constants.MAXBUFSIZE) {
                                        readBytes = (int)remaingLength;
                                    } else {
                                        readBytes = Constants.MAXBUFSIZE;
                                    }

                                    if ((readLength = encryptIS.read(buffer, 0, readBytes)) != -1) {
                                        skipOffset += readLength;
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
                                    if (remaingLength < Constants.MAXBUFSIZE) {
                                        readBytes = (int)remaingLength;
                                    } else {
                                        readBytes = Constants.MAXBUFSIZE;
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
                    return -1;
                }

                if (encryptIS != null) {
                    try {
                        encryptIS.close();
                    } catch (IOException e) {
                        PrintStack.logging(logger, e);
                        return -1;
                    }
                }
            }
        }

        return actualSize;
    }

    public static CtrCryptoOutputStream initCtrEncrypt(FileOutputStream out, String customerKey) throws IOException {
		byte[] iv = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };

		byte[] key = new byte[32];
		logger.info(customerKey);
		for (int i = 0; i < 32; i++) {
			if (i < customerKey.getBytes().length)
				key[i] = customerKey.getBytes()[i];
			else
				key[i] = 0;
		}

		Properties property = new Properties();
		property.setProperty(Constants.PROPERTY_COMMONS_CRYPTO_STREAM_BUFFER_SIZE, Long.toString(Constants.COMMONS_CRYPTO_STREAM_BUFFER_SIZE));
		CtrCryptoOutputStream cipherOut = new CtrCryptoOutputStream(property, out, key, iv);

		return cipherOut;
	}
	
	public static CtrCryptoInputStream initCtrDecrypt(FileInputStream in, String customerKey) throws IOException {
		byte[] iv = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };

		byte[] key = new byte[32];
		logger.info("init ctr decrypt key : {}", customerKey);
		for (int i = 0; i < 32; i++) {
			if (i < customerKey.getBytes().length)
				key[i] = customerKey.getBytes()[i];
			else
				key[i] = 0;
		}

		Properties property = new Properties();
		property.setProperty(Constants.PROPERTY_COMMONS_CRYPTO_STREAM_BUFFER_SIZE, Long.toString(Constants.COMMONS_CRYPTO_STREAM_BUFFER_SIZE));
		CtrCryptoInputStream cipherIn = new CtrCryptoInputStream(property, in, key, iv);

		return cipherIn;
	}

    public static Date getDate(String strDate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
        Date date = null;
        try {
            date = simpleDateFormat.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }
}
