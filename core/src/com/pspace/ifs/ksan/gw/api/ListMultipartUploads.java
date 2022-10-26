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

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.data.DataListMultipartUploads;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.multipart.ResultUploads;
import com.pspace.ifs.ksan.libs.multipart.Upload;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.ObjMultipart;

import org.slf4j.LoggerFactory;

public class ListMultipartUploads extends S3Request {

	public ListMultipartUploads(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(ListMultipartUploads.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_LIST_MULTIPART_UPLOADS_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

		GWUtils.checkCors(s3Parameter);
		
		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		DataListMultipartUploads dataListMultipartUploads = new DataListMultipartUploads(s3Parameter);
		dataListMultipartUploads.extract();

		if (!checkPolicyBucket(GWConstants.ACTION_LIST_BUCKET_MULTIPART_UPLOADS, s3Parameter, dataListMultipartUploads)) {
			checkGrantBucket(s3Parameter.isPublicAccess(), s3Parameter.getUser().getUserId(), GWConstants.GRANT_READ);
		}

		String delimiter = dataListMultipartUploads.getDelimiter();
		String encodingType = dataListMultipartUploads.getEncodingType();
		String prefix = dataListMultipartUploads.getPrefix();
		String keyMarker = dataListMultipartUploads.getKeyMarker();
		String uploadIdMarker = dataListMultipartUploads.getUploadIdMarker();
		String maxUploads = dataListMultipartUploads.getMaxUploads();

		if (Strings.isNullOrEmpty(maxUploads)) {
			maxUploads = GWConstants.DEFAULT_MAX_KEYS;
		} else {
			if (Integer.valueOf(maxUploads) < 0) {
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		}

		ResultUploads resultUploads = null;
		ObjMultipart objMultipart = null;
		try {
			objMultipart = getInstanceObjMultipart(bucket);
			resultUploads = objMultipart.getUploads(bucket, delimiter, prefix, keyMarker, uploadIdMarker, Integer.valueOf(maxUploads));
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.INTERNAL_SERVER_ERROR, s3Parameter);
		}
		
		s3Parameter.getResponse().setCharacterEncoding(GWConstants.CHARSET_UTF_8);
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		try (Writer writer = s3Parameter.getResponse().getWriter()) {
			s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
			XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
			xmlStreamWriter.writeStartDocument();
			xmlStreamWriter.writeStartElement(GWConstants.LIST_MULTIPART_UPLOADS_RESULT);
			xmlStreamWriter.writeDefaultNamespace(GWConstants.AWS_XMLNS);
			
			writeSimpleElement(xmlStreamWriter, GWConstants.BUCKET, bucket);

			if (prefix == null) {
				xmlStreamWriter.writeEmptyElement(GWConstants.XML_PREFIX);
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_PREFIX, GWUtils.encodeBlob(encodingType, prefix));
			}

			writeSimpleElement(xmlStreamWriter, GWConstants.XML_MAX_UPLOADS, String.valueOf(maxUploads));
			
			if (keyMarker == null) {
				xmlStreamWriter.writeEmptyElement(GWConstants.XML_KEY_MARKER);
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_KEY_MARKER, GWUtils.encodeBlob(encodingType, keyMarker));
			}
			
			if (uploadIdMarker == null) {
				xmlStreamWriter.writeEmptyElement(GWConstants.XML_UPLOADID_MARKER);
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_UPLOADID_MARKER, GWUtils.encodeBlob(encodingType, uploadIdMarker));
			}

			if (delimiter != null) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_DELIMITER, GWUtils.encodeBlob(encodingType, delimiter));
			}

			if (encodingType != null && encodingType.equals(GWConstants.URL)) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_ENCODING_TYPE, encodingType);
			}

			if (resultUploads.isTruncated()) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_TRUNCATED, GWConstants.XML_TRUE);
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_NEXT_KEY_MARKER, GWUtils.encodeBlob(encodingType, resultUploads.getKeyMarker()));
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_NEXT_UPLOADID_MARKER, GWUtils.encodeBlob(encodingType, resultUploads.getUploadIdMarker()));
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_TRUNCATED, GWConstants.XML_FALSE);
			}

			for (Upload upload : resultUploads.getList()) {
				xmlStreamWriter.writeStartElement(GWConstants.XML_UPLOAD);
				writeSimpleElement(xmlStreamWriter, GWConstants.KEY, upload.getObject());
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_UPLOADID, upload.getUploadId());
				writeInitiatorStanza(xmlStreamWriter);
				writeOwnerInfini(xmlStreamWriter, upload.getOwnerID(), upload.getOwnerName());
				writeSimpleElement(xmlStreamWriter, GWConstants.STORAGE_CLASS, GWConstants.AWS_TIER_STANTARD);
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_INITIATED, formatDate(upload.getChangeTime()));
				xmlStreamWriter.writeEndElement();
				logger.debug(GWConstants.LOG_LIST_MULTIPART_UPLOADS_KEY, upload.getObject());
				logger.debug(GWConstants.LOG_LIST_MULTIPART_UPLOADS_UPLOADID, upload.getUploadId());
				logger.debug(GWConstants.LOG_LIST_MULTIPART_UPLOADS_CHANGE_TIME, formatDate(upload.getChangeTime()));
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
