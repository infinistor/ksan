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
using PortalData.Requests.Services.Configs;
using PortalData.Responses.Services;
using PortalData.Responses.Services.Configs;
using PortalModels;
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
		/// <param name="request">서비스 등록 요청 객체</param>
		/// <returns>서비스 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseServiceGroupWithServices>> Add(RequestServiceGroup request)
		{
			ResponseData<ResponseServiceGroupWithServices> result = new ResponseData<ResponseServiceGroupWithServices>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 동일한 이름이 존재하는지 확인한다.
				ResponseData<bool> responseExist = await this.IsNameExist(null, new RequestIsServiceGroupNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseServiceGroupWithServices>(responseExist.Result, responseExist.Code, responseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_DUPLICATED_NAME);

				// 서비스 GUID 목록
				List<Guid> serviceIds = new List<Guid>();
      
				// 모든 서비스 아이디에 대해서 처리
				if (request.ServiceIds != null && request.ServiceIds.Count > 0)
				{
					// 모든 서비스 아이디에 대해서 처리
					foreach (string serviceId in request.ServiceIds)
					{
						if(!Guid.TryParse(serviceId, out Guid guidServiceId))
							return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_INVALID_SERVICE_ID);
						serviceIds.Add(guidServiceId);
					}
      
					// 주어진 서비스 아이디로 다른 그룹에 속하지 않는 서비스 아이디 수가 요청 개수와 다른 경우  
					if(await this.m_dbContext.Services.AsNoTracking()
						.Where(i => serviceIds.Contains(i.Id) && i.GroupId == null && i.ServiceType == (EnumDbServiceType) request.ServiceType)
						.CountAsync() != serviceIds.Count)
						return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_NOT_AVAILABLE_SERVICE_ID_USED);
				}

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						ServiceGroup newData = new ServiceGroup()
						{
							Id = Guid.NewGuid(),
							Name = request.Name,
							Description = request.Description,
							ServiceType = (EnumDbServiceType) request.ServiceType,
							ServiceIpAddress = request.ServiceIpAddress,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.ServiceGroups.AddAsync(newData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						
						// 모든 서비스 아이디에 대해서 처리
						foreach (Guid guidServiceId in serviceIds)
						{
							// 해당 서비스 정보를 가져온다.
							Service service = await m_dbContext.Services
								.Where(i => i.Id == guidServiceId)
								.FirstOrDefaultAsync();

							// 그룹 아이디 변경
							service.GroupId = newData.Id;
						}
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
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
		public async Task<ResponseData> Update(string id, RequestServiceGroup request)
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
				ResponseData<bool> responseExist = await this.IsNameExist(id, new RequestIsServiceGroupNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData(responseExist.Result, responseExist.Code, responseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_DUPLICATED_NAME);

				// 서비스 GUID 목록
				List<Guid> serviceIds = new List<Guid>();
					
				// 모든 서비스 아이디에 대해서 처리
				if (request.ServiceIds != null && request.ServiceIds.Count > 0)
				{
					// 모든 서비스 아이디에 대해서 처리
					foreach (string serviceId in request.ServiceIds)
					{
						if(!Guid.TryParse(serviceId, out Guid guidServiceId))
							return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_INVALID_SERVICE_ID);
						serviceIds.Add(guidServiceId);
					}
					
					// 주어진 서비스 아이디로 다른 그룹에 속하지 않는 서비스 아이디 수가 요청 개수와 다른 경우  
					if(await this.m_dbContext.Services.AsNoTracking()
						.Where(i => serviceIds.Contains(i.Id) && (i.GroupId == null || i.GroupId == guidId) && i.ServiceType == (EnumDbServiceType) request.ServiceType)
						.CountAsync() != serviceIds.Count)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_SERVICE_GROUPS_NOT_AVAILABLE_SERVICE_ID_USED);
				}
				
				// 해당 정보를 가져온다.
				ServiceGroup exist = await m_dbContext.ServiceGroups
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				
				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
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
							foreach (Service oldService in oldServices)
								// 그룹 아이디 변경
								oldService.GroupId = null;
							// 데이터가 변경된 경우 저장
							if (m_dbContext.HasChanges())
								await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}
						
						// 모든 서비스 아이디에 대해서 처리
						foreach (Guid guidServiceId in serviceIds)
						{
							// 해당 서비스 정보를 가져온다.
							Service service = await m_dbContext.Services
								.Where(i => i.Id == guidServiceId)
								.FirstOrDefaultAsync();

							// 그룹 아이디 변경
							service.GroupId = guidId;
						}
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 정보를 수정한다.
						exist.Name = request.Name;
						exist.Description = request.Description;
						exist.ServiceType = (EnumDbServiceType) request.ServiceType;
						exist.ServiceIpAddress = request.ServiceIpAddress;
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
				ServiceGroup exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(result.Result = EnumResponseResult.Success);

				// 해당 그룹에 속한 서비스 목록을 가져온다.
				List<Service> services = await this.m_dbContext.Services
					.Where(i => i.GroupId == guidId)
					.ToListAsync();
				
				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 모든 서비스에 대해서 처리
						foreach (Service service in services)
							service.GroupId = null;
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 해당 데이터 삭제
						m_dbContext.ServiceGroups.Remove(exist);
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
		/// <param name="orderFields">정렬필드목록 (Name, Description, ServiceType, ServiceIpAddress)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, ServiceType, ServiceIpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseServiceGroup>> GetList(
			int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseServiceGroup> result = new ResponseList<ResponseServiceGroup>();
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
				result.Data = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i =>
						(
				            searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
				            || (searchFields.Contains("name") && i.Name.Contains(searchKeyword))
				            || (searchFields.Contains("description") && i.Description.Contains(searchKeyword))
				            || (searchFields.Contains("servicetype") && i.ServiceType == (EnumDbServiceType) serviceType)
				            || (searchFields.Contains("serviceipaddress") && i.ServiceIpAddress.Contains(searchKeyword))
					    )
					)
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<ServiceGroup, ResponseServiceGroup>(skip, countPerPage);

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
		public async Task<ResponseData<ResponseServiceGroupWithServices>> Get(string id)
		{
			ResponseData<ResponseServiceGroupWithServices> result = new ResponseData<ResponseServiceGroupWithServices>();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);
				
				// 정보를 가져온다.
				ResponseServiceGroupWithServices exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<ResponseServiceGroupWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				
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
		public async Task<ResponseData<bool>> IsNameExist(string exceptId, RequestIsServiceGroupNameExist request)
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
				if (await m_dbContext.ServiceGroups.AsNoTracking().AnyAsync(i => (exceptId.IsEmpty() || i.Id != guidId) && i.Name == request.Name))
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

		/// <summary>해당 서비스 타입으로 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="serviceType">서비스 타입</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>참여가 가능한 서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseService>> GetAvailableServices(
			EnumServiceType serviceType
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "")
		{
			ResponseList<ResponseService> result = new ResponseList<ResponseService>();

			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);
					
				// 목록을 가져온다.
				result.Data = await m_dbContext.Services.AsNoTracking()
					.Where(i =>
						i.ServiceType == (EnumDbServiceType) serviceType
						&& i.GroupId == null
						&& (
							searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
							|| (searchFields.Contains("name") && i.Name.Contains(searchKeyword))
							|| (searchFields.Contains("description") && i.Description.Contains(searchKeyword))
							|| (searchFields.Contains("ipaddress") && i.Vlans.Any(j => j.NetworkInterfaceVlan != null && j.NetworkInterfaceVlan.IpAddress.Contains(searchKeyword)))
						)
					)
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<Service, ResponseService>(skip, countPerPage);

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

		/// <summary>주어진 서비스 그룹 아이디에 해당하는 서비스 그룹에 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="id">서비스 그룹 아이디 (null인 경우, 어느 그룹에도 속하지 않은 서비스만 검색한다.)</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>참여가 가능한 서비스 목록 객체</returns>
		public async Task<ResponseList<ResponseService>> GetAvailableServices(
			string id
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "")
		{
			ResponseList<ResponseService> result = new ResponseList<ResponseService>();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseList<ResponseService>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);
				
				// 정보를 가져온다.
				ResponseServiceGroup exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroup>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseList<ResponseService>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);
					
				// 목록을 가져온다.
				result.Data = await m_dbContext.Services.AsNoTracking()
					.Where(i =>
						i.ServiceType == (EnumDbServiceType) exist.ServiceType
						&& (i.GroupId == null || (!id.IsEmpty() && i.GroupId == guidId))
						&& (
							searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
							|| (searchFields.Contains("name") && i.Name.Contains(searchKeyword))
							|| (searchFields.Contains("description") && i.Description.Contains(searchKeyword))
							|| (searchFields.Contains("ipaddress") && i.Vlans.Any(j => j.NetworkInterfaceVlan != null && j.NetworkInterfaceVlan.IpAddress.Contains(searchKeyword)))
						)
					)
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<Service, ResponseService>(skip, countPerPage);

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
				
				// 정보를 가져온다.
				ResponseServiceGroupWithServices exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				StringBuilder errorCodes = new StringBuilder();
				StringBuilder errorMessages = new StringBuilder();
				
				// 모든 서비스에 대해서 처리
				foreach (ResponseService service in exist.Services)
				{
					// 서비스를 시작한다.
					ResponseData response = await m_serviceProvider.Start(service.Id);
				
					// 성공이 아닌 경우
					if (response.Result != EnumResponseResult.Success)
					{
						errorCodes.AppendLine(response.Code);
						errorMessages.AppendLine(response.Message);
					}
				}

				// 에러 코드가 존재하지 않는 경우
				if (errorCodes.Length == 0)
					result.Result = EnumResponseResult.Success;
				// 에러 코드가 존재하는 경우, 경고로 처리하고 해당 코드와 메세지 저장
				else
				{
					result.Result = EnumResponseResult.Warning;
					result.Code = errorCodes.ToString();
					result.Message = errorMessages.ToString();
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
		
		/// <summary>서비스 중지</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>서비스 그룹 중지 결과 객체</returns>
		public async Task<ResponseData> Stop(string id)
		{
			ResponseData result = new ResponseData();
			
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);
				
				// 정보를 가져온다.
				ResponseServiceGroupWithServices exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				StringBuilder errorCodes = new StringBuilder();
				StringBuilder errorMessages = new StringBuilder();
				
				// 모든 서비스에 대해서 처리
				foreach (ResponseService service in exist.Services)
				{
					// 서비스를 중지한다.
					ResponseData response = await m_serviceProvider.Stop(service.Id);
				
					// 성공이 아닌 경우
					if (response.Result != EnumResponseResult.Success)
					{
						errorCodes.AppendLine(response.Code);
						errorMessages.AppendLine(response.Message);
					}
				}

				// 에러 코드가 존재하지 않는 경우
				if (errorCodes.Length == 0)
					result.Result = EnumResponseResult.Success;
				// 에러 코드가 존재하는 경우, 경고로 처리하고 해당 코드와 메세지 저장
				else
				{
					result.Result = EnumResponseResult.Warning;
					result.Code = errorCodes.ToString();
					result.Message = errorMessages.ToString();
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
				
				// 정보를 가져온다.
				ResponseServiceGroupWithServices exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				StringBuilder errorCodes = new StringBuilder();
				StringBuilder errorMessages = new StringBuilder();
				
				// 모든 서비스에 대해서 처리
				foreach (ResponseService service in exist.Services)
				{
					// 서비스를 재시작한다.
					ResponseData response = await m_serviceProvider.Restart(service.Id);
				
					// 성공이 아닌 경우
					if (response.Result != EnumResponseResult.Success)
					{
						errorCodes.AppendLine(response.Code);
						errorMessages.AppendLine(response.Message);
					}
				}

				// 에러 코드가 존재하지 않는 경우
				if (errorCodes.Length == 0)
					result.Result = EnumResponseResult.Success;
				// 에러 코드가 존재하는 경우, 경고로 처리하고 해당 코드와 메세지 저장
				else
				{
					result.Result = EnumResponseResult.Warning;
					result.Code = errorCodes.ToString();
					result.Message = errorMessages.ToString();
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

		/// <summary>특정 서비스 그룹의 설정 정보를 가져온다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>설정 문자열이 포함된 결과 객체</returns>
		public async Task<ResponseData<T>> GetConfig<T>(string id) where T : IResponseServiceConfig
		{
			ResponseData<T> result = new ResponseData<T>();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData<T>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);
				
				// 정보를 가져온다.
				ResponseServiceGroupWithServices exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<T>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 첫번째 서비스 설정 응답 객체
				T firstServiceConfig = default(T);
				
				StringBuilder errorCodes = new StringBuilder();
				StringBuilder errorMessages = new StringBuilder();

				// 해당 그룹의 모든 서비스에 대해서 처리
				foreach (ResponseService service in exist.Services)
				{
					ResponseData<T> response = await m_serviceProvider.GetConfig<T>(service.Id);
					
					// // 응답 및 설정 객체
					// ResponseData response = new ResponseData();
					// IResponseServiceConfig config = null;
					//
					// // 서비스 타입에 따라서 처리
					// switch (exist.ServiceType)
					// {
					// 	case EnumServiceType.HaProxy:
					// 		ResponseData<T> responseConfig = await m_serviceProvider.GetConfig<T>(service.Id);
					// 		response.CopyValueFrom(responseConfig);
					// 		config = responseConfig.Data;
					// 		break;
					// 	default:
					// 		response = new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
					// 		break;
					// }
					
					// 성공이 아닌 경우
					if (response.Result != EnumResponseResult.Success)
					{
						errorCodes.AppendLine(response.Code);
						errorMessages.AppendLine($"{response.Message} ({service.Name})");
						continue;
					}
					// 설정 객체가 유효하지 않은 경우
					else if (response.Data == null)
					{
						errorCodes.AppendLine(Resource.EC_COMMON__NOT_FOUND);
						errorMessages.AppendLine($"{Resource.EM_COMMON__NOT_FOUND} ({service.Name})");
						continue;
					}

					// 첫번째 서비스 설정이 존재하지 않는 경우, 첫번째 서비스 설정에 저장
					if (firstServiceConfig == null)
						firstServiceConfig = response.Data;
					// 첫번째 서비스 설정이 존재하는 경우
					else
					{
						// 첫번째 설정과 동일하지 않은 경우
						if (firstServiceConfig.Config != response.Data.Config)
						{
							errorCodes.AppendLine(Resource.EC_COMMON__INVALID_INFORMATION);
							errorMessages.AppendLine($"{Resource.EM_SERVICE_GROUPS_SERVICE_CONFIG_IS_DIFFERENT_FROM_OTHER_SERVICE} ({service.Name})");
						}
					}
				}

				// 에러 코드가 존재하지 않는 경우
				if (errorCodes.Length == 0 && firstServiceConfig != null)
				{
					result.Data = firstServiceConfig;
					result.Result = EnumResponseResult.Success;
				}
				// 에러 코드가 존재하는 경우, 경고로 처리하고 해당 코드와 메세지 저장
				else
				{
					result.Result = EnumResponseResult.Warning;
					result.Code = errorCodes.ToString();
					result.Message = errorMessages.ToString();
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

		/// <summary>주어진 설정 정보를 특정 서비스 그룹에 저장한다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
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
				
				// 정보를 가져온다.
				ResponseServiceGroupWithServices exist = await m_dbContext.ServiceGroups.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Services)
					.FirstOrDefaultAsync<ServiceGroup, ResponseServiceGroupWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				StringBuilder errorCodes = new StringBuilder();
				StringBuilder errorMessages = new StringBuilder();

				// 해당 그룹의 모든 서비스에 대해서 처리
				foreach (ResponseService service in exist.Services)
				{
					// 서비스에 설정을 저장한다.
					ResponseData response = await m_serviceProvider.SetConfig(service.Id, config);
				
					// 성공이 아닌 경우
					if (response.Result != EnumResponseResult.Success)
					{
						errorCodes.AppendLine(response.Code);
						errorMessages.AppendLine(response.Message);
					}
				}

				// 에러 코드가 존재하지 않는 경우
				if (errorCodes.Length == 0)
					result.Result = EnumResponseResult.Success;
				// 에러 코드가 존재하는 경우, 경고로 처리하고 해당 코드와 메세지 저장
				else
				{
					result.Result = EnumResponseResult.Warning;
					result.Code = errorCodes.ToString();
					result.Message = errorMessages.ToString();
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