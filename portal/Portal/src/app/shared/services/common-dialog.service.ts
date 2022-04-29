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
import {DialogCloseResult, DialogRef, DialogService} from '@progress/kendo-angular-dialog';
import {Injectable, TemplateRef, ViewContainerRef} from '@angular/core';
import {Observable} from 'rxjs';
import {DialogResult} from '@progress/kendo-angular-dialog/dist/es2015/dialog/dialog-settings';
import {TranslateService} from '@ngx-translate/core';

/**
 * 다이얼로그 설정 옵션
 */
export type DialogOption = {
	/**
	 * 다이얼로그 제목
	 */
	title: string,
	/**
	 * 다이얼로그 내용 혹은 템플릿
	 */
	content: string | TemplateRef<any>,
	/**
	 * 다이얼로그 버튼 문자열 목록 혹은 템플릿
	 */
	actionButtons: any[] | TemplateRef<any>,
	/**
	 * 다이얼로그 타이틀 영역 닫기 버튼 활성화 여부
	 */
	enableClose?: boolean,
	/**
	 * 자동 포커스 컨트롤명
	 */
	autoFocusedElement?: string,
	/**
	 * 다이얼로그 넓이
	 */
	width?: number,
	/**
	 * 다이얼로그 높이
	 */
	height?: number,
	/**
	 * 표시할 상위 컨테이너
	 */
	parentContainer?: ViewContainerRef
};

@Injectable({ providedIn: 'root' })
export class CommonDialogService {

	/**
	 * 다이얼로그 서비스
	 */
	private _dialogService: DialogService = null;

	constructor(
		dialogService: DialogService,
		private translateService: TranslateService,
	) {
		this._dialogService = dialogService;
	}

	/**
	 * 확인 다이얼로그를 표시한다.
	 * @param title 다이얼로그 제목
	 * @param content 다이얼로그 내용 혹은 템플릿
	 * @param width 다이얼로그 넓이 (0인 경우, 지정안함)
	 * @param height 다이얼로그 높이 (0인 경우, 지정안함)
	 * @param parentContainer 표시할 컨테이너
	 */
	confirm(title: string, content: string | TemplateRef<any>, width: number = 0, height: number = 0, parentContainer: ViewContainerRef = null): Observable<DialogResult> {

		const yes: string = this.translateService.instant('UL_COMMON__YES');
		const no: string = this.translateService.instant('UL_COMMON__NO');

		const dialog: DialogRef = this.create({
			title,
			content,
			actionButtons: [
				{ text: yes, primary: true },
				{ text: no }
			],
			enableClose: true,
			autoFocusedElement: null,
			width,
			height,
			parentContainer
		});

		return dialog.result;
	}

	/**
	 * 안내 다이얼로그를 표시한다.
	 * @param title 다이얼로그 제목
	 * @param content 다이얼로그 내용 혹은 템플릿
	 * @param width 다이얼로그 넓이 (0인 경우, 지정안함)
	 * @param height 다이얼로그 높이 (0인 경우, 지정안함)
	 * @param parentContainer 표시할 컨테이너
	 */
	information(title: string, content: string | TemplateRef<any>, width: number = 0, height: number = 0, parentContainer: ViewContainerRef = null): Observable<DialogResult> {

		const ok: string = this.translateService.instant('UL_COMMON__OK');

		const dialog: DialogRef = this.create({
			title,
			content,
			actionButtons: [
				{ text: ok, primary: true }
			],
			enableClose: true,
			autoFocusedElement: null,
			width,
			height,
			parentContainer
		});

		return dialog.result;
	}

	/**
	 * 모달 다이얼로그를 표시한다.
	 * @param dialogOption 다이얼로그 옵션
	 */
	showModal(dialogOption: DialogOption): Observable<DialogResult> {
		const dialog: DialogRef = this.create(dialogOption);

		return dialog.result;
	}

	/**
	 * 모달리스 다이얼로그를 표시한다.
	 * @param dialogOption 다이얼로그 옵션
	 */
	show(dialogOption: DialogOption): DialogRef {
		return this.create(dialogOption);
	}

	/**
	 * 다이얼로그를 생성한다.
	 * @param dialogOption 다이얼로그 옵션
	 */
	private create(dialogOption: DialogOption): DialogRef {

		if (dialogOption) {

			// 자동 포커스명이 유효하지만 '#'으로 시작되지 않는 경우, 추가
			if (dialogOption.autoFocusedElement && !dialogOption.autoFocusedElement.isEmpty() && !dialogOption.autoFocusedElement.startsWith('#'))
				dialogOption.autoFocusedElement = '#' + dialogOption.autoFocusedElement;

			return this._dialogService.open({
				title: dialogOption.title,
				content: dialogOption.content,
				actions: dialogOption.actionButtons,
				width: dialogOption.width,
				height: dialogOption.height,
				minWidth: dialogOption.width,
				appendTo: dialogOption.parentContainer,
				autoFocusedElement: dialogOption.autoFocusedElement,
				preventAction: (ev) => {
					return !dialogOption.enableClose && ev instanceof DialogCloseResult;
				}
			});
		}
	}
}
