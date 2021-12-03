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
	/// <summary>역할 프로바이더</summary>
	public class RoleProvider : BaseProvider<PortalModel>, IRoleProvider
	{
		/// <summary>역할 매니져</summary>
		protected readonly RoleManager<NNApplicationRole> m_roleManager;

		/// <summary>사용자 프로바이더</summary>
		protected readonly IUserProvider m_userProvider;

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		/// <param name="userProvider">계정 프로바이더</param>
		/// <param name="roleManager">역할 관리자</param>
		public RoleProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<RoleProvider> logger,
			IUserProvider userProvider,
			RoleManager<NNApplicationRole> roleManager
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
			m_userProvider = userProvider;
			m_roleManager = roleManager;
		}

		/// <summary>역할을 추가한다.</summary>
		/// <param name="request">역할 정보</param>
		/// <returns>역할 등록 결과</returns>
		public async Task<ResponseData<ResponseRole>> AddRole(RequestRole request)
		{
			ResponseData<ResponseRole> result = new ResponseData<ResponseRole>();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseRole>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 해당 역할이 존재하지 않는 경우 생성
				if (!await m_roleManager.RoleExistsAsync(request.Name))
				{
					NNApplicationRole role = new NNApplicationRole {Id = Guid.NewGuid(), Name = request.Name};
					await m_roleManager.CreateAsync(role);
					result.Data = new ResponseRole();
					result.Data.Id = role.Id;
					result.Data.Name = role.Name;
					result.Result = EnumResponseResult.Success;
				}
				// 해당 역할이 존재하는 경우
				else
					return new ResponseData<ResponseRole>(EnumResponseResult.Warning, Resource.EC_COMMON__ALREADY_EXIST, Resource.EM_COMMON__ALREADY_EXIST);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>역할을 수정한다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <param name="request">역할 정보</param>
		/// <returns>역할 등록 결과</returns>
		public async Task<ResponseData> UpdateRole(string id, RequestRole request)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
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
					// 해당 역할 아이디의 역할을 가져온다.
					NNApplicationRole role = await m_roleManager.FindByIdAsync(id);

					// 해당 역할이 존재하는 경우
					if (role != null)
					{
						// 수정할 Role이 미리 정의된 역할인 경우
						if (role.Name == PredefinedRoleNames.RoleNameSupervisor
						    || role.Name == PredefinedRoleNames.RoleNameInternalService)
						{
							result.Code = Resource.EC_COMMON_ACCOUNT_ROLE_PREDEFINED_ROLE_CANNOT_UPDATE;
							result.Message = Resource.EM_COMMON_ACCOUNT_ROLE_PREDEFINED_ROLE_CANNOT_UPDATE;
						}
						// 수정할 수 있는 경우
						else
						{
							role.Name = request.Name;
							await m_roleManager.UpdateAsync(role);
							result.Result = EnumResponseResult.Success;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할을 삭제한다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <returns>역할 삭제 결과</returns>
		public async Task<ResponseData> RemoveRole(string id)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (id.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할 아이디의 역할을 가져온다.
					NNApplicationRole role = await m_roleManager.FindByIdAsync(id);

					// 해당 역할이 존재하는 경우
					if (role != null)
					{
						// 삭제할 Role이 미리 정의된 역할인 경우
						if (role.Name == PredefinedRoleNames.RoleNameSupervisor
						    || role.Name == PredefinedRoleNames.RoleNameInternalService)
						{
							result.Code = Resource.EC_COMMON_ACCOUNT_ROLE_PREDEFINED_ROLE_CANNOT_DELETE;
							result.Message = Resource.EM_COMMON_ACCOUNT_ROLE_PREDEFINED_ROLE_CANNOT_DELETE;
						}
						// 삭제할 수 있는 Role인 경우
						else
						{
							await m_roleManager.DeleteAsync(role);
							result.Result = EnumResponseResult.Success;
							result.Message = Resource.SM_COMMON__DELETED;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						result.Result = EnumResponseResult.Warning;
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>전체 역할을 가져온다.</summary>
		/// <returns>역할 목록</returns>
		public async Task<ResponseList<ResponseRole>> GetRoles()
		{
			ResponseList<ResponseRole> result = new ResponseList<ResponseRole>();
			List<ResponseRole> responseRoles = new List<ResponseRole>();
			try
			{
				// 역할 목록을 가져온다.
				List<Role> roles = await m_dbContext.Roles.AsNoTracking()
					.Include(i => i.RoleClaims)
					.Include(i => i.UserRoles)
					.ToListAsync();

				if (roles != null)
				{
					Role supervisorRole = roles.FirstOrDefault(i => i.Name == "Supervisor");
					if (supervisorRole != null)
						responseRoles.Add(new ResponseRole()
							{Id = supervisorRole.Id, Name = supervisorRole.Name, ClaimCount = supervisorRole.RoleClaims.Count, UserCount = supervisorRole.UserRoles.Count});

					Role adminRole = roles.FirstOrDefault(i => i.Name == "Admin");
					if (adminRole != null)
						responseRoles.Add(new ResponseRole() {Id = adminRole.Id, Name = adminRole.Name, ClaimCount = adminRole.RoleClaims.Count, UserCount = adminRole.UserRoles.Count});

					Role userRole = roles.FirstOrDefault(i => i.Name == "User");
					if (userRole != null)
						responseRoles.Add(new ResponseRole() {Id = userRole.Id, Name = userRole.Name, ClaimCount = userRole.RoleClaims.Count, UserCount = userRole.UserRoles.Count});

					// 모든 역할에 대해서 처리
					foreach (Role role in roles.Where(i => i.Name != "Supervisor" && i.Name != "Admin" && i.Name != "User"))
						responseRoles.Add(new ResponseRole() {Id = role.Id, Name = role.Name, ClaimCount = role.RoleClaims.Count, UserCount = role.UserRoles.Count});
				}

				// 목록으로 변환
				result.Data = responseRoles.CreateList();
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

		/// <summary>역할 아이디로 특정 역할을 가져온다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <returns>해당 역할 데이터</returns>
		public async Task<ResponseData<ResponseRole>> GetRoleById(string id)
		{
			ResponseData<ResponseRole> result = new ResponseData<ResponseRole>();
			try
			{
				Guid searchId;

				// 역할 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out searchId))
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할 정보를 가져온다.
					Role role = await m_dbContext.Roles.AsNoTracking()
						.Where(i => i.Id == searchId)
						.Include(i => i.RoleClaims)
						.Include(i => i.UserRoles)
						.FirstOrDefaultAsync();

					// 해당 역할 정보가 존재하는 경우
					if (role != null)
					{
						result.Data = new ResponseRole() {Id = role.Id, Name = role.Name, ClaimCount = role.RoleClaims.Count, UserCount = role.UserRoles.Count};
						result.Result = EnumResponseResult.Success;
					}
					// 해당 역할 정보가 존재하지 않는 경우
					else
					{
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할명으로 특정 역할을 가져온다.</summary>
		/// <param name="name">역할명</param>
		/// <returns>해당 역할 데이터</returns>
		public async Task<ResponseData<ResponseRole>> GetRoleByName(string name)
		{
			ResponseData<ResponseRole> result = new ResponseData<ResponseRole>();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (name.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할 정보를 가져온다.
					Role role = await m_dbContext.Roles.AsNoTracking()
						.Where(i => i.Name == name)
						.Include(i => i.RoleClaims)
						.Include(i => i.UserRoles)
						.FirstOrDefaultAsync();

					// 해당 역할 정보가 존재하는 경우
					if (role != null)
					{
						result.Data = new ResponseRole() {Id = role.Id, Name = role.Name, ClaimCount = role.RoleClaims.Count, UserCount = role.UserRoles.Count};
						result.Result = EnumResponseResult.Success;
					}
					// 해당 역할 정보가 존재하지 않는 경우
					else
					{
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>특정 역할에 대한 권한 목록을 가져온다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>특정 역할에 대한 사용자 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetRoleClaims(string id, int skip = 0, int countPerPage = int.MaxValue, string searchKeyword = "")
		{
			ResponseList<ResponseClaim> result = new ResponseList<ResponseClaim>();
			try
			{
				Guid searchId;

				// 역할 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out searchId))
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할의 권한들을 가져온다.
					result.Data = await m_dbContext.RoleClaims.AsNoTracking()
						.Where(i => i.RoleId == searchId
						            && (searchKeyword.IsEmpty() || i.ClaimValue.Contains(searchKeyword)))
						.CreateListAsync<RoleClaim, ResponseClaim>(skip, countPerPage);

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

		/// <summary>특정 역할에 대한 사용자 목록을 가져온다.</summary>
		/// <param name="id">역할 아이디</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지당 레코드 수 (옵션, 기본 20)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>특정 역할에 대한 사용자 목록</returns>
		public async Task<ResponseList<ResponseUser>> GetRoleUsers(string id, int skip = 0, int countPerPage = 100, string searchKeyword = "")
		{
			ResponseList<ResponseUser> result = new ResponseList<ResponseUser>();
			try
			{
				Guid searchId;

				// 역할 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out searchId))
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할의 사용자를 가져온다.
					result.Data = await m_dbContext.UserRoles.AsNoTracking()
						.Join(m_dbContext.Users, i => i.UserId, i => i.Id, (i, j) => new {Role = i, User = j})
						.Where(i => i.Role.RoleId == searchId
						            && i.User.IsDeleted == false
						            && (searchKeyword.IsEmpty()
						                || i.User.LoginId.Contains(searchKeyword)
						                || i.User.Email.Contains(searchKeyword)
						                || i.User.Name.Contains(searchKeyword)
						            ))
						//.Select(i => i.User)
						.Select(i => new
						{
							Id = i.User.Id.ToString(),
							i.User.LoginId,
							i.User.Email,
							i.User.Name,
							i.User.Code,
							Status = !i.User.EmailConfirmed ? EnumUserStatus.VerifyingEmail : i.User.LockoutEnd > DateTime.Now ? EnumUserStatus.Locked : EnumUserStatus.Activated,
						})
						.OrderByWithDirection(i => i.Name)
						.CreateListAsync<dynamic, ResponseUser>(skip, countPerPage);

					// 모든 사용자에 대해서 처리
					foreach (ResponseUser user in result.Data.Items)
					{
						NNApplicationUser applicationUser = await m_userProvider.GetUserById(user.Id);
						user.Status = await m_userManager.GetUserStatus(applicationUser);
					}

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

		/// <summary>역할에 사용자를 추가한다.</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="request">사용자 정보</param>
		/// <returns>사용자 추가 결과</returns>
		public async Task<ResponseData> AddUserToRole(string roleId, RequestAddUserToRole request)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (roleId.IsEmpty())
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
					// 해당 역할 아이디의 역할을 가져온다.
					NNApplicationRole role = await m_roleManager.FindByIdAsync(roleId);

					// 해당 역할이 존재하는 경우
					if (role != null)
					{
						// 해당 사용자를 가져온다.
						NNApplicationUser user = await m_userProvider.GetUserById(request.Id);

						// 해당 사용자가 존재하는 경우
						if (user != null)
						{
							// 해당 역할에 포함되어 있지 않은 경우
							if (!await m_userManager.IsInRoleAsync(user, role.Name))
							{
								await m_userManager.AddToRoleAsync(user, role.Name);
								result.Result = EnumResponseResult.Success;
							}
							// 해당 역할에 포함되어 있는 경우
							else
							{
								result.Result = EnumResponseResult.Warning;
								result.Code = Resource.EC_COMMON__ALREADY_EXIST;
								result.Message = Resource.EM_COMMON__ALREADY_EXIST;
							}
						}
						// 해당 사용자가 존재하지 않는 경우
						else
						{
							result.Result = EnumResponseResult.Error;
							result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할에 사용자를 추가한다.</summary>
		/// <param name="roleName">역할명</param>
		/// <param name="request">사용자 정보</param>
		/// <returns>사용자 추가 결과</returns>
		public async Task<ResponseData> AddUserToRoleByName(string roleName, RequestAddUserToRole request)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 역할명이 유효하지 않은 경우
				if (roleName.IsEmpty())
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
					// 해당 역할명의 역할을 가져온다.
					NNApplicationRole role = await m_roleManager.FindByNameAsync(roleName);

					// 해당 역할이 존재하는 경우
					if (role != null)
					{
						// 해당 사용자를 가져온다.
						NNApplicationUser user = await m_userProvider.GetUserById(request.Id);

						// 해당 사용자가 존재하는 경우
						if (user != null)
						{
							// 해당 역할에 포함되어 있지 않은 경우
							if (!await m_userManager.IsInRoleAsync(user, role.Name))
							{
								await m_userManager.AddToRoleAsync(user, role.Name);
								result.Result = EnumResponseResult.Success;
							}
							// 해당 역할에 포함되어 있는 경우
							else
							{
								result.Result = EnumResponseResult.Warning;
								result.Code = Resource.EC_COMMON__ALREADY_EXIST;
								result.Message = Resource.EM_COMMON__ALREADY_EXIST;
							}
						}
						// 해당 사용자가 존재하지 않는 경우
						else
						{
							result.Result = EnumResponseResult.Error;
							result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할에 권한을 추가한다.</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="request">권한 정보</param>
		/// <returns>권한 추가 결과</returns>
		public async Task<ResponseData> AddClaimToRole(string roleId, RequestAddClaimToRole request)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (roleId.IsEmpty())
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
					// 해당 역할 아이디의 역할을 가져온다.
					NNApplicationRole role = await m_roleManager.FindByIdAsync(roleId);

					// 해당 역할이 존재하는 경우
					if (role != null)
					{
						bool exist = false;

						// 해당 역할의 기존 권한 목록을 가져온다.
						IList<Claim> claims = await m_roleManager.GetClaimsAsync(role);
						if (claims != null)
						{
							// 해당 역할의 권한 중에 해당 권한이 있는지 검사
							if (claims.Where(i => i.Type == "Permission" && i.Value == request.ClaimValue).Any())
								exist = true;
						}

						// 해당 역할에 권한이 포함되어 있지 않은 경우
						if (!exist)
						{
							await m_roleManager.AddClaimAsync(role, new Claim("Permission", request.ClaimValue));
							result.Result = EnumResponseResult.Success;
						}
						// 해당 역할에 권한이 포함되어 있는 경우
						else
						{
							result.Result = EnumResponseResult.Warning;
							result.Code = Resource.EC_COMMON__ALREADY_EXIST;
							result.Message = Resource.EM_COMMON__ALREADY_EXIST;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할의 권한을 수정한다.</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="request">역할 권한 목록 객체</param>
		/// <returns>역할 권한 수정 결과</returns>
		public async Task<ResponseData> UpdateRoleClaims(string roleId, List<RequestRoleClaim> request)
		{
			ResponseData result = new ResponseData();
			try
			{
				Guid searchId;

				// 역할 아이디가 유효하지 않은 경우
				if (roleId.IsEmpty() || !Guid.TryParse(roleId, out searchId))
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효하지 않은 경우
				else if (request == null)
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 요청한 모든 권한에 대해서 처리
					foreach (RequestRoleClaim claim in request)
					{
						// 파라미터가 유효하지 않은 경우
						if (!claim.IsValid())
						{
							result.Code = claim.GetErrorCode();
							result.Message = claim.GetErrorMessage();
							return result;
						}
					}

					// 해당 역할 아이디의 역할을 가져온다.
					NNApplicationRole role = await m_roleManager.FindByIdAsync(roleId);

					// 해당 역할이 존재하지 않는 경우
					if (role == null)
					{
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
					}
					// 해당 역할이 존재하는 경우
					else
					{
						using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
						{
							try
							{
								// 기존 역할 권한을 가져온다.
								List<RoleClaim> oldClaims = await m_dbContext.RoleClaims
									.Where(i => i.RoleId == searchId)
									.ToListAsync();

								// 기존 역할 권한 삭제
								m_dbContext.RoleClaims.RemoveRange(oldClaims);
								await this.m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

								// 요청한 모든 권한에 대해서 처리
								foreach (RequestRoleClaim claim in request)
								{
									// 역할 권한 추가
									await m_dbContext.RoleClaims.AddAsync(new RoleClaim()
									{
										RoleId = Guid.Parse(roleId),
										ClaimType = "Permission",
										ClaimValue = claim.ClaimValue
									});
								}

								await this.m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

								transaction.Commit();

								result.Result = EnumResponseResult.Success;
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

		/// <summary>역할에서 사용자를 삭제한다.</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="userId">사용자 식별자</param>
		/// <returns>사용자 추가 결과</returns>
		public async Task<ResponseData> RemoveUserFromRole(string roleId, string userId)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (roleId.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효하지 않은 경우
				else if (userId.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 역할 아이디의 역할을 가져온다.
					NNApplicationRole role = await m_roleManager.FindByIdAsync(roleId);

					// 해당 역할이 존재하는 경우
					if (role != null)
					{
						// 해당 사용자를 가져온다.
						NNApplicationUser user = await m_userProvider.GetUserById(userId);

						// 해당 사용자가 존재하는 경우
						if (user != null)
						{
							// 해당 역할에 포함되어 있는 경우
							if (await m_userManager.IsInRoleAsync(user, role.Name))
							{
								// 역할에서 해당 사용자 삭제
								await m_userManager.RemoveFromRoleAsync(user, role.Name);
								result.Result = EnumResponseResult.Success;
							}
							// 해당 역할에 포함되어 있지 않은 경우
							else
							{
								result.Result = EnumResponseResult.Warning;
								result.Code = Resource.EC_COMMON__NOT_FOUND;
								result.Message = Resource.EM_COMMON__NOT_FOUND;
							}
						}
						// 해당 사용자가 존재하지 않는 경우
						else
						{
							result.Result = EnumResponseResult.Error;
							result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할에서 사용자를 삭제한다.</summary>
		/// <param name="roleName">역할명</param>
		/// <param name="userId">사용자 식별자</param>
		/// <returns>사용자 추가 결과</returns>
		public async Task<ResponseData> RemoveUserFromRoleByName(string roleName, string userId)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 역할명이 유효하지 않은 경우
				if (roleName.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효하지 않은 경우
				else if (userId.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 역할명의 역할을 가져온다.
					NNApplicationRole role = await m_roleManager.FindByNameAsync(roleName);

					// 해당 역할이 존재하는 경우
					if (role != null)
					{
						// 해당 사용자를 가져온다.
						NNApplicationUser user = await m_userProvider.GetUserById(userId);

						// 해당 사용자가 존재하는 경우
						if (user != null)
						{
							// 해당 역할에 포함되어 있는 경우
							if (await m_userManager.IsInRoleAsync(user, role.Name))
							{
								// 역할에서 해당 사용자 삭제
								await m_userManager.RemoveFromRoleAsync(user, role.Name);
								result.Result = EnumResponseResult.Success;
							}
							// 해당 역할에 포함되어 있지 않은 경우
							else
							{
								result.Result = EnumResponseResult.Warning;
								result.Code = Resource.EC_COMMON__NOT_FOUND;
								result.Message = Resource.EM_COMMON__NOT_FOUND;
							}
						}
						// 해당 사용자가 존재하지 않는 경우
						else
						{
							result.Result = EnumResponseResult.Error;
							result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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
		/// <param name="roleId">역할 아이디</param>
		/// <param name="claimValue">권한값</param>
		/// <returns>권한 추가 결과</returns>
		public async Task<ResponseData> RemoveClaimFromRole(string roleId, string claimValue)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (roleId.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효하지 않은 경우
				else if (claimValue.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 역할 아이디의 역할을 가져온다.
					NNApplicationRole role = await m_roleManager.FindByIdAsync(roleId);

					// 해당 역할이 존재하는 경우
					if (role != null)
					{
						Claim claim = null;

						// 해당 역할의 기존 권한 목록을 가져온다.
						IList<Claim> claims = await m_roleManager.GetClaimsAsync(role);
						if (claims != null)
						{
							// 해당 역할의 권한 중에 해당 권한이 있는지 검사
							claim = claims.Where(i => i.Type == "Permission" && i.Value == claimValue).FirstOrDefault();
						}

						// 해당 역할에 권한이 포함되어 있는 경우
						if (claim != null)
						{
							await m_roleManager.RemoveClaimAsync(role, claim);
							result.Result = EnumResponseResult.Success;
						}
						// 해당 역할에 권한이 포함되어 있지 않은 경우
						else
						{
							result.Result = EnumResponseResult.Warning;
							result.Code = Resource.EC_COMMON__NOT_FOUND;
							result.Message = Resource.EM_COMMON__NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						result.Result = EnumResponseResult.Error;
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>권한 제목을 추가한다.</summary>
		/// <param name="claimType">권한 타입</param>
		/// <param name="claimValue">권한 값</param>
		/// <param name="claimTitle">권한 제목</param>
		/// <param name="depth">뎁스</param>
		/// <param name="orderNo">순서</param>
		/// <returns>권한 제목 추가 결과</returns>
		public async Task<ResponseData> AddClaimTitle(string claimType, string claimValue, string claimTitle, short depth, string orderNo)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (claimType.IsEmpty() || claimValue.IsEmpty() || claimTitle.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 권한 제목이 존재하지 않는 경우
					if (!await m_dbContext.ClaimNames.AsNoTracking().Where(i => i.ClaimType == claimType && i.ClaimValue == claimValue).AnyAsync())
					{
						// 권한 제목 등록
						await m_dbContext.ClaimNames.AddAsync(new ClaimName() {ClaimType = claimType, ClaimValue = claimValue, ClaimTitle = claimTitle, Depth = depth, OrderNo = orderNo});
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
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

		/// <summary>모든 권한 목록을 가져온다.</summary>
		/// <returns>권한 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetAllClaims()
		{
			ResponseList<ResponseClaim> result = new ResponseList<ResponseClaim>();
			try
			{
				// 전체 권한 정보를 가져온다.
				result.Data = await m_dbContext.ClaimNames.AsNoTracking()
					.OrderByWithDirection(i => i.OrderNo)
					.CreateListAsync<ClaimName, ResponseClaim>();

				// 모든 권한에 대해서 처리
				foreach (ResponseClaim claim in result.Data.Items)
				{
					claim.HasChild = result.Data.Items
						.Any(i =>
							i.ClaimValue.StartsWith(claim.ClaimValue)
							&& i.Depth == claim.Depth + 1
						);
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

		/// <summary>특정 권한의 하위 권한 목록을 가져온다.</summary>
		/// <param name="parentId">상위 권한 아이디</param>
		/// <returns>권한 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetClaims(int parentId)
		{
			ResponseList<ResponseClaim> result = new ResponseList<ResponseClaim>();
			ResponseClaim parentClaim;
			try
			{
				// 상위 권한 정보를 가져온다.
				parentClaim = await m_dbContext.ClaimNames.AsNoTracking()
					.Where(i => i.Id == parentId)
					.FirstOrDefaultAsync<ClaimName, ResponseClaim>();

				if (parentClaim != null)
				{
					// 하위 권한 정보를 가져온다.
					result.Data = await m_dbContext.ClaimNames.AsNoTracking()
						.Where(i => i.ClaimValue.StartsWith(parentClaim.ClaimValue) && i.Depth == parentClaim.Depth + 1)
						.OrderByWithDirection(i => i.OrderNo)
						.CreateListAsync<ClaimName, ResponseClaim>();

					// 모든 권한에 대해서 처리
					foreach (ResponseClaim claim in result.Data.Items)
					{
						claim.HasChild = result.Data.Items
							.Any(i =>
								i.ClaimValue.StartsWith(claim.ClaimValue)
								&& i.Depth == claim.Depth + 1
							);
					}
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
	}
}