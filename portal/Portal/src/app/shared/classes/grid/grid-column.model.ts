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
 * 그리드 컬럼
 */
export class GridColumn {

	// 컬럼 타이틀
	public title?: string = '';

	// 필드명 (값)
	public field?: string = '';

	// 가로 px
	public width?: number = null;

	// 포멧
	public format?: string = '';

	// 파이프
	public pipe?: string = '';

	// 파이프의 파라미터로 넘길 값
	public pipeExtra?: any;

	// 사용자 정의 템플릿
	public customTemplate?: string = '';

	// 감춤 여부
	public isHidden?: boolean = false;

	// 수정 가능 여부
	public editable?: boolean = false;

	// 수정 타입
	public editType?: string = '';

	// 템플릿 사용 여부
	public useTemplateOutlet?: boolean = false;

	// 셀 클래스
	public class?: string = '';

	// LOCK 여부
	public locked?: boolean = false;

	// 소팅 가능 여부
	public sortable?: boolean = true;

	/**
	 * 생성자
	 * @param title
	 * @param field
	 * @param width
	 */
	constructor(title: string, field: string, width: number) {
		this.title = title;
		this.field = field;
		this.width = width;
	}
}
