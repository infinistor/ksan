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
using System.Net;
using System.Security.Authentication;
using System.Security.Cryptography.X509Certificates;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Server.Kestrel.Https;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using MTLib.Core;
using PortalSvr.Services;

namespace PortalSvr
{
	/// <summary>프로그램 클래스</summary>
	public class Program
	{
		/// <summary>메인 함수</summary>
		/// <param name="args">인수 목록</param>
		public static void Main(string[] args)
		{
			try
			{
				// 최초 실행시 환경변수를 통한 설정 정보 수정
				var Ini = new EnvironmentInitializer();
				Ini.Initialize();

				CreateHostBuilder(args).Build().Run();
			}
			catch (Exception ex)
			{
				Console.WriteLine(ex);
				NNException.Log(ex);
			}
		}

		/// <summary>호스트 빌더를 생성한다.</summary>
		/// <param name="args">인자 목록</param>
		/// <returns>IHostBuilder 객체</returns>
		public static IHostBuilder CreateHostBuilder(string[] args) =>
			Host.CreateDefaultBuilder(args)
				.ConfigureWebHostDefaults(webBuilder =>
				{
					webBuilder.ConfigureKestrel((hostContext, serverOptions) =>
						{
							IConfiguration configuration = hostContext.Configuration;

							serverOptions.Limits.MaxRequestBodySize = 1024 * 1024 * 1024;

							serverOptions.Listen(IPAddress.Any, 56080);
							try
							{
								serverOptions.Listen(IPAddress.Any, 56443, listenOptions =>
								{
									listenOptions.UseHttps(new HttpsConnectionAdapterOptions
									{
										ServerCertificate = new X509Certificate2(configuration["AppSettings:SharedAuthTicketKeyCertificateFilePath"], configuration["AppSettings:SharedAuthTicketKeyCertificatePassword"]),
										SslProtocols = SslProtocols.Tls12 | SslProtocols.Tls11 | SslProtocols.Tls
									});
								});
							}
							catch (Exception ex)
							{
								NNException.Log(ex);
							}
						})
						.UseStartup<Startup>();
				});
	}
}
