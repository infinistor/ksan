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
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;

namespace PortalProviderInterface
{
	/// <summary>역할 프로바이더 인터페이스</summary>
	public interface IRoleProvider : IBaseProvider
	{
		/// <summary>역할을 추가한다.</summary>
		/// <param name="request">역할 정보</param>
		/// <returns>역할 등록 결과</returns>
		Task<ResponseData<ResponseRole>> AddRole(RequestRole request);

		/// <summary>역할을 수정한다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <param name="request">역할 정보</param>
		/// <returns>역할 수정 결과</returns>
		Task<ResponseData> UpdateRole(string id, RequestRole request);

		/// <summary>역할을 삭제한다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <returns>역할 삭제 결과</returns>
		Task<ResponseData> RemoveRole(string id);

		/// <summary>역할 아이디로 특정 역할을 가져온다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <returns>해당 역할 데이터</returns>
		Task<ResponseData<ResponseRole>> GetRoleById(string id);

		/// <summary>역할명으로 특정 역할을 가져온다.</summary>
		/// <param name="name">역할명</param>
		/// <returns>해당 역할 데이터</returns>
		Task<ResponseData<ResponseRole>> GetRoleByName(string name);

		/// <summary>전체 역할들을 가져온다.</summary>
		/// <returns>해당 역할 데이터</returns>
		Task<ResponseList<ResponseRole>> GetRoles();

		/// <summary>특정 역할에 대한 권한 목록을 가져온다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>특정 역할에 대한 사용자 목록</returns>
		Task<ResponseList<ResponseClaim>> GetRoleClaims(string id, int skip = 0, int countPerPage = int.MaxValue, string searchKeyword = "");

		/// <summary>특정 역할에 대한 사용자 목록을 가져온다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지당 레코드 수 (옵션, 기본 20)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>특정 역할에 대한 사용자 목록</returns>
		Task<ResponseList<ResponseUser>> GetRoleUsers(string id, int skip = 0, int countPerPage = 100, string searchKeyword = "");

		/// <summary>역할에 사용자를 추가한다.</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="request">사용자 정보</param>
		/// <returns>역할 등록 결과</returns>
		Task<ResponseData> AddUserToRole(string roleId, RequestAddUserToRole request);
		/// <summary>역할에 사용자를 추가한다.</summary>
		/// <param name="roleName">역할명</param>
		/// <param name="request">사용자 정보</param>
		/// <returns>사용자 추가 결과</returns>
		Task<ResponseData> AddUserToRoleByName(string roleName, RequestAddUserToRole request);

		/// <summary>역할에 권한을 추가한다.</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="request">권한 정보</param>
		/// <returns>역할 등록 결과</returns>
		Task<ResponseData> AddClaimToRole(string roleId, RequestAddClaimToRole request);

		/// <summary>역할의 권한을 수정한다.</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="request">역할 권한 목록 객체</param>
		/// <returns>역할 권한 수정 결과</returns>
		Task<ResponseData> UpdateRoleClaims(string roleId, List<RequestRoleClaim> request);

		/// <summary>역할에서 사용자를 삭제한다.</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="userId">사용자 식별자</param>
		/// <returns>사용자 삭제 결과</returns>
		Task<ResponseData> RemoveUserFromRole(string roleId, string userId);

		/// <summary>역할에서 사용자를 삭제한다.</summary>
		/// <param name="roleName">역할명</param>
		/// <param name="userId">사용자 식별자</param>
		/// <returns>사용자 삭제 결과</returns>
		Task<ResponseData> RemoveUserFromRoleByName(string roleName, string userId);

		/// <summary>역할에서 권한을 삭제한다.</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="claimValue">권한값</param>
		/// <returns>권한 삭제 결과</returns>
		Task<ResponseData> RemoveClaimFromRole(string roleId, string claimValue);

		/// <summary>권한 제목을 추가한다.</summary>
		/// <param name="claimType">권한 타입</param>
		/// <param name="claimValue">권한 값</param>
		/// <param name="claimTitle">권한 제목</param>
		/// <param name="depth">뎁스</param>
		/// <param name="orderNo">순서</param>
		/// <returns>권한 제목 추가 결과</returns>
		Task<ResponseData> AddClaimTitle(string claimType, string claimValue, string claimTitle, short depth, string orderNo);

		/// <summary>모든 권한 목록을 가져온다.</summary>
		/// <returns>권한 목록</returns>
		Task<ResponseList<ResponseClaim>> GetAllClaims();

		/// <summary>특정 권한의 하위 권한 목록을 가져온다.</summary>
		/// <param name="parentId">상위 권한 아이디</param>
		/// <returns>권한 목록</returns>
		Task<ResponseList<ResponseClaim>> GetClaims(int parentId);
	}

	/// <summary>미리 정의된 역할명</summary>
	public class PredefinedRoleNames
	{
		/// <summary>슈퍼바이저 역할명</summary>
		public static readonly string RoleNameSupervisor = "Supervisor";

		/// <summary>내부 서비스 역할명</summary>
		public static readonly string RoleNameInternalService = "InternalService";

		/// <summary>사용자 역할명</summary>
		public static readonly string RoleNameUser = "User";
	}
}
