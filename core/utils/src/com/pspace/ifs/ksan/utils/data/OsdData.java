package com.pspace.ifs.ksan.utils.data;

public class OsdData {
    public static final String STOP = "ST";
    public static final String GET = "GE";
    public static final String PUT = "PU";
    public static final String DELETE = "DE";
    public static final String DELETE_REPLICA = "DR";
    public static final String COPY = "CO";
    public static final String PART = "PA";
    public static final String PART_COPY = "PC";
    public static final String COMPLETE_MULTIPART = "CM";
    public static final String ABORT_MULTIPART = "AB";
    public static final String GET_PART = "GP";
    public static final String DELETE_PART = "DP";
    public static final String FILE = "FILE";
    public static final String DELIMITER = ":";

    public static final int INDICATOR_SIZE = 2;
    public static final int PATH_INDEX = 1;
    public static final int OBJID_INDEX = 2;
    public static final int VERSIONID_INDEX = 3;
    public static final int SOURCE_RANGE_INDEX = 4;
    public static final int KEY_INDEX = 5;
    public static final int DEST_PATH_INDEX = 4;
    public static final int DEST_OBJID_INDEX = 5;
    public static final int DEST_VERSIONID_INDEX = 6;
    public static final int DEST_PARTNO_INDEX = 6;
    public static final int SRC_RANAGE_INDEX = 7;
    public static final int SOURCE_PATH_INDEX = 1;
    public static final int SOURCE_OBJID_INDEX = 2;
    public static final int TARGET_PATH_INDEX = 3;
    public static final int TARGET_OBJID_INDEX = 4;
    public static final int OFFSET_INDEX = 4;
    public static final int GET_LENGTH_INDEX = 5;
    public static final int PUT_LENGTH_INDEX = 4;
    public static final int PUT_REPLICATION_INDEX = 5;
    public static final int PUT_REPLICA_DISK_ID_INDEX = 6;
    public static final int PUT_KEY_INDEX = 7;
    public static final int PUT_MODE_INDEX = 8;
    public static final int PARTNO_INDEX = 3;
    public static final int COMPLETE_MULTIPART_PARTNOS = 4;
    public static final int ABORT_MULTIPART_PARTNOS = 3;
    public static final int PART_NO_INDEX = 3;
    public static final int PARTNOS_INDEX = 3;
    public static final int PART_COPY_OFFSET_INDEX = 7;
    public static final int PART_COPY_LENGTH_INDEX = 8;
    public static final int COPY_REPLICATION_INDED = 7;
    public static final int COPY_REPLICA_DISK_ID_INDEX = 8;

    private String ETag;
    private long fileSize;

    public OsdData() {
        ETag = "";
        fileSize = 0L;
    }

    public String getETag() {
        return ETag;
    }
    public void setETag(String eTag) {
        ETag = eTag;
    }
    public long getFileSize() {
        return fileSize;
    }
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
