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
using System.Net;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalProviderInterface;
using PortalSvr.Services;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using MTLib.AspNetCore;
using Swashbuckle.AspNetCore.Annotations;
using PortalData.Responses.Configs;
using PortalData.Requests.Configs;
using Microsoft.Extensions.Logging;

namespace PortalSvr.Controllers.Config
{
	/// <summary>설정 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class ConfigController : BaseController
	{
		/// <summary>설정 데이터 프로바이더</summary>
		private readonly IConfigProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">설정 데이터 프로바이더</param>
		public ConfigController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<ConfigController> logger,
			IConfigProvider dataProvider
		)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		#region All Config
		/// <summary>설정 목록을 가져온다.</summary>
		/// <param name="Request">서비스 설정 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List")]
		public async Task<ActionResult> GetConfigList([FromBody] RequestServiceConfig Request)
		{
			return Json(await m_dataProvider.GetConfigList(Request.Type));
		}

		/// <summary>설정을 가져온다.</summary>
		/// <param name="Request">서비스 설정 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet()]
		public async Task<ActionResult> GetConfig([FromBody] RequestServiceConfig Request)
		{
			return Json(await m_dataProvider.GetConfig(Request.Type));
		}

		/// <summary>특정 버전의 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("{Version}")]
		public async Task<ActionResult> GetConfig([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanOsd, Version));
		}

		/// <summary>설정을 저장한다.</summary>
		/// <param name="Request">서비스 설정 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost()]
		public async Task<ActionResult> SetConfig([FromBody] RequestServiceConfig Request)
		{
			return Json(await m_dataProvider.SetConfig(Request));
		}
		#endregion

		#region RabbitMq
		/// <summary>RabbitMq 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("RabbitMq")]
		public async Task<ActionResult> GetConfigForRabbitMq()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.RabbitMq));
		}
		#endregion
		#region MariaDB
		/// <summary>MariaDB 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("MariaDB")]
		public async Task<ActionResult> GetConfigForMariaDB()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.MariaDB));
		}
		#endregion
		#region MongoDB
		/// <summary>MongoDB 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("MongoDB")]
		public async Task<ActionResult> GetConfigForMongoDB()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.MongoDB));
		}
		#endregion
		#region KsanOsd
		/// <summary>KsanOsd 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanOsd")]
		public async Task<ActionResult> GetConfigListForKsanOsd()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.KsanOsd));
		}

		/// <summary>KsanOsd 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanOsd")]
		public async Task<ActionResult> GetConfigForKsanOsd()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanOsd));
		}
		/// <summary>특정 버전의 KsanOsd 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanOsd/{Version}")]
		public async Task<ActionResult> GetConfigForKsanOsd([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanOsd, Version));
		}

		/// <summary>KsanOsd 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanOsd")]
		public async Task<ActionResult> SetConfigForKsanOsd([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.KsanOsd, Config = Config }));
		}

		/// <summary>KsanOsd 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanOsd/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanOsd([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.KsanOsd, Version));
		}

		/// <summary>KsanOsd 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanOsd/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanOsd([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.KsanOsd, Version));
		}
		#endregion
		#region KsanGw
		/// <summary>KsanGw 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanGw")]
		public async Task<ActionResult> GetConfigListForKsanGw()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.KsanGw));
		}

		/// <summary>KsanGw 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanGw")]
		public async Task<ActionResult> GetConfigForKsanGw()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanGw));
		}

		/// <summary>특정 버전의 KsanGw 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanGw/{Version}")]
		public async Task<ActionResult> GetConfigForKsanGw([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanGw, Version));
		}

		/// <summary>KsanGw 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanGw")]
		public async Task<ActionResult> SetConfigForKsanGw([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.KsanGw, Config = Config }));
		}

		/// <summary>KsanGw 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanGw/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanGw([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.KsanGw, Version));
		}

		/// <summary>KsanGw 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanGw/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanGw([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.KsanGw, Version));
		}
		#endregion
		#region KsanRecovery
		/// <summary>KsanRecovery 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanRecovery")]
		public async Task<ActionResult> GetConfigListForKsanRecovery()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.KsanRecovery));
		}

		/// <summary>KsanRecovery 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanRecovery")]
		public async Task<ActionResult> GetConfigForKsanRecovery()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanRecovery));
		}

		/// <summary>특정 버전의 KsanRecovery 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanRecovery/{Version}")]
		public async Task<ActionResult> GetConfigForKsanRecovery([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanRecovery, Version));
		}

		/// <summary>KsanRecovery 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanRecovery")]
		public async Task<ActionResult> SetConfigForKsanRecovery([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.KsanRecovery, Config = Config }));
		}

		/// <summary>KsanRecovery 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanRecovery/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanRecovery([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.KsanRecovery, Version));
		}

		/// <summary>KsanRecovery 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanRecovery/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanRecovery([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.KsanRecovery, Version));
		}
		#endregion
		#region KsanLifecycle
		/// <summary>KsanLifecycle 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanLifecycle")]
		public async Task<ActionResult> GetConfigListForKsanLifecycle()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.KsanLifecycle));
		}

		/// <summary>KsanLifecycle 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanLifecycle")]
		public async Task<ActionResult> GetConfigForKsanLifecycle()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanLifecycle));
		}

		/// <summary>특정 버전의 KsanLifecycle 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanLifecycle/{Version}")]
		public async Task<ActionResult> GetConfigForKsanLifecycle([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanLifecycle, Version));
		}

		/// <summary>KsanLifecycle 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanLifecycle")]
		public async Task<ActionResult> SetConfigForKsanLifecycle([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.KsanLifecycle, Config = Config }));
		}

		/// <summary>KsanLifecycle 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanLifecycle/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanLifecycle([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.KsanLifecycle, Version));
		}

		/// <summary>KsanLifecycle 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanLifecycle/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanLifecycle([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.KsanLifecycle, Version));
		}
		#endregion
		#region KsanReplication
		/// <summary>KsanReplication 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanReplication")]
		public async Task<ActionResult> GetConfigListForKsanReplication()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.KsanReplication));
		}

		/// <summary>KsanReplication 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanReplication")]
		public async Task<ActionResult> GetConfigForKsanReplication()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanReplication));
		}

		/// <summary>특정 버전의 KsanReplication 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanReplication/{Version}")]
		public async Task<ActionResult> GetConfigForKsanReplication([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanReplication, Version));
		}

		/// <summary>KsanReplication 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanReplication")]
		public async Task<ActionResult> SetConfigForKsanReplication([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.KsanReplication, Config = Config }));
		}

		/// <summary>KsanReplication 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanReplication/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanReplication([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.KsanReplication, Version));
		}

		/// <summary>KsanReplication 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanReplication/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanReplication([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.KsanReplication, Version));
		}
		#endregion
		#region KsanLogExport
		/// <summary>KsanLogExport 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanLogExport")]
		public async Task<ActionResult> GetConfigListForKsanLogExport()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.KsanLogExport));
		}

		/// <summary>KsanLogExport 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanLogExport")]
		public async Task<ActionResult> GetConfigForKsanLogExport()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanLogExport));
		}

		/// <summary>특정 버전의 KsanLogExport 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanLogExport/{Version}")]
		public async Task<ActionResult> GetConfigForKsanLogExport([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanLogExport, Version));
		}

		/// <summary>KsanLogExport 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanLogExport")]
		public async Task<ActionResult> SetConfigForKsanLogExport([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.KsanLogExport, Config = Config }));
		}

		/// <summary>KsanLogExport 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanLogExport/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanLogExport([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.KsanLogExport, Version));
		}

		/// <summary>KsanLogExport 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanLogExport/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanLogExport([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.KsanLogExport, Version));
		}
		#endregion
		#region KsanMetering
		/// <summary>KsanMetering 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanMetering")]
		public async Task<ActionResult> GetConfigListForKsanMetering()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.KsanMetering));
		}

		/// <summary>KsanMetering 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanMetering")]
		public async Task<ActionResult> GetConfigForKsanMetering()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanMetering));
		}

		/// <summary>특정 버전의 KsanMetering 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanMetering/{Version}")]
		public async Task<ActionResult> GetConfigForKsanMetering([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.KsanMetering, Version));
		}

		/// <summary>KsanMetering 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanMetering")]
		public async Task<ActionResult> SetConfigForKsanMetering([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.KsanMetering, Config = Config }));
		}

		/// <summary>KsanMetering 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanMetering/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanMetering([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.KsanMetering, Version));
		}

		/// <summary>KsanMetering 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanMetering/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanMetering([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.KsanMetering, Version));
		}
		#endregion
	}
}