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
using System.IO;
using System.Linq;
using System.Text;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalData.Responses.Services.Configs.HaProxy
{
	/// <summary>HA Proxy 전역 설정 응답 객체</summary>
	public class ResponseHaProxyConfigGlobal : ResponseHaProxyConfigBase
	{
		/// <summary>섹션명</summary>
		public override string SectionName { get; } = "global";

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

					this.Daemon = false;
					this.NbProc = null;
					this.NbThread = null;
					
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
							}
							// 해당 라인이 섹션명과 동일하지 않은 경우
							else
							{
								// 섹션이 시작된 경우
								if (isBegin)
								{
									
									// 지정된 헤더로 시작되는 경우 
									if (Headers.Any(i => items[0] == i))
										break;

									config.AppendLine(line);
									
									switch (items[0])
									{
										case "log":
											this.LogIpAddress = items.Length >= 2 ? items[1] : "127.0.0.1";
											this.LogHost = items.Length >= 3 ? items[2] : "local2";
											break;
										case "chroot":
											this.ChRoot = items.Length >= 2 ? items[1] : "/var/lib/haproxy";
											break;
										case "pidfile":
											this.PidFile = items.Length >= 2 ? items[1] : "/var/run/haproxy.pid";
											break;
										case "maxconn":
											this.MaxConn =  (int)this.GetIntValue(items.Length >= 2 ? items[1] : "", 50000);
											break;
										case "daemon":
											this.Daemon = true;
											break;
										case "nbproc":
											this.NbProc =  (ushort?) this.GetIntValue(items.Length >= 2 ? items[1] : "", null);
											break;
										case "nbthread":
											this.NbProc =  (ushort?) this.GetIntValue(items.Length >= 2 ? items[1] : "", null);
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

		/// <summary>log 아이피</summary>
		public string LogIpAddress { get; set; } = "127.0.0.1";

		/// <summary>log 아이피</summary>
		public string LogHost { get; set; } = "local2";

		/// <summary>chroot</summary>
		public string ChRoot { get; set; } = "/var/lib/haproxy";

		/// <summary>pidfile</summary>
		public string PidFile { get; set; } = "/var/run/haproxy.pid";

		/// <summary>maxconn</summary>
		public int MaxConn { get; set; } = 50000;

		/// <summary>daemon (true 시 기록)</summary>
		public bool Daemon { get; set; } = true;

		/// <summary>nbproc (null아 아닌 경우만 기록)</summary>
		public ushort? NbProc { get; set; }

		/// <summary>nbproc (null아 아닌 경우만 기록)</summary>
		public ushort? NbThread { get; set; }

		/// <summary>설정 내용 문자열</summary>
		public override string Config { get; set; } = "";
	}
}