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
using PortalData;
using PortalData.Enums;
using PortalData.Requests.Servers;
using PortalData.Responses.Servers;
using PortalModels;
using PortalProviderInterface;
using PortalResources;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.EntityFramework;
using MTLib.HttpClient;
using PortalData.Requests.Agent;
using PortalProvider.Providers.RabbitMQ;
using MTLib.Reflection;

namespace PortalProvider.Providers.Servers
{
	/// <summary>서버 데이터 프로바이더 클래스</summary>
	public class ServerProvider : BaseProvider<PortalModel>, IServerProvider
	{

		/// <summary>API KEY 데이터 프로바이더 객체</summary>
		readonly IApiKeyProvider m_apiKeyProvider;

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public ServerProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<ServerProvider> logger,
			IApiKeyProvider apiKeyProvider
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
			m_apiKeyProvider = apiKeyProvider;
		}

		/// <summary>서버 등록</summary>
		/// <param name="Request">서버 등록 요청 객체</param>
		/// <returns>서버 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseServerDetail>> Add(RequestServer Request, Guid? ModId = null, string ModName = null)
		{
			var Result = new ResponseData<ResponseServerDetail>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseServerDetail>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재하는 경우
				if (await this.IsNameExist(Request.Name))
					return new ResponseData<ResponseServerDetail>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVERS_DUPLICATED_NAME);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						var NewData = new Server()
						{
							Id = Guid.NewGuid(),
							Name = Request.Name,
							Description = Request.Description,
							CpuModel = Request.CpuModel,
							Clock = Request.Clock,
							State = (EnumDbServerState)Request.State,
							Rack = Request.Rack,
							MemoryTotal = Request.MemoryTotal,
							ModId = ModId != null ? ModId : LoginUserId,
							ModName = ModName != null ? ModName : LoginUserName,
							ModDate = DateTime.Now
						};

						await m_dbContext.Servers.AddAsync(NewData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Data = (await this.Get(NewData.Id.ToString())).Data;

						// 추가된 서버 정보 전송
						SendMq("*.servers.added", Result.Data);
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
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

		/// <summary>서버 초기화</summary>
		/// <param name="Request">서버 초기화 요청 객체</param>
		/// <returns>서버 초기화 결과 객체</returns>
		public async Task<ResponseData> Initialize(RequestServerInitialize Request)
		{
			var Result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 내부 서비스용 API 키를 가져온다.
				var InternalServiceApiKey = await m_apiKeyProvider.GetMainApiKey();

				if (InternalServiceApiKey == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__COMMUNICATION_ERROR_TO_API, Resource.EM_COMMON__COMMUNICATION_ERROR_TO_API);

				// 클라이언트 생성
				NNHttpClient client = new NNHttpClient(NNException.Logger, 100, true, true);

				// RabbitMq 정보를 가져온다.
				IConfigurationSection Section = m_configuration.GetSection("AppSettings:RabbitMQ");
				RabbitMQConfiguration RabbitMQ = Section.Get<RabbitMQConfiguration>();

				var SendData = new RequestAgentInitialize()
				{
					LocalIp = Request.ServerIp,
					PortalHost = m_configuration["AppSettings:Host"],
					PortalPort = 6443,
					MQHost = RabbitMQ.Host,
					MQPort = RabbitMQ.Port,
					MQUser = RabbitMQ.User,
					MQPassword = RabbitMQ.Password,
					PortalApiKey = InternalServiceApiKey.KeyValue
				};
				var Response = await client.Post<ResponseData>($"http://{Request.ServerIp}:6380/api/v1/Servers", SendData);
				Result.CopyValueFrom(Response.Data);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서버 수정</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <param name="Request">서버 수정 요청 객체</param>
		/// <returns>서버 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string Id, RequestServer Request)
		{
			var Result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);


				// 해당 정보를 가져온다.
				Server Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServerGuid))
					Exist = await m_dbContext.Servers
					.Where(i => i.Id == ServerGuid)
					.Include(i => i.Services)
					.Include(i => i.NetworkInterfaces)
					.ThenInclude(i => i.NetworkInterfaceVlans)
					.ThenInclude(i => i.ServiceNetworkInterfaceVlans)
					.FirstOrDefaultAsync();
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Servers
					.Where(i => i.Name == Id)
					.Include(i => i.Services)
					.Include(i => i.NetworkInterfaces)
					.ThenInclude(i => i.NetworkInterfaceVlans)
					.ThenInclude(i => i.ServiceNetworkInterfaceVlans)
					.FirstOrDefaultAsync();

				//서버가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 동일한 이름이 존재하는 경우
				if (await this.IsNameExist(Request.Name, Exist.Id))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVERS_DUPLICATED_NAME);


				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 전체 메모리 크기가 변경된 경우
						if (Exist.MemoryTotal != Request.MemoryTotal)
						{
							// 모든 네트워크 인터페이스에 대해서 처리
							foreach (var NetworkInterface in Exist.NetworkInterfaces)
							{
								// 모든 VLAN에 대해서 처리
								foreach (var NetworkInterfaceVlan in NetworkInterface.NetworkInterfaceVlans)
								{
									// 모든 서비스 연결 정보에 대해서 처리
									foreach (var ServiceNetworkInterfaceVlan in NetworkInterfaceVlan.ServiceNetworkInterfaceVlans)
									{
										// 오프라인 처리
										ServiceNetworkInterfaceVlan.Service.MemoryTotal = Request.MemoryTotal;
										if (m_dbContext.HasChanges())
											await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
									}
								}
							}
						}

						// 정보를 수정한다.
						Exist.Name = Request.Name;
						Exist.Description = Request.Description;
						Exist.CpuModel = Request.CpuModel;
						Exist.Clock = Request.Clock;
						Exist.State = (EnumDbServerState)Request.State;
						Exist.Rack = Request.Rack;
						Exist.MemoryTotal = Request.MemoryTotal;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							Exist.ModId = LoginUserId;
							Exist.ModName = LoginUserName;
							Exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}
						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 상세 정보를 가져온다.
						var Response = (await this.Get(Id)).Data;

						// 수정된 서버 정보 전송
						SendMq("*.servers.updated", Response);
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
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

		/// <summary>서버 상태 수정</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <param name="State">서버 상태</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(string Id, EnumServerState State, string ModId = "", string ModName = "")
		{
			var Result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Server Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServerGuid))
					Exist = await m_dbContext.Servers.FirstOrDefaultAsync(i => i.Id == ServerGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Servers.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 파라미터로 넘어온 수정자 아이디 파싱
				Guid guidModId = Guid.Empty;
				if (!ModId.IsEmpty())
					Guid.TryParse(ModId, out guidModId);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.State = (EnumDbServerState)State;
						if (LoginUserId != Guid.Empty || guidModId != Guid.Empty)
							Exist.ModId = LoginUserId != Guid.Empty ? LoginUserId : guidModId;
						if (!LoginUserName.IsEmpty() || !ModName.IsEmpty())
							Exist.ModName = !LoginUserName.IsEmpty() ? LoginUserName : ModName;
						Exist.ModDate = DateTime.Now;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
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


		/// <summary>서버 상태 수정</summary>
		/// <param name="Request">서버 상태 수정 요청 객체</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(RequestServerState Request, string ModId = "", string ModName = "")
		{
			var Result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 상태 변경
				Result = await this.UpdateState(Request.Id, Request.State, ModId, ModName);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서버 사용 정보 수정</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <param name="LoadAverage1M">1분 Load Average</param>
		/// <param name="LoadAverage5M">5분 Load Average</param>
		/// <param name="LoadAverage15M">15분 Load Average</param>
		/// <param name="MemoryUsed">메모리 사용량</param>
		/// <returns>서버 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(string Id, float LoadAverage1M, float LoadAverage5M, float LoadAverage15M, decimal MemoryUsed)
		{
			var Result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Server Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServerGuid))
					Exist = await m_dbContext.Servers.FirstOrDefaultAsync(i => i.Id == ServerGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Servers.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.LoadAverage1M = LoadAverage1M;
						Exist.LoadAverage5M = LoadAverage5M;
						Exist.LoadAverage15M = LoadAverage15M;
						Exist.MemoryUsed = MemoryUsed;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 사용 정보 추가
						m_dbContext.ServerUsages.Add(new ServerUsage()
						{
							Id = Exist.Id,
							RegDate = DateTime.Now,
							LoadAverage1M = LoadAverage1M,
							LoadAverage5M = LoadAverage5M,
							LoadAverage15M = LoadAverage15M,
							MemoryTotal = Exist.MemoryTotal,
							MemoryUsed = MemoryUsed
						});
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
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

		/// <summary>서버 사용 정보 수정</summary>
		/// <param name="Request">서버 사용 정보 수정 요청 객체</param>
		/// <returns>서버 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(RequestServerUsage Request)
		{
			var Result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 상태 변경
				Result = await this.UpdateUsage(Request.Id, Request.LoadAverage1M, Request.LoadAverage5M, Request.LoadAverage15M, Request.MemoryUsed);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서버 삭제</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <returns>서버 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Server Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServerId))
					Exist = await m_dbContext.Servers.AsNoTracking().FirstOrDefaultAsync(i => i.Id == ServerId);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Servers.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(Result.Result = EnumResponseResult.Success);

				// 해당 서버에 연결된 디스크가 존재하는 경우
				if (await m_dbContext.Disks.AnyAsync(i => i.ServerId == Exist.Id))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVERS_REMOVE_AFTER_REMOVING_DISK);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 네트워크 인터페이스 목록을 가져온다.
						var NetworkInterfaces = await m_dbContext.NetworkInterfaces.AsNoTracking()
							.Where(i => i.ServerId == Exist.Id)
							.ToListAsync();

						// 모든 네트워크 인터페이스에 대해서 처리
						foreach (var NetworkInterface in NetworkInterfaces)
						{
							// 서비스 연결 목록을 가져온다.
							var Services = await m_dbContext.ServiceNetworkInterfaceVlans
								.Where(i => i.NetworkInterfaceVlan.InterfaceId == NetworkInterface.Id)
								.ToListAsync();
							// 서비스 연결 목록 삭제
							if (Services.Count > 0)
								m_dbContext.ServiceNetworkInterfaceVlans.RemoveRange(Services);

							// VLAN 목록을 가져온다.
							var Vlans = await m_dbContext.NetworkInterfaceVlans
								.Where(i => i.InterfaceId == NetworkInterface.Id)
								.ToListAsync();
							// VLAN 목록 삭제
							if (Vlans.Count > 0)
								m_dbContext.NetworkInterfaceVlans.RemoveRange(Vlans);

							// 네트워크 사용 정보 삭제
							var Usages = await m_dbContext.NetworkInterfaceUsages
								.Where(i => i.Id == NetworkInterface.Id)
								.ToListAsync();
							// 네트워크 사용 정보 목록 삭제
							if (Vlans.Count > 0)
								m_dbContext.NetworkInterfaceUsages.RemoveRange(Usages);
						}
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 네트워크 인터페이스 삭제
						m_dbContext.NetworkInterfaces.RemoveRange(NetworkInterfaces);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 서버 사용 정보를 가져온다.
						var ServerUsages = await m_dbContext.ServerUsages
							.Where(i => i.Id == Exist.Id)
							.ToListAsync();

						// 서버 사용 정보 삭제
						m_dbContext.ServerUsages.RemoveRange(ServerUsages);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 해당 데이터 삭제
						m_dbContext.Servers.Remove(Exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 삭제된 서버 정보 전송
						SendMq("*.servers.removed", new { Id = Exist.Id, Name = Exist.Name });
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
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

		/// <summary>서버 목록을 가져온다.</summary>
		/// <param name="SearchStates">검색할 서버 상태 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, CpuModel, Clock, State, Rack, LoadAverage1M, LoadAverage5M, LoadAverage15M, MemoryTotal, MemoryUsed, MemoryFree)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>서버 목록 객체</returns>
		public async Task<ResponseList<ResponseServer>> GetList(
			List<EnumServerState> SearchStates = null,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Result = new ResponseList<ResponseServer>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				short Clock = -1;
				if (SearchFields != null && SearchFields.Contains("clock"))
				{
					if (!short.TryParse(SearchKeyword, out Clock))
						Clock = -1;
				}

				// 목록을 가져온다.
				Result.Data = await m_dbContext.Servers.AsNoTracking()
					.Where(i =>
						(
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("name") && i.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("description") && i.Description.Contains(SearchKeyword))
							|| (SearchFields.Contains("cpumodel") && i.CpuModel.Contains(SearchKeyword))
							|| (SearchFields.Contains("clock") && i.Clock == Clock)
						)
						&& (SearchStates == null || SearchStates.Count == 0 || SearchStates.Select(j => (int)j).Contains((int)i.State))
					)
					.Select(i => new
					{
						i.Id,
						i.Name,
						i.Description,
						i.CpuModel,
						i.Clock,
						i.State,
						i.Rack,
						i.LoadAverage1M,
						i.LoadAverage5M,
						i.LoadAverage15M,
						i.MemoryTotal,
						i.MemoryUsed,
						MemoryFree = i.MemoryTotal - i.MemoryUsed < 0 ? 0 : i.MemoryTotal - i.MemoryUsed,
						i.ModDate,
						i.ModId,
						i.ModName
					})
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<dynamic, ResponseServer>(Skip, CountPerPage);

				Result.Result = EnumResponseResult.Success;

			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서버 정보를 가져온다.</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <returns>서버 정보 객체</returns>
		public async Task<ResponseData<ResponseServerDetail>> Get(string Id)
		{
			var Result = new ResponseData<ResponseServerDetail>();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData<ResponseServerDetail>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				ResponseServerDetail Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Servers.AsNoTracking()
					.Where(i => i.Id == GuidId)
					.Include(i => i.Disks)
					.Include(i => i.Services)
					.Include(i => i.NetworkInterfaces)
					.ThenInclude(i => i.NetworkInterfaceVlans)
					.FirstOrDefaultAsync<Server, ResponseServerDetail>();
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Servers.AsNoTracking()
					.Where(i => i.Name == Id)
					.Include(i => i.Disks)
					.Include(i => i.Services)
					.Include(i => i.NetworkInterfaces)
					.ThenInclude(i => i.NetworkInterfaceVlans)
					.FirstOrDefaultAsync<Server, ResponseServerDetail>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseServerDetail>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 서버 내의 모든 Vlan ID 목록
				var VlanIds = new List<Guid>();

				//디스크 풀이름을 가져온다.
				if (Exist.Disks.Count > 0)
				{
					foreach (var Disk in Exist.Disks)
					{
						Guid.TryParse(Disk.DiskPoolId, out Guid DiskpoolGuid);
						string diskName = await m_dbContext.DiskPools.AsNoTracking()
							.Where(i => i.Id == DiskpoolGuid)
							.Select(i => i.Name)
							.FirstOrDefaultAsync();
						Disk.DiskPoolName = diskName;
					}
				}

				Result.Data = Exist;
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>해당 이름이 존재하는지 여부</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 서버 아이디</param>
		/// <param name="Name">검색할 이름</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsNameExist(string ExceptId, string Name)
		{
			ResponseData<bool> Result = new ResponseData<bool>();

			try
			{
				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				Guid GuidId = Guid.Empty;
				if (!ExceptId.IsEmpty() && !Guid.TryParse(ExceptId, out GuidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청 객체가 유효하지 않은 경우
				if (Name.IsEmpty())
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVERS_REQUIRE_NAME);

				Result.Data = await IsNameExist(Name, GuidId != Guid.Empty ? GuidId : null);
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>해당 이름이 존재하는지 여부</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 서버 아이디</param>
		/// <param name="Name">검색할 이름</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<bool> IsNameExist(string Name, Guid? ExceptId = null)
		{
			try
			{
				return await m_dbContext.Servers.AsNoTracking().AnyAsync(i => (ExceptId == null || i.Id != ExceptId) && i.Name == Name);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}

			return false;
		}

		public static readonly string THRESHOLD_SERVER_TIMEOUT = "THRESHOLD.SERVER_TIMEOUT";
		public static readonly long DEFAULT_THRESHOLD_SERVER_TIMEOUT = 30000;

		/// <summary>서버 타임아웃 임계값을 가져온다.</summary>
		/// <returns>임계값 정보 객체</returns>
		public async Task<ResponseData<long>> GetThreshold()
		{
			var Result = new ResponseData<long>();

			try
			{
				// 해당 정보를 가져온다.
				var ServerConfig = await m_dbContext.Configs.AsNoTracking().Where(i => i.Key == THRESHOLD_SERVER_TIMEOUT).FirstOrDefaultAsync();

				if (ServerConfig != null && long.TryParse(ServerConfig.Value, out long ServerTimeout)) Result.Data = ServerTimeout;
				else Result.Data = DEFAULT_THRESHOLD_SERVER_TIMEOUT;

				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서버 타임아웃 임계값을 설정한다.</summary>
		/// <param name="Timeout">임계값 정보 객체</param>
		/// <returns>처리 결과</returns>
		public async Task<ResponseData> SetThreshold(long Timeout)
		{
			var Result = new ResponseData();

			try
			{
				// 요청이 유효하지 않은 경우
				if (Timeout < 1)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON_THRESHOLD_INVALID);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 해당 정보를 가져온다.
						var ServerConfig = await m_dbContext.Configs.FirstOrDefaultAsync(i => i.Key == THRESHOLD_SERVER_TIMEOUT);

						// 해당 정보가 존재하지 않는 경우 추가한다.
						if (ServerConfig == null)
							await m_dbContext.Configs.AddAsync(new Config() { Key = THRESHOLD_SERVER_TIMEOUT, Value = Timeout.ToString() });
						// 존재할 경우 수정한다.
						else
							ServerConfig.Value = Timeout.ToString();

						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
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