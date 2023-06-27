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
package com.pspace.ifs.ksan.gw.api.gcs;

import jakarta.servlet.http.HttpServletResponse;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.objmanager.Bucket;

import org.slf4j.LoggerFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.base.Strings;

public class GCSCreateBucket extends GCSRequest{

    public GCSCreateBucket(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(GCSCreateBucket.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_GCS_CREATE_BUCKET_START);
        
        String bucketName = gcsRequestData.getBucketName();
        checkBucketName(bucketName);

        logger.debug(GWConstants.LOG_CREATE_BUCKET_NAME, bucketName);

        logger.info("user : {}, {}, {}", s3Parameter.getUser().getAccessKey(), s3Parameter.getUser().getUserDiskpoolId(GWConstants.AWS_TIER_STANTARD), s3Parameter.getUser().getUserDefaultDiskpoolId());
		String diskpoolId = s3Parameter.getUser().getUserDefaultDiskpoolId();
		logger.info("user default diskpoolId : {}", diskpoolId);

        Bucket bucket = new Bucket();
		bucket.setName(bucketName);
		bucket.setUserId(s3Parameter.getUser().getUserId());
		bucket.setUserName(s3Parameter.getUser().getUserName());
		// bucket.setAcl(xml);
		bucket.setDiskPoolId(diskpoolId);

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setBucket(bucket.getName());
		s3Bucket.setUserName(bucket.getUserName());
		s3Parameter.setBucket(s3Bucket);

        createBucket(bucket);

        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();

        json.put("name", bucketName);
        json.put("acl", array);
        json.put("cors", array);
        json.put("defaultObjectAcl", array);
        json.put("id", bucketName);

        s3Parameter.getResponse().setContentType(GWConstants.JSON_CONTENT_TYPE);
        try {
            s3Parameter.getResponse().getOutputStream().write(json.toString().getBytes());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
        }

        s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }

    private boolean isValidBucketName(String bucketName) {
		if (bucketName == null ||
				bucketName.length() < 3 || bucketName.length() > 255 ||
				bucketName.startsWith(GWConstants.POINT) || bucketName.startsWith(GWConstants.DASH) ||
				bucketName.endsWith(GWConstants.POINT) || bucketName.endsWith(GWConstants.DASH) ||
				!GWConstants.VALID_BUCKET_CHAR.matchesAllOf(bucketName) ||
				bucketName.startsWith(GWConstants.XNN_DASH_DASH) || bucketName.contains(GWConstants.DOUBLE_POINT) ||
				bucketName.contains(GWConstants.POINT_DASH) || bucketName.contains(GWConstants.DASH_POINT)) {

			return false;
		}

		return true;
	}
	
	private void checkBucketName(String bucket) throws GWException {
		if (Strings.isNullOrEmpty(bucket)) {
			logger.error(GWConstants.LOG_BUCKET_IS_NULL);
			throw new GWException(GWErrorCode.METHOD_NOT_ALLOWED, s3Parameter);
		}
		
		if (!isValidBucketName(bucket)) {
			logger.error(GWConstants.LOG_CREATE_BUCKET_INVALID, bucket);
			throw new GWException(GWErrorCode.INVALID_BUCKET_NAME, s3Parameter);
		}
	}
}
