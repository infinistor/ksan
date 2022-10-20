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

import com.pspace.ifs.ksan.gw.data.DataPutObjectLegalHold;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.LegalHold;
import com.pspace.ifs.ksan.gw.format.ObjectLockConfiguration;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.objmanager.Metadata;

import org.slf4j.LoggerFactory;

public class PutObjectLegalHold extends S3Request {

    public PutObjectLegalHold(S3Parameter s3Parameter) {
        super(s3Parameter);
		logger = LoggerFactory.getLogger(PutObjectLegalHold.class);
    }
    
    @Override
	public void process() throws GWException {
        logger.info(GWConstants.LOG_PUT_OBJECT_LEGALHOLD_START);
		String bucket = s3Parameter.getBucketName();
		String object = s3Parameter.getObjectName();
		initBucketInfo(bucket);

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

        DataPutObjectLegalHold dataPutObjectLegalHold = new DataPutObjectLegalHold(s3Parameter);
        dataPutObjectLegalHold.extract();

        if (!checkPolicyBucket(GWConstants.ACTION_PUT_OBJECT_LEGAL_HOLD, s3Parameter, dataPutObjectLegalHold)) {
            checkGrantBucket(s3Parameter.isPublicAccess(), s3Parameter.getUser().getUserId(), GWConstants.GRANT_WRITE);
        }

        String versionId = dataPutObjectLegalHold.getVersionId();
        
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
                throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
            }
        } catch (IOException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        String status;
        LegalHold lh;
        try {
            lh = new XmlMapper().readValue(dataPutObjectLegalHold.getLegalHoldXml(), LegalHold.class);
        } catch (JsonProcessingException e) {
            PrintStack.logging(logger, e);
            throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
        }

        status = lh.status;
        if (!Strings.isNullOrEmpty(status)) {
            if (!status.equals(GWConstants.ON) && !status.equals(GWConstants.OFF)) {
                throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
            }
        } else {
            throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
        }

        s3Metadata.setLegalHold(status);
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
