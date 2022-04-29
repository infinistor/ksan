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
using PortalProviderInterface;
using PortalResources;
using PortalSvr.Services;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalSvr.Controllers.Accounts
{
	/// <summary>사용자 관련 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class UsersController : BaseController
	{
		/// <summary>데이터 프로바이더</summary>
		private readonly IUserProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public UsersController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<UsersController> logger,
			IUserProvider dataProvider
			)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>사용자 목록을 반환한다.</summary>
		/// <param name="searchRoleName">검색할 역할명</param>
		/// <param name="regStartDate">가입일 검색 시작일자</param>
		/// <param name="regEndDate">가입일 검색 종료일자</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드목록 (LoginId, Email, Name)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.list")]
		[HttpGet]
		public async Task<ActionResult> Get(
			string searchRoleName = "", DateTime? regStartDate = null, DateTime? regEndDate = null,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseUserWithRoles> response = new ResponseList<ResponseUserWithRoles>();
			try
			{
				// 사용자 목록을 반환한다.
				response = await m_dataProvider.GetUsers(
					searchRoleName, regStartDate, regEndDate,
					skip, countPerPage,
					orderFields, orderDirections,
					searchFields, searchKeyword
				);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>로그인 아이디 중복 여부를 검사한다.</summary>
		/// <param name="loginId">로그인 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.view")]
		[HttpGet("Unique/{loginId}")]
		public async Task<ActionResult> IsUniqueLoginId([FromRoute] string loginId)
		{
			ResponseData response = new ResponseData();
			try
			{
				// 로그인 아이디가 유효하지 않은 경우
				if (loginId.IsEmpty())
				{
					response.Code = Resource.EC_COMMON__INVALID_REQUEST;
					response.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 로그인 아이디가 유효한 경우
				else
				{
					// 해당 로그인 아이디의 사용자 정보를 가져온다.
					NNApplicationUser existUser = await m_dataProvider.GetUserByLoginId(loginId);

					// 해당 로그인 아이디의 사용자가 존재하는 경우
					if (existUser != null)
					{
						response.Code = Resource.EC_COMMON__ALREADY_EXIST;
						response.Message = Resource.EM_COMMON_ACCOUNT_ALREADY_EXIST;
					}
					// 해당 로그인 아이디의 사용자가 존재하지 않는 경우
					else
					{
						response.Result = EnumResponseResult.Success;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>특정 사용자 정보를 반환한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.view")]
		[HttpGet("{id}")]
		public async Task<ActionResult> Get([FromRoute] string id)
		{
			ResponseData<ResponseUserWithRoles> response = new ResponseData<ResponseUserWithRoles>();
			try
			{
				// 사용자 정보를 가져온다.
				response = await m_dataProvider.GetUser(id);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>특정 사용자의 역할/사용자 권한 목록을 반환한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.view")]
		[HttpGet("{id}/Claims")]
		public async Task<ActionResult> GetClaims([FromRoute] string id, int skip = 0, int countPerPage = int.MaxValue, string searchKeyword = "")
		{
			ResponseList<ResponseClaim> response = new ResponseList<ResponseClaim>();
			try
			{
				// 특정 역할의 권한 목록을 가져온다.
				response = await m_dataProvider.GetClaims(id, skip, countPerPage, searchKeyword);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>특정 사용자의 사용자 권한 목록을 반환한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.view")]
		[HttpGet("{id}/UserClaims")]
		public async Task<ActionResult> GetUserClaims([FromRoute] string id, int skip = 0, int countPerPage = int.MaxValue, string searchKeyword = "")
		{
			ResponseList<ResponseClaim> response = new ResponseList<ResponseClaim>();
			try
			{
				// 특정 역할의 권한 목록을 가져온다.
				response = await m_dataProvider.GetUserClaims(id, skip, countPerPage, searchKeyword);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>사용자 정보를 등록한다.</summary>
		/// <param name="request">사용자 등록 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.add")]
		[HttpPost]
		public async Task<ActionResult> Post([FromBody] RequestUserRegist request)
		{
			return Json(await m_dataProvider.Add(request));
		}

		/// <summary>사용자 정보를 수정한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="request">사용자 수정 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.update")]
		[HttpPut("{id}")]
		public async Task<ActionResult> Put([FromRoute] string id, [FromBody] RequestUserUpdate request)
		{
			ResponseData response = new ResponseData();
			try
			{
				// 사용자 수정
				response = await m_dataProvider.Update(id, request);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>사용자 정보를 수정한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="request">사용자 비밀번호 수정 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.update")]
		[HttpPut("{id}/ChangePassword")]
		public async Task<ActionResult> ChangePassword([FromRoute] string id, [FromBody] RequestUserChangePassword request)
		{
			ResponseData response = new ResponseData();
			try
			{
				// 사용자 비밀번호 변경
				response = await m_dataProvider.ChangePassword(id, request);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>사용자 정보를 삭제한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.remove")]
		[HttpDelete("{id}")]
		public async Task<ActionResult> Delete([FromRoute] string id)
		{
			ResponseData response = new ResponseData();
			try
			{
				// 사용자 삭제
				response = await m_dataProvider.Remove(id);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>특정 사용자에 권한을 등록한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="request">권한 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.add,common.account.users.update")]
		[HttpPost("{id}/Claims")]
		public async Task<ActionResult> AddClaim([FromRoute] string id, [FromBody] RequestAddClaimToUser request)
		{
			ResponseData response = new ResponseData();
			try
			{

				// 사용자에 권한을 등록한다.
				response = await m_dataProvider.AddClaim(id, request);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>특정 사용자에서 권한을 삭제한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="claimValue">권한값</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.roles.add,common.account.roles.update")]
		[HttpDelete("{id}/Claims/{claimValue}")]
		public async Task<ActionResult> RemoveClaim([FromRoute] string id, [FromRoute] string claimValue)
		{
			ResponseData response = new ResponseData();
			try
			{

				// 사용자에서 권한을 삭제한다.
				response = await m_dataProvider.RemoveClaim(id, claimValue);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>특정 사용자에 역할을 등록한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="request">역할 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.add,common.account.users.update")]
		[HttpPost("{id}/Roles")]
		public async Task<ActionResult> AddRole([FromRoute] string id, [FromBody] RequestAddRoleToUser request)
		{
			ResponseData response = new ResponseData();
			try
			{

				// 사용자에 역할을 등록한다.
				response = await m_dataProvider.AddRole(id, request);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}

		/// <summary>특정 사용자에서 역할을 삭제한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="roleName">역할명</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.roles.add,common.account.roles.update")]
		[HttpDelete("{id}/Roles/{roleName}")]
		public async Task<ActionResult> RemoveRole([FromRoute] string id, [FromRoute] string roleName)
		{
			ResponseData response = new ResponseData();
			try
			{

				// 사용자에서 역할을 삭제한다.
				response = await m_dataProvider.RemoveRole(id, roleName);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				response.Code = Resource.EC_COMMON__EXCEPTION;
				response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(response);
		}
	}
}
