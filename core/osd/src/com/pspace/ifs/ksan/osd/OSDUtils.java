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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.osd.DISKPOOLLIST.DISKPOOL.SERVER;
import com.pspace.ifs.ksan.osd.DISKPOOLLIST.DISKPOOL.SERVER.DISK;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSDUtils {
    private final static Logger logger = LoggerFactory.getLogger(OSDUtils.class);
    private OSDConfig config;
    private String localIP = null;

    public static OSDUtils getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final OSDUtils INSTANCE = new OSDUtils();
    }

    private OSDUtils() {
        config = new OSDConfig(OSDConstants.CONFIG_PATH);
        try {
            config.configure();
        } catch (URISyntaxException e) {
            logger.error(e.getMessage());
            System.exit(-1);
        }
    }

    public int getPoolSize() {
        return Integer.parseInt(config.getPoolSize());
    }

    public int getPort() {
        return Integer.parseInt(config.getPort());
    }

    public long getECFileSize() {
        return Long.parseLong(config.getECFileSize());
    }

    public int getECScheduleMinutes() {
        return Integer.parseInt(config.getECScheduleMinutes());
    }

    public int getECApplyMinutes() {
        return Integer.parseInt(config.getECApplyMinutes());
    }

    public String getCacheDisk() {
        return config.getCacheDisk();
    }

    public int getCacheScheduleMinutes() {
        return Integer.parseInt(config.getCacheScheduleMinutes());
    }
    public long getCacheFileSize() {
        return Long.parseLong(config.getCacheFileSize());
    }

    public int getCacheLimitMinutes() {
        return Integer.parseInt(config.getCacheLimitMinutes());
    }

    public int getTrashScheduleMinutes() {
        return Integer.parseInt(config.getTrashScheduleMinutes());
    }

    public void writePID() {
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
                logger.error(e.getMessage());
            }

            logger.debug(OSDConstants.LOG_OSD_SERVER_PID, pid);
            fw.write(String.valueOf(pid));
            fw.flush();
            fw.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            System.exit(-1);
        }
    }

    public String getOSDIP(DISKPOOLLIST diskPoolList, String diskID) {
        for (SERVER server : diskPoolList.getDiskpool().getServers()) {
            for (DISK disk : server.getDisks()) {
                if (disk.getId().equals(diskID)) {
                    return server.getIp();
                }
            }
        }

        return null;
    }

    public String getPath(DISKPOOLLIST diskPoolList, String diskID) {
        for (SERVER server : diskPoolList.getDiskpool().getServers()) {
            for (DISK disk : server.getDisks()) {
                if (disk.getId().equals(diskID)) {
                    return disk.getPath();
                }
            }
        }

        return null;
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

    public String makePath(String path, String fileName) {
        String fullPath = path + OSDConstants.SLASH + OSDConstants.OBJ_DIR + makeDirectoryName(fileName) + OSDConstants.SLASH + fileName;
        return fullPath;
    }

    public String makeObjPath(String path, String objId, String versionId) {
        String fullPath = path + OSDConstants.SLASH + OSDConstants.OBJ_DIR + makeDirectoryName(objId) + OSDConstants.SLASH + objId + OSDConstants.UNDERSCORE + versionId;
        return fullPath;
    }

    public String makeTempPath(String path, String objId, String versionId) {
        String fullPath = path + OSDConstants.SLASH + OSDConstants.TEMP_DIR + OSDConstants.SLASH + objId + OSDConstants.UNDERSCORE + versionId;
        return fullPath;
    }

    public String makeTrashPath(String path, String objId, String versionId) {
        String uuid = UUID.randomUUID().toString();
        String fullPath = path + OSDConstants.SLASH + OSDConstants.TRASH_DIR + OSDConstants.SLASH + objId + OSDConstants.UNDERSCORE + versionId + uuid;
        return fullPath;
    }

    public String makeCachePath(String path) {
        String fullPath = getCacheDisk() + path;
        return fullPath;
    }

    public String makeECPath(String path, String objId, String versionId) {
        String fullPath = path + OSDConstants.SLASH + OSDConstants.EC_DIR + makeDirectoryName(objId) + OSDConstants.SLASH + OSDConstants.POINT + objId + OSDConstants.UNDERSCORE + versionId;
        return fullPath;
    }

    public String makeECDirectory(String fileName, String ecPath) {
        String fullPath = ecPath + makeDirectoryName(fileName);
        return fullPath;
    }

    public String makeECTempPath(String fileName, String ecPath) {
        String fullPath = ecPath + makeDirectoryName(fileName) + OSDConstants.SLASH + OSDConstants.POINT + fileName;
        return fullPath;
    }

    public DISKPOOLLIST getDiskPoolList() {
        DISKPOOLLIST diskpoolList = null;
        try {
            // logger.debug(OSDConstants.LOG_OSD_SERVER_CONFIGURE_DISPOOLS);
			XmlMapper xmlMapper = new XmlMapper();
			InputStream is = new FileInputStream(OSDConstants.DISKPOOL_CONF_PATH);
			byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
			try {
				is.read(buffer, 0, OSDConstants.MAXBUFSIZE);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
			String xml = new String(buffer);
			
			// logger.debug(xml);
			diskpoolList = xmlMapper.readValue(xml, DISKPOOLLIST.class);
			// logger.debug(OSDConstants.LOG_OSD_SERVER_DISK_POOL_INFO, diskpoolList.getDiskpool().getId(), diskpoolList.getDiskpool().getName());
			// logger.debug(OSDConstants.LOG_OSD_SERVER_SERVER_SIZE, diskpoolList.getDiskpool().getServers().size());
		} catch (JsonProcessingException | FileNotFoundException e) {
			logger.error(e.getMessage());
		}

        return diskpoolList;
    }

    public void setAttributeFileReplication(File file, String value, String replicaDiskID) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        try {
            view.write(OSDConstants.FILE_ATTRIBUTE_REPLICATION, Charset.defaultCharset().encode(value));
            view.write(OSDConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID, Charset.defaultCharset().encode(replicaDiskID));
        } catch (IOException e) {
            logger.error(e.getMessage());
            for ( StackTraceElement k : e.getStackTrace() ) {
                logger.error(k.toString());
            }
        }
    }

    public String getAttributeFileReplication(File file) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(Paths.get(file.getPath()), UserDefinedFileAttributeView.class);
        ByteBuffer buf = null;
        try {
            buf = ByteBuffer.allocate(view.size(OSDConstants.FILE_ATTRIBUTE_REPLICATION));
            view.read(OSDConstants.FILE_ATTRIBUTE_REPLICATION, buf);
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
            buf = ByteBuffer.allocate(view.size(OSDConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID));
            view.read(OSDConstants.FILE_ATTRIBUTE_REPLICA_DISK_ID, buf);
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

    public String getLocalIP() {
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
}
