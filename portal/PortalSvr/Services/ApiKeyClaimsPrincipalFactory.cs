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
using System.Security.Claims;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Identity;
using Microsoft.Extensions.Options;
using MTLib.AspNetCore;

namespace PortalSvr.Services
{
	/// <summary>사용자 정의 User Claims Principal Factory 클래스</summary>
	public class ApiKeyClaimsPrincipalFactory : UserClaimsPrincipalFactory<NNApplicationUser, NNApplicationRole>
	{
		/// <summary>생성자</summary>
		/// <param name="userManager">사용자 관리자 객체</param>
		/// <param name="roleManager">역할 관리자 객체</param>
		/// <param name="options">옵션 객체</param>
		public ApiKeyClaimsPrincipalFactory(UserManager<NNApplicationUser> userManager, RoleManager<NNApplicationRole> roleManager, IOptions<IdentityOptions> options) : base(userManager, roleManager, options)
		{
		}

		/// <summary>사용자 로그인 정보를 생성한다. </summary>
		/// <param name="user">로그인한 사용자 정보</param>
		/// <returns></returns>
		public async override Task<ClaimsPrincipal> CreateAsync(NNApplicationUser user)
		{
			// 사용자 로그인 정보를 생성한다.
			ClaimsPrincipal principal = await base.CreateAsync(user);

			// 사용자명을 추가한다.
			((ClaimsIdentity)principal?.Identity)?.AddClaims(new[] {
				new Claim(ClaimTypes.Name, user.Name),
			});

			return principal;
		}
	}
}