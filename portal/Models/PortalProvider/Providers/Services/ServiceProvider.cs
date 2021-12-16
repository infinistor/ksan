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
using PortalData.Requests.Services;
using PortalData.Requests.Services.Configs;
using PortalData.Responses.Networks;
using PortalData.Responses.Services;
using PortalData.Responses.Services.Configs;
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
using IServiceProvider = PortalProviderInterface.IServiceProvider;

namespace PortalProvider.Providers.Services
{
    /// <summary>서비스 데이터 프로바이더 클래스</summary>
    public class ServiceProvider : BaseProvider<PortalModel>, IServiceProvider
	{
		
		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public ServiceProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<ServiceProvider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		/// <summary>서비스 등록</summary>
		/// <param name="request">서비스 등록 요청 객체</param>
		/// <returns>서비스 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseServiceWithVlans>> Add(RequestService request)
		{
			ResponseData<ResponseServiceWithVlans> result = new ResponseData<ResponseServiceWithVlans>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 동일한 이름이 존재하는지 확인한다.
				ResponseData<bool> responseExist = await this.IsNameExist(null, new RequestIsServiceNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseServiceWithVlans>(responseExist.Result, responseExist.Code, responseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICES_DUPLICATED_NAME);
				
				// 그룹 아이디가 존재하고 유효한 Guid가 아닌 경우
				Guid guidGroupId = Guid.Empty;
				if(!request.GroupId.IsEmpty() && !Guid.TryParse(request.GroupId, out guidGroupId))
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_GROUP_ID);

				// 그룹 아이디가 유효한 경우
				if (guidGroupId != Guid.Empty)
				{
					// 해당 서비스 그룹 정보를 가져온다.
					ServiceGroup serviceGroup = await this.m_dbContext.ServiceGroups.AsNoTracking()
						.Where(i => i.Id == guidGroupId)
						.FirstOrDefaultAsync();
	
					// 해당 서비스 그룹이 존재하지 않는 경우
					if(serviceGroup == null)
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_SERVICE_GROUP);
				}

				decimal memoryTotal = 0;
				List<Server> servers = new List<Server>();
		
				// 모든 VLAN 아이디들에 대해서 처리
				List<Guid> guidVlanIds = new List<Guid>();
				foreach (string vlanId in request.VlanIds)
				{
					// VLAN 아이디가 유효하지 않은 경우
					if(vlanId.IsEmpty() || !Guid.TryParse(vlanId, out Guid guidVlanId))
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_VLAN_ID);

					// 해당 VLAN 정보를 가져온다.
					NetworkInterfaceVlan vlan = await this.m_dbContext.NetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.Id == guidVlanId)
						.Include(i => i.NetworkInterface)
						.ThenInclude(i => i.Server)
						.FirstOrDefaultAsync();
	
					// 해당 VLAN 정보가 존재하지 않는 경우
					if(vlan == null)
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_VLAN);

					guidVlanIds.Add(guidVlanId);
							
					// 이전에 처리한 서버가 아닌 경우
					if (servers.Count(i => i.Id == vlan.NetworkInterface.Server.Id) == 0)
					{
						servers.Add(vlan.NetworkInterface.Server);
					
						// 전체 메모리 크기 저장
						memoryTotal = vlan.NetworkInterface.Server.MemoryTotal ?? 0;
					}
				}

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						Service newData = new Service()
						{
							Id = Guid.NewGuid(),
							GroupId = guidGroupId == Guid.Empty ? null : guidGroupId,
							Name = request.Name,
							Description = request.Description,
							ServiceType = (EnumDbServiceType) request.ServiceType,
							HaAction = (EnumDbHaAction) request.HaAction,
							State = (EnumDbServiceState) request.State,
							MemoryTotal = memoryTotal,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.Services.AddAsync(newData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						
						// 모든 VLAN 아이디들에 대해서 처리
						foreach (Guid guidVlanId in guidVlanIds)
						{
							// 해당 VLAN 정보를 생성한다.
							ServiceNetworkInterfaceVlan newVlan = new ServiceNetworkInterfaceVlan()
							{
								ServiceId = newData.Id,
								VlanId = guidVlanId
							};
							await this.m_dbContext.ServiceNetworkInterfaceVlans.AddAsync(newVlan);
						}
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						
						await transaction.CommitAsync();
						
						result.Result = EnumResponseResult.Success;
						result.Data = (await this.Get(newData.Id.ToString())).Data;
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
		
		/// <summary>서비스 수정</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="request">서비스 수정 요청 객체</param>
		/// <returns>서비스 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string id, RequestService request)
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
				ResponseData<bool> responseExist = await this.IsNameExist(id, new RequestIsServiceNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData(responseExist.Result, responseExist.Code, responseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICES_DUPLICATED_NAME);
				
				// 그룹 아이디가 존재하고 유효한 Guid가 아닌 경우
				Guid guidGroupId = Guid.Empty;
				if(!request.GroupId.IsEmpty() && !Guid.TryParse(request.GroupId, out guidGroupId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_GROUP_ID);

				// 그룹 아이디가 유효한 경우
				if (guidGroupId != Guid.Empty)
				{
					// 해당 서비스 그룹 정보를 가져온다.
					ServiceGroup serviceGroup = await this.m_dbContext.ServiceGroups.AsNoTracking()
						.Where(i => i.Id == guidGroupId)
						.FirstOrDefaultAsync();
	
					// 해당 서비스 그룹이 존재하지 않는 경우
					if(serviceGroup == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_SERVICE_GROUP);
				}
				
				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				decimal memoryTotal = 0;
				List<Server> servers = new List<Server>();
				
				// 모든 VLAN 아이디들에 대해서 처리
				List<Guid> guidVlanIds = new List<Guid>();
				foreach (string vlanId in request.VlanIds)
				{
					// VLAN 아이디가 유효하지 않은 경우
					if(vlanId.IsEmpty() || !Guid.TryParse(vlanId, out Guid guidVlanId))
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_VLAN_ID);

					// 해당 VLAN 정보를 가져온다.
					NetworkInterfaceVlan vlan = await this.m_dbContext.NetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.Id == guidVlanId)
						.Include(i => i.NetworkInterface)
						.ThenInclude(i => i.Server)
						.FirstOrDefaultAsync();
	
					// 해당 VLAN 정보가 존재하지 않는 경우
					if(vlan == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_VLAN);

					guidVlanIds.Add(guidVlanId);
					
					// 이전에 처리한 서버가 아닌 경우
					if (servers.Count(i => i.Id == vlan.NetworkInterface.Server.Id) == 0)
					{
						servers.Add(vlan.NetworkInterface.Server);
					
						// 전체 메모리 크기 저장
						memoryTotal = vlan.NetworkInterface.Server.MemoryTotal ?? 0;
					}
				}
				
				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.GroupId = guidGroupId == Guid.Empty ? null : guidGroupId;
						exist.Name = request.Name;
						exist.Description = request.Description;
						exist.ServiceType = (EnumDbServiceType) request.ServiceType;
						exist.HaAction = (EnumDbHaAction) request.HaAction;
						exist.State = (EnumDbServiceState) request.State;
						exist.MemoryTotal = memoryTotal;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							exist.ModId = LoginUserId;
							exist.ModName = LoginUserName;
							exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}

						// 기존 VLAN 목록을 가져온다.
						List<ServiceNetworkInterfaceVlan> existVlans = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
							.Where(i => i.ServiceId == guidId)
							.ToListAsync();
						m_dbContext.RemoveRange(existVlans);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
							
						// 모든 VLAN 아이디들에 대해서 처리
						foreach (Guid guidVlanId in guidVlanIds)
						{
							// 해당 VLAN 정보를 생성한다.
							ServiceNetworkInterfaceVlan newVlan = new ServiceNetworkInterfaceVlan()
							{
								ServiceId = guidId,
								VlanId = guidVlanId
							};
							await this.m_dbContext.ServiceNetworkInterfaceVlans.AddAsync(newVlan);
						}
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

		/// <summary>서비스 상태 수정</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="state">서비스 상태</param>
		/// <param name="modId">수정자 아이디</param>
		/// <param name="modName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(string id, EnumServiceState state, string modId = "", string modName = "")
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services
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
						// 정보를 수정한다.
						exist.State = (EnumDbServiceState) state;
						if(LoginUserId != Guid.Empty || guidModId != Guid.Empty)
							exist.ModId = LoginUserId != Guid.Empty ? LoginUserId : guidModId;
						if(!LoginUserName.IsEmpty() || !modName.IsEmpty())
							exist.ModName = !LoginUserName.IsEmpty() ? LoginUserName : modName;
						// 데이터가 변경된 경우 저장
						if(m_dbContext.HasChanges())
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

		/// <summary>서비스 상태 수정</summary>
		/// <param name="request">상태 수정 요청 객체</param>
		/// <param name="modId">수정자 아이디</param>
		/// <param name="modName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(RequestServiceState request, string modId = "", string modName = "")
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 서비스 상태 수정
				result = await UpdateState(request.Id, request.State, modId, modName);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			
			return result;
		}

		/// <summary>서비스 사용 정보 수정</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="cpuUsage">CPU 사용률</param>
		/// <param name="memoryUsed">서버 아이디</param>
		/// <param name="threadCount">스레드 수</param>
		/// <returns>서비스 사용 정보 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(string id, float cpuUsage, decimal memoryUsed, int threadCount)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.CpuUsage = cpuUsage;
						exist.MemoryUsed = memoryUsed;
						exist.ThreadCount = threadCount;
						// 데이터가 변경된 경우 저장
						if(m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						
						// 사용 정보 추가
						m_dbContext.ServiceUsages.Add(new ServiceUsage()
						{
							Id = exist.Id,
							RegDate = DateTime.Now,
							CpuUsage = cpuUsage,
							MemoryTotal = exist.MemoryTotal,
							MemoryUsed = memoryUsed,
							ThreadCount = threadCount
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

		/// <summary>서비스 사용 정보 수정</summary>
		/// <param name="request">서비스 사용 정보 수정 요청 객체</param>
		/// <returns>서비스 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(RequestServiceUsage request)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 서비스 상태 수정
				result = await UpdateUsage(request.Id, request.CpuUsage, request.MemoryUsed, request.ThreadCount);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			
			return result;
		}

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="state">HA 상태</param>
		/// <param name="modId">수정자 아이디</param>
		/// <param name="modName">수정자명</param>
		/// <returns>서비스 HA 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateHaAction(string id, EnumHaAction state, string modId = "", string modName = "")
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services
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
						// 정보를 수정한다.
						exist.HaAction = (EnumDbHaAction) state;
						if(LoginUserId != Guid.Empty || guidModId != Guid.Empty)
							exist.ModId = LoginUserId != Guid.Empty ? LoginUserId : guidModId;
						if(!LoginUserName.IsEmpty() || !modName.IsEmpty())
							exist.ModName = !LoginUserName.IsEmpty() ? LoginUserName : modName;
						// 데이터가 변경된 경우 저장
						if(m_dbContext.HasChanges())
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

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="request">HA 상태 수정 요청 객체</param>
		/// <param name="modId">수정자 아이디</param>
		/// <param name="modName">수정자명</param>
		/// <returns>서비스 HA 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateHaAction(RequestServiceHaAction request, string modId = "", string modName = "")
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 서비스 상태 수정
				result = await UpdateHaAction(request.Id, request.HaAction, modId, modName);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			
			return result;
		}
		
		/// <summary>서비스 삭제</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>서비스 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string id)
		{
			ResponseData result = new ResponseData();
			
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services.AsNoTracking()
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(result.Result = EnumResponseResult.Success);
				
				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// VLAN 연결 목록을 가져온다.
						List<ServiceNetworkInterfaceVlan> vlans = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
							.Where(i => i.ServiceId == guidId)
							.ToListAsync();
						// 서비스 연결 목록 삭제
						m_dbContext.ServiceNetworkInterfaceVlans.RemoveRange(vlans);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 서비스 사용 정보 목록을 가져온다.
						List<ServiceUsage> usages = await m_dbContext.ServiceUsages.AsNoTracking()
							.Where(i => i.Id == guidId)
							.ToListAsync();
						// 서비스 사용 정보 목록 삭제
						m_dbContext.ServiceUsages.RemoveRange(usages);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						
						// 해당 데이터 삭제
						m_dbContext.Services.Remove(exist);
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
		
		/// <summary>서비스 목록을 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description, ServiceType)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (GroupName, Name, Description, ServiceType, IpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseServiceWithGroup>> GetList(
			int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseServiceWithGroup> result = new ResponseList<ResponseServiceWithGroup>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				EnumServiceType serviceType = EnumServiceType.Unknown;
				if (searchFields.Contains("servicetype"))
					Enum.TryParse(searchKeyword, out serviceType);
					
				// 목록을 가져온다.
				result.Data = await m_dbContext.Services.AsNoTracking()
					.Where(i =>
						(
				            searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
				            || (searchFields.Contains("groupname") && i.ServiceGroup != null && i.ServiceGroup.Name.Contains(searchKeyword))
				            || (searchFields.Contains("name") && i.Name.Contains(searchKeyword))
				            || (searchFields.Contains("description") && i.Description.Contains(searchKeyword))
				            || (searchFields.Contains("servicetype") && i.ServiceType == (EnumDbServiceType) serviceType)
				            || (searchFields.Contains("ipaddress") && i.Vlans.Any(j => j.NetworkInterfaceVlan != null && j.NetworkInterfaceVlan.IpAddress.Contains(searchKeyword)))
					    )
					)
					.OrderByWithDirection(orderFields, orderDirections)
					.Include(i => i.ServiceGroup)
					.CreateListAsync<Service, ResponseServiceWithGroup>(skip, countPerPage);

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
		
		/// <summary>서비스 정보를 가져온다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>서비스 정보 객체</returns>
		public async Task<ResponseData<ResponseServiceWithVlans>> Get(string id)
		{
			ResponseData<ResponseServiceWithVlans> result = new ResponseData<ResponseServiceWithVlans>();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);
				
				// 정보를 가져온다.
				ResponseServiceWithVlans exist = await m_dbContext.Services.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.ServiceGroup)
					.Include(i => i.Vlans)
					.ThenInclude(i => i.NetworkInterfaceVlan)
					.FirstOrDefaultAsync<Service, ResponseServiceWithVlans>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 서비스 아이디가 유효한 경우
				if (Guid.TryParse(exist.Id, out Guid guidServiceId))
				{
					// 해당 서버스의 VLAN 정보를 가져온다.
					exist.Vlans = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.ServiceId == guidServiceId)
						.Select(i => i.NetworkInterfaceVlan)
						.ToListAsync<NetworkInterfaceVlan, ResponseNetworkInterfaceVlan>();
						
					// VLAN 아이디가 유효한 경우
					foreach(var Vlan in exist.Vlans)
					{
						if (Guid.TryParse(Vlan.InterfaceId, out Guid interfaceId))
						{
							var ServerId = await m_dbContext.NetworkInterfaces.AsNoTracking()
								.Where(i => i.Id == interfaceId)
								.Select(i => i.ServerId)
								.FirstOrDefaultAsync();
							Vlan.ServerId = ServerId.ToString();
						}
					}
				}

				// 서비스 그룹 아이디가 유효한 경우
				if (Guid.TryParse(exist.GroupId, out Guid guidGroupId))
				{
					// 해당 서버스의 서비스 그룹 정보를 가져온다.
					exist.ServiceGroup = await m_dbContext.ServiceGroups.AsNoTracking()
						.Where(i => i.Id == guidGroupId)
						.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroup>();
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
		/// <param name="exceptId">이름 검색 시 제외할 서비스 아이디</param>
		/// <param name="request">특정 이름의 서비스 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsNameExist(string exceptId, RequestIsServiceNameExist request)
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
				if (await m_dbContext.Services.AsNoTracking().AnyAsync(i => (exceptId.IsEmpty() || i.Id != guidId) && i.Name == request.Name))
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

		/// <summary>서비스 시작</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>서비스 시작 결과 객체</returns>
		public async Task<ResponseData> Start(string id)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 서비스와 연결된 서버 아이디를 가져온다.
				var Server = Get(id);
				
				// 서비스와 연결된 서버가 존재하지 않을 경우 에러 반환
				if(Server.Result.Data.Vlans.Count == 0)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.UL_COMMON__NO_SERVER);

				//서비스 아이디
				var ServerId = Server.Result.Data.Vlans[0].ServerId;

				// 서비스 시작 요청
				ResponseData response = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.services.{ServerId}.control", new RequestServiceControl(id, EnumServiceControl.Start), 10);
			
				// 실패인 경우
				if (response.Result != EnumResponseResult.Success)
					return new ResponseData(EnumResponseResult.Error, response.Code, response.Message);

				// 상태 수정
				result = await this.UpdateState(id, EnumServiceState.Online);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			
			return result;
		}
		
		/// <summary>서비스 중지</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>서비스 중지 결과 객체</returns>
		public async Task<ResponseData> Stop(string id)
		{
			ResponseData result = new ResponseData();
			
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services.AsNoTracking()
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 서비스와 연결된 서버 아이디를 가져온다.
				var Server = Get(id);
				
				// 서비스와 연결된 서버가 존재하지 않을 경우 에러 반환
				if(Server.Result.Data.Vlans.Count == 0)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.UL_COMMON__NO_SERVER);

				//서비스 아이디
				var ServerId = Server.Result.Data.Vlans[0].ServerId;

				// 서비스 중지 요청
				ResponseData response = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.services.{ServerId}.control", new RequestServiceControl(id, EnumServiceControl.Stop), 10);
				
				// 실패인 경우
				if (response.Result != EnumResponseResult.Success)
					return new ResponseData(EnumResponseResult.Error, response.Code, response.Message);

				// 상태 수정
				result = await this.UpdateState(id, EnumServiceState.Offline);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			
			return result;
		}
		
		/// <summary>서비스 재시작</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>서비스 재시작 결과 객체</returns>
		public async Task<ResponseData> Restart(string id)
		{
			ResponseData result = new ResponseData();
			
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services.AsNoTracking()
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 서비스와 연결된 서버 아이디를 가져온다.
				var Server = Get(id);
				
				// 서비스와 연결된 서버가 존재하지 않을 경우 에러 반환
				if(Server.Result.Data.Vlans.Count == 0)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.UL_COMMON__NO_SERVER);

				//서비스 아이디
				var ServerId = Server.Result.Data.Vlans[0].ServerId;

				// 서비스 재시작 요청
				ResponseData response = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.services.{ServerId}.control", new RequestServiceControl(id, EnumServiceControl.Restart), 10);
			
				// 실패인 경우
				if (response.Result != EnumResponseResult.Success)
					return new ResponseData(EnumResponseResult.Error, response.Code, response.Message);

				// 상태 수정
				result = await this.UpdateState(id, EnumServiceState.Online);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			
			return result;
		}

		/// <summary>특정 서비스의 설정 정보를 가져온다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>설정 문자열이 포함된 결과 객체</returns>
		public async Task<ResponseData<T>> GetConfig<T>(string id) where T : IResponseServiceConfig
		{
			ResponseData<T> result = new ResponseData<T>();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData<T>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services.AsNoTracking()
					.Include(i => i.Vlans)
					.ThenInclude(i => i.NetworkInterfaceVlan)
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<T>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 서비스와 연관된 모든 아이피 목록을 저장
				List<string> ips = new List<string>();
				foreach (ServiceNetworkInterfaceVlan serviceVlan in exist.Vlans)
					ips.Add(serviceVlan.NetworkInterfaceVlan.IpAddress);
				
				// 해당 서비스와 연결된 서버 아이디를 가져온다.
				var Server = Get(id);
				
				// 서비스와 연결된 서버가 존재하지 않을 경우 에러 반환
				if(Server.Result.Data.Vlans.Count == 0)
					return new ResponseData<T>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.UL_COMMON__NO_SERVER);
				
				//서비스 아이디
				var ServerId = Server.Result.Data.Vlans[0].ServerId;

				// 타입에 따라 라우팅 키 설정
				Type type = typeof(T);
				string routingKey = $"*.services{ServerId}.config.{type.Name.Replace("ResponseServiceConfigFor", "").ToLower()}.load";
						
				// 설정 로드 요청
				ResponseData<string> response = SendRpcMq<string>(RabbitMqConfiguration.ExchangeName, routingKey, new RequestServiceLoadConfig(id, exist.Name, ips), 10);
			
				// 실패인 경우
				if (response.Result != EnumResponseResult.Success)
					return new ResponseData<T>(EnumResponseResult.Error, response.Code, response.Message);
				
				string config = response.Data;

				// 반환할 새로운 객체를 생성한다.
				T instance = Activator.CreateInstance<T>();

				// 객체 생성에 실패한 경우
				if (instance == null)
					return new ResponseData<T>(EnumResponseResult.Error, Resource.EC_COMMON__EXCEPTION, Resource.EM_SERVICES_CANNOT_CREATE_CONFIG_OBJECT);

				// 설정 문자열로 부터 설정 객체를 가져온다.
				ResponseData responseDeserialize = instance.Deserialize(config);
					
				// 설정 객체로 값을 가져오는데 성공한 경우
				if (responseDeserialize.Result == EnumResponseResult.Success)
				{
					result.Data = instance;
					result.Result = EnumResponseResult.Success;
				}
				else
				{
					result.Code = responseDeserialize.Code;
					result.Message = responseDeserialize.Message;
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

		/// <summary>주어진 설정 정보를 특정 서비스에 저장한다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="config">서비스 설정 객체</param>
		/// <returns>설정 결과 객체</returns>
		public async Task<ResponseData> SetConfig<T>(string id, T config) where T : CommonRequestData, IRequestServiceConfig
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service exist = await m_dbContext.Services.AsNoTracking()
					.Include(i => i.Vlans)
					.ThenInclude(i => i.NetworkInterfaceVlan)
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 설정을 문자열로 변환한다.
				ResponseData<string> responseConfig = config.Serialize();
				
				// 설정 문자열 변환에 성공인 경우
				if (responseConfig.Result == EnumResponseResult.Success)
				{
					// 해당 서비스와 연관된 모든 아이피 목록을 저장
					List<string> ips = new List<string>();
					foreach (ServiceNetworkInterfaceVlan serviceVlan in exist.Vlans)
						ips.Add(serviceVlan.NetworkInterfaceVlan.IpAddress);

					// 해당 서비스와 연결된 서버 아이디를 가져온다.
					var Server = Get(id);

					// 서비스와 연결된 서버가 존재하지 않을 경우 에러 반환
					if(Server.Result.Data.Vlans.Count == 0)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.UL_COMMON__NO_SERVER);
					
					//서비스 아이디
					var ServerId = Server.Result.Data.Vlans[0].ServerId;

					// 타입에 따라 라우팅 키 설정
					Type type = typeof(T);
					string routingKey = $"*.services.{ServerId}.config.{type.Name.Replace("RequestServiceConfigFor", "").ToLower()}.save";

					// 설정 저장 요청을 전송한다.
					ResponseData response = SendRpcMq(RabbitMqConfiguration.ExchangeName, routingKey, new RequestServiceSaveConfig(id, exist.Name, ips, responseConfig.Data), 10);
				
					// 실패인 경우
					if (response.Result != EnumResponseResult.Success)
						return new ResponseData(EnumResponseResult.Error, response.Code, response.Message);

					result.Result = EnumResponseResult.Success;
				}
				// 설정 문자열 변환에 실패인 경우
				else
				{
					result.Code = responseConfig.Code;
					result.Message = responseConfig.Message;
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
	}
}