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
using PortalData.Requests.Servers;
using PortalData.Responses.Servers;

namespace PortalProviderInterface
{
	/// <summary>서버 데이터 프로바이더 인터페이스</summary>
	public interface IServerProvider : IBaseProvider
	{
		/// <summary>서버 등록</summary>
		/// <param name="Request">서버 등록 요청 객체</param>
		/// <returns>서버 등록 결과 객체</returns>
		Task<ResponseData<ResponseServerDetail>> Add(RequestServer Request);

		/// <summary>서버 초기화</summary>
		/// <param name="Request">서버 초기화 요청 객체</param>
		/// <returns>서버 초기화 결과 객체</returns>
		Task<ResponseData> Initialize(RequestServerInitialize Request);

		/// <summary>서버 수정</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <param name="Request">서버 수정 요청 객체</param>
		/// <returns>서버 수정 결과 객체</returns>
		Task<ResponseData> Update(string Id, RequestServer Request);

		/// <summary>서버 상태 수정</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <param name="State">서버 상태</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		Task<ResponseData> UpdateState(string Id, EnumServerState State, string ModId = "", string ModName = "");

		/// <summary>서버 상태 수정</summary>
		/// <param name="Request">서버 상태 수정 요청 객체</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		Task<ResponseData> UpdateState(RequestServerState Request, string ModId = "", string ModName = "");

		/// <summary>서버 사용 정보 수정</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <param name="LoadAverage1M">1분 Load Average</param>
		/// <param name="LoadAverage5M">5분 Load Average</param>
		/// <param name="LoadAverage15M">15분 Load Average</param>
		/// <param name="MemoryUsed">메모리 사용량</param>
		/// <returns>서버 사용 정보 수정 결과 객체</returns>
		Task<ResponseData> UpdateUsage(string Id, float LoadAverage1M, float LoadAverage5M, float LoadAverage15M, decimal MemoryUsed);

		/// <summary>서버 사용 정보 수정</summary>
		/// <param name="Request">서버 사용 정보 수정 요청 객체</param>
		/// <returns>서버 사용 정보 수정 결과 객체</returns>
		Task<ResponseData> UpdateUsage(RequestServerUsage Request);

		/// <summary>서버 삭제</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <returns>서버 삭제 결과 객체</returns>
		Task<ResponseData> Remove(string Id);

		/// <summary>서버 목록을 가져온다.</summary>
		/// <param name="SearchStates">검색할 서버 상태 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, CpuModel, Clock, State, Rack, LoadAverage1M, LoadAverage5M, LoadAverage15M, MemoryTotal, MemoryUsed, MemoryFree)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>서버 목록 객체</returns>
		Task<ResponseList<ResponseServer>> GetList(
			List<EnumServerState> SearchStates,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		);

		/// <summary>서버 정보를 가져온다.</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <returns>서버 정보 객체</returns>
		Task<ResponseData<ResponseServerDetail>> Get(string Id);

		/// <summary>해당 이름이 존재하는지 여부</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 서버 아이디</param>
		/// <param name="Request">특정 이름의 서버 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		Task<ResponseData<bool>> IsNameExist(string ExceptId, RequestIsServerNameExist Request);
	}
}