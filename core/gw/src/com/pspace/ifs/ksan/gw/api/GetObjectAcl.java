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

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataGetObjectAcl;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;

import org.slf4j.LoggerFactory;

public class GetObjectAcl extends S3Request {

    public GetObjectAcl(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(GetObjectAcl.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_GET_OBJECT_ACL_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		String object = s3Parameter.getObjectName();
		logger.debug(GWConstants.LOG_BUCKET_OBJECT, bucket, object);

		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);
		GWUtils.checkCors(s3Parameter);
		
		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

        DataGetObjectAcl dataGetObjectAcl = new DataGetObjectAcl(s3Parameter);
        dataGetObjectAcl.extract();
        String versionId = dataGetObjectAcl.getVersionId();

        Metadata objMeta = null;
        if (Strings.isNullOrEmpty(versionId)) {
			objMeta = open(bucket, object);
		} else {
            objMeta = open(bucket, object, versionId);
        }
		logger.debug(GWConstants.LOG_OBJECT_META, objMeta.toString());
        objMeta.setAcl(GWUtils.makeOriginalXml(objMeta.getAcl()));
        
        checkGrantObjectOwner(s3Parameter.isPublicAccess(), objMeta, String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_READ_ACP);

        String aclInfo = objMeta.getAcl();

        if (!aclInfo.contains(GWConstants.XML_VERSION)) {
            aclInfo = GWConstants.XML_VERSION_FULL_STANDALONE + aclInfo;
        }

        aclInfo = aclInfo.replace(GWConstants.ACCESS_CONTROL_POLICY, GWConstants.ACCESS_CONTROL_POLICY_XMLNS);
        aclInfo = aclInfo.replace(GWConstants.ACCESS_CONTROL_POLICY_ID, "");
        aclInfo = aclInfo.replace(GWConstants.ACCESS_CONTROL_POLICY_DISPLAY_NAME, "");
        aclInfo = aclInfo.replace(GWConstants.ACCESS_CONTROL_POLICY_EMAIL_ADDRESS, "");
        aclInfo = aclInfo.replace(GWConstants.ACCESS_CONTROL_POLICY_URI, "");
        logger.debug(GWConstants.LOG_ACL, aclInfo);
        try {
            if (!Strings.isNullOrEmpty(aclInfo)) {
                s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
				s3Parameter.getResponse().getOutputStream().write(aclInfo.getBytes());
			}

		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		
		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
    
}
