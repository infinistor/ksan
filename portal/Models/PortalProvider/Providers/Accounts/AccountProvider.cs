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
		/// <param name="request">생성할 사용자 정보</param>
		/// <returns>생성 결과</returns>
		public async Task<ResponseData<ResponseLogin>> Create(RequestSnasUserRegister request)
		{
			ResponseData<ResponseLogin> result = new ResponseData<ResponseLogin>();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 계정 객체를 생성한다.
					NNApplicationUser user = new NNApplicationUser { LoginId = request.LoginId, Email = request.LoginId + "@pspace.co.kr", Name = request.Name };

					// 계정을 생성한다.
					IdentityResult identityResult = await m_userManager.CreateAsync(user, Guid.NewGuid().ToString());

					// 계정 생성에 성공한 경우
					if (identityResult.Succeeded)
					{
						// 비밀번호 변경 일시를 지금으로 저장
						user.PasswordChangeDate = DateTime.Now;
						await m_userManager.UpdateAsync(user);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 역할 추가
						await m_userManager.AddToRoleAsync(user, PredefinedRoleNames.RoleNameUser);

						// 확인 메일 발송을 위한 토큰을 가져온다.
						string mailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(user);

						// 이메일 확인 처리
						identityResult = await m_userManager.ConfirmEmailAsync(user, mailConfirmToken);
						// 이메일 확인 처리에 성공한 경우
						if (identityResult.Succeeded)
						{
							// 추가된 사용자 정보를 가져온다.
							User addedUser = await m_dbContext.Users.Where(i => i.Id == user.Id).FirstOrDefaultAsync();
							addedUser.Email = addedUser.LoginId;
							addedUser.PasswordHash = null;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

							// 역할을 포함한 사용자 정보를 가져온다.
							ResponseData<ResponseUserWithRoles> responseUserWithRoles = await m_userProvider.GetUser(user.Id.ToString());
							result.CopyValueFrom(responseUserWithRoles);

							result.Code = Resource.SC_COMMON__SUCCESS;
							result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_CREATED;
						}
						// 이메일 확인 처리에 실패한 경우
						else
						{
							result.Code = Resource.EC_COMMON_ACCOUNT_INVALID_TOKEN;
							result.Message = Resource.EM_COMMON_ACCOUNT_INVALID_TOKEN;
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
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return result;
		}

		/// <summary>사용자를 생성한다.</summary>
		/// <param name="request">생성할 사용자 정보</param>
		/// <param name="httpRequest">HttpRequest 객체</param>
		/// <param name="setConfirmEmail">메일 인증됨으로 처리할지 여부</param>
		/// <returns>생성 결과</returns>
		public async Task<ResponseData<ResponseLogin>> Create(RequestRegister request, HttpRequest httpRequest, bool setConfirmEmail = false)
		{
			ResponseData<ResponseLogin> result = new ResponseData<ResponseLogin>();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자명이 없는 경우, 사용자 아이디를 사용자명으로 설정
					if (request.Name.IsEmpty())
						request.Name = request.Email;

					// 해당 계정 객체를 생성한다.
					user = new NNApplicationUser { LoginId = request.LoginId, Email = request.Email, Name = request.Name, PhoneNumber = request.PhoneNumber };

					// 계정을 생성한다.
					IdentityResult identityResult = await m_userManager.CreateAsync(user, request.Password);

					// 계정 생성에 성공한 경우
					if (identityResult.Succeeded)
					{
						// 비밀번호 변경 일시를 지금으로 저장
						user.PasswordChangeDate = DateTime.Now;
						await m_userManager.UpdateAsync(user);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await m_userManager.AddToRoleAsync(user, PredefinedRoleNames.RoleNameUser);

						ResponseData<ResponseUserWithRoles> responseUserWithRoles = await m_userProvider.GetUser(user.Id.ToString());

						// 메일 인증됨으로 처리하는 경우
						if (setConfirmEmail)
						{
							// 확인 메일 발송을 위한 토큰을 가져온다.
							string mailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(user);

							// 이메일 확인 처리
							identityResult = await m_userManager.ConfirmEmailAsync(user, mailConfirmToken);
							// 이메일 확인 처리에 성공한 경우
							if (identityResult.Succeeded)
							{
								result.CopyValueFrom(responseUserWithRoles);

								result.Code = Resource.SC_COMMON__SUCCESS;
								result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_CREATED;
							}
							// 이메일 확인 처리에 실패한 경우
							else
							{
								result.Code = Resource.EC_COMMON_ACCOUNT_INVALID_TOKEN;
								result.Message = Resource.EM_COMMON_ACCOUNT_INVALID_TOKEN;
							}
						}
						// 메일 인증을 수행해야 하는 경우
						else
						{
							// 프로토콜과 호스트 저장
							string protocol = request.Protocol.IsEmpty() ? httpRequest.Scheme : request.Protocol;
							string host = request.Host.IsEmpty() ? httpRequest.Host.ToString() : request.Host;
							
							// 확인 메일 발송
							ResponseData sendResult = await SendConfirmEmail(user, protocol, host);

							// 에러가 아닌 경우
							if (sendResult.Result != EnumResponseResult.Error)
								result.CopyValueFrom(responseUserWithRoles);

							result.Result = sendResult.Result;
							result.Code = sendResult.Code;
							result.Message = sendResult.Message;
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
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return result;
		}

		/// <summary>로그인 처리</summary>
		/// <param name="apiKey">API 키 문자열</param>
		/// <param name="httpRequest">HttpRequest 객체</param>
		/// <returns>로그인 결과</returns>
		public async Task<ResponseData<ResponseLogin>> LoginWithApiKey(string apiKey, HttpRequest httpRequest)
		{
			ResponseData<ResponseLogin> result = new ResponseData<ResponseLogin>();

			try
			{
				// API 키가 존재하지 않는 경우
				if (apiKey.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					result.Message = Resource.EC_COMMON__INVALID_REQUEST;
				}
				// API 키가 존재하는 경우
				else
				{
					// 해당 API Key에 대한 정보를 가져온다.
					ResponseData<ResponseApiKey> responseApiKey = await m_apiKeyProvider.GetApiKey(apiKey);

					// 정보를 가져오는데 성공한 경우
					if (responseApiKey.Result == EnumResponseResult.Success)
					{
						// 해당 계정을 찾는다.
						NNApplicationUser user = await m_userProvider.GetUserById(responseApiKey.Data.UserId);

						//해당 계정을 찾을수 없는 경우
						if (user == null || user.IsDeleted)
						{
							result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
							result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
						}
						// 해당 계정을 찾은 경우
						else
						{
							//이메일 인증이 되어있지 않는 경우
							if (await m_userManager.IsEmailConfirmedAsync(user) == false)
							{
								result.Code = Resource.EC_COMMON_ACCOUNT_LOGIN_AFTER_EMAIL_CONFIRM;
								result.Message = Resource.EM_COMMON_ACCOUNT_LOGIN_AFTER_EMAIL_CONFIRM;
							}
							//이메일 인증이 되어있는 경우
							else
							{
								// 로그인
								await m_signInManager.SignInAsync(user, false);
								result = await ProcessAfterLogin(user, httpRequest);
							}
						}
					}
					// 정보를 가져오는데 실패한 경우
					else
					{
						result.Code = responseApiKey.Code;
						result.Message = responseApiKey.Message;
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

		/// <summary>로그인 처리</summary>
		/// <param name="request">로그인 요청 객체</param>
		/// <param name="httpRequest">HttpRequest 객체</param>
		/// <returns>로그인 결과</returns>
		public async Task<ResponseData<ResponseLogin>> Login(RequestLogin request, HttpRequest httpRequest)
		{
			ResponseData<ResponseLogin> result = new ResponseData<ResponseLogin>();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 해당 계정을 찾는다.
					NNApplicationUser user = await m_userProvider.GetUserByLoginId(request.LoginId);

					//해당 계정을 찾을수 없는 경우
					if (user == null || user.IsDeleted)
					{
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						//이메일 인증이 되어있지 않는 경우
						if (await m_userManager.IsEmailConfirmedAsync(user) == false)
						{
							result.Code = Resource.EC_COMMON_ACCOUNT_LOGIN_AFTER_EMAIL_CONFIRM;
							result.Message = Resource.EM_COMMON_ACCOUNT_LOGIN_AFTER_EMAIL_CONFIRM;
						}
						//이메일 인증이 되어있는 경우
						else
						{
							// 로그인
							SignInResult signInResult = await m_signInManager.PasswordSignInAsync(user, request.Password, request.RememberMe, lockoutOnFailure: false);
							
							// 로그인 성공 시
							if (signInResult.Succeeded)
							{
								result = await ProcessAfterLogin(user, httpRequest);
							}
							// 계정이 잠겨있는 경우
							else if (signInResult.IsLockedOut)
							{
								result.Code = Resource.EC_COMMON_ACCOUNT_IS_LOCKED;
								result.Message = Resource.EM_COMMON_ACCOUNT_IS_LOCKED;
							}
							// 로그인 실패 시
							else
							{
								result.Code = Resource.EC_COMMON_ACCOUNT_ID_OR_PASSWORD_DO_NOT_MATCH;
								result.Message = Resource.EM_COMMON_ACCOUNT_ID_OR_PASSWORD_DO_NOT_MATCH;
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

		/// <summary>로그인 후 필요한 작업을 처리한다.</summary>
		/// <param name="user">사용자 객체</param>
		/// <param name="httpRequest">HttpRequest 객체</param>
		/// <returns>처리 결과</returns>
		private async Task<ResponseData<ResponseLogin>> ProcessAfterLogin(NNApplicationUser user, HttpRequest httpRequest)
		{
			ResponseData<ResponseLogin> result = new ResponseData<ResponseLogin>();

			try
			{
				// 사용자 객체가 유효하지 않은 경우
				if (user == null)
				{
					result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 사용자 객체가 유효한 경우
				else
				{
					// 로그인 성공 시 접속 가능한 IP를 확인한다.
					bool allowIp = false;

					// 역할 목록 저장
					IList<string> roles = await m_userManager.GetRolesAsync(user);

					// 사용자가 가지는 역할 정보를 가져온다.
					List<string> userRoleIds = await m_roleManager.Roles.Where(i => roles.Contains(i.Name)).Select(i => i.Id.ToString()).ToListAsync();

					// 모든 역할 ID에 대해서 처리
					foreach (string roleId in userRoleIds)
					{
						// 접속한 아이피가 사용자가 가지는 역할에서 허용된 아이피인지 검사한다.
						if (m_allowAddressProvider.IsAllowIp(roleId, httpRequest?.HttpContext.Connection.RemoteIpAddress))
						{
							allowIp = true;
							break;
						}
					}

					// 허용된 ip 가 아닌 경우
					if (!allowIp)
					{
						// 로그아웃
						await m_signInManager.SignOutAsync();

						return new ResponseData<ResponseLogin>(EnumResponseResult.Error, Resource.EC_COMMON__ACCESS_DENIED, Resource.EM_COMMON__ACCESS_DENIED, false, true);
					}
					
					// 로그인 로그 저장
					await this.m_systemLogProvider.Add(
						EnumLogLevel.Information
						, "User login '{0} ({1})'"
						, user.GetDisplayName()
						, this.UserIpAddress);

					// 로그인 기록 저장
					DateTime? lastLoginDateTime = await m_dbContext.UserLoginHistories.AsNoTracking()
						.Where(i => i.Id == user.Id)
						.OrderByDescending(i => i.LoginDate)
						.Select(i => i.LoginDate)
						.LastOrDefaultAsync();

					try
					{
						// 로그인 기록 저장
						await m_dbContext.UserLoginHistories.AddAsync(new UserLoginHistory()
						{
							Id = user.Id,
							LoginDate = DateTime.Now
						});
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
					}
					catch (Exception)
					{
						// ignored
					}

					result.Result = EnumResponseResult.Success;
					result.IsNeedLogin = false;
					result.Data = new ResponseLogin();
					if (roles != null && roles.Count > 0)
						result.Data.Roles.AddRange(roles);
					result.Data.Id = user.Id.ToString();
					result.Data.Email = user.Email;
					result.Data.Name = user.Name;
					result.Data.DisplayName = user.GetDisplayName();
					result.Data.LastLoginDateTIme = lastLoginDateTime;
					result.Data.ProductType = m_configuration["AppSettings:ProductType"];
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

		/// <summary>로그아웃 처리</summary>
		/// <returns>로그아웃 결과</returns>
		public async Task<ResponseData> Logout()
		{
			ResponseData result = new ResponseData();
			try
			{
				//로그아웃
				await m_signInManager.SignOutAsync();
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

		/// <summary>로그인 여부를 가져온다.</summary>
		/// <param name="user">로그인 사용자 정보 객체</param>
		/// <param name="requireRoles">필요한 역할명 목록 (',' 으로 구분)</param>
		/// <returns>로그인 여부 정보</returns>
		public ResponseData CheckLogin(ClaimsPrincipal user, string requireRoles = "")
		{
			ResponseData result = new ResponseData();
			try
			{
				// 로그인한 사용자인 경우
				if (user != null && user.Identity != null && user.Identity.IsAuthenticated)
				{
					// 필요한 역할 목록이 존재하는 경우
					if (!requireRoles.IsEmpty())
					{
						// 역할 목록을 분리한다.
						string[] requireRoleList = requireRoles.Split(new[] { ',', ' ' }, StringSplitOptions.RemoveEmptyEntries);

						// 모든 역할 목록에 대해서 처리
						foreach (string requireRole in requireRoleList)
						{
							// 역할이 존재하는 경우
							if (user.IsInRole(requireRole))
							{
								result.Result = EnumResponseResult.Success;
								break;
							}
						}
					}
					else
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

		/// <summary>로그인 처리</summary>
		/// <param name="loginUser">로그인 사용자 정보 객체</param>
		/// <returns>로그인 결과</returns>
		public async Task<ResponseData<ResponseLogin>> GetLogin(ClaimsPrincipal loginUser)
		{
			ResponseData<ResponseLogin> result = new ResponseData<ResponseLogin>();
			NNApplicationUser user;

			try
			{
				// 로그인한 사용자 계정을 가져온다.
				user = await m_userManager.GetUserAsync(loginUser);

				//해당 계정을 찾을수 없는 경우
				if (user == null || user.IsDeleted)
				{
					result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
					result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
				}
				// 해당 계정을 찾은 경우
				else
				{
					DateTime? lastLoginDateTime = await m_dbContext.UserLoginHistories.AsNoTracking()
																	.Where(i => i.Id == user.Id)
																	.OrderByDescending(i => i.LoginDate)
																	.Select(i => i.LoginDate)
																	.LastOrDefaultAsync();

					result.Result = EnumResponseResult.Success;
					result.IsNeedLogin = false;
					result.Data = new ResponseLogin();
					result.Data.Id = user.Id.ToString();
					result.Data.LoginId = user.LoginId;
					result.Data.Email = user.Email;
					result.Data.Name = user.Name;
					result.Data.DisplayName = user.GetDisplayName();
					result.Data.PasswordChangeDate = user.PasswordChangeDate;
					result.Data.PhoneNumber = user.PhoneNumber;
					result.Data.LastLoginDateTIme = lastLoginDateTime;
					result.Data.ProductType = m_configuration["AppSettings:ProductType"];

					// 역할 목록 저장
					IList<string> roles = await m_userManager.GetRolesAsync(user);
					if (roles != null && roles.Count > 0)
						result.Data.Roles.AddRange(roles);
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

		/// <summary>이메일 주소 인증 처리</summary>
		/// <param name="user">사용자 정보 객체</param>
		/// <param name="protocol">프로토콜</param>
		/// <param name="host">호스트</param>
		/// <returns>인증 처리 결과</returns>
		private async Task<ResponseData> SendConfirmEmail(NNApplicationUser user, string protocol, string host)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (user == null || user.IsDeleted)
				{
					result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
					result.Message = Resource.EM_COMMON__INVALID_INFORMATION;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 확인 메일 발송을 위한 토큰을 가져온다.
					string mailConfirmToken = await m_userManager.GenerateEmailConfirmationTokenAsync(user);

					// 메일 내용 중 이메일 주소 확인 버튼 클릭 시 돌아올 콜백 URL
					string callbackUrl;
					callbackUrl = string.Format("{0}://{1}/{2}?userId={3}&code={4}", protocol, host, "Account/ConfirmEmail", WebUtility.UrlEncode(user.LoginId), WebUtility.UrlEncode(mailConfirmToken));

					// 계정 확인 메일에 사용할 대체 문자열 저장
					Dictionary<string, string> replace = new Dictionary<string, string>();
					replace.Add("<%CALLBACK_URL%>", callbackUrl);
					replace.Add("<%DOMAIN%>", string.Format("{0}://{1}", protocol, host));

					// 메일 발송에 성공한 경우
					if (await m_emailSender.SendEmailAsync(user.Email, Resource.UL_COMMON_ACCOUNT_MAIL_EMAIL_CONFIRM, m_pathProvider.MapPath("/email/" + Resource.EMAIL_COMMON_ACCOUNT_EMAIL_CONFIRMATION), replace))
					{
						result.Result = EnumResponseResult.Success;
						result.Code = Resource.SC_COMMON__SUCCESS;
						result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_SEND_CONFIRM_MAIL;
					}
					// 메일 발송에 실패한 경우
					else
					{
						result.Result = EnumResponseResult.Warning;
						result.Code = Resource.EC_COMMON__MAIL_SEND_ERROR;
						result.Message = Resource.EM_COMMON__MAIL_SEND_ERROR;
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

		/// <summary>이메일 주소 인증 처리</summary>
		/// <param name="request">이메일 인증 요청 객체</param>
		/// <returns>인증 처리 결과</returns>
		public async Task<ResponseData> ConfirmEmail(RequestConfirmEmail request)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 아이디로 해당 회원 정보를 가져온다.
					user = await m_userProvider.GetUserByLoginId(request.LoginId);

					// 회원 정보가 유효하지 않은 경우
					if (user == null || user.IsDeleted)
					{
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 이메일 확인 처리된 회원인 경우
						if(user.EmailConfirmed)
						{
							result.Result = EnumResponseResult.Success;
							result.Code = Resource.SC_COMMON__SUCCESS;
							result.Message = Resource.SM_COMMON_ACCOUNT_EMAIL_CONFIRM_SUCCESS;
						}
						// 이메일 확인 처리되지 않은 회원인 경우
						else
						{
							// 이메일 확인 처리
							IdentityResult identityResult = await m_userManager.ConfirmEmailAsync(user, request.Code);

							// 이메일 확인 처리에 성공한 경우
							if (identityResult.Succeeded)
							{
								result.Result = EnumResponseResult.Success;
								result.Code = Resource.SC_COMMON__SUCCESS;
								result.Message = Resource.SM_COMMON_ACCOUNT_EMAIL_CONFIRM_SUCCESS;
							}
							// 이메일 확인 처리에 실패한 경우
							else
							{
								result.Code = Resource.EC_COMMON_ACCOUNT_INVALID_TOKEN;
								result.Message = Resource.EM_COMMON_ACCOUNT_INVALID_TOKEN;
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

		/// <summary>현재 로그인한 사용자의 비밀번호를 변경한다.</summary>
		/// <param name="loginUser">로그인 사용자 정보 객체</param>
		/// <param name="request">비밀번호 요청 객체</param>
		/// <returns>비밀번호 변경 결과</returns>
		public async Task<ResponseData> ChangePassword(ClaimsPrincipal loginUser, RequestChangePassword request)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 현재 비밀번호와 변경하려는 비밀번호가 일치하는 경우
				else if (request.Password == request.NewPassword)
				{
					result.Code = Resource.EC_COMMON_ACCOUNT_CURRENT_PASSWORD_NEW_PASSWORD_SHOULD_NOT_BE_SAME;
					result.Message = Resource.EM_COMMON_ACCOUNT_CURRENT_PASSWORD_NEW_PASSWORD_SHOULD_NOT_BE_SAME;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 로그인한 사용자 계정을 가져온다.
					user = await m_userManager.GetUserAsync(loginUser);

					//해당 계정을 찾을수 없는 경우
					if (user == null || user.IsDeleted)
					{
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 비밀번호 변경
						IdentityResult identityResult = await m_userManager.ChangePasswordAsync(user, request.Password, request.NewPassword);

						// 비밀번호 변경에 성공한 경우
						if (identityResult.Succeeded)
						{
							// 비밀번호 변경 일시를 지금으로 저장
							user.PasswordChangeDate = DateTime.Now;
							await m_userManager.UpdateAsync(user);
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

							result.Result = EnumResponseResult.Success;
							result.Code = Resource.SC_COMMON__SUCCESS;
							result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_CHANGE_PASSWORD;
						}
						// 비밀번호 변경에 실패한 경우
						else
						{
							// 에러 내용 저장
							result.Code = Resource.EC_COMMON_ACCOUNT_FAIL_TO_CHANGE_PASSWORD;
							result.Message = identityResult.Errors.FirstOrDefault() == null ? "" : identityResult.Errors.FirstOrDefault()?.Description;
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

		/// <summary>비밀번호 찾기 요청</summary>
		/// <param name="request">비밀번호 찾기 요청 객체</param>
		/// <param name="httpRequest">HttpRequest 객체</param>
		/// <returns>비밀번호 찾기 요청 처리 결과</returns>
		public async Task<ResponseData> ForgotPassword(RequestForgetPassword request, HttpRequest httpRequest)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 아이디로 해당 회원 정보를 가져온다.
					user = await m_userProvider.GetUserByLoginId(request.LoginId);

					// 회원 정보가 유효하지 않은 경우
					if (user == null || user.IsDeleted)
					{
						// 에러 출력
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;

						//// 외부 해킹을 통해서 가입자 확인이 불가능하도록 하기 위해서 성공 반환
						//result.Result = EnumResponseResult.Success;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 이메일 인증이 되지 않은 경우
						if (await m_userManager.IsEmailConfirmedAsync(user) == false)
						{
							result.Code = Resource.EC_COMMON_ACCOUNT_IS_NOT_AUTH_EMAIL;
							result.Message = Resource.EM_COMMON_ACCOUNT_IS_NOT_AUTH_EMAIL;
						}
						// 이메일 인증이 된 경우
						else
						{
							// 비밀번호 재설정 토큰 생성
							string code = await m_userManager.GeneratePasswordResetTokenAsync(user);

							// 프로토콜과 호스트 저장
							string protocol = request.Protocol.IsEmpty() ? httpRequest.Scheme : request.Protocol;
							string host = request.Host.IsEmpty() ? httpRequest.Host.ToString() : request.Host;
							
							// 메일 내용 중 이메일 주소 확인 버튼 클릭 시 돌아올 콜백 URL
							string callbackUrl;
							callbackUrl = string.Format("{0}://{1}/{2}?userId={3}&code={4}", protocol, host, "Account/ResetPassword", WebUtility.UrlEncode(user.LoginId), WebUtility.UrlEncode(code));

							// 계정 확인 메일에 사용할 대체 문자열 저장
							Dictionary<string, string> replace = new Dictionary<string, string>();
							replace.Add("<%CALLBACK_URL%>", callbackUrl);
							replace.Add("<%DOMAIN%>", string.Format("{0}://{1}", protocol, host));

							// 메일 발송에 성공한 경우
							if (await m_emailSender.SendEmailAsync(user.Email, Resource.UL_COMMON_ACCOUNT_MAIL_PASSWORD_RESET, m_pathProvider.MapPath("/email/" + Resource.EMAIL_COMMON_ACCOUNT_RESET_PASSWORD), replace))
							{
								result.Result = EnumResponseResult.Success;
								result.Code = Resource.SC_COMMON__SUCCESS;
								result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_SEND_RESET_MAIL;
							}
							// 메일 발송에 실패한 경우
							else
							{
								result.Code = Resource.EC_COMMON__MAIL_SEND_ERROR;
								result.Message = Resource.EM_COMMON__MAIL_SEND_ERROR;
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

		/// <summary>비밀번호 재설정</summary>
		/// <param name="request">비밀번호 재설정 요청 객체</param>
		/// <returns>비밀번호 재설정 결과</returns>
		public async Task<ResponseData> ResetPassword(RequestResetPassword request)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 사용자 아이디로 해당 회원 정보를 가져온다.
					user = await m_userProvider.GetUserByLoginId(request.LoginId);

					// 회원 정보가 유효하지 않은 경우
					if (user == null || user.IsDeleted)
					{
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 이메일 인증이 되지 않은 경우
						if (await m_userManager.IsEmailConfirmedAsync(user) == false)
						{
							result.Code = Resource.EC_COMMON_ACCOUNT_IS_NOT_AUTH_EMAIL;
							result.Message = Resource.EM_COMMON_ACCOUNT_IS_NOT_AUTH_EMAIL;
						}
						// 이메일 인증이 된 경우
						else
						{
							// 비밀번호 재설정
							IdentityResult identityResult = await m_userManager.ResetPasswordAsync(user, request.Code, request.NewPassword);

							// 이메일 확인 처리에 성공한 경우
							if (identityResult.Succeeded)
							{
								// 비밀번호 변경 일시를 지금으로 저장
								user.PasswordChangeDate = DateTime.Now;
								await m_userManager.UpdateAsync(user);
								await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

								result.Result = EnumResponseResult.Success;
								result.Code = Resource.SC_COMMON__SUCCESS;
								result.Message = Resource.SM_COMMON_ACCOUNT_SUCCESS_RESET_PASSWORD;
							}
							// 이메일 확인 처리에 실패한 경우
							else
							{
								result.Code = Resource.EC_COMMON_ACCOUNT_FAIL_TO_RESET_PASSWORD;
								result.Message = identityResult.Errors.FirstOrDefault() == null ? "" : identityResult.Errors.FirstOrDefault()?.Description;
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

		/// <summary>현재 로그인한 사용자 정보를 수정한다.</summary>
		/// <param name="loginUser">로그인 사용자 정보 객체</param>
		/// <param name="request">회원 정보 수정 요청 객체</param>
		/// <returns>사용자 정보 수정 결과</returns>
		public async Task<ResponseData> Update(ClaimsPrincipal loginUser, RequestUpdate request)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (!request.IsValid())
				{
					result.Code = request.GetErrorCode();
					result.Message = request.GetErrorMessage();
				}
				// 파라미터가 유효한 경우
				else
				{
					// 로그인한 사용자 계정을 가져온다.
					user = await m_userManager.GetUserAsync(loginUser);

					//해당 계정을 찾을수 없는 경우
					if (user == null || user.IsDeleted)
					{
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 해당 계정을 찾은 경우
					else
					{
						// 사용자 정보 변경
						user.Name = request.Name;
						user.PhoneNumber = request.PhoneNumber;
						await m_userManager.UpdateAsync(user);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						result.Result = EnumResponseResult.Success;
						result.Code = Resource.SC_COMMON__SUCCESS;
						result.Message = Resource.SM_COMMON__UPDATED;
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

		/// <summary>특정 사용자에게 역할을 추가한다.</summary>
		/// <param name="id">회원아이디</param>
		/// <param name="roleName">역할명</param>
		/// <returns>역할 추가 결과</returns>
		public async Task<ResponseData> AddToRole(string id, string roleName)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (id.IsEmpty() || roleName.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 회원아이디로로 해당 회원 정보를 가져온다.
					user = await m_userProvider.GetUserById(id);

					// 회원 정보가 유효하지 않은 경우
					if (user == null || user.IsDeleted)
					{
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 해당 인증이 포함되어 있지 않은 경우
						if (!await m_userManager.IsInRoleAsync(user, roleName))
						{
							await m_userManager.AddToRoleAsync(user, roleName);
							result.Result = EnumResponseResult.Success;
						}
						// 해당 인증이 포함되어 있는 경우
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

		/// <summary>특정 사용자에서 역할을 삭제한다.</summary>
		/// <param name="id">회원아이디</param>
		/// <param name="roleName">역할명</param>
		/// <returns>역할 삭제 결과</returns>
		public async Task<ResponseData> RemoveFromRole(string id, string roleName)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 파라미터가 유효하지 않은 경우
				if (id.IsEmpty() || roleName.IsEmpty())
				{
					result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					result.Message = Resource.EM_COMMON__INVALID_REQUEST;
				}
				// 파라미터가 유효한 경우
				else
				{
					// 회원아이디로로 해당 회원 정보를 가져온다.
					user = await m_userProvider.GetUserById(id);

					// 회원 정보가 유효하지 않은 경우
					if (user == null || user.IsDeleted)
					{
						result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
						result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
					}
					// 회원 정보가 유효한 경우
					else
					{
						// 해당 인증이 포함되어 있는 경우
						if (await m_userManager.IsInRoleAsync(user, roleName))
						{
							await m_userManager.RemoveFromRoleAsync(user, roleName);
							result.Result = EnumResponseResult.Success;
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

		/// <summary>로그인한 사용자에 대한 권한 목록을 가져온다.</summary>
		/// <param name="loginUser">로그인 사용자 정보 객체</param>
		/// <returns>로그인한 사용자에 대한 사용자 목록</returns>
		public async Task<ResponseList<ResponseClaim>> GetClaims(ClaimsPrincipal loginUser)
		{
			ResponseList<ResponseClaim> result = new ResponseList<ResponseClaim>();
			NNApplicationUser user;

			try
			{
				// 로그인한 사용자 계정을 가져온다.
				user = await m_userManager.GetUserAsync(loginUser);

				//해당 계정을 찾을수 없는 경우
				if (user == null || user.IsDeleted)
				{
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
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return result;
		}

		/// <summary>로그인한 사용자의 권한 중 해당 권한이 존재하는지 확인한다.</summary>
		/// <param name="loginUser">로그인 사용자 정보 객체</param>
		/// <param name="claimValue">검사할 권한 값</param>
		/// <returns>로그인한 사용자의 권한 중 해당 권한이 존재하는지 여부</returns>
		public async Task<ResponseData> HasClaim(ClaimsPrincipal loginUser, string claimValue)
		{
			ResponseData result = new ResponseData();
			NNApplicationUser user;

			try
			{
				// 로그인한 사용자 계정을 가져온다.
				user = await m_userManager.GetUserAsync(loginUser);

				//해당 계정을 찾을수 없는 경우
				if (user == null || user.IsDeleted)
				{
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

					// 해당 사용자의 역할 권한 및 사용자 권한에서 해당 권한이 있는 경우
					if (await m_dbContext.RoleClaims.AsNoTracking()
														.Where(i => roleNames.Contains(i.Role.Name)
															&& i.ClaimType == "Permission"
															&& i.ClaimValue == claimValue)
														.AnyAsync()
						|| await m_dbContext.UserClaims.AsNoTracking()
														.Where(i => i.UserId == user.Id
															&& i.ClaimType == "Permission"
															&& i.ClaimValue == claimValue)
														.AnyAsync()
					)
						result.Result = EnumResponseResult.Success;
					else
					{
						result.AccessDenied = true;
						result.Code = Resource.EC_COMMON__ACCESS_DENIED;
						result.Message = Resource.EM_COMMON__ACCESS_DENIED;
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
