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
using Newtonsoft.Json;
using Swashbuckle.AspNetCore.Annotations;

namespace PortalSvr.Controllers.Services
{
	/// <summary>서비스 그룹 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class ServiceGroupsController : BaseController
	{

		/// <summary>서비스 그룹 데이터 프로바이더</summary>
		private readonly IServiceGroupProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public ServiceGroupsController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<ServiceGroupsController> logger,
			IServiceGroupProvider dataProvider
		)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>서비스 그룹 정보를 추가한다.</summary>
		/// <param name="request">서비스 그룹 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceGroupWithServices>))]
		[HttpPost]
		public async Task<ActionResult> Add([FromBody] RequestServiceGroup request)
		{
			return Json(await m_dataProvider.Add(request));
		}

		/// <summary>서비스 그룹 정보를 수정한다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <param name="request">서비스 그룹 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}")]
		public async Task<ActionResult> Update([FromRoute] string id, [FromBody] RequestServiceGroup request)
		{
			return Json(await m_dataProvider.Update(id, request));
		}

		/// <summary>서비스 그룹 정보를 삭제한다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{id}")]
		public async Task<ActionResult> Remove([FromRoute] string id)
		{
			return Json(await m_dataProvider.Remove(id));
		}

		/// <summary>서비스 목록을 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description, ServiceType, ServiceIpAddress)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, ServiceType, ServiceIpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceGroup>))]
		[HttpGet]
		public async Task<ActionResult> GetList(
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

		/// <summary>특정 서비스 그룹 정보를 가져온다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceGroupWithServices>))]
		[HttpGet("{id}")]
		public async Task<ActionResult> Get([FromRoute] string id)
		{
			return Json(await m_dataProvider.Get(id));
		}

		/// <summary>특정 이름의 서비스 그룹가 존재하는지 확인한다.</summary>
		/// <param name="request">특정 이름의 서비스 그룹 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist")]
		public async Task<ActionResult> IsNameExist([FromBody] RequestIsServiceGroupNameExist request)
		{
			return Json(await m_dataProvider.IsNameExist(null, request));
		}

		/// <summary>특정 이름의 서비스 그룹가 존재하는지 확인한다.</summary>
		/// <param name="exceptId">이름 검색 시 제외할 서비스 그룹 아이디</param>
		/// <param name="request">특정 이름의 서비스 그룹 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{exceptId}")]
		public async Task<ActionResult> IsNameExist([FromRoute] string exceptId, [FromBody] RequestIsServiceGroupNameExist request)
		{
			return Json(await m_dataProvider.IsNameExist(exceptId, request));
		}

		/// <summary>해당 서비스 타입으로 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="serviceType">서비스 타입</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseService>))]
		[HttpGet("AvailableServices/{serviceType}")]
		public async Task<ActionResult> GetAvailableServices(
			[FromRoute] EnumServiceType serviceType
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "")
		{
			return Json(await m_dataProvider.GetAvailableServices(
				serviceType
				, skip, countPerPage
				, orderFields, orderDirections
				, searchFields, searchKeyword));
		}

		/// <summary>주어진 서비스 그룹 아이디에 해당하는 서비스 그룹에 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="id">서비스 그룹 아이디 (null인 경우, 어느 그룹에도 속하지 않은 서비스만 검색한다.)</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseService>))]
		[HttpGet("{id}/AvailableServices")]
		public async Task<ActionResult> GetAvailableServices(
			[FromRoute] string id
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "")
		{
			return Json(await m_dataProvider.GetAvailableServices(
				id
				, skip, countPerPage
				, orderFields, orderDirections
				, searchFields, searchKeyword));
		}

		/// <summary>서비스 그룹 시작</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Start")]
		public async Task<ActionResult> Start([FromRoute] string id)
		{
			return Json(await m_dataProvider.Start(id));
		}

		/// <summary>서비스 그룹 중지</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Stop")]
		public async Task<ActionResult> Stop([FromRoute] string id)
		{
			return Json(await m_dataProvider.Stop(id));
		}

		/// <summary>서비스 그룹 재시작</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{id}/Restart")]
		public async Task<ActionResult> Restart([FromRoute] string id)
		{
			return Json(await m_dataProvider.Restart(id));
		}

		/// <summary>특정 서비스 그룹의 설정 정보를 가져온다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<IResponseServiceConfig>))]
		[HttpGet("{id}/Config")]
		public async Task<ActionResult> GetConfig([FromRoute] string id)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceGroupWithServices> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				switch (response.Data.ServiceType)
				{
					case EnumServiceType.HaProxy:
						return Json(await m_dataProvider.GetConfig<ResponseServiceConfigForHaProxy>(id));
					default:
						return Json(new ResponseData<IResponseServiceConfig>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND));
				}
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData<IResponseServiceConfig>(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>주어진 설정 정보를 특정 서비스 그룹에 저장한다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <param name="config">서비스 설정 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}/Config")]
		public async Task<ActionResult> SetConfig([FromRoute] string id, [FromBody] dynamic config)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceGroupWithServices> response = await m_dataProvider.Get(id);

			// 해당 서비스 정보를 가져오는데 성공한 경우
			if (response.Result == EnumResponseResult.Success)
			{
				switch (response.Data.ServiceType)
				{
					case EnumServiceType.HaProxy:
						return Json(await m_dataProvider.SetConfig<RequestServiceConfigForHaProxy>(id, JsonConvert.DeserializeObject<RequestServiceConfigForHaProxy>(config.ToString())));
					default:
						return Json(new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND));
				}
			}
			// 해당 서비스 정보를 가져오는데 실패한 경우
			else
				return Json(new ResponseData(EnumResponseResult.Error, response.Code, response.Message));
		}

		/// <summary>주어진 설정 정보를 특정 서비스 그룹에 저장한다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <param name="config">서비스 설정 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}/Config/HaProxy")]
		public async Task<ActionResult> SetConfig([FromRoute] string id, [FromBody] RequestServiceConfigForHaProxy config)
		{
			// 해당 서비스 정보를 가져온다.
			ResponseData<ResponseServiceGroupWithServices> response = await m_dataProvider.Get(id);

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
	}
}