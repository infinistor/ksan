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
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;
using PortalProviderInterface;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalSvr.Services
{
	/// <summary>역할 초기화 인터페이스</summary>
	public interface IRoleInitializer
	{
		/// <summary>역할이 하나도 없는 경우, 기본 역할을 생성한다.</summary>
		Task Initialize();
	}

	/// <summary>역할 초기화 클래스</summary>
	public class RoleInitializer : IRoleInitializer
	{
		/// <summary>프로바이더</summary>
		private readonly IRoleProvider m_provider;

		/// <summary>생성자</summary>
		/// <param name="provider">계정에 대한 프로바이더 객체</param>
		public RoleInitializer(IRoleProvider provider)
		{
			m_provider = provider;
		}

		/// <summary>역할이 하나도 없는 경우, 기본 역할을 생성한다.</summary>
		public async Task Initialize()
		{
			try
			{
				// Supervisor 역할이 존재하지 않는 경우
				ResponseData<ResponseRole> supervisorRole = await m_provider.GetRoleByName(PredefinedRoleNames.RoleNameSupervisor);
				if (supervisorRole.Result == EnumResponseResult.Error && supervisorRole.Code == Resource.EC_COMMON__NOT_FOUND)
				{
					// Supervisor 역할 추가
					supervisorRole = await m_provider.AddRole(new RequestRole() { Name = PredefinedRoleNames.RoleNameSupervisor });
				}

				// InternalService 역할이 존재하지 않는 경우
				ResponseData<ResponseRole> internalServiceRole = await m_provider.GetRoleByName(PredefinedRoleNames.RoleNameInternalService);
				if (internalServiceRole.Result == EnumResponseResult.Error && internalServiceRole.Code == Resource.EC_COMMON__NOT_FOUND)
				{
					// InternalService 역할 추가
					internalServiceRole = await m_provider.AddRole(new RequestRole() { Name = PredefinedRoleNames.RoleNameInternalService });
				}

				// User 역할이 존재하지 않는 경우
				ResponseData<ResponseRole> userRole = await m_provider.GetRoleByName(PredefinedRoleNames.RoleNameUser);
				if (userRole.Result == EnumResponseResult.Error && userRole.Code == Resource.EC_COMMON__NOT_FOUND)
				{
					// User 역할 추가
					userRole = await m_provider.AddRole(new RequestRole() { Name = PredefinedRoleNames.RoleNameUser });
				}

				// ClaimInitializer claimInitializer = new ClaimInitializer(m_provider);
				// // 권한 초기화
				// await claimInitializer.InitializeClaims();
				// // Supervisor 의 권한 추가
				// await claimInitializer.InitializeSupervisor(supervisorRole.Data.Id.ToString());
				// // InternalService 의 권한 추가
				// await claimInitializer.InitializeInternalService(internalServiceRole.Data.Id.ToString());
				// // User 의 권한 추가
				// await claimInitializer.InitializeUser(userRole.Data.Id.ToString());
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
	}
}