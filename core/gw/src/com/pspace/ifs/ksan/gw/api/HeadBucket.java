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

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataHeadBucket;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;

import org.slf4j.LoggerFactory;

public class HeadBucket extends S3Request {

	public HeadBucket(S3Parameter ip) {
		super(ip);
		logger = LoggerFactory.getLogger(HeadBucket.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_HEAD_BUCKET_START);
		
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

		checkGrantBucket(s3Parameter.isPublicAccess(), String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_READ);
		
		DataHeadBucket dataHeadBucket = new DataHeadBucket(s3Parameter);
		dataHeadBucket.extract();

		String expectedBucketOwner = dataHeadBucket.getExpectedBucketOwner();
		if (!Strings.isNullOrEmpty(expectedBucketOwner)) {
			if (!isBucketOwner(expectedBucketOwner)) {
				throw new GWException(GWErrorCode.ACCESS_DENIED);
			}
		}
		
		if (isGrant(String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_READ)) {
			s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
		} else {
			throw new GWException(GWErrorCode.ACCESS_DENIED);
		}
	}
}