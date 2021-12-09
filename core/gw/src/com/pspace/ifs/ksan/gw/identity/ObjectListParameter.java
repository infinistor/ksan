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
package com.pspace.ifs.ksan.gw.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Strings;

public class ObjectListParameter {
	private boolean istruncated;
	private String nextMarker;
	private String nextVersion;
	private String nextUploadid;
	private int nextPartNumber;
	private SortedMap<String, String> commonPrefixes;
	private List<S3Metadata> objects;
	
	public ObjectListParameter() {
        istruncated = false;
        nextMarker = null;
        nextVersion = null;
        nextUploadid = null;
        nextPartNumber = 0;
		objects = new ArrayList<S3Metadata>();
		commonPrefixes = new TreeMap<String, String>();
	}

    public boolean isTruncated() {
        return istruncated;
    }

    public void setIstruncated(boolean istruncated) {
        this.istruncated = istruncated;
    }

    public String getNextMarker() {
        return Strings.nullToEmpty(nextMarker);
    }

    public void setNextMarker(String nextMarker) {
        this.nextMarker = nextMarker;
    }

    public String getNextVersion() {
        return Strings.nullToEmpty(nextVersion);
    }

    public void setNextVersion(String nextVersion) {
        this.nextVersion = nextVersion;
    }

    public String getNextUploadid() {
        return Strings.nullToEmpty(nextUploadid);
    }

    public void setNextUploadid(String nextUploadid) {
        this.nextUploadid = nextUploadid;
    }

    public int getNextPartNumber() {
        return nextPartNumber;
    }

    public void setNextPartNumber(int nextPartNumber) {
        this.nextPartNumber = nextPartNumber;
    }

    public SortedMap<String, String> getCommonPrefixes() {
        return commonPrefixes;
    }

    public void setCommonPrefixes(SortedMap<String, String> commonPrefixes) {
        this.commonPrefixes = commonPrefixes;
    }

    public List<S3Metadata> getObjects() {
        return objects;
    }

    public void setObjects(List<S3Metadata> objects) {
        this.objects = objects;
    }
}
