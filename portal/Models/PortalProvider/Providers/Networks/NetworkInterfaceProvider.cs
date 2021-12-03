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
	/// <summary>네트워크 인터페이스 데이터 프로바이더 클래스</summary>
	public class NetworkInterfaceProvider : BaseProvider<PortalModel>, INetworkInterfaceProvider
	{
		
		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public NetworkInterfaceProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<NetworkInterfaceProvider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		/// <summary>네트워크 인터페이스 등록</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="request">네트워크 인터페이스 등록 요청 객체</param>
		/// <returns>네트워크 인터페이스 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseNetworkInterface>> Add(string serverId, RequestNetworkInterface request)
		{
			ResponseData<ResponseNetworkInterface> result = new ResponseData<ResponseNetworkInterface>();

			try
			{
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);
				
				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);
				
				// 동일한 이름이 존재하는지 확인한다.
				ResponseData<bool> responseExist = await IsNameExist(serverId, null, new RequestIsNetworkInterfaceNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseNetworkInterface>(responseExist.Result, responseExist.Code, responseExist.Message);

				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_NETWORKS_NETWORK_INTERFACE_DUPLICATED_NAME);

				// 신규 아이디
				Guid newId = Guid.NewGuid();
				
				// 네트워크 인터페이스 등록을 요청한다.
				ResponseData response = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.servers.{serverId}.interfaces.add", new
				{
					Id = newId.ToString(),
					ServerId = serverId,
					request.Name,
					request.Description,
					request.Dhcp,
					request.MacAddress,
					request.LinkState,
					request.IpAddress,
					request.SubnetMask,
					request.Gateway,
					request.Dns1,
					request.Dns2,
					request.BandWidth,
					request.IsManagement,
				}, 10);
				
				// 실패인 경우
				if (response.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, response.Code, response.Message);
				
				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						NetworkInterface newData = new NetworkInterface
						{
							Id = newId,
							ServerId = guidServerId,
							Name = request.Name,
							Description = request.Description,
							Dhcp = (EnumDbYesNo?) request.Dhcp,
							MacAddress = request.MacAddress,
							LinkState = (EnumDbNetworkLinkState?) request.LinkState,
							IpAddress = request.IpAddress,
							SubnetMask = request.SubnetMask,
							Gateway = request.Gateway,
							Dns1 = request.Dns1,
							Dns2 = request.Dns2,
							BandWidth = request.BandWidth,
							IsManagement = request.IsManagement,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.NetworkInterfaces.AddAsync(newData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						NetworkInterfaceVlan newVlan = new NetworkInterfaceVlan
						{
							Id = Guid.NewGuid(),
							InterfaceId = newData.Id,
							Tag = 1,
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
						await m_dbContext.NetworkInterfaceVlans.AddAsync(newVlan);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
						result.Data = (await Get(serverId, newData.Id.ToString())).Data;
						
						// 추가된 네트워크 인터페이스 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.added", new ResponseNetworkInterface().CopyValueFrom(newData));
						// 추가된 네트워크 인터페이스 VLAN 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.vlans.added", new ResponseNetworkInterfaceVlan().CopyValueFrom(newVlan));
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
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <param name="request">네트워크 인터페이스 수정 요청 객체</param>
		/// <returns>네트워크 인터페이스 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string serverId, string id, RequestNetworkInterface request)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());
				
				// 동일한 이름이 존재하는지 확인한다.
				ResponseData<bool> responseExist = await IsNameExist(serverId, id, new RequestIsNetworkInterfaceNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData(responseExist.Result, responseExist.Code, responseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_NETWORKS_NETWORK_INTERFACE_DUPLICATED_NAME);

				// 해당 정보를 가져온다.
				NetworkInterface exist = await m_dbContext.NetworkInterfaces
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 인터페이스와 연결된 Tag가 1인 VLAN 정보를 가져온다. 
				NetworkInterfaceVlan existVlan = await m_dbContext.NetworkInterfaceVlans
					.FirstOrDefaultAsync(i => i.InterfaceId == guidId && i.Tag == 1);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.Name = request.Name;
						exist.Description = request.Description;
						exist.Dhcp = (EnumDbYesNo?) request.Dhcp;
						exist.MacAddress = request.MacAddress;
						exist.LinkState = (EnumDbNetworkLinkState?) request.LinkState;
						exist.IpAddress = request.IpAddress;
						exist.SubnetMask = request.SubnetMask;
						exist.Gateway = request.Gateway;
						exist.Dns1 = request.Dns1;
						exist.Dns2 = request.Dns2;
						exist.BandWidth = request.BandWidth;
						exist.IsManagement = request.IsManagement;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							// 네트워크 인터페이스 수정을 요청한다.
							ResponseData response = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.servers.{serverId}.interfaces.update", new
							{
								Id = id,
								ServerId = serverId,
								request.Name,
								request.Description,
								request.Dhcp,
								request.MacAddress,
								request.LinkState,
								request.IpAddress,
								request.SubnetMask,
								request.Gateway,
								request.Dns1,
								request.Dns2,
								request.BandWidth,
								request.IsManagement,
							}, 10);
				
							// 실패인 경우
							if (response.Result != EnumResponseResult.Success)
							{
								await transaction.RollbackAsync();
								return new ResponseData(EnumResponseResult.Error, response.Code, response.Message);
							}

							exist.ModId = LoginUserId;
							exist.ModName = LoginUserName;
							exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}

						bool isChangedVlan = false;
						
						// 해당 인터페이스와 연결된 Tag가 1인 VLAN 정보가 존재하지 않는 경우
						if (existVlan == null)
						{
							NetworkInterfaceVlan newVlan = new NetworkInterfaceVlan
							{
								Id = Guid.NewGuid(),
								InterfaceId = guidId,
								Tag = 1,
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
							await m_dbContext.NetworkInterfaceVlans.AddAsync(newVlan);
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

							existVlan = newVlan;
							isChangedVlan = true;
						}
						// 해당 인터페이스와 연결된 Tag가 1인 VLAN 정보가 존재하는 경우
						else
						{
							existVlan.IpAddress = request.IpAddress;
							existVlan.SubnetMask = request.SubnetMask;
							existVlan.Gateway = request.Gateway;
							// 데이터가 변경된 경우 저장
							if (m_dbContext.HasChanges())
							{
								existVlan.ModId = LoginUserId;
								existVlan.ModName = LoginUserName;
								existVlan.ModDate = DateTime.Now;
								await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
								isChangedVlan = true;
							}
						}

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
						
						// 수정된 네트워크 인터페이스 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.updated", new ResponseNetworkInterface().CopyValueFrom(exist));
						// 수정된 네트워크 인터페이스 VLAN 정보 전송
						if(isChangedVlan)
							SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.vlans.updated", new ResponseNetworkInterfaceVlan().CopyValueFrom(existVlan));
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

		/// <summary>네트워크 인터페이스 상태 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <param name="state">네트워크 인터페이스 링크 상태</param>
		/// <returns>네트워크 인터페이스 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateLinkState(string serverId, string id, EnumNetworkLinkState state)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				NetworkInterface exist = await m_dbContext.NetworkInterfaces
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.LinkState = (EnumDbNetworkLinkState) state;
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

		/// <summary>네트워크 인터페이스 상태 수정</summary>
		/// <param name="request">네트워크 인터페이스 링크 상태 수정 요청 객체</param>
		/// <returns>네트워크 인터페이스 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateLinkState(RequestNetworkInterfaceLinkState request)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 상태 업데이트
				result = await UpdateLinkState(request.ServerId, request.InterfaceId, request.LinkState);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			
			return result;
		}
	
		/// <summary>네트워크 인터페이스 사용 정보 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="rx">RX 값</param>
		/// <param name="tx">TX 값</param>
		/// <returns>네트워크 인터페이스 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(string serverId, string interfaceId, decimal rx, decimal tx)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 서버 아이디가 유효하지 않은 경우
				if (serverId.IsEmpty() || !Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);
				
				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (interfaceId.IsEmpty() || !Guid.TryParse(interfaceId, out Guid guidInterfaceId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_INTERFACE_ID);
				
				// 해당 네트워크 인터페이스 정보를 가져온다.
				NetworkInterface exist = await m_dbContext.NetworkInterfaces
					.FirstOrDefaultAsync(i => i.Id == guidInterfaceId && i.ServerId == guidServerId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				
				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.Rx = rx;
						exist.Tx = tx;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 사용 정보 추가
						m_dbContext.NetworkInterfaceUsages.Add(new NetworkInterfaceUsage
						{
							Id = exist.Id,
							RegDate = DateTime.Now,
							BandWidth = exist.BandWidth,
							Rx = rx,
							Tx = tx
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
	
		/// <summary>네트워크 인터페이스 사용 정보 수정</summary>
		/// <param name="request">서버 사용 정보 수정 요청 객체</param>
		/// <returns>네트워크 인터페이스 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(RequestNetworkInterfaceUsage request)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 상태 변경
				result = await UpdateUsage(request.ServerId, request.InterfaceId, request.Rx, request.Tx);
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
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <returns>네트워크 인터페이스 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string serverId, string id)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);
				
				// 해당 정보를 가져온다.
				NetworkInterface exist = await m_dbContext.NetworkInterfaces.AsNoTracking()
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Success);
				
				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 서비스 연결 목록을 가져온다.
						List<ServiceNetworkInterfaceVlan> services = await m_dbContext.ServiceNetworkInterfaceVlans
							.Where(i => i.NetworkInterfaceVlan.InterfaceId == guidId)
							.ToListAsync();
						// 서비스 연결 목록 삭제
						m_dbContext.ServiceNetworkInterfaceVlans.RemoveRange(services);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// VLAN 목록을 가져온다.
						List<NetworkInterfaceVlan> vlans = await m_dbContext.NetworkInterfaceVlans
							.Where(i => i.InterfaceId == guidId)
							.ToListAsync();
						// VLAN 목록 삭제
						m_dbContext.NetworkInterfaceVlans.RemoveRange(vlans);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 네트워크 사용 목록을 가져온다.
						List<NetworkInterfaceUsage> usages = await m_dbContext.NetworkInterfaceUsages
							.Where(i => i.Id == guidId)
							.ToListAsync();
						// 네트워크 사용 목록 삭제
						m_dbContext.NetworkInterfaceUsages.RemoveRange(usages);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 해당 데이터 삭제
						m_dbContext.NetworkInterfaces.Remove(exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
						
						foreach (NetworkInterfaceVlan vlan in vlans)
							// 삭제된 네트워크 인터페이스 VLAN 정보 전송
							SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.vlans.deleted", new ResponseNetworkInterfaceVlan().CopyValueFrom(vlan));
						// 삭제된 네트워크 인터페이스 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.deleted", new ResponseNetworkInterface().CopyValueFrom(exist));
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
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description, MacAddress, IpAddress, SubnetMask, Gateway, Dns1, Dns2)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, MacAddress, IpAddress, SubnetMask, Gateway, Dns1, Dns2)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>네트워크 인터페이스 목록 객체</returns>
		public async Task<ResponseList<ResponseNetworkInterface>> GetList(
			string serverId
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseNetworkInterface> result = new ResponseList<ResponseNetworkInterface>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseList<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseList<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);
					
				// 목록을 가져온다.
				result.Data = await m_dbContext.NetworkInterfaces.AsNoTracking()
					.Where(i => i.ServerId == guidServerId
						&& (
							searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
							|| (searchFields.Contains("name") && i.Name.Contains(searchKeyword))
							|| (searchFields.Contains("description") && i.Description.Contains(searchKeyword))
							|| (searchFields.Contains("macaddress") && i.MacAddress.Contains(searchKeyword))
							|| (searchFields.Contains("ipaddress") && i.IpAddress.Contains(searchKeyword))
							|| (searchFields.Contains("subnetmask") && i.SubnetMask.Contains(searchKeyword))
							|| (searchFields.Contains("gateway") && i.Gateway.Contains(searchKeyword))
							|| (searchFields.Contains("dns1") && i.Dns1.Contains(searchKeyword))
							|| (searchFields.Contains("dns2") && i.Dns2.Contains(searchKeyword))
						)
					)
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<NetworkInterface, ResponseNetworkInterface>(skip, countPerPage);

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
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <returns>네트워크 인터페이스 정보 객체</returns>
		public async Task<ResponseData<ResponseNetworkInterfaceDetail>> Get(string serverId, string id)
		{
			ResponseData<ResponseNetworkInterfaceDetail> result = new ResponseData<ResponseNetworkInterfaceDetail>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceDetail>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData<ResponseNetworkInterfaceDetail>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData<ResponseNetworkInterfaceDetail>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);
				
				// 정보를 가져온다.
				ResponseNetworkInterfaceDetail exist = await m_dbContext.NetworkInterfaces.AsNoTracking()
					.Where(i => i.ServerId == guidServerId && i.Id == guidId)
					.Include(i => i.NetworkInterfaceVlans)
					.FirstOrDefaultAsync<NetworkInterface, ResponseNetworkInterfaceDetail>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<ResponseNetworkInterfaceDetail>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				
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
		/// <param name="serverId">서버 아이디</param>
		/// <param name="exceptId">이름 검색 시 제외할 네트워크 인터페이스 아이디</param>
		/// <param name="request">특정 이름의 네트워크 인터페이스 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsNameExist(string serverId, string exceptId, RequestIsNetworkInterfaceNameExist request)
		{
			ResponseData<bool> result = new ResponseData<bool>();
			Guid guidId = Guid.Empty;
			
			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				if (!exceptId.IsEmpty() && !Guid.TryParse(exceptId, out guidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);
				
				// 요청 객체가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<bool>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());
					
				// 동일한 이름이 존재하는 경우
				if (await m_dbContext.NetworkInterfaces.AsNoTracking().AnyAsync(i => (exceptId.IsEmpty() || i.Id != guidId) && i.ServerId == guidServerId && i.Name == request.Name))
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