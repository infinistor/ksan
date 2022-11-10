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
using System.ComponentModel.DataAnnotations;
using PortalResources;
using MTLib.CommonData;

namespace PortalData.Requests.Accounts
{
	/// <summary>가입 요청 정보 클래스</summary>
	public class RequestRegister : CommonRequestData
	{
		/// <summary>프로토콜</summary>
		public string Protocol { get; set; } = "";

		/// <summary>호스트</summary>
		public string Host { get; set; } = "";

		/// <summary>로그인 아이디</summary>
		[Required(ErrorMessageResourceName = "EM_COMMON_ACCOUNT_REQUIRE_LOGIN_ID", ErrorMessageResourceType = typeof(Resource))]
		public string LoginId { get; set; } = "";

		/// <summary>이메일 주소</summary>
		[Required(ErrorMessageResourceName = "EM_COMMON_ACCOUNT_REQUIRE_EMAIL", ErrorMessageResourceType = typeof(Resource))]
		[EmailAddress(ErrorMessageResourceName = "EM_COMMON_ACCOUNT_INVALID_EMAIL", ErrorMessageResourceType = typeof(Resource))]
		public string Email { get; set; } = "";

		/// <summary>사용자명</summary>
		[Required(ErrorMessageResourceName = "EM_COMMON_ACCOUNT_REQUIRE_NAME", ErrorMessageResourceType = typeof(Resource))]
		public string Name { get; set; } = "";

		/// <summary>비밀번호</summary>
		[Required(ErrorMessageResourceName = "EM_COMMON_ACCOUNT_REQUIRE_PASSWORD", ErrorMessageResourceType = typeof(Resource))]
		[DataType(DataType.Password, ErrorMessageResourceName = "EM_COMMON_ACCOUNT_INVALID_PASSWORD", ErrorMessageResourceType = typeof(Resource))]
		public string Password { get; set; } = "";

		/// <summary>확인 비밀번호</summary>
		[Required(ErrorMessageResourceName = "EM_COMMON_ACCOUNT_REQUIRE_CONFIRM_PASSWORD", ErrorMessageResourceType = typeof(Resource))]
		[DataType(DataType.Password, ErrorMessageResourceName = "EM_COMMON_ACCOUNT_INVALID_PASSWORD", ErrorMessageResourceType = typeof(Resource))]
		[Compare("Password", ErrorMessageResourceName = "EM_COMMON_ACCOUNT_PASSWORD_AND_CONFIRM_PASSWORD_DO_NOT_MATCH", ErrorMessageResourceType = typeof(Resource))]
		public string ConfirmPassword { get; set; } = "";

		/// <summary>전화번호</summary>
		//[Required(ErrorMessageResourceName = "EM_COMMON_ACCOUNT_REQUIRE_PHONE_NUMBER", ErrorMessageResourceType = typeof(PortalResources.Resource))]
		//[Phone(ErrorMessageResourceName = "EM_COMMON_ACCOUNT_INVALID_PHONE_NUMBER", ErrorMessageResourceType = typeof(PortalResources.Resource))]
		public string PhoneNumber { get; set; } = "";

		/// <summary>마케팅활용 동의 여부</summary>
		public bool IsAgreeMarketing { get; set; } = false;
	}
}
