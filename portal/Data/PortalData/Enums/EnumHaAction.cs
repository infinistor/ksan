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
	/// <summary>동기화 작업 구분</summary>
	[JsonConverter(typeof(StringEnumConverter))]
	public enum EnumHaAction
	{
		/// <summary>미설정 혹은 알수 없음</summary>
		[Display(Name = "UL_COMMON__HA_ACTION_UNKNOWN", Description = "UL_COMMON__HA_ACTION_UNKNOWN", GroupName = "UL_COMMON__HA_ACTION", ShortName = "UL_COMMON__HA_ACTION_UNKNOWN", Prompt = "UL_COMMON__HA_ACTION_UNKNOWN", ResourceType = typeof(PortalResources.Resource))]
		Unknown = -1,
		/// <summary>초기화</summary>
		[Display(Name = "UL_COMMON__HA_ACTION_INIT", Description = "UL_COMMON__HA_ACTION_INIT", GroupName = "UL_COMMON__HA_ACTION", ShortName = "UL_COMMON__HA_ACTION_INIT", Prompt = "UL_COMMON__HA_ACTION_INIT", ResourceType = typeof(PortalResources.Resource))]
		Initializing,
		/// <summary>Standby 상태</summary>
		[Display(Name = "UL_COMMON__HA_ACTION_STANDBY", Description = "UL_COMMON__HA_ACTION_STANDBY", GroupName = "UL_COMMON__HA_ACTION", ShortName = "UL_COMMON__HA_ACTION_STANDBY", Prompt = "UL_COMMON__HA_ACTION_STANDBY", ResourceType = typeof(PortalResources.Resource))]
		Standby,
		/// <summary>Active 상태</summary>
		[Display(Name = "UL_COMMON__HA_ACTION_ACTIVE", Description = "UL_COMMON__HA_ACTION_ACTIVE", GroupName = "UL_COMMON__HA_ACTION", ShortName = "UL_COMMON__HA_ACTION_ACTIVE", Prompt = "UL_COMMON__HA_ACTION_ACTIVE", ResourceType = typeof(PortalResources.Resource))]
		Active,
		/// <summary>데이터 동기화 중 (Active->Standby)</summary>
		[Display(Name = "UL_COMMON__HA_ACTION_SYNC", Description = "UL_COMMON__HA_ACTION_SYNC", GroupName = "UL_COMMON__HA_ACTION", ShortName = "UL_COMMON__HA_ACTION_SYNC", Prompt = "UL_COMMON__HA_ACTION_SYNC", ResourceType = typeof(PortalResources.Resource))]
		Sync
	}
}