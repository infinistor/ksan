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

package com.pspace.ifs.ksan.gw.api.azure;

import com.pspace.ifs.ksan.gw.exception.AzuErrorCode;
import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.gw.utils.AzuUtils;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

public class HeadBlob extends AzuRequest {

    public HeadBlob(AzuParameter parameter) {
        super(parameter);
        logger = LoggerFactory.getLogger(HeadBlob.class);
    }

    @Override
    public void process() throws AzuException {
        logger.info(AzuConstants.LOG_HEAD_BLOB_START);

		Metadata objMeta = open(azuParameter.getContainerName(), azuParameter.getBlobName());
        ObjectMapper objectMapper = new ObjectMapper();
        S3Metadata s3Metadata = null;
		try {
			s3Metadata = objectMapper.readValue(objMeta.getMeta(), S3Metadata.class);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}

        azuParameter.getResponse().setCharacterEncoding(AzuConstants.CHARSET_UTF_8);
        try {
            azuParameter.getResponse().setStatus(HttpServletResponse.SC_OK);
            azuParameter.getResponse().setContentLength(s3Metadata.getContentLength().intValue());
            azuParameter.getResponse().addHeader(AzuConstants.HEADER_LASTMODIFIED, formatDate(s3Metadata.getLastModified()));
            azuParameter.getResponse().addHeader(AzuConstants.HEADER_X_MS_CREATION_TIME, formatDate(s3Metadata.getCreationDate()));
            azuParameter.getResponse().addHeader(AzuConstants.HEADER_X_MS_BLOB_TYPE, AzuConstants.BLOB_TYPE_BLOCKBLOB);
            azuParameter.getResponse().addHeader(AzuConstants.HEADER_X_MS_LEASE_STATE, AzuConstants.LEASE_STATE_AVAILABLE);
            azuParameter.getResponse().addHeader(AzuConstants.HEADER_X_MS_LEASE_STATUS, AzuConstants.LEASE_STATUS_UNLOCKED);
            azuParameter.getResponse().addHeader(AzuConstants.HEADER_ETAG, AzuConstants.ETAG_DEFAULT);
            azuParameter.getResponse().addHeader(AzuConstants.HEADER_CONTENT_MD5, s3Metadata.getContentMD5());
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        }        
    }
}

