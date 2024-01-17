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
package com.pspace.backend.lifecycle.lifecycle;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.backend.libs.Ksan.AgentConfig;
import com.pspace.backend.libs.Ksan.ObjManagerHelper;
import com.pspace.backend.libs.S3.LifecycleConfiguration;
import com.pspace.backend.libs.S3.Tagging;
import com.pspace.backend.libs.S3.LifecycleConfiguration.Rule;
import com.pspace.backend.libs.data.Constants;
import com.pspace.backend.libs.data.lifecycle.LifecycleEventData;
import com.pspace.ifs.ksan.libs.mq.MQSender;
import com.pspace.ifs.ksan.objmanager.Metadata;

public class LifecycleFilter {
	private final Logger logger = LoggerFactory.getLogger(LifecycleFilter.class);
	private final AgentConfig ksanConfig;
	private final ObjManagerHelper objManager;
	private final MQSender mq;

	boolean todayIsRun = false;
	List<LifecycleEventData> events = new ArrayList<>();

	public LifecycleFilter() throws Exception {
		objManager = ObjManagerHelper.getInstance();
		this.ksanConfig = AgentConfig.getInstance();
		mq = new MQSender(
				ksanConfig.mqHost,
				ksanConfig.mqPort,
				ksanConfig.mqUser,
				ksanConfig.mqPassword,
				Constants.MQ_KSAN_LOG_EXCHANGE,
				Constants.MQ_EXCHANGE_OPTION_TOPIC,
				Constants.MQ_BINDING_LIFECYCLE_EVENT);
	}

	public boolean filtering() {
		logger.info("Lifecycle Filtering Start!");
		todayIsRun = true;
		var result = false;

		// 버킷목록을 가져온다.
		var buckets = objManager.getBucketList();
		logger.info("Bucket Count : {}", buckets.size());

		for (var bucket : buckets) {
			var bucketName = bucket.getName();
			// 버킷의 수명주기 설정을 가져온다.
			var lifecycle = getLifecycleConfiguration(bucket.getLifecycle());

			// 버킷의 수명주기 설정을 불러오지 못할 경우 스킵
			if (lifecycle == null)
				continue;

			// 버킷의 수명주기 규칙이 비어있거나 재대로 불러오지 못할 경우 스킵
			if (lifecycle.rules == null || lifecycle.rules.isEmpty()) {
				logger.error("invalid rules");
				continue;
			}

			logger.info("[{}] is filtering", bucketName);

			// 룰정보 가져오기
			var rules = lifecycle.rules;

			for (var rule : rules) {
				// 버킷의 수명주기 설정이 활성화 되어있지 않을 경우 스킵
				if (!isEnabled(rule.status))
					continue;

				// 룰에 prefix 가 설정되어 있을 경우 가져오기
				var prefix = getPrefix2Rule(rule);

				// Current 버전의 Transition 수명주기 설정이 되어있을 경우
				if (rule.transition != null) {
					var expired = 0L;
					// 수명주기 설정이 특정 날짜일 경우
					if (!StringUtils.isBlank(rule.transition.date)) {
						expired = getExpiredTimeDate(rule.transition.date);
						// 수명주기 설정이 일정 기간일 경우 기간을 숫자로 변환
					} else if (!StringUtils.isBlank(rule.transition.days)) {
						expired = getExpiredTimeNoncurrentDays(rule.transition.days);
					}

					// 수명주기 설정이 정상적일 경우
					if (expired > 0) {

						if (StringUtils.isBlank(rule.transition.StorageClass)) {
							logger.error("[{}] invalid transition storage class", bucketName);
							continue;
						}

						logger.info("[{}] current object filtering. {}", bucketName, long2Date(expired));

						// 해당 버킷의 모든 오브젝트에 대해 필터링
						var nextMarker = "";
						var isTruncated = true;
						while (isTruncated) {

							// 오브젝트 목록 가져오기
							var objects = objManager.listExpiredObjects(bucketName, prefix, nextMarker, expired);

							// 오브젝트가 없을 경우 스킵
							if (objects == null || objects.isEmpty())
								break;

							logger.info("[{}] Expired Current Object List Get. ({}, {}, {}) : {}", bucketName, prefix, nextMarker, expired, objects.size());

							if (objects.size() < ObjManagerHelper.MAX_KEY_SIZE)
								isTruncated = false;
							else {
								nextMarker = objects.get(objects.size() - 1).getPath();
							}

							for (var object : objects) {
								// 스킵 체크
								if (skipCheck(rule, object))
									continue;
								// 수명주기가 만료된 오브젝트를 DB에 저장
								sendEvent(new LifecycleEventData.Builder(bucketName, object.getPath()).setStorageClass(rule.transition.StorageClass).build());
							}
						}
					}
				}

				// Noncurrent 버전의 Transition 수명주기 설정이 되어있을 경우
				if (rule.versionTransition != null && !StringUtils.isBlank(rule.versionTransition.NoncurrentDays)) {
					// 기간을 숫자로 변환
					var expired = getExpiredTimeNoncurrentDays(rule.versionTransition.NoncurrentDays);
					logger.info("[{}] Noncurrent object filtering. {}, {}", bucketName, expired, long2Date(expired));

					// 수명주기 설정이 정상적일 경우
					if (expired > 0) {
						// 해당 버킷의 모든 오브젝트에 대해 필터링
						var nextMarker = "";
						var nextVersionId = "";
						var isTruncated = true;
						while (isTruncated) {

							// 오브젝트 목록 가져오기
							var objects = objManager.listExpiredObjectVersions(bucketName, prefix, nextMarker, nextVersionId, expired);
							if (objects == null || objects.isEmpty())
								break;
							logger.info("[{}] Expired Noncurrent Object List Get. ({}, {}, {}, {}) : {}", bucketName, prefix, nextMarker, nextVersionId, expired, objects.size());

							if (objects.size() < ObjManagerHelper.MAX_KEY_SIZE)
								isTruncated = false;
							else {
								nextMarker = objects.get(objects.size() - 1).getPath();
								nextVersionId = objects.get(objects.size() - 1).getVersionId();
							}

							for (var object : objects) {
								// 스킵 체크
								if (skipCheck(rule, object))
									continue;
								// 수명주기가 만료된 오브젝트를 DB에 저장
								sendEvent(
										new LifecycleEventData.Builder(bucketName, object.getPath()).setVersionId(object.getVersionId()).setStorageClass(rule.versionTransition.StorageClass).build());
							}
						}
					}
				}

				// Current 버전의 수명주기 설정이 되어있을 경우
				if (rule.expiration != null) {
					var expired = 0L;
					// 수명주기 설정이 특정 날짜일 경우
					if (!StringUtils.isBlank(rule.expiration.date)) {
						expired = getExpiredTimeDate(rule.expiration.date);
						// 수명주기 설정이 일정 기간일 경우 기간을 숫자로 변환
					} else if (!StringUtils.isBlank(rule.expiration.days)) {
						expired = getExpiredTimeNoncurrentDays(rule.expiration.days);
					}

					// 수명주기 설정이 정상적일 경우
					if (expired > 0) {

						logger.info("[{}] current object filtering. {}, {}", bucketName, expired, long2Date(expired));

						// 해당 버킷의 모든 오브젝트에 대해 필터링
						var nextMarker = "";
						var isTruncated = true;
						while (isTruncated) {

							// 오브젝트 목록 가져오기
							var objects = objManager.listExpiredObjects(bucketName, prefix, nextMarker, expired);

							// 오브젝트가 없을 경우 스킵
							if (objects == null || objects.isEmpty())
								break;

							logger.info("[{}] Expired Current Object List Get. ({}, {}, {}) : {}", bucketName, prefix, nextMarker, expired, objects.size());

							if (objects.size() < ObjManagerHelper.MAX_KEY_SIZE)
								isTruncated = false;
							else {
								nextMarker = objects.get(objects.size() - 1).getPath();
							}

							for (var object : objects) {
								// 스킵 체크
								if (skipCheck(rule, object))
									continue;
								// 수명주기가 만료된 오브젝트를 DB에 저장
								sendEvent(new LifecycleEventData.Builder(bucketName, object.getPath()).build());
							}
						}
					}

					// DeleteMarker 수명주기 설정이 되어있을 경우
					if (Boolean.parseBoolean(rule.expiration.ExpiredObjectDeleteMarker)) {
						var nextMarker = "";
						var isTruncated = true;
						while (isTruncated) {
							// DeleteMarker가 current 버전인 오브젝트 목록가져오기
							logger.info("[{}] Expired DeleteMarker Object List Get. ({}, {})", bucketName, prefix,
									nextMarker);
							var objects = objManager.listDeleteMarkers(bucketName, prefix, nextMarker);

							// 오브젝트가 없을 경우 스킵
							if (objects == null || objects.isEmpty())
								break;

							logger.info("[{}] Expired DeleteMarker Object List Get. ({}, {}) : {}", bucketName, prefix,
									nextMarker, objects.size());

							if (objects.size() < ObjManagerHelper.MAX_KEY_SIZE)
								isTruncated = false;
							else {
								nextMarker = objects.get(objects.size() - 1).getPath();
							}

							for (var object : objects) {
								// // 해당 오브젝트의 갯수 확인
								// var objectCount = objManager.getObjectCount(bucketName, object.getPath());
								// // 오브젝트가 1개 초과일 경우 스킵
								// if (objectCount > 1)
								// continue;
								// // 스킵 체크
								// if (skipCheck(rule, object))
								// continue;
								// 수명주기가 만료된 오브젝트를 DB에 저장
								sendEvent(new LifecycleEventData.Builder(bucketName, object.getPath())
										.setVersionId(object.getVersionId())
										.build());
							}
						}
					}
				}

				// Noncurrent 버전의 수명주기 설정이 되어있을 경우
				if (rule.versionExpiration != null && !StringUtils.isBlank(rule.versionExpiration.days)) {
					// 기간을 숫자로 변환
					var expired = getExpiredTimeNoncurrentDays(rule.versionExpiration.days);
					logger.info("[{}] Noncurrent object filtering. {}, {}", bucketName, expired, long2Date(expired));

					// 수명주기 설정이 정상적일 경우
					if (expired > 0) {
						// 해당 버킷의 모든 오브젝트에 대해 필터링
						var nextMarker = "";
						var nextVersionId = "";
						var isTruncated = true;
						while (isTruncated) {

							// 오브젝트 목록 가져오기
							var objects = objManager.listExpiredObjectVersions(bucketName, prefix, nextMarker, nextVersionId, expired);
							if (objects == null || objects.isEmpty())
								break;
							logger.info("[{}] Expired Noncurrent Object List Get. ({}, {}, {}) : {}", bucketName, nextMarker, nextVersionId, expired, objects.size());

							if (objects.size() < ObjManagerHelper.MAX_KEY_SIZE)
								isTruncated = false;
							else {
								nextMarker = objects.get(objects.size() - 1).getPath();
								nextVersionId = objects.get(objects.size() - 1).getVersionId();
							}

							for (var object : objects) {
								// 스킵 체크
								if (skipCheck(rule, object))
									continue;
								// 수명주기가 만료된 오브젝트를 DB에 저장
								sendEvent(new LifecycleEventData.Builder(bucketName, object.getPath())
										.setVersionId(object.getVersionId())
										.build());
							}
						}
					}
				}

				// Multipart 수명주기 설정이 되어있을 경우
				if (rule.abortIncompleteMultipartUpload != null && !StringUtils.isBlank(rule.abortIncompleteMultipartUpload.DaysAfterInitiation)) {
					// 멀티파트 인스턴스를 가져온다.
					var multiManager = objManager.getMultipartInstance(bucketName);
					// 기간을 숫자로 변환
					var expiredNoncurrentDays = NumberUtils
							.toInt(rule.abortIncompleteMultipartUpload.DaysAfterInitiation);

					// 해당 버킷의 모든 오브젝트에 대해 필터링
					var nextMarker = "";
					var nextUploadIdMarker = "";
					var isTruncated = true;
					while (isTruncated) {
						try {
							// Multipart 목록을 가져온다.
							var multipartList = multiManager.listUploads(null, prefix, nextMarker, nextUploadIdMarker,
									ObjManagerHelper.MAX_KEY_SIZE);

							if (multipartList.size() < ObjManagerHelper.MAX_KEY_SIZE) {
								isTruncated = false;
							} else {
								nextMarker = multipartList.get(multipartList.size() - 1).getKey();
								nextUploadIdMarker = multipartList.get(multipartList.size() - 1).getUploadId();
							}
							for (var multipart : multipartList) {
								// Multipart의 수명주기가 만료 되었을 경우
								if (checkExpired(multipart.getLastModified(), expiredNoncurrentDays)) {
									sendEvent(new LifecycleEventData.Builder(bucketName, multipart.getKey())
											.setUploadId(multipart.getUploadId())
											.build());
								}
							}
						} catch (SQLException e) {
							logger.error("", e);
							break;
						}
					}
				}
			}
			// 이벤트 저장
			logger.info("[{}] is filtering end", bucketName);
		}
		logger.info("Lifecycle Filtering End");
		return result;
	}

	private boolean skipCheck(Rule rule, Metadata object) {
		// 필터가 존재할 경우
		if (rule.filter != null) {
			// And 필터가 존재할 경우
			if (rule.filter.and != null) {
				// 태그 필터가 설정되어 있을 경우
				if (!rule.filter.and.tag.isEmpty()) {
					// 오브젝트의 모든 태그를 비교
					int tagCount = rule.filter.and.tag.size();
					var tags = string2Tag(object.getTag());
					// 태그가 없을 경우 스킵
					if (tags == null || tags.isEmpty())
						return true;
					for (var filterTag : rule.filter.and.tag) {
						for (var objectTag : tags) {
							if (filterTag.key.equals(objectTag.key) && filterTag.value.equals(objectTag.value))
								tagCount--;
						}
					}
					// 필터에 설정된 태그목록이 오브젝트의 태그목록에 포함되지 않을경우 스킵
					if (tagCount > 0)
						return true;
				}
			}
			// 태그 필터가 설정 되어 있을 경우
			else if (rule.filter.tag != null) {
				var filterTag = rule.filter.tag;
				boolean find = false;

				var tags = string2Tag(object.getTag());
				// 태그가 없을 경우 스킵
				if (tags == null)
					return true;
				// 오브젝트의 모든 태그를 비교
				for (var objectTag : tags)
					if (filterTag.key.equals(objectTag.key) && filterTag.value.equals(objectTag.value))
						find = true;

				// 필터에 설정된 태그가 오브젝트의 태그에 존재하지 않을경우 스킵
				if (!find)
					return true;
			}
			// 최소 크기 필터가 설정되어 있을 경우
			if (StringUtils.isNotBlank(rule.filter.objectSizeGreaterThan)) {
				// 오브젝트가 최소크기보다 작을 경우 스킵
				var minFileSize = NumberUtils.toInt(rule.filter.objectSizeGreaterThan);
				if (object.getSize() < minFileSize)
					return true;
			}
			// 최대 크기 필터가 설정되어 있을 경우
			else if (StringUtils.isNotBlank(rule.filter.objectSizeLessThan)) {
				// 오브젝트가 최대크기보다 클 경우 스킵
				var maxFileSize = NumberUtils.toInt(rule.filter.objectSizeLessThan);
				if (object.getSize() > maxFileSize)
					return true;
			}
		}
		return false;
	}

	private String getPrefix2Rule(Rule rule) {
		// 필터가 존재할 경우
		if (rule.filter != null) {
			// And 필터가 존재할 경우
			if (rule.filter.and != null) {
				// Prefix가 설정되어 있을 경우
				if (StringUtils.isNotBlank(rule.filter.and.prefix)) {
					return rule.filter.and.prefix;
				}
			}
			// Prefix가 설정되어 있을 경우
			else if (StringUtils.isNotBlank(rule.filter.prefix)) {
				return rule.filter.prefix;
			}
		}
		return null;
	}

	/***************************************
	 * Utility
	 *******************************************/
	static final String ENABLED = "Enabled";

	private boolean isEnabled(String status) {
		return ENABLED.equalsIgnoreCase(status);
	}

	private LifecycleConfiguration getLifecycleConfiguration(String strLifecycle) {
		if (StringUtils.isBlank(strLifecycle))
			return null;
		try {
			// 수명주기 설정 언마샬링
			return new XmlMapper().readValue(strLifecycle, LifecycleConfiguration.class);
		} catch (Exception e) {
			logger.error("Lifecycle read failed \n", e);
			return null;
		}
	}

	private boolean checkExpired(Date lastModified, int expiredDay) {
		// 마지막 수정시간에서 수명주기가 설정된 시간만큼 날짜를 더하기
		Calendar cal = Calendar.getInstance();
		cal.setTime(lastModified);
		cal.add(Calendar.DATE, expiredDay);

		var expiredTime = cal.getTime();
		var now = new Date();

		// 만료시간이 지났을 경우
		if (now.getTime() > expiredTime.getTime())
			return true;
		return false;
	}

	private long getExpiredTimeDate(String date) {
		Instant instant = Instant.parse(date);
		Instant now = Instant.now();
		if (instant.getEpochSecond() > now.getEpochSecond())
			return instant.getEpochSecond() * 1000000000L + instant.getNano();
		else
			return Long.MAX_VALUE;

	}

	private long getExpiredTimeNoncurrentDays(String noncurrentDays) {
		var expired = NumberUtils.toInt(noncurrentDays);
		Instant instant = Instant.now().plus(-expired, ChronoUnit.DAYS);
		return instant.getEpochSecond() * 1000000000L + instant.getNano();
	}

	private static String long2Date(long timestamp) {
		return Instant.ofEpochSecond(timestamp / 1000000000L, timestamp % 1000000000L).toString();
	}

	private Collection<com.pspace.backend.libs.S3.Tagging.TagSet.Tag> string2Tag(String strTags) {
		try {
			var tagging = new XmlMapper().readValue(strTags, Tagging.class);
			return tagging.tags.tags;
		} catch (Exception e) {
			logger.error("", e);
			return new ArrayList<>();
		}
	}

	private void sendEvent(LifecycleEventData event) {
		try {
			mq.send(event.toString(), Constants.MQ_BINDING_LIFECYCLE_EVENT);
			logger.debug("Lifecycle Event Send : {}", event);
		} catch (Exception e) {
			logger.error("", e);
		}
	}
}
