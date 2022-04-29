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
export class CommonSearch {
	// 기간 검색 필드명
	public searchPeriodField: string = null;
	// 시작일자
	public searchStartDate: string = null;
	// 종료일자
	public searchEndDate: string = null;
	// 검색항목
	public searchFields: Array<string> = new Array<string>();
	// 검색어
	public searchKeyword: string = '';

	/**
	 * 생성자
	 * @param searchPeriodField 검색 기간 필드
	 * @param searchStartDate 검색 시작 일시
	 * @param searchEndDate 검색 종료 일시
	 * @param searchFields 검색 필드 목록
	 * @param searchKeyword 검색어
	 */
	constructor(searchPeriodField: string, searchStartDate: Date, searchEndDate: Date, searchFields: Array<string>, searchKeyword: string) {
		this.searchPeriodField = searchPeriodField;
		this.searchStartDate = searchStartDate != null ? this.toLongDateString(searchStartDate) : null;
		this.searchEndDate = searchEndDate != null ? this.toLongDateString(searchEndDate) : null;
		this.searchFields = searchFields;
		this.searchKeyword = searchKeyword;
	}

	/**
	 * 일자 시분초를 포함한 날짜 문자열을 반환한다.
	 * (yyyy-MM-dd HH:mm:ss 형식의 문자열로 변환)
	 */
	private toLongDateString(date: Date): string {
		return date.getFullYear() + '-' +
			('0' + (date.getMonth() + 1)).slice(-2) + '-' +
			('0' + date.getDate()).slice(-2) + ' ' +
			('0' + date.getHours()).slice(-2) + ':' +
			('0' + date.getMinutes()).slice(-2) + ':' +
			('0' + date.getSeconds()).slice(-2);
	}
}
