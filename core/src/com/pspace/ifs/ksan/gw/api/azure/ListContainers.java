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
import com.pspace.ifs.ksan.gw.utils.AzuUtils;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.identity.S3BucketSimpleInfo;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

public class ListContainers extends AzuRequest {

    public ListContainers(AzuParameter parameter) {
        super(parameter);
        logger = LoggerFactory.getLogger(ListContainers.class);
    }

    @Override
    public void process() throws AzuException {
        logger.info(AzuConstants.LOG_LIST_CONTAINER_START);

        List<S3BucketSimpleInfo> containerList = listContainerSimpleInfo(azuParameter.getUser().getUserName(), azuParameter.getUser().getUserId());

        azuParameter.getResponse().setCharacterEncoding(AzuConstants.CHARSET_UTF_8);
        try {
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            try (Writer writer = azuParameter.getResponse().getWriter()) {
                azuParameter.getResponse().setContentType(AzuConstants.CONTENT_TYPE_XML);
                XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
                xmlStreamWriter.writeStartDocument();
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ENUMERATION_RESULT);
                xmlStreamWriter.writeAttribute(AzuConstants.STORAGE_SERVICE_PROPERTIES, AzuConstants.XML_ATTRIBUTE_SERVICE_ENDPOINT_VALUE + azuParameter.getUserName());
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_PREFIX);
                xmlStreamWriter.writeEndElement();  // End Prefix
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_MAX_RESULT);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_LISTCONTAINER_MAX_RESULT_VALUE);
                xmlStreamWriter.writeEndElement();  // End MaxResults
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CONTAINERS);
    
                for (S3BucketSimpleInfo container : containerList) {
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CONTAINER);
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_NAME);
                    xmlStreamWriter.writeCharacters(container.getBucketName());
                    xmlStreamWriter.writeEndElement();  // end Name
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_PROPERTIES);
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_LAST_MODIFIED);
                    xmlStreamWriter.writeCharacters(formatDate(container.getCreateDate()));
                    xmlStreamWriter.writeEndElement();  // End Last-Modified
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ETAG);
                    xmlStreamWriter.writeCharacters(AzuConstants.ETAG_DEFAULT);
                    xmlStreamWriter.writeEndElement();  // End Etag
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_LEASE_STATUS);
                    xmlStreamWriter.writeCharacters(AzuConstants.LEASE_STATUS_UNLOCKED);
                    xmlStreamWriter.writeEndElement();  // End LeaseStatus
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_LEASE_STATE);
                    xmlStreamWriter.writeCharacters(AzuConstants.LEASE_STATE_AVAILABLE);
                    xmlStreamWriter.writeEndElement();  // End LeaseState
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_HAS_IMMUTABILITY_POLICY);
                    xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_FALSE);
                    xmlStreamWriter.writeEndElement();  // End HasImmutabilityPolicy
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_HAS_LEGALHOLD);
                    xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_FALSE);
                    xmlStreamWriter.writeEndElement();  // End HasLegalHold
                    xmlStreamWriter.writeEndElement();  // End Properties
                    xmlStreamWriter.writeEndElement();  // End Container

                    logger.debug(AzuConstants.LOG_LIST_CONTAINER_NAME_DATE, container.getBucketName(), formatDate(container.getCreateDate()));
                }
                
                xmlStreamWriter.writeEndElement();  // End Containers
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_NEXT_MARKER);
                xmlStreamWriter.writeEndElement();  // End NextMarker
                xmlStreamWriter.writeEndElement();  // End EnumerationResults
                xmlStreamWriter.flush();
            } catch (XMLStreamException | IOException e) {
                PrintStack.logging(logger, e);
                throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
            }
        } catch (RuntimeException e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
        }
        
        azuParameter.getResponse().setStatus(HttpServletResponse.SC_OK);
    }
}

