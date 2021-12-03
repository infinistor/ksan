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
using PortalResources;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;

namespace PortalData.Enums
{
	/// <summary>로그 레벨</summary>
	[JsonConverter(typeof(StringEnumConverter))]
	public enum EnumLogLevel : short
	{
		/// <summary>디버그</summary>
		[Display(Name = "UL_COMMON__LOG_LEVEL_DEBUG", Description = "UL_COMMON__LOG_LEVEL_DEBUG", GroupName = "UL_COMMON__LOG_LEVEL", ShortName = "UL_COMMON__LOG_LEVEL_DEBUG", Prompt = "UL_COMMON__LOG_LEVEL_DEBUG", ResourceType = typeof(Resource))]
		Debug,
		/// <summary>정보</summary>
		[Display(Name = "UL_COMMON__LOG_LEVEL_INFORMATION", Description = "UL_COMMON__LOG_LEVEL_INFORMATION", GroupName = "UL_COMMON__LOG_LEVEL", ShortName = "UL_COMMON__LOG_LEVEL_INFORMATION", Prompt = "UL_COMMON__LOG_LEVEL_INFORMATION", ResourceType = typeof(Resource))]
		Information,
		/// <summary>경고</summary>
		[Display(Name = "UL_COMMON__LOG_LEVEL_WARNING", Description = "UL_COMMON__LOG_LEVEL_WARNING", GroupName = "UL_COMMON__LOG_LEVEL", ShortName = "UL_COMMON__LOG_LEVEL_WARNING", Prompt = "UL_COMMON__LOG_LEVEL_WARNING", ResourceType = typeof(Resource))]
		Warning,
		/// <summary>에러</summary>
		[Display(Name = "UL_COMMON__LOG_LEVEL_ERROR", Description = "UL_COMMON__LOG_LEVEL_ERROR", GroupName = "UL_COMMON__LOG_LEVEL", ShortName = "UL_COMMON__LOG_LEVEL_ERROR", Prompt = "UL_COMMON__LOG_LEVEL_ERROR", ResourceType = typeof(Resource))]
		Error
	}
}
