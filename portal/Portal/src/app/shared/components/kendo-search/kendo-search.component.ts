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
import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {Align, PopupComponent} from '@progress/kendo-angular-popup';
import {CommonSearch} from '../../classes/grid/common-search.model';
import * as moment from 'moment';
import {MultiSelectComponent} from '@progress/kendo-angular-dropdowns';
import {delay, map, switchMap} from 'rxjs/operators';
import {from} from 'rxjs';

type ItemType = { text: string, value: string };

@Component({
		selector: 'app-common-kendo-search',
		templateUrl: 'kendo-search.component.html',
		styleUrls: ['./kendo-search.component.scss']
})
export class CommonKendoSearchComponent implements OnInit, AfterViewInit {
	/**
	 * 그룹 시작선을 보여줄지 여부
	 */
	@Input() displayGroupStart: boolean = true;

	/**
	 * 검색 이벤트
	 */
	@Output() search: EventEmitter<CommonSearch> = new EventEmitter<CommonSearch>();

	/**
	 * 날짜검색 사용여부
	 */
	@Input() enablePeriodSearch: boolean = false;

	/**
	 * 날짜검색 시 시간 사용여부
	 */
	@Input()
	get displayTime(): boolean {
		return this._displayTime;
	}
	set displayTime(value: boolean) {
		this._displayTime = value;

		// 시간 표시를 하는 경우
		if (this._displayTime) {
			this.startDateFormat = 'yyyy-MM-dd HH:mm:ss';
			this.endDateFormat = 'yyyy-MM-dd HH:mm:ss';
		}
		// 시간 표시를 하지 않는 경우
		else {
			this.startDateFormat = 'yyyy-MM-dd';
			this.endDateFormat = 'yyyy-MM-dd';
		}
	}
	private _displayTime: boolean = false;

	/**
	 * 키워드검색 사용여부
	 */
	@Input() enableKeywordSearch: boolean = true;

	/**
	 * 입력 방지 여부
	 */
	@Input() disableInput: boolean = false;

	/**
	 * 검색필드 목록
	 */
	@Input()
	get fields(): { text: string, value: any }[] {
		return this._fields;
	}
	set fields(value: { text: string, value: any }[]) {
		this._fields = value;
		this.initKeywordSearch();
	}
	private _fields: { text: string, value: any }[] = [];

	/**
	 * 필터된 검색필드 목록
	 */
	items: ItemType[] = [];

	/**
	 * 선택된 검색 필드
	 */
	searchFields: ItemType[] = [];

	/**
	 * 검색 키워드
	 */
	searchKeyword: string = '';

	/**
	 * 시작일자
	 */
	get startDate(): Date {
		return this._startDate;
	}
	set startDate(value: Date) {
		this._startDate = value;
	}
	_startDate: Date = new Date();

	/**
	 * 시작일자 표시 포맷
	 */
	startDateFormat: string = 'yyyy-MM-dd';

	/**
	 * 종료일자
	 */
	@Input()
	get endDate(): Date {
		return this._endDate;
	}
	set endDate(value: Date) {
		this._endDate = value;
	}
	_endDate: Date = new Date();

	/**
	 * 종료일자 표시 포맷
	 */
	endDateFormat: string = 'yyyy-MM-dd';

	/**
	 * 종료일자 초기값
	 */
	initializedEndDate: Date = null;

	/**
	 * 선택가능한 마지막 날짜
	 */
	maxDate: Date;

	/**
	 * 시작일자와 종료일자의 차이
	 */
	@Input() days: number = 7;

	/**
	 * 검색 기간 필드 목록
	 */
	@Input() periodFields: { text: string, value: any }[];

	/**
	 * 검색 기간 시간 목록
	 */
	@Input() hoursList: number[] = [ ];

	/**
	 * 검색 기간 일수 목록
	 */
	@Input() daysList: number[] = [ 30, 60, 90 ];

	/**
	 * 비활성화 여부
	 */
	@Input() disabled: boolean = false;

	/**
	 * 검색 버튼 타이틀
	 */
	@Input() searchButtonTitle: string = '';

	/**
	 * 검색 버튼 아이콘 클래스
	 */
	@Input() searchButtonIconClass: string = 'fas fa-search';

	/**
	 * 새로고침 사용여부
	 */
	@Input() enableRefreshButton: boolean = true;

	/**
	 * 날짜 검색 팝업을 표시할지 여부
	 */
	showSearchPeriodPopup: boolean = false;
	/**
	 * 날짜 검색 필드 팝업 위치
	 */
	popupAlign: Align = { horizontal: 'left', vertical: 'top' };

	/**
	 * 선택된 날짜 검색 필드
	 */
	selectedPeriodField: string;

	/**
	 * 날짜 선택 팝업에서 선택하여 변경되었는지 여부
	 */
	private fromCalendar: boolean = false;

	/**
	 * 검색 필드 팝업
	 */
	@ViewChild('searchFieldsMultiSelect', { static: false }) searchFieldsMultiSelect: MultiSelectComponent;
	/**
	 * 검색 기간 팝업
	 */
	@ViewChild('searchPeriodPopup', { static: false }) searchPeriodPopup: PopupComponent;

	/**
	 * 생성자
	 * @param translate 번역 서비스 객체
	 */
	constructor(private translate: TranslateService,
	) {
	}

	/**
	 * 초기화 이벤트
	 */
	ngOnInit(): void {
		this.initializedEndDate = this.endDate;
		this.initPeriod();
	}

	/**
	 * 뷰 초기화 후 이벤트
	 */
	ngAfterViewInit(): void {
		// 컴포넌트 초기화 이후 검색 필터링 설정
		if (this.enableKeywordSearch) {
				const contains = value => s => s.text.toLowerCase().indexOf(value.toLowerCase()) !== -1;
				this.searchFieldsMultiSelect.filterChange.asObservable().pipe(
					switchMap(value => from([this.fields]).pipe(
						// tap(() => this.searchFieldsMultiSelect.loading = true),
						delay(100),
						map((data) => data.filter(contains(value)))
					))
				)
				.subscribe(x => {
					this.items = x;
					// this.searchFieldsMultiSelect.loading = false;
				});
		}

		if (this.enablePeriodSearch) {
				// 컴포넌트 초기화 이후 리스트를 가져오기 위해 검색 이벤트 호출
				this.doSearch();
		}
	}

	/**
	 * 날짜 검색에 필요한 프로퍼티 초기화
	 */
	private initPeriod(): void {

		if (!this.periodFields)
			this.periodFields = [];

		// 날짜검색을 사용할 경우 필드 초기화
		if (!this.enablePeriodSearch)
			return;

		const today = new Date();
		// this.startDate = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0);
		// this.startDate.setDate(today.getDate() - this.days);
		// this.modifyDaysFromDate(this.days);

		// 시간을 표시하지 않는 경우
		if (!this.displayTime) {
			// 검색 종료일을 초기화한 일자 혹은 오늘 날짜로 설정
			this.endDate = this.initializedEndDate == null ? new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0) : this.initializedEndDate;
			// 최대 설정 가능일을 오늘 날짜로 설정
			this.maxDate = new Date(this.endDate.getFullYear(), this.endDate.getMonth(), this.endDate.getDate());
		}
		// 시간을 표시하는 경우, 자정까지 설정을 하기 위해 설정
		else {
			// 초기화한 마지막 일자가 존재하지 않는 경우
			if (this.initializedEndDate == null) {
				// 검색 종료일을 초기화한 일자 혹은 내일 날짜로 설정
				this.endDate = moment(new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0)).add(1, 'days').toDate();
			}
			// 초기화한 마지막 일자가 존재하는 경우
			else {
				// 검색 종료일을 초기화한 일자로 설정
				this.endDate = this.initializedEndDate;
			}
			// 최대 설정 가능일을 내일 날짜로 설정
			this.maxDate = this.endDate;
		}

		if (this.daysList.length > 0)
		{
			this.days = this.daysList[0];
			this.startDate = this.endDate;
			this.modifyDaysFromDate(this.days);
		}

		if (this.periodFields) {
			// 모든 필드 정보에 대해서 처리
			for (const item of this.periodFields) {
				item.text = this.translate.instant(item.text);
			}
		}

		if (this.periodFields.length === 0) {
			this.periodFields.push({ text: 'UL_COMMON_SEARCH_PERIOD_FIELD_REG_DATE', value: 'RegDate' });
			this.selectedPeriodField = 'RegDate';
		} else {
			this.selectedPeriodField = this.periodFields[0].value;
		}
	}

	/**
	 * 키워드 검색에 필요한 프로퍼티 초기화
	 */
	private initKeywordSearch(): void {
		// 키워드검색을 사용할 경우 필드 초기화
		if (this.enableKeywordSearch) {
			this.items = new Array<any>();

			// 모든 필드 정보에 대해서 처리
			for (const item of this.fields) {
				item.text = this.translate.instant(item.text);
			}

			this.items = this.fields.slice();
		}
	}

	/**
	 * 주어진 시간으로 시작일시 수정
	 */
	modifyHoursFromDate(value: number): void {
		if (!this.startDate || !this.endDate)
			return;

		const tempEndDate = new Date(this.endDate.getFullYear(), this.endDate.getMonth(), this.endDate.getDate());

		tempEndDate.setTime(this.endDate.getTime() - (value * 60 * 60 * 1000));

		this.startDate = new Date(tempEndDate.getFullYear(), tempEndDate.getMonth(), tempEndDate.getDate()
			, tempEndDate.getHours(), tempEndDate.getMinutes(), tempEndDate.getSeconds());
	}

	/**
	 * 주어진 일수로 시작일시 수정
	 */
	modifyDaysFromDate(value: number): void {
		if (!this.startDate || !this.endDate)
			return;

		const tempEndDate = new Date(this.endDate.getFullYear(), this.endDate.getMonth(), this.endDate.getDate());

		tempEndDate.setDate(this.endDate.getDate() - value);

		this.startDate = new Date(tempEndDate.getFullYear(),
			tempEndDate.getMonth(), tempEndDate.getDate());
	}

	// // 시작일시가 종료일시보다 클 경우 종료일시의 -days 만큼 변경
	// dateChangeEvent(startDate: Date): void {
	// 	if (!startDate || !this.endDate)
	// 		return;
	//
	// 	if (startDate > this.endDate) {
	// 		this.startDate.setDate(this.endDate.getDate() - this.days);
	// 	}
	// }

	// // 달력 키보드 입력 방지
	// keydownEvent(e: any): void {
	// 	if (this.disableInput)
	// 		e.preventDefault();
	// }

	/**
	 * 검색필드에서 엔터키 입력시 검색 이벤트 호출
	 */
	searchKeywordEnter(e: any): void {
		if (e.keyCode === 13) {
				this.doSearch();
		}
	}

	/**
	 * 검색 이벤트 발생시 상위 컴포넌트로 파라미터 전달
	 */
	doSearch(): void {
		this.showSearchPeriodPopup = false;
		if (this.searchFieldsMultiSelect)
			this.searchFieldsMultiSelect.toggle(false);
		this.search.emit(this.getSearchCondition());
	}

	/**
	 * 검색 조건을 가져온다.
	 */
	getSearchCondition(): CommonSearch {
		// 선택한 검색필드에서 value 값 추출
		const selectedSearchFields: Array<string> = new Array<string>();
		this.searchFields.forEach((object) => selectedSearchFields.push(object.value));

		switch (this.selectedPeriodField) {
				default:
					return new CommonSearch(this.selectedPeriodField,
						this.enablePeriodSearch ? this.startDate : null,
						this.enablePeriodSearch ? (this.displayTime ? this.endDate : moment(this.endDate).add(1, 'days').toDate()) : null,
															selectedSearchFields, this.searchKeyword);
		}
	}

	/**
	 * 검색필드 데이터 초기화
	 */
	reset(): void {
		this.initPeriod();
		this.searchFields = [];
		this.searchKeyword = '';

		this.doSearch();
	}

	/**
	 * 날짜 검색 필드 축소시 팝업
	 */
	searchDatePopup(): void {
		this.showSearchPeriodPopup = !this.showSearchPeriodPopup;
	}

	/**
	 * 시작 일자/시간 변경 시 발생하는 이벤트
	 * @param date 시작 일자/시간
	 */
	onStartDateTimeChanged(date: Date): void {

		if (this.fromCalendar)
			this.onStartDateChanged(date);
		else
			this._startDate = date;

		this.fromCalendar = false;
	}

	/**
	 * 시작 일자 변경 시 발생하는 이벤트
	 * @param date 시작 일자
	 */
	onStartDateChanged(date: Date): void {

		this._startDate = moment(date).startOf('day')
			.add(this._startDate.getHours(), 'hours')
			.add(this._startDate.getMinutes(), 'minutes')
			.add(this._startDate.getSeconds(), 'seconds')
			.toDate();
	}

	/**
	 * 시작 시간 변경 시 발생하는 이벤트
	 * @param time 시작 시간
	 */
	onStartTimeChanged(time: Date): void {
		this._startDate = moment(this._startDate).startOf('day')
			.add(time.getHours(), 'hours')
			.add(time.getMinutes(), 'minutes')
			.add(time.getSeconds(), 'seconds')
			.toDate();
	}

	/**
	 * 종료 일자/시간 변경 시 발생하는 이벤트
	 * @param date 종료 일자/시간
	 */
	onEndDateTimeChanged(date: Date): void {

		if (this.fromCalendar)
			this.onEndDateChanged(date);
		else
			this._endDate = date;

		this.fromCalendar = false;
	}

	/**
	 * 종료 일자 변경 시 발생하는 이벤트
	 * @param date 종료 일자
	 */
	onEndDateChanged(date: Date): void {

		this._endDate = moment(date).startOf('day')
			.add(this._endDate.getHours(), 'hours')
			.add(this._endDate.getMinutes(), 'minutes')
			.add(this._endDate.getSeconds(), 'seconds')
			.toDate();
	}

	/**
	 * 종료 시간 변경 시 발생하는 이벤트
	 * @param time 종료 시간
	 */
	onEndTimeChanged(time: Date): void {
		this._endDate = moment(this._endDate).startOf('day')
			.add(time.getHours(), 'hours')
			.add(time.getMinutes(), 'minutes')
			.add(time.getSeconds(), 'seconds')
			.toDate();
	}

	/**
	 * 날짜 팝업 선택 시 발생하는 이벤트
	 */
	onCalendarSelect(_: any): void {
		this.fromCalendar = true;
	}

	/**
	 * 기간 일자 변경 클릭 시 이벤트
	 * @param event
	 */
	onDurationDayClicked(event: any): void {
		console.log(event);
	}

	/**
	 * 아이템이 선택되었는지 여부
	 * @param item 검사할 아이템 객체
	 */
	public isItemSelected(item: ItemType): boolean {
		return this.searchFields.some(i => i.value === item.value);
	}
}
