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
package com.pspace.ifs.ksan.gw.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.LifecycleConfiguration;
import com.pspace.ifs.ksan.gw.format.LifecycleConfiguration.Rule;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.format.LoggingConfiguration;

import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;

import org.slf4j.LoggerFactory;

public class DataPutBucketLogging extends S3DataRequest {
    private String contentMD5;
	private String expectedBucketOwner;
	private String LoggingXml;
    
    public DataPutBucketLogging(S3Parameter s3Parameter) throws GWException {
        super(s3Parameter);
		logger = LoggerFactory.getLogger(DataPutBucketLogging.class);
    }

    @Override
    public void extract() throws GWException {
        for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.equalsIgnoreCase(GWConstants.CONTENT_MD5)) {
				contentMD5 = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_EXPECTED_BUCKET_OWNER)) {
				expectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			}
		}
    }
    
    public String getContentMD5() {
		return contentMD5;
	}

	public String getExpectedBucketOwner() {
		return expectedBucketOwner;
	}

	public String getLoggingXml() throws GWException {
		LoggingXml = readXml();

		XmlMapper xmlMapper = new XmlMapper();
		LoggingConfiguration lc;
		try {
			lc = xmlMapper.readValue(LoggingXml, LoggingConfiguration.class);
			if (lc.loggingEnabled != null) {
				if (lc.loggingEnabled.targetBucket != null) {
					ObjManager objManager = null;
					boolean isTargetBucket = true;
					
					try {
						objManager = ObjManagers.getInstance().getObjManager();
						isTargetBucket = objManager.isBucketExist(lc.loggingEnabled.targetBucket);
					} catch (Exception e) {
						PrintStack.logging(logger, e);
						throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
					} 

					if (!isTargetBucket) {
						throw new GWException(GWErrorCode.INVALID_TARGET_BUCKET_FOR_LOGGING, s3Parameter);
					}
				}
				
				if (lc.loggingEnabled.targetGrants != null) {
					for (LoggingConfiguration.LoggingEnabled.TargetGrants.Grant grant : lc.loggingEnabled.targetGrants.grants) {
						if (grant.grantee != null && grant.grantee.id != null) {
							if (S3UserManager.getInstance().getUserById(grant.grantee.id) == null) {
								throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
							}
						}
					}
				}
			}
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		return LoggingXml;
	}
}
