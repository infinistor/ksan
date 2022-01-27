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

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.LifecycleConfiguration;
import com.pspace.ifs.ksan.gw.format.LifecycleConfiguration.Rule;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.LoggerFactory;

public class DataPutBucketLifeCycle extends S3DataRequest {
    private String contentMD5;
	private String expectedBucketOwner;
	private String lifecycleXml;
	
	public DataPutBucketLifeCycle(S3Parameter s3Parameter) throws GWException {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(DataPutBucketLifeCycle.class);
	}

	@Override
	public void extract() {
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

	public String getLifecycleXml() throws GWException {
		lifecycleXml = readXml();

		XmlMapper xmlMapper = new XmlMapper();
		LifecycleConfiguration lcc;
		try {
			lcc = xmlMapper.readValue(lifecycleXml, LifecycleConfiguration.class);
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		Map<String, String> id = new HashMap<String, String>(); 
		
		if (lcc.rules != null) {
			for (Rule rl : lcc.rules) {
				if (rl.id != null) { 
					if (rl.id.length() > 255) {
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				
					id.put(rl.id, rl.id);
				} else {
					String generatedString = UUID.randomUUID().toString().substring(24).toUpperCase();
					id.put(generatedString, generatedString);
					rl.id =generatedString;
				}
				
				if (rl.status != null && rl.status.compareTo(GWConstants.STATUS_ENABLED) != 0 && rl.status.compareTo(GWConstants.STATUS_DISABLED) != 0) {
					logger.error("rl.status : {}", rl.status);
					throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
				}
			}

			if( lcc.rules.size() > id.size() ) {
				logger.error("lcc.rules.isze : {}, id.size : {}", lcc.rules.size(), id.size());
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		}

		try {
			lifecycleXml = xmlMapper.writeValueAsString(lcc);
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_ID, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_DATE, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_EXPIRED_OBJECT_DELETE_MARKER, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_NON_CURRENT_VERSION_EXPIRATION, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_NON_CURRENT_VERSION_TRANSITION, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_ABORT_INCOMPLETE_MULTIPART_UPLOAD, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_PREFIX, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_TRANSITION, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_FILITER, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_DAYS, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_STORAGE_CLASS, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_NON_CURRENT_DAYS, "");
		lifecycleXml = lifecycleXml.replaceAll(GWConstants.LIFECYCLE_XML_DAYS_AFTER_INITIATION, "");

		if(!lifecycleXml.contains(GWConstants.XML_VERSION)) {
			lifecycleXml = GWConstants.XML_VERSION_FULL_STANDALONE + lifecycleXml;
		}

		return lifecycleXml;
	}
}
