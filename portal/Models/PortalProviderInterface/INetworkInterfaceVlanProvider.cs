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
using System.Collections.Generic;
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Networks;
using PortalData.Responses.Networks;

namespace PortalProviderInterface
{
	/// <summary>네트워크 인터페이스 VLAN 데이터 프로바이더 인터페이스</summary>
	public interface INetworkInterfaceVlanProvider : IBaseProvider
	{
		/// <summary>네트워크 인터페이스 VLAN 등록</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Request">네트워크 인터페이스 VLAN 등록 요청 객체</param>
		/// <returns>네트워크 인터페이스 VLAN 등록 결과 객체</returns>
		Task<ResponseData<ResponseNetworkInterfaceVlan>> Add(string ServerId, string InterfaceId, RequestNetworkInterfaceVlan Request);

		/// <summary>네트워크 인터페이스 VLAN 수정</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Id">네트워크 인터페이스 VLAN 아이디</param>
		/// <param name="Request">네트워크 인터페이스 VLAN 수정 요청 객체</param>
		/// <returns>네트워크 인터페이스 VLAN 수정 결과 객체</returns>
		Task<ResponseData> Update(string ServerId, string InterfaceId, string Id, RequestNetworkInterfaceVlan Request);

		/// <summary>네트워크 인터페이스 VLAN 삭제</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Id">네트워크 인터페이스 VLAN 아이디</param>
		/// <returns>네트워크 인터페이스 VLAN 삭제 결과 객체</returns>
		Task<ResponseData> Remove(string ServerId, string InterfaceId, string Id);

		/// <summary>네트워크 인터페이스 VLAN 목록을 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Tag)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Tag)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>네트워크 인터페이스 VLAN 목록 객체</returns>
		Task<ResponseList<ResponseNetworkInterfaceVlan>> GetList(
			string ServerId, string InterfaceId
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		);

		/// <summary>네트워크 인터페이스 VLAN 정보를 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Id">네트워크 인터페이스 VLAN 아이디</param>
		/// <returns>네트워크 인터페이스 VLAN 정보 객체</returns>
		Task<ResponseData<ResponseNetworkInterfaceVlan>> Get(string ServerId, string InterfaceId, string Id);

		/// <summary>해당 VLAN 태그가 존재하는지 여부</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="ExceptId">이름 검색 시 제외할 네트워크 인터페이스 VLAN 아이디</param>
		/// <param name="Request">특정 VLAN 태그 존재여부 확인 요청 객체</param>
		/// <returns>해당 VLAN 태그가 존재하는지 여부</returns>
		Task<ResponseData<bool>> IsTagExist(string ServerId, string InterfaceId, string ExceptId, RequestIsNetworkInterfaceVlanExist Request);
	}
}