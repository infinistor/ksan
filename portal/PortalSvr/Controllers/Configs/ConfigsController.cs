using System.Collections.Generic;
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
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using Swashbuckle.AspNetCore.Annotations;
using PortalData.Responses.Configs;
using PortalData.Requests.Configs;

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

		/// <summary>OSD 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/Osd")]
		public async Task<ActionResult> GetConfigListForOSD()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.OSD));
		}

		/// <summary>OSD 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Osd")]
		public async Task<ActionResult> GetConfigForOSD()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.OSD));
		}

		/// <summary>특정 버전의 OSD 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Osd/{Version}")]
		public async Task<ActionResult> GetConfigForOSD([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.OSD, Version));
		}

		/// <summary>OSD 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("Osd")]
		public async Task<ActionResult> SetConfigForOSD([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.OSD, Config = Config }));
		}

		/// <summary>OSD 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("Osd/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForOSD([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.OSD, Version));
		}

		/// <summary>OSD 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Osd/{Version}")]
		public async Task<ActionResult> RemoveConfigForOSD([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.OSD, Version));
		}

		/// <summary>GW 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/GW")]
		public async Task<ActionResult> GetConfigListForGW()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.GW));
		}

		/// <summary>GW 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("GW")]
		public async Task<ActionResult> GetConfigForGW()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.GW));
		}

		/// <summary>특정 버전의 GW 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("GW/{Version}")]
		public async Task<ActionResult> GetConfigForGW([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.GW, Version));
		}

		/// <summary>GW 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("GW")]
		public async Task<ActionResult> SetConfigForGW([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.GW, Config = Config }));
		}

		/// <summary>GW 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("GW/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForGW([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.GW, Version));
		}

		/// <summary>GW 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("GW/{Version}")]
		public async Task<ActionResult> RemoveConfigForGW([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.GW, Version));
		}

		/// <summary>Recovery 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/Recovery")]
		public async Task<ActionResult> GetConfigListForRecovery()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.Recovery));
		}

		/// <summary>Recovery 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Recovery")]
		public async Task<ActionResult> GetConfigForRecovery()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Recovery));
		}

		/// <summary>특정 버전의 Recovery 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Recovery/{Version}")]
		public async Task<ActionResult> GetConfigForRecovery([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Recovery, Version));
		}

		/// <summary>Recovery 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("Recovery")]
		public async Task<ActionResult> SetConfigForRecovery([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.Recovery, Config = Config }));
		}

		/// <summary>Recovery 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("Recovery/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForRecovery([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.Recovery, Version));
		}

		/// <summary>Recovery 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Recovery/{Version}")]
		public async Task<ActionResult> RemoveConfigForRecovery([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.Recovery, Version));
		}

		/// <summary>HaProxy 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/HaProxy")]
		public async Task<ActionResult> GetConfigListForHaProxy()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.HaProxy));
		}

		/// <summary>HaProxy 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("HaProxy")]
		public async Task<ActionResult> GetConfigForHaProxy()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.HaProxy));
		}

		/// <summary>특정 버전의 HaProxy 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("HaProxy/{Version}")]
		public async Task<ActionResult> GetConfigForHaProxy([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.HaProxy, Version));
		}

		/// <summary>HaProxy 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("HaProxy")]
		public async Task<ActionResult> SetConfigForHaProxy([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.HaProxy, Config = Config }));
		}

		/// <summary>HaProxy 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("HaProxy/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForHaProxy([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.HaProxy, Version));
		}

		/// <summary>HaProxy 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("HaProxy/{Version}")]
		public async Task<ActionResult> RemoveConfigForHaProxy([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.HaProxy, Version));
		}

		/// <summary>Monitor 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/Monitor")]
		public async Task<ActionResult> GetConfigListForMonitor()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.Monitor));
		}

		/// <summary>Monitor 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Monitor")]
		public async Task<ActionResult> GetConfigForMonitor()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Monitor));
		}

		/// <summary>특정 버전의 Monitor 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Monitor/{Version}")]
		public async Task<ActionResult> GetConfigForMonitor([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Monitor, Version));
		}

		/// <summary>Monitor 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("Monitor")]
		public async Task<ActionResult> SetConfigForMonitor([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.Monitor, Config = Config }));
		}

		/// <summary>Monitor 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("Monitor/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForMonitor([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.Monitor, Version));
		}

		/// <summary>Monitor 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Monitor/{Version}")]
		public async Task<ActionResult> RemoveConfigForMonitor([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.Monitor, Version));
		}

		/// <summary>Agent 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/Agent")]
		public async Task<ActionResult> GetConfigListForAgent()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.Agent));
		}

		/// <summary>Agent 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Agent")]
		public async Task<ActionResult> GetConfigForAgent()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Agent));
		}

		/// <summary>특정 버전의 Agent 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Agent/{Version}")]
		public async Task<ActionResult> GetConfigForAgent([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Agent, Version));
		}

		/// <summary>Agent 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("Agent")]
		public async Task<ActionResult> SetConfigForAgent([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.Agent, Config = Config }));
		}

		/// <summary>Agent 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("Agent/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForAgent([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.Agent, Version));
		}

		/// <summary>Agent 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Agent/{Version}")]
		public async Task<ActionResult> RemoveConfigForAgent([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.Agent, Version));
		}

		/// <summary>MongoDB 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/MongoDB")]
		public async Task<ActionResult> GetConfigListForMongoDB()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.MongoDB));
		}

		/// <summary>MongoDB 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("MongoDB")]
		public async Task<ActionResult> GetConfigForMongoDB()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.MongoDB));
		}

		/// <summary>특정 버전의 MongoDB 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("MongoDB/{Version}")]
		public async Task<ActionResult> GetConfigForMongoDB([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.MongoDB, Version));
		}

		/// <summary>MongoDB 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("MongoDB")]
		public async Task<ActionResult> SetConfigForMongoDB([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.MongoDB, Config = Config }));
		}

		/// <summary>MongoDB 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("MongoDB/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForMongoDB([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.MongoDB, Version));
		}

		/// <summary>MongoDB 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("MongoDB/{Version}")]
		public async Task<ActionResult> RemoveConfigForMongoDB([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.MongoDB, Version));
		}

		/// <summary>Lifecycle 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/Lifecycle")]
		public async Task<ActionResult> GetConfigListForLifecycle()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.Lifecycle));
		}

		/// <summary>Lifecycle 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Lifecycle")]
		public async Task<ActionResult> GetConfigForLifecycle()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Lifecycle));
		}

		/// <summary>특정 버전의 Lifecycle 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Lifecycle/{Version}")]
		public async Task<ActionResult> GetConfigForLifecycle([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Lifecycle, Version));
		}

		/// <summary>Lifecycle 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("Lifecycle")]
		public async Task<ActionResult> SetConfigForLifecycle([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.Lifecycle, Config = Config }));
		}

		/// <summary>Lifecycle 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("Lifecycle/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForLifecycle([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.Lifecycle, Version));
		}

		/// <summary>Lifecycle 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Lifecycle/{Version}")]
		public async Task<ActionResult> RemoveConfigForLifecycle([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.Lifecycle, Version));
		}

		/// <summary>Replication 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/Replication")]
		public async Task<ActionResult> GetConfigListForReplication()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.Replication));
		}

		/// <summary>Replication 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Replication")]
		public async Task<ActionResult> GetConfigForReplication()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Replication));
		}

		/// <summary>특정 버전의 Replication 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Replication/{Version}")]
		public async Task<ActionResult> GetConfigForReplication([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Replication, Version));
		}

		/// <summary>Replication 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("Replication")]
		public async Task<ActionResult> SetConfigForReplication([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.Replication, Config = Config }));
		}

		/// <summary>Replication 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("Replication/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForReplication([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.Replication, Version));
		}

		/// <summary>Replication 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Replication/{Version}")]
		public async Task<ActionResult> RemoveConfigForReplication([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.Replication, Version));
		}

		/// <summary>Logging 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/Logging")]
		public async Task<ActionResult> GetConfigListForLogging()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.Logging));
		}

		/// <summary>Logging 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Logging")]
		public async Task<ActionResult> GetConfigForLogging()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Logging));
		}

		/// <summary>특정 버전의 Logging 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Logging/{Version}")]
		public async Task<ActionResult> GetConfigForLogging([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Logging, Version));
		}

		/// <summary>Logging 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("Logging")]
		public async Task<ActionResult> SetConfigForLogging([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.Logging, Config = Config }));
		}

		/// <summary>Logging 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("Logging/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForLogging([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.Logging, Version));
		}

		/// <summary>Logging 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Logging/{Version}")]
		public async Task<ActionResult> RemoveConfigForLogging([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.Logging, Version));
		}

		/// <summary>Metering 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/Metering")]
		public async Task<ActionResult> GetConfigListForMetering()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.Metering));
		}

		/// <summary>Metering 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Metering")]
		public async Task<ActionResult> GetConfigForMetering()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Metering));
		}

		/// <summary>특정 버전의 Metering 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Metering/{Version}")]
		public async Task<ActionResult> GetConfigForMetering([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Metering, Version));
		}

		/// <summary>Metering 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("Metering")]
		public async Task<ActionResult> SetConfigForMetering([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.Metering, Config = Config }));
		}

		/// <summary>Metering 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("Metering/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForMetering([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.Metering, Version));
		}

		/// <summary>Metering 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Metering/{Version}")]
		public async Task<ActionResult> RemoveConfigForMetering([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.Metering, Version));
		}
	}
}