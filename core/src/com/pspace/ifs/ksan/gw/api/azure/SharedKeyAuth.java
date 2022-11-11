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
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.pspace.ifs.ksan.libs.PrintStack;

import com.google.common.base.Strings;

import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

public class SharedKeyAuth extends AzuRequest {

    public SharedKeyAuth(AzuParameter parameter) {
        super(parameter);
        logger = LoggerFactory.getLogger(ListContainers.class);
    }

    @Override
    public void process() throws AzuException {
        logger.info(AzuConstants.LOG_SHARED_KEY_AUTH_START);
        if (Strings.isNullOrEmpty(S3UserManager.getInstance().getUserByName(azuParameter.getUserName()).getAzureKey())) {
            azuParameter.getResponse().setCharacterEncoding(AzuConstants.CHARSET_UTF_8);
            try {
                XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
                try (Writer writer = azuParameter.getResponse().getWriter()) {
                    azuParameter.getResponse().setContentType(AzuConstants.CONTENT_TYPE_XML);
                    XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
                    xmlStreamWriter.writeStartDocument();
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ERROR);
                    
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CODE);
                    xmlStreamWriter.writeCharacters(AzuConstants.INVALID_OPERATION);
                    xmlStreamWriter.writeEndElement();  // End Code
                    xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_MESSAGE);
                    String message = AzuConstants.INVALID_STORAGE_ACCOUNT + AzuConstants.NEWLINE + AzuConstants.REQUEST_ID + AzuConstants.COLON
                        + azuParameter.getRequest().getHeader(AzuConstants.X_MS_CLIENT_REQUEST_ID) + AzuConstants.NEWLINE + AzuConstants.TIME + AzuConstants.COLON;
                    TimeZone tz = TimeZone.getTimeZone(GWConstants.UTC);
                    DateFormat df = new SimpleDateFormat(AzuConstants.TIME_FORMAT);
                    df.setTimeZone(tz);
                    String nowTime = df.format(new Date());
                    message += nowTime;
                    xmlStreamWriter.writeCharacters(message);
                    xmlStreamWriter.writeEndElement();  // End Message
                    xmlStreamWriter.writeEndElement();  // End Error
                    xmlStreamWriter.flush();
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                    throw new AzuException(AzuErrorCode.SERVER_ERROR, azuParameter);
                }
            } catch (Exception e) {
                PrintStack.logging(logger, e);
            }
            throw new AzuException(AzuErrorCode.INVALID_ACCOUNT, azuParameter);
        } else {
            azuParameter.getResponse().setStatus(HttpServletResponse.SC_OK);
        }
    }
    
}

