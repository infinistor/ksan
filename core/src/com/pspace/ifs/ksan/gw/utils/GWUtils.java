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
import com.pspace.ifs.ksan.gw.format.AccessControlPolicy.Owner;
import com.pspace.ifs.ksan.gw.format.AclTransfer;
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
			for (DiskPool diskpool : DiskManager.getInstance().getDiskPoolList()) {
				for (Server server : diskpool.getServerList()) {
					if (GWUtils.getLocalIP().equals(server.getIp())) {
						for (Disk disk : server.getDiskList()) {
							File file = new File(cacheDisk + disk.getPath() + GWConstants.SLASH + Constants.OBJ_DIR);
							file.mkdirs();
							file = new File(cacheDisk + disk.getPath() + GWConstants.SLASH + Constants.TEMP_DIR);
							file.mkdirs();
							file = new File(cacheDisk + disk.getPath() + GWConstants.SLASH + Constants.TRASH_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + Constants.OBJ_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + Constants.TEMP_DIR);
							file.mkdirs();
							file = new File(disk.getPath() + GWConstants.SLASH + Constants.TRASH_DIR);
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
		logger.info("init ctr decrypt key : {}", customerKey);
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
}