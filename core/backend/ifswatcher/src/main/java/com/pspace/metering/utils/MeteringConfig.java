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

package com.pspace.metering.utils;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MeteringConfig {

	@JsonProperty("dbhost")
	private String dbHost;

	@JsonProperty("dbs3")
	private String dbS3;

	@JsonProperty("dbport")
	private String dbPort;

	@JsonProperty("dbuser")
	private String dbUser;

	@JsonProperty("dbpass")
	private String dbPass;

	@JsonProperty("chkvip")
	private String chkVIP;

	public MeteringConfig() {
		this.dbHost = "";
		this.dbS3 = "";
		this.dbPort = "";
		this.dbUser = "";
		this.dbPass = "";
		this.chkVIP = "";
	}

	public MeteringConfig(String dbHost, String dbS3, String dbPort, String dbUser, String dbPass, String chkVIP) {
		this.dbHost = dbHost;
		this.dbS3 = dbS3;
		this.dbPort = dbPort;
		this.dbUser = dbUser;
		this.dbPass = dbPass;
		this.chkVIP = chkVIP;
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
