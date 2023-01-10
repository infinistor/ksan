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
import java.util.ArrayList;

import jakarta.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataPutObjectAcl;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.Owner;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.pspace.ifs.ksan.objmanager.Metadata;

import org.slf4j.LoggerFactory;

public class KsanPutObjectAcl extends S3Request {
    public KsanPutObjectAcl(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(KsanPutObjectAcl.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_ADMIN_PUT_OBJECT_ACL_START);
		
		String bucket = s3Parameter.getBucketName();
		String object = s3Parameter.getObjectName();
		initBucketInfo(bucket);

		GWUtils.checkCors(s3Parameter);

		DataPutObjectAcl dataPutObjectAcl = new DataPutObjectAcl(s3Parameter);
		dataPutObjectAcl.extract();
		
		String versionId = dataPutObjectAcl.getVersionId();

		Metadata objMeta = null;
		if (Strings.isNullOrEmpty(versionId)) {
			objMeta = open(bucket, object);
		} else {
			objMeta = open(bucket, object, versionId);
		}

		String xml = makeAcl(bucketAccessControlPolicy, dataPutObjectAcl.getAclXml(), dataPutObjectAcl);

		logger.debug(GWConstants.LOG_ACL, xml);
		
		objMeta.setAcl(xml);

		updateObjectAcl(objMeta);
		
		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}
}
