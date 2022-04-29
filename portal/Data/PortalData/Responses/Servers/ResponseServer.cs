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
using System;
using PortalData.Enums;
using MTLib.CommonData.Interfaces;

namespace PortalData.Responses.Servers
{
	/// <summary>서버 정보 응답 클래스</summary>
	public class ResponseServer : IModifier
	{
		/// <summary>서버 아이디</summary>
		public string Id { get; set; }

		/// <summary>서버명</summary>
		public string Name { get; set; }

		/// <summary>설명</summary>
		public string Description { get; set; }

		/// <summary>CPU 모델</summary>
		public string CpuModel { get; set; }

		/// <summary>CPU 클럭</summary>
		public short? Clock { get; set; }

		/// <summary>서버 상태</summary>
		public EnumServerState State { get; set; }

		/// <summary>Rack 정보</summary>
		public string Rack { get; set; }

		/// <summary>1분 Load Average</summary>
		public float LoadAverage1M { get; set; }

		/// <summary>5분 Load Average</summary>
		public float LoadAverage5M { get; set; }

		/// <summary>15분 Load Average</summary>
		public float LoadAverage15M { get; set; }

		/// <summary>전체 메모리 크기</summary>
		public decimal MemoryTotal { get; set; }

		/// <summary>사용 메모리 크기</summary>
		public decimal MemoryUsed { get; set; }

		/// <summary>남은 메모리 크기</summary>
		public decimal MemoryFree { get; set; }

		/// <summary>수정일시</summary>
		public DateTime? ModDate { get; set; } = null;

		/// <summary>수정인 아이디</summary>
		public string ModId { get; set; } = "";

		/// <summary>수정인 이름</summary>
		public string ModName { get; set; } = "";
	}
}