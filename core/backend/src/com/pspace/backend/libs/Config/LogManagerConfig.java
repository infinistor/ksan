/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License. See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.libs.Config;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;

public class LogManagerConfig implements IConfig {

	@JsonProperty("ksan.region")
	public String Region;

	@JsonProperty("logM.db_repository")
	public String dbType;

	@JsonProperty("logM.db_host")
	public String dbHost;

	@JsonProperty("logM.db_port")
	public int dbPort;

	@JsonProperty("logM.db_name")
	public String dbName;

	@JsonProperty("logM.db_user")
	public String dbUser;

	@JsonProperty("logM.db_password")
	public String dbPassword;

	@JsonProperty("logM.db_pool_size")
	public int dbPoolSize;

	@JsonProperty("logM.db_expires")
	public int dbExpires;

	@JsonProperty("logM.check_interval")
	public int checkInterval;

	@JsonProperty("logM.meter_minute")
	public int meterMinute;

	@JsonProperty("logM.assert_hour")
	public int assertHour;

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "";
		}
	}

	public ObjManagerConfig getObjManagerConfig() throws IOException {
		var config = new ObjManagerConfig();
		config.dbRepository = dbType;
		config.dbHost = dbHost;
		config.dbPort = dbPort;
		config.dbName = dbName;
		config.dbUsername = dbUser;
		config.dbPassword = dbPassword;

		return config;
	}

	public DBConfig getDBConfig() {
		var config = new DBConfig();
		config.Type = dbType;
		config.Host = dbHost;
		config.Port = dbPort;
		config.DatabaseName = dbName;
		config.User = dbUser;
		config.Password = dbPassword;
		config.PoolSize = dbPoolSize;
		config.Expires = dbExpires;

		return config;
	}
}
