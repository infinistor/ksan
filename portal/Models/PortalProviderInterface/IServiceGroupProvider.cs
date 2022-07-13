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
using PortalData.Enums;
using PortalData.Requests.Services;
using PortalData.Responses.Services;

namespace PortalProviderInterface
{
	/// <summary>서비스 그룹 데이터 프로바이더 인터페이스</summary>
	public interface IServiceGroupProvider : IBaseProvider
	{
		/// <summary>서비스 그룹 등록</summary>
		/// <param name="Request">서비스 그룹 등록 요청 객체</param>
		/// <returns>서비스 그룹 등록 결과 객체</returns>
		Task<ResponseData<ResponseServiceGroupWithServices>> Add(RequestServiceGroup Request);

		/// <summary>서비스 그룹 수정</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <param name="Request">서비스 그룹 수정 요청 객체</param>
		/// <returns>서비스 그룹 수정 결과 객체</returns>
		Task<ResponseData> Update(string Id, RequestServiceGroup Request);

		/// <summary>서비스 그룹 삭제</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 삭제 결과 객체</returns>
		Task<ResponseData> Remove(string Id);

		/// <summary>서비스 그룹 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>서비스 그룹 목록 객체</returns>
		Task<ResponseList<ResponseServiceGroup>> GetList(
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		);

		/// <summary>서비스 그룹 정보를 가져온다.</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 정보 객체</returns>
		Task<ResponseData<ResponseServiceGroupWithServices>> Get(string Id);

		/// <summary>해당 이름이 존재하는지 여부</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 서비스 그룹 아이디</param>
		/// <param name="Request">특정 이름의 서비스 그룹 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		Task<ResponseData<bool>> IsNameExist(string ExceptId, RequestIsServiceGroupNameExist Request);

		/// <summary>해당 서비스 타입으로 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>참여가 가능한 서비스 목록 객체</returns>
		Task<ResponseList<ResponseService>> GetAvailableServices(
			EnumServiceType ServiceType
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "");

		/// <summary>주어진 서비스 그룹 아이디에 해당하는 서비스 그룹에 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="Id">서비스 그룹 아이디 (null인 경우, 어느 그룹에도 속하지 않은 서비스만 검색한다.)</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>참여가 가능한 서비스 목록 객체</returns>
		Task<ResponseList<ResponseService>> GetAvailableServices(
			string Id
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "");

		/// <summary>서비스 그룹 시작</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 시작 결과 객체</returns>
		Task<ResponseData> Start(string Id);

		/// <summary>서비스 그룹 중지</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 중지 결과 객체</returns>
		Task<ResponseData> Stop(string Id);

		/// <summary>서비스 그룹 재시작</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 재시작 결과 객체</returns>
		Task<ResponseData> Restart(string Id);
	}
}