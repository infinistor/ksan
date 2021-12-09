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
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;


public abstract class S3DataRequest {
	String contentLength;
	protected Logger logger;
	protected S3Parameter s3Parameter;

	public S3DataRequest(S3Parameter s3Parameter) throws GWException {
		this.s3Parameter = s3Parameter;
		checkContentLength();
	}

	private void checkContentLength() throws GWException {
		contentLength = s3Parameter.getRequest().getHeader(HttpHeaders.CONTENT_LENGTH);
		if (!Strings.isNullOrEmpty(contentLength)) {
			long length = Long.parseLong(contentLength);
			if (length < 0) {
				throw new GWException(GWErrorCode.INVALID_ARGUMENT);
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
}
