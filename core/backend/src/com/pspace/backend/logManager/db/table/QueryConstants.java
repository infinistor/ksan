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
package com.pspace.backend.logManager.db.table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QueryConstants {
	public static final int MAX_QUERY_SIZE = 1000;
	public static final String DB_DATE_FORMAT = "%Y-%m-%d %k:%i:%s";

	public static String getTodayTableName(String tableName) {
		var now = LocalDate.now();
		var formatter = DateTimeFormatter.ofPattern("_yyyy_MM");
		return tableName + now.format(formatter);
	}
	
	public static int getMonth() {
		var now = LocalDate.now();
		return now.getMonthValue();
	}
	
	// 1시간 뒤에 월이 바뀌는지 체크
	public static boolean isMonthChanged(int month) {
		var now = LocalDateTime.now();
		now.plusHours(1);
		return now.getMonthValue() != month;
	}

	public static int nextMonth(int month) {
		return month % 12 + 1;
	}
}
