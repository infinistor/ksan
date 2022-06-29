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
using System.Linq;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
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
using PortalData.Responses.Configs;
using PortalData.Requests.Configs;
using PortalProvider.Providers.RabbitMq;

namespace PortalProvider.Providers.Services
{
	/// <summary>서비스 데이터 프로바이더 클래스</summary>
	public class ConfigProvider : BaseProvider<PortalModel>, IConfigProvider
	{
		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public ConfigProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<ConfigProvider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		/// <summary>서비스의 설정 정보 목록을 가져온다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <returns>설정 정보가 포함된 결과 목록 객체</returns>
		public async Task<ResponseList<ResponseServiceConfig>> GetConfigList(EnumServiceType ServiceType)
		{
			var Result = new ResponseList<ResponseServiceConfig>();

			try
			{
				// 해당 정보를 가져온다.
				Result.Data = await m_dbContext.ServiceConfigs.AsNoTracking()
					.Where(i => i.Type == (EnumDbServiceType)ServiceType)
					.OrderByDescending(i => i.Version)
					.CreateListAsync<ServiceConfig, ResponseServiceConfig>();

				// 해당 데이터가 존재하지 않는 경우
				if (Result.Data == null)
					return new ResponseList<ResponseServiceConfig>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스의 최신 설정 정보를 가져온다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <returns>설정 정보가 포함된 결과 객체</returns>
		public async Task<ResponseData<ResponseServiceConfig>> GetConfig(EnumServiceType ServiceType)
		{
			var Result = new ResponseData<ResponseServiceConfig>();

			try
			{
				// 해당 정보를 가져온다.
				Result.Data = await m_dbContext.ServiceConfigs.AsNoTracking()
					.Where(i => i.Type == (EnumDbServiceType)ServiceType && i.LastVersion == true)
					.OrderByDescending(i => i.Version)
					.FirstOrDefaultAsync<ServiceConfig, ResponseServiceConfig>();

				// 해당 데이터가 존재하지 않는 경우
				if (Result.Data == null)
					return new ResponseData<ResponseServiceConfig>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				Result.Result = EnumResponseResult.Success;

			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary> 특정 버전의 서비스 설정 정보를 가져온다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Version">서비스 버전</param>
		/// <returns>설정 정보가 포함된 결과 객체</returns>
		public async Task<ResponseData<ResponseServiceConfig>> GetConfig(EnumServiceType ServiceType, int Version)
		{
			var Result = new ResponseData<ResponseServiceConfig>();

			try
			{
				// 해당 정보를 가져온다.
				Result.Data = await m_dbContext.ServiceConfigs.AsNoTracking()
					.Where(i => i.Type == (EnumDbServiceType)ServiceType && i.Version == Version)
					.FirstOrDefaultAsync<ServiceConfig, ResponseServiceConfig>();

				// 해당 데이터가 존재하지 않는 경우
				if (Result.Data == null)
					return new ResponseData<ResponseServiceConfig>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				Result.Result = EnumResponseResult.Success;

			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>주어진 설정 정보를 특정 서비스에 저장한다.</summary>
		/// <param name="Request">서비스 설정 객체</param>
		/// <returns>설정 결과 객체</returns>
		public async Task<ResponseData<ResponseUpdateConfig>> SetConfig(RequestServiceConfig Request)
		{
			var Result = new ResponseData<ResponseUpdateConfig>();

			try
			{
				// 설정이 유효하지 않은 경우
				if (string.IsNullOrEmpty(Request.Config))
					return new ResponseData<ResponseUpdateConfig>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);


				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						var NewData = new ServiceConfig()
						{
							Type = (EnumDbServiceType)Request.Type,
							Config = Request.Config,
							RegDate = DateTime.Now,
							LastVersion = false,
						};

						// 저장
						await m_dbContext.ServiceConfigs.AddAsync(NewData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						Transaction.Rollback();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}

			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스 설정을 특정 버전으로 한다.</summary
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Version">서비스 버전</param>
		/// <returns>설정 결과 객체</returns>
		public async Task<ResponseData<ResponseUpdateConfig>> SetConfigLastVersion(EnumServiceType ServiceType, int Version)
		{
			var Result = new ResponseData<ResponseUpdateConfig>();

			try
			{
				// 해당 버전이 존재하는지 확인
				var MyVersion = await m_dbContext.ServiceConfigs.AsNoTracking().FirstOrDefaultAsync(i => i.Type == (EnumDbServiceType)ServiceType && i.Version == Version);

				// 해당 버전이 존재하지 않을 경우
				if (MyVersion == null)
					return new ResponseData<ResponseUpdateConfig>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 해당 버전이 최신 버전일 경우
				if (MyVersion.LastVersion)
					return new ResponseData<ResponseUpdateConfig>(EnumResponseResult.Error, Resource.EC_COMMON__FAIL_TO_UPDATE, Resource.EM_CONFIGS_ALREADY_LAST_VERSION);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 입력된 버전을 제외한 나머지 버전을 이전버전으로 변경
						var Items = await m_dbContext.ServiceConfigs.Where(i => i.Type == (EnumDbServiceType)ServiceType).CreateListAsync();
						foreach (var Item in Items.Items)
						{
							if (Item.Version == Version) Item.LastVersion = true;
							else Item.LastVersion = false;
						}

						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						await Transaction.CommitAsync();

						var NewData = new ResponseUpdateConfig()
						{
							RegDate = MyVersion.RegDate,
							Version = MyVersion.Version,
						};

						Result.Data = NewData;
						Result.Result = EnumResponseResult.Success;

						// Config 변경 알림
						SendMq(RabbitMqConfiguration.ExchangeName, $"*.services.{ServiceType.ToString().ToLower()}.config.updated", NewData);
					}
					catch (Exception ex)
					{
						Transaction.Rollback();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}

			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>서비스의 설정 정보를 제거한다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Version">서비스 버전</param>
		/// <returns>삭제 결과 객체</returns>
		public async Task<ResponseData> RemoveConfig(EnumServiceType ServiceType, int Version)
		{
			var Result = new ResponseData();

			try
			{
				// 해당 정보를 가져온다.
				var MyVersion = await m_dbContext.ServiceConfigs.AsNoTracking().FirstOrDefaultAsync(i => i.Type == (EnumDbServiceType)ServiceType && i.Version == Version);

				// 해당 정보가 존재하지 않는 경우
				if (MyVersion == null) return new ResponseData(EnumResponseResult.Success);

				// 해당 버전이 최신버전일 경우 삭제 불가
				if (MyVersion.LastVersion) return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__FAIL_TO_DELETE_MAY_BE_IN_USE, Resource.EM_CONFIGS_LIST_VERSION_CANNOT_DELETE);

				using (var transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 해당 데이터 삭제
						m_dbContext.ServiceConfigs.Remove(MyVersion);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 저장
						await transaction.CommitAsync();
						Result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						transaction.Rollback();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}

			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}
	}
}