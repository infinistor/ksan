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
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.LoggerFactory;

public class DataCompleteMultipartUpload extends S3DataRequest {
	private String uploadId;
	private String requestPayer;
	private String expectedBucketOwner;
	private String multipartXml;
	private String versionId;
	private String replication;
	private String dr;
	private String logging;
	
	public DataCompleteMultipartUpload(S3Parameter s3Parameter) throws GWException {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(DataCompleteMultipartUpload.class);
	}

	@Override
	public void extract() throws GWException {	
		uploadId = s3Parameter.getRequest().getParameter(GWConstants.UPLOAD_ID);
		if (Strings.isNullOrEmpty(uploadId)) {
			logger.error(GWConstants.LOG_DATA_UPLOAD_ID_NULL);
			throw new GWException(GWErrorCode.NO_SUCH_UPLOAD, s3Parameter);
		}
		
		for (String headerName : Collections.list(s3Parameter.getRequest().getHeaderNames())) {
			if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_REQUEST_PAYER)) {
				requestPayer = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_EXPECTED_BUCKET_OWNER)) {
				expectedBucketOwner = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_IFS_VERSION_ID)) {
				versionId = Strings.nullToEmpty(s3Parameter.getRequest().getHeader(headerName));
			} else if (headerName.equalsIgnoreCase(GWConstants.X_IFS_REPLICATION)) {
				replication = GWConstants.IFS_HEADER_REPLICATION;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_IFS_DR)) {
				dr = GWConstants.IFS_HEADER_DR;
			} else if (headerName.equalsIgnoreCase(GWConstants.X_IFS_LOGGING)) {
				logging = GWConstants.IFS_HEADER_LOGGING;
			}
		}
	}

	public String getUploadId() {
		return uploadId;
	}

	public String getRequestPayer() {
		return requestPayer;
	}

	public String getExpectedBucketOwner() {
		return expectedBucketOwner;
	}

	public String getVersionId() {
		return versionId;
	}

	public String getReplication() {
		return replication;
	}

	public String getDr() {
		return dr;
	}

	public String getLogging() {
		return logging;
	}

	public String getMultipartXml() throws GWException {
		try {
			multipartXml = readXml();
		} catch (GWException e) {
			logger.error(GWErrorCode.MALFORMED_X_M_L.getMessage());
			throw new GWException(GWErrorCode.MALFORMED_X_M_L, s3Parameter);
		}
	
		return multipartXml;
	}
}
