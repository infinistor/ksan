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
using System;
using PortalData.Enums;
using MTLib.Core;

namespace PortalData.Responses.Accounts
{
	/// <summary>사용자 정보 응답 클래스</summary>
	public class ResponseUser
	{
		/// <summary>사용자 식별자</summary>
		public string Id { get; set; } = "";

		/// <summary>로그인 아이디</summary>
		public string LoginId { get; set; } = "";

		/// <summary>이메일 주소</summary>
		public string Email { get; set; } = "";

		/// <summary>사용자명</summary>
		public string Name { get; set; } = "";

		/// <summary>화면 표시명</summary>
		public string DisplayName
		{
			get
			{
				string Result = "";
				try
				{
					Result = UserExtension.GetDisplayName("", Email, Name);
				}
				catch (Exception ex)
				{
					NNException.Log(ex);
				}
				return Result;
			}
		}

		/// <summary>사번 등 식별 코드</summary>
		public string Code { get; set; } = "";

		/// <summary>사용자 계정 상태</summary>
		public EnumUserStatus Status { get; set; } = EnumUserStatus.Locked;

		/// <summary>가입 일시</summary>
		public DateTime JoinDate { get; set; } = DateTime.Now;

		/// <summary>탈퇴 일시</summary>
		public DateTime? WithdrawDate { get; set; } = null;

		/// <summary>로그인 횟수</summary>
		public int LoginCount { get; set; } = 0;
	}
}
