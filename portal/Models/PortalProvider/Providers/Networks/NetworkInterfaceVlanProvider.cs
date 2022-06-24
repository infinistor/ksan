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
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Request">네트워크 인터페이스 등록 요청 객체</param>
		/// <returns>네트워크 인터페이스 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseNetworkInterfaceVlan>> Add(string ServerId, string InterfaceId, RequestNetworkInterfaceVlan Request)
		{
			var Result = new ResponseData<ResponseNetworkInterfaceVlan>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (InterfaceId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(InterfaceId, out Guid GuidInterfaceId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);

				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 해당 네트워크 인터페이스를 가져온다.
				var ExistNetworkInterface = await m_dbContext.NetworkInterfaces.AsNoTracking()
					.Where(i => i.Id == GuidInterfaceId && i.ServerId == GuidServerId)
					.FirstOrDefaultAsync();

				// 해당 네트워크 인터페이스가 존재하지 않는 경우
				if (ExistNetworkInterface == null)
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 동일한 VLAN 태그가 존재하는지 확인한다.
				var ResponseExist = await this.IsTagExist(GuidServerId.ToString(), GuidInterfaceId.ToString(), null, new RequestIsNetworkInterfaceVlanExist(Request.Tag));
				// 동일한 VLAN 태그가 존재하는지 확인하는데 실패한 경우
				if (ResponseExist.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseNetworkInterfaceVlan>(ResponseExist.Result, ResponseExist.Code, ResponseExist.Message);

				// 동일한 VLAN 태그가 존재하는 경우
				if (ResponseExist.Data)
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_DUPLICATED_TAG);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						var NewData = new NetworkInterfaceVlan()
						{
							Id = Guid.NewGuid(),
							InterfaceId = GuidInterfaceId,
							Tag = Request.Tag,
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
						await m_dbContext.NetworkInterfaceVlans.AddAsync(NewData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Data = (await this.Get(GuidServerId.ToString(), GuidInterfaceId.ToString(), NewData.Id.ToString())).Data;

						// 추가된 네트워크 인터페이스 VLAN 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.vlans.added", Result.Data);
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
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <param name="Request">네트워크 인터페이스 수정 요청 객체</param>
		/// <returns>네트워크 인터페이스 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string ServerId, string InterfaceId, string Id, RequestNetworkInterfaceVlan Request)
		{
			var Result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (InterfaceId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(InterfaceId, out Guid GuidInterfaceId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 VLAN 태그가 존재하는지 확인한다.
				var ResponseExist = await this.IsTagExist(GuidServerId.ToString(), GuidInterfaceId.ToString(), Id, new RequestIsNetworkInterfaceVlanExist(Request.Tag));
				// 동일한 VLAN 태그가 존재하는지 확인하는데 실패한 경우
				if (ResponseExist.Result != EnumResponseResult.Success)
					return new ResponseData(ResponseExist.Result, ResponseExist.Code, ResponseExist.Message);
				// 동일한 VLAN 태그가 존재하는 경우
				if (ResponseExist.Data)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_DUPLICATED_TAG);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.NetworkInterfaceVlans
					.Include(i => i.NetworkInterface)
					.FirstOrDefaultAsync(i => i.NetworkInterface.ServerId == GuidServerId && i.InterfaceId == GuidInterfaceId && i.Id == GuidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 태그가 1인 경우, 수정할 수 없음을 반환
				if (Exist.Tag == 1)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_RESERVED_TAG_IS_READ_ONLY);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.Tag = Request.Tag;
						Exist.IpAddress = Request.IpAddress;
						Exist.SubnetMask = Request.SubnetMask;
						Exist.Gateway = Request.Gateway;
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

						// 수정된 네트워크 인터페이스 VLAN 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.vlans.updated", new ResponseNetworkInterfaceVlan().CopyValueFrom(Exist));
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

		/// <summary>네트워크 인터페이스 삭제</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <returns>네트워크 인터페이스 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string ServerId, string InterfaceId, string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (InterfaceId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(InterfaceId, out Guid GuidInterfaceId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.NetworkInterfaceVlans.AsNoTracking()
					.FirstOrDefaultAsync(i => i.NetworkInterface.ServerId == GuidServerId && i.InterfaceId == GuidInterfaceId && i.Id == GuidId);

				// 태그가 1인 경우, 수정할 수 없음을 반환
				if (Exist.Tag == 1)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_RESERVED_TAG_IS_READ_ONLY);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Success);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 서비스 연결 목록을 가져온다.
						var Services = await m_dbContext.ServiceNetworkInterfaceVlans.AsNoTracking()
							.Where(i => i.VlanId == GuidId)
							.ToListAsync();
						// 서비스 연결 목록 삭제
						m_dbContext.ServiceNetworkInterfaceVlans.RemoveRange(Services);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 해당 데이터 삭제
						m_dbContext.NetworkInterfaceVlans.Remove(Exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 삭제된 네트워크 인터페이스 VLAN 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.interfaces.vlans.deleted", new ResponseNetworkInterfaceVlan().CopyValueFrom(Exist));
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
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Tag)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Tag)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>네트워크 인터페이스 목록 객체</returns>
		public async Task<ResponseList<ResponseNetworkInterfaceVlan>> GetList(
			string ServerId, string InterfaceId
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Result = new ResponseList<ResponseNetworkInterfaceVlan>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseList<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseList<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (InterfaceId.IsEmpty())
					return new ResponseList<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(InterfaceId, out Guid GuidInterfaceId))
					return new ResponseList<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Tag", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.NetworkInterfaceVlans.AsNoTracking()
					.Where(i =>
						i.NetworkInterface.ServerId == GuidServerId
						&& i.InterfaceId == GuidInterfaceId
						&& (
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("tag") && i.Tag.ToString().Contains(SearchKeyword))
						)
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<NetworkInterfaceVlan, ResponseNetworkInterfaceVlan>(Skip, CountPerPage);

				// 서버 아이디 저장
				foreach (var Vlan in Result.Data.Items)
					Vlan.ServerId = ServerId;

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
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <returns>네트워크 인터페이스 정보 객체</returns>
		public async Task<ResponseData<ResponseNetworkInterfaceVlan>> Get(string ServerId, string InterfaceId, string Id)
		{
			var Result = new ResponseData<ResponseNetworkInterfaceVlan>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (InterfaceId.IsEmpty())
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(InterfaceId, out Guid GuidInterfaceId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.NetworkInterfaceVlans.AsNoTracking()
					.Where(i => i.NetworkInterface.ServerId == GuidServerId && i.InterfaceId == GuidInterfaceId && i.Id == GuidId)
					.FirstOrDefaultAsync<NetworkInterfaceVlan, ResponseNetworkInterfaceVlan>();

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseNetworkInterfaceVlan>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 서버 아이디 저장
				Exist.ServerId = ServerId;

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

		/// <summary>해당 VLAN 태그가 존재하는지 여부</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="ExceptId">이름 검색 시 제외할 네트워크 인터페이스 VLAN 아이디</param>
		/// <param name="Request">특정 VLAN 태그 존재여부 확인 요청 객체</param>
		/// <returns>해당 VLAN 태그가 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsTagExist(string ServerId, string InterfaceId, string ExceptId, RequestIsNetworkInterfaceVlanExist Request)
		{
			ResponseData<bool> Result = new ResponseData<bool>();
			Guid GuidId = Guid.Empty;

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_SERVER_ID);

				// 네트워크 인터페이스 아이디가 존재하지 않는 경우
				if (InterfaceId.IsEmpty())
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_REQUIRE_INTERFACE_ID);

				// 네트워크 인터페이스 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(InterfaceId, out Guid GuidInterfaceId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_NETWORKS_NETWORK_INTERFACE_VLAN_INVALID_INTERFACE_ID);

				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				if (!ExceptId.IsEmpty() && !Guid.TryParse(ExceptId, out GuidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청 객체가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<bool>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 VLAN 태그가 존재하는 경우
				if (await m_dbContext.NetworkInterfaceVlans.AsNoTracking()
					.AnyAsync(i =>
						(ExceptId.IsEmpty() || i.Id != GuidId)
						&& i.NetworkInterface.ServerId == GuidServerId
						&& i.InterfaceId == GuidInterfaceId
						&& i.Tag == Request.Tag))
					Result.Data = true;
				// 동일한 VLAN 태그가 존재하지 않는 경우
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