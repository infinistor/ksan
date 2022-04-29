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
using PortalData.Responses.Networks;
using PortalData.Responses.Servers;
using PortalData.Responses.Services;
using PortalModels;
using PortalProvider.Providers.RabbitMq;
using PortalProviderInterface;
using PortalResources;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Storage;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.EntityFramework;
using MTLib.Reflection;

namespace PortalProvider.Providers.Servers
{
	/// <summary>서버 데이터 프로바이더 클래스</summary>
	public class ServerProvider : BaseProvider<PortalModel>, IServerProvider
	{

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
			ILogger<ServerProvider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		/// <summary>서버 등록</summary>
		/// <param name="request">서버 등록 요청 객체</param>
		/// <returns>서버 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseServerDetail>> Add(RequestServer request)
		{
			ResponseData<ResponseServerDetail> result = new ResponseData<ResponseServerDetail>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseServerDetail>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 동일한 이름이 존재하는지 확인한다.
				ResponseData<bool> responseExist = await this.IsNameExist(null, new RequestIsServerNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseServerDetail>(responseExist.Result, responseExist.Code, responseExist.Message);

				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData<ResponseServerDetail>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVERS_DUPLICATED_NAME);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						Server newData = new Server()
						{
							Id = Guid.NewGuid(),
							Name = request.Name,
							Description = request.Description,
							CpuModel = request.CpuModel,
							Clock = request.Clock,
							State = (EnumDbServerState)request.State,
							Rack = request.Rack,
							MemoryTotal = request.MemoryTotal,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.Servers.AddAsync(newData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
						result.Data = (await this.Get(newData.Id.ToString())).Data;

						// 추가된 서버 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.added", result.Data);
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>서버 수정</summary>
		/// <param name="id">서버 아이디</param>
		/// <param name="request">서버 수정 요청 객체</param>
		/// <returns>서버 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string id, RequestServer request)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 동일한 이름이 존재하는지 확인한다.
				ResponseData<bool> responseExist = await this.IsNameExist(id, new RequestIsServerNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData(responseExist.Result, responseExist.Code, responseExist.Message);

				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVERS_DUPLICATED_NAME);

				// 해당 정보를 가져온다.
				Server exist = await m_dbContext.Servers
					.Include(i => i.NetworkInterfaces)
					.ThenInclude(i => i.NetworkInterfaceVlans)
					.ThenInclude(i => i.ServiceNetworkInterfaceVlans)
					.ThenInclude(i => i.Service)
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 전체 메모리 크기가 변경된 경우
						if (exist.MemoryTotal != request.MemoryTotal)
						{
							// 모든 네트워크 인터페이스에 대해서 처리
							foreach (NetworkInterface networkInterface in exist.NetworkInterfaces)
							{
								// 모든 VLAN에 대해서 처리
								foreach (NetworkInterfaceVlan networkInterfaceVlan in networkInterface.NetworkInterfaceVlans)
								{
									// 모든 서비스 연결 정보에 대해서 처리
									foreach (ServiceNetworkInterfaceVlan serviceNetworkInterfaceVlan in networkInterfaceVlan.ServiceNetworkInterfaceVlans)
									{
										// 오프라인 처리
										serviceNetworkInterfaceVlan.Service.MemoryTotal = request.MemoryTotal;
										if (m_dbContext.HasChanges())
											await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
									}
								}
							}
						}

						// 정보를 수정한다.
						exist.Name = request.Name;
						exist.Description = request.Description;
						exist.CpuModel = request.CpuModel;
						exist.Clock = request.Clock;
						exist.State = (EnumDbServerState)request.State;
						exist.Rack = request.Rack;
						exist.MemoryTotal = request.MemoryTotal;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							exist.ModId = LoginUserId;
							exist.ModName = LoginUserName;
							exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}
						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;

						// 상세 정보를 가져온다.
						ResponseServerDetail response = (await this.Get(id)).Data;

						// 수정된 서버 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.updated", response);
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>서버 상태 수정</summary>
		/// <param name="id">서버 아이디</param>
		/// <param name="state">서버 상태</param>
		/// <param name="modId">수정자 아이디</param>
		/// <param name="modName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(string id, EnumServerState state, string modId = "", string modName = "")
		{
			ResponseData result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Server exist = await m_dbContext.Servers
					// .Include(i => i.NetworkInterfaces)
					// .ThenInclude(i => i.NetworkInterfaceVlans)
					// .ThenInclude(i => i.ServiceNetworkInterfaceVlans)
					// .ThenInclude(i => i.Service)
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 파라미터로 넘어온 수정자 아이디 파싱
				Guid guidModId = Guid.Empty;
				if (!modId.IsEmpty())
					Guid.TryParse(modId, out guidModId);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// // 오프라인 상태인 경우, 해당 서버와 연관된 모든 서비스 오프라인 처리
						// if (state == EnumServerState.Offline)
						// {
						// 	// 모든 네트워크 인터페이스에 대해서 처리
						// 	foreach (NetworkInterface networkInterface in exist.NetworkInterfaces)
						// 	{
						// 		// 모든 VLAN에 대해서 처리
						// 		foreach (NetworkInterfaceVlan networkInterfaceVlan in networkInterface.NetworkInterfaceVlans)
						// 		{
						// 			// 모든 서비스 연결 정보에 대해서 처리
						// 			foreach (ServiceNetworkInterfaceVlan serviceNetworkInterfaceVlan in networkInterfaceVlan.ServiceNetworkInterfaceVlans)
						// 			{
						// 				// 오프라인 처리
						// 				serviceNetworkInterfaceVlan.Service.State = EnumDbServiceState.Offline;
						// 			}
						// 		}
						// 	}
						// }

						// 정보를 수정한다.
						exist.State = (EnumDbServerState)state;
						if (LoginUserId != Guid.Empty || guidModId != Guid.Empty)
							exist.ModId = LoginUserId != Guid.Empty ? LoginUserId : guidModId;
						if (!LoginUserName.IsEmpty() || !modName.IsEmpty())
							exist.ModName = !LoginUserName.IsEmpty() ? LoginUserName : modName;
						exist.ModDate = DateTime.Now;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}


		/// <summary>서버 상태 수정</summary>
		/// <param name="request">서버 상태 수정 요청 객체</param>
		/// <param name="modId">수정자 아이디</param>
		/// <param name="modName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(RequestServerState request, string modId = "", string modName = "")
		{
			ResponseData result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 상태 변경
				result = await this.UpdateState(request.Id, request.State, modId, modName);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>서버 사용 정보 수정</summary>
		/// <param name="id">서버 아이디</param>
		/// <param name="loadAverage1M">1분 Load Average</param>
		/// <param name="loadAverage5M">5분 Load Average</param>
		/// <param name="loadAverage15M">15분 Load Average</param>
		/// <param name="memoryUsed">서버 아이디</param>
		/// <returns>서버 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(string id, float loadAverage1M, float loadAverage5M, float loadAverage15M, decimal memoryUsed)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Server exist = await m_dbContext.Servers
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.LoadAverage1M = loadAverage1M;
						exist.LoadAverage5M = loadAverage5M;
						exist.LoadAverage15M = loadAverage15M;
						exist.MemoryUsed = memoryUsed;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 사용 정보 추가
						m_dbContext.ServerUsages.Add(new ServerUsage()
						{
							Id = exist.Id,
							RegDate = DateTime.Now,
							LoadAverage1M = loadAverage1M,
							LoadAverage5M = loadAverage5M,
							LoadAverage15M = loadAverage15M,
							MemoryTotal = exist.MemoryTotal,
							MemoryUsed = memoryUsed
						});
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>서버 사용 정보 수정</summary>
		/// <param name="request">서버 사용 정보 수정 요청 객체</param>
		/// <returns>서버 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(RequestServerUsage request)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 상태 변경
				result = await this.UpdateUsage(request.Id, request.LoadAverage1M, request.LoadAverage5M, request.LoadAverage15M, request.MemoryUsed);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>서버 삭제</summary>
		/// <param name="id">서버 아이디</param>
		/// <returns>서버 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string id)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Server exist = await m_dbContext.Servers.AsNoTracking()
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(result.Result = EnumResponseResult.Success);

				// 해당 서버에 연결된 디스크가 존재하는 경우
				if (await m_dbContext.Disks.AnyAsync(i => i.ServerId == guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVERS_REMOVE_AFTER_REMOVING_DISK);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 네트워크 인터페이스 목록을 가져온다.
						List<NetworkInterface> networkInterfaces = await m_dbContext.NetworkInterfaces.AsNoTracking()
							.Where(i => i.ServerId == guidId)
							.ToListAsync();

						// 모든 네트워크 인터페이스에 대해서 처리
						foreach (NetworkInterface networkInterface in networkInterfaces)
						{
							// 서비스 연결 목록을 가져온다.
							List<ServiceNetworkInterfaceVlan> services = await m_dbContext.ServiceNetworkInterfaceVlans
								.Where(i => i.NetworkInterfaceVlan.InterfaceId == networkInterface.Id)
								.ToListAsync();
							// 서비스 연결 목록 삭제
							if (services.Count > 0)
								m_dbContext.ServiceNetworkInterfaceVlans.RemoveRange(services);

							// VLAN 목록을 가져온다.
							List<NetworkInterfaceVlan> vlans = await m_dbContext.NetworkInterfaceVlans
								.Where(i => i.InterfaceId == networkInterface.Id)
								.ToListAsync();
							// VLAN 목록 삭제
							if (vlans.Count > 0)
								m_dbContext.NetworkInterfaceVlans.RemoveRange(vlans);

							// 네트워크 사용 정보 삭제
							List<NetworkInterfaceUsage> usages = await m_dbContext.NetworkInterfaceUsages
								.Where(i => i.Id == networkInterface.Id)
								.ToListAsync();
							// 네트워크 사용 정보 목록 삭제
							if (vlans.Count > 0)
								m_dbContext.NetworkInterfaceUsages.RemoveRange(usages);
						}
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 네트워크 인터페이스 삭제
						m_dbContext.NetworkInterfaces.RemoveRange(networkInterfaces);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 서버 사용 정보를 가져온다.
						List<ServerUsage> serverUsages = await m_dbContext.ServerUsages
							.Where(i => i.Id == guidId)
							.ToListAsync();

						// 서버 사용 정보 삭제
						m_dbContext.ServerUsages.RemoveRange(serverUsages);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 해당 데이터 삭제
						m_dbContext.Servers.Remove(exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;

						// MQ로 전송할 객체 생성
						ResponseServer response = new ResponseServer();
						response.CopyValueFrom(exist);

						// 삭제된 서버 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.removed", response);
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>서버 목록을 가져온다.</summary>
		/// <param name="searchStates">검색할 서버 상태 목록</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description, CpuModel, Clock, State, Rack, LoadAverage1M, LoadAverage5M, LoadAverage15M, MemoryTotal, MemoryUsed, MemoryFree)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>서버 목록 객체</returns>
		public async Task<ResponseList<ResponseServer>> GetList(
			List<EnumServerState> searchStates,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseServer> result = new ResponseList<ResponseServer>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				short clock = -1;
				if (searchFields.Contains("clock"))
				{
					if (!short.TryParse(searchKeyword, out clock))
						clock = -1;
				}

				// 목록을 가져온다.
				result.Data = await m_dbContext.Servers.AsNoTracking()
					.Where(i =>
						(
							searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
							|| (searchFields.Contains("name") && i.Name.Contains(searchKeyword))
							|| (searchFields.Contains("description") && i.Description.Contains(searchKeyword))
							|| (searchFields.Contains("cpumodel") && i.CpuModel.Contains(searchKeyword))
							|| (searchFields.Contains("clock") && i.Clock == clock)
						)
						&& (searchStates == null || searchStates.Count == 0 || searchStates.Select(j => (int)j).Contains((int)i.State))
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
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<dynamic, ResponseServer>(skip, countPerPage);

				result.Result = EnumResponseResult.Success;

			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>서버 정보를 가져온다.</summary>
		/// <param name="id">서버 아이디</param>
		/// <returns>서버 정보 객체</returns>
		public async Task<ResponseData<ResponseServerDetail>> Get(string id)
		{
			ResponseData<ResponseServerDetail> result = new ResponseData<ResponseServerDetail>();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData<ResponseServerDetail>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				ResponseServerDetail exist = await m_dbContext.Servers.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.NetworkInterfaces)
					.ThenInclude(i => i.NetworkInterfaceVlans)
					.Include(i => i.Disks)
					.FirstOrDefaultAsync<Server, ResponseServerDetail>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<ResponseServerDetail>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 서버 내의 모든 Vlan ID 목록
				List<Guid> vlanIds = new List<Guid>();

				// 모든 네트워크 인터페이스의 Vlan ID들을 추가한다.
				foreach (ResponseNetworkInterfaceDetail networkInterfaceDetail in exist.NetworkInterfaces)
					vlanIds.AddRange(networkInterfaceDetail.NetworkInterfaceVlans.Select(i => Guid.Parse(i.Id)).ToList());

				// 서버의 Vlan과 연결된 모든 서비스를 가져온다.
				List<ResponseService> services = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
					.Where(i => vlanIds.Contains(i.VlanId))
					.Select(i => i.Service)
					.ToListAsync<Service, ResponseService>();

				if (services.Count > 0) exist.Services = services;

				//디스크 풀이름을 가져온다.
				if (exist.Disks.Count > 0)
				{
					foreach (var disk in exist.Disks)
					{
						Guid.TryParse(disk.DiskPoolId, out Guid diskpoolId);
						string diskName = await m_dbContext.DiskPools.AsNoTracking()
							.Where(i => i.Id == diskpoolId)
							.Select(i => i.Name)
							.FirstOrDefaultAsync();
						disk.DiskPoolName = diskName;
					}
				}

				result.Data = exist;
				result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>해당 이름이 존재하는지 여부</summary>
		/// <param name="exceptId">이름 검색 시 제외할 서버 아이디</param>
		/// <param name="request">특정 이름의 서버 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsNameExist(string exceptId, RequestIsServerNameExist request)
		{
			ResponseData<bool> result = new ResponseData<bool>();
			Guid guidId = Guid.Empty;

			try
			{
				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				if (!exceptId.IsEmpty() && !Guid.TryParse(exceptId, out guidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청 객체가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<bool>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 동일한 이름이 존재하는 경우
				if (await m_dbContext.Servers.AsNoTracking().AnyAsync(i => (exceptId.IsEmpty() || i.Id != guidId) && i.Name == request.Name))
					result.Data = true;
				// 동일한 이름이 존재하지 않는 경우
				else
					result.Data = false;
				result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}
	}
}