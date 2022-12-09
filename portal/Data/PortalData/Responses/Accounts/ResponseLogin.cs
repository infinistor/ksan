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
using System;
using System.Collections.Generic;

namespace PortalData.Responses.Accounts
{
	/// <summary>로그인 응답 클래스</summary>
	public class ResponseLogin
	{
		/// <summary>사용자 식별자</summary>
		public string Id { get; set; } = "";

		/// <summary>로그인 사용자 아이디</summary>
		public string LoginId { get; set; } = "";

		/// <summary>이메일 주소</summary>
		public string Email { get; set; } = "";

		/// <summary>사용자명</summary>
		public string Name { get; set; } = "";

		/// <summary>화면 표시명</summary>
		public string DisplayName { get; set; } = "";

		/// <summary>비밀번호 변경일자</summary>
		public DateTime? PasswordChangeDate { get; set; } = null;

		/// <summary>전화번호</summary>
		public string PhoneNumber { get; set; } = "";

		/// <summary>SMS 수신 여부</summary>
		public bool ReceiveSms { get; set; } = true;

		/// <summary>이메일 수신 여부</summary>
		public bool ReceiveEmail { get; set; } = true;

		/// <summary>역할 목록</summary>
		public List<string> Roles { get; set; } = new List<string>();

		/// <summary>마지막 로그인 일시</summary>
		public DateTime? LastLoginDateTIme { get; set; } = null;

		/// <summary>제품 타입</summary>
		public string ProductType { get; set; } = "";
	}
}
