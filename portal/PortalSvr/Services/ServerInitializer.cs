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
using System.IO;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.CommonData;
using MTLib.Core;
using PortalData.Enums;
using PortalData.Requests.Servers;
using PortalData.Requests.Services;
using PortalProviderInterface;
using IServiceProvider = PortalProviderInterface.IServiceProvider;

namespace PortalSvr.Services
{
	/// <summary> 서버 초기화 인터페이스</summary>
	public interface IServerInitializer
	{
		/// <summary> 서버가 등록되지 않았을 경우 등록한다.</summary>
		Task Initialize();
	}

	/// <summary> 서버 초기화 클래스</summary>
	public class ServerInitializer : IServerInitializer
	{
		/// <summary>설정 정보</summary>
		protected readonly IConfiguration m_configuration;

		/// <summary> 서버 프로바이더</summary>
		protected readonly IServerProvider m_serverProvider;

		/// <summary> 서비스 프로바이더</summary>
		protected readonly IServiceProvider m_serviceProvider;

		/// <summary> Api Key 프로바이더</summary>
		protected readonly IApiKeyProvider m_apiKeyProvider;

		/// <summary>로거</summary>
		private readonly ILogger m_logger;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="serverProvider">서버에 대한 프로바이더 객체</param>
		/// <param name="serviceProvider">서비스에 대한 프로바이더 객체</param>
		/// <param name="apiKeyProvider">API Key에 대한 프로바이더 객체</param>
		/// <param name="logger">로거</param>
		public ServerInitializer(IConfiguration configuration
			, IServerProvider serverProvider
			, IApiKeyProvider apiKeyProvider
			, IServiceProvider serviceProvider
			, ILogger<ServerInitializer> logger)
		{
			m_configuration = configuration;
			m_serverProvider = serverProvider;
			m_serviceProvider = serviceProvider;
			m_apiKeyProvider = apiKeyProvider;
			m_logger = logger;
		}

		/// <summary> 서버 / 서비스가 등록되지 않았을 경우 등록한다.</summary>
		public async Task Initialize()
		{
			try
			{
				//호스트 명을 가져온다
				var HostName = m_configuration["AppSettings:Host"];

				// 내부 서비스용 API 키를 가져온다.
				var ApiKey = await m_apiKeyProvider.GetMainApiKey();

				if (ApiKey == null)
					throw new Exception("Internal Service ApiKey is null");

				if (!Guid.TryParse(ApiKey.UserId, out Guid UserGuid))
					throw new Exception("Internal Service ApiKey UserGuid is Empty");

				// 서버가 존재하지 않을 경우
				var ServerName = HostName.IsEmpty() ? "ksan_mgmt" : HostName;
				var Server = await m_serverProvider.Get(ServerName);
				if (Server == null || Server.Result == EnumResponseResult.Error)
				{

					// 서버를 등록한다
					var Request = new RequestServer()
					{
						Name = ServerName,
						Description = "",
						CpuModel = "",
						Clock = 0,
						State = EnumServerState.Online,
					};

					var Response = await m_serverProvider.Add(Request, UserGuid, ApiKey.UserName);

					if (Response.Result == EnumResponseResult.Success)
					{
						Server = Response;
						m_logger.LogInformation($"{ServerName} Add Success");

						// 서버등록에 성공할 경우 ksanMonitor.conf파일을 생성한다.
						var Datas = new List<string>()
							{
								"[mgs]",
								$"PortalIp = {m_configuration["AppSettings:RabbitMq:Host"]}",
								$"PortalPort = 5443",
								$"MqPort = {m_configuration["AppSettings:RabbitMq:Port"]}",
								$"MqUser = {m_configuration["AppSettings:RabbitMq:User"]}",
								$"MqPassword = {m_configuration["AppSettings:RabbitMq:Password"]}",
								$"PortalApiKey = {ApiKey.KeyValue}",
								$"ServerId = {Response.Data.Id}",
								"ManagementNetDev = ",
								"DefaultNetworkId = ",
								"[monitor]",
								"ServerMonitorInterval = 5",
								"NetworkMonitorInterval = 5",
								"DiskMonitorInterval = 5",
								"ServiceMonitorInterval = 5",
							};

						await File.WriteAllLinesAsync("/usr/local/ksan/etc/ksanMonitor.conf", Datas);
						m_logger.LogInformation("Save ksanMonitor.conf");
					}
					else
						throw new Exception($"{ServerName} Add Failure");

				}
				var ServerId = Server.Data.Id;

				// 자기자신의 서비스가 등록되어있는지 확인한다.
				var ServiceName = "ksanapiportal";
				var Service = await m_serviceProvider.Get(ServiceName);

				//서비스가 등록되지 않았을 경우
				if (Service == null || Service.Result == EnumResponseResult.Error)
				{
					var Request = new RequestService()
					{
						Name = ServiceName,
						ServerId = ServerId,
						Description = "",
						ServiceType = EnumServiceType.ksanApiPortal,
						State = EnumServiceState.Online,
					};

					// 서비스 등록
					var Response = await m_serviceProvider.Add(Request, UserGuid, ApiKey.UserName);

					if (Response.Result == EnumResponseResult.Success)
						m_logger.LogInformation($"{ServiceName} Add Success");
					else
						m_logger.LogError($"{ServiceName} Add Failed");
				}


			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
	}

}