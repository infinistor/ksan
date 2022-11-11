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
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;

namespace PortalData.Enums
{
	/// <summary>서버 상태</summary>
	[JsonConverter(typeof(StringEnumConverter))]
	public enum EnumServerState
	{
		/// <summary>알수없음</summary>
		[Display(Name = "UL_SERVERS_SERVER_STATE_UNKNOWN", Description = "UL_SERVERS_SERVER_STATE_UNKNOWN", GroupName = "UL_SERVERS_SERVER_STATE", ShortName = "UL_SERVERS_SERVER_STATE_UNKNOWN", Prompt = "UL_SERVERS_SERVER_STATE_UNKNOWN", ResourceType = typeof(Resource))]
		Unknown = -2,
		/// <summary>타임아웃</summary>
		[Display(Name = "UL_SERVERS_SERVER_STATE_TIMEOUT", Description = "UL_SERVERS_SERVER_STATE_TIMEOUT", GroupName = "UL_SERVERS_SERVER_STATE", ShortName = "UL_SERVERS_SERVER_STATE_TIMEOUT", Prompt = "UL_SERVERS_SERVER_STATE_TIMEOUT", ResourceType = typeof(Resource))]
		Timeout = -1,
		/// <summary>오프라인</summary>
		[Display(Name = "UL_SERVERS_SERVER_STATE_OFFLINE", Description = "UL_SERVERS_SERVER_STATE_OFFLINE", GroupName = "UL_SERVERS_SERVER_STATE", ShortName = "UL_SERVERS_SERVER_STATE_OFFLINE", Prompt = "UL_SERVERS_SERVER_STATE_OFFLINE", ResourceType = typeof(Resource))]
		Offline = 0,
		/// <summary>온라인</summary>
		[Display(Name = "UL_SERVERS_SERVER_STATE_ONLINE", Description = "UL_SERVERS_SERVER_STATE_ONLINE", GroupName = "UL_SERVERS_SERVER_STATE", ShortName = "UL_SERVERS_SERVER_STATE_ONLINE", Prompt = "UL_SERVERS_SERVER_STATE_ONLINE", ResourceType = typeof(Resource))]
		Online = 1
	}
}