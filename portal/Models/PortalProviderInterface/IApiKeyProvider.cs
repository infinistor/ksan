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
using System.Collections.Generic;
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;

namespace PortalProviderInterface
{
	/// <summary>API 키 프로바이더 인터페이스</summary>
	public interface IApiKeyProvider : IBaseProvider
	{

		/// <summary>해당 사용자의 API 키 정보 목록을 가져온다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (KeyName, ExpireDate, KeyValue)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드목록 (KeyName, KeyValue)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>API 키 정보 목록 객체</returns>
		Task<ResponseList<ResponseApiKey>> GetApiKeys(Guid UserId
			, int Skip = 0, int CountPerPage = 100, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "");

		/// <summary>해당 사용자의 API 키 정보를 가져온다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="KeyId">키 아이디</param>
		/// <returns>API 키 정보 목록 객체</returns>
		Task<ResponseData<ResponseApiKey>> GetApiKey(Guid UserId, Guid KeyId);

		/// <summary>해당 사용자의 API 키를 발행한다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="Request">키 요청 객체</param>
		/// <returns>API 키 추가 결과 객체</returns>
		Task<ResponseData<ResponseApiKey>> IssueApiKey(Guid UserId, RequestApiKey Request);

		/// <summary>해당 사용자의 API 키를 키 값까지 지정하여 발행한다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="Request">키 요청 객체</param>
		/// <returns>API 키 추가 결과 객체</returns>
		Task<ResponseData<ResponseApiKey>> IssueApiKey(Guid UserId, RequestApiKeyEx Request);

		/// <summary>해당 사용자의 API 키를 해제한다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="KeyId">키 아이디</param>
		/// <returns>API 키 해제 결과 객체</returns>
		Task<ResponseData> RevokeApiKey(Guid UserId, Guid KeyId);

		/// <summary>해당 키 아이디의 API 키 정보를 가져온다.</summary>
		/// <param name="KeyValue">키 값</param>
		/// <returns>API 키 정보 목록 객체</returns>
		Task<ResponseData<ResponseApiKey>> GetApiKey(string KeyValue);

		/// <summary> 메인키의 정보를 가져온다. </summary>
		/// <returns> API 키 객체 </returns>
		Task<ResponseApiKey> GetMainApiKey();
	}
}