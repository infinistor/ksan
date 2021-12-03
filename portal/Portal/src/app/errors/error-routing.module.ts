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
import {RouterModule, Routes} from '@angular/router';
import {Error400PageComponent} from './400/error-400-page.component';
import {Error401PageComponent} from './401/error-401-page.component';
import {Error403PageComponent} from './403/error-403-page.component';
import {Error404PageComponent} from './404/error-404-page.component';
import {Error500PageComponent} from './500/error-500-page.component';
import {Error503PageComponent} from './503/error-503-page.component';
import {Error504PageComponent} from './504/error-504-page.component';

const routes: Routes = [
	{path: '', redirectTo: '404', pathMatch: 'full'},
	{path: '400', component: Error400PageComponent},
	{path: '401', component: Error401PageComponent},
	{path: '403', component: Error403PageComponent},
	{path: '404', component: Error404PageComponent},
	{path: '500', component: Error500PageComponent},
	{path: '503', component: Error503PageComponent},
	{path: '504', component: Error504PageComponent},
];

@NgModule({
	imports: [RouterModule.forChild(routes)],
	exports: [RouterModule]
})
export class ErrorRoutingModule {
}
