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
package config;

import java.io.IOException;

import com.pspace.backend.libs.Config.ReplicationConfig;
import com.pspace.backend.libs.Ksan.PortalManager;
import com.pspace.ifs.ksan.objmanager.ObjManagerConfig;

public class ConfigManager {

	private static ReplicationConfig config = null;
	private static PortalManager portal = null;

	public static ConfigManager getInstance() {
		return LazyHolder.INSTANCE;
	}

	private static class LazyHolder {
		private static final ConfigManager INSTANCE = new ConfigManager();
	}

	private ConfigManager() {
		portal = PortalManager.getInstance();
	}

	public void update() throws IllegalStateException {
		config = portal.getReplicationConfig();
		if (config == null)
			throw new IllegalStateException("Backend Config is not initialized");
	}

	public ObjManagerConfig getObjManagerConfig() throws IOException {
		var item = new ObjManagerConfig();
		item.dbRepository = config.DBType;
		item.dbHost = config.DBHost;
		item.dbport = config.DBPort;
		item.dbName = config.DBName;
		item.dbUsername = config.DBUser;
		item.dbPassword = config.DBPassword;

		return item;
	}

	public String getDBType() {
		return config.DBType;
	}

	public String getDBHost() {
		return config.DBHost;
	}

	public int getDBPort() {
		return config.DBPort;
	}

	public String getDBName() {
		return config.DBName;
	}

	public String getDBUser() {
		return config.DBUser;
	}

	public String getDBPassword() {
		return config.DBPassword;
	}

	public String getRegion() {
		return config.Region;
	}

	public int getReplicationUploadThreadCount() {
		return config.ReplicationUploadThreadCount;
	}

	public long getReplicationPartSize() {
		return config.ReplicationPartSize;
	}
}
