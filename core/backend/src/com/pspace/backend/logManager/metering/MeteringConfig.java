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
	
	private static final int DEFAULT_MINUTE = 5;
	private static final int DEFAULT_HOUR = 1;
	private static final int MINUTE = 60 * 1000;
	private static final int HOUR = 60 * MINUTE;
	private int MeterDelay;
	private int AssetDelay;

	public MeteringConfig(int MeterDelay, int AssetDelay) {
		SetMeterDelay(MeterDelay);
		SetAssetDelay(AssetDelay);
	}

	public void Init() {
		// 기본값 5분
		SetMeterDelay(0);
		// 기본값 1시간
		SetAssetDelay(0);
	}

	public void SetMeterDelay(int Minutes) {
		//시간을 ms 단위로 변경한다
		if (Minutes > 0)
			this.MeterDelay = Minutes * MINUTE;
		else
			this.MeterDelay = DEFAULT_MINUTE * MINUTE;
	}

	public void SetAssetDelay(int Hours) {
		//시간을 ms 단위로 변경한다
		if (Hours > 0)
		this.AssetDelay = Hours * HOUR;
	else
		this.AssetDelay = DEFAULT_HOUR * HOUR;
	}

	public int getMeterDelay() {
		return this.MeterDelay;
	}
	public int getMeter(){
		return this.MeterDelay / MINUTE;
	}
	public int getAssetDelay() {
		return this.AssetDelay;
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
