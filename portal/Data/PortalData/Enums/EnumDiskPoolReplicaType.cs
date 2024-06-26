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
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;
using PortalResources;

namespace PortalData.Enums
{
	/// <summary>서비스 타입</summary>
	[JsonConverter(typeof(StringEnumConverter))]
	public enum EnumDiskPoolReplicaType
	{
		/// <summary> 1+0 </summary>
		[Display(Name = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_ZERO", Description = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_ZERO", GroupName = "UL_DISKPOOL_REPLICA_TYPE", ShortName = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_ZERO", Prompt = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_ZERO", ResourceType = typeof(Resource))]
		OnePlusZero = 1,
		/// <summary> 1+1 </summary>
		[Display(Name = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_ONE", Description = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_ONE", GroupName = "UL_DISKPOOL_REPLICA_TYPE", ShortName = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_ONE", Prompt = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_ONE", ResourceType = typeof(Resource))]
		OnePlusOne,
		/// <summary> 1+2 </summary>
		[Display(Name = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_TWO", Description = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_TWO", GroupName = "UL_DISKPOOL_REPLICA_TYPE", ShortName = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_TWO", Prompt = "UL_DISKPOOL_REPLICA_TYPE_ONE_PULS_TWO", ResourceType = typeof(Resource))]
		OnePlusTwo,
		/// <summary> Erasure Code </summary>
		[Display(Name = "UL_DISKPOOL_REPLICA_TYPE_EC", Description = "UL_DISKPOOL_REPLICA_TYPE_EC", GroupName = "UL_DISKPOOL_REPLICA_TYPE", ShortName = "UL_DISKPOOL_REPLICA_TYPE_EC", Prompt = "UL_DISKPOOL_REPLICA_TYPE_EC", ResourceType = typeof(Resource))]
		ErasureCode
	}
}