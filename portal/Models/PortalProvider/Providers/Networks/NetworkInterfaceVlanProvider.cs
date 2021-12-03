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
using PortalData.Requests.Networks;
using PortalData.Responses.Networks;
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

namespace PortalProvider.Providers.Networks
{
	/// <summary>네트워크 인터페이스 VLAN 데이터 프로바이더 클래스</summary>
	public class NetworkInterfaceVlanProvider : BaseProvider<PortalModel>, INetworkInterfaceVlanProvider
	{
		
		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public NetworkInterfaceVlanProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<NetworkInterfaceVlanProvider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		/// <summary>네트워크 인터페이스 등록</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="request">네트워크 인터페이스 등록 요청 객체</param>
		/// <returns>네트워크 인터페이스 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseNetworkInterfaceVlan>> Add(string serverId, string interfaceId, RequestNetworkInterfaceVlan request)
		{
			ResponseData<ResponseNetworkInterfaceVlan> result = new ResponseData<ResponseNetworkInterfaceVlan>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (interfaceId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(interfaceId, out Guid guidInterfaceId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);
				
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 해당 네트워크 인터페이스를 가져온다.
				NetworkInterface existNetworkInterface = await m_dbContext.NetworkInterfaces.AsNoTracking()
					.Where(i => i.Id == guidInterfaceId && i.ServerId == guidServerId)
					.FirstOrDefaultAsync();
				
				// 해당 네트워크 인터페이스가 존재하지 않는 경우
				if(existNetworkInterface == null)
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				
				// 동일한 VLAN 태그가 존재하는지 확인한다.
				ResponseData<bool> responseExist = await this.IsTagExist(guidServerId.ToString(), guidInterfaceId.ToString(), null, new RequestIsNetworkInterfaceVlanExist(request.Tag));
				// 동일한 VLAN 태그가 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseNetworkInterfaceVlan>(responseExist.Result, responseExist.Code, responseExist.Message);

				// 동일한 VLAN 태그가 존재하는 경우
				if (responseExist.Data)
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_DUPLICATED_TAG);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						NetworkInterfaceVlan newData = new NetworkInterfaceVlan()
						{
							Id = Guid.NewGuid(),
							InterfaceId = guidInterfaceId,
							Tag = request.Tag,
							IpAddress = request.IpAddress,
							SubnetMask = request.SubnetMask,
							Gateway = request.Gateway,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.NetworkInterfaceVlans.AddAsync(newData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
						result.Data = (await this.Get(guidServerId.ToString(), guidInterfaceId.ToString(), newData.Id.ToString())).Data;
						
						// 추가된 네트워크 인터페이스 VLAN 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.vlans.added", result.Data);
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

		/// <summary>네트워크 인터페이스 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <param name="request">네트워크 인터페이스 수정 요청 객체</param>
		/// <returns>네트워크 인터페이스 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string serverId, string interfaceId, string id, RequestNetworkInterfaceVlan request)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (interfaceId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(interfaceId, out Guid guidInterfaceId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);
				
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);
				
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 동일한 VLAN 태그가 존재하는지 확인한다.
				ResponseData<bool> responseExist = await this.IsTagExist(guidServerId.ToString(), guidInterfaceId.ToString(), id, new RequestIsNetworkInterfaceVlanExist(request.Tag));
				// 동일한 VLAN 태그가 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData(responseExist.Result, responseExist.Code, responseExist.Message);
				// 동일한 VLAN 태그가 존재하는 경우
				if (responseExist.Data)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_DUPLICATED_TAG);

				// 해당 정보를 가져온다.
				NetworkInterfaceVlan exist = await m_dbContext.NetworkInterfaceVlans
					.Include(i => i.NetworkInterface)
					.FirstOrDefaultAsync(i => i.NetworkInterface.ServerId == guidServerId && i.InterfaceId == guidInterfaceId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				
				// 태그가 1인 경우, 수정할 수 없음을 반환
				if (exist.Tag == 1)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_RESERVED_TAG_IS_READ_ONLY);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.Tag = request.Tag;
						exist.IpAddress = request.IpAddress;
						exist.SubnetMask = request.SubnetMask;
						exist.Gateway = request.Gateway;
						// 데이터가 변경된 경우 저장
						if(m_dbContext.HasChanges())
						{
							exist.ModId = LoginUserId;
							exist.ModName = LoginUserName;
							exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}
						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
						
						// 수정된 네트워크 인터페이스 VLAN 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.vlans.updated", new ResponseNetworkInterfaceVlan().CopyValueFrom(exist));
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
	
		/// <summary>네트워크 인터페이스 삭제</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <returns>네트워크 인터페이스 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string serverId, string interfaceId, string id)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (interfaceId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(interfaceId, out Guid guidInterfaceId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);
				
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				NetworkInterfaceVlan exist = await m_dbContext.NetworkInterfaceVlans.AsNoTracking()
					.FirstOrDefaultAsync(i => i.NetworkInterface.ServerId == guidServerId && i.InterfaceId == guidInterfaceId && i.Id == guidId);
				
				// 태그가 1인 경우, 수정할 수 없음을 반환
				if (exist.Tag == 1)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_RESERVED_TAG_IS_READ_ONLY);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Success);
				
				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 서비스 연결 목록을 가져온다.
						List<ServiceNetworkInterfaceVlan> services = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
							.Where(i => i.VlanId == guidId)
							.ToListAsync();
						// 서비스 연결 목록 삭제
						m_dbContext.ServiceNetworkInterfaceVlans.RemoveRange(services);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						
						// 해당 데이터 삭제
						m_dbContext.NetworkInterfaceVlans.Remove(exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
						
						// 삭제된 네트워크 인터페이스 VLAN 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.vlans.deleted", new ResponseNetworkInterfaceVlan().CopyValueFrom(exist));
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

		/// <summary>네트워크 인터페이스 목록을 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Tag)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Tag)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>네트워크 인터페이스 목록 객체</returns>
		public async Task<ResponseList<ResponseNetworkInterfaceVlan>> GetList(
			string serverId, string interfaceId
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseNetworkInterfaceVlan> result = new ResponseList<ResponseNetworkInterfaceVlan>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseList<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseList<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (interfaceId.IsEmpty())
					return new ResponseList<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(interfaceId, out Guid guidInterfaceId))
					return new ResponseList<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);
				
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Tag", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);
					
				// 목록을 가져온다.
				result.Data = await m_dbContext.NetworkInterfaceVlans.AsNoTracking()
					.Where(i => 
						i.NetworkInterface.ServerId == guidServerId
						&& i.InterfaceId == guidInterfaceId
                        && (
                            searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
                            || (searchFields.Contains("tag") && i.Tag.ToString().Contains(searchKeyword))
                        )
					)
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<NetworkInterfaceVlan, ResponseNetworkInterfaceVlan>(skip, countPerPage);

				// 서버 아이디 저장
				foreach (ResponseNetworkInterfaceVlan vlan in result.Data.Items)
					vlan.ServerId = serverId;

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

		/// <summary>네트워크 인터페이스 정보를 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <returns>네트워크 인터페이스 정보 객체</returns>
		public async Task<ResponseData<ResponseNetworkInterfaceVlan>> Get(string serverId, string interfaceId, string id)
		{
			ResponseData<ResponseNetworkInterfaceVlan> result = new ResponseData<ResponseNetworkInterfaceVlan>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (interfaceId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(interfaceId, out Guid guidInterfaceId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);
				
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				ResponseNetworkInterfaceVlan exist = await m_dbContext.NetworkInterfaceVlans.AsNoTracking()
					.Where(i => i.NetworkInterface.ServerId == guidServerId && i.InterfaceId == guidInterfaceId && i.Id == guidId)
					.FirstOrDefaultAsync<NetworkInterfaceVlan, ResponseNetworkInterfaceVlan>();	

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				
				// 서버 아이디 저장
				exist.ServerId = serverId;
				
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

		/// <summary>해당 VLAN 태그가 존재하는지 여부</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="exceptId">이름 검색 시 제외할 네트워크 인터페이스 VLAN 아이디</param>
		/// <param name="request">특정 VLAN 태그 존재여부 확인 요청 객체</param>
		/// <returns>해당 VLAN 태그가 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsTagExist(string serverId, string interfaceId, string exceptId, RequestIsNetworkInterfaceVlanExist request)
		{
			ResponseData<bool> result = new ResponseData<bool>();
			Guid guidId = Guid.Empty;

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (interfaceId.IsEmpty())
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(interfaceId, out Guid guidInterfaceId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);
				
				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				if (!exceptId.IsEmpty() && !Guid.TryParse(exceptId, out guidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청 객체가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<bool>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());
					
				// 동일한 VLAN 태그가 존재하는 경우
				if (await m_dbContext.NetworkInterfaceVlans.AsNoTracking()
					.AnyAsync(i => 
						(exceptId.IsEmpty() || i.Id != guidId)
						&& i.NetworkInterface.ServerId == guidServerId
						&& i.InterfaceId == guidInterfaceId
						&& i.Tag == request.Tag))
					result.Data = true;
				// 동일한 VLAN 태그가 존재하지 않는 경우
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