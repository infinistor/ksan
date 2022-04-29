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

import { EnumUserStatus } from 'src/app/_model/enums/enum-user-status.model';

export class ResponseUser {
	public Id: string;
	public Email: string;
	public Name: string;
	public DisplayName: string;
	public Code: string;
	public Status: EnumUserStatus;
	public JoinDate: Date;
	public WithdrawDate: Date;
	public LoginCount: number;

constructor(Id?: string, Email?: string, Name?: string, DisplayName?: string, Code?: string, Status?: EnumUserStatus, JoinDate?: Date, WithdrawDate?: Date, LoginCount?: number) {
			
		this.Id = Id;
		this.Email = Email;
		this.Name = Name;
		this.DisplayName = DisplayName;
		this.Code = Code;
		this.Status = Status;
		this.JoinDate = JoinDate;
		this.WithdrawDate = WithdrawDate;
		this.LoginCount = LoginCount;
	}
}


