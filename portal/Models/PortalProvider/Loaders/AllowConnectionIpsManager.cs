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
using System.Threading.Tasks;
using PortalModels;
using PortalProviderInterface;
using Microsoft.EntityFrameworkCore;
using MTLib.Core;

namespace PortalProvider.Loaders
{
	/// <summary>접속 허용 아이피 검사 클래스</summary>
	public class AllowConnectionIpsManager : IAllowConnectionIpsManager
	{
		/// <summary>허용된 아이피 목록</summary>
		private List<AllowedConnectionIp> m_allowedConnectionIps = new List<AllowedConnectionIp>();

		/// <summary>전체 허용인지 여부</summary>
		public bool IsAllAllowed => m_isAllAllowed;
		private bool m_isAllAllowed;

		/// <summary>생성자</summary>
		public AllowConnectionIpsManager()
		{
			try
			{
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
		
		/// <summary>19219
		/// 허용된 아이피 목록을 로드한다.</summary>
		/// <param name="context">DB 컨텍스트</param>
		/// <returns>로드 성공 여부</returns>
		public async Task<bool> LoadAllowedConnectionIps(DbContext context)
		{
			bool result = false;

			m_isAllAllowed = false;
			
			try
			{
				if (context != null)
				{
					m_allowedConnectionIps = await ((PortalModel)context).AllowedConnectionIps.AsNoTracking().ToListAsync();

					if (m_allowedConnectionIps.Count == 0)
						m_isAllAllowed = true;

					result = true;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}

		/// <summary>특정 역할에 해당하는 접속 허용 아이피인지 여부</summary>
		/// <param name="roleId">역할 아이디</param>
		/// <param name="address">검사할 아이피 주소</param>
		/// <returns>접속 허용 아이피인지 여부</returns>
		public bool IsAllowIp(string roleId, IPAddress address)
		{
			bool result = false;
			uint addressNumber;
			try
			{
				// 역할 아이디와 주소가 유효한 경우
				if (!roleId.IsEmpty() && address != null)
				{
					// 숫자로 변환
					addressNumber = address.ToUint();

					// 허용된 아이피 목록을 설정하지 않았거나, 설정된 범위 내의 아이피인 경우
					if (m_allowedConnectionIps.All(i => i.RoleId.ToString() != roleId)
					    || m_allowedConnectionIps.Any(i => i.RoleId.ToString() == roleId && i.StartAddress <= addressNumber && addressNumber <= i.EndAddress))
						result = true;
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
