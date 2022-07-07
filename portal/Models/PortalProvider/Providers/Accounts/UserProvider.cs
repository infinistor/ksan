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
using System.Security.Claims;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
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

namespace PortalProvider.Providers.Accounts
{
	/// <summary>사용자 프로바이더 클래스</summary>
	public class UserProvider : BaseProvider<PortalModel>, IUserProvider
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
		public UserProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<UserProvider> logger,
			RoleManager<NNApplicationRole> roleManager
			)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
			m_roleManager = roleManager;
		}

		/// <summary>사용자를 추가한다.</summary>
		/// <param name="Request">사용자 정보</param>
		/// <param name="Password">비밀번호 (옵션)</param>
		/// <param name="ConfirmPassword">확인 비밀번호 (옵션)</param>
		/// <returns>사용자 등록 결과</returns>
		public async Task<ResponseData<ResponseUserWithRoles>> Add(RequestUserRegist Request, string Password = "", string ConfirmPassword = "")
		{
			var Result = new ResponseData<ResponseUserWithRoles>();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseUserWithRoles>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 비밀번호가 유효하고, 확인 비밀번호와 일치하지 않는 경우
				if (!Password.IsEmpty() && Password != ConfirmPassword)
					return new ResponseData<ResponseUserWithRoles>(EnumResponseResult.Error, Resource.EM_COMMON_ACCOUNT_INVALID_PASSWORD, Resource.EM_COMMON_ACCOUNT_PASSWORD_AND_CONFIRM_PASSWORD_DO_NOT_MATCH);

				// 사용자명이 없는 경우, 사용자 아이디를 사용자명으로 설정
				if (Request.Name.IsEmpty())
					Request.Name = Request.Email;

				// 해당 계정 객체를 생성한다.
				var User = new NNApplicationUser { Id = Guid.NewGuid(), LoginId = Request.LoginId, Email = Request.Email, Name = Request.Name, Code = Request.Code };

				// 이메일 확인 중인 경우
				if (Request.Status == EnumUserStatus.VerifyingEmail)
					// 이메일 미확인으로 설정
					User.EmailConfirmed = false;

				IdentityResult IdentityResult;

				// 비밀번호가 있는 경우
				if (!Password.IsEmpty())
					IdentityResult = await m_userManager.CreateAsync(User, Password);
				// 비밀번호가 없는 경우
				else
					IdentityResult = await m_userManager.CreateAsync(User);

				// 계정 생성에 성공한 경우
				if (IdentityResult.Succeeded)
				{
					// 비밀번호가 존재하는 경우
					if (!Password.IsEmpty())
					{
						// 비밀번호 변경 일시를 지금으로 저장
						User.PasswordChangeDate = DateTime.Now;
						await m_userManager.UpdateAsync(User);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
					}

					// 사용자 상태에 따라서 처리
					switch (Request.Status)
					{
						// 잠김인 경우
						case EnumUserStatus.Locked:
							{
								// 잠금 일시를 100년 뒤까지로 수정
								await m_userManager.SetLockoutEndDateAsync(User, DateTime.Now.AddYears(100));
							}
							break;
						case EnumUserStatus.VerifyingEmail:
							{
								// 잠금 상태 해제
								await m_userManager.SetLockoutEndDateAsync(User, null);
							}
							break;
						case EnumUserStatus.Activated:
							{
								// 확인 메일 발송을 위한 토큰을 가져온다.
								string mailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(User);
								// 이메일 확인 처리
								await m_userManager.ConfirmEmailAsync(User, mailConfirmToken);

								// 잠금 상태 해제
								await m_userManager.SetLockoutEndDateAsync(User, null);
							}
							break;
					}

					// 역할을 등록한다.
					if (Request.Roles != null && Request.Roles.Count > 0)
						await m_userManager.AddToRolesAsync(User, Request.Roles);

					// 리턴할 정보 저장
					Result.Data = new ResponseUserWithRoles();
					Result.Data.Id = User.Id.ToString();
					Result.Data.Email = Request.Email;
					Result.Data.Name = Request.Name;
					Result.Data.Code = Request.Code;
					if (Request.Roles != null)
						Result.Data.Roles.AddRange(Request.Roles);
					Result.Data.Status = Request.Status;

					Result.Result = EnumResponseResult.Success;
					Result.Code = Resource.SC_COMMON__SUCCESS;
					Result.Message = Resource.SM_COMMON__CREATED;

					// 로그인 사용자 정보가 존재하는 경우
					if (LoginUser != null)
					{
						// 로그 기록
						await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
														, Resource.SM_COMMON_ACCOUNT_USER_CREATED, Request.Name, Request.Email, Request.Email);
					}
					// 로그인 사용자 정보가 존재하지 않는 경우
					else
					{
						// 로그 기록
						await m_systemLogProvider.Add(EnumLogLevel.Information
														, Resource.SM_COMMON_ACCOUNT_USER_CREATED, Request.Name, Request.Email, Request.Email);
					}
				}
				// 계정 생성에 실패한 경우
				else
				{
					// 에러 내용 저장
					Result.Code = Resource.EC_COMMON_ACCOUNT_FAIL_TO_REGIST;
					Result.Message = IdentityResult.Errors.FirstOrDefault() == null ? "" : IdentityResult.Errors.FirstOrDefault()?.Description;
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

		/// <summary>사용자를 수정한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">사용자 정보</param>
		/// <param name="IncludeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 수정 결과</returns>
		public async Task<ResponseData> Update(string Id, RequestUserUpdate Request, bool IncludeDeletedUser = false)
		{
			var Result = new ResponseData();
			var oldUser = new NNApplicationUser();

			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON__INVALID_INFORMATION);

				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 사용자 계정을 가져온다.
				var User = await this.GetUserById(Id, IncludeDeletedUser);

				// 해당 계정을 찾을 수 없는 경우
				if (User == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				oldUser.Email = User.Email;
				oldUser.LoginId = User.LoginId;
				oldUser.Name = User.Name;
				oldUser.Code = User.Code;

				// 이메일 변경
				User.Email = Request.Email;

				// 사용자명 변경
				User.Name = Request.Name;

				// 사번 등 식별코드
				User.Code = Request.Code;

				// 삭제 안함으로 처리
				User.IsDeleted = false;

				// 이메일 확인 중인 경우
				if (Request.Status == EnumUserStatus.VerifyingEmail)
					// 이메일 미확인으로 설정
					User.EmailConfirmed = false;

				// 사용자 정보 변경
				var IdentityResult = await m_userManager.UpdateAsync(User);

				// 사용자 정보 변경에 실패한 경우
				if (!IdentityResult.Succeeded)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_FAIL_TO_UPDATE, IdentityResult.Errors.FirstOrDefault() == null ? "" : IdentityResult.Errors.FirstOrDefault()?.Description);

				// 사용자 상태에 따라서 처리
				switch (Request.Status)
				{
					// 잠김인 경우
					case EnumUserStatus.Locked:
						{
							// 잠금 일시를 100년 뒤까지로 수정
							await m_userManager.SetLockoutEndDateAsync(User, DateTime.Now.AddYears(100));
						}
						break;
					case EnumUserStatus.VerifyingEmail:
						{
							// 잠금 상태 해제
							await m_userManager.SetLockoutEndDateAsync(User, null);
						}
						break;
					case EnumUserStatus.Activated:
						{
							// 확인 메일 발송을 위한 토큰을 가져온다.
							string mailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(User);
							// 이메일 확인 처리
							await m_userManager.ConfirmEmailAsync(User, mailConfirmToken);

							// 잠금 상태 해제
							await m_userManager.SetLockoutEndDateAsync(User, null);
						}
						break;
				}

				// 역할을 수정한다.
				if (Request.Roles != null && Request.Roles.Count > 0)
				{
					// 기존 역할 삭제
					await m_userManager.RemoveFromRolesAsync(User, await m_userManager.GetRolesAsync(User));
					// 신규 역할 추가
					await m_userManager.AddToRolesAsync(User, Request.Roles);
				}

				// DB 변경 내용이 존재하는 경우
				if (m_dbContext.HasChanges())
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

				Result.Result = EnumResponseResult.Success;
				Result.Code = Resource.SC_COMMON__SUCCESS;
				Result.Message = Resource.SM_COMMON__UPDATED;

				// 로그 기록
				await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
					, Resource.SM_COMMON_ACCOUNT_USER_UPDATED, oldUser.Name, oldUser.Email, Request.Name);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>사용자를 삭제한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <returns>사용자 삭제 결과</returns>
		public async Task<ResponseData> Remove(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid SearchId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 사용자를 찾는다.
				var User = await m_dbContext.Users.Where(i => i.Id == SearchId && i.IsDeleted == false).FirstOrDefaultAsync();

				// 해당 계정을 찾을 수 없는 경우
				if (User == null || User.IsDeleted)
					return new ResponseData(EnumResponseResult.Warning, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 삭제 처리
						User.IsDeleted = true;
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						Transaction.Commit();

						Result.Result = EnumResponseResult.Success;
						Result.Code = Resource.SC_COMMON__SUCCESS;
						Result.Message = Resource.SM_COMMON__DELETED;

						// 로그 기록
						await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
							, Resource.SM_COMMON_ACCOUNT_USER_DISABLED, User.Name, User.LoginId, User.Email);
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

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>사용자 비밀번호를 변경한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">사용자 정보</param>
		/// <returns>사용자 수정 결과</returns>
		public async Task<ResponseData> ChangePassword(string Id, RequestUserChangePassword Request)
		{
			var Result = new ResponseData();
			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON__INVALID_INFORMATION);

				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 로그인한 사용자 계정을 가져온다.
				var Manager = this.LoginUser;

				// 해당 계정을 찾을수 없는 경우
				if (Manager == null || Manager.IsDeleted)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NEED_LOGIN, Resource.EM_COMMON__NEED_LOGIN);

				// // 사용자 수정 권한이 존재하는 경우
				// ResponseData responseClaim = await this.HasClaim(manager, "common.account.users.update");
				// if(responseClaim.Result == EnumResponseResult.Error)
				// 	return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_HAVE_PERMISSION, Resource.EM_COMMON__NOT_HAVE_PERMISSION);

				// 이메일로 해당 회원 정보를 가져온다.
				var User = await m_userManager.FindByIdAsync(Id);

				// 비밀번호 변경 토큰 생성
				string changePasswordToken = await m_userManager.GeneratePasswordResetTokenAsync(User);

				// 비밀번호 재설정
				var IdentityResult = await m_userManager.ResetPasswordAsync(User, changePasswordToken, Request.NewPassword);

				// 이메일 확인 처리에 성공한 경우
				if (IdentityResult.Succeeded)
				{
					// 비밀번호 변경 일시를 지금으로 저장
					User.PasswordChangeDate = DateTime.Now;
					await m_userManager.UpdateAsync(User);
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					Result.Result = EnumResponseResult.Success;
					Result.Code = Resource.SC_COMMON__SUCCESS;
					Result.Message = Resource.SM_COMMON_USER_SUCCESS_CHANGE_PASSWORD;
				}
				// 이메일 확인 처리에 실패한 경우
				else
				{
					Result.Code = Resource.EC_COMMON_ACCOUNT_FAIL_TO_RESET_PASSWORD;
					Result.Message = IdentityResult.Errors.FirstOrDefault() == null ? "" : IdentityResult.Errors.FirstOrDefault()?.Description;
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

		/// <summary>특정 사용자 식별자에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="IncludeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		public async Task<NNApplicationUser> GetUserById(string Id, bool IncludeDeletedUser = false)
		{
			try
			{
				if (!Id.IsEmpty())
				{
					// 사용자 ID로 해당 사용자 정보를 반환한다.
					var User = await m_userManager.FindByIdAsync(Id);

					// 해당 사용자가 존재하고 삭제 처리되지 않은 경우
					if (User != null && (IncludeDeletedUser || !User.IsDeleted))
						return User;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return null;
		}

		/// <summary>특정 로그인 이메일에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="Email">이메일 주소</param>
		/// <param name="IncludeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		public async Task<NNApplicationUser> GetUserByEmail(string Email, bool IncludeDeletedUser = false)
		{
			try
			{
				// 로그인 이메일로 해당 사용자 정보를 반환한다.
				NNApplicationUser User = await m_userManager.FindByEmailAsync(Email);

				// 해당 사용자가 존재하고 삭제 처리되지 않은 경우
				if (User != null && (IncludeDeletedUser || !User.IsDeleted))
					return User;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return null;
		}

		/// <summary>특정 로그인 ID에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="LoginId">로그인 아이디</param>
		/// <param name="IncludeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		public async Task<NNApplicationUser> GetUserByLoginId(string LoginId, bool IncludeDeletedUser = false)
		{
			try
			{
				// 주어진 로그인 아이디에 해당하는 사용자 정보를 가져온다.
				var Exist = await m_dbContext.Users.AsNoTracking()
					.Where(i => i.LoginId == LoginId)
					.FirstOrDefaultAsync();

				if (Exist == null)
					return null;

				// 로그인 ID로 해당 사용자 정보를 반환한다.
				var User = await m_userManager.FindByIdAsync(Exist.Id.ToString());

				// 해당 사용자가 존재하고 삭제 처리되지 않은 경우
				if (User != null && (IncludeDeletedUser || !User.IsDeleted))
					return User;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return null;
		}

		/// <summary>사용자 식별자로 특정 사용자를 가져온다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <returns>해당 사용자 데이터</returns>
		public async Task<ResponseData<ResponseUserWithRoles>> GetUser(string id)
		{
			var Result = new ResponseData<ResponseUserWithRoles>();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid SearchId))
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 사용자 정보를 가져온다.
					var User = await m_dbContext.Users.AsNoTracking()
						.Where(i => i.IsDeleted == false && i.Id == SearchId)
						.Select(i => new
						{
							i.Id,
							i.Email,
							i.Name,
							i.Code,
							LoginCount = i.UserLoginHistories.Count(),
							Roles = i.UserRoles.Select(j => j.Role.Name).ToList(),
						})
						.FirstOrDefaultAsync<dynamic, ResponseUserWithRoles>();

					// 해당 계정을 찾을 수 없는 경우
					if (User == null)
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						var ApplicationUser = await GetUserById(User.Id);
						User.Status = await m_userManager.GetUserStatus(ApplicationUser);

						Result.Data = User;
						Result.Result = EnumResponseResult.Success;
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

		/// <summary>전체 사용자를 가져온다.</summary>
		/// <param name="SearchRoleName">검색할 역할명</param>
		/// <param name="RegStartDate">가입일 검색 시작일자</param>
		/// <param name="RegEndDate">가입일 검색 종료일자</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드목록 (LoginId, Email, Name)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>사용자 목록</returns>
		public async Task<ResponseList<ResponseUserWithRoles>> GetUsers(
			string SearchRoleName = "", DateTime? RegStartDate = null, DateTime? RegEndDate = null,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Result = new ResponseList<ResponseUserWithRoles>();
			var SearchRoleId = Guid.Empty;
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				// 일반사용자 역할명의 역할 정보를 가져온다.
				var UserRole = await m_roleManager.FindByNameAsync(PredefinedRoleNames.RoleNameUser);

				// 검색할 역할명이 존재하는 경우
				if (!SearchRoleName.IsEmpty())
				{
					// 일반 사용자 역할이 아닌 경우
					if (SearchRoleName != PredefinedRoleNames.RoleNameUser)
					{
						// 해당 역할명의 역할 정보를 가져온다.
						var Role = await m_roleManager.FindByNameAsync(SearchRoleName);
						// 해당 역할이 존재하는 경우, 검색할 역할 아이디 저장
						if (Role != null)
							SearchRoleId = Role.Id;
					}
					// 일반 사용자 역할인 경우
					else if (UserRole != null)
						SearchRoleId = UserRole.Id;
				}

				// 사용자 목록을 가져온다.
				Result.Data = await m_dbContext.Users.AsNoTracking()
					.Where(i => i.IsDeleted == false
						&& (SearchRoleId == Guid.Empty || i.UserRoles.Any(j => j.RoleId == SearchRoleId))
						&& (SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("loginid") && i.LoginId.Contains(SearchKeyword))
							|| (SearchFields.Contains("Email") && i.Email.Contains(SearchKeyword))
							|| (SearchFields.Contains("name") && i.Name.Contains(SearchKeyword))
						)
					)
					.Select(i => new
					{
						Id = i.Id.ToString(),
						i.LoginId,
						i.Email,
						i.Name,
						i.Code,
						LoginCount = i.UserLoginHistories.Count(),
						Roles = i.UserRoles.Select(j => j.Role.Name).ToList(),
					})
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<dynamic, ResponseUserWithRoles>(Skip, CountPerPage);

				// 모든 사용자에 대해서 처리
				foreach (var User in Result.Data.Items)
				{
					var ApplicationUser = await GetUserById(User.Id);
					User.Status = await m_userManager.GetUserStatus(ApplicationUser);
				}

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

		/// <summary>특정 사용자에 대한 역할/사용자 권한 목록을 가져온다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>특정 사용자에 대한 권한 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetClaims(string Id, int Skip = 0, int CountPerPage = int.MaxValue, string SearchKeyword = "")
		{
			var Result = new ResponseList<ResponseClaim>();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (Id.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					var User = await this.GetUserById(Id);

					// 해당 계정을 찾을 수 없는 경우
					if (User == null || User.IsDeleted)
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 사용자의 역할을 가져온다.
						var RoleNames = (List<string>)await m_userManager.GetRolesAsync(User);
						if (RoleNames == null)
							RoleNames = new List<string>();

						// 해당 사용자의 역할 권한 및 사용자 권한을 가져온다.
						Result.Data = await m_dbContext.ClaimNames.AsNoTracking()
							.Where(i =>
								(
									m_dbContext.RoleClaims
										.Any(j => RoleNames.Contains(j.Role.Name)
													&& j.ClaimType == "Permission"
													&& j.ClaimValue == i.ClaimValue)
									|| m_dbContext.UserClaims
										.Any(j => j.UserId == User.Id
													&& j.ClaimType == "Permission"
													&& j.ClaimValue == i.ClaimValue)
								)
								&& (SearchKeyword.IsEmpty() || i.ClaimValue.Contains(SearchKeyword))
							)
							.OrderByWithDirection(i => i.ClaimValue)
							.CreateListAsync<ClaimName, ResponseClaim>();

						// 데이터가 존재하는 경우, 가져온 권한 중 중복 제거 처리
						if (Result.Data.Items.Count > 0)
						{
							List<ResponseClaim> distinctClaims = Result.Data.Items.Distinct(new ResponseClaimEqualityComparer()).ToList();
							Result.Data.TotalCount = distinctClaims.Count;
							Result.Data.Items.Clear();
							Result.Data.Items.AddRange(distinctClaims);
						}
						Result.Result = EnumResponseResult.Success;
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

		/// <summary>특정 사용자에 대한 사용자 권한 목록을 가져온다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>특정 사용자에 대한 권한 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetUserClaims(string Id, int Skip = 0, int CountPerPage = int.MaxValue, string SearchKeyword = "")
		{
			var Result = new ResponseList<ResponseClaim>();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (Id.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					var User = await this.GetUserById(Id);

					// 해당 계정을 찾을 수 없는 경우
					if (User == null || User.IsDeleted)
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 사용자의 사용자 권한을 가져온다.
						Result.Data = await m_dbContext.ClaimNames.AsNoTracking()
							.Where(i =>
								(
									m_dbContext.UserClaims
										.Any(j =>
											j.UserId == User.Id
											&& j.ClaimType == "Permission"
											&& j.ClaimValue == i.ClaimValue
										)
								)
								&& (SearchKeyword.IsEmpty() || i.ClaimValue.Contains(SearchKeyword))
							)
							.OrderByWithDirection(i => i.ClaimValue)
							.CreateListAsync<ClaimName, ResponseClaim>();

						// 데이터가 존재하는 경우, 가져온 권한 중 중복 제거 처리
						if (Result.Data.Items.Count > 0)
						{
							List<ResponseClaim> distinctClaims = Result.Data.Items.Distinct(new ResponseClaimEqualityComparer()).ToList();
							Result.Data.TotalCount = distinctClaims.Count;
							Result.Data.Items.Clear();
							Result.Data.Items.AddRange(distinctClaims);
						}
						Result.Result = EnumResponseResult.Success;
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

		/// <summary>특정 사용자에 권한을 추가한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">권한 정보</param>
		/// <returns>권한 등록 결과</returns>
		public async Task<ResponseData> AddClaim(string Id, RequestAddClaimToUser Request)
		{
			var Result = new ResponseData();
			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (Id.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효하지 않은 경우
				else if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					var User = await this.GetUserById(Id);

					// 해당 계정을 찾을 수 없는 경우
					if (User == null || User.IsDeleted)
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 사용자의 권한을 가져온다.
						var Claims = await m_userManager.GetClaimsAsync(User);

						// 해당 권한이 존재하지 않는 경우
						if (!Claims.Where(i => i.Type == "Permission" && i.Value == Request.ClaimValue).Any())
						{
							// 해당 권한 추가
							await m_userManager.AddClaimAsync(User, new Claim("Permission", Request.ClaimValue));
							Result.Result = EnumResponseResult.Success;
							Result.Code = Resource.SC_COMMON__SUCCESS;
							Result.Message = Resource.SM_COMMON__ADDED;

							// 로그 기록
							await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
															, Resource.SM_COMMON_ACCOUNT_USER_ADD_CLAIM, User.LoginId, Request.ClaimValue);
						}
						// 해당 권한이 존재하는 경우
						else
						{
							Result.Result = EnumResponseResult.Warning;
							Result.Code = Resource.EC_COMMON__ALREADY_EXIST;
							Result.Message = Resource.EM_COMMON__ALREADY_EXIST;
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

		/// <summary>역할에서 권한을 삭제한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="ClaimValue">권한값</param>
		/// <returns>권한 삭제 결과</returns>
		public async Task<ResponseData> RemoveClaim(string Id, string ClaimValue)
		{
			var Result = new ResponseData();
			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (Id.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 권한 값이 유효하지 않은 경우
				else if (ClaimValue.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					var User = await this.GetUserById(Id);

					// 해당 계정을 찾을 수 없는 경우
					if (User == null || User.IsDeleted)
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 사용자의 권한을 가져온다.
						var Claims = await m_userManager.GetClaimsAsync(User);
						var Claim = Claims.Where(i => i.Type == "Permission" && i.Value == ClaimValue).FirstOrDefault();

						// 해당 권한이 존재하는 경우
						if (Claim != null)
						{
							// 해당 권한 삭제
							await m_userManager.RemoveClaimAsync(User, Claim);
							Result.Result = EnumResponseResult.Success;
							Result.Code = Resource.SC_COMMON__SUCCESS;
							Result.Message = Resource.SM_COMMON__REMOVED;

							// 로그 기록
							await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
															, Resource.SM_COMMON_ACCOUNT_USER_REMOVE_CLAIM, User.LoginId, ClaimValue);
						}
						// 해당 권한이 존재하지 않는 경우
						else
						{
							Result.Result = EnumResponseResult.Warning;
							Result.Code = Resource.EC_COMMON__NOT_FOUND;
							Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>특정 권한이 사용자에게 존재하는지 검사한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="NeedClaim">필요 권한</param>
		/// <returns>권한 존재 여부</returns>
		public async Task<ResponseData> HasClaim(string Id, string NeedClaim)
		{
			var Result = new ResponseData();
			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (Id.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					var User = await this.GetUserById(Id);

					// 해당 권한을 가지고 있는지 검사
					Result = await HasClaim(User, NeedClaim);
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

		/// <summary>특정 권한이 사용자에게 존재하는지 검사한다.</summary>
		/// <param name="User">사용자 객체</param>
		/// <param name="NeedClaim">필요 권한</param>
		/// <returns>권한 존재 여부</returns>
		public async Task<ResponseData> HasClaim(NNApplicationUser User, string NeedClaim)
		{
			var Result = new ResponseData();
			var NeedClaims = new List<string>();

			try
			{
				// 해당 계정을 찾을 수 없는 경우
				if (User == null || User.IsDeleted)
				{
					Result.Result = EnumResponseResult.Error;
					Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
					Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
				}
				// 해당 계정을 찾은 경우
				else
				{
					Result.Result = EnumResponseResult.Error;
					Result.Code = Resource.EC_COMMON__NOT_HAVE_PERMISSION;
					Result.Message = Resource.EM_COMMON__NOT_HAVE_PERMISSION;

					// 검사할 권한 목록 작성
					var NeedClaimArray = NeedClaim.Split(new[] { ' ', ',' }, StringSplitOptions.RemoveEmptyEntries);
					if (NeedClaimArray.Length > 0)
						NeedClaims.AddRange(NeedClaimArray);

					// 해당 사용자에 대한 권한 목록을 가져온다.
					var Claims = await m_userManager.GetClaimsAsync(User);

					// 해당 권한이 존재하는 경우
					if (Claims.Any(i => NeedClaims.Contains(i.Value)))
						Result.Result = EnumResponseResult.Success;
					// 해당 권한이 존재하지 않는 경우
					else
					{
						// 해당 권한을 가지는 역할 목록을 가져온다.
						var RoleNames = m_dbContext.RoleClaims.AsNoTracking()
							.Where(i => NeedClaims.Contains(i.ClaimValue))
							.Select(i => i.Role.Name)
							.ToList();

						// 모든 권한에 대해서 처리
						foreach (var RoleName in RoleNames)
						{
							// 사용자가 해당 권한을 가지는 경우, 권한이 있음으로 설정하고 종료
							if (await m_userManager.IsInRoleAsync(User, RoleName))
							{
								Result.Result = EnumResponseResult.Success;
								break;
							}
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

		/// <summary>특정 사용자에 역할을 추가한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="Request">역할 정보</param>
		/// <returns>역할 등록 결과</returns>
		public async Task<ResponseData> AddRole(string Id, RequestAddRoleToUser Request)
		{
			var Result = new ResponseData();
			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (Id.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효하지 않은 경우
				else if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					var User = await this.GetUserById(Id);

					// 해당 계정을 찾을 수 없는 경우
					if (User == null || User.IsDeleted)
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 역할에 포함되어 있지 않은 경우
						if (!await m_userManager.IsInRoleAsync(User, Request.RoleName))
						{
							// 해당 역할 추가
							await m_userManager.AddToRoleAsync(User, Request.RoleName);
							Result.Result = EnumResponseResult.Success;
							Result.Code = Resource.SC_COMMON__SUCCESS;
							Result.Message = Resource.SM_COMMON__ADDED;

							// 로그인 사용자 정보가 존재하는 경우
							if (LoginUser != null)
							{
								// 로그 기록
								await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
																, Resource.SM_COMMON_ACCOUNT_USER_ADD_ROLE, User.LoginId, Request.RoleName);
							}
							// 로그인 사용자 정보가 존재하지 않는 경우
							else
							{
								// 로그 기록
								await m_systemLogProvider.Add(EnumLogLevel.Information
																, Resource.SM_COMMON_ACCOUNT_USER_ADD_ROLE, User.LoginId, Request.RoleName);
							}
						}
						// 해당 역할에 포함되어 있는 경우
						else
						{
							Result.Result = EnumResponseResult.Warning;
							Result.Code = Resource.EC_COMMON__ALREADY_EXIST;
							Result.Message = Resource.EM_COMMON__ALREADY_EXIST;
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

		/// <summary>역할에서 역할을 삭제한다.</summary>
		/// <param name="Id">사용자 식별자</param>
		/// <param name="RoleName">역할명</param>
		/// <returns>역할 삭제 결과</returns>
		public async Task<ResponseData> RemoveRole(string Id, string RoleName)
		{
			var Result = new ResponseData();
			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (Id.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 값이 유효하지 않은 경우
				else if (RoleName.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					var User = await this.GetUserById(Id);

					// 해당 계정을 찾을 수 없는 경우
					if (User == null || User.IsDeleted)
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 역할 정보를 가져온다.
						var Role = await m_roleManager.FindByNameAsync(RoleName);

						// 해당 역할을 찾은 경우
						if (Role != null)
						{
							// 해당 역할에 포함되어 있는 경우
							if (await m_userManager.IsInRoleAsync(User, Role.Name))
							{
								// 해당 역할 추가
								await m_userManager.RemoveFromRoleAsync(User, Role.Name);
								Result.Result = EnumResponseResult.Success;
								Result.Code = Resource.SC_COMMON__SUCCESS;
								Result.Message = Resource.SM_COMMON__REMOVED;

								// 로그 기록
								await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
																, Resource.SM_COMMON_ACCOUNT_USER_REMOVE_ROLE, User.LoginId, RoleName);
							}
							// 해당 역할에 포함되어 있지 않은 경우
							else
							{
								Result.Result = EnumResponseResult.Warning;
								Result.Code = Resource.EC_COMMON__NOT_FOUND;
								Result.Message = Resource.EM_COMMON__NOT_FOUND;
							}
						}
						// 해당 역할을 찾을 수 없는 경우
						else
						{
							Result.Result = EnumResponseResult.Error;
							Result.Code = Resource.EC_COMMON_ACCOUNT_ROLE_NOT_FOUND;
							Result.Message = Resource.EM_COMMON_ACCOUNT_ROLE_NOT_FOUND;
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

		/// <summary>사용자 수를 반환한다.</summary>
		/// <returns></returns>
		public async Task<int> UserCount()
		{
			int Result = 0;
			try
			{
				Result = await m_dbContext.Users.AsNoTracking().CountAsync();
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return Result;
		}
	}
}
