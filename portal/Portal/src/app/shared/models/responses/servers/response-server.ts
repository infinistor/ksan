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
import {EnumServerState} from '../../enums/enum-server-state';


export class ResponseServer {
	/**
	 * 서버 아이디
	 */
	public Id: string;
	/**
	 * 서버명
	 */
	public Name: string;
	/**
	 * 설명
	 */
	public Description: string;
	/**
	 * CPU 모델
	 */
	public CpuModel: string;
	/**
	 * CPU 클럭
	 */
	public Clock: number;
	/**
	 * 서버 상태
	 */
	public State: EnumServerState;
	/**
	 * Rack 정보
	 */
	public Rack: string;
	/**
	 * 1분 Load Average
	 */
	public LoadAverage1M: number;
	/**
	 * 5분 Load Average
	 */
	public LoadAverage5M: number;
	/**
	 * 15분 Load Average
	 */
	public LoadAverage15M: number;
	/**
	 * 전체 메모리 크기
	 */
	public MemoryTotal: number;
	/**
	 * 사용 메모리 크기
	 */
	public MemoryUsed: number;
	/**
	 * 남은 메모리 크기
	 */
	public MemoryFree: number;
	/**
	 * 수정일시
	 */
	public ModDate: Date;
	/**
	 * 수정인 아이디
	 */
	public ModId: string;
	/**
	 * 수정인 이름
	 */
	public ModName: string;
}


