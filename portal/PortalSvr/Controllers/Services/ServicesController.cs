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
using System.Collections.Generic;
using System.Net;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Requests.Services;
using PortalData.Requests.Services.Configs;
using PortalData.Responses.Services;
using PortalData.Responses.Services.Configs;
using PortalProviderInterface;
using PortalResources;
using PortalSvr.Services;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Reflection;
using Swashbuckle.AspNetCore.Annotations;

namespace PortalSvr.Controllers.Services
{
	/// <summary>서비스 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class ServicesController : BaseController
	{

		/// <summary>서비스 데이터 프로바이더</summary>
		private readonly IServiceProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public ServicesController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<ServicesController> logger,
			IServiceProvider dataProvider
		)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>서비스 정보를 추가한다.</summary>
		/// <param name="request">서비스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceWithVlans>))]
		[HttpPost]
		public async Task<ActionResult> AddService([FromBody] RequestService request)
		{
			return Json(await m_dataProvider.Add(request));
		}

		/// <summary>서비스 정보를 수정한다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="request">서비스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}")]
		public async Task<ActionResult> UpdateService([FromRoute] string id, [FromBody] RequestService request)
		{
			return Json(await m_dataProvider.Update(id, request));
		}

		/// <summary>서비스 상태 수정</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="state">서비스 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}/State/{state}")]
		public async Task<ActionResult> UpdateServiceState([FromRoute] string id, [FromRoute] EnumServiceState state)
		{
			return Json(await m_dataProvider.UpdateState(id, state));
		}

		/// <summary>서비스 상태 수정</summary>
		/// <param name="request">상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("State")]
		public async Task<ActionResult> UpdateServiceState([FromBody] RequestServiceState request)
		{
			return Json(await m_dataProvider.UpdateState(request));
		}

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="state">HA 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}/HaAction/{state}")]
		public async Task<ActionResult> UpdateHaAction([FromRoute] string id, [FromRoute] EnumHaAction state)
		{
			return Json(await m_dataProvider.UpdateHaAction(id, state));
		}

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="request">HA 상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("HaAction")]
		public async Task<ActionResult> UpdateHaAction([FromBody] RequestServiceHaAction request)
		{
			return Json(await m_dataProvider.UpdateHaAction(request));
		}

		/// <summary>서비스 사용 정보 수정</summary>
		/// <param name="request">서비스 사용 정보 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Usage")]
		public async Task<ActionResult> UpdateUsage([FromBody] RequestServiceUsage request)
		{
			return Json(await m_dataProvider.UpdateUsage(request));
		}

		/// <summary>서비스 정보를 삭제한다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{id}")]
		public async Task<ActionResult> RemoveService([FromRoute] string id)
		{
			return Json(await m_dataProvider.Remove(id));
		}

		/// <summary>서비스 목록을 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description, ServiceType)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (GroupName, Name, Description, ServiceType, IpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceWithGroup>))]
		[HttpGet]
		public async Task<ActionResult> GetServices(
			int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetList(
				skip, countPerPage
				, orderFields, orderDirections
				, searchFields, searchKeyword
			));
		}

		/// <summary>특정 서비스 정보를 가져온다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceWithVlans>))]
		[HttpGet("{id}")]
		public async Task<ActionResult> GetService([FromRoute] string id)
		{
			return Json(await m_dataProvider.Get(id));
		}

		/// <summary>특정 이름의 서비스가 존재하는지 확인한다.</summary>
		/// <param name="request">특정 이름의 서비스 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist")]
		public async Task<ActionResult> IsServiceNameExist([FromBody] RequestIsServiceNameExist request)
		{
			return Json(await m_dataProvider.IsNameExist(null, request));
		}

		/// <summary>특정 이름의 서비스가 존재하는지 확인한다.</summary>
		/// <param name="exceptId">이름 검색 시 제외할 서비스 아이디</param>
		/// <param name="request">특정 이름의 서비스 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{exceptId}")]
		public async Task<ActionResult> IsServiceNameExist([FromRoute] string exceptId, [FromBody] RequestIsServiceNameExist request)
		{
			return Json(await m_dataProvider.IsNameExist(exceptId, request));
		}

		/// <summary>서비스 시작</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Start")]
		public async Task<ActionResult> StartService([FromRoute] string id)
		{
			return Json(await m_dataProvider.Start(id));
		}

		/// <summary>서비스 중지</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Stop")]
		public async Task<ActionResult> StopService([FromRoute] string id)
		{
			return Json(await m_dataProvider.Stop(id));
		}

		/// <summary>서비스 재시작</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Restart")]
		public async Task<ActionResult> RestartService([FromRoute] string id)
		{
			return Json(await m_dataProvider.Restart(id));
		}

		/// <summary>HA Proxy 서비스의 설정 정보를 가져온다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfigForHaProxy>))]
		[HttpGet("{id}/Config/HaProxy")]
		public async Task<ActionResult> GetConfigForHaProxy([FromRoute] string id)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceWithVlans> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				// HA Proxy 타입인 경우
				if (response.Data.ServiceType == EnumServiceType.HaProxy)
					return Json(await m_dataProvider.GetConfig<ResponseServiceConfigForHaProxy>(id));

				// 그 외
				return Json(new ResponseData<ResponseServiceConfigForHaProxy>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST));
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData<ResponseServiceConfigForHaProxy>(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>HA Proxy 설정 정보를 특정 서비스에 저장한다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="config">서비스 설정 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Config/HaProxy")]
		public async Task<ActionResult> SetConfigForHaProxy([FromRoute] string id, [FromBody] RequestServiceConfigForHaProxy config)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceWithVlans> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				// HA Proxy 타입인 경우
				if (response.Data.ServiceType == EnumServiceType.HaProxy)
					return Json(await m_dataProvider.SetConfig(id, config));

				// 그 외
				return Json(new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST));
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>HA Proxy 설정 정보를 특정 서비스에 저장한다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="requestConfig">서비스 설정 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Config/HaProxy/String")]
		public async Task<ActionResult> SetConfigForHaProxy([FromRoute] string id, [FromBody] RequestServiceConfigFromString requestConfig)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceWithVlans> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				// 설정 문자열로 부터 설정 객체를 가져온다.
				ResponseServiceConfigForHaProxy config = new ResponseServiceConfigForHaProxy();
				ResponseData responseLoadConfig = config.Deserialize(requestConfig.Config);
				// 설정 객체을 로드하는데 성공한 경우
				if (responseLoadConfig.Result == EnumResponseResult.Success)
				{
					RequestServiceConfigForHaProxy request = new RequestServiceConfigForHaProxy();
					request.ConfigGlobal.CopyValueFrom(config.ConfigGlobal);
					request.ConfigDefault.CopyValueFrom(config.ConfigDefault);
					request.ConfigListens.CopyValueFrom(config.ConfigListens);
					return Json(await m_dataProvider.SetConfig(id, request));
				}
				// 설정 객체를 로드하는데 실패하는 경우
				else
					return Json(responseLoadConfig);
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>S3 서비스의 설정 정보를 가져온다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfigForS3>))]
		[HttpGet("{id}/Config/S3")]
		public async Task<ActionResult> GetConfigForS3([FromRoute] string id)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceWithVlans> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				// HA Proxy 타입인 경우
				if (response.Data.ServiceType == EnumServiceType.S3)
					return Json(await m_dataProvider.GetConfig<ResponseServiceConfigForS3>(id));

				// 그 외
				return Json(new ResponseData<ResponseServiceConfigForS3>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST));
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData<ResponseServiceConfigForS3>(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>S3 설정 정보를 특정 서비스에 저장한다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="config">서비스 설정 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Config/S3")]
		public async Task<ActionResult> SetConfigForS3([FromRoute] string id, [FromBody] RequestServiceConfigForS3 config)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceWithVlans> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				// HA Proxy 타입인 경우
				if (response.Data.ServiceType == EnumServiceType.S3)
					return Json(await m_dataProvider.SetConfig(id, config));

				// 그 외
				return Json(new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST));
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>S3 설정 정보를 특정 서비스에 저장한다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="requestConfig">서비스 설정 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Config/S3/String")]
		public async Task<ActionResult> SetConfigForS3([FromRoute] string id, [FromBody] RequestServiceConfigFromString requestConfig)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceWithVlans> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				// 설정 문자열로 부터 설정 객체를 가져온다.
				ResponseServiceConfigForS3 config = new ResponseServiceConfigForS3();
				ResponseData responseLoadConfig = config.Deserialize(requestConfig.Config);
				// 설정 객체을 로드하는데 성공한 경우
				if (responseLoadConfig.Result == EnumResponseResult.Success)
				{
					RequestServiceConfigForS3 request = new RequestServiceConfigForS3();
					request.Database.CopyValueFrom(config.Database);
					request.MessageQueue.CopyValueFrom(config.MessageQueue);
					return Json(await m_dataProvider.SetConfig(id, request));
				}
				// 설정 객체를 로드하는데 실패하는 경우
				else
					return Json(responseLoadConfig);
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>OSD 서비스의 설정 정보를 가져온다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceConfigForOsd>))]
		[HttpGet("{id}/Config/Osd")]
		public async Task<ActionResult> GetConfigForOsd([FromRoute] string id)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceWithVlans> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				// HA Proxy 타입인 경우
				if (response.Data.ServiceType == EnumServiceType.OSD)
					return Json(await m_dataProvider.GetConfig<ResponseServiceConfigForOsd>(id));

				// 그 외
				return Json(new ResponseData<ResponseServiceConfigForS3>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST));
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData<ResponseServiceConfigForS3>(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>OSD 설정 정보를 특정 서비스에 저장한다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="config">서비스 설정 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Config/Osd")]
		public async Task<ActionResult> SetConfigForOsd([FromRoute] string id, [FromBody] RequestServiceConfigForOsd config)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceWithVlans> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				// HA Proxy 타입인 경우
				if (response.Data.ServiceType == EnumServiceType.OSD)
					return Json(await m_dataProvider.SetConfig(id, config));

				// 그 외
				return Json(new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST));
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>OSD 설정 정보를 특정 서비스에 저장한다.</summary>
		/// <param name="id">서비스 아이디</param>
		/// <param name="requestConfig">서비스 설정 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Config/Osd/String")]
		public async Task<ActionResult> SetConfigForOsd([FromRoute] string id, [FromBody] RequestServiceConfigFromString requestConfig)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceWithVlans> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				// 설정 문자열로 부터 설정 객체를 가져온다.
				ResponseServiceConfigForOsd config = new ResponseServiceConfigForOsd();
				ResponseData responseLoadConfig = config.Deserialize(requestConfig.Config);
				// 설정 객체을 로드하는데 성공한 경우
				if (responseLoadConfig.Result == EnumResponseResult.Success)
				{
					RequestServiceConfigForOsd request = new RequestServiceConfigForOsd();
					request.Database.CopyValueFrom(config.Database);
					request.MessageQueue.CopyValueFrom(config.MessageQueue);
					return Json(await m_dataProvider.SetConfig(id, request));
				}
				// 설정 객체를 로드하는데 실패하는 경우
				else
					return Json(responseLoadConfig);
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData(EnumResponseResult.Error, response.Code, response.Message));
		}
	}
}