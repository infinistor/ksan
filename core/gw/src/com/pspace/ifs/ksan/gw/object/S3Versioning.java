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
package com.pspace.ifs.ksan.gw.object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.Versioning;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Versioning {
    private final Logger logger = LoggerFactory.getLogger(S3Versioning.class);
	
	S3Parameter s3Parameter;
	private String status = null;
	
	public S3Versioning(S3Parameter s3Parameter) {
		this.s3Parameter = s3Parameter;
	}
	
	public void build() throws GWException {
		status = checkBucketVersioning(s3Parameter);
	}

    public void build(String xml) throws GWException {
        status = checkBucketVersioning(xml);
    }

	private String checkBucketVersioning(S3Parameter s3Parameter) throws GWException {
		try {
			if(s3Parameter.getBucket().getVersioning() == null)
				return GWConstants.STATUS_DISABLED;

			Versioning ver = new XmlMapper().readValue(s3Parameter.getBucket().getVersioning(), Versioning.class);
			return ver.status;
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}

    private String checkBucketVersioning(String xml) throws GWException {
        try {
			if(Strings.isNullOrEmpty(xml))
				return GWConstants.STATUS_DISABLED;

			Versioning ver = new XmlMapper().readValue(xml, Versioning.class);
			return ver.status;
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
    }

    public String getStatus() {
        return status;
    }

    public String getVersioningXml() {
        String xml = GWConstants.XML_VERSION_FULL;

        if (status == null) {
            xml += GWConstants.XML_VERSION_CONFIGURATION_END;
        } else {
            xml += GWConstants.XML_VERSION_CONFIGURATION;
            xml += GWConstants.XML_VERSION_CONFIGURATION_STATUS + status + GWConstants.XML_VERSION_CONFIGURATION_STATUS_TAIL;
            xml += GWConstants.XML_VERSION_CONFIGURATION_TAIL;
        }

        return xml;
    }
}
