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
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.HashMap;

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
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.Owner;
import com.pspace.ifs.ksan.gw.format.CORSConfiguration;
import com.pspace.ifs.ksan.gw.format.LifecycleConfiguration;
import com.pspace.ifs.ksan.gw.format.Policy;
import com.pspace.ifs.ksan.gw.format.PublicAccessBlockConfiguration;
import com.pspace.ifs.ksan.gw.format.Tagging;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant;
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.AccessControlList.Grant.Grantee;
import com.pspace.ifs.ksan.gw.format.CORSConfiguration.CORSRule;
import com.pspace.ifs.ksan.gw.format.Policy.Statement;
import com.pspace.ifs.ksan.gw.format.Tagging.TagSet.Tag;
import com.pspace.ifs.ksan.libs.identity.S3Metadata;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.objmanager.Bucket;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.disk.Disk;
import com.pspace.ifs.ksan.libs.disk.DiskPool;
import com.pspace.ifs.ksan.libs.disk.Server;
import com.pspace.ifs.ksan.libs.Constants;

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

	public static long parseTime8601(String date) {
		SimpleDateFormat formatter = null;
		if (date == null) {
			return 0;
		}

		if (date.length() >= 23) {
			formatter = new SimpleDateFormat(GWConstants.ISO_8601_TIME_FORMAT_MILI);
		} else if (date.contains(":") && date.length() < 23) {
			formatter = new SimpleDateFormat(GWConstants.ISO_8601_TIME_SIMPLE_FORMAT);
		} else if (!date.contains(":") && date.length() < 23) {
			formatter = new SimpleDateFormat(GWConstants.ISO_8601_TIME_FORMAT);
		}

		if (formatter == null) {
			return 0;
		}

		formatter.setTimeZone(TimeZone.getTimeZone(GWConstants.UTC));
		logger.debug(GWConstants.LOG_8601_DATE, date);
		try {
			return formatter.parse(date).getTime() / 1000;
		} catch (ParseException pe) {
			PrintStack.logging(logger, pe);
		}

		return 0;
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
		return MessageDigest.isEqual(x.getBytes(StandardCharsets.UTF_8), y.getBytes(StandardCharsets.UTF_8));
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
			int length = 0;
			for (DiskPool diskpool : DiskManager.getInstance().getDiskPoolList()) {
				for (Server server : diskpool.getServerList()) {
					if (GWUtils.getLocalIP().equals(server.getIp())) {
						for (Disk disk : server.getDiskList()) {
							StringBuilder sb = new StringBuilder();
							sb.append(cacheDisk);
							sb.append(disk.getPath());
							sb.append(Constants.SLASH); 
							length = sb.length();
							sb.append(Constants.OBJ_DIR);

							File file = new File(sb.toString());
							if (!file.exists()) {
								if (!file.mkdirs()) {
									logger.error(GWConstants.LOG_UTILS_GW_DISK_MAKE_DIR_FAILED, file.getAbsolutePath());
								}
							}

							// StringBuilder sbTemp = sb.delete(length, sb.length());
							// sbTemp.append(Constants.TEMP_DIR);
							// file = new File(sbTemp.toString());
							// if (!file.exists()) {
							// 	if (!file.mkdirs()) {
							// 		logger.error(GWConstants.LOG_UTILS_GW_DISK_MAKE_DIR_FAILED, file.getAbsolutePath());
							// 	}
							// }

							StringBuilder sbTemp = sb.delete(length, sb.length());
							sbTemp.append(Constants.TRASH_DIR);
							file = new File(sbTemp.toString());
							if (!file.exists()) {
								if (!file.mkdirs()) {
									logger.error(GWConstants.LOG_UTILS_GW_DISK_MAKE_DIR_FAILED, file.getAbsolutePath());
								}
							}
						}
					}
				}
			}
		}
	}

	public static void initDisk() {
		for (DiskPool diskpool : DiskManager.getInstance().getDiskPoolList()) {
			for (Server server : diskpool.getServerList()) {
				if (GWUtils.getLocalIP().equals(server.getIp())) {
					for (Disk disk : server.getDiskList()) {
						File file = new File(disk.getPath() + GWConstants.SLASH + Constants.OBJ_DIR);
						if (!file.exists()) {
							if (!file.mkdirs()) {
								logger.error(GWConstants.LOG_UTILS_GW_DISK_MAKE_DIR_FAILED, file.getAbsolutePath());
							}
						}
						// file = new File(disk.getPath() + GWConstants.SLASH + Constants.TEMP_DIR);
						// if (!file.exists()) {
						// 	if (!file.mkdirs()) {
						// 		logger.error(GWConstants.LOG_UTILS_GW_DISK_MAKE_DIR_FAILED, file.getAbsolutePath());
						// 	}
						// }
						file = new File(disk.getPath() + GWConstants.SLASH + Constants.TRASH_DIR);
						if (!file.exists()) {
							if (!file.mkdirs()) {
								logger.error(GWConstants.LOG_UTILS_GW_DISK_MAKE_DIR_FAILED, file.getAbsolutePath());
							}
						}
					}
				}
			}
		}
	}

	public static void initEC() {
		HashMap<String, String> localDiskInfoMap = DiskManager.getInstance().getLocalDiskInfo();

		if (localDiskInfoMap!= null) {
			localDiskInfoMap.forEach((diskId, diskPath) -> {
				int numberOfCodingChunks = DiskManager.getInstance().getECM(diskId);
				int numberOfDataChunks = DiskManager.getInstance().getECK(diskId);
				if (numberOfCodingChunks > 0 && numberOfDataChunks > 0) {
					// check EC
					StringBuilder sb = new StringBuilder();
					sb.append(diskPath);
					sb.append(Constants.SLASH);
					sb.append(Constants.EC_DIR);
					File file = new File(sb.toString());
					if (file.exists()) {
						if (!file.mkdirs()) {
							logger.error(GWConstants.LOG_UTILS_GW_DISK_MAKE_DIR_FAILED, file.getAbsolutePath());
						}
					}
				}
			});
		}
    }

	private static void makeSubDirs(String path) {
		byte[] data = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
		
		byte[] subPath = new byte[6];
		subPath[0] = '/';
		subPath[3] = '/';

		File file = null;
		for (int i = 0; i < 36; i++) {
			for (int j = 0; j < 36; j++) {
				for (int k = 0; k < 36; k++) {
					for (int l = 0; l < 36; l++) {
						subPath[1] = data[i];
						subPath[2] = data[j];
						subPath[4] = data[k];
						subPath[5] = data[l];

						file = new File(path + new String(subPath, StandardCharsets.UTF_8));
						if (!file.exists()) {
							if (!file.mkdirs()) {
								logger.error(GWConstants.LOG_UTILS_GW_DISK_MAKE_DIR_FAILED, file.getAbsolutePath());
							}
						}
					}
				}
			}
		}
	}

	// public static CtrCryptoOutputStream initCtrEncrypt(FileOutputStream out, String customerKey) throws IOException {
	// 	byte[] iv = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };

	// 	byte[] key = new byte[32];
	// 	logger.info(customerKey);
	// 	for (int i = 0; i < 32; i++) {
	// 		if (i < customerKey.getBytes().length)
	// 			key[i] = customerKey.getBytes()[i];
	// 		else
	// 			key[i] = 0;
	// 	}

	// 	Properties property = new Properties();
	// 	property.setProperty(GWConstants.PROPERTY_COMMONS_CRYPTO_STREAM_BUFFER_SIZE, Long.toString(GWConstants.COMMONS_CRYPTO_STREAM_BUFFER_SIZE));
	// 	CtrCryptoOutputStream cipherOut = new CtrCryptoOutputStream(property, out, key, iv);

	// 	return cipherOut;
	// }

	public static CtrCryptoOutputStream initCtrEncrypt(OutputStream out, String customerKey) throws IOException {
		byte[] iv = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };

		byte[] key = new byte[32];
		logger.info(customerKey);
		for (int i = 0; i < 32; i++) {
			if (i < customerKey.getBytes(StandardCharsets.UTF_8).length)
				key[i] = customerKey.getBytes(StandardCharsets.UTF_8)[i];
			else
				key[i] = 0;
		}

		Properties property = new Properties();
		property.setProperty(GWConstants.PROPERTY_COMMONS_CRYPTO_STREAM_BUFFER_SIZE, Long.toString(GWConstants.COMMONS_CRYPTO_STREAM_BUFFER_SIZE));
		CtrCryptoOutputStream cipherOut = new CtrCryptoOutputStream(property, out, key, iv);

		return cipherOut;
	}
	
	public static CtrCryptoInputStream initCtrDecrypt(InputStream in, String customerKey) throws IOException {
		byte[] iv = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };

		byte[] key = new byte[32];
		logger.info("init ctr decrypt key : {}", customerKey);
		for (int i = 0; i < 32; i++) {
			if (i < customerKey.getBytes(StandardCharsets.UTF_8).length)
				key[i] = customerKey.getBytes(StandardCharsets.UTF_8)[i];
			else
				key[i] = 0;
		}

		Properties property = new Properties();
		property.setProperty(GWConstants.PROPERTY_COMMONS_CRYPTO_STREAM_BUFFER_SIZE, Long.toString(GWConstants.COMMONS_CRYPTO_STREAM_BUFFER_SIZE));
		CtrCryptoInputStream cipherIn = new CtrCryptoInputStream(property, in, key, iv);

		return cipherIn;
	}

	public static String[] xffncr(String[] xffs) {
		ArrayList<String> xffncrlist = new ArrayList<String>();
		ArrayList<String[]> orderxfflist = new ArrayList<String[]>();

		combinationOderXFFS(xffs, orderxfflist);

		for (String[] xff : orderxfflist) {
			String line = "";

			for (int i = 0; i < xff.length; i++) {
				if (xff.length > 1 && xff.length - 1 != i) {
					line += xff[i] + ", ";
				} else {
					line += xff[i];
				}
			}

			xffncrlist.add(line);
			// System.out.println(line);
		}

		String[] str = new String[xffncrlist.size()];
		str = (String[]) xffncrlist.toArray(str);

		return str;
	}

	static void combinationOderXFFS(String[] xffs, ArrayList<String[]> orderxfflist) {
		int n = xffs.length;
		for (int i = 1; i <= n; i++) {
			String[] permutation = new String[i];
			int[] check = new int[n];
			ordercombination(0, i, n, xffs, check, permutation, orderxfflist);
		}
	}

	public static void ordercombination(int level, int subsetnum, int setnum, String[] xffs, int[] check,
			String[] permutation, ArrayList<String[]> orderxfflist) {
		if (level == subsetnum) {
			orderxfflist.add(permutation.clone());
		} else {
			for (int i = 0; i < setnum; i++) {
				if (check[i] == 0) {
					check[i] = 1;
					permutation[level] = xffs[i].trim();
					ordercombination(level + 1, subsetnum, setnum, xffs, check, permutation, orderxfflist);
					check[i] = 0;
				}
			}
		}
	}

	/**
	 * Parse ISO 8601 timestamp into seconds since 1970.
	 * 
	 * @throws GWException
	 */
	public static long parseRetentionTimeExpire(String date, S3Parameter s3Parameter) throws GWException {
		SimpleDateFormat formatter = new SimpleDateFormat(GWConstants.ISO_8601_TIME_FORMAT_MILI);
		formatter.setTimeZone(TimeZone.getTimeZone(GWConstants.UTC));
		logger.debug(GWConstants.LOG_8601_DATE, date);
		try {
			return formatter.parse(date).getTime() / 1000;
		} catch (ParseException pe) {
			PrintStack.logging(logger, pe);
			throw new GWException(GWErrorCode.BAD_REQUEST, s3Parameter);
		}
	}

	public static String calculateMD5HashBase64(String input) {
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md5Digest.digest(input.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

	public static boolean skipLifecycleCheck(LifecycleConfiguration.Rule rule, Metadata meta) {
		// 필터가 존재할 경우
		if (rule.filter != null) {
			// And 필터가 존재할 경우
			if (rule.filter.and != null) {
				// Prefix가 설정되어 있을 경우
				if (!Strings.isNullOrEmpty(rule.filter.and.prefix)) {
					// Prefix가 일치하지 않을 경우 스킵
					if (!meta.getPath().startsWith(rule.filter.and.prefix)) {
						return true;
					}
				}

				// 태그 필터가 설정되어 있는 경우
				if (rule.filter.and.tag.size() > 0) {
					// 오브젝트의 모든 태크를 비교
					int tagCount = rule.filter.and.tag.size();
					for (var filterTag : rule.filter.and.tag) {
						if (!Strings.isNullOrEmpty(meta.getTag())) {
							XmlMapper xmlMapper = new XmlMapper();
							try {
								Tagging tagging = xmlMapper.readValue(meta.getTag(), Tagging.class);
								if (tagging != null && tagging.tagset != null && tagging.tagset.tags != null) {
									for (Tag tag : tagging.tagset.tags) {
										if (filterTag.key.equals(tag.key) && filterTag.value.equals(tag.value)) {
											tagCount--;
										}
									}
								}
							} catch (JsonProcessingException e) {
								PrintStack.logging(logger, e);
							}
						}
					}

					// 필터에 설정된 태그 목록이 오브젝트의 태그 목록에 포함되지 않을 경우 스킵
					if (tagCount > 0) {
						return true;
					}
				}
			}
			// Prefix가 설정되어 있을 경우
			else if (!Strings.isNullOrEmpty(rule.filter.prefix)) {
				if (!meta.getPath().startsWith(rule.filter.prefix)) {
					return true;
				}
			}
			// 태그 필터가 설정되어 있을 경우
			else if (rule.filter.tag != null) {
				var filterTag = rule.filter.tag;
				boolean find = false;

				// 오브젝트의 모든 태그를 비교
				if (!Strings.isNullOrEmpty(meta.getTag())) {
					XmlMapper xmlMapper = new XmlMapper();
					try {
						Tagging tagging = xmlMapper.readValue(meta.getTag(), Tagging.class);
						if (tagging != null && tagging.tagset != null && tagging.tagset.tags != null) {
							for (Tag tag : tagging.tagset.tags) {
								if (filterTag.key.equals(tag.key) && filterTag.value.equals(tag.value)) {
									find = true;
								}
							}
						}
					} catch (JsonProcessingException e) {
						PrintStack.logging(logger, e);
					}
				}
				// 필터에 설정된 태그가 오브젝트의 태그에 존재하지 않을 경우 스킵
				if (!find) {
					return true;
				}
			}

			// 최소 크기 필터가 설정되어 있을 경우
			if (!Strings.isNullOrEmpty(rule.filter.objectSizeGreaterThan)) {
				// 오브젝트가 최소 크기보다 작을 경우 스킵
				long minFileSize = Long.parseLong(rule.filter.objectSizeGreaterThan);
				if (meta.getSize() < minFileSize) {
					return true;
				}
			}
			// 최대 크기 필터가 설정되어 있을 경우
			else if (!Strings.isNullOrEmpty(rule.filter.objectSizeLessThan)) {
				// 오브젝트가 최대 크기보다 클 경우 스킵
				long maxFileSize = Long.parseLong(rule.filter.objectSizeLessThan);
				if (meta.getSize() > maxFileSize) {
					return true;
				}
			}
		}
		return false;
	}

	public static S3Metadata checkForLifecycle(S3Metadata s3Metadata, Metadata metadata, String lifecycle) throws GWException {
		String oldestDate = GWConstants.EMPTY_STRING;
		try {
			if (!Strings.isNullOrEmpty(lifecycle)) {
				logger.info("lifecycle : {}", lifecycle);
				XmlMapper xmlMapper = new XmlMapper();
				LifecycleConfiguration BucketLifecycle = xmlMapper.readValue(lifecycle, LifecycleConfiguration.class);
				if (BucketLifecycle.rules != null && BucketLifecycle.rules.size() > 0) {
					for (LifecycleConfiguration.Rule r : BucketLifecycle.rules) {
						if (GWUtils.skipLifecycleCheck(r, metadata) == false) {
							// expiration rule
							if (r.expiration != null) {
								if (!Strings.isNullOrEmpty(r.expiration.date)) {
									if (metadata.getLastVersion()) {
										// expiry-date="Fri, 23 Dec 2012 00:00:00 GMT", rule-id="picture-deletion-rule"
										SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
										sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // 시간대를 GMT로 설정
										Date expireDate;
										Date olderDate = null;

										try {
											expireDate = sdf.parse(r.expiration.date);
											if (!Strings.isNullOrEmpty(oldestDate) )
												olderDate = sdf.parse(oldestDate);
										} catch (ParseException e) {
											PrintStack.logging(logger, e);
											continue;
										} 
										
										if (olderDate != null) {
											if ( olderDate.compareTo(expireDate) > 0) {
												s3Metadata.setExpirationDate(
													"expiry-date=\"" + r.expiration.date + "\", rule-id=\"" + r.id + "\"");
												oldestDate = r.expiration.date;
											}
										} else {
											// UTC 형식의 문자열로 변환
											s3Metadata.setExpirationDate(
												"expiry-date=\"" + r.expiration.date + "\", rule-id=\"" + r.id + "\"");
											oldestDate = r.expiration.date;
										}
									}
								}

								if (!Strings.isNullOrEmpty(r.expiration.days)) {
									if (metadata.getLastVersion()) {
										Calendar cal = Calendar.getInstance();
										cal.setTime(s3Metadata.getLastModified());
										cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(r.expiration.days));
										Date newDate = cal.getTime();

										SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
										sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // 시간대를 GMT로 설정
										String utcDate = sdf.format(newDate); // UTC 형식의 문자열로 변환
										Date olderDate = null;

										try {
											if (!Strings.isNullOrEmpty(oldestDate)) {
												olderDate = sdf.parse(oldestDate);
											}
										} catch (ParseException e) {
											PrintStack.logging(logger, e);
											continue;
										}

										if (olderDate != null) {
											if (olderDate.compareTo(newDate) > 0) {
												s3Metadata.setExpirationDate("expiry-date=\"" + utcDate + "\", rule-id=\"" + r.id + "\"");
												oldestDate = utcDate;
											}
										} else {
											s3Metadata.setExpirationDate("expiry-date=\"" + utcDate + "\", rule-id=\"" + r.id + "\"");
											oldestDate = utcDate;
										}
									}
								}
							}

							// expiration noncurrency rule
							if (r.versionexpiration != null) {
								if (!Strings.isNullOrEmpty(r.versionexpiration.noncurrentDays)) {
									if (metadata.getLastVersion()) {
										Calendar cal = Calendar.getInstance();
										cal.setTime(s3Metadata.getLastModified());
										cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(r.versionexpiration.noncurrentDays));
										Date newDate = cal.getTime();

										SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
										sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // 시간대를 UTC로 설정
										String utcDate = sdf.format(newDate); // UTC 형식의 문자열로 변환
										s3Metadata.setExpirationDate("expiry-date=\"" + utcDate + "\", rule-id=\"" + r.id + "\"");
									}
								}
							}
						}
					}
				}
			}
		} catch (JsonProcessingException e) {
			PrintStack.logging(logger, e);
		}

		return s3Metadata;
	}
}