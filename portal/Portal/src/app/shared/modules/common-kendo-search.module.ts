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
import { NgModule, LOCALE_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { HttpClient } from '@angular/common/http';

import { DateInputsModule, DateTimePickerModule } from '@progress/kendo-angular-dateinputs';
import { ButtonsModule } from '@progress/kendo-angular-buttons';
import { DropDownsModule } from '@progress/kendo-angular-dropdowns';
import { FormsModule } from '@angular/forms';
import { PopupModule } from '@progress/kendo-angular-popup';

import { CommonKendoSearchComponent } from '../components/kendo-search/kendo-search.component';

import '@progress/kendo-angular-intl/locales/ko/calendar';
import '@progress/kendo-angular-intl/locales/en/calendar';
import {TooltipModule} from '@progress/kendo-angular-tooltip';
import {CheckBoxModule, TextBoxModule} from '@progress/kendo-angular-inputs';

// 번역 json 파일 로드
export function createTranslateLoader(http: HttpClient): TranslateHttpLoader {
	return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

@NgModule({
	imports: [
		CommonModule,
		DateInputsModule,
		DateTimePickerModule,
		ButtonsModule,
		DropDownsModule,
		FormsModule,
		PopupModule,
		TooltipModule,
		TranslateModule.forChild({
			loader: {
				provide: TranslateLoader,
				useFactory: (createTranslateLoader),
				deps: [HttpClient]
			}
		}),
		TextBoxModule,
		CheckBoxModule
	],
	declarations: [
		CommonKendoSearchComponent
	],
	exports: [
		CommonKendoSearchComponent
	],
	providers: [
		{ provide: LOCALE_ID, useValue: navigator.language }
	]
})
export class CommonKendoSearchModule {

}
