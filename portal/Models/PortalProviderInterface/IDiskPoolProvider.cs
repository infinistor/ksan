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
using PortalData.Requests.Disks;
using PortalData.Responses.Disks;

namespace PortalProviderInterface
{
	/// <summary>디스크 풀 데이터 프로바이더 인터페이스</summary>
	public interface IDiskPoolProvider : IBaseProvider
	{
		/// <summary>디스크 풀 등록</summary>
		/// <param name="Request">디스크 풀 등록 요청 객체</param>
		/// <returns>디스크 풀 등록 결과 객체</returns>
		Task<ResponseData<ResponseDiskPoolWithDisks>> Add(RequestDiskPool Request);

		/// <summary>디스크 풀 수정</summary>
		/// <param name="Id">디스크 풀 아이디</param>
		/// <param name="Request">디스크 풀 수정 요청 객체</param>
		/// <returns>디스크 풀 수정 결과 객체</returns>
		Task<ResponseData> Update(string Id, RequestDiskPool Request);

		/// <summary>디스크 풀 삭제</summary>
		/// <param name="Id">디스크 풀 아이디</param>
		/// <returns>디스크 풀 삭제 결과 객체</returns>
		Task<ResponseData> Remove(string Id);

		/// <summary>디스크 풀 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>디스크 풀 목록 객체</returns>
		Task<ResponseList<ResponseDiskPool>> GetList(
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		);

		/// <summary>디스크 풀 목록을 가져온다.</summary>
		/// <returns>디스크 풀 목록 객체</returns>
		Task<ResponseList<ResponseDiskPoolDetails>> GetListDetails();

		/// <summary>특정 디스크 풀 정보를 가져온다.</summary>
		/// <param name="Id">디스크 풀 아이디</param>
		/// <returns>디스크 풀 정보 객체</returns>
		Task<ResponseData<ResponseDiskPoolWithDisks>> Get(string Id);

		/// <summary>특정 이름의 디스크 풀가 존재하는지 확인한다.</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 디스크 풀 아이디</param>
		/// <param name="Request">특정 이름의 디스크 풀 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		Task<ResponseData<bool>> IsNameExist(string ExceptId, RequestIsDiskPoolNameExist Request);

		/// <summary>해당 디스크 타입으로 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>참여가 가능한 디스크 목록 객체</returns>
		Task<ResponseList<ResponseDisk>> GetAvailableDisks(int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "");

		/// <summary>주어진 디스크 풀 아이디에 해당하는 디스크 풀에 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="Id">디스크 풀 아이디 (null인 경우, 어느 풀에도 속하지 않은 디스크만 검색한다.)</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>참여가 가능한 디스크 목록 객체</returns>
		Task<ResponseList<ResponseDisk>> GetAvailableDisks(
			string Id
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "");
	}
}