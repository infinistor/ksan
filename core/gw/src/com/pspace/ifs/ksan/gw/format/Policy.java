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
package com.pspace.ifs.ksan.gw.format;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.pspace.ifs.ksan.gw.utils.GWConstants;

public final class Policy {
	@JsonProperty(GWConstants.VERSION)
	public
	String version;

	@JsonProperty(GWConstants.POLICY_ID)
	public
	String id;

	@JsonProperty(GWConstants.STATEMENT)
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	public
	List<Statement> statements;
	
	public static final class Statement {
		@JsonProperty(GWConstants.SID)
		public
		String sid;

		@JsonProperty(GWConstants.EFFECT)
		public
		String effect;

		@JsonProperty(GWConstants.PRINCIPAL)
		public
		Principal principal;

		public static final class Principal {
			@JsonProperty(GWConstants.AWS)
			@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
			public
			List<String> aws;
		}

		@JsonProperty(GWConstants.ACTION)
		@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
		public
		List<String> actions;

		@JsonProperty(GWConstants.RESOURCE)
		@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
		public
		List<String> resources;

		@JsonProperty(GWConstants.CONDITION)
		public
		Condition condition;

		public static final class Condition { 
			private Multimap<String, JsonNode> userExtensions = ArrayListMultimap.create(); 
			
			@JsonAnyGetter 
			public Multimap<String, JsonNode> getUserExtensions() { 
				return userExtensions; 
			} 
			
			@JsonAnySetter 
			public void setUserExtensions(String key, JsonNode value) { 
				userExtensions.put(key, value);
			}
		}
	}
}