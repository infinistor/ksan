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
import com.pspace.ifs.ksan.libs.PrintStack;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

public class GetProperties extends AzuRequest {

    public GetProperties(AzuParameter parameter) {
        super(parameter);
        logger = LoggerFactory.getLogger(GetProperties.class);
    }

    @Override
    public void process() throws AzuException {
        logger.info(AzuConstants.LOG_GET_PROPERTIES_START);

        azuParameter.getResponse().setCharacterEncoding(AzuConstants.CHARSET_UTF_8);
        try {
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            try (Writer writer = azuParameter.getResponse().getWriter()) {
                azuParameter.getResponse().setContentType(AzuConstants.CONTENT_TYPE_XML);
                XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
                xmlStreamWriter.writeStartDocument();
                xmlStreamWriter.writeStartElement(AzuConstants.STORAGE_SERVICE_PROPERTIES);
                
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_LOGGING);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_VERSION);
                xmlStreamWriter.writeCharacters(AzuConstants.VERSION_VALUE);
                xmlStreamWriter.writeEndElement();  // End Version
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_DELETE);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_TRUE);
                xmlStreamWriter.writeEndElement();  // End Delete
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_READ);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_TRUE);
                xmlStreamWriter.writeEndElement();  // End Read
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_WRITE);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_TRUE);
                xmlStreamWriter.writeEndElement();  // End Write
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_RETENTION_POLICY);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ENABLED);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_FALSE);
                xmlStreamWriter.writeEndElement();  // End Enabled
                xmlStreamWriter.writeEndElement();  // End RetentionPolicy
                xmlStreamWriter.writeEndElement();  // End Logging

                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_HOUR_METRICS);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_VERSION);
                xmlStreamWriter.writeCharacters(AzuConstants.VERSION_VALUE);
                xmlStreamWriter.writeEndElement();  // End Version
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ENABLED);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_FALSE);
                xmlStreamWriter.writeEndElement();  // End Enabled
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_RETENTION_POLICY);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ENABLED);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_FALSE);
                xmlStreamWriter.writeEndElement();  // End Enabled
                xmlStreamWriter.writeEndElement();  // End RetentionPolicy
                xmlStreamWriter.writeEndElement();  // End HourMetrics

                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_MINUTE_METRICS);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_VERSION);
                xmlStreamWriter.writeCharacters(AzuConstants.VERSION_VALUE);
                xmlStreamWriter.writeEndElement();  // End Version
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ENABLED);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_FALSE);
                xmlStreamWriter.writeEndElement();  // End Enabled
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_RETENTION_POLICY);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ENABLED);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_FALSE);
                xmlStreamWriter.writeEndElement();  // End Enabled
                xmlStreamWriter.writeEndElement();  // End RetentionPolicy
                xmlStreamWriter.writeEndElement();  // End MinuteMetrics

                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CORS);
                xmlStreamWriter.writeEndElement();  // End Enabled

                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_DEFAULT_SERVICE_VERSION);
                xmlStreamWriter.writeCharacters(AzuConstants.SERVICE_VERSION_VALUE);
                xmlStreamWriter.writeEndElement();  // End DefaultServiceVersion

                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_STATIC_WEBSITE);
                xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ENABLED);
                xmlStreamWriter.writeCharacters(AzuConstants.XML_ELEMENT_VALUE_FALSE);
                xmlStreamWriter.writeEndElement();  // End Enabled
                xmlStreamWriter.writeEndElement();  // End StaticWebsite

                xmlStreamWriter.writeEndElement();  // End StorageServiceProperties
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

