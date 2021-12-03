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
	    /// <param name="userId">사용자 아이디</param>
	    /// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
	    /// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
	    /// <param name="orderFields">정렬필드목록 (KeyName, ExpireDate, KeyValue)</param>
	    /// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
	    /// <param name="searchFields">검색필드목록 (KeyName, KeyValue)</param>
	    /// <param name="searchKeyword">검색어</param>
	    /// <returns>API 키 정보 목록 객체</returns>
	    Task<ResponseList<ResponseApiKey>> GetApiKeys(Guid userId
		    , int skip = 0, int countPerPage = 100, List<string> orderFields = null, List<string> orderDirections = null
		    , List<string> searchFields = null, string searchKeyword = "");

        /// <summary>해당 사용자의 API 키 정보를 가져온다.</summary>
        /// <param name="userId">사용자 아이디</param>
        /// <param name="keyId">키 아이디</param>
        /// <returns>API 키 정보 목록 객체</returns>
        Task<ResponseData<ResponseApiKey>> GetApiKey(Guid userId, Guid keyId);

        /// <summary>해당 사용자의 API 키를 발행한다.</summary>
        /// <param name="userId">사용자 아이디</param>
        /// <param name="request">키 요청 객체</param>
        /// <returns>API 키 추가 결과 객체</returns>
        Task<ResponseData<ResponseApiKey>> IssueApiKey(Guid userId, RequestApiKey request);

        /// <summary>해당 사용자의 API 키를 키 값까지 지정하여 발행한다.</summary>
        /// <param name="userId">사용자 아이디</param>
        /// <param name="request">키 요청 객체</param>
        /// <returns>API 키 추가 결과 객체</returns>
        Task<ResponseData<ResponseApiKey>> IssueApiKey(Guid userId, RequestApiKeyEx request);

        /// <summary>해당 사용자의 API 키를 해제한다.</summary>
        /// <param name="userId">사용자 아이디</param>
        /// <param name="keyId">키 아이디</param>
        /// <returns>API 키 해제 결과 객체</returns>
        Task<ResponseData> RevokeApiKey(Guid userId, Guid keyId);

        /// <summary>해당 키 아이디의 API 키 정보를 가져온다.</summary>
        /// <param name="keyValue">키 값</param>
        /// <returns>API 키 정보 목록 객체</returns>
        Task<ResponseData<ResponseApiKey>> GetApiKey(string keyValue);
    }

    /// <summary>미리 정의된 API 키</summary>
    public class PredefinedApiKey
    {
	    /// <summary>내부 시스템</summary>
	    public static readonly string InternalSystemApiKey = "5de46d7ccd5d0954fad7d11ffc22a417e2784cbedd9f1dae3992a46e97b367e8";
    }
}