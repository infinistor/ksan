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
import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import {RequestLogin} from '../../shared/models/requests/account/request-login.model';
import {MessageNotificationService} from '../../shared/services/message-notification.service';
import {AuthService} from '../../shared/services/auth.service';
import {LocalCacheService} from '../../shared/services/cache/cache.service';
import {EnumResponseResult} from '../../shared/models/enums/enum-response-result.model';


@Component({
	selector: 'app-login',
	templateUrl: './login.component.html',
	styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

	// 통신중 플래그
	public inCommunication = false;
	// 로그인 정보
	requestLogin: RequestLogin = new RequestLogin();
	// 로그인 후 이동할 URL
	public returnUrl = '';

	/**
	 * 생성자
	 * @param route 라우트 객체
	 * @param router 라우터 객체
	 * @param messageService 메시지 서비스 객체
	 * @param authService 인증 서비스 객체
	 * @param localCacheService 로컬 캐시 서비스 객체
	 */
	constructor(
		private route: ActivatedRoute,
		private router: Router,
		private messageService: MessageNotificationService,
		private authService: AuthService,
		private localCacheService: LocalCacheService,
	) {
		const returnUrl = decodeURI(this.route.snapshot.queryParams.returnUrl || '/');
		this.returnUrl = decodeURI(returnUrl);
	}

	/**
	 * 초기화 이벤트
	 */
	ngOnInit(): void {

		// 아이디 저장 설정을 가져온다.
		if (this.localCacheService.has('rememberMe'))
			this.requestLogin.RememberMe = this.localCacheService.get('rememberMe');

		// 아이디 저장으로 설정되어 있는 경우
		if (this.requestLogin.RememberMe && this.localCacheService.has('loginId'))
			this.requestLogin.LoginId = this.localCacheService.get('loginId');
	}

	/**
	 * 로그인
	 */
	onSubmit(): void {

		this.authService.login(this.requestLogin)
		.pipe(
			finalize(() => {
				this.inCommunication = false;
			})
		)
		.subscribe(
			(result) => {

				if (result.Result === EnumResponseResult.Success) {

					// 아이디 저장으로 설정되어 있는 경우
					if (this.requestLogin.RememberMe) {
						// 아이디 저장 및 아이디 저장
						this.localCacheService.set('rememberMe', this.requestLogin.RememberMe);
						this.localCacheService.set('loginId', this.requestLogin.LoginId);
					}
					else {
						// 아이디 저장 및 아이디 저장
						this.localCacheService.remove('rememberMe');
						this.localCacheService.remove('loginId');
					}

					// 이동할 URL이 없는 경우
					if (!this.returnUrl || this.returnUrl === '/' || this.returnUrl.startsWith('/dashboard'))
					{
						if (result.Data.ProductType === 'monster')
							this.router.navigateByUrl('/dashboard/monster');
						else
							this.router.navigateByUrl(this.returnUrl);
					}
					// 이동할 URL이 존재하는 경우
					else {
						// URL 이동
						this.router.navigateByUrl(this.returnUrl);
					}
				}
				else
					this.messageService.error('[' + result.Code + '] ' + result.Message);
			},
			(err: HttpErrorResponse) => {
				this.messageService.error(err.error);
			}
		);
	}
}
