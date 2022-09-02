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
using System.Net;
using System.Security.Claims;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;
using PortalModels;
using PortalProviderInterface;
using PortalResources;
using Microsoft.AspNetCore.Http;
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
	/// <summary>계정 관련 프로바이더</summary>
	public class AccountProvider : BaseProvider<PortalModel>, IAccountProvider
	{
		/// <summary>사용자 프로바이더</summary>
		private readonly IUserProvider m_userProvider;

		/// <summary>사용자 로그인 관리자</summary>
		private readonly SignInManager<NNApplicationUser> m_signInManager;

		/// <summary>역할 매니져</summary>
		private readonly RoleManager<NNApplicationRole> m_roleManager;

		/// <summary>이메일 발송자</summary>
		private readonly IEmailSender m_emailSender;

		/// <summary>경로 도우미 객체</summary>
		private readonly IPathProvider m_pathProvider;

		/// <summary>접속 허용 IP 관리자 객체</summary>
		readonly IAllowConnectionIpsManager m_allowAddressProvider;

		/// <summary>API KEY 데이터 프로바이더 객체</summary>
		readonly IApiKeyProvider m_apiKeyProvider;

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		/// <param name="userProvider">사용자 정보 프로바이더</param>
		/// <param name="signInManager">로그인 관리자</param>
		/// <param name="roleManager">역할 관리자</param>
		/// <param name="emailSender">메일 발송자</param>
		/// <param name="pathProvider">경로 프로바이더</param>
		/// <param name="allowAddressProvider">접속 허용 IP 관리자 객체</param>
		/// <param name="apiKeyProvider">API KEY 데이터 프로바이더 객체</param>
		public AccountProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<AccountProvider> logger,
			IUserProvider userProvider,
			SignInManager<NNApplicationUser> signInManager,
			RoleManager<NNApplicationRole> roleManager,
			IEmailSender emailSender,
			IPathProvider pathProvider,
			IAllowConnectionIpsManager allowAddressProvider,
			IApiKeyProvider apiKeyProvider
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
			m_userProvider = userProvider;
			m_signInManager = signInManager;
			m_roleManager = roleManager;
			m_emailSender = emailSender;
			m_pathProvider = pathProvider;
			m_allowAddressProvider = allowAddressProvider;
			m_apiKeyProvider = apiKeyProvider;
		}

		/// <summary>볼륨 사용자용 포털 사용자를 생성한다.</summary>
		/// <param name="Request">생성할 사용자 정보</param>
		/// <returns>생성 결과</returns>
		public async Task<ResponseData<ResponseLogin>> Create(RequestSnasUserRegister Request)
		{
			var Result = new ResponseData<ResponseLogin>();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 계정 객체를 생성한다.
					var User = new NNApplicationUser { LoginId = Request.LoginId, Email = Request.LoginId + "@pspace.co.kr", Name = Request.Name };

					// 계정을 생성한다.
					var IdentityResult = await m_userManager.CreateAsync(User, Guid.NewGuid().ToString());

					// 계정 생성에 성공한 경우
					if (IdentityResult.Succeeded)
					{
						// 비밀번호 변경 일시를 지금으로 저장
						User.PasswordChangeDate = DateTime.Now;
						await m_userManager.UpdateAsync(User);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 역할 추가
						await m_userManager.AddToRoleAsync(User, PredefinedRoleNames.RoleNameUser);

						// 확인 메일 발송을 위한 토큰을 가져온다.
						var MailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(User);

						// 이메일 확인 처리
						IdentityResult = await m_userManager.ConfirmEmailAsync(User, MailConfirmToken);
						// 이메일 확인 처리에 성공한 경우
						if (IdentityResult.Succeeded)
						{
							// 추가된 사용자 정보를 가져온다.
							var AddedUser = await m_dbContext.Users.Where(i => i.Id == User.Id).FirstOrDefaultAsync();
							AddedUser.Email = AddedUser.LoginId;
							AddedUser.PasswordHash = null;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

							// 역할을 포함한 사용자 정보를 가져온다.
							var responseUserWithRoles = await m_userProvider.GetUser(User.Id.ToString());
							Result.CopyValueFrom(responseUserWithRoles);

							Result.Code = Resource.SC_COMMON__SUCCESS;
							Result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_CREATED;
						}
						// 이메일 확인 처리에 실패한 경우
						else
						{
							Result.Code = Resource.EC_COMMON_ACCOUNT_INVALID_TOKEN;
							Result.Message = Resource.EM_COMMON_ACCOUNT_INVALID_TOKEN;
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
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>사용자를 생성한다.</summary>
		/// <param name="Request">생성할 사용자 정보</param>
		/// <param name="HttpRequest">HttpRequest 객체</param>
		/// <param name="SetConfirmEmail">메일 인증됨으로 처리할지 여부</param>
		/// <returns>생성 결과</returns>
		public async Task<ResponseData<ResponseLogin>> Create(RequestRegister Request, HttpRequest HttpRequest, bool SetConfirmEmail = false)
		{
			var Result = new ResponseData<ResponseLogin>();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자명이 없는 경우, 사용자 아이디를 사용자명으로 설정
					if (Request.Name.IsEmpty())
						Request.Name = Request.Email;

					// 해당 계정 객체를 생성한다.
					var User = new NNApplicationUser { LoginId = Request.LoginId, Email = Request.Email, Name = Request.Name, PhoneNumber = Request.PhoneNumber };

					// 계정을 생성한다.
					var IdentityResult = await m_userManager.CreateAsync(User, Request.Password);

					// 계정 생성에 성공한 경우
					if (IdentityResult.Succeeded)
					{
						// 비밀번호 변경 일시를 지금으로 저장
						User.PasswordChangeDate = DateTime.Now;
						await m_userManager.UpdateAsync(User);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await m_userManager.AddToRoleAsync(User, PredefinedRoleNames.RoleNameUser);

						var ResponseUserWithRoles = await m_userProvider.GetUser(User.Id.ToString());

						// 메일 인증됨으로 처리하는 경우
						if (SetConfirmEmail)
						{
							// 확인 메일 발송을 위한 토큰을 가져온다.
							var mailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(User);

							// 이메일 확인 처리
							IdentityResult = await m_userManager.ConfirmEmailAsync(User, mailConfirmToken);
							// 이메일 확인 처리에 성공한 경우
							if (IdentityResult.Succeeded)
							{
								Result.CopyValueFrom(ResponseUserWithRoles);

								Result.Code = Resource.SC_COMMON__SUCCESS;
								Result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_CREATED;
							}
							// 이메일 확인 처리에 실패한 경우
							else
							{
								Result.Code = Resource.EC_COMMON_ACCOUNT_INVALID_TOKEN;
								Result.Message = Resource.EM_COMMON_ACCOUNT_INVALID_TOKEN;
							}
						}
						// 메일 인증을 수행해야 하는 경우
						else
						{
							// 프로토콜과 호스트 저장
							var Protocol = Request.Protocol.IsEmpty() ? HttpRequest.Scheme : Request.Protocol;
							var Host = Request.Host.IsEmpty() ? HttpRequest.Host.ToString() : Request.Host;

							// 확인 메일 발송
							var SendResult = await SendConfirmEmail(User, Protocol, Host);

							// 에러가 아닌 경우
							if (SendResult.Result != EnumResponseResult.Error)
								Result.CopyValueFrom(ResponseUserWithRoles);

							Result.Result = SendResult.Result;
							Result.Code = SendResult.Code;
							Result.Message = SendResult.Message;
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
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>로그인 처리</summary>
		/// <param name="ApiKey">API 키 문자열</param>
		/// <param name="HttpRequest">HttpRequest 객체</param>
		/// <returns>로그인 결과</returns>
		public async Task<ResponseData<ResponseLogin>> LoginWithApiKey(string ApiKey, HttpRequest HttpRequest)
		{
			var Result = new ResponseData<ResponseLogin>();

			try
			{
				// API 키가 존재하지 않는 경우
				if (ApiKey.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EC_COMMON__INVALID_REQUEST;
				}
				// API 키가 존재하는 경우
				else
				{
					// 해당 API Key에 대한 정보를 가져온다.
					var ResponseApiKey = await m_apiKeyProvider.GetApiKey(ApiKey);

					// 정보를 가져오는데 성공한 경우
					if (ResponseApiKey.Result == EnumResponseResult.Success)
					{
						// 해당 계정을 찾는다.
						var User = await m_userProvider.GetUserById(ResponseApiKey.Data.UserId);

						//해당 계정을 찾을수 없는 경우
						if (User == null || User.IsDeleted)
						{
							Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
						// 해당 계정을 찾은 경우
						else
						{
							//이메일 인증이 되어있지 않는 경우
							if (await m_userManager.IsEmailConfirmedAsync(User) == false)
							{
								Result.Code = Resource.EC_COMMON_ACCOUNT_LOGIN_AFTER_EMAIL_CONFIRM;
								Result.Message = Resource.EM_COMMON_ACCOUNT_LOGIN_AFTER_EMAIL_CONFIRM;
							}
							//이메일 인증이 되어있는 경우
							else
							{
								// 로그인
								await m_signInManager.SignInAsync(User, false);
								Result = await ProcessAfterLogin(User, HttpRequest);
							}
						}
					}
					// 정보를 가져오는데 실패한 경우
					else
					{
						Result.Code = ResponseApiKey.Code;
						Result.Message = ResponseApiKey.Message;
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

		/// <summary>로그인 처리</summary>
		/// <param name="Request">로그인 요청 객체</param>
		/// <param name="HttpRequest">HttpRequest 객체</param>
		/// <returns>로그인 결과</returns>
		public async Task<ResponseData<ResponseLogin>> Login(RequestLogin Request, HttpRequest HttpRequest)
		{
			var Result = new ResponseData<ResponseLogin>();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 계정을 찾는다.
					var User = await m_userProvider.GetUserByLoginId(Request.LoginId);

					//해당 계정을 찾을수 없는 경우
					if (User == null || User.IsDeleted)
					{
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						//이메일 인증이 되어있지 않는 경우
						if (await m_userManager.IsEmailConfirmedAsync(User) == false)
						{
							Result.Code = Resource.EC_COMMON_ACCOUNT_LOGIN_AFTER_EMAIL_CONFIRM;
							Result.Message = Resource.EM_COMMON_ACCOUNT_LOGIN_AFTER_EMAIL_CONFIRM;
						}
						//이메일 인증이 되어있는 경우
						else
						{
							// 로그인
							var SignInResult = await m_signInManager.PasswordSignInAsync(User, Request.Password, Request.RememberMe, lockoutOnFailure: false);

							// 로그인 성공 시
							if (SignInResult.Succeeded)
							{
								Result = await ProcessAfterLogin(User, HttpRequest);
							}
							// 계정이 잠겨있는 경우
							else if (SignInResult.IsLockedOut)
							{
								Result.Code = Resource.EC_COMMON_ACCOUNT_IS_LOCKED;
								Result.Message = Resource.EM_COMMON_ACCOUNT_IS_LOCKED;
							}
							// 로그인 실패 시
							else
							{
								Result.Code = Resource.EC_COMMON_ACCOUNT_ID_OR_PASSWORD_DO_NOT_MATCH;
								Result.Message = Resource.EM_COMMON_ACCOUNT_ID_OR_PASSWORD_DO_NOT_MATCH;
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

		/// <summary>로그인 후 필요한 작업을 처리한다.</summary>
		/// <param name="User">사용자 객체</param>
		/// <param name="HttpRequest">HttpRequest 객체</param>
		/// <returns>처리 결과</returns>
		private async Task<ResponseData<ResponseLogin>> ProcessAfterLogin(NNApplicationUser User, HttpRequest HttpRequest)
		{
			var Result = new ResponseData<ResponseLogin>();

			try
			{
				// 사용자 객체가 유효하지 않은 경우
				if (User == null)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 사용자 객체가 유효한 경우
				else
				{
					// 로그인 성공 시 접속 가능한 IP를 확인한다.
					bool AllowIp = false;

					// 역할 목록 저장
					var Roles = await m_userManager.GetRolesAsync(User);

					// 사용자가 가지는 역할 정보를 가져온다.
					var UserRoleIds = await m_roleManager.Roles.Where(i => Roles.Contains(i.Name)).Select(i => i.Id.ToString()).ToListAsync();

					// 모든 역할 ID에 대해서 처리
					foreach (var RoleId in UserRoleIds)
					{
						// 접속한 아이피가 사용자가 가지는 역할에서 허용된 아이피인지 검사한다.
						if (m_allowAddressProvider.IsAllowIp(RoleId, HttpRequest?.HttpContext.Connection.RemoteIpAddress))
						{
							AllowIp = true;
							break;
						}
					}

					// 허용된 ip 가 아닌 경우
					if (!AllowIp)
					{
						// 로그아웃
						await m_signInManager.SignOutAsync();

						return new ResponseData<ResponseLogin>(EnumResponseResult.Error, Resource.EC_COMMON__ACCESS_DENIED, Resource.EM_COMMON__ACCESS_DENIED, false, true);
					}

					// 로그인 로그 저장
					await this.m_systemLogProvider.Add(
						EnumLogLevel.Information
						, "User login '{0} ({1})'"
						, User.GetDisplayName()
						, this.UserIpAddress);

					// 로그인 기록 저장
					DateTime? LastLoginDateTime = await m_dbContext.UserLoginHistories.AsNoTracking()
						.Where(i => i.Id == User.Id)
						.OrderByDescending(i => i.LoginDate)
						.Select(i => i.LoginDate)
						.LastOrDefaultAsync();

					try
					{
						// 로그인 기록 저장
						await m_dbContext.UserLoginHistories.AddAsync(new UserLoginHistory()
						{
							Id = User.Id,
							LoginDate = DateTime.Now
						});
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
					}
					catch (Exception)
					{
						// ignored
					}

					Result.Result = EnumResponseResult.Success;
					Result.IsNeedLogin = false;
					Result.Data = new ResponseLogin();
					if (Roles != null && Roles.Count > 0)
						Result.Data.Roles.AddRange(Roles);
					Result.Data.Id = User.Id.ToString();
					Result.Data.Email = User.Email;
					Result.Data.Name = User.Name;
					Result.Data.DisplayName = User.GetDisplayName();
					Result.Data.LastLoginDateTIme = LastLoginDateTime;
					Result.Data.ProductType = m_configuration["AppSettings:ProductType"];
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

		/// <summary>로그아웃 처리</summary>
		/// <returns>로그아웃 결과</returns>
		public async Task<ResponseData> Logout()
		{
			var Result = new ResponseData();
			try
			{
				//로그아웃
				await m_signInManager.SignOutAsync();
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

		/// <summary>로그인 여부를 가져온다.</summary>
		/// <param name="User">로그인 사용자 정보 객체</param>
		/// <param name="RequireRoles">필요한 역할명 목록 (',' 으로 구분)</param>
		/// <returns>로그인 여부 정보</returns>
		public ResponseData CheckLogin(ClaimsPrincipal User, string RequireRoles = "")
		{
			var Result = new ResponseData();
			try
			{
				// 로그인한 사용자인 경우
				if (User != null && User.Identity != null && User.Identity.IsAuthenticated)
				{
					// 필요한 역할 목록이 존재하는 경우
					if (!RequireRoles.IsEmpty())
					{
						// 역할 목록을 분리한다.
						var RequireRoleList = RequireRoles.Split(new[] { ',', ' ' }, StringSplitOptions.RemoveEmptyEntries);

						// 모든 역할 목록에 대해서 처리
						foreach (var RequireRole in RequireRoleList)
						{
							// 역할이 존재하는 경우
							if (User.IsInRole(RequireRole))
							{
								Result.Result = EnumResponseResult.Success;
								break;
							}
						}
					}
					else
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

		/// <summary>로그인 처리</summary>
		/// <param name="LoginUser">로그인 사용자 정보 객체</param>
		/// <returns>로그인 결과</returns>
		public async Task<ResponseData<ResponseLogin>> GetLogin(ClaimsPrincipal LoginUser)
		{
			var Result = new ResponseData<ResponseLogin>();
			try
			{
				// 로그인한 사용자 계정을 가져온다.
				var User = await m_userManager.GetUserAsync(LoginUser);

				//해당 계정을 찾을수 없는 경우
				if (User == null || User.IsDeleted)
				{
					Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
					Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
				}
				// 해당 계정을 찾은 경우
				else
				{
					DateTime? LastLoginDateTime = await m_dbContext.UserLoginHistories.AsNoTracking()
																	.Where(i => i.Id == User.Id)
																	.OrderByDescending(i => i.LoginDate)
																	.Select(i => i.LoginDate)
																	.LastOrDefaultAsync();

					Result.Result = EnumResponseResult.Success;
					Result.IsNeedLogin = false;
					Result.Data = new ResponseLogin();
					Result.Data.Id = User.Id.ToString();
					Result.Data.LoginId = User.LoginId;
					Result.Data.Email = User.Email;
					Result.Data.Name = User.Name;
					Result.Data.DisplayName = User.GetDisplayName();
					Result.Data.PasswordChangeDate = User.PasswordChangeDate;
					Result.Data.PhoneNumber = User.PhoneNumber;
					Result.Data.LastLoginDateTIme = LastLoginDateTime;
					Result.Data.ProductType = m_configuration["AppSettings:ProductType"];

					// 역할 목록 저장
					var Roles = await m_userManager.GetRolesAsync(User);
					if (Roles != null && Roles.Count > 0)
						Result.Data.Roles.AddRange(Roles);
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

		/// <summary>이메일 주소 인증 처리</summary>
		/// <param name="User">사용자 정보 객체</param>
		/// <param name="Protocol">프로토콜</param>
		/// <param name="Host">호스트</param>
		/// <returns>인증 처리 결과</returns>
		private async Task<ResponseData> SendConfirmEmail(NNApplicationUser User, string Protocol, string Host)
		{
			var Result = new ResponseData();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (User == null || User.IsDeleted)
				{
					Result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					Result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 확인 메일 발송을 위한 토큰을 가져온다.
					var MailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(User);

					// 메일 내용 중 이메일 주소 확인 버튼 클릭 시 돌아올 콜백 URL
					var CallbackUrl = string.Format("{0}://{1}/{2}?userId={3}&Code={4}", Protocol, Host, "Account/ConfirmEmail", WebUtility.UrlEncode(User.LoginId), WebUtility.UrlEncode(MailConfirmToken));

					// 계정 확인 메일에 사용할 대체 문자열 저장
					var Replace = new Dictionary<string, string>();
					Replace.Add("<%CALLBACK_URL%>", CallbackUrl);
					Replace.Add("<%DOMAIN%>", string.Format("{0}://{1}", Protocol, Host));

					// 메일 발송에 성공한 경우
					if (await m_emailSender.SendEmailAsync(User.Email, Resource.UL_COMMON_ACCOUNT_MAIL_EMAIL_CONFIRM, m_pathProvider.MapPath("/Email/" + Resource.EMAIL_COMMON_ACCOUNT_EMAIL_CONFIRMATION), Replace))
					{
						Result.Result = EnumResponseResult.Success;
						Result.Code = Resource.SC_COMMON__SUCCESS;
						Result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_SEND_CONFIRM_MAIL;
					}
					// 메일 발송에 실패한 경우
					else
					{
						Result.Result = EnumResponseResult.Warning;
						Result.Code = Resource.EC_COMMON__MAIL_SEND_ERROR;
						Result.Message = Resource.EM_COMMON__MAIL_SEND_ERROR;
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

		/// <summary>이메일 주소 인증 처리</summary>
		/// <param name="Request">이메일 인증 요청 객체</param>
		/// <returns>인증 처리 결과</returns>
		public async Task<ResponseData> ConfirmEmail(RequestConfirmEmail Request)
		{
			var Result = new ResponseData();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 아이디로 해당 회원 정보를 가져온다.
					var User = await m_userProvider.GetUserByLoginId(Request.LoginId);

					// 회원 정보가 유효하지 않은 경우
					if (User == null || User.IsDeleted)
					{
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 이메일 확인 처리된 회원인 경우
						if (User.EmailConfirmed)
						{
							Result.Result = EnumResponseResult.Success;
							Result.Code = Resource.SC_COMMON__SUCCESS;
							Result.Message = Resource.SM_COMMON_ACCOUNT_EMAIL_CONFIRM_SUCCESS;
						}
						// 이메일 확인 처리되지 않은 회원인 경우
						else
						{
							// 이메일 확인 처리
							var IdentityResult = await m_userManager.ConfirmEmailAsync(User, Request.Code);

							// 이메일 확인 처리에 성공한 경우
							if (IdentityResult.Succeeded)
							{
								Result.Result = EnumResponseResult.Success;
								Result.Code = Resource.SC_COMMON__SUCCESS;
								Result.Message = Resource.SM_COMMON_ACCOUNT_EMAIL_CONFIRM_SUCCESS;
							}
							// 이메일 확인 처리에 실패한 경우
							else
							{
								Result.Code = Resource.EC_COMMON_ACCOUNT_INVALID_TOKEN;
								Result.Message = Resource.EM_COMMON_ACCOUNT_INVALID_TOKEN;
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

		/// <summary>현재 로그인한 사용자의 비밀번호를 변경한다.</summary>
		/// <param name="LoginUser">로그인 사용자 정보 객체</param>
		/// <param name="Request">비밀번호 요청 객체</param>
		/// <returns>비밀번호 변경 결과</returns>
		public async Task<ResponseData> ChangePassword(ClaimsPrincipal LoginUser, RequestChangePassword Request)
		{
			var Result = new ResponseData();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 현재 비밀번호와 변경하려는 비밀번호가 일치하는 경우
				else if (Request.Password == Request.NewPassword)
				{
					Result.Code = Resource.EC_COMMON_ACCOUNT_CURRENT_PASSWORD_NEW_PASSWORD_SHOULD_NOT_BE_SAME;
					Result.Message = Resource.EM_COMMON_ACCOUNT_CURRENT_PASSWORD_NEW_PASSWORD_SHOULD_NOT_BE_SAME;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 로그인한 사용자 계정을 가져온다.
					var User = await m_userManager.GetUserAsync(LoginUser);

					//해당 계정을 찾을수 없는 경우
					if (User == null || User.IsDeleted)
					{
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 비밀번호 변경
						var IdentityResult = await m_userManager.ChangePasswordAsync(User, Request.Password, Request.NewPassword);

						// 비밀번호 변경에 성공한 경우
						if (IdentityResult.Succeeded)
						{
							// 비밀번호 변경 일시를 지금으로 저장
							User.PasswordChangeDate = DateTime.Now;
							await m_userManager.UpdateAsync(User);
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

							Result.Result = EnumResponseResult.Success;
							Result.Code = Resource.SC_COMMON__SUCCESS;
							Result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_CHANGE_PASSWORD;
						}
						// 비밀번호 변경에 실패한 경우
						else
						{
							// 에러 내용 저장
							Result.Code = Resource.EC_COMMON_ACCOUNT_FAIL_TO_CHANGE_PASSWORD;
							Result.Message = IdentityResult.Errors.FirstOrDefault() == null ? "" : IdentityResult.Errors.FirstOrDefault()?.Description;
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

		/// <summary>비밀번호 찾기 요청</summary>
		/// <param name="Request">비밀번호 찾기 요청 객체</param>
		/// <param name="HttpRequest">HttpRequest 객체</param>
		/// <returns>비밀번호 찾기 요청 처리 결과</returns>
		public async Task<ResponseData> ForgotPassword(RequestForgetPassword Request, HttpRequest HttpRequest)
		{
			var Result = new ResponseData();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 아이디로 해당 회원 정보를 가져온다.
					var User = await m_userProvider.GetUserByLoginId(Request.LoginId);

					// 회원 정보가 유효하지 않은 경우
					if (User == null || User.IsDeleted)
					{
						// 에러 출력
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;

						//// 외부 해킹을 통해서 가입자 확인이 불가능하도록 하기 위해서 성공 반환
						//Result.Result = EnumResponseResult.Success;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 이메일 인증이 되지 않은 경우
						if (await m_userManager.IsEmailConfirmedAsync(User) == false)
						{
							Result.Code = Resource.EC_COMMON_ACCOUNT_IS_NOT_AUTH_EMAIL;
							Result.Message = Resource.EM_COMMON_ACCOUNT_IS_NOT_AUTH_EMAIL;
						}
						// 이메일 인증이 된 경우
						else
						{
							// 비밀번호 재설정 토큰 생성
							var Code = await m_userManager.GeneratePasswordResetTokenAsync(User);

							// 프로토콜과 호스트 저장
							var Protocol = Request.Protocol.IsEmpty() ? HttpRequest.Scheme : Request.Protocol;
							var Host = Request.Host.IsEmpty() ? HttpRequest.Host.ToString() : Request.Host;

							// 메일 내용 중 이메일 주소 확인 버튼 클릭 시 돌아올 콜백 URL
							string CallbackUrl = string.Format("{0}://{1}/{2}?userId={3}&Code={4}", Protocol, Host, "Account/ResetPassword", WebUtility.UrlEncode(User.LoginId), WebUtility.UrlEncode(Code));

							// 계정 확인 메일에 사용할 대체 문자열 저장
							var Replace = new Dictionary<string, string>();
							Replace.Add("<%CALLBACK_URL%>", CallbackUrl);
							Replace.Add("<%DOMAIN%>", string.Format("{0}://{1}", Protocol, Host));

							// 메일 발송에 성공한 경우
							if (await m_emailSender.SendEmailAsync(User.Email, Resource.UL_COMMON_ACCOUNT_MAIL_PASSWORD_RESET, m_pathProvider.MapPath("/Email/" + Resource.EMAIL_COMMON_ACCOUNT_RESET_PASSWORD), Replace))
							{
								Result.Result = EnumResponseResult.Success;
								Result.Code = Resource.SC_COMMON__SUCCESS;
								Result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_SEND_RESET_MAIL;
							}
							// 메일 발송에 실패한 경우
							else
							{
								Result.Code = Resource.EC_COMMON__MAIL_SEND_ERROR;
								Result.Message = Resource.EM_COMMON__MAIL_SEND_ERROR;
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

		/// <summary>비밀번호 재설정</summary>
		/// <param name="Request">비밀번호 재설정 요청 객체</param>
		/// <returns>비밀번호 재설정 결과</returns>
		public async Task<ResponseData> ResetPassword(RequestResetPassword Request)
		{
			var Result = new ResponseData();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 아이디로 해당 회원 정보를 가져온다.
					var User = await m_userProvider.GetUserByLoginId(Request.LoginId);

					// 회원 정보가 유효하지 않은 경우
					if (User == null || User.IsDeleted)
					{
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 이메일 인증이 되지 않은 경우
						if (await m_userManager.IsEmailConfirmedAsync(User) == false)
						{
							Result.Code = Resource.EC_COMMON_ACCOUNT_IS_NOT_AUTH_EMAIL;
							Result.Message = Resource.EM_COMMON_ACCOUNT_IS_NOT_AUTH_EMAIL;
						}
						// 이메일 인증이 된 경우
						else
						{
							// 비밀번호 재설정
							var IdentityResult = await m_userManager.ResetPasswordAsync(User, Request.Code, Request.NewPassword);

							// 이메일 확인 처리에 성공한 경우
							if (IdentityResult.Succeeded)
							{
								// 비밀번호 변경 일시를 지금으로 저장
								User.PasswordChangeDate = DateTime.Now;
								await m_userManager.UpdateAsync(User);
								await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

								Result.Result = EnumResponseResult.Success;
								Result.Code = Resource.SC_COMMON__SUCCESS;
								Result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_RESET_PASSWORD;
							}
							// 이메일 확인 처리에 실패한 경우
							else
							{
								Result.Code = Resource.EC_COMMON_ACCOUNT_FAIL_TO_RESET_PASSWORD;
								Result.Message = IdentityResult.Errors.FirstOrDefault() == null ? "" : IdentityResult.Errors.FirstOrDefault()?.Description;
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

		/// <summary>현재 로그인한 사용자 정보를 수정한다.</summary>
		/// <param name="LoginUser">로그인 사용자 정보 객체</param>
		/// <param name="Request">회원 정보 수정 요청 객체</param>
		/// <returns>사용자 정보 수정 결과</returns>
		public async Task<ResponseData> Update(ClaimsPrincipal LoginUser, RequestUpdate Request)
		{
			var Result = new ResponseData();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!Request.IsValid())
				{
					Result.Code = Request.GetErrorCode();
					Result.Message = Request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 로그인한 사용자 계정을 가져온다.
					var User = await m_userManager.GetUserAsync(LoginUser);

					//해당 계정을 찾을수 없는 경우
					if (User == null || User.IsDeleted)
					{
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 사용자 정보 변경
						User.Name = Request.Name;
						User.PhoneNumber = Request.PhoneNumber;
						await m_userManager.UpdateAsync(User);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Code = Resource.SC_COMMON__SUCCESS;
						Result.Message = Resource.SM_COMMON__UPDATED;
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

		/// <summary>특정 사용자에게 역할을 추가한다.</summary>
		/// <param name="Id">회원아이디</param>
		/// <param name="RoleName">역할명</param>
		/// <returns>역할 추가 결과</returns>
		public async Task<ResponseData> AddToRole(string Id, string RoleName)
		{
			var Result = new ResponseData();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (Id.IsEmpty() || RoleName.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 회원아이디로로 해당 회원 정보를 가져온다.
					var User = await m_userProvider.GetUserById(Id);

					// 회원 정보가 유효하지 않은 경우
					if (User == null || User.IsDeleted)
					{
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 해당 인증이 포함되어 있지 않은 경우
						if (!await m_userManager.IsInRoleAsync(User, RoleName))
						{
							await m_userManager.AddToRoleAsync(User, RoleName);
							Result.Result = EnumResponseResult.Success;
						}
						// 해당 인증이 포함되어 있는 경우
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

		/// <summary>특정 사용자에서 역할을 삭제한다.</summary>
		/// <param name="Id">회원아이디</param>
		/// <param name="RoleName">역할명</param>
		/// <returns>역할 삭제 결과</returns>
		public async Task<ResponseData> RemoveFromRole(string Id, string RoleName)
		{
			var Result = new ResponseData();
			try
			{
				// 파라미터가 유효하지 않은 경우
				if (Id.IsEmpty() || RoleName.IsEmpty())
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 회원아이디로로 해당 회원 정보를 가져온다.
					var User = await m_userProvider.GetUserById(Id);

					// 회원 정보가 유효하지 않은 경우
					if (User == null || User.IsDeleted)
					{
						Result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						Result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 해당 인증이 포함되어 있는 경우
						if (await m_userManager.IsInRoleAsync(User, RoleName))
						{
							await m_userManager.RemoveFromRoleAsync(User, RoleName);
							Result.Result = EnumResponseResult.Success;
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

		/// <summary>로그인한 사용자에 대한 권한 목록을 가져온다.</summary>
		/// <param name="loginUser">로그인 사용자 정보 객체</param>
		/// <returns>로그인한 사용자에 대한 사용자 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetClaims(ClaimsPrincipal loginUser)
		{
			var Result = new ResponseList<ResponseClaim>();
			try
			{
				// 로그인한 사용자 계정을 가져온다.
				var User = await m_userManager.GetUserAsync(loginUser);

				//해당 계정을 찾을수 없는 경우
				if (User == null || User.IsDeleted)
				{
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
					Result.Data = await m_dbContext.ClaimNames
						.AsNoTracking()
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
						)
						.OrderByWithDirection(i => i.ClaimValue)
						.CreateListAsync<ClaimName, ResponseClaim>();

					// 데이터가 존재하는 경우, 가져온 권한 중 중복 제거 처리
					if (Result.Data.Items.Count > 0)
					{
						var DistinctClaims = Result.Data.Items.Distinct(new ResponseClaimEqualityComparer()).ToList();
						Result.Data.TotalCount = DistinctClaims.Count;
						Result.Data.Items.Clear();
						Result.Data.Items.AddRange(DistinctClaims);
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

		/// <summary>로그인한 사용자의 권한 중 해당 권한이 존재하는지 확인한다.</summary>
		/// <param name="LoginUser">로그인 사용자 정보 객체</param>
		/// <param name="ClaimValue">검사할 권한 값</param>
		/// <returns>로그인한 사용자의 권한 중 해당 권한이 존재하는지 여부</returns>
		public async Task<ResponseData> HasClaim(ClaimsPrincipal LoginUser, string ClaimValue)
		{
			var Result = new ResponseData();
			try
			{
				// 로그인한 사용자 계정을 가져온다.
				var User = await m_userManager.GetUserAsync(LoginUser);

				//해당 계정을 찾을수 없는 경우
				if (User == null || User.IsDeleted)
				{
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

					// 해당 사용자의 역할 권한 및 사용자 권한에서 해당 권한이 있는 경우
					if (await m_dbContext.RoleClaims.AsNoTracking()
														.Where(i => RoleNames.Contains(i.Role.Name)
															&& i.ClaimType == "Permission"
															&& i.ClaimValue == ClaimValue)
														.AnyAsync()
						|| await m_dbContext.UserClaims.AsNoTracking()
														.Where(i => i.UserId == User.Id
															&& i.ClaimType == "Permission"
															&& i.ClaimValue == ClaimValue)
														.AnyAsync()
					)
						Result.Result = EnumResponseResult.Success;
					else
					{
						Result.AccessDenied = true;
						Result.Code = Resource.EC_COMMON__ACCESS_DENIED;
						Result.Message = Resource.EM_COMMON__ACCESS_DENIED;
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
	}

	/// <summary>ResponseClaim에 대한 비교 클래스</summary>
	class ResponseClaimEqualityComparer : IEqualityComparer<ResponseClaim>
	{
		public bool Equals(ResponseClaim x, ResponseClaim y)
		{
			// Two items are equal if their keys are equal.
			return y != null && x != null && x.ClaimValue == y.ClaimValue;
		}

		public int GetHashCode(ResponseClaim obj)
		{
			return obj.ClaimValue.GetHashCode();
		}
	}
}
