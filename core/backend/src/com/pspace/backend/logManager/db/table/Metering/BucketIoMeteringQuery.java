package com.pspace.backend.logManager.db.table.Metering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Metering.DateRange;
import com.pspace.backend.libs.Data.Metering.IoLogData;
import com.pspace.backend.logManager.db.table.Logging.S3LogQuery;

public class BucketIoMeteringQuery implements BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BucketIoMeteringQuery.class);

	static final String DB_TABLE_NAME_METER = "bucket_io_meter";
	static final String DB_TABLE_NAME_ASSET = "bucket_io_asset";
	static final String DB_UPLOAD = "upload";
	static final String DB_DOWNLOAD = "download";

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " datetime NOT NULL, " +
				DB_USER + " varchar(200) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_UPLOAD + " bigint DEFAULT NULL, " +
				DB_DOWNLOAD + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " datetime NOT NULL, " +
				DB_USER + " varchar(200) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_UPLOAD + " bigint DEFAULT NULL, " +
				DB_DOWNLOAD + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String selectMeter(DateRange range) {
		return "select bucket_name, sum(request_length) as upload, sum(response_length) as download"
				+ " from " + S3LogQuery.getTableName()
				+ " where date_time >= '" + range.start + "' and date_time <= '" + range.end + "' group by bucket_name;";
	}

	public static String insertMeter() {
		return String.format(
				"INSERT INTO %s(%s, %s, %s, %s, %s) VALUES(?, ?, ?, ?, ?) on duplicate key update upload = VALUES(upload), download = VALUES(download);",
				DB_TABLE_NAME_METER,
				DB_IN_DATE, DB_USER, DB_BUCKET, DB_UPLOAD, DB_DOWNLOAD);
	}

	public static String insertAsset(DateRange range) {
		return "insert into " + DB_TABLE_NAME_ASSET
				+ "(indate, volume, user, bucket, upload, download) select '" + range.start
				+ "', volume, user, bucket, sum(upload), sum(download) from (select * from "
				+ DB_TABLE_NAME_METER
				+ " where indate > '" + range.start + "' and indate < '" + range.end
				+ "') as bucket_io_meter group by volume, user, bucket"
				+ " on duplicate key update upload = VALUES(upload), download = VALUES(download);";
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

	public static List<IoLogData> getMeterList(DateRange range, List<HashMap<String, Object>> results) {
		if (results == null)
			return null;

		var items = new ArrayList<IoLogData>();

		try {

			for (var result : results) {
				var bucket = (String) result.get(S3LogQuery.DB_BUCKET_NAME);
				var user = (String) result.get(S3LogQuery.DB_USER_NAME);
				var upload = Long.parseLong(String.valueOf(result.get(DB_UPLOAD)));
				var download = Long.parseLong(String.valueOf(result.get(DB_DOWNLOAD)));
				items.add(new IoLogData(range.start, user, bucket, upload, download));
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