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
using System.Linq;
using System.Threading.Tasks;
using PortalProviderInterface;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using MTLib.AspNetCore;
using MTLib.Core;

namespace PortalSvr.Services
{
	/// <summary>요청에 대한 접근 아이피 검사 미들웨어 클래스</summary>
	public class AllowConnectionIpCheckMiddleware
	{
		/// <summary>Request를 처리하는 함수</summary>
		private readonly RequestDelegate _next;

		/// <summary>생성자</summary>
		/// <param name="next">Request에 대한 다음 처리 함수</param>
		public AllowConnectionIpCheckMiddleware(RequestDelegate next)
		{
			_next = next;
		}

		/// <summary>처리 함수</summary>
		/// <param name="context">HttpContext 객체</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="roleManager">역할 관리자</param>
		/// <param name="allowConnectionIpManager">허용 주소 검사 매니저 객체</param>
		public async Task Invoke(HttpContext context, UserManager<NNApplicationUser> userManager, RoleManager<NNApplicationRole> roleManager, IAllowConnectionIpsManager allowConnectionIpManager)
		{
			List<string> roleIds = new List<string>();
			bool allowIp = false;
			try
			{
				// 전체 허용이 아니고 항상 허용되는 URL 경로가 아닌 경우
				if (!allowConnectionIpManager.IsAllAllowed && !IsAlwaysAllowPath(context.Request))
				{
					if (userManager != null && roleManager != null)
					{
						// 로그인한 사용자에 대한 객체를 가져온다.
						NNApplicationUser user = await userManager.GetUserAsync(context.User);

						// 사용자가 가지는 역할명 목록을 가져온댜.
						IList<string> userRoleNames;

						// 로그인 상태인 경우
						if (user != null)
							// 사용자가 가지는 역할명 목록을 가져온댜.
							userRoleNames = await userManager.GetRolesAsync(user);
						// 로그인 상태가 아닌 경우
						else
							userRoleNames = new List<string>();

						// 사용자가 가지는 역할명 목록이 존재하는 경우
						if (userRoleNames.Count > 0)
						{
							// 사용자가 가지는 역할 정보를 가져온다.
							List<string> userRoleIds = await roleManager.Roles.Where(i => userRoleNames.Contains(i.Name)).Select(i => i.Id.ToString()).ToListAsync();

							// 역할 정보가 존재하는 경우
							if (userRoleIds.Count > 0)
							{
								roleIds.AddRange(userRoleIds);
							}
						}

						// 모든 역할 ID에 대해서 처리
						foreach (var roleId in roleIds)
						{
							// 접속한 아이피가 사용자가 가지는 역할에서 허용된 아이피인지 검사한다.
							if (allowConnectionIpManager.IsAllowIp(roleId, context.Connection.RemoteIpAddress))
							{
								allowIp = true;
								break;
							}
						}
					}

					// 허용된 아이피가 아닌 경우
					if (!allowIp)
					{
						string[] pathItems = context.Request.Path.ToString().Split(new[] { '/' }, StringSplitOptions.RemoveEmptyEntries);
						if (pathItems.Length >= 2 && pathItems[0] == "api")
							context.Response.Redirect($"/{pathItems[0]}/{pathItems[1]}/Account/AccessDenied");
						else
							context.Response.Redirect("/api/v1/Account/AccessDenied");
					}
					// 허용된 아이피인 경우
					else
						await _next.Invoke(context);
				}
				// 항상 허용되는 URL 경로인 경우
				else
					await _next.Invoke(context);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				throw;
			}
		}

		/// <summary>항상 허용되는 URL 경로인지 여부를 반환한다.</summary>
		/// <param name="httpRequest">HttpRequest 객체</param>
		/// <returns>항상 허용되는 URL 경로인지 여부</returns>
		private bool IsAlwaysAllowPath(HttpRequest httpRequest)
		{
			bool Result = false;

			try
			{
				if (httpRequest.Path.Value != null && httpRequest.Path.Value.ToLower().Contains("/account/")
				//|| httpRequest.Path.Value.ToLower().EndsWith("/account/login")
				//|| httpRequest.Path.Value.ToLower().EndsWith("/account/logout")
				//|| httpRequest.Path.Value.ToLower().EndsWith("/account/needlogin")
				)
					Result = true;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return Result;
		}
	}

	/// <summary>요청에 대한 접근 아이피 검사 미들웨어 확장 클래스</summary>
	public static class AllowConnectionIpCheckMiddlewareExtensions
	{
		/// <summary>요청에 대한 접근 아이피 검사 미들웨어 사용 처리</summary>
		/// <param name="builder">어플리케이션 빌더 객체</param>
		/// <returns>어플리케이션 빌더 객체</returns>
		public static IApplicationBuilder UseAllowConnectionIpCheckMiddleware(this IApplicationBuilder builder)
		{
			return builder.UseMiddleware<AllowConnectionIpCheckMiddleware>();
		}
	}
}
