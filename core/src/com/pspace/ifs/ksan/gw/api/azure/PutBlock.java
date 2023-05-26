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

import static com.google.common.io.BaseEncoding.base64;

import com.google.common.base.Strings;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import com.pspace.ifs.ksan.gw.exception.AzuErrorCode;
import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.gw.utils.AzuConfig;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.AzuUtils;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.object.AzuObjectOperation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.security.MessageDigest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import jakarta.servlet.http.HttpServletResponse;

public class PutBlock extends AzuRequest {

    public PutBlock(AzuParameter parameter) {
        super(parameter);
        logger = LoggerFactory.getLogger(PutBlock.class);
    }

    @Override
    public void process() throws AzuException {
        logger.info(AzuConstants.LOG_UPLOAD_BLOCK_START);

        String containerName = azuParameter.getContainerName();
        String blobName = azuParameter.getBlobName();
		String storageClass = GWConstants.AWS_TIER_STANTARD;
        String diskpoolId = "";
        try {
            diskpoolId = azuParameter.getUser().getUserDefaultDiskpoolId();
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }

        String blockId = azuRequestData.getBlockId();
        if (Strings.isNullOrEmpty(blockId)) {
            throw new AzuException(AzuErrorCode.BAD_REQUEST, azuParameter);
        }

        String contentsLength = azuRequestData.getContentLength();
        long blockLength = Long.parseLong(contentsLength);

        String versionId = GWConstants.VERSIONING_DISABLE_TAIL;
        Metadata objMeta = null;
        try {
            // check exist object
            objMeta = open(containerName, blobName);
        } catch (AzuException e) {
            logger.info(e.getMessage());
            // reset error code
            azuParameter.setErrorCode(GWConstants.EMPTY_STRING);
            objMeta = createLocal(diskpoolId, containerName, blobName, versionId);
            objMeta.set(AzuConstants.EMPTY_STRING, AzuConstants.EMPTY_STRING, AzuConstants.EMPTY_STRING, AzuConstants.EMPTY_STRING, 0);
            objMeta.setVersionId(versionId, GWConstants.OBJECT_TYPE_FILE, true);
            try {
                insertObject(containerName, blobName, objMeta);
            } catch (AzuException e1) {
                // duplicate key error
                logger.info(e1.getMessage());
                objMeta = open(containerName, blobName);
            }
        }

        // AzuObjectOperation azuObjectOperation = new AzuObjectOperation(objMeta, null, azuParameter, versionId);
        // S3Object s3Object = azuObjectOperation.uploadBlock(blockId, blockLength);
        // logger.info("blockId : {}, etag : {}", blockId, s3Object.getEtag());
        
        azuParameter.getResponse().setStatus(HttpServletResponse.SC_CREATED);
    }
    
}

