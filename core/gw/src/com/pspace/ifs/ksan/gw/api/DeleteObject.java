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
package com.pspace.ifs.ksan.gw.api;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataDeleteObject;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3ObjectOperation;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

import org.slf4j.LoggerFactory;

public class DeleteObject extends S3Request {

	public DeleteObject(S3Parameter ip) {
		super(ip);
		logger = LoggerFactory.getLogger(DeleteObject.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_DELETE_OBJECT_START);
		
		String bucket = s3Parameter.getBucketName();
        initBucketInfo(bucket);

		String object = s3Parameter.getObjectName();
		logger.debug(GWConstants.LOG_DELETE_OBJECT, bucket, object);

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);
		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		checkGrantBucketOwner(s3Parameter.isPublicAccess(), String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_WRITE);
		
		DataDeleteObject dataDeleteObject = new DataDeleteObject(s3Parameter);
		dataDeleteObject.extract();

		S3Metadata s3Metadata = new S3Metadata();
		s3Metadata.setName(object);
		s3Metadata.setOwnerId(String.valueOf(s3Parameter.getUser().getUserId()));
		s3Metadata.setOwnerName(s3Parameter.getUser().getUserName());
		
		String versionId = dataDeleteObject.getVersionId();
		boolean isLastVersion = true;
		String deleteMarker = null;

        String versioningStatus = null;

		versioningStatus = getBucketVersioning(bucket);

		Metadata objMeta = null;
		try {
			if (Strings.isNullOrEmpty(versionId)) {
				objMeta = open(bucket, object);
				versionId = objMeta.getVersionId();
			} else {
				objMeta = open(bucket, object, versionId);
			}
		} catch (GWException e) {
			s3Parameter.getResponse().addHeader(GWConstants.X_AMZ_VERSION_ID, versionId);
			s3Parameter.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
			return;
		}

		S3ObjectOperation objectOperation = new S3ObjectOperation(objMeta, s3Metadata, s3Parameter, versionId, null);

		isLastVersion = objMeta.getLastVersion();
		deleteMarker = objMeta.getDeleteMarker();

		logger.debug(GWConstants.LOG_DELETE_OBJECT_INFO, versionId, isLastVersion, deleteMarker);
		logger.debug(GWConstants.LOG_DELETE_OBJECT_BUCKET_VERSIONING, versioningStatus);
		
		if (Strings.isNullOrEmpty(versioningStatus)) {
			logger.debug(GWConstants.LOG_DELETE_OBJECT_BUCKET_VERSIONING_DISABLED);
            remove(bucket, object);
			objectOperation.deleteObject();
		} else {
			if (versioningStatus.equalsIgnoreCase(GWConstants.VERSIONING_ENABLED)) { // Bucket Versioning Enabled
				logger.debug(GWConstants.LOG_DELETE_OBJECT_BUCKET_VERSIONING_ENABLED);
				if (GWConstants.VERSIONING_DISABLE_TAIL.equals(versionId)) {	// request versionId is null
					if (deleteMarker != null && deleteMarker.equalsIgnoreCase(GWConstants.OBJECT_TYPE_MARK)) {
						remove(bucket, object, versionId);
					} else {
						if (isLastVersion) {
							remove(bucket, object, versionId);
						} else {
							// put delete marker
							try {
								s3Metadata.setDeleteMarker(GWConstants.OBJECT_TYPE_MARK);
								s3Metadata.setVersionId(versionId);
								ObjectMapper jsonMapper = new ObjectMapper();
								String jsonmeta = "";
								jsonmeta = jsonMapper.writeValueAsString(s3Metadata);
								int result;
								if (objMeta.getReplicaDisk() != null) {
									result = insertObject(bucket, object, "", jsonmeta, "", 0L, "", objMeta.getPrimaryDisk().getPath(), objMeta.getReplicaDisk().getPath(), versionId, GWConstants.OBJECT_TYPE_MARK);
								} else {
									result = insertObject(bucket, object, "", jsonmeta, "", 0L, "", objMeta.getPrimaryDisk().getPath(), "", versionId, GWConstants.OBJECT_TYPE_MARK);
								}
								if (result != 0) {
									logger.error(GWConstants.LOG_DELETE_OBJECT_FAILED_MARKER, bucket, object);
								}
								logger.debug(GWConstants.LOG_PUT_DELETE_MARKER);
							} catch (ResourceNotFoundException | JsonProcessingException e) {
								PrintStack.logging(logger, e);
								throw new GWException(GWErrorCode.SERVER_ERROR);
							}
						}		
					}
				} else {	// request with versionId
					if (isLastVersion) {
						if (deleteMarker.equalsIgnoreCase(GWConstants.OBJECT_TYPE_MARK)) {
							remove(bucket, object);
						} else if (deleteMarker.equalsIgnoreCase(GWConstants.OBJECT_TYPE_FILE)) {
							remove(bucket, object, versionId);
							objectOperation.deleteObject();
						} else {
							logger.error(GWConstants.LOG_DELETE_OBJECT_DELETE_MARKER_WRONG, deleteMarker);
							throw new GWException(GWErrorCode.SERVER_ERROR);
						}
					} else {	// request with versionId not currentVid
						remove(bucket, object, versionId);
						objectOperation.deleteObject();
					}
				}
			} else if (versioningStatus.equalsIgnoreCase(GWConstants.VERSIONING_SUSPENDED)) { // Bucket Versioning Suspended 
				logger.debug(GWConstants.LOG_DELETE_OBJECT_BUCKET_VERSIONING_SUSPENDED);
				if (GWConstants.VERSIONING_DISABLE_TAIL.equals(versionId)) {
					if (isLastVersion) {
						if (deleteMarker.equalsIgnoreCase(GWConstants.OBJECT_TYPE_MARK)) {
							remove(bucket, object, versionId);
						} else {
							try {
								s3Metadata.setDeleteMarker(GWConstants.OBJECT_TYPE_MARK);
								s3Metadata.setVersionId(GWConstants.VERSIONING_DISABLE_TAIL);
								ObjectMapper jsonMapper = new ObjectMapper();
								String jsonmeta = "";
								jsonmeta = jsonMapper.writeValueAsString(s3Metadata);
								int result;
								if (objMeta.getReplicaDisk() != null) {
									result = insertObject(bucket, object, "", jsonmeta, "", 0L, "", objMeta.getPrimaryDisk().getPath(), objMeta.getReplicaDisk().getPath(), versionId, GWConstants.OBJECT_TYPE_MARK);
								} else {
									result = insertObject(bucket, object, "", jsonmeta, "", 0L, "", objMeta.getPrimaryDisk().getPath(), "", versionId, GWConstants.OBJECT_TYPE_MARK);
								}
								if (result != 0) {
									logger.error(GWConstants.LOG_DELETE_OBJECT_FAILED_MARKER, bucket, object);
								}
								logger.debug(GWConstants.LOG_PUT_DELETE_MARKER);
							} catch (ResourceNotFoundException | JsonProcessingException e) {
								PrintStack.logging(logger, e);
								throw new GWException(GWErrorCode.SERVER_ERROR);
							}
						}
					}
				} else {	// request with versionId
					remove(bucket, object, versionId);
					objectOperation.deleteObject();
				}
			} else {
				logger.error(GWConstants.LOG_DELETE_OBJECT_BUCKET_VERSIONING_WRONG, versioningStatus);
				throw new GWException(GWErrorCode.SERVER_ERROR);
			}
		}
				
		s3Parameter.getResponse().addHeader(GWConstants.X_AMZ_VERSION_ID, versionId);
		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
}
