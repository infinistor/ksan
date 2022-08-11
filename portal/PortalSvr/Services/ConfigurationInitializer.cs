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
using System.Threading.Tasks;
using PortalData.Enums;
using PortalProviderInterface;
using Microsoft.Extensions.Logging;
using MTLib.CommonData;
using MTLib.Core;
using Microsoft.Extensions.Configuration;
using PortalProvider.Providers.RabbitMq;
using PortalProvider.Providers.DB;
using Newtonsoft.Json;
using System.IO;

namespace PortalSvr.Services
{
	/// <summary>설정 초기화 인터페이스</summary>
	public interface IConfigurationInitializer
	{
		/// <summary>설정이 없는 경우, 기본 설정을 생성한다.</summary>
		Task Initialize();
	}

	/// <summary>설정 초기화 클래스</summary>
	public class ConfigurationInitializer : IConfigurationInitializer
	{
		/// <summary>설정에 대한 프로바이더</summary>
		private readonly IConfigProvider m_configProvider;

		/// <summary>설정 정보</summary>
		protected readonly IConfiguration m_configuration;

		/// <summary>로거</summary>
		private readonly ILogger m_logger;

		/// <summary>생성자</summary>
		/// <param name="configProvider">설정에 대한 프로바이더 객체</param>
		/// <param name="configuration">설정 정보</param>
		/// <param name="logger">로거</param>
		public ConfigurationInitializer(
			IConfigProvider configProvider,
			IConfiguration configuration,
			ILogger<ConfigurationInitializer> logger
		)
		{
			m_configProvider = configProvider;
			m_configuration = configuration;
			m_logger = logger;
		}

		/// <summary>설정이 없는 경우, 기본 설정을 생성한다.</summary>
		public async Task Initialize()
		{
			try
			{
				// RabbitMq 설정이 없는 경우
				var RabbitMqConfig = await m_configProvider.GetConfig(EnumServiceType.RabbitMq);
				if (RabbitMqConfig == null || RabbitMqConfig.Result == EnumResponseResult.Error)
				{
					// RabbitMq 설정
					IConfigurationSection Section = m_configuration.GetSection("AppSettings:RabbitMq");
					RabbitMqConfiguration Configuration = Section.Get<RabbitMqConfiguration>();

					var Result = await m_configProvider.SetConfig(EnumServiceType.RabbitMq, JsonConvert.SerializeObject(Configuration));
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.RabbitMq, Result.Data.Version);
				}

				// MariaDB 설정이 없는 경우
				var MariaDBConfig = await m_configProvider.GetConfig(EnumServiceType.MariaDB);
				if (MariaDBConfig == null || MariaDBConfig.Result == EnumResponseResult.Error)
				{
					// MariaDB 설정
					IConfigurationSection Section = m_configuration.GetSection("MariaDB");
					MariaDBConfiguration Configuration = Section.Get<MariaDBConfiguration>();

					var Result = await m_configProvider.SetConfig(EnumServiceType.MariaDB, JsonConvert.SerializeObject(Configuration));
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.MariaDB, Result.Data.Version);
				}

				// MariaDB 설정이 없는 경우
				var MongoDBConfig = await m_configProvider.GetConfig(EnumServiceType.MongoDB);
				if (MongoDBConfig == null || MongoDBConfig.Result == EnumResponseResult.Error)
				{
					// MariaDB 설정
					IConfigurationSection Section = m_configuration.GetSection("MongoDB");
					MongoDBConfiguration Configuration = Section.Get<MongoDBConfiguration>();

					var Result = await m_configProvider.SetConfig(EnumServiceType.MongoDB, JsonConvert.SerializeObject(Configuration));
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.MongoDB, Result.Data.Version);
				}

				// KsanGw 설정이 없는 경우
				var KsanGwConfig = await m_configProvider.GetConfig(EnumServiceType.KsanGw);
				if (KsanGwConfig == null || KsanGwConfig.Result == EnumResponseResult.Error)
				{
					// KsanGw의 기본 설정 정보를 읽어온다.
					string StrKsanGw = File.ReadAllText("Resources/ksangw.json");

					var Result = await m_configProvider.SetConfig(EnumServiceType.KsanGw, StrKsanGw);
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.KsanGw, Result.Data.Version);
				}

				// KsanOsd 설정이 없는 경우
				var KsanOsdConfig = await m_configProvider.GetConfig(EnumServiceType.KsanOsd);
				if (KsanOsdConfig == null || KsanOsdConfig.Result == EnumResponseResult.Error)
				{
					// Ksan Gw의 기본 설정 정보를 읽어온다.
					string StrKsanOsd = File.ReadAllText("Resources/ksanosd.json");

					var Result = await m_configProvider.SetConfig(EnumServiceType.KsanOsd, StrKsanOsd);
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.KsanOsd, Result.Data.Version);
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
	}
}
