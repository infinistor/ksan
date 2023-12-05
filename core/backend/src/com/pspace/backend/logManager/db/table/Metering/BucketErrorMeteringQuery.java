package com.pspace.backend.logManager.db.table.Metering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Metering.DateRange;
import com.pspace.backend.libs.Data.Metering.ErrorLogData;
import com.pspace.backend.logManager.db.table.Logging.S3LogQuery;

public class BucketErrorMeteringQuery implements BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BucketErrorMeteringQuery.class);

	public static final String DB_TABLE_NAME_METER = "bucket_error_meter";
	public static final String DB_TABLE_NAME_ASSET = "bucket_error_asset";
	public static final String DB_CLIENT_ERROR_COUNT = "client_error_count";
	public static final String DB_SERVER_ERROR_COUNT = "server_error_count";

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " datetime NOT NULL, " +
				DB_USER + " varchar(200) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_CLIENT_ERROR_COUNT + " bigint NOT NULL, " +
				DB_SERVER_ERROR_COUNT + " bigint NOT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " datetime NOT NULL, " +
				DB_USER + " varchar(200) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_CLIENT_ERROR_COUNT + " bigint NOT NULL, " +
				DB_SERVER_ERROR_COUNT + " bigint NOT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String selectMeter(DateRange range) {
		return "select bucket_name, "
				+ " count(case when status_code >= 400 and status_code < 500 then 1 end) as client_error_count,"
				+ " count(case when status_code >= 500 then 1 end) as server_error_count"
				+ " from " + S3LogQuery.getTableName()
				+ " where date_time >= '" + range.start + "' and date_time <= '" + range.end
				+ "' group by bucket_name;";
	}

	public static String insertMeter() {
		return String.format(
				"INSERT INTO %s(%s, %s, %s, %s, %s) VALUES(?, ?, ?, ?, ?) on duplicate key update client_error_count = VALUES(client_error_count), server_error_count = VALUES(server_error_count);",
				DB_TABLE_NAME_METER,
				DB_IN_DATE, DB_USER, DB_BUCKET, DB_CLIENT_ERROR_COUNT, DB_SERVER_ERROR_COUNT);
	}

	public static String insertAsset(DateRange range) {
		return "insert into " + DB_TABLE_NAME_ASSET
				+ "(indate, volume, user, bucket, client_error_count, server_error_count)"
				+ " select '" + range.start
				+ "', volume, user, bucket, sum(client_error_count), sum(server_error_count) from"
				+ " (select * from " + DB_TABLE_NAME_METER
				+ " where indate > '" + range.start + "' and indate < '" + range.end
				+ "') as bucket_error_meter group by volume, user, bucket"
				+ " on duplicate key update client_error_count = VALUES(client_error_count), server_error_count = VALUES(server_error_count);";
	}

	public static String expiredMeter() {
		return "DELETE FROM " + DB_TABLE_NAME_METER + " WHERE " + DB_IN_DATE + " < DATE_ADD(DATE_FORMAT(NOW() , '%Y-%m-%d %k:%i:%s'), INTERVAL 1 DAYS);";
	}

	public static String expiredAsset(int days) {
		if (days < 1)
			days = DEFAULT_EXPIRES_DAY;
		return String.format("delete FROM %s where %s < date_add(date_format(now() , '%s'), interval -%d days);",
				DB_TABLE_NAME_ASSET, DB_IN_DATE, DB_DATE_FORMAT, days);
	}

	public static List<ErrorLogData> getMeterList(DateRange range, List<HashMap<String, Object>> results) {
		if (results == null)
			return null;
		var items = new ArrayList<ErrorLogData>();
		try {
			for (var result : results) {
				var bucket = (String) result.get(S3LogQuery.DB_BUCKET_NAME);
				var user = (String) result.get(S3LogQuery.DB_USER_NAME);
				var clientError = (long) result.get(DB_CLIENT_ERROR_COUNT);
				var serverError = (long) result.get(DB_SERVER_ERROR_COUNT);
				items.add(new ErrorLogData(range.start, user, bucket, clientError, serverError));
			}
		} catch (Exception e) {
			log.error("ErrorLogData getMeterList error: ", e);
		} finally {
			results.clear();
			results = null;
		}
		return items;
	}
}