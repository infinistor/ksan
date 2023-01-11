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

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.Retention;
import com.pspace.ifs.ksan.gw.format.ObjectLockConfiguration;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.objmanager.Metadata;

import org.slf4j.LoggerFactory;

public class PutObjectRetention extends S3Request {
    public PutObjectRetention(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(PutObjectRetention.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_PUT_OBJECT_RETENTION_START);
		String bucket = s3Parameter.getBucketName();
		String object = s3Parameter.getObjectName();
		initBucketInfo(bucket);

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		String versionId = s3RequestData.getVersionId();

		if (!checkPolicyBucket(GWConstants.ACTION_PUT_OBJECT_RETENTION, s3Parameter)) {
			checkGrantBucket(false, GWConstants.GRANT_WRITE);
		}
		
		Metadata objMeta = null;
		if (Strings.isNullOrEmpty(versionId)) {
			objMeta = open(bucket, object);
			versionId = objMeta.getVersionId();
		} else {
			objMeta = open(bucket, object, versionId);
		}

		// meta info
		S3Metadata s3Metadata = null;
		String meta = "";
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			logger.debug(GWConstants.LOG_META, objMeta.getMeta());
			s3Metadata = objectMapper.readValue(objMeta.getMeta(), S3Metadata.class);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		try {
			String objectLock = getBucketInfo().getObjectLock();
			if (Strings.isNullOrEmpty(objectLock)) {
				logger.info("bucket objectlock is null or empty.");
				throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
			}
			ObjectLockConfiguration oc = new XmlMapper().readValue(objectLock, ObjectLockConfiguration.class);
			if (!oc.objectLockEnabled.equals(GWConstants.STATUS_ENABLED)) {
				logger.info("bucket objectlock is not equals Enabled.");
				throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
			}
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		String mode;
		String retainUntilDate;
		try {
			Retention rt = new XmlMapper().readValue(s3RequestData.getRetentionXml(), Retention.class);
			mode = rt.mode;
			retainUntilDate = rt.date;

			if (!Strings.isNullOrEmpty(mode)) {
				if (!rt.mode.equals(GWConstants.GOVERNANCE) && !rt.mode.equals(GWConstants.COMPLIANCE)) {
					throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
				}
			} else {
				throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
			}
			
			if (!Strings.isNullOrEmpty(s3Metadata.getLockMode())) {
				if (s3Metadata.getLockMode().equals(GWConstants.COMPLIANCE) && mode.equals(GWConstants.GOVERNANCE)) {
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}
			}

			if (!Strings.isNullOrEmpty(s3Metadata.getLockExpires())) {
				long curDate = GWUtils.parseRetentionTimeExpire(s3Metadata.getLockExpires(), s3Parameter);
				long newDate = GWUtils.parseRetentionTimeExpire(retainUntilDate, s3Parameter);
				logger.info(GWConstants.LOG_CUR_NEW_DATE, curDate, newDate);
				if (!Strings.isNullOrEmpty(s3RequestData.getBypassGovernanceRetention())) {
					if (!s3RequestData.getBypassGovernanceRetention().equalsIgnoreCase(GWConstants.XML_TRUE) && curDate > newDate) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
				} else {
					if (curDate > newDate) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
				}
			}
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		s3Metadata.setLockMode(mode);
		s3Metadata.setLockExpires(retainUntilDate);

		try {
			// objectMapper.setSerializationInclusion(Include.NON_NULL);
			meta = objectMapper.writeValueAsString(s3Metadata);
			logger.debug("meta : {}", meta);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		objMeta.setMeta(meta);
		updateObjectMeta(objMeta);
		
		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}
}
