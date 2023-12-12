package com.pspace.backend.LogManager.DB.Table.Metering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.Libs.Data.Metering.DateRange;
import com.pspace.backend.Libs.Data.Metering.ErrorLogData;
import com.pspace.backend.LogManager.DB.Table.Logging.S3LogQuery;

public class BucketErrorMeteringQuery implements BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BucketErrorMeteringQuery.class);

	public static final String DB_TABLE_NAME_METER = "BUCKET_ERROR_METERS";
	public static final String DB_TABLE_NAME_ASSET = "BUCKET_ERROR_ASSETS";
	public static final String DB_CLIENT_ERROR_COUNT = "CLIENT_ERROR_COUNT";
	public static final String DB_SERVER_ERROR_COUNT = "SERVER_ERROR_COUNT";

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_CLIENT_ERROR_COUNT + " BIGINT NOT NULL, " +
				DB_SERVER_ERROR_COUNT + " BIGINT NOT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER_NAME + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET_NAME + " VARCHAR(64) NOT NULL, " +
				DB_CLIENT_ERROR_COUNT + " BIGINT NOT NULL, " +
				DB_SERVER_ERROR_COUNT + " BIGINT NOT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String selectMeter(DateRange range) {
		return "SELECT " + S3LogQuery.DB_BUCKET_NAME + ", " + S3LogQuery.DB_REQUEST_USER + ", "
				+ " COUNT(CASE WHEN " + S3LogQuery.DB_STATUS_CODE + " >= 400 AND " + S3LogQuery.DB_STATUS_CODE + " < 500 THEN 1 END) AS " + DB_CLIENT_ERROR_COUNT + ","
				+ " COUNT(CASE WHEN " + S3LogQuery.DB_STATUS_CODE + " >= 500 THEN 1 END) AS " + DB_SERVER_ERROR_COUNT + ""
				+ " FROM " + S3LogQuery.getTableName()
				+ " WHERE " + S3LogQuery.DB_DATE_TIME + " >= '" + range.start + "' AND " + S3LogQuery.DB_DATE_TIME + " <= '" + range.end
				+ "' GROUP BY " + S3LogQuery.DB_BUCKET_NAME + ";";
	}

	public static String insertMeter() {
		return "INSERT INTO " + DB_TABLE_NAME_METER + "(" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_CLIENT_ERROR_COUNT + ", " + DB_SERVER_ERROR_COUNT + ") "
				+ " VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + DB_CLIENT_ERROR_COUNT + " = VALUES(" + DB_CLIENT_ERROR_COUNT + "), " + DB_SERVER_ERROR_COUNT + " = VALUES("
				+ DB_SERVER_ERROR_COUNT + ");";
	}

	public static String insertAsset(DateRange range) {
		return "INSERT INTO " + DB_TABLE_NAME_ASSET
				+ "(" + DB_IN_DATE + ", " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", " + DB_CLIENT_ERROR_COUNT + ", " + DB_SERVER_ERROR_COUNT + ")"
				+ " SELECT '" + range.start + "', " + DB_USER_NAME + ", " + DB_BUCKET_NAME + ", sum(" + DB_CLIENT_ERROR_COUNT + "), sum(" + DB_SERVER_ERROR_COUNT + ") FROM"
				+ " (SELECT * FROM " + DB_TABLE_NAME_METER + " where " + DB_IN_DATE + " > '" + range.start + "' AND " + DB_IN_DATE + " < '" + range.end + "') AS " + DB_TABLE_NAME_METER + " GROUP BY "
				+ DB_USER_NAME + ", " + DB_BUCKET_NAME
				+ " ON DUPLICATE KEY UPDATE " + DB_CLIENT_ERROR_COUNT + " = VALUES(" + DB_CLIENT_ERROR_COUNT + "), " + DB_SERVER_ERROR_COUNT + " = VALUES(" + DB_SERVER_ERROR_COUNT + ");";
	}

	public static String expiredMeter() {
		return "DELETE FROM " + DB_TABLE_NAME_METER + " WHERE " + DB_IN_DATE + " < DATE_ADD(DATE_FORMAT(NOW() , '%Y-%m-%d %k:%i:%s'), INTERVAL -1 DAY);";
	}

	public static List<ErrorLogData> getMeterList(DateRange range, List<HashMap<String, Object>> results) {
		if (results == null)
			return null;
		var items = new ArrayList<ErrorLogData>();
		try {
			for (var result : results) {
				var bucket = (String) result.get(S3LogQuery.DB_BUCKET_NAME);
				var user = (String) result.get(S3LogQuery.DB_REQUEST_USER);
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