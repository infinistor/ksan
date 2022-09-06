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
package com.pspace.backend.Lifecycle;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.ifs.ksan.objmanager.LifeCycle;
import com.pspace.ifs.ksan.objmanager.ObjManagerUtil;
import com.pspace.s3format.LifecycleConfiguration;
import com.pspace.s3format.LifecycleConfiguration.Rule;
import com.pspace.s3format.Tagging;

public class LifecycleFilter {
	private final SimpleDateFormat StringDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private final ObjManagerUtil ObjManager;
	private final Logger logger;

	public LifecycleFilter(ObjManagerUtil DB) {
		this.ObjManager = DB;
		logger = LoggerFactory.getLogger(LifecycleFilter.class);
	}

	public boolean Filtering() throws SQLException {
		logger.info("Lifecycle Filtering Start!");
		var result = false;

		var BucketList = ObjManager.getBucketList();
		var LifecycleManager = ObjManager.getLifeCycleManagmentInsatance();

		if (BucketList.size() > 0) {
			for (var Bucket : BucketList) {
				var BucketName = Bucket.getName();
				var EventList = new ArrayList<LifeCycle>();

				// 버킷의 수명주기 설정을 가져온다.
				var Lifecycle = GetLifecycleConfiguration(Bucket.getLifecycle());

				// 버킷의 수명주기 설정을 불러오지 못할 경우 스킵
				if (Lifecycle == null) continue;

				// 버킷의 수명주기 규칙이 비어있거나 재대로 불러오지 못할 경우 스킵
				if (Lifecycle.rules == null || Lifecycle.rules.size() == 0) {
					logger.error("[{}] is not a valid rules");
					continue;
				}

				// 오브젝트 목록 가져오기
				var ObjectList = ObjManager.listObjects(BucketName, "", 1000);
				// 룰정보 가져오기
				var Rules = Lifecycle.rules;

				for (var Rule : Rules) {
					// 버킷의 수명주기 설정이 활성화 되어있지 않을 경우 스킵
					if (!isEnabled(Rule.status))
						continue;

					// 오브젝트의 수명주기가 설정되었을 경우
					if (Rule.expiration != null) {
						// 오브젝트의 수명주기가 특정한 날짜일 경우
						if (Rule.expiration.date != null) {
							// 문자열에서 DATE로 변경
							Date ExpiredTime = StringToDate(Rule.expiration.date);

							// 변경 성공시에만 동작
							if (ExpiredTime != null) {
								// 버킷에 존재하는 모든 오브젝트에 대한 수명주기 검사
								for (int Index = ObjectList.size() - 1; Index >= 0; Index--) {
									var Object = ObjectList.get(Index);

									// 스킵 체크
									if (LifecycleSkipCheck(Rule, Object))
										continue;

									// 오브젝트의 수명주기가 만료되었을 경우
									if (ExpiredCheck(ExpiredTime)) {
										// DB에 저장
										EventList.add(new LifeCycle(0, Object.getBucket(), Object.getPath(),
												Object.getVersionId(), "", ""));

										// 목록에서 제거
										ObjectList.remove(Index);

										// Filter로 1개 이상 이벤트를 처리함을 표시
										if (!result)
											result = true;
									}
								}
							}
						}
						// 오브젝트의 수명주기가 일정 기간일 경우
						else if (Rule.expiration.days != null) {
							// 기간을 숫자로 변환
							var ExpiredDays = NumberUtils.toInt(Rule.expiration.days);

							// 버킷에 존재하는 모든 오브젝트에 대한 수명주기 검사
							for (int Index = ObjectList.size() - 1; Index >= 0; Index--) {
								var Object = ObjectList.get(Index);

								// 스킵 체크
								if (LifecycleSkipCheck(Rule, Object))
									continue;

								// 오브젝트의 수명주기가 만료되었을 경우
								if (ExpiredCheck(Object.getLastModified(), ExpiredDays)) {
									// DB에 저장
									EventList.add(new LifeCycle(0, Object.getBucket(), Object.getPath(),
											Object.getVersionId(), "", ""));

									// 목록에서 제거
									ObjectList.remove(Index);

									// Filter로 1개 이상 이벤트를 처리함을 표시
									if (!result)
										result = true;
								}
							}
						}
					}
					// 오브젝트의 버저닝 수명주기가 설정 되었을 경우
					if (Rule.versionexpiration != null && !Rule.versionexpiration.NoncurrentDays.isBlank()) {
						// 기간을 숫자로 변환
						var ExpiredDays = NumberUtils.toInt(Rule.versionexpiration.NoncurrentDays);

						// 버킷에 존재하는 모든 오브젝트에 대한 수명주기 검사
						for (int Index = ObjectList.size() - 1; Index >= 0; Index--) {
							var Object = ObjectList.get(Index);

							// 스킵 체크
							if (LifecycleVersioningSkipCheck(Rule, Object))
								continue;

							// 오브젝트의 수명주기가 만료되었을 경우
							if (ExpiredCheck(Object.getLastModified(), ExpiredDays)) {
								// DB에 저장
								EventList.add(new LifeCycle(0, Object.getBucket(), Object.getPath(),
										Object.getVersionId(), "", ""));

								// 목록에서 제거
								ObjectList.remove(Index);

								// Filter로 1개 이상 이벤트를 처리함을 표시
								if (!result)
									result = true;
							}
						}
					}

					// Multipart의 part 수명주기가 설정 되었을 경우
					if (Rule.abortincompletemultipartupload != null
							&& !Rule.abortincompletemultipartupload.DaysAfterInitiation.isBlank()) {
						// 기간을 숫자로 변환
						var ExpiredDays = NumberUtils.toInt(Rule.abortincompletemultipartupload.DaysAfterInitiation);

						// 업로드 중인 Multipart 목록을 가져온다.
						var Multiparts = ObjManager.getMultipartInsatance(BucketName);
						for (var Multipart : Multiparts.listUploads("", "", "", "", 1000)) {

							// 오브젝트의 이름이 필터 설정에 만족하지 못할 경우 스킵
							if (LifecycleMultipartSkipCheck(Rule, Multipart.getKey()))
								continue;

							// 오브젝트의 Part 목록을 가져온다.
							var Parts = Multiparts.getParts(Multipart.getUploadId());

							for (var Index : Parts.keySet()) {
								// 파트를 가져온다.
								var Part = Parts.get(Index);

								// Multipart의 수명주기가 만료 되었을 경우
								if (ExpiredCheck(Part.getLastModified(), ExpiredDays)) {
									EventList.add(new LifeCycle(0, Multipart.getBucket(), Multipart.getKey(), "",
											Multipart.getUploadId(), ""));

									// Filter로 1개 이상 이벤트를 처리함을 표시
									if (!result)
										result = true;
								}
							}
						}
					}
				}

				LifecycleManager.putLifeCycleEvents(EventList);
			}
		}
		return result;
	}

	private boolean LifecycleSkipCheck(Rule Rule, com.pspace.ifs.ksan.objmanager.Metadata Object) {
		// 마지막 버전이 아닐 경우 스킵
		if (!Object.getLastVersion())
			return true;

		// DeleteMarker일 경우 스킵
		if (isMARKER(Object.getDeleteMarker()))
			return true;

		// 필터가 존재할 경우
		if (Rule.filter != null) {
			var Tags = getTagging(Object.getTag());
			// And 필터가 존재할 경우
			if (Rule.filter.and != null) {
				// Prefix가 설정되어 있을 경우
				if (!Rule.filter.and.prefix.isBlank()) {
					// Prefix가 일치하지 않을 경우 스킵
					if (!Object.getPath().startsWith(Rule.filter.and.prefix))
						return true;
				}

				// 태그 필터가 설정되어 있을 경우
				if (Rule.filter.and.tag.size() > 0) {
					// 오브젝트의 모든 태그를 비교
					int TagCount = Rule.filter.and.tag.size();
					for (var FilterTag : Rule.filter.and.tag) {
						for (var ObjectTag : Tags.tagset.tags) {
							if (FilterTag.key == ObjectTag.key && FilterTag.value == ObjectTag.value)
								TagCount--;
						}
					}
					// 필터에 설정된 태그목록이 오브젝트의 태그목록에 포함되지 않을경우 스킵
					if (TagCount > 0)
						return true;
				}

				// 최소 크기 필터가 설정되어 있을 경우
				if (!Rule.filter.objectSizeGreaterThan.isBlank()) {
					// 오브젝트가 최소크기보다 작을 경우 스킵
					var MinFileSize = NumberUtils.toInt(Rule.filter.objectSizeGreaterThan);
					if (Object.getSize() < MinFileSize)
						return true;
				}

				// 최대 크기 필터가 설정되어 있을 경우
				if (!Rule.filter.objectSizeLessThan.isBlank()) {
					// 오브젝트가 최대크기보다 클 경우 스킵
					var MaxFileSize = NumberUtils.toInt(Rule.filter.objectSizeLessThan);
					if (Object.getSize() > MaxFileSize)
						return true;
				}
			}
			// Prefix가 설정되어 있을 경우
			else if (!Rule.filter.prefix.isBlank()) {
				if (!Object.getPath().startsWith(Rule.filter.prefix))
					return true;
			}
			// 태그 필터가 설정 되어 있을 경우
			else if (Rule.filter.tag != null) {
				var FilterTag = Rule.filter.tag;
				boolean Find = false;

				// 오브젝트의 모든 태그를 비교
				for (var ObjectTag : Tags.tagset.tags)
					if (FilterTag.key == ObjectTag.key && FilterTag.value == ObjectTag.value)
						Find = true;

				// 필터에 설정된 태그가 오브젝트의 태그에 존재하지 않을경우 스킵
				if (!Find)
					return true;
			}
			// 최소 크기 필터가 설정되어 있을 경우
			else if (!Rule.filter.objectSizeGreaterThan.isBlank()) {
				// 오브젝트가 최소크기보다 작을 경우 스킵
				var MinFileSize = NumberUtils.toInt(Rule.filter.objectSizeGreaterThan);
				if (Object.getSize() < MinFileSize)
					return true;
			}
			// 최대 크기 필터가 설정되어 있을 경우
			else if (!Rule.filter.objectSizeLessThan.isBlank()) {
				// 오브젝트가 최대크기보다 클 경우 스킵
				var MaxFileSize = NumberUtils.toInt(Rule.filter.objectSizeLessThan);
				if (Object.getSize() > MaxFileSize)
					return true;
			}
		}
		return false;
	}

	private boolean LifecycleVersioningSkipCheck(Rule Rule, com.pspace.ifs.ksan.objmanager.Metadata Object) {
		if (Object.getLastVersion()) {
			// 마지막 버전이 DeleteMarker 일 경우
			if (isMARKER(Object.getDeleteMarker())) {
				// DeleteMarker 옵션이 없을 경우 스킵
				if (Rule.expiration == null)
					return true;
				if (Rule.expiration.ExpiredObjectDeleteMarker == null)
					return true;
				else if (!Boolean.parseBoolean(Rule.expiration.ExpiredObjectDeleteMarker))
					return true;
			}
			// 마지막 버전일 경우 스킵
			else
				return true;
		}

		// 필터가 존재할 경우
		if (Rule.filter != null || !Rule.filter.prefix.isBlank()) {
			// 필터가 일치하지 않을 경우 스킵
			if (!Object.getPath().startsWith(Rule.filter.prefix))
				return true;
		}
		return false;
	}

	private boolean LifecycleMultipartSkipCheck(Rule Rule, String KeyName) {
		// 필터가 존재할 경우
		if (Rule.filter != null || !Rule.filter.prefix.isBlank()) {
			// 필터가 일치하지 않을 경우 스킵
			if (!KeyName.startsWith(Rule.filter.prefix))
				return true;
		}
		return false;
	}

	/***************************************
	 * Utility
	 *******************************************/
	private final String ENABLED = "Enabled";
	// private final String DISABLED = "Disabled";
	private final String MARKER = "mark";

	private boolean isEnabled(String Status) {
		return ENABLED.equalsIgnoreCase(Status);
	}

	// private boolean isDisabled(String Status) {return
	// DISABLED.equalsIgnoreCase(Status);}
	private boolean isMARKER(String DeleteMarker) {
		return MARKER.equalsIgnoreCase(DeleteMarker);
	}

	private boolean ExpiredCheck(Date ExpiredTime) {
		if (new Date().getTime() > ExpiredTime.getTime())
			return true;
		return false;
	}

	private boolean ExpiredCheck(long LastModified, int ExpiredDay) {
		var LocalDate = LocalDateTime.ofInstant(Instant.ofEpochSecond((Long.MAX_VALUE - LastModified) / 1000000000L),
				TimeZone.getDefault().toZoneId());

		// 마지막 수정시간에서 수명주기가 설정된 시간만큼 날짜를 더하기
		var ExpiredTime = LocalDate.plusDays(ExpiredDay);
		var NowTime = LocalDateTime.now();

		// 만료시간이 지났을 경우
		if (NowTime.isAfter(ExpiredTime))
			return true;
		return false;
	}

	private boolean ExpiredCheck(Date LastModified, int ExpiredDay) {
		// 마지막 수정시간에서 수명주기가 설정된 시간만큼 날짜를 더하기
		Calendar cal = Calendar.getInstance();
		cal.setTime(LastModified);
		cal.add(Calendar.DATE, ExpiredDay);

		var ExpiredTime = cal.getTime();
		var NowTime = new Date();

		// 만료시간이 지났을 경우
		if (NowTime.getTime() > ExpiredTime.getTime())
			return true;
		return false;
	}

	private Date StringToDate(String Data) {
		try {
			int Index = Data.indexOf("T");
			return StringDateFormat.parse(Data.substring(0, Index));
		} catch (Exception e) {
			logger.error("", e);
			return null;
		}
	}

	private LifecycleConfiguration GetLifecycleConfiguration(String StrLifecycle) {
		if (StrLifecycle.isBlank()) return null;
		try {
			// 수명주기 설정 언마샬링
			return new XmlMapper().readValue(StrLifecycle, LifecycleConfiguration.class);
		} catch (Exception e) {
			logger.error("Lifecycle read failed \n", e);
			return null;
		}
	}

	private Tagging getTagging(String StrTags)
	{
		try {
			// 수명주기 설정 언마샬링
			return new XmlMapper().readValue(StrTags, Tagging.class);
		} catch (Exception e) {
			logger.error("Tag read Failed : {}", e);
			return new Tagging();
		}
	}
}
