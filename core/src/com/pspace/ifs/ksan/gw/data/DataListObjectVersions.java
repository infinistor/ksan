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

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.LoggerFactory;

public class DataListObjectVersions extends S3DataRequest {
	private String delimiter;
	private String encodingType;
	private String keyMarker;
	private String maxKeys;
	private String prefix;
	private String versionIdMarker;
	
	private String expectedBucketOwner;

	public DataListObjectVersions(S3Parameter s3Parameter) throws GWException {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(DataListObjectVersions.class);
	}

	@Override
	public void extract() throws GWException {
		delimiter = s3Parameter.getRequest().getParameter(GWConstants.DELIMITER);
		if (Strings.isNullOrEmpty(delimiter)) {
			logger.info(GWConstants.LOG_DATA_DELIMITER_NULL);
		}
		
		encodingType = s3Parameter.getRequest().getParameter(GWConstants.ENCODING_TYPE);
		if (Strings.isNullOrEmpty(encodingType)) {
			logger.info(GWConstants.LOG_DATA_ENCODING_TYPE_NULL);
		}
		
		keyMarker = s3Parameter.getRequest().getParameter(GWConstants.KEY_MARKER);
		if (Strings.isNullOrEmpty(keyMarker)) {
			logger.info(GWConstants.LOG_DATA_KEY_MARKER_NULL);
		}
		
		maxKeys = s3Parameter.getRequest().getParameter(GWConstants.MAX_KEYS);
		if (Strings.isNullOrEmpty(maxKeys)) {
			logger.info(GWConstants.LOG_DATA_MAX_KEYS_NULL);
		}
		
		prefix = s3Parameter.getRequest().getParameter(GWConstants.PREFIX);
		if (Strings.isNullOrEmpty(prefix)) {
			logger.info(GWConstants.LOG_DATA_PREFIX_NULL);
		}
		
		versionIdMarker = s3Parameter.getRequest().getParameter(GWConstants.VERSION_ID_MARKER);
		if (Strings.isNullOrEmpty(versionIdMarker)) {
			logger.info(GWConstants.LOG_DATA_VERSION_ID_MARKER_NULL);
		}
		
		for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_EXPECTED_BUCKET_OWNER)) {
				expectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			}
		}

		keyDelimiter = getDelimiter();
		keyMaxKeys = getMaxKeys();
		keyPrefix = getPrefix();
	}

	public String getDelimiter() {
		return delimiter;
	}

	public String getEncodingType() {
		return encodingType;
	}

	public String getKeyMarker() {
		return keyMarker;
	}

	public String getMaxKeys() {
		return maxKeys;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getVersionIdMarker() {
		return versionIdMarker;
	}

	public String getExpectedBucketOwner() {
		return expectedBucketOwner;
	}
}
