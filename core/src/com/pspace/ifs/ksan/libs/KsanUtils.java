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
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
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
            boolean created = false;
            if (!file.exists()) {
                created = file.createNewFile();
            }

            if (!created) {
                try(FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
                    Long pid = ProcessHandle.current().pid();
    
                    fw.write(String.valueOf(pid));
                    fw.flush();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    System.exit(-1);
                }
            } else {
                logger.error("failed to create pid file");
                System.exit(-1);
            }
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
        byte[] byteObjId = objId.getBytes(StandardCharsets.UTF_8);
        path[0] = Constants.CHAR_SLASH;
        path[1] = byteObjId[0];
        path[2] = byteObjId[1];

        return new String(path, StandardCharsets.UTF_8);
    }

    private static String makeDirectoryName(String objId) {
        byte[] path = new byte[6];
        byte[] byteObjId = objId.getBytes(StandardCharsets.UTF_8);

        path[0] = Constants.CHAR_SLASH;
        path[1] = byteObjId[0];
        path[2] = byteObjId[1];
        path[3] = Constants.CHAR_SLASH;
        path[4] = byteObjId[2];
        path[5] = byteObjId[3];

        return new String(path, StandardCharsets.UTF_8);
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
            if (!objDir.mkdir()) {
                return null;
            }
        }
        sb.append(makeDirectorySub(objId.substring(2, 4)));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            if (!objDir.mkdir()) {
                return null;
            }
        }

        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
    }

    public static boolean makeMultipartDirectory(String path, String objId, String uploadId) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.OBJ_DIR);
        sb.append(makeDirectorySub(objId.substring(0, 2)));

        File objDir = new File(sb.toString());
        if (!objDir.exists()) {
            if (!objDir.mkdir()) {
                return false;
            }
        }
        sb.append(makeDirectorySub(objId.substring(2, 4)));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            if (!objDir.mkdir()) {
                return false;
            }
        }
        sb.append(Constants.SLASH);
        sb.append(uploadId);
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            if (!objDir.mkdir()) {
                return false;
            }
        }
        return true;
    }

    public static String makePartPath(String path, String objId, String uploadId, String partNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.OBJ_DIR);
        sb.append(makeDirectorySub(objId.substring(0, 2)));

        File objDir = new File(sb.toString());
        if (!objDir.exists()) {
            if (!objDir.mkdir()) {
                return null;
            }
        }
        sb.append(makeDirectorySub(objId.substring(2, 4)));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            if (!objDir.mkdir()) {
                return null;
            }
        }
        sb.append(Constants.SLASH);
        sb.append(uploadId);
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            if (!objDir.mkdir()) {
                return null;
            }
        }

        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(uploadId);
        sb.append(Constants.UNDERSCORE);
        sb.append(partNumber);

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

    public static String makeObjMultipartPathForOpen(String path, String objId, String versionId, String uploadId) {
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
        sb.append(Constants.UNDERSCORE);
        sb.append(uploadId);

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

    public static String makeTrashPath(String path, String name) {
        String uuid = UUID.randomUUID().toString();
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.TRASH_DIR);
        sb.append(Constants.SLASH);
        sb.append(name);
        sb.append(Constants.UNDERSCORE);
        sb.append(uuid);

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
            if (!objDir.mkdir()) {
                return null;
            }
        }
        sb.append(makeDirectorySub(objId.substring(2, 4)));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            if (!objDir.mkdir()) {
                return null;
            }
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
            if (!objDir.mkdir()) {
                return null;
            }
        }

        sb.append(makeDirectorySub(fileName.substring(2, 4)));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            if (!objDir.mkdir()) {
                return null;
            }
        }

        return sb.toString();
    }

    public static void setAttributeFileReplication(File file, String replica, String diskID) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        try {
            view.write(Constants.FILE_ATTRIBUTE_REPLICATION, ByteBuffer.wrap(replica.getBytes(StandardCharsets.UTF_8)));
            view.write(Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID, ByteBuffer.wrap(diskID.getBytes(StandardCharsets.UTF_8)));
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

        String replica;
        if (buf != null) {
            try {
                CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
                replica = decoder.decode(buf).toString();
            } catch (CharacterCodingException e) {
                logger.error(e.getMessage());
                replica = "";
            }
        } else {
            replica = "";
        }
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

        String diskID;
        if (buf != null) {
            try {
                CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
                diskID = decoder.decode(buf).toString();
            } catch (CharacterCodingException e) {
                logger.error(e.getMessage());
                diskID = "";
            }
        } else {
            diskID = "";
        }
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

    public static CtrCryptoOutputStream initCtrEncrypt(FileOutputStream out, String customerKey) throws IOException {
		byte[] iv = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };

		byte[] key = new byte[32];
		logger.info(customerKey);
		for (int i = 0; i < 32; i++) {
			if (i < customerKey.getBytes(StandardCharsets.UTF_8).length)
				key[i] = customerKey.getBytes(StandardCharsets.UTF_8)[i];
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
			if (i < customerKey.getBytes(StandardCharsets.UTF_8).length)
				key[i] = customerKey.getBytes(StandardCharsets.UTF_8)[i];
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
