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
import {
	Component,
	Input,
	OnInit,
	HostListener,
	Output,
	EventEmitter,
	ViewChild,
	KeyValueDiffer,
	KeyValueDiffers,
	ViewEncapsulation,
	ContentChild,
	TemplateRef, DoCheck,
} from '@angular/core';
import { Grid } from '../../classes/grid/grid.model';
import {
		RowArgs,
		PageChangeEvent,
		GridComponent,
		SelectionEvent,
		CellClickEvent,
} from '@progress/kendo-angular-grid';
import {HttpErrorResponse, HttpClient} from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import {tap, map, catchError} from 'rxjs/operators';
import { GridColumnConfigService } from '../../services/grid-column-config.service';
import 'rxjs/add/observable/of';
import 'rxjs/add/operator/map';
import { ResizedEvent } from 'angular-resize-event';
import { SortDescriptor } from '@progress/kendo-data-query';
import { List } from 'linq-collections';
import { ColorColumn } from '../../classes/color-column';
import {MessageNotificationService} from '../../services/message-notification.service';
import { TooltipDirective } from '@progress/kendo-angular-tooltip';
import { ClipboardService } from 'ngx-clipboard';
import {TranslateService} from '@ngx-translate/core';
import {GridDataLoadedEvent} from '../../classes/grid/grid-data-loaded-event.model';
import {IGridComponent} from '../../classes/grid/grid-component-interface';
import { EnumResponseResult } from '../../models/enums/enum-response-result.model';
import { ResponseData } from '../../models/response-data.model';
import { ResponseList } from '../../models/response-list.model';

@Component({
	selector: 'app-common-kendo-grid-virtual',
	templateUrl: 'kendo-grid-virtual.component.html',
	styles: [
		`
			kendo-grid-column-menu {
				vertical-align: middle;
			}
			.k-grid .k-grid-content td {
				white-space: nowrap;
				overflow: hidden;
				text-overflow: ellipsis;
			}

			.k-grid-header th > .k-link {
				padding-left: 20px !important;;
			}
			.k-grid-header th > .k-link > .k-icon {
				position: absolute !important;
				margin-left: -2px !important;
				margin-top: 2px !important;;
				left: 0;
				top: 50%;
				transform: translateY(-50%);
			}
			.k-grid-header th > .k-link > .k-sort-order {
				position: absolute !important;
				margin-left: 0 !important;;
				margin-top: 1px !important;;
				left: 10px !important;;
				top: 50%;
				transform: translateY(-50%);
			}
			.k-grid-header .k-header {
				position: relative;
				vertical-align: middle;
				cursor: default;
				white-space: normal;
			}
			.k-grid-header .k-with-icon, .k-grid-header .k-filterable {
				padding-right: calc( calc( 0.75rem + 2px) + 0.5rem);
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
export class CommonKendoGridVirtualComponent implements OnInit, DoCheck, IGridComponent {
	@Input()
	get grid(): Grid {
		return this._grid;
	}
	set grid(value: Grid) {
		value.component = this;
		this._grid = value;
		this._grid.component = this;
	}

	constructor(
		private httpClient: HttpClient,
		private keyValueDiffers: KeyValueDiffers,
		private messageService: MessageNotificationService,
		private gridColumnConfigService: GridColumnConfigService,
		private clipboardService: ClipboardService,
		private translateService: TranslateService,
	) {
			this.KeyValueDiffer = keyValueDiffers.find({}).create();
	}
	private _grid: Grid = null;

	@Input() isLoading: boolean = false;
	@Input() disabled: boolean = false;
	@Input() heightOffset: number = 0;

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

	private loadedConfig: boolean = false;

	public KeyValueDiffer: KeyValueDiffer<any, any>;

	private isLoadedFirst = false;
	private firstGridResizeIsEmitted = true;
	public isCellRendering = false;

	// Http 요청에 대한 구독 배열
	private subscriptions = new Array();

	@ViewChild('commonVirtualGrid', { static: false })
	private commonVirtualGrid: GridComponent;

	@ContentChild('cellTemplate', { static: false })
	public cellTemplate: TemplateRef<any>;

	colorColumn: ColorColumn = ColorColumn.create();
	displayMasterColor = false;
	displayDetailColor = false;

	/**
	 * 툴팁 객체
	 */
	@ViewChild(TooltipDirective, { static: false } ) public tooltipDir: TooltipDirective;

	ngOnInit(): void {
		this.setGridHeight();
	}

	onGridResized(event: ResizedEvent): void {

		if (event.newHeight === 0) {
			// console.log('의미 없는 리사이즈 버림');
			return;
		}

		this.changePageSize(event.newHeight);

		if (event.oldHeight === 0) {
			// console.log('첫 리사이즈 이벤트 발생함');
			this.firstGridResizeIsEmitted = true;
		}

		if (!this.isLoadedFirst) {
			// console.log('첫로딩 전에 리사이즈 이벤트 발생하면 화면 업데이트를 하지 않고 로딩이 끝난 후 업데이트 할 것임');
			return;
		}
		// console.log('resize 이벤트에서 update');
		this.update();
	}

	changePageSize(gridHeight: number): void {
		const rowHeight = this.commonVirtualGrid.rowHeight;
		const headerHeight = 34;
		this.grid.pageSize = this.calculatePageSize(gridHeight, rowHeight, headerHeight);
	}

	calculatePageSize(gridHeight: number, rowHeight: number, headerHeight: number, multipleNumberOfNextPrevious: number = 2): number {
		const rowsHeightWithoutHeader = gridHeight - headerHeight;
		const rowCount = Math.ceil(rowsHeightWithoutHeader / rowHeight);

		// 페이지 사이즈 조정을 사용할경우
		if (this.grid.isUseCalculatePageSize) 
			return rowCount * multipleNumberOfNextPrevious;
		// 페이지 사이즈 조정을 사용하지 경우
		else
			return this.grid.pageSize;
	}

	/**
	 * 객체의 변경 여부를 감시한다
	 */
	public ngDoCheck(): void {
		if (this.keyValueDiffers) {
			// 검색 오브젝트의 변화를 감지
			const searchChanges = this.KeyValueDiffer.diff(this.grid.commonSearch);

			if (searchChanges) {
				this.applySearchChange();
			}
		}
	}

	/**
	 * 검색 기능을 초기화한다
	 */
	public applySearchChange(): void {
		this.setGridBase();
		this.grid.searchKeyword = this.grid.commonSearch.searchKeyword;
		this.grid.searchFields = this.grid.commonSearch.searchFields;
		this.loadData();
	}

	/**
	 * 그리드 기본정보를 세팅한다
	 */
	public setGridBase(): void {
		this.grid.skip = 0;
		this.grid.gridView.data = [];
		this.grid.gridView.total = 0;
	}

	public setGridHeight(): void {
		this.grid.height = this.grid.isUseFixedHeight
			? this.grid.height
			: window.innerHeight - 190 - this.grid.flexingSize + this.heightOffset;
	}

	/**
	 * 브라우저 사이즈 변경시 호출되는 이벤트
	 */
	@HostListener('window:resize')
	public onWindowResize(): void {
		this.setGridHeight();
	}

	/**
	 * 데이터를 로드한다.
	 */
	public loadData(): void {

		// API 를 제공하는경우에만 사용한다
		if (this.grid.baseApi) {
			this.isLoading = this.grid.isUseLoading;
			this.loadConfig();
			this.firstItems();
		} else {
			this.bindListNoneApi();
		}
	}

	/**
	 * 설정을 로드한다.
	 */
	private loadConfig(): void {
		// console.log('load config', this.grid.location, this.grid.name, this.loadedConfig);
		if (!this.grid.location || !this.grid.name || this.loadedConfig) {
			return;
		}

		this.loadedConfig = true;
		const config = this.gridColumnConfigService.getConfigByGrid(this.grid);
		this.grid.applyColumnsConfig(config);
	}

	private firstItems(): void {
		this.grid.skip = 0;

		this.getList().subscribe(
			response => {
				// 스크롤 길이를 위해 전체 Data 길이를 맞춤
				this.grid.items = [...response.Data.Items, ...new Array(response.Data.TotalCount - response.Data.Items.length).fill(null)];

				if (this.grid.items.length > 0) {
					// api 조회 후 데이터가 있으면 메모리에 저장
					this.setGridItems(response, this.grid.skip);
					this.firstUpdate();
				}
				this.isLoading = false;
				this.isLoadedFirst = true;

				this.dataLoadedEmitter.emit(new GridDataLoadedEvent(this.grid, response.Data.Items));
			},
			(err: HttpErrorResponse) => {
				this.messageService.error(err.message);
				this.isLoading = false;
			}
		);
	}

	private firstUpdate(): void {
		// console.log('첫 로딩 후 업데이트');
		// resize 첫 resize 가 일어나지 않았으면 로딩 후 chagePageSize 와 update가 실행됨.
		if (!this.firstGridResizeIsEmitted) {
			this.firstGridResizeIsEmitted = false;
			// console.log('grid resize 이벤트가 발생되서 update 실행될 것이므로 update 호출하지 않고 리턴');
			return;
		}

		this.update();
	}

	private takeItems(): Observable<any[]> {
		const skip = this.grid.skip;

		this.subscriptions.forEach(item => {
			if (!item.closed) item.unsubscribe();
		});
		this.subscriptions = [];

		const currentView = this.getCurrentViewIfHasNotNull();
		if (currentView) {
			return of(currentView);
		}

		return this.getList().pipe(
			tap((response: ResponseList<any>) => {
				this.setGridItems(response, skip);
				this.dataLoadedEmitter.emit(new GridDataLoadedEvent(this.grid, response.Data.Items));
			}),
			map(response => response.Data.Items)
		);
	}

	private setGridItems(response: ResponseList<any>, skip: number): void {
		let left: any[] = [];
		let right: any[] = [];

		left = this.grid.items.slice(0, skip);
		right = this.grid.items.slice(skip + response.Data.Items.length, response.Data.TotalCount);
		this.grid.items = [...left, ...response.Data.Items, ...right];
	}

	private getList(): Observable<ResponseList<any>> {
		const param = {
			skip: this.grid.skip,
			countPerPage: this.grid.pageSize,
			searchFields: typeof this.grid.searchFields !== 'undefined' ? this.grid.searchFields : [],
			searchKeyword: typeof this.grid.searchKeyword !== 'undefined' ? this.grid.searchKeyword : '',
			orderFields: new List(this.grid.sortColumns).select(i => i.field).toArray(),
			orderDirections: new List(this.grid.sortColumns).select(i => i.dir).toArray(),
			...this.grid.commonSearchOptions,
		};

		return this.httpClient.get<ResponseList<any>>(this.grid.baseApi, { params: param })
			.pipe(
				map((result) => {
					if (result.Result === EnumResponseResult.Error) {
						this.errorEmitter.emit(result);
					}
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	private getCurrentViewIfHasNotNull(): any[] {
		const currentView = this.grid.items.slice(this.grid.skip, this.grid.skip + this.grid.pageSize);
		if (currentView.length > 0 && currentView.every(i => i !== null)) {
			return currentView;
		} else {
			return null;
		}
	}

	private emptyArray(pageSize: number, skip: number, totalCount: number): any[] {
		const size = totalCount - skip < pageSize ? pageSize : totalCount - skip;
		return new Array(size).fill(null);
	}

	public update(): void {
		// console.log('update : 현재 skip과 page에 해당하는 데이터 부분만 골라서 bind');
		this.isCellRendering = true;
		const { skip, pageSize, totalCount } = this.grid;
		const subscription = this.takeItems()
			.pipe(
				catchError((err: HttpErrorResponse) => {
					this.messageService.error(err.message);

					// Error 가 발생해도 이전 page 데이터가 남아있지 않도록 빈 데이터로 교체한다.
					const emptyArrayForError: any[] = this.emptyArray(pageSize, skip, totalCount);
					return of(emptyArrayForError);
				})
			)
			.subscribe((items: any[]) => {
				this.updateGridView(items);
			});
		this.subscriptions.push(subscription);
	}

	public updateGridView(items: any[]): void {
		this.grid.gridView = {
			data: [...items],
			total: this.grid.items.length,
			grid: this.grid
		};
		this.displayMasterColor = this.colorColumn.hasColor(items);
		this.displayDetailColor = this.colorColumn.hasItemColor(items);
		this.isCellRendering = false;
	}

	public pageChange(event: PageChangeEvent): void {
		this.grid.skip = event.skip;
		this.update();
		this.pageChangeEmitter.emit(event);
	}

	public cellClick(event: CellClickEvent): void {

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
	 * 셀 셀렉트시 반응하는 이벤트
	 */
	public selectionChange(event: SelectionEvent): void {
		this.selectionChangeEmiiter.emit(event);
	}

	/**
	 * 체크박스 셀릭트시 발생하는 이벤트
	 */
	public onSelectedKeysChange(e: any): void {
		// 선택된 항목 저장
		this.grid.selectedItems = this.grid.items.filter(item => e.includes(this.GetKeyValue(item)));
		this.selectedKeysChangeEmiiter.emit(e);
	}

	/**
	 * API 요청이 아닐때
	 */
	public bindListNoneApi(): void {
		this.updateGridView(this.grid.items.slice(this.grid.skip, this.grid.skip + this.grid.pageSize));
	}

	/**
	 * 키 선택 변경 시 발생하는 이벤트
	 * @param context 행 정보 객체
	 */
	public getSelectionKey(context: RowArgs): string {

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
	public GetKeyValue(rowData: any): string {
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
	public sortChange(sorts: SortDescriptor[]): void {
		this.grid.setSortColumns(sorts.filter((item: any) => item.dir));
		this.loadData();
		this.sortChangeEmitter.emit(sorts);
	}

	/**
	 * 툴팁 처리
	 * @param e 마우스 이벤트 객체
	 */
	public showTooltip(e: MouseEvent): void {
		const element = e.target as HTMLElement;
		if (element.innerText) {
			if (((element.nodeName === 'TD' || element.nodeName === 'TH' || element.nodeName === 'DIV' || element.nodeName === 'A') && element.offsetWidth < element.scrollWidth)
				|| (element.nodeName === 'SPAN' && element.parentElement.offsetWidth < element.parentElement.scrollWidth)
			) {
				this.tooltipDir.toggle(element);
			} else {
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
				this.selectionChangeEmiiter.emit({selectedRows, deselectedRows, ctrlKey: false, shiftKey: false});
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
