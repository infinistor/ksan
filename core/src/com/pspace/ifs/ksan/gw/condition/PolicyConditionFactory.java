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

import com.fasterxml.jackson.databind.JsonNode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyConditionFactory {
    private final static Logger logger = LoggerFactory.getLogger(PolicyConditionFactory.class);

	public PolicyConditionFactory() {}

	public static PolicyCondition createPolicyCondition(String category, JsonNode jsonNode) throws GWException {
		switch (category) {
			case PolicyCondition.STRING_EQUALS:
				return new StringEqual(jsonNode);
			case PolicyCondition.STRING_LIKE:
				return new StringLike(jsonNode);
			case PolicyCondition.STRING_NOT_LIKE:
				return new StringNotLike(jsonNode);
			case PolicyCondition.STRING_EQUALS_IGNORE_CASE:
				return new StringEqualsIgnoreCase(jsonNode);				
			case PolicyCondition.STRING_NOT_EQUALS:
				return new StringNotEquals(jsonNode);
			case PolicyCondition.STRING_NOT_EQUALS_IGNORE_CASE:
				return new StringNotEqualsIgnoreCase(jsonNode);

			case PolicyCondition.NUMERIC_EQUALS:
				return new NumericEquals(jsonNode);
			case PolicyCondition.NUMERIC_NOT_EQUALS:
				return new NumericNotEquals(jsonNode);
            case PolicyCondition.NUMERIC_LESS_THAN:
				return new NumericLessThan(jsonNode);
			case PolicyCondition.NUMERIC_LESS_THAN_EQUALS:
				return new NumericLessThanEquals(jsonNode);
			case PolicyCondition.NUMERIC_GREATER_THAN:
				return new NumericGreaterThan(jsonNode);
			case PolicyCondition.NUMERIC_GREATER_THAN_EQUALS:
				return new NumericGreaterThanEquals(jsonNode);

			case PolicyCondition.DATE_EQUALS:
				return new DateEquals(jsonNode);
			case PolicyCondition.DATE_NOT_EQUALS:
				return new DateNotEquals(jsonNode);
			case PolicyCondition.DATE_LESS_THAN:
				return new DateLessThan(jsonNode);
			case PolicyCondition.DATE_LESS_THAN_EQUALS:
				return new DateLessThanEquals(jsonNode);
			case PolicyCondition.DATE_GREATER_THAN:
				return new DateGreaterThan(jsonNode);
			case PolicyCondition.DATE_GREATER_THAN_EQUALS:
				return new DateGreaterThanEquals(jsonNode);

			case PolicyCondition.IP_ADDRESS:
				return new IpAddress(jsonNode);
			case PolicyCondition.NOT_IP_ADDRESS:
				return new NotIpAddress(jsonNode);
			default:
				logger.error(PolicyCondition.LOG_UNKNOWN_CONDITION_TYPE);
				break;
		}

		return null;
	}
}
