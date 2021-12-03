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
import { Router, ActivatedRoute } from '@angular/router';
import { AccountsService } from '../../shared/services/data-providers/accounts.service';
import { finalize } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import {RequestResetPassword} from '../../shared/models/requests/account/request-reset-password.model';
import {MessageNotificationService} from '../../shared/services/message-notification.service';
import {EnumResponseResult} from '../../shared/models/enums/enum-response-result.model';


@Component({
	selector: 'app-reset-password',
	templateUrl: './reset-password.component.html',
	styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {
	/**
	 * 통신중 플래그
	 */
	public inCommunication = false;
	/**
	 *  인증 완료
	 */
	public isDone = false;
	/**
	 * 인증 에러
	 */
	public errorMessage = '';
	/**
	 * 비밀번호 재설정 정보
	 */
	requestResetPassword: RequestResetPassword = new RequestResetPassword();

	/**
	 * 생성자
	 * @param route 라우트 객체
	 * @param router 라우터 객체
	 * @param translateService 번역 서비스 객체
	 * @param messageService 메시지 알림 서비스 객체
	 * @param accountsService 계정 서비스 객체
	 */
	constructor(
		private route: ActivatedRoute,
		private router: Router,
		private translateService: TranslateService,
		private messageService: MessageNotificationService,
		private accountsService: AccountsService
	) {
	}

	/**
	 * 초기화 이벤트
	 */
	ngOnInit(): void {
		// 아이디와 코드 가져오기
		const userId = this.route.snapshot.queryParams.userId || '';
		const code = this.route.snapshot.queryParams.code || '';

		// 이메일이 존재하지 않는 경우
		if (userId.isEmpty()) {
			this.isDone = false;
			this.errorMessage = this.translateService.instant('EM_COMMON_ACCOUNT_RESET_PASSWORD_REQUIRE_EMAIL');
		}
		// 인증코드가 존재하지 않는 경우
		else if (code.isEmpty()) {
			this.isDone = false;
			this.errorMessage = this.translateService.instant('EM_COMMON_ACCOUNT_RESET_PASSWORD_REQUIRE_CODE');
		}
		// 이메일, 인증코드가 모두 유효한 경우
		else {
			this.requestResetPassword.LoginId = userId;
			this.requestResetPassword.Code = code;
			this.isDone = true;
		}
	}

	/**
	 * 비밀번호 초기화
	 */
	onSubmit(): void {

		// 비밀번호 초기화를 요청한다.
		this.accountsService.resetPassword(new RequestResetPassword(this.requestResetPassword.LoginId, this.requestResetPassword.Code, this.requestResetPassword.NewPassword, this.requestResetPassword.NewConfirmPassword))
			.pipe(
				finalize(() => {
					this.inCommunication = false;
				})
			)
			.subscribe(
				(result) => {
					// 결과가 성공인 경우
					if (result.Result === EnumResponseResult.Success) {
						this.messageService.success(result.Message);
						this.router.navigate(['/account/login']);
					}
					// 결과가 실패인 경우
					else {
						this.messageService.error('[' + result.Code + '] ' + result.Message);
					}
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
