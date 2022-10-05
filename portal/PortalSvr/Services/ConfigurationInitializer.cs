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
using PortalProvider.Providers.RabbitMQ;
using PortalProvider.Providers.DB;
using Newtonsoft.Json;
using System.IO;
using System.Collections.Generic;

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

		/// <summary> Api Key 프로바이더</summary>
		protected readonly IApiKeyProvider m_apiKeyProvider;

		/// <summary>설정 정보</summary>
		protected readonly IConfiguration m_configuration;

		/// <summary>로거</summary>
		private readonly ILogger m_logger;

		/// <summary> KsanConfig </summary>
		public static string KsanConfig = "/usr/local/ksan/etc/ksanAgent.conf";

		/// <summary>생성자</summary>
		/// <param name="configProvider">설정에 대한 프로바이더 객체</param>
		/// <param name="apiKeyProvider">API Key에 대한 프로바이더 객체</param>
		/// <param name="configuration">설정 정보</param>
		/// <param name="logger">로거</param>
		public ConfigurationInitializer(
			IConfigProvider configProvider,
			IApiKeyProvider apiKeyProvider,
			IConfiguration configuration,
			ILogger<ConfigurationInitializer> logger
		)
		{
			m_configProvider = configProvider;
			m_apiKeyProvider = apiKeyProvider;
			m_configuration = configuration;
			m_logger = logger;
		}

		/// <summary>설정이 없는 경우, 기본 설정을 생성한다.</summary>
		public async Task Initialize()
		{
			try
			{
				// RabbitMQ 설정이 없는 경우
				var RabbitMQConfig = await m_configProvider.GetConfig(EnumServiceType.RabbitMQ);
				if (RabbitMQConfig == null || RabbitMQConfig.Result == EnumResponseResult.Error)
				{
					// RabbitMQ 설정
					IConfigurationSection Section = m_configuration.GetSection("AppSettings:RabbitMQ");
					RabbitMQConfiguration Configuration = Section.Get<RabbitMQConfiguration>();

					var Result = await m_configProvider.SetConfig(EnumServiceType.RabbitMQ, JsonConvert.SerializeObject(Configuration));
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.RabbitMQ, Result.Data.Version);
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

				// MongoDB 설정이 없는 경우
				var MongoDBConfig = await m_configProvider.GetConfig(EnumServiceType.MongoDB);
				if (MongoDBConfig == null || MongoDBConfig.Result == EnumResponseResult.Error)
				{
					// MongoDB 설정
					IConfigurationSection Section = m_configuration.GetSection("MongoDB");
					MongoDBConfiguration Configuration = Section.Get<MongoDBConfiguration>();

					var Result = await m_configProvider.SetConfig(EnumServiceType.MongoDB, JsonConvert.SerializeObject(Configuration));
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.MongoDB, Result.Data.Version);
				}

				// KsanGW 설정이 없는 경우
				var KsanGWConfig = await m_configProvider.GetConfig(EnumServiceType.ksanGW);
				if (KsanGWConfig == null || KsanGWConfig.Result == EnumResponseResult.Error)
				{
					// KsanGW의 기본 설정 정보를 읽어온다.
					string StrKsanGW = File.ReadAllText(EnvironmentInitializer.KSAN_GW_SETTINGS_FILE);

					var Result = await m_configProvider.SetConfig(EnumServiceType.ksanGW, StrKsanGW);
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.ksanGW, Result.Data.Version);
				}

				// KsanOSD 설정이 없는 경우
				var KsanOSDConfig = await m_configProvider.GetConfig(EnumServiceType.ksanOSD);
				if (KsanOSDConfig == null || KsanOSDConfig.Result == EnumResponseResult.Error)
				{
					// Ksan Gw의 기본 설정 정보를 읽어온다.
					string StrKsanOSD = File.ReadAllText(EnvironmentInitializer.KSAN_OSD_SETTINGS_FILE);

					var Result = await m_configProvider.SetConfig(EnumServiceType.ksanOSD, StrKsanOSD);
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.ksanOSD, Result.Data.Version);
				}

				// KsanLifecycle 설정이 없는 경우
				var KsanLifecycleConfig = await m_configProvider.GetConfig(EnumServiceType.ksanLifecycle);
				if (KsanLifecycleConfig == null || KsanLifecycleConfig.Result == EnumResponseResult.Error)
				{
					// Ksan Gw의 기본 설정 정보를 읽어온다.
					string StrKsanLifecycle = File.ReadAllText(EnvironmentInitializer.KSAN_LIFECYCLE_SETTINGS_FILE);

					var Result = await m_configProvider.SetConfig(EnumServiceType.ksanLifecycle, StrKsanLifecycle);
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.ksanLifecycle, Result.Data.Version);
				}

				// KsanLogManager 설정이 없는 경우
				var KsanLogManagerConfig = await m_configProvider.GetConfig(EnumServiceType.ksanLogManager);
				if (KsanLogManagerConfig == null || KsanLogManagerConfig.Result == EnumResponseResult.Error)
				{
					// Ksan Gw의 기본 설정 정보를 읽어온다.
					string StrKsanLogManager = File.ReadAllText(EnvironmentInitializer.KSAN_LOGMANAGER_SETTINGS_FILE);

					var Result = await m_configProvider.SetConfig(EnumServiceType.ksanLogManager, StrKsanLogManager);
					if (Result != null && Result.Result == EnumResponseResult.Success) await m_configProvider.SetConfigLastVersion(EnumServiceType.ksanLogManager, Result.Data.Version);
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
	}
}
