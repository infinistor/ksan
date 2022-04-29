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
/**
 * 그리드 컴포넌트 인터페이스
 * (Grid 객체가 그리드 컴포넌트를 호출하기 위한 인터페이스, Grid 객체를 사용하는 컴포넌트는 이 인터페이스를 상속 받아야 함)
 */
export interface IGridComponent {
	/**
	 * 주어진 인덱스의 데이터를 선택한다.
	 * @param index 선택할 데이터 인덱스
	 */
	selectByIndex(index: number): any;

	/**
	 * 해당 인덱스 목록의 데이터를 선택한다.
	 * @param indexes 선택할 인덱스 목록
	 */
	selectByIndexes(indexes: number[]): any[];

	/**
	 * 주어진 데이터 동일한 키를 가지는 데이터를 선택한다.
	 * @param item 선택할 데이터 객체
	 */
	selectByItem(item: any): any;

	/**
	 * 주어진 데이터 동일한 키를 가지는 데이터를 선택한다.
	 * @param items 선택할 데이터 객체 목록
	 */
	selectByItems(items: any[]): any[];

	/**
	 * 모든 선택을 해제 한다.
	 */
	clearSelection(): any;

	/**
	 * 그리드 뷰를 업데이트 한다.
	 * @param data 데이터
	 * @param total 전체 개수
	 */
	updateGridView(data: any[], total: number): void;
}
