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
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Request">네트워크 인터페이스 등록 요청 객체</param>
		/// <returns>네트워크 인터페이스 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseNetworkInterface>> Add(string ServerId, RequestNetworkInterface Request)
		{
			var Result = new ResponseData<ResponseNetworkInterface>();

			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 동일한 이름이 존재하는지 확인한다.
				var ResponseExist = await IsNameExist(ServerId, null, new RequestIsNetworkInterfaceNameExist(Request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (ResponseExist.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseNetworkInterface>(ResponseExist.Result, ResponseExist.Code, ResponseExist.Message);

				// 동일한 이름이 존재하는 경우
				if (ResponseExist.Data)
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_NETWORKS_NETWORK_INTERFACE_DUPLICATED_NAME);

				// 신규 아이디
				var NewId = Guid.NewGuid();

				// 네트워크 인터페이스 등록을 요청한다.
				var Response = SendRpcMq($"*.servers.{ServerId}.interfaces.check", new
				{
					Id = NewId.ToString(),
					ServerId = ServerId,
					Request.Name,
					Request.Description,
					Request.Dhcp,
					Request.MacAddress,
					Request.LinkState,
					Request.IpAddress,
					Request.SubnetMask,
					Request.Gateway,
					Request.Dns1,
					Request.Dns2,
					Request.BandWidth,
					Request.IsManagement,
				}, 10);

				// 실패인 경우
				if (Response.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseNetworkInterface>(EnumResponseResult.Error, Response.Code, Response.Message);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						var NewData = new NetworkInterface
						{
							Id = NewId,
							ServerId = GuidServerId,
							Name = Request.Name,
							Description = Request.Description,
							Dhcp = (EnumDbYesNo?)Request.Dhcp,
							MacAddress = Request.MacAddress,
							LinkState = (EnumDbNetworkLinkState?)Request.LinkState,
							IpAddress = Request.IpAddress,
							SubnetMask = Request.SubnetMask,
							Gateway = Request.Gateway,
							Dns1 = Request.Dns1,
							Dns2 = Request.Dns2,
							BandWidth = Request.BandWidth,
							IsManagement = Request.IsManagement,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.NetworkInterfaces.AddAsync(NewData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						var NewVlan = new NetworkInterfaceVlan
						{
							Id = Guid.NewGuid(),
							InterfaceId = NewData.Id,
							Tag = 1,
							IpAddress = Request.IpAddress,
							SubnetMask = Request.SubnetMask,
							Gateway = Request.Gateway,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.NetworkInterfaceVlans.AddAsync(NewVlan);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Data = (await Get(ServerId, NewData.Id.ToString())).Data;

						// 추가된 네트워크 인터페이스 정보 전송
						SendMq("*.servers.interfaces.added", new ResponseNetworkInterface().CopyValueFrom(NewData));
						// 추가된 네트워크 인터페이스 VLAN 정보 전송
						SendMq("*.servers.interfaces.vlans.added", new ResponseNetworkInterfaceVlan().CopyValueFrom(NewVlan));
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

		/// <summary>네트워크 인터페이스 수정</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <param name="Request">네트워크 인터페이스 수정 요청 객체</param>
		/// <returns>네트워크 인터페이스 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string ServerId, string Id, RequestNetworkInterface Request)
		{
			var Result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재하는지 확인한다.
				var ResponseExist = await IsNameExist(ServerId, Id, new RequestIsNetworkInterfaceNameExist(Request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (ResponseExist.Result != EnumResponseResult.Success)
					return new ResponseData(ResponseExist.Result, ResponseExist.Code, ResponseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (ResponseExist.Data)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_NETWORKS_NETWORK_INTERFACE_DUPLICATED_NAME);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.NetworkInterfaces
					.FirstOrDefaultAsync(i => i.ServerId == GuidServerId && i.Id == GuidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 인터페이스와 연결된 Tag가 1인 VLAN 정보를 가져온다.
				var ExistVlan = await m_dbContext.NetworkInterfaceVlans
					.FirstOrDefaultAsync(i => i.InterfaceId == GuidId && i.Tag == 1);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.Name = Request.Name;
						Exist.Description = Request.Description;
						Exist.Dhcp = (EnumDbYesNo?)Request.Dhcp;
						Exist.MacAddress = Request.MacAddress;
						Exist.LinkState = (EnumDbNetworkLinkState?)Request.LinkState;
						Exist.IpAddress = Request.IpAddress;
						Exist.SubnetMask = Request.SubnetMask;
						Exist.Gateway = Request.Gateway;
						Exist.Dns1 = Request.Dns1;
						Exist.Dns2 = Request.Dns2;
						Exist.BandWidth = Request.BandWidth;
						Exist.IsManagement = Request.IsManagement;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							// 네트워크 인터페이스 수정을 요청한다.
							var Response = SendRpcMq($"*.servers.{ServerId}.interfaces.check", new
							{
								Id = Id,
								ServerId = ServerId,
								Request.Name,
								Request.Description,
								Request.Dhcp,
								Request.MacAddress,
								Request.LinkState,
								Request.IpAddress,
								Request.SubnetMask,
								Request.Gateway,
								Request.Dns1,
								Request.Dns2,
								Request.BandWidth,
								Request.IsManagement,
							}, 10);

							// 실패인 경우
							if (Response.Result != EnumResponseResult.Success)
							{
								await Transaction.RollbackAsync();
								return new ResponseData(EnumResponseResult.Error, Response.Code, Response.Message);
							}

							Exist.ModId = LoginUserId;
							Exist.ModName = LoginUserName;
							Exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}

						bool ChangedVlan = false;

						// 해당 인터페이스와 연결된 Tag가 1인 VLAN 정보가 존재하지 않는 경우
						if (ExistVlan == null)
						{
							var NewVlan = new NetworkInterfaceVlan
							{
								Id = Guid.NewGuid(),
								InterfaceId = GuidId,
								Tag = 1,
								IpAddress = Request.IpAddress,
								SubnetMask = Request.SubnetMask,
								Gateway = Request.Gateway,
								RegId = LoginUserId,
								RegName = LoginUserName,
								RegDate = DateTime.Now,
								ModId = LoginUserId,
								ModName = LoginUserName,
								ModDate = DateTime.Now
							};
							await m_dbContext.NetworkInterfaceVlans.AddAsync(NewVlan);
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

							ExistVlan = NewVlan;
							ChangedVlan = true;
						}
						// 해당 인터페이스와 연결된 Tag가 1인 VLAN 정보가 존재하는 경우
						else
						{
							ExistVlan.IpAddress = Request.IpAddress;
							ExistVlan.SubnetMask = Request.SubnetMask;
							ExistVlan.Gateway = Request.Gateway;
							// 데이터가 변경된 경우 저장
							if (m_dbContext.HasChanges())
							{
								ExistVlan.ModId = LoginUserId;
								ExistVlan.ModName = LoginUserName;
								ExistVlan.ModDate = DateTime.Now;
								await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
								ChangedVlan = true;
							}
						}

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 수정된 네트워크 인터페이스 정보 전송
						SendMq("*.servers.interfaces.updated", new ResponseNetworkInterface().CopyValueFrom(Exist));
						// 수정된 네트워크 인터페이스 VLAN 정보 전송
						if (ChangedVlan)
							SendMq("*.servers.interfaces.vlans.updated", new ResponseNetworkInterfaceVlan().CopyValueFrom(ExistVlan));
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

		/// <summary>네트워크 인터페이스 상태 수정</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <param name="State">네트워크 인터페이스 링크 상태</param>
		/// <returns>네트워크 인터페이스 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateLinkState(string ServerId, string Id, EnumNetworkLinkState State)
		{
			var Result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.NetworkInterfaces
					.FirstOrDefaultAsync(i => i.ServerId == GuidServerId && i.Id == GuidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.LinkState = (EnumDbNetworkLinkState)State;
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

		/// <summary>네트워크 인터페이스 상태 수정</summary>
		/// <param name="Request">네트워크 인터페이스 링크 상태 수정 요청 객체</param>
		/// <returns>네트워크 인터페이스 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateLinkState(RequestNetworkInterfaceLinkState Request)
		{
			var Result = new ResponseData();

			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 상태 업데이트
				Result = await UpdateLinkState(Request.ServerId, Request.InterfaceId, Request.LinkState);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>네트워크 인터페이스 사용 정보 수정</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Rx">RX 값</param>
		/// <param name="Tx">TX 값</param>
		/// <returns>네트워크 인터페이스 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(string ServerId, string InterfaceId, decimal Rx, decimal Tx)
		{
			var Result = new ResponseData();
			try
			{
				// 서버 아이디가 유효하지 않은 경우
				if (ServerId.IsEmpty() || !Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (InterfaceId.IsEmpty() || !Guid.TryParse(InterfaceId, out Guid GuidInterfaceId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_INTERFACE_ID);

				// 해당 네트워크 인터페이스 정보를 가져온다.
				var Exist = await m_dbContext.NetworkInterfaces
					.FirstOrDefaultAsync(i => i.Id == GuidInterfaceId && i.ServerId == GuidServerId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.Rx = Rx;
						Exist.Tx = Tx;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 사용 정보 추가
						m_dbContext.NetworkInterfaceUsages.Add(new NetworkInterfaceUsage
						{
							Id = Exist.Id,
							RegDate = DateTime.Now,
							BandWidth = Exist.BandWidth,
							Rx = Rx,
							Tx = Tx
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

		/// <summary>네트워크 인터페이스 사용 정보 수정</summary>
		/// <param name="Request">서버 사용 정보 수정 요청 객체</param>
		/// <returns>네트워크 인터페이스 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(RequestNetworkInterfaceUsage Request)
		{
			var Result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 상태 변경
				Result = await UpdateUsage(Request.ServerId, Request.InterfaceId, Request.Rx, Request.Tx);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>네트워크 인터페이스 삭제</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <returns>네트워크 인터페이스 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string ServerId, string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.NetworkInterfaces.AsNoTracking()
					.FirstOrDefaultAsync(i => i.ServerId == GuidServerId && i.Id == GuidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Success);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 서비스 연결 목록을 가져온다.
						var Services = await m_dbContext.ServiceNetworkInterfaceVlans
							.Where(i => i.NetworkInterfaceVlan.InterfaceId == GuidId)
							.ToListAsync();
						// 서비스 연결 목록 삭제
						m_dbContext.ServiceNetworkInterfaceVlans.RemoveRange(Services);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// VLAN 목록을 가져온다.
						var Vlans = await m_dbContext.NetworkInterfaceVlans
							.Where(i => i.InterfaceId == GuidId)
							.ToListAsync();
						// VLAN 목록 삭제
						m_dbContext.NetworkInterfaceVlans.RemoveRange(Vlans);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 네트워크 사용 목록을 가져온다.
						var Usages = await m_dbContext.NetworkInterfaceUsages
							.Where(i => i.Id == GuidId)
							.ToListAsync();
						// 네트워크 사용 목록 삭제
						m_dbContext.NetworkInterfaceUsages.RemoveRange(Usages);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 해당 데이터 삭제
						m_dbContext.NetworkInterfaces.Remove(Exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						foreach (var Vlan in Vlans)
							// 삭제된 네트워크 인터페이스 VLAN 정보 전송
							SendMq("*.servers.interfaces.vlans.deleted", new { Id = Vlan.Id, Vlan.InterfaceId });
						// 삭제된 네트워크 인터페이스 정보 전송
						SendMq("*.servers.interfaces.deleted", new { Exist.Id, Exist.ServerId, Exist.Name });
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

		/// <summary>네트워크 인터페이스 목록을 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, MacAddress, IpAddress, SubnetMask, Gateway, Dns1, Dns2)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, MacAddress, IpAddress, SubnetMask, Gateway, Dns1, Dns2)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>네트워크 인터페이스 목록 객체</returns>
		public async Task<ResponseList<ResponseNetworkInterface>> GetList(
			string ServerId
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Result = new ResponseList<ResponseNetworkInterface>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseList<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseList<ResponseNetworkInterface>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.NetworkInterfaces.AsNoTracking()
					.Where(i => i.ServerId == GuidServerId
						&& (
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("name") && i.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("description") && i.Description.Contains(SearchKeyword))
							|| (SearchFields.Contains("macaddress") && i.MacAddress.Contains(SearchKeyword))
							|| (SearchFields.Contains("ipaddress") && i.IpAddress.Contains(SearchKeyword))
							|| (SearchFields.Contains("subnetmask") && i.SubnetMask.Contains(SearchKeyword))
							|| (SearchFields.Contains("gateway") && i.Gateway.Contains(SearchKeyword))
							|| (SearchFields.Contains("dns1") && i.Dns1.Contains(SearchKeyword))
							|| (SearchFields.Contains("dns2") && i.Dns2.Contains(SearchKeyword))
						)
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<NetworkInterface, ResponseNetworkInterface>(Skip, CountPerPage);

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

		/// <summary>네트워크 인터페이스 정보를 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <returns>네트워크 인터페이스 정보 객체</returns>
		public async Task<ResponseData<ResponseNetworkInterfaceDetail>> Get(string ServerId, string Id)
		{
			var Result = new ResponseData<ResponseNetworkInterfaceDetail>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceDetail>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData<ResponseNetworkInterfaceDetail>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData<ResponseNetworkInterfaceDetail>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				var Exist = await m_dbContext.NetworkInterfaces.AsNoTracking()
					.Where(i => i.ServerId == GuidServerId && i.Id == GuidId)
					.Include(i => i.NetworkInterfaceVlans)
					.FirstOrDefaultAsync<NetworkInterface, ResponseNetworkInterfaceDetail>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseNetworkInterfaceDetail>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

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
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="ExceptId">이름 검색 시 제외할 네트워크 인터페이스 아이디</param>
		/// <param name="Request">특정 이름의 네트워크 인터페이스 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsNameExist(string ServerId, string ExceptId, RequestIsNetworkInterfaceNameExist Request)
		{
			ResponseData<bool> Result = new ResponseData<bool>();
			Guid GuidId = Guid.Empty;

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_INVALID_SERVER_ID);

				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				if (!ExceptId.IsEmpty() && !Guid.TryParse(ExceptId, out GuidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청 객체가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<bool>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재하는 경우
				if (await m_dbContext.NetworkInterfaces.AsNoTracking().AnyAsync(i => (ExceptId.IsEmpty() || i.Id != GuidId) && i.ServerId == GuidServerId && i.Name == Request.Name))
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
	}
}