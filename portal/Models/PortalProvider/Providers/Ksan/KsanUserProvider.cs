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
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Ksan;
using PortalData.Responses.Ksan;
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
using PortalProvider.Providers.RabbitMq;

namespace PortalProvider.Providers.Accounts
{
	/// <summary>Ksan 사용자 프로바이더 클래스</summary>
	public class KsanUserProvider : BaseProvider<PortalModel>, IKsanUserProvider
	{
		/// <summary>역할 매니져</summary>
		protected readonly RoleManager<NNApplicationRole> m_roleManager;

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		/// <param name="roleManager">역할 관리자</param>
		public KsanUserProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<KsanUserProvider> logger,
			RoleManager<NNApplicationRole> roleManager
			)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
			m_roleManager = roleManager;
		}

		/// <summary>Ksan 사용자를 추가한다.</summary>
		/// <param name="Request">Ksan 사용자 정보</param>
		/// <returns>Ksan 사용자 등록 결과</returns>
		public async Task<ResponseData<ResponseKsanUser>> Add(RequestKsanUser Request)
		{
			var Result = new ResponseData<ResponseKsanUser>();

			try
			{
				// 사용자명 유효하지 않을 경우
				if (Request.Name.IsEmpty() || !IdChecker.IsMatch(Request.Name))
					return new ResponseData<ResponseKsanUser>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON_ACCOUNT_REQUIRE_NAME);

				// 이름 중복 검사
				var responseDuplicatedName = await CheckUserNameDuplicated(Request.Name);

				// 중복검사 실패시
				if (responseDuplicatedName.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseKsanUser>(EnumResponseResult.Error, responseDuplicatedName.Code, responseDuplicatedName.Message);

				//이메일 체크
				string Email = null;

				//이메일을 설정했다면
				if (!Request.Email.IsEmpty())
				{
					// 이메일 중복 검사
					var responseDuplicatedEmail = await CheckEmailDuplicated(Request.Email);

					// 이메일이 중복된 경우
					if (responseDuplicatedEmail.Result != EnumResponseResult.Success)
						return new ResponseData<ResponseKsanUser>(EnumResponseResult.Error, responseDuplicatedEmail.Code, responseDuplicatedEmail.Message);
					Email = Request.Email;
				}

				// 기본 디스크풀 ID가 유효하지 않을 경우
				if (Request.StandardDiskPoolId.IsEmpty() || !Guid.TryParse(Request.StandardDiskPoolId, out Guid DiskPoolId))
					return new ResponseData<ResponseKsanUser>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 디스크풀 정보를 가져온다.
				var Exist = await m_dbContext.DiskPools.AsNoTracking().FirstOrDefaultAsync(i => i.Id == DiskPoolId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseKsanUser>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 요청이 유효한 경우 Ksan 사용자 객체를 생성한다.
				var User = new KsanUser
				{
					Id = Guid.NewGuid(),
					Name = Request.Name,
					Email = Email,
					AccessKey = RandomText(ACCESS_KEY_LENGTH),
					SecretKey = RandomTextLong(SECRET_KEY_LENGTH),
				};

				// Ksan 사용자 등록
				await m_dbContext.KsanUsers.AddAsync(User);

				// 기본 스토리지 클래스 등록
				var StorageClass = new UserDiskPool { UserId = User.Id, DiskPoolId = DiskPoolId, StorageClass = Resource.UL_DISKPOOL_DEFAULT_STANDARD_DISKPOOL_NAME };
				await m_dbContext.UserDiskPools.AddAsync(StorageClass);
				await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

				Result.Data = await m_dbContext.KsanUsers.AsNoTracking().Where(i => i.Id == User.Id)
					.Include(i => i.UserDiskPools)
					.FirstOrDefaultAsync<KsanUser, ResponseKsanUser>();

				Result.Result = EnumResponseResult.Success;
				Result.Data = new ResponseKsanUser();
				Result.Data.CopyValueFrom(User);

				// Ksan 사용자 등록 알림
				SendMq(RabbitMqConfiguration.ExchangeName, "*.services.s3.user.added", Result.Data);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>Ksan 사용자를 수정한다.</summary>
		/// <param name="Id">Ksan 사용자 식별자</param>
		/// <param name="Request">Ksan 사용자 정보</param>
		/// <returns>Ksan 사용자 수정 결과</returns>
		public async Task<ResponseData> Update(string Id, RequestKsanUserUpdate Request)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 사용자명이 없는 경우
				if (Request.Name.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON_ACCOUNT_REQUIRE_NAME);

				// 이름으로 조회할 경우
				if (!Guid.TryParse(Id, out Guid GuidId))
				{
					var KsanUser = await m_dbContext.KsanUsers.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

					//서비스가 존재하지 않는 경우
					if (KsanUser == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

					GuidId = KsanUser.Id;
				}

				// 사용자 계정을 가져온다.
				var User = await m_dbContext.KsanUsers.Where(i => i.Id == GuidId).FirstOrDefaultAsync();

				// 해당 계정을 찾을 수 없는 경우
				if (User == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				//이름이 변경되었을 경우 중복검사
				if (!User.Name.Equals(Request.Name))
				{
					// 이름 중복 검사
					ResponseData responseDuplicatedName = await CheckUserNameDuplicated(Request.Name);

					// 이름이 중복된 경우
					if (responseDuplicatedName.Result != EnumResponseResult.Success)
						return new ResponseData(EnumResponseResult.Error, responseDuplicatedName.Code, responseDuplicatedName.Message);

					// 사용자명 변경
					User.Name = Request.Name;
				}

				//이메일이 변경되었을 경우 중복검사
				if (!User.Email.Equals(Request.Email))
				{

					//이메일을 제거하려고 하는 경우
					if (Request.Email.IsEmpty())
					{
						User.Email = null;
					}
					else
					{
						// 이메일 중복 검사
						ResponseData responseDuplicatedEmail = await CheckEmailDuplicated(Request.Email);

						// 이메일이 중복된 경우
						if (responseDuplicatedEmail.Result != EnumResponseResult.Success)
							return new ResponseData(EnumResponseResult.Error, responseDuplicatedEmail.Code, responseDuplicatedEmail.Message);

						User.Email = Request.Email;
					}
				}

				//설정 적용
				if (m_dbContext.HasChanges())
					await this.m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

				Result.Result = EnumResponseResult.Success;
				Result.Code = Resource.SC_COMMON__SUCCESS;
				Result.Message = Resource.SM_COMMON__UPDATED;

				//Ksan 사용자 변경 알림
				var responseKsanUser = new ResponseKsanUser();
				responseKsanUser.CopyValueFrom(User);
				SendMq(RabbitMqConfiguration.ExchangeName, "*.services.s3.user.updated", responseKsanUser);
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

		/// <summary>Ksan 사용자를 삭제한다.</summary>
		/// <param name="Id">Ksan 사용자 식별자</param>
		/// <returns>Ksan 사용자 삭제 결과</returns>
		public async Task<ResponseData> Remove(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않을 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 사용자 계정을 가져온다.
				var User = await m_dbContext.KsanUsers.AsNoTracking().Where(i => i.Id == guidId).FirstOrDefaultAsync();

				// 해당 계정을 찾을 수 없는 경우
				if (User == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				// 계정 삭제
				m_dbContext.KsanUsers.Remove(User);
				await this.m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
				Result.Result = EnumResponseResult.Success;

				//Ksan 사용자 삭제 알림
				var responseKsanUser = new ResponseKsanUser();
				responseKsanUser.CopyValueFrom(User);
				SendMq(RabbitMqConfiguration.ExchangeName, "*.services.s3.user.removed", responseKsanUser);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>전체 사용자를 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드목록 (Id, Email, Name)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>Ksan 사용자 목록</returns>
		public async Task<ResponseList<ResponseKsanUser>> GetUsers(int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = "")
		{
			ResponseList<ResponseKsanUser> Result = new ResponseList<ResponseKsanUser>();

			try
			{

				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				Result.Data = await m_dbContext.KsanUsers.AsNoTracking().Where(i =>
						SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
						|| (SearchFields.Contains("Email") && i.Email.Contains(SearchKeyword))
						|| (SearchFields.Contains("name") && i.Name.Contains(SearchKeyword)))
					.OrderByWithDirection(OrderFields, OrderDirections)
					.Include(i => i.UserDiskPools)
					.CreateListAsync<KsanUser, ResponseKsanUser>(Skip, CountPerPage);

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

		/// <summary>Ksan 사용자 이름 중복 여부를 검사한다.</summary>
		/// <param name="userName">Ksan 사용자 이름</param>
		/// <returns>검사 결과 객체</returns>
		public async Task<ResponseData> CheckUserNameDuplicated(string userName)
		{
			var Result = new ResponseData();
			try
			{
				// 요청이 유효하지 않을 경우
				if (userName.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON_ACCOUNT_REQUIRE_NAME;
					Result.Result = EnumResponseResult.Error;
				}
				// 요청이 유효한 경우
				else
				{
					// 해당 사용자 이름이 존재하는 경우
					if (await m_dbContext.KsanUsers.AsNoTracking().Where(i => i.Name == userName).AnyAsync())
					{
						Result.Code = Resource.EC_COMMON__DUPLICATED_DATA;
						Result.Message = Resource.EM_COMMON_NAME_ALREADY_EXIST;
						Result.Result = EnumResponseResult.Error;
					}
					// 존재하지 않을 경우
					else Result.Result = EnumResponseResult.Success;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
				Result.Result = EnumResponseResult.Error;
			}
			return Result;
		}

		/// <summary>이메일 중복 여부를 검사한다.</summary>
		/// <param name="Email">이메일</param>
		/// <returns>검사 결과 객체</returns>
		public async Task<ResponseData> CheckEmailDuplicated(string Email)
		{
			var Result = new ResponseData();
			try
			{
				// 요청이 유효하지 않을 경우
				if (Email.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON_ACCOUNT_REQUIRE_EMAIL;
					Result.Result = EnumResponseResult.Error;
				}
				// 이메일 주소의 형식이 올바르지 않을 경우
				else if (!EmailChecker.IsMatch(Email))
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON_ACCOUNT_INVALID_EMAIL;
					Result.Result = EnumResponseResult.Error;
				}
				// 요청이 유효한 경우
				else
				{
					// Ksan 사용자중 해당 이메일이 존재하는 경우
					if (await m_dbContext.KsanUsers.AsNoTracking().Where(i => i.Email == Email).AnyAsync())
					{
						Result.Code = Resource.EC_COMMON__DUPLICATED_DATA;
						Result.Message = Resource.EM_COMMON_ACCOUNT_ALREADY_AUTH_EMAIL;
						Result.Result = EnumResponseResult.Error;
					}
					// 존재하지 않을 경우
					else Result.Result = EnumResponseResult.Success;
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

		/// <summary>특정 사용자 식별자에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="includeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		public async Task<ResponseData<ResponseKsanUser>> GetUserById(string id)
		{
			var Result = new ResponseData<ResponseKsanUser>();
			try
			{
				// 아이디가 유효하지 않을 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid GuidId))
					return new ResponseData<ResponseKsanUser>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 사용자 ID로 해당 사용자 정보를 반환한다.
				var User = await m_dbContext.KsanUsers
					.AsNoTracking()
					.Where(i => i.Id == GuidId)
					.Include(i => i.UserDiskPools)
					.FirstOrDefaultAsync<KsanUser, ResponseKsanUser>();

				// 해당 계정을 찾을 수 없는 경우
				if (User == null)
				{
					Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
					Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					Result.Result = EnumResponseResult.Error;
				}
				else
				{
					Result.Data = User;
					Result.Result = EnumResponseResult.Success;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
				Result.Result = EnumResponseResult.Error;
			}
			return Result;
		}

		/// <summary>특정 사용자 식별자에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <returns>사용자 정보 객체</returns>
		public async Task<ResponseData<ResponseKsanUser>> GetUserByName(string Name)
		{
			var Result = new ResponseData<ResponseKsanUser>();
			try
			{
				// 사용자 식별자가 유효하지 않을 경우
				if (Name.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON_ACCOUNT_REQUIRE_NAME;
					Result.Result = EnumResponseResult.Error;
				}

				// 사용자 이름으로 해당 사용자 정보를 반환한다.
				var User = await m_dbContext.KsanUsers.AsNoTracking().Where(i => i.Name == Name).Include(i => i.UserDiskPools).FirstOrDefaultAsync<KsanUser, ResponseKsanUser>();

				// 해당 계정을 찾을 수 없는 경우
				if (User == null)
				{
					Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
					Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					Result.Result = EnumResponseResult.Error;
				}
				else
				{
					Result.Data = User;
					Result.Result = EnumResponseResult.Success;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
				Result.Result = EnumResponseResult.Error;
			}
			return Result;
		}

		/// <summary>특정 로그인 이메일에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="Email">이메일 주소</param>
		/// <returns>사용자 정보 객체</returns>
		public async Task<ResponseData<ResponseKsanUser>> GetUserByEmail(string Email)
		{
			var Result = new ResponseData<ResponseKsanUser>();
			try
			{
				// 사용자 식별자가 유효하지 않을 경우
				if (Email.IsEmpty())
					return new ResponseData<ResponseKsanUser>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON_ACCOUNT_REQUIRE_EMAIL);
				// 사용자 이메일로 해당 사용자 정보를 반환한다.
				var User = await m_dbContext.KsanUsers.AsNoTracking().Where(i => i.Email == Email).FirstOrDefaultAsync<KsanUser, ResponseKsanUser>();

				// 해당 계정을 찾을 수 없는 경우
				if (User == null)
				{
					Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
					Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					Result.Result = EnumResponseResult.Error;
				}
				else
				{
					Result.Data = User;
					Result.Result = EnumResponseResult.Success;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
				Result.Result = EnumResponseResult.Error;
			}
			return Result;
		}

		/// <summary>스토리지 클래스명 중복 여부를 검사한다.</summary>
		/// <param name="UserId">사용자 아이디</param>
		/// <param name="DiskPoolId"> 디스크풀 아이디 </param>
		/// <param name="StorageClass"> 스토리지 클래스 명 </param>
		public async Task<bool> CheckStorageClassDuplicated(Guid UserId, Guid DiskPoolId, string StorageClass)
		{
			try
			{
				// 중복 여부를 확인한다.
				var Data = await m_dbContext.UserDiskPools.AsNoTracking()
					.Where(i => i.UserId == UserId && i.DiskPoolId == DiskPoolId && i.StorageClass == StorageClass)
					.FirstOrDefaultAsync();

				if (Data == null) return false;
				return true;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
				return false;
			}
		}

		/// <summary>유저의 스토리지 클래스 정보를 추가한다.</summary>
		/// <param name="Request">유저 스토리지 클래스 추가 객체</param>
		/// <returns>스토리지 클래스 추가 결과</returns>
		public async Task<ResponseData> AddStorageClass(RequestStorageClass Request)
		{
			var Result = new ResponseData();
			try
			{
				// 유저 아이디가 유효하지 않을 경우
				if (Request.UserId.IsEmpty() || !Guid.TryParse(Request.UserId, out Guid UserId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON_ACCOUNT_INVALID_USERID);

				// 디스크풀 아이디가 유효하지 않을 경우
				if (Request.DiskPoolId.IsEmpty() || !Guid.TryParse(Request.DiskPoolId, out Guid DiskPoolId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EN_DISK_POOLS_INVALID_ID);

				// 스토리지 클래스가 유효하지 않을 경우
				if (Request.StorageClass.IsEmpty() || Resource.UL_DISKPOOL_DEFAULT_STANDARD_DISKPOOL_NAME.Equals(Request.StorageClass, StringComparison.OrdinalIgnoreCase))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_STORAGE_CLASS_INVALID_NAME);

				// 중복 체크
				if (await CheckStorageClassDuplicated(UserId, DiskPoolId, Request.StorageClass))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_STORAGE_CLASS_ALREADY_EXIST);

				// 사용자 계정을 가져온다.
				var User = await m_dbContext.KsanUsers.AsNoTracking().Where(i => i.Id == UserId).FirstOrDefaultAsync();

				// 해당 계정을 찾을 수 없는 경우
				if (User == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				// 디스크풀 정보를 가져온다.
				var DiskPool = await m_dbContext.DiskPools.AsNoTracking().Where(i => i.Id == DiskPoolId).FirstOrDefaultAsync();

				// 디스크풀 정보를 찾을 수 없는 경우
				if (DiskPool == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EN_DISK_POOLS_INVALID_ID);

				// 유저에 스토리지 클래스 추가
				var NewData = new UserDiskPool { UserId = UserId, DiskPoolId = DiskPoolId, StorageClass = Request.StorageClass };
				await m_dbContext.UserDiskPools.AddAsync(NewData);
				await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

				Result.Result = EnumResponseResult.Success;
				Result.Code = Resource.SC_COMMON__SUCCESS;
				Result.Message = Resource.SM_COMMON__UPDATED;

				//Ksan 사용자 변경 알림
				User.UserDiskPools.Add(NewData);
				SendMq(RabbitMqConfiguration.ExchangeName, "*.services.s3.user.updated", User);
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

		/// <summary> 유저의 스토리지 클래스 정보를 삭제한다.</summary>
		/// <param name="Request"> 유저 스토리지 클래스 삭제 객체</param>
		/// <returns> 유저 스토리지 클래스 삭제 결과 </returns>
		public async Task<ResponseData> RemoveStorageClass(RequestStorageClass Request)
		{
			var Result = new ResponseData();
			try
			{
				// 유저 아이디가 유효하지 않을 경우
				if (Request.UserId.IsEmpty() || !Guid.TryParse(Request.UserId, out Guid UserId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON_ACCOUNT_INVALID_USERID);

				// 디스크풀 아이디가 유효하지 않을 경우
				if (Request.DiskPoolId.IsEmpty() || !Guid.TryParse(Request.DiskPoolId, out Guid DiskPoolId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EN_DISK_POOLS_INVALID_ID);

				// 스토리지 클래스가 유효하지 않을 경우
				if (Request.StorageClass.IsEmpty() || Resource.UL_DISKPOOL_DEFAULT_STANDARD_DISKPOOL_NAME.Equals(Request.StorageClass, StringComparison.OrdinalIgnoreCase))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_STORAGE_CLASS_INVALID_NAME);

				// 유저의 스토리지 클래스 조회
				var Data = await m_dbContext.UserDiskPools
					.AsNoTracking()
					.FirstOrDefaultAsync(i => i.UserId == UserId && i.DiskPoolId == DiskPoolId && i.StorageClass == Request.StorageClass);

				// 스토리지 클래스가 존재할 경우에만 삭제
				if (Data == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__FAIL_TO_DELETE, Resource.EC_COMMON__NOT_FOUND);

				// 스토리지 클래스 삭제
				m_dbContext.UserDiskPools.Remove(Data);
				await this.m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

				//Ksan 사용자 변경 알림
				var User = await m_dbContext.KsanUsers.AsNoTracking().Where(i => i.Id == UserId).FirstOrDefaultAsync<KsanUser, ResponseKsanUser>();
				SendMq(RabbitMqConfiguration.ExchangeName, "*.services.s3.user.updated", User);

				Result.Result = EnumResponseResult.Success;
				Result.Code = Resource.SC_COMMON__SUCCESS;
				Result.Message = Resource.SM_COMMON__UPDATED;


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

		/************************************************************************************************************/
		protected readonly Regex IdChecker = new Regex(@"^[0-9a-zA-Z가-힣]{1,}$");
		protected readonly Regex EmailChecker = new Regex(@"^([0-9a-zA-Z]+)@([0-9a-zA-Z]+)(\.[0-9a-zA-Z]+){1,}$");
		protected readonly int ACCESS_KEY_LENGTH = 20;
		protected readonly int SECRET_KEY_LENGTH = 40;
		protected readonly char[] TEXT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".ToCharArray();
		protected readonly char[] TEXT_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".ToCharArray();
		/// <summary>랜덤한 문자열(대문자+숫자)을 생성한다.</summary>
		/// <param name="Length">문자열 길이</param>
		/// <returns>생성한 문자열</returns>
		protected string RandomText(int Length)
		{
			var rand = new Random();
			var chars = Enumerable.Range(0, Length).Select(x => TEXT[rand.Next(0, TEXT.Length)]);
			return new string(chars.ToArray());
		}

		/// <summary>랜덤한 문자열(대문자+소문자+숫자)을 생성한다.</summary>
		/// <param name="Length">문자열 길이</param>
		/// <returns>생성한 문자열</returns>
		protected string RandomTextLong(int Length)
		{
			var rand = new Random();
			var chars = Enumerable.Range(0, Length).Select(x => TEXT_STRING[rand.Next(0, TEXT_STRING.Length)]);
			return new string(chars.ToArray());
		}
	}
}