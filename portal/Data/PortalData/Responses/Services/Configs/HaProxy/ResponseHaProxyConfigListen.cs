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
using PortalData.Enums;
using PortalData.Requests.Services.Configs.HaProxy;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalData.Responses.Services.Configs.HaProxy
{
	/// <summary>HA Proxy 서비스 리슨 설정 응답 객체</summary>
	public class ResponseHaProxyConfigListen : ResponseHaProxyConfigBase
	{
		/// <summary>섹션명</summary>
		public override string SectionName { get; } = "listen";

		/// <summary>설정을 읽어 들인다.</summary>
		/// <param name="configContent">설정 내용</param>
		/// <returns>읽기 결과 응답 객체</returns>
		public override ResponseData Deserialize(string configContent)
		{
			ResponseData result = new ResponseData();
			bool isBegin = false;
			StringBuilder config = new StringBuilder();

			try
			{
				using (StringReader reader = new StringReader(configContent))
				{
					string line;

					this.Mode = null;
					this.Balance = null;
					this.Servers.Clear();

					do
					{
						// 한줄을 읽어 들인다.
						line = reader.ReadLine();
					
						// 빈 문자열이 아닌 경우
						if (line != null && !line.IsEmpty())
						{
							// 이름과 값으로 분리한다.
							string[] items = line.Trim().Split(new [] {' ', '\t'}, StringSplitOptions.RemoveEmptyEntries);

							if(items.Length == 0) continue;
							
							// 해당 라인이 섹션명과 동일한 경우
							if (items[0] == SectionName)
							{
								isBegin = true;

								if (items.Length >= 2)
									this.Name = items[1];
							}
							// 해당 라인이 섹션명과 동일하지 않은 경우
							else
							{
								// 섹션이 시작된 경우
								if (isBegin)
								{
									// 지정된 헤더로 시작되는 경우 
									if (Headers.Any(i => line.StartsWith(i)))
										break;

									config.AppendLine(line);
									
									switch (items[0])
									{
										case "bind":
											this.BindPort = (uint) this.GetIntValue(items.Length >= 2 ? items[1].Replace("*:", "") : "", 80);
											break;
										case "mode":
											if (items.Length >= 2)
											{
												switch (items[1])
												{
													case "tcp":
														this.Mode = EnumHaProxyListenMode.Tcp;
														break;
													case "udp":
														this.Mode = EnumHaProxyListenMode.Udp;
														break;
												}
											}
											break;
										case "balance":
											if (items.Length >= 2)
											{
												switch (items[1])
												{
													case "roundrobin":
														this.Balance = EnumHaProxyBalance.RoundRobin;
														break;
													case "static-rr":
														this.Balance = EnumHaProxyBalance.StaticRr;
														break;
													case "leastconn":
														this.Balance = EnumHaProxyBalance.LeastConn;
														break;
													case "source":
														this.Balance = EnumHaProxyBalance.Source;
														break;
												}
											}
											break;
										case "server":
											if (items.Length >= 3)
											{
												RequestHaProxyConfigServer server = new RequestHaProxyConfigServer();
												server.Host = items[1];
												string[] ipAndPort = items[2].Split(new [] {':'}, StringSplitOptions.RemoveEmptyEntries);
												if(ipAndPort.Length >= 1)
													server.IpAddress = ipAndPort[0];
												if(ipAndPort.Length >= 2)
													server.Port = (uint) this.GetIntValue(ipAndPort[1], 80);
												if (items.Length >= 4)
												{
													List<string> paramItems = items.Skip(3).ToList();
													server.Param = paramItems.Aggregate((cur, next) => cur + " " + next);
												}
												this.Servers.Add(server);
											}
											break;
									}
								}
							}
						}
					} while (line != null);
					
					reader.Close();
				}
				
				this.Config = config.ToString();
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
		public uint BindPort { get; set; }

		/// <summary>모드</summary>
		public EnumHaProxyListenMode? Mode { get; set; } = EnumHaProxyListenMode.Tcp;

		/// <summary>발랜스</summary>
		public EnumHaProxyBalance? Balance { get; set; } = EnumHaProxyBalance.Source;

		/// <summary>서버 목록</summary>
		public List<RequestHaProxyConfigServer> Servers { get; set; } = new List<RequestHaProxyConfigServer>();

		/// <summary>설정 내용 문자열</summary>
		public override string Config { get; set; } = "";
	}
}