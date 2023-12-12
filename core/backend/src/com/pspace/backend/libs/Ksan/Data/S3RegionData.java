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
package com.pspace.backend.Libs.Ksan.Data;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.backend.Libs.Utility;

public class S3RegionData {
	@JsonProperty("Name")
	public String name;
	@JsonProperty("Address")
	public String address;
	@JsonProperty("Port")
	public int port;
	@JsonProperty("SSLPort")
	public int sslPort;
	@JsonProperty("AccessKey")
	public String accessKey;
	@JsonProperty("SecretKey")
	public String secretKey;

	@JsonIgnore
	public AmazonS3 client;

	public void setClient() {
		client = Utility.createClient(this);
	}

	public S3RegionData(String Name, String Address, int Port, int SSLPort, String AccessKey, String SecretKey) {
		this.name = Name;
		this.address = Address;
		this.port = Port;
		this.sslPort = SSLPort;
		this.accessKey = AccessKey;
		this.secretKey = SecretKey;
		setClient();
	}
	public S3RegionData(ResponseRegion Data) {
		this.name = Data.Name;
		this.address = Data.Address;
		this.port = Data.Port;
		this.sslPort = Data.SSLPort;
		this.accessKey = Data.AccessKey;
		this.secretKey = Data.SecretKey;
		setClient();
	}

	public void update(S3RegionData Data) {
		name = Data.name;
		address = Data.address;
		port = Data.port;
		sslPort = Data.sslPort;
		accessKey = Data.accessKey;
		secretKey = Data.secretKey;
		setClient();
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}

	public String getHttpURL() {
		return String.format("http://%s:%d", address, port);
	}

	public String getHttpsURL() {
		return String.format("https://%s:%d", address, sslPort);
	}
}
