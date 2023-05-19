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
using MTLib.CommonData;
using MTLib.Core;
using PortalData.ValidationAttributes;

namespace PortalData.Requests.Services
{
	/// <summary>특정 이름의 서비스 그룹 존재여부 확인 요청 클래스</summary>
	public class RequestIsServiceGroupNameExist : CommonRequestData
	{
		/// <summary>서버명</summary>
		[Name(ErrorMessageResourceName = "EM_COMMON_INVALID_NAME", ErrorMessageResourceType = typeof(Resource))]
		public string Name
		{
			get => m_name;
			set => m_name = value.IsEmpty() ? "" : value.Trim();
		}
		private string m_name;

		/// <summary>생성자</summary>
		public RequestIsServiceGroupNameExist()
		{

		}

		/// <summary>생성자</summary>
		/// <param name="name">초기화할 이름</param>
		public RequestIsServiceGroupNameExist(string name)
		{
			this.Name = name;
		}
	}
}