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
using System.ComponentModel.DataAnnotations;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;

namespace PortalData.Enums
{
	/// <summary>사용자 계정 상태</summary>
	[JsonConverter(typeof(StringEnumConverter))]
	public enum EnumUserStatus
	{
		/// <summary>잠김</summary>
		[Display(Name = "UL_COMMON_ACCOUNT_USER_STATUS_LOCKED", Description = "UL_COMMON_ACCOUNT_USER_STATUS_LOCKED",
			GroupName = "UL_COMMON_ACCOUNT_USER_STATUS", ShortName = "UL_COMMON_ACCOUNT_USER_STATUS_LOCKED",
			Prompt = "UL_COMMON_ACCOUNT_USER_STATUS_LOCKED", ResourceType = typeof(PortalResources.Resource))]
		Locked,
		/// <summary>이메일 확인 중</summary>
		[Display(Name = "UL_COMMON_ACCOUNT_USER_STATUS_VERIFYING_EMAIL",
			Description = "UL_COMMON_ACCOUNT_USER_STATUS_VERIFYING_EMAIL", GroupName = "UL_COMMON_ACCOUNT_USER_STATUS",
			ShortName = "UL_COMMON_ACCOUNT_USER_STATUS_VERIFYING_EMAIL",
			Prompt = "UL_COMMON_ACCOUNT_USER_STATUS_VERIFYING_EMAIL",
			ResourceType = typeof(PortalResources.Resource))]
		VerifyingEmail,
		/// <summary>활성화</summary>
		[Display(Name = "UL_COMMON_ACCOUNT_USER_STATUS_ACTIVED", Description = "UL_COMMON_ACCOUNT_USER_STATUS_ACTIVED",
			GroupName = "UL_COMMON_ACCOUNT_USER_STATUS", ShortName = "UL_COMMON_ACCOUNT_USER_STATUS_ACTIVED",
			Prompt = "UL_COMMON_ACCOUNT_USER_STATUS_ACTIVED", ResourceType = typeof(PortalResources.Resource))]
		Activated
	}
}
