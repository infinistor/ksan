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
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';

import { AuthService } from './auth.service';
import {ResponseData} from '../models/response-data-simple.model';
import {EnumResponseResult} from '../models/enums/enum-response-result.model';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {

	constructor(
		private authService: AuthService,
		private router: Router
	) {
	}

	/**
	 * 활성화 가능한지 여부
	 * @param next 다음에 로드할 경로에 대한 정보 객체
	 * @param state 라우터 상태 객체
	 */
	async canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {

		if (next.data && next.data.expectedClaims && next.data.expectedClaims.length > 0) {
			const hasClaim = await this.authService.hasClaimInChacheOneOf(next.data.expectedClaims);

			if (!hasClaim) {
				if (next.data.expectedClaims.length === 1 && next.data.expectedClaims[0] === 'cssp.dashboard.all')
					this.router.navigate(['/dashboard/blank']);
				else {
					this.router.navigate(['/account/login']);
				}
			}
			return hasClaim;
		}
		else {
			const checkLoginResponse: ResponseData = await this.authService.checkLogin().toPromise();
			if (checkLoginResponse.Result === EnumResponseResult.Error) {
				this.router.navigate(['/account/login']);
			}
			else
				return true;
		}
	}
}
