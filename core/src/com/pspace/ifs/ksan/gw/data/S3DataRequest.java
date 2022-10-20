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

import java.io.IOException;

import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;


public abstract class S3DataRequest {
	protected String contentLength;

	protected Logger logger;
	protected S3Parameter s3Parameter;

	protected String keyDelimiter;
	protected String keyMaxKeys;
	protected String keyPrefix;

	// policy key putobject
	protected String keyXAmzAcl;
	protected String keyXAmzCopySource;
	protected String keyXAmzGrantFullControl;
	protected String keyXAmzGrantRead;
	protected String keyXAmzGrantReadAcp;
	protected String keyXAmzGrantWrite;
	protected String keyXAmzGrantWriteAcp;
	protected String keyXAmzMetadataDirective;
	protected String keyXAmzServerSideEncryption;
	protected String keyXAmzServerSideEncryptionAwsKmsKeyId;
	protected String keyXAmzStorageClass;
	protected String keyXAmzWebsiteRedirectLocation;
	protected String keyXAmzObjectLockMode;
	protected String keyXAmzObjectLockRetainUntilDate;
	protected String keyXAmzObjectLockRemainingRetentionDays;
	protected String keyXAmzObjectLockLegalHold;

	public S3DataRequest(S3Parameter s3Parameter) throws GWException {
		this.s3Parameter = s3Parameter;
		checkContentLength();
		if (s3Parameter.getResponse() != null) {
			for (String header : s3Parameter.getResponse().getHeaderNames()) {
				s3Parameter.addResponseSize(header.length());
				String value = s3Parameter.getResponse().getHeader(header);
				if (!Strings.isNullOrEmpty(value)) {
					s3Parameter.addResponseSize(value.length());
				}
			}
		}
	}

	private void checkContentLength() throws GWException {
		contentLength = s3Parameter.getRequest().getHeader(HttpHeaders.CONTENT_LENGTH);
		if (!Strings.isNullOrEmpty(contentLength)) {
			long length = Long.parseLong(contentLength);
			if (length < 0) {
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		}
	}

	protected String getContentLength() {
		return contentLength;
	}

	protected String readXml() throws GWException {
		String ret = null;

		try {
			byte[] xml = s3Parameter.getInputStream().readAllBytes();
			s3Parameter.addRequestSize(xml.length);
			ret = new String(xml);
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		logger.info(ret);
		
		if(Strings.isNullOrEmpty(ret)) {
			logger.warn(GWErrorCode.INVALID_ARGUMENT.getMessage());
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}

		if(!ret.contains(GWConstants.XML_VERSION)) {
			ret = GWConstants.XML_VERSION_FULL_STANDALONE + ret;
		}
		
		return ret;
	}

	protected String readJson() throws GWException {
		String ret = null;

		try {
			byte[] json = s3Parameter.getInputStream().readAllBytes();
			s3Parameter.addRequestSize(json.length);
			ret = new String(json);
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		logger.info(ret);
		
		if(Strings.isNullOrEmpty(ret)) {
			logger.warn(GWErrorCode.INVALID_ARGUMENT.getMessage());
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}
		
		return ret;
	}

	public abstract void extract() throws GWException;

	public String getPolicyDelimter() {
		return keyDelimiter;
	}

	public String getPolicyMaxkeys() {
		return keyMaxKeys;
	}

	public String getPolicyPrefix() {
		return keyPrefix;
	}

	public String getXAmzAcl() {
		return keyXAmzAcl;
	}

	public String getXAmzCopySource() {
		return keyXAmzCopySource;
	}

	public String getXAmzGrantFullControl() {
		return keyXAmzGrantFullControl;
	}

	public String getXAmzGrantRead() {
		return keyXAmzGrantRead;
	}

	public String getXAmzGrantReadAcp() {
		return keyXAmzGrantReadAcp;
	}

	public String getXAmzGrantWrite() {
		return keyXAmzGrantWrite;
	}

	public String getXAmzGrantWriteAcp() {
		return keyXAmzGrantWriteAcp;
	}

	public String getXAmzMetadataDirective() {
		return keyXAmzMetadataDirective;
	}

	public String getXAmzServerSideEncryption() {
		return keyXAmzServerSideEncryption;
	}
	
	public String getXAmzServerSideEncryptionAwsKmsKeyId() {
		return keyXAmzServerSideEncryptionAwsKmsKeyId;
	}

	public String getXAmzStorageClass() {
		return keyXAmzStorageClass;
	}

	public String getXAmzWebsiteRedirectLocation() {
		return keyXAmzWebsiteRedirectLocation;
	}

	public String getXAmzObjectLockMode() {
		return keyXAmzObjectLockMode;
	}

	public String getXAmzObjectLockRetainUntilDate() {
		return keyXAmzObjectLockRetainUntilDate;
	}

	public String getXAmzObjectLockRemainingRetentionDays() {
		return keyXAmzObjectLockRemainingRetentionDays;
	}

	public String getXAmzObjectLockLegalHold() {
		return keyXAmzObjectLockLegalHold;
	}
	
	// policy key delete
	protected String keyVersionId;

	public String getVersionId() {
		return keyVersionId;
	}
}
