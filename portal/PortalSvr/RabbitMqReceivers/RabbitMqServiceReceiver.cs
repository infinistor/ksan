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
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Services;
using PortalData.Responses.Accounts;
using PortalProvider.Providers.RabbitMQ;
using PortalProviderInterface;
using PortalResources;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.Reflection;
using Newtonsoft.Json;
using IServiceProvider = PortalProviderInterface.IServiceProvider;

// ReSharper disable TemplateIsNotCompileTimeConstantProblem

namespace PortalSvr.RabbitMQReceivers
{
	/// <summary>Rabbit MQ로 부터 서비스 정보를 수신하는 클래스</summary>
	public class RabbitMQServiceReceiver : RabbitMQReceiver
	{
		/// <summary>생성자</summary>
		/// <param name="rabbitMqOptions">Rabbit MQ 설정 옵션 객체</param>
		/// <param name="logger">로거</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		public RabbitMQServiceReceiver(
			IOptions<RabbitMQConfiguration> rabbitMqOptions,
			ILogger<RabbitMQServiceReceiver> logger,
			IServiceScopeFactory serviceScopeFactory
		) : base(
			"ksan-api-portal.services",
			new[]
			{
				"*.services.*",
				"*.services.*.*",
				"*.services.*.*.*",
				"portalsvr.services.*",
				"portalsvr.services.*.*",
				"portalsvr.services.*.*.*"
			},
			rabbitMqOptions,
			logger,
			serviceScopeFactory)
		{
		}

		/// <summary>메시지 처리</summary>
		/// <param name="RoutingKey">라우팅 키</param>
		/// <param name="Body">내용</param>
		protected override async Task<ResponseMqData> HandleMessage(string RoutingKey, byte[] Body)
		{
			var Result = new ResponseMqData();

			try
			{
				// 수신된 데이터를 문자열로 변환
				string json = Body.GetString();

				using var Scope = m_serviceScopeFactory.CreateScope();
				// API 키 프로바이더를 가져온다.
				var ApiKeyProvider = Scope.ServiceProvider.GetService<IApiKeyProvider>();
				if (ApiKeyProvider == null)
					return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

				// 내부 시스템 API 키 정보를 가져온다.
				var ApiKey = await ApiKeyProvider.GetMainApiKey();

				// API 키를 가져오는데 실패한 경우
				if (ApiKey == null)
					return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 서비스 상태 관련인 경우
				if (RoutingKey.EndsWith("services.state"))
				{
					// 처리할 프로바이더를 가져온다.
					var DataProvider = Scope.ServiceProvider.GetService<IServiceProvider>();
					if (DataProvider == null)
						return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

					// json을 객체로 변환
					var Request = JsonConvert.DeserializeObject<RequestServiceState>(json);

					// 서버 상태 수정
					var Response = await DataProvider.UpdateState(Request, ApiKey.UserId, ApiKey.UserName);

					Result.CopyValueFrom(Response);
					Result.IsProcessed = true;
				}
				// 서비스 HA Action 관련인 경우
				else if (RoutingKey.EndsWith("services.haaction"))
				{
					// 처리할 프로바이더를 가져온다.
					var DataProvider = Scope.ServiceProvider.GetService<IServiceProvider>();
					if (DataProvider == null)
						return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

					// json을 객체로 변환
					var Request = JsonConvert.DeserializeObject<RequestServiceHaAction>(json);

					// 서비스 HA 상태 수정
					var Response = await DataProvider.UpdateHaAction(Request, ApiKey.UserId, ApiKey.UserName);

					Result.CopyValueFrom(Response);
					Result.IsProcessed = true;
				}
				// 서비스 사용 정보 관련인 경우
				else if (RoutingKey.EndsWith("services.usage"))
				{
					// 처리할 프로바이더를 가져온다.
					var DataProvider = Scope.ServiceProvider.GetService<IServiceProvider>();
					if (DataProvider == null)
						return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

					// json을 객체로 변환
					var Request = JsonConvert.DeserializeObject<RequestServiceUsage>(json);

					// 서비스 사용 정보 수정
					var Response = await DataProvider.UpdateUsage(Request);

					Result.CopyValueFrom(Response);
					Result.IsProcessed = true;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}
	}
}