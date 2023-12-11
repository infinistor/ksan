package com.pspace.backend.logManager.db.table.Metering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.Data.Metering.DateRange;
import com.pspace.backend.libs.Data.Metering.IoLogData;
import com.pspace.backend.logManager.db.table.Logging.BackendLogQuery;

public class BackendIoMeteringQuery implements BaseMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(BackendIoMeteringQuery.class);

	static final String DB_TABLE_NAME_METER = "BACKEND_IO_METER";
	static final String DB_TABLE_NAME_ASSET = "BACKEND_IO_ASSET";
	static final String DB_UPLOAD = "UPLOAD";
	static final String DB_DOWNLOAD = "DOWNLOAD";

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET + " VARCHAR(64) NOT NULL, " +
				DB_UPLOAD + " BIGINT DEFAULT NULL, " +
				DB_DOWNLOAD + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_IN_DATE + " DATETIME NOT NULL, " +
				DB_USER + " VARCHAR(200) NOT NULL, " +
				DB_BUCKET + " VARCHAR(64) NOT NULL, " +
				DB_UPLOAD + " BIGINT DEFAULT NULL, " +
				DB_DOWNLOAD + " BIGINT DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + "))" +
				"ENGINE=INNODB DEFAULT CHARSET=UTF8MB4;";
	}

	public static String selectMeter(DateRange range) {
		return "SELECT " + BackendLogQuery.DB_BUCKET_NAME + ", SUM(" + BackendLogQuery.DB_REQUEST_LENGTH + ") AS " + DB_UPLOAD + ", SUM(" + BackendLogQuery.DB_RESPONSE_LENGTH + ") AS " + DB_DOWNLOAD
				+ ""
				+ " FROM " + BackendLogQuery.getTableName()
				+ " WHERE " + BackendLogQuery.DB_DATE_TIME + " >= '" + range.start + "' AND " + BackendLogQuery.DB_DATE_TIME + " <= '" + range.end + "' GROUP BY " + BackendLogQuery.DB_BUCKET_NAME
				+ ";";
	}

	public static String insertMeter() {
		return "INSERT INTO " + DB_TABLE_NAME_METER + "(" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_UPLOAD + ", " + DB_DOWNLOAD + ") "
				+ " VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + DB_UPLOAD + " = VALUES(" + DB_UPLOAD + "), " + DB_DOWNLOAD + " = VALUES("
				+ DB_DOWNLOAD + ");";
	}

	public static String insertAsset(DateRange range) {
		return "INSERT INTO " + DB_TABLE_NAME_ASSET
				+ "(" + DB_IN_DATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_UPLOAD + ", " + DB_DOWNLOAD + ") SELECT '" + range.start
				+ "', " + DB_USER + ", " + DB_BUCKET + ", SUM(" + DB_UPLOAD + "), SUM(" + DB_DOWNLOAD + ") FROM (SELECT * FROM "
				+ DB_TABLE_NAME_METER
				+ " WHERE " + DB_IN_DATE + " > '" + range.start + "' AND " + DB_IN_DATE + " < '" + range.end
				+ "') AS " + DB_TABLE_NAME_METER + " GROUP BY " + DB_USER + ", " + DB_BUCKET
				+ " ON DUPLICATE KEY UPDATE " + DB_UPLOAD + " = VALUES(" + DB_UPLOAD + "), " + DB_DOWNLOAD + " = VALUES(" + DB_DOWNLOAD + ");";
	}

	public static String expiredMeter() {
		return "DELETE FROM " + DB_TABLE_NAME_METER + " WHERE " + DB_IN_DATE + " < DATE_ADD(DATE_FORMAT(NOW() , '%Y-%m-%d %k:%i:%s'), INTERVAL -1 DAY);";
	}

	public static List<IoLogData> getMeterList(DateRange range, List<HashMap<String, Object>> results) {
		if (results == null)
			return null;

		var items = new ArrayList<IoLogData>();

		try {

			for (var result : results) {
				var bucket = (String) result.get(BackendLogQuery.DB_BUCKET_NAME);
				var user = (String) result.get(BackendLogQuery.DB_USER_NAME);
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