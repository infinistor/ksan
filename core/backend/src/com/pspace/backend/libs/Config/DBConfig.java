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
package com.pspace.backend.libs.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DBConfig {
	static final int DEFAULT_POOL_SIZE = 20;

	@JsonProperty("host")
	public String host;
	@JsonProperty("port")
	public int port;
	@JsonProperty("db_name")
	public String databaseName;
	@JsonProperty("user")
	public String user;
	@JsonProperty("password")
	public String password;
	@JsonProperty("pool_size")
	public int poolSize;

	public DBConfig() {
		init();
	}

	public DBConfig(String host, int port, String databaseName, String user, String password, int poolSize) {
		this.host = host;
		this.port = port;
		this.databaseName = databaseName;
		this.user = user;
		this.password = password;
		this.poolSize = poolSize;
		if (this.poolSize < 1)
			this.poolSize = DEFAULT_POOL_SIZE;
	}

	public void init() {
		host = "";
		port = 3306;
		databaseName = "ksan";
		user = "root";
		password = "qwe123";
		poolSize = DEFAULT_POOL_SIZE;
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "";
		}
	}
}
