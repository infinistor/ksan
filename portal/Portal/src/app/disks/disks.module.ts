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
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {AppSharedModule} from '../shared/app-shared.module';
import {GridModule} from '@progress/kendo-angular-grid';
import {CommonDynamicPipeModule} from '../shared/modules/dynamic-pipe.module';
import {DisksRoutingModule} from './disks-routing.module';
import {DisksComponent} from './disks/disks.component';
import {DiskPoolsComponent} from './disk-pools/disk-pools.component';
import {CardModule} from '@progress/kendo-angular-layout';
import {CommonKendoExportButtonModule} from '../shared/modules/common-kendo-export-button.module';
import {CommonKendoPaginateModule} from '../shared/modules/common-kendo-grid-paginate.module';
import {CommonKendoSearchModule} from '../shared/modules/common-kendo-search.module';
import {EnumDropDownListModule} from '../shared/modules/enum-dropdownlist.module';
import {EnumMultiSelectModule} from '../shared/modules/enum-multi-select.module';


@NgModule({
	declarations: [
		DisksComponent,
		DiskPoolsComponent,
	],
	imports: [
		DisksRoutingModule,
		CommonModule,
		AppSharedModule,
		GridModule,
		CommonDynamicPipeModule,
		CardModule,
		CommonKendoExportButtonModule,
		CommonKendoPaginateModule,
		CommonKendoSearchModule,
		EnumDropDownListModule,
		EnumMultiSelectModule
	],
	providers: [

	],
})
export class DisksModule {
}
