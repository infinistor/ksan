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
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.CommonData;
using MTLib.Core;
using PortalData.Enums;
using PortalData.Requests.Servers;
using PortalData.Requests.Services;
using PortalProviderInterface;
using IServiceProvider = PortalProviderInterface.IServiceProvider;
using PortalResources;
using PortalData.Requests.Disks;
using Microsoft.Extensions.Hosting;
using System.Threading;

namespace PortalSvr.Services
{
	/// <summary> 서버 초기화 인터페이스</summary>
	public interface IServerInitializer : IHostedService, IDisposable
	{
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

		/// <summary> 디스크풀 프로바이더</summary>
		protected readonly IDiskPoolProvider m_diskPoolProvider;

		/// <summary> 디스크 프로바이더</summary>
		protected readonly IDiskProvider m_diskProvider;

		/// <summary> 유저 프로바이더</summary>
		protected readonly IKsanUserProvider m_userProvider;

		/// <summary> 네트워크 프로바이더</summary>
		protected readonly INetworkInterfaceProvider m_networkInterfaceProvider;

		/// <summary>로거</summary>
		private readonly ILogger m_logger;

		/// <summary> 타이머</summary>
		private Timer m_timer = null;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="serverProvider">서버에 대한 프로바이더 객체</param>
		/// <param name="serviceProvider">서비스에 대한 프로바이더 객체</param>
		/// <param name="apiKeyProvider">API Key에 대한 프로바이더 객체</param>
		/// <param name="diskPoolProvider">Disk Pool에 대한 프로바이더 객체</param>
		/// <param name="diskProvider">Disk에 대한 프로바이더 객체</param>
		/// <param name="ksanUserProvider">User에 대한 프로바이더 객체</param>
		/// <param name="networkInterfaceProvider">네트워크에 대한 프로바이더 객체</param>
		/// <param name="logger">로거</param>
		public ServerInitializer(IConfiguration configuration
			, IServerProvider serverProvider
			, IApiKeyProvider apiKeyProvider
			, IServiceProvider serviceProvider
			, IDiskPoolProvider diskPoolProvider
			, IDiskProvider diskProvider
			, IKsanUserProvider ksanUserProvider
			, INetworkInterfaceProvider networkInterfaceProvider
			, ILogger<ServerInitializer> logger)
		{
			m_configuration = configuration;
			m_serverProvider = serverProvider;
			m_serviceProvider = serviceProvider;
			m_apiKeyProvider = apiKeyProvider;
			m_diskPoolProvider = diskPoolProvider;
			m_diskProvider = diskProvider;
			m_userProvider = ksanUserProvider;
			m_networkInterfaceProvider = networkInterfaceProvider;
			m_logger = logger;
		}

		/// <summary> 서버 / 서비스가 등록되지 않았을 경우 등록한다.</summary>
		public Task StartAsync(CancellationToken cancellationToken)
		{
			m_logger.LogInformation("Server Initialize Timer Create");
			m_timer = new Timer(DoWork, null, TimeSpan.FromSeconds(5), TimeSpan.FromSeconds(999));
			return Task.CompletedTask;
		}

		/// <summary> 정지 </summary>
		public Task StopAsync(CancellationToken cancellationToken)
		{
			m_logger.LogInformation("Server Initialize Timer Stop");
			return Task.CompletedTask;
		}

		/// <summary> 서버 감시 해제 </summary>
		public void Dispose()
		{
			if (m_timer != null) m_timer.Dispose();
		}

		async void DoWork(object state)
		{
			m_timer.Change(Timeout.Infinite, 0);

			try
			{
				if (!EnvironmentInitializer.GetEnvValue(Resource.ENV_INIT_TYPE, out string InitType)) return;
				m_logger.LogInformation("Server Initialize Start");

				if (!InitType.Equals(Resource.ENV_INIT_TYPE_ALL_IN_ONE, StringComparison.OrdinalIgnoreCase)) return;

				// 내부 서비스용 API 키를 가져온다.
				var ApiKey = await m_apiKeyProvider.GetMainApiKey();

				if (ApiKey == null)
					throw new Exception("Internal Service ApiKey is null");

				if (!Guid.TryParse(ApiKey.UserId, out Guid UserGuid))
					throw new Exception("Internal Service ApiKey UserGuid is Empty");

				// 서버가 존재하지 않을 경우
				var Servers = await m_serverProvider.GetList();
				if (Servers == null || Servers.Result != EnumResponseResult.Success || Servers.Data.Items.Count < 1)
				{
					// 서버를 등록한다
					var Response = await m_serverProvider.Initialize(new RequestServerInitialize() { ServerIp = m_configuration["AppSettings:Host"] });

					// 서버 등록을 실패할 경우
					if (Response == null || Response.Result != EnumResponseResult.Success) throw new Exception($"Server Add Failure. {Response.Message}");

					// 서버 조회를 실패할 경우
					Servers = await m_serverProvider.GetList();
					if (Servers == null || Servers.Result != EnumResponseResult.Success || Servers.Data.Items.Count < 1)
						throw new Exception($"Server Add Failure. {Response.Message}");

					m_logger.LogInformation($"Server Add Success");
				}

				//서버 정보를 가져온다.
				var Server = Servers.Data.Items[0];
				var ServerName = Server.Name;
				var ServerId = Server.Id;

				// KsanAgent가 동작할때까지 대기
				while (true)
				{
					var Networks = await m_networkInterfaceProvider.GetList(ServerId);
					if (Networks != null && Networks.Data.Items.Count > 0) break;
					else Thread.Sleep(1000);
				}

				//디스크풀이 존재하는지 확인한다.
				var DiskPoolName = "diskpool1";
				var DiskPool = await m_diskPoolProvider.Get(DiskPoolName);
				if (DiskPool == null || DiskPool.Result == EnumResponseResult.Error)
				{
					// 디스크풀이 존재하지 않을 경우 생성한다.
					var Request = new RequestDiskPool() { Name = DiskPoolName, ReplicationType = EnumDiskPoolReplicaType.OnePlusZero };
					var Response = await m_diskPoolProvider.Add(Request);

					// 디스크풀 생성에 실패할 경우
					if (Response == null || Response.Result != EnumResponseResult.Success) throw new Exception($"{DiskPoolName} Add Failure");
					else m_logger.LogInformation($"{DiskPoolName} Add Success");
					DiskPool = Response;
				}
				//disk가 존재하는지 확인한다.
				if (DiskPool.Data.Disks.Count < 1)
				{
					//환경변수에서 디스크 목록을 가져온다.
					if (!EnvironmentInitializer.GetEnvValue(Resource.ENV_OSDDISK_PATHS, out string StrDisks) || StrDisks.IsEmpty())
						throw new Exception("Disk Path is Empty!");

					var DiskPaths = StrDisks.Trim().Split(',');

					//디스크가 존재하지 않을 경우 생성한다.
					var DiskCount = 1;
					foreach (var DiskPath in DiskPaths)
					{
						if (DiskPath.IsEmpty()) continue;
						var Request = new RequestDisk()
						{
							Name = $"{ServerName}_disk{DiskCount++}",
							DiskPoolId = DiskPoolName,
							Path = DiskPath,
							State = EnumDiskState.Good
						};

						// 디스크를 추가한다.
						var Response = await m_diskProvider.Add(ServerName, Request);

						// 디스크 등록에 실패할 경우
						if (Response == null || Response.Result != EnumResponseResult.Success) throw new Exception($"{Request.Name} Add Failure. {Response.Message}");
						else m_logger.LogInformation($"{Request.Name} Add Success");
					}
				}

				//유저가 존재하지 않을 경우 생성한다.
				var UserName = "ksanuser";
				var User = await m_userProvider.GetUser(UserName);
				if (User == null || User.Result != EnumResponseResult.Success)
				{
					EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_USER_ACCESSKEY, out string AccessKey);
					EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_USER_SECRETKEY, out string SecretKey);

					// 액세스키, 시크릿키를 설정하지 않았을 경우 기본유저 생성 하지 않음
					if (AccessKey == null || SecretKey == null || AccessKey.IsEmpty() || SecretKey.IsEmpty())
						m_logger.LogInformation("User Create Skip");
					else
					{
						var Response = await m_userProvider.Add(UserName, AccessKey, SecretKey);

						// 유저 생성에 실패할 경우
						if (Response == null || Response.Result != EnumResponseResult.Success) throw new Exception($"{UserName} Add Failure. {Response.Message}");
						else m_logger.LogInformation($"{UserName} Add Success");
					}
				}

				//gw 서비스가 등록되지 않은 경우 등록한다.
				var GWName = "ksanGW1";
				var GW = await m_serviceProvider.Get(GWName);
				if (GW == null || GW.Result != EnumResponseResult.Success)
				{
					var Request = new RequestService()
					{
						Name = GWName,
						ServerId = ServerName,
						ServiceType = EnumServiceType.ksanGW,
					};

					var Response = await m_serviceProvider.Add(Request, UserGuid, ApiKey.UserName);

					// 서비스 등록에 실패할 경우
					if (Response == null || Response.Result != EnumResponseResult.Success) throw new Exception($"{Request.Name} Add Failure. {Response.Message}");
					else m_logger.LogInformation($"{Request.Name} Add Success");
				}

				//osd 서비스가 등록되지 않은 경우 등록한다.
				var OSDName = "ksanOSD1";
				var OSD = await m_serviceProvider.Get(OSDName);
				if (OSD == null || OSD.Result != EnumResponseResult.Success)
				{
					var Request = new RequestService()
					{
						Name = OSDName,
						ServerId = ServerName,
						ServiceType = EnumServiceType.ksanOSD,
					};

					var Response = await m_serviceProvider.Add(Request, UserGuid, ApiKey.UserName);

					// 서비스 등록에 실패할 경우
					if (Response == null || Response.Result != EnumResponseResult.Success) throw new Exception($"{Request.Name} Add Failure {Response.Message}");
					else m_logger.LogInformation($"{Request.Name} Add Success");
				}

			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
	}
}