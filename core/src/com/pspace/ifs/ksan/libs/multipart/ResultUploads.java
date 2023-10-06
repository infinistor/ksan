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
package com.pspace.ifs.ksan.libs.multipart;

import java.util.ArrayList;
import java.util.List;

public class ResultUploads {
    private int maxUploads;
    private String delimiter;
    private String keyMarker;
    private String prefix;
    private String uploadIdMarker;
    private List<Upload> list;
    private boolean isTruncated;

    public int getMaxUploads() {
        return maxUploads;
    }
    
    public void setMaxUploads(int maxUploads) {
        this.maxUploads = maxUploads;
    }
    
    public String getDelimiter() {
        return delimiter;
    }
    
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
    
    public String getKeyMarker() {
        return keyMarker;
    }
    
    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public String getUploadIdMarker() {
        return uploadIdMarker;
    }
    
    public void setUploadIdMarker(String uploadIdMarker) {
        this.uploadIdMarker = uploadIdMarker;
    }
    
    public List<Upload> getList() {
        return list;
    }
    
    public void setList(List<Upload> list) {
        this.list = list;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public void setTruncated(boolean truncated) {
        this.isTruncated = truncated;
    }
}
