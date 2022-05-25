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

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataGetObjectTagging;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Metadata;

import org.slf4j.LoggerFactory;

public class GetObjectTagging extends S3Request {
    public GetObjectTagging(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(GetObjectTagging.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_GET_OBJECT_TAGGING_START);
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);
		GWUtils.checkCors(s3Parameter);
		
		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}
		
		DataGetObjectTagging dataGetObjectTagging = new DataGetObjectTagging(s3Parameter);
		dataGetObjectTagging.extract();

		String object = s3Parameter.getObjectName();

		String versionId = dataGetObjectTagging.getVersionId();
		
		Metadata objMeta = null;
		if (Strings.isNullOrEmpty(versionId)) {
			objMeta = open(bucket, object);
		} else {
			objMeta = open(bucket, object, versionId);
		}
		
		objMeta.setAcl(GWUtils.makeOriginalXml(objMeta.getAcl(), s3Parameter));
        
        checkGrantObjectOwner(s3Parameter.isPublicAccess(), objMeta, String.valueOf(s3Parameter.getUser().getUserId()), GWConstants.GRANT_READ);

		String taggingInfo = objMeta.getTag();
		logger.info(GWConstants.LOG_TAGGING, taggingInfo);
		if ( Strings.isNullOrEmpty(taggingInfo)) {
			XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
			javax.xml.stream.XMLStreamWriter xml;
			try {
				xml = xmlOutputFactory.createXMLStreamWriter(s3Parameter.getResponse().getOutputStream());
				xml.writeStartDocument();
				xml.writeStartElement(GWConstants.TAGGING);
				xml.writeDefaultNamespace(GWConstants.AWS_XMLNS);
				xml.writeStartElement(GWConstants.TAG_SET);
				xml.writeEndElement();
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
			try {
				if (!Strings.isNullOrEmpty(taggingInfo)) {
					s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
					s3Parameter.getResponse().getOutputStream().write(taggingInfo.getBytes());
				}
			} catch (IOException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		s3Parameter.getResponse().setStatus(HttpServletResponse.SC_OK);
	}
}
