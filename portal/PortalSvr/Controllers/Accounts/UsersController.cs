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
		/// <param name="SearchRoleName">검색할 역할명</param>
		/// <param name="RegStartDate">가입일 검색 시작일자</param>
		/// <param name="RegEndDate">가입일 검색 종료일자</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드목록 (LoginId, Email, Name)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.list")]
		[HttpGet]
		public async Task<ActionResult> Get(
			string SearchRoleName = "", DateTime? RegStartDate = null, DateTime? RegEndDate = null,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Response = new ResponseList<ResponseUserWithRoles>();
			try
			{
				// 사용자 목록을 반환한다.
				Response = await m_dataProvider.GetUsers(
					SearchRoleName, RegStartDate, RegEndDate,
					Skip, CountPerPage,
					OrderFields, OrderDirections,
					SearchFields, SearchKeyword
				);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>로그인 아이디 중복 여부를 검사한다.</summary>
		/// <param name="LoginId">로그인 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.view")]
		[HttpGet("Unique/{LoginId}")]
		public async Task<ActionResult> IsUniqueLoginId([FromRoute] string LoginId)
		{
			var Response = new ResponseData();
			try
			{
				// 로그인 아이디가 유효하지 않은 경우
				if (LoginId.IsEmpty())
				{
					Response.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Response.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 로그인 아이디가 유효한 경우
				else
				{
					// 해당 로그인 아이디의 사용자 정보를 가져온다.
					NNApplicationUser existUser = await m_dataProvider.GetUserByLoginId(LoginId);

					// 해당 로그인 아이디의 사용자가 존재하는 경우
					if (existUser != null)
					{
						Response.Code = Resource.EC_COMMON__ALREADY_EXIST;
						Response.Message = Resource.EM_COMMON_ACCOUNT_ALREADY_EXIST;
					}
					// 해당 로그인 아이디의 사용자가 존재하지 않는 경우
					else
					{
						Response.Result = EnumResponseResult.Success;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>특정 사용자 정보를 반환한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.view")]
		[HttpGet("{Id}")]
		public async Task<ActionResult> Get([FromRoute] string Id)
		{
			var Response = new ResponseData<ResponseUserWithRoles>();
			try
			{
				// 사용자 정보를 가져온다.
				Response = await m_dataProvider.GetUser(Id);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>특정 사용자의 역할/사용자 권한 목록을 반환한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.view")]
		[HttpGet("{Id}/Claims")]
		public async Task<ActionResult> GetClaims([FromRoute] string Id, int Skip = 0, int CountPerPage = int.MaxValue, string SearchKeyword = "")
		{
			var Response = new ResponseList<ResponseClaim>();
			try
			{
				// 특정 역할의 권한 목록을 가져온다.
				Response = await m_dataProvider.GetClaims(Id, Skip, CountPerPage, SearchKeyword);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>특정 사용자의 사용자 권한 목록을 반환한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.view")]
		[HttpGet("{Id}/UserClaims")]
		public async Task<ActionResult> GetUserClaims([FromRoute] string Id, int Skip = 0, int CountPerPage = int.MaxValue, string SearchKeyword = "")
		{
			var Response = new ResponseList<ResponseClaim>();
			try
			{
				// 특정 역할의 권한 목록을 가져온다.
				Response = await m_dataProvider.GetUserClaims(Id, Skip, CountPerPage, SearchKeyword);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>사용자 정보를 등록한다.</summary>
		/// <param name="Request">사용자 등록 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.add")]
		[HttpPost]
		public async Task<ActionResult> Post([FromBody] RequestUserRegist Request)
		{
			return Json(await m_dataProvider.Add(Request));
		}

		/// <summary>사용자 정보를 수정한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">사용자 수정 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.update")]
		[HttpPut("{Id}")]
		public async Task<ActionResult> Put([FromRoute] string Id, [FromBody] RequestUserUpdate Request)
		{
			var Response = new ResponseData();
			try
			{
				// 사용자 수정
				Response = await m_dataProvider.Update(Id, Request);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>사용자 정보를 수정한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">사용자 비밀번호 수정 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.update")]
		[HttpPut("{Id}/ChangePassword")]
		public async Task<ActionResult> ChangePassword([FromRoute] string Id, [FromBody] RequestUserChangePassword Request)
		{
			var Response = new ResponseData();
			try
			{
				// 사용자 비밀번호 변경
				Response = await m_dataProvider.ChangePassword(Id, Request);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>사용자 정보를 삭제한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.remove")]
		[HttpDelete("{Id}")]
		public async Task<ActionResult> Delete([FromRoute] string Id)
		{
			var Response = new ResponseData();
			try
			{
				// 사용자 삭제
				Response = await m_dataProvider.Remove(Id);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>특정 사용자에 권한을 등록한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">권한 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.add,common.account.users.update")]
		[HttpPost("{Id}/Claims")]
		public async Task<ActionResult> AddClaim([FromRoute] string Id, [FromBody] RequestAddClaimToUser Request)
		{
			var Response = new ResponseData();
			try
			{

				// 사용자에 권한을 등록한다.
				Response = await m_dataProvider.AddClaim(Id, Request);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>특정 사용자에서 권한을 삭제한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="ClaimValue">권한값</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.roles.add,common.account.roles.update")]
		[HttpDelete("{Id}/Claims/{ClaimValue}")]
		public async Task<ActionResult> RemoveClaim([FromRoute] string Id, [FromRoute] string ClaimValue)
		{
			var Response = new ResponseData();
			try
			{

				// 사용자에서 권한을 삭제한다.
				Response = await m_dataProvider.RemoveClaim(Id, ClaimValue);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>특정 사용자에 역할을 등록한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">역할 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.add,common.account.users.update")]
		[HttpPost("{Id}/Roles")]
		public async Task<ActionResult> AddRole([FromRoute] string Id, [FromBody] RequestAddRoleToUser Request)
		{
			var Response = new ResponseData();
			try
			{

				// 사용자에 역할을 등록한다.
				Response = await m_dataProvider.AddRole(Id, Request);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}

		/// <summary>특정 사용자에서 역할을 삭제한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="RoleName">역할명</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.roles.add,common.account.roles.update")]
		[HttpDelete("{Id}/Roles/{RoleName}")]
		public async Task<ActionResult> RemoveRole([FromRoute] string Id, [FromRoute] string RoleName)
		{
			var Response = new ResponseData();
			try
			{

				// 사용자에서 역할을 삭제한다.
				Response = await m_dataProvider.RemoveRole(Id, RoleName);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Response.Code = Resource.EC_COMMON__EXCEPTION;
				Response.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Json(Response);
		}
	}
}
