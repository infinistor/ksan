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
using System.Collections.Generic;
using MTLib.Core;

namespace PortalData.Responses.Services.Configs.HaProxy
{
	/// <summary>HA Proxy 설정 기본 응답 객체</summary>
	public abstract class ResponseHaProxyConfigBase : IResponseServiceConfig
	{
		/// <summary>헤더 목록</summary>
		[Newtonsoft.Json.JsonIgnore]
		[System.Text.Json.Serialization.JsonIgnore]
		public static List<string> Headers = new List<string>() { "global", "defaults", "listen" };

		/// <summary>섹션명</summary>
		[Newtonsoft.Json.JsonIgnore]
		[System.Text.Json.Serialization.JsonIgnore]
		public virtual string SectionName { get; } = "";

		/// <summary>설정을 읽어 들인다.</summary>
		/// <param name="configContent">설정 내용</param>
		/// <returns>읽기 결과 응답 객체</returns>
		public abstract ResponseData Deserialize(string configContent);

		/// <summary>문자열을 정수값으로 반환한다. (변환하지 못하는 경우, 기본 값으로 반환)</summary>
		/// <param name="valueString">문자열 값</param>
		/// <param name="defaultValue">기본 값</param>
		/// <returns>정수 값</returns>
		protected int? GetIntValue(string valueString, int? defaultValue)
		{
			int? result = defaultValue;

			// 문자열 값이 빈문자열이 아닌 경우
			if (!valueString.IsEmpty())
			{
				if (int.TryParse(valueString, out int temp))
					result = temp;
			}

			return result;
		}

		/// <summary>설정 내용 문자열</summary>
		public virtual string Config { get; set; } = "";
	}
}