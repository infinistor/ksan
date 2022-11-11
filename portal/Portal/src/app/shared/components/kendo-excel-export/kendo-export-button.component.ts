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
import {saveAs} from '@progress/kendo-file-saver';
import {Component, EventEmitter, Input, OnInit, Output, ViewChild, ViewEncapsulation} from '@angular/core';
import {Grid} from '../../classes/grid/grid.model';
import {List} from 'linq-collections';
import {catchError, finalize, map} from 'rxjs/operators';
import {Observable, throwError} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {TranslateService} from '@ngx-translate/core';
import {MessageNotificationService} from '../../services/message-notification.service';
import {ExcelExportComponent, Workbook, WorkbookSheetColumn, WorkbookSheetRow, WorkbookSheetRowCell} from '@progress/kendo-angular-excel-export';
import {PDFExportComponent} from '@progress/kendo-angular-pdf-export';
import {EnumResponseResult} from '../../models/enums/enum-response-result.model';
import {ResponseList} from '../../models/response-list.model';
import {DynamicPipe} from '../../pipes/dynamic.pipe';

@Component({
	selector: 'app-common-kendo-export-button',
	templateUrl: 'kendo-export-button.component.html',
	styleUrls: ['kendo-export-button.component.scss'],
	encapsulation: ViewEncapsulation.None,
})
export class CommonKendoExportButtonComponent implements OnInit {

	/**
	 * 내보내기 이벤트
	 */
	@Output() onExport: EventEmitter<any> = new EventEmitter<any>();

	/**
	 * 그리드 설정 객체
	 */
	@Input() grid: Grid = null;
	/**
	 * 파일명
	 */
	@Input() fileName: string = 'export';
	/**
	 * 비활성화 여부
	 */
	@Input() disabled: boolean = false;
	/**
	 * 순번 표시 여부
	 */
	@Input() isUseSequence: boolean = false;
	/**
	 * 최대 내보내기 데이터 수 (30,000)
	 */
	@Input() maxExportCount: number = 30000;
	/**
	 * Excel 내보내기 활성화 여부
	 */
	@Input()
	get enableExcelExport(): boolean {
		return this._enableExcelExport;
	}
	set enableExcelExport(value: boolean) {
		this._enableExcelExport = value;
		this.setExportTypes();
	}
	private _enableExcelExport: boolean = true;
	/**
	 * PDF 내보내기 활성화 여부
	 */
	@Input()
	get enablePdfExport(): boolean {
		return this._enablePdfExport;
	}
	set enablePdfExport(value: boolean) {
		this._enablePdfExport = value;
		this.setExportTypes();
	}
	private _enablePdfExport: boolean = true;
	/**
	 * 그룹버튼 제목
	 */
	@Input()
	get groupButtonTitle(): string {
		return this._groupButtonTitle;
	}
	set groupButtonTitle(value: string) {
		this._groupButtonTitle = this.translateService.instant(value);
	}
	private _groupButtonTitle: string = ''; // this.translateService.instant('UL_BUTTON_COMMON_EXPORT_TO_EXCEL');
	/**
	 * 그룹버튼 아이콘 클래스
	 */
	@Input() groupButtonIconClass: string = 'far fa-file-excel';

	/**
	 * 내보내기 버튼 정보 목록
	 */
	exportButtons: any[] = [];

	/**
	 * 엑셀 내보내기 버튼
	 */
	@ViewChild('excelexport', { static: false }) exportExcel: ExcelExportComponent;

	/**
	 * PDF 내보내기 버튼
	 */
	@ViewChild('pdfexport', { static: false }) exportPdf: PDFExportComponent;

	/**
	 * 출력데이터
	 */
	data: any = null;

	/**
	 * 생성자
	 * @param httpClient HttpClient 객체
	 * @param translateService 번역 객체
	 * @param messageService 메세지 서비스 객체
	 */
	constructor(
		private httpClient: HttpClient,
		private translateService: TranslateService,
		private messageService: MessageNotificationService,
	) {
	}

	/**
	 * 초기화
	 */
	ngOnInit(): void {
	}

	/**
	 * 내보내기 버튼 설정
	 */
	setExportTypes(): void {
		this.exportButtons = [];
		if (this.enableExcelExport && this.enablePdfExport) {
			this.exportButtons.push({ text: 'Excel', icon: 'fas fa-file-excel' });
			this.exportButtons.push({ text: 'PDF', icon: 'fas fa-file-pdf' });
		}
		else if (this.enableExcelExport && !this.enablePdfExport) {
			this.exportButtons.push({ text: 'Excel', icon: 'fas fa-file-excel' });
		}
		else if (!this.enableExcelExport && this.enablePdfExport) {
			this.exportButtons.push({ text: 'PDF', icon: 'fas fa-file-pdf' });
		}
	}

	/**
	 * 내보내기
	 * @param exportMedia 출력할 미디어 타입
	 */
	export(exportMedia: string): void {

		exportMedia = exportMedia.toLowerCase();

		// 최대 내보내기 건 수 초과의 데이터인 경우, 에러 출력
		if (this.grid.totalCount > this.maxExportCount)
		{
			this.messageService.error(this.translateService.instant('EM_COMMON__EXCEL_EXPORT_MAX_EXPORT_LIMIT', { MaxExportCount: this.maxExportCount }));
		}
		// 최대 내보내기 건 수 이하인 경우
		else {
			this.disabled = true;

			this.onExport.emit();

			// 데이터 목록을 가져온다.
			this.getList()
				.pipe(
					finalize(() => {
						this.disabled = false;
					})
				)
				.subscribe(response => {
					if (response.Result === EnumResponseResult.Success) {
						// this.data = response.Data.Items;

						// 엑셀
						if (exportMedia === 'excel') {

							this.disabled = true;

							this.exportExcel.fileName = this.fileName + '.xlsx';
							this.exportExcel.save(response.Data.Items);

							this.disabled = false;
						}
						// PDF
						else if (exportMedia === 'pdf') {
							this.disabled = true;

							this.exportPdf.saveAs(this.fileName + '.pdf');

							this.disabled = false;
						}
					}
					else {
						this.messageService.error(response.Message);
					}
				});
		}
	}

	exportToExcel(exportMedia: string): void {

		exportMedia = exportMedia.toLowerCase();

		// 최대 내보내기 건 수 초과의 데이터인 경우, 에러 출력
		if (this.grid.totalCount > this.maxExportCount)
		{
			this.messageService.error(this.translateService.instant('EM_COMMON__EXCEL_EXPORT_MAX_EXPORT_LIMIT', { MaxExportCount: this.maxExportCount }));
		}
		// 최대 내보내기 건 수 이하인 경우
		else {
			this.disabled = true;

			this.onExport.emit();

			// 데이터 목록을 가져온다.
			this.getList()
				.pipe(
					finalize(() => {
						this.disabled = false;
					})
				)
				.subscribe(response => {
					if (response.Result === EnumResponseResult.Success) {
						// this.data = response.Data.Items;

						// 엑셀
						if (exportMedia === 'excel') {

							this.disabled = true;

							const columns: WorkbookSheetColumn[] = [];
							this.grid.columns.map(() => {
								columns.push({ autoWidth: true });
							});

							const rows: WorkbookSheetRow[] = [];

							// 헤더 추가
							let cells: WorkbookSheetRowCell[] = [];
							this.grid.columns.map((column) => {
								cells.push({ value: column.title, background: '#7A7A7A', bold: true, color: '#FFFFFF' });
							});
							rows.push({ cells });

							const dynamicPipe: DynamicPipe = new DynamicPipe(this.translateService);

							// 데이터 추가
							response.Data.Items.map((row) => {
								cells = [];
								this.grid.columns.map((column) => {

									let color: string = '#000000';

									// 색상 컬럼이 존재하는 경우
									// @ts-ignore
									if (row[DynamicPipe.COLOR_PROPERTY_NAME] && row[DynamicPipe.COLOR_PROPERTY_NAME][column.field] && row[DynamicPipe.COLOR_PROPERTY_NAME][column.field]) {
										// @ts-ignore
										color = row[DynamicPipe.COLOR_PROPERTY_NAME][column.field];
									}

									if (column.pipe) {
										if (column.customTemplate) {
											// @ts-ignore
											cells.push({ value: dynamicPipe.transform(row, column.pipe, column.customTemplate, column.field, false), color });
										}
										else {
											// @ts-ignore
											cells.push({ value: dynamicPipe.transform(row, column.pipe, column.pipeExtra, column.field, false), color });
										}
									}
									else {
										// @ts-ignore
										cells.push({ value: row[column.field], color });
									}
								});
								rows.push({ cells });
							});

							const workbook = new Workbook({
								sheets: [
									{
										columns,
										name: this.grid.name,
										rows
									}
								]
							});
							workbook.toDataURL().then((dataUrl) => {
								saveAs(dataUrl, this.fileName);
							});

							this.disabled = false;
						}
						// PDF
						else if (exportMedia === 'pdf') {
							this.disabled = true;

							this.exportPdf.saveAs(this.fileName + '.pdf');

							this.disabled = false;
						}
					}
					else {
						this.messageService.error(response.Message);
					}
				});
		}
	}

	/**
	 * 리스트 API 요청을 한다
	 */
	getList<T>(): Observable<ResponseList<T>> {

		const param = {
			skip: 0,
			countPerPage: 99999999,
			orderFields: new List(this.grid.sortColumns).select(i => i.field).toArray(),
			orderDirections: new List(this.grid.sortColumns).select(i => i.dir).toArray(),
			...this.grid.commonSearchOptions,
		};

		return this.httpClient.get<ResponseList<T>>(this.grid.baseApi, { params: param })
			.pipe(
				map((result) => {
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}
}
