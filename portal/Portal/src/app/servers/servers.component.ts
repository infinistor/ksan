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
import {AfterViewInit, Component, OnDestroy, OnInit, TemplateRef} from '@angular/core';
import {Router} from '@angular/router';
import {EnumServerState} from '../shared/models/enums/enum-server-state';
import {Grid} from '../shared/classes/grid/grid.model';
import {GridDataLoadedEvent} from '../shared/classes/grid/grid-data-loaded-event.model';
import {CellClickEvent, PageChangeEvent} from '@progress/kendo-angular-grid';
import {environment} from '../../environments/environment';
import {GridColumnConfigService} from '../shared/services/grid-column-config.service';
import {DialogRef} from '@progress/kendo-angular-dialog';
import {CommonDialogService} from '../shared/services/common-dialog.service';
import {ServersProvider} from '../shared/services/data-providers/servers.service';
import {finalize} from 'rxjs/operators';
import {EnumResponseResult} from '../shared/models/enums/enum-response-result.model';
import {MessageNotificationService} from '../shared/services/message-notification.service';
import {ResponseData} from '../shared/models/response-data.model';
import {ResponseServer} from '../shared/models/responses/servers/response-server';
import {List} from 'linq-collections';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {RequestServer} from '../shared/models/requests/servers/request-server-update.model';
import {TranslateService} from '@ngx-translate/core';
import {enumerate, EnumItem} from '../shared/extensions/enum.extension';

@Component({
	selector: 'app-servers',
	templateUrl: './servers.component.html',
	styleUrls: ['./servers.component.scss']
})
export class ServersComponent implements OnInit, OnDestroy, AfterViewInit {
	/**
	 * 통신중 플래그
	 */
	inCommunication: boolean = false;

	/**
	 * 초기화 상태
	 */
	initState: { viewInitialized: boolean, requestLoadData: boolean } = { viewInitialized: false, requestLoadData: false };

	/**
	 * 서버 상태 목록
	 */
	serverStateList: EnumItem[] = enumerate<EnumServerState>(EnumServerState);
	/**
	 * 선택한 서버 상태 목록
	 */
	serverStateSelected: EnumServerState[];

	/**
	 * 그리드
	 */
	grid: Grid = new Grid();

	/**
	 * 표시되고 있는 다이얼로그 객체
	 */
	dialogs: DialogRef[] = [];

	/**
	 * 서버 정보 폼
	 */
	formGroupServer: FormGroup = new FormGroup(
		{
			Id: new FormControl('', [Validators.required]),
			Name: new FormControl('', [Validators.required]),
			Description: new FormControl('', []),
			CpuModel: new FormControl('', []),
			Clock: new FormControl(0, []),
			State: new FormControl('', [Validators.required]),
			Rack: new FormControl('', []),
		}
	);

	/**
	 * 서버 정보 변경 폼
	 */
	formUpdateGroupServer: FormGroup = new FormGroup(
		{
			Id: new FormControl('', [Validators.required]),
			Name: new FormControl('', [Validators.required]),
			Description: new FormControl('', []),
			CpuModel: new FormControl('', []),
			Clock: new FormControl(0, []),
			State: new FormControl('', [Validators.required]),
			Rack: new FormControl('', []),
		}
	);

	/**
	 * 서버 정보 등록 폼
	 */
	formAddGroupServer: FormGroup = new FormGroup(
		{
			Name: new FormControl('', [Validators.required]),
			Description: new FormControl('', []),
			CpuModel: new FormControl('', []),
			Clock: new FormControl(0, []),
			State: new FormControl('', [Validators.required]),
			Rack: new FormControl('', []),
		}
	);

	/**
	 * 생성자
	 * @param router 라우터 객체
	 * @param translateService 번역 서비스 객체
	 * @param messageService 메시지 알림 서비스 객체
	 * @param commonDialogService 다이얼로그 서비스 객체
	 * @param gridColumnConfigService 그리드 설정 관리 서비스 객체
	 * @param serversProvider 서버 데이터 프로바이더 객체
	 */
	constructor(
		private router: Router,
		private translateService: TranslateService,
		private messageService: MessageNotificationService,
		private commonDialogService: CommonDialogService,
		private gridColumnConfigService: GridColumnConfigService,
		private serversProvider: ServersProvider
	) {
	}

	/**
	 * 초기화 이벤트
	 */
	ngOnInit(): void {
		// 그리드 설정
		this.setGridBase();
		this.setGridColumns();
	}

	/**
	 * 뷰 초기화 이벤트
	 */
	ngAfterViewInit(): void {

		// 뷰 초기화 완료 후, 데이터 로드가 요청이 된 경우
		if (this.initState.requestLoadData) {
			setTimeout(() => {
				this.loadGrid(true);
			});
		}

		// 초기화 완료됨으로 설정
		this.initState.viewInitialized = true;
	}

	/**
	 * 해제 이벤트
	 */
	ngOnDestroy(): void {
	}

	/**
	 * 그리드 기본 설정
	 * @private
	 */
	private setGridBase(): void {
		this.grid.name = 'default';
		this.grid.location = this.router.url;
		this.grid.skip = 0;
		this.grid.pageSize = 20;
		this.grid.gridView.data = [];
		this.grid.gridView.total = 0;
		this.grid.isUseCalculatePageSize = false;
		this.grid.isUseSequence = true;
		this.grid.baseApi = `${environment.apiUrl}/Servers`;
		this.grid.isUseCheckboxSelect = false;
		this.grid.selectableSettings = {
			enabled: true,
			mode: 'single',
			checkboxOnly: false
		};
		this.grid.flexingSize = 50;
	}

	// 그리드 컬럼 설정
	private setGridColumns(): void {

		this.grid.keyColumnNames = ['Id'];
		this.grid.columns = [
			{ title: 'Name', field: 'Name', width: null, format: '', pipe: 'enableClick', pipeExtra: null, class: 'text-left' },
			{ title: 'Description', field: 'Description', width: null, format: '', pipe: null, pipeExtra: null, class: 'text-left' },
			{ title: 'Cpu Model', field: 'CpuModel', width: null, format: '', pipe: null, pipeExtra: null, class: 'text-left' },
			{ title: 'Clock', field: 'Clock', width: 100, format: '', pipe: 'numberWithCommas', pipeExtra: null, class: 'text-right' },
			{ title: 'State', field: 'State', width: 100, format: '', pipe: 'enumTranslate', pipeExtra: EnumServerState, class: 'text-center' },
			{ title: 'Rack', field: 'Rack', width: 250, format: '', pipe: 'numberWithCommas', pipeExtra: null, class: 'text-left' },
			{ title: 'Used', field: 'MemoryUsed', width: 100, format: '', pipe: 'sizeAuto', pipeExtra: null, class: 'text-right' },
			{ title: 'Free', field: 'MemoryFree', width: 100, format: '', pipe: 'sizeAuto', pipeExtra: null, class: 'text-right' },
		];

		this.grid.setSortColumns([ { field: 'Name', dir: 'asc' } ]);

		const config = this.gridColumnConfigService.getConfigByGrid(this.grid);
		config.hiddenColumnFields = [];
		this.grid.applyColumnsConfig(config);
	}

	/**
	 * 검색
	 * @param param 검색 조건
	 */
	search(param: any): void {
		this.grid.commonSearchOptions = {
			searchStates: this.serverStateSelected,
			...param
		};

		// 뷰 초기화가 완료된 경우
		if (this.initState.viewInitialized)
			this.loadGrid(true);
		// 뷰 초기화가 완료되지 않은 경우, 뷰 초기화 완료 후 실행하도록 플래그 설정
		else
			this.initState.requestLoadData = true;
	}

	/**
	 * 데이터 내보내기
	 */
	onExport(): void {
	}

	// 데이터 로드
	loadGrid(clearSelection?: boolean): void {

		if (clearSelection) {
			this.grid.clearSelection();
		}
		this.inCommunication = true;
		this.grid.forceApiReload();
	}

	/**
	 * 데이터 로드 후 발생하는 이벤트
	 * @param event 데이터 로드 정보 객체
	 */
	onDataLoaded(event: GridDataLoadedEvent): void {
		this.inCommunication = false;

		// 아무 행도 선택되지 않은 경우
		if (event.grid && event.grid.isNotSelectedAnyRow()) {
			event.grid.selectByItem(event.grid.items[0]);
		}
	}

	/**
	 * 페이지 변경 시 발생하는 이벤트
	 * @param event 페이지 변경 정보 객체
	 */
	onPageChange(event: PageChangeEvent): void {
		// this.inCommunication = true;
	}

	/**
	 * 그리드 항목 선택 변경 시 발생하는 이벤트
	 * @param event
	 */
	onItemSelectChange(event: any): void {
	}

	/**
	 * 그리드 셀 클릭 시 발생하는 이벤트
	 * @param event 셀 클릭 정보 객체
	 * @param content 조회 다이얼로그 내용 템플릿
	 * @param actions 조회 다이얼로그 버튼 템플릿
	 */
	onCellClick(event: CellClickEvent, content: TemplateRef<any>, actions: TemplateRef<any>): void {
		if (event.column.field === 'Name') {
			this.view(event.dataItem.Id, content, actions);
		}
	}

	/**
	 * 다이얼로그를 닫는다.
	 */
	closeDialog(): void {

		// 화면에 표시된 다이얼로그가 존재하는 경우
		if (this.dialogs && this.dialogs.length > 0) {
			// 다이얼로그 객체를 가져온다.
			const dialog = this.dialogs.pop();
			dialog.close();
		}
	}

	/**
	 * 정보 조회 화면을 보여준다.
	 * @param id 서버 아이디
	 * @param content 조회 다이얼로그 내용 템플릿
	 * @param actions 조회 다이얼로그 버튼 템플릿
	 */
	view(id: string, content: TemplateRef<any>, actions: TemplateRef<any>): void {

		this.inCommunication = true;

		// 서버 정보를 가져온다.
		this.serversProvider.get(id)
			.pipe(
				finalize(() => {
					this.inCommunication = false;
				})
			)
			.subscribe((response: ResponseData<ResponseServer>) => {

				// 서버 정보를 가져오는데 성공한 경우
				if (response.Result === EnumResponseResult.Success) {

					// 그리드 선택항목이 유효한 경우
					if (this.grid.selectedItems.length > 0) {
						// 조회한 정보의 선택 항목을 가져온다.
						const targetItem = new List<ResponseServer>(this.grid.selectedItems).where(i => i.Id === response.Data.Id).firstOrDefault();
						// 조회한 정보의 선택 항목이 존재하는 경우, 새로 가져온 내용을 복사
						if (targetItem) {
							const copyObject = Object.assign({}, response.Data);
							Object.assign(targetItem, Object.removeProperties(copyObject, targetItem));
						}
					}

					// 폴에 없는 속성 삭제
					Object.removeProperties(response.Data, this.formGroupServer.controls);

					// 데이터 설정
					this.formGroupServer.setValue(response.Data);

					// 다이얼로그 표시
					this.dialogs.push(this.commonDialogService.show({
						title: `Server - ${response.Data.Name}`,
						content,
						actionButtons: actions,
						enableClose: true,
						width: 600
					}));
				}
				// 서버 정보를 가져오는데 실패한 경우
				else {
					this.messageService.error('[' + response.Code + '] ' + response.Message);
				}
			});
	}

	/**
	 * 정보 수정 화면을 보여준다.
	 * @param id 서버 아이디
	 * @param content 수정 다이얼로그 내용 템플릿
	 * @param actions 수정 다이얼로그 버튼 템플릿
	 */
	showUpdate(id: string, content: TemplateRef<any>, actions: TemplateRef<any>): void {

		this.inCommunication = true;

		// 서버 정보를 가져온다.
		this.serversProvider.get(id)
			.pipe(
				finalize(() => {
					this.inCommunication = false;
				})
			)
			.subscribe((response: ResponseData<ResponseServer>) => {

				// 서버 정보를 가져오는데 성공한 경우
				if (response.Result === EnumResponseResult.Success) {

					// 그리드 선택항목이 유효한 경우
					if (this.grid.selectedItems.length > 0) {
						// 조회한 정보의 선택 항목을 가져온다.
						const targetItem = new List<ResponseServer>(this.grid.selectedItems).where(i => i.Id === response.Data.Id).firstOrDefault();
						// 조회한 정보의 선택 항목이 존재하는 경우, 새로 가져온 내용을 복사
						if (targetItem) {
							const copyObject = Object.assign({}, response.Data);
							Object.assign(targetItem, Object.removeProperties(copyObject, targetItem));
						}
					}

					// 폴에 없는 속성 삭제
					Object.removeProperties(response.Data, this.formGroupServer.controls);

					// 데이터 설정
					this.formUpdateGroupServer.setValue(response.Data);

					// 다이얼로그 표시
					this.dialogs.push(this.commonDialogService.show({
						title: `Update Server - ${response.Data.Name}`,
						content,
						actionButtons: actions,
						enableClose: true,
						autoFocusedElement: 'Name',
						width: 600
					}));
				}
				// 서버 정보를 가져오는데 실패한 경우
				else {
					this.messageService.error('[' + response.Code + '] ' + response.Message);
				}
			});
	}

	/**
	 * 정보 수정
	 * @param form 폼 객체
	 */
	update(form: FormGroup): void {

		// 폴 객체가 유효하고 입력이 유효한 경우
		if (form && form.valid) {

			this.inCommunication = true;

			// 요청 객체 생성
			const request: RequestServer = Object.assign(new RequestServer(), form.getRawValue());

			// 서버 정보를 수정한다.
			this.serversProvider.update(form.getRawValue().Id, request)
				.pipe(
					finalize(() => {
						this.inCommunication = false;
					})
				)
				.subscribe((response: ResponseData<ResponseServer>) => {
					// 서버 정보를 가져오는데 성공한 경우
					if (response.Result === EnumResponseResult.Success) {
						if (response.Message)
							this.messageService.info(response.Message);

						// 변경된 값 반영
						this.formGroupServer.setValue(form.value);

						// 조회한 정보의 선택 항목을 가져온다.
						const targetItem = new List<ResponseServer>(this.grid.selectedItems).where(i => i.Id === form.getRawValue().Id).firstOrDefault();
						// 조회한 정보의 선택 항목이 존재하는 경우, 새로 가져온 내용을 복사
						if (targetItem) {
							const copyObject = Object.assign({}, form.value);
							Object.assign(targetItem, Object.removeProperties(copyObject, targetItem));
						}

						// 다이얼로그를 닫는다.
						this.closeDialog();
					}
					// 서버 정보를 가져오는데 실패한 경우
					else {
						this.messageService.error('[' + response.Code + '] ' + response.Message);
					}
				});
		}
	}

	/**
	 * 정보 등록 화면을 보여준다.
	 * @param content 수정 다이얼로그 내용 템플릿
	 * @param actions 수정 다이얼로그 버튼 템플릿
	 */
	showAdd(content: TemplateRef<any>, actions: TemplateRef<any>): void {

		// 데이터 설정
		this.formAddGroupServer.reset(new RequestServer());
		this.formAddGroupServer.controls.State.setValue('Offline');

		// 다이얼로그 표시
		this.dialogs.push(this.commonDialogService.show({
			title: `Add Server`,
			content,
			actionButtons: actions,
			enableClose: true,
			autoFocusedElement: 'Name',
			width: 600
		}));
	}

	/**
	 * 정보 수정
	 * @param form 폼 객체
	 */
	add(form: FormGroup): void {

		// 폴 객체가 유효하고 입력이 유효한 경우
		if (form && form.valid) {

			this.inCommunication = true;

			// 요청 객체 생성
			const request: RequestServer = Object.assign(new RequestServer(), form.getRawValue());

			// 서버 정보를 등록한다.
			this.serversProvider.add(request)
				.pipe(
					finalize(() => {
						this.inCommunication = false;
					})
				)
				.subscribe((response: ResponseData<ResponseServer>) => {
					// 서버 정보를 가져오는데 성공한 경우
					if (response.Result === EnumResponseResult.Success) {
						if (response.Message)
							this.messageService.info(response.Message);

						// 선택을 초기화하고 그리드 데이터를 로드한다.
						this.loadGrid(true);

						// 다이얼로그를 닫는다.
						this.closeDialog();
					}
					// 서버 정보를 가져오는데 실패한 경우
					else {
						this.messageService.error('[' + response.Code + '] ' + response.Message);
					}
				});
		}
	}

	/**
	 * 정보 삭제 화면을 보여준다.
	 * @param content 삭제 다이얼로그 내용 템플릿
	 * @param actions 삭제 다이얼로그 버튼 템플릿
	 */
	showDelete(content: TemplateRef<any>, actions: TemplateRef<any>): void {

		// 그리드에서 선택한 항목이 존재하는 경우
		if (this.grid.selectedItems.length > 0) {
			// 데이터 설정
			this.formUpdateGroupServer.reset(new RequestServer());

			// 다이얼로그 표시
			this.dialogs.push(this.commonDialogService.show({
				title: `Delete Server - ${this.grid.selectedItems[0].Name}`,
				content,
				actionButtons: actions,
				enableClose: true,
				autoFocusedElement: 'Name',
				width: 400
			}));
		}
	}

	/**
	 * 정보 삭제
	 */
	delete(): void {

		// 그리드에서 선택한 항목이 존재하는 경우
		if (this.grid.selectedItems.length > 0) {

			this.inCommunication = true;

			// 서버 정보를 삭제한다.
			this.serversProvider.delete(this.grid.selectedItems[0].Id)
				.pipe(
					finalize(() => {
						this.inCommunication = false;
					})
				)
				.subscribe((response: ResponseData<ResponseServer>) => {
					// 서버 정보를 가져오는데 성공한 경우
					if (response.Result === EnumResponseResult.Success) {
						if (response.Message)
							this.messageService.info(response.Message);

						// 선택을 초기화하고 그리드 데이터를 로드한다.
						this.loadGrid(true);

						// 다이얼로그를 닫는다.
						this.closeDialog();
					}
					// 서버 정보를 가져오는데 실패한 경우
					else {
						this.messageService.error('[' + response.Code + '] ' + response.Message);
					}
				});
		}
	}
}
