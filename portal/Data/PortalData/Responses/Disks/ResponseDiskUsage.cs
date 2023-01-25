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
using MTLib.CommonData;
using PortalData.Enums;
using PortalResources;

namespace PortalData.Requests.Disks
{
	/// <summary>디스크 사용량 클래스</summary>
	public class ResponseDiskUsage : CommonRequestData
	{
		/// <summary> 디스크 아이디 </summary>
		public string Id { get; set; }
		
		/// <summary>디스크 상태</summary>
		public EnumDiskState State { get; set; } = EnumDiskState.Disable;
		
		/// <summary>전체 크기</summary>
		public decimal TotalSize { get; set; }

		/// <summary>사용된 크기</summary>
		public decimal UsedSize { get; set; }

		/// <summary> 디스크 읽기 </summary>
		public virtual decimal Read { get; set; }

		/// <summary> 디스크 쓰기 </summary>
		public virtual decimal Write { get; set; }
	}
}