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
using System.Security.Claims;
using System.Threading.Tasks;
using PortalData;
using PortalProviderInterface;
using PortalResources;
using Microsoft.AspNetCore.Identity;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.Reflection;
using Newtonsoft.Json;

namespace PortalProvider
{
	/// <summary>프로바이더 기본 클래스</summary>
	public class BaseProvider<T> : IBaseProvider
	{
		/// <summary>디비 컨텍스트</summary>
		protected readonly T m_dbContext;
		/// <summary>설정 정보</summary>
		protected readonly IConfiguration m_configuration;
		/// <summary>사용자 관리자</summary>
		protected readonly UserManager<NNApplicationUser> m_userManager;
		/// <summary>시스템 로그 프로바이더</summary>
		protected readonly ISystemLogProvider m_systemLogProvider;
		/// <summary>사용자 동작 로그 프로바이더</summary>
		protected readonly IUserActionLogProvider m_userActionLogProvider;
		/// <summary>서비스 팩토리</summary>
		protected readonly IServiceScopeFactory m_serviceScopeFactory;
		/// <summary>로거</summary>
		protected readonly ILogger m_logger;

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public BaseProvider(
			T dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger logger
			)
		{
			m_dbContext = dbContext;
			m_configuration = configuration;
			m_userManager = userManager;
			m_systemLogProvider = systemLogProvider;
			m_userActionLogProvider = userActionLogProvider;
			m_serviceScopeFactory = serviceScopeFactory;
			m_logger = logger;
		}

		/// <summary>로그인 사용자 권한 정보</summary>
		public ClaimsPrincipal UserClaimsPrincipal
		{
			get => m_userClaimsPrincipal;
			set => m_userActionLogProvider.UserClaimsPrincipal = m_userClaimsPrincipal = value;
		}
		private ClaimsPrincipal m_userClaimsPrincipal;

		/// <summary>로그인 사용자 정보</summary>
		public NNApplicationUser LoginUser
		{
			get
			{
				if (m_userManager == null)
					return null;
				else
				{
					if (m_loginUser == null && UserClaimsPrincipal != null)
						m_loginUser = m_userManager.GetUserAsync(UserClaimsPrincipal).Result;

					return m_loginUser;
				}
			}
		}
		private NNApplicationUser m_loginUser;

		/// <summary>로그인 사용자 아이디</summary>
		public Guid? LoginUserId
		{
			get
			{
				if (LoginUser != null)
					return LoginUser.Id;
				else
					return null;
			}
		}

		/// <summary>로그인 사용자명</summary>
		public string LoginUserName
		{
			get
			{
				if (LoginUser != null)
					return LoginUser.Name;
				else
					return "";
			}
		}

		/// <summary>접속한 아이피 주소</summary>
		public string UserIpAddress
		{
			get
			{
				return m_userIpAddress;
			}
			set
			{
				m_userActionLogProvider.UserIpAddress = m_userIpAddress = value;
			}
		}
		private string m_userIpAddress = "";

		/// <summary>로그인 사용자의 역할 목록</summary>
		public async Task<List<string>> GetUserRoles()
		{
			var Result = new List<string>();

			if (m_userManager != null)
			{
				// 로그인한 사용자의 역할을 가져온다.
				var Roles = await m_userManager.GetRolesAsync(LoginUser);

				if (Roles != null && Roles.Count > 0)
					Result.AddRange(Roles);
			}

			return Result;
		}

		/// <summary>검색어 필드를 초기화한다.</summary>
		/// <param name="SearchFields">검색어 필드 목록</param>
		protected void InitSearchFields(ref List<string> SearchFields)
		{
			// 검색 필드 목록을 모두 소문자로 변환
			if (SearchFields != null)
				SearchFields = SearchFields.ToLower();
		}

		/// <summary>기본 정렬 필드</summary>
		protected List<string> DefaultOrderFields { get; } = new List<string>();

		/// <summary>기본 정렬 방향</summary>
		protected List<string> DefaultOrderDirections { get; } = new List<string>();

		/// <summary>기본 정렬 정보를 모두 삭제한다.</summary>
		protected void ClearDefaultOrders()
		{
			DefaultOrderFields.Clear();
			DefaultOrderDirections.Clear();
		}

		/// <summary>기본 정렬 정보를 추가한다.</summary>
		/// <param name="Field">필드명</param>
		/// <param name="Direction">정렬방향</param>
		protected void AddDefaultOrders(string Field, string Direction)
		{
			DefaultOrderFields.Add(Field);
			DefaultOrderDirections.Add(Direction);
		}

		/// <summary>정렬 필드를 초기화한다.</summary>
		/// <param name="OrderFields">정렬 필드 목록</param>
		/// <param name="OrderDirections">정렬 필드 목록</param>
		protected void InitOrderFields(ref List<string> OrderFields, ref List<string> OrderDirections)
		{
			OrderFields ??= new List<string>();
			OrderDirections ??= new List<string>();

			// 정렬 필드가 지정되지 않은 경우, 기본 정렬 필드 설정
			if (OrderFields.Count == 0 && DefaultOrderFields.Count > 0)
			{
				OrderFields.AddRange(DefaultOrderFields);
				OrderDirections.Clear();
			}

			// 정렬방향목록이 지정되지 않은 경우, 기본 정렬방향목록 설정
			if (OrderDirections.Count == 0 && DefaultOrderDirections.Count > 0 && OrderFields.TrueForAll(i => DefaultOrderFields.Contains(i)))
				OrderDirections.AddRange(DefaultOrderDirections);
		}

		/// <summary>RPC로 Message Queue를 전송한다.</summary>
		/// <param name="RoutingKey">라우팅 키</param>
		/// <param name="Request">전송할 객체</param>
		/// <param name="WaitForResponseTimeoutSec">응답 대기 타임 아웃 시간 (초)</param>
		/// <returns>전송 결과 객체</returns>
		protected ResponseData SendRpcMq(string RoutingKey, object Request, int WaitForResponseTimeoutSec)
		{
			var Result = new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

			try
			{
				using var ServiceScope = m_serviceScopeFactory.CreateScope();
				// Rabbit MQ RPC 객체를 생성한다.
				var RabbitMQRpc = ServiceScope.ServiceProvider.GetService<IRabbitMQRpc>();
				// Rabbit MQ RPC 객체 생성에 실패한 경우
				if (RabbitMQRpc == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__FAIL_TO_CREATE_COMMUNICATION_OBJECT, Resource.EM_COMMON__FAIL_TO_CREATE_COMMUNICATION_OBJECT);

				// 디스크가 이미 마운트되어 있는지 확인 요청 전송
				var Response = RabbitMQRpc.Send(RoutingKey, Request, WaitForResponseTimeoutSec);

				// 에러인 경우
				if (Response.Result == EnumResponseResult.Error) Result.CopyValueFrom(Response);
				// 에러가 아닌 경우
				else
				{
					// 결과 데이터를 객체로 변환
					ResponseData responseData = JsonConvert.DeserializeObject<ResponseData>(Response.Data);
					Result.CopyValueFrom(responseData);
				}

				RabbitMQRpc.Close();
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>RPC로 Message Queue를 전송한다.</summary>
		/// <param name="Exchange">Exchange 명</param>
		/// <param name="RoutingKey">라우팅 키</param>
		/// <param name="Request">전송할 객체</param>
		/// <param name="WaitForResponseTimeoutSec">응답 대기 타임 아웃 시간 (초)</param>
		/// <returns>전송 결과 객체</returns>
		protected ResponseData<U> SendRpcMq<U>(string Exchange, string RoutingKey, object Request, int WaitForResponseTimeoutSec)
		{
			var Result = new ResponseData<U>(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

			try
			{
				using var ServiceScope = m_serviceScopeFactory.CreateScope();
				// Rabbit MQ RPC 객체를 생성한다.
				var RabbitMQRpc = ServiceScope.ServiceProvider.GetService<IRabbitMQRpc>();
				// Rabbit MQ RPC 객체 생성에 실패한 경우
				if (RabbitMQRpc == null)
					return new ResponseData<U>(EnumResponseResult.Error, Resource.EC_COMMON__FAIL_TO_CREATE_COMMUNICATION_OBJECT, Resource.EM_COMMON__FAIL_TO_CREATE_COMMUNICATION_OBJECT);

				// 요청 전달
				var Response = RabbitMQRpc.Send(RoutingKey, Request, WaitForResponseTimeoutSec);

				// 에러인 경우
				if (Response.Result == EnumResponseResult.Error)
					Result.CopyValueFrom(Response);
				// 에러가 아닌 경우
				else
				{
					// 결과 데이터를 객체로 변환
					var responseData = JsonConvert.DeserializeObject<ResponseData<U>>(Response.Data);
					Result.CopyValueFrom(responseData);
				}

				RabbitMQRpc.Close();
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>RPC로 Message Queue를 전송한다.</summary>
		/// <param name="RoutingKey">라우팅 키</param>
		/// <param name="Request">전송할 객체</param>
		/// <returns>전송 결과 객체</returns>
		protected ResponseData SendMq(string RoutingKey, object Request)
		{
			var Result = new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__CANNOT_CREATE_INSTANCE, Resource.EM_COMMON__CANNOT_CREATE_INSTANCE);

			try
			{
				// DI 객체를 생성한다.
				using var ServiceScope = m_serviceScopeFactory.CreateScope();
				// Rabbit MQ Sender 객체를 생성한다.
				var RabbitMQSender = ServiceScope.ServiceProvider.GetService<IRabbitMQSender>();
				// Rabbit MQ Sender 객체 생성에 실패한 경우
				if (RabbitMQSender == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__FAIL_TO_CREATE_COMMUNICATION_OBJECT, Resource.EM_COMMON__FAIL_TO_CREATE_COMMUNICATION_OBJECT);

				// 추가된 디스크 정보 전송
				Result = RabbitMQSender.Send(RoutingKey, Request);
				RabbitMQSender.Close();
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
