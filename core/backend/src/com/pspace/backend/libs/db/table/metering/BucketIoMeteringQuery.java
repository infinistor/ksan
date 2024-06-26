package com.pspace.backend.libs.db.table.metering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.data.Metering.DateRange;
import com.pspace.backend.libs.data.Metering.IoLogData;
import com.pspace.backend.libs.db.table.logging.S3LogQuery;

public class BucketIoMeteringQuery extends BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BucketIoMeteringQuery.class);

	static final String DB_TABLE_NAME_METER = "BUCKET_IO_METERS";
	static final String DB_TABLE_NAME_ASSET = "BUCKET_IO_ASSETS";
	static final String DB_UPLOAD = "UPLOAD";
	static final String DB_DOWNLOAD = "DOWNLOAD";

	private BucketIoMeteringQuery() {
		super();
	}

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_UPLOAD + " BIGINT DEFAULT NULL, " +
				DB_DOWNLOAD + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_UPLOAD + " BIGINT DEFAULT NULL, " +
				DB_DOWNLOAD + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String selectMeter(DateRange range) {
		return "SELECT " + S3LogQuery.DB_BUCKET_NAME + ", " + S3LogQuery.DB_REQUEST_USER + ", SUM(" + S3LogQuery.DB_REQUEST_LENGTH + ") AS " + DB_UPLOAD + ", SUM(" + S3LogQuery.DB_RESPONSE_LENGTH
				+ ") AS " + DB_DOWNLOAD + ""
				+ " FROM " + S3LogQuery.getTableName()
				+ " WHERE " + S3LogQuery.DB_DATE_TIME + " >= '" + range.start + "' AND " + S3LogQuery.DB_DATE_TIME + " <= '" + range.end + "' GROUP BY " + S3LogQuery.DB_BUCKET_NAME + ";";
	}

	public static String insertMeter() {
		return "INSERT INTO " + DB_TABLE_NAME_METER + "(" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_UPLOAD + ", " + DB_DOWNLOAD + ") "
				+ " VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + DB_UPLOAD + " = VALUES(" + DB_UPLOAD + "), " + DB_DOWNLOAD + " = VALUES("
				+ DB_DOWNLOAD + ");";
	}

	public static String insertAsset(DateRange range) {
		return "INSERT INTO " + DB_TABLE_NAME_ASSET
				+ "(" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_UPLOAD + ", " + DB_DOWNLOAD + ") SELECT '" + range.start
				+ "', " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", SUM(" + DB_UPLOAD + "), SUM(" + DB_DOWNLOAD + ") FROM (SELECT * FROM "
				+ DB_TABLE_NAME_METER
				+ " WHERE " + DB_IN_DATE + " > '" + range.start + "' AND " + DB_IN_DATE + " < '" + range.end
				+ "') AS " + DB_TABLE_NAME_METER + " GROUP BY " + DB_USER_NAME + ", " + DB_BUCKET_NAME
				+ " ON DUPLICATE KEY UPDATE " + DB_UPLOAD + " = VALUES(" + DB_UPLOAD + "), " + DB_DOWNLOAD + " = VALUES(" + DB_DOWNLOAD + ");";
	}

	public static String expiredMeter() {
		return "DELETE FROM " + DB_TABLE_NAME_METER + " WHERE " + DB_IN_DATE + " < DATE_ADD(DATE_FORMAT(NOW() , '%Y-%m-%d %k:%i:%s'), INTERVAL -1 DAY);";
	}

	public static List<IoLogData> getMeterList(DateRange range, List<HashMap<String, Object>> results) {
		if (results == null || results.isEmpty())
			return new ArrayList<>();

		try {
			var items = new ArrayList<IoLogData>();

			for (var result : results) {
				var bucket = (String) result.get(S3LogQuery.DB_BUCKET_NAME);
				var user = (String) result.get(S3LogQuery.DB_REQUEST_USER);
				var upload = Long.parseLong(String.valueOf(result.get(DB_UPLOAD)));
				var download = Long.parseLong(String.valueOf(result.get(DB_DOWNLOAD)));
				items.add(new IoLogData(range.start, user, bucket, upload, download));
			}
			return items;
		} catch (Exception e) {
			log.error("getMeterList error: ", e);
			return new ArrayList<>();
		} finally {
			results.clear();
			results = null;
		}
	}
}