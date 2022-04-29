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
using System.Linq;
using System.Threading.Tasks;
using PortalModels;
using PortalProviderInterface;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.Core;

namespace PortalSvr.Controllers
{
	/// <summary>기본 컨트롤러 클래스</summary>
	[ResponseCache(CacheProfileName = "Default")]
	public class BaseController : Controller
	{
		/// <summary>디비 컨텍스트</summary>
		protected readonly PortalModel m_dbContext;
		/// <summary>설정 정보</summary>
		protected readonly IConfiguration m_configuration;
		/// <summary>프로바이더 목록</summary>
		protected readonly List<IBaseProvider> m_providers = new List<IBaseProvider>();
		/// <summary>사용자 관리자</summary>
		protected UserManager<NNApplicationUser> m_userManager;
		/// <summary>로거</summary>
		protected readonly ILogger m_logger;

		/// <summary>생성자</summary>
		public BaseController()
		{

		}

		/// <summary>생성자</summary>
		/// <param name="configuration">환경 설정</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="provider">기본 프로바이더</param>
		public BaseController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger logger,
			IBaseProvider provider)
			: this(null, configuration, userManager, logger, provider)
		{
		}

		/// <summary>생성자</summary>
		/// <param name="configuration">환경 설정</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="providers">프로바이더 목록</param>
		public BaseController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger logger,
			params IBaseProvider[] providers)
		{
			m_dbContext = null;
			m_configuration = configuration;
			if (providers != null)
				m_providers.AddRange(providers);
			m_userManager = userManager;
			m_logger = logger;
		}

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">환경 설정</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="providers">프로바이더 목록</param>
		public BaseController(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger logger,
			params IBaseProvider[] providers)
		{
			m_dbContext = dbContext;
			m_configuration = configuration;
			if (providers != null)
				m_providers.AddRange(providers);
			m_userManager = userManager;
			m_logger = logger;
		}

		/// <summary>로그인 사용자 정보</summary>
		public NNApplicationUser LoginUser
		{
			get
			{
				if (m_userManager == null)
					return null;

				if (m_loginUser == null && m_providers != null && m_providers.Count > 0)
					m_loginUser = m_providers.Where(i => i.LoginUser != null).Select(i => i.LoginUser).FirstOrDefault();

				return m_loginUser;
			}
		}
		private NNApplicationUser m_loginUser;

		/// <summary>로그인 사용자 아이디</summary>
		public string LoginUserId
		{
			get
			{
				if (this.LoginUser != null)
					return this.LoginUser.Id.ToString();
				return "";
			}
		}

		/// <summary>로그인 사용자명</summary>
		public string LoginUserName
		{
			get
			{
				if (this.LoginUser != null)
					return this.LoginUser.Name;
				return "";
			}
		}

		/// <summary>접속한 아이피 주소</summary>
		public string UserIpAddress
		{
			get
			{
				if (m_userManager == null)
					return null;

				if (m_userIpAddress.IsEmpty() && m_providers != null && m_providers.Count > 0)
					m_userIpAddress = m_providers.Where(i => i.UserIpAddress != null).Select(i => i.UserIpAddress).FirstOrDefault();

				return m_userIpAddress;
			}
		}
		private string m_userIpAddress = "";

		/// <summary>Action이 실행되기 전에 호출되는 함수</summary>
		/// <param name="context">액션 실행 컨텍스트</param>
		/// <param name="next">액션 실행 함수</param>
		/// <returns>실행 결과</returns>
		public override Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
		{
			try
			{
				if (m_providers != null)
				{
					foreach (IBaseProvider provider in m_providers)
					{
						provider.UserClaimsPrincipal = User;
						provider.UserIpAddress = HttpContext.GetRequestIp();
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return base.OnActionExecutionAsync(context, next);
		}
	}
}
