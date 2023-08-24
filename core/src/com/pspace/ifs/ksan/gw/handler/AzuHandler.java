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
package com.pspace.ifs.ksan.gw.handler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;

import org.apache.http.HttpHeaders;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.api.azure.*;
import com.pspace.ifs.ksan.gw.exception.AzuErrorCode;
import com.pspace.ifs.ksan.gw.exception.AzuException;
import com.pspace.ifs.ksan.gw.identity.AzuParameter;
import com.pspace.ifs.ksan.gw.sign.AzuSigning;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.libs.PrintStack;

public class AzuHandler {
    private static final Logger logger = LoggerFactory.getLogger(AzuHandler.class);
    private final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
	private AzuRequestFactory azuRequestFactory;
    private String requestId;

    public AzuHandler() {
		azuRequestFactory = new AzuRequestFactory();
    }

    public final void doHandle(Request baseRequest, HttpServletRequest request, HttpServletResponse response, InputStream is) throws AzuException {
        long requestSize = 0L;
		String method = request.getMethod().toUpperCase();
		requestSize = method.length();

		String uri = request.getRequestURI();
		requestSize += uri.length();

		// long startTime = System.currentTimeMillis();

		AzuParameter azuParameter = new AzuParameter();
		
		logger.info(AzuConstants.LOG_PRE_URI, uri);
		uri = removeDuplicateRoot(uri);
		logger.info(AzuConstants.LOG_URI, uri);

		logger.info(AzuConstants.LOG_CLIENT_ADDRESS, request.getRemoteAddr());
		logger.info(AzuConstants.LOG_CLIENT_HOST, request.getRemoteHost());
		logger.info(AzuConstants.LOG_METHOD, method);

		for (String parameter : Collections.list(request.getParameterNames())) {
			logger.info(AzuConstants.LOG_PARAMETER, parameter, Strings.nullToEmpty(request.getParameter(parameter)));
			if (parameter.equalsIgnoreCase(AzuConstants.PARAMETER_COMP)) {
				azuParameter.setComp(request.getParameter(parameter));
			} else if (parameter.equalsIgnoreCase(AzuConstants.PARAMETER_RESTYPE)) {
				azuParameter.setRestype(request.getParameter(parameter));
			} else if (parameter.equalsIgnoreCase(AzuConstants.PARAMETER_PREFIX)) {
				azuParameter.setPrefix(request.getParameter(parameter));
			} else if (parameter.equalsIgnoreCase(AzuConstants.PARAMETER_DELIMITER)) {
				azuParameter.setDelimiter(request.getParameter(parameter));
			} else if (parameter.equalsIgnoreCase(AzuConstants.PARAMETER_INCLUDE)) {
				azuParameter.setInclude(request.getParameter(parameter));
			} else if (parameter.equalsIgnoreCase(AzuConstants.PARAMETER_MAX_RESULTS)) {
				azuParameter.setMaxresults(request.getParameter(parameter));
			} else if (parameter.equalsIgnoreCase(AzuConstants.PARAMETER_TIMEOUT)) {
				azuParameter.setTimeout(request.getParameter(parameter));
			} else if (parameter.equalsIgnoreCase(AzuConstants.PARAMETER_MARKER)) {
                azuParameter.setMarker(request.getParameter(parameter));
            }
			requestSize += parameter.length();
			if (!Strings.isNullOrEmpty(request.getParameter(parameter))) {
				requestSize += request.getParameter(parameter).length();
			}
		}

		for (String headerName : Collections.list(request.getHeaderNames())) {
			for (String headerValue : Collections.list(request.getHeaders(headerName))) {
				logger.info(AzuConstants.LOG_HEADER, headerName, Strings.nullToEmpty(headerValue));
                if (headerName.equalsIgnoreCase(AzuConstants.X_MS_CLIENT_REQUEST_ID)) {
                    requestId = Strings.nullToEmpty(headerValue);
                }

				requestSize += headerName.length();
				if (!Strings.isNullOrEmpty(headerValue)) {
					requestSize += headerValue.length();
				}
			}
		}

		// make request id
		// String requestID = UUID.randomUUID().toString().substring(24).toUpperCase();
		String userName = "";
		String containerName = "";
		String blobName = "";
		String pathCategory = "";

		try {
			String[] path = uri.split(AzuConstants.SEPARATOR, 4);
			try {
				for (int i = 0; i < path.length; i++) {
					path[i] = URLDecoder.decode(path[i], AzuConstants.CHARSET_UTF_8);
					logger.info(AzuConstants.LOG_PATH, i, path[i]);
				}
			} catch (UnsupportedEncodingException e) {
				PrintStack.logging(logger, e);
				throw new AzuException(AzuErrorCode.BAD_REQUEST, null);
			}
	
			userName = path[1];
			if (path.length > 2) {
				containerName = path[2];
				// $logs, $blobchangfeed
				if (containerName.equalsIgnoreCase(AzuConstants.REQUEST_LOGS) || containerName.equalsIgnoreCase(AzuConstants.REQUEST_BLOCK_CHANGE_FEED)) {
					logger.info(AzuConstants.LOG_LOGS_BLOB_CHANGE_FEED, containerName);
					response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					return;
				}
			}
			if (path.length > 3) {
				blobName = path[3];
				if (!File.separator.equals(AzuConstants.SEPARATOR)) {
					blobName = blobName.replaceAll(AzuConstants.SEPARATOR, Matcher.quoteReplacement(File.separator));
				}
			}
			
			if (path.length == 2) {
				pathCategory = AzuConstants.PATH_CATEGORY_ROOT;
			} else if (path.length == 3) {
				pathCategory = AzuConstants.PATH_CATEGORY_CONTAINER;
			} else if (path.length == 4) {
				pathCategory = AzuConstants.PATH_CATEGORY_BLOB;
			}
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			throw new AzuException(AzuErrorCode.SERVER_ERROR, null);
		}
		
		azuParameter.setMethod(method);
		azuParameter.setRequest(request);
		azuParameter.setResponse(response);
		azuParameter.setInputStream(is);
		azuParameter.setUserName(userName);
		azuParameter.setContainerName(containerName);
		azuParameter.setBlobName(blobName);
		azuParameter.setPathCategory(pathCategory);

		try {
            azuParameter.setUser(new AzuSigning(azuParameter).getUser());
		} catch (Exception e) {
			writeSigningError(request, response);
		}

		if (azuParameter.getComp().equalsIgnoreCase(AzuConstants.COMP_PROPERTIES) && azuParameter.getRestype().equalsIgnoreCase(AzuConstants.RESTYPE_ACCOUNT)) {
			logger.info("request : account, send SC_OK");
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}
		
		AzuRequest azuRequest = azuRequestFactory.createRequest(azuParameter);
		azuRequest.process();
    }

    private String removeDuplicateRoot(String s) {
		boolean find = false;
		int sLength = s.length();
		StringBuilder resultStr = new StringBuilder();
		for (int i = 0; i < sLength; i++) {
			char tempChar = s.charAt(i);
			int tempVal = (int) tempChar;

			if (tempVal == AzuConstants.SEPARATOR_CHAR) {
				if (find == true) {
					continue;
				}

				find = true;
			} else {
				find = false;
			}

			resultStr.append(tempChar);
		}
		return resultStr.toString();
	}

	private void writeSigningError(HttpServletRequest request, HttpServletResponse response) {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN, "Server failed to authenticate the request. Make sure the value of the Authorization header is formed correctly including the signature.");
		response.setCharacterEncoding(AzuConstants.CHARSET_UTF_8);
		try {
			XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
			try (Writer writer = response.getWriter()) {
				response.setContentType(AzuConstants.CONTENT_TYPE_XML);
				XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(writer);
				xmlStreamWriter.writeStartDocument();
				xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_ERROR);
				
				xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_CODE);
				xmlStreamWriter.writeCharacters("AuthorizationFailure");
				xmlStreamWriter.writeEndElement();  // End Code
				xmlStreamWriter.writeStartElement(AzuConstants.XML_ELEMENT_MESSAGE);
				String message = "Server failed to authenticate the request. Make sure the value of the Authorization header is formed correctly including the signature.\n" + "RequestId:" + request.getHeader(AzuConstants.X_MS_CLIENT_REQUEST_ID) + "\nTime:";
				TimeZone tz = TimeZone.getTimeZone(GWConstants.UTC);
				DateFormat df = new SimpleDateFormat(AzuConstants.TIME_FORMAT);
				df.setTimeZone(tz);
				String nowTime = df.format(new Date());
				message += nowTime;
				xmlStreamWriter.writeEndElement();  // End Message
				xmlStreamWriter.writeEndElement();  // End Error
				xmlStreamWriter.flush();
			} catch (Exception e) {
				PrintStack.logging(logger, e);
			}
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}
	}

    protected final void sendSimpleErrorResponse(
			HttpServletRequest request, HttpServletResponse response,
			AzuErrorCode code, String message,
			Map<String, String> elements, AzuParameter parameter) throws IOException {

		response.setStatus(code.getHttpStatusCode());

		if (request.getMethod().equals("HEAD")) {
			// The HEAD method is identical to GET except that the server MUST
			// NOT return a message-body in the response.
			return;
		}

		try (Writer writer = response.getWriter()) {
			response.setContentType(AzuConstants.CONTENT_TYPE_XML);
			XMLStreamWriter xml = xmlOutputFactory.createXMLStreamWriter(writer);
			xml.writeStartDocument();
			xml.writeStartElement(AzuConstants.XML_ELEMENT_ERROR);

			writeSimpleElement(xml, AzuConstants.XML_ELEMENT_CODE, "400");
			writeSimpleElement(xml, AzuConstants.XML_ELEMENT_MESSAGE, message);

			// for (Map.Entry<String, String> entry : elements.entrySet()) {
			// 	writeSimpleElement(xml, entry.getKey(), entry.getValue());
			// }

			writeSimpleElement(xml, AzuConstants.REQUEST_ID, requestId);

			xml.writeEndElement();
			xml.flush();
		} catch (XMLStreamException xse) {
			throw new IOException(xse);
		}
	}

    private void sendException(HttpServletRequest request, HttpServletResponse response, AzuException se) throws IOException {
		sendSimpleErrorResponse(
			request, 
			response,
			se.getError(), 
			se.getMessage(), 
			se.getElements(),
			se.getAZUParameter());
	}
    
    private void writeSimpleElement(XMLStreamWriter xml, String elementName, String characters) throws XMLStreamException {
		xml.writeStartElement(elementName);
		xml.writeCharacters(characters);
		xml.writeEndElement();
	}
}
