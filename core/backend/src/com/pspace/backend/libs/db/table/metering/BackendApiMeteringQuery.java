package com.pspace.backend.libs.db.table.metering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.data.Metering.ApiLogData;
import com.pspace.backend.libs.data.Metering.DateRange;
import com.pspace.backend.libs.db.table.logging.BackendLogQuery;

public class BackendApiMeteringQuery extends BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BackendApiMeteringQuery.class);

	public static final String DB_TABLE_NAME_METER = "BACKEND_API_METERS";
	public static final String DB_TABLE_NAME_ASSET = "BACKEND_API_ASSETS";
	public static final String DB_EVENT = "EVENT";
	public static final String DB_COUNT = "COUNT";

	protected BackendApiMeteringQuery() {
		super();
	}

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_EVENT + " VARCHAR(200) NOT NULL, " +
				DB_COUNT + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_EVENT + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_EVENT + " VARCHAR(200) NOT NULL, " +
				DB_COUNT + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_EVENT + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String selectMeter(DateRange range) {
		return "SELECT " + BackendLogQuery.DB_BUCKET_NAME + ", " + BackendLogQuery.DB_REQUEST_USER + ", " + BackendLogQuery.DB_OPERATION + ", COUNT(*) AS COUNT FROM " + BackendLogQuery.getTableName()
				+ " WHERE " + BackendLogQuery.DB_DATE_TIME + " > '" + range.start + "' AND " + BackendLogQuery.DB_DATE_TIME + " < '" + range.end
				+ "' GROUP BY " + BackendLogQuery.DB_BUCKET_NAME + ", " + BackendLogQuery.DB_OPERATION + ";";
	}

	public static String insertMeter() {
		return "INSERT INTO " + DB_TABLE_NAME_METER + "(" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_EVENT + ", " + DB_COUNT + ") "
				+ " VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + DB_COUNT + " = VALUES(" + DB_COUNT + ");";
	}

	public static String insertAsset(DateRange range) {
		return "INSERT INTO " + DB_TABLE_NAME_ASSET
				+ "(" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_EVENT + ", " + DB_COUNT + ")"
				+ " SELECT '" + range.start + "', " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_EVENT + ", SUM(" + DB_COUNT + ") FROM"
				+ " (SELECT * FROM " + DB_TABLE_NAME_METER
				+ " WHERE " + DB_IN_DATE + " > '" + range.start + "' AND " + DB_IN_DATE + " < '" + range.end
				+ "') AS " + DB_TABLE_NAME_METER + " GROUP BY " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_EVENT
				+ " ON DUPLICATE KEY UPDATE COUNT = VALUES(COUNT);";
	}

	public static String expiredMeter() {
		return "DELETE FROM " + DB_TABLE_NAME_METER + " WHERE " + DB_IN_DATE + " < DATE_ADD(DATE_FORMAT(NOW() , '%Y-%m-%d %k:%i:%s'), INTERVAL -1 DAY);";
	}

	public static List<ApiLogData> getMeterList(DateRange range, List<HashMap<String, Object>> results) {
		if (results == null || results.isEmpty())
			return Collections.emptyList();
		try {
			var items = new ArrayList<ApiLogData>();
			for (var result : results) {

				var bucket = (String) result.get(BackendLogQuery.DB_REQUEST_USER);
				var user = (String) result.get(BackendLogQuery.DB_REQUEST_USER);
				var event = (String) result.get(BackendLogQuery.DB_OPERATION);
				var count = Long.parseLong(String.valueOf(result.get(DB_COUNT)));
				items.add(new ApiLogData(range.start, user, bucket, event, count));
			}
			return items;
		} catch (Exception e) {
			log.error("getMeterList error: ", e);
			return Collections.emptyList();
		} finally {
			results.clear();
			results = null;
		}
	}
}