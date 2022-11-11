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
package metering;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MeteringConfig {

	static final int DEFAULT_METER_MINUTE = 5;
	static final int DEFAULT_ASSET_HOUR = 1;
	static final int MINUTE = 60 * 1000;
	static final int HOUR = 60 * MINUTE;
	final int meter;
	final int asset;

	public MeteringConfig(int meter, int asset) {
		if (meter > 1) 	this.meter = meter;
		else 			this.meter = DEFAULT_METER_MINUTE;
		if (asset > 1) 	this.asset = asset;
		else 			this.asset = DEFAULT_ASSET_HOUR;
	}

	public int getMeterDelay() {
		return this.meter * MINUTE;
	}

	public int getMeter() {
		return this.meter;
	}

	public int getAssetDelay() {
		return this.asset * HOUR;
	}

	public int getAsset() {
		return this.asset;
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
