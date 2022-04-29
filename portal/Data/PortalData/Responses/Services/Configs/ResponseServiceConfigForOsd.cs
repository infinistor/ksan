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
using PortalData.Responses.Services.Configs.OSD;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.Reflection;

namespace PortalData.Responses.Services.Configs
{
	/// <summary>OSD 서비스에 대한 설정 응답 객체</summary>
	public class ResponseServiceConfigForOsd : IResponseServiceConfig
	{
		/// <summary>데이터베이스 설정</summary>
		public ResponseOsdConfigDatabase Database { get; } = new ResponseOsdConfigDatabase();

		/// <summary>메시지 큐 설정</summary>
		public ResponseOsdConfigMessageQueue MessageQueue { get; } = new ResponseOsdConfigMessageQueue();

		/// <summary>설정 문자열을 설정 객체로 변환한다.</summary>
		/// <param name="config">설정 문자열</param>
		/// <returns>설정 문자열을 반영한 설정 객체</returns>
		public ResponseData Deserialize(string config)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 설정 내용이 존재하지 않는 경우
				if (config.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				ResponseServiceConfigForOsd request = new ResponseServiceConfigForOsd();

				// 메인 설정을 읽어온다.
				ResponseData response = request.Database.Deserialize(config);
				if (response.Result == EnumResponseResult.Error)
					return new ResponseData(response.Result, response.Code, response.Message);

				// Proxy 설정을 읽어온다.
				response = request.MessageQueue.Deserialize(config);
				if (response.Result == EnumResponseResult.Error)
					return new ResponseData(response.Result, response.Code, response.Message);

				Database.CopyValueFrom(request.Database);
				MessageQueue.CopyValueFrom(request.MessageQueue);
				Config = config;
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

		/// <summary>설정 내용 문자열</summary>
		public string Config { get; set; }
	}
}