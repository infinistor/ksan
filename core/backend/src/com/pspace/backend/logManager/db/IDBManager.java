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
package com.pspace.backend.logManager.db;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Lifecycle.LifecycleLogData;
import com.pspace.backend.libs.Data.Lifecycle.RestoreLogData;
import com.pspace.backend.libs.Data.Metering.ApiLogData;
import com.pspace.backend.libs.Data.Metering.DateRange;
import com.pspace.backend.libs.Data.Metering.ErrorLogData;
import com.pspace.backend.libs.Data.Metering.IoLogData;
import com.pspace.backend.libs.Data.Metering.UsageLogData;
import com.pspace.backend.libs.Data.Replication.ReplicationLogData;
import com.pspace.backend.libs.Data.S3.S3LogData;

public interface IDBManager {
	static final Logger logger = LoggerFactory.getLogger(DBManager.class);

	void connect() throws Exception;

	boolean check();

	/**
	 * S3 로그를 DB에 저장
	 * 
	 * @param data
	 *            S3 로그 데이터
	 * @return 성공 여부
	 */
	boolean insertS3Log(S3LogData Event);

	/**
	 * S3 로그를 DB에 저장
	 * 
	 * @param data
	 *            S3 로그 데이터
	 * @return 성공 여부
	 */
	boolean insertBackendLog(S3LogData Event);

	/**
	 * Replication 로그를 DB에 저장
	 * 
	 * @param data
	 *            Replication 로그 데이터
	 * @return 성공 여부
	 */
	boolean insertReplicationLog(ReplicationLogData data);

	/**
	 * Lifecycle 로그를 DB에 저장
	 * 
	 * @param data
	 *            Lifecycle 로그 데이터
	 * @return 성공 여부
	 */
	boolean insertLifecycleLog(LifecycleLogData data);

	/**
	 * Restore 로그를 DB에 저장
	 * 
	 * @param data
	 *            Restore 로그 데이터
	 * @return 성공 여부
	 */
	boolean insertRestoreLog(RestoreLogData data);

	/**
	 * Usage 로그를 DB에 저장
	 * 
	 * @param data
	 *            Usage 로그 데이터
	 * @return 성공 여부
	 */
	List<ApiLogData> getBucketApiMeteringEvents(DateRange range);

	/**
	 * 분단위 IO 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	List<IoLogData> getBucketIoMeteringEvents(DateRange range);

	/**
	 * 분단위 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	List<ErrorLogData> getBucketErrorMeteringEvents(DateRange range);

	/**
	 * 분단위 Backend API 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	List<ApiLogData> getBackendApiMeteringEvents(DateRange range);

	/**
	 * 분단위 Backend IO 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	List<IoLogData> getBackendIoMeteringEvents(DateRange range);

	/**
	 * 분단위 Backend 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	List<ErrorLogData> getBackendErrorMeteringEvents(DateRange range);

	/**
	 * 분단위 Usage 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	boolean insertUsageMeter(List<UsageLogData> events);

	/**
	 * 분단위 Usage 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	boolean insertUsageAsset(DateRange range);

	/**
	 * 분단위 API 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	boolean insertBucketApiMeter(List<ApiLogData> events);

	/**
	 * 분단위 API 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	boolean insertBucketApiAsset(DateRange range);

	/**
	 * 분단위 IO 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	boolean insertBucketIoMeter(List<IoLogData> events);

	/**
	 * 분단위 IO 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	boolean insertBucketIoAsset(DateRange range);

	/**
	 * 분단위 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	boolean insertBucketErrorMeter(List<ErrorLogData> events);

	/**
	 * 분단위 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	boolean insertBucketErrorAsset(DateRange range);

	/**
	 * 분단위 Backend API 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	boolean insertBackendApiMeter(List<ApiLogData> events);

	/**
	 * 분단위 Backend API 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	boolean insertBackendApiAsset(DateRange range);

	/**
	 * 분단위 Backend IO 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	boolean insertBackendIoMeter(List<IoLogData> events);

	/**
	 * 분단위 Backend IO 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	boolean insertBackendIoAsset(DateRange range);

	/**
	 * 분단위 Backend 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	boolean insertBackendErrorMeter(List<ErrorLogData> events);

	/**
	 * 분단위 Backend 에러 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 성공 여부
	 */
	boolean insertBackendErrorAsset(DateRange range);

	/**
	 * 만료된 Metering 데이터를 DB에서 삭제
	 * 
	 * @return 성공 여부
	 */
	boolean expiredMeter();
}