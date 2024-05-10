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
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;
using MTLib.AspNetCore;

namespace PortalProviderInterface
{
	/// <summary>사용자 프로바이더 인터페이스</summary>
	public interface IUserProvider : IBaseProvider
	{
		/// <summary>사용자를 추가한다.</summary>
		/// <param name="Request">사용자 정보</param>
		/// <param name="Password">비밀번호 (옵션)</param>
		/// <param name="ConfirmPassword">확인 비밀번호 (옵션)</param>
		/// <returns>사용자 등록 결과</returns>
		Task<ResponseData<ResponseUserWithRoles>> Add(RequestUserCreate Request, string Password = "", string ConfirmPassword = "");

		/// <summary>사용자를 수정한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">사용자 정보</param>
		/// <param name="IncludeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 수정 결과</returns>
		Task<ResponseData> Update(string Id, RequestUserUpdate Request, bool IncludeDeletedUser = false);

		/// <summary>사용자를 삭제한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <returns>사용자 삭제 결과</returns>
		Task<ResponseData> Remove(string Id);

		/// <summary>사용자 비밀번호를 변경한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">사용자 정보</param>
		/// <returns>사용자 수정 결과</returns>
		Task<ResponseData> ChangePassword(string Id, RequestUserChangePassword Request);

		/// <summary>특정 사용자 식별자에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="IncludeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		Task<NNApplicationUser> GetUserById(string Id, bool IncludeDeletedUser = false);

		/// <summary>특정 로그인 이메일에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="Email">이메일 주소</param>
		/// <param name="IncludeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		Task<NNApplicationUser> GetUserByEmail(string Email, bool IncludeDeletedUser = false);

		/// <summary>특정 로그인 ID에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="LoginId">로그인 아이디</param>
		/// <param name="IncludeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		Task<NNApplicationUser> GetUserByLoginId(string LoginId, bool IncludeDeletedUser = false);

		/// <summary>사용자 식별자로 특정 사용자를 가져온다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <returns>해당 사용자 데이터</returns>
		Task<ResponseData<ResponseUserWithRoles>> GetUser(string Id);

		/// <summary>전체 사용자를 가져온다.</summary>
		/// <param name="SearchRoleName">검색할 역할명</param>
		/// <param name="RegStartDate">가입일 검색 시작일자</param>
		/// <param name="RegEndDate">가입일 검색 종료일자</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드목록 (LoginId, Email, Name)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>사용자 목록</returns>
		Task<ResponseList<ResponseUserWithRoles>> GetUsers(
			string SearchRoleName = "", DateTime? RegStartDate = null, DateTime? RegEndDate = null,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = null
		);

		/// <summary>특정 사용자에 대한 권한 목록을 가져온다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>특정 사용자에 대한 권한 목록</returns>
		Task<ResponseList<ResponseClaim>> GetClaims(string Id, int Skip = 0, int CountPerPage = int.MaxValue, string SearchKeyword = null);

		/// <summary>특정 사용자에 대한 사용자 권한 목록을 가져온다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>특정 사용자에 대한 권한 목록</returns>
		Task<ResponseList<ResponseClaim>> GetUserClaims(string Id, int Skip = 0, int CountPerPage = int.MaxValue, string SearchKeyword = null);

		/// <summary>특정 사용자에 권한을 추가한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">권한 정보</param>
		/// <returns>권한 등록 결과</returns>
		Task<ResponseData> AddClaim(string Id, RequestAddClaimToUser Request);

		/// <summary>특정 사용자에서 권한을 삭제한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="ClaimValue">권한값</param>
		/// <returns>권한 삭제 결과</returns>
		Task<ResponseData> RemoveClaim(string Id, string ClaimValue);

		/// <summary>특정 사용자에 역할을 추가한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">역할 정보</param>
		/// <returns>역할 등록 결과</returns>
		Task<ResponseData> AddRole(string Id, RequestAddRoleToUser Request);

		/// <summary>특정 사용자에서 역할을 삭제한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="RoleId">역할아이디</param>
		/// <returns>역할 삭제 결과</returns>
		Task<ResponseData> RemoveRole(string Id, string RoleId);

		/// <summary>사용자 수를 반환한다.</summary>
		/// <returns>전체 사용자 수</returns>
		Task<int> UserCount();
	}
}
