/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License.See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
/**
 * 데이터 목록 정보 클래스
 */
export class QueryResults<T> {
	/**
	 * 생성자
	 * @param Items 데이터 목록
	 * @param TotalCount 전체 데이터 수
	 * @param Skips 건너뛸 데이터 수
	 * @param PageNo 현재 페이지 번호
	 * @param CountPerPage 페이지 당 레코드 수
	 * @param PagePerSection 섹션 당 페이지 수
	 */
	constructor(Items?: Array<T>, TotalCount?: number, Skips?: number, PageNo ?: number, CountPerPage?: number, PagePerSection?: number) {
		this.Items = Items;
		this.TotalCount = TotalCount;
		this.Skips = Skips;
		this.PageNo = PageNo;
		this.CountPerPage = CountPerPage;
		this.PagePerSection = PagePerSection;
	}

	/**
	 * 데이터 목록
	 */
	public Items: Array<T>;
	/**
	 * 전체 데이터 수
	 */
	public TotalCount: number;
	/**
	 * 건너뛸 데이터 수
	 */
	public Skips: number;
	/**
	 * 현재 페이지 번호
	 */
	public PageNo: number;
	/**
	 * 페이지 당 레코드 수
	 */
	public CountPerPage: number;
	/**
	 * 섹션 당 페이지 수
	 */
	public PagePerSection: number;
	/**
	 * 전체 페이지 수
	 */
	public TotalPage: number;
	/**
	 * 시작 페이지 번호
	 */
	public StartPageNo: number;
	/**
	 * 마지막 페이지 번호
	 */
	public EndPageNo: number;
	/**
	 * 페이지 번호 목록
	 */
	public PageNos: Array<number>;
	/**
	 * 이전 페이지가 존재하는지 여부
	 */
	public HavePreviousPage: boolean;
	/**
	 * 다음 페이지가 존재하는지 여부
	 */
	public HaveNextPage: boolean;
	/**
	 * 이전 섹션이 존재하는지 여부
	 */
	public HavePreviousPageSection: boolean;
	/**
	 * 다음 섹션이 존재하는지 여부
	 */
	public HaveNextPageSection: boolean;
}
