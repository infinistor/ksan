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
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {CommonModule, DatePipe} from '@angular/common';
import {HttpClientModule} from '@angular/common/http';
import {RouterModule} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';

import {MomentModule} from 'ngx-moment';

import './extensions/object.extension';
import './extensions/array.extension';
import './extensions/string.extension';
import './extensions/number.extension';
import './extensions/enum.extension';
import {GridCoreModule} from './services/grid-core.module';
import {DisableControlDirective} from './directive/disable-control-directive';
import {CommonDialogService} from './services/common-dialog.service';
import {CommonWindowService} from './services/common-window.service';
import {WINDOW_PROVIDERS} from './services/windows.service';
import {MessageService} from '@progress/kendo-angular-l10n';
import {ButtonsModule} from '@progress/kendo-angular-buttons';
import {InputsModule} from '@progress/kendo-angular-inputs';

@NgModule({
	declarations: [
		DisableControlDirective
	],
	exports: [
		FormsModule,
		CommonModule,
		TranslateModule,
		HttpClientModule,
		RouterModule,
		MomentModule,
		ReactiveFormsModule,
		GridCoreModule,
		InputsModule,
		ButtonsModule,
		DisableControlDirective,
	],
	imports: [
	],
	providers: [
		DatePipe,
		WINDOW_PROVIDERS,
		CommonDialogService,
		CommonWindowService,
		MessageService
	]
})
export class AppSharedModule {
}
