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
package com.pspace.Portal;

import java.io.File;
import java.io.FileReader;

import org.apache.commons.lang3.math.NumberUtils;
import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalConfig {
	private final String STR_FILENAME = "/usr/local/ksan/etc/ksanMonitor.conf";
	/////////////////////////// MGS /////////////////////////////////////
	private final String STR_MG = "mgs";
	private final String STR_MGS_IP = "PortalIp";
	private final String STR_MGS_PORT = "PortalPort";
	private final String STR_MGS_KEY = "PortalApiKey";
		
	/*********************************************************************************************************/
	static final Logger logger = LoggerFactory.getLogger(PortalConfig.class);
	public final String FileName;
	private final Ini ini = new Ini();
	/*********************************************************************************************************/

	public String Ip;
	public int Port;
	public String Key;

	public PortalConfig(String FileName) {
		if (FileName.isEmpty())
			this.FileName = STR_FILENAME;
		else
			this.FileName = FileName;
	}

	public boolean GetConfig() {

		File file = new File(FileName);
		try {
			ini.load(new FileReader(file));

			Ip = ReadKeyToString(STR_MG, STR_MGS_IP);
			Port = ReadKeyToInt(STR_MG, STR_MGS_PORT);
			Key = ReadKeyToString(STR_MG, STR_MGS_KEY);

		} catch (Exception e) {
			logger.error("", e);
			return false;
		}
		return true;
	}

	//////////////////////////////////////////////////////////////////////////
	private String ReadKeyToString(String Section, String Key) {
		String Value = ini.get(Section, Key);
		return (Value == null) ? "" : Value;
	}

	private int ReadKeyToInt(String Section, String Key) {
		return NumberUtils.toInt(ini.get(Section, Key));
	}
	// private boolean ReadKeyToBoolean(String Section, String Key)
	// {
	// return Boolean.parseBoolean(ini.get(Section, Key));
	// }
}
