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
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;
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
		/// <param name="userId">사용자 아이디</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (KeyName, ExpireDate, KeyValue)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드목록 (KeyName, KeyValue)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>API 키 정보 목록 객체</returns>
		public async Task<ResponseList<ResponseApiKey>> GetApiKeys(Guid userId
			, int skip = 0, int countPerPage = 100, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "")
		{
			ResponseList<ResponseApiKey> result = new ResponseList<ResponseApiKey>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (userId == Guid.Empty)
					return new ResponseList<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 사용자 정보를 가져온다.
				NNApplicationUser user = await m_userManager.FindByIdAsync(userId.ToString());
				if (user == null)
					return new ResponseList<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("ExpireDate", "desc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				// API 키 목록을 가져온다.
				result.Data = await m_dbContext.ApiKeys.AsNoTracking()
					.Where(i => i.UserId == userId
								&& (
									searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
									|| (searchFields.Contains("keyname") && i.KeyName.Contains(searchKeyword))
									|| (searchFields.Contains("keyvalue") && i.KeyValue.Contains(searchKeyword))
								)
					)
					.Select(i => new
					{
						i.KeyId,
						i.UserId,
						UserName = user.Name,
						i.KeyName,
						i.KeyValue,
						i.ExpireDate
					})
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<dynamic, ResponseApiKey>();

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

		/// <summary>해당 사용자의 API 키 정보를 가져온다.</summary>
		/// <param name="userId">사용자 아이디</param>
		/// <param name="keyId">키 아이디</param>
		/// <returns>API 키 정보 목록 객체</returns>
		public async Task<ResponseData<ResponseApiKey>> GetApiKey(Guid userId, Guid keyId)
		{
			ResponseData<ResponseApiKey> result = new ResponseData<ResponseApiKey>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (userId == Guid.Empty || keyId == Guid.Empty)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 사용자 정보를 가져온다.
				NNApplicationUser user = await m_userManager.FindByIdAsync(userId.ToString());
				if (user == null)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				// API 키 정보를 가져온다.
				ResponseApiKey exist = await m_dbContext.ApiKeys.AsNoTracking()
					.Where(i => i.KeyId == keyId && i.UserId == userId)
					.Select(i => new
					{
						i.KeyId,
						i.UserId,
						UserName = user.Name,
						i.KeyName,
						i.KeyValue,
						i.ExpireDate
					})
					.FirstOrDefaultAsync<dynamic, ResponseApiKey>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
				{
					result.Code = Resource.EC_COMMON__NOT_FOUND;
					result.Message = Resource.EM_COMMON__NOT_FOUND;
				}
				// 해당 데이터가 존재하는 경우
				else
				{
					result.Data = exist;
					result.Result = EnumResponseResult.Success;
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

		/// <summary>해당 사용자의 API 키를 발행한다.</summary>
		/// <param name="userId">사용자 아이디</param>
		/// <param name="request">키 요청 객체</param>
		/// <returns>API 키 추가 결과 객체</returns>
		public async Task<ResponseData<ResponseApiKey>> IssueApiKey(Guid userId, RequestApiKey request)
		{
			return await IssueApiKey(userId, new RequestApiKeyEx(request));
		}

		/// <summary>해당 사용자의 API 키를 키 값까지 지정하여 발행한다.</summary>
		/// <param name="userId">사용자 아이디</param>
		/// <param name="request">키 요청 객체</param>
		/// <returns>API 키 추가 결과 객체</returns>
		public async Task<ResponseData<ResponseApiKey>> IssueApiKey(Guid userId, RequestApiKeyEx request)
		{
			ResponseData<ResponseApiKey> result = new ResponseData<ResponseApiKey>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (userId == Guid.Empty)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 해당 사용자 정보를 가져온다.
				NNApplicationUser user = await m_userManager.FindByIdAsync(userId.ToString());
				if (user == null)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				if (request.KeyValue.IsEmpty())
					request.KeyValue = Guid.NewGuid().ToString().GetSHA256Hash();

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						ApiKey newData = new ApiKey()
						{
							KeyId = Guid.NewGuid(),
							UserId = userId,
							KeyName = request.KeyName,
							ExpireDate = request.ExpireDate,
							KeyValue = request.KeyValue
						};

						// API 키를 생성한다.
						await m_dbContext.ApiKeys.AddAsync(newData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Data = new ResponseApiKey();
						result.Data.CopyValueFrom(newData);
						result.Data.UserName = user.Name;
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

		/// <summary>해당 사용자의 API 키를 해제한다.</summary>
		/// <param name="userId">사용자 아이디</param>
		/// <param name="keyId">키 아이디</param>
		/// <returns>API 키 해제 결과 객체</returns>
		public async Task<ResponseData> RevokeApiKey(Guid userId, Guid keyId)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 요청이 유효하지 않은 경우
				if (userId == Guid.Empty || keyId == Guid.Empty)
				{
					result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 요청이 유효한 경우
				else
				{
					// API 키 정보를 가져온다.
					ApiKey exist = await m_dbContext.ApiKeys.AsNoTracking()
						.Where(i => i.UserId == userId && i.KeyId == keyId)
						.FirstOrDefaultAsync();

					// 해당 데이터가 존재하지 않는 경우
					if (exist == null)
					{
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
					}
					// 해당 데이터가 존재하는 경우
					else
					{
						using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
						{
							try
							{
								// 해당 데이터 삭제
								m_dbContext.ApiKeys.Remove(exist);
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

		/// <summary>해당 키 아이디의 API 키 정보를 가져온다.</summary>
		/// <param name="keyValue">키 값</param>
		/// <returns>API 키 정보 목록 객체</returns>
		public async Task<ResponseData<ResponseApiKey>> GetApiKey(string keyValue)
		{
			ResponseData<ResponseApiKey> result = new ResponseData<ResponseApiKey>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (keyValue.IsEmpty())
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// API 키 정보를 가져온다.
				ResponseApiKey exist = await m_dbContext.ApiKeys.AsNoTracking()
					.Where(i => i.KeyValue == keyValue && i.ExpireDate >= DateTime.Now)
					.FirstOrDefaultAsync<ApiKey, ResponseApiKey>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<ResponseApiKey>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				result.Data = exist;

				// 해당 사용자 정보를 가져온다.
				NNApplicationUser user = await m_userManager.FindByIdAsync(exist.UserId);
				if (user != null)
					result.Data.UserName = user.Name;

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