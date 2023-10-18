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
import java.util.Map.Entry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;

import org.slf4j.LoggerFactory;

public class ListObjectsV2 extends S3Request {

	public ListObjectsV2(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(ListObjectsV2.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_LIST_OBJECT_V2_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		if (!checkPolicyBucket(GWConstants.ACTION_LIST_BUCKET, s3Parameter)) {
			checkGrantBucket(false, GWConstants.GRANT_READ);
		}

		// read header
		S3ObjectList s3ObjectList = new S3ObjectList();
		
		if (!Strings.isNullOrEmpty(s3RequestData.getMaxKeys())) {
			try {
				if (Integer.parseInt(s3RequestData.getMaxKeys()) < 0) {
					throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
				} else if (Integer.parseInt(s3RequestData.getMaxKeys()) > 1000) {
					s3ObjectList.setMaxKeys(GWConstants.DEFAULT_MAX_KEYS);
				} else {
					s3ObjectList.setMaxKeys(s3RequestData.getMaxKeys());
				}
			} catch (NumberFormatException e) {
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		} else {
			s3ObjectList.setMaxKeys(GWConstants.DEFAULT_MAX_KEYS);
		}
		
		s3ObjectList.setContinuationToken(s3RequestData.getContinuationToken());
		s3ObjectList.setDelimiter(s3RequestData.getDelimiter());
		s3ObjectList.setEncodingType(s3RequestData.getEncodingType());
		s3ObjectList.setPrefix(s3RequestData.getPrefix());
		s3ObjectList.setStartAfter(s3RequestData.getStartAfter());
		s3ObjectList.setFetchOwner(s3RequestData.getFetchOwner());

		logger.debug("continuationToken : {}", s3ObjectList.getContinuationToken());
		logger.debug("delimiter : {}", s3ObjectList.getDelimiter());
		logger.debug("prefix : {}", s3ObjectList.getPrefix());
		logger.debug("startAfter : {}", s3ObjectList.getStartAfter());
		logger.debug("maxKeys : {}", s3ObjectList.getMaxKeys());

		s3Parameter.getResponse().setCharacterEncoding(Constants.CHARSET_UTF_8);
		
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

        ObjectListParameter objectListParameter = listObjectV2(bucket, s3ObjectList);
		
		try (Writer writer = s3Parameter.getResponse().getWriter()) {
			s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
			XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
			xmlStreamWriter.writeStartDocument();
			xmlStreamWriter.writeStartElement(GWConstants.LIST_BUCKET_RESULT);
			xmlStreamWriter.writeDefaultNamespace(GWConstants.AWS_XMLNS);
			
			writeSimpleElement(xmlStreamWriter, GWConstants.XML_NAME, bucket);

			String encodingType = s3ObjectList.getEncodingType();
			String prefix = s3ObjectList.getPrefix();
			if (prefix == null) {
				xmlStreamWriter.writeEmptyElement(GWConstants.XML_PREFIX);
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_PREFIX, GWUtils.encodeObjectName(encodingType, prefix));
			}

			writeSimpleElement(xmlStreamWriter, GWConstants.XML_MAX_KEYS, String.valueOf(s3ObjectList.getMaxKeys()));

			if (s3ObjectList.getStartAfter() == null) {
				xmlStreamWriter.writeEmptyElement(GWConstants.XML_START_AFTER);
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_START_AFTER, GWUtils.encodeObjectName(encodingType, s3ObjectList.getStartAfter()));
			}
			
			if (s3ObjectList.getContinuationToken() == null) {
				xmlStreamWriter.writeEmptyElement(GWConstants.XML_CONTINUEATION_TOKEN);
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_CONTINUEATION_TOKEN, GWUtils.encodeObjectName(encodingType, s3ObjectList.getContinuationToken()));
			}

			writeSimpleElement(xmlStreamWriter, GWConstants.XML_KEY_COUNT, String.valueOf(objectListParameter.getObjects().size()));
			
			if (s3ObjectList.getDelimiter() != null) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_DELIMITER, GWUtils.encodeObjectName(encodingType, s3ObjectList.getDelimiter()));
			}

			if (encodingType != null && encodingType.equals(GWConstants.URL)) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_ENCODING_TYPE, encodingType);
			}

			if (objectListParameter.isTruncated()) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_TRUNCATED, GWConstants.XML_TRUE);
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_NEXT_CONTINUATION_TOKEN, GWUtils.encodeObjectName(encodingType, objectListParameter.getNextMarker()));
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_TRUNCATED, GWConstants.XML_FALSE);
			}
			
			for (S3Metadata s3Metadata : objectListParameter.getObjects()) {
				xmlStreamWriter.writeStartElement(GWConstants.XML_CONTENTS);
				writeSimpleElement(xmlStreamWriter, GWConstants.KEY, GWUtils.encodeObjectName(encodingType, s3Metadata.getName()));
				if (s3Metadata.getLastModified() != null) {
					writeSimpleElement(xmlStreamWriter, GWConstants.LAST_MODIFIED, formatDate(s3Metadata.getLastModified()));
				}
				
				if (s3Metadata.getETag() != null) {
					writeSimpleElement(xmlStreamWriter, GWConstants.ETAG, GWUtils.maybeQuoteETag(s3Metadata.getETag()));
				}

				if( !Strings.isNullOrEmpty(s3ObjectList.getFetchOwner()) && s3ObjectList.getFetchOwner().equals(GWConstants.XML_TRUE)) {
					writeOwnerInfini(xmlStreamWriter, s3Metadata.getOwnerId(), s3Metadata.getOwnerName());
				}
				
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_SIZE, s3Metadata.getContentLength().toString());
				writeSimpleElement(xmlStreamWriter, GWConstants.STORAGE_CLASS, s3Metadata.getTier());
				
				xmlStreamWriter.writeEndElement();
			}
			
			for (Entry<String, String> entry : objectListParameter.getCommonPrefixes().entrySet()) {
				xmlStreamWriter.writeStartElement(GWConstants.XML_COMMON_PREFIXES);
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_PREFIX, GWUtils.encodeObjectName(encodingType, entry.getValue()));
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
