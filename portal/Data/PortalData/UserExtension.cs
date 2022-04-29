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
using MTLib.Core;

namespace PortalData
{
	/// <summary>사용자 정보 확장 클래스</summary>
	public static class UserExtension
	{
		/// <summary>화면 표시명 포맷</summary>
		public static string DisplayNameFormat { get; set; } = "{name}";

		/// <summary>화면 표시 사용자명을 반환한다.</summary>
		/// <param name="loginId">로그인 아이디</param>
		/// <param name="name">사용자명</param>
		/// <param name="email">이메일 주소</param>
		/// <returns>화면 표시 사용자명</returns>
		public static string GetDisplayName(string loginId, string email, string name)
		{
			string result = "";
			try
			{
				if (DisplayNameFormat.IsEmpty())
					result = name;
				else
				{
					result = DisplayNameFormat.Replace("{login_id}", loginId)
											.Replace("{name}", name)
											.Replace("{email}", email);
				}
			}
			catch (System.Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}
	}
}
