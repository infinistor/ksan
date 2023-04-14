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
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using PortalData.Enums;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;
using PortalData.ValidationAttributes;

namespace PortalData.Requests.Services
{
	/// <summary>서비스 정보 요청 클래스</summary>
	public class RequestService : CommonRequestData
	{
		/// <summary>그룹 아이디</summary>
		public string GroupId { get; set; }

		/// <summary>서비스명</summary>
		[Name(ErrorMessageResourceName = "EM_COMMON_INVALID_NAME", ErrorMessageResourceType = typeof(Resource))]
		public string Name
		{
			get => m_name;
			set => m_name = value.IsEmpty() ? "" : value.Trim();
		}
		private string m_name;

		/// <summary>서버 아이디</summary>
		[Required(ErrorMessageResourceName = "EM_SERVERS_REQUIRE_ID", ErrorMessageResourceType = typeof(Resource))]
		public string ServerId { get; set; }

		/// <summary>설명</summary>
		public string Description { get; set; }

		/// <summary>서비스 타입</summary>
		public EnumServiceType ServiceType { get; set; }

		/// <summary>HA 작업</summary>
		public EnumHaAction HaAction { get; set; } = EnumHaAction.Unknown;

		/// <summary>서비스 상태</summary>
		public EnumServiceState State { get; set; } = EnumServiceState.Unknown;

		/// <summary>네트워크 인터페이스 VLAN 목록</summary>
		public List<string> VlanIds { get; set; } = new List<string>();
	}
}