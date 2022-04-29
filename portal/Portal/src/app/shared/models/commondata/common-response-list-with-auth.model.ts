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
import { CommonResponseList } from './common-response-list.model';
import {EnumResponseResult} from '../enums/enum-response-result.model';
import {QueryResults} from './query-results.model';

/**
 * 인증 결과가 포함된 일반 데이터 목록 응답 클래스
 */
export class CommonResponseListWithAuth<T> extends CommonResponseList<T>
{
	/**
	 * 생성자
	 * @param IsNeedLogin 로그인이 필요한지 여부
	 * @param AccessDenied 접근 거부 여부
	 * @param Result 응답 결과
	 * @param Code 응답 에러/경고 코드
	 * @param Message 응답 에러/경고 메시지
	 * @param Data 데이터 목록 정보 객체
	 */
	constructor(IsNeedLogin?: boolean, AccessDenied?: boolean, Result: EnumResponseResult = EnumResponseResult.Error, Code: string = '', Message: string = '', Data: QueryResults<T> = null) {
		super(Result, Code, Message, Data);
		this.IsNeedLogin = IsNeedLogin;
		this.AccessDenied = AccessDenied;
	}

	/**
	 * 로그인이 필요한지 여부
	 */
	public IsNeedLogin: boolean;
	/**
	 * 접근 거부 여부
	 */
	public AccessDenied: boolean;
}
