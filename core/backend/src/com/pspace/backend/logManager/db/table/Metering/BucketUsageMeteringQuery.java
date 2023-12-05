package com.pspace.backend.logManager.db.table.Metering;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Metering.DateRange;
import com.pspace.backend.libs.Data.Metering.UsageLogData;

public class BucketUsageMeteringQuery implements BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BucketUsageMeteringQuery.class);

	public static final String DB_TABLE_NAME_METER = "BUCKET_METER";
	public static final String DB_TABLE_NAME_ASSET = "BUCKET_ASSET";
	public static final String DB_USED = "USED";
	public static final String DB_MAX_USED = "MAX_" + DB_USED;
	public static final String DB_AVG_USED = "AVG_" + DB_USED;
	public static final String DB_MIN_USED = "MIN_" + DB_USED;

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " datetime NOT NULL, " +
				DB_USER + " varchar(200) NOT NULL, " +
				DB_BUCKET + " varchar(200) NOT NULL, " +
				DB_USED + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " datetime NOT NULL, " +
				DB_USER + " varchar(200) NOT NULL, " +
				DB_BUCKET + " varchar(200) NOT NULL, " +
				DB_MAX_USED + " bigint DEFAULT NULL, " +
				DB_AVG_USED + " bigint DEFAULT NULL, " +
				DB_MIN_USED + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String insertMeter() {
		return String.format(
				"insert into %s (%s, %s, %s, %s) VALUES(?, ?, ?, ?) on duplicate key update %s = VALUES(%s);",
				DB_TABLE_NAME_METER, DB_IN_DATE, DB_USER, DB_BUCKET,
				DB_USED, DB_USED, DB_USED);
	}

	public static String insertAsset(DateRange range) {
		return "INSERT INTO " + DB_TABLE_NAME_ASSET
				+ "(indate, volume, user, bucket, max_used, avg_used, min_used) "
				+ " SELECT '" + range.start + "', volume, user, bucket, max(used), avg(used), min(used) FROM"
				+ " (SELECT * FROM " + DB_TABLE_NAME_METER
				+ " WHERE indate > '" + range.start + "' AND indate < '" + range.end
				+ "') AS bucket_meter GROUP BY volume, user, bucket"
				+ " on duplicate key update max_used = VALUES(max_used), avg_used = VALUES(avg_used), min_used = VALUES(min_used);";
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

	public static List<UsageLogData> getMeterList(DateRange range, List<HashMap<String, Object>> results) {
		if (results == null)
			return null;

		var items = new java.util.ArrayList<UsageLogData>();
		try {
			for (var result : results) {
				var user = (String) result.get(DB_USER);
				var bucket = (String) result.get(DB_BUCKET);
				var usedSize = (Long) result.get(DB_USED);
				items.add(new UsageLogData(range.start, user, bucket, usedSize));
			}
			return items;
		} catch (Exception e) {
			log.error("getMeterList: " + e.getMessage());
			return null;
		}
	}
}
