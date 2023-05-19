/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.ifs.ksan.libs.data;

public class OsdData {
    public static final String STOP = "ST";
    public static final String GET = "GE";
    public static final String PUT = "PU";
    public static final String DELETE = "DT";
    public static final String DELETE_REPLICA = "DR";
    public static final String COPY = "CO";
    public static final String PART = "PA";
    public static final String PART_COPY = "PC";
    public static final String COMPLETE_MULTIPART = "CM";
    public static final String ABORT_MULTIPART = "AB";
    public static final String GET_PART = "GP";
    public static final String DELETE_PART = "DP";
    public static final String FILE = "FILE";
    public static final String GET_EC_PART = "GC";
    public static final String PUT_EC_PART = "PE";
    public static final String DELETE_EC_PART = "DE";
    public static final String GET_MULTIPART = "GM";
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
    public static final int SRC_LENGTH_INDEX = 8;
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
    public static final int COMPLETE_MULTIPART_KEY_INDEX = 4;
    public static final int COMPLETE_MULTIPART_REPLICATION_INDEX = 5;
    public static final int COMPLETE_MULTIPART_REPLICA_DISKID_INDEX = 6;
    public static final int COMPLETE_MULTIPART_PARTNOS_INDEX = 7;
    public static final int ABORT_MULTIPART_PARTNOS = 3;
    public static final int UPLOAD_KEY_INDEX = 3;
    public static final int PART_NO_INDEX = 4;
    public static final int PART_RANGE_INDEX = 2;
    public static final int PART_LENGTH_INDEX = 5;
    public static final int PART_KEY_INDEX = 6;
    public static final int PART_COPY_OFFSET_INDEX = 7;
    public static final int PART_COPY_LENGTH_INDEX = 8;
    public static final int COPY_REPLICATION_INDED = 7;
    public static final int COPY_REPLICA_DISK_ID_INDEX = 8;
    public static final int PUT_EC_LENGTH_INDEX = 2;
    public static final int PART_DELETE_UPLOADID_INDEX = 3;
    public static final int PART_DELETE_PARTNUMBER_INDEX = 4;

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
