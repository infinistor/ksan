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
import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {List} from 'linq-collections';

type ItemType = { text: string, name: string, value: number, enable: boolean, isSelectItem: boolean, isAllItem: boolean };

@Component({
	selector: 'app-enum-dropdownlist',
	templateUrl: 'enum-dropdownlist.component.html',
	styleUrls: ['./enum-dropdownlist.component.scss'],
})
export class EnumDropDownListComponent implements OnInit {
	/**
	 * 적용할 클래스명
	 */
	@Input() class: string;
	/**
	 * 목록에 표시될 enum 데이터
	 */
	@Input() data: any;
	/**
	 * 데이터 목록
	 */
	public dataItem: ItemType[] = [];
	/**
	 * '선택' 텍스트를 표시할지 여부
	 */
	@Input() includeSelect: boolean = false;
	/**
	 * '전체' 텍스트를 표시할지 여부
	 */
	@Input() includeAll: boolean = true;
	/**
	 * 폼 컨트롤명
	 */
	@Input() formControlName: string = null;

	/**
	 * '선택' 텍스트
	 */
	@Input()
	set selectText(value: string) {

		this._selectText = value;

		// 선택 항목을 가져온다.
		const selectItem = new List<any>(this.dataItem).where(i => i.isSelectItem).firstOrDefault();
		// 선택 항목이 존재하는 경우
		if (selectItem) {
			selectItem.text = this.selectText;
		}
		// 선택 항목이 존재하지 않는 경우
		else {
			this.dataItem.push({text: this.selectText, name: null, value: null, enable: true, isSelectItem: true, isAllItem: false});
		}
	}
	get selectText(): string {
		return this._selectText;
	}
	private _selectText: string = 'UL_COMMON__SELECT';

	/**
	 * '전체' 텍스트
	 */
	@Input()
	set allText(value: string) {

		this._allText = value;

		// 전체 항목을 가져온다.
		const allItem = new List<any>(this.dataItem).where(i => i.isAllItem).firstOrDefault();
		// 전체 항목이 존재하는 경우
		if (allItem) {
			allItem.text = this._allText;
		}
		// 전체 항목이 존재하지 않는 경우
		else {
			this.dataItem.push({text: this._allText, name: null, value: null, enable: true, isSelectItem: false, isAllItem: true});
		}
	}
	get allText(): string {
		return this._allText;
	}
	private _allText: string = 'UL_COMMON__ALL';

	/**
	 * 선택된 값
	 */
	@Input() selectedValue: string = null;
	/**
	 * 노출할 값 목록
	 */
	@Input() filter?: (value: string) => boolean;
	/**
	 * 비활성화 여부
	 */
	@Input() disabled: boolean = false;

	/**
	 * 비활성화 아이템 값
	 */
	@Input()
	get disabledValues(): string[] {
		return this._disabledValues;
	}
	set disabledValues(values: string[]) {

		// 비활성화 목록 저장
		this._disabledValues = values;

		// 모든 데이터 목록에 대해서 처리
		this.dataItem.map((item) => {

			// 비활성화 목록에 존재하는 경우, 비활성화
			item.enable = this._disabledValues.findIndex((value) => item.name === value) < 0;
		});
	}
	private _disabledValues: string[] = [];

	/**
	 * 감춤 아이템 값
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
	@Output('onChange') changeEmitter: EventEmitter<any> = new EventEmitter<any>();

	/**
	 * 초기화 이벤트
	 */
	ngOnInit(): void {

		this.setData();
	}

	/**
	 * 사용자가 제공한 데이터로 데이터 목록을 생성한다.
	 */
	setData(): void {

		this.dataItem = [];

		// 선택 텍스트 포함인 경우
		if (this.includeSelect) {
			// 선택 항목이 존재하지 않는 경우
			if (!new List<any>(this.dataItem).where(i => i.isSelectItem).any()) {
				this.dataItem.push({text: this.selectText, name: null, value: null, enable: true, isSelectItem: true, isAllItem: false});
			}
		}
		// 전체 포함인 경우
		if (this.includeAll) {
			// 전체 항목이 존재하지 않는 경우
			if (!new List<any>(this.dataItem).where(i => i.isAllItem).any()) {
				this.dataItem.push({text: this.allText, name: null, value: null, enable: true, isSelectItem: false, isAllItem: true});
			}
		}

		// 데이터 값을 드롭다운 리스트에 맞도록 변경
		const data = this.data;

		// 데이터의 모든 속성에 대해서 처리
		for (const value in data) {

			// 소유하고 있는 프로퍼티인 경우
			if (data.hasOwnProperty(value)) {

				// 값 저장
				const enumValue = data[value];

				// 값이 문자열인 경우 (이름인 경우)
				if (typeof enumValue === 'string') {

					if (this.filter && !this.filter(enumValue)) {
						continue;
					}

					// 감춤 목록에 존재하는 경우, 스킵
					if (this.hiddenValues.findIndex((hiddenValue) => {
						return enumValue === hiddenValue;
					}) >= 0)
						continue;

					let enable = true;

					// 비활성화 목록에 존재하는 경우, 비활성화
					if (this.disabledValues.findIndex((disabledValue) => enumValue === disabledValue) >= 0) {
						enable = false;
					}

					if (data.hasOwnProperty('toDisplayShortName')) {
						this.dataItem.push({text: data.toDisplayShortName(enumValue), name: enumValue, value: data[enumValue], enable, isSelectItem: false, isAllItem: false});
					} else {
						this.dataItem.push({text: data[enumValue], name: enumValue, value: data[enumValue], enable, isSelectItem: false, isAllItem: false});
					}
				}
			}
		}
	}

	/**
	 * 값 변경 이벤트
	 * @param event 선택 값 객체
	 */
	public onChange(event: any): void {
		this.changeEmitter.emit(event);
	}

	/**
	 * 초기값으로 설정
	 */
	public setInit(): void {
		if (this.includeAll) {
			this.selectedValue = null;
		} else {
			this.selectedValue = this.dataItem[0].name;
		}
	}

	/**
	 * 비활성화 아이템인지 여부를 반환한다.
	 * @param itemArgs 비활성화할 아이템 정보 객체
	 */
	public getItemDisabled(itemArgs: { dataItem: any, index: number }): boolean {
		return !itemArgs.dataItem.enable;
	}
}
