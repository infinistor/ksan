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
package com.pspace.backend.LogManager.Metering;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.Libs.Utility;
import com.pspace.backend.Libs.Config.MeteringConfig;
import com.pspace.backend.Libs.Data.Metering.ApiLogData;
import com.pspace.backend.Libs.Data.Metering.ErrorLogData;
import com.pspace.backend.Libs.Data.Metering.IoLogData;
import com.pspace.backend.Libs.Ksan.ObjManagerHelper;
import com.pspace.backend.Libs.S3.S3Parameters;
import com.pspace.backend.LogManager.DB.DBManager;

public class SendMetering {
	private final Logger logger = LoggerFactory.getLogger(SendMetering.class);

	private final MeteringConfig config;
	private final DBManager db = DBManager.getInstance();
	private final ObjManagerHelper objManager = ObjManagerHelper.getInstance();
	private boolean _pause = false;
	private boolean _quit = false;

	public SendMetering(MeteringConfig config) {
		this.config = config;
	}

	public void pause() {
		_pause = true;
	}

	public void resume() {
		_pause = false;
	}

	public void quit() {
		_quit = true;
	}

	public void start() {
		int assetHour = 0;
		logger.info("Metering Start!");

		while (!_quit) {
			// 일시정지 확인
			if (_pause) {
				Utility.delay(config.getMeterDelay());
				continue;
			}
			try {
				// Metering 조회 기간 설정
				var meterRange = Utility.getDateRangeMinute(config.getMeter());
				logger.info("Metering Range: " + meterRange.start + " ~ " + meterRange.end);

				// Bucket Metering 집계
				var bucketIoEvents = db.getBucketIoMeteringEvents(meterRange);
				var bucketApiEvents = db.getBucketApiMeteringEvents(meterRange);
				var bucketErrorEvents = db.getBucketErrorMeteringEvents(meterRange);

				// Backend Metering 집계
				var backendIoEvents = db.getBackendIoMeteringEvents(meterRange);
				var backendApiEvents = db.getBackendApiMeteringEvents(meterRange);
				var backendErrorEvents = db.getBackendErrorMeteringEvents(meterRange);

				// 버킷 목록 및 Usage 가져오기
				var buckets = objManager.getBucketUsages(meterRange);
				if (buckets == null) {
					logger.error("Bucket Usage is null");
					// Metering 설정한 시간만큼 대기
					Utility.delay(config.getMeterDelay());
					continue;
				}
				logger.info("Bucket Usage Count: " + buckets.size());

				// Bucket Metering 데이터 생성
				for (var bucket : buckets) {
					// API Metering 목록에서 Bucket이 존재하지 않을 경우
					if (bucketApiEvents.stream().noneMatch(x -> x.bucket.equals(bucket.bucket))) {
						var apiEvent = new ApiLogData(bucket.inDate, bucket.user, bucket.bucket, S3Parameters.OP_PUT_OBJECT, 0);
						bucketApiEvents.add(apiEvent);
					}

					// IO Metering 목록에서 Bucket이 존재하지 않을 경우
					if (bucketIoEvents.stream().noneMatch(x -> x.bucket.equals(bucket.bucket))) {
						var ioEvent = new IoLogData(bucket.inDate, bucket.user, bucket.bucket, 0, 0);
						bucketIoEvents.add(ioEvent);
					}

					// Error Metering 목록에서 Bucket이 존재하지 않을 경우
					if (bucketErrorEvents.stream().noneMatch(x -> x.bucket.equals(bucket.bucket))) {
						var errorEvent = new ErrorLogData(bucket.inDate, bucket.user, bucket.bucket, 0, 0);
						bucketErrorEvents.add(errorEvent);
					}

					// Backend API Metering 목록에서 Bucket이 존재하지 않을 경우
					if (backendApiEvents.stream().noneMatch(x -> x.bucket.equals(bucket.bucket))) {
						var apiEvent = new ApiLogData(bucket.inDate, bucket.user, bucket.bucket, S3Parameters.OP_PUT_OBJECT, 0);
						backendApiEvents.add(apiEvent);
					}

					// Backend IO Metering 목록에서 Bucket이 존재하지 않을 경우
					if (backendIoEvents.stream().noneMatch(x -> x.bucket.equals(bucket.bucket))) {
						var ioEvent = new IoLogData(bucket.inDate, bucket.user, bucket.bucket, 0, 0);
						backendIoEvents.add(ioEvent);
					}

					// Backend Error Metering 목록에서 Bucket이 존재하지 않을 경우
					if (backendErrorEvents.stream().noneMatch(x -> x.bucket.equals(bucket.bucket))) {
						var errorEvent = new ErrorLogData(bucket.inDate, bucket.user, bucket.bucket, 0, 0);
						backendErrorEvents.add(errorEvent);
					}
				}

				// DB에 저장
				db.insertBucketApiMeter(bucketApiEvents);
				db.insertBucketIoMeter(bucketIoEvents);
				db.insertBucketErrorMeter(bucketErrorEvents);
				db.insertUsageMeter(buckets);
				db.insertBackendApiMeter(backendApiEvents);
				db.insertBackendIoMeter(backendIoEvents);
				db.insertBackendErrorMeter(backendErrorEvents);

				// 현재 시간 가져오기
				var hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				// 시간이 바뀌었다면 집계
				if (assetHour != hour) {
					assetHour = hour;
					var assetRange = Utility.getDateRangeHour(config.getMeter());
					logger.info("Asset Range: " + assetRange.start + " ~ " + assetRange.end);

					db.insertBucketApiAsset(assetRange);
					db.insertBucketIoAsset(assetRange);
					db.insertBucketErrorAsset(assetRange);
					db.insertUsageAsset(assetRange);
					db.insertBackendApiAsset(assetRange);
					db.insertBackendIoAsset(assetRange);
					db.insertBackendErrorAsset(assetRange);

					db.expiredMeter();
					db.check();
				}

				// Metering 설정한 시간만큼 대기
				Utility.delay(config.getMeterDelay());

			} catch (Exception e) {
				logger.error("", e);
			}
		}
	}
}
