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

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64.Decoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataPostObject;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.PostPolicy;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.sign.S3Signing;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.objmanager.Metadata;

import org.slf4j.LoggerFactory;

public class PostObject extends S3Request {
    public PostObject(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(PostObject.class);
	}

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_POST_OBJECT_START);
		
        // throw new GWException(GWErrorCode.NOT_IMPLEMENTED);
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		String object = s3Parameter.getObjectName();
		logger.debug(GWConstants.LOG_BUCKET_OBJECT, bucket, object);

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);

		DataPostObject dataPostObject = new DataPostObject(s3Parameter);
		dataPostObject.extract();
        
		if (Strings.isNullOrEmpty(dataPostObject.getKey())) {
			logger.info(GWErrorCode.BAD_REQUEST.getMessage());
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		}

        if (!Strings.isNullOrEmpty(dataPostObject.getPolicy())) {
			Decoder decoder = Base64.getDecoder();
			byte[] bytePostPolicy = decoder.decode(dataPostObject.getPolicy());
			String postPolicy = new String(bytePostPolicy);
			ObjectMapper jsonMapper = new ObjectMapper();

			PostPolicy postPolicyJson = null;
			try {
				postPolicyJson = jsonMapper.readValue(postPolicy, PostPolicy.class);
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}

			Map<String, String> conditionMap = new HashMap<String, String>();
			if (postPolicyJson.conditions == null) {
				logger.info(GWErrorCode.BAD_REQUEST.getMessage());
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}

			if (postPolicyJson.conditions.size() == 0) {
				logger.info(GWErrorCode.BAD_REQUEST.getMessage());
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}

			for (Object o : postPolicyJson.conditions) {
				// check
				logger.info("conditions ==> className(" + o.getClass().getName() + ")");
				if (o.getClass().getName().equals("java.util.LinkedHashMap")) {
					@SuppressWarnings("unchecked")
					Map<String, String> policyMap = (HashMap<String, String>) o;

					for (Map.Entry<String, String> s : policyMap.entrySet()) {
						logger.info("conditions ==> key(" + s.getKey() + "), value(" + s.getValue() + ")");
						dataPostObject.checkPolicy(s.getKey(), s.getValue());
						conditionMap.put(s.getKey().toLowerCase(), s.getValue());
					}
				} else if (o.getClass().getName().equals("java.util.ArrayList")) {
					@SuppressWarnings("unchecked")
					List<Object> policyList = (List<Object>) o;

					if (!((String) policyList.get(0)).equalsIgnoreCase("starts-with")
							&& !((String) policyList.get(0)).equalsIgnoreCase("eq")
							&& !((String) policyList.get(0)).equalsIgnoreCase("content-length-range")) {
						throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
					}

					if (((String) policyList.get(0)).equalsIgnoreCase("eq")) {
						logger.info("conditions ==> cond(" + policyList.get(0) + "), value1 (" + policyList.get(1)
								+ "), value2 (" + policyList.get(2) + ")");
						dataPostObject.checkPolicy((String) policyList.get(1), (String) policyList.get(2));
					} else if (((String) policyList.get(0)).equalsIgnoreCase("starts-with")) {
						logger.info("conditions ==> cond(" + policyList.get(0) + "), value1 (" + policyList.get(1)
								+ "), value2 (" + policyList.get(2) + ")");
						dataPostObject.checkPolityStarts((String) policyList.get(1), (String) policyList.get(2));
					} else if (((String) policyList.get(0)).equalsIgnoreCase("content-length-range")) {
						if (policyList.size() != 3) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}

						logger.info("conditions ==> cond(" + policyList.get(0) + "), value1 (" + policyList.get(1)
								+ "), value2 (" + policyList.get(2) + ")");
						if ((int) policyList.get(1) < 0) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}

						if (dataPostObject.getPayload().length < (int) policyList.get(1)) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}

						if ((int) policyList.get(2) < 0) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}

						if (dataPostObject.getPayload().length > (int) policyList.get(2)) {
							logger.info(GWErrorCode.BAD_REQUEST.getMessage());
							throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
						}
					}
				} else {
					logger.info(o.getClass().getName());
				}
			}

			if (Strings.isNullOrEmpty(postPolicyJson.expiration)) {
				logger.info(GWErrorCode.BAD_REQUEST.getMessage());
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			} else {
				dataPostObject.setExpiration(postPolicyJson.getExpiration());
			}

			// bucket check
			if (Strings.isNullOrEmpty(conditionMap.get(GWConstants.CATEGORY_BUCKET))) {
				logger.info(GWErrorCode.ACCESS_DENIED.getMessage());
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
        }

		if (!Strings.isNullOrEmpty(dataPostObject.getAccessKey())) {
			if (Strings.isNullOrEmpty(dataPostObject.getSignature())) {
				logger.info(GWErrorCode.BAD_REQUEST.getMessage());
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}

			// signing check
			S3Signing s3signing = new S3Signing(s3Parameter);
			s3Parameter = s3signing.validatePost(dataPostObject);

			if (!isGrantBucket(String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_WRITE)) {
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		} else {
			if (!isGrantBucket(GWConstants.LOG_REQUEST_ROOT_ID, GWConstants.GRANT_WRITE)) {
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
		}

		s3Parameter.setInputStream(new ByteArrayInputStream(dataPostObject.getPayload()));
		S3Metadata s3Metadata = new S3Metadata();

    }
    
}
