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

import com.pspace.ifs.ksan.gw.data.azure.DataListBlobs;
import com.pspace.ifs.ksan.gw.exception.AzuErrorCode;
import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.AzuUtils;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.libs.identity.ObjectListParameter;
import com.pspace.ifs.ksan.libs.identity.S3ObjectList;

import java.util.Map.Entry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class ListBlobs extends AzuRequest {

    public ListBlobs(AzuParameter parameter) {
        super(parameter);
        logger = LoggerFactory.getLogger(ListBlobs.class);
    }

    @Override
    public void process() throws AzuException {
        logger.info(AzuConstants.LOG_LIST_BLOB_START);

        String containerName = azuParameter.getContainerName();
        String prefix = azuParameter.getPrefix();
        String maxResults = azuParameter.getMaxResults();
        String delimiter = azuParameter.getDelimiter();
        String marker = azuParameter.getMarker();
        String prefixPath = null;

        S3ObjectList s3ObjectList = new S3ObjectList();
		if (!Strings.isNullOrEmpty(maxResults)) {
			try {
				if (Integer.valueOf(maxResults) <= 0) {
					throw new AzuException(AzuErrorCode.BAD_REQUEST, azuParameter);
				} else if (Integer.valueOf(maxResults) > 1000) {
					s3ObjectList.setMaxKeys(GWConstants.DEFAULT_MAX_KEYS);
				} else {
					s3ObjectList.setMaxKeys(maxResults);
				}
			} catch (NumberFormatException e) {
				throw new AzuException(AzuErrorCode.BAD_REQUEST, azuParameter);
			}
		} else {
			s3ObjectList.setMaxKeys(GWConstants.DEFAULT_MAX_KEYS);
		}

		s3ObjectList.setDelimiter(delimiter);
		s3ObjectList.setEncodingType(AzuConstants.CHARSET_UTF_8);
		s3ObjectList.setMarker(marker);
		s3ObjectList.setPrefix(prefix);
        logger.debug("container : {}", containerName);
        logger.debug("maxResults : {}", s3ObjectList.getMaxKeys());
		logger.debug("delimiter : {}", s3ObjectList.getDelimiter());
		logger.debug("marker : {}", s3ObjectList.getMarker());
		logger.debug("prefix : {}", s3ObjectList.getPrefix());

		ObjectListParameter objectListParameter = listObject(containerName, s3ObjectList);

        azuParameter.getResponse().setCharacterEncoding(AzuConstants.CHARSET_UTF_8);
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        try (Writer writer = azuParameter.getResponse().getWriter()) {
            azuParameter.getResponse().setContentType(AzuConstants.CONTENT_TYPE_XML);
            XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
            xmlStreamWriter.writeStartDocument();
            xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ENUMERATION_RESULT);
            xmlStreamWriter.writeAttribute(AzuConstants.STORAGE_SERVICE_PROPERTIES, AzuConstants.XML_ATTRIBUTE_SERVICE_ENDPOINT_VALUE + azuParameter.getUserName());
            xmlStreamWriter.writeAttribute(AzuConstants.XML_ATTRIBUTE_CONTAINER_NAME, azuParameter.getContainerName());
            xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_PREFIX);
            if (!Strings.isNullOrEmpty(prefix)) {
                xmlStreamWriter.writeCharacters(prefix);
                logger.debug("prefix : {}", prefix);
            }
            xmlStreamWriter.writeEndElement();  // End Prefix
            xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_MARKER);
            if (!Strings.isNullOrEmpty(marker)) {
                xmlStreamWriter.writeCharacters(marker);
            }
            xmlStreamWriter.writeEndElement();  // End Marker
            xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_MAX_RESULT);
            xmlStreamWriter.writeCharacters(maxResults);
            xmlStreamWriter.writeEndElement();  // End MaxResults
            xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_DELIMITER);
            if (!Strings.isNullOrEmpty(delimiter)) {
                xmlStreamWriter.writeCharacters(delimiter);
            }
            xmlStreamWriter.writeEndElement();  // End Delimiter
            xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_BLOBS);

            for (S3Metadata s3Metadata : objectListParameter.getObjects()) {
                String blobName = s3Metadata.getName();
                logger.info("bolb : {}", blobName);

                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_BLOB);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_NAME);
                xmlStreamWriter.writeCharacters(blobName);
                xmlStreamWriter.writeEndElement();  // end Name
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_PROPERTIES);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CREATION_TIME);
                xmlStreamWriter.writeCharacters(formatDate(s3Metadata.getCreationDate()));
                xmlStreamWriter.writeEndElement();  // End Creation-Time
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_LAST_MODIFIED);
                xmlStreamWriter.writeCharacters(formatDate(s3Metadata.getLastModified()));
                xmlStreamWriter.writeEndElement();  // End Last-Modified
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ETAG);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_ETAG);
                xmlStreamWriter.writeEndElement();  // End Etag
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CONTENT_LENGTH);
                xmlStreamWriter.writeCharacters(Long.toString(s3Metadata.getContentLength()));
                xmlStreamWriter.writeEndElement();  // End Content-Length
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CONTENT_TYPE);
                xmlStreamWriter.writeCharacters(s3Metadata.getContentType());
                xmlStreamWriter.writeEndElement();  // End Content-Type
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CONTENT_ENCODING);
                xmlStreamWriter.writeEndElement();  // End Content-Encoding
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CONTENT_LANGUAGE);
                xmlStreamWriter.writeEndElement();  // End Content-Language
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CONTENT_MD5);
                xmlStreamWriter.writeCharacters(s3Metadata.getETag());
                xmlStreamWriter.writeEndElement();  // End Content-MD5
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CONTENT_DISPOSITION);
                xmlStreamWriter.writeEndElement();  // End Content-Disposition
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CACHE_CONTROL);
                xmlStreamWriter.writeEndElement();  // End Cache-Control/
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_BLOB_TYPE);
                xmlStreamWriter.writeCharacters(AzuConstants.BLOB_TYPE_BLOCKBLOB);
                xmlStreamWriter.writeEndElement();  // End BlobType
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_LEASE_STATUS);
                xmlStreamWriter.writeCharacters(AzuConstants.LEASE_STATUS_UNLOCKED);
                xmlStreamWriter.writeEndElement();  // End LeaseStatus
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_LEASE_STATE);
                xmlStreamWriter.writeCharacters(AzuConstants.LEASE_STATE_AVAILABLE);
                xmlStreamWriter.writeEndElement();  // End LeaseState
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_SERVER_ENCRYPTED);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_TRUE);
                xmlStreamWriter.writeEndElement();  // End ServerEncrypted
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ACCESS_TIER);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_HOT);
                xmlStreamWriter.writeEndElement();  // End AccessTier
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ACCESS_TIER_INFERRED);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_TRUE);
                xmlStreamWriter.writeEndElement();  // End AccessTierInferred
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ACCESS_TIER_CHANGE_TIME);
                xmlStreamWriter.writeCharacters(formatDate(s3Metadata.getLastModified()));
                xmlStreamWriter.writeEndElement();  // End AccessTierChangeTime
                xmlStreamWriter.writeEndElement();  // End Properties
                xmlStreamWriter.writeEndElement();  // End Blob
            }

            for (Entry<String, String> entry : objectListParameter.getCommonPrefixes().entrySet()) {
                logger.info("dir : {}", entry.getValue());
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_BLOB_PREFIX);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_NAME);
                xmlStreamWriter.writeCharacters(entry.getValue());
                xmlStreamWriter.writeEndElement();  // end Name
                xmlStreamWriter.writeEndElement();  // end BlobPrefix
            }

            xmlStreamWriter.writeEndElement();  // End Blobs
            xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_NEXT_MARKER);
            xmlStreamWriter.writeEndElement();  // End NextMarker
            xmlStreamWriter.writeEndElement();  // End EnumerationResults
            xmlStreamWriter.flush();
        } catch (XMLStreamException | IOException e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        }

        azuParameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
}

