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
using System.Text;
using PortalData.Enums;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalData.Requests.Services.Configs.HaProxy
{
	/// <summary>HA Proxy 서비스 리슨 설정 요청 객체</summary>
	public class RequestHaProxyConfigListen : RequestHaProxyConfigBase
	{
		/// <summary>섹션명</summary>
		public override string SectionName { get; } = "listen";

		/// <summary>설정 문자열로 변환한다.</summary>
		/// <returns>설정 문자열 응답 객체</returns>
		public override ResponseData<string> Serialize()
		{
			ResponseData<string> result = new ResponseData<string>();
			StringBuilder config = new StringBuilder();

			try
			{
				config.AppendLine($"{this.SectionName} {this.Name}");
				config.AppendLine($"\tbind *:{this.BindPort}");
				if (this.Mode != null)
				{
					string mode = this.Mode == EnumHaProxyListenMode.Tcp ? "tcp" : "udp";
					config.AppendLine($"\tmode {mode}");
				}
				if (this.Balance != null)
				{
					switch (this.Balance)
					{
						case EnumHaProxyBalance.RoundRobin:
							config.AppendLine($"\tbalance roundrobin");
							break;
						case EnumHaProxyBalance.StaticRr:
							config.AppendLine($"\tbalance static-rr");
							break;
						case EnumHaProxyBalance.LeastConn:
							config.AppendLine($"\tbalance leastconn");
							break;
						case EnumHaProxyBalance.Source:
							config.AppendLine($"\tbalance source");
							break;
					}
				}

				// 모든 서버에 대해서 처리
				foreach (RequestHaProxyConfigServer server in this.Servers)
				{
					List<string> serverItem = new List<string>();

					serverItem.Add($"{server.Host}");
					if (server.Port == null)
						serverItem.Add($"{server.IpAddress}");
					else
						serverItem.Add($"{server.IpAddress}:{server.Port}");
					serverItem.Add($"{server.Param}");

					string serverContent = serverItem.Aggregate((cur, next) => cur + " " + next);

					config.AppendLine($"\tserver {serverContent}");
				}

				result.Data = config.ToString();
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

		/// <summary>리슨명</summary>
		public string Name { get; set; } = "";

		/// <summary>바인드 포트</summary>
		public uint BindPort { get; set; } = 0;

		/// <summary>모드</summary>
		public EnumHaProxyListenMode? Mode { get; set; } = EnumHaProxyListenMode.Tcp;

		/// <summary>발랜스</summary>
		public EnumHaProxyBalance? Balance { get; set; } = EnumHaProxyBalance.Source;

		/// <summary>서버 목록</summary>
		public List<RequestHaProxyConfigServer> Servers { get; set; } = new List<RequestHaProxyConfigServer>();
	}
}