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

import java.util.ArrayList;

import jakarta.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.data.DataCreateBucket;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.Owner;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;

import org.slf4j.LoggerFactory;


public class CreateBucket extends S3Request {

    public CreateBucket(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(CreateBucket.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_CREATE_BUCKET_START);
		
		String bucket = s3Parameter.getBucketName();
		logger.debug(GWConstants.LOG_CREATE_BUCKET_NAME, bucket);
		checkBucketName(bucket);

		if (isExistBucket(bucket) || bucket.equalsIgnoreCase(GWConstants.WEBSITE)) {
			logger.info(GWConstants.LOG_CREATE_BUCKET_EXIST, bucket);
			initBucketInfo(bucket);
			if (isBucketOwner(s3Parameter.getUser().getUserId())) {
				throw new GWException(GWErrorCode.BUCKET_ALREADY_OWNED_BY_YOU, s3Parameter);
			}
            throw new GWException(GWErrorCode.BUCKET_ALREADY_EXISTS, s3Parameter);
        }
		
		DataCreateBucket dataCreateBucket = new DataCreateBucket(s3Parameter);
		dataCreateBucket.extract();

		accessControlPolicy = new AccessControlPolicy();
		accessControlPolicy.aclList = new AccessControlList();
		accessControlPolicy.aclList.grants = new ArrayList<Grant>();
		accessControlPolicy.owner = new Owner();
		accessControlPolicy.owner.id = s3Parameter.getUser().getUserId();
		accessControlPolicy.owner.displayName = s3Parameter.getUser().getUserName();
		
		String xml = GWUtils.makeAclXml(accessControlPolicy, 
										null, 
										dataCreateBucket.hasAclKeyword(), 
										null, 
										dataCreateBucket.getAcl(),
										getBucketInfo(),
										s3Parameter.getUser().getUserId(),
										s3Parameter.getUser().getUserName(),
										dataCreateBucket.getGrantRead(),
										dataCreateBucket.getGrantWrite(), 
										dataCreateBucket.getGrantFullControl(), 
										dataCreateBucket.getGrantReadAcp(), 
										dataCreateBucket.getGrantWriteAcp(),
										s3Parameter);
		logger.debug(GWConstants.LOG_ACL, xml);

		int result = 0;
		if (!Strings.isNullOrEmpty(dataCreateBucket.getBucketObjectLockEnabled()) && GWConstants.STRING_TRUE.equalsIgnoreCase(dataCreateBucket.getBucketObjectLockEnabled())) {
			logger.info(GWConstants.LOG_CREATE_BUCKET_VERSIONING_ENABLED_OBJECT_LOCK_TRUE);
			String objectLockXml = GWConstants.OBJECT_LOCK_XML;
			result = createBucket(bucket, s3Parameter.getUser().getUserName(), s3Parameter.getUser().getUserId(), xml, "", objectLockXml);
			putBucketVersioning(bucket, GWConstants.STATUS_ENABLED);
		} else {
			result = createBucket(bucket, s3Parameter.getUser().getUserName(), s3Parameter.getUser().getUserId(), xml, "", "");
		}

		if (result != 0) {
			throw new GWException(GWErrorCode.INTERNAL_SERVER_DB_ERROR, s3Parameter);
		}
		
		s3Parameter.getResponse().addHeader(HttpHeaders.LOCATION, GWConstants.SLASH + bucket);
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
