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

public class DataUploadPartCopy extends S3DataRequest {
	private String partNumber;
	private String uploadId;
	
	private String copySource;
	private String copySourceRange;
	private String copySourceIfMatch;
	private String copySourceIfNoneMatch;
	private String copySourceIfModifiedSince;
	private String copySourceIfUnmodifiedSince;
	private String copySourceServerSideEncryptionCustomerAlgorithm;
	private String copySourceServerSideEncryptionCustomerKey;
	private String copySourceServerSideEncryptionCustomerKeyMD5;
	private String serverSideEncryptionCustomerAlgorithm;
	private String serverSideEncryptionCustomerKey;
	private String serverSideEncryptionCustomerKeyMD5;
	private String requestPayer;
	private String expectedBucketOwner;
	private String sourceExpectedBucketOwner;

	public DataUploadPartCopy(S3Parameter s3Parameter) throws GWException {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(DataUploadPartCopy.class);
	}

	@Override
	public void extract() throws GWException {
		partNumber = s3Parameter.getRequest().getParameter(GWConstants.PART_NUMBER);
		if (Strings.isNullOrEmpty(partNumber)) {
			logger.error(GWConstants.LOG_DATA_PART_NUMBER_NULL);
		}
		
		uploadId = s3Parameter.getRequest().getParameter(GWConstants.UPLOAD_ID);
		if (Strings.isNullOrEmpty(uploadId)) {
			logger.error(GWConstants.LOG_DATA_UPLOAD_ID_NULL);
		}
		
		for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE)) {
				copySource = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_RANGE)) {
				copySourceRange = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_IF_MATCH)) {
				copySourceIfMatch = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_IF_NONE_MATCH)) {
				copySourceIfNoneMatch = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_IF_MODIFIED_SINCE)) {
				copySourceIfModifiedSince = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_IF_UNMODIFIED_SINCE)) {
				copySourceIfUnmodifiedSince = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM)) {
				serverSideEncryptionCustomerAlgorithm = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY)) {
				serverSideEncryptionCustomerKey = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5)) {
				serverSideEncryptionCustomerKeyMD5 = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM)) {
				copySourceServerSideEncryptionCustomerAlgorithm = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY)) {
				copySourceServerSideEncryptionCustomerKey = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5)) {
				copySourceServerSideEncryptionCustomerKeyMD5 = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_REQUEST_PAYER)) {
				requestPayer = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_EXPECTED_BUCKET_OWNER)) {
				expectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_SOURCE_EXPECTED_BUCKET_OWNER)) {
				sourceExpectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			}
		}
	}

	public String getPartNumber() {
		return partNumber;
	}

	public String getUploadId() {
		return uploadId;
	}

	public String getCopySource() {
		return copySource;
	}

	public String getCopySourceRange() {
		return copySourceRange;
	}

	public String getRequestPayer() {
		return requestPayer;
	}

	public String getExpectedBucketOwner() {
		return expectedBucketOwner;
	}

	public String getSourceExpectedBucketOwner() {
		return sourceExpectedBucketOwner;
	}

	public String getCopySourceIfMatch() {
		return copySourceIfMatch;
	}

	public String getCopySourceIfNoneMatch() {
		return copySourceIfNoneMatch;
	}

	public String getCopySourceIfModifiedSince() {
		return copySourceIfModifiedSince;
	}

	public String getCopySourceIfUnmodifiedSince() {
		return copySourceIfUnmodifiedSince;
	}

	public String getCopySourceServerSideEncryptionCustomerAlgorithm() {
		return copySourceServerSideEncryptionCustomerAlgorithm;
	}

	public String getCopySourceServerSideEncryptionCustomerKey() {
		return copySourceServerSideEncryptionCustomerKey;
	}

	public String getCopySourceServerSideEncryptionCustomerKeyMD5() {
		return copySourceServerSideEncryptionCustomerKeyMD5;
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
