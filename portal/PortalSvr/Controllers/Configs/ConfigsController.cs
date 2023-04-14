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
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanOSD, Version));
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

		#region ksanObjManager
		/// <summary>KsanObjManager 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanObjManager")]
		public async Task<ActionResult> GetConfigListForKsanObjManager()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.ksanObjManager));
		}

		/// <summary>KsanObjManager 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanObjManager")]
		public async Task<ActionResult> GetConfigForKsanObjManager()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanObjManager));
		}
		/// <summary>특정 버전의 KsanObjManager 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanObjManager/{Version}")]
		public async Task<ActionResult> GetConfigForKsanObjManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanObjManager, Version));
		}

		/// <summary>KsanObjManager 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanObjManager")]
		public async Task<ActionResult> SetConfigForKsanObjManager([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.ksanObjManager, Config = Config }));
		}

		/// <summary>KsanObjManager 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanObjManager/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanObjManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.ksanObjManager, Version));
		}

		/// <summary>KsanObjManager 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanObjManager/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanObjManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.ksanObjManager, Version));
		}
		#endregion
		#region KsanOSD
		/// <summary>KsanOSD 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanOSD")]
		public async Task<ActionResult> GetConfigListForKsanOSD()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.ksanOSD));
		}

		/// <summary>KsanOSD 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanOSD")]
		public async Task<ActionResult> GetConfigForKsanOSD()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanOSD));
		}
		/// <summary>특정 버전의 KsanOSD 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanOSD/{Version}")]
		public async Task<ActionResult> GetConfigForKsanOSD([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanOSD, Version));
		}

		/// <summary>KsanOSD 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanOSD")]
		public async Task<ActionResult> SetConfigForKsanOSD([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.ksanOSD, Config = Config }));
		}

		/// <summary>KsanOSD 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanOSD/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanOSD([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.ksanOSD, Version));
		}

		/// <summary>KsanOSD 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanOSD/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanOSD([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.ksanOSD, Version));
		}
		#endregion
		#region KsanGW
		/// <summary>KsanGW 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanGW")]
		public async Task<ActionResult> GetConfigListForKsanGW()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.ksanGW));
		}

		/// <summary>KsanGW 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanGW")]
		public async Task<ActionResult> GetConfigForKsanGW()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanGW));
		}

		/// <summary>특정 버전의 KsanGW 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanGW/{Version}")]
		public async Task<ActionResult> GetConfigForKsanGW([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanGW, Version));
		}

		/// <summary>KsanGW 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanGW")]
		public async Task<ActionResult> SetConfigForKsanGW([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.ksanGW, Config = Config }));
		}

		/// <summary>KsanGW 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanGW/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanGW([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.ksanGW, Version));
		}

		/// <summary>KsanGW 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanGW/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanGW([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.ksanGW, Version));
		}
		#endregion
		#region KsanRecovery
		/// <summary>KsanRecovery 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanRecovery")]
		public async Task<ActionResult> GetConfigListForKsanRecovery()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.ksanRecovery));
		}

		/// <summary>KsanRecovery 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanRecovery")]
		public async Task<ActionResult> GetConfigForKsanRecovery()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanRecovery));
		}

		/// <summary>특정 버전의 KsanRecovery 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanRecovery/{Version}")]
		public async Task<ActionResult> GetConfigForKsanRecovery([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanRecovery, Version));
		}

		/// <summary>KsanRecovery 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanRecovery")]
		public async Task<ActionResult> SetConfigForKsanRecovery([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.ksanRecovery, Config = Config }));
		}

		/// <summary>KsanRecovery 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanRecovery/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanRecovery([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.ksanRecovery, Version));
		}

		/// <summary>KsanRecovery 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanRecovery/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanRecovery([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.ksanRecovery, Version));
		}
		#endregion
		#region KsanLifecycleManager
		/// <summary>KsanLifecycleManager 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanLifecycleManager")]
		public async Task<ActionResult> GetConfigListForKsanLifecycleManager()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.ksanLifecycleManager));
		}

		/// <summary>KsanLifecycleManager 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanLifecycleManager")]
		public async Task<ActionResult> GetConfigForKsanLifecycleManager()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanLifecycleManager));
		}

		/// <summary>특정 버전의 KsanLifecycleManager 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanLifecycleManager/{Version}")]
		public async Task<ActionResult> GetConfigForKsanLifecycleManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanLifecycleManager, Version));
		}

		/// <summary>KsanLifecycleManager 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanLifecycleManager")]
		public async Task<ActionResult> SetConfigForKsanLifecycleManager([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.ksanLifecycleManager, Config = Config }));
		}

		/// <summary>KsanLifecycleManager 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanLifecycleManager/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanLifecycleManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.ksanLifecycleManager, Version));
		}

		/// <summary>KsanLifecycleManager 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanLifecycleManager/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanLifecycleManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.ksanLifecycleManager, Version));
		}
		#endregion
		#region KsanReplicationManager
		/// <summary>KsanReplicationManager 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanReplicationManager")]
		public async Task<ActionResult> GetConfigListForKsanReplicationManager()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.ksanReplicationManager));
		}

		/// <summary>KsanReplicationManager 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanReplicationManager")]
		public async Task<ActionResult> GetConfigForKsanReplicationManager()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanReplicationManager));
		}

		/// <summary>특정 버전의 KsanReplicationManager 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanReplicationManager/{Version}")]
		public async Task<ActionResult> GetConfigForKsanReplicationManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanReplicationManager, Version));
		}

		/// <summary>KsanReplicationManager 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanReplicationManager")]
		public async Task<ActionResult> SetConfigForKsanReplicationManager([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.ksanReplicationManager, Config = Config }));
		}

		/// <summary>KsanReplicationManager 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanReplicationManager/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanReplicationManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.ksanReplicationManager, Version));
		}

		/// <summary>KsanReplicationManager 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanReplicationManager/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanReplicationManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.ksanReplicationManager, Version));
		}
		#endregion
		#region KsanLogManager
		/// <summary>KsanLogManager 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/KsanLogManager")]
		public async Task<ActionResult> GetConfigListForKsanLogManager()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.ksanLogManager));
		}

		/// <summary>KsanLogManager 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanLogManager")]
		public async Task<ActionResult> GetConfigForKsanLogManager()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanLogManager));
		}

		/// <summary>특정 버전의 KsanLogManager 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("KsanLogManager/{Version}")]
		public async Task<ActionResult> GetConfigForKsanLogManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.ksanLogManager, Version));
		}

		/// <summary>KsanLogManager 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("KsanLogManager")]
		public async Task<ActionResult> SetConfigForKsanLogManager([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.ksanLogManager, Config = Config }));
		}

		/// <summary>KsanLogManager 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("KsanLogManager/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForKsanLogManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.ksanLogManager, Version));
		}

		/// <summary>KsanLogManager 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("KsanLogManager/{Version}")]
		public async Task<ActionResult> RemoveConfigForKsanLogManager([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.ksanLogManager, Version));
		}
		#endregion
	}
}