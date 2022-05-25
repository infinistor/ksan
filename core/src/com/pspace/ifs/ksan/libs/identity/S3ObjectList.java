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
package com.pspace.ifs.ksan.libs.identity;

import com.google.common.base.Strings;

public class S3ObjectList {
	
	// common
	private String encodingType;
	private String delimiter;
	private String prefix;
	private String maxKeys;
	private String marker;
	
	// list versions
	private String keyMarker;
	private String versionIdMarker;
	
	// list object v2
	
	private String startAfter;
	private String continuationToken;

	private String fetchOwner;

	public S3ObjectList() {
		encodingType = "";
		delimiter = "";
		prefix = "";
		marker = "";
		maxKeys = "";
		keyMarker = "";
		versionIdMarker = "";
		startAfter = "";
		continuationToken = "";
		fetchOwner = "";
	}

	public String getEncodingType() {
		return Strings.nullToEmpty(encodingType);
	}

	public void setEncodingType(String encodingType) {
		this.encodingType = encodingType;
	}

	public String getDelimiter() {
		return Strings.nullToEmpty(delimiter);
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public String getPrefix() {
		return Strings.nullToEmpty(prefix);
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getMaxKeys() {
		return Strings.nullToEmpty(maxKeys);
	}

	public void setMaxKeys(String maxKeys) {
		this.maxKeys = maxKeys;
	}

	public String getMarker() {
		return Strings.nullToEmpty(marker);
	}

	public void setMarker(String marker) {
		this.marker = marker;
	}

	public String getKeyMarker() {
		return Strings.nullToEmpty(keyMarker);
	}

	public void setKeyMarker(String keyMarker) {
		this.keyMarker = keyMarker;
	}

	public String getVersionIdMarker() {
		return Strings.nullToEmpty(versionIdMarker);
	}

	public void setVersionIdMarker(String versionIdMarker) {
		this.versionIdMarker = versionIdMarker;
	}

	public String getStartAfter() {
		return Strings.nullToEmpty(startAfter);
	}

	public void setStartAfter(String startAfter) {
		this.startAfter = startAfter;
	}

	public String getContinuationToken() {
		return Strings.nullToEmpty(continuationToken);
	}

	public void setContinuationToken(String continuationToken) {
		this.continuationToken = continuationToken;
	}

	public void setFetchOwner(String fetchOwner) {
		this.fetchOwner = fetchOwner;
    }

	public String getFetchOwner() {
		return fetchOwner;
    }
}