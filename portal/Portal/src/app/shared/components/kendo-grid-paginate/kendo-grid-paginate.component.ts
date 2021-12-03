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
import {Component, DoCheck, ElementRef, EventEmitter, HostListener, Input, KeyValueDiffer, KeyValueDiffers, OnInit, Output, ViewChild, ViewEncapsulation } from '@angular/core';
import {Grid} from '../../classes/grid/grid.model';
import {CellClickEvent, DetailCollapseEvent, DetailExpandEvent, GridComponent, PageChangeEvent, RowArgs, SelectionEvent} from '@progress/kendo-angular-grid';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {GridColumnConfigService} from '../../services/grid-column-config.service';
import {SortDescriptor} from '@progress/kendo-data-query';
import {List} from 'linq-collections';
import {ColorColumn} from '../../classes/color-column';
import {MessageNotificationService} from '../../services/message-notification.service';
import {TooltipDirective} from '@progress/kendo-angular-tooltip';
import {ClipboardService} from 'ngx-clipboard';
import {TranslateService} from '@ngx-translate/core';
import {GridDataLoadedEvent} from '../../classes/grid/grid-data-loaded-event.model';
import {IGridComponent} from '../../classes/grid/grid-component-interface';
import {EnumResponseResult} from '../../models/enums/enum-response-result.model';
import {ResponseData} from '../../models/response-data.model';
import {ResponseList} from '../../models/response-list.model';

@Component({
	selector: 'app-common-kendo-grid-paginate',
	templateUrl: 'kendo-grid-paginate.component.html',
	styles: [
		`
			.k-cell-inner > .k-link {
				padding-left: 20px !important;
			}
			.k-cell-inner > .k-link > .k-icon.k-i-sort-desc-sm,
			.k-cell-inner > .k-link > .k-icon.k-i-sort-asc-sm {
				position: absolute !important;
				margin-left: -12px !important;
				margin-top: 2px !important;;
				left: 0;
				top: 50%;
				transform: translateY(-50%);
			}
			.k-grid-header .k-i-sort-asc-sm, .k-grid-header .k-i-sort-desc-sm, .k-grid-header .k-sort-order {
				position: absolute !important;
				margin-left: 0 !important;;
				margin-top: 1px !important;;
				left: 10px !important;;
				top: 50%;
				transform: translateY(-50%);
			}
			.k-grid-header .k-grid-filter, .k-grid-header .k-header-column-menu {
				width: calc( 0.75rem + 2px);
				padding-left: 0;
				padding-right: 0;
				right: 0.3rem;
				bottom: 0;
				height: 100%;
				vertical-align: middle;
			}
			/* 그리드 셀 링크 */
			.grid-cell-click {
				color: #337ab7;
				cursor: pointer;
			}
			.grid-cell-click:hover {
				text-decoration: underline;
			}
		`
	],
	encapsulation: ViewEncapsulation.None,
})
export class CommonKendoGridPaginateComponent implements OnInit, DoCheck, IGridComponent {

	/**
	 * 호스트 컴포넌트에서 받아올 그리드 객체
	 */
	@Input()
	get grid(): Grid {
		return this._grid;
	}
	set grid(value: Grid) {
		value.component = this;
		this._grid = value;
		this._grid.component = this;
	}

	/**
	 * 생성자
	 * @param httpService
	 */
	constructor(
		private httpClient: HttpClient,
		private keyValueDiffers: KeyValueDiffers,
		private messageService: MessageNotificationService,
		private gridColumnConfigService: GridColumnConfigService,
		private clipboardService: ClipboardService,
		private translateService: TranslateService,
	) {
		this.searchConditionDiffer = keyValueDiffers.find({}).create();
	}

	/**
	 * 검색 조건 변경 감지를 위한 값 저장 객체
	 */
	searchConditionDiffer: KeyValueDiffer<any, any>;
	private _grid: Grid = null;

	/**
	 * 로딩 중 여부
	 */
	@Input() isLoading: boolean = false;

	/**
	 * 비활성화 여부
	 */
	@Input() disabled: boolean = false;

	/**
	 * 상세 그리드 툴바 템플릿
	 */
	@Input() detailToolbarTemplate: ElementRef = null;

	/**
	 * 데이터가 없을 경우 메세지
	 */
	@Input() noDataMessage: string = 'UL_COMMON__NO_DATA';

	/**
	 * 데이터 로드 완료 이벤트 객체
	 */
	@Output('dataLoaded') dataLoadedEmitter: EventEmitter<GridDataLoadedEvent> = new EventEmitter<GridDataLoadedEvent>();

	/**
	 * 호스트 컴포넌트로 전달할 페이지 변경 이벤트 객체
	 */
	@Output('pageChange') pageChangeEmitter: EventEmitter<PageChangeEvent> = new EventEmitter<PageChangeEvent>();

	/**
	 * 선택 변경시 일어나는 이벤트
	 */
	@Output('selectionChange') selectionChangeEmiiter: EventEmitter<SelectionEvent> = new EventEmitter<SelectionEvent>();

	/**
	 * 선택 키 변경시 일어나는 이벤트
	 */
	@Output('selectedKeysChange') selectedKeysChangeEmiiter: EventEmitter<any[]> = new EventEmitter<any[]>();

	/**
	 * 셀 클릭 시 발생하는 이벤트
	 */
	@Output('cellClick') cellClickEmitter: EventEmitter<CellClickEvent> = new EventEmitter<CellClickEvent>();

	/**
	 * 정렬 변경 시 발생하는 이벤트
	 */
	@Output('sortChange') sortChangeEmitter: EventEmitter<SortDescriptor[]> = new EventEmitter<SortDescriptor[]>();

	/**
	 * 에러 시 발생하는 이벤트
	 */
	@Output('error') errorEmitter: EventEmitter<ResponseData> = new EventEmitter<ResponseData>();

	/**
	 * 체크 박스 컬럼 헤더의 모든 컬럼 선택이 변경 시 발생하는 이벤트
	 */
	@Output('checkColumnAllChanged') checkColumnAllChangedEmitter: EventEmitter<{field: string, checked: boolean}> = new EventEmitter<{field: string, checked: boolean}>();

	/**
	 * 체크 박스 컬럼 헤더의 컬럼 선택이 변경 시 발생하는 이벤트
	 */
	@Output('checkColumnChanged') checkColumnChangedEmitter: EventEmitter<{data: any, field: string, checked: boolean}> = new EventEmitter<{data: any, field: string, checked: boolean}>();

	/**
	 * 상세 그리드가 열리는 경우 발생하는 이벤트
	 */
	@Output('detailExpand') detailExpandEmitter: EventEmitter<any> = new EventEmitter<any>();

	/**
	 * 상세 그리드가 닫힐때 경우 발생하는 이벤트
	 */
	@Output('detailCollapse') detailCollapseEmitter: EventEmitter<any> = new EventEmitter<any>();

	/**
	 * 상세 데이터 로드 완료 이벤트 객체
	 */
	@Output('detailDataLoaded') detailDataLoadedEmitter: EventEmitter<GridDataLoadedEvent> = new EventEmitter<GridDataLoadedEvent>();

	/**
	 * 호스트 컴포넌트로 전달할 상세 페이지 변경 이벤트 객체
	 */
	@Output('detailPageChange') detailPageChangeEmitter: EventEmitter<PageChangeEvent> = new EventEmitter<PageChangeEvent>();

	/**
	 * 상세 선택 변경시 일어나는 이벤트
	 */
	@Output('detailSelectionChange') detailSelectionChangeEmiiter: EventEmitter<SelectionEvent> = new EventEmitter<SelectionEvent>();

	/**
	 * 상세 선택 키 변경시 일어나는 이벤트
	 */
	@Output('detailSelectedKeysChange') detailSelectedKeysChangeEmiiter: EventEmitter<any[]> = new EventEmitter<any[]>();

	/**
	 * 상세 셀 클릭 시 발생하는 이벤트
	 */
	@Output('detailCellClick') detailCellClickEmitter: EventEmitter<CellClickEvent> = new EventEmitter<CellClickEvent>();

	/**
	 * 상세 정렬 변경 시 발생하는 이벤트
	 */
	@Output('detailSortChange') detailSortChangeEmitter: EventEmitter<SortDescriptor[]> = new EventEmitter<SortDescriptor[]>();

	/**
	 * 상세 에러 시 발생하는 이벤트
	 */
	@Output('detailError') detailErrorEmitter: EventEmitter<ResponseData> = new EventEmitter<ResponseData>();

	/**
	 * 체크 박스 컬럼 헤더의 모든 컬럼 선택이 변경 시 발생하는 이벤트
	 */
	@Output('detailCheckColumnAllChanged') detailCheckColumnAllChangedEmitter: EventEmitter<{field: string, checked: boolean}> = new EventEmitter<{field: string, checked: boolean}>();

	/**
	 * 체크 박스 컬럼 헤더의 컬럼 선택이 변경 시 발생하는 이벤트
	 */
	@Output('detailCheckColumnChanged') detailCheckColumnChangedEmitter: EventEmitter<{data: any, field: string, checked: boolean}> = new EventEmitter<{data: any, field: string, checked: boolean}>();

	private loadedConfig: boolean = false;

	colorColumn: ColorColumn = ColorColumn.create();
	displayMasterColor = false;
	displayDetailColor = false;

	@ViewChild('commonGrid', { static : false }) private commonGrid: GridComponent;

	/**
	 * 툴팁 객체
	 */
	@ViewChild(TooltipDirective, { static: false } ) tooltipDir: TooltipDirective;

	/**
	 * ngOnInit
	 */
	ngOnInit(): void {

		this.setGridHeight();
	}

	/**
	 * 객체의 변경 여부를 감시한다
	 */
	ngDoCheck(): void {
		if (this.searchConditionDiffer) {
			// 검색 오브젝트의 변화를 감지
			const searchChanges = this.searchConditionDiffer.diff(this.grid.commonSearch);

			if (searchChanges) this.applySearchChange();
		}
	}

	/**
	 * 검색 기능을 초기화한다
	 */
	applySearchChange(): void {
		this.setGridBase();
		this.grid.searchKeyword = this.grid.commonSearch.searchKeyword;
		this.grid.searchFields = this.grid.commonSearch.searchFields;
		this.loadData();
	}

	/**
	 * 그리드 기본정보를 세팅한다
	 */
	setGridBase(): void {
		this.grid.gridView.data = [];
		this.grid.gridView.total = 0;
	}

	/**
	 * 최초 그리드 사이즈를 설정한다
	 */
	setGridHeight(): void {
		this.grid.height = this.grid.isUseFixedHeight ? this.grid.height : window.innerHeight - 160 - this.grid.flexingSize;
	}

	/**
	 * 브라우저 사이즈 변경시 호출되는 이벤트
	 * @param event
	 */
	@HostListener('window:resize', ['$event'])
	onResize(event: any): void {
		this.setGridHeight();
	}

	/**
	 * 데이터를 로드한다
	 */
	loadData(): void {
		// API 를 제공하는경우에만 사용한다
		if (this.grid.baseApi) {
			this.isLoading = this.grid.isUseLoading;
			this.loadConfig();
			this.getList<any>(this.grid.baseApi, this.grid.skip, this.grid.pageSize, this.grid.searchFields, this.grid.searchKeyword);
		} else this.bindListNoneApi();

		if (this.commonGrid) {
			for (let index = 0; index < this.grid.items.length; index++)
				this.commonGrid.collapseRow(index);
		}
	}

	/**
	 * 설정을 로드한다.
	 */
	private loadConfig(): void {
		if (!this.grid.location || !this.grid.name || this.loadedConfig) {
			return;
		}

		this.loadedConfig = true;
		const config = this.gridColumnConfigService.getConfigByGrid(this.grid);
		this.grid.applyColumnsConfig(config);
	}

	/**
	 * 페이지 변경시 발생하는 이벤트
	 * @param event
	 */
	pageChange(event: PageChangeEvent): void {
		this.grid.skip = event.skip;
		this.loadData();
		this.pageChangeEmitter.emit(event);
	}

	/**
	 * 셀 클릭 시 발생하는 이벤트
	 * @param event
	 */
	cellClick(event: CellClickEvent): void {
		if (event.type !== 'click') {
			return;
		}

		// 더블 클릭인 경우
		if (event.originalEvent.detail === 2) {
			// 해당 컬럼의 값이 존재하는 경우
			if (event.dataItem[event.column.field] && event.dataItem[event.column.field].toString().trim().length > 0) {
				this.clipboardService.copyFromContent(event.dataItem[event.column.field]);
				this.messageService.info(this.translateService.instant('SM_COMMON__COPIED_TO_CLIPBOARD'), 1);
			}
		}

		this.cellClickEmitter.emit(event);
	}

	/**
	 * 리스트 API 요청을 한다
	 * @param apiUrl
	 * @param skip
	 * @param countPerPage
	 * @param searchFields
	 * @param searchKeyword
	 */
	getList<T>(apiUrl: string, skip: number, countPerPage: number, searchFields: Array<string> = [], searchKeyword = ''): void {
		const param = {
			skip,
			countPerPage,
			searchFields,
			searchKeyword,
			orderFields: new List(this.grid.sortColumns).select(i => i.field).toArray(),
			orderDirections: new List(this.grid.sortColumns).select(i => i.dir).toArray(),
			...this.grid.commonSearchOptions,
		};

		this.httpClient.get<ResponseList<T>>(apiUrl, { params: param }).subscribe((response: any) => {
				if (response.Result === EnumResponseResult.Error) {
					this.errorEmitter.emit(response);
				}
				this.bindList(skip, response);
			},
			(err: HttpErrorResponse) => {
				this.messageService.error(err.message);

				// err 발생시 grid 의 아이템을 초기화 한다
				this.grid.diffItems = [];
				this.grid.diffTotal = 0;
			},
			() => {
				// 통신중 플레그 비활성화
				this.isLoading = false;
			}
		);
	}

	/**
	 * 데이터를 바인딩한다
	 * @param currSkip
	 * @param data
	 */
	bindList<T>(currSkip: number, response: ResponseList<T>): void {
		this.updateGridView(response.Data.Items, response.Data.TotalCount);
		this.grid.doManual();
	}

	/**
	 * API 요청이 아닐때
	 */
	bindListNoneApi(): void {
		this.updateGridView(this.grid.items.slice(this.grid.skip, this.grid.skip + this.grid.pageSize), this.grid.items.length);
	}

	/**
	 * 그리드 뷰를 업데이트 한다.
	 * @param data 데이터
	 * @param total 전체 개수
	 */
	updateGridView(data: any[], total: number = 0): void {
		this.grid.items = data;
		this.grid.gridView = { data, total, grid: this.grid };
		this.displayMasterColor = this.colorColumn.hasColor(data);
		this.displayDetailColor = this.colorColumn.hasItemColor(data);
		this.dataLoadedEmitter.emit(new GridDataLoadedEvent(this.grid, data));
	}

	/**
	 * 로드된 데이터를 초기화 한다.
	 */
	clear(): any {
		this.updateGridView([], 0);
	}

	/**
	 * 셀 셀렉트시 반응하는 이벤트
	 * @param event
	 */
	selectionChange(event: SelectionEvent): void {
		this.selectionChangeEmiiter.emit(event);
	}

	/**
	 * 체크박스 셀릭트시 발생하는 이벤트
	 * @param e
	 */
	onSelectedKeysChange(e: any): void {
		// 선택된 항목 저장
		this.grid.selectedItems = this.grid.items.filter(item => e.includes(this.GetKeyValue(item)));
		this.selectedKeysChangeEmiiter.emit(e);
	}

	/**
	 * 키 선택 변경 시 발생하는 이벤트
	 * @param context 행 정보 객체
	 */
	getSelectionKey(context: RowArgs): string {

		// 이 컴포넌트 객체를 가져온다.
		const $this: any = ((this) as any).grid.data.grid.component;

		// 컴포넌트 객체가 유효한 경우
		if ($this)
			// 해당 행 데이터의 키 값을 가져온다.
			return $this.GetKeyValue(context.dataItem);
		else
			return '';
	}

	/**
	 * 행 데이터에서 키 값을 가져온다.
	 * @param rowData 행 데이터
	 */
	GetKeyValue(rowData: any): string {
		let keyValue = '';

		if (rowData !== null) {
			for (let index = 0; index < this.grid.keyColumnNames.length; index++) {
				const value = rowData[this.grid.keyColumnNames[index]];
				keyValue = keyValue ? keyValue + ',' + value : value;
			}
		}

		return keyValue;
	}

	hiddenChanged(e: any): void {
		this.grid.setDisplayColumn(e.field, e.isHidden);
		this.gridColumnConfigService.setConfigByGrid(this.grid);
	}

	/**
	 * 정렬 변경 시 발생하는 이벤트
	 * @param sorts 소트정보
	 */
	sortChange(sorts: SortDescriptor[]): void {
		this.grid.setSortColumns(sorts.filter((item: any) => item.dir));
		this.loadData();
		this.sortChangeEmitter.emit(sorts);
	}

	/**
	 * 상세 창이 열릴 때 발생하는 이벤트
	 * @param event
	 */
	detailExpand(event: DetailExpandEvent): void {

		// 여러 상세 그리드를 표시하지 않는 경우
		if (this.grid.useMultipleDetailGrid === false && this.commonGrid) {
			// 모든 데이터에 대해서
			for (let index = 0; index < (this.commonGrid.data as any).total; index++) {
				// 이번에 열리는 인덱스가 아닌 경우, 상세 그리드를 닫는다.
				if (index !== event.index)
					this.commonGrid.collapseRow(index);
			}
		}

		this.grid.detailGrid.clearSelection();

		this.detailExpandEmitter.emit(event.dataItem);
	}

	/**
	 * 행의 상세 그리드를 축소한다.
	 * @param index
	 */
	collapseRow(index: number): void {
		if (this.commonGrid) {
			if (index >= 0 && index < this.grid.items.length)
				this.commonGrid.collapseRow(index);
		}
	}

	/**
	 * 행의 상세 그리드를 확장한다.
	 * @param index
	 */
	expandRow(index: number): void {
		if (index >= 0 && index < this.grid.items.length)
			this.commonGrid.expandRow(index);
	}

	/**
	 * 상세 창이 열릴 때 발생하는 이벤트
	 * @param event
	 */
	detailCollapse(event: DetailCollapseEvent): void {
		this.detailCollapseEmitter.emit(event.dataItem);
	}

	// 상세 페이지 변경 시 발생하는 이벤트
	onDetailPageChange(event: PageChangeEvent): void {
		this.detailPageChangeEmitter.emit(event);
	}

	// 상세 데이터 로드 후 발생하는 이벤트
	onDetailDataLoaded(event: GridDataLoadedEvent): void {
		this.detailDataLoadedEmitter.emit(event);
	}

	// 상세 선택 변경 시 발생하는 이벤트
	onDetailSelectionChange(event: any): void {
		this.detailSelectionChangeEmiiter.emit(event);
	}

	// 상세 선택 키 변경 시 발생하는 이벤트
	onDetailSelectedKeysChange(event: any): void {
		this.detailSelectedKeysChangeEmiiter.emit(event);
	}

	// 상세 셀 클릭 시 발생하는 이벤트
	onDetailCellClick(event: any): void {
		this.detailCellClickEmitter.emit(event);
	}

	// 상세 셀 소팅 시 발생하는 이벤트
	onDetailSortChange(event: any): void {
		this.detailSortChangeEmitter.emit(event);
	}

	// 상세 셀 소팅 시 발생하는 이벤트
	onDetailError(event: any): void {
		this.detailErrorEmitter.emit(event);
	}

	// 전체 선택 체크 박스 변경 시 발생하는 이벤트
	onDetailCheckColumnAllChanged(event: any): void {
		this.detailCheckColumnAllChangedEmitter.emit(event);
	}

	// 체크 박스 변경 시 발생하는 이벤트
	onDetailCheckColumnChanged(event: any): void {
		this.detailCheckColumnChangedEmitter.emit(event);
	}

	/**
	 * 체크 박스 컬럼 헤더의 모든 컬럼 선택이 변경 시 발생하는 이벤트
	 * @param field 필드명
	 * @param event 선택 여부
	 */
	onChangeColumnCheckAll(field: string, event: any): void {
		this.checkColumnAllChangedEmitter.emit({ field, checked: event});
	}

	/**
	 * 체크 박스 컬럼의 체크 변경 시 발생하는 이벤트
	 * @param item 아이템 객체
	 * @param field 필드명
	 * @param event 선택 여부
	 */
	onChangeColumnCheck(item: any, field: string, event: any): void {
		item[field] = event;
		this.checkColumnChangedEmitter.emit({ data: item, field, checked: event});
	}

	/**
	 * 툴팁 처리
	 * @param e 마우스 이벤트 객체
	 */
	showTooltip(e: MouseEvent): void {
		const element = e.target as HTMLElement;
		if (element.innerText) {
			let showTooltip: boolean = false;

			if (element.nodeName === 'TH' || (element.parentElement && element.parentElement.nodeName === 'TH')) {
				if (element.clientWidth < element.scrollWidth) {
					showTooltip = true;
				}
			}
			else if (element.nodeName === 'TD' || (element.parentElement && element.parentElement.nodeName === 'TD')) {
				if (element.parentElement.clientWidth < element.parentElement.scrollWidth) {
					showTooltip = true;
				}
			}

			if (showTooltip) {
				this.tooltipDir.toggle(element);
			}
			else {
				this.tooltipDir.hide();
			}
		}
	}

	/**
	 * 특정 인덱스의 데이터를 선택한다.
	 * @param index 선택할 인덱스
	 */
	selectByIndex(index: number): any {

		const deselectedRows: RowArgs[] = [];

		// 선택해제 되는 아이템 목록
		if (this.grid.selectedItems) {
			this.grid.selectedItems.map((item, idx) => {
				deselectedRows.push({dataItem: item, index: idx});
			});
		}

		// 데이터가 유효하고, 인덱스가 유효한 값인 경우
		if (this.grid.items && index >= 0 && index < this.grid.items.length) {
			const selectedItem = this.grid.items[index];
			const selctedKeyValue: string = this.GetKeyValue(selectedItem);

			// 키 값이 유효한 경우
			if (selctedKeyValue) {
				this.grid.selectedItems = [selectedItem];
				this.grid.selectedKeys = [selctedKeyValue];
				const selectedRows: RowArgs[] = [];
				// 선택 되는 아이템 목록
				if (this.grid.selectedItems) {
					this.grid.selectedItems.map((item, idx) => {
						selectedRows.push({dataItem: item, index: idx});
					});
				}
				this.selectionChangeEmiiter.emit({ selectedRows, deselectedRows, ctrlKey: false, shiftKey: false});
				this.selectedKeysChangeEmiiter.emit(this.grid.selectedKeys);

				return selectedItem;
			}
			else {
				return null;
			}
		}
		// 데이터 혹은 인덱스가 유효하지 않은 경우
		else {
			return null;
		}
	}

	/**
	 * 해당 인덱스 목록의 데이터를 선택한다.
	 * @param indexes 선택할 인덱스 목록
	 */
	selectByIndexes(indexes: number[]): any[] {
		const deselectedRows: RowArgs[] = [];

		// 선택해제 되는 아이템 목록
		if (this.grid.selectedItems) {
			this.grid.selectedItems.map((item, idx) => {
				deselectedRows.push({dataItem: item, index: idx});
			});
		}

		// 데이터가 유효하고, 인덱스가 유효한 값인 경우
		if (this.grid.items && indexes && indexes.length > 0) {
			const selectedItems: any[] = [];
			const selctedKeyValues: string[] = [];

			indexes.map((index) => {
				if (index >= 0 && index < this.grid.items.length) {
					const selectedItem = this.grid.items[index];
					const selctedKeyValue: string = this.GetKeyValue(selectedItem);

					selectedItems.push(selectedItem);
					selctedKeyValues.push(selctedKeyValue);
				}
			});

			// 키 값이 유효한 경우
			if (selctedKeyValues.length > 0) {
				this.grid.selectedItems = selectedItems;
				this.grid.selectedKeys = selctedKeyValues;
				const selectedRows: RowArgs[] = [];
				// 선택 되는 아이템 목록
				if (this.grid.selectedItems) {
					this.grid.selectedItems.map((item, idx) => {
						selectedRows.push({dataItem: item, index: idx});
					});
				}
				this.selectionChangeEmiiter.emit({selectedRows, deselectedRows, ctrlKey: false, shiftKey: false});
				this.selectedKeysChangeEmiiter.emit(this.grid.selectedKeys);

				return selectedItems;
			}
			else {
				return null;
			}
		}
		// 데이터 혹은 인덱스가 유효하지 않은 경우
		else {
			return null;
		}
	}

	/**
	 * 주어진 데이터 동일한 키를 가지는 데이터를 선택한다.
	 * @param item 선택할 데이터 객체
	 */
	selectByItem(item: any): any {
		// 데이터가 유효한 경우
		if (this.grid.items && item) {

			const selctedKeyValue: string = this.GetKeyValue(item);

			// 키 값이 유효한 경우
			if (selctedKeyValue) {

				// 해당 데이터의 인덱스
				const index: number = this.grid.items.findIndex((value) => selctedKeyValue === this.GetKeyValue(value));

				// 해당 인덱스의 데이터를 선택한다.
				return this.selectByIndex(index);
			}
			// 키 값이 유효하지 않은 경우
			else {
				return null;
			}
		}
		// 데이터가 유효하지 않은 경우
		else {
			return null;
		}
	}

	/**
	 * 주어진 데이터 동일한 키를 가지는 데이터를 선택한다.
	 * @param items 선택할 데이터 객체 목록
	 */
	selectByItems(items: any[]): any[] {
		// 데이터가 유효한 경우
		if (this.grid.items && items && items.length > 0) {

			const selectedKeyValues: string[] = [];

			items.map((item) => {
				const selectedKeyValue: string = this.GetKeyValue(item);

				// 키 값이 유효한 경우
				if (selectedKeyValue)
					selectedKeyValues.push(selectedKeyValue);
			});

			// 키 값이 유효한 경우
			if (selectedKeyValues.length > 0) {

				const selectedIndexes: number[] = [];

				// 모든 키 값 목록에 대해서 처리
				selectedKeyValues.map((selectedKeyValue) => {
					// 해당 데이터의 인덱스
					const index: number = this.grid.items.findIndex((value) => selectedKeyValue === this.GetKeyValue(value));
					if (index >= 0)
						selectedIndexes.push(index);
				});

				// 해당 인덱스의 데이터를 선택한다.
				return this.selectByIndexes(selectedIndexes);
			}
			// 키 값이 유효하지 않은 경우
			else {
				return null;
			}
		}
		// 데이터가 유효하지 않은 경우
		else {
			return null;
		}
	}

	/**
	 * 모든 선택을 해제 한다.
	 */
	clearSelection(): any {
		this.grid.selectedKeys = [];
		this.grid.selectedItems = [];
	}
}
