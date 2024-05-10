﻿/*
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
using PortalData.Enums;
using PortalData.Requests.Accounts;
using PortalProviderInterface;
using Microsoft.Extensions.Logging;
using MTLib.CommonData;
using MTLib.Core;
using PortalProvider.Providers.Accounts;
using PortalResources;

namespace PortalSvr.Services
{
	/// <summary>계정 초기화 인터페이스</summary>
	public interface IAccountInitializer
	{
		/// <summary>계정이 하나도 없는 경우, 기본 계정을 생성한다.</summary>
		Task Initialize();
	}

	/// <summary>계정 초기화 클래스</summary>
	public class AccountInitializer : IAccountInitializer
	{
		/// <summary>사용자에 대한 프로바이더</summary>
		private readonly IUserProvider m_userProvider;

		/// <summary>API Key에 대한 프로바이더</summary>
		private readonly IApiKeyProvider m_apiKeyProvider;

		/// <summary>로거</summary>
		private readonly ILogger m_logger;

		/// <summary>생성자</summary>
		/// <param name="userProvider">사용자에 대한 프로바이더 객체</param>
		/// <param name="apiKeyProvider">API Key에 대한 프로바이더 객체</param>
		/// <param name="logger">로거</param>
		public AccountInitializer(
			IUserProvider userProvider,
			IApiKeyProvider apiKeyProvider,
			ILogger<AccountInitializer> logger
		)
		{
			m_userProvider = userProvider;
			m_apiKeyProvider = apiKeyProvider;
			m_logger = logger;
		}

		/// <summary>계정이 하나도 없는 경우, 기본 계정을 생성한다.</summary>
		public async Task Initialize()
		{
			try
			{
				// 사용자가 없는 경우
				if (await m_userProvider.UserCount() == 0)
				{
					// "admin" 계정을 생성하고, "Supervisor" 역할에 추가한다.
					await m_userProvider.Add(new RequestUserCreate()
					{
						LoginId = "admin",
						Email = "admin@pspace.co.kr",
						Name = "admin",
						Status = EnumUserStatus.Activated,
						Roles = new List<string>() { PredefinedRoleNames.RoleNameSupervisor }
					}, "qwe123", "qwe123");

					// "InternalService" 계정을 생성하고, "InternalService" 역할에 추가한다.
					// 해당 계정으로는 로그인 불가하며, API 키만 사용 가능하다.
					var Response = await m_userProvider.Add(new RequestUserCreate()
					{
						LoginId = "InternalService",
						Email = "InternalService@pspace.co.kr",
						Name = "InternalService",
						Status = EnumUserStatus.Activated,
						Roles = new List<string>() { PredefinedRoleNames.RoleNameInternalService }
					}, null, null);

					// "InternalService" 계정 생성에 성공한 경우, API KEY를 생성한다.
					if (Response.Result == EnumResponseResult.Success)
					{
						string APIKey = null;
						try
						{
							// 환경변수로 api key 값을 입력할 경우
							APIKey = Environment.GetEnvironmentVariable(Resource.ENV_SERVICE_API_KEY);
						}catch{
							APIKey = null;
						}

						// API 키를 발급한다.
						var ResponseApiKey = await m_apiKeyProvider.IssueApiKey(
							Guid.Parse(Response.Data.Id),
							new RequestApiKeyEx()
							{
								KeyName = Resource.INTERNAL_SERVICE_API_KEY,
								ExpireDate = DateTime.MaxValue,
								KeyValue = string.IsNullOrWhiteSpace(APIKey) ? KsanUserProvider.RandomTextLong(64) : APIKey
							}
						);

						if (ResponseApiKey.Result == EnumResponseResult.Success)
							m_logger.LogInformation("API KEY has been created. : {KeyValue}", ResponseApiKey.Data.KeyValue);
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
	}
}
