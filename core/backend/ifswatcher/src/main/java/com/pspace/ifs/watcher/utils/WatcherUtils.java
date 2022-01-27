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

import java.util.List;
import com.google.common.base.Splitter;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class WatcherUtils {

	//private static final Logger logger = LoggerFactory.getLogger(WatcherUtils.class);

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