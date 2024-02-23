/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License.See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Resources;
using System.Threading.Tasks;
using PortalModels;
using PortalProvider.Loaders;
using PortalProvider.Logs;
using PortalProvider.Providers.Accounts;
using PortalProvider.Providers.DiskGuids;
using PortalProvider.Providers.Networks;
using PortalProvider.Providers.RabbitMQ;
using PortalProvider.Providers.Servers;
using PortalProvider.Providers.Services;
using PortalProvider.Providers.S3;
using PortalProviderInterface;
using PortalResources;
using PortalSvr.RabbitMQReceivers;
using PortalSvr.Services;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Localization;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Net.Http.Headers;
using Microsoft.OpenApi.Models;
using MTLib.AspNetCore;
using MTLib.Core;
using MTLib.EntityFramework;
using Swashbuckle.AspNetCore.SwaggerUI;
using PortalProvider.Providers.DB;

namespace PortalSvr
{
	/// <summary>시작 클래스</summary>
	public class Startup
	{
		/// <summary>설정 객체</summary>
		public IConfiguration Configuration { get; }

		/// <summary>설정 옵션 객체</summary>
		public NNConfigurationOptions ConfigurationOptions { get; }

		/// <summary>서비스 프로바이더 싱글톤 객체</summary>
		public static System.IServiceProvider ServiceProviderSignleton { get; set; }

		/// <summary>생성자</summary>
		/// <param name="env">호스팅 환경 객체</param>
		/// <param name="configuration">환경 설정 객체</param>
		public Startup(IWebHostEnvironment env, IConfiguration configuration)
		{
			try
			{
				Configuration = configuration;

				// 기본 설정 옵션 생성
				ConfigurationOptions = new NNConfigurationOptions(env: env,
					applicationName: "CSSPApi",
					domain: Configuration["AppSettings:Domain"],
					sharedAuthTicketKeyPath: Configuration["AppSettings:SharedAuthTicketKeyPath"],
					sharedAuthTicketKeyCertificateFilePath: Configuration["AppSettings:SharedAuthTicketKeyCertificateFilePath"],
					sharedAuthTicketKeyCertificatePassword: Configuration["AppSettings:SharedAuthTicketKeyCertificatePassword"],
					sessionTimeoutMins: Configuration.GetValue<int>("AppSettings:ExpireMinutes")
				);

				IList<CultureInfo> supportedCultures = new[] { new CultureInfo("en"), new CultureInfo("ko") };

				ConfigurationOptions.Localization.DefaultRequestCulture = new RequestCulture("ko");
				ConfigurationOptions.Localization.SupportedCultures = supportedCultures;
				ConfigurationOptions.Localization.SupportedUICultures = supportedCultures;

				ConfigurationOptions.Identity.User.RequireUniqueEmail = true;
				ConfigurationOptions.Identity.Password.RequireUppercase = false;
				ConfigurationOptions.Identity.Password.RequireNonAlphanumeric = false;
				ConfigurationOptions.Identity.Password.RequireDigit = true;
				ConfigurationOptions.CookieAuthentication.LoginPath = new PathString("/api/v1/Account/NeedLogin");
				ConfigurationOptions.CookieAuthentication.LogoutPath = new PathString("/api/v1/Account/Logout");
				ConfigurationOptions.CookieAuthentication.AccessDeniedPath = new PathString("/api/v1/Account/AccessDenied");
				ConfigurationOptions.CookieAuthentication.Events.OnRedirectToLogin = (context) =>
				{
					context.HttpContext.Response.Redirect(context.RedirectUri.Replace("http://", "https://"));
					return Task.CompletedTask;
				};
				ConfigurationOptions.CookieAuthentication.Events.OnRedirectToAccessDenied = (context) =>
				{
					context.HttpContext.Response.Redirect(context.RedirectUri.Replace("http://", "https://"));
					return Task.CompletedTask;
				};
				ConfigurationOptions.LoggingSectionName = "Logging";
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}

		/// <summary>컨테이너에 서비스들을 추가한다.</summary>
		/// <param name="Services">서비스 집합 객체</param>
		public void ConfigureServices(IServiceCollection Services)
		{
			try
			{
				// MariaDB 설정
				IConfigurationSection configurationSectionMariaDB = Configuration.GetSection(Resource.ENV_DATABASE_TYPE_MARIA_DB);
				MariaDBConfiguration mariaDBConfiguration = configurationSectionMariaDB.Get<MariaDBConfiguration>();
				var ConnectionMariaDBString = mariaDBConfiguration.GetConnectionMariaDBString();

				// 데이터베이스 연결 설정
				Services.AddDbContext<PortalModel>(Options => Options.UseMySql(ConnectionMariaDBString, MySqlServerVersion.LatestSupportedServerVersion));

				// 사용자 인증 관련 데이터베이스 연결 설정
				Services.AddDbContext<ApplicationIdentityDbContext>(Options => Options.UseMySql(ConnectionMariaDBString, MySqlServerVersion.LatestSupportedServerVersion));

				// 컨테이너에 기본 서비스들을 추가한다.
				Services.ConfigureServices(true, ConfigurationOptions);

				// 업로드 설정 로더 변경
				Services.ReplaceTransient<IUploadConfigLoader, UploadConfigLoader>();
				// Smtp 설정 로더 변경
				Services.ReplaceTransient<ISmtpConfigLoader, SmtpConfigLoader>();

				// 프로바이더 객체 DI 설정
				Services.AddSingleton<ISystemConfigLoader, SystemConfigLoader>();
				Services.AddSingleton<IAllowConnectionIpsManager, AllowConnectionIpsManager>();
				Services.AddScoped<IUserClaimsPrincipalFactory<NNApplicationUser>, ApiKeyClaimsPrincipalFactory>();
				Services.AddTransient<IAccountInitializer, AccountInitializer>();
				Services.AddTransient<IRoleInitializer, RoleInitializer>();
				Services.AddTransient<IConfigurationInitializer, ConfigurationInitializer>();
				Services.AddTransient<IRoleProvider, RoleProvider>();
				Services.AddTransient<IUserProvider, UserProvider>();
				Services.AddTransient<IAccountProvider, AccountProvider>();
				Services.AddTransient<LogProvider, LogProvider>();
				Services.AddTransient<IUserActionLogProvider, UserActionLogProvider>();
				Services.AddTransient<IApiKeyProvider, ApiKeyProvider>();
				Services.AddTransient<IRabbitMQSender, RabbitMQSender>();
				Services.AddTransient<IRabbitMQRpc, RabbitMQRpc>();
				Services.AddTransient<IServerProvider, ServerProvider>();
				Services.AddTransient<INetworkInterfaceProvider, NetworkInterfaceProvider>();
				Services.AddTransient<INetworkInterfaceVlanProvider, NetworkInterfaceVlanProvider>();
				Services.AddTransient<IDiskProvider, DiskProvider>();
				Services.AddTransient<IConfigProvider, ConfigProvider>();
				Services.AddTransient<IDiskPoolProvider, DiskPoolProvider>();
				Services.AddTransient<PortalProviderInterface.IServiceProvider, PortalProvider.Providers.Services.ServiceProvider>();
				Services.AddTransient<IServiceGroupProvider, ServiceGroupProvider>();
				Services.AddTransient<IKsanUserProvider, KsanUserProvider>();
				Services.AddTransient<IRegionProvider, RegionProvider>();
				Services.AddTransient<ISystemLogProvider, SystemLogProvider>();
				Services.AddTransient<IServerWatcher, ServerWatcher>();
				Services.AddTransient<IServerInitializer, ServerInitializer>();
				Services.AddTransient<IS3LogProvider, S3LogProvider>();
				Services.AddTransient<IS3Provider, S3Provider>();

				// 서버 감시
				Services.AddHostedService<ServerWatcher>();
				// 최초 기동시 서버 및 서비스를 등록 한다.
				Services.AddHostedService<ServerInitializer>();

				Services.AddSwaggerGen(c =>
				{
					c.SwaggerDoc("v1", new OpenApiInfo { Title = "KSAN", Version = "v1" });
					c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
					{
						Description = $@"Bearer 체계를 사용하는 JWT Authorization 헤더.<br/>'Bearer'[공백]을 입력 한 다음 아래 텍스트 입력에 토큰을 입력하십시오.",
						Name = "Authorization",
						In = ParameterLocation.Header,
						Type = SecuritySchemeType.ApiKey,
						Scheme = "Bearer"
					});
					c.AddSecurityRequirement(new OpenApiSecurityRequirement()
					{
						{
							new OpenApiSecurityScheme
							{
								Reference = new OpenApiReference
								{
									Type = ReferenceType.SecurityScheme,
									Id = "Bearer"
								},
								Scheme = "oauth2",
								Name = "Bearer",
								In = ParameterLocation.Header,

							},
							new List<string>()
						}
					});

					// Swagger JSON and UI에서 사용할 코멘트 경로를 설정한다.
					c.IncludeXmlComments(Path.Combine(AppContext.BaseDirectory, "PortalSvr.xml"));
					c.IncludeXmlComments(Path.Combine(AppContext.BaseDirectory, "PortalData.xml"));
				});
				Services.AddSwaggerGenNewtonsoftSupport();

				// 서비스 프로바이더 저장
				ServiceProviderSignleton = Services.BuildServiceProvider();

				// Rabbit MQ 설정
				IConfigurationSection configurationSectionRabbitMQ = Configuration.GetSection("AppSettings:RabbitMQ");
				RabbitMQConfiguration rabbitMqConfiguration = configurationSectionRabbitMQ.Get<RabbitMQConfiguration>();
				Services.Configure<RabbitMQConfiguration>(configurationSectionRabbitMQ);

				// Rabbit MQ
				if (rabbitMqConfiguration.Enabled)
				{
					Services.AddHostedService<RabbitMQServerReceiver>();
					Services.AddHostedService<RabbitMQServiceReceiver>();
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}

		/// <summary>HTTP 요청 파이프 라인을 구성한다.</summary>
		/// <param name="app">어플리케이션 빌더 객체</param>
		/// <param name="env">호스팅 환경 객체</param>
		/// <param name="loggerFactory">로거 팩토리</param>
		/// <param name="pathProvider">경로 도우미 객체</param>
		/// <param name="identityDbContext">인증 관련 DB 컨텍스트</param>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="roleInitializer">역할 초기화 객체</param>
		/// <param name="accountInitializer">계정 초기화 객체</param>
		/// <param name="configurationInitializer">설정 초기화 객체</param>
		/// <param name="serverInitializer">서버 초기화 객체</param>
		/// <param name="allowAddressManager">허용된 주소 검사 관리자 객체</param>
		/// <param name="systemConfigLoader">시스템 환경 설정 로더</param>
		/// <param name="smtpConfigLoader">SMTP 설정 로더</param>
		/// <param name="uploadConfigLoader">업로드 설정 로더</param>
		public void Configure(IApplicationBuilder app, IWebHostEnvironment env, ILoggerFactory loggerFactory, IPathProvider pathProvider
			, ApplicationIdentityDbContext identityDbContext, PortalModel dbContext
			, IRoleInitializer roleInitializer, IAccountInitializer accountInitializer
			, IConfigurationInitializer configurationInitializer, IServerInitializer serverInitializer
			, IAllowConnectionIpsManager allowAddressManager, ISystemConfigLoader systemConfigLoader
			, ISmtpConfigLoader smtpConfigLoader, IUploadConfigLoader uploadConfigLoader
		)
		{
			try
			{
				// 생성 된 Swagger를 JSON 끝점으로 제공 할 수 있게 미들웨어를 활성화한다.
				app.UseSwagger();

				// swagger-ui 제공을 위해 Swagger JSON 끝점을 명시하여 미들웨어를 활성화한다.
				app.UseSwaggerUI(c =>
				{
					c.SwaggerEndpoint("/swagger/v1/swagger.json", "PortalSvr v1");
					c.RoutePrefix = "api";
					c.DocExpansion(DocExpansion.None);
				});

				// 개발 환경인 경우
				if (env.IsDevelopment())
				{
					// 개발자 Exception 페이지 사용
					app.UseDeveloperExceptionPage();
				}
				// 운영 환경인 경우
				else
				{
					app.UseHsts();
				}

				// 정적 파일 사용
				app.UseStaticFiles(new StaticFileOptions
				{
					OnPrepareResponse = ctx =>
					{
						const int durationInSeconds = 60 * 60 * 24;
						ctx.Context.Response.Headers[HeaderNames.CacheControl] = "public,max-age=" + durationInSeconds;
					}
				});

				// 역방향 프록시 세팅 (For Nginx : Nginx로 헤더 및 프로토콜 전달)
				app.UseForwardedHeaders(new ForwardedHeadersOptions { ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto });

				// 마이그레이션 수행
				dbContext.Migrate();

				// 기본 설정을 처리한다. (요청에 대한 접근 아이피 검사 미들웨어 추가, Access Log 미들웨어 추가)
				app.Configure(env, loggerFactory, Configuration, ConfigurationOptions, new List<Type>() { typeof(AllowConnectionIpCheckMiddleware), typeof(AccessLogMiddleware) });

				// 업로드 설정을 가져온다.
				uploadConfigLoader.GetConfig();

				// 데이터를 초기화 한다.
				roleInitializer?.Initialize().Wait();
				accountInitializer?.Initialize().Wait();

				// 최초 기동시 설정을 등록 한다.
				configurationInitializer?.Initialize().Wait();

				// 환경 설정을 초기화 및 로드 한다.
				ConfigInitializeAndLoad(dbContext, systemConfigLoader, smtpConfigLoader, uploadConfigLoader);

				// 허용된 아이피 목록을 로드한다.
				allowAddressManager.LoadAllowedConnectionIps(dbContext);

				// 문자열 리소스 경로가 존재하는 경우
				if (!Configuration["AppSettings:i18nPath"].IsEmpty())
				{
					// 모든 리소스 매니저를 리스트로 생성
					var ResourceManagers = new List<ResourceManager>()
					{
						Resource.ResourceManager
					};

					string i18nRootPath = $"{Configuration["AppSettings:I18nPath"].Replace('/', Path.DirectorySeparatorChar)}";

					// 모든 언어에 대해서 리소스 파일 내용을 Json으로 저장
					foreach (var culture in ConfigurationOptions.Localization.SupportedUICultures)
						ResourceManagers.Save(culture, Path.Combine(i18nRootPath, string.Format("{0}.json", culture.Name)));
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}

		/// <summary>환경 설정을 초기화 및 로드 한다.</summary>
		/// <param name="dbContext">MSquare DB 컨텍스트</param>
		/// <param name="systemConfigLoader">시스템 환경 설정 로더</param>
		/// <param name="smtpConfigLoader">SMTP 설정 로더</param>
		/// <param name="uploadConfigLoader">업로드 설정 로더</param>
		private void ConfigInitializeAndLoad(PortalModel dbContext, ISystemConfigLoader systemConfigLoader
			, ISmtpConfigLoader smtpConfigLoader, IUploadConfigLoader uploadConfigLoader)
		{
			bool configChanged = false;

			try
			{
				// 모든 설정 로드
				systemConfigLoader.Load(dbContext);

				// SMTP 설정 관련 초기화할 목록을 가져온다.
				var smtpConfigValues = smtpConfigLoader.GetListForInitialization();
				// SMTP 설정 관련 초기화할 항목이 존재하는 경우
				if (smtpConfigValues != null && smtpConfigValues.Count > 0)
				{
					// 항목 추가
					foreach (var keyValue in smtpConfigValues)
						dbContext.Configs.Add(new Config() { Key = keyValue.Key, Value = keyValue.Value });
					dbContext.SaveChangesWithConcurrencyResolution();
					configChanged = true;
				}

				// 업로드 설정 관련 초기화할 목록을 가져온다.
				var uploadConfigValues = uploadConfigLoader.GetListForInitialization();

				// 업로드 설정 관련 초기화할 항목이 존재하는 경우
				if (uploadConfigValues != null && uploadConfigValues.Count > 0)
				{
					// 항목 추가
					foreach (var keyValue in uploadConfigValues)
						dbContext.Configs.Add(new Config() { Key = keyValue.Key, Value = keyValue.Value });
					dbContext.SaveChangesWithConcurrencyResolution();
					configChanged = true;
				}

				// 설정이 변경된 경우, 모든 설정 로드
				if (configChanged)
					systemConfigLoader.Load(dbContext);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}
	}
}
