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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataPutBucketObjectLock;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.ObjectLockConfiguration;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;

import org.slf4j.LoggerFactory;

public class PutObjectLockConfiguration extends S3Request {

    public PutObjectLockConfiguration(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(PutObjectLockConfiguration.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_PUT_BUCKET_OBJECT_LOCK_START);
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);
		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}
		
		checkGrantBucketOwner(s3Parameter.isPublicAccess(), String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_WRITE_ACP);
		
		DataPutBucketObjectLock dataPutBucketObjectLock = new DataPutBucketObjectLock(s3Parameter);
		dataPutBucketObjectLock.extract();

		String ObjectLockXml = dataPutBucketObjectLock.getObjectLockXml();

		if(Strings.isNullOrEmpty(getBucketInfo().getObjectLock())) {
			throw new GWException(GWErrorCode.INVALID_BUCKET_STATE, s3Parameter);
		}

		if(Strings.isNullOrEmpty(ObjectLockXml)) {
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}

		logger.info(GWConstants.LOG_PUT_BUCKET_OBJECT_LOCK, getBucketInfo().getObjectLock());
		try {
			ObjectLockConfiguration oc = new XmlMapper().readValue(getBucketInfo().getObjectLock(), ObjectLockConfiguration.class);
			if (!oc.objectLockEnabled.equals(GWConstants.STATUS_ENABLED)) {
				throw new GWException(GWErrorCode.INVALID_BUCKET_STATE, s3Parameter);
			}

			oc = new XmlMapper().readValue(ObjectLockXml, ObjectLockConfiguration.class);
			if (!oc.objectLockEnabled.equals(GWConstants.STATUS_ENABLED)) {
				throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
			}

			if (!Strings.isNullOrEmpty(oc.rules.defaultRetention.days) && !Strings.isNullOrEmpty(oc.rules.defaultRetention.years)) {
				throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
			}

			if (!Strings.isNullOrEmpty(oc.rules.defaultRetention.days)) {
				try {
					if (Integer.parseInt(oc.rules.defaultRetention.days) <= 0) {
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				} catch (NumberFormatException e) {
					throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
				}
			}

			if (!Strings.isNullOrEmpty(oc.rules.defaultRetention.years)) {
				try {
					if (Integer.parseInt(oc.rules.defaultRetention.years) <= 0) {
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				} catch (NumberFormatException e) {
					throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
				}
			}

			if (!Strings.isNullOrEmpty(oc.rules.defaultRetention.mode)) {
				if (!oc.rules.defaultRetention.mode.equals(GWConstants.GOVERNANCE) && !oc.rules.defaultRetention.mode.equals(GWConstants.COMPLIANCE)) {
					throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
				}
			}
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

        updateBucketObjectLock(bucket, ObjectLockXml);

		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
    
}
