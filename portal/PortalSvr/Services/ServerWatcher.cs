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
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using MTLib.Core;
using PortalData.Enums;
using PortalProviderInterface;

namespace PortalSvr.Services
{
	/// <summary> 서버 감시 인터페이스</summary>
	public interface IServerWatcher : IHostedService, IDisposable
	{

	}

	/// <summary> 서버 감시 클래스</summary>
	public class ServerWatcher : IServerWatcher
	{
		/// <summary> 서버 프로바이더</summary>
		protected readonly IServerProvider m_serverProvider;

		/// <summary> 타이머</summary>
		private Timer m_timer = null;

		/// <summary>로거</summary>
		private readonly ILogger m_logger;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="serverProvider">서버에 대한 프로바이더 객체</param>
		/// <param name="logger">로거</param>
		public ServerWatcher(IConfiguration configuration
			, IServerProvider serverProvider
			, ILogger<ServerWatcher> logger)
		{
			m_serverProvider = serverProvider;
			m_logger = logger;
		}


		/// <summary> 서버 감시 시작 </summary>
		public Task StartAsync(System.Threading.CancellationToken cancellationToken)
		{

			m_timer = new Timer(DoWork, null, TimeSpan.Zero, TimeSpan.FromSeconds(5));
			m_logger.LogInformation("Server Whtaher Start");
			return Task.CompletedTask;
		}

		/// <summary> 서버 감시 정지 </summary>
		public Task StopAsync(System.Threading.CancellationToken cancellationToken)
		{
			m_timer.Change(Timeout.Infinite, 0);
			m_logger.LogInformation("Server Whtaher Stop");
			return Task.CompletedTask;
		}

		/// <summary> 서버 감시 해제 </summary>
		public void Dispose()
		{
			m_timer.Dispose();
		}

		private async void DoWork(object state)
		{
			try
			{
				// 서버의 갱신 임계값을 가져온다.
				var Threshold = await m_serverProvider.GetThreshold();

				//온라인 상태인 서버 목록을 가져온다.
				var SearchStates = new List<EnumServerState>() { EnumServerState.Online };
				var Servers = await m_serverProvider.GetList(SearchStates:SearchStates);

				// 타임아웃 값을 계산한다
				var Timeout = DateTime.Now.AddMilliseconds(-Threshold.Data);

				foreach (var Server in Servers.Data.Items)
				{
					// 서버의 갱신일자가 임계값을 넘었을 경우 Timeout 상태로 변경한다.
					if (Server.ModDate < Timeout)
						await m_serverProvider.UpdateState(Server.Id, EnumServerState.Timeout);
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}

		}

	}

}