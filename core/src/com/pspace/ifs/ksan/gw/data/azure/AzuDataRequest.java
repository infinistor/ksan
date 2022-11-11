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

package com.pspace.ifs.ksan.gw.data.azure;

import java.io.IOException;

import org.slf4j.Logger;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.exception.AzuErrorCode;
import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.libs.PrintStack;

import com.pspace.ifs.ksan.gw.utils.AzuConstants;

public abstract class AzuDataRequest {
    String contentLength;
	protected Logger logger;
	protected AzuParameter parameter;

    public AzuDataRequest(AzuParameter parameter) throws AzuException {
		this.parameter = parameter;
		checkContentLength();
		if (parameter.getResponse() != null) {
			for (String header : parameter.getResponse().getHeaderNames()) {
				parameter.addResponseSize(header.length());
				String value = parameter.getResponse().getHeader(header);
				if (!Strings.isNullOrEmpty(value)) {
					parameter.addResponseSize(value.length());
				}
			}
		}
	}

	private void checkContentLength() throws AzuException {
		contentLength = parameter.getRequest().getHeader(HttpHeaders.CONTENT_LENGTH);
		if (!Strings.isNullOrEmpty(contentLength)) {
			long length = Long.parseLong(contentLength);
			if (length < 0) {
				throw new AzuException(AzuErrorCode.INVALID_ARGUMENT, parameter);
			}
		}
	}

	protected String getContentLength() {
		return contentLength;
	}

	protected String readXml() throws AzuException {
		String ret = null;

		try {
			byte[] xml = parameter.getInputStream().readAllBytes();
			parameter.addRequestSize(xml.length);
			ret = new String(xml);
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.INTERNAL_SERVER_ERROR, parameter);
		}

		logger.info(ret);
		
		if(Strings.isNullOrEmpty(ret)) {
			logger.warn(AzuErrorCode.INVALID_ARGUMENT.getMessage());
			throw new AzuException(AzuErrorCode.INVALID_ARGUMENT, parameter);
		}

		if (!ret.contains(AzuConstants.XML_VERSION)) {
			ret = AzuConstants.XML_VERSION_FULL + ret;
		}
		
		return ret;
	}

	protected String readJson() throws AzuException {
		String ret = null;

		try {
			byte[] json = parameter.getInputStream().readAllBytes();
			parameter.addRequestSize(json.length);
			ret = new String(json);
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.INTERNAL_SERVER_ERROR, parameter);
		}

		logger.info(ret);
		
		if(Strings.isNullOrEmpty(ret)) {
			logger.warn(AzuErrorCode.INVALID_ARGUMENT.getMessage());
			throw new AzuException(AzuErrorCode.INVALID_ARGUMENT, parameter);
		}
		
		return ret;
	}

	public abstract void extract() throws AzuException;
}

