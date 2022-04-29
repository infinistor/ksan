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
using PortalData.Enums;
using Microsoft.AspNetCore.Identity;
using MTLib.AspNetCore;
using MTLib.Core;

namespace PortalProvider
{
	/// <summary>UserManager 확장 클래스</summary>
	public static class UserManagerExtension
	{
		/// <summary>사용자 상태를 가져온다.</summary>
		/// <param name="userManager">사용자 관리자 객체</param>
		/// <param name="user">사용자 객체</param>
		/// <returns>사용자 상태</returns>
		public static async Task<EnumUserStatus> GetUserStatus(this UserManager<NNApplicationUser> userManager, NNApplicationUser user)
		{
			EnumUserStatus result = EnumUserStatus.Locked;
			try
			{
				if (user != null)
				{
					// 계정이 잠금상태인 경우
					if (await userManager.IsLockedOutAsync(user))
						result = EnumUserStatus.Locked;
					// 이메일 확인이 되지 않은 경우
					else if (!await userManager.IsEmailConfirmedAsync(user))
						result = EnumUserStatus.VerifyingEmail;
					// 그외는 활성화 상태로 저장
					else
						result = EnumUserStatus.Activated;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}
	}
}
