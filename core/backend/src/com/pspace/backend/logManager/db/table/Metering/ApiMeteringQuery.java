package db.table.Metering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.table.Logging.LoggingQuery;

public class ApiMeteringQuery {
	static final Logger log = LoggerFactory.getLogger(ApiMeteringQuery.class);

	static final String DB_TABLE_NAME_METER = "BUCKET_API_METERS";
	static final String DB_TABLE_NAME_ASSET = "BUCKET_API_ASSETS";
	static final String DB_INDATE = "INDATE";
	static final String DB_USER = "USER_NAME";
	static final String DB_BUCKET = "BUCKET_NAME";
	static final String DB_EVENT = "OPERATION";
	static final String DB_COUNT = "COUNT";

	public static String createMeter() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_METER + " ( " +
				DB_INDATE + " datetime NOT NULL, " +
				DB_USER + " varchar(64) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_EVENT + " varchar(200) NOT NULL, " +
				DB_COUNT + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_INDATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_EVENT + "));";
	}

	public static String createAsset() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME_ASSET + " ( " +
				DB_INDATE + " datetime NOT NULL, " +
				DB_USER + " varchar(64) NOT NULL, " +
				DB_BUCKET + " varchar(64) NOT NULL, " +
				DB_EVENT + " varchar(200) NOT NULL, " +
				DB_COUNT + " bigint DEFAULT NULL, " +
				"PRIMARY KEY (" + DB_INDATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_EVENT + "));";
	}

	public static String insertMeter(int times) {
		String.format("insert into %s (%s, %s, %s, %s, %s)", DB_TABLE_NAME_METER, DB_INDATE, DB_USER, DB_BUCKET, DB_EVENT, DB_COUNT);
		
		return "insert into " + DB_TABLE_NAME_METER + "("+ DB_INDATE + ", " + DB_USER + ", " + DB_BUCKET + ", " + DB_EVENT + ", " + DB_COUNT + ")"
				+ "select now(), user_name, bucket_name, operation, count(*) as count "
				+ "from (select * from " + LoggingQuery.DB_TABLE_NAME
				+ " where DATE_SUB(NOW(), INTERVAL "+ times +" MINUTE) < date_time) as s3logging, "
				// + S3BucketQuery.DB_TABLE_NAME
				+ " where s3logging.bucket_name = bucketlist.bucket group by user_name, bucket_name, operation;";
	}

	public static String insertAsset() {
		return "insert into " + DB_TABLE_NAME_ASSET + "(INDATE, USER, BUCKET, EVENT, COUNT)"
				+ " select now(), user, bucket, event, sum(count) from " + DB_TABLE_NAME_METER
				+ " group by user, bucket, event;";
	}

	public static String meterClear() {
		return String.format("TRUNCATE %s;", DB_TABLE_NAME_METER);
	}
}