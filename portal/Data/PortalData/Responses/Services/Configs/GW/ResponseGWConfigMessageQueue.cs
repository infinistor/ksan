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
using System.Text;
using PortalResources;
using MTLib.CommonData;
using MTLib.Core;

namespace PortalData.Responses.Services.Configs.GW
{
	/// <summary>S3 Proxy 메시지 큐 설정 응답 객체</summary>
	public class ResponseGWConfigMessageQueue : ResponseGWConfigBase
	{
		/// <summary>Host</summary>
		public string Host { get; set; } = "";

		/// <summary>Port</summary>
		public int Port { get; set; }

		/// <summary>Disk Pool Queue Name</summary>
		public string DiskPoolQueueName { get; set; } = "";

		/// <summary>Disk Pool Exchange Name</summary>
		public string DiskPoolExchangeName { get; set; } = "";

		/// <summary>OSD Exchange Name</summary>
		public string OsdExchangeName { get; set; } = "";

		/// <summary>설정을 읽어 들인다.</summary>
		/// <param name="configContent">설정 내용</param>
		/// <returns>읽기 결과 응답 객체</returns>
		public override ResponseData Deserialize(string configContent)
		{
			ResponseData result = new ResponseData();
			StringBuilder config = new StringBuilder();

			try
			{
				using (StringReader reader = new StringReader(configContent))
				{
					string line;
					
					do
					{
						// 한줄을 읽어 들인다.
						line = reader.ReadLine();
					
						// 빈 문자열이 아닌 경우
						if (line != null && !line.IsEmpty())
						{
							// 빈 문자열인 경우, 스킵
							if (line.IsEmpty()) continue;

							// 좌우 공백을 없앤다.
							line = line.Trim();

							// 이름과 값으로 분리한다.
							string[] items = line.Split(new [] {' ', '='}, StringSplitOptions.RemoveEmptyEntries);

							// 항목이 부족한 경우, 스킵
							if (items.Length < 2) continue;
							
							// 메시지 큐 관련 설정
							if (items[0].StartsWith("mq."))
							{
								if (items[0] == "mq.host")
								{
									this.Host = items[1];
									config.AppendLine(line);
								}
								if (items[0] == "mq.port")
								{
									if (int.TryParse(items[1], out int port))
									{
										this.Port = port;
										config.AppendLine(line);
									}
								}
								if (items[0] == "mq.diskpool.queuename")
								{
									this.DiskPoolQueueName = items[1];
									config.AppendLine(line);
								}
								if (items[0] == "mq.diskpool.exchangename")
								{
									this.DiskPoolExchangeName = items[1];
									config.AppendLine(line);
								}
								if (items[0] == "mq.osd.exchangename")
								{
									this.OsdExchangeName = items[1];
									config.AppendLine(line);
								}
							}
						}
					} while (line != null);
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
	}
}