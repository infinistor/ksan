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
package com.pspace.ifs.ksan.gw.sign;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.identity.S3User;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagerHelper;
import com.pspace.ifs.ksan.gw.utils.PrintStack;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.ObjManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerException.ResourceNotFoundException;

public class S3Signing {
	private static final Logger logger = LoggerFactory.getLogger(S3Signing.class);
	S3Parameter ip;
	private int maxDateSkew;
	
	public S3Signing(S3Parameter ip) {
		this.ip = ip;
		this.maxDateSkew = ip.getMaxTimeSkew();
	}
	
	private boolean ishasDateHeader(HttpServletRequest request) {
		for (String headerName : Collections.list(request.getHeaderNames())) {
			if (headerName.equalsIgnoreCase(HttpHeaders.DATE)) {
				return true;
			} 
		}

		return false;
	}

	private boolean ishasXAmzDateHeader(HttpServletRequest request) {
		for (String headerName : Collections.list(request.getHeaderNames())) {

			if (headerName.equalsIgnoreCase(GWConstants.X_AMZ_DATE)) {
				// why x-amz-date name exist,but value is null?
				if ("".equals(request.getHeader(GWConstants.X_AMZ_DATE)) || request.getHeader(GWConstants.X_AMZ_DATE) == null) {
					logger.info(GWConstants.LOG_S3SIGNING_HAVE_DATE);
				} else {
					return true;
				}
			}
		}

		return false;
	}

	public S3Parameter publicvalidation() throws GWException {
		String uri = ip.getRequest().getRequestURI();
		String hostHeader = ip.getRequest().getHeader(HttpHeaders.HOST);
		String preuri = uriReconstructer(uri, hostHeader, Optional.fromNullable(ip.getVirtualHost()));

		String bucket;
		String[] path = null;
		if(preuri.startsWith(GWConstants.SLASH_WEBSITE)) {
			path = preuri.split(GWConstants.SLASH, 4);
			bucket = path[2];
			ip.setWebsite(true);
		} else {
			path = preuri.split(GWConstants.SLASH, 3);
			bucket = path[1];
			ip.setWebsite(false);
		}

		for (int i = 0; i < path.length; i++) {
			try {
				path[i] = URLDecoder.decode(path[i], GWConstants.CHARSET_UTF_8);
			} catch (UnsupportedEncodingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, GWConstants.LOG_S3SIGNING_UNSUPPORT_ENCODING_LANGUAGE, ip);
			}
		}

		if(preuri.startsWith(GWConstants.SLASH_WEBSITE)) {
			path = preuri.split(GWConstants.SLASH, 4);
		} else {
			path = preuri.split(GWConstants.SLASH, 3);
		}

		Bucket bucketInfo = null;
		ObjManager objManager = null;
		try {
			objManager = ObjManagerHelper.getInstance().getObjManager();
			bucketInfo = ObjManagerHelper.getInstance().getObjManager().getBucket(bucket);
		} catch (ResourceNotFoundException e) {
			PrintStack.logging(logger, e);
			e.printStackTrace();
		} catch (SQLException e) {
			PrintStack.logging(logger, e);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		} finally {
			try {
				ObjManagerHelper.getInstance().returnObjManager(objManager);
			} catch (Exception e) {
				PrintStack.logging(logger, e);
			}
		}

		if (bucketInfo == null) {
			throw new GWException(GWErrorCode.INVALID_ACCESS_KEY_ID, ip);
		}
		S3User user = GWUtils.getDBInstance().getIdentityByID(bucketInfo.getUserId());
		if (user == null) {
			throw new GWException(GWErrorCode.INVALID_ACCESS_KEY_ID, ip);
		}

		ip.setUser(user);

		if (ip.isWebsite()) {
			String[] enhancepath = new String[path.length - 1];
			for (int i = 0; i < path.length; i++) {
				if (i == 0) {
					enhancepath[i] = path[i];
					continue;
				}
	
				if (i == 1) {
					continue;
				}
	
				enhancepath[i - 1] = path[i];
				logger.debug(GWConstants.LOG_S3SIGNING_ENHANCE_PATH, i, enhancepath[i]);
			}
			// ip.path = enhancepath;
		} else {
			// ip.path = path;
		}

		return ip;
	}
	
	public S3Parameter validation() throws GWException {
		boolean hasDateHeader = ishasDateHeader(ip.getRequest());
		boolean hasXAmzDateHeader = ishasXAmzDateHeader(ip.getRequest());

		boolean haveBothDateHeader = false;
		if (hasDateHeader && hasXAmzDateHeader) {
			haveBothDateHeader = true;
		}
		
		String uri = ip.getRequest().getRequestURI();
		String hostHeader = ip.getRequest().getHeader(HttpHeaders.HOST);
		boolean headernull = false;
		
		if (!hasDateHeader && !hasXAmzDateHeader && ip.getRequest().getParameter(GWConstants.X_AMZ_DATE) == null && ip.getRequest().getParameter(GWConstants.EXPIRES) == null) {
			logger.error(GWConstants.LOG_S3SIGNING_SIGNATURE_OR_AUTH_HEADER_NULL, uri);
			throw new GWException(GWErrorCode.ACCESS_DENIED, GWConstants.LOG_S3SIGNING_AWS_REQUIRES_VALID_DATE);
		}
		
		String[] path = uri.split(GWConstants.SLASH, 3);
		for (int i = 0; i < path.length; i++) {
			try {
				path[i] = URLDecoder.decode(path[i], GWConstants.CHARSET_UTF_8);
			} catch (UnsupportedEncodingException e) {
				throw new GWException(GWErrorCode.SERVER_ERROR, GWConstants.LOG_S3SIGNING_UNSUPPORT_ENCODING_LANGUAGE);
			}
		}
		
		S3AuthorizationHeader authHeader = null;
		String headerAuthorization = ip.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
		
		if (headerAuthorization == null) {
			String algorithm = ip.getRequest().getParameter(GWConstants.X_AMZ_ALGORITHM);

			if (algorithm == null) { //v2 query
				String identity = ip.getRequest().getParameter(GWConstants.AWS_ACCESS_KEY_ID);
				String signature = ip.getRequest().getParameter(GWConstants.SIGNATURE);
				if (identity == null || signature == null) {
					logger.error(GWConstants.LOG_S3SIGNING_V2_SIGNATURE_NULL, uri);
					throw new GWException(GWErrorCode.ACCESS_DENIED);
				}
				headerAuthorization = GWConstants.AWS_SPACE + identity + GWConstants.COLON + signature;
				headernull = true;
			} else if (algorithm.equals(GWConstants.AWS4_HMAC_SHA256)) { //v4 query
				String credential = ip.getRequest().getParameter(GWConstants.X_AMZ_CREDENTIAL);
				String signedHeaders = ip.getRequest().getParameter(GWConstants.X_AMZ_SIGNEDHEADERS);
				String signature = ip.getRequest().getParameter(GWConstants.X_AMZ_SIGNATURE);
				if (credential == null || signedHeaders == null || signature == null) {
					logger.error(GWConstants.LOG_S3SIGNING_V4_SIGNATURE_NULL, uri);
					throw new GWException(GWErrorCode.ACCESS_DENIED);
				}
				headerAuthorization = GWConstants.AWS4_HMAC_SHA256 +
						GWConstants.SIGN_CREDENTIAL + credential +
						GWConstants.SIGN_REQEUEST_SIGNED_HEADERS + signedHeaders +
						GWConstants.SIGN_SIGNATURE + signature;
				headernull = true;
			} else {
				logger.error(GWConstants.LOG_S3SIGNING_UNKNOWN_ALGORITHM_VALUE, algorithm);
				throw new IllegalArgumentException(GWConstants.LOG_S3SIGNING_UNKNOWN_ALGORITHM + algorithm);
			}
		}
		
		try {
			authHeader = new S3AuthorizationHeader(headerAuthorization);
			//whether v2 or v4 (normal header and query)
			logger.debug(GWConstants.LOG_S3SIGNING_AUTH_HEADER, authHeader);
		} catch (IllegalArgumentException iae) {
			throw new GWException(GWErrorCode.INVALID_ARGUMENT, iae);
		}
		
		String requestIdentity = authHeader.identity;
		
		if(requestIdentity == null) {
			logger.error(GWConstants.LOG_S3SIGNING_ACCESS_NULL);
			throw new GWException(GWErrorCode.INVALID_ACCESS_KEY_ID);
		}

		String preuri = uriReconstructer(uri, hostHeader, Optional.fromNullable(null));
		S3User user = GWUtils.getDBInstance().getIdentity(requestIdentity);
		if (user == null) {
			logger.error(GWConstants.LOG_S3SIGNING_USER_NULL);
			throw new GWException(GWErrorCode.INVALID_ACCESS_KEY_ID, ip);
		}

		logger.info(GWConstants.LOG_S3SIGNING_USER, user.getUserName());
		if (headernull) {
			headerAuthorization = null;
		}
		
		boolean presignedUrl = false;
		
		if (headerAuthorization == null) {
			String algorithm = ip.getRequest().getParameter(GWConstants.X_AMZ_ALGORITHM);

			if (algorithm == null) { //v2 query
				String identity = ip.getRequest().getParameter(GWConstants.AWS_ACCESS_KEY_ID);
				String signature = ip.getRequest().getParameter(GWConstants.SIGNATURE);
				if (identity == null || signature == null) {
					logger.error(GWConstants.LOG_S3SIGNING_V2_SIGNATURE_NULL, uri);
					throw new GWException(GWErrorCode.ACCESS_DENIED);
				}
				headerAuthorization = GWConstants.AWS_SPACE + identity + GWConstants.COLON + signature;
				presignedUrl = true;
			} else if (algorithm.equals(GWConstants.AWS4_HMAC_SHA256)) { //v4 query
				String credential = ip.getRequest().getParameter(GWConstants.X_AMZ_CREDENTIAL);
				String signedHeaders = ip.getRequest().getParameter(GWConstants.X_AMZ_SIGNEDHEADERS);
				String signature = ip.getRequest().getParameter(GWConstants.X_AMZ_SIGNATURE);
				if (credential == null || signedHeaders == null || signature == null) {
					logger.error(GWConstants.LOG_S3SIGNING_V4_SIGNATURE_NULL, uri);
					throw new GWException(GWErrorCode.ACCESS_DENIED);
				}
				headerAuthorization = GWConstants.AWS4_HMAC_SHA256 +
						GWConstants.SIGN_CREDENTIAL + credential +
						GWConstants.SIGN_REQEUEST_SIGNED_HEADERS + signedHeaders +
						GWConstants.SIGN_SIGNATURE + signature;
				presignedUrl = true;
			} else {
				logger.error(GWConstants.LOG_S3SIGNING_UNKNOWN_ALGORITHM_VALUE, algorithm);
				throw new IllegalArgumentException(GWConstants.LOG_S3SIGNING_UNKNOWN_ALGORITHM + algorithm);
			}
		}

		long dateSkew = 0; //date for timeskew check

		//v2 GET /s3proxy-1080747708/foo?AWSAccessKeyId=local-identity&Expires=
		//1510322602&Signature=UTyfHY1b1Wgr5BFEn9dpPlWdtFE%3D)
		//have no date
		boolean haveDate = true;

		AuthenticationType finalAuthType = null;
		if (authHeader.authenticationType == AuthenticationType.AWS_V2) {
			finalAuthType = AuthenticationType.AWS_V2;
		} else if ( authHeader.authenticationType == AuthenticationType.AWS_V4) {
			finalAuthType = AuthenticationType.AWS_V4;
		} else {
			logger.error(GWConstants.LOG_S3SIGNING_AUTHENTICATION_NULL, uri);
			throw new GWException(GWErrorCode.ACCESS_DENIED);
		}

		if (hasXAmzDateHeader) { //format diff between v2 and v4
			if (finalAuthType == AuthenticationType.AWS_V2) {
				logger.info(GWConstants.LOG_S3SIGNING_INTO_V2, ip.getRequest().getHeader(GWConstants.X_AMZ_DATE));
				dateSkew = ip.getRequest().getDateHeader(GWConstants.X_AMZ_DATE);
				dateSkew /= 1000;
				//case sensetive?
			} else if (finalAuthType == AuthenticationType.AWS_V4) {
				logger.info(GWConstants.LOG_S3SIGNING_INTO_V4, ip.getRequest().getHeader(GWConstants.X_AMZ_DATE));
				dateSkew = GWUtils.parseIso8601(ip.getRequest().getHeader(GWConstants.X_AMZ_DATE));
			}
		} else if (ip.getRequest().getParameter(GWConstants.X_AMZ_DATE) != null) { // v4 query
			String dateString = ip.getRequest().getParameter(GWConstants.X_AMZ_DATE);
			dateSkew = GWUtils.parseIso8601(dateString);
			logger.info(GWConstants.LOG_S3SIGNING_DATE, dateString);
		} else if (hasDateHeader) {
			try {
				dateSkew = ip.getRequest().getDateHeader(HttpHeaders.DATE);
				dateSkew /= 1000;
				logger.info(GWConstants.LOG_S3SIGNING_DATE_HEADER, dateSkew);
			} catch (IllegalArgumentException iae) {
				logger.info(GWConstants.LOG_S3SIGNING_ILLEGAL_DATE_SKEW, dateSkew);
				throw new GWException(GWErrorCode.ACCESS_DENIED, iae);
			}				
		} else {
			haveDate = false;
		}

		if (haveDate) {
			GWUtils.isTimeSkewed(dateSkew, maxDateSkew);
		}
		
		String credential = user.getAccessSecret();
		
		String expiresString = ip.getRequest().getParameter(GWConstants.EXPIRES);
		if (expiresString != null) { // v2 query
			long expires = Long.parseLong(expiresString);
			long nowSeconds = System.currentTimeMillis() / 1000;
			if (nowSeconds >= expires) {
				logger.error(GWConstants.LOG_S3SIGNING_EXPIRES, expiresString);
				throw new GWException(GWErrorCode.ACCESS_DENIED);
			}
		}

		String dateString = ip.getRequest().getParameter(GWConstants.X_AMZ_DATE);

		//from para v4 query
		expiresString = ip.getRequest().getParameter(GWConstants.X_AMZ_EXPIRES);
		if (dateString != null && expiresString != null) { //v4 query
			long date = GWUtils.parseIso8601(dateString);
			long expires = Long.parseLong(expiresString);
			long nowSeconds = System.currentTimeMillis() / 1000;
			if (nowSeconds >= date + expires) {
				logger.error(GWConstants.LOG_S3SIGNING_EXPIRES, expiresString);
				throw new GWException(GWErrorCode.ACCESS_DENIED, GWConstants.LOG_S3SIGNING_HAS_EXPIRED);
			}
		}
		
		String expectedSignature = null;

		// When presigned url is generated, it doesn't consider service path
		//String uriForSigning = presignedUrl ? uri : uri;
		String uriForSigning = preuri;
		
		S3Signature s3Signature = new S3Signature();
		
		logger.info(GWConstants.LOG_S3SIGNING_URI, preuri);
		if (authHeader.hmacAlgorithm == null) { //v2
			expectedSignature = s3Signature.createAuthorizationSignature(
					ip.getRequest(), uriForSigning, credential, presignedUrl, haveBothDateHeader);
		} else {
			String contentSha256 = ip.getRequest().getHeader(GWConstants.X_AMZ_CONTENT_SHA256);
			byte[] payload = null;
			int skip=0;

			if (ip.getRequest().getParameter(GWConstants.X_AMZ_ALGORITHM) != null) {
				payload = new byte[0];
			} else if (GWConstants.STREAMING_AWS4_HMAC_SHA256_PAYLOAD.equals(contentSha256)) {
				payload = new byte[0];
				ip.setInputStream(new ChunkedInputStream(ip.getInputStream()));
			} else if (GWConstants.UNSIGNED_PAYLOAD.equals(contentSha256)) {
				payload = new byte[0];
			} else {
				logger.info(GWConstants.LOG_S3SIGNING_PATH_LENGTH, path.length);
				if (ip.getRequest().getMethod().equals(GWConstants.METHOD_PUT) && path.length > 2) {
					skip = 1;
				}

				if(skip == 0) {
					try {
						payload = ByteStreams.toByteArray(ByteStreams.limit(ip.getInputStream(), 1048576 + 1));
					} catch (IOException e) {
						PrintStack.logging(logger, e);
					}
					
					ip.setInputStream(new ByteArrayInputStream(payload));
				}
			}

			if(skip == 1) {
				expectedSignature = authHeader.signature;
			} else {
				try {
					expectedSignature = s3Signature.createAuthorizationSignatureV4(// v4 sign
							ip.getRequest(), authHeader, payload, uriForSigning, credential);

				} catch (InvalidKeyException | NoSuchAlgorithmException e) {
					PrintStack.logging(logger, e);
					throw new GWException(GWErrorCode.INVALID_ARGUMENT, e);
				} catch (IOException e) {
					PrintStack.logging(logger, e);
					throw new GWException(GWErrorCode.INVALID_ARGUMENT, e);
				}
			}
		}

		if (!GWUtils.constantTimeEquals(expectedSignature, authHeader.signature)) {
			logger.error(GWConstants.LOG_S3SIGNING_FAILED_VALIDATE_EXPECT_AND_AUTH_HEADER, expectedSignature, authHeader.signature );
			throw new GWException(GWErrorCode.SIGNATURE_DOES_NOT_MATCH);
		}
		
		ip.setUser(user);
		return ip;
	}
	
	public String uriReconstructer(String uri, String hostHeader, Optional<String> virtualHost) {
		if (hostHeader != null && virtualHost.isPresent()) {
			hostHeader = HostAndPort.fromString(hostHeader).getHost();
			String virtualHostSuffix = GWConstants.POINT + virtualHost.get();
			if (!hostHeader.equals(virtualHost.get())) {
				if (hostHeader.endsWith(virtualHostSuffix)) {
					String bucket = hostHeader.substring(0, hostHeader.length() - virtualHostSuffix.length());
					uri = GWConstants.SLASH + bucket + uri;
				} else {
					String bucket = hostHeader.toLowerCase();
					uri = GWConstants.SLASH + bucket + uri;
				}

				if (hostHeader.endsWith(GWConstants.POINT_WEBSIZE_POINT + virtualHostSuffix)) {
					String bucket = hostHeader.substring(0, hostHeader.length() - virtualHostSuffix.length());
					uri = GWConstants.SLASH_WEBSITE_SLASH + bucket + uri;
				} else {
					String bucket = hostHeader.toLowerCase();
					uri = GWConstants.SLASH_WEBSITE_SLASH + bucket + uri;
				}
			}
		}

		return uri;
	}
}
