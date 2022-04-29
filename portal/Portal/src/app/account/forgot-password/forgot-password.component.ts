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
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import {RequestForgetPassword} from '../../shared/models/requests/account/request-forget-password.model';
import {MessageNotificationService} from '../../shared/services/message-notification.service';
import {EnumResponseResult} from '../../shared/models/enums/enum-response-result.model';
import {AccountsService} from '../../shared/services/data-providers/accounts.service';

@Component({
	selector: 'app-forgot-password',
	templateUrl: './forgot-password.component.html',
	styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent implements OnInit {
	/**
	 * 통신중 플래그
	 */
	public inCommunication = false;
	/**
	 * 비밀번호 찾기 정보
	 */
	requestForgotPassword: RequestForgetPassword = new RequestForgetPassword();

	/**
	 * 생성자
	 * @param router 라우터 객체
	 * @param messageService 메시지 알림 서비스 객체
	 * @param accountService 계정 서비스 객체
	 */
	constructor(
		private router: Router,
		private messageService: MessageNotificationService,
		private accountService: AccountsService
	) {
	}

	/**
	 * 초기화 이벤트
	 */
	ngOnInit(): void {
	}

	/**
	 * 비밀번호 찾기
	 */
	onSubmit(): void {

		this.requestForgotPassword.Protocol = window.location.protocol.replace(':', '');
		this.requestForgotPassword.Host = window.location.host;

		this.accountService.forgotPassword(this.requestForgotPassword)
		.pipe(
			finalize(() => {
				this.inCommunication = false;
			})
		)
		.subscribe(
			(result) => {
				if (result.Result === EnumResponseResult.Success) {
					this.messageService.success(result.Message);
					this.router.navigate(['/account/login']);
				}
				else
					this.messageService.error('[' + result.Code + '] ' + result.Message);
			},
			(err: HttpErrorResponse) => {
				this.messageService.error(err.error);
			}
		);
	}

	/**
	 * 취소 클릭 시 이벤트
	 */
	onCancel(): void {
		this.router.navigate(['/account/login']);
	}
}
