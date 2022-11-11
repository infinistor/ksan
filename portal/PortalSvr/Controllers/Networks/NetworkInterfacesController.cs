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
using PortalData.Requests.Networks;
using PortalData.Responses.Networks;
using PortalProviderInterface;
using PortalSvr.Services;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using Swashbuckle.AspNetCore.Annotations;

namespace PortalSvr.Controllers.Networks
{
	/// <summary>네트워크 인터페이스 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class NetworkInterfacesController : BaseController
	{

		/// <summary>데이터 프로바이더</summary>
		private readonly INetworkInterfaceProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public NetworkInterfacesController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<NetworkInterfacesController> logger,
			INetworkInterfaceProvider dataProvider
		)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>네트워크 인터페이스 정보를 추가한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Request">네트워크 인터페이스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseNetworkInterface>))]
		[HttpPost("{ServerId}")]
		public async Task<ActionResult> Add([FromRoute] string ServerId, [FromBody] RequestNetworkInterface Request)
		{
			return Json(await m_dataProvider.Add(ServerId, Request));
		}

		/// <summary>네트워크 인터페이스 정보를 수정한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <param name="Request">네트워크 인터페이스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{ServerId}/{Id}")]
		public async Task<ActionResult> Update([FromRoute] string ServerId, [FromRoute] string Id, [FromBody] RequestNetworkInterface Request)
		{
			return Json(await m_dataProvider.Update(ServerId, Id, Request));
		}

		/// <summary>네트워크 인터페이스 상태를 수정한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <param name="State">네트워크 인터페이스 링크 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{ServerId}/{Id}/LinkStatus/{State}")]
		public async Task<ActionResult> UpdateLinkState([FromRoute] string ServerId, [FromRoute] string Id, [FromRoute] EnumNetworkLinkState State)
		{
			return Json(await m_dataProvider.UpdateLinkState(ServerId, Id, State));
		}

		/// <summary>네트워크 인터페이스 상태를 수정한다.</summary>
		/// <param name="Request">네트워크 인터페이스 링크 상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("LinkStatus")]
		public async Task<ActionResult> UpdateLinkState([FromBody] RequestNetworkInterfaceLinkState Request)
		{
			return Json(await m_dataProvider.UpdateLinkState(Request));
		}

		/// <summary>네트워크 인터페이스 사용 정보 수정</summary>
		/// <param name="Request">네트워크 인터페이스 사용 정보 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Usage")]
		public async Task<ActionResult> UpdateUsage([FromBody] RequestNetworkInterfaceUsage Request)
		{
			return Json(await m_dataProvider.UpdateUsage(Request));
		}

		/// <summary>네트워크 인터페이스 정보를 삭제한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{ServerId}/{Id}")]
		public async Task<ActionResult> Remove([FromRoute] string ServerId, [FromRoute] string Id)
		{
			return Json(await m_dataProvider.Remove(ServerId, Id));
		}

		/// <summary>네트워크 인터페이스 목록을 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseNetworkInterface>))]
		[HttpGet("{ServerId}")]
		public async Task<ActionResult> GetServers(
			[FromRoute] string ServerId
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetList(
				ServerId
				, Skip, CountPerPage
				, OrderFields, OrderDirections
				, SearchFields, SearchKeyword
			));
		}

		/// <summary>특정 네트워크 인터페이스 정보를 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseNetworkInterface>))]
		[HttpGet("{ServerId}/{Id}")]
		public async Task<ActionResult> GetServer([FromRoute] string ServerId, [FromRoute] string Id)
		{
			return Json(await m_dataProvider.Get(ServerId, Id));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Request">특정 이름의 네트워크 인터페이스 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{ServerId}/Exist")]
		public async Task<ActionResult> IsNameExist([FromRoute] string ServerId, [FromBody] RequestIsNetworkInterfaceNameExist Request)
		{
			return Json(await m_dataProvider.IsNameExist(ServerId, null, Request));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="ExceptId">이름 검색 시 제외할 네트워크 인터페이스 아이디</param>
		/// <param name="Request">특정 이름의 네트워크 인터페이스 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{ServerId}/Exist/{ExceptId}")]
		public async Task<ActionResult> IsNameExist([FromRoute] string ServerId, [FromRoute] string ExceptId, [FromBody] RequestIsNetworkInterfaceNameExist Request)
		{
			return Json(await m_dataProvider.IsNameExist(ServerId, ExceptId, Request));
		}
	}
}