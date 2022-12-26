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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.common.base.Strings;
import com.pspace.ifs.ksan.gw.api.S3Request;
import com.pspace.ifs.ksan.gw.api.S3RequestFactory;
import com.pspace.ifs.ksan.gw.db.GWDB;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagerHelper;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
import com.pspace.ifs.ksan.gw.object.osdclient.OSDClientManager;
import com.pspace.ifs.ksan.gw.sign.S3Signing;
import com.pspace.ifs.ksan.gw.utils.AsyncHandler;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.objmanager.ObjManager;

import org.apache.http.HttpHeaders;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GWHandler {
    private static final Logger logger = LoggerFactory.getLogger(GWHandler.class);
    private final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    private final long maxFileSize;
    private final int maxTimeSkew;
    // private final String virtualHost;
    // private final String notifyHost;
    // private final String proxyHost;
	private S3RequestFactory s3RequestFactory;
	

    public GWHandler() {
        maxFileSize = GWConfig.getInstance().getMaxFileSize();
        maxTimeSkew = (int)GWConfig.getInstance().getMaxTimeSkew();
        // virtualHost = config.virtualHost();

		s3RequestFactory = new S3RequestFactory();
    }

    public final void doHandle(Request baseRequest, HttpServletRequest request, HttpServletResponse response, InputStream is) throws GWException {
		if (GWConfig.getInstance().isNoOperation()) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}
		
        long requestSize = 0L;
		String method = request.getMethod();
		requestSize = method.length();

		String uri = request.getRequestURI();
		requestSize += uri.length();

		long startTime = System.currentTimeMillis();
		
		logger.info(GWConstants.LOG_GWHANDLER_PREURI, uri);
		uri = removeDuplicateRoot(uri);
		logger.info(GWConstants.LOG_GWHANDLER_URI, uri);

		logger.info(GWConstants.LOG_GWHANDLER_CLIENT_ADDRESS, request.getRemoteAddr());
		logger.info(GWConstants.LOG_GWHANDLER_CLIENT_HOST, request.getRemoteHost());
		logger.info(GWConstants.LOG_GWHANDLER_METHOD, method);

		for (String parameter : Collections.list(request.getParameterNames())) {
			logger.info(GWConstants.LOG_GWHANDLER_PARAMETER, parameter, Strings.nullToEmpty(request.getParameter(parameter)));
			requestSize += parameter.length();
			if (!Strings.isNullOrEmpty(request.getParameter(parameter))) {
				requestSize += request.getParameter(parameter).length();
			}
		}

		for (String headerName : Collections.list(request.getHeaderNames())) {
			for (String headerValue : Collections.list(request.getHeaders(headerName))) {
				logger.info(GWConstants.LOG_GWHANDLER_HEADER, headerName, Strings.nullToEmpty(headerValue));
				requestSize += headerName.length();
				if (!Strings.isNullOrEmpty(headerValue)) {
					requestSize += headerValue.length();
				}
			}
		}

		// make request id
		String requestID = UUID.randomUUID().toString().substring(24).toUpperCase();

		String[] path = uri.split(GWConstants.SLASH, 3);
		try {
			for (int i = 0; i < path.length; i++) {
				path[i] = URLDecoder.decode(path[i], Constants.CHARSET_UTF_8);
				logger.info(GWConstants.LOG_GWHANDLER_PATH, i, path[i]);
			}
		} catch (UnsupportedEncodingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.BAD_REQUEST, null);
		}

		String pathCategory = GWConstants.EMPTY_STRING;
		if (uri.equals(GWConstants.SLASH)) {
			pathCategory = GWConstants.CATEGORY_ROOT;
		} else if (path.length <= 2 || path[2].isEmpty()) {
			pathCategory = GWConstants.CATEGORY_BUCKET;
		} else {
			pathCategory = GWConstants.CATEGORY_OBJECT;
		}

		S3Parameter s3Parameter = new S3Parameter();
		s3Parameter.setURI(uri);
		s3Parameter.setRequestSize(requestSize);
		s3Parameter.setRequestID(requestID);
		s3Parameter.setRequest(request);
		s3Parameter.setResponse(response);
		s3Parameter.setInputStream(is);
		if (!Strings.isNullOrEmpty(path[1])) {
			s3Parameter.setBucketName(path[1]);
		}
		if (path.length == 3 && !Strings.isNullOrEmpty(path[2])) {
			s3Parameter.setObjectName(path[2]);
		}
		s3Parameter.setMethod(method);
		s3Parameter.setStartTime(startTime);
		s3Parameter.setPathCategory(pathCategory);
		s3Parameter.setMaxFileSize(maxFileSize);
		s3Parameter.setMaxTimeSkew(maxTimeSkew);
		s3Parameter.setRemoteHost(request.getRemoteHost());
		s3Parameter.setRequestURI(request.getRequestURI());
		s3Parameter.setReferer(request.getHeader(HttpHeaders.REFERER));
		s3Parameter.setUserAgent(request.getHeader(HttpHeaders.USER_AGENT));
		s3Parameter.setAuthorization(request.getHeader(HttpHeaders.AUTHORIZATION));
		s3Parameter.setxAmzAlgorithm(request.getParameter(GWConstants.X_AMZ_ALGORITHM));
		s3Parameter.setHostName(request.getHeader(HttpHeaders.HOST));
		s3Parameter.setHostID(request.getHeader(GWConstants.X_AMZ_ID_2));
		s3Parameter.setRemoteAddr(!Strings.isNullOrEmpty(request.getHeader(GWConstants.X_FORWARDED_FOR)) ? request.getHeader(GWConstants.X_FORWARDED_FOR) : request.getRemoteAddr());

		S3Signing s3signing = new S3Signing(s3Parameter);
		if (request.getHeader(HttpHeaders.AUTHORIZATION) == null 
		 	&& request.getParameter(GWConstants.X_AMZ_ALGORITHM) == null 
			&& request.getParameter(GWConstants.AWS_ACCESS_KEY_ID) == null) {
			
			if (s3Parameter.getPathCategory().equals(GWConstants.CATEGORY_ROOT)) {
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}
			
			s3Parameter = s3signing.publicvalidation();
			s3Parameter.setPublicAccess(true);
			s3Parameter.setAdmin(false);
		} else if (request.getHeader(GWConstants.X_IFS_ADMIN) != null) {
			s3Parameter = s3signing.validation(true);
			s3Parameter.setAdmin(true);
		} else {
			s3Parameter = s3signing.validation(false);
			s3Parameter.setPublicAccess(false);
			s3Parameter.setAdmin(false);
		}

		S3Request s3Request = null;
		if (s3Parameter.isAdmin()) {
			logger.info(GWConstants.LOG_GWHANDLER_ADMIN_MOTHOD_CATEGORY, s3Parameter.getMethod(), s3Parameter.getPathCategory());
		} else {
			logger.info(GWConstants.LOG_GWHANDLER_MOTHOD_CATEGORY, s3Parameter.getMethod(), s3Parameter.getPathCategory());
		}
		
		s3Request = s3RequestFactory.createS3Request(s3Parameter);
		
		if (GWConfig.getInstance().isPREV()) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		s3Request.process();
		s3Parameter.setStatusCode(response.getStatus());
		
		if (GWConfig.getInstance().isLogging()) {
			AsyncHandler.s3logging(s3Parameter);
		}
    }

    private String removeDuplicateRoot(String s) {
		boolean find = false;
		int sLength = s.length();
		StringBuilder resultStr = new StringBuilder();
		for (int i = 0; i < sLength; i++) {
			char tempChar = s.charAt(i);
			int tempVal = (int) tempChar;

			if(tempVal == GWConstants.CHAR_SLASH) {
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

	protected final void sendSimpleErrorResponse(
			HttpServletRequest request, HttpServletResponse response,
			GWErrorCode code, String message,
			Map<String, String> elements, S3Parameter s3Parameter) throws IOException {

		response.setStatus(code.getHttpStatusCode());

		if (request.getMethod().equals(GWConstants.METHOD_HEAD)) {
			// The HEAD method is identical to GET except that the server MUST
			// NOT return a message-body in the response.
			return;
		}

		try (Writer writer = response.getWriter()) {
			response.setContentType(GWConstants.XML_CONTENT_TYPE);
			XMLStreamWriter xml = xmlOutputFactory.createXMLStreamWriter(writer);
			xml.writeStartDocument();
			xml.writeStartElement(GWConstants.STRING_ERROR);

			writeSimpleElement(xml, GWConstants.CODE, code.getErrorCode());
			writeSimpleElement(xml, GWConstants.MESSAGE, message);

			for (Map.Entry<String, String> entry : elements.entrySet()) {
				writeSimpleElement(xml, entry.getKey(), entry.getValue());
			}

			writeSimpleElement(xml, GWConstants.REQUEST_ID, GWConstants.FAKE_REQUEST_ID);

			xml.writeEndElement();
			xml.flush();
		} catch (XMLStreamException xse) {
			throw new IOException(xse);
		}

		if (GWConfig.getInstance().isLogging()) {
			AsyncHandler.s3logging(s3Parameter);
		}
	}

	private void sendS3Exception(HttpServletRequest request, HttpServletResponse response, GWException se) throws IOException {
		sendSimpleErrorResponse(
			request, 
			response,
			se.getError(), 
			se.getMessage(), 
			se.getElements(),
			se.getS3Parameter());
	}
    
    private void writeSimpleElement(XMLStreamWriter xml, String elementName, String characters) throws XMLStreamException {
		xml.writeStartElement(elementName);
		xml.writeCharacters(characters);
		xml.writeEndElement();
	}
}
