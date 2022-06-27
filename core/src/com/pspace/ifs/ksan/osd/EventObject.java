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

import static com.google.common.io.BaseEncoding.base16;

import com.pspace.ifs.ksan.osd.utils.OSDConstants;

import java.io.File;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;

import com.pspace.ifs.ksan.libs.config.MonConfig;
import com.pspace.ifs.ksan.osd.utils.OSDConstants;
import com.pspace.ifs.ksan.osd.utils.OSDUtils;
import com.pspace.ifs.ksan.libs.KsanUtils;
import com.pspace.ifs.ksan.osd.utils.OSDConfig;
import com.pspace.ifs.ksan.libs.data.OsdData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.security.MessageDigest;

import org.json.simple.parser.ParseException;
import java.security.NoSuchAlgorithmException;

class MoveObjectCallback implements MQCallback {
	private static final Logger logger = LoggerFactory.getLogger(MoveObjectCallback.class);
	private static final String OBJECT_ID = "ObjId";
	private static final String VERSION_ID = "VersionId";
	private static final String SOURCE_DISK_ID = "SourceDiskId";
	private static final String SOURCE_DISK_PATH = "SourceDiskPath";
	private static final String TARGET_DISK_ID = "TargetDiskId";
	private static final String TARGET_DISK_PATH = "TargetDiskPath";
	private static final String TARGET_OSD_IP = "TargetOsdIp";

	private String objId;
	private String versionId;
	private String sourceDiskId;
	private String sourceDiskPath;
	private String targetDiskId;
	private String targetDiskPath;
	private String targetOsdIp;

	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info("receive move object ...");
		logger.info("BiningKey : {}, body : {}}", routingKey, body);
		
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		try {
            jsonObject = (JSONObject)parser.parse(body);
        } catch (ParseException ex) {
            logger.error("Error parsing JSON body");
			return new MQResponse(MQResponseType.ERROR, "", "Error parsing JSON body", 0);
        }
        
		objId = (String) jsonObject.get(OBJECT_ID);
		versionId = (String) jsonObject.get(VERSION_ID);
		sourceDiskId = (String) jsonObject.get(SOURCE_DISK_ID);
		sourceDiskPath = (String) jsonObject.get(SOURCE_DISK_PATH);
		targetDiskId = (String) jsonObject.get(TARGET_DISK_ID);
		targetDiskPath = (String) jsonObject.get(TARGET_DISK_PATH);
		targetOsdIp = (String) jsonObject.get(TARGET_OSD_IP);

		byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
		File srcFile = new File(OSDUtils.getInstance().makeObjPath(sourceDiskPath, objId, versionId));
		try (FileInputStream fis = new FileInputStream(srcFile)) {
			if (KsanUtils.getLocalIP().equals(targetOsdIp)) {
				File file = new File(OSDUtils.getInstance().makeObjPath(targetDiskPath, objId, versionId));
				File tmpFile = new File(OSDUtils.getInstance().makeTempPath(targetDiskPath, objId, versionId));
				File trashFile = new File(OSDUtils.getInstance().makeTrashPath(targetDiskPath, objId, versionId));
	
				com.google.common.io.Files.createParentDirs(file);
				com.google.common.io.Files.createParentDirs(tmpFile);
				try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
					int readLength = 0;
					while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
						fos.write(buffer, 0, readLength);
					}
					fos.flush();
				}
				if (file.exists()) {
					File temp = new File(file.getAbsolutePath());
					KsanUtils.retryRenameTo(temp, trashFile);
				}
				KsanUtils.retryRenameTo(tmpFile, file);
			} else {
				try (Socket destSocket = new Socket(targetOsdIp, OSDConfig.getInstance().getPort())) {
					String header = OsdData.PUT 
						+ OsdData.DELIMITER + targetDiskPath 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId 
						+ OsdData.DELIMITER + String.valueOf(srcFile.length()) 
						+ OsdData.DELIMITER + "primary" 
						+ OsdData.DELIMITER + ""
						+ OsdData.DELIMITER + ""
						+ OsdData.DELIMITER + "";

					logger.debug(OSDConstants.LOG_OSD_SERVER_COPY_RELAY_OSD, targetOsdIp, header);
					OSDUtils.sendHeader(destSocket, header);
					MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5);

					int readLength = 0;
					while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
						destSocket.getOutputStream().write(buffer, 0, readLength);
						md5er.update(buffer, 0, readLength);
					}
					destSocket.getOutputStream().flush();

					byte[] digest = md5er.digest();
					String eTag = base16().lowerCase().encode(digest);

					OsdData data = OSDUtils.receiveData(destSocket);
					if (!eTag.equals(data.getETag())) {
						logger.error(OSDConstants.LOG_OSD_SERVER_DIFFERENCE_ETAG, eTag, data.getETag());
					}
				}
			}
			srcFile.delete();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new MQResponse(MQResponseType.ERROR, "", e.getMessage(), 0);
		}

		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
	}
}

class DeleteObjectCallback implements MQCallback {
	private static final Logger logger = LoggerFactory.getLogger(DeleteObjectCallback.class);
	
	private static final String OBJECT_ID = "ObjId";
	private static final String VERSION_ID = "VersionId";
	private static final String DISK_ID = "DiskId";
	private static final String DISK_PATH = "DiskPath";

	private String objId;
	private String versionId;
	private String diskId;
	private String diskPath;

	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info("receive delete object ...");
		logger.info("BiningKey : {}, body : {}}", routingKey, body);

		JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
		
		try {
			jsonObject = (JSONObject)parser.parse(body);
		} catch (ParseException e) {
			logger.error("Error parsing JSON body");
			return new MQResponse(MQResponseType.ERROR, "", "Error parsing JSON body", 0);
		}

		objId = (String) jsonObject.get(OBJECT_ID);
		versionId = (String) jsonObject.get(VERSION_ID);
		diskId = (String) jsonObject.get(DISK_ID);
		diskPath = (String) jsonObject.get(DISK_PATH);
		
		File file = new File(OSDUtils.getInstance().makeObjPath(diskPath, objId, versionId));
		if (file.exists()) {
			if (file.delete()) {
				return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
			} else {
				return new MQResponse(MQResponseType.ERROR, "failed", "delete failed", 0);
			}
		} else {
			return new MQResponse(MQResponseType.ERROR, "failed", "not exist", 0);
		}
	}
}

class GetAttrObjectCallBack implements MQCallback {
	private static final Logger logger = LoggerFactory.getLogger(GetAttrObjectCallBack.class);

	private static final String BUCKET_NAME = "BucketName";
	private static final String OBJECT_ID = "ObjId";
	private static final String VERSION_ID = "VersionId";
	private static final String DISK_ID = "DiskId";
	private static final String DISK_PATH = "DiskPath";

	private String bucketName;
	private String objId;
	private String versionId;
	private String diskId;
	private String diskPath;
	private String ETag;
	private long size;

	private String message;

	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info("receive getattr object ...");
		logger.info("BiningKey : {}, body : {}}", routingKey, body);

		JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
		
		try {
			jsonObject = (JSONObject)parser.parse(body);
		} catch (ParseException e) {
			logger.error("Error parsing JSON body");
			return new MQResponse(MQResponseType.ERROR, "", "Error parsing JSON body", 0);
		}

		bucketName = (String) jsonObject.get(BUCKET_NAME);
		objId = (String) jsonObject.get(OBJECT_ID);
		versionId = (String) jsonObject.get(VERSION_ID);
		diskId = (String) jsonObject.get(DISK_ID);
		diskPath = (String) jsonObject.get(DISK_PATH);

		File file = new File(OSDUtils.getInstance().makeObjPath(diskPath, objId, versionId));
		if (file.exists()) {
			byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
			MessageDigest md5er = null;
			try {
				md5er = MessageDigest.getInstance(OSDConstants.MD5);
			} catch (NoSuchAlgorithmException e) {
				return new MQResponse(MQResponseType.ERROR, "failed", e.getMessage(), 0);
			}

			size = 0L;
			int readLength = 0;
			try (FileInputStream fis = new FileInputStream(file)) {
				while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
					size += readLength;
					md5er.update(buffer, 0, readLength);
				}
				byte[] digest = md5er.digest();
				ETag = base16().lowerCase().encode(digest);
				message = "{\"BucketName\":\"" + bucketName + "\",\"ObjId\":\"" + objId + "\",\"VersionId\":\"" + versionId + "\",\"MD5\":\"" + ETag + "\",\"Size\":" + size + "}";
				return new MQResponse(MQResponseType.SUCCESS, "", message, 0);
			} catch(Exception e) {
				return new MQResponse(MQResponseType.ERROR, "failed", e.getMessage(), 0);
			}
			
		} else {
			return new MQResponse(MQResponseType.ERROR, "failed", "not exist", 0);
		}
	}
}

class CopyObjectCallback implements MQCallback {
	private static final Logger logger = LoggerFactory.getLogger(CopyObjectCallback.class);
	private static final String OBJECT_ID = "ObjId";
	private static final String VERSION_ID = "VersionId";
	private static final String SOURCE_DISK_ID = "SourceDiskId";
	private static final String SOURCE_DISK_PATH = "SourceDiskPath";
	private static final String TARGET_DISK_ID = "TargetDiskId";
	private static final String TARGET_DISK_PATH = "TargetDiskPath";
	private static final String TARGET_OSD_IP = "TargetOsdIp";

	private String objId;
	private String versionId;
	private String sourceDiskId;
	private String sourceDiskPath;
	private String targetDiskId;
	private String targetDiskPath;
	private String targetOsdIp;

	@Override
	public MQResponse call(String routingKey, String body) {
		logger.info("receive copy object ...");
		logger.info("BiningKey : {}, body : {}}", routingKey, body);
		
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = null;
		try {
            jsonObject = (JSONObject)parser.parse(body);
        } catch (ParseException ex) {
            logger.error("Error parsing JSON body");
			return new MQResponse(MQResponseType.ERROR, "", "Error parsing JSON body", 0);
        }
        
		objId = (String) jsonObject.get(OBJECT_ID);
		versionId = (String) jsonObject.get(VERSION_ID);
		sourceDiskId = (String) jsonObject.get(SOURCE_DISK_ID);
		sourceDiskPath = (String) jsonObject.get(SOURCE_DISK_PATH);
		targetDiskId = (String) jsonObject.get(TARGET_DISK_ID);
		targetDiskPath = (String) jsonObject.get(TARGET_DISK_PATH);
		targetOsdIp = (String) jsonObject.get(TARGET_OSD_IP);

		byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
		File srcFile = new File(OSDUtils.getInstance().makeObjPath(sourceDiskPath, objId, versionId));
		try (FileInputStream fis = new FileInputStream(srcFile)) {
			if (KsanUtils.getLocalIP().equals(targetOsdIp)) {
				File file = new File(OSDUtils.getInstance().makeObjPath(targetDiskPath, objId, versionId));
				File tmpFile = new File(OSDUtils.getInstance().makeTempPath(targetDiskPath, objId, versionId));
				File trashFile = new File(OSDUtils.getInstance().makeTrashPath(targetDiskPath, objId, versionId));
	
				com.google.common.io.Files.createParentDirs(file);
				com.google.common.io.Files.createParentDirs(tmpFile);
				try (FileOutputStream fos = new FileOutputStream(tmpFile, false)) {
					int readLength = 0;
					while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
						fos.write(buffer, 0, readLength);
					}
					fos.flush();
				}
				if (file.exists()) {
					File temp = new File(file.getAbsolutePath());
					KsanUtils.retryRenameTo(temp, trashFile);
				}
				KsanUtils.retryRenameTo(tmpFile, file);
			} else {
				try (Socket destSocket = new Socket(targetOsdIp, OSDConfig.getInstance().getPort())) {
					String header = OsdData.PUT 
						+ OsdData.DELIMITER + targetDiskPath 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId 
						+ OsdData.DELIMITER + String.valueOf(srcFile.length()) 
						+ OsdData.DELIMITER + "primary" 
						+ OsdData.DELIMITER + ""
						+ OsdData.DELIMITER + ""
						+ OsdData.DELIMITER + "";

					logger.debug(OSDConstants.LOG_OSD_SERVER_COPY_RELAY_OSD, targetOsdIp, header);
					OSDUtils.sendHeader(destSocket, header);
					MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5);

					int readLength = 0;
					while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
						destSocket.getOutputStream().write(buffer, 0, readLength);
						md5er.update(buffer, 0, readLength);
					}
					destSocket.getOutputStream().flush();

					byte[] digest = md5er.digest();
					String eTag = base16().lowerCase().encode(digest);

					OsdData data = OSDUtils.receiveData(destSocket);
					if (!eTag.equals(data.getETag())) {
						logger.error(OSDConstants.LOG_OSD_SERVER_DIFFERENCE_ETAG, eTag, data.getETag());
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new MQResponse(MQResponseType.ERROR, "", e.getMessage(), 0);
		}

		return new MQResponse(MQResponseType.SUCCESS, "", "", 0);
	}
}

public class EventObject {
    private MonConfig config;

    private static final Logger logger = LoggerFactory.getLogger(EventObject.class);

    public static EventObject getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final EventObject INSTANCE = new EventObject();
    }
    
    private EventObject() {
        config = MonConfig.getInstance(); 
        config.configure();
    }

	public void regist() {
		try
		{
			MQCallback moveObjectCallback = new MoveObjectCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
				OSDConstants.MQUEUE_NAME_OSD_MOVE_OBJECT + config.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME_FOR_OSD, 
				false, 
				"fanout", 
				OSDConstants.MQUEUE_NAME_OSD_OBJECT_ROUTING_KEY_PREFIX + config.getServerId() + OSDConstants.MQUEUE_NAME_OSD_MOVE_OBJECT_ROUTING_KEY_SUFFIX, 
				moveObjectCallback);
			mq1ton.addCallback(moveObjectCallback);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}

		try {
			MQCallback deleteObjectCallback = new DeleteObjectCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
				OSDConstants.MQUEUE_NAME_OSD_DELETE_OBJECT + config.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME_FOR_OSD, 
				false, 
				"fanout", 
				OSDConstants.MQUEUE_NAME_OSD_OBJECT_ROUTING_KEY_PREFIX + config.getServerId() + OSDConstants.MQUEUE_NAME_OSD_DELETE_OBJECT_ROUTING_KEY_SUFFIX, 
				deleteObjectCallback);
			mq1ton.addCallback(deleteObjectCallback);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}

		try {
			MQCallback getAttrObjectCallBack = new GetAttrObjectCallBack();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
				OSDConstants.MQUEUE_NAME_OSD_GETATTR_OBJECT + config.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME_FOR_OSD, 
				false, 
				"fanout", 
				OSDConstants.MQUEUE_NAME_OSD_OBJECT_ROUTING_KEY_PREFIX + config.getServerId() + OSDConstants.MQUEUE_NAME_OSD_GETATTR_OBJECT_ROUTING_KEY_SUFFIX, 
				getAttrObjectCallBack);
			mq1ton.addCallback(getAttrObjectCallBack);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}

		try {
			MQCallback copyObjectCallback = new CopyObjectCallback();
			MQReceiver mq1ton = new MQReceiver(config.getPortalIp(), 
				OSDConstants.MQUEUE_NAME_OSD_GETATTR_OBJECT + config.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME_FOR_OSD, 
				false, 
				"fanout", 
				OSDConstants.MQUEUE_NAME_OSD_OBJECT_ROUTING_KEY_PREFIX + config.getServerId() + OSDConstants.MQUEUE_NAME_OSD_COPY_OBJECT_ROUTING_KEY_SUFFIX, 
				copyObjectCallback);
			mq1ton.addCallback(copyObjectCallback);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}
	}
}
