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
package com.pspace.ifs.ksan.gw.api;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.gw.data.DataGetObjectLockConofiguration;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.ObjectLockConfiguration;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;

import org.slf4j.LoggerFactory;

public class GetObjectLockConfiguration extends S3Request {

    public GetObjectLockConfiguration(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(GetObjectLockConfiguration.class);
    }

    @Override
    public void process() throws GWException {
        logger.info(GWConstants.LOG_GET_OBJECT_LOCK_CONFIGURATION_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		String object = s3Parameter.getObjectName();
		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}
		
		checkGrantBucketOwner(s3Parameter.isPublicAccess(), s3Parameter.getUser().getUserId(), GWConstants.GRANT_READ_ACP);

		DataGetObjectLockConofiguration dataGetObjectLockConofiguration = new DataGetObjectLockConofiguration(s3Parameter);
		dataGetObjectLockConofiguration.extract();

		String versionId = dataGetObjectLockConofiguration.getVersionId();
		s3Parameter.setVersionId(versionId);

		Metadata objMeta = null;
        if (Strings.isNullOrEmpty(versionId)) {
			objMeta = open(bucket, object);
		} else {
			objMeta = open(bucket, object, versionId);
		}

		logger.debug(GWConstants.LOG_OBJECT_META, objMeta.toString());

		String objectLock = getBucketInfo().getObjectLock();
		logger.debug(GWConstants.LOG_OBJECT_LOCK, objectLock);
        if (Strings.isNullOrEmpty(objectLock)) {
            throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
        }

		try {
            ObjectLockConfiguration oc = new XmlMapper().readValue(objectLock, ObjectLockConfiguration.class);
            if (!GWConstants.STATUS_ENABLED.equals(oc.objectLockEnabled)) {
                throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
            }
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		
		String meta = objMeta.getMeta();
        S3Metadata s3Metadata;
        try {
            s3Metadata = new ObjectMapper().readValue(meta, S3Metadata.class);
        } catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
		String legalHold = s3Metadata.getLegalHold();
		if (!Strings.isNullOrEmpty(legalHold)) {
			XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
			javax.xml.stream.XMLStreamWriter xml;
			try {
				xml = xmlOutputFactory.createXMLStreamWriter(s3Parameter.getResponse().getOutputStream());
				xml.writeStartDocument();
				xml.writeStartElement(GWConstants.LEGAL_HOLD);
				writeSimpleElement(xml, GWConstants.XML_STATUS, legalHold);
				xml.writeEndElement();
				xml.flush();
			} catch (XMLStreamException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			} catch (IOException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		} else {
			throw new GWException(GWErrorCode.INVALID_REQUEST, s3Parameter);
		}
		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
    
}
