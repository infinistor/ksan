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
        }
    }

    public static void setAttributeFileReplication(File file, String replica, String diskID) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        try {
            view.write(Constants.FILE_ATTRIBUTE_REPLICATION, Charset.defaultCharset().encode(replica));
            view.write(Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID, Charset.defaultCharset().encode(diskID));
        } catch (IOException e) {
            PrintStack.logging(logger, e);
        }
    }

    public static String getAttributeFileReplication(File file) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        ByteBuffer buf = null;
        try {
            buf = ByteBuffer.allocateDirect(view.size(Constants.FILE_ATTRIBUTE_REPLICATION));
            view.read(Constants.FILE_ATTRIBUTE_REPLICATION, buf);
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

    public static String getAttributeFileReplicaDiskID(File file) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        ByteBuffer buf = null;
        try {
            buf = ByteBuffer.allocateDirect(view.size(Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID));
            view.read(Constants.FILE_ATTRIBUTE_REPLICA_DISK_ID, buf);
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
