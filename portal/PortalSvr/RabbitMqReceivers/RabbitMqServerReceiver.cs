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
using PortalData.Requests.Disks;
using PortalData.Requests.Networks;
using PortalData.Requests.Servers;
using PortalProvider.Providers.RabbitMq;
using PortalProviderInterface;
using PortalResources;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.Reflection;
using Newtonsoft.Json;
// ReSharper disable TemplateIsNotCompileTimeConstantProblem

namespace PortalSvr.RabbitMqReceivers
{
	/// <summary>Rabbit MQ로 부터 서버 정보를 수신하는 클래스</summary>
	public class RabbitMqServerReceiver : RabbitMqReceiver
	{
		/// <summary>생성자</summary>
		/// <param name="rabbitMqOptions">Rabbit MQ 설정 옵션 객체</param>
		/// <param name="logger">로거</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		public RabbitMqServerReceiver(
			IOptions<RabbitMqConfiguration> rabbitMqOptions,
			ILogger<RabbitMqServerReceiver> logger,
			IServiceScopeFactory serviceScopeFactory
		) : base(
			"portalsvr.servers",
			new[]
			{
				"*.servers.*",
				"*.servers.*.*",
				"*.servers.*.*.*",
				"portalsvr.servers.*",
				"portalsvr.servers.*.*",
				"portalsvr.servers.*.*.*"
			},
			rabbitMqOptions,
			logger,
			serviceScopeFactory)
		{
			try
			{
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				throw;
			}
		}

		/// <summary>메시지 처리</summary>
		/// <param name="RoutingKey">라우팅 키</param>
		/// <param name="Body">내용</param>
		protected override async Task<ResponseMqData> HandleMessage(string RoutingKey, byte[] Body)
		{
			ResponseMqData Result = new ResponseMqData();

			try
			{
				// 수신된 데이터를 문자열로 변환
				string json = Body.GetString();
				m_logger.LogDebug(json);

				using (var scope = m_serviceScopeFactory.CreateScope())
				{
					// API 키 프로바이더를 가져온다.
					var ApiKeyProvider = scope.ServiceProvider.GetService<IApiKeyProvider>();
					if (ApiKeyProvider == null)
						return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

					// 내부 시스템 API 키 정보를 가져온다.
					var ResponseApiKey = await ApiKeyProvider.GetMainApiKey();

					// API 키를 가져오는데 실패한 경우
					if (ResponseApiKey == null)
						return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

					// 서버 상태 관련인 경우
					if (RoutingKey.EndsWith("servers.state"))
					{
						// 처리할 프로바이더를 가져온다.
						var DataProvider = scope.ServiceProvider.GetService<IServerProvider>();
						if (DataProvider == null)
							return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

						// json을 객체로 변환
						var Request = JsonConvert.DeserializeObject<RequestServerState>(json);

						// 서버 상태 수정
						var Response = await DataProvider.UpdateState(Request, ResponseApiKey.UserId, ResponseApiKey.UserName);

						Result.CopyValueFrom(Response);
						Result.IsProcessed = true;
					}
					// 서버 사용 정보 관련인 경우
					else if (RoutingKey.EndsWith("servers.usage"))
					{
						// 처리할 프로바이더를 가져온다.
						var DataProvider = scope.ServiceProvider.GetService<IServerProvider>();
						if (DataProvider == null)
							return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

						// json을 객체로 변환
						var Request = JsonConvert.DeserializeObject<RequestServerUsage>(json);

						// 서버 사용 정보 수정
						var Response = await DataProvider.UpdateUsage(Request);

						Result.CopyValueFrom(Response);
						Result.IsProcessed = true;
					}
					// 네트워크 인터페이스 연결 상태 관련인 경우
					else if (RoutingKey.EndsWith("servers.interfaces.linkstate"))
					{
						// 처리할 프로바이더를 가져온다.
						var DataProvider = scope.ServiceProvider.GetService<INetworkInterfaceProvider>();
						if (DataProvider == null)
							return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

						// json을 객체로 변환
						var Request = JsonConvert.DeserializeObject<RequestNetworkInterfaceLinkState>(json);

						// 네트워크 인터페이스 연결 상태 수정
						var Response = await DataProvider.UpdateLinkState(Request);

						Result.CopyValueFrom(Response);
						Result.IsProcessed = true;
					}
					// 네트워크 인터페이스 사용 정보 관련인 경우
					else if (RoutingKey.EndsWith("servers.interfaces.usage"))
					{
						// 처리할 프로바이더를 가져온다.
						var DataProvider = scope.ServiceProvider.GetService<INetworkInterfaceProvider>();
						if (DataProvider == null)
							return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

						// json을 객체로 변환
						var Request = JsonConvert.DeserializeObject<RequestNetworkInterfaceUsage>(json);

						// 네트워크 인터페이스 사용 정보 수정
						var Response = await DataProvider.UpdateUsage(Request);

						Result.CopyValueFrom(Response);
						Result.IsProcessed = true;
					}
					// 디스크 사용량 관련인 경우
					else if (RoutingKey.EndsWith("servers.disks.usage"))
					{
						// 처리할 프로바이더를 가져온다.
						var DataProvider = scope.ServiceProvider.GetService<IDiskProvider>();
						if (DataProvider == null)
							return new ResponseMqData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

						// json을 객체로 변환
						var Request = JsonConvert.DeserializeObject<RequestDiskUsage>(json);

						// 디스크 크기 수정
						var Response = await DataProvider.UpdateUsage(Request);

						Result.CopyValueFrom(Response);
						Result.IsProcessed = true;
					}
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