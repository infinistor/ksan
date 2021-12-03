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
		/// <param name="request">사용자 정보</param>
		/// <param name="password">비밀번호 (옵션)</param>
		/// <param name="confirmPassword">확인 비밀번호 (옵션)</param>
		/// <returns>사용자 등록 결과</returns>
		public async Task<ResponseData<ResponseUserWithRoles>> Add(RequestUserRegist request, string password = "", string confirmPassword = "")
		{
			ResponseData<ResponseUserWithRoles> result = new ResponseData<ResponseUserWithRoles>();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseUserWithRoles>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 비밀번호가 유효하고, 확인 비밀번호와 일치하지 않는 경우
				if (!password.IsEmpty() && password != confirmPassword)
					return new ResponseData<ResponseUserWithRoles>(EnumResponseResult.Error, Resource.EM_COMMON_ACCOUNT_INVALID_PASSWORD, Resource.EM_COMMON_ACCOUNT_PASSWORD_AND_CONFIRM_PASSWORD_DO_NOT_MATCH);
			
				// 사용자명이 없는 경우, 사용자 아이디를 사용자명으로 설정
				if (request.Name.IsEmpty())
					request.Name = request.Email;

				// 해당 계정 객체를 생성한다.
				user = new NNApplicationUser { Id = Guid.NewGuid(), LoginId = request.LoginId, Email = request.Email, Name = request.Name, Code = request.Code };

				// 이메일 확인 중인 경우
				if (request.Status == EnumUserStatus.VerifyingEmail)
					// 이메일 미확인으로 설정
					user.EmailConfirmed = false;

				IdentityResult identityResult;

				// 비밀번호가 있는 경우
				if (!password.IsEmpty())
					identityResult = await m_userManager.CreateAsync(user, password);
				// 비밀번호가 없는 경우
				else
					identityResult = await m_userManager.CreateAsync(user);

				// 계정 생성에 성공한 경우
				if (identityResult.Succeeded)
				{
					// 비밀번호가 존재하는 경우
					if (!password.IsEmpty())
					{
						// 비밀번호 변경 일시를 지금으로 저장
						user.PasswordChangeDate = DateTime.Now;
						await m_userManager.UpdateAsync(user);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
					}

					// 사용자 상태에 따라서 처리
					switch (request.Status)
					{
						// 잠김인 경우
						case EnumUserStatus.Locked:
							{
								// 잠금 일시를 100년 뒤까지로 수정
								await m_userManager.SetLockoutEndDateAsync(user, DateTime.Now.AddYears(100));
							}
							break;
						case EnumUserStatus.VerifyingEmail:
							{
								// 잠금 상태 해제
								await m_userManager.SetLockoutEndDateAsync(user, null);
							}
							break;
						case EnumUserStatus.Activated:
							{
								// 확인 메일 발송을 위한 토큰을 가져온다.
								string mailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(user);
								// 이메일 확인 처리
								await m_userManager.ConfirmEmailAsync(user, mailConfirmToken);

								// 잠금 상태 해제
								await m_userManager.SetLockoutEndDateAsync(user, null);
							}
							break;
					}

					// 역할을 등록한다.
					if (request.Roles != null && request.Roles.Count > 0)
						await m_userManager.AddToRolesAsync(user, request.Roles);

					// 리턴할 정보 저장
					result.Data = new ResponseUserWithRoles();
					result.Data.Id = user.Id.ToString();
					result.Data.Email = request.Email;
					result.Data.Name = request.Name;
					result.Data.Code = request.Code;
					if(request.Roles != null)
						result.Data.Roles.AddRange(request.Roles);
					result.Data.Status = request.Status;

					result.Result = EnumResponseResult.Success;
					result.Code = Resource.SC_COMMON__SUCCESS;
					result.Message = Resource.SM_COMMON__CREATED;

					// 로그인 사용자 정보가 존재하는 경우
					if (LoginUser != null)
					{
						// 로그 기록
						await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
														, Resource.SM_COMMON_ACCOUNT_USER_CREATED, request.Name, request.Email, request.Email);
					}
					// 로그인 사용자 정보가 존재하지 않는 경우
					else
					{
						// 로그 기록
						await m_systemLogProvider.Add(EnumLogLevel.Information
														, Resource.SM_COMMON_ACCOUNT_USER_CREATED, request.Name, request.Email, request.Email);
					}
				}
				// 계정 생성에 실패한 경우
				else
				{
					// 에러 내용 저장
					result.Code = Resource.EC_COMMON_ACCOUNT_FAIL_TO_REGIST;
					result.Message = identityResult.Errors.FirstOrDefault() == null ? "" : identityResult.Errors.FirstOrDefault()?.Description;
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

		/// <summary>사용자를 수정한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="request">사용자 정보</param>
		/// <param name="includeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 수정 결과</returns>
		public async Task<ResponseData> Update(string id, RequestUserUpdate request, bool includeDeletedUser = false)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser oldUser = new NNApplicationUser();
			NNApplicationUser user;

			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON__INVALID_INFORMATION);

				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());
				
				// 사용자 계정을 가져온다.
				user = await this.GetUserById(id, includeDeletedUser);

				// 해당 계정을 찾을 수 없는 경우
				if (user == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);
			
				oldUser.Email = user.Email;
				oldUser.LoginId = user.LoginId;
				oldUser.Name = user.Name;
				oldUser.Code = user.Code;

				// 이메일 변경
				user.Email = request.Email;

				// 사용자명 변경
				user.Name = request.Name;

				// 사번 등 식별코드
				user.Code = request.Code;

				// 삭제 안함으로 처리
				user.IsDeleted = false;

				// 이메일 확인 중인 경우
				if (request.Status == EnumUserStatus.VerifyingEmail)
					// 이메일 미확인으로 설정
					user.EmailConfirmed = false;

				// 사용자 정보 변경
				IdentityResult identityResult = await m_userManager.UpdateAsync(user);
				
				// 사용자 정보 변경에 실패한 경우
				if (!identityResult.Succeeded)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_FAIL_TO_UPDATE, identityResult.Errors.FirstOrDefault() == null ? "" : identityResult.Errors.FirstOrDefault()?.Description);

				// 사용자 상태에 따라서 처리
				switch (request.Status)
				{
					// 잠김인 경우
					case EnumUserStatus.Locked:
					{
						// 잠금 일시를 100년 뒤까지로 수정
						await m_userManager.SetLockoutEndDateAsync(user, DateTime.Now.AddYears(100));
					}
						break;
					case EnumUserStatus.VerifyingEmail:
					{
						// 잠금 상태 해제
						await m_userManager.SetLockoutEndDateAsync(user, null);
					}
						break;
					case EnumUserStatus.Activated:
					{
						// 확인 메일 발송을 위한 토큰을 가져온다.
						string mailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(user);
						// 이메일 확인 처리
						await m_userManager.ConfirmEmailAsync(user, mailConfirmToken);

						// 잠금 상태 해제
						await m_userManager.SetLockoutEndDateAsync(user, null);
					}
						break;
				}

				// 역할을 수정한다.
				if (request.Roles != null && request.Roles.Count > 0)
				{
					// 기존 역할 삭제
					await m_userManager.RemoveFromRolesAsync(user, await m_userManager.GetRolesAsync(user));
					// 신규 역할 추가
					await m_userManager.AddToRolesAsync(user, request.Roles);
				}

				// DB 변경 내용이 존재하는 경우
				if(m_dbContext.HasChanges())
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

				result.Result = EnumResponseResult.Success;
				result.Code = Resource.SC_COMMON__SUCCESS;
				result.Message = Resource.SM_COMMON__UPDATED;

				// 로그 기록
				await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
					, Resource.SM_COMMON_ACCOUNT_USER_UPDATED, oldUser.Name, oldUser.Email, request.Name);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return result;
		}

		/// <summary>사용자를 삭제한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <returns>사용자 삭제 결과</returns>
		public async Task<ResponseData> Remove(string id)
		{
			ResponseData result = new ResponseData();

			try
			{
				Guid searchId;

				// 파라미터가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out searchId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 사용자를 찾는다.
				User user = await m_dbContext.Users.Where(i => i.Id == searchId && i.IsDeleted == false).FirstOrDefaultAsync();

				// 해당 계정을 찾을 수 없는 경우
				if (user == null || user.IsDeleted)
					return new ResponseData(EnumResponseResult.Warning, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);
				
				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 삭제 처리
						user.IsDeleted = true;
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						transaction.Commit();

						result.Result = EnumResponseResult.Success;
						result.Code = Resource.SC_COMMON__SUCCESS;
						result.Message = Resource.SM_COMMON__DELETED;

						// 로그 기록
						await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
							, Resource.SM_COMMON_ACCOUNT_USER_DISABLED, user.Name, user.LoginId, user.Email);
					}
					catch (Exception ex)
					{
						transaction.Rollback();

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

		/// <summary>사용자 비밀번호를 변경한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="request">사용자 정보</param>
		/// <returns>사용자 수정 결과</returns>
		public async Task<ResponseData> ChangePassword(string id, RequestUserChangePassword request)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser manager;
			NNApplicationUser user;

			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON__INVALID_INFORMATION);

				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 로그인한 사용자 계정을 가져온다.
				manager = this.LoginUser;

				// 해당 계정을 찾을수 없는 경우
				if (manager == null || manager.IsDeleted)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NEED_LOGIN, Resource.EM_COMMON__NEED_LOGIN);
				
				// // 사용자 수정 권한이 존재하는 경우
				// ResponseData responseClaim = await this.HasClaim(manager, "common.account.users.update");
				// if(responseClaim.Result == EnumResponseResult.Error)
				// 	return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_HAVE_PERMISSION, Resource.EM_COMMON__NOT_HAVE_PERMISSION);
				
				// 이메일로 해당 회원 정보를 가져온다.
				user = await m_userManager.FindByIdAsync(id);

				// 비밀번호 변경 토큰 생성
				string changePasswordToken = await m_userManager.GeneratePasswordResetTokenAsync(user);

				// 비밀번호 재설정
				IdentityResult identityResult = await m_userManager.ResetPasswordAsync(user, changePasswordToken, request.NewPassword);

				// 이메일 확인 처리에 성공한 경우
				if (identityResult.Succeeded)
				{
					// 비밀번호 변경 일시를 지금으로 저장
					user.PasswordChangeDate = DateTime.Now;
					await m_userManager.UpdateAsync(user);
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					result.Result = EnumResponseResult.Success;
					result.Code = Resource.SC_COMMON__SUCCESS;
					result.Message = Resource.SM_COMMON_USER_SUCCESS_CHANGE_PASSWORD;
				}
				// 이메일 확인 처리에 실패한 경우
				else
				{
					result.Code = Resource.EC_COMMON_ACCOUNT_FAIL_TO_RESET_PASSWORD;
					result.Message = identityResult.Errors.FirstOrDefault() == null ? "" : identityResult.Errors.FirstOrDefault()?.Description;
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

		/// <summary>특정 사용자 식별자에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="includeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		public async Task<NNApplicationUser> GetUserById(string id, bool includeDeletedUser = false)
		{
			NNApplicationUser result = null;
			try
			{
				if (!id.IsEmpty())
				{
					// 사용자 ID로 해당 사용자 정보를 반환한다.
					NNApplicationUser user = await m_userManager.FindByIdAsync(id);

					// 해당 사용자가 존재하고 삭제 처리되지 않은 경우
					if (user != null && (includeDeletedUser || !user.IsDeleted))
						result = user;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}

		/// <summary>특정 로그인 이메일에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="email">이메일 주소</param>
		/// <param name="includeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		public async Task<NNApplicationUser> GetUserByEmail(string email, bool includeDeletedUser = false)
		{
			NNApplicationUser result = null;
			try
			{
				// 로그인 이메일로 해당 사용자 정보를 반환한다.
				NNApplicationUser user = await m_userManager.FindByEmailAsync(email);

				// 해당 사용자가 존재하고 삭제 처리되지 않은 경우
				if (user != null && (includeDeletedUser || !user.IsDeleted))
					result = user;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}

		/// <summary>특정 로그인 ID에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="loginId">로그인 아이디</param>
		/// <param name="includeDeletedUser">삭제된 사용자도 포함할지 여부</param>
		/// <returns>사용자 정보 객체</returns>
		public async Task<NNApplicationUser> GetUserByLoginId(string loginId, bool includeDeletedUser = false)
		{
			NNApplicationUser result = null;
			try
			{
				// 주어진 로그인 아이디에 해당하는 사용자 정보를 가져온다.
				User exist = await m_dbContext.Users.AsNoTracking()
					.Where(i => i.LoginId == loginId)
					.FirstOrDefaultAsync();

				if (exist == null)
					return null;
				
				// 로그인 ID로 해당 사용자 정보를 반환한다.
				NNApplicationUser user = await m_userManager.FindByIdAsync(exist.Id.ToString());

				// 해당 사용자가 존재하고 삭제 처리되지 않은 경우
				if (user != null && (includeDeletedUser || !user.IsDeleted))
					result = user;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}

		/// <summary>사용자 식별자로 특정 사용자를 가져온다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <returns>해당 사용자 데이터</returns>
		public async Task<ResponseData<ResponseUserWithRoles>> GetUser(string id)
		{
			ResponseData<ResponseUserWithRoles> result = new ResponseData<ResponseUserWithRoles>();
			try
			{
				Guid searchId;
				
				// 파라미터가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out searchId))
				{
					result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 사용자 정보를 가져온다.
					ResponseUserWithRoles user = await m_dbContext.Users.AsNoTracking()
														.Where(i => i.IsDeleted == false && i.Id == searchId)
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
					if (user == null)
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						NNApplicationUser applicationUser = await GetUserById(user.Id);
						user.Status = await m_userManager.GetUserStatus(applicationUser);

						result.Data = user;
						result.Result = EnumResponseResult.Success;
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

		/// <summary>전체 사용자를 가져온다.</summary>
		/// <param name="searchRoleName">검색할 역할명</param>
		/// <param name="regStartDate">가입일 검색 시작일자</param>
		/// <param name="regEndDate">가입일 검색 종료일자</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드목록 (LoginId, Email, Name)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>사용자 목록</returns>
		public async Task<ResponseList<ResponseUserWithRoles>> GetUsers(
			string searchRoleName = "", DateTime? regStartDate = null, DateTime? regEndDate = null,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseUserWithRoles> result = new ResponseList<ResponseUserWithRoles>();
			Guid searchRoleId = Guid.Empty;
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				// 일반사용자 역할명의 역할 정보를 가져온다.
				NNApplicationRole userRole = await m_roleManager.FindByNameAsync(PredefinedRoleNames.RoleNameUser);

				// 검색할 역할명이 존재하는 경우
				if (!searchRoleName.IsEmpty())
				{
					// 일반 사용자 역할이 아닌 경우
					if(searchRoleName != PredefinedRoleNames.RoleNameUser)
					{
						// 해당 역할명의 역할 정보를 가져온다.
						NNApplicationRole role = await m_roleManager.FindByNameAsync(searchRoleName);
						// 해당 역할이 존재하는 경우, 검색할 역할 아이디 저장
						if (role != null)
							searchRoleId = role.Id;
					}
					// 일반 사용자 역할인 경우
					else if (userRole != null) 
						searchRoleId = userRole.Id;
				}

				// 사용자 목록을 가져온다.
				result.Data = await m_dbContext.Users.AsNoTracking()
													.Where(i => i.IsDeleted == false
														&& (searchRoleId == Guid.Empty || i.UserRoles.Any(j => j.RoleId == searchRoleId))
														&& (searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
														    || (searchFields.Contains("loginid") && i.LoginId.Contains(searchKeyword))
															|| (searchFields.Contains("email") && i.Email.Contains(searchKeyword))
															|| (searchFields.Contains("name") && i.Name.Contains(searchKeyword))
														)
													)
													.Select(i => new {
														Id = i.Id.ToString(),
														i.LoginId,
														i.Email,
														i.Name,
														i.Code,
														LoginCount = i.UserLoginHistories.Count(),
														Roles = i.UserRoles.Select(j => j.Role.Name).ToList(),
													})
													.OrderByWithDirection(orderFields, orderDirections)
													.CreateListAsync<dynamic, ResponseUserWithRoles>(skip, countPerPage);

				// 모든 사용자에 대해서 처리
				foreach (ResponseUserWithRoles user in result.Data.Items)
				{
					NNApplicationUser applicationUser = await GetUserById(user.Id);
					user.Status = await m_userManager.GetUserStatus(applicationUser);
				}

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

		/// <summary>특정 사용자에 대한 역할/사용자 권한 목록을 가져온다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>특정 사용자에 대한 권한 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetClaims(string id, int skip = 0, int countPerPage = int.MaxValue, string searchKeyword = "")
		{
			ResponseList<ResponseClaim> result = new ResponseList<ResponseClaim>();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (id.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					user = await this.GetUserById(id);

					// 해당 계정을 찾을 수 없는 경우
					if (user == null || user.IsDeleted)
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 사용자의 역할을 가져온다.
						List<string> roleNames = (List<string>)await m_userManager.GetRolesAsync(user);
						if (roleNames == null)
							roleNames = new List<string>();

						// 해당 사용자의 역할 권한 및 사용자 권한을 가져온다.
						result.Data = await m_dbContext.ClaimNames.AsNoTracking()
														.Where(i =>
															(
																m_dbContext.RoleClaims
																	.Any(j => roleNames.Contains(j.Role.Name)
																	          && j.ClaimType == "Permission"
																	          && j.ClaimValue == i.ClaimValue)
																|| m_dbContext.UserClaims
																	.Any(j => j.UserId == user.Id
																	          && j.ClaimType == "Permission"
																	          && j.ClaimValue == i.ClaimValue)
															)
															&& (searchKeyword.IsEmpty() || i.ClaimValue.Contains(searchKeyword))
														)
														.OrderByWithDirection(i => i.ClaimValue)
														.CreateListAsync<ClaimName, ResponseClaim>();

						// 데이터가 존재하는 경우, 가져온 권한 중 중복 제거 처리
						if (result.Data.Items.Count > 0)
						{
							List<ResponseClaim> distinctClaims = result.Data.Items.Distinct(new ResponseClaimEqualityComparer()).ToList();
							result.Data.TotalCount = distinctClaims.Count;
							result.Data.Items.Clear();
							result.Data.Items.AddRange(distinctClaims);
						}
						result.Result = EnumResponseResult.Success;
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

		/// <summary>특정 사용자에 대한 사용자 권한 목록을 가져온다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>특정 사용자에 대한 권한 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetUserClaims(string id, int skip = 0, int countPerPage = int.MaxValue, string searchKeyword = "")
		{
			ResponseList<ResponseClaim> result = new ResponseList<ResponseClaim>();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (id.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					user = await this.GetUserById(id);

					// 해당 계정을 찾을 수 없는 경우
					if (user == null || user.IsDeleted)
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 사용자의 사용자 권한을 가져온다.
						result.Data = await m_dbContext.ClaimNames.AsNoTracking()
														.Where(i =>
															(
																m_dbContext.UserClaims
																	.Any(j => 
																		j.UserId == user.Id
																		&& j.ClaimType == "Permission"
																		&& j.ClaimValue == i.ClaimValue
																	)
															)
															&& (searchKeyword.IsEmpty() || i.ClaimValue.Contains(searchKeyword))
														)
														.OrderByWithDirection(i => i.ClaimValue)
														.CreateListAsync<ClaimName, ResponseClaim>();

						// 데이터가 존재하는 경우, 가져온 권한 중 중복 제거 처리
						if (result.Data.Items.Count > 0)
						{
							List<ResponseClaim> distinctClaims = result.Data.Items.Distinct(new ResponseClaimEqualityComparer()).ToList();
							result.Data.TotalCount = distinctClaims.Count;
							result.Data.Items.Clear();
							result.Data.Items.AddRange(distinctClaims);
						}
						result.Result = EnumResponseResult.Success;
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

		/// <summary>특정 사용자에 권한을 추가한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="request">권한 정보</param>
		/// <returns>권한 등록 결과</returns>
		public async Task<ResponseData> AddClaim(string id, RequestAddClaimToUser request)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (id.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효하지 않은 경우
				else if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					user = await this.GetUserById(id);

					// 해당 계정을 찾을 수 없는 경우
					if (user == null || user.IsDeleted)
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 사용자의 권한을 가져온다.
						IList<Claim> claims = await m_userManager.GetClaimsAsync(user);

						// 해당 권한이 존재하지 않는 경우
						if (!claims.Where(i => i.Type == "Permission" && i.Value == request.ClaimValue).Any())
						{
							// 해당 권한 추가
							await m_userManager.AddClaimAsync(user, new Claim("Permission", request.ClaimValue));
							result.Result = EnumResponseResult.Success;
							result.Code = Resource.SC_COMMON__SUCCESS;
							result.Message = Resource.SM_COMMON__ADDED;

							// 로그 기록
							await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
															, Resource.SM_COMMON_ACCOUNT_USER_ADD_CLAIM, user.LoginId, request.ClaimValue);
						}
						// 해당 권한이 존재하는 경우
						else
						{
							result.Result = EnumResponseResult.Warning;
							result.Code = Resource.EC_COMMON__ALREADY_EXIST;
							result.Message = Resource.EM_COMMON__ALREADY_EXIST;
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

		/// <summary>역할에서 권한을 삭제한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="claimValue">권한값</param>
		/// <returns>권한 삭제 결과</returns>
		public async Task<ResponseData> RemoveClaim(string id, string claimValue)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (id.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 권한 값이 유효하지 않은 경우
				else if (claimValue.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					user = await this.GetUserById(id);

					// 해당 계정을 찾을 수 없는 경우
					if (user == null || user.IsDeleted)
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 사용자의 권한을 가져온다.
						IList<Claim> claims = await m_userManager.GetClaimsAsync(user);
						Claim claim = claims.Where(i => i.Type == "Permission" && i.Value == claimValue).FirstOrDefault();

						// 해당 권한이 존재하는 경우
						if (claim != null)
						{
							// 해당 권한 삭제
							await m_userManager.RemoveClaimAsync(user, claim);
							result.Result = EnumResponseResult.Success;
							result.Code = Resource.SC_COMMON__SUCCESS;
							result.Message = Resource.SM_COMMON__REMOVED;

							// 로그 기록
							await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
															, Resource.SM_COMMON_ACCOUNT_USER_REMOVE_CLAIM, user.LoginId, claimValue);
						}
						// 해당 권한이 존재하지 않는 경우
						else
						{
							result.Result = EnumResponseResult.Warning;
							result.Code = Resource.EC_COMMON__NOT_FOUND;
							result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>특정 권한이 사용자에게 존재하는지 검사한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="needClaim">필요 권한</param>
		/// <returns>권한 존재 여부</returns>
		public async Task<ResponseData> HasClaim(string id, string needClaim)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (id.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					user = await this.GetUserById(id);

					// 해당 권한을 가지고 있는지 검사
					result = await HasClaim(user, needClaim);
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

		/// <summary>특정 권한이 사용자에게 존재하는지 검사한다.</summary>
		/// <param name="user">사용자 객체</param>
		/// <param name="needClaim">필요 권한</param>
		/// <returns>권한 존재 여부</returns>
		public async Task<ResponseData> HasClaim(NNApplicationUser user, string needClaim)
		{
			ResponseData result = new ResponseData();
			List<string> needClaims = new List<string>();

			try
			{
				// 해당 계정을 찾을 수 없는 경우
				if (user == null || user.IsDeleted)
				{
					result.Result = EnumResponseResult.Error;
					result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
					result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
				}
				// 해당 계정을 찾은 경우
				else
				{
					result.Result = EnumResponseResult.Error;
					result.Code = Resource.EC_COMMON__NOT_HAVE_PERMISSION;
					result.Message = Resource.EM_COMMON__NOT_HAVE_PERMISSION;

					// 검사할 권한 목록 작성
					string[] needClaimArray = needClaim.Split(new [] { ' ', ',' }, StringSplitOptions.RemoveEmptyEntries);
					if (needClaimArray.Length > 0)
						needClaims.AddRange(needClaimArray);

					// 해당 사용자에 대한 권한 목록을 가져온다.
					IList<Claim> claims = await m_userManager.GetClaimsAsync(user);

					// 해당 권한이 존재하는 경우
					if (claims.Any(i => needClaims.Contains(i.Value)))
						result.Result = EnumResponseResult.Success;
					// 해당 권한이 존재하지 않는 경우
					else
					{
						// 해당 권한을 가지는 역할 목록을 가져온다.
						List<string> roleNames = m_dbContext.RoleClaims.AsNoTracking()
																			.Where(i => needClaims.Contains(i.ClaimValue))
																			.Select(i => i.Role.Name)
																			.ToList();

						// 모든 권한에 대해서 처리
						foreach (string roleName in roleNames)
						{
							// 사용자가 해당 권한을 가지는 경우, 권한이 있음으로 설정하고 종료
							if (await m_userManager.IsInRoleAsync(user, roleName))
							{
								result.Result = EnumResponseResult.Success;
								break;
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

		/// <summary>특정 사용자에 역할을 추가한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="request">역할 정보</param>
		/// <returns>역할 등록 결과</returns>
		public async Task<ResponseData> AddRole(string id, RequestAddRoleToUser request)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (id.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효하지 않은 경우
				else if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					user = await this.GetUserById(id);

					// 해당 계정을 찾을 수 없는 경우
					if (user == null || user.IsDeleted)
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 해당 역할에 포함되어 있지 않은 경우
						if (!await m_userManager.IsInRoleAsync(user, request.RoleName))
						{
							// 해당 역할 추가
							await m_userManager.AddToRoleAsync(user, request.RoleName);
							result.Result = EnumResponseResult.Success;
							result.Code = Resource.SC_COMMON__SUCCESS;
							result.Message = Resource.SM_COMMON__ADDED;

							// 로그인 사용자 정보가 존재하는 경우
							if (LoginUser != null)
							{
								// 로그 기록
								await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
																, Resource.SM_COMMON_ACCOUNT_USER_ADD_ROLE, user.LoginId, request.RoleName);
							}
							// 로그인 사용자 정보가 존재하지 않는 경우
							else
							{
								// 로그 기록
								await m_systemLogProvider.Add(EnumLogLevel.Information
																, Resource.SM_COMMON_ACCOUNT_USER_ADD_ROLE, user.LoginId, request.RoleName);
							}
						}
						// 해당 역할에 포함되어 있는 경우
						else
						{
							result.Result = EnumResponseResult.Warning;
							result.Code = Resource.EC_COMMON__ALREADY_EXIST;
							result.Message = Resource.EM_COMMON__ALREADY_EXIST;
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

		/// <summary>역할에서 역할을 삭제한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="roleName">역할명</param>
		/// <returns>역할 삭제 결과</returns>
		public async Task<ResponseData> RemoveRole(string id, string roleName)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 사용자 식별자가 유효하지 않은 경우
				if (id.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 값이 유효하지 않은 경우
				else if (roleName.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 계정을 가져온다.
					user = await this.GetUserById(id);

					// 해당 계정을 찾을 수 없는 경우
					if (user == null || user.IsDeleted)
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 역할 정보를 가져온다.
						NNApplicationRole role = await m_roleManager.FindByNameAsync(roleName);

						// 해당 역할을 찾은 경우
						if (role != null)
						{
							// 해당 역할에 포함되어 있는 경우
							if (await m_userManager.IsInRoleAsync(user, role.Name))
							{
								// 해당 역할 추가
								await m_userManager.RemoveFromRoleAsync(user, role.Name);
								result.Result = EnumResponseResult.Success;
								result.Code = Resource.SC_COMMON__SUCCESS;
								result.Message = Resource.SM_COMMON__REMOVED;

								// 로그 기록
								await m_userActionLogProvider.Add(EnumLogLevel.Information, this.LoginUser, UserIpAddress
																, Resource.SM_COMMON_ACCOUNT_USER_REMOVE_ROLE, user.LoginId, roleName);
							}
							// 해당 역할에 포함되어 있지 않은 경우
							else
							{
								result.Result = EnumResponseResult.Warning;
								result.Code = Resource.EC_COMMON__NOT_FOUND;
								result.Message = Resource.EM_COMMON__NOT_FOUND;
							}
						}
						// 해당 역할을 찾을 수 없는 경우
						else
						{
							result.Result = EnumResponseResult.Error;
							result.Code = Resource.EC_COMMON_ACCOUNT_ROLE_NOT_FOUND;
							result.Message = Resource.EM_COMMON_ACCOUNT_ROLE_NOT_FOUND;
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

		/// <summary>사용자 수를 반환한다.</summary>
		/// <returns></returns>
		public async Task<int> UserCount()
		{
			int result = 0;
			try
			{
				result = await m_dbContext.Users.AsNoTracking().CountAsync();
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}
	}
}
