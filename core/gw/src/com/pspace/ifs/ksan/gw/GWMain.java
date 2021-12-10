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
package com.pspace.ifs.ksan.gw;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.api.S3Request;
import com.pspace.ifs.ksan.gw.api.S3RequestFactory;
import com.pspace.ifs.ksan.gw.db.GWDB;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagerHelper;
import com.pspace.ifs.ksan.gw.object.osdclient.OSDClientManager;
import com.pspace.ifs.ksan.gw.sign.S3Signing;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.AsyncHandler;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@WebServlet(GWConstants.SLASH)
public class GWMain extends HttpServlet {
	GWConfig s3Config;
	S3RequestFactory s3RequestFactory;
	private final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
	static final Logger logger = LoggerFactory.getLogger(GWMain.class);

	public GWMain() {
		super();
	}

	@Override
	public void init() throws ServletException {
		super.init();

		logger.info(GWConstants.LOG_GWMAIN_INIT);

		try {
			s3Config = GWConfig.getInstance();
			s3Config.configure(GWConstants.CONFIG_PATH);
		} catch (URISyntaxException | GWException e) {
			PrintStack.logging(logger, e);
		}

		s3RequestFactory = new S3RequestFactory();

		GWDB s3DB = GWUtils.getDBInstance();
		try {
			s3DB.init(s3Config.dbHost(), s3Config.dbPort(), s3Config.database(), s3Config.dbUser(), s3Config.dbPass(), s3Config.dbPoolSize());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}

		try {
			OSDClientManager.getInstance().init(s3Config.osdPort(), s3Config.osdClientCount());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}

		try {
			ObjManagerHelper.getInstance().init(s3Config.objManagerCount());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}
	}

	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			handler(request, response);
		} catch (NoSuchAlgorithmException | IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (GWException e) {
			sendS3Exception(request, response, e);
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			handler(request, response);
		} catch (NoSuchAlgorithmException | IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (GWException e) {
			sendS3Exception(request, response, e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)	throws ServletException, IOException {
		try {
			handler(request, response);
		} catch (NoSuchAlgorithmException | IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (GWException e) {
			sendS3Exception(request, response, e);
		}


	}

	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			handler(request, response);
		} catch (NoSuchAlgorithmException | IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (GWException e) {
			sendS3Exception(request, response, e);
		}
		
	}

	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			handler(request, response);
		} catch (NoSuchAlgorithmException | IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (GWException e) {
			sendS3Exception(request, response, e);
		}
	}

	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			handler(request, response);
		} catch (NoSuchAlgorithmException | IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (GWException e) {
			sendS3Exception(request, response, e);
		}
	}

	private void handler(HttpServletRequest request, HttpServletResponse response) throws GWException, ServletException, IOException, NoSuchAlgorithmException {
		S3Parameter s3Parameter = s3Init(request, response);

		if (request.getHeader(HttpHeaders.AUTHORIZATION) == null 
			&& request.getParameter(GWConstants.X_AMZ_ALGORITHM) == null 
			&& request.getParameter(GWConstants.AWS_ACCESS_KEY_ID) == null) {
			if (s3Parameter.getPathCategory().equals(GWConstants.REQUEST_ROOT)) {
				throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
			}

			S3Signing s3signing = new S3Signing(s3Parameter);
			s3Parameter = s3signing.publicvalidation();
			s3Parameter.setPublicAccess(true);
		} else {
			S3Signing s3signing = new S3Signing(s3Parameter);
			s3Parameter = s3signing.validation();
			s3Parameter.setPublicAccess(false);
		}

		logger.info(GWConstants.LOG_GWMAIN_MOTHOD_CATEGORY, s3Parameter.getMethod(), s3Parameter.getPathCategory());
		S3Request s3Request = s3RequestFactory.createS3Request(s3Parameter);
		s3Request.process();
		s3Parameter.setStatusCode(response.getStatus());
		AsyncHandler.s3logging(s3Parameter);
	}

	private S3Parameter s3Init(HttpServletRequest request, HttpServletResponse response) {
		String method = request.getMethod();
		String uri = request.getRequestURI();
		long startTime = System.currentTimeMillis();
		long requestSize = 0L;

		requestSize += method.length();
		requestSize += uri.length();

		logger.info(GWConstants.LOG_GWMAIN_PREURI, uri);
		uri = removeDuplicateRoot(uri);
		logger.info(GWConstants.LOG_GWMAIN_URI, uri);
		logger.info(GWConstants.LOG_GWMAIN_CLIENT_ADDRESS, request.getRemoteAddr());
		logger.info(GWConstants.LOG_GWMAIN_CLIENT_HOST, request.getRemoteHost());
		logger.info(GWConstants.LOG_GWMAIN_METHOD, request.getMethod());

		for (String headerName : Collections.list(request.getHeaderNames())) {
			for (String headerValue : Collections.list(request.getHeaders(headerName))) {
				logger.info(GWConstants.LOG_GWMAIN_HEADER, headerName, Strings.nullToEmpty(headerValue));

				requestSize += headerName.length();
				if (!Strings.isNullOrEmpty(headerValue)) {
					requestSize += headerValue.length();
				}
			}
		}

		for (String parameter : Collections.list(request.getParameterNames())) {
			logger.info(GWConstants.LOG_GWMAIN_PARAMETER, parameter, Strings.nullToEmpty(request.getParameter(parameter)));

			requestSize += parameter.length();
			if (!Strings.isNullOrEmpty(request.getParameter(parameter))) {
				requestSize += request.getParameter(parameter).length();
			}
		}

		// select path category
		String[] path = uri.split(GWConstants.SLASH, 3);
		for (int i = 0; i < path.length; i++) {
			try {
				path[i] = URLDecoder.decode(path[i], GWConstants.CHARSET_UTF_8);
			} catch (UnsupportedEncodingException e) {
				PrintStack.logging(logger, e);
			}
			logger.info(GWConstants.LOG_GWMAIN_PATH, i, path[i]);
		}

		String pathCategory = "";
		if (uri.equals(GWConstants.SLASH)) {
			pathCategory = GWConstants.REQUEST_ROOT;
		} else if (path.length <= 2 || path[2].isEmpty()) {
			pathCategory = GWConstants.REQUEST_BUCKET;
		} else {
			pathCategory = GWConstants.REQUEST_OBJECT;
		}
		
		S3Parameter s3Parameter = new S3Parameter();
		try {
			s3Parameter.setInputStream(request.getInputStream());
		} catch (IOException e) {
			PrintStack.logging(logger, e);
		}
		s3Parameter.setRequestSize(requestSize);
		s3Parameter.setRequestID(UUID.randomUUID().toString().substring(24).toUpperCase());
		s3Parameter.setRequest(request);
		s3Parameter.setResponse(response);
		if (!Strings.isNullOrEmpty(path[1])) {
			s3Parameter.setBucketName(path[1]);
		}
		if (path.length == 3 && !Strings.isNullOrEmpty(path[2])) {
			s3Parameter.setObjectName(path[2]);
		}
		s3Parameter.setMethod(method);
		s3Parameter.setStartTime(startTime);
		s3Parameter.setPathCategory(pathCategory);
		s3Parameter.setMaxFileSize(s3Config.maxFileSize());
		s3Parameter.setMaxTimeSkew(s3Config.maxTimeSkew());
		s3Parameter.setRemoteHost(request.getRemoteHost());
		s3Parameter.setRequestURI(request.getRequestURI());
		s3Parameter.setReferer(request.getHeader(HttpHeaders.REFERER));
		s3Parameter.setUserAgent(request.getHeader(HttpHeaders.USER_AGENT));
		s3Parameter.setAuthorization(request.getHeader(HttpHeaders.AUTHORIZATION));
		s3Parameter.setxAmzAlgorithm(request.getParameter(GWConstants.X_AMZ_ALGORITHM));
		s3Parameter.setHostName(request.getHeader(HttpHeaders.HOST));
		s3Parameter.setHostID(request.getHeader(GWConstants.X_AMZ_ID_2));
		s3Parameter.setRemoteHost(!Strings.isNullOrEmpty(request.getHeader(GWConstants.X_FORWARDED_FOR)) ? request.getHeader(GWConstants.X_FORWARDED_FOR) : request.getRemoteAddr());

		return s3Parameter;
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

		AsyncHandler.s3logging(s3Parameter);
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
