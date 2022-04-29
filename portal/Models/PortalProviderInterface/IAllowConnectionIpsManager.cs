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
using System.Net;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;

namespace PortalProviderInterface
{
	/// <summary>접속 허용 아이피 검사 인터페이스</summary>
	public interface IAllowConnectionIpsManager
	{
		/// <summary>전체 허용인지 여부</summary>
		bool IsAllAllowed { get; }

		/// <summary>특정 역할에 대한 접속 허용 아이피인지 여부</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="address">검사할 아이피 주소</param>
		/// <returns>접속 허용 아이피인지 여부</returns>
		bool IsAllowIp(string roleId, IPAddress address);

		/// <summary>허용된 아이피 목록을 로드한다.</summary>
		/// <param name="context">DB 컨텍스트</param>
		/// <returns>로드 성공 여부</returns>
		Task<bool> LoadAllowedConnectionIps(DbContext context);
	}
}
