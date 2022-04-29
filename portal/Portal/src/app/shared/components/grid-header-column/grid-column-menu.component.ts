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
import { Component, Input, EventEmitter, Output, ViewEncapsulation } from '@angular/core';
import { BehaviorSubject, Subscription } from 'rxjs';

import { Align } from '@progress/kendo-angular-popup';
import { GridComponent } from '@progress/kendo-angular-grid';

@Component({
	selector: 'app-grid-column-menu',
	templateUrl: 'grid-column-menu.component.html',
	styles: [`
		.girdColumnMenuSet{ padding: 5px; }
	`],
	encapsulation: ViewEncapsulation.None
})
export class GridColumnMenuComponent {
	/*
	* groupName은 같은 화면 내에서 중복되지 않는 값이 들어와야 함. 필수 값은 아니지만 2개 이상의 그리드가 존재하게 될 경우 반드시 입력
	*/
	@Input() groupName: string = '';
	@Input() columns: any[];
	@Input() isShow = new BehaviorSubject<boolean>(false);
	@Input() offset = new BehaviorSubject<{ top: number, left: number }>({ top: 0, left: 0 });

	@Input() public set for(grid: GridComponent) {
		this.unsubscribe();
		if (grid) {
			this.gridCellClickSubscription = grid.cellClick.subscribe(this.onGridCellClick);
		}
	}

	@Output('hiddenChange') hiddenChangeEmitter: EventEmitter<{ field: string, isHidden: boolean }> = new EventEmitter();

	private gridCellClickSubscription: Subscription;
	public gridPopupAlign: Align = { horizontal: 'left', vertical: 'top' };

	constructor(
	) {
		this.onGridCellClick = this.onGridCellClick.bind(this);
	}

	private onGridCellClick(type: any, originalEvent: any): void {
		if (type === 'contextmenu') {
			originalEvent.preventDefault();
			this.isShow.next(true);
			this.offset.next({ top: originalEvent.pageY, left: originalEvent.pageX });
		}
	}

	private unsubscribe(): void {
		if (this.gridCellClickSubscription) {
			this.gridCellClickSubscription.unsubscribe();
			this.gridCellClickSubscription = null;
		}
	}

	hiddenChanged(e: any): void {
		const isHidden = !e.target.checked;

		if (isHidden && !this.canHide()) {
			e.target.checked = true;
			return;
		}

		const field = e.target.value;

		this.hiddenChangeEmitter.emit({ field, isHidden });
	}

	canHide(): boolean {
		return this.columns.filter(item => !item.isHidden).length > 1;
	}

	close(): void {
		this.isShow.next(false);
	}
}
