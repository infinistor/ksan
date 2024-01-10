package com.pspace.backend.libs.db.table.metering;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.data.Metering.DateRange;
import com.pspace.backend.libs.data.Metering.UsageLogData;
import java.util.Collections;

public class BucketUsageMeteringQuery extends BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BucketUsageMeteringQuery.class);

	public static final String DB_TABLE_NAME_METER = "BUCKET_METERS";
	public static final String DB_TABLE_NAME_ASSET = "BUCKET_ASSETS";
	public static final String DB_USED = "USED";
	public static final String DB_MAX_USED = "MAX_" + DB_USED;
	public static final String DB_AVG_USED = "AVG_" + DB_USED;
	public static final String DB_MIN_USED = "MIN_" + DB_USED;

	private BucketUsageMeteringQuery() {
		super();
	}

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(200) NOT NULL, " +
				DB_USED + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(200) NOT NULL, " +
				DB_MAX_USED + " BIGINT DEFAULT NULL, " +
				DB_AVG_USED + " BIGINT DEFAULT NULL, " +
				DB_MIN_USED + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String insertMeter() {
		return "INSERT INTO " + DB_TABLE_NAME_METER + "(" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_USED + ") "
				+ " VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + DB_USED + " = VALUES(" + DB_USED + ");";
	}

	public static String insertAsset(DateRange range) {
		return "INSERT INTO " + DB_TABLE_NAME_ASSET
				+ "(" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_MAX_USED + ", " + DB_AVG_USED + ", " + DB_MIN_USED + ") "
				+ " SELECT '" + range.start + "', " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", MAX(" + DB_USED + "), AVG(" + DB_USED + "), MIN(" + DB_USED + ") FROM"
				+ " (SELECT * FROM " + DB_TABLE_NAME_METER
				+ " WHERE " + DB_IN_DATE + " > '" + range.start + "' AND " + DB_IN_DATE + " < '" + range.end
				+ "') AS " + DB_TABLE_NAME_ASSET + " GROUP BY " + DB_USER_NAME + ", " + DB_BUCKET_NAME
				+ " ON DUPLICATE KEY UPDATE " + DB_MAX_USED + " = VALUES(" + DB_MAX_USED + "), " + DB_AVG_USED + " = VALUES(" + DB_AVG_USED + "), " + DB_MIN_USED + " = VALUES(" + DB_MIN_USED + ");";
	}

	public static String expiredMeter() {
		return "DELETE FROM " + DB_TABLE_NAME_METER + " WHERE " + DB_IN_DATE + " < DATE_ADD(DATE_FORMAT(NOW() , '%Y-%m-%d %k:%i:%s'), INTERVAL -1 DAY);";
	}

	public static List<UsageLogData> getMeterList(DateRange range, List<HashMap<String, Object>> results) {
		if (results == null || results.isEmpty())
			return Collections.emptyList();

		var items = new java.util.ArrayList<UsageLogData>();
		try {
			for (var result : results) {
				var user = (String) result.get(DB_USER_NAME);
				var bucket = (String) result.get(DB_BUCKET_NAME);
				var usedSize = (Long) result.get(DB_USED);
				items.add(new UsageLogData(range.start, user, bucket, usedSize));
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
