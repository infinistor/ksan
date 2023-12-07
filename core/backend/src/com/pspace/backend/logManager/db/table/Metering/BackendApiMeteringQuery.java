package com.pspace.backend.logManager.db.table.Metering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Metering.ApiLogData;
import com.pspace.backend.libs.Data.Metering.DateRange;
import com.pspace.backend.logManager.db.table.Logging.BackendLogQuery;

public class BackendApiMeteringQuery implements BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BackendApiMeteringQuery.class);

	public static final String DB_TABLE_NAME_METER = "BACKEND_API_METER";
	public static final String DB_TABLE_NAME_ASSET = "BACKEND_API_ASSET";
	public static final String DB_EVENT = "EVENT";
	public static final String DB_COUNT = "COUNT";

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET + " VARCHAR(64) NOT NULL, " +
				DB_EVENT + " VARCHAR(200) NOT NULL, " +
				DB_COUNT + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_EVENT
				+ "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET + " VARCHAR(64) NOT NULL, " +
				DB_EVENT + " VARCHAR(200) NOT NULL, " +
				DB_COUNT + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_EVENT
				+ "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String selectMeter(DateRange range) {
		return "SELECT BUCKET_NAME, USER_NAME, OPERATION, COUNT(*) AS COUNT FROM " + BackendLogQuery.getTableName()
				+ " WHERE DATE_TIME >= '" + range.start
				+ "' AND DATE_TIME <= '" + range.end + "' GROUP BY BUCKET_NAME, OPERATION;";
	}

	public static String insertMeter() {
		return String.format(
				"INSERT INTO %s(%s, %s, %s, %s, %s) VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE COUNT = VALUES(COUNT);",
				DB_TABLE_NAME_METER, DB_IN_DATE, DB_USER, DB_BUCKET, DB_EVENT, DB_COUNT);
	}

	public static String insertAsset(DateRange range) {
		return "INSERT INTO " + DB_TABLE_NAME_ASSET
				+ "(INDATE, USER, BUCKET, EVENT, COUNT)"
				+ " SELECT '" + range.start + "', volume, user, bucket, event, sum(COUNT) FROM"
				+ " (SELECT * FROM " + DB_TABLE_NAME_METER
				+ " WHERE indate > '" + range.start + "' AND indate < '" + range.end
				+ "') AS bucket_io_meter GROUP BY volume, user, bucket, event"
				+ " ON DUPLICATE KEY UPDATE COUNT = VALUES(COUNT);";
	}

	public static String expiredMeter() {
		return "DELETE FROM " + DB_TABLE_NAME_METER + " WHERE " + DB_IN_DATE + " < DATE_ADD(DATE_FORMAT(NOW() , '%Y-%m-%d %k:%i:%s'), INTERVAL 1 DAYS);";
	}

	public static String expiredAsset(int days) {
		if (days < 1)
			days = DEFAULT_EXPIRES_DAY;
		return String.format("DELETE FROM %s WHERE %s < DATE_ADD(DATE_FORMAT(NOW() , '%s'), INTERVAL -%d DAYS);",
				DB_TABLE_NAME_ASSET, DB_IN_DATE, DB_DATE_FORMAT, days);
	}

	public static List<ApiLogData> getMeterList(DateRange range, List<HashMap<String, Object>> results) {
		if (results == null)
			return null;
		var items = new ArrayList<ApiLogData>();
		try {
			for (var result : results) {

				var bucket = (String) result.get(BackendLogQuery.DB_BUCKET_NAME);
				var user = (String) result.get(BackendLogQuery.DB_USER_NAME);
				var event = (String) result.get(BackendLogQuery.DB_OPERATION);
				var COUNT = Long.parseLong(String.valueOf(result.get(DB_COUNT)));
				items.add(new ApiLogData(range.start, user, bucket, event, COUNT));
			}
		} catch (Exception e) {
			log.error("getMeterList error: ", e);
		} finally {
			results.clear();
			results = null;
		}
		return items;
	}
}