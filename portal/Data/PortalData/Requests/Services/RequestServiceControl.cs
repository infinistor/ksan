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
using PortalData.Enums;

namespace PortalData.Requests.Services
{
	/// <summary>서비스 제어 명령 요청 클래스</summary>
	public class RequestServiceControl
	{
		/// <summary>서비스 아이디</summary>
		public string Id { get; set; }
		
		/// <summary>서비스 종류</summary>
		public EnumServiceType ServiceType { get; set; }

		/// <summary>서비스 제어 명령</summary>
		public EnumServiceControl Control { get; set; }

		/// <summary>생성자</summary>
		public RequestServiceControl()
		{

		}

		/// <summary>생성자</summary>
		/// <param name="Id">서비스 아이디</param>
		/// <param name="ServiceType">서비스 종류</param>
		/// <param name="Control">서비스 제어 명령</param>
		public RequestServiceControl(string Id, EnumServiceType ServiceType, EnumServiceControl Control)
		{
			this.Id = Id;
			this.ServiceType = ServiceType;
			this.Control = Control;
		}
	}
}