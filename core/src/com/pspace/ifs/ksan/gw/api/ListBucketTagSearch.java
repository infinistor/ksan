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
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.gw.identity.S3Bucket;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;

import org.slf4j.LoggerFactory;

public class ListBucketTagSearch extends S3Request {

    public ListBucketTagSearch(S3Parameter s3Parameter) {
        super(s3Parameter);
        logger = LoggerFactory.getLogger(ListBucketTagSearch.class);
    }
    
    @Override
	public void process() throws GWException {
		logger.info(GWConstants.LOG_LIST_BUCKET_TAG_SEARCH_START);

		String bucket = s3Parameter.getBucketName();
		initBucketInfo(bucket);

		GWUtils.checkCors(s3Parameter);

		if (s3Parameter.isPublicAccess() && GWUtils.isIgnorePublicAcls(s3Parameter)) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}

		logger.info("bucket policy : {}", s3Parameter.getBucket().getPolicy());
		if (!checkPolicyBucket(GWConstants.ACTION_LIST_BUCKET, s3Parameter)) {
			checkGrantBucket(false, GWConstants.GRANT_READ);
		}

		String encodingType = s3RequestData.getEncodingType();
		String maxKeys = s3RequestData.getMaxkeys();
		int max = 1000;

		if (!Strings.isNullOrEmpty(maxKeys)) {
			max = Integer.parseInt(maxKeys);
		}

		s3Parameter.getResponse().setCharacterEncoding(Constants.CHARSET_UTF_8);
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

		List<Metadata> tagList = listBucketTags(bucket, s3RequestData.getTag(), max);

		logger.info("tag : {}", s3RequestData.getTag());
		logger.info("tagList count : {}", tagList.size());
		try (Writer writer = s3Parameter.getResponse().getWriter()) {
			s3Parameter.getResponse().setContentType(GWConstants.XML_CONTENT_TYPE);
			XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
			xmlStreamWriter.writeStartDocument();
			xmlStreamWriter.writeStartElement(GWConstants.LIST_BUCKET_TAG_SEARCH);
			xmlStreamWriter.writeDefaultNamespace(GWConstants.AWS_XMLNS);
			
			for (Metadata meta : tagList) {
				S3Metadata s3Metadata = new S3Metadata();
				ObjectMapper jsonMapper = new ObjectMapper();
				
				try {
					s3Metadata = jsonMapper.readValue(meta.getMeta(), S3Metadata.class);
				} catch (Exception e) {
					PrintStack.logging(logger, e);
					throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
				}

				xmlStreamWriter.writeStartElement(GWConstants.XML_CONTENTS);
				logger.debug("meta info : {}, {}, {}, {}, {}, {}, {}, {}", meta.getPath(), formatDate(s3Metadata.getLastModified()), s3Metadata.getETag(), s3Metadata.getContentLength(), s3Metadata.getTier(), meta.getTag(), s3Metadata.getOwnerId(), s3Metadata.getOwnerName());

				writeSimpleElement(xmlStreamWriter, GWConstants.KEY, GWUtils.encodeObjectName(encodingType, meta.getPath()));
				if (s3Metadata.getLastModified() != null) {
					writeSimpleElement(xmlStreamWriter, GWConstants.LAST_MODIFIED, formatDate(s3Metadata.getLastModified()));
				}
				
				if (s3Metadata.getETag() != null) {
					writeSimpleElement(xmlStreamWriter, GWConstants.ETAG, GWUtils.maybeQuoteETag(s3Metadata.getETag()));
				}
				
				writeSimpleElement(xmlStreamWriter, GWConstants.XML_SIZE, s3Metadata.getContentLength().toString());
				writeSimpleElement(xmlStreamWriter, GWConstants.STORAGE_CLASS, s3Metadata.getTier());
				// writeSimpleElement(xmlStreamWriter, GWConstants.XML_TAG, meta.getTag());
				writeOwnerInfini(xmlStreamWriter, s3Metadata.getOwnerId(), s3Metadata.getOwnerName());
				xmlStreamWriter.writeEndElement();
			}
			
			xmlStreamWriter.writeEndElement();
			xmlStreamWriter.flush();
		} catch (IOException | XMLStreamException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}
	}
}
