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
using PortalData;
using PortalProviderInterface;
using PortalResources;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.Extensions.DependencyInjection;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalSvr.Services
{
	/// <summary>API Key 인증 확인 클래스</summary>
	public class ApiKeyAuthorize : AuthorizeAttribute, IAuthorizationFilter
	{
		/// <summary>API KEY 인증 헤더명</summary>
		public static string AuthorizationHeaderName = "Authorization";

		/// <summary>권한 확인</summary>
		/// <param name="context"></param>
		public void OnAuthorization(AuthorizationFilterContext context)
		{
			// 인증된 상태인 경우
			if (context.HttpContext.User.Identity != null && context.HttpContext.User.Identity.IsAuthenticated)
				return;

			// API Key 관련 헤더가 존재하는 경우
			if (context.HttpContext.Request.Headers.ContainsKey(AuthorizationHeaderName) || context.HttpContext.Request.Headers.ContainsKey(AuthorizationHeaderName.ToLower()))
			{
				// API Key 프로바이더 객체를 가져온다.
				var ApiKeyProvider = context.HttpContext.RequestServices.GetService<IApiKeyProvider>();

				// API Key 프로바이더 객체가 존재하는 경우
				if (ApiKeyProvider != null)
				{
					// API Key 관련 헤더 값을 가져온다.
					string AuthorizationHeader = context.HttpContext.Request.Headers[AuthorizationHeaderName];
					if (AuthorizationHeader.IsEmpty())
						AuthorizationHeader = context.HttpContext.Request.Headers[AuthorizationHeaderName.ToLower()];

					// API Key 정보를 가져온다.
					var ResponseApiKey = ApiKeyProvider.GetApiKey(ParseApiKeyFromHeader(AuthorizationHeader)).Result;

					// API Key 정보를 가져오는데 성공한 경우
					if (ResponseApiKey.Result == EnumResponseResult.Success)
					{
						// 사용자 관리자
						var UserManager = context.HttpContext.RequestServices.GetService<UserManager<NNApplicationUser>>();
						// 로그인 관리자
						var SignInManager = context.HttpContext.RequestServices.GetService<SignInManager<NNApplicationUser>>();
						// Claims Principal Factory
						var ClaimsPrincipalFactory = context.HttpContext.RequestServices.GetService<IUserClaimsPrincipalFactory<NNApplicationUser>>();

						// 사용자/로그인 관리자가 유효한 경우
						if (UserManager != null && SignInManager != null && ClaimsPrincipalFactory != null)
						{
							// API 키에 대한 사용자 정보를 가져온다.
							var User = UserManager.FindByIdAsync(ResponseApiKey.Data.UserId).Result;

							// 해당 사용자로 로그인 처리
							SignInManager.SignInAsync(User, false).Wait();

							// 로그인 정보를 저장한다. (이번 요청부터 로그인된 상태를 처리하기 하기 위해서)
							context.HttpContext.User = ClaimsPrincipalFactory.CreateAsync(User).Result;

							return;
						}
					}
				}
			}

			// 로그인이 필요로 설정
			var Response = new ResponseData();
			Response.IsNeedLogin = true;
			Response.Code = Resource.EC_COMMON__NEED_LOGIN;
			Response.Message = Resource.EM_COMMON__NEED_LOGIN;
			context.Result = new UnauthorizedObjectResult(Response);
		}

		/// <summary>헤더의 인증 정보에서 API Key를 가져온다.</summary>
		/// <param name="authorizationHeader">인증 정보 헤더 값</param>
		/// <returns>API Key 문자열</returns>
		protected string ParseApiKeyFromHeader(string authorizationHeader)
		{
			string Result = "";

			// 인증 정보가 유효한 경우
			if (!authorizationHeader.IsEmpty())
			{
				const string prefix = "bearer ";

				Result = authorizationHeader;

				// bearer로 시작하는 경우
				if (authorizationHeader.ToLower().StartsWith(prefix))
				{
					// API Key를 가져온다.
					Result = authorizationHeader.Substring(prefix.Length);
				}
			}

			return Result;
		}
	}
}