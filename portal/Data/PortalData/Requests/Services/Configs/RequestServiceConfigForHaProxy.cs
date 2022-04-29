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
using System.Text;
using PortalData.Requests.Services.Configs.HaProxy;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalData.Requests.Services.Configs
{
	/// <summary>HA PROXY 서비스에 대한 설정 요청 객체</summary>
	public class RequestServiceConfigForHaProxy : CommonRequestData, IRequestServiceConfig
	{
		/// <summary>전역 설정</summary>
		public RequestHaProxyConfigGlobal ConfigGlobal { get; set; } = new RequestHaProxyConfigGlobal();

		/// <summary>기본 설정</summary>
		public RequestHaProxyConfigDefault ConfigDefault { get; set; } = new RequestHaProxyConfigDefault();

		/// <summary>서비스 리슨 설정</summary>
		public List<RequestHaProxyConfigListen> ConfigListens { get; set; } = new List<RequestHaProxyConfigListen>();

		/// <summary>설정 객체의 내용을 문자열로 변환한다.</summary>
		/// <returns>설정 문자열</returns>
		public ResponseData<string> Serialize()
		{
			ResponseData<string> result = new ResponseData<string>();
			StringBuilder config = new StringBuilder();

			try
			{
				// global 설정을 읽어온다.
				ResponseData<string> responseGlobal = ConfigGlobal.Serialize();
				if (responseGlobal.Result == EnumResponseResult.Error)
					return new ResponseData<string>(responseGlobal.Result, responseGlobal.Code, responseGlobal.Message);
				config.AppendLine(responseGlobal.Data);

				// default 설정을 읽어온다.
				ResponseData<string> responseDefault = ConfigDefault.Serialize();
				if (responseDefault.Result == EnumResponseResult.Error)
					return new ResponseData<string>(responseDefault.Result, responseDefault.Code, responseDefault.Message);
				config.AppendLine(responseDefault.Data);

				// 모든 listen 설정을 처리 한다.
				foreach (RequestHaProxyConfigListen configListen in ConfigListens)
				{
					ResponseData<string> responseListen = configListen.Serialize();
					if (responseListen.Result == EnumResponseResult.Error)
						return new ResponseData<string>(responseListen.Result, responseListen.Code, responseListen.Message);
					config.AppendLine(responseListen.Data);
				}

				config.AppendLine("listen stats 0.0.0.0:9000");
				config.AppendLine("    mode http");
				config.AppendLine("    balance");
				config.AppendLine("    timeout client 5000");
				config.AppendLine("    timeout connect 4000");
				config.AppendLine("    timeout server 300000");
				config.AppendLine("");
				config.AppendLine("    stats uri /haproxy_stats");
				config.AppendLine("    stats realm HAProxy\\Statistics");
				config.AppendLine("    stats auth admin:admin");
				config.AppendLine("    stats admin if TRUE");

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
	}
}