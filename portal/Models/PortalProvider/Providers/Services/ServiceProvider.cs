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
using PortalData.Responses.Networks;
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
		public async Task<ResponseData<ResponseServiceWithVlans>> Add(RequestService Request)
		{
			var Result = new ResponseData<ResponseServiceWithVlans>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재하는 경우
				if (await this.IsNameExist(Request.Name))
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICES_DUPLICATED_NAME);

				// 그룹 아이디가 존재하고 유효한 Guid가 아닌 경우
				Guid GuidGroupId = Guid.Empty;
				if (!Request.GroupId.IsEmpty() && !Guid.TryParse(Request.GroupId, out GuidGroupId))
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_GROUP_ID);

				// 그룹 아이디가 유효한 경우
				if (GuidGroupId != Guid.Empty)
				{
					// 해당 서비스 그룹 정보를 가져온다.
					var ServiceGroup = await this.m_dbContext.ServiceGroups.AsNoTracking()
						.Where(i => i.Id == GuidGroupId)
						.FirstOrDefaultAsync();

					// 해당 서비스 그룹이 존재하지 않는 경우
					if (ServiceGroup == null)
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_SERVICE_GROUP);
				}

				decimal MemoryTotal = 0;
				var Servers = new List<Server>();

				// 모든 VLAN 아이디들에 대해서 처리
				var GuidVlanIds = new List<Guid>();
				foreach (var VlanId in Request.VlanIds)
				{
					// VLAN 아이디가 유효하지 않은 경우
					if (VlanId.IsEmpty() || !Guid.TryParse(VlanId, out Guid GuidVlanId))
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_VLAN_ID);

					// 해당 VLAN 정보를 가져온다.
					var Vlan = await this.m_dbContext.NetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.Id == GuidVlanId)
						.Include(i => i.NetworkInterface)
						.ThenInclude(i => i.Server)
						.FirstOrDefaultAsync();

					// 해당 VLAN 정보가 존재하지 않는 경우
					if (Vlan == null)
						return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_VLAN);

					GuidVlanIds.Add(GuidVlanId);

					// 이전에 처리한 서버가 아닌 경우
					if (Servers.Count(i => i.Id == Vlan.NetworkInterface.Server.Id) == 0)
					{
						Servers.Add(Vlan.NetworkInterface.Server);

						// 전체 메모리 크기 저장
						MemoryTotal = Vlan.NetworkInterface.Server.MemoryTotal ?? 0;
					}
				}

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						var NewData = new Service()
						{
							Id = Guid.NewGuid(),
							GroupId = GuidGroupId == Guid.Empty ? null : GuidGroupId,
							Name = Request.Name,
							Description = Request.Description,
							ServiceType = (EnumDbServiceType)Request.ServiceType,
							HaAction = (EnumDbHaAction)Request.HaAction,
							State = (EnumDbServiceState)Request.State,
							MemoryTotal = MemoryTotal,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.Services.AddAsync(NewData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 모든 VLAN 아이디들에 대해서 처리
						foreach (var GuidVlanId in GuidVlanIds)
						{
							// 해당 VLAN 정보를 생성한다.
							var NewVlan = new ServiceNetworkInterfaceVlan()
							{
								ServiceId = NewData.Id,
								VlanId = GuidVlanId
							};
							await this.m_dbContext.ServiceNetworkInterfaceVlans.AddAsync(NewVlan);
						}
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Data = (await this.Get(NewData.Id.ToString())).Data;

						// 서비스 추가 메시지 전송
						SendMq("*.services.added", new ResponseSerivceMq().CopyValueFrom(NewData));
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
				Guid GuidGroupId = Guid.Empty;
				if (!Request.GroupId.IsEmpty() && !Guid.TryParse(Request.GroupId, out GuidGroupId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_GROUP_ID);

				// 그룹 아이디가 유효한 경우
				if (GuidGroupId != Guid.Empty)
				{
					// 해당 서비스 그룹 정보를 가져온다.
					var ServiceGroup = await this.m_dbContext.ServiceGroups.AsNoTracking()
						.Where(i => i.Id == GuidGroupId)
						.FirstOrDefaultAsync();

					// 해당 서비스 그룹이 존재하지 않는 경우
					if (ServiceGroup == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_SERVICE_GROUP);
				}

				Service Exist = null;

				// 아이디로 조회할 경우
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Id == GuidId);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 동일한 이름이 존재하는 경우
				if (await this.IsNameExist(Request.Name, Exist.Id))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICES_DUPLICATED_NAME);

				decimal MemoryTotal = 0;
				var Servers = new List<Server>();

				// 모든 VLAN 아이디들에 대해서 처리
				var GuidVlanIds = new List<Guid>();
				foreach (var VlanId in Request.VlanIds)
				{
					// VLAN 아이디가 유효하지 않은 경우
					if (VlanId.IsEmpty() || !Guid.TryParse(VlanId, out Guid guidVlanId))
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_INVALID_VLAN_ID);

					// 해당 VLAN 정보를 가져온다.
					var Vlan = await this.m_dbContext.NetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.Id == guidVlanId)
						.Include(i => i.NetworkInterface)
						.ThenInclude(i => i.Server)
						.FirstOrDefaultAsync();

					// 해당 VLAN 정보가 존재하지 않는 경우
					if (Vlan == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_SERVICES_THERE_IS_NO_VLAN);

					GuidVlanIds.Add(guidVlanId);

					// 이전에 처리한 서버가 아닌 경우
					if (Servers.Count(i => i.Id == Vlan.NetworkInterface.Server.Id) == 0)
					{
						Servers.Add(Vlan.NetworkInterface.Server);

						// 전체 메모리 크기 저장
						MemoryTotal = Vlan.NetworkInterface.Server.MemoryTotal ?? 0;
					}
				}

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.GroupId = GuidGroupId == Guid.Empty ? null : GuidGroupId;
						Exist.Name = Request.Name;
						Exist.Description = Request.Description;
						Exist.ServiceType = (EnumDbServiceType)Request.ServiceType;
						Exist.HaAction = (EnumDbHaAction)Request.HaAction;
						Exist.State = (EnumDbServiceState)Request.State;
						Exist.MemoryTotal = MemoryTotal;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							Exist.ModId = LoginUserId;
							Exist.ModName = LoginUserName;
							Exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}

						// 기존 VLAN 목록을 가져온다.
						var ExistVlans = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
							.Where(i => i.ServiceId == Exist.Id)
							.ToListAsync();
						m_dbContext.RemoveRange(ExistVlans);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 모든 VLAN 아이디들에 대해서 처리
						foreach (var GuidVlanId in GuidVlanIds)
						{
							// 해당 VLAN 정보를 생성한다.
							var NewVlan = new ServiceNetworkInterfaceVlan()
							{
								ServiceId = Exist.Id,
								VlanId = GuidVlanId
							};
							await this.m_dbContext.ServiceNetworkInterfaceVlans.AddAsync(NewVlan);
						}
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 서비스 변경 메시지 전송
						SendMq("*.services.updated", new ResponseSerivceMq().CopyValueFrom(Exist));
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

		/// <summary>서비스 상태 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="State">서비스 상태</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(string Id, EnumServiceState State, string ModId = "", string ModName = "")
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
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Id == GuidId);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 파라미터로 넘어온 수정자 아이디 파싱
				var GuidModId = Guid.Empty;
				if (!ModId.IsEmpty())
					Guid.TryParse(ModId, out GuidModId);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.State = (EnumDbServiceState)State;
						if (LoginUserId != Guid.Empty || GuidModId != Guid.Empty)
							Exist.ModId = LoginUserId != Guid.Empty ? LoginUserId : GuidModId;
						if (!LoginUserName.IsEmpty() || !ModName.IsEmpty())
							Exist.ModName = !LoginUserName.IsEmpty() ? LoginUserName : ModName;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 서비스 변경 메시지 전송
						SendMq("*.services.updated", new ResponseSerivceMq().CopyValueFrom(Exist));
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

		/// <summary>서비스 상태 수정</summary>
		/// <param name="Request">상태 수정 요청 객체</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(RequestServiceState Request, string ModId = "", string ModName = "")
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
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Id == GuidId);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.CpuUsage = CpuUsage;
						Exist.MemoryUsed = MemoryUsed;
						Exist.ThreadCount = ThreadCount;
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
		public async Task<ResponseData> UpdateHaAction(string Id, EnumHaAction State, string ModId = "", string ModName = "")
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
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Id == GuidId);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 파라미터로 넘어온 수정자 아이디 파싱
				Guid GuidModId = Guid.Empty;
				if (!ModId.IsEmpty())
					Guid.TryParse(ModId, out GuidModId);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.HaAction = (EnumDbHaAction)State;
						if (LoginUserId != Guid.Empty || GuidModId != Guid.Empty)
							Exist.ModId = LoginUserId != Guid.Empty ? LoginUserId : GuidModId;
						if (!LoginUserName.IsEmpty() || !ModName.IsEmpty())
							Exist.ModName = !LoginUserName.IsEmpty() ? LoginUserName : ModName;
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

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="Request">HA 상태 수정 요청 객체</param>
		/// <param name="ModId">수정자 아이디</param>
		/// <param name="ModName">수정자명</param>
		/// <returns>서비스 HA 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateHaAction(RequestServiceHaAction Request, string ModId = "", string ModName = "")
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
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == GuidId);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(Result.Result = EnumResponseResult.Success);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
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
						SendMq("*.services.removed", new ResponseSerivceMq().CopyValueFrom(Exist));
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

		/// <summary>서비스 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, ServiceType)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (GroupName, Name, Description, ServiceType, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseServiceWithGroup>> GetList(
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
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

				var serviceType = EnumServiceType.Unknown;
				if (SearchFields.Contains("servicetype"))
					Enum.TryParse(SearchKeyword, out serviceType);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.Services.AsNoTracking()
					.Where(i =>
						(
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("groupname") && i.ServiceGroup != null && i.ServiceGroup.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("name") && i.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("description") && i.Description.Contains(SearchKeyword))
							|| (SearchFields.Contains("servicetype") && i.ServiceType == (EnumDbServiceType)serviceType)
							|| (SearchFields.Contains("ipaddress") && i.Vlans.Any(j => j.NetworkInterfaceVlan != null && j.NetworkInterfaceVlan.IpAddress.Contains(SearchKeyword)))
						)
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.Include(i => i.ServiceGroup)
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

				ResponseServiceWithVlans Exist = null;

				// 해당 정보를 가져온다.
				// Id로 조회할경우
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Services.AsNoTracking()
					.Where(i => i.Id == GuidId)
					.Include(i => i.ServiceGroup)
					.Include(i => i.Vlans)
					.ThenInclude(i => i.NetworkInterfaceVlan)
					.FirstOrDefaultAsync<Service, ResponseServiceWithVlans>();
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking()
					.Where(i => i.Name == Id)
					.Include(i => i.ServiceGroup)
					.Include(i => i.Vlans)
					.ThenInclude(i => i.NetworkInterfaceVlan)
					.FirstOrDefaultAsync<Service, ResponseServiceWithVlans>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseServiceWithVlans>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 서비스 아이디가 유효한 경우
				if (Guid.TryParse(Exist.Id, out Guid guidServiceId))
				{
					// 해당 서버스의 VLAN 정보를 가져온다.
					Exist.Vlans = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
						.Where(i => i.ServiceId == guidServiceId)
						.Select(i => i.NetworkInterfaceVlan)
						.ToListAsync<NetworkInterfaceVlan, ResponseNetworkInterfaceVlan>();

					// VLAN 아이디가 유효한 경우
					foreach (var Vlan in Exist.Vlans)
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
				if (Guid.TryParse(Exist.GroupId, out Guid GuidGroupId))
				{
					// 해당 서버스의 서비스 그룹 정보를 가져온다.
					Exist.ServiceGroup = await m_dbContext.ServiceGroups.AsNoTracking()
						.Where(i => i.Id == GuidGroupId)
						.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroup>();
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
				return await m_dbContext.Services.AsNoTracking().AnyAsync(i => (ExceptId == null || i.Id != ExceptId) && i.Name == Name);
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
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == GuidId);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 서비스와 연결된 서버 아이디를 가져온다.
				var Server = Get(Id);

				// 서비스와 연결된 서버가 존재하지 않을 경우 에러 반환
				if (Server.Result.Data.Vlans.Count == 0)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.UL_COMMON__NO_SERVER);

				//서비스 아이디
				var ServerId = Server.Result.Data.Vlans[0].ServerId;

				// 서비스 시작 요청
				ResponseData Response = SendRpcMq($"*.services.{ServerId}.control",
					new RequestServiceControl(Id, Server.Result.Data.ServiceType, EnumServiceControl.Start), 10);

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
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == GuidId);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 서비스와 연결된 서버 아이디를 가져온다.
				var Server = Get(Id);

				// 서비스와 연결된 서버가 존재하지 않을 경우 에러 반환
				if (Server.Result.Data.Vlans.Count == 0)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.UL_COMMON__NO_SERVER);

				//서비스 아이디
				var ServerId = Server.Result.Data.Vlans[0].ServerId;

				// 서비스 중지 요청
				var Response = SendRpcMq($"*.services.{ServerId}.control",
					new RequestServiceControl(Id, Server.Result.Data.ServiceType, EnumServiceControl.Stop), 10);

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
				if (Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Id == GuidId);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Services.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 서비스와 연결된 서버 아이디를 가져온다.
				var Server = Get(Id);

				// 서비스와 연결된 서버가 존재하지 않을 경우 에러 반환
				if (Server.Result.Data.Vlans.Count == 0)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.UL_COMMON__NO_SERVER);

				//서비스 아이디
				var ServerId = Server.Result.Data.Vlans[0].ServerId;

				// 서비스 재시작 요청
				ResponseData Response = SendRpcMq($"*.services.{ServerId}.control",
					new RequestServiceControl(Id, Server.Result.Data.ServiceType, EnumServiceControl.Restart), 10);

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
	}
}