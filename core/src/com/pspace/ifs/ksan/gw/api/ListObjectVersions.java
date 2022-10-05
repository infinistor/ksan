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
import com.pspace.ifs.ksan.gw.data.DataListObjectVersions;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;

import org.slf4j.LoggerFactory;

public class ListObjectVersions extends S3Request {

	public ListObjectVersions(S3Parameter s3Parameter) {
		super(s3Parameter);
		logger = LoggerFactory.getLogger(ListObjectVersions.class);
	}

	@Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_LIST_OBJECT_VERSIONS_START);
		
		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);
		S3Bucket s3Bucket = new S3Bucket();
		s3Bucket.setBucket(bucket);
		s3Bucket.setUserName(getBucketInfo().getUserName());
		s3Bucket.setCors(getBucketInfo().getCors());
		s3Bucket.setAccess(getBucketInfo().getAccess());
		s3Parameter.setBucket(s3Bucket);

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}
		
		checkGrantBucket(s3Parameter.isPublicAccess(), s3Parameter.getUser().getUserId(), GWConstants.GRANT_READ);
		
		DataListObjectVersions dataListObjectVersions = new DataListObjectVersions(s3Parameter);
		dataListObjectVersions.extract();
		String delimiter = dataListObjectVersions.getDelimiter();
		String encodingType = dataListObjectVersions.getEncodingType();
		String keyMarker = dataListObjectVersions.getKeyMarker();
		String maxKeys = dataListObjectVersions.getMaxKeys();
		String prefix = dataListObjectVersions.getPrefix();
		String versionIdMarker = dataListObjectVersions.getVersionIdMarker();

		S3ObjectList s3ObjectList = new S3ObjectList();
		s3ObjectList.setDelimiter(delimiter);
		s3ObjectList.setEncodingType(encodingType);
		s3ObjectList.setKeyMarker(keyMarker);

		if (!Strings.isNullOrEmpty(maxKeys)) {
			try {
				if (Integer.valueOf(maxKeys) < 0) {
					throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
				} else if (Integer.valueOf(maxKeys) > 1000) {
					s3ObjectList.setMaxKeys(GWConstants.DEFAULT_MAX_KEYS);
				} else {
					s3ObjectList.setMaxKeys(maxKeys);
				}
			} catch (NumberFormatException e) {
				throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
			}
		} else {
			s3ObjectList.setMaxKeys(GWConstants.DEFAULT_MAX_KEYS);
		}

		logger.debug(GWConstants.LOG_LIST_OBJECT_VERSIONS_MAXKEYS, s3ObjectList.getMaxKeys());
		if (Integer.valueOf(s3ObjectList.getMaxKeys()) < 0) {
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
		}
		s3ObjectList.setPrefix(prefix);
		s3ObjectList.setVersionIdMarker(versionIdMarker);

		s3Parameter.getResponse().setCharacterEncoding(GWConstants.CHARSET_UTF_8);
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        
		ObjectListParameter objectListParameter = listObjectVersions(bucket, s3ObjectList);

		try (Writer writer = s3Parameter.getResponse().getWriter()) {
			s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
			XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
			xmlStreamWriter.writeStartDocument();
			xmlStreamWriter.writeStartElement(GWConstants.LIST_VERSIONS_RESULT);
			xmlStreamWriter.writeDefaultNamespace(GWConstants.AWS_XMLNS);
			
			writeSimpleElement(xmlStreamWriter, GWConstants.XML_NAME, bucket);

			if (prefix == null) {
				xmlStreamWriter.writeEmptyElement(GWConstants.XML_PREFIX);
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_PREFIX, GWUtils.encodeBlob(encodingType, prefix));
			}

			writeSimpleElement(xmlStreamWriter, GWConstants.XML_MAX_KEYS, String.valueOf(s3ObjectList.getMaxKeys()));

			if (keyMarker == null) {
				xmlStreamWriter.writeEmptyElement(GWConstants.XML_KEY_MARKER);
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_KEY_MARKER, GWUtils.encodeBlob(encodingType, keyMarker));
			}
			
			if (versionIdMarker == null) {
				xmlStreamWriter.writeEmptyElement(GWConstants.XML_VERSIONID_MARKER);
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_VERSIONID_MARKER, GWUtils.encodeBlob(encodingType, versionIdMarker));
			}
			logger.debug(GWConstants.LOG_LIST_OBJECT_VERSIONS_KEY_COUNT, objectListParameter.getObjects().size());
			writeSimpleElement(xmlStreamWriter, GWConstants.XML_KEY_COUNT, String.valueOf(objectListParameter.getObjects().size()));
			
			if (delimiter != null) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_DELIMITER, GWUtils.encodeBlob(encodingType, delimiter));
			}

			if (encodingType != null && encodingType.equals(GWConstants.URL)) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_ENCODING_TYPE, encodingType);
			}

			if (objectListParameter.isTruncated()) {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_TRUNCATED, GWConstants.XML_TRUE);
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_NEXT_KEY_MARKER, GWUtils.encodeBlob(encodingType, objectListParameter.getNextMarker()));
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_NEXT_VERSIONID_MARKER, GWUtils.encodeBlob(encodingType, objectListParameter.getNextVersion()));
			} else {
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_TRUNCATED, GWConstants.XML_FALSE);
			}
			
			for (S3Metadata s3Metadata : objectListParameter.getObjects()) {
				if (s3Metadata != null) {
					if (s3Metadata.getDeleteMarker() != null) {
						if (s3Metadata.getDeleteMarker().compareTo(GWConstants.OBJECT_TYPE_MARK) == 0) {
							xmlStreamWriter.writeStartElement(GWConstants.XML_DELETE_MARKER);	
						} else {
							xmlStreamWriter.writeStartElement(GWConstants.VERSION);
						}
						
						writeSimpleElement(xmlStreamWriter, GWConstants.KEY, GWUtils.encodeBlob(encodingType, s3Metadata.getName()));
						if (s3Metadata.getLastModified() != null) {
							writeSimpleElement(xmlStreamWriter, GWConstants.LAST_MODIFIED, formatDate(s3Metadata.getLastModified()));
						}
						
						if ( s3Metadata.getETag() != null ) {
							writeSimpleElement(xmlStreamWriter, GWConstants.ETAG, GWUtils.maybeQuoteETag(s3Metadata.getETag()));
						}
						
						writeSimpleElement(xmlStreamWriter, GWConstants.XML_SIZE, s3Metadata.getContentLength().toString());
						writeSimpleElement(xmlStreamWriter, GWConstants.STORAGE_CLASS, s3Metadata.getTier());
						writeSimpleElement(xmlStreamWriter, GWConstants.VERSIONID, s3Metadata.getVersionId());
						writeSimpleElement(xmlStreamWriter, GWConstants.XML_IS_LATEST, s3Metadata.getIsLatest());
						writeOwnerInfini(xmlStreamWriter, s3Metadata.getOwnerId(), s3Metadata.getOwnerName());
						xmlStreamWriter.writeEndElement();

						logger.debug(GWConstants.LOG_LIST_OBJECT_VERSIONS_INFO, s3Metadata.getName(), s3Metadata.getLastModified(), s3Metadata.getVersionId());
					}
				}
			}
			
			for (Entry<String, String> entry : objectListParameter.getCommonPrefixes().entrySet()) {
				xmlStreamWriter.writeStartElement(GWConstants.XML_COMMON_PREFIXES);
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_PREFIX, GWUtils.encodeBlob(encodingType, entry.getValue()));
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
