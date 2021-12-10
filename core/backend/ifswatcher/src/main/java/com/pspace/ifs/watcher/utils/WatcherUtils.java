/*
 * Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
 * KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 
 * 3 of the License.  See LICENSE.md for details
 *
 * 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
 * KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
 * KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
 */

package com.pspace.ifs.watcher.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Splitter;
import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatcherUtils {

	private static final Logger logger = LoggerFactory.getLogger(WatcherUtils.class);
	private static final Escaper urlEscaper = new PercentEscaper("*-./_", false);

	/**
	 * Parse ISO 8601 timestamp into seconds since 1970.
	 * 
	 */
	public static long parseTime8601(String date) {
		SimpleDateFormat formatter = null;
		if(date == null) {
			return 0;
		}

		if( date.length() >= 23 ) {
			formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		} else if ( date.contains(":") && date.length() < 23 ) {
			formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		} else if ( !date.contains(":") && date.length() < 23 ) {
			formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
		}

		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		logger.debug("8601date {}", date);
		try {
			return formatter.parse(date).getTime() / 1000;
		} catch (ParseException pe) {
			PrintStack.logging(logger, pe);
		}

		return 0;
	}

	public static String maybeQuoteETag(String eTag) {
		if (!eTag.startsWith("\"") && !eTag.endsWith("\"")) {
			eTag = "\"" + eTag + "\"";
		}
		return eTag;
	}

	private static boolean startsWithIgnoreCase(String string, String prefix) {
		return string.toLowerCase().startsWith(prefix.toLowerCase());
	}

	public static boolean isField(String string, String field) {
		return startsWithIgnoreCase(string, "Content-Disposition: form-data; name=\"" + field + "\"");
	}

	public static boolean startsField(String string, String field) {
		return startsWithIgnoreCase(string, "Content-Disposition: form-data; name=\"" + field);
	}

	static byte[] hmac(String algorithm, byte[] data, byte[] key) {
		try {
			Mac mac = Mac.getInstance(algorithm);
			mac.init(new SecretKeySpec(key, algorithm));
			return mac.doFinal(data);
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	// Encode blob name if client requests it. This allows for characters
	// which XML 1.0 cannot represent.
	public static String encodeBlob(String encodingType, String blobName) {
		if (encodingType != null && encodingType.equalsIgnoreCase("url")) {
			return urlEscaper.escape(blobName);
		} else {
			return blobName;
		}
	}

	public static boolean validateIpAddress(String string) {
		List<String> parts = Splitter.on('.').splitToList(string);
		if (parts.size() != 4) {
			return false;
		}
		for (String part : parts) {
			try {
				int num = Integer.parseInt(part);
				if (num < 0 || num > 255) {
					return false;
				}
			} catch (NumberFormatException nfe) {
				return false;
			}
		}
		return true;
	}

	public static boolean constantTimeEquals(String x, String y) {
		return MessageDigest.isEqual(x.getBytes(StandardCharsets.UTF_8), y.getBytes(StandardCharsets.UTF_8));
	}

	public static String infini_passdecoding(String infini_password) throws Exception {
		String password = null;
		try {
			Process p = Runtime.getRuntime().exec("/usr/local/pspace/bin/ifs_passwd_dec " + infini_password);
			int exitcode = p.waitFor();

			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = reader.readLine();
			password = line;

			if (line != null) {
				if (exitcode != 0) {
					throw new Exception("Password decoding fail");
				}
			} else {
				throw new Exception("Password decoding fail");
			}
		} catch (IOException e) {
			PrintStack.logging(logger, e);
			throw e;
		} catch (InterruptedException e) {
			PrintStack.logging(logger, e);
			throw e;
		}

		return password;
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
		if (first.length() > 1 && first.charAt(0) == '*' && second.length() == 0)
			return false;

		// If the first string contains '?',
		// or current characters of both strings match
		if ((first.length() > 1 && first.charAt(0) == '?')
				|| (first.length() != 0 && second.length() != 0 && first.charAt(0) == second.charAt(0)))
			return likematch(first.substring(1), second.substring(1));

		// If there is *, then there are two possibilities
		// a) We consider current character of second string
		// b) We ignore current character of second string.
		if (first.length() > 0 && first.charAt(0) == '*')
			return likematch(first.substring(1), second) || likematch(first, second.substring(1));
		return false;
	}
}