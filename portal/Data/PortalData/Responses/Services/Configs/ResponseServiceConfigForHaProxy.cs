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
using System.IO;
using System.Linq;
using System.Text;
using PortalData.Responses.Services.Configs.HaProxy;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalData.Responses.Services.Configs
{
	/// <summary>HA PROXY 서비스에 대한 설정 응답 객체</summary>
	public class ResponseServiceConfigForHaProxy : IResponseServiceConfig
	{
		/// <summary>전역 설정</summary>
		public ResponseHaProxyConfigGlobal ConfigGlobal { get; } = new ResponseHaProxyConfigGlobal();

		/// <summary>기본 설정</summary>
		public ResponseHaProxyConfigDefault ConfigDefault { get; } = new ResponseHaProxyConfigDefault();

		/// <summary>서비스 리슨 설정</summary>
		public List<ResponseHaProxyConfigListen> ConfigListens { get; } = new List<ResponseHaProxyConfigListen>();

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

				// 섹션별 내용
				List<KeyValuePair<string, string>> sectionContents = new List<KeyValuePair<string, string>>();
				// 현재 섹션 내용
				StringBuilder currentSection = new StringBuilder();
				// 현재 섹션명
				string currentSectionName = "";

				using (StringReader reader = new StringReader(config))
				{
					string line;

					do
					{
						// 한줄을 읽어 들인다.
						line = reader.ReadLine();

						// 빈 문자열이 아닌 경우
						if (line != null && !line.IsEmpty())
						{
							// 이름과 값으로 분리한다.
							string[] items = line.Trim().Split(new[] { ' ', '\t' }, StringSplitOptions.RemoveEmptyEntries);

							// 지정된 헤더로 시작되는 경우
							if (ResponseHaProxyConfigBase.Headers.Any(i => items[0] == i))
							{
								// 현재 처리 중인 섹션명이 존재하는 경우, 지금까지의 내용 저장
								if (!currentSectionName.IsEmpty())
									sectionContents.Add(new KeyValuePair<string, string>(currentSectionName, currentSection.ToString()));

								currentSection.Clear();
								currentSectionName = items[0];
								currentSection.AppendLine(line);

								// 해당 라인이 섹션명과 동일한 경우
								if (items.Length >= 3 && items[1] == "stats")
									currentSectionName = items[0] + " stats";
							}
							// 지정된 헤더로 시작되지 않는 경우
							else
								currentSection.AppendLine(line);
						}
						// 빈 문자열인 경우
						else
						{
							if (!currentSectionName.IsEmpty())
								currentSection.AppendLine("");
						}
					} while (line != null);

					// 현재 처리 중인 섹션명이 존재하는 경우, 지금까지의 내용 저장
					if (!currentSectionName.IsEmpty())
						sectionContents.Add(new KeyValuePair<string, string>(currentSectionName, currentSection.ToString()));

					reader.Close();
				}

				// 모든 섹션에 대해서 처리
				foreach (KeyValuePair<string, string> section in sectionContents)
				{
					switch (section.Key)
					{
						case "global":
							{
								// global 설정을 읽어온다.
								ResponseData response = ConfigGlobal.Deserialize(section.Value);
								if (response.Result == EnumResponseResult.Error)
									return new ResponseData(response.Result, response.Code, response.Message);
							}
							break;
						case "defaults":
							{
								// default 설정을 읽어온다.
								ResponseData response = ConfigDefault.Deserialize(section.Value);
								if (response.Result == EnumResponseResult.Error)
									return new ResponseData(response.Result, response.Code, response.Message);
							}
							break;
						case "listen":
							{
								ResponseHaProxyConfigListen listenConfig = new ResponseHaProxyConfigListen();
								ResponseData response = listenConfig.Deserialize(section.Value);
								if (response.Result == EnumResponseResult.Success)
									ConfigListens.Add(listenConfig);
							}
							break;
					}
				}

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