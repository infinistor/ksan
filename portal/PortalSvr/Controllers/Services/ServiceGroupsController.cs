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
using System.Collections.Generic;
using System.Net;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Requests.Services;
using PortalData.Responses.Services;
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
		/// <param name="Request">서비스 그룹 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceGroupWithServices>))]
		[HttpPost]
		public async Task<ActionResult> Add([FromBody] RequestServiceGroup Request)
		{
			return Json(await m_dataProvider.Add(Request));
		}

		/// <summary>서비스 그룹 정보를 수정한다.</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <param name="Request">서비스 그룹 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}")]
		public async Task<ActionResult> Update([FromRoute] string Id, [FromBody] RequestServiceGroup Request)
		{
			return Json(await m_dataProvider.Update(Id, Request));
		}

		/// <summary>서비스 그룹 정보를 삭제한다.</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{Id}")]
		public async Task<ActionResult> Remove([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Remove(Id));
		}

		/// <summary>서비스 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, ServiceType, ServiceIpAddress)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, ServiceType, ServiceIpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceGroup>))]
		[HttpGet]
		public async Task<ActionResult> GetList(
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetList(
				Skip, CountPerPage
				, OrderFields, OrderDirections
				, SearchFields, SearchKeyword
			));
		}

		/// <summary>특정 서비스 그룹 정보를 가져온다.</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceGroupWithServices>))]
		[HttpGet("{Id}")]
		public async Task<ActionResult> Get([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Get(Id));
		}

		/// <summary>특정 이름의 서비스 그룹가 존재하는지 확인한다.</summary>
		/// <param name="Request">특정 이름의 서비스 그룹 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist")]
		public async Task<ActionResult> IsNameExist([FromBody] RequestIsServiceGroupNameExist Request)
		{
			return Json(await m_dataProvider.IsNameExist(null, Request));
		}

		/// <summary>특정 이름의 서비스 그룹가 존재하는지 확인한다.</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 서비스 그룹 아이디</param>
		/// <param name="Request">특정 이름의 서비스 그룹 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{ExceptId}")]
		public async Task<ActionResult> IsNameExist([FromRoute] string ExceptId, [FromBody] RequestIsServiceGroupNameExist Request)
		{
			return Json(await m_dataProvider.IsNameExist(ExceptId, Request));
		}

		/// <summary>해당 서비스 타입으로 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseService>))]
		[HttpGet("AvailableServices/{ServiceType}")]
		public async Task<ActionResult> GetAvailableServices(
			[FromRoute] EnumServiceType ServiceType
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "")
		{
			return Json(await m_dataProvider.GetAvailableServices(
				ServiceType
				, Skip, CountPerPage
				, OrderFields, OrderDirections
				, SearchFields, SearchKeyword));
		}

		/// <summary>주어진 서비스 그룹 아이디에 해당하는 서비스 그룹에 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="Id">서비스 그룹 아이디 (null인 경우, 어느 그룹에도 속하지 않은 서비스만 검색한다.)</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseService>))]
		[HttpGet("{Id}/AvailableServices")]
		public async Task<ActionResult> GetAvailableServices(
			[FromRoute] string Id
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "")
		{
			return Json(await m_dataProvider.GetAvailableServices(
				Id
				, Skip, CountPerPage
				, OrderFields, OrderDirections
				, SearchFields, SearchKeyword));
		}

		/// <summary>서비스 그룹 시작</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{Id}/Start")]
		public async Task<ActionResult> Start([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Start(Id));
		}

		/// <summary>서비스 그룹 중지</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{Id}/Stop")]
		public async Task<ActionResult> Stop([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Stop(Id));
		}

		/// <summary>서비스 그룹 재시작</summary>
		/// <param name="Id">서비스 그룹 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{Id}/Restart")]
		public async Task<ActionResult> Restart([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Restart(Id));
		}
	}
}