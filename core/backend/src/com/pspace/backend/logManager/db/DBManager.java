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
package com.pspace.backend.LogManager.DB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import com.pspace.backend.Libs.Config.DBConfig;
import com.pspace.backend.Libs.Data.Lifecycle.LifecycleLogData;
import com.pspace.backend.Libs.Data.Lifecycle.RestoreLogData;
import com.pspace.backend.Libs.Data.Metering.ApiLogData;
import com.pspace.backend.Libs.Data.Metering.DateRange;
import com.pspace.backend.Libs.Data.Metering.ErrorLogData;
import com.pspace.backend.Libs.Data.Metering.IoLogData;
import com.pspace.backend.Libs.Data.Metering.UsageLogData;
import com.pspace.backend.Libs.Data.Replication.ReplicationLogData;
import com.pspace.backend.Libs.Data.S3.S3LogData;
import com.pspace.backend.LogManager.DB.MariaDB.MariaDBManager;

public class DBManager implements IDBManager {
	static final Logger logger = LoggerFactory.getLogger(DBManager.class);
	IDBManager dbManager;
	DBConfig config;

	public static DBManager getInstance() {
		return LazyHolder.INSTANCE;
	}

	private static class LazyHolder {
		private static final DBManager INSTANCE = new DBManager();
	}

	public void init(DBConfig config) {
		this.config = config;
		dbManager = new MariaDBManager(config);
		// if (config.Type.equalsIgnoreCase(Constants.DB_TYPE_MARIADB))
		// dbManager = new MariaDBManager(config);
		// else
		// dbManager = new MongoDBManager(config);
	}

	/**
	 * DB 연결
	 * 
	 * @throws Exception
	 *             DB 연결 실패시
	 */
	public void connect() throws Exception {
		dbManager.connect();
	}

	/**
	 * DB 확인
	 */
	public boolean check() {
		return dbManager.check();
	}

	// #region select

	/**
	 * 분단위 API 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	public List<ApiLogData> getBucketApiMeteringEvents(DateRange range) {
		return dbManager.getBucketApiMeteringEvents(range);
	}

	/**
	 * 분단위 IO 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	public List<IoLogData> getBucketIoMeteringEvents(DateRange range) {
		return dbManager.getBucketIoMeteringEvents(range);
	}

	/**
	 * 분단위 에러 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	public List<ErrorLogData> getBucketErrorMeteringEvents(DateRange range) {
		return dbManager.getBucketErrorMeteringEvents(range);
	}

	/**
	 * 분단위 Backend API 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	public List<ApiLogData> getBackendApiMeteringEvents(DateRange range) {
		return dbManager.getBackendApiMeteringEvents(range);
	}

	/**
	 * 분단위 Backend IO 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	public List<IoLogData> getBackendIoMeteringEvents(DateRange range) {
		return dbManager.getBackendIoMeteringEvents(range);
	}

	/**
	 * 분단위 Backend 에러 사용량 로그를 DB에서 조회
	 * 
	 * @param range
	 *            조회 기간
	 * @return 사용량 로그
	 */
	public List<ErrorLogData> getBackendErrorMeteringEvents(DateRange range) {
		return dbManager.getBackendErrorMeteringEvents(range);
	}

	// #endregion

	// #region insert

	/**
	 * S3 로그를 DB에 저장
	 * 
	 * @param data
	 *            S3 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertS3Log(S3LogData data) {
		return dbManager.insertS3Log(data);
	}

	/**
	 * S3 로그를 DB에 저장
	 * 
	 * @param data
	 *            S3 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBackendLog(S3LogData data) {
		return dbManager.insertBackendLog(data);
	}

	/**
	 * 복제 로그를 DB에 저장
	 * 
	 * @param data
	 *            복제 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertReplicationLog(ReplicationLogData data) {
		return dbManager.insertReplicationLog(data);
	}

	/**
	 * 만료 로그를 DB에 저장
	 * 
	 * @param data
	 *            복제 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertLifecycleLog(LifecycleLogData data) {
		return dbManager.insertLifecycleLog(data);
	}

	/**
	 * 복구 로그를 DB에 저장
	 * 
	 * @param data
	 *            복제 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertRestoreLog(RestoreLogData data) {
		return dbManager.insertRestoreLog(data);
	}

	/**
	 * 분단위 사용량 로그를 DB에 저장
	 * 
	 * @param events
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertUsageMeter(List<UsageLogData> events) {
		return dbManager.insertUsageMeter(events);
	}

	/**
	 * 시단위 사용량 로그를 DB에 저장
	 * 
	 * @param range
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertUsageAsset(DateRange range) {
		return dbManager.insertUsageAsset(range);
	}

	/**
	 * 분단위 API 사용량 로그를 DB에 저장
	 * 
	 * @param events
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBucketApiMeter(List<ApiLogData> events) {
		return dbManager.insertBucketApiMeter(events);
	}

	/**
	 * 시단위 API 사용량 로그를 DB에 저장
	 * 
	 * @param range
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBucketApiAsset(DateRange range) {
		return dbManager.insertBucketApiAsset(range);
	}

	/**
	 * 분단위 IO 사용량 로그를 DB에 저장
	 * 
	 * @param events
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBucketIoMeter(List<IoLogData> events) {
		return dbManager.insertBucketIoMeter(events);
	}

	/**
	 * 시단위 IO 사용량 로그를 DB에 저장
	 * 
	 * @param range
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBucketIoAsset(DateRange range) {
		return dbManager.insertBucketIoAsset(range);
	}

	/**
	 * 분단위 에러 사용량 로그를 DB에 저장
	 * 
	 * @param events
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBucketErrorMeter(List<ErrorLogData> events) {
		return dbManager.insertBucketErrorMeter(events);
	}

	/**
	 * 시단위 에러 사용량 로그를 DB에 저장
	 * 
	 * @param range
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBucketErrorAsset(DateRange range) {
		return dbManager.insertBucketErrorAsset(range);
	}

	/**
	 * 분단위 Backend API 사용량 로그를 DB에 저장
	 * 
	 * @param events
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBackendApiMeter(List<ApiLogData> events) {
		return dbManager.insertBackendApiMeter(events);
	}

	/**
	 * 분단위 Backend IO 사용량 로그를 DB에 저장
	 * 
	 * @param events
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBackendIoMeter(List<IoLogData> events) {
		return dbManager.insertBackendIoMeter(events);
	}

	/**
	 * 분단위 Backend 에러 사용량 로그를 DB에 저장
	 * 
	 * @param events
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBackendErrorMeter(List<ErrorLogData> events) {
		return dbManager.insertBackendErrorMeter(events);
	}

	/**
	 * 시단위 Backend API 사용량 로그를 DB에 저장
	 * 
	 * @param range
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBackendApiAsset(DateRange range) {
		return dbManager.insertBackendApiAsset(range);
	}

	/**
	 * 시단위 Backend IO 사용량 로그를 DB에 저장
	 * 
	 * @param range
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBackendIoAsset(DateRange range) {
		return dbManager.insertBackendIoAsset(range);
	}

	/**
	 * 시단위 Backend 에러 사용량 로그를 DB에 저장
	 * 
	 * @param range
	 *            사용량 로그 데이터
	 * @return 성공 여부
	 */
	public boolean insertBackendErrorAsset(DateRange range) {
		return dbManager.insertBackendErrorAsset(range);
	}

	// #endregion

	// #region delete

	/**
	 * 만료된 분단위 사용량 로그를 DB에서 삭제
	 * 
	 * @return 성공 여부
	 */
	public boolean expiredMeter() {
		return dbManager.expiredMeter();
	}

	// #endregion
}