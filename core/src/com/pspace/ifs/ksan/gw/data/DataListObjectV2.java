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

public class DataListObjectV2 extends S3DataRequest {
	private String continuationToken;
	private String delimiter;
	private String encodingType;
	private String fetchOwner;
	private String maxKeys;
	private String prefix;
	private String startAfter;
	
	private String requestPayer;
	private String expectedBucketOwner;
	
	public DataListObjectV2(S3Parameter s3Parameter) throws GWException {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(DataListObjectV2.class);
	}

	@Override
	public void extract() throws GWException {
		continuationToken = s3Parameter.getRequest().getParameter(GWConstants.CONTINUATION_TOKEN);
		if (Strings.isNullOrEmpty(continuationToken)) {
			logger.info(GWConstants.LOG_DATA_CONTINUATION_TOKEN_NULL);
		}
		
		delimiter = s3Parameter.getRequest().getParameter(GWConstants.DELIMITER);
		if (Strings.isNullOrEmpty(delimiter)) {
			logger.info(GWConstants.LOG_DATA_DELIMITER_NULL);
		}
		
		encodingType = s3Parameter.getRequest().getParameter(GWConstants.ENCODING_TYPE);
		if (Strings.isNullOrEmpty(encodingType)) {
			logger.info(GWConstants.LOG_DATA_ENCODING_TYPE_NULL);
		}
		
		fetchOwner = s3Parameter.getRequest().getParameter(GWConstants.FETCH_OWNER);
		if (Strings.isNullOrEmpty(fetchOwner)) {
			logger.info(GWConstants.LOG_DATA_FETCH_OWNER_NULL);
		}
		
		maxKeys = s3Parameter.getRequest().getParameter(GWConstants.MAX_KEYS);
		if (Strings.isNullOrEmpty(maxKeys)) {
			logger.info(GWConstants.LOG_DATA_MAX_KEYS_NULL);
		}
		
		prefix = s3Parameter.getRequest().getParameter(GWConstants.PREFIX);
		if (Strings.isNullOrEmpty(prefix)) {
			logger.info(GWConstants.LOG_DATA_PREFIX_NULL);
		}
		
		startAfter = s3Parameter.getRequest().getParameter(GWConstants.START_AFTER);
		if (Strings.isNullOrEmpty(startAfter)) {
			logger.info(GWConstants.LOG_DATA_START_AFTER_NULL);
		}
		
		for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_REQUEST_S3_PAYER)) {
				requestPayer = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_EXPECTED_BUCKET_OWNER)) {
				expectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			}
		}
	}

	public String getContinuationToken() {
		return continuationToken;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public String getEncodingType() {
		return encodingType;
	}

	public String getFetchOwner() {
		return fetchOwner;
	}

	public String getMaxKeys() {
		return maxKeys;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getStartAfter() {
		return startAfter;
	}

	public String getRequestPayer() {
		return requestPayer;
	}

	public String getExpectedBucketOwner() {
		return expectedBucketOwner;
	}
}
