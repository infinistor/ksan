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
package com.pspace.ifs.ksan.gw.object.multipart;

import java.util.SortedMap;

public class ResultParts {
    private int maxParts;
    private String partNumberMarker;
    private String uploadId;
    private SortedMap<Integer, Part> list;
    private boolean isTruncated;

    public ResultParts() {}

    public ResultParts(String uploadId, int maxParts) {
        this.uploadId = uploadId;
        this.maxParts = maxParts;
    }

    public int getMaxParts() {
        return maxParts;
    }
    
    public void setMaxParts(int maxParts) {
        this.maxParts = maxParts;
    }
    
    public String getPartNumberMarker() {
        return partNumberMarker;
    }
    
    public void setPartNumberMarker(String partNumberMarker) {
        this.partNumberMarker = partNumberMarker;
    }
    
    public String getUploadId() {
        return uploadId;
    }
    
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }
    
    public SortedMap<Integer, Part> getListPart() {
        return list;
    }
    
    public void setListPart(SortedMap<Integer, Part> listPart) {
        this.list = listPart;
    }
    
    public boolean isTruncated() {
        return isTruncated;
    }

    public void setTruncated(boolean truncated) {
        this.isTruncated = truncated;
    }
}
