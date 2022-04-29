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
import {delay, map, switchMap} from 'rxjs/operators';
import {from} from 'rxjs';
import {MultiSelectComponent} from '@progress/kendo-angular-dropdowns';
import {TranslateService} from '@ngx-translate/core';
import {List} from 'linq-collections';
import {EnumItem} from '../../extensions/enum.extension';

@Component({
	selector: 'app-enum-multi-select',
	templateUrl: 'enum-multi-select.component.html',
	styleUrls: ['enum-multi-select.component.scss'],
})
export class EnumMultiSelectComponent implements OnInit, AfterViewInit {

	/**
	 * 적용할 스타일 클래스
	 */
	@Input() class: string;

	/**
	 * 데이터 목록
	 */
	@Input() data: EnumItem[];
	/**
	 * 필터된 데이터 목록
	 */
	public filteredData: EnumItem[] = [];

	/**
	 *
	 * @param value
	 */
	@Input() placeholder: string = 'UL_COMMON__ALL';

	/**
	 * 초기 선택 값 목록
	 */
	@Input() selectedValues: string[] = [];

	/**
	 * 비활성화 여부
	 */
	@Input() disabled: boolean = false;

	/**
	 * 비활성화 항목 값 목록
	 */
	@Input()
	get disabledValues(): string[] {
		return this._disabledValues;
	}
	set disabledValues(values: string[]) {

		this._disabledValues = values;

		this.setData();
	}
	private _disabledValues: string[] = [];

	/**
	 * 감춤 항목 값 목록
	 */
	@Input()
	get hiddenValues(): string[] {
		return this._hiddenValues;
	}
	set hiddenValues(values: string[]) {

		this._hiddenValues = values;

		this.setData();
	}
	private _hiddenValues: string[] = [];

	/**
	 * 선택 변경 이벤트
	 */
	@Output('onChange') onChangeEmitter: EventEmitter<any[]> = new EventEmitter<any[]>();

	/**
	 * 다중 선택 컴포넌트
	 */
	@ViewChild('searchMultiSelect', { static: false }) searchMultiSelect: MultiSelectComponent;

	/**
	 * 생성자
	 * @param translateService 번역 서비스 객체
	 */
	constructor(
		private translateService: TranslateService,
	) {
	}

	/**
	 * 초기화 이벤트
	 */
	ngOnInit(): void {

		// 사용자가 제공한 데이터로 데이터 목록을 생성한다.
		this.setData();

	}

	/**
	 * 뷰 초기화 후 이벤트
	 */
	ngAfterViewInit(): void {
		// 주어진 문자열을 포함하고 있는지 여부를 검사
		const contains = value => s => s.text.toLowerCase().indexOf(value.toLowerCase()) !== -1;
		// 필터링 문자열이 변경될 때 발생하는 이벤트
		this.searchMultiSelect.filterChange.asObservable().pipe(
			switchMap(value => from([this.data]).pipe(
				// tap(() => this.searchMultiSelect.loading = true),
				delay(100),
				map((data) => data.filter(contains(value)))
			))
		)
			.subscribe(x => {
				this.filteredData = x;
				this.searchMultiSelect.loading = false;
			});
	}

	/**
	 * 사용자가 제공한 데이터로 데이터 목록을 생성한다.
	 */
	setData(): void {

		// 정수 값으로 데이터 정렬
		this.data = new List<EnumItem>(this.data).orderBy(i => i.value).toArray();

		// 모든 목록을 필터된 목록에 저장
		this.data.forEach(val => this.filteredData.push(Object.assign({}, val)));

		// 데이터 목록에 포함된 데이터만 선택된 데이터로 남도록 수정
		this.selectedValues = new List<string>(this.selectedValues).where(i => this.data.findIndex((value) => value.name === i) >= 0).toArray();
	}

	/**
	 * 값 변경 이벤트
	 */
	public onChange(event: any): void {
		this.searchMultiSelect.clearFilter();
		this.onChangeEmitter.emit(event);
	}

	/**
	 * 비활성화 아이템인지 여부를 반환한다.
	 * @param itemArgs 비활성화 아이템인지 여부를 검색할 항목
	 */
	public getItemDisabled(itemArgs: { dataItem: any, index: number }): boolean {
		return !itemArgs.dataItem.enable;
	}

	/**
	 * 아이템이 선택되었는지 여부
	 * @param item 검사할 아이템 객체
	 */
	public isItemSelected(item: EnumItem): boolean {
		return this.selectedValues.some(i => i === item.name);
	}
}
