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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.ifs.ksan.gw.data.DataPutBucketAcl;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.Owner;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;

import org.slf4j.LoggerFactory;

public class PutBucketAcl extends S3Request {

	public PutBucketAcl(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(PutBucketAcl.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_PUT_BUCKET_ACL_START);
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setBucket(bucket);
		s3Bucket.setUserName(getBucketInfo().getUserName());
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}
		
		checkGrantBucketOwner(s3Parameter.isPublicAccess(), s3Parameter.getUser().getUserId(), GWConstants.GRANT_WRITE_ACP);

		DataPutBucketAcl dataPutBucketAcl = new DataPutBucketAcl(s3Parameter);
		dataPutBucketAcl.extract();

		AccessControlPolicy preAccessControlPolicy = null;
		
		try {
			XmlMapper xmlMapper = new XmlMapper();
			preAccessControlPolicy = xmlMapper.readValue(getBucketInfo().getAcl(), AccessControlPolicy.class);
		} catch (JsonMappingException e) {
			logger.error(e.getMessage());
			new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
			new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		accessControlPolicy = new AccessControlPolicy();
		accessControlPolicy.aclList = new AccessControlList();
		accessControlPolicy.aclList.grants = new ArrayList<Grant>();
		accessControlPolicy.owner = new Owner();
		accessControlPolicy.owner.id = s3Parameter.getUser().getUserId();
		accessControlPolicy.owner.displayName = s3Parameter.getUser().getUserName();

		String xml = GWUtils.makeAclXml(accessControlPolicy, 
										preAccessControlPolicy, 
										dataPutBucketAcl.hasAclKeyword(), 
										dataPutBucketAcl.getAclXml(), 
										dataPutBucketAcl.getAcl(),
										getBucketInfo(),
										s3Parameter.getUser().getUserId(),
										s3Parameter.getUser().getUserName(),
										dataPutBucketAcl.getGrantRead(),
										dataPutBucketAcl.getGrantWrite(), 
										dataPutBucketAcl.getGrantFullControl(), 
										dataPutBucketAcl.getGrantReadAcp(), 
										dataPutBucketAcl.getGrantWriteAcp(),
										s3Parameter);

		logger.debug(GWConstants.LOG_ACL, xml);

		updateBucketAcl(bucket, xml);
	
		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}
}
