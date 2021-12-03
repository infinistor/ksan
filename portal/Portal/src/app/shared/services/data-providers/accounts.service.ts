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

import { Observable, throwError} from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ResponseData } from '../../models/response-data.model';
import { RequestRegister } from '../../models/requests/account/request-register.model';
import { RequestConfirmEmail } from '../../models/requests/account/request-confirm-email.model';
import { RequestForgetPassword } from '../../models/requests/account/request-forget-password.model';
import { RequestResetPassword } from '../../models/requests/account/request-reset-password.model';
import { RequestChangePassword } from '../../models/requests/account/request-change-password.model';

@Injectable()

export class AccountsService {
	// 회원 정보 URL
	private ACCOUNT_URL = `${environment.apiUrl}/Account`;

	// 생성자
	constructor(
		private httpClient: HttpClient
	) {
	}

	/**
	 * 회원 가입
	 */
	register(request: RequestRegister): Observable<ResponseData> {
		return this.httpClient.post<ResponseData>(`${this.ACCOUNT_URL}/Register`, request)
			.pipe(
				map((result) => {
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 이메일 인증
	 */
	confirmEmail(request: RequestConfirmEmail): Observable<ResponseData> {
		return this.httpClient.post<ResponseData>(`${this.ACCOUNT_URL}/ConfirmEmail`, request)
			.pipe(
				map((result) => {
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 비밀번호 찾기
	 */
	forgotPassword(request: RequestForgetPassword): Observable<ResponseData> {
		return this.httpClient.post<ResponseData>(`${this.ACCOUNT_URL}/ForgotPassword`, request)
			.pipe(
				map((result) => {
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 비밀번호 재설정
	 */
	resetPassword(request: RequestResetPassword): Observable<ResponseData> {
		return this.httpClient.post<ResponseData>(`${this.ACCOUNT_URL}/ResetPassword`, request)
			.pipe(
				map((result) => {
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 비밀번호 변경
	 */
	changePassword(request: RequestChangePassword): Observable<ResponseData> {
		return this.httpClient.post<ResponseData>(`${this.ACCOUNT_URL}/ChangePassword`, request)
			.pipe(
				map((result) => {
					return result;
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}
}
