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
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.pspace.ifs.ksan.libs.mq.MQCallback;
import com.pspace.ifs.ksan.libs.mq.MQReceiver;
import com.pspace.ifs.ksan.libs.mq.MQResponse;
import com.pspace.ifs.ksan.libs.mq.MQResponseType;
import com.pspace.ifs.ksan.libs.mq.MQResponseCode;

import com.pspace.ifs.ksan.libs.config.AgentConfig;
import com.pspace.ifs.ksan.osd.utils.OSDUtils;
import com.pspace.ifs.ksan.libs.KsanUtils;
import com.pspace.ifs.ksan.osd.utils.OSDConfig;
import com.pspace.ifs.ksan.libs.data.OsdData;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.osd.OSDClient;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;
import com.pspace.ifs.ksan.libs.data.ECPart;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.PrintStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.security.MessageDigest;

import org.json.simple.parser.ParseException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.FileUtils;

class MoveObjectCallback implements MQCallback {
	private static final Logger logger = LoggerFactory.getLogger(MoveObjectCallback.class);
	private static final String OBJECT_ID = "ObjId";
	private static final String VERSION_ID = "VersionId";
	private static final String SOURCE_DISK_ID = "SourceDiskId";
	private static final String SOURCE_DISK_PATH = "SourceDiskPath";
	private static final String TARGET_DISK_ID = "TargetDiskId";
	private static final String TARGET_DISK_PATH = "TargetDiskPath";
	private static final String TARGET_OSD_IP = "TargetOSDIP";

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
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_INVALID_REQUEST, "Error parsing JSON body", 0);
        }
        
		objId = (String) jsonObject.get(OBJECT_ID);
		versionId = (String) jsonObject.get(VERSION_ID);
		sourceDiskId = (String) jsonObject.get(SOURCE_DISK_ID);
		sourceDiskPath = (String) jsonObject.get(SOURCE_DISK_PATH);
		targetDiskId = (String) jsonObject.get(TARGET_DISK_ID);
		targetDiskPath = (String) jsonObject.get(TARGET_DISK_PATH);
		targetOsdIp = (String) jsonObject.get(TARGET_OSD_IP);

		logger.info("objId : {}", objId);
		logger.info("versionId : {}", versionId);
		logger.info("sourceDiskId : {}", sourceDiskId);
		logger.info("sourceDiskPath : {}", sourceDiskPath);
		logger.info("targetDiskId : {}", targetDiskId);
		logger.info("targetDiskPath : {}", targetDiskPath);
		logger.info("targetOsdIp : {}", targetOsdIp);

		byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
		String fullPath = KsanUtils.makeObjPath(sourceDiskPath, objId, versionId);

		// check EC exists
        File ecFile = new File(KsanUtils.makeECPathForOpen(sourceDiskPath, objId, versionId));
        logger.debug("ecfile : {}", ecFile.getAbsolutePath());
        if (ecFile.exists()) {
			try {
				List<ECPart> ecList = new ArrayList<ECPart>();
				for (DiskPool pool : DiskManager.getInstance().getDiskPoolList()) {
					for (Server server : pool.getServerList()) {
						for (Disk disk : server.getDiskList()) {
							ECPart ecPart = new ECPart(server.getIp(), disk.getId(), disk.getPath(), false);
							ecList.add(ecPart);
						}
					}
				}
				int numberOfCodingChunks = DiskManager.getInstance().getECM(sourceDiskId);
				int numberOfDataChunks = DiskManager.getInstance().getECK(sourceDiskId);
				logger.debug("numberOfCodingChunks : {}, numberOfDataChunks : {}", numberOfCodingChunks, numberOfDataChunks);
				int getECPartCount = 0;
				for (ECPart ecPart : ecList) {
					String newECPartPath = ecFile.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
					logger.debug("ec part file : {}", newECPartPath);
					File newECPartFile = new File(newECPartPath);
					if (ecPart.getServerIP().equals(KsanUtils.getLocalIP())) {
						// if local disk, move file
						File sourceECPartFile = new File(KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objId, versionId));
						if (sourceECPartFile.exists()) {
							FileUtils.copyFile(sourceECPartFile, newECPartFile);
							ecPart.setProcessed(true);
							getECPartCount++;
						} else {
							logger.info("ec part does not exist. {}", sourceECPartFile.getAbsolutePath());
						}
					} else {
						try (FileOutputStream fos = new FileOutputStream(newECPartFile)) {
							String getPath = KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objId, versionId);
							OSDClient ecClient = new OSDClient(ecPart.getServerIP(), OSDConfig.getInstance().getPort());
							logger.debug("get ec part file : {}, to : {}, {}", getPath, ecPart.getServerIP(), ecPart.getDiskPath());
							ecClient.getECPartInit(getPath, fos);
							if (ecClient.getECPart() == 0) {
								logger.debug("no data ...");
								if (ecClient.isValid()) {
									ecClient.close();
								}
								continue;
							}
							ecPart.setProcessed(true);
							getECPartCount++;
						} catch (IOException e) {
							PrintStack.logging(logger, e);
						}
					}
				}
				// zunfec
				String ecAllFilePath = KsanUtils.makeECPathForOpen(sourceDiskPath, objId, versionId);
				String command = "";
				getECPartCount = 0;
				StringBuffer sb = new StringBuffer();
				sb.append(Constants.ZUNFEC + ecAllFilePath);
				for (ECPart ecPart : ecList) {
					String ecPartPath = ecFile.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
					if (ecPart.isProcessed()) {
						getECPartCount++;
						sb.append(Constants.SPACE + ecPartPath);
					}
				}
				command = sb.toString();
				logger.debug("command : {}", command);
				Process p = Runtime.getRuntime().exec(command);
				try {
					int exitCode = p.waitFor();
					p.destroy();
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}

				// delete junk file
				String ecDir = KsanUtils.makeECDirectoryPath(sourceDiskPath, objId);
				File dir = new File(ecDir);
				File[] ecFiles = dir.listFiles();
				if (ecFiles != null) {
					for (int i = 0; i < ecFiles.length; i++) {
						if (ecFiles[i].getName().startsWith(Constants.POINT)) {
							if (ecFiles[i].getName().charAt(ecFiles[i].getName().length() - 2) == Constants.CHAR_POINT) {
								ecFiles[i].delete();
							}
						}
					}
				}
				
				fullPath = ecAllFilePath;
			} catch (IOException e) {
				PrintStack.logging(logger, e);
			}
        }

		logger.info("source full path : {}", fullPath);
		File srcFile = new File(fullPath);
		try (FileInputStream fis = new FileInputStream(srcFile)) {
			if (KsanUtils.getLocalIP().equals(targetOsdIp)) {
				File file = new File(KsanUtils.makeObjPath(targetDiskPath, objId, versionId));
				File tmpFile = new File(KsanUtils.makeTempPath(targetDiskPath, objId, versionId));
				File trashFile = new File(KsanUtils.makeTrashPath(targetDiskPath, objId, versionId));
	
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
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		}
		logger.info("success move file : {}", fullPath);
		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
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
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_INVALID_REQUEST, "Error parsing JSON body", 0);
		}

		objId = (String) jsonObject.get(OBJECT_ID);
		versionId = (String) jsonObject.get(VERSION_ID);
		diskId = (String) jsonObject.get(DISK_ID);
		diskPath = (String) jsonObject.get(DISK_PATH);
		logger.info("objId : {}", objId);
		logger.info("versionId : {}", versionId);
		logger.info("diskId : {}", diskId);
		logger.info("diskPath : {}", diskPath);
		
		// check EC exists
        File ecFile = new File(KsanUtils.makeECPathForOpen(diskPath, objId, versionId));
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
                String getPath = KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objId, versionId);
                if (ecPart.getServerIP().equals(KsanUtils.getLocalIP())) {
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
                        OSDClient ecClient = new OSDClient(ecPart.getServerIP(), OSDConfig.getInstance().getPort());
                        logger.debug("delete ec part file : {}, to : {}, {}", getPath, ecPart.getServerIP(), ecPart.getDiskPath());
                        ecClient.deleteECPart(getPath);
                    } catch (Exception e) {
                        PrintStack.logging(logger, e);
                    }
                }
            }

			logger.info("success delete object : {}", ecFile.getAbsolutePath());
			return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
        }

		String fullPath = KsanUtils.makeObjPath(diskPath, objId, versionId);
		logger.info("full path : {}", fullPath);
		File file = new File(fullPath);
		if (file.exists()) {
			if (file.delete()) {
				logger.info("success delete object : {}", fullPath);
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
			} else {
				logger.info("failed delete object : {}", fullPath);
				return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_OBJECT_NOT_FOUND, "object not exist", 0);
			}
		} else {
			logger.info("file not exists: {}", fullPath);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_OBJECT_NOT_FOUND, "object not exist", 0);
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
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_INVALID_REQUEST, "Error parsing JSON body", 0);
		}

		bucketName = (String) jsonObject.get(BUCKET_NAME);
		objId = (String) jsonObject.get(OBJECT_ID);
		versionId = (String) jsonObject.get(VERSION_ID);
		diskId = (String) jsonObject.get(DISK_ID);
		diskPath = (String) jsonObject.get(DISK_PATH);

		logger.info("bucketName : {}", bucketName);
		logger.info("objId : {}", objId);
		logger.info("versionId : {}", versionId);
		logger.info("diskId : {}", diskId);
		logger.info("diskPath : {}", diskPath);

		String fullPath = KsanUtils.makeObjPath(diskPath, objId, versionId);
		logger.info("full path : {}", fullPath);

		// check EC exists
        File ecFile = new File(KsanUtils.makeECPathForOpen(diskPath, objId, versionId));
        logger.debug("ecfile : {}", ecFile.getAbsolutePath());
        if (ecFile.exists()) {
			try {
				List<ECPart> ecList = new ArrayList<ECPart>();
				for (DiskPool pool : DiskManager.getInstance().getDiskPoolList()) {
					for (Server server : pool.getServerList()) {
						for (Disk disk : server.getDiskList()) {
							ECPart ecPart = new ECPart(server.getIp(), disk.getId(), disk.getPath(), false);
							ecList.add(ecPart);
						}
					}
				}
				int numberOfCodingChunks = DiskManager.getInstance().getECM(diskId);
				int numberOfDataChunks = DiskManager.getInstance().getECK(diskId);
				logger.debug("numberOfCodingChunks : {}, numberOfDataChunks : {}", numberOfCodingChunks, numberOfDataChunks);
				int getECPartCount = 0;
				for (ECPart ecPart : ecList) {
					String newECPartPath = ecFile.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
					logger.debug("ec part file : {}", newECPartPath);
					File newECPartFile = new File(newECPartPath);
					if (ecPart.getServerIP().equals(KsanUtils.getLocalIP())) {
						// if local disk, move file
						File sourceECPartFile = new File(KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objId, versionId));
						if (sourceECPartFile.exists()) {
							FileUtils.copyFile(sourceECPartFile, newECPartFile);
							ecPart.setProcessed(true);
							getECPartCount++;
						} else {
							logger.info("ec part does not exist. {}", sourceECPartFile.getAbsolutePath());
						}
					} else {
						try (FileOutputStream fos = new FileOutputStream(newECPartFile)) {
							String getPath = KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objId, versionId);
							OSDClient ecClient = new OSDClient(ecPart.getServerIP(), OSDConfig.getInstance().getPort());
							logger.debug("get ec part file : {}, to : {}, {}", getPath, ecPart.getServerIP(), ecPart.getDiskPath());
							ecClient.getECPartInit(getPath, fos);
							if (ecClient.getECPart() == 0) {
								logger.debug("no data ...");
								if (ecClient.isValid()) {
									ecClient.close();
								}
								continue;
							}
							ecPart.setProcessed(true);
							getECPartCount++;
						} catch (IOException e) {
							PrintStack.logging(logger, e);
						}
					}
				}
				// zunfec
				String ecAllFilePath = KsanUtils.makeECPathForOpen(diskPath, objId, versionId);
				String command = "";
				getECPartCount = 0;
				StringBuffer sb = new StringBuffer();
				sb.append(Constants.ZUNFEC + ecAllFilePath);
				for (ECPart ecPart : ecList) {
					String ecPartPath = ecFile.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
					if (ecPart.isProcessed()) {
						getECPartCount++;
						sb.append(Constants.SPACE + ecPartPath);
					}
				}
				command = sb.toString();
				logger.debug("command : {}", command);
				Process p = Runtime.getRuntime().exec(command);
				try {
					int exitCode = p.waitFor();
					p.destroy();
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}

				// delete junk file
				String ecDir = KsanUtils.makeECDirectoryPath(diskPath, objId);
				File dir = new File(ecDir);
				File[] ecFiles = dir.listFiles();
				if (ecFiles != null) {
					for (int i = 0; i < ecFiles.length; i++) {
						if (ecFiles[i].getName().startsWith(Constants.POINT)) {
							if (ecFiles[i].getName().charAt(ecFiles[i].getName().length() - 2) == Constants.CHAR_POINT) {
								ecFiles[i].delete();
							}
						}
					}
				}
				
				fullPath = ecAllFilePath;
			} catch (IOException e) {
				PrintStack.logging(logger, e);
			}
        }
		
		File file = new File(fullPath);
		logger.debug("file path : {}", file.getAbsolutePath());
		if (file.exists()) {
			byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
			MessageDigest md5er = null;
			try {
				md5er = MessageDigest.getInstance(OSDConstants.MD5);
			} catch (NoSuchAlgorithmException e) {
				return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
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
				logger.info("send message: {}", message);
				return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, message, 0);
			} catch(Exception e) {
				logger.error(e.getMessage());
				return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_OBJECT_NOT_FOUND, e.getMessage(), 0);
			}
			
		} else {
			logger.info("file not exists: {}", fullPath);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_OBJECT_NOT_FOUND, "object not exist", 0);
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
	private static final String TARGET_OSD_IP = "TargetOSDIP";

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
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_INVALID_REQUEST, "Error parsing JSON body", 0);
        }
        
		objId = (String) jsonObject.get(OBJECT_ID);
		versionId = (String) jsonObject.get(VERSION_ID);
		sourceDiskId = (String) jsonObject.get(SOURCE_DISK_ID);
		sourceDiskPath = (String) jsonObject.get(SOURCE_DISK_PATH);
		targetDiskId = (String) jsonObject.get(TARGET_DISK_ID);
		targetDiskPath = (String) jsonObject.get(TARGET_DISK_PATH);
		targetOsdIp = (String) jsonObject.get(TARGET_OSD_IP);

		logger.info("objId : {}", objId);
		logger.info("versionId : {}", versionId);
		logger.info("sourceDiskId : {}", sourceDiskId);
		logger.info("sourceDiskPath : {}", sourceDiskPath);
		logger.info("targetDiskId : {}", targetDiskId);
		logger.info("targetDiskPath : {}", targetDiskPath);
		logger.info("targetOsdIp : {}", targetOsdIp);

		byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
		String fullPath = KsanUtils.makeObjPath(sourceDiskPath, objId, versionId);

		// check EC exists
        File ecFile = new File(KsanUtils.makeECPathForOpen(sourceDiskPath, objId, versionId));
        logger.debug("ecfile : {}", ecFile.getAbsolutePath());
        if (ecFile.exists()) {
			try {
				List<ECPart> ecList = new ArrayList<ECPart>();
				for (DiskPool pool : DiskManager.getInstance().getDiskPoolList()) {
					for (Server server : pool.getServerList()) {
						for (Disk disk : server.getDiskList()) {
							ECPart ecPart = new ECPart(server.getIp(), disk.getId(), disk.getPath(), false);
							ecList.add(ecPart);
						}
					}
				}
				int numberOfCodingChunks = DiskManager.getInstance().getECM(sourceDiskId);
				int numberOfDataChunks = DiskManager.getInstance().getECK(sourceDiskId);
				logger.debug("numberOfCodingChunks : {}, numberOfDataChunks : {}", numberOfCodingChunks, numberOfDataChunks);
				int getECPartCount = 0;
				for (ECPart ecPart : ecList) {
					String newECPartPath = ecFile.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
					logger.debug("ec part file : {}", newECPartPath);
					File newECPartFile = new File(newECPartPath);
					if (ecPart.getServerIP().equals(KsanUtils.getLocalIP())) {
						// if local disk, move file
						File sourceECPartFile = new File(KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objId, versionId));
						if (sourceECPartFile.exists()) {
							FileUtils.copyFile(sourceECPartFile, newECPartFile);
							ecPart.setProcessed(true);
							getECPartCount++;
						} else {
							logger.info("ec part does not exist. {}", sourceECPartFile.getAbsolutePath());
						}
					} else {
						try (FileOutputStream fos = new FileOutputStream(newECPartFile)) {
							String getPath = KsanUtils.makeECPathForOpen(ecPart.getDiskPath(), objId, versionId);
							OSDClient ecClient = new OSDClient(ecPart.getServerIP(), OSDConfig.getInstance().getPort());
							logger.debug("get ec part file : {}, to : {}, {}", getPath, ecPart.getServerIP(), ecPart.getDiskPath());
							ecClient.getECPartInit(getPath, fos);
							if (ecClient.getECPart() == 0) {
								logger.debug("no data ...");
								if (ecClient.isValid()) {
									ecClient.close();
								}
								continue;
							}
							ecPart.setProcessed(true);
							getECPartCount++;
						} catch (IOException e) {
							PrintStack.logging(logger, e);
						}
					}
				}
				// zunfec
				String ecAllFilePath = KsanUtils.makeECPathForOpen(sourceDiskPath, objId, versionId);
				String command = "";
				getECPartCount = 0;
				StringBuffer sb = new StringBuffer();
				sb.append(Constants.ZUNFEC + ecAllFilePath);
				for (ECPart ecPart : ecList) {
					String ecPartPath = ecFile.getAbsolutePath() + Constants.POINT + Integer.toString(getECPartCount);
					if (ecPart.isProcessed()) {
						getECPartCount++;
						sb.append(Constants.SPACE + ecPartPath);
					}
				}
				command = sb.toString();
				logger.debug("command : {}", command);
				Process p = Runtime.getRuntime().exec(command);
				try {
					int exitCode = p.waitFor();
					p.destroy();
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}

				// delete junk file
				String ecDir = KsanUtils.makeECDirectoryPath(sourceDiskPath, objId);
				File dir = new File(ecDir);
				File[] ecFiles = dir.listFiles();
				if (ecFile != null) {
					for (int i = 0; i < ecFiles.length; i++) {
						if (ecFiles[i].getName().startsWith(Constants.POINT)) {
							if (ecFiles[i].getName().charAt(ecFiles[i].getName().length() - 2) == Constants.CHAR_POINT) {
								ecFiles[i].delete();
							}
						}
					}
				}
				
				fullPath = ecAllFilePath;
			} catch (IOException e) {
				PrintStack.logging(logger, e);
			}
        }

		logger.info("full path : {}", fullPath);
		File srcFile = new File(fullPath);
		try (FileInputStream fis = new FileInputStream(srcFile)) {
			if (KsanUtils.getLocalIP().equals(targetOsdIp)) {
				File file = new File(KsanUtils.makeObjPath(targetDiskPath, objId, versionId));
				File tmpFile = new File(KsanUtils.makeTempPath(targetDiskPath, objId, versionId));
				File trashFile = new File(KsanUtils.makeTrashPath(targetDiskPath, objId, versionId));
	
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
					// MessageDigest md5er = MessageDigest.getInstance(OSDConstants.MD5);

					int readLength = 0;
					while ((readLength = fis.read(buffer, 0, OSDConstants.MAXBUFSIZE)) != -1) {
						destSocket.getOutputStream().write(buffer, 0, readLength);
						// md5er.update(buffer, 0, readLength);
					}
					destSocket.getOutputStream().flush();

					// byte[] digest = md5er.digest();
					// String eTag = base16().lowerCase().encode(digest);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_OBJECT_NOT_FOUND, e.getMessage(), 0);
				}
			}
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new MQResponse(MQResponseType.ERROR, MQResponseCode.MQ_UNKNOWN_ERROR, e.getMessage(), 0);
		}

		logger.info("success copy file : {}", fullPath);
		return new MQResponse(MQResponseType.SUCCESS, MQResponseCode.MQ_SUCCESS, "", 0);
	}
}

public class EventObject {
    private AgentConfig config;
	private MQReceiver receiverMoveObjectCallback;
	private MQReceiver receiverDeleteObjectCallback;
	private MQReceiver receiverGetAttrObjectCallBack;
	private MQReceiver receiverCopyObjectCallback;

    private static final Logger logger = LoggerFactory.getLogger(EventObject.class);

    public static EventObject getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final EventObject INSTANCE = new EventObject();
    }
    
    private EventObject() {
        config = AgentConfig.getInstance(); 
        config.configure();
    }

	public void regist() {
		// MQReceiver(String host, String qname, String exchangeName, boolean queeuDurableity, String exchangeOption, String routingKey, MQCallback callback)
		int mqPort = Integer.parseInt(config.getMQPort());
		String serviceId = OSDPortal.getInstance().getServiceId();

		try
		{
			MQCallback moveObjectCallback = new MoveObjectCallback();
			receiverMoveObjectCallback = new MQReceiver(config.getMQHost(),
				mqPort,
				config.getMQUser(),
				config.getMQPassword(),
				OSDConstants.MQUEUE_NAME_OSD_MOVE_OBJECT + serviceId, //config.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME_FOR_OSD, 
				false, 
				"direct", 
				OSDConstants.MQUEUE_NAME_OSD_OBJECT_ROUTING_KEY_PREFIX + config.getServerId() + OSDConstants.MQUEUE_NAME_OSD_MOVE_OBJECT_ROUTING_KEY_SUFFIX, 
				moveObjectCallback);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}

		try {
			MQCallback deleteObjectCallback = new DeleteObjectCallback();
			receiverDeleteObjectCallback = new MQReceiver(config.getMQHost(), 
				mqPort,
				config.getMQUser(),
				config.getMQPassword(),
				OSDConstants.MQUEUE_NAME_OSD_DELETE_OBJECT + serviceId, //config.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME_FOR_OSD, 
				false, 
				"direct", 
				OSDConstants.MQUEUE_NAME_OSD_OBJECT_ROUTING_KEY_PREFIX + config.getServerId() + OSDConstants.MQUEUE_NAME_OSD_DELETE_OBJECT_ROUTING_KEY_SUFFIX, 
				deleteObjectCallback);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}

		try {
			MQCallback getAttrObjectCallBack = new GetAttrObjectCallBack();
			receiverGetAttrObjectCallBack = new MQReceiver(config.getMQHost(), 
				mqPort,
				config.getMQUser(),
				config.getMQPassword(),
				OSDConstants.MQUEUE_NAME_OSD_GETATTR_OBJECT + serviceId, //config.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME_FOR_OSD, 
				false, 
				"direct", 
				OSDConstants.MQUEUE_NAME_OSD_OBJECT_ROUTING_KEY_PREFIX + config.getServerId() + OSDConstants.MQUEUE_NAME_OSD_GETATTR_OBJECT_ROUTING_KEY_SUFFIX, 
				getAttrObjectCallBack);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}

		try {
			MQCallback copyObjectCallback = new CopyObjectCallback();
			receiverCopyObjectCallback = new MQReceiver(config.getMQHost(), 
				mqPort,
				config.getMQUser(),
				config.getMQPassword(),
				OSDConstants.MQUEUE_NAME_OSD_COPY_OBJECT + serviceId, //config.getServerId(), 
				OSDConstants.MQUEUE_EXCHANGE_NAME_FOR_OSD, 
				false, 
				"direct", 
				OSDConstants.MQUEUE_NAME_OSD_OBJECT_ROUTING_KEY_PREFIX + config.getServerId() + OSDConstants.MQUEUE_NAME_OSD_COPY_OBJECT_ROUTING_KEY_SUFFIX, 
				copyObjectCallback);
		} catch (Exception ex){
			throw new RuntimeException(ex);
		}
	}
}
