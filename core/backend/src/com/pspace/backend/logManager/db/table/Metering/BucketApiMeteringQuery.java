package com.pspace.backend.LogManager.DB.Table.Metering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.Libs.Data.Metering.ApiLogData;
import com.pspace.backend.Libs.Data.Metering.DateRange;
import com.pspace.backend.LogManager.DB.Table.Logging.S3LogQuery;

public class BucketApiMeteringQuery implements BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BucketApiMeteringQuery.class);

	public static final String DB_TABLE_NAME_METER = "BUCKET_API_METERS";
	public static final String DB_TABLE_NAME_ASSET = "BUCKET_API_ASSETS";
	public static final String DB_EVENT = "EVENT";
	public static final String DB_COUNT = "COUNT";

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_EVENT + " VARCHAR(200) NOT NULL, " +
				DB_COUNT + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_EVENT + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_EVENT + " VARCHAR(200) NOT NULL, " +
				DB_COUNT + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_EVENT + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String selectMeter(DateRange range) {
		return "SELECT " + S3LogQuery.DB_BUCKET_NAME + ", " + S3LogQuery.DB_OPERATION + ", COUNT(*) AS COUNT FROM " + S3LogQuery.getTableName()
				+ " WHERE " + S3LogQuery.DB_DATE_TIME + " > '" + range.start + "' AND " + S3LogQuery.DB_DATE_TIME + " < '" + range.end
				+ "' GROUP BY " + S3LogQuery.DB_BUCKET_NAME + ", " + S3LogQuery.DB_OPERATION + ";";
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
		if (results == null)
			return null;
		var items = new ArrayList<ApiLogData>();
		try {
			for (var result : results) {

				var bucket = (String) result.get(S3LogQuery.DB_BUCKET_NAME);
				var user = (String) result.get(S3LogQuery.DB_USER_NAME);
				var event = (String) result.get(S3LogQuery.DB_OPERATION);
				var count = Long.parseLong(String.valueOf(result.get(DB_COUNT)));
				items.add(new ApiLogData(range.start, user, bucket, event, count));
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