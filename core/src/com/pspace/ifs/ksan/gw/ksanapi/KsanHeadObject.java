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
package com.pspace.ifs.ksan.gw.ksanapi;
import com.pspace.ifs.ksan.gw.api.S3Request;

import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataHeadObject;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.pspace.ifs.ksan.objmanager.Metadata;

import org.slf4j.LoggerFactory;

public class KsanHeadObject extends S3Request {
    public KsanHeadObject(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(KsanHeadObject.class);
	}

	@Override
	public void process() throws GWException {		
		logger.info(GWConstants.LOG_HEAD_OBJECT_START);
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		String object = s3Parameter.getObjectName();
		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setBucket(bucket);
		s3Bucket.setUserName(getBucketInfo().getUserName());
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);
		GWUtils.checkCors(s3Parameter);
		
		// if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
		// 	throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		// }
		
		DataHeadObject dataHeadObject = new DataHeadObject(s3Parameter);
		dataHeadObject.extract();
		
		String versionId = dataHeadObject.getVersionId();
		
		String expectedBucketOwner = dataHeadObject.getExpectedBucketOwner();
		if (!Strings.isNullOrEmpty(expectedBucketOwner)) {
			if (!isBucketOwner(expectedBucketOwner)) {
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}

		Metadata objMeta = null;
		if (Strings.isNullOrEmpty(versionId)) {
			objMeta = open(bucket, object);
			versionId = objMeta.getVersionId();
		} else {
			objMeta = open(bucket, object, versionId);
		}

		// objMeta.setAcl(GWUtils.makeOriginalXml(objMeta.getAcl(), s3Parameter));
		// checkGrantObject(s3Parameter.isPublicAccess(), objMeta, s3Parameter.getUser().getUserId(), GWConstants.GRANT_READ);

		// meta info
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			logger.debug(GWConstants.LOG_META, objMeta.getMeta());
			S3Metadata s3Metadata = objectMapper.readValue(objMeta.getMeta(), S3Metadata.class);

			// check customer-key
			if (!Strings.isNullOrEmpty(s3Metadata.getCustomerKey())) {
				if (!Strings.isNullOrEmpty(dataHeadObject.getServerSideEncryptionCustomerKey())) {
					if (!s3Metadata.getCustomerKey().equals(dataHeadObject.getServerSideEncryptionCustomerKey())) {
						logger.warn(GWConstants.ENCRYPTION_CUSTOMER_KEY_IS_INVALID);
					throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
					}
				} else {
					logger.warn(GWConstants.ENCRYPTION_CUSTOMER_KEY_IS_NULL);
					throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
				}
			}

			s3Parameter.getResponse().addHeader(GWConstants.X_AMZ_VERSION_ID, s3Metadata.getVersionId());
			GWUtils.addMetadataToResponse(s3Parameter.getRequest(), s3Parameter.getResponse(), s3Metadata, null, null);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		
		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}
}
