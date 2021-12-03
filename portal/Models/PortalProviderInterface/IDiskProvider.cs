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
using PortalData.Requests.Disks;
using PortalData.Responses.Disks;

namespace PortalProviderInterface
{
	/// <summary>디스크 데이터 관련 인터페이스</summary>
	public interface IDiskProvider : IBaseProvider
	{
		/// <summary>디스크 등록</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="request">디스크 등록 요청 객체</param>
		/// <returns>디스크 등록 결과 객체</returns>
		Task<ResponseData<ResponseDiskWithServer>> Add(string serverId, RequestDisk request);
		
		/// <summary>디스크 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="request">디스크 수정 요청 객체</param>
		/// <returns>디스크 수정 결과 객체</returns>
		Task<ResponseData> Update(string serverId, string id, RequestDisk request);
	
		/// <summary>디스크 상태 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="state">디스크 상태</param>
		/// <returns>디스크 상태 수정 결과 객체</returns>
		Task<ResponseData> UpdateState(string serverId, string id, EnumDiskState state);
	
		/// <summary>디스크 상태 수정</summary>
		/// <param name="request">상태 수정 요청 객체</param>
		/// <returns>디스크 상태 수정 결과 객체</returns>
		Task<ResponseData> UpdateState(RequestDiskState request);

		/// <summary>디스크 크기 수정</summary>
		/// <param name="request">디스크 크기 수정 요청 객체</param>
		/// <returns>디스크 크기 수정 결과 객체</returns>
		Task<ResponseData> UpdateSize(RequestDiskSize request);

		/// <summary>디스크 읽기/쓰기 모드 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="diskRwMode">디스크 읽기/쓰기 모드</param>
		/// <returns>디스크 읽기/쓰기 모드 수정 결과 객체</returns>
		Task<ResponseData> UpdateRwMode(string serverId, string id, EnumDiskRwMode diskRwMode);

		/// <summary>디스크 읽기/쓰기 모드 수정</summary>
		/// <param name="request">디스크 읽기/쓰기 모드 수정 요청 객체</param>
		/// <returns>디스크 읽기/쓰기 모드 수정 결과 객체</returns>
		Task<ResponseData> UpdateRwMode(RequestDiskRwMode request);
	
		/// <summary>디스크 삭제</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <returns>디스크 삭제 결과 객체</returns>
		Task<ResponseData> Remove(string serverId, string id);
		
		/// <summary>특정 서버의 디스크 목록을 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="searchStates">검색할 디스크 상태 목록</param>
		/// <param name="searchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>디스크 목록 객체</returns>
		Task<ResponseList<ResponseDisk>> GetList(
			string serverId,
			List<EnumDiskState> searchStates, List<EnumDiskRwMode> searchRwModes,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		);
			
		/// <summary>전체 디스크 목록을 가져온다.</summary>
		/// <param name="searchStates">검색할 디스크 상태 목록</param>
		/// <param name="searchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>디스크 목록 객체</returns>
		Task<ResponseList<ResponseDiskWithServer>> GetList(
			List<EnumDiskState> searchStates, List<EnumDiskRwMode> searchRwModes,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		);
	
		/// <summary>디스크 정보를 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <returns>디스크 정보 객체</returns>
		Task<ResponseData<ResponseDiskWithServices>> Get(string serverId, string id);
		
		/// <summary>DiskNo로 디스크 ID를 가져온다.</summary>
		/// <param name="diskNo">디스크 No</param>
		/// <returns>디스크 아이디 응답 객체</returns>
		Task<ResponseData<ResponseDiskId>> Get(string diskNo);
	}
}