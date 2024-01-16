/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License. See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.libs;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.pspace.backend.libs.Ksan.Data.S3RegionData;
import com.pspace.backend.libs.data.Metering.DateRange;

public class Utility {
	static final Logger logger = LoggerFactory.getLogger(Utility.class);
	private static final int TIME_OUT = 3 * 1000;
	static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul");

	private Utility() {
	}

	public static String getServiceId(String filePath) {
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			return reader.readLine();
		} catch (Exception e) {
			return null;
		}
	}

	public static String getDateTime() {
		Calendar time = Calendar.getInstance(SEOUL, Locale.KOREA);
		var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return dateFormat.format(time.getTime());
	}

	public static int string2Time(String value) {
		if (value == null || value.isBlank())
			return -1;

		var token = value.split(":");
		if (token.length > 1) {
			int hour = Integer.parseInt(token[0]);
			int min = Integer.parseInt(token[1]);
			return hour * 60 + min;
		} else
			return -1;
	}

	public static boolean isRun(int time) {
		return time == getNowTime2Int();
	}

	public static int getNowDay() {
		Calendar time = Calendar.getInstance(SEOUL, Locale.KOREA);
		return time.get(Calendar.DAY_OF_YEAR);
	}

	public static int getNowTime2Int() {
		Calendar time = Calendar.getInstance(SEOUL, Locale.KOREA);
		var hour = time.get(Calendar.HOUR_OF_DAY);
		var minute = time.get(Calendar.MINUTE);

		return hour * 60 + minute;
	}

	public static void delay(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			logger.error("", e);
			Thread.currentThread().interrupt();
		}
	}

	public static void delay(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			logger.error("", e);
			Thread.currentThread().interrupt();
		}
	}

	public static boolean checkAlive(String url) {
		try {
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet getRequest = new HttpGet(url);
			RequestConfig requestConfig = RequestConfig.custom()
					.setSocketTimeout(TIME_OUT)
					.setConnectTimeout(TIME_OUT)
					.setConnectionRequestTimeout(TIME_OUT)
					.build();
			getRequest.setConfig(requestConfig);
			client.execute(getRequest);
			getRequest.releaseConnection();
			return true;
		} catch (Exception e) {
			logger.error("URL : {}", url, e);
		}
		return false;
	}

	public static InputStream createBody(String body) {
		return new ByteArrayInputStream(body.getBytes());
	}

	public static AmazonS3 createClient(S3RegionData region) {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(region.accessKey, region.secretKey);
		logger.debug("Client : {}, {}", region.accessKey, region.secretKey);

		return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(region.getHttpURL(), ""))
				.withPathStyleAccessEnabled(true).build();
	}

	public static DateRange getDateRangeMinute(int interval) {
		var start = Calendar.getInstance(SEOUL, Locale.KOREA);
		var end = (Calendar) start.clone();
		start.add(Calendar.MINUTE, -interval);
		end.add(Calendar.MINUTE, -1);
		var strStart = String.format("%04d-%02d-%02d %02d:%02d:00",
				start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1, start.get(Calendar.DAY_OF_MONTH),
				start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE));
		var strEnd = String.format("%04d-%02d-%02d %02d:%02d:59",
				end.get(Calendar.YEAR), end.get(Calendar.MONTH) + 1,
				end.get(Calendar.DAY_OF_MONTH), end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE));
		return new DateRange(strStart, strEnd);
	}

	public static DateRange getDateRangeHour(int interval) {
		var start = Calendar.getInstance(SEOUL, Locale.KOREA);
		var end = (Calendar) start.clone();
		start.add(Calendar.HOUR, -interval);
		end.add(Calendar.HOUR, -1);
		var strStart = String.format("%04d-%02d-%02d %02d:00:00",
				start.get(Calendar.YEAR), start.get(Calendar.MONTH) + 1, start.get(Calendar.DAY_OF_MONTH),
				start.get(Calendar.HOUR_OF_DAY));
		var strEnd = String.format("%04d-%02d-%02d %02d:59:59",
				end.get(Calendar.YEAR), end.get(Calendar.MONTH) + 1, end.get(Calendar.DAY_OF_MONTH),
				end.get(Calendar.HOUR_OF_DAY));
		return new DateRange(strStart, strEnd);
	}
}