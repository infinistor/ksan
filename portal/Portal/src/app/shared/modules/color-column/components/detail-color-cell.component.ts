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
import { Component, OnInit, ChangeDetectionStrategy, Input, OnChanges } from '@angular/core';
import { ColorColumnService } from '../services/color-column.service';
import { CellColorStyleMaker } from '../services/cell-color-style-maker.service';
import { ColorColumn } from '../../../classes/color-column';

@Component({
	selector: 'app-detail-color-cell',
	templateUrl: './detail-color-cell.component.html',
	styleUrls: ['./detail-color-cell.component.scss'],
	changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailColorCellComponent implements OnInit, OnChanges {
	@Input() colorColumn: ColorColumn;
	@Input() rowData: any;
	@Input() countOfColor = 1;
	colorStyle: { [key: string]: string };

	constructor(private colorColumnService: ColorColumnService, private cellColorStyleMaker: CellColorStyleMaker) {}

	ngOnInit(): void {}

	ngOnChanges(): void {
		this.fillColor();
	}

	private fillColor(): void {
		if (!this.colorColumn || !this.rowData || this.countOfColor < 1) {
			this.colorStyle = undefined;
			return;
		}

		const cellColor = this.colorColumnService.createFirstCellColor(this.rowData, this.colorColumn);

		const itemsByColor = cellColor.top(this.countOfColor);
		this.colorStyle = this.cellColorStyleMaker.make(itemsByColor);
	}
}
