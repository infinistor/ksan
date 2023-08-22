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
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.exception.AzuErrorCode;
import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.PrintStack;

import com.pspace.ifs.ksan.gw.utils.AzuConstants;

public class AzuRequestData {
	private Logger logger;
	private AzuParameter parameter;

    public AzuRequestData(AzuParameter parameter) {
		this.parameter = parameter;
		logger = LoggerFactory.getLogger(AzuRequestData.class);
	}

	public String getContentLength() throws AzuException {
		String contentLength = parameter.getRequest().getHeader(HttpHeaders.CONTENT_LENGTH);
		if (!Strings.isNullOrEmpty(contentLength)) {
			long length = Long.parseLong(contentLength);
			if (length < 0) {
				throw new AzuException(AzuErrorCode.INVALID_ARGUMENT, parameter);
			}
		}

		return contentLength;
	}

	public String getPrefix() {
        return parameter.getRequest().getParameter(AzuConstants.PARAMETER_PREFIX);
    }

    public String getDelimiter() {
        return parameter.getRequest().getParameter(AzuConstants.PARAMETER_DELIMITER);
    }

    public String getMaxResults() {
        return parameter.getRequest().getParameter(AzuConstants.PARAMETER_MAX_RESULTS);
    }

    public String getInclude() {
        return parameter.getRequest().getParameter(AzuConstants.PARAMETER_INCLUDE);
    }

	public String getContentType() {
        return parameter.getRequest().getHeader(AzuConstants.X_MS_BLOB_CONTENT_TYPE);
    }

    public String getContentMD5() {
        return parameter.getRequest().getHeader(AzuConstants.X_MS_BLOB_CONTENT_MD5);
    }

	public String getBlockId() {
        return parameter.getRequest().getParameter(AzuConstants.BLOCKID);
    }

    public String getBlobContentType() {
        return parameter.getRequest().getHeader(AzuConstants.X_MS_BLOB_CONTENT_TYPE);
    }

    public String getBlobContentMD5() {
        return parameter.getRequest().getHeader(AzuConstants.X_MS_BLOB_CONTENT_MD5);
    }

	public String getXml() throws AzuException {
        return readXml();
    }

	public String readXml() throws AzuException {
		String ret = null;

		try {
			byte[] xml = parameter.getInputStream().readAllBytes();
			parameter.addRequestSize(xml.length);
			ret = new String(xml, Charset.forName(Constants.UTF_8));
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

	public String readJson() throws AzuException {
		String ret = null;

		try {
			byte[] json = parameter.getInputStream().readAllBytes();
			parameter.addRequestSize(json.length);
			ret = new String(json, Charset.forName(Constants.UTF_8));
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
}

