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
import {Component, OnDestroy, OnInit, TemplateRef} from '@angular/core';
import {BreadCrumbCollapseMode, BreadCrumbItem} from '@progress/kendo-angular-navigation';
import {Subscription} from 'rxjs';
import {Router} from '@angular/router';
import {MessageNotificationService} from './shared/services/message-notification.service';
import {CommonDialogService} from './shared/services/common-dialog.service';
import {AuthService} from './shared/services/auth.service';
import {AccountsService} from './shared/services/data-providers/accounts.service';
import {EnumResponseResult} from './shared/models/enums/enum-response-result.model';
import {ResponseLogin} from './shared/models/responses/account/response-login.model';
import {TranslateService} from '@ngx-translate/core';
import {DialogRef} from '@progress/kendo-angular-dialog';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {equalToValidator} from './shared/modules/validators-equal-to';
import {RequestChangePassword} from './shared/models/requests/account/request-change-password.model';
import {finalize} from 'rxjs/operators';
import {HttpErrorResponse} from '@angular/common/http';

@Component({
	selector: 'app-root',
	templateUrl: './app.component.html',
	styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {

	kendokaAvatar: string = 'https://www.telerik.com/kendo-angular-ui-develop/components/navigation/appbar/assets/kendoka-angular.png';

	/**
	 * 통신중 플래그
	 */
	inCommunication: boolean = false;

	/**
	 * 로그인 여부 구독 객체
	 */
	private isLoginSubscription: Subscription = null;

	/**
	 * 로그인 사용자 정보
	 */
	loginInfo: ResponseLogin = null;

	/**
	 * BreadCrumb 목록
	 */
	breadCrumbItems: BreadCrumbItem[] = [];
	breadCrumbCollapseMode: BreadCrumbCollapseMode = 'auto';
	breadCrumbWidth: number = 100;

	/**
	 * 라우트 데이터
	 */
	private routesData: Subscription;

	/**
	 * 프로필 메뉴 목록
	 */
	profileMenuItems: {text: string, value: string}[] = [];

	/**
	 * 표시되고 있는 다이얼로그 객체
	 */
	dialog: DialogRef = null;

	/**
	 * 비밀번호 변경 폼
	 */
	formGroupChangePassword: FormGroup = new FormGroup(
		{
			Password: new FormControl('', [Validators.required, Validators.minLength(6)]),
			NewPassword: new FormControl('', [Validators.required, Validators.minLength(6)]),
			NewConfirmPassword: new FormControl('', [Validators.required, Validators.minLength(6)]),
		}, equalToValidator('NewPassword', 'NewConfirmPassword')
	);

	/**
	 * 생성자
	 * @param router 라우터 객체
	 * @param messageService 메시지 알림 서비스 객체
	 * @param commonDialogService 다이얼로그 서비스 객체
	 * @param authService 인증 서비스 객체
	 * @param accountService 계정 서비스 객체
	 * @param translateService 번역 서비스 객체
	 * @param formBuilder 폼 빌더 객체
	 */
	constructor(
		private router: Router,
		private messageService: MessageNotificationService,
		private commonDialogService: CommonDialogService,
		private authService: AuthService,
		private accountService: AccountsService,
		private translateService: TranslateService,
		private formBuilder: FormBuilder,
	) {
		this.initRoutes();

		this.profileMenuItems.push({ text: 'UL_COMMON_ACCOUNT_CHANGE_PASSWORD', value: 'password-change'});
		// this.profileMenuItems.push({ text: 'UL_COMMON_APIKEY_MANAGEMENT', value: 'manage-api-key'});
		this.profileMenuItems.push({ text: 'UL_COMMON_ACCOUNT_LOGOUT', value: 'logout'});
	}

	/**
	 * 초기화 이벤트
	 */
	ngOnInit(): void {

		// 로그인 여부 구독 설정
		this.isLoginSubscription = this.authService.isLoginSource$
			.subscribe((item) => {

				if (item === null) return;
				if (item.Result === EnumResponseResult.Success) {
					// 로그인 사용자 정보를 가져온다.
					this.authService.getLogin()
						.subscribe(async (response) => {

							if (response.Result === EnumResponseResult.Success)
								this.loginInfo = response.Data;
							else
								this.loginInfo = null;
						});
				} else if (item.Result === EnumResponseResult.Error) {
					this.loginInfo = null;
					this.router.navigate(['/account/login']);
				}
			});
	}

	/**
	 * 해제 이벤트
	 */
	ngOnDestroy(): void {
	}

	/**
	 * BreadCrumb 아이템 클릭 시 발생하는 이벤트
	 * @param item 클릭한 아이템 객체
	 */
	async onBreadCrumbItemClick(item: BreadCrumbItem): Promise<void> {

		const selectedItemIndex = this.breadCrumbItems.findIndex(i => i.text === item.text);
		console.log(selectedItemIndex);
		const url: string[] = this.breadCrumbItems.slice(1, selectedItemIndex + 1).map(i => i.text.toLowerCase());
		console.log(url);

		if (url.length === 0) {
			await this.router.navigate(['/']);
		} else {
			await this.router.navigate(url);
		}
	}

	/**
	 * 라우트 초기화
	 */
	private initRoutes(): void {
		this.routesData = this.router.events.subscribe(() => {
			// Exclude query parameters from URL
			const route = this.router.url;
			this.breadCrumbItems = route
				.substring(0, route.indexOf('?') !== -1 ? route.indexOf('?') : route.length)
				.split('/')
				.filter(Boolean)
				.map((segment) => {
					return {
						text: segment.charAt(0).toUpperCase() + segment.slice(1),
						title: segment
					};
				});

			this.breadCrumbItems = [
				{text: 'Item1'},
				{text: 'Item2'},
				{text: 'Item3'},
				{text: 'Item4'},
				{text: 'Item5'},
				{text: 'Item6'},
				{text: 'Item7'},
				{text: 'Item8'},
				{text: 'Item9'}

				// {
				// 	text: 'Home',
				// 	title: '11111',
				// 	// icon: 'home'
				// },
				// {
				// 	text: 'Test1',
				// 	title: 'test1',
				// 	// icon: 'home'
				// },
				// {
				// 	text: 'Test2',
				// 	title: 'test2',
				// 	// icon: 'home'
				// },
				// {
				// 	text: 'Test3',
				// 	title: 'test3',
				// 	// icon: 'home'
				// },
				// {
				// 	text: 'Test4',
				// 	title: 'test4',
				// 	// icon: 'home'
				// },
				// {
				// 	text: 'Test5',
				// 	title: 'test5',
				// 	// icon: 'home'
				// },
				// {
				// 	text: 'Test6',
				// 	title: 'test6',
				// 	// icon: 'home'
				// },
			];
		});
	}

	/**
	 * 프로필 메뉴 클릭 시 이벤트
	 * @param item 클릭한 메뉴명
	 */
	onClickProfileMenu(item: string): void {
		switch (item) {
			case 'manage-api-key':
				break;
			case 'logout':
				this.authService.logout()
					.subscribe(() => {
					});
				break;
		}
	}

	/**
	 * 비밀번호 변경 다이얼로그 표시
	 * @param title 다이얼로그 제목
	 * @param content 다이얼로그 내용 템플릿
	 * @param actions 다이얼로그 버튼 템플릿
	 */
	showChangePassword(title: string, content: TemplateRef<any>, actions: TemplateRef<any>): void {
		// 폼 초기화
		this.formGroupChangePassword.reset();
		this.formGroupChangePassword.controls.Password.setValue('');
		this.formGroupChangePassword.controls.NewPassword.setValue('');
		this.formGroupChangePassword.controls.NewConfirmPassword.setValue('');

		// 문자열 리소스를 제목으로 변환한다.
		title = this.translateService.instant(title);

		// 다이얼로그 표시
		this.dialog = this.commonDialogService.show({
			title,
			content,
			actionButtons: actions,
			enableClose: false,
			autoFocusedElement: 'Password',
			width: 600
		});
	}

	/**
	 * 비밀번호를 변경한다.
	 */
	changePassword(): void {

		// 비밀번호 입력이 유효한 경우
		if (this.formGroupChangePassword.valid) {

			this.inCommunication = true;

			// 값을 복사한다.
			const value: RequestChangePassword = Object.assign(new RequestChangePassword(), this.formGroupChangePassword.getRawValue());

			// 비밀번호를 변경한다.
			this.accountService.changePassword(value)
				.pipe(
					finalize(() => {
						this.inCommunication = false;
					})
				)
				.subscribe((response) => {
					// 비밀번호 변경에 성공한 경우
					if (response.Result === EnumResponseResult.Success) {
						// 성공 메시지가 존재하는 경우 출력
						if (response.Message)
							this.messageService.info(response.Message);

						// 다이얼로그를 닫는다.
						this.dialog.close();
					}
					else
						this.messageService.error('[' + response.Code + '] ' + response.Message);
				},
				(err: HttpErrorResponse) => {
					this.messageService.error(err.error);
				}
			);
		}
	}
}
