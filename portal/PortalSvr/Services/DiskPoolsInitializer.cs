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
using PortalData.Requests.Disks;
using PortalProviderInterface;
using MTLib.Core;

namespace PortalSvr.Services
{
	/// <summary>디스크풀 초기화 인터페이스</summary>
	public interface IDiskPoolsInitializer
	{
		/// <summary>디스크풀이 하나도 없을 경우 기본 디스크풀을 생성한다.</summary>
		Task Initialize();
	}

	/// <summary>역할 초기화 클래스</summary>
	public class DiskPoolsInitializer : IDiskPoolsInitializer
	{
		/// <summary>프로바이더</summary>
		private readonly IDiskPoolProvider m_provider;

		/// <summary>생성자</summary>
		/// <param name="provider">계정에 대한 프로바이더 객체</param>
		public DiskPoolsInitializer(IDiskPoolProvider provider)
		{
			m_provider = provider;
		}

		/// <summary>디스크풀이 하나도 없을 경우 기본 디스크풀을 생성한다.</summary>
		public async Task Initialize()
		{
			try
			{
				// // 디스크풀 목록을 가져온다.
				// var Response = await m_provider.GetList();

				// // 목록이 비어있지 않는 경우 종료
				// if (Response.Data.Items.Count > 0) return;

				// RequestDiskPool request = new RequestDiskPool()
				// {
				// 	Name = "Default",
				// 	Description = "Default disk pool"
				// };

				// await m_provider.Add(request);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
	}
}
