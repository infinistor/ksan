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
	/// <summary>디스크 읽기/쓰기 모드</summary>
	[JsonConverter(typeof(StringEnumConverter))]
	public enum EnumDiskRwMode
	{
		/// <summary>읽기 전용</summary>
		[Display(Name = "UL_DISKS_READ_WRITE_MODE_READ_ONLY", Description = "UL_DISKS_READ_WRITE_MODE_READ_ONLY", GroupName = "UL_DISKS_READ_WRITE_MODE", ShortName = "UL_DISKS_READ_WRITE_MODE_READ_ONLY", Prompt = "UL_DISKS_READ_WRITE_MODE_READ_ONLY", ResourceType = typeof(Resource))]
		ReadOnly,
		/// <summary>읽기/쓰기</summary>
		[Display(Name = "UL_DISKS_READ_WRITE_MODE_READ_WRITE", Description = "UL_DISKS_READ_WRITE_MODE_READ_WRITE", GroupName = "UL_DISKS_READ_WRITE_MODE", ShortName = "UL_DISKS_READ_WRITE_MODE_READ_WRITE", Prompt = "UL_DISKS_READ_WRITE_MODE_READ_WRITE", ResourceType = typeof(Resource))]
		ReadWrite
	}
}