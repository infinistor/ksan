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
import { HttpClient } from '@angular/common/http';
import { map, catchError } from 'rxjs/operators';
import { throwError, Observable, BehaviorSubject } from 'rxjs';

import { SessionCacheService } from './cache/cache.service';

import { environment } from '../../../environments/environment';
import {EnumResponseResult} from '../models/enums/enum-response-result.model';
import {ResponseLogin} from '../models/responses/account/response-login.model';
import {RequestLogin} from '../models/requests/account/request-login.model';
import {ResponseData} from '../models/response-data.model';
import {ResponseClaim} from '../models/responses/account/response-claim.model';
import {ResponseList} from '../models/response-list.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
	// claim 캐시 이름
	private CACHE_CLAIM = 'claims';
	// 인증 관련 API
	private AUTH_URL = `${environment.apiUrl}/account`;

	// 로그인 여부 소스
	private _isLoginSource: BehaviorSubject<ResponseData> = null;
	// 로그인 여부 소스 변경 감시
	isLoginSource$: Observable<ResponseData> = null;

	// 로그인 정보 소스
	private _loginSource = new BehaviorSubject<ResponseLogin>(null);
	// 로그인 정보 소스 변경 감시
	loginSource$ = this._loginSource.asObservable();

	/**
	 * 생성자
	 * @param http
	 * @param sessionCacheService
	 */
	constructor(
		private http: HttpClient,
		private sessionCacheService: SessionCacheService,
	) {
		const initIsLogin: ResponseData = new ResponseData();
		initIsLogin.Result = EnumResponseResult.Warning;
		this._isLoginSource = new BehaviorSubject<ResponseData>(initIsLogin);
		this.isLoginSource$ = this._isLoginSource.asObservable();
	}

	/**
	 * 로그인 처리
	 * @param userInfo
	 */
	login(userInfo: RequestLogin): Observable<ResponseData<ResponseLogin>> {

		userInfo.RequireRoles = ['Supervisor', 'Admin', 'User'];

		return this.http.post<ResponseData<ResponseLogin>>(`${this.AUTH_URL}/login`, userInfo)
			.pipe(
				map(result => {
					if (result.Result === EnumResponseResult.Success) {
						this.setLogin();
						const responseData = new ResponseData();
						responseData.Result = EnumResponseResult.Success;
						this._isLoginSource.next(responseData);
					}
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 로그아웃 처리
	 */
	logout(): Observable<ResponseData> {
		return this.http.get<ResponseData>(`${this.AUTH_URL}/logout`)
			.pipe(
				map((result) => {
					this.setLogout();
					const responseData = new ResponseData();
					responseData.Result = EnumResponseResult.Error;
					this._isLoginSource.next(responseData);
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 로그인 여부 확인
	 */
	checkLogin(): Observable<ResponseData> {
		return this.http.get<ResponseData>(`${this.AUTH_URL}/checklogin`)
			.pipe(
				map((result) => {
					this._isLoginSource.next(result);
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 로그인 정보 가져오기
	 */
	getLogin(): Observable<ResponseData<ResponseLogin>> {
		return this.http.get<ResponseData<ResponseLogin>>(`${this.AUTH_URL}/login`)
			.pipe(
				map((result) => {
					this._loginSource.next(result.Data);
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 로그인한 사용자의 권한 가져오기
	 */
	getClaims(): Observable<ResponseList<ResponseClaim>> {
		return this.http.get<ResponseList<ResponseClaim>>(`${this.AUTH_URL}/claims`);
	}

	/**
	 * 권한 존재 여부 확인
	 * @param claimValue
	 */
	hasClaim(claimValue: string): Observable<boolean> {
		return this.http.get<ResponseData>(`${this.AUTH_URL}/claims/${claimValue}`)
			.pipe(
				map(() => {
					return true;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 권한 존재 여부 확인(캐시에서 확인)
	 * @param claimValue
	 */
	async hasClaimInChache(claimValue: string): Promise<boolean> {

		// 캐시 값이 있는 경우
		if (this.sessionCacheService.has(this.CACHE_CLAIM)) {
			return this.sessionCacheService.hasValue(this.CACHE_CLAIM, claimValue);
		}
		// 없는 경우 캐시에 저장
		else {
			const claims = await this.getClaims().toPromise();
			if (claims) {
				this.sessionCacheService.set(this.CACHE_CLAIM, claims.Data.Items.map(i => i.ClaimValue));
			}

			return this.sessionCacheService.hasValue(this.CACHE_CLAIM, claimValue);
		}
	}

	/**
	 * 권한 존재 여부 확인(캐시에서 확인)
	 * @param claimValues
	 */
	async hasClaimInChacheOneOf(claimValues: string[]): Promise<boolean> {

		let result: boolean = false;

		// 모든 권한에 대해서 처리
		for (let claimIndex = 0; claimIndex < claimValues.length; claimIndex++) {

			// 권한이 존재하는 경우
			if (await this.hasClaimInChache(claimValues[claimIndex])) {
				result = true;
				break;
			}
		}

		return result;
	}

	/**
	 * 권한 존재 여부 확인(캐시에서 확인)
	 * @param claimValues
	 */
	async getHasClaimsInChache(claimValues: string[]): Promise<{ [id: string]: boolean }> {

		const result: { [id: string]: boolean } = {};

		// 모든 권한에 대해서 처리
		for (let claimIndex = 0; claimIndex < claimValues.length; claimIndex++) {

			// 권한이 존재하는 경우
			if (await this.hasClaimInChache(claimValues[claimIndex])) {
				result[claimValues[claimIndex]] = true;
			}
		}

		return result;
	}

	// 로그인 후 처리
	private async setLogin(): Promise<void> {
		// 기존 권한 삭제 후 새로 입력
		this.sessionCacheService.remove(this.CACHE_CLAIM);

		const claims = await this.getClaims().toPromise();
		if (claims) {
			this.sessionCacheService.set(this.CACHE_CLAIM, claims.Data.Items.map(i => i.ClaimValue));
		}
	}

	// 로그아웃 후 처리
	private setLogout(): void {
		// 권한 세션 캐시 삭제
		this.sessionCacheService.remove(this.CACHE_CLAIM);
	}
}
