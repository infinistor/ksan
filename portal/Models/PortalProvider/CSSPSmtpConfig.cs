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
using PortalData.Configs;
using MTLib.AspNetCore;

namespace PortalProvider
{
	/// <summary>STMP 설정</summary>
	public class CSSPSmtpConfig : SmtpConfig
	{
		/// <summary>SMTP 호스트</summary>
		[KeyAndDefaultValue(Key = "SMTP.HOST", DefaultValue = "127.0.0.1")]
		public override string Host { get; set; } = "127.0.0.1";

		/// <summary>SMTP 포트</summary>
		[KeyAndDefaultValue(Key = "SMTP.PORT", DefaultValue = "25")]
		public override int Port { get; set; } = 25;

		/// <summary>SMTP 계정</summary>
		[KeyAndDefaultValue(Key = "SMTP.ACCOUNT", DefaultValue = "no-reply")]
		public override string Account { get; set; } = "no-reply";

		/// <summary>SMTP 비밀번호</summary>
		[KeyAndDefaultValue(Key = "SMTP.PASSWORD", DefaultValue = "")]
		public override string Password { get; set; } = "";

		/// <summary>SMTP SSL 사용여부</summary>
		[KeyAndDefaultValue(Key = "SMTP.ENABLE_SSL", DefaultValue = "false")]
		public override bool UseSsl { get; set; } = false;

		/// <summary>SMTP 발송자 이메일</summary>
		[KeyAndDefaultValue(Key = "SMTP.DEFAULT_SENDER_EMAIL", DefaultValue = "support@papace.co.kr")]
		public override string DefaultSenderEmail { get; set; } = "support@papace.co.kr";

		/// <summary>SMTP 발송자명</summary>
		[KeyAndDefaultValue(Key = "SMTP.DEFAULT_SENDER_NAME", DefaultValue = "PSPACE CSSP")]
		public override string DefaultSenderName { get; set; } = "PSPACE CSSP";
	}
}
