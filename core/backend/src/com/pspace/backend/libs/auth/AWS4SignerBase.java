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
package com.pspace.backend.libs.auth;

import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.amazonaws.util.BinaryUtils;

public abstract class AWS4SignerBase {

	public static final String EMPTY_BODY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
	public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

	public static final String SCHEME = "AWS4";
	public static final String ALGORITHM = "HMAC-SHA256";
	public static final String TERMINATOR = "aws4_request";

	public static final String ISO8601BasicFormat = "yyyyMMdd'T'HHmmss'Z'";
	public static final String DateStringFormat = "yyyyMMdd";

	protected URL endpointUrl;
	protected String httpMethod;
	protected String serviceName;
	protected String regionName;

	protected final SimpleDateFormat dateTimeFormat;
	protected final SimpleDateFormat dateStampFormat;

	public AWS4SignerBase(URL endpointUrl, String httpMethod,
			String serviceName, String regionName) {
		this.endpointUrl = endpointUrl;
		this.httpMethod = httpMethod;
		this.serviceName = serviceName;
		this.regionName = regionName;

		dateTimeFormat = new SimpleDateFormat(ISO8601BasicFormat);
		dateTimeFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
		dateStampFormat = new SimpleDateFormat(DateStringFormat);
		dateStampFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
	}

	protected static String getCanonicalizeHeaderNames(Map<String, String> headers) {
		List<String> sortedHeaders = new ArrayList<String>();
		sortedHeaders.addAll(headers.keySet());
		Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

		StringBuilder buffer = new StringBuilder();
		for (String header : sortedHeaders) {
			if (buffer.length() > 0)
				buffer.append(";");
			buffer.append(header.toLowerCase());
		}

		return buffer.toString();
	}

	/**
	 * Computes the canonical headers with values for the request. For AWS4, all
	 * headers must be included in the signing process.
	 */
	protected static String getCanonicalizedHeaderString(Map<String, String> headers) {
		if (headers == null || headers.isEmpty()) {
			return "";
		}

		// step1: sort the headers by case-insensitive order
		List<String> sortedHeaders = new ArrayList<String>();
		sortedHeaders.addAll(headers.keySet());
		Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

		// step2: form the canonical header:value entries in sorted order.
		// Multiple white spaces in the values should be compressed to a single
		// space.
		StringBuilder buffer = new StringBuilder();
		for (String key : sortedHeaders) {
			buffer.append(key.toLowerCase().replaceAll("\\s+", " ") + ":" + headers.get(key).replaceAll("\\s+", " "));
			buffer.append("\n");
		}

		return buffer.toString();
	}

	/**
	 * Returns the canonical request string to go into the signer process; this
	 * consists of several canonical sub-parts.
	 * 
	 * @return
	 */
	protected static String getCanonicalRequest(URL endpoint,
			String httpMethod,
			String queryParameters,
			String canonicalizedHeaderNames,
			String canonicalizedHeaders,
			String bodyHash) {
		String canonicalRequest = httpMethod + "\n" +
				getCanonicalizedResourcePath(endpoint) + "\n" +
				queryParameters + "\n" +
				canonicalizedHeaders + "\n" +
				canonicalizedHeaderNames + "\n" +
				bodyHash;
		return canonicalRequest;
	}

	/**
	 * Returns the canonicalized resource path for the service endpoint.
	 */
	protected static String getCanonicalizedResourcePath(URL endpoint) {
		if (endpoint == null) {
			return "/";
		}
		String path = endpoint.getPath();
		if (path == null || path.isEmpty()) {
			return "/";
		}

		String encodedPath = NetUtils.urlEncode(path, true);
		if (encodedPath.startsWith("/")) {
			return encodedPath;
		} else {
			return "/".concat(encodedPath);
		}
	}

	/**
	 * Examines the specified query string parameters and returns a
	 * canonicalized form.
	 * <p>
	 * The canonicalized query string is formed by first sorting all the query
	 * string parameters, then URI encoding both the key and value and then
	 * joining them, in order, separating key value pairs with an '&'.
	 *
	 * @param parameters
	 *                   The query string parameters to be canonicalized.
	 *
	 * @return A canonicalized form for the specified query string parameters.
	 */
	public static String getCanonicalizedQueryString(Map<String, String> parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return "";
		}

		SortedMap<String, String> sorted = new TreeMap<String, String>();

		Iterator<Map.Entry<String, String>> pairs = parameters.entrySet().iterator();
		while (pairs.hasNext()) {
			Map.Entry<String, String> pair = pairs.next();
			String key = pair.getKey();
			String value = pair.getValue();
			sorted.put(NetUtils.urlEncode(key, false), NetUtils.urlEncode(value, false));
		}

		StringBuilder builder = new StringBuilder();
		pairs = sorted.entrySet().iterator();
		while (pairs.hasNext()) {
			Map.Entry<String, String> pair = pairs.next();
			builder.append(pair.getKey());
			builder.append("=");
			builder.append(pair.getValue());
			if (pairs.hasNext()) {
				builder.append("&");
			}
		}

		return builder.toString();
	}

	protected static String getStringToSign(String scheme, String algorithm, String dateTime, String scope,
			String canonicalRequest) {
		String stringToSign = scheme + "-" + algorithm + "\n" +
				dateTime + "\n" +
				scope + "\n" +
				BinaryUtils.toHex(hash(canonicalRequest));
		return stringToSign;
	}

	public static byte[] hash(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(text.getBytes("UTF-8"));
			return md.digest();
		} catch (Exception e) {
			throw new RuntimeException("Unable to compute hash while signing request: " + e.getMessage(), e);
		}
	}

	public static byte[] hash(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data);
			return md.digest();
		} catch (Exception e) {
			throw new RuntimeException("Unable to compute hash while signing request: " + e.getMessage(), e);
		}
	}

	protected static byte[] sign(String stringData, byte[] key, String algorithm) {
		try {
			byte[] data = stringData.getBytes("UTF-8");
			Mac mac = Mac.getInstance(algorithm);
			mac.init(new SecretKeySpec(key, algorithm));
			return mac.doFinal(data);
		} catch (Exception e) {
			throw new RuntimeException("Unable to calculate a request signature: " + e.getMessage(), e);
		}
	}
}
