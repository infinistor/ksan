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
using System.Collections.Generic;
using PortalData.Requests.Ksan;
using Newtonsoft.Json;

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
			m_timer = new Timer(DoWork, null, TimeSpan.FromSeconds(10), TimeSpan.FromSeconds(60));
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
			Dispose(true);
			GC.SuppressFinalize(this);
		}

		/// <summary> 서버 감시 해제 </summary>
		/// <param name="disposing">해제 여부</param>
		protected virtual void Dispose(bool disposing)
		{
			if (disposing)
			{
				m_timer?.Dispose();
			}
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
					{
						m_logger.LogInformation("Internal Service ApiKey is null");
						return;
					}

					// 서버정보를 가져온다.
					var m_serverProvider = ServiceScope.ServiceProvider.GetService<IServerProvider>();
					var Servers = await m_serverProvider.GetList();
					// 서버가 존재하지 않을 경우
					if (Servers == null || Servers.Result != EnumResponseResult.Success || Servers.Data.Items.Count < 1)
					{
						// 서버를 등록한다
						var Request = new RequestServerInitialize() { Ip = m_configuration["AppSettings:Host"] };
						if (EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_SERVER_ID, out string TempServerId)) Request.Id = TempServerId;
						if (EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_SERVER_NAME, out string TempServerName)) Request.Name = TempServerName;
						if (EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_SERVICE_AGENT_ID, out string TempAgentId)) Request.AgentServiceId = TempAgentId;

						m_logger.LogInformation($"Server Add Start. {JsonConvert.SerializeObject(Request)}");

						var Response = await m_serverProvider.Initialize(Request);

						// 서버 등록을 실패할 경우
						if (Response == null)
						{
							m_logger.LogError($"Server Add Failure. Response is null");
							return;
						}
						else if (Response.Result != EnumResponseResult.Success)
						{
							m_logger.LogInformation($"Server Add Failure. {Response.Message}");
							return;
						}
						else m_logger.LogInformation($"Server Add Success");

						// 서버 조회를 실패할 경우
						Servers = await m_serverProvider.GetList();
						if (Servers == null)
						{
							m_logger.LogError($"Server Add Failure. Server is null");
							return;
						}
						else if (Servers.Result != EnumResponseResult.Success || Servers.Data.Items.Count < 1)
						{
							m_logger.LogError($"Server Add Failure. {Servers.Message}");

							return;
						}
					}

					m_logger.LogInformation($"Get Server Info");

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
					m_logger.LogInformation($"Ksan Agent is Running");


					//디스크풀이 존재하는지 확인한다.
					var m_diskPoolProvider = ServiceScope.ServiceProvider.GetService<IDiskPoolProvider>();
					var DiskPools = await m_diskPoolProvider.GetList();
					if (DiskPools.Data.Items.Count == 0)
					{
						var DiskPoolName = "diskpool1";
						var Request = new RequestDiskPool() { Name = DiskPoolName, ReplicationType = EnumDiskPoolReplicaType.OnePlusZero };

						// 환경변수에 기본 디스크풀 Id가 존재할 경우 사용한다.
						if (EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_DISK_POOL_ID, out string DefaultDiskPoolId)) Request.Id = DefaultDiskPoolId;

						m_logger.LogInformation($"DiskPool Add Start. {JsonConvert.SerializeObject(Request)}");

						// 디스크풀이 존재하지 않을 경우 생성한다.
						var Response = await m_diskPoolProvider.Add(Request);

						// 디스크풀 생성에 실패할 경우
						if (Response == null)
						{
							m_logger.LogError($"{DiskPoolName} Add Failure. Response is null");
							return;
						}
						else if (Response.Result != EnumResponseResult.Success)
						{
							m_logger.LogError($"{DiskPoolName} Add Failure. {Response.Message}");
							return;
						}
						m_logger.LogInformation($"{DiskPoolName} Add Success");
						var DiskPool = Response;

						//disk가 존재하는지 확인한다.
						if (DiskPool.Data.Disks.Count == 0)
						{
							//환경변수에서 디스크 목록을 가져온다.
							if (!EnvironmentInitializer.GetEnvValue(Resource.ENV_DISK_PATHS, out string StrDisks) || StrDisks.IsEmpty()) m_logger.LogInformation("Disk Path is Empty!");
							else
							{
								var DiskPaths = StrDisks.Trim().Split(',');

								//디스크가 존재하지 않을 경우 생성한다.
								var m_diskProvider = ServiceScope.ServiceProvider.GetService<IDiskProvider>();
								var DiskCount = 1;
								foreach (var DiskPath in DiskPaths)
								{
									if (DiskPath.IsEmpty()) continue;
									var DiskRequest = new RequestDisk()
									{
										Name = $"{ServerName}_disk{DiskCount++}",
										DiskPoolId = DiskPoolName,
										Path = DiskPath,
										State = EnumDiskState.Good
									};

									// 디스크를 추가한다.
									var DiskResponse = await m_diskProvider.Add(ServerName, DiskRequest);

									// 디스크 등록에 실패할 경우
									if (DiskResponse == null)
									{
										m_logger.LogError($"{DiskRequest.Name} Add Failure. Response is null");
										return;
									}
									else if (DiskResponse.Result != EnumResponseResult.Success)
									{
										m_logger.LogError($"{DiskRequest.Name} Add Failure. {DiskResponse.Message}");
										return;
									}
									else m_logger.LogInformation($"{DiskRequest.Name} Add Success");
								}
							}
						}
					}

					//유저가 존재하는지 확인한다.
					var m_userProvider = ServiceScope.ServiceProvider.GetService<IKsanUserProvider>();
					var UserName = "ksanUser";
					var User = await m_userProvider.GetUser(UserName);

					// 유저가 존재하지 않을 경우 생성한다.
					if (User == null || User.Result != EnumResponseResult.Success)
					{

						EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_USER_ID, out string UserId);
						EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_USER_ACCESS_KEY, out string AccessKey);
						EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_USER_SECRET_KEY, out string SecretKey);

						// 액세스키, 시크릿키를 설정하지 않았을 경우 기본유저 생성 하지 않음
						if (AccessKey == null || SecretKey == null || AccessKey.IsEmpty() || SecretKey.IsEmpty())
							m_logger.LogInformation("User Create Skip");
						else
						{
							// 유저 생성
							var Request = new RequestKsanUser()
							{
								Name = UserName,
								Id = UserId,
								AccessKey = AccessKey,
								SecretKey = SecretKey
							};
							m_logger.LogInformation($"User Create Start.{JsonConvert.SerializeObject(Request)}");

							var Response = await m_userProvider.Add(Request);

							// 유저 생성에 실패할 경우
							if (Response == null)
							{
								m_logger.LogError($"{UserName} Add Failure. Response is null");
								return;
							}
							else if (Response.Result != EnumResponseResult.Success)
							{
								m_logger.LogError($"{UserName} Add Failure. {Response.Message}");
								return;
							}
							else m_logger.LogInformation($"{UserName} Add Success");
						}
					}

					// 리전이 존재하는지 확인한다.
					var m_regionProvider = ServiceScope.ServiceProvider.GetService<IRegionProvider>();
					var RegionName = m_configuration["AppSettings:RegionName"];
					var Region = await m_regionProvider.Get(RegionName);

					//리전이 존재하지 않을 경우 생성한다.
					if (Region == null || Region.Result != EnumResponseResult.Success)
					{
						var Request = new RequestRegion()
						{
							Name = RegionName,
							Address = m_configuration["AppSettings:Host"],
							Port = 7070,
							SSLPort = 7443
						};
						
						m_logger.LogInformation($"Region Add Start.{JsonConvert.SerializeObject(Request)}");

						var Response = await m_regionProvider.Add(Request);

						// 리전 생성에 실패할 경우
						if (Response == null)
						{
							m_logger.LogError($"{RegionName} Add Failure. Response is null");
							return;
						}
						else if (Response.Result != EnumResponseResult.Success)
						{
							m_logger.LogError($"{RegionName} Add Failure. {Response.Message}");
							return;
						}
						else m_logger.LogInformation($"{RegionName} Add Success");
					}

					// All in one 일 경우
					if (!InitType.Equals(Resource.ENV_INIT_TYPE_ALL_IN_ONE, StringComparison.OrdinalIgnoreCase)) return;

					//제외할 서비스 목록
					if (!EnvironmentInitializer.GetEnvValue(Resource.ENV_EXCLUDE_SERVICES, out string StrExcludeServices)) StrExcludeServices = string.Empty;

					List<string> ExcludeServices;

					//제외할 서비스 목록이 존재할 경우
					if (!StrExcludeServices.IsEmpty()) ExcludeServices = new List<string>(StrExcludeServices.Trim().Split(',', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries));
					else ExcludeServices = new List<string>();

					// 모두 제외할 경우
					if (ExcludeServices.Exists(x => x.Equals(Resource.ENV_EXCLUDE_SERVICES_ALL, StringComparison.OrdinalIgnoreCase))) return;

					var m_serviceProvider = ServiceScope.ServiceProvider.GetService<IServiceProvider>();
					
					// 제외 서비스 목록에 GW가 있는지 확인한다.
					if (!ExcludeServices.Exists(x => x.Equals(Resource.ENV_EXCLUDE_SERVICES_KSAN_GW, StringComparison.OrdinalIgnoreCase)))
					{
						//gw 서비스가 등록되지 않은 경우 등록한다.
						var GWName = "GW1";
						var GW = await m_serviceProvider.Get(GWName);
						if (GW == null || GW.Result != EnumResponseResult.Success)
						{
							var Request = new RequestService()
							{
								Name = GWName,
								ServerId = ServerName,
								ServiceType = EnumServiceType.ksanGW,
							};

							// 환경변수에 기본 GW Id가 존재할 경우 사용한다.
							if (EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_SERVICE_GW_ID, out string DefaultGWId)) Request.Id = DefaultGWId;

							m_logger.LogInformation($"GW Add Start. {JsonConvert.SerializeObject(Request)}");

							var Response = await m_serviceProvider.Add(Request, ApiKey.UserId, ApiKey.UserName);

							// 서비스 등록에 실패할 경우
							if (Response == null)
							{
								m_logger.LogError($"{Request.Name} Add Failure. Response is null");
								return;
							}
							else if (Response.Result != EnumResponseResult.Success)
							{
								m_logger.LogError($"{Request.Name} Add Failure. {Response.Message}");
								return;
							}
							else m_logger.LogInformation($"{Request.Name} Add Success");
						}
					}

					// 제외 서비스 목록에 OSD가 있는지 확인한다.
					if (!ExcludeServices.Exists(x => x.Equals(Resource.ENV_EXCLUDE_SERVICES_KSAN_OSD, StringComparison.OrdinalIgnoreCase)))
					{
						//osd 서비스가 등록되지 않은 경우 등록한다.
						var OSDName = "OSD1";
						var OSD = await m_serviceProvider.Get(OSDName);
						if (OSD == null || OSD.Result != EnumResponseResult.Success)
						{
							var Request = new RequestService()
							{
								Name = OSDName,
								ServerId = ServerName,
								ServiceType = EnumServiceType.ksanOSD,
							};

							// 환경변수에 기본 OSD Id가 존재할 경우 사용한다.
							if (EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_SERVICE_OSD_ID, out string DefaultOSDId)) Request.Id = DefaultOSDId;

							m_logger.LogInformation($"OSD Add Start. {JsonConvert.SerializeObject(Request)}");

							var Response = await m_serviceProvider.Add(Request, ApiKey.UserId, ApiKey.UserName);

							// 서비스 등록에 실패할 경우
							if (Response == null)
							{
								m_logger.LogError($"{Request.Name} Add Failure. Response is null");
								return;
							}
							else if (Response.Result != EnumResponseResult.Success)
							{
								m_logger.LogError($"{Request.Name} Add Failure. {Response.Message}");
								return;
							}
							else m_logger.LogInformation($"{Request.Name} Add Success");
						}
					}

					// 제외 서비스 목록에 Lifecycle이 있는지 확인한다.
					if (!ExcludeServices.Exists(x => x.Equals(Resource.ENV_EXCLUDE_SERVICES_KSAN_LIFECYCLE_MANAGER, StringComparison.OrdinalIgnoreCase)))
					{
						//Lifecycle 서비스가 등록되지 않은 경우 등록한다.
						var LifecycleName = "LifecycleManager1";
						var Lifecycle = await m_serviceProvider.Get(LifecycleName);
						if (Lifecycle == null || Lifecycle.Result != EnumResponseResult.Success)
						{
							var Request = new RequestService()
							{
								Name = LifecycleName,
								ServerId = ServerName,
								ServiceType = EnumServiceType.ksanLifecycleManager,
							};

							// 환경변수에 기본 Lifecycle Id가 존재할 경우 사용한다.
							if (EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_SERVICE_LIFECYCLE_MANAGER_ID, out string DefaultLifecycleId)) Request.Id = DefaultLifecycleId;

							m_logger.LogInformation($"Lifecycle Add Start. {JsonConvert.SerializeObject(Request)}");

							var Response = await m_serviceProvider.Add(Request, ApiKey.UserId, ApiKey.UserName);

							// 서비스 등록에 실패할 경우
							if (Response == null)
							{
								m_logger.LogError($"{Request.Name} Add Failure. Response is null");
								return;
							}
							else if (Response.Result != EnumResponseResult.Success)
							{
								m_logger.LogError($"{Request.Name} Add Failure. {Response.Message}");
								return;
							}
							else m_logger.LogInformation($"{Request.Name} Add Success");
						}
					}

					// 제외 서비스 목록에 LogManager가 있는지 확인한다.
					if (!ExcludeServices.Exists(x => x.Equals(Resource.ENV_EXCLUDE_SERVICES_KSAN_LOG_MANAGER, StringComparison.OrdinalIgnoreCase)))
					{
						//LogManager 서비스가 등록되지 않은 경우 등록한다.
						var LogManagerName = "LogManager1";
						var LogManager = await m_serviceProvider.Get(LogManagerName);
						if (LogManager == null || LogManager.Result != EnumResponseResult.Success)
						{
							var Request = new RequestService()
							{
								Name = LogManagerName,
								ServerId = ServerName,
								ServiceType = EnumServiceType.ksanLogManager,
							};

							// 환경변수에 기본 LogManager Id가 존재할 경우 사용한다.
							if (EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_SERVICE_LOG_MANAGER_ID, out string DefaultLogManagerId)) Request.Id = DefaultLogManagerId;

							m_logger.LogInformation($"LogManager Add Start. {JsonConvert.SerializeObject(Request)}");

							var Response = await m_serviceProvider.Add(Request, ApiKey.UserId, ApiKey.UserName);

							// 서비스 등록에 실패할 경우
							if (Response == null)
							{
								m_logger.LogError($"{Request.Name} Add Failure. Response is null");
								return;
							}
							else if (Response.Result != EnumResponseResult.Success)
							{
								m_logger.LogError($"{Request.Name} Add Failure. {Response.Message}");
								return;
							}
							else m_logger.LogInformation($"{Request.Name} Add Success");
						}
					}

					// 제외 서비스 목록에 Replication이 있는지 확인한다.
					if (!ExcludeServices.Exists(x => x.Equals(Resource.ENV_EXCLUDE_SERVICES_KSAN_REPLICATION_MANAGER, StringComparison.OrdinalIgnoreCase)))
					{
						//Replication 서비스가 등록되지 않은 경우 등록한다.
						var ReplicationName = "ReplicationManager1";
						var Replication = await m_serviceProvider.Get(ReplicationName);
						if (Replication == null || Replication.Result != EnumResponseResult.Success)
						{
							var Request = new RequestService()
							{
								Name = ReplicationName,
								ServerId = ServerName,
								ServiceType = EnumServiceType.ksanReplicationManager,
							};

							// 환경변수에 기본 Replication Id가 존재할 경우 사용한다.
							if (EnvironmentInitializer.GetEnvValue(Resource.ENV_DEFAULT_SERVICE_REPLICATION_MANAGER_ID, out string DefaultReplicationId)) Request.Id = DefaultReplicationId;

							m_logger.LogInformation($"Replication Add Start. {JsonConvert.SerializeObject(Request)}");

							var Response = await m_serviceProvider.Add(Request, ApiKey.UserId, ApiKey.UserName);

							// 서비스 등록에 실패할 경우
							if (Response == null)
							{
								m_logger.LogError($"{Request.Name} Add Failure. Response is null");
								return;
							}
							else if (Response.Result != EnumResponseResult.Success)
							{
								m_logger.LogError($"{Request.Name} Add Failure. {Response.Message}");
								return;
							}
							else m_logger.LogInformation($"{Request.Name} Add Success");
						}
					}
				}
				m_timer.Change(Timeout.Infinite, 0);
				m_timer?.Dispose();
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
	}
}