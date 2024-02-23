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
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.AzuUtils;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.object.AzuObjectOperation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.google.common.primitives.Longs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

public class GetBlob extends AzuRequest {

    public GetBlob(AzuParameter parameter) {
        super(parameter);
        logger = LoggerFactory.getLogger(GetBlob.class);
    }

    @Override
    public void process() throws AzuException {
        logger.info(AzuConstants.LOG_GET_BLOB_START);

        String containerName = azuParameter.getContainerName();
        String blobName = azuParameter.getBlobName();
        String versionId = GWConstants.VERSIONING_DISABLE_TAIL;
        
        Metadata objMeta = open(containerName, blobName);
        S3Metadata s3Metadata = S3Metadata.getS3Metadata(objMeta.getMeta());

        String range = azuParameter.getRequest().getHeader(AzuConstants.X_MS_RANGE);
        logger.debug("range : {}", range);

		AzuObjectOperation azuObjectOperation = new AzuObjectOperation(objMeta, null, azuParameter, versionId);
		try {
			azuObjectOperation.getObject(range);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
		}

        azuParameter.getResponse().setCharacterEncoding(AzuConstants.CHARSET_UTF_8);
        
        azuParameter.getResponse().addHeader(AzuConstants.HEADER_LASTMODIFIED, formatDate(s3Metadata.getLastModified()));
        azuParameter.getResponse().addHeader(AzuConstants.HEADER_X_MS_CREATION_TIME, formatDate(s3Metadata.getCreationDate()));
        azuParameter.getResponse().addHeader(AzuConstants.HEADER_X_MS_BLOB_TYPE, AzuConstants.BLOB_TYPE_BLOCKBLOB);
        azuParameter.getResponse().addHeader(AzuConstants.HEADER_X_MS_LEASE_STATE, AzuConstants.LEASE_STATE_AVAILABLE);
        azuParameter.getResponse().addHeader(AzuConstants.HEADER_X_MS_LEASE_STATUS, AzuConstants.LEASE_STATUS_UNLOCKED);
        azuParameter.getResponse().addHeader(AzuConstants.HEADER_ETAG, AzuConstants.ETAG_DEFAULT);
        azuParameter.getResponse().addHeader(AzuConstants.HEADER_CONTENT_MD5, s3Metadata.getContentMD5());
        

        if (Strings.isNullOrEmpty(range)) {
            azuParameter.getResponse().addHeader(AzuConstants.HEADER_CONTENT_LENGTH, String.valueOf(azuParameter.getResponseSize()));
            azuParameter.getResponse().setStatus(HttpServletResponse.SC_OK);
        } else {
            String[] infos = range.split(GWConstants.EQUAL);
            String[] ranges = infos[1].split(GWConstants.DASH);
            long offset = Longs.tryParse(ranges[0]);
            long length = Longs.tryParse(ranges[1]);
            String responseRange = "bytes " + offset + "-" + length + "/" + azuParameter.getResponseSize();
            azuParameter.getResponse().addHeader(AzuConstants.HEADER_CONTENT_RANGE, responseRange);
            azuParameter.getResponse().setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
    }
}

