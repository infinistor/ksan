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
using System.Security.Claims;
using System.Threading.Tasks;
using PortalData;
using PortalData.Responses.Accounts;
using PortalProviderInterface;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalSvr.Services
{
	/// <summary>ClaimRequirement Attribute 클래스</summary>
	public class ClaimRequirementAttribute : TypeFilterAttribute
	{
		/// <summary>생성자</summary>
		/// <param name="claimType">권한 타입</param>
		/// <param name="claimValue">권한 값</param>
		public ClaimRequirementAttribute(string claimType, string claimValue) : base(typeof(ClaimRequirementFilter))
		{
			Arguments = new object[] { new Claim(claimType, claimValue) };
		}
	}

	/// <summary>ClaimRequirement 필터 클래스</summary>
	public class ClaimRequirementFilter : IAsyncActionFilter
	{
		/// <summary>권한 객체</summary>
		readonly List<Claim> m_claims = new List<Claim>();

		/// <summary>권한 값 객체</summary>
		readonly List<string> m_claimValues = new List<string>();

		/// <summary>데이터 프로바이더</summary>
		private readonly IAccountProvider m_accountProvider;

		/// <summary>생성자</summary>
		/// <param name="accountProvider">사용자 데이터 프로바이더</param>
		/// <param name="claim">가져야 하는 권한 객체</param>
		public ClaimRequirementFilter(IAccountProvider accountProvider, Claim claim)
		{
			string[] claimValues = claim.Value.Split(new[] { ' ', ',' }, StringSplitOptions.RemoveEmptyEntries);
			if (claimValues.Length > 0)
			{
				foreach (var claimValue in claimValues)
					m_claims.Add(new Claim(claim.Type, claimValue));

				m_claimValues = m_claims.Select(i => i.Value).ToList();
			}
			m_accountProvider = accountProvider;
		}

		/// <summary>인증/권한 검사</summary>
		/// <param name="context">인증/권한 필터 Context 객체</param>
		/// <param name="next">다음에 실행할 대리자 객체</param>
		public async Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
		{
			bool hasClaim = false;
			try
			{
				// 권한을 가져온다.
				ResponseList<ResponseClaim> responseClaims = await m_accountProvider.GetClaims(context.HttpContext.User);
				// 권한을 가지고 있는지 확인한다.
				if (responseClaims.Result == EnumResponseResult.Success && responseClaims.Data.Items.Exists(i => m_claimValues.Contains(i.ClaimValue)))
					hasClaim = true;

				// 권한을 가지지 못한 경우
				if (!hasClaim)
					context.Result = new ForbidResult();
				// 권한을 가지는 경우
				else
					await next();
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				throw;
			}
		}
	}
}
