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

	boolean insertLogging(S3LogData Event);
	boolean insertReplicationLog(ReplicationLogData data);
	boolean insertLifecycleLog(LifecycleLogData data);
	boolean insertRestoreLog(RestoreLogData data);
	
	List<ApiLogData> getBucketApiMeteringEvents(DateRange range);
	List<IoLogData> getBucketIoMeteringEvents(DateRange range);
	List<ErrorLogData> getBucketErrorMeteringEvents(DateRange range);
	List<ApiLogData> getBackendApiMeteringEvents(DateRange range);
	List<IoLogData> getBackendIoMeteringEvents(DateRange range);
	List<ErrorLogData> getBackendErrorMeteringEvents(DateRange range);

	boolean insertUsageMeter(List<UsageLogData> events);
	boolean insertUsageAsset(DateRange range);
	boolean insertBucketApiMeter(List<ApiLogData> events);
	boolean insertBucketApiAsset(DateRange range);
	boolean insertBucketIoMeter(List<IoLogData> events);
	boolean insertBucketIoAsset(DateRange range);
	boolean insertBucketErrorMeter(List<ErrorLogData> events);
	boolean insertBucketErrorAsset(DateRange range);

	boolean insertBackendApiMeter(List<ApiLogData> events);
	boolean insertBackendApiAsset(DateRange range);
	boolean insertBackendIoMeter(List<IoLogData> events);
	boolean insertBackendIoAsset(DateRange range);
	boolean insertBackendErrorMeter(List<ErrorLogData> events);
	boolean insertBackendErrorAsset(DateRange range);

	boolean expiredMeter();
}