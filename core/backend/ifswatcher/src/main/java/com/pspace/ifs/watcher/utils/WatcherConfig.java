/*
 * Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
 * KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 
 * 3 of the License.  See LICENSE.md for details
 *
 * 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
 * KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
 * KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
 */

package com.pspace.ifs.watcher.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

public class WatcherConfig {
	private Properties properties;

	private String dbHost;
	private String dbS3;
	private String dbPort;
	private String dbUser;
	private String dbPass;
	private String chkVIP;

	public WatcherConfig() {
	}
	
	public WatcherConfig(String path) {
		properties = new Properties();
		try (InputStream myis = new FileInputStream(path)) {
			properties.load(myis);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Properties file is not exist");
		} catch (IOException e) {
			throw new IllegalArgumentException("Properties file load is fail");
		}
	}
	
	public void configure() throws URISyntaxException {
	    this.dbHost = properties.getProperty(WatcherConstants.PROPERTY_DB_HOST);
	    if (this.dbHost == null) {
	    	throw new IllegalArgumentException("Properties file must contain: " + WatcherConstants.PROPERTY_DB_HOST);
	    }
	    
	    this.dbS3 = properties.getProperty(WatcherConstants.PROPERTY_DB_NAME);
	    if (this.dbS3 == null) {
	    	throw new IllegalArgumentException("Properties file must contain: " + WatcherConstants.PROPERTY_DB_NAME);
	    }
	    
	    this.dbPort = properties.getProperty(WatcherConstants.PROPERTY_DB_PORT);
	    if (this.dbPort == null) {
	    	throw new IllegalArgumentException("Properties file must contain: " + WatcherConstants.PROPERTY_DB_PORT);
	    }
	    
	    this.dbUser = properties.getProperty(WatcherConstants.PROPERTY_DB_USER);
	    if (this.dbUser == null) {
	    	throw new IllegalArgumentException("Properties file must contain: " + WatcherConstants.PROPERTY_DB_USER);
	    }
	    
	    this.dbPass = properties.getProperty(WatcherConstants.PROPERTY_DB_PASS);
	    if (this.dbPass == null) {
	    	throw new IllegalArgumentException("Properties file must contain: " + WatcherConstants.PROPERTY_DB_PASS);
	    }

		this.chkVIP = properties.getProperty(WatcherConstants.PROPERTY_CHK_VIP);
	    if (this.chkVIP == null) {
	    	throw new IllegalArgumentException("Properties file must contain: " + WatcherConstants.PROPERTY_CHK_VIP);
	    }
		
	}

	public String dbHost() {
		return this.dbHost;
	}
	
	public String dbS3() {
		return this.dbS3;
	}
	
	public String dbPort() {
		return this.dbPort;
	}
	
	public String dbUser() {
		return this.dbUser;
	}
	
	public String dbPass() {
		return this.dbPass;
	}

	public String chkVIP() {
		return this.chkVIP;
	}

	public void setDbHost(String dbHost) {
		this.dbHost = dbHost;
	}
	
	public void setDbS3(String dbS3) {
		this.dbS3 = dbS3;
	}
	
	public void setDbPort(String dbPort) {
		this.dbPort = dbPort;
	}
	
	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}
	
	public void setDbPass(String dbPass) {
		this.dbPass = dbPass;
	}

	public void setchkVIP(String chkVIP) {
		this.chkVIP = chkVIP;
	}
}
