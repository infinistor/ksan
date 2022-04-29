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
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Accounts;
using PortalProviderInterface;
using PortalResources;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.Core;

namespace PortalSvr.Controllers.Accounts
{
	/// <summary>계정 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[Authorize]
	public class AccountController : BaseController
	{

		/// <summary>데이터 프로바이더</summary>
		private readonly IAccountProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public AccountController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<AccountController> logger,
			IAccountProvider dataProvider
			)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>회원 가입을 처리한다.</summary>
		/// <param name="request">회원 가입 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[HttpPost("Register")]
		[AllowAnonymous]
		public async Task<JsonResult> Register([FromBody] RequestRegister request)
		{
			return Json(await m_dataProvider.Create(request, HttpContext.Request));
		}

		/// <summary>이메일 주소 인증 처리</summary>
		/// <param name="request">이메일 인증 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[HttpPost("ConfirmEmail")]
		[AllowAnonymous]
		public async Task<JsonResult> ConfirmEmail([FromBody] RequestConfirmEmail request)
		{
			return Json(await m_dataProvider.ConfirmEmail(request));
		}

		/// <summary>로그인 처리</summary>
		/// <param name="request">로그인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[HttpPost("Login")]
		[AllowAnonymous]
		public async Task<JsonResult> Login([FromBody] RequestLogin request)
		{
			return Json(await m_dataProvider.Login(request, this.Request));
		}

		/// <summary>로그인한 사용자 정보를 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[HttpGet("Login")]
		public async Task<JsonResult> Login()
		{
			return Json(await m_dataProvider.GetLogin(User));
		}

		/// <summary>로그인 여부를 가져온다.</summary>
		/// <param name="requireRoles">필요한 역할명 목록 (',' 으로 구분)</param>
		/// <returns>결과 JSON 문자열</returns>
		[HttpGet("CheckLogin/{requireRoles?}")]
		[AllowAnonymous]
		public JsonResult CheckLogin([FromRoute] string requireRoles = "")
		{
			return Json(m_dataProvider.CheckLogin(User, requireRoles));
		}

		/// <summary>로그 아웃</summary>
		/// <returns>결과 JSON 문자열</returns>
		[HttpGet("Logout")]
		[AllowAnonymous]
		public async Task<JsonResult> Logout()
		{
			return Json(await m_dataProvider.Logout());
		}

		/// <summary>현재 로그인한 사용자의 비밀번호를 변경한다.</summary>
		/// <param name="request">비밀번호 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[HttpPost("ChangePassword")]
		public async Task<JsonResult> ChangePassword([FromBody] RequestChangePassword request)
		{
			return Json(await m_dataProvider.ChangePassword(User, request));
		}

		/// <summary>비밀번호 찾기 요청</summary>
		/// <param name="request">비밀번호 찾기 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[HttpPost("ForgotPassword")]
		[AllowAnonymous]
		public async Task<JsonResult> ForgotPassword([FromBody] RequestForgetPassword request)
		{
			return Json(await m_dataProvider.ForgotPassword(request, HttpContext.Request));
		}

		/// <summary>비밀번호 재설정</summary>
		/// <param name="request">비밀번호 재설정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[HttpPost("ResetPassword")]
		[AllowAnonymous]
		public async Task<JsonResult> ResetPassword([FromBody] RequestResetPassword request)
		{
			return Json(await m_dataProvider.ResetPassword(request));
		}

		/// <summary>현재 로그인한 사용자 정보를 수정한다.</summary>
		/// <param name="request">비밀번호 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[HttpPut("Update")]
		public async Task<JsonResult> Update([FromBody] RequestUpdate request)
		{
			return Json(await m_dataProvider.Update(User, request));
		}

		/// <summary>로그인한 사용자의 권한 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[HttpGet("Claims")]
		public async Task<JsonResult> GetClaims()
		{
			return Json(await m_dataProvider.GetClaims(User));
		}

		/// <summary>로그인한 사용자의 권한 중 해당 권한이 존재하는지 확인한다.</summary>
		/// <param name="claimValue">검사할 권한 값</param>
		/// <returns>결과 JSON 문자열</returns>
		[HttpGet("Claims/{claimValue}")]
		public async Task<JsonResult> HasClaim([FromRoute] string claimValue)
		{
			return Json(await m_dataProvider.HasClaim(User, claimValue));
		}

		#region 직접 호출하지 않지만 로그인 혹은 권한이 필요한 경우 호출되는 Action
		/// <summary>로그인이 필요하다는 내용을 전달한다.</summary>
		/// <returns>로그인 필요 정보가 포함된 객체</returns>
		[HttpGet("NeedLogin")]
		[HttpPost("NeedLogin")]
		[HttpPut("NeedLogin")]
		[HttpPatch("NeedLogin")]
		[HttpDelete("NeedLogin")]
		[HttpHead("NeedLogin")]
		[HttpOptions("NeedLogin")]
		[AllowAnonymous]
		public UnauthorizedObjectResult NeedLogin()
		{
			ResponseData response = new ResponseData();
			try
			{
				// 로그인이 필요로 설정
				response.IsNeedLogin = true;
				response.Code = Resource.EC_COMMON__NEED_LOGIN;
				response.Message = Resource.EM_COMMON__NEED_LOGIN;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return Unauthorized(response);
		}

		/// <summary>로그인이 필요하다는 내용을 전달한다.</summary>
		/// <returns>로그인 필요 정보가 포함된 객체</returns>
		[HttpGet("AccessDenied")]
		[HttpPost("AccessDenied")]
		[HttpPut("AccessDenied")]
		[HttpPatch("AccessDenied")]
		[HttpDelete("AccessDenied")]
		[HttpHead("AccessDenied")]
		[HttpOptions("AccessDenied")]
		[AllowAnonymous]
		public UnauthorizedObjectResult AccessDenied()
		{
			ResponseData response = new ResponseData();
			try
			{
				response.IsNeedLogin = false;
				response.AccessDenied = true;
				response.Code = Resource.EC_COMMON__ACCESS_DENIED;
				response.Message = Resource.EM_COMMON__ACCESS_DENIED;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return Unauthorized(response);
		}
		#endregion
	}
}
