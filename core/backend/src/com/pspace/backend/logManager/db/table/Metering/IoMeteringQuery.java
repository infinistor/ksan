package db.table.Metering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.table.Logging.LoggingQuery;

public class IoMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(IoMeteringQuery.class);

	static final String DB_TABLE_NAME_METER = "BUCKET_IO_METER";
	static final String DB_TABLE_NAME_ASSET = "BUCKET_IO_ASSET";
	static final String DB_INDATE = "INDATE";
	static final String DB_USER = "USER";
	static final String DB_BUCKET = "BUCKET";
	static final String DB_UPLOAD = "UPLOAD";
	static final String DB_DOWNLOAD = "DOWNLOAD";

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_INDATE + " datetime NOT NULL, " +
				DB_USER + " varchar(64) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_UPLOAD + " bigint DEFAULT NULL, " +
				DB_DOWNLOAD + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_INDATE + ", " + DB_USER + ", " + DB_BUCKET + "));";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_INDATE + " datetime NOT NULL, " +
				DB_USER + " varchar(64) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_UPLOAD + " bigint DEFAULT NULL, " +
				DB_DOWNLOAD + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_INDATE + ", " + DB_USER + ", " + DB_BUCKET + "));";
	}

	public static String getInsertMeter(int times) {
		return "insert into " + DB_TABLE_NAME_METER + "(indate, volume, user, bucket, upload, download) "
				+ "select now(), user_name, bucketlist.volume, bucket_name, sum(request_length), sum(response_length) "
				+ "from (select * from " + LoggingQuery.DB_TABLE_NAME
				+ " where DATE_SUB(NOW(), INTERVAL " + times + " MINUTE) < date_time) as s3logging, "
				// + S3BucketQuery.DB_TABLE_NAME
				+ " where s3logging.bucket_name = bucketlist.bucket group by user_name, bucket_name;";
	}

	public static String getInsertAsset() {
		return "insert into " + DB_TABLE_NAME_ASSET + "(indate, volume, user, bucket, upload, download)"
				+ " select now(), volume, user, bucket, sum(upload), sum(download) from "
				+ DB_TABLE_NAME_METER
				+ " group by volume, user, bucket;";
	}

	public static String getMeterClear() {
		return String.format("TRUNCATE %s;", DB_TABLE_NAME_METER);
	}
}