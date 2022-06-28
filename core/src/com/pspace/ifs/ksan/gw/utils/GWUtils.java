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
package com.pspace.ifs.ksan.gw.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.crypto.stream.CtrCryptoInputStream;
import org.apache.commons.crypto.stream.CtrCryptoOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.net.HttpHeaders;
import com.google.common.net.PercentEscaper;
import com.pspace.ifs.ksan.gw.db.MariaDB;
import com.pspace.ifs.ksan.gw.db.GWDB;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicyJson;
import com.pspace.ifs.ksan.gw.format.CORSConfiguration;
import com.pspace.ifs.ksan.gw.format.Policy;
import com.pspace.ifs.ksan.gw.format.PublicAccessBlockConfiguration;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant.Grantee;
import com.pspace.ifs.ksan.gw.format.CORSConfiguration.CORSRule;
import com.pspace.ifs.ksan.gw.format.Policy.Statement;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;

public class GWUtils {

	private static final Logger logger = LoggerFactory.getLogger(GWUtils.class);
	private static final Escaper urlEscaper = new PercentEscaper(GWConstants.PERCENT_ESCAPER, true);
	private static String localIP = null;

	private static void addResponseHeaderWithOverride(
			HttpServletRequest request, HttpServletResponse response,
			String headerName, String overrideHeaderName, String value) {
		String override = request.getParameter(overrideHeaderName);

		// NPE in if value is null
		override = (override != null) ? override : value;

		if (override != null) {
			response.addHeader(headerName, override);
		}
	}
	
	public static void addMetadataToResponse(HttpServletRequest request, HttpServletResponse response, S3Metadata s3Metadata, List<String> ContentLength_Headers, Long streamsize) {
		
		addResponseHeaderWithOverride(request, response,
				HttpHeaders.CACHE_CONTROL, GWConstants.RESPONSE_CACHE_CONTROL,
				s3Metadata.getCacheControl());
		addResponseHeaderWithOverride(request, response,
				HttpHeaders.CONTENT_ENCODING, GWConstants.RESPONSE_CONTENT_ENCODING,
				s3Metadata.getContentEncoding()); 
		addResponseHeaderWithOverride(request, response,
				HttpHeaders.CONTENT_LANGUAGE, GWConstants.RESPONSE_CONTENT_LANGUAGE, 
				s3Metadata.getContentLanguage());
		addResponseHeaderWithOverride(request, response,
				HttpHeaders.CONTENT_DISPOSITION, GWConstants.RESPONSE_CONTENT_DISPOSITION,
				s3Metadata.getContentDisposition());
		
		// TODO: handles only a single range due to jclouds limitations
		Collection<String> contentRanges = ContentLength_Headers;
		if (ContentLength_Headers != null && !contentRanges.isEmpty()) {
			for (String contents : ContentLength_Headers) {
				response.addHeader(HttpHeaders.CONTENT_RANGE, contents);
			}
			
			response.addHeader(HttpHeaders.ACCEPT_RANGES, GWConstants.BYTES);
			response.addHeader(HttpHeaders.CONTENT_LENGTH, streamsize.toString());
		} else {
			response.addHeader(HttpHeaders.CONTENT_LENGTH, s3Metadata.getContentLength().toString());
		}
				
		String overrideContentType = request.getParameter(GWConstants.RESPONSE_CONTENT_TYPE);
		response.setContentType(overrideContentType != null ? overrideContentType : s3Metadata.getContentType());
		
		if (s3Metadata.getCustomerAlgorithm() != null ) {
			response.addHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM, s3Metadata.getCustomerAlgorithm());
		}
		
		if (s3Metadata.getCustomerKey() != null ) {
			response.addHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY, s3Metadata.getCustomerKey());
		}
		
		if (s3Metadata.getCustomerKeyMD5() != null ) {
			response.addHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5, s3Metadata.getCustomerKeyMD5());
		}
		
		if (s3Metadata.getServersideEncryption() != null ) {
			response.addHeader(GWConstants.X_AMZ_SERVER_SIDE_ENCRYPTION, s3Metadata.getServersideEncryption());
		}
		
		if (s3Metadata.getLockMode() != null) {
			response.addHeader(GWConstants.X_AMZ_OBJECT_LOCK_MODE, s3Metadata.getLockMode());
		}

		if (s3Metadata.getLockExpires() != null) {
			response.addHeader(GWConstants.X_AMZ_OBJECT_LOCK_RETAIN_UNTIL_DATE, s3Metadata.getLockExpires());
		}

		if (s3Metadata.getLegalHold() != null) {
			response.addHeader(GWConstants.X_AMZ_OBJECT_LOCK_LEGAL_HOLD, s3Metadata.getLegalHold());
		}

		if (s3Metadata.getUserMetadataMap() != null ) {
			for (Map.Entry<String, String> entry : s3Metadata.getUserMetadataMap().entrySet()) {
				response.addHeader(entry.getKey(), entry.getValue());
				logger.info(GWConstants.LOG_UTILS_USER_META_DATA, entry.getKey(), entry.getValue());
			}
		}

		response.addHeader(HttpHeaders.ETAG, maybeQuoteETag(s3Metadata.getETag()));
		
		String overrideExpires = request.getParameter(GWConstants.RESPONSE_EXPIRES);
		if (overrideExpires != null) {
			response.addHeader(HttpHeaders.EXPIRES, overrideExpires);
		} else {
			Date expires = s3Metadata.getExpires();
			if (expires != null) {
				response.addDateHeader(HttpHeaders.EXPIRES, expires.getTime());
			}
		}
		
		response.addDateHeader(HttpHeaders.LAST_MODIFIED, s3Metadata.getLastModified().getTime());
		
		if (s3Metadata.getTaggingCount() != null) {
			response.addHeader(GWConstants.X_AMZ_TAGGING_COUNT, s3Metadata.getTaggingCount());
		}
		
		response.addHeader(GWConstants.X_AMZ_VERSION_ID, s3Metadata.getVersionId());
	}

	/** Parse ISO 8601 timestamp into seconds since 1970. */
	public static long parseTimeExpire(String date, S3Parameter s3Parameter) throws GWException {
		SimpleDateFormat formatter = new SimpleDateFormat(GWConstants.ISO_8601_TIME_SIMPLE_FORMAT);
		formatter.setTimeZone(TimeZone.getTimeZone(GWConstants.UTC));
		logger.debug(GWConstants.LOG_UTILS_8061_DATE, date);
		try {
			return formatter.parse(date).getTime() / 1000;
		} catch (ParseException pe) {
			PrintStack.logging(logger, pe);
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		}
	}

	/** Parse ISO 8601 timestamp into seconds since 1970. */
	public static long parseIso8601(String date, S3Parameter s3Parameter) throws GWException {
		SimpleDateFormat formatter = new SimpleDateFormat(GWConstants.ISO_8601_TIME_FORMAT);
		formatter.setTimeZone(TimeZone.getTimeZone(GWConstants.UTC));
		logger.debug(GWConstants.LOG_UTILS_8061_DATE, date);
		try {
			return formatter.parse(date).getTime() / 1000;
		} catch (ParseException pe) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}
	}

	public static void isTimeSkewed(long date, int maxTimeSkew, S3Parameter s3Parameter) throws GWException  {
		if (date < 0) {
			throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
		}
		
		long now = System.currentTimeMillis() / 1000;
		if (now + maxTimeSkew < date ||	now - maxTimeSkew > date) {
			logger.debug(GWConstants.LOG_UTILS_MAX_TIME_SKEW, maxTimeSkew);
			logger.error(GWConstants.LOG_UTILS_TIME_SKEWED, date, now);
			throw new GWException(GWErrorCode.REQUEST_TIME_TOO_SKEWED, s3Parameter);
		}
	}

	public static String maybeQuoteETag(String eTag) {
		if (!eTag.startsWith(GWConstants.DOUBLE_QUOTE) && !eTag.endsWith(GWConstants.DOUBLE_QUOTE)) {
			eTag = GWConstants.DOUBLE_QUOTE + eTag + GWConstants.DOUBLE_QUOTE;
		}
		return eTag;
	}

	private static boolean startsWithIgnoreCase(String string, String prefix) {
		return string.toLowerCase().startsWith(prefix.toLowerCase());
	}

	public static boolean isField(String string, String field) {
		return startsWithIgnoreCase(string, GWConstants.CONTENT_DISPOSITION_FORM_DATA + field + GWConstants.DOUBLE_QUOTE);
	}

	public static boolean startsField(String string, String field) {
		return startsWithIgnoreCase(string, GWConstants.CONTENT_DISPOSITION_FORM_DATA + field);
	}

	public static boolean constantTimeEquals(String x, String y) {
		return MessageDigest.isEqual(x.getBytes(StandardCharsets.UTF_8),
				y.getBytes(StandardCharsets.UTF_8));
	}
	
	// Encode blob name if client requests it.  This allows for characters
	// which XML 1.0 cannot represent.
	public static String encodeBlob(String encodingType, String blobName) {
		if (encodingType != null && encodingType.equals(GWConstants.URL)) {
			return urlEscaper.escape(blobName);
		} else {
			return blobName;
		}
	}

	public static String encodeObjectName(String encodingType, String blobName) {
		if (encodingType != null && encodingType.equalsIgnoreCase(GWConstants.URL)) {
			return urlEscaper.escape(blobName).replace(GWConstants.URL_ESCAPER_FORMAT, GWConstants.PLUS);
		} else {
			return blobName;
		}
	}
	
	public static GWDB getDBInstance() {
		if (GWConfig.getInstance().getDbRepository().equalsIgnoreCase(GWConstants.MARIADB)) {
			return MariaDB.getInstance();
		} else {
			logger.error(GWConstants.LOG_UTILS_UNDEFINED_DB);
			return null;
		}
	}

	public static boolean likematch(String first, String second) {
		// If we reach at the end of both strings,
		// we are done
		if (first.length() == 0 && second.length() == 0)
			return true;

		// Make sure that the characters after '*'
		// are present in second string.
		// This function assumes that the first
		// string will not contain two consecutive '*'
		if (first.length() > 1 && first.charAt(0) == GWConstants.CHAR_ASTERISK && second.length() == 0)
			return false;

		// If the first string contains '?',
		// or current characters of both strings match
		if ((first.length() > 1 && first.charAt(0) == '?')
				|| (first.length() != 0 && second.length() != 0 && first.charAt(0) == second.charAt(0)))
			return likematch(first.substring(1), second.substring(1));

		// If there is *, then there are two possibilities
		// a) We consider current character of second string
		// b) We ignore current character of second string.
		if (first.length() > 0 && first.charAt(0) == GWConstants.CHAR_ASTERISK)
			return likematch(first.substring(1), second) || likematch(first, second.substring(1));
		return false;
	}

	public static void checkCors(S3Parameter s3Parameter) {
		if (!Strings.isNullOrEmpty(s3Parameter.getBucket().getCors())) {
			XmlMapper xmlMapper = new XmlMapper();
			try {
				CORSConfiguration corsConfiguration = xmlMapper.readValue(s3Parameter.getBucket().getCors(), CORSConfiguration.class);
				String corsOrigin = s3Parameter.getRequest().getHeader(HttpHeaders.ORIGIN);
				String corsMethods = s3Parameter.getRequest().getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);

				boolean originpass = false;
				String resOrigin = "";
				String allowMethods = "";
				String allowHeaders = "";
				String maxAges = "";
				String exposeHeaders = "";
				for (CORSRule icors : corsConfiguration.CORSRules) {
					if (!Strings.isNullOrEmpty(corsOrigin)) {
						for (String origin : icors.AllowedOrigins) {
							if (GWUtils.likematch(origin, corsOrigin)) {
								if (origin.equals(GWConstants.ASTERISK)) {
									resOrigin = origin;
								} else {
									resOrigin = corsOrigin;
								}
								
								originpass = true;
							}
						}
					}

					if (originpass == false)
						continue;
					
					int first = 0;
					if (Strings.isNullOrEmpty(corsMethods) ) {
						if (icors.AllowedMethods != null) {
							String corsMethod = "";
							boolean temp = false;
							for (String method : icors.AllowedMethods) {
								temp = true;
								corsMethod = method;
							}

							if (temp == true && first == 0) {
								allowMethods += corsMethod;
								first++;
							} else if (temp == true && first > 0) {
								allowMethods += GWConstants.COMMA + corsMethod;
								first++;
							}
						}

						if (!allowMethods.contains(s3Parameter.getMethod())) {
							return;
						}
					} else {
						for (String corsMethod : corsMethods.split(GWConstants.COMMA)) {
							boolean temp = false;

							if(icors.AllowedMethods == null) {
								continue;
							}

							for (String method : icors.AllowedMethods) {
								if (method.compareTo(GWConstants.ASTERISK) == 0 || corsMethod.trim().compareTo(method) == 0) {
									temp = true;
								}
							}

							if (temp == true && first == 0) {
								allowMethods += corsMethod;
								first++;
							} else if (temp == true && first > 0) {
								allowMethods += GWConstants.COMMA + corsMethod;
								first++;
							}
						}

						if(Strings.isNullOrEmpty(allowMethods)) {
							return;
						}
					}

					first = 0;
					if (icors.AllowedHeaders != null) {
						String corsHeader = "";
						boolean temp = false;
						for (String header : icors.AllowedHeaders) {
							temp = true;
							corsHeader = header;
						}

						if (temp == true && first == 0) {
							allowHeaders += corsHeader;
							first++;
						} else if (temp == true && first > 0) {
							allowHeaders += GWConstants.COMMA + corsHeader;
							first++;
						}
					}

					first = 0;
					if (icors.ExposeHeaders != null) {
						for (String exposeHeader : icors.ExposeHeaders) {
							if (first == 0) {
								exposeHeaders += exposeHeader;
								first++;
							} else {
								exposeHeaders += GWConstants.COMMA + exposeHeader;
								first++;
							}
						}
					}

					if (!Strings.isNullOrEmpty(icors.MaxAgeSeconds))
						maxAges = icors.MaxAgeSeconds;

					if (originpass == true)
						break;
				}

				if (originpass == false ) {
					return;
				}

				String vary = HttpHeaders.ORIGIN;
				if (!Strings.isNullOrEmpty(allowMethods)) {
					vary += GWConstants.COMMA + HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
				}

				if (!Strings.isNullOrEmpty(allowHeaders)) {
					vary += GWConstants.COMMA + HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
				}

				if (!Strings.isNullOrEmpty(exposeHeaders)) {
					vary += GWConstants.COMMA + HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
				}

				if (!Strings.isNullOrEmpty(maxAges)) {
					vary += GWConstants.COMMA + HttpHeaders.ACCESS_CONTROL_MAX_AGE;
				}

				s3Parameter.getResponse().addHeader(HttpHeaders.VARY, vary);
				s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, resOrigin);

				if (!Strings.isNullOrEmpty(allowMethods)) {
					s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowMethods);
				}

				if (!Strings.isNullOrEmpty(allowHeaders)) {
					s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
				}

				if (!Strings.isNullOrEmpty(exposeHeaders)) {
					s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders);
				}

				if (!Strings.isNullOrEmpty(maxAges)) {
					s3Parameter.getResponse().addHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, maxAges);
				}

			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
			}
		}
	}

	public static boolean isIgnorePublicAcls(S3Parameter s3Parameter) throws GWException {
		PublicAccessBlockConfiguration pabc = null;
		if (s3Parameter.getBucket() != null && !Strings.isNullOrEmpty(s3Parameter.getBucket().getAccess())) {
			try {
				pabc = new XmlMapper().readValue(s3Parameter.getBucket().getAccess(), PublicAccessBlockConfiguration.class);
				if (pabc.IgnorePublicAcls.equalsIgnoreCase(GWConstants.STRING_TRUE)) {
					return true;
				}
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, e, s3Parameter);
			}
		}

		return false;
	}

	public static boolean isPublicPolicyBucket(String policyInfo, S3Parameter s3Parameter) throws GWException {
		PublicAccessBlockConfiguration pabc = null;
		if (s3Parameter.getBucket() != null && !Strings.isNullOrEmpty(s3Parameter.getBucket().getAccess())) {
			try {
				pabc = new XmlMapper().readValue(s3Parameter.getBucket().getAccess(), PublicAccessBlockConfiguration.class);
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, e, s3Parameter);
			}
		}

		boolean effect = false;
		if (Strings.isNullOrEmpty(policyInfo)) {
			return effect;
		}

		Policy policy = null;
		// read policy
		ObjectMapper jsonMapper = new ObjectMapper();
		try {
			policy = jsonMapper.readValue(policyInfo, Policy.class);

			if (policy == null) {
				return effect;
			}
		} catch (JsonMappingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		// check policy - loop statement
		for (Statement s : policy.statements) {
			boolean effectcheck = false;

			// check principal (id)
			for (String aws : s.principal.aws) {
				if (aws.equals(GWConstants.ASTERISK)) {
					if (pabc != null && pabc.BlockPublicPolicy.equalsIgnoreCase(GWConstants.STRING_TRUE)) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					effectcheck = true;
					break;
				}
			}

			// check Resource (object path, bucket path)
			for (String resource : s.resources) {
				if (resource.equals(GWConstants.ASTERISK)) {
					if (pabc != null && pabc.BlockPublicPolicy.equalsIgnoreCase(GWConstants.STRING_TRUE)) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					effectcheck = true;
					break;
				}

				String[] res = resource.split(GWConstants.COLON, -1);
				// all resource check
				if (!Strings.isNullOrEmpty(res[5]) && res[5].equals(GWConstants.ASTERISK)) {
					if (pabc != null && pabc.BlockPublicPolicy.equalsIgnoreCase(GWConstants.STRING_TRUE)) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					effectcheck = true;
					break;
				}
			}

			boolean conditioncheck = false;
			if (s.condition == null) {
				conditioncheck = false;
			} else {
				for (Map.Entry<String, JsonNode> entry : s.condition.getUserExtensions().entries()) {
					JsonNode jsonNode = entry.getValue();
					if (jsonNode.isObject()) {
						Iterator<String> fieldNames = jsonNode.fieldNames();
						if (fieldNames.hasNext()) {
							// read key
							String fieldName = fieldNames.next();
							String key = fieldName;
							logger.info(GWConstants.LOG_UTILS_KEY, key);

							if (key.equals(GWConstants.AWS_SOURCE_ARN)) {
								conditioncheck = true;
								break;
							} else if (key.equals(GWConstants.AWS_SOURCE_VPC)) {
								conditioncheck = true;
								break;
							} else if (key.equals(GWConstants.AWS_SOURCE_VPCE)) {
								conditioncheck = true;
								break;
							} else if (key.equals(GWConstants.AWS_SOURCE_OWNER)) {
								conditioncheck = true;
								break;
							} else if (key.equals(GWConstants.AWS_SOURCE_ACCOUNT)) {
								conditioncheck = true;
								break;
							} else if (key.equals(GWConstants.S3_SERVER_SIDE_ENCRYPTION_AWS_KMS_KEY_ID)) {
								conditioncheck = true;
								break;
							} else if (key.equals(GWConstants.S3_DATA_ACCESS_POINT_ARN)) {
								conditioncheck = true;
								break;
							} else if (key.equals(GWConstants.AWS_SOURCE_IP)) {
								conditioncheck = true;
								break;
							}
						}
					}
				}
			}

			if (s.effect.equals(GWConstants.ALLOW)) {
				if (effectcheck == true && conditioncheck == false) {
					if (pabc != null && pabc.BlockPublicPolicy.equalsIgnoreCase(GWConstants.STRING_TRUE)) {
						throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
					}
					effect = true;
					return effect;
				}
			}
		}

		return effect;
	}

	public static String makeOriginalXml(String xml, S3Parameter s3Parameter) throws GWException {
		logger.debug(GWConstants.LOG_UTILS_SOURCE_ACL, xml);
		if (Strings.isNullOrEmpty(xml)) {
			return "";
		}

		ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		
		AccessControlPolicyJson actualObj;
		try {
			actualObj = objectMapper.readValue(xml, AccessControlPolicyJson.class);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		AccessControlPolicy accessControlPolicy = new AccessControlPolicy();
		accessControlPolicy.owner = new AccessControlPolicy.Owner();
		if(actualObj.ow != null) {
			if(!Strings.isNullOrEmpty(actualObj.ow.id)) {
				accessControlPolicy.owner.id = actualObj.ow.id;
			}

			if(!Strings.isNullOrEmpty(actualObj.ow.dN)) {
				accessControlPolicy.owner.displayName = actualObj.ow.dN;
			}
		}

		if(actualObj.acs != null) {
			accessControlPolicy.aclList = new AccessControlPolicy.AccessControlList();
			if( actualObj.acs.gt != null) {
				accessControlPolicy.aclList.grants = new ArrayList<AccessControlPolicy.AccessControlList.Grant>();
				for(AccessControlPolicyJson.ACS.Gt gt :  actualObj.acs.gt) {
					AccessControlPolicy.AccessControlList.Grant grant = new AccessControlPolicy.AccessControlList.Grant();
					if(!Strings.isNullOrEmpty(gt.perm)) {
						if(gt.perm.equals(GWConstants.GRANT_AB_FC)) {
							grant.permission = GWConstants.GRANT_FULL_CONTROL;
						} else if (gt.perm.equals(GWConstants.GRANT_AB_W)) {
							grant.permission = GWConstants.GRANT_WRITE;
						} else if (gt.perm.equals(GWConstants.GRANT_AB_R)) {
							grant.permission = GWConstants.GRANT_READ;
						} else if (gt.perm.equals(GWConstants.GRANT_AB_RA)) {
							grant.permission = GWConstants.GRANT_READ_ACP;
						} else if (gt.perm.equals(GWConstants.GRANT_AB_WA)) {
							grant.permission = GWConstants.GRANT_WRITE_ACP;
						}
					}
					
					if(gt.gte != null) {
						AccessControlPolicy.AccessControlList.Grant.Grantee grantee = new AccessControlPolicy.AccessControlList.Grant.Grantee();
						if(!Strings.isNullOrEmpty(gt.gte.id)) {
							grantee.id = gt.gte.id;
						}

						if(!Strings.isNullOrEmpty(gt.gte.ddN)) {
							grantee.displayName = gt.gte.ddN;
						}

						if(!Strings.isNullOrEmpty(gt.gte.eA)) {
							grantee.emailAddress = gt.gte.eA;
						}

						if(!Strings.isNullOrEmpty(gt.gte.type)) {
							if(gt.gte.type.equals(GWConstants.GRANT_AB_CU)) {
								grantee.type = GWConstants.CANONICAL_USER;
							} else if (gt.gte.type.equals(GWConstants.GRANT_AB_G)) {
								grantee.type = GWConstants.GROUP;
							}
						}

						if(!Strings.isNullOrEmpty(gt.gte.uri)) {
							if(gt.gte.uri.equals(GWConstants.GRANT_AB_PU)) {
								grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
							} else if(gt.gte.uri.equals(GWConstants.GRANT_AB_AU)) {
								grantee.uri = GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS;
							}
						}

						grant.grantee = grantee;
					}

					accessControlPolicy.aclList.grants.add(grant);
				}
			}
		}

		String aclXml = "";
		XmlMapper xmlMapper = new XmlMapper();
		try {
			xmlMapper.setSerializationInclusion(Include.NON_EMPTY);
			aclXml = xmlMapper.writeValueAsString(accessControlPolicy).replaceAll(GWConstants.WSTXNS, GWConstants.XSI);
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		aclXml = aclXml.replace(GWConstants.ACCESS_CONTROL_POLICY, GWConstants.ACCESS_CONTROL_POLICY_XMLNS); 

		if(!aclXml.contains(GWConstants.XML_VERSION)) {
			aclXml = GWConstants.XML_VERSION_FULL_STANDALONE + aclXml;
		}

		return aclXml;
	}
	
	protected static void readAclHeader(String grantstr, String permission, AccessControlPolicy policy) {
		String[] ids = grantstr.split(GWConstants.COMMA);
		for (String readid : ids) {
			String[] idkeyvalue = readid.split(GWConstants.EQUAL);
			Grant rg = new Grant();
			rg.grantee = new Grantee();

			if (idkeyvalue[0].trim().compareTo(GWConstants.ID) == 0) {
				rg.grantee.type = GWConstants.CANONICAL_USER;
				rg.grantee.id = idkeyvalue[1].replaceAll(GWConstants.DOUBLE_QUOTE, "");
			}

			if (idkeyvalue[0].trim().compareTo(GWConstants.URI) == 0) {
				rg.grantee.type = GWConstants.GROUP;
				rg.grantee.uri = idkeyvalue[1].replaceAll(GWConstants.DOUBLE_QUOTE, "");
			}

			if (idkeyvalue[0].trim().compareTo(GWConstants.EMAIL_ADDRESS) == 0) {
				rg.grantee.type = GWConstants.CANONICAL_USER;
				rg.grantee.emailAddress = idkeyvalue[1].replaceAll(GWConstants.DOUBLE_QUOTE, "");
			}

			rg.permission = permission;
			policy.aclList.grants.add(rg);
		}
	}

	public static String makeAclXml(AccessControlPolicy accessControlPolicy, 
								    AccessControlPolicy preAccessControlPolicy,
									boolean hasKeyWord,
									String getAclXml,
									String cannedAcl,
									Bucket bucketInfo,
									String userId, 
									String userName,
									String getGrantRead, 
									String getGrantWrite, 
									String getGrantFullControl, 
									String getGrantReadAcp, 
									String getGrantWriteAcp, 
									S3Parameter s3Parameter) throws GWException {
		
		PublicAccessBlockConfiguration pabc = null;
		if (bucketInfo != null && !Strings.isNullOrEmpty(bucketInfo.getAccess())) {
			try {
				pabc = new XmlMapper().readValue(bucketInfo.getAccess(), PublicAccessBlockConfiguration.class);
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}
		logger.info(GWConstants.LOG_UTILS_CANNED_ACL, cannedAcl);
		logger.info(GWConstants.LOG_UTILS_ACL_XML, getAclXml);

		if (preAccessControlPolicy != null && preAccessControlPolicy.owner != null) {
			accessControlPolicy.owner.id = preAccessControlPolicy.owner.id;
			accessControlPolicy.owner.displayName = preAccessControlPolicy.owner.displayName;
		} else {
			accessControlPolicy.owner.id = userId;
			accessControlPolicy.owner.displayName = userName;
		}

		String aclXml = null;
		if (!hasKeyWord) {
			aclXml = getAclXml;
		}
		if (Strings.isNullOrEmpty(cannedAcl)) {
			if (Strings.isNullOrEmpty(aclXml)) {
				if (Strings.isNullOrEmpty(getGrantRead)
						&& Strings.isNullOrEmpty(getGrantWrite)
						&& Strings.isNullOrEmpty(getGrantReadAcp)
						&& Strings.isNullOrEmpty(getGrantWriteAcp)
						&& Strings.isNullOrEmpty(getGrantFullControl)) {
					Grant priUser = new Grant();
					priUser.grantee = new Grantee();
					priUser.grantee.type = GWConstants.CANONICAL_USER;
					priUser.grantee.id = accessControlPolicy.owner.id;
					priUser.grantee.displayName = accessControlPolicy.owner.displayName;
					priUser.permission = GWConstants.GRANT_FULL_CONTROL;
					accessControlPolicy.aclList.grants.add(priUser);
				}
			}
		} else {
			if (GWConstants.CANNED_ACLS_PRIVATE.equalsIgnoreCase(cannedAcl)) {
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = accessControlPolicy.owner.id;
				priUser.grantee.displayName = accessControlPolicy.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				accessControlPolicy.aclList.grants.add(priUser);
			} else if (GWConstants.CANNED_ACLS_PUBLIC_READ.equalsIgnoreCase(cannedAcl)) {
				if (pabc != null && GWConstants.STRING_TRUE.equalsIgnoreCase(pabc.BlockPublicAcls)) {
					logger.info(GWConstants.LOG_ACCESS_DENIED_PUBLIC_ACLS);
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = accessControlPolicy.owner.id;
				priUser.grantee.displayName = accessControlPolicy.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				accessControlPolicy.aclList.grants.add(priUser);

				Grant pubReadUser = new Grant();
				pubReadUser.grantee = new Grantee();
				pubReadUser.grantee.type = GWConstants.GROUP;
				pubReadUser.grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
				pubReadUser.permission = GWConstants.GRANT_READ;
				accessControlPolicy.aclList.grants.add(pubReadUser);
			} else if (GWConstants.CANNED_ACLS_PUBLIC_READ_WRITE.equalsIgnoreCase(cannedAcl)) {
				if (pabc != null && GWConstants.STRING_TRUE.equalsIgnoreCase(pabc.BlockPublicAcls)) {
					logger.info(GWConstants.LOG_ACCESS_DENIED_PUBLIC_ACLS);
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = accessControlPolicy.owner.id;
				priUser.grantee.displayName = accessControlPolicy.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				accessControlPolicy.aclList.grants.add(priUser);

				Grant pubReadUser = new Grant();
				pubReadUser.grantee = new Grantee();
				pubReadUser.grantee.type = GWConstants.GROUP;
				pubReadUser.grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
				pubReadUser.permission = GWConstants.GRANT_READ;
				accessControlPolicy.aclList.grants.add(pubReadUser);

				Grant pubWriteUser = new Grant();
				pubWriteUser.grantee = new Grantee();
				pubWriteUser.grantee.type = GWConstants.GROUP;
				pubWriteUser.grantee.uri = GWConstants.AWS_GRANT_URI_ALL_USERS;
				pubWriteUser.permission = GWConstants.GRANT_WRITE;
				accessControlPolicy.aclList.grants.add(pubWriteUser);
			} else if (GWConstants.CANNED_ACLS_AUTHENTICATED_READ.equalsIgnoreCase(cannedAcl)) {
				if (pabc != null && GWConstants.STRING_TRUE.equalsIgnoreCase(pabc.BlockPublicAcls)) {
					logger.info(GWConstants.LOG_ACCESS_DENIED_PUBLIC_ACLS);
					throw new GWException(GWErrorCode.ACCESS_DENIED, s3Parameter);
				}
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = accessControlPolicy.owner.id;
				priUser.grantee.displayName = accessControlPolicy.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				accessControlPolicy.aclList.grants.add(priUser);

				Grant authReadUser = new Grant();
				authReadUser.grantee = new Grantee();
				authReadUser.grantee.type = GWConstants.GROUP;
				authReadUser.grantee.uri = GWConstants.AWS_GRANT_URI_AUTHENTICATED_USERS;
				authReadUser.permission = GWConstants.GRANT_READ;
				accessControlPolicy.aclList.grants.add(authReadUser);
			} else if (GWConstants.CANNED_ACLS_BUCKET_OWNER_READ.equalsIgnoreCase(cannedAcl)) {
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = accessControlPolicy.owner.id;
				priUser.grantee.displayName = accessControlPolicy.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				accessControlPolicy.aclList.grants.add(priUser);

				Grant bucketOwnerReadUser = new Grant();
				bucketOwnerReadUser.grantee = new Grantee();
				bucketOwnerReadUser.grantee.type = GWConstants.CANONICAL_USER;
				bucketOwnerReadUser.grantee.id = bucketInfo.getUserId();
				bucketOwnerReadUser.grantee.displayName = bucketInfo.getUserName();
				bucketOwnerReadUser.permission = GWConstants.GRANT_READ;
				accessControlPolicy.aclList.grants.add(bucketOwnerReadUser);
			} else if (GWConstants.CANNED_ACLS_BUCKET_OWNER_FULL_CONTROL.equalsIgnoreCase(cannedAcl)) {
				Grant priUser = new Grant();
				priUser.grantee = new Grantee();
				priUser.grantee.type = GWConstants.CANONICAL_USER;
				priUser.grantee.id = accessControlPolicy.owner.id;
				priUser.grantee.displayName = accessControlPolicy.owner.displayName;
				priUser.permission = GWConstants.GRANT_FULL_CONTROL;
				accessControlPolicy.aclList.grants.add(priUser);

				Grant bucketOwnerFullUser = new Grant();
				bucketOwnerFullUser.grantee = new Grantee();
				bucketOwnerFullUser.grantee.type = GWConstants.CANONICAL_USER;
				bucketOwnerFullUser.grantee.id = bucketInfo.getUserId();
				bucketOwnerFullUser.grantee.displayName = bucketInfo.getUserName();
				bucketOwnerFullUser.permission = GWConstants.GRANT_FULL_CONTROL;
				accessControlPolicy.aclList.grants.add(bucketOwnerFullUser);
			} else if (GWConstants.CANNED_ACLS.contains(cannedAcl)) {
				logger.error(GWErrorCode.NOT_IMPLEMENTED.getMessage() + GWConstants.LOG_ACCESS_CANNED_ACL, cannedAcl);
				throw new GWException(GWErrorCode.NOT_IMPLEMENTED, s3Parameter);
			} else {
				logger.error(HttpServletResponse.SC_BAD_REQUEST + GWConstants.LOG_ACCESS_PROCESS_FAILED);
				throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
			}
		}

		if (!Strings.isNullOrEmpty(getGrantRead)) {
			readAclHeader(getGrantRead, GWConstants.GRANT_READ, accessControlPolicy);
		}
		if (!Strings.isNullOrEmpty(getGrantWrite)) {
			readAclHeader(getGrantWrite, GWConstants.GRANT_WRITE, accessControlPolicy);
		}
		if (!Strings.isNullOrEmpty(getGrantReadAcp)) {
			readAclHeader(getGrantReadAcp, GWConstants.GRANT_READ_ACP, accessControlPolicy);
		}
		if (!Strings.isNullOrEmpty(getGrantWriteAcp)) {
			readAclHeader(getGrantWriteAcp, GWConstants.GRANT_WRITE_ACP, accessControlPolicy);
		}
		if (!Strings.isNullOrEmpty(getGrantFullControl)) {
			readAclHeader(getGrantFullControl, GWConstants.GRANT_FULL_CONTROL, accessControlPolicy);
		}

		if (Strings.isNullOrEmpty(aclXml)) {
			XmlMapper xmlMapper = new XmlMapper();
			try {
				aclXml = xmlMapper.writeValueAsString(accessControlPolicy).replaceAll(GWConstants.WSTXNS, GWConstants.XSI);
			} catch (JsonProcessingException e) {
				PrintStack.logging(logger, e);
				throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
			}
		}

		// check user
		try {
			XmlMapper xmlMapper = new XmlMapper();
			AccessControlPolicy checkAcl = xmlMapper.readValue(aclXml, AccessControlPolicy.class);
			aclXml = checkAcl.toString();
			if (checkAcl.aclList.grants != null) {
				for (Grant user : checkAcl.aclList.grants) {
					if (!Strings.isNullOrEmpty(user.grantee.displayName)
							// && GWUtils.getDBInstance().getIdentityByName(user.grantee.displayName, s3Parameter) == null) {
							&& S3UserManager.getInstance().getUserByName(user.grantee.displayName) == null) {
						logger.info(user.grantee.displayName);
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}

					// KSAN은 userId가 숫자로만 될 필요가 없음
					// if (!Strings.isNullOrEmpty(user.grantee.id) && !user.grantee.id.matches(GWConstants.BACKSLASH_D_PLUS)) {
					// 	logger.info(user.grantee.id);
					// 	throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					// }

					// if (!Strings.isNullOrEmpty(user.grantee.id) && GWUtils.getDBInstance().getIdentityByID(user.grantee.id, s3Parameter) == null) {
					if (!Strings.isNullOrEmpty(user.grantee.id) && S3UserManager.getInstance().getUserById(user.grantee.id) == null) {
						logger.info(user.grantee.id);
						throw new GWException(GWErrorCode.INVALID_ARGUMENT, s3Parameter);
					}
				}
			}
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
			throw new GWException(GWErrorCode.SERVER_ERROR, s3Parameter);
		}

		return aclXml;
	}

	public static String getLocalIP() {
		if (!Strings.isNullOrEmpty(localIP)) {
			return localIP;
		} else {
			InetAddress local = null;
			try {
				local = InetAddress.getLocalHost();
				localIP = local.getHostAddress();
			} catch (UnknownHostException e) {
				logger.error(e.getMessage());
			}
			return localIP;
		}
	}

	public static void initCache(String cacheDisk) {
		if (!Strings.isNullOrEmpty(cacheDisk)) {
			logger.debug(GWConstants.LOG_UTILS_INIT_CACHE);
			for (DiskPool diskpool : DiskManager.getInstance().getDiskPoolList()) {
				for (Server server : diskpool.getServerList()) {
					if (GWUtils.getLocalIP().equals(server.getIp())) {
						for (Disk disk : server.getDiskList()) {
							File file = new File(cacheDisk + disk.getPath() + GWConstants.SLASH + GWConstants.OBJ_DIR);
							file.mkdirs();
							file = new File(cacheDisk + disk.getPath() + GWConstants.SLASH + GWConstants.TEMP_DIR);
							file.mkdirs();
							file = new File(cacheDisk + disk.getPath() + GWConstants.SLASH + GWConstants.TRASH_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.OBJ_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TEMP_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TRASH_DIR);
							file.mkdirs();
						}
					}
				}
			}
		} else {
			logger.debug(GWConstants.LOG_UTILS_INIT_DIR);
			for (DiskPool diskpool : DiskManager.getInstance().getDiskPoolList()) {
				for (Server server : diskpool.getServerList()) {
					if (GWUtils.getLocalIP().equals(server.getIp())) {
						for (Disk disk : server.getDiskList()) {
							File file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.OBJ_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TEMP_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + GWConstants.TRASH_DIR);
							file.mkdirs();
						}
					}
				}
			}
		}
	}

	public static CtrCryptoOutputStream initCtrEncrypt(FileOutputStream out, String customerKey) throws IOException {
		byte[] iv = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };

		byte[] key = new byte[32];
		logger.info(customerKey);
		for (int i = 0; i < 32; i++) {
			if (i < customerKey.getBytes().length)
				key[i] = customerKey.getBytes()[i];
			else
				key[i] = 0;
		}

		Properties property = new Properties();
		property.setProperty(GWConstants.PROPERTY_COMMONS_CRYPTO_STREAM_BUFFER_SIZE, Long.toString(GWConstants.COMMONS_CRYPTO_STREAM_BUFFER_SIZE));
		CtrCryptoOutputStream cipherOut = new CtrCryptoOutputStream(property, out, key, iv);

		return cipherOut;
	}
	
	public static CtrCryptoInputStream initCtrDecrypt(FileInputStream in, String customerKey) throws IOException {
		byte[] iv = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };

		byte[] key = new byte[32];
		logger.info(customerKey);
		for (int i = 0; i < 32; i++) {
			if (i < customerKey.getBytes().length)
				key[i] = customerKey.getBytes()[i];
			else
				key[i] = 0;
		}

		Properties property = new Properties();
		property.setProperty(GWConstants.PROPERTY_COMMONS_CRYPTO_STREAM_BUFFER_SIZE, Long.toString(GWConstants.COMMONS_CRYPTO_STREAM_BUFFER_SIZE));
		CtrCryptoInputStream cipherIn = new CtrCryptoInputStream(property, in, key, iv);

		return cipherIn;
	}

	public static reportRecovery(String message) {      
        try { 
            MQSender mq1ton = new MQSender(MConfig.getInstance().getPortalIp(), GWConstants.UTILITY_EXCHANGE_KEY, GWConstants.MESSAGE_QUEUE_OPTION, "");
            mq1ton.send(message);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}
	}
}