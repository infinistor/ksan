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
import java.io.Writer;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataListParts;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.multipart.Multipart;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.libs.multipart.ResultParts;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;

import org.slf4j.LoggerFactory;

public class ListParts extends S3Request {

	public ListParts(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(ListParts.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_LIST_PARTS_START);
		
		String bucket = s3Parameter.getBucketName();
		if (!isExistBucket(bucket)) {
			logger.error(GWConstants.LOG_BUCKET_IS_NOT_EXIST, bucket);
            throw new GWException(GWErrorCode.NO_SUCH_BUCKET, s3Parameter);
        }
		initBucketInfo(bucket);

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		DataListParts dataListParts = new DataListParts(s3Parameter);
		dataListParts.extract();

		if (!checkPolicyBucket(GWConstants.ACTION_LIST_MULTIPART_UPLOAD_PARTS, s3Parameter, dataListParts)) {
			checkGrantBucket(s3Parameter.isPublicAccess(), s3Parameter.getUser().getUserId(), GWConstants.GRANT_READ);
		}

		String maxParts = dataListParts.getMaxParts();
		String partNumberMarker = dataListParts.getPartNumberMarker();
		String uploadId = dataListParts.getUploadId();
		int maxPartsValue = GWConstants.MAX_PART_VALUE;
		
		if (!Strings.isNullOrEmpty(maxParts)) {
			if (Integer.valueOf(maxParts) < 0) {
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			} else if (Integer.valueOf(maxParts) > 1000) {
				maxPartsValue = GWConstants.MAX_PART_VALUE;
			} else {
				maxPartsValue = Integer.valueOf(maxParts);
			}
		}
		
		ResultParts resultPart = null;
		ObjMultipart objMultipart = null;
		Multipart multipart = null;
		try {
			objMultipart = getInstanceObjMultipart(bucket);
			multipart = objMultipart.getMultipart(uploadId);
			if (multipart == null) {
				logger.error(GWConstants.LOG_UPLOAD_NOT_FOUND, uploadId);
				throw new GWException(GWErrorCode.NO_SUCH_UPLOAD, s3Parameter);
			}
			// check acl use multipart acl
			logger.info("uploadId : {}, partNumberMarker : {}, maxPartValue : {}", uploadId, partNumberMarker, maxPartsValue);
			resultPart = objMultipart.getParts(uploadId, partNumberMarker, maxPartsValue);
			logger.info("listpart size : {}", resultPart.getListPart().size());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		String meta = multipart.getMeta();
		ObjectMapper jsonMapper = new ObjectMapper();
		S3Metadata s3Metadata;
		try {
			s3Metadata = jsonMapper.readValue(meta, S3Metadata.class);
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}

		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

		s3Parameter.getResponse().setCharacterEncoding(Constants.CHARSET_UTF_8);
		try (Writer writer = s3Parameter.getResponse().getWriter()) {
			s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
			XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
			xmlStreamWriter.writeStartDocument();
			xmlStreamWriter.writeStartElement(GWConstants.LIST_PARTS_RESULT);
			xmlStreamWriter.writeDefaultNamespace(GWConstants.AWS_XMLNS);

			writeSimpleElement(xmlStreamWriter, GWConstants.BUCKET, bucket);
			writeSimpleElement(xmlStreamWriter, GWConstants.KEY, s3Parameter.getObjectName());
			writeSimpleElement(xmlStreamWriter, GWConstants.XML_UPLOADID, uploadId);
			writeInitiatorStanza(xmlStreamWriter);
			writeOwnerInfini(xmlStreamWriter, s3Metadata.getOwnerId(), s3Metadata.getOwnerName());
			writeSimpleElement(xmlStreamWriter, GWConstants.STORAGE_CLASS, GWConstants.AWS_TIER_STANTARD);

			if (resultPart.isTruncated()) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_TRUNCATED, GWConstants.XML_TRUE);
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_NEXT_PART_NUMBER, String.valueOf(resultPart.getPartNumberMarker()));
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_TRUNCATED, GWConstants.XML_FALSE);
			}

			for (Map.Entry entry : resultPart.getListPart().entrySet()) {
				xmlStreamWriter.writeStartElement(GWConstants.PART);
				Part part = (Part) entry.getValue();
				writeSimpleElement(xmlStreamWriter, GWConstants.PARTNUMBER, String.valueOf(part.getPartNumber()));
				if (part.getLastModified() != null) {
					writeSimpleElement(xmlStreamWriter, GWConstants.LAST_MODIFIED, formatDate(part.getLastModified()));
				}
				if (part.getPartETag() != null) {
					writeSimpleElement(xmlStreamWriter, GWConstants.ETAG, part.getPartETag());
				}
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_SIZE, String.valueOf(part.getPartSize()));
				xmlStreamWriter.writeEndElement();
			}
			
			xmlStreamWriter.writeEndElement();
			xmlStreamWriter.flush();
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} catch (XMLStreamException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}
}
