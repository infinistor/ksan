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
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Requests.Services;
using PortalData.Responses.Services;

namespace PortalProviderInterface
{
	/// <summary>서비스 데이터 프로바이더 인터페이스</summary>
	public interface IServiceProvider : IBaseProvider
	{
		/// <summary>서비스 등록</summary>
		/// <param name="Request">서비스 등록 요청 객체</param>
		/// <returns>서비스 등록 결과 객체</returns>
		Task<ResponseData<ResponseServiceWithVlans>> Add(RequestService Request, Guid? ModId = null, string ModName = null);

		/// <summary>서비스 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="Request">서비스 수정 요청 객체</param>
		/// <returns>서비스 수정 결과 객체</returns>
		Task<ResponseData> Update(string Id, RequestService Request);

		/// <summary>서비스 상태 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="State">서비스 상태</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		Task<ResponseData> UpdateState(string Id, EnumServiceState State, Guid? ModId = null, string ModName = null);

		/// <summary>서비스 상태 수정</summary>
		/// <param name="Request">상태 수정 요청 객체</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		Task<ResponseData> UpdateState(RequestServiceState Request, Guid? ModId = null, string ModName = null);

		/// <summary>서비스 사용 정보 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="CpuUsage">CPU 사용률</param>
		/// <param name="MemoryUsed">메모리 사용량</param>
		/// <param name="ThreadCount">스레드 수</param>
		/// <returns>서비스 사용 정보 정보 수정 결과 객체</returns>
		Task<ResponseData> UpdateUsage(string Id, float CpuUsage, decimal MemoryUsed, int ThreadCount);

		/// <summary>서비스 사용 정보 수정</summary>
		/// <param name="Request">서비스 사용 정보 수정 요청 객체</param>
		/// <returns>서비스 사용 정보 수정 결과 객체</returns>
		Task<ResponseData> UpdateUsage(RequestServiceUsage Request);

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="State">HA 상태</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서비스 HA 상태 수정 결과 객체</returns>
		Task<ResponseData> UpdateHaAction(string Id, EnumHaAction State, Guid? ModId = null, string ModName = null);

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="Request">HA 상태 수정 요청 객체</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서비스 HA 상태 수정 결과 객체</returns>
		Task<ResponseData> UpdateHaAction(RequestServiceHaAction Request, Guid? ModId = null, string ModName = null);

		/// <summary>서비스 삭제</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 삭제 결과 객체</returns>
		Task<ResponseData> Remove(string Id);

		/// <summary>서비스 목록을 가져온다.</summary>
		/// <param name="SearchState">검색할 서비스 상태 목록</param>
		/// <param name="SearchType">검색할 서비스 타입 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, ServiceType)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (GroupName, Name, Description, ServiceType, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>서비스 목록 객체</returns>
		Task<ResponseList<ResponseServiceWithGroup>> GetList(
			EnumServiceState? SearchState = null, EnumServiceType? SearchType = null,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = null
		);

		/// <summary>서비스 정보를 가져온다.</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 정보 객체</returns>
		Task<ResponseData<ResponseServiceWithVlans>> Get(string Id);

		/// <summary>해당 이름이 존재하는지 여부</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 서비스 아이디</param>
		/// <param name="Name">검색할 이름</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		Task<ResponseData<bool>> IsNameExist(string ExceptId, string Name);

		/// <summary>해당 이름이 존재하는지 여부</summary>
		/// <param name="Name">검색할 이름</param>
		/// <param name="ExceptId">이름 검색 시 제외할 서비스 아이디</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		Task<bool> IsNameExist(string Name, Guid? ExceptId = null);

		/// <summary>서비스 시작</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 시작 결과 객체</returns>
		Task<ResponseData> Start(string Id);

		/// <summary>서비스 중지</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 중지 결과 객체</returns>
		Task<ResponseData> Stop(string Id);

		/// <summary>서비스 재시작</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 재시작 결과 객체</returns>
		Task<ResponseData> Restart(string Id);

		/// <summary>서비스 이벤트를 추가한다.</summary>
		/// <param name="Request">서비스 이벤트 요청 객체</param>
		/// <returns>서버 이벤트 추가 결과 객체</returns>
		Task<ResponseData> AddEvent(RequestServiceEvent Request);

		/// <summary>서비스 목록을 가져온다.</summary>
		/// <param name="Id"> 서비스 아이디 / 이름</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (RegDate, ServiceEventType)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (ServiceEventType)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>서비스 목록 객체</returns>
		Task<ResponseList<ResponseServiceEvent>> GetEventList(string Id,
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = null
		);
	}
}