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
		item.dbRepository = config.dbType;
		item.dbHost = config.dbHost;
		item.dbport = config.dbPort;
		item.dbName = config.dbName;
		item.dbUsername = config.dbUser;
		item.dbPassword = config.dbPassword;

		return item;
	}

	public String getDBType() {
		return config.dbType;
	}

	public String getDBHost() {
		return config.dbHost;
	}

	public int getDBPort() {
		return config.dbPort;
	}

	public String getDBName() {
		return config.dbName;
	}

	public String getDBUser() {
		return config.dbUser;
	}

	public String getDBPassword() {
		return config.dbPassword;
	}

	public String getRegion() {
		return config.region;
	}

	public int getReplicationUploadThreadCount() {
		return config.threadCount;
	}

	public long getReplicationPartSize() {
		return config.partSize;
	}
}
