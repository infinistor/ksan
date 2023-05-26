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
using MTLib.CommonData;
using MTLib.Core;
using PortalData.ValidationAttributes;
using PortalResources;

namespace PortalData.Requests.Ksan
{
	/// <summary>Ksan 사용자 정보 등록 요청 클래스</summary>
	public class RequestKsanUserUpdate : CommonRequestData
	{
		/// <summary>Ksan 사용자명</summary>
		[Name(ErrorMessageResourceName = "EM_COMMON_INVALID_NAME", ErrorMessageResourceType = typeof(Resource))]
		public string Name
		{
			get => m_name;
			set => m_name = value.IsEmpty() ? "" : value.Trim();
		}
		private string m_name;

		/// <summary>이메일 주소</summary>
		public string Email { get; set; } = "";
	}
}
