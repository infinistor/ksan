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
using PortalData.Enums;
using PortalResources;
using MTLib.CommonData;

namespace PortalData.Requests.Disks
{
	/// <summary>디스크 읽기/쓰기 모드 변경 요청 클래스</summary>
	public class RequestDiskRwMode : CommonRequestData
	{
		/// <summary>디스크 아이디</summary>
		public string Id { get; set; }

		/// <summary>서버 아이디</summary>
		[Required(ErrorMessageResourceName = "EM_DISKS_REQUIRE_SERVER_ID", ErrorMessageResourceType = typeof(Resource))]
		public string ServerId { get; set; }

		/// <summary>디스크 번호</summary>
		public string DiskNo { get; set; }

		/// <summary>읽기/쓰기 모드</summary>
		public EnumDiskRwMode RwMode { get; set; }

		/// <summary>생성자</summary>
		public RequestDiskRwMode()
		{

		}

		/// <summary>생성자</summary>
		/// <param name="id">디스크 아이디</param>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="diskNo">디스크 번호</param>
		/// <param name="rwMode">읽기/쓰기 모드</param>
		public RequestDiskRwMode(string id, string serverId, string diskNo, EnumDiskRwMode rwMode)
		{
			Id = id;
			ServerId = serverId;
			DiskNo = diskNo;
			RwMode = rwMode;
		}
	}
}