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
using PortalSvr.Services;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
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
		/// <param name="Request">서비스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceWithVlans>))]
		[HttpPost]
		public async Task<ActionResult> AddService([FromBody] RequestService Request)
		{
			return Json(await m_dataProvider.Add(Request));
		}

		/// <summary>서비스 정보를 수정한다.</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="Request">서비스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}")]
		public async Task<ActionResult> UpdateService([FromRoute] string Id, [FromBody] RequestService Request)
		{
			return Json(await m_dataProvider.Update(Id, Request));
		}

		/// <summary>서비스 상태 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="State">서비스 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}/State/{State}")]
		public async Task<ActionResult> UpdateServiceState([FromRoute] string Id, [FromRoute] EnumServiceState State)
		{
			return Json(await m_dataProvider.UpdateState(Id, State));
		}

		/// <summary>서비스 상태 수정</summary>
		/// <param name="Request">상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("State")]
		public async Task<ActionResult> UpdateServiceState([FromBody] RequestServiceState Request)
		{
			return Json(await m_dataProvider.UpdateState(Request));
		}

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <param name="State">HA 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}/HaAction/{State}")]
		public async Task<ActionResult> UpdateHaAction([FromRoute] string Id, [FromRoute] EnumHaAction State)
		{
			return Json(await m_dataProvider.UpdateHaAction(Id, State));
		}

		/// <summary>서비스 HA 상태 수정</summary>
		/// <param name="Request">HA 상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("HaAction")]
		public async Task<ActionResult> UpdateHaAction([FromBody] RequestServiceHaAction Request)
		{
			return Json(await m_dataProvider.UpdateHaAction(Request));
		}

		/// <summary>서비스 사용 정보 수정</summary>
		/// <param name="Request">서비스 사용 정보 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Usage")]
		public async Task<ActionResult> UpdateUsage([FromBody] RequestServiceUsage Request)
		{
			return Json(await m_dataProvider.UpdateUsage(Request));
		}

		/// <summary>서비스 정보를 삭제한다.</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{Id}")]
		public async Task<ActionResult> RemoveService([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Remove(Id));
		}

		/// <summary>서비스 목록을 가져온다.</summary>
		/// <param name="SearchState">검색할 서비스 상태 목록</param>
		/// <param name="SearchType">검색할 서비스 타입 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, ServiceType)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (GroupName, Name, Description, ServiceType, IpAddress)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceWithGroup>))]
		[HttpGet]
		public async Task<ActionResult> GetServices(
			EnumServiceState? SearchState = null, EnumServiceType? SearchType = null, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = null
		)
		{
			return Json(await m_dataProvider.GetList(
				SearchState, SearchType, Skip, CountPerPage
				, OrderFields, OrderDirections
				, SearchFields, SearchKeyword
			));
		}

		/// <summary>특정 서비스 정보를 가져온다.</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServiceWithVlans>))]
		[HttpGet("{Id}")]
		public async Task<ActionResult> GetService([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Get(Id));
		}

		/// <summary>특정 이름의 서비스가 존재하는지 확인한다.</summary>
		/// <param name="Name">검색할 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{Name}")]
		public async Task<ActionResult> IsServiceNameExist([FromRoute] string Name)
		{
			return Json(await m_dataProvider.IsNameExist(null, Name));
		}

		/// <summary>특정 이름의 서비스가 존재하는지 확인한다.</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 서비스 아이디</param>
		/// <param name="Name">검색할 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{ExceptId}/{Name}")]
		public async Task<ActionResult> IsServiceNameExist([FromRoute] string ExceptId, [FromRoute] string Name)
		{
			return Json(await m_dataProvider.IsNameExist(ExceptId, Name));
		}

		/// <summary>서비스 시작</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{Id}/Start")]
		public async Task<ActionResult> StartService([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Start(Id));
		}

		/// <summary>서비스 중지</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{Id}/Stop")]
		public async Task<ActionResult> StopService([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Stop(Id));
		}

		/// <summary>서비스 재시작</summary>
		/// <param name="Id">서비스 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("{Id}/Restart")]
		public async Task<ActionResult> RestartService([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Restart(Id));
		}

		/// <summary>서비스 이벤트를 추가한다</summary>
		/// <param name="Request">서비스 이벤트 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("Event")]
		public async Task<ActionResult> AddEvent([FromBody] RequestServiceEvent Request)
		{
			return Json(await m_dataProvider.AddEvent(Request));
		}

		/// <summary>서비스 이벤트 목록을 가져온다.</summary>
		/// <param name="Id"> 서비스 아이디 / 이름</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (기본 RegDate, ServiceEventType)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (ServiceEventType)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServiceEvent>))]
		[HttpGet("Event/{Id}")]
		public async Task<ActionResult> GetServiceEvents([FromRoute] string Id, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = null
		)
		{
			return Json(await m_dataProvider.GetEventList(Id, Skip, CountPerPage
				, OrderFields, OrderDirections, SearchFields, SearchKeyword
			));
		}
	}
}