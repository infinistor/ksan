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
package com.pspace.backend.libs.data.s3;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.backend.libs.s3.LoggingConfiguration;
import com.pspace.backend.libs.s3.NotificationConfiguration;
import com.pspace.backend.libs.s3.ReplicationConfiguration;
import com.pspace.ifs.ksan.objmanager.Bucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author KJW
 */
public class S3BucketData {
	static final Logger logger = LoggerFactory.getLogger(S3BucketData.class);

	public String bucketName;
	public LoggingConfiguration loggings;
	public NotificationConfiguration notification;
	public List<ReplicationData> replications;

	public boolean isLogging;
	public boolean isNotification;
	public boolean isReplication;

	public S3BucketData(String bucketName) {
		this.bucketName = bucketName;
		this.isLogging = false;
		this.isNotification = false;
		this.isReplication = false;
	}

	public S3BucketData(Bucket bucket) {
		this.bucketName = bucket.getName();
		setLoggingConfiguration(bucket.getLogging());
		setReplicationConfiguration(bucket.getReplication());
		this.isNotification = false;
		setNotificationConfiguration(bucket.getNotification());
	}

	/**
	 * 로깅설정 텍스트를 언마샬링하여 필요한 설정을 추출한다.
	 * 
	 * @param strLogging
	 *            로깅 설정 텍스트
	 * @return 성공 / 실패 여부
	 */
	public boolean setLoggingConfiguration(String strLogging) {
		// 초기화
		isLogging = false;
		loggings = null;

		if (StringUtils.isBlank(strLogging))
			return false;

		try {
			// 로깅 설정 언마샬링
			loggings = new XmlMapper().readValue(strLogging, LoggingConfiguration.class);
			if (loggings == null)
				return false;
			if (loggings.loggingEnabled == null)
				return false;

			isLogging = true;
			return true;
		} catch (Exception e) {
			logger.error("Bucket info read failed : {}", bucketName, e);
			return false;
		}
	}

	/**
	 * 알림설정 텍스트를 언마샬링하여 필요한 설정을 추출한다.
	 * 
	 * @param strNotification
	 *            알림 설정 텍스트
	 * @return 성공 / 실패 여부
	 */
	public boolean setNotificationConfiguration(String strNotification) {
		// 초기화
		isNotification = false;
		notification = null;

		if (StringUtils.isBlank(strNotification))
			return false;

		try {
			// 알림 설정 언마샬링
			notification = new XmlMapper().readValue(strNotification, NotificationConfiguration.class);
			if (notification == null)
				return false;
			if (notification.topics == null)
				return false;

			isNotification = true;
			return true;
		} catch (Exception e) {
			logger.error("Bucket info read failed : {}", bucketName, e);
			return false;
		}
	}

	/**
	 * 복제설정 텍스트를 언마샬링하여 필요한 설정을 추출한다.
	 * 
	 * @param strReplication
	 *            복제 설정 텍스트
	 * @return 성공 / 실패 여부
	 */
	public boolean setReplicationConfiguration(String strReplication) {
		// 초기화
		isReplication = false;

		if (replications == null)
			replications = new ArrayList<>();
		else
			replications.clear();

		if (StringUtils.isBlank(strReplication))
			return false;

		try {
			// 복제 설정 언마샬링
			ReplicationConfiguration replication = new XmlMapper().readValue(strReplication, ReplicationConfiguration.class);

			if (replication == null || replication.rules == null || replication.rules.isEmpty())
				return false;

			for (var Role : replication.rules) {
				var data = new ReplicationData();

				if (data.Set(Role)) {
					logger.debug("{}", data);
					replications.add(data);
				} else
					logger.error("Role is invalid : {}", bucketName);
			}
			isReplication = true;
			return true;
		} catch (Exception e) {
			logger.error("Bucket info read failed : {}", bucketName, e);
			return false;
		}
	}

	public void removeLoggingConfiguration() {
		isLogging = false;
		loggings = null;
	}

	public void removeNotificationConfiguration() {
		isNotification = false;
		notification = null;
	}

	public void removeReplicationConfiguration() {
		isReplication = false;
		replications = null;
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "";
		}
	}
}
