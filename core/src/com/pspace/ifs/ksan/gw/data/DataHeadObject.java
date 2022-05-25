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

public class DataHeadObject extends S3DataRequest {
	private String partNumber;
	private String versionId;
	private String ifMatch;
	private String ifNoneMatch;
	private String ifModifiedSince;
	private String ifUnmodifiedSince;
	private String range;
	private String requestPayer;
	private String expectedBucketOwner;
	private String serverSideEncryptionCustomerAlgorithm;
	private String serverSideEncryptionCustomerKey;
	private String serverSideEncryptionCustomerKeyMD5;

	public DataHeadObject(S3Parameter s3Parameter) throws GWException {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(DataHeadObject.class);
	}

	@Override
	public void extract() throws GWException {
		partNumber = s3Parameter.getRequest().getParameter(GWConstants.PART_NUMBER);
		if (Strings.isNullOrEmpty(partNumber)) {
			logger.info(GWConstants.LOG_DATA_PART_NUMBER_NULL);
		}
		
		versionId = s3Parameter.getRequest().getParameter(GWConstants.VERSION_ID);
		if (Strings.isNullOrEmpty(versionId)) {
			logger.info(GWConstants.LOG_DATA_VERSION_ID_NULL);
		}
		
		for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.equalsIgnoreCase(GWConstants.IF_MATCH)) {
				ifMatch = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.IF_NONE_MATCH)) {
				ifNoneMatch = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.IF_MODIFIED_SINCE)) {
				ifModifiedSince = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.IF_UNMODIFIED_SINCE)) {
				ifUnmodifiedSince = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.RANGE)) {
				range = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_REQUEST_PAYER)) {
				requestPayer = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_EXPECTED_BUCKET_OWNER)) {
				expectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM)) {
				serverSideEncryptionCustomerAlgorithm = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY)) {
				serverSideEncryptionCustomerKey = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5)) {
				serverSideEncryptionCustomerKeyMD5 = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			}
		}
	}

	public String getPartNumber() {
		return partNumber;
	}

	public String getVersionId() {
		return versionId;
	}

	public String getRequestPayer() {
		return requestPayer;
	}

	public String getExpectedBucketOwner() {
		return expectedBucketOwner;
	}	
	
	public String getRange() {
		return range;
	}

	public String getIfMatch() {
		return ifMatch;
	}

	public String getIfNoneMatch() {
		return ifNoneMatch;
	}

	public String getIfModifiedSince() {
		return ifModifiedSince;
	}

	public String getIfUnmodifiedSince() {
		return ifUnmodifiedSince;
	}

	public String getServerSideEncryptionCustomerAlgorithm() {
		return serverSideEncryptionCustomerAlgorithm;
	}

	public String getServerSideEncryptionCustomerKey() {
		return serverSideEncryptionCustomerKey;
	}

	public String getServerSideEncryptionCustomerKeyMD5() {
		return serverSideEncryptionCustomerKeyMD5;
	}
}
