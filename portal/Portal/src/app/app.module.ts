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
import {LOCALE_ID, NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {NavigationModule} from '@progress/kendo-angular-navigation';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {IndicatorsModule} from '@progress/kendo-angular-indicators';
import {IconsModule} from '@progress/kendo-angular-icons';
import {LayoutModule} from '@progress/kendo-angular-layout';
import {ButtonsModule} from '@progress/kendo-angular-buttons';
import {DateInputsModule} from '@progress/kendo-angular-dateinputs';
import {DialogsModule} from '@progress/kendo-angular-dialog';
import {DropDownsModule} from '@progress/kendo-angular-dropdowns';
import {GridModule} from '@progress/kendo-angular-grid';
import {InputsModule} from '@progress/kendo-angular-inputs';
import {LabelModule} from '@progress/kendo-angular-label';
import {NotificationModule} from '@progress/kendo-angular-notification';
import {ExcelExportModule} from '@progress/kendo-angular-excel-export';
import {PDFExportModule} from '@progress/kendo-angular-pdf-export';
import {PagerModule} from '@progress/kendo-angular-pager';
import {RippleModule} from '@progress/kendo-angular-ripple';
import {ToolBarModule} from '@progress/kendo-angular-toolbar';
import {TooltipModule} from '@progress/kendo-angular-tooltip';
import {UploadModule} from '@progress/kendo-angular-upload';
import {HTTP_INTERCEPTORS, HttpClient, HttpClientModule} from '@angular/common/http';
import {TranslateLoader, TranslateModule, TranslateService} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import '@progress/kendo-angular-intl/locales/ko/all';
import '@progress/kendo-angular-intl/locales/en/all';
import {ContextMenuModule, MenuModule} from '@progress/kendo-angular-menu';
import {MessageNotificationService} from './shared/services/message-notification.service';
import {HttpServiceInterceptor} from './shared/services/http-service.interceptor';
import {UrlSerializer} from '@angular/router';
import {LowerCaseUrlSerializer} from './lower-case-url-serializer';
import {AuthService} from './shared/services/auth.service';
import {AuthGuard} from './shared/services/auth.guard';
import {AccountsService} from './shared/services/data-providers/accounts.service';
import {ReactiveFormsModule} from '@angular/forms';
import {AppSharedModule} from './shared/app-shared.module';


// 번역 json 파일 로드
export function createTranslateLoader(http: HttpClient): TranslateHttpLoader {
	return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}


@NgModule({
	declarations: [
		AppComponent
	],
	imports: [
		BrowserModule,
		HttpClientModule,
		AppRoutingModule,
		NavigationModule,
		BrowserAnimationsModule,
		IndicatorsModule,
		IconsModule,
		LayoutModule,
		ButtonsModule,
		DateInputsModule,
		DialogsModule,
		DropDownsModule,
		GridModule,
		InputsModule,
		LabelModule,
		NotificationModule,
		ExcelExportModule,
		PDFExportModule,
		PagerModule,
		RippleModule,
		ToolBarModule,
		TooltipModule,
		UploadModule,
		TranslateModule.forRoot({
			loader: {
				provide: TranslateLoader,
				useFactory: (createTranslateLoader),
				deps: [HttpClient]
			}
		}),
		MenuModule,
		ContextMenuModule,
		ReactiveFormsModule,
		AppSharedModule
	],
	providers: [
		{provide: UrlSerializer, useClass: LowerCaseUrlSerializer},
		MessageNotificationService,
		{
			provide: HTTP_INTERCEPTORS,
			useClass: HttpServiceInterceptor,
			multi: true
		},
		AuthService,
		AuthGuard,
		AccountsService,
		{provide: LOCALE_ID, useValue: navigator.language}
	],
	bootstrap: [AppComponent]
})
export class AppModule {
	// 생성자
	constructor(private translateService: TranslateService) {
		// 언어 번역 설정
		this.translateService.addLangs(['en', 'ko']);
		this.translateService.setDefaultLang('ko');

		const browserLang = this.translateService.getBrowserLang();
		this.translateService.use(browserLang.match(/en|ko/) ? browserLang : 'ko');
	}
}
