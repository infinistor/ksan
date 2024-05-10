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
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Requests.Services;
using PortalData.Responses.Services;
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
using IServiceProvider = PortalProviderInterface.IServiceProvider;
using MTLib.Reflection;

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
		/// <param name="Request">서비스 등록 요청 객체</param>
		/// <returns>서비스 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseServiceWithVlans>> Add(RequestService Request, Guid? ModId = null, string ModName = null)
		{
			var Result = new ResponseData<ResponseServiceWithVlans>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재하는 경우
				if (await IsNameExist(Request.Name))
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICES_DUPLICATED_NAME);


				// Id를 입력했을 경우
				var Id = Guid.NewGuid();
				if (!string.IsNullOrEmpty(Request.Id))
				{
					// Guid로 반환 가능한지 확인
					if (!Guid.TryParse(Request.Id, out Id))
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

					// 중복 체크
					if (await IsExistId(Id))
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DUPLICATED_ID);
				}

				// 그룹 아이디가 존재하고 유효한 Guid가 아닌 경우
				Guid GroupGuid = Guid.Empty;
				if (!Request.GroupId.IsEmpty() && !Guid.TryParse(Request.GroupId, out GroupGuid))
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_GROUP_ID);

				// 그룹 아이디가 유효한 경우
				if (GroupGuid != Guid.Empty)
				{
					// 해당 서비스 그룹 정보를 가져온다.
					var ServiceGroup = await this.m_dbContext.ServiceGroups.AsNoTracking()
						.Where(i => i.Id == GroupGuid)
						.FirstOrDefaultAsync();

					// 해당 서비스 그룹이 존재하지 않는 경우
					if (ServiceGroup == null)
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_SERVICE_GROUP);
				}

				// 서버 정보를 가져온다
				Server Server = null;

				// Id로 조회할경우
				if (Guid.TryParse(Request.ServerId, out Guid ServerGuid))
					Server = await m_dbContext.Servers.AsNoTracking().FirstOrDefaultAsync(i => i.Id == ServerGuid);
				// 이름으로 조회할 경우
				else
					Server = await m_dbContext.Servers.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Request.ServerId);

				// 해당 서버가 존재하지 않는 경우
				if (Server == null)
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.UL_COMMON__NO_SERVER);

				// 해당 서버에 서비스가 이미 등록되어 있을 경우
				if (await m_dbContext.Services.AsNoTracking().Where(i => i.ServerId == Server.Id && i.ServiceType == (EnumDbServiceType)Request.ServiceType).AnyAsync())
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVERS_SERVICE_ALREADY_REGISTERED);

				// 모든 VLAN 아이디들에 대해서 처리
				var VlanGuids = new List<Guid>();

				foreach (var VlanId in Request.VlanIds)
				{
					// VLAN 아이디가 유효하지 않은 경우
					if (VlanId.IsEmpty() || !Guid.TryParse(VlanId, out Guid VlanGuid))
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_VLAN_ID);

					// 해당 VLAN 정보를 가져온다.
					var Vlan = await m_dbContext.NetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.Id == VlanGuid)
						.Include(i => i.NetworkInterface)
						.FirstOrDefaultAsync();

					// 해당 VLAN 정보가 존재하지 않는 경우 또는 동일서버에 존재하지 않는 경우
					if (Vlan == null || Vlan.NetworkInterface.ServerId != Server.Id)
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_VLAN);

					VlanGuids.Add(VlanGuid);
				}

				using var Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					// 정보를 생성한다.
					var NewData = new Service()
					{
						Id = Id,
						GroupId = GroupGuid == Guid.Empty ? null : GroupGuid,
						ServerId = Server.Id,
						Name = Request.Name,
						Description = Request.Description,
						ServiceType = (EnumDbServiceType)Request.ServiceType,
						HaAction = (EnumDbHaAction)Request.HaAction,
						State = (EnumDbServiceState)Request.State,
						MemoryTotal = Server.MemoryTotal,
						ModId = ModId ?? LoginUserId,
						ModName = ModName ?? LoginUserName,
					};
					await m_dbContext.Services.AddAsync(NewData);
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					// 모든 VLAN 아이디들에 대해서 처리
					foreach (var VlanGuid in VlanGuids)
					{
						// 해당 VLAN 정보를 생성한다.
						var NewVlan = new ServiceNetworkInterfaceVlan()
						{
							ServiceId = NewData.Id,
							VlanId = VlanGuid
						};
						await this.m_dbContext.ServiceNetworkInterfaceVlans.AddAsync(NewVlan);
					}
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					await Transaction.CommitAsync();

					Result.Result = EnumResponseResult.Success;
					Result.Data = (await this.Get(NewData.Id.ToString())).Data;

					// 서비스 추가 메시지 전송
					SendMq("*.services.added", new ResponseServiceMq().CopyValueFrom(NewData));
				}
				catch (Exception ex)
				{
					await Transaction.RollbackAsync();

					NNException.Log(ex);

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__EXCEPTION;
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

		/// <summary>서비스 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="Request">서비스 수정 요청 객체</param>
		/// <returns>서비스 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string Id, RequestService Request)
		{
			var Result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);


				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());


				// 그룹 아이디가 존재하고 유효한 Guid가 아닌 경우
				Guid GroupGuid = Guid.Empty;
				if (!Request.GroupId.IsEmpty() && !Guid.TryParse(Request.GroupId, out GroupGuid))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_GROUP_ID);

				// 그룹 아이디가 유효한 경우
				if (GroupGuid != Guid.Empty)
				{
					// 해당 서비스 그룹 정보를 가져온다.
					var ServiceGroup = await this.m_dbContext.ServiceGroups.AsNoTracking()
						.Where(i => i.Id == GroupGuid)
						.FirstOrDefaultAsync();

					// 해당 서비스 그룹이 존재하지 않는 경우
					if (ServiceGroup == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_SERVICE_GROUP);
				}

				// 서버 정보를 가져온다
				Server Server = null;

				// Id로 조회할경우
				if (Guid.TryParse(Request.ServerId, out Guid ServerGuid))
					Server = await m_dbContext.Servers.AsNoTracking().FirstOrDefaultAsync(i => i.Id == ServerGuid);
				// 이름으로 조회할 경우
				else
					Server = await m_dbContext.Servers.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Request.ServerId);

				// 해당 서버가 존재하지 않는 경우
				if (Server == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.UL_COMMON__NO_SERVER);

				// 해당 정보를 가져온다.
				Service Exist = null;

				// 아이디로 조회할 경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 동일한 이름이 존재하는 경우
				if (await this.IsNameExist(Request.Name, Exist.Id))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICES_DUPLICATED_NAME);

				// 모든 VLAN 아이디들에 대해서 처리
				var VlanGuids = new List<Guid>();
				foreach (var VlanId in Request.VlanIds)
				{
					// VLAN 아이디가 유효하지 않은 경우
					if (VlanId.IsEmpty() || !Guid.TryParse(VlanId, out Guid VlanGuid))
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_VLAN_ID);

					// 해당 VLAN 정보를 가져온다.
					var Vlan = await this.m_dbContext.NetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.Id == VlanGuid)
						.Include(i => i.NetworkInterface)
						.FirstOrDefaultAsync();

					// 해당 VLAN 정보가 존재하지 않는 경우 또는 동일서버에 존재하지 않는 경우
					if (Vlan == null || Vlan.NetworkInterface.ServerId != Server.Id)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_VLAN);

					VlanGuids.Add(VlanGuid);
				}

				using var Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					// 정보를 수정한다.
					Exist.GroupId = GroupGuid == Guid.Empty ? null : GroupGuid;
					Exist.ServerId = Server.Id;
					Exist.Name = Request.Name;
					Exist.Description = Request.Description;
					Exist.ServiceType = (EnumDbServiceType)Request.ServiceType;
					Exist.HaAction = (EnumDbHaAction)Request.HaAction;
					Exist.State = (EnumDbServiceState)Request.State;
					Exist.MemoryTotal = Server.MemoryTotal;

					// 데이터가 변경된 경우 저장
					if (m_dbContext.HasChanges())
					{
						Exist.ModId = LoginUserId;
						Exist.ModName = LoginUserName;
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
					}

					// 기존 VLAN 매핑 목록을 가져온다.
					var ExistVlans = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.ServiceId == Exist.Id)
						.ToListAsync();

					// 기존 VLAN 매핑 목록 삭제
					m_dbContext.RemoveRange(ExistVlans);
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					// 모든 VLAN 아이디들에 대해서 처리
					foreach (var VlanGuid in VlanGuids)
					{
						// 해당 VLAN 정보를 생성한다.
						var NewVlan = new ServiceNetworkInterfaceVlan()
						{
							ServiceId = Exist.Id,
							VlanId = VlanGuid
						};
						await this.m_dbContext.ServiceNetworkInterfaceVlans.AddAsync(NewVlan);
					}
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					await Transaction.CommitAsync();

					Result.Result = EnumResponseResult.Success;

					// 서비스 변경 메시지 전송
					SendMq("*.services.updated", new ResponseServiceMq().CopyValueFrom(Exist));
				}
				catch (Exception ex)
				{
					await Transaction.RollbackAsync();

					NNException.Log(ex);

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__EXCEPTION;
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

		/// <summary>서비스 상태 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="State">서비스 상태</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(string Id, EnumServiceState State, Guid? ModId = null, string ModName = null)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using var Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					// 상태가 변경되었는지 확인한다.
					var isChange = Exist.State != (EnumDbServiceState)State;

					// 정보를 수정한다.
					Exist.State = (EnumDbServiceState)State;
					Exist.ModId = ModId ?? LoginUserId;
					Exist.ModName = ModName ?? LoginUserName;
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
					await Transaction.CommitAsync();

					// 상태가 변경되었을 경우 메시지 전송
					if (isChange) SendMq("*.services.updated", new ResponseServiceMq().CopyValueFrom(Exist));
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
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 상태 수정</summary>
		/// <param name="Request">상태 수정 요청 객체</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(RequestServiceState Request, Guid? ModId = null, string ModName = null)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 서비스 상태 수정
				Result = await UpdateState(Request.Id, Request.State, ModId, ModName);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 사용 정보 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="CpuUsage">CPU 사용률</param>
		/// <param name="MemoryUsed">메모리 사용량</param>
		/// <param name="ThreadCount">스레드 수</param>
		/// <returns>서비스 사용 정보 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(string Id, float CpuUsage, decimal MemoryUsed, int ThreadCount)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using var Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					// 정보를 수정한다.
					Exist.CpuUsage = CpuUsage;
					Exist.MemoryUsed = MemoryUsed;
					Exist.ThreadCount = ThreadCount;
					Exist.ModId = LoginUserId;
					Exist.ModName = LoginUserName;

					// 데이터가 변경된 경우 저장
					if (m_dbContext.HasChanges())
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					// 사용 정보 추가
					m_dbContext.ServiceUsages.Add(new ServiceUsage()
					{
						Id = Exist.Id,
						RegDate = DateTime.Now,
						CpuUsage = CpuUsage,
						MemoryTotal = Exist.MemoryTotal,
						MemoryUsed = MemoryUsed,
						ThreadCount = ThreadCount
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
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 사용 정보 수정</summary>
		/// <param name="Request">서비스 사용 정보 수정 요청 객체</param>
		/// <returns>서비스 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(RequestServiceUsage Request)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 서비스 상태 수정
				Result = await UpdateUsage(Request.Id, Request.CpuUsage, Request.MemoryUsed, Request.ThreadCount);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="State">HA 상태</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서비스 HA 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateHaAction(string Id, EnumHaAction State, Guid? ModId = null, string ModName = null)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using var Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					// 정보를 수정한다.
					Exist.HaAction = (EnumDbHaAction)State;
					Exist.ModId = ModId ?? LoginUserId;
					Exist.ModName = ModName ?? LoginUserName;

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
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="Request">HA 상태 수정 요청 객체</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서비스 HA 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateHaAction(RequestServiceHaAction Request, Guid? ModId = null, string ModName = null)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 서비스 상태 수정
				Result = await UpdateHaAction(Request.Id, Request.HaAction, ModId, ModName);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 삭제</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Success);

				using var Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					// VLAN 연결 목록을 가져온다.
					var Vlans = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.ServiceId == Exist.Id)
						.ToListAsync();
					// 서비스 연결 목록 삭제
					m_dbContext.ServiceNetworkInterfaceVlans.RemoveRange(Vlans);
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					// 서비스 사용 정보 목록을 가져온다.
					var Usages = await m_dbContext.ServiceUsages.AsNoTracking()
						.Where(i => i.Id == Exist.Id)
						.ToListAsync();
					// 서비스 사용 정보 목록 삭제
					m_dbContext.ServiceUsages.RemoveRange(Usages);
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					// 해당 데이터 삭제
					m_dbContext.Services.Remove(Exist);
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					await Transaction.CommitAsync();

					Result.Result = EnumResponseResult.Success;

					// 서비스 삭제 메시지 전송
					SendMq("*.services.removed", new ResponseServiceMq().CopyValueFrom(Exist));
				}
				catch (Exception ex)
				{
					await Transaction.RollbackAsync();

					NNException.Log(ex);

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__EXCEPTION;
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

		/// <summary>서비스 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="SearchState">검색 서비스 상태 (옵션)</param>
		/// <param name="SearchType">검색 서비스 타입 (옵션)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, ServiceType)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (GroupName, Name, Description, ServiceType, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseServiceWithGroup>> GetList(
			EnumServiceState? SearchState = null, EnumServiceType? SearchType = null,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = null
		)
		{
			var Result = new ResponseList<ResponseServiceWithGroup>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);


				// 목록을 가져온다.
				Result.Data = await m_dbContext.Services.AsNoTracking()
					.Where(i =>
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchType != null && i.ServiceType == (EnumDbServiceType)SearchType)
							|| (SearchState != null && i.State == (EnumDbServiceState)SearchState)
							|| (SearchFields.Contains("GroupName") && i.ServiceGroup != null && i.ServiceGroup.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("Name") && i.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("Description") && i.Description.Contains(SearchKeyword))
							|| (SearchFields.Contains("IpAddress") && i.Vlans.Any(j => j.NetworkInterfaceVlan != null && j.NetworkInterfaceVlan.IpAddress.Contains(SearchKeyword)))
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.Include(i => i.ServiceGroup)
					.Include(i => i.Server)
					.CreateListAsync<Service, ResponseServiceWithGroup>(Skip, CountPerPage);

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

		/// <summary>서비스 정보를 가져온다.</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 정보 객체</returns>
		public async Task<ResponseData<ResponseServiceWithVlans>> Get(string Id)
		{
			var Result = new ResponseData<ResponseServiceWithVlans>();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				ResponseServiceWithVlans Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.AsNoTracking()
					.Where(i => i.Id == ServiceGuid)
					.Include(i => i.ServiceGroup)
					.Include(i => i.Vlans)
					.Include(i => i.Server)
					.FirstOrDefaultAsync<Service, ResponseServiceWithVlans>();
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking()
					.Where(i => i.Name == Id)
					.Include(i => i.ServiceGroup)
					.Include(i => i.Vlans)
					.Include(i => i.Server)
					.FirstOrDefaultAsync<Service, ResponseServiceWithVlans>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

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
		/// <param name="ExceptId">이름 검색 시 제외할 서비스 아이디</param>
		/// <param name="Name">검색할 이름</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsNameExist(string ExceptId, string Name)
		{
			ResponseData<bool> Result = new ResponseData<bool>();

			try
			{
				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				var GuidId = Guid.Empty;
				if (!ExceptId.IsEmpty() && !Guid.TryParse(ExceptId, out GuidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청 객체가 유효하지 않은 경우
				if (Name.IsEmpty())
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISK_POOLS_REQUIRE_NAME);

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
		/// <param name="Name">검색할 이름</param>
		/// <param name="ExceptId">이름 검색 시 제외할 서비스 아이디</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<bool> IsNameExist(string Name, Guid? ExceptId = null)
		{
			try
			{
				return await m_dbContext.Services.AsNoTracking().AnyAsync(i => (ExceptId == null || i.Id != ExceptId) && i.Name.Equals(Name));
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}

			return false;
		}

		/// <summary>서비스 시작</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 시작 결과 객체</returns>
		public async Task<ResponseData> Start(string Id)
		{
			var Result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 서비스 시작 요청
				ResponseData Response = SendRpcMq($"*.services.{Exist.ServerId}.control",
					new RequestServiceControl(Id, (EnumServiceType)Exist.ServiceType, EnumServiceControl.Start), 10);

				// 실패인 경우
				if (Response.Result != EnumResponseResult.Success)
					return new ResponseData(EnumResponseResult.Error, Response.Code, Response.Message);

				// 상태 수정
				Result = await this.UpdateState(Id, EnumServiceState.Online);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 중지</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 중지 결과 객체</returns>
		public async Task<ResponseData> Stop(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 서비스 중지 요청
				var Response = SendRpcMq($"*.services.{Exist.ServerId}.control",
					new RequestServiceControl(Id, (EnumServiceType)Exist.ServiceType, EnumServiceControl.Stop), 10);

				// 실패인 경우
				if (Response.Result != EnumResponseResult.Success)
					return new ResponseData(EnumResponseResult.Error, Response.Code, Response.Message);

				// 상태 수정
				Result = await this.UpdateState(Id, EnumServiceState.Offline);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 재시작</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 재시작 결과 객체</returns>
		public async Task<ResponseData> Restart(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 서비스 재시작 요청
				var Response = SendRpcMq($"*.services.{Exist.ServerId}.control",
					new RequestServiceControl(Id, (EnumServiceType)Exist.ServiceType, EnumServiceControl.Restart), 10);

				// 실패인 경우
				if (Response.Result != EnumResponseResult.Success)
					return new ResponseData(EnumResponseResult.Error, Response.Code, Response.Message);

				// 상태 수정
				Result = await this.UpdateState(Id, EnumServiceState.Online);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 이벤트를 추가한다</summary>
		/// <param name="Request">서비스 이벤트 요청 객체</param>
		/// <returns>서버 이벤트 추가 결과 객체</returns>
		public async Task<ResponseData> AddEvent(RequestServiceEvent Request)
		{
			var Result = new ResponseData();

			try
			{
				m_logger.LogInformation($"{Request.Id}, {Request.EventType}, {Request.Message}");
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Service Exist = null;

				// Id로 조회할경우
				if (Guid.TryParse(Request.Id, out Guid ServiceGuid))
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Name == Request.Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 새로운 이벤트를 저장한다.
				using var Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					var NewData = new ServiceEventLog()
					{
						Id = Exist.Id,
						RegDate = DateTime.Now,
						EventType = (EnumDbServiceEventType)Request.EventType,
						Message = Request.Message,
					};
					await m_dbContext.ServiceEventLogs.AddAsync(NewData);

					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
					await Transaction.CommitAsync();

					Result.Result = EnumResponseResult.Success;

					// 종료 이벤트가 발생할 경우 해당 서비스의 상태를 Offline으로 변경한다.
					if (Request.EventType == EnumServiceEventType.Stop) await UpdateState(Request.Id, EnumServiceState.Offline);
				}
				catch (Exception ex)
				{
					await Transaction.RollbackAsync();

					NNException.Log(ex);

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__EXCEPTION;
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


		/// <summary>서비스 이벤트 목록을 가져온다.</summary>
		/// <param name="Id"> 서비스 아이디 / 이름</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (기본 RegDate, ServiceEventType)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (ServiceEventType)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseServiceEvent>> GetEventList(
			string Id, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = null
		)
		{
			var Result = new ResponseList<ResponseServiceEvent>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("RegDate", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				var ServiceEventType = EnumServiceEventType.Error;
				if (SearchFields.Contains("ServiceEventType", StringComparer.OrdinalIgnoreCase))
					Enum.TryParse(SearchKeyword, out ServiceEventType);

				// 서비스 정보를 가져온다
				Service Service = null;

				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid ServiceGuid))
					Service = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == ServiceGuid);
				// 이름으로 조회할 경우
				else
					Service = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 서비스가 존재하지 않는 경우
				if (Service == null)
					return new ResponseList<ResponseServiceEvent>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.UL_COMMON__NO_SERVER);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.ServiceEventLogs.AsNoTracking()
					.Where(i =>
						(
							i.Id == Service.Id && (SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("ServiceEventType") && i.EventType == (EnumDbServiceEventType)ServiceEventType))
						)
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<ServiceEventLog, ResponseServiceEvent>(Skip, CountPerPage);

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

		
		/// <summary> 해당 Id가 존재하는지 여부 </summary>
		/// <param name="Id">검색할 Id</param>
		/// <returns>해당 Id가 존재하는지 여부</returns>
		public async Task<bool> IsExistId(Guid Id)
		{
			try
			{
				// 동일한 Id가 존재하는지 확인한다.
				return await m_dbContext.Services.AsNoTracking().AnyAsync(i => i.Id == Id);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}

			return false;
		}
	}
}