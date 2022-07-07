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
using System.Text;
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

namespace PortalProvider.Providers.Services
{
	/// <summary>서비스 데이터 프로바이더 클래스</summary>
	public class ServiceGroupProvider : BaseProvider<PortalModel>, IServiceGroupProvider
	{
		/// <summary>서비스 데이터 프로바이더</summary>
		protected readonly IServiceProvider m_serviceProvider;

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		/// <param name="serviceProvider">서비스 데이터 프로바이더</param>
		public ServiceGroupProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<ServiceGroupProvider> logger,
			IServiceProvider serviceProvider
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
			m_serviceProvider = serviceProvider;
		}

		/// <summary>서비스 등록</summary>
		/// <param name="Request">서비스 등록 요청 객체</param>
		/// <returns>서비스 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseServiceGroupWithServices>> Add(RequestServiceGroup Request)
		{
			var Result = new ResponseData<ResponseServiceGroupWithServices>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재하는지 확인한다.
				var ResponseExist = await this.IsNameExist(null, new RequestIsServiceGroupNameExist(Request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (ResponseExist.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseServiceGroupWithServices>(ResponseExist.Result, ResponseExist.Code, ResponseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (ResponseExist.Data)
					return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_DUPLICATED_NAME);

				// 서비스 GUID 목록
				var ServiceIds = new List<Guid>();

				// 모든 서비스 아이디에 대해서 처리
				if (Request.ServiceIds != null && Request.ServiceIds.Count > 0)
				{
					// 모든 서비스 아이디에 대해서 처리
					foreach (var ServiceId in Request.ServiceIds)
					{
						if (!Guid.TryParse(ServiceId, out Guid GuidServiceId))
							return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_INVALID_SERVICE_ID);
						ServiceIds.Add(GuidServiceId);
					}

					// 주어진 서비스 아이디로 다른 그룹에 속하지 않는 서비스 아이디 수가 요청 개수와 다른 경우
					if (await this.m_dbContext.Services.AsNoTracking()
						.Where(i => ServiceIds.Contains(i.Id) && i.GroupId == null && i.ServiceType == (EnumDbServiceType)Request.ServiceType)
						.CountAsync() != ServiceIds.Count)
						return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_NOT_AVAILABLE_SERVICE_ID_USED);
				}

				using (var transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						var NewData = new ServiceGroup()
						{
							Id = Guid.NewGuid(),
							Name = Request.Name,
							Description = Request.Description,
							ServiceType = (EnumDbServiceType)Request.ServiceType,
							ServiceIpAddress = Request.ServiceIpAddress,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.ServiceGroups.AddAsync(NewData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 모든 서비스 아이디에 대해서 처리
						foreach (var GuidServiceId in ServiceIds)
						{
							// 해당 서비스 정보를 가져온다.
							var Service = await m_dbContext.Services
								.Where(i => i.Id == GuidServiceId)
								.FirstOrDefaultAsync();

							// 그룹 아이디 변경
							Service.GroupId = NewData.Id;
						}
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Data = (await this.Get(NewData.Id.ToString())).Data;
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

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
		public async Task<ResponseData> Update(string Id, RequestServiceGroup Request)
		{
			var Result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재하는지 확인한다.
				var ResponseExist = await this.IsNameExist(Id, new RequestIsServiceGroupNameExist(Request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (ResponseExist.Result != EnumResponseResult.Success)
					return new ResponseData(ResponseExist.Result, ResponseExist.Code, ResponseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (ResponseExist.Data)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_DUPLICATED_NAME);

				// 서비스 GUID 목록
				var ServiceIds = new List<Guid>();

				// 모든 서비스 아이디에 대해서 처리
				if (Request.ServiceIds != null && Request.ServiceIds.Count > 0)
				{
					// 모든 서비스 아이디에 대해서 처리
					foreach (var serviceId in Request.ServiceIds)
					{
						if (!Guid.TryParse(serviceId, out Guid GuidServiceId))
							return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_INVALID_SERVICE_ID);
						ServiceIds.Add(GuidServiceId);
					}

					// 주어진 서비스 아이디로 다른 그룹에 속하지 않는 서비스 아이디 수가 요청 개수와 다른 경우
					if (await this.m_dbContext.Services.AsNoTracking()
						.Where(i => ServiceIds.Contains(i.Id) && (i.GroupId == null || i.GroupId == guidId) && i.ServiceType == (EnumDbServiceType)Request.ServiceType)
						.CountAsync() != ServiceIds.Count)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_NOT_AVAILABLE_SERVICE_ID_USED);
				}

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.ServiceGroups
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 이 서비스 그룹 아이디를 사용하는 이전 서비스의 그룹 아이디 초기화
						List<Service> oldServices = await m_dbContext.Services
							.Where(i => i.GroupId == guidId)
							.ToListAsync();
						if (oldServices.Count > 0)
						{
							// 모든 서비스 아이디에 대해서 처리
							foreach (var oldService in oldServices)
								// 그룹 아이디 변경
								oldService.GroupId = null;
							// 데이터가 변경된 경우 저장
							if (m_dbContext.HasChanges())
								await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}

						// 모든 서비스 아이디에 대해서 처리
						foreach (var GuidServiceId in ServiceIds)
						{
							// 해당 서비스 정보를 가져온다.
							var service = await m_dbContext.Services
								.Where(i => i.Id == GuidServiceId)
								.FirstOrDefaultAsync();

							// 그룹 아이디 변경
							service.GroupId = guidId;
						}
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 정보를 수정한다.
						Exist.Name = Request.Name;
						Exist.Description = Request.Description;
						Exist.ServiceType = (EnumDbServiceType)Request.ServiceType;
						Exist.ServiceIpAddress = Request.ServiceIpAddress;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							Exist.ModId = LoginUserId;
							Exist.ModName = LoginUserName;
							Exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}
						await transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

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

		/// <summary>서비스 삭제</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(Result.Result = EnumResponseResult.Success);

				// 해당 그룹에 속한 서비스 목록을 가져온다.
				var Services = await this.m_dbContext.Services
					.Where(i => i.GroupId == guidId)
					.ToListAsync();

				using (var transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 모든 서비스에 대해서 처리
						foreach (var Service in Services)
							Service.GroupId = null;
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 해당 데이터 삭제
						m_dbContext.ServiceGroups.Remove(Exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

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
		/// <param name="OrderFields">정렬필드목록 (Name, Description, ServiceType, ServiceIpAddress)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, ServiceType, ServiceIpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseServiceGroup>> GetList(
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Result = new ResponseList<ResponseServiceGroup>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				EnumServiceType ServiceType = EnumServiceType.Unknown;
				if (SearchFields.Contains("servicetype"))
					Enum.TryParse(SearchKeyword, out ServiceType);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i =>
						(
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("name") && i.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("description") && i.Description.Contains(SearchKeyword))
							|| (SearchFields.Contains("servicetype") && i.ServiceType == (EnumDbServiceType)ServiceType)
							|| (SearchFields.Contains("serviceipaddress") && i.ServiceIpAddress.Contains(SearchKeyword))
						)
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<ServiceGroup, ResponseServiceGroup>(Skip, CountPerPage);

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
		public async Task<ResponseData<ResponseServiceGroupWithServices>> Get(string Id)
		{
			var Result = new ResponseData<ResponseServiceGroupWithServices>();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				var Exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

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
		/// <param name="Request">특정 이름의 서비스 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsNameExist(string ExceptId, RequestIsServiceGroupNameExist Request)
		{
			var Result = new ResponseData<bool>();
			var guidId = Guid.Empty;

			try
			{
				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				if (!ExceptId.IsEmpty() && !Guid.TryParse(ExceptId, out guidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청 객체가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<bool>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재하는 경우
				if (await m_dbContext.ServiceGroups.AsNoTracking().AnyAsync(i => (ExceptId.IsEmpty() || i.Id != guidId) && i.Name == Request.Name))
					Result.Data = true;
				// 동일한 이름이 존재하지 않는 경우
				else
					Result.Data = false;
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

		/// <summary>해당 서비스 타입으로 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>참여가 가능한 서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseService>> GetAvailableServices(
			EnumServiceType ServiceType
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "")
		{
			var Result = new ResponseList<ResponseService>();

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
						i.ServiceType == (EnumDbServiceType)ServiceType
						&& i.GroupId == null
						&& (
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("name") && i.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("description") && i.Description.Contains(SearchKeyword))
							|| (SearchFields.Contains("ipaddress") && i.Vlans.Any(j => j.NetworkInterfaceVlan != null && j.NetworkInterfaceVlan.IpAddress.Contains(SearchKeyword)))
						)
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<Service, ResponseService>(Skip, CountPerPage);

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

		/// <summary>주어진 서비스 그룹 아이디에 해당하는 서비스 그룹에 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="Id">서비스 그룹 아이디 (null인 경우, 어느 그룹에도 속하지 않은 서비스만 검색한다.)</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>참여가 가능한 서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseService>> GetAvailableServices(
			string Id
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "")
		{
			var Result = new ResponseList<ResponseService>();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseList<ResponseService>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				ResponseServiceGroup Exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroup>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseList<ResponseService>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

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
						i.ServiceType == (EnumDbServiceType)Exist.ServiceType
						&& (i.GroupId == null || (!Id.IsEmpty() && i.GroupId == guidId))
						&& (
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("name") && i.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("description") && i.Description.Contains(SearchKeyword))
							|| (SearchFields.Contains("ipaddress") && i.Vlans.Any(j => j.NetworkInterfaceVlan != null && j.NetworkInterfaceVlan.IpAddress.Contains(SearchKeyword)))
						)
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<Service, ResponseService>(Skip, CountPerPage);

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

		/// <summary>서비스 시작</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 시작 결과 객체</returns>
		public async Task<ResponseData> Start(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				var Exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				var ErrorCodes = new StringBuilder();
				var ErrorMessages = new StringBuilder();

				// 모든 서비스에 대해서 처리
				foreach (var Service in Exist.Services)
				{
					// 서비스를 시작한다.
					var Response = await m_serviceProvider.Start(Service.Id);

					// 성공이 아닌 경우
					if (Response.Result != EnumResponseResult.Success)
					{
						ErrorCodes.AppendLine(Response.Code);
						ErrorMessages.AppendLine(Response.Message);
					}
				}

				// 에러 코드가 존재하지 않는 경우
				if (ErrorCodes.Length == 0)
					Result.Result = EnumResponseResult.Success;
				// 에러 코드가 존재하는 경우, 경고로 처리하고 해당 코드와 메세지 저장
				else
				{
					Result.Result = EnumResponseResult.Warning;
					Result.Code = ErrorCodes.ToString();
					Result.Message = ErrorMessages.ToString();
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

		/// <summary>서비스 중지</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 그룹 중지 결과 객체</returns>
		public async Task<ResponseData> Stop(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				var Exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				var ErrorCodes = new StringBuilder();
				var ErrorMessages = new StringBuilder();

				// 모든 서비스에 대해서 처리
				foreach (ResponseService service in Exist.Services)
				{
					// 서비스를 중지한다.
					ResponseData Response = await m_serviceProvider.Stop(service.Id);

					// 성공이 아닌 경우
					if (Response.Result != EnumResponseResult.Success)
					{
						ErrorCodes.AppendLine(Response.Code);
						ErrorMessages.AppendLine(Response.Message);
					}
				}

				// 에러 코드가 존재하지 않는 경우
				if (ErrorCodes.Length == 0)
					Result.Result = EnumResponseResult.Success;
				// 에러 코드가 존재하는 경우, 경고로 처리하고 해당 코드와 메세지 저장
				else
				{
					Result.Result = EnumResponseResult.Warning;
					Result.Code = ErrorCodes.ToString();
					Result.Message = ErrorMessages.ToString();
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

		/// <summary>서비스 재시작</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>서비스 재시작 결과 객체</returns>
		public async Task<ResponseData> Restart(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				ResponseServiceGroupWithServices Exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				StringBuilder ErrorCodes = new StringBuilder();
				StringBuilder errorMessages = new StringBuilder();

				// 모든 서비스에 대해서 처리
				foreach (ResponseService service in Exist.Services)
				{
					// 서비스를 재시작한다.
					ResponseData Response = await m_serviceProvider.Restart(service.Id);

					// 성공이 아닌 경우
					if (Response.Result != EnumResponseResult.Success)
					{
						ErrorCodes.AppendLine(Response.Code);
						errorMessages.AppendLine(Response.Message);
					}
				}

				// 에러 코드가 존재하지 않는 경우
				if (ErrorCodes.Length == 0)
					Result.Result = EnumResponseResult.Success;
				// 에러 코드가 존재하는 경우, 경고로 처리하고 해당 코드와 메세지 저장
				else
				{
					Result.Result = EnumResponseResult.Warning;
					Result.Code = ErrorCodes.ToString();
					Result.Message = errorMessages.ToString();
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