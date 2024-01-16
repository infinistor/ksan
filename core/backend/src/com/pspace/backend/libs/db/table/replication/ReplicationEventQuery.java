package com.pspace.backend.libs.db.table.replication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.backend.libs.data.Replication.ReplicationEventData;

public class ReplicationEventQuery extends BaseReplicationQuery {
	static final Logger log = LoggerFactory.getLogger(ReplicationEventQuery.class);

	private static final String DB_TABLE_NAME = "replication_event";

	protected ReplicationEventQuery() {
		super();
	}

	public static String create() {
		return "CREATE TABLE IF NOT EXISTS " + DB_TABLE_NAME + " ( " +
				DB_ID + " BIGINT AUTO_INCREMENT PRIMARY KEY, " +
				DB_IN_DATE + " TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP, " +
				DB_OPERATION + " VARCHAR(64) NOT NULL, " +
				DB_OBJECTNAME + " VARCHAR(2048) NOT NULL, " +
				DB_VERSION_ID + " VARCHAR(32) NOT NULL, " +
				DB_SOURCE_BUCKET_NAME + " VARCHAR(256) NOT NULL, " +
				DB_TARGET_BUCKET_NAME + " VARCHAR(256) NOT NULL, " +
				DB_TARGET_REGION + " VARCHAR(32) NULL) " +
				"ENGINE=INNODB DEFAULT CHARSET=utf8mb4;";
	}

	public static String insert() {
		return String.format("INSERT INTO %s(%s, %s, %s, %s, %s, %s) VALUES(?, ?, ?, ?, ?, ?);",
				DB_TABLE_NAME, DB_OPERATION, DB_OBJECTNAME, DB_VERSION_ID,
				DB_SOURCE_BUCKET_NAME, DB_TARGET_BUCKET_NAME, DB_TARGET_REGION);
	}

	public static String select(long index) {
		return String.format("SELECT * FROM %s WHERE id > %d LIMIT %d;", DB_TABLE_NAME, index, MAX_QUERY_SIZE);
	}

	public static String delete(long index) {
		return String.format("DELETE FROM %s WHERE id = %d;", DB_TABLE_NAME, index);
	}

	public static String clear(long lastIndex) {
		if (lastIndex == 0)
			return String.format("DELETE FROM %s;", DB_TABLE_NAME);
		else
			return String.format("DELETE FROM %s WHERE id <= %d;", DB_TABLE_NAME, lastIndex);
	}

	public static String clear(String bucketName) {
		return String.format("DELETE FROM %s WHERE %s = '%s';", DB_TABLE_NAME, DB_SOURCE_BUCKET_NAME, bucketName);
	}

	public static List<ReplicationEventData> getList(List<HashMap<String, Object>> results) {
		if (results == null)
			return Collections.emptyList();
		var items = new ArrayList<ReplicationEventData>();
		try {

			for (var result : results) {
				items.add(
						ReplicationEventData.newBuilder()
								.setIndex((long) result.get(DB_ID))
								.setInDate((String) result.get(DB_IN_DATE))
								.setOperation((String) result.get(DB_OPERATION))
								.setObjectName((String) result.get(DB_OBJECTNAME))
								.setVersionId((String) result.get(DB_VERSION_ID))
								.setSourceBucketName((String) result.get(DB_SOURCE_BUCKET_NAME))
								.setTargetBucketName((String) result.get(DB_TARGET_BUCKET_NAME))
								.setTargetRegion((String) result.get(DB_TARGET_REGION))
								.build());
			}
			return items;
		} catch (Exception e) {
			log.error("Exception in getList", e);
			return Collections.emptyList();
		} finally {
			results.clear();
			results = null;
		}
	}
}
