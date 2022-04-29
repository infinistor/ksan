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
import { NotificationService } from '@progress/kendo-angular-notification';

@Injectable({ providedIn: 'root' })
export class MessageNotificationService {

	// 알람 서비스
	private _notificationService: NotificationService = null;

	constructor(notificationService: NotificationService) {
		this._notificationService = notificationService;
	}

	// Shortcut methods
	success(content: string, hideAfter: number = 3): any {
		return this.create('success', content, hideAfter);
	}

	error(content: string, hideAfter: number = 6): any {
		return this.create('error', content, hideAfter);
	}

	info(content: string, hideAfter: number = 3): any {
		return this.create('info', content, hideAfter);
	}

	warning(content: string, hideAfter: number = 3): any {
		return this.create('warning', content, hideAfter);
	}

	create(type: 'none' | 'success' | 'warning' | 'error' | 'info', content: string, hideAfter: number = 0): any {
		this._notificationService.show({
			animation: { type: 'fade', duration: 800 },
			type: { style: type, icon: true },
			content,
			position: { horizontal: 'center', vertical: 'top' },
			hideAfter: hideAfter * 1000,
			closable: hideAfter === 0 ? true : false
		});
	}
}
