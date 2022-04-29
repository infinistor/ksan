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
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { DialogModule, WindowModule } from '@progress/kendo-angular-dialog';
import { ButtonsModule } from '@progress/kendo-angular-buttons';
import { DropDownsModule } from '@progress/kendo-angular-dropdowns';
import { TreeViewModule } from '@progress/kendo-angular-treeview';
import {HTTP_INTERCEPTORS, HttpClient} from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DateInputsModule } from '@progress/kendo-angular-dateinputs';

import { GridModule } from '@progress/kendo-angular-grid';
import { CommonKendoGridPaginateComponent } from '../components/kendo-grid-paginate/kendo-grid-paginate.component';

import { CommonDynamicPipeModule } from './dynamic-pipe.module';
import { LoadingModule } from './loading.module';
import { GridColumnHeaderModule } from './grid-column-header.module';
import { CommonKendoPaginateDetailGridComponent } from '../components/kendo-grid-paginate/kendo-grid-paginate-detail.component';
import { TooltipModule } from '@progress/kendo-angular-tooltip';
import { ClipboardModule } from 'ngx-clipboard';
import {SiteSharedModule} from './site-shared.module';
import {HttpServiceInterceptor} from '../services/http-service.interceptor';

// 번역 json 파일 로드
export function createTranslateLoader(http: HttpClient): TranslateHttpLoader {
	return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

@NgModule({
	imports: [
		CommonModule,
		GridModule,
		FormsModule,
		DialogModule,
		WindowModule,
		ButtonsModule,
		DropDownsModule,
		TreeViewModule,
		DateInputsModule,
		CommonDynamicPipeModule,
		LoadingModule,
		GridColumnHeaderModule,
		TranslateModule.forChild({
			loader: {
				provide: TranslateLoader,
				useFactory: (createTranslateLoader),
				deps: [HttpClient]
			}
		}),
		SiteSharedModule,
		TooltipModule,
		ClipboardModule
	],
	declarations: [
		CommonKendoGridPaginateComponent,
		CommonKendoPaginateDetailGridComponent,
	],
	exports: [
		CommonKendoGridPaginateComponent,
		CommonKendoPaginateDetailGridComponent,
	],
	providers: [
		{
			provide: HTTP_INTERCEPTORS,
			useClass: HttpServiceInterceptor,
			multi: true
		},
	]
})
export class CommonKendoPaginateModule {

}
