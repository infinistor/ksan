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
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalData.Requests.Services.Configs.S3
{
	/// <summary>S3 데이터베이스 설정 요청 객체</summary>
	public class RequestS3ConfigDatabase : RequestS3ConfigBase
	{
		/// <summary>Host</summary>
		public string Host { get; set; } = "";

		/// <summary>Port</summary>
		public int Port { get; set; } = 0;

		/// <summary>Name</summary>
		public string Name { get; set; } = "";

		/// <summary>User Name</summary>
		public string UserName { get; set; } = "";

		/// <summary>Password</summary>
		public string Password { get; set; } = "";

		/// <summary>설정 객체의 내용을 문자열로 변환한다.</summary>
		/// <returns>설정 문자열 응답 객체</returns>
		public override ResponseData<string> Serialize()
		{
			ResponseData<string> result = new ResponseData<string>();
			StringBuilder config = new StringBuilder();

			try
			{
				config.AppendLine($"db.host={this.Host}");
				config.AppendLine($"db.port={this.Port}");
				config.AppendLine($"db.name={this.Name}");
				config.AppendLine($"db.username={this.UserName}");
				config.AppendLine($"db.password={this.Password}");

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