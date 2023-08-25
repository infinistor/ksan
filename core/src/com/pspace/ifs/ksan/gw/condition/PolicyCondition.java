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
package com.pspace.ifs.ksan.gw.condition;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.identity.S3Parameter;

import org.slf4j.Logger;

public abstract class PolicyCondition {
    protected Logger logger;

	protected JsonNode jsonNode;
	public PolicyCondition(JsonNode jsonNode) {
		this.jsonNode = jsonNode;
	}

	protected String key;
    protected List<String> value;

	public String getKey() {
		return key;
	}

    public List<String> getValue() {
		return new ArrayList<>(this.value);
	}

	public abstract void process() throws GWException;

	public abstract boolean compare(String comp) throws GWException;

	public abstract boolean compareTagging(S3Parameter s3Parameter) throws GWException;

    public static final String STRING_EQUALS = "StringEquals";
    public static final String STRING_LIKE = "StringLike";
    public static final String STRING_NOT_LIKE = "StringNotLike";
	public static final String STRING_EQUALS_IGNORE_CASE = "StringEqualsIgnoreCase";
    public static final String STRING_NOT_EQUALS = "StringNotEquals";
	public static final String STRING_NOT_EQUALS_IGNORE_CASE = "StringNotEqualsIgnoreCase";
	public static final String NUMERIC_EQUALS = "NumericEquals";
    public static final String NUMERIC_NOT_EQUALS = "NumericNotEquals";
	public static final String NUMERIC_LESS_THAN = "NumericLessThan";
	public static final String NUMERIC_LESS_THAN_EQUALS = "NumericLessThanEquals";
	public static final String NUMERIC_GREATER_THAN = "NumericGreaterThan";
	public static final String NUMERIC_GREATER_THAN_EQUALS = "NumericGreaterThanEquals";
	public static final String DATE_EQUALS = "DateEquals";
    public static final String DATE_NOT_EQUALS = "DateNotEquals";
	public static final String DATE_LESS_THAN = "DateLessThan";
	public static final String DATE_LESS_THAN_EQUALS = "DateLessThanEquals";
	public static final String DATE_GREATER_THAN = "DateGreaterThan";
	public static final String DATE_GREATER_THAN_EQUALS = "DateGreaterThanEquals";
    public static final String IP_ADDRESS = "IpAddress";
    public static final String NOT_IP_ADDRESS = "NotIpAddress";
	public static final String LOG_UNKNOWN_CONDITION_TYPE = "Unknown Condition Type";
}
