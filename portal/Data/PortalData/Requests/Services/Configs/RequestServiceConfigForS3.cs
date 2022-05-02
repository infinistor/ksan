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
using System.Text;
using PortalData.Requests.Services.Configs.S3;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalData.Requests.Services.Configs
{
	/// <summary>S3 서비스에 대한 설정 요청 객체</summary>
	public class RequestServiceConfigForS3 : CommonRequestData, IRequestServiceConfig
	{
		/// <summary>데이터베이스 설정</summary>
		public RequestS3ConfigDatabase Database { get; } = new RequestS3ConfigDatabase();

		/// <summary>메시지 큐 설정</summary>
		public RequestS3ConfigMessageQueue MessageQueue { get; } = new RequestS3ConfigMessageQueue();

		/// <summary>설정 객체의 내용을 문자열로 변환한다.</summary>
		/// <returns>설정 문자열</returns>
		public ResponseData<string> Serialize()
		{
			ResponseData<string> result = new ResponseData<string>();
			StringBuilder config = new StringBuilder();

			try
			{
				// Database 설정을 읽어온다.
				ResponseData<string> responseMain = Database.Serialize();
				if (responseMain.Result == EnumResponseResult.Error)
					return new ResponseData<string>(responseMain.Result, responseMain.Code, responseMain.Message);
				config.AppendLine(responseMain.Data);

				// Message Queue 설정을 읽어온다.
				ResponseData<string> responseProxy = MessageQueue.Serialize();
				if (responseProxy.Result == EnumResponseResult.Error)
					return new ResponseData<string>(responseProxy.Result, responseProxy.Code, responseProxy.Message);
				config.AppendLine(responseProxy.Data);

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