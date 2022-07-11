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
using PortalData.Requests.Region;
using PortalData.Responses.Region;
using MTLib.Reflection;

namespace PortalProvider.Providers.Accounts
{
	/// <summary>사용자 프로바이더 클래스</summary>
	public class RegionProvider : BaseProvider<PortalModel>, IRegionProvider
	{
		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public RegionProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<RegionProvider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		/// <summary>리전을 생성한다.</summary>
		/// <param name="Request">리전 정보</param>
		/// <returns>리전 등록 결과</returns>
		public async Task<ResponseData<ResponseRegion>> Add(RequestRegion Request)
		{
			var Result = new ResponseData<ResponseRegion>();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseRegion>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 해당 리전 객체를 생성한다.
				var NewData = new Region
				{
					Name = Request.Name,
					Address = Request.Address,
					Port = Request.Port,
					SSLPort = Request.SSLPort,
					AccessKey = KsanUserProvider.CreateAccessKey(),
					SecretKey = KsanUserProvider.CreateSecretKey(),
				};

				// 리전 등록
				await m_dbContext.Regions.AddAsync(NewData);
				await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

				Result.Data = new ResponseRegion();
				Result.Data.CopyValueFrom(NewData);
				
				Result.Result = EnumResponseResult.Success;
				Result.Code = Resource.SC_COMMON__SUCCESS;
				Result.Message = Resource.SM_COMMON__CREATED;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>리전을 동기화한다.</summary>
		/// <param name="Request">리전 정보</param>
		/// <returns>리전 등록 결과</returns>
		public async Task<ResponseData> Sync(List<RequestRegion> Requests)
		{
			var Result = new ResponseData();

			using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
			{
				try
				{
					// 파라미터가 유효하지 않은 경우
					foreach (var Request in Requests)
					{
						if (!Request.IsValid())
							return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

						// 해당 리전 객체를 생성한다.
						var NewData = new Region
						{
							Name = Request.Name,
							Address = Request.Address,
							Port = Request.Port,
							SSLPort = Request.SSLPort,
							AccessKey = Request.AccessKey,
							SecretKey = Request.SecretKey
						};

						// 리전 등록
						await m_dbContext.Regions.AddAsync(NewData);
					}
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					Result.Result = EnumResponseResult.Success;
					Result.Code = Resource.SC_COMMON__SUCCESS;
					Result.Message = Resource.SM_COMMON__CREATED;
				}
				catch (Exception ex)
				{
					await Transaction.RollbackAsync();
					NNException.Log(ex);

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__EXCEPTION;
				}
				return Result;
			}
		}

		/// <summary>리전을 수정한다.</summary>
		/// <param name="RegionName">리전 식별자</param>
		/// <param name="Request">리전 정보</param>
		/// <returns>리전 수정 결과</returns>
		public async Task<ResponseData> Update(string RegionName, RequestRegion Request)
		{
			var Result = new ResponseData();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 리전 정보를 가져온다.
				var Exist = await m_dbContext.Regions.FirstOrDefaultAsync(i => i.Name == RegionName);

				// 가져오는데 실패할경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 리전 변경
						Exist.Address = Request.Address;
						Exist.Port = Request.Port;
						Exist.SSLPort = Request.SSLPort;
						Exist.AccessKey = Request.AccessKey;
						Exist.SecretKey = Request.SecretKey;

						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Code = Resource.SC_COMMON__SUCCESS;
						Result.Message = Resource.SM_COMMON__CREATED;
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

		/// <summary>리전 식별자로 특정 리전을 가져온다.</summary>
		/// <param name="RegionName">리전 식별자</param>
		/// <returns>리전 정보 객체</returns>
		public async Task<ResponseData> Remove(string RegionName)
		{
			var Result = new ResponseData();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (RegionName.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 리전 정보를 가져온다.
				var Exist = await m_dbContext.Regions.AsNoTracking().FirstOrDefaultAsync(i => i.Name == RegionName);

				// 가져오는데 실패할경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 리전 삭제
						m_dbContext.Regions.Remove(Exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Code = Resource.SC_COMMON__SUCCESS;
						Result.Message = Resource.SM_COMMON__CREATED;
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

		/// <summary>리전 식별자로 특정 리전을 가져온다.</summary>
		/// <param name="RegionName">리전 식별자</param>
		/// <returns>리전 정보 객체</returns>
		public async Task<ResponseData<ResponseRegion>> Get(string RegionName)
		{
			var Result = new ResponseData<ResponseRegion>();
			try
			{
				// 리전을 가져온다.
				Result.Data = await m_dbContext.Regions.AsNoTracking()
					.Where(i => i.Name == RegionName)
					.FirstOrDefaultAsync<Region, ResponseRegion>();

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

		/// <summary>리전 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <returns>리전 목록</returns>
		public async Task<ResponseList<ResponseRegion>> Get(
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null
		)
		{
			var Result = new ResponseList<ResponseRegion>();
			try
			{
				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 리전 목록을 가져온다.
				Result.Data = await m_dbContext.Regions.AsNoTracking()
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<Region, ResponseRegion>(Skip, CountPerPage);

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
