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
import {WindowRef, WindowService} from '@progress/kendo-angular-dialog';
import {Injectable, TemplateRef, ViewContainerRef} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {List} from 'linq-collections';

@Injectable({ providedIn: 'root' })
export class CommonWindowService {

	/**
	 * 윈도우 서비스
	 */
	private _windowService: WindowService = null;

	// 모달 모드 변경 여부
	private _modalModeChanged: BehaviorSubject<boolean> = null;
	// 모달 모드 변경 여부 변경 감시
	modalModeChanged$: Observable<boolean> = null;

	/**
	 * 모달 모드인지 여부
	 */
	public get modalMode(): boolean {
		let result: boolean = false;

		// 모달 상태인 윈도우가 존재하는 경우
		if (new List<WindowRef>(this._windows).where(i => (i as any).modal === true).any())
			result = true;

		return result;
	}

	/**
	 * 윈도우 목록
	 */
	private _windows: WindowRef[] = [];

	constructor(windowService: WindowService) {
		this._windowService = windowService;

		this._modalModeChanged = new BehaviorSubject<boolean>(false);
		this.modalModeChanged$ = this._modalModeChanged.asObservable();
	}

	/**
	 * 다이얼로그를 표시한다.
	 * @param title 다이얼로그 제목
	 * @param content 내용
	 * @param keepContent 윈도우가 최소화되어도 컨텐츠를 유지할지 여부
	 * @param resizable 크기 변경 가능 여부
	 * @param width 다이얼로그 넓이 (0인 경우, 지정안함)
	 * @param height 다이얼로그 높이 (0인 경우, 지정안함)
	 * @param modal 모달 형태로 표시할지 여부
	 * @param containerRef 윈도우를 표시할 뷰 컨테이너 참조 객체
	 */
	show(title: string | TemplateRef<any>, content: string | TemplateRef<any>, keepContent: boolean, resizable: boolean, width: number = 0, height: number = 0, modal: boolean = false, containerRef: ViewContainerRef = null): WindowRef {

		let window: WindowRef = null;
		const settings: any = {
			content,
			resizable,
			keepContent,
			width,
			height,
			minWidth: width
		};

		// 타이틀이 템플릿 객체인 경우
		if (title instanceof TemplateRef)
			settings.titleBarContent = title;
		// 타이틀이 템플릿 객체가 아닌 경우, 문자열로 처리
		else
			settings.title = title;

		// 별도의 컨테이너에 표시하고자 하는 경우
		if (containerRef)
			settings.appendTo = containerRef;

		window = this._windowService.open(settings);
		(window as any).modal = modal;
		this._windows.push(window);

		// 모달 모드 변경 알림
		this._modalModeChanged.next(this.modalMode);

		// 윈도우 종료 시
		window.result.subscribe(() => {
			const index = this._windows.findIndex(i => i === window);
			if (index >= 0)
				this._windows.splice(index, 1);

			// 모달 모드 변경 알림
			this._modalModeChanged.next(this.modalMode);
		});

		return window;
	}
}
