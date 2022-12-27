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

import com.google.common.base.Strings;

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

    private static String makeDirectoryFirstName(String objId) {
        byte[] path = new byte[3];
        byte[] byteObjId = objId.getBytes();
        path[0] = Constants.CHAR_SLASH;
        path[1] = byteObjId[0];
        path[2] = byteObjId[1];

        return new String(path);
    }

    private static String makeDirectorySecondName(String objId) {
        byte[] path = new byte[3];
        byte[] byteObjId = objId.getBytes();
        path[0] = Constants.CHAR_SLASH;
        path[1] = byteObjId[2];
        path[2] = byteObjId[3];

        return new String(path);
    }

    private static String makeDirectoryName(String objId) {
        byte[] path = new byte[6];
        byte[] byteObjId = objId.getBytes();

        path[0] = Constants.CHAR_SLASH;
        int index = 1;
        
        path[index++] = byteObjId[0];
        path[index++] = byteObjId[1];
        path[index++] = Constants.CHAR_SLASH;
        path[index++] = byteObjId[2];
        path[index] = byteObjId[3];

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
        sb.append(makeDirectoryFirstName(objId));

        File objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }
        sb.append(makeDirectorySecondName(objId));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }

        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
        // String fullPath = path + Constants.SLASH + Constants.OBJ_DIR + makeDirectoryName(objId) + Constants.SLASH + objId + Constants.UNDERSCORE + versionId;
        // return fullPath;
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
        // String fullPath = path + Constants.SLASH + Constants.TEMP_DIR + Constants.SLASH + objId + Constants.UNDERSCORE + uuid + Constants.UNDERSCORE + versionId;
        // return fullPath;
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
        // String fullPath = path + Constants.SLASH + Constants.TRASH_DIR + Constants.SLASH + objId + Constants.UNDERSCORE + versionId + Constants.DASH + uuid;
        // return fullPath;
    }

    public static String makeECPath(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.EC_DIR);
        sb.append(makeDirectoryFirstName(objId));

        File objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }
        sb.append(makeDirectorySecondName(objId));
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
        // String fullPath = path + Constants.SLASH + Constants.EC_DIR + makeDirectoryName(objId) + Constants.SLASH + Constants.POINT + objId + Constants.UNDERSCORE + versionId;
        // return fullPath;
    }

    public static String makeTempPartPath(String path, String objId, String partNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.TEMP_DIR);
        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(partNumber);

        return sb.toString();
        // String fullPath = path + Constants.SLASH + Constants.TEMP_DIR + Constants.SLASH + objId + Constants.UNDERSCORE + partNumber;
        // return fullPath;
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
        // String fullPath = path + Constants.SLASH + Constants.TEMP_COMPLETE_DIR + Constants.SLASH + objId + Constants.UNDERSCORE + versionId;
        // return fullPath;
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
        // String fullPath = path + Constants.SLASH + Constants.TEMP_COPY_DIR + Constants.SLASH + objId + Constants.UNDERSCORE + versionId;
        // return fullPath;
    }

    public static String makeECDecodePath(String path, String objId, String versionId) {
        if (Strings.isNullOrEmpty(versionId)) {
            versionId = Constants.VERSIONING_DISABLE_TAIL;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.EC_DIR);
        sb.append(makeDirectoryFirstName(objId));

        File objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }
        sb.append(makeDirectorySecondName(objId));
        objDir = new File(sb.toString());
        if (!objDir.exists()) {
            objDir.mkdir();
        }

        sb.append(Constants.SLASH);
        sb.append(objId);
        sb.append(Constants.UNDERSCORE);
        sb.append(versionId);

        return sb.toString();
        // String fullPath = path + Constants.SLASH + Constants.EC_DIR + makeDirectoryName(objId) + Constants.SLASH + objId + Constants.UNDERSCORE + versionId;
        // return fullPath;
    }

    public static String makeECDirectoryPath(String path, String objId) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        sb.append(Constants.SLASH);
        sb.append(Constants.EC_DIR);
        sb.append(makeDirectoryName(objId));

        return sb.toString();
        // String fullPath = path + Constants.SLASH + Constants.EC_DIR + makeDirectoryName(objId);
        // return fullPath;
    }

    public static String makeECDirectory(String fileName, String ecPath) {
        StringBuilder sb = new StringBuilder();
        sb.append(ecPath);
        sb.append(makeDirectoryName(fileName));

        return sb.toString();
        // String fullPath = ecPath + makeDirectoryName(fileName);
        // return fullPath;
    }

    // public static String makeECTempPath(String fileName, String ecPath) {
    //     String fullPath = ecPath + makeDirectoryName(fileName) + Constants.SLASH + Constants.POINT + fileName;
    //     return fullPath;
    // }

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
}
