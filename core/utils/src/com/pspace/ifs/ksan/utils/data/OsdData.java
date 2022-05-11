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
