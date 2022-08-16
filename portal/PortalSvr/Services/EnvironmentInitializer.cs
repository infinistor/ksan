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
using Newtonsoft.Json.Linq;
using PortalResources;

namespace PortalSvr.Services
{
	/// <summary> 초기화 설정 </summary>
	public class EnvironmentInitializer
	{
		#region AppSettings
		/// <summary> AppSettings </summary>
		public static readonly string KEY_APP_SETTINGS = "AppSettings";
		/// <summary> RabbitMq </summary>
		public static readonly string KEY_RABBITMQ = "RabbitMq";
		/// <summary> AppSettings Host </summary>
		public static readonly string KEY_PORTAL_HOST = "Host";
		/// <summary> SSL 인증서 파일 경로 </summary>
		public static readonly string KEY_CERTIFICATE_FILE_PATH = "SharedAuthTicketKeyCertificateFilePath";
		/// <summary> SSL 인증서 비밀번호 </summary>
		public static readonly string KEY_CERTIFICATE_PASSWORD = "SharedAuthTicketKeyCertificatePassword";
		/// <summary> Database Type </summary>
		public static readonly string KEY_DATABASE_TYPE = "DatabaseType";
		/// <summary> Logging </summary>
		public static readonly string KEY_LOGGING = "Logging";
		/// <summary> LogLevel </summary>
		public static readonly string KEY_LOG_LEVEL = "LogLevel";
		/// <summary> Default </summary>
		public static readonly string KEY_LOG_DEFAULT = "Default";
		/// <summary> Microsoft </summary>
		public static readonly string KEY_LOG_MICROSOFT = "Microsoft";
		/// <summary> Microsoft.Hosting.Lifetime </summary>
		public static readonly string KEY_LOG_MICROSOFT_HOSTING_LIFETIME = "Microsoft.Hosting.Lifetime";
		/// <summary> PortalProvider </summary>
		public static readonly string KEY_LOG_PORTALPROVIDER = "PortalProvider";
		/// <summary> allowedHosts </summary>
		public static readonly string KEY_ALLOWED_HOSTS = "AllowedHosts";
		/// <summary> MariaDB </summary>
		public static readonly string KEY_MARIADB = "MariaDB";
		/// <summary> MongoDB </summary>
		public static readonly string KEY_MONGODB = "MongoDB";
		/// <summary> Host </summary>
		public static readonly string KEY_HOST = "Host";
		/// <summary> Name </summary>
		public static readonly string KEY_DB_NAME = "Name";
		/// <summary> Port </summary>
		public static readonly string KEY_PORT = "Port";
		/// <summary> User </summary>
		public static readonly string KEY_USER = "User";
		/// <summary> Password </summary>
		public static readonly string KEY_PASSWORD = "Password";
		/// <summary> LicenseKey </summary>
		public static readonly string KEY_DB_LICENSEKEY = "LicenseKey";
		#endregion
		#region KsanGW
		/// <summary> GW Repository </summary>
		public static readonly string KEY_GW_DB_REPOSITORY = "gw.db_repository";
		/// <summary> GW DB 호스트 </summary>
		public static readonly string KEY_GW_DB_HOST = "gw.db_host";
		/// <summary> GW DB 포트 </summary>
		public static readonly string KEY_GW_DB_PORT = "gw.dbPort";
		/// <summary> GW DB 명 </summary>
		public static readonly string KEY_GW_DB_NAME = "gw.db_name";
		/// <summary> GW DB 아이디 </summary>
		public static readonly string KEY_GW_DB_USER = "gw.db_user";
		/// <summary> GW DB 비밀번호 </summary>
		public static readonly string KEY_GW_DB_PASSWORD = "gw.db_password";
		#endregion
		#region Ksan Obj Manager
		/// <summary> Ksan Obj Manager Repository </summary>
		public static readonly string KEY_OBJ_DB_REPOSITORY = "objM.db_repository";
		/// <summary> Ksan Obj Manager Host </summary>
		public static readonly string KEY_OBJ_DB_HOST = "objM.db_host";
		/// <summary> Ksan Obj Manager Port </summary>
		public static readonly string KEY_OBJ_DB_PORT = "objM.db_port";
		/// <summary> Ksan Obj Manager Name </summary>
		public static readonly string KEY_OBJ_DB_NAME = "objM.db_name";
		/// <summary> Ksan Obj Manager User </summary>
		public static readonly string KEY_OBJ_DB_USER = "objM.db_user";
		/// <summary> Ksan Obj Manager Password </summary>
		public static readonly string KEY_OBJ_DB_PASSWORD = "objM.db_password";
		#endregion

		#region FilePath
		/// <summary> 포탈 설정 파일 경로 </summary>
		public const string PORTAL_SETTINGS_FILE = "appsettings.json";
		/// <summary> Ksan GW 설정 파일 경로 </summary>
		public const string KSAN_GW_SETTINGS_FILE = "Resources/ksangw.json";
		/// <summary> Ksan OSD 설정 파일 경로 </summary>
		public const string KSAN_OSD_SETTINGS_FILE = "Resources/ksanosd.json";
		#endregion

		/// <summary> 초기화 </summary>
		public void Initialize()
		{
			// Ksan API Portal의 기본 설정 정보를 읽어온다.
			string StrKsanApi = File.ReadAllText(PORTAL_SETTINGS_FILE);
			JObject KsanApi = JObject.Parse(StrKsanApi);
			if (KsanApi == null)
			{
				Console.WriteLine($"{PORTAL_SETTINGS_FILE} is Empty");
				return;
			}

			// KsanGw의 기본 설정 정보를 읽어온다.
			string StrKsanGw = File.ReadAllText(KSAN_GW_SETTINGS_FILE);
			JObject KsanGw = JObject.Parse(StrKsanGw);
			if (KsanGw == null)
			{
				Console.WriteLine($"{PORTAL_SETTINGS_FILE} is Empty");
				return;
			}

			// KsanOsd의 기본 설정 정보를 읽어온다.
			string StrKsanOsd = File.ReadAllText(KSAN_OSD_SETTINGS_FILE);
			JObject KsanOsd = JObject.Parse(StrKsanOsd);
			if (KsanOsd == null)
			{
				Console.WriteLine($"{PORTAL_SETTINGS_FILE} is Empty");
				return;
			}

			// 호스트 주소
			if (GetEnvValue(Resource.ENV_PORTAL_HOST, out string PortalHost))
				KsanApi[KEY_APP_SETTINGS][KEY_PORTAL_HOST] = PortalHost;

			// SSL 인증서 파일 경로
			if (GetEnvValue(Resource.ENV_CERTIFICATE_FILE_PATH, out string CertificateFilePath))
				KsanApi[KEY_APP_SETTINGS][KEY_CERTIFICATE_FILE_PATH] = CertificateFilePath;

			// SSL 인증서 비밀번호
			if (GetEnvValue(Resource.ENV_CERTIFICATE_PASSWORD, out string CertificatePassword))
				KsanApi[KEY_APP_SETTINGS][KEY_CERTIFICATE_PASSWORD] = CertificatePassword;

			if (GetEnvValue(Resource.ENV_ALLOWED_HOSTS, out string allowedHosts))
			{
				KsanApi[KEY_APP_SETTINGS][KEY_ALLOWED_HOSTS] = allowedHosts;
			}

			// RabbitMq 정보
			if (GetEnvValue(Resource.ENV_RABBITMQ_HOST, out string RabbitmqHost))
				KsanApi[KEY_APP_SETTINGS][KEY_RABBITMQ][KEY_HOST] = RabbitmqHost;

			if (GetEnvValue(Resource.ENV_RABBITMQ_PORT, out string RabbitmqPort))
				KsanApi[KEY_APP_SETTINGS][KEY_RABBITMQ][KEY_PORT] = RabbitmqPort;

			if (GetEnvValue(Resource.ENV_RABBITMQ_USER, out string RabbitmqUser))
				KsanApi[KEY_APP_SETTINGS][KEY_RABBITMQ][KEY_USER] = RabbitmqUser;

			if (GetEnvValue(Resource.ENV_RABBITMQ_PASSWORD, out string RabbitmqPassword))
				KsanApi[KEY_APP_SETTINGS][KEY_RABBITMQ][KEY_PASSWORD] = RabbitmqPassword;

			// 로그레벨
			if (GetEnvValue(Resource.ENV_LOG_LAVEL, out string LogLevel))
			{
				KsanApi[KEY_LOGGING][KEY_LOG_LEVEL][KEY_LOG_DEFAULT] = LogLevel;
				KsanApi[KEY_LOGGING][KEY_LOG_LEVEL][KEY_LOG_MICROSOFT] = LogLevel;
				KsanApi[KEY_LOGGING][KEY_LOG_LEVEL][KEY_LOG_MICROSOFT_HOSTING_LIFETIME] = LogLevel;
				KsanApi[KEY_LOGGING][KEY_LOG_LEVEL][KEY_LOG_PORTALPROVIDER] = LogLevel;
			}

			// Database
			if (GetEnvValue(Resource.ENV_DATABASE_TYPE, out string DatabaseType))
				KsanApi[KEY_APP_SETTINGS][KEY_DATABASE_TYPE] = DatabaseType;

			if (GetEnvValue(Resource.ENV_DATABASE, out string DatabaseName))
			{
				KsanGw[KEY_GW_DB_NAME] = DatabaseName;
				KsanOsd[KEY_OBJ_DB_NAME] = DatabaseName;
				KsanApi[KEY_MARIADB][KEY_DB_NAME] = DatabaseName;
				KsanApi[KEY_MONGODB][KEY_DB_NAME] = DatabaseName;
			}

			if (GetEnvValue(Resource.ENV_MARIADB_HOST, out string MariaDBHost))
				KsanApi[KEY_MARIADB][KEY_HOST] = MariaDBHost;

			if (GetEnvValue(Resource.ENV_MARIADB_PORT, out string MariaDBPort))
				KsanApi[KEY_MARIADB][KEY_PORT] = MariaDBPort;

			if (GetEnvValue(Resource.ENV_MARIADB_ROOT_USER, out string MariaDBUser))
				KsanApi[KEY_MARIADB][KEY_USER] = MariaDBUser;

			if (GetEnvValue(Resource.ENV_MARIADB_ROOT_PASSWORD, out string MariaDBPassword))
				KsanApi[KEY_MARIADB][KEY_PASSWORD] = MariaDBPassword;

			if (GetEnvValue(Resource.ENV_MARIADB_LICENSEKEY, out string MariaDBLicensekey))
				KsanApi[KEY_MARIADB][KEY_DB_LICENSEKEY] = MariaDBLicensekey;

			if (GetEnvValue(Resource.ENV_MONGODB_HOST, out string MongoDBHost))
				KsanApi[KEY_MONGODB][KEY_HOST] = MongoDBHost;

			if (GetEnvValue(Resource.ENV_MONGODB_PORT, out string MongoDBPort))
				KsanApi[KEY_MONGODB][KEY_PORT] = MongoDBPort;

			if (GetEnvValue(Resource.ENV_MONGODB_ROOT_USER, out string MongoDBUser))
				KsanApi[KEY_MONGODB][KEY_USER] = MongoDBUser;

			if (GetEnvValue(Resource.ENV_MONGODB_ROOT_PASSWORD, out string MongoDBPassword))
				KsanApi[KEY_MONGODB][KEY_PASSWORD] = MongoDBPassword;

			// Ksan obj Manager DB 설정
			if (!string.IsNullOrWhiteSpace(DatabaseType) && DatabaseType.Equals("MongoDB"))
			{
				KsanGw[KEY_OBJ_DB_REPOSITORY] = "MongoDB";
				KsanGw[KEY_OBJ_DB_HOST] = MongoDBHost;
				KsanGw[KEY_OBJ_DB_PORT] = MongoDBPort;
				KsanGw[KEY_OBJ_DB_NAME] = DatabaseName;
				KsanGw[KEY_OBJ_DB_USER] = MongoDBUser;
				KsanGw[KEY_OBJ_DB_PASSWORD] = MongoDBPassword;
			}
			else
			{
				KsanGw[KEY_OBJ_DB_REPOSITORY] = "MariaDB";
				KsanGw[KEY_OBJ_DB_HOST] = MariaDBHost;
				KsanGw[KEY_OBJ_DB_PORT] = MariaDBPort;
				KsanGw[KEY_OBJ_DB_NAME] = DatabaseName;
				KsanGw[KEY_OBJ_DB_USER] = MariaDBUser;
				KsanGw[KEY_OBJ_DB_PASSWORD] = MariaDBPassword;
			}

			// KsanGW DB 설정
			KsanGw[KEY_GW_DB_REPOSITORY] = "MariaDB";
			KsanGw[KEY_GW_DB_HOST] = MariaDBHost;
			KsanGw[KEY_GW_DB_PORT] = MariaDBPort;
			KsanGw[KEY_GW_DB_NAME] = DatabaseName;
			KsanGw[KEY_GW_DB_USER] = MariaDBUser;
			KsanGw[KEY_GW_DB_PASSWORD] = MariaDBPassword;

			if (!string.IsNullOrWhiteSpace(DatabaseType))
			{
				File.WriteAllText(PORTAL_SETTINGS_FILE, KsanApi.ToString());
				File.WriteAllText(KSAN_GW_SETTINGS_FILE, KsanGw.ToString());
				File.WriteAllText(KSAN_OSD_SETTINGS_FILE, KsanOsd.ToString());
			}
		}

		private bool GetEnvValue(string Key, out string Value)
		{
			Value = "";
			try
			{
				Value = Environment.GetEnvironmentVariable(Key);
				return true;
			}
			catch
			{
				return false;
			}
		}
	}
}