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
		/// <param name="Request">역할 정보</param>
		/// <returns>역할 등록 결과</returns>
		public async Task<ResponseData<ResponseRole>> AddRole(RequestRole Request)
		{
			var Result = new ResponseData<ResponseRole>();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseRole>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 해당 역할이 존재하지 않는 경우 생성
				if (!await m_roleManager.RoleExistsAsync(Request.Name))
				{
					var Role = new NNApplicationRole { Id = Guid.NewGuid(), Name = Request.Name };
					await m_roleManager.CreateAsync(Role);
					Result.Data = new ResponseRole();
					Result.Data.Id = Role.Id;
					Result.Data.Name = Role.Name;
					Result.Result = EnumResponseResult.Success;
				}
				// 해당 역할이 존재하는 경우
				else
					return new ResponseData<ResponseRole>(EnumResponseResult.Warning, Resource.EC_COMMON__ALREADY_EXIST, Resource.EM_COMMON__ALREADY_EXIST);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>역할을 수정한다.</summary>
		/// <param name="Id">역할 아이디</param>
		/// <param name="Request">역할 정보</param>
		/// <returns>역할 등록 결과</returns>
		public async Task<ResponseData> UpdateRole(string Id, RequestRole Request)
		{
			var Result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
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
					// 해당 역할 아이디의 역할을 가져온다.
					var Role = await m_roleManager.FindByIdAsync(Id);

					// 해당 역할이 존재하는 경우
					if (Role != null)
					{
						// 수정할 Role이 미리 정의된 역할인 경우
						if (Role.Name == PredefinedRoleNames.RoleNameSupervisor || Role.Name == PredefinedRoleNames.RoleNameInternalService)
						{
							Result.Code = Resource.EC_COMMON_ACCOUNT_ROLE_PREDEFINED_ROLE_CANNOT_UPDATE;
							Result.Message = Resource.EM_COMMON_ACCOUNT_ROLE_PREDEFINED_ROLE_CANNOT_UPDATE;
						}
						// 수정할 수 있는 경우
						else
						{
							Role.Name = Request.Name;
							await m_roleManager.UpdateAsync(Role);
							Result.Result = EnumResponseResult.Success;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할을 삭제한다.</summary>
		/// <param name="Id">역할 아이디</param>
		/// <returns>역할 삭제 결과</returns>
		public async Task<ResponseData> RemoveRole(string Id)
		{
			var Result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할 아이디의 역할을 가져온다.
					var Role = await m_roleManager.FindByIdAsync(Id);

					// 해당 역할이 존재하는 경우
					if (Role != null)
					{
						// 삭제할 Role이 미리 정의된 역할인 경우
						if (Role.Name == PredefinedRoleNames.RoleNameSupervisor || Role.Name == PredefinedRoleNames.RoleNameInternalService)
						{
							Result.Code = Resource.EC_COMMON_ACCOUNT_ROLE_PREDEFINED_ROLE_CANNOT_DELETE;
							Result.Message = Resource.EM_COMMON_ACCOUNT_ROLE_PREDEFINED_ROLE_CANNOT_DELETE;
						}
						// 삭제할 수 있는 Role인 경우
						else
						{
							await m_roleManager.DeleteAsync(Role);
							Result.Result = EnumResponseResult.Success;
							Result.Message = Resource.SM_COMMON__DELETED;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						Result.Result = EnumResponseResult.Warning;
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>전체 역할을 가져온다.</summary>
		/// <returns>역할 목록</returns>
		public async Task<ResponseList<ResponseRole>> GetRoles()
		{
			var Result = new ResponseList<ResponseRole>();
			var ResponseRoles = new List<ResponseRole>();
			try
			{
				// 역할 목록을 가져온다.
				var Roles = await m_dbContext.Roles.AsNoTracking()
					.Include(i => i.RoleClaims)
					.Include(i => i.UserRoles)
					.ToListAsync();

				if (Roles != null)
				{
					var SupervisorRole = Roles.FirstOrDefault(i => i.Name == "Supervisor");
					if (SupervisorRole != null)
						ResponseRoles.Add(new ResponseRole()
						{ Id = SupervisorRole.Id, Name = SupervisorRole.Name, ClaimCount = SupervisorRole.RoleClaims.Count, UserCount = SupervisorRole.UserRoles.Count });

					var AdminRole = Roles.FirstOrDefault(i => i.Name == "Admin");
					if (AdminRole != null)
						ResponseRoles.Add(new ResponseRole() { Id = AdminRole.Id, Name = AdminRole.Name, ClaimCount = AdminRole.RoleClaims.Count, UserCount = AdminRole.UserRoles.Count });

					var UserRole = Roles.FirstOrDefault(i => i.Name == "User");
					if (UserRole != null)
						ResponseRoles.Add(new ResponseRole() { Id = UserRole.Id, Name = UserRole.Name, ClaimCount = UserRole.RoleClaims.Count, UserCount = UserRole.UserRoles.Count });

					// 모든 역할에 대해서 처리
					foreach (var Role in Roles.Where(i => i.Name != "Supervisor" && i.Name != "Admin" && i.Name != "User"))
						ResponseRoles.Add(new ResponseRole() { Id = Role.Id, Name = Role.Name, ClaimCount = Role.RoleClaims.Count, UserCount = Role.UserRoles.Count });
				}

				// 목록으로 변환
				Result.Data = ResponseRoles.CreateList();
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

		/// <summary>역할 아이디로 특정 역할을 가져온다.</summary>
		/// <param name="Id">역할 아이디</param>
		/// <returns>해당 역할 데이터</returns>
		public async Task<ResponseData<ResponseRole>> GetRoleById(string Id)
		{
			var Result = new ResponseData<ResponseRole>();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid SearchId))
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할 정보를 가져온다.
					var Role = await m_dbContext.Roles.AsNoTracking()
						.Where(i => i.Id == SearchId)
						.Include(i => i.RoleClaims)
						.Include(i => i.UserRoles)
						.FirstOrDefaultAsync();

					// 해당 역할 정보가 존재하는 경우
					if (Role != null)
					{
						Result.Data = new ResponseRole() { Id = Role.Id, Name = Role.Name, ClaimCount = Role.RoleClaims.Count, UserCount = Role.UserRoles.Count };
						Result.Result = EnumResponseResult.Success;
					}
					// 해당 역할 정보가 존재하지 않는 경우
					else
					{
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할명으로 특정 역할을 가져온다.</summary>
		/// <param name="Name">역할명</param>
		/// <returns>해당 역할 데이터</returns>
		public async Task<ResponseData<ResponseRole>> GetRoleByName(string Name)
		{
			var Result = new ResponseData<ResponseRole>();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (Name.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할 정보를 가져온다.
					var Role = await m_dbContext.Roles.AsNoTracking()
						.Where(i => i.Name == Name)
						.Include(i => i.RoleClaims)
						.Include(i => i.UserRoles)
						.FirstOrDefaultAsync();

					// 해당 역할 정보가 존재하는 경우
					if (Role != null)
					{
						Result.Data = new ResponseRole() { Id = Role.Id, Name = Role.Name, ClaimCount = Role.RoleClaims.Count, UserCount = Role.UserRoles.Count };
						Result.Result = EnumResponseResult.Success;
					}
					// 해당 역할 정보가 존재하지 않는 경우
					else
					{
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>특정 역할에 대한 권한 목록을 가져온다.</summary>
		/// <param name="Id">역할 아이디</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지당 레코드 수 (옵션, 기본 int.MaxValue)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>특정 역할에 대한 사용자 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetRoleClaims(string Id, int Skip = 0, int CountPerPage = int.MaxValue, string SearchKeyword = null)
		{
			ResponseList<ResponseClaim> Result = new ResponseList<ResponseClaim>();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid SearchId))
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할의 권한들을 가져온다.
					Result.Data = await m_dbContext.RoleClaims.AsNoTracking()
						.Where(i => i.RoleId == SearchId
									&& (SearchKeyword.IsEmpty() || i.ClaimValue.Contains(SearchKeyword)))
						.CreateListAsync<RoleClaim, ResponseClaim>(Skip, CountPerPage);

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

		/// <summary>특정 역할에 대한 사용자 목록을 가져온다.</summary>
		/// <param name="Id">역할 아이디</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지당 레코드 수 (옵션, 기본 20)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>특정 역할에 대한 사용자 목록</returns>
		public async Task<ResponseList<ResponseUser>> GetRoleUsers(string Id, int Skip = 0, int CountPerPage = 100, string SearchKeyword = null)
		{
			ResponseList<ResponseUser> Result = new ResponseList<ResponseUser>();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid SearchId))
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 역할 아이디가 유효한 경우
				else
				{
					// 해당 역할의 사용자를 가져온다.
					Result.Data = await m_dbContext.UserRoles.AsNoTracking()
						.Join(m_dbContext.Users, i => i.UserId, i => i.Id, (i, j) => new { Role = i, User = j })
						.Where(i => i.Role.RoleId == SearchId
									&& !i.User.IsDeleted
									&& (SearchKeyword.IsEmpty()
										|| i.User.LoginId.Contains(SearchKeyword)
										|| i.User.Email.Contains(SearchKeyword)
										|| i.User.Name.Contains(SearchKeyword)
									))
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
						.CreateListAsync<dynamic, ResponseUser>(Skip, CountPerPage);

					// 모든 사용자에 대해서 처리
					foreach (var user in Result.Data.Items)
					{
						NNApplicationUser applicationUser = await m_userProvider.GetUserById(user.Id);
						user.Status = await m_userManager.GetUserStatus(applicationUser);
					}

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

		/// <summary>역할에 사용자를 추가한다.</summary>
		/// <param name="RoleId">역할 아이디</param>
		/// <param name="Request">사용자 정보</param>
		/// <returns>사용자 추가 결과</returns>
		public async Task<ResponseData> AddUserToRole(string RoleId, RequestAddUserToRole Request)
		{
			var Result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (RoleId.IsEmpty())
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
					// 해당 역할 아이디의 역할을 가져온다.
					var Role = await m_roleManager.FindByIdAsync(RoleId);

					// 해당 역할이 존재하는 경우
					if (Role != null)
					{
						// 해당 사용자를 가져온다.
						var User = await m_userProvider.GetUserById(Request.Id);

						// 해당 사용자가 존재하는 경우
						if (User != null)
						{
							// 해당 역할에 포함되어 있지 않은 경우
							if (!await m_userManager.IsInRoleAsync(User, Role.Name))
							{
								await m_userManager.AddToRoleAsync(User, Role.Name);
								Result.Result = EnumResponseResult.Success;
							}
							// 해당 역할에 포함되어 있는 경우
							else
							{
								Result.Result = EnumResponseResult.Warning;
								Result.Code = Resource.EC_COMMON__ALREADY_EXIST;
								Result.Message = Resource.EM_COMMON__ALREADY_EXIST;
							}
						}
						// 해당 사용자가 존재하지 않는 경우
						else
						{
							Result.Result = EnumResponseResult.Error;
							Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할에 사용자를 추가한다.</summary>
		/// <param name="RoleName">역할명</param>
		/// <param name="Request">사용자 정보</param>
		/// <returns>사용자 추가 결과</returns>
		public async Task<ResponseData> AddUserToRoleByName(string RoleName, RequestAddUserToRole Request)
		{
			var Result = new ResponseData();
			try
			{
				// 역할명이 유효하지 않은 경우
				if (RoleName.IsEmpty())
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
					// 해당 역할명의 역할을 가져온다.
					var Role = await m_roleManager.FindByNameAsync(RoleName);

					// 해당 역할이 존재하는 경우
					if (Role != null)
					{
						// 해당 사용자를 가져온다.
						var User = await m_userProvider.GetUserById(Request.Id);

						// 해당 사용자가 존재하는 경우
						if (User != null)
						{
							// 해당 역할에 포함되어 있지 않은 경우
							if (!await m_userManager.IsInRoleAsync(User, Role.Name))
							{
								await m_userManager.AddToRoleAsync(User, Role.Name);
								Result.Result = EnumResponseResult.Success;
							}
							// 해당 역할에 포함되어 있는 경우
							else
							{
								Result.Result = EnumResponseResult.Warning;
								Result.Code = Resource.EC_COMMON__ALREADY_EXIST;
								Result.Message = Resource.EM_COMMON__ALREADY_EXIST;
							}
						}
						// 해당 사용자가 존재하지 않는 경우
						else
						{
							Result.Result = EnumResponseResult.Error;
							Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할에 권한을 추가한다.</summary>
		/// <param name="RoleId">역할 아이디</param>
		/// <param name="Request">권한 정보</param>
		/// <returns>권한 추가 결과</returns>
		public async Task<ResponseData> AddClaimToRole(string RoleId, RequestAddClaimToRole Request)
		{
			var Result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (RoleId.IsEmpty())
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
					// 해당 역할 아이디의 역할을 가져온다.
					var Role = await m_roleManager.FindByIdAsync(RoleId);

					// 해당 역할이 존재하는 경우
					if (Role != null)
					{
						bool Exist = false;

						// 해당 역할의 기존 권한 목록을 가져온다.
						var claims = await m_roleManager.GetClaimsAsync(Role);

						// 해당 역할의 권한 중에 해당 권한이 있는지 검사
						if (claims != null && claims.Any(i => i.Type == "Permission" && i.Value == Request.ClaimValue)) Exist = true;

						// 해당 역할에 권한이 포함되어 있지 않은 경우
						if (!Exist)
						{
							await m_roleManager.AddClaimAsync(Role, new Claim("Permission", Request.ClaimValue));
							Result.Result = EnumResponseResult.Success;
						}
						// 해당 역할에 권한이 포함되어 있는 경우
						else
						{
							Result.Result = EnumResponseResult.Warning;
							Result.Code = Resource.EC_COMMON__ALREADY_EXIST;
							Result.Message = Resource.EM_COMMON__ALREADY_EXIST;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할의 권한을 수정한다.</summary>
		/// <param name="RoleId">역할 아이디</param>
		/// <param name="Request">역할 권한 목록 객체</param>
		/// <returns>역할 권한 수정 결과</returns>
		public async Task<ResponseData> UpdateRoleClaims(string RoleId, List<RequestRoleClaim> Request)
		{
			var Result = new ResponseData();
			try
			{
				// 역할 아이디가 유효하지 않은 경우
				if (RoleId.IsEmpty() || !Guid.TryParse(RoleId, out Guid SearchId) || Request == null)
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 요청한 모든 권한에 대해서 처리
					foreach (var Claim in Request)
					{
						// 파라미터가 유효하지 않은 경우
						if (!Claim.IsValid())
						{
							Result.Code = Claim.GetErrorCode();
							Result.Message = Claim.GetErrorMessage();
							return Result;
						}
					}

					// 해당 역할 아이디의 역할을 가져온다.
					var Role = await m_roleManager.FindByIdAsync(RoleId);

					// 해당 역할이 존재하지 않는 경우
					if (Role == null)
					{
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
					}
					// 해당 역할이 존재하는 경우
					else
					{
						using var Transaction = await m_dbContext.Database.BeginTransactionAsync();
						try
						{
							// 기존 역할 권한을 가져온다.
							var OldClaims = await m_dbContext.RoleClaims
								.Where(i => i.RoleId == SearchId)
								.ToListAsync();

							// 기존 역할 권한 삭제
							m_dbContext.RoleClaims.RemoveRange(OldClaims);
							await this.m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

							// 요청한 모든 권한에 대해서 처리
							foreach (var Claim in Request)
							{
								// 역할 권한 추가
								await m_dbContext.RoleClaims.AddAsync(new RoleClaim()
								{
									RoleId = Guid.Parse(RoleId),
									ClaimType = "Permission",
									ClaimValue = Claim.ClaimValue
								});
							}

							await this.m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
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
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>역할에서 사용자를 삭제한다.</summary>
		/// <param name="RoleId">역할 아이디</param>
		/// <param name="UserId">사용자 식별자</param>
		/// <returns>사용자 추가 결과</returns>
		public async Task<ResponseData> RemoveUserFromRole(string RoleId, string UserId)
		{
			var Result = new ResponseData();
			try
			{
				// 유효하지 않은 경우
				if (RoleId.IsEmpty() || UserId.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 역할 아이디의 역할을 가져온다.
					var Role = await m_roleManager.FindByIdAsync(RoleId);

					// 해당 역할이 존재하는 경우
					if (Role != null)
					{
						// 해당 사용자를 가져온다.
						var User = await m_userProvider.GetUserById(UserId);

						// 해당 사용자가 존재하는 경우
						if (User != null)
						{
							// 해당 역할에 포함되어 있는 경우
							if (await m_userManager.IsInRoleAsync(User, Role.Name))
							{
								// 역할에서 해당 사용자 삭제
								await m_userManager.RemoveFromRoleAsync(User, Role.Name);
								Result.Result = EnumResponseResult.Success;
							}
							// 해당 역할에 포함되어 있지 않은 경우
							else
							{
								Result.Result = EnumResponseResult.Warning;
								Result.Code = Resource.EC_COMMON__NOT_FOUND;
								Result.Message = Resource.EM_COMMON__NOT_FOUND;
							}
						}
						// 해당 사용자가 존재하지 않는 경우
						else
						{
							Result.Result = EnumResponseResult.Error;
							Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>역할에서 사용자를 삭제한다.</summary>
		/// <param name="RoleName">역할명</param>
		/// <param name="UserId">사용자 식별자</param>
		/// <returns>사용자 추가 결과</returns>
		public async Task<ResponseData> RemoveUserFromRoleByName(string RoleName, string UserId)
		{
			var Result = new ResponseData();
			try
			{
				// 유효하지 않은 경우
				if (RoleName.IsEmpty() || UserId.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 역할명의 역할을 가져온다.
					var Role = await m_roleManager.FindByNameAsync(RoleName);

					// 해당 역할이 존재하는 경우
					if (Role != null)
					{
						// 해당 사용자를 가져온다.
						var User = await m_userProvider.GetUserById(UserId);

						// 해당 사용자가 존재하는 경우
						if (User != null)
						{
							// 해당 역할에 포함되어 있는 경우
							if (await m_userManager.IsInRoleAsync(User, Role.Name))
							{
								// 역할에서 해당 사용자 삭제
								await m_userManager.RemoveFromRoleAsync(User, Role.Name);
								Result.Result = EnumResponseResult.Success;
							}
							// 해당 역할에 포함되어 있지 않은 경우
							else
							{
								Result.Result = EnumResponseResult.Warning;
								Result.Code = Resource.EC_COMMON__NOT_FOUND;
								Result.Message = Resource.EM_COMMON__NOT_FOUND;
							}
						}
						// 해당 사용자가 존재하지 않는 경우
						else
						{
							Result.Result = EnumResponseResult.Error;
							Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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
		/// <param name="RoleId">역할 아이디</param>
		/// <param name="ClaimValue">권한값</param>
		/// <returns>권한 추가 결과</returns>
		public async Task<ResponseData> RemoveClaimFromRole(string RoleId, string ClaimValue)
		{
			var Result = new ResponseData();
			try
			{
				// 유효하지 않은 경우
				if (RoleId.IsEmpty() || ClaimValue.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 역할 아이디의 역할을 가져온다.
					var Role = await m_roleManager.FindByIdAsync(RoleId);

					// 해당 역할이 존재하는 경우
					if (Role != null)
					{
						// 해당 역할의 기존 권한 목록을 가져온다.
						Claim Claim = null;

						var Claims = await m_roleManager.GetClaimsAsync(Role);
						if (Claims != null)
						{
							// 해당 역할의 권한 중에 해당 권한이 있는지 검사
							Claim = Claims.FirstOrDefault(i => i.Type == "Permission" && i.Value == ClaimValue);
						}

						// 해당 역할에 권한이 포함되어 있는 경우
						if (Claim != null)
						{
							await m_roleManager.RemoveClaimAsync(Role, Claim);
							Result.Result = EnumResponseResult.Success;
						}
						// 해당 역할에 권한이 포함되어 있지 않은 경우
						else
						{
							Result.Result = EnumResponseResult.Warning;
							Result.Code = Resource.EC_COMMON__NOT_FOUND;
							Result.Message = Resource.EM_COMMON__NOT_FOUND;
						}
					}
					// 해당 역할이 존재하지 않는 경우
					else
					{
						Result.Result = EnumResponseResult.Error;
						Result.Code = Resource.EC_COMMON__NOT_FOUND;
						Result.Message = Resource.EM_COMMON__NOT_FOUND;
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

		/// <summary>권한 제목을 추가한다.</summary>
		/// <param name="ClaimType">권한 타입</param>
		/// <param name="ClaimValue">권한 값</param>
		/// <param name="ClaimTitle">권한 제목</param>
		/// <param name="Depth">뎁스</param>
		/// <param name="OrderNo">순서</param>
		/// <returns>권한 제목 추가 결과</returns>
		public async Task<ResponseData> AddClaimTitle(string ClaimType, string ClaimValue, string ClaimTitle, short Depth, string OrderNo)
		{
			var Result = new ResponseData();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (ClaimType.IsEmpty() || ClaimValue.IsEmpty() || ClaimTitle.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 권한 제목이 존재하지 않는 경우
					if (!await m_dbContext.ClaimNames.AsNoTracking().Where(i => i.ClaimType == ClaimType && i.ClaimValue == ClaimValue).AnyAsync())
					{
						// 권한 제목 등록
						await m_dbContext.ClaimNames.AddAsync(new ClaimName() { ClaimType = ClaimType, ClaimValue = ClaimValue, ClaimTitle = ClaimTitle, Depth = Depth, OrderNo = OrderNo });
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
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

		/// <summary>모든 권한 목록을 가져온다.</summary>
		/// <returns>권한 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetAllClaims()
		{
			var Result = new ResponseList<ResponseClaim>();
			try
			{
				// 전체 권한 정보를 가져온다.
				Result.Data = await m_dbContext.ClaimNames.AsNoTracking()
					.OrderByWithDirection(i => i.OrderNo)
					.CreateListAsync<ClaimName, ResponseClaim>();

				// 모든 권한에 대해서 처리
				foreach (var Claim in Result.Data.Items)
				{
					Claim.HasChild = Result.Data.Items
						.Exists(i =>
							i.ClaimValue.StartsWith(Claim.ClaimValue)
							&& i.Depth == Claim.Depth + 1
						);
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

		/// <summary>특정 권한의 하위 권한 목록을 가져온다.</summary>
		/// <param name="ParentId">상위 권한 아이디</param>
		/// <returns>권한 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetClaims(int ParentId)
		{
			var Result = new ResponseList<ResponseClaim>();
			try
			{
				// 상위 권한 정보를 가져온다.
				var ParentClaim = await m_dbContext.ClaimNames.AsNoTracking()
					.Where(i => i.Id == ParentId)
					.FirstOrDefaultAsync<ClaimName, ResponseClaim>();

				if (ParentClaim != null)
				{
					// 하위 권한 정보를 가져온다.
					Result.Data = await m_dbContext.ClaimNames.AsNoTracking()
						.Where(i => i.ClaimValue.StartsWith(ParentClaim.ClaimValue) && i.Depth == ParentClaim.Depth + 1)
						.OrderByWithDirection(i => i.OrderNo)
						.CreateListAsync<ClaimName, ResponseClaim>();

					// 모든 권한에 대해서 처리
					foreach (var Claim in Result.Data.Items)
					{
						Claim.HasChild = Result.Data.Items
							.Exists(i =>
								i.ClaimValue.StartsWith(Claim.ClaimValue)
								&& i.Depth == Claim.Depth + 1
							);
					}
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
	}
}