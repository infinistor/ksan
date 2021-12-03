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
import {AfterViewInit, Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Grid} from '../../shared/classes/grid/grid.model';
import {GridColumnConfigService} from '../../shared/services/grid-column-config.service';
import {environment} from '../../../environments/environment';
import {GridDataLoadedEvent} from '../../shared/classes/grid/grid-data-loaded-event.model';
import {PageChangeEvent} from '@progress/kendo-angular-grid';

@Component({
	selector: 'app-disk-pools',
	templateUrl: './disk-pools.component.html',
	styleUrls: ['./disk-pools.component.scss']
})
export class DiskPoolsComponent implements OnInit, OnDestroy, AfterViewInit {

	/**
	 * 통신중 플래그
	 */
	inCommunication: boolean = false;

	/**
	 * 초기화 상태
	 */
	initState: { viewInitialized: boolean, requestLoadData: boolean } = { viewInitialized: false, requestLoadData: false };

	/**
	 * 그리드
	 */
	grid: Grid = new Grid();

	/**
	 * 생성자
	 * @param router 라우터 객체
	 * @param gridColumnConfigService 그리드 설정 관리 서비스 객체
	 */
	constructor(
		private router: Router,
		private gridColumnConfigService: GridColumnConfigService,
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
		this.grid.baseApi = `${environment.apiUrl}/DiskPools`;
		this.grid.isUseCheckboxSelect = false;
		this.grid.selectableSettings = {
			enabled: true,
			mode: 'single',
			checkboxOnly: false
		};
	}

	// 그리드 컬럼 설정
	private setGridColumns(): void {

		this.grid.keyColumnNames = ['Id'];
		this.grid.columns = [
			{ title: 'Name', field: 'Name', width: null, format: '', pipe: null, pipeExtra: null, class: 'text-left' },
			{ title: 'Description', field: 'Description', width: null, format: '', pipe: null, pipeExtra: null, class: 'text-left' },
			{ title: 'Reg. Date', field: 'RegDate', width: 170, format: '', pipe: 'formatDate', pipeExtra: 'YYYY-MM-DD HH:mm:ss', class: 'text-center' },
			{ title: 'Reg. Name', field: 'RegName', width: 160, format: '', pipe: null, pipeExtra: null, class: 'text-center' },
			{ title: 'Mod. Date', field: 'ModDate', width: 170, format: '', pipe: 'formatDate', pipeExtra: 'YYYY-MM-DD HH:mm:ss', class: 'text-center' },
			{ title: 'Mod. Name', field: 'ModName', width: 160, format: '', pipe: null, pipeExtra: null, class: 'text-center' },
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
}
