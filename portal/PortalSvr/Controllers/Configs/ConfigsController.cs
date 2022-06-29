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

		/// <summary>S3 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/S3")]
		public async Task<ActionResult> GetConfigListForS3()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.S3));
		}

		/// <summary>S3 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("S3")]
		public async Task<ActionResult> GetConfigForS3()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.S3));
		}

		/// <summary>특정 버전의 S3 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("S3/{Version}")]
		public async Task<ActionResult> GetConfigForS3([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.S3, Version));
		}

		/// <summary>S3 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("S3")]
		public async Task<ActionResult> SetConfigForS3([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.S3, Config = Config }));
		}

		/// <summary>S3 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("S3/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForS3([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.S3, Version));
		}

		/// <summary>S3 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("S3/{Version}")]
		public async Task<ActionResult> RemoveConfigForS3([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.S3, Version));
		}

		/// <summary>S3Backend 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/S3Backend")]
		public async Task<ActionResult> GetConfigListForS3Backend()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.S3Backend));
		}

		/// <summary>S3Backend 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("S3Backend")]
		public async Task<ActionResult> GetConfigForS3Backend()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.S3Backend));
		}

		/// <summary>특정 버전의 S3Backend 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("S3Backend/{Version}")]
		public async Task<ActionResult> GetConfigForS3Backend([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.S3Backend, Version));
		}

		/// <summary>S3Backend 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("S3Backend")]
		public async Task<ActionResult> SetConfigForS3Backend([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.S3Backend, Config = Config }));
		}

		/// <summary>S3Backend 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("S3Backend/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForS3Backend([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.S3Backend, Version));
		}

		/// <summary>S3Backend 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("S3Backend/{Version}")]
		public async Task<ActionResult> RemoveConfigForS3Backend([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.S3Backend, Version));
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

		/// <summary>Edge 설정 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceConfig>))]
		[HttpGet("List/Edge")]
		public async Task<ActionResult> GetConfigListForEdge()
		{
			return Json(await m_dataProvider.GetConfigList(EnumServiceType.Edge));
		}

		/// <summary>Edge 설정을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Edge")]
		public async Task<ActionResult> GetConfigForEdge()
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Edge));
		}

		/// <summary>특정 버전의 Edge 설정을 가져온다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfig>))]
		[HttpGet("Edge/{Version}")]
		public async Task<ActionResult> GetConfigForEdge([FromRoute] int Version)
		{
			return Json(await m_dataProvider.GetConfig(EnumServiceType.Edge, Version));
		}

		/// <summary>Edge 설정을 저장한다.</summary>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPost("Edge")]
		public async Task<ActionResult> SetConfigForEdge([FromBody] string Config)
		{
			return Json(await m_dataProvider.SetConfig(new RequestServiceConfig() { Type = EnumServiceType.Edge, Config = Config }));
		}

		/// <summary>Edge 설정의 버전을 변경한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseUpdateConfig>))]
		[HttpPut("Edge/{Version}")]
		public async Task<ActionResult> SetConfigLastVersionForEdge([FromRoute] int Version)
		{
			return Json(await m_dataProvider.SetConfigLastVersion(EnumServiceType.Edge, Version));
		}

		/// <summary>Edge 설정의 버전을 삭제한다.</summary>
		/// <param name="Version">서비스 버전</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Edge/{Version}")]
		public async Task<ActionResult> RemoveConfigForEdge([FromRoute] int Version)
		{
			return Json(await m_dataProvider.RemoveConfig(EnumServiceType.Edge, Version));
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
	}
}