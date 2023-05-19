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
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;
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

namespace PortalProvider.Providers.Accounts
{
	/// <summary>API 키 프로바이더 클래스</summary>
	public class ApiKeyProvider : BaseProvider<PortalModel>, IApiKeyProvider
	{
		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public ApiKeyProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<ApiKeyProvider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		/// <summary>해당 사용자의 API 키 정보 목록을 가져온다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (KeyName, ExpireDate, KeyValue)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드목록 (KeyName, KeyValue)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>API 키 정보 목록 객체</returns>
		public async Task<ResponseList<ResponseApiKey>> GetApiKeys(Guid UserId
			, int Skip = 0, int CountPerPage = 100, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "")
		{
			var Result = new ResponseList<ResponseApiKey>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (UserId == Guid.Empty)
					return new ResponseList<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 사용자 정보를 가져온다.
				var User = await m_userManager.FindByIdAsync(UserId.ToString());
				if (User == null)
					return new ResponseList<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("ExpireDate", "desc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				// API 키 목록을 가져온다.
				Result.Data = await m_dbContext.ApiKeys.AsNoTracking()
					.Where(i => i.UserId == UserId
								&& (
									SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
									|| (SearchFields.Contains("keyname") && i.KeyName.Contains(SearchKeyword))
									|| (SearchFields.Contains("keyvalue") && i.KeyValue.Contains(SearchKeyword))
								)
					)
					.Select(i => new
					{
						i.KeyId,
						i.UserId,
						UserName = User.Name,
						i.KeyName,
						i.KeyValue,
						i.ExpireDate
					})
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<dynamic, ResponseApiKey>();

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

		/// <summary>해당 사용자의 API 키 정보를 가져온다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="KeyId">키 아이디</param>
		/// <returns>API 키 정보 목록 객체</returns>
		public async Task<ResponseData<ResponseApiKey>> GetApiKey(Guid UserId, Guid KeyId)
		{
			var Result = new ResponseData<ResponseApiKey>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (UserId == Guid.Empty || KeyId == Guid.Empty)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 사용자 정보를 가져온다.
				var User = await m_userManager.FindByIdAsync(UserId.ToString());
				if (User == null)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				// API 키 정보를 가져온다.
				var Exist = await m_dbContext.ApiKeys.AsNoTracking()
					.Where(i => i.KeyId == KeyId && i.UserId == UserId)
					.Select(i => new
					{
						i.KeyId,
						i.UserId,
						UserName = User.Name,
						i.KeyName,
						i.KeyValue,
						i.ExpireDate
					})
					.FirstOrDefaultAsync<dynamic, ResponseApiKey>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);
				// 해당 데이터가 존재하는 경우
				else
				{
					Result.Data = Exist;
					Result.Result = EnumResponseResult.Success;
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

		/// <summary>해당 사용자의 API 키를 발행한다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="Request">키 요청 객체</param>
		/// <returns>API 키 추가 결과 객체</returns>
		public async Task<ResponseData<ResponseApiKey>> IssueApiKey(Guid UserId, RequestApiKey Request)
		{
			return await IssueApiKey(UserId, new RequestApiKeyEx(Request));
		}

		/// <summary>해당 사용자의 API 키를 키 값까지 지정하여 발행한다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="Request">키 요청 객체</param>
		/// <returns>API 키 추가 결과 객체</returns>
		public async Task<ResponseData<ResponseApiKey>> IssueApiKey(Guid UserId, RequestApiKeyEx Request)
		{
			var Result = new ResponseData<ResponseApiKey>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (UserId == Guid.Empty)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 해당 사용자 정보를 가져온다.
				var User = await m_userManager.FindByIdAsync(UserId.ToString());
				if (User == null)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				if (Request.KeyValue.IsEmpty())
					Request.KeyValue = Guid.NewGuid().ToString().GetSHA256Hash();

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						var NewData = new ApiKey()
						{
							KeyId = Guid.NewGuid(),
							UserId = UserId,
							KeyName = Request.KeyName,
							ExpireDate = Request.ExpireDate,
							KeyValue = Request.KeyValue
						};

						// API 키를 생성한다.
						await m_dbContext.ApiKeys.AddAsync(NewData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Data = new ResponseApiKey();
						Result.Data.CopyValueFrom(NewData);
						Result.Data.UserName = User.Name;
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

		/// <summary>해당 사용자의 API 키를 해제한다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="KeyId">키 아이디</param>
		/// <returns>API 키 해제 결과 객체</returns>
		public async Task<ResponseData> RevokeApiKey(Guid UserId, Guid KeyId)
		{
			var Result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (UserId == Guid.Empty || KeyId == Guid.Empty)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 요청이 유효한 경우
				else
				{
					// API 키 정보를 가져온다.
					var Exist = await m_dbContext.ApiKeys.AsNoTracking()
						.Where(i => i.UserId == UserId && i.KeyId == KeyId)
						.FirstOrDefaultAsync();

					// 해당 데이터가 존재하지 않는 경우
					if (Exist == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

					// 해당 데이터가 존재하는 경우
					using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
					{
						try
						{
							// 해당 데이터 삭제
							m_dbContext.ApiKeys.Remove(Exist);
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
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>해당 키 아이디의 API 키 정보를 가져온다.</summary>
		/// <param name="KeyValue">키 값</param>
		/// <returns>API 키 정보 목록 객체</returns>
		public async Task<ResponseData<ResponseApiKey>> GetApiKey(string KeyValue)
		{
			var Result = new ResponseData<ResponseApiKey>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (KeyValue.IsEmpty())
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// API 키 정보를 가져온다.
				var Exist = await m_dbContext.ApiKeys.AsNoTracking()
					.Where(i => i.KeyValue == KeyValue && i.ExpireDate >= DateTime.Now)
					.FirstOrDefaultAsync<ApiKey, ResponseApiKey>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 정보를 저장한다.
				Result.Data = Exist;
				Result.Result = EnumResponseResult.Success;

				// 해당 사용자 정보를 가져온다.
				var User = await m_userManager.FindByIdAsync(Exist.UserId);
				if (User != null)
					Result.Data.UserName = User.Name;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary> 메인키의 정보를 가져온다. </summary>
		/// <returns> API 키 객체 </returns>
		public async Task<ResponseApiKey> GetMainApiKey()
		{
			try
			{
				// API 키 정보를 가져온다.
				var Exist = await m_dbContext.ApiKeys.AsNoTracking()
					.Where(i => i.KeyName == Resource.INTERNAL_SERVICE_API_KEY)
					.FirstOrDefaultAsync<ApiKey, ResponseApiKey>();

				// 해당 데이터가 존재하는 경우
				return Exist;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return null;
		}
	}
}