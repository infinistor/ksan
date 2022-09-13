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
using PortalData.Requests.Region;
using Microsoft.Extensions.DependencyInjection;

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
		/// <summary>서비스 팩토리</summary>
		protected readonly IServiceScopeFactory m_serviceScopeFactory;

		/// <summary>로거</summary>
		private readonly ILogger m_logger;

		/// <summary> 타이머</summary>
		private Timer m_timer = null;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public ServerInitializer(IConfiguration configuration
			, IServiceScopeFactory serviceScopeFactory
			, ILogger<ServerInitializer> logger)
		{
			m_configuration = configuration;
			m_serviceScopeFactory = serviceScopeFactory;
			m_logger = logger;
		}

		/// <summary> 서버 / 서비스가 등록되지 않았을 경우 등록한다.</summary>
		public Task StartAsync(CancellationToken cancellationToken)
		{
			m_logger.LogInformation("Server Initialize Timer Create");
			m_timer = new Timer(DoWork, null, TimeSpan.FromSeconds(10), TimeSpan.FromSeconds(5));
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
			try
			{
				using (var ServiceScope = m_serviceScopeFactory.CreateScope())
				{

					if (!EnvironmentInitializer.GetEnvValue(Resource.ENV_INIT_TYPE, out string InitType)) return;
					m_logger.LogInformation("Server Initialize Start");

					// 내부 서비스용 API 키를 가져온다.
					var ApiKey = await ServiceScope.ServiceProvider.GetService<IApiKeyProvider>().GetMainApiKey();

					if (ApiKey == null)
						throw new Exception("Internal Service ApiKey is null");

					if (!Guid.TryParse(ApiKey.UserId, out Guid UserGuid))
						throw new Exception("Internal Service ApiKey UserGuid is Empty");

					// 서버가 존재하지 않을 경우
					var m_serverProvider = ServiceScope.ServiceProvider.GetService<IServerProvider>();
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
					var m_networkInterfaceProvider = ServiceScope.ServiceProvider.GetService<INetworkInterfaceProvider>();
					while (true)
					{
						var Networks = await m_networkInterfaceProvider.GetList(ServerId);
						if (Networks != null && Networks.Data.Items.Count > 0) break;
						else Thread.Sleep(1000);
					}

					//디스크풀이 존재하는지 확인한다.
					var m_diskPoolProvider = ServiceScope.ServiceProvider.GetService<IDiskPoolProvider>();
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
						var m_diskProvider = ServiceScope.ServiceProvider.GetService<IDiskProvider>();
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

					// All in one 일 경우
					if (!InitType.Equals(Resource.ENV_INIT_TYPE_ALL_IN_ONE, StringComparison.OrdinalIgnoreCase)) return;

					//유저가 존재하지 않을 경우 생성한다.
					var m_userProvider = ServiceScope.ServiceProvider.GetService<IKsanUserProvider>();
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

					//리전이 존재하지 않을 경우 생성한다.
					var m_regionProvider = ServiceScope.ServiceProvider.GetService<IRegionProvider>();
					var RegionName = m_configuration["AppSettings:RegionName"];
					var Region = await m_regionProvider.Get(RegionName);
					m_logger.LogInformation($"{RegionName}, {Region.Data.AccessKey}, {Region.Data.SecretKey}");
					if (Region == null || Region.Result != EnumResponseResult.Success)
					{
						var Request = new RequestRegion()
						{
							Name = RegionName,
							Address = m_configuration["AppSettings:Host"],
							Port = 8080,
							SSLPort = 8443
						};
						var Response = await m_regionProvider.Add(Request);

						// 리전 생성에 실패할 경우
						if (Response == null || Response.Result != EnumResponseResult.Success) throw new Exception($"{RegionName} Add Failure. {Response.Message}");
						else m_logger.LogInformation($"{RegionName} Add Success");
					}

					//gw 서비스가 등록되지 않은 경우 등록한다.
					var m_serviceProvider = ServiceScope.ServiceProvider.GetService<IServiceProvider>();
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

					//Lifecycle 서비스가 등록되지 않은 경우 등록한다.
					var LifecycleName = "ksanLifecycle1";
					var Lifecycle = await m_serviceProvider.Get(LifecycleName);
					if (Lifecycle == null || Lifecycle.Result != EnumResponseResult.Success)
					{
						var Request = new RequestService()
						{
							Name = LifecycleName,
							ServerId = ServerName,
							ServiceType = EnumServiceType.ksanLifecycle,
						};

						var Response = await m_serviceProvider.Add(Request, UserGuid, ApiKey.UserName);

						// 서비스 등록에 실패할 경우
						if (Response == null || Response.Result != EnumResponseResult.Success) throw new Exception($"{Request.Name} Add Failure {Response.Message}");
						else m_logger.LogInformation($"{Request.Name} Add Success");
					}
				}
				m_timer.Change(Timeout.Infinite, 0);

			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				m_logger.LogError(ex.Message);
			}
		}
	}
}