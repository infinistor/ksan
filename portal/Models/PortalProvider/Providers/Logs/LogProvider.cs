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
using PortalModels;
using PortalProviderInterface;
using Microsoft.AspNetCore.Identity;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Networks;
using System;
using MTLib.Core;
using PortalResources;
using Microsoft.EntityFrameworkCore;
using System.Linq;
using System.Collections.Generic;
using MTLib.EntityFramework;
using MTLib.CommonData;
using PortalData.Requests.Disks;

namespace PortalProvider.Providers.Services
{
	/// <summary>서비스 데이터 프로바이더 클래스</summary>
	public class LogProvider : BaseProvider<PortalModel>, ILogProvider
	{

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public LogProvider(
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

		/// <summary> 네트워크인터페이스 사용량 목록을 가져온다.</summary>
		/// <returns>로그 목록 객체</returns>
		public async Task<ResponseList<ResponseNetworkInterfaceUsage>> GetLastNetworkUsages()
		{
			var Result = new ResponseList<ResponseNetworkInterfaceUsage>();

			try
			{
				// 현재 온라인 상태인 서버의 정보를 가져온다.
				var Servers = await m_dbContext.Servers.AsNoTracking()
					.Where(i => i.State == EnumDbServerState.Online)
					.Include(i => i.NetworkInterfaces)
					.ThenInclude(i => i.NetworkInterfaceVlans)
					.ToListAsync();

				//값이 비어있을 경우 빈 값을 반환한다.
				if (Servers == null || Servers.Count == 0) return Result;

				// 조회한 서버의 네트워크 인터페이스 목록을 추출한다.
				var NetworkInterfaces = new List<NetworkInterface>();
				foreach (var Server in Servers)
					NetworkInterfaces.AddRange(Server.NetworkInterfaces);

				// 값이 비어있을 경우 빈 값을 반환한다.
				if (NetworkInterfaces.Count == 0) return Result;

				//네트워크 인터페이스의 사용량을 조회한다.
				Result.Data = await m_dbContext.NetworkInterfaceUsages.AsNoTracking()
				.Where(i=> i.RegDate >= DateTime.Now.AddSeconds(-8))
				.OrderBy(i=>i.RegDate)
				.CreateListAsync<dynamic, ResponseNetworkInterfaceUsage>(0, NetworkInterfaces.Count);
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

		/// <summary> 디스크 사용량 목록을 가져온다.</summary>
		/// <returns>로그 목록 객체</returns>
		public async Task<ResponseList<ResponseDiskUsage>> GetLastDiskUsages()
		{
			var Result = new ResponseList<ResponseDiskUsage>();

			try
			{
				// 현재 온라인 상태인 서버의 정보를 가져온다.
				var Servers = await m_dbContext.Servers.AsNoTracking()
					.Where(i => i.State == EnumDbServerState.Online)
					.Include(i => i.Disks)
					.ToListAsync();

				//값이 비어있을 경우 빈 값을 반환한다.
				if (Servers == null || Servers.Count == 0) return Result;

				// 조회한 서버의 디스크 목록을 추출한다.
				var Disks = new List<Disk>();
				foreach (var Server in Servers)
					Disks.AddRange(Server.Disks);

				// 값이 비어있을 경우 빈 값을 반환한다.
				if (Disks.Count == 0) return Result;

				//디스크의 사용량을 조회한다.
				Result.Data = await m_dbContext.DiskUsages.AsNoTracking()
				.Where(i=> i.RegDate >= DateTime.Now.AddSeconds(-8))
				.OrderBy(i=>i.RegDate)
				.CreateListAsync<dynamic, ResponseDiskUsage>(0, Disks.Count);
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