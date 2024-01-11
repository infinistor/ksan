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
using PortalData.Requests.Servers;
using PortalData.Responses.Networks;
using PortalData.Responses.Servers;
using PortalProviderInterface;
using PortalSvr.Services;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using Swashbuckle.AspNetCore.Annotations;

namespace PortalSvr.Controllers.Servers
{
	/// <summary>서버 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class ServersController : BaseController
	{

		/// <summary>서버 데이터 프로바이더</summary>
		private readonly IServerProvider m_dataProvider;

		/// <summary>네트워크 인터페이스 데이터 프로바이더</summary>
		private readonly INetworkInterfaceProvider m_networkInterfaceProvider;

		/// <summary>네트워크 인터페이스 VLAN 데이터 프로바이더</summary>
		private readonly INetworkInterfaceVlanProvider m_networkInterfaceVlanProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		/// <param name="networkInterfaceProvider">네트워크 인터페이스 데이터 프로바이더</param>
		/// <param name="networkInterfaceVlanProvider">네트워크 인터페이스 VLAN 데이터 프로바이더</param>
		public ServersController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<ServersController> logger,
			IServerProvider dataProvider,
			INetworkInterfaceProvider networkInterfaceProvider,
			INetworkInterfaceVlanProvider networkInterfaceVlanProvider
		)
			: base(configuration, userManager, logger, dataProvider, networkInterfaceProvider, networkInterfaceVlanProvider)
		{
			m_dataProvider = dataProvider;
			m_networkInterfaceProvider = networkInterfaceProvider;
			m_networkInterfaceVlanProvider = networkInterfaceVlanProvider;
		}

		#region 서버 관련

		/// <summary>서버 정보를 추가한다.</summary>
		/// <param name="Request">서버 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServer>))]
		[HttpPost]
		public async Task<ActionResult> AddServer([FromBody] RequestServer Request)
		{
			return Json(await m_dataProvider.Add(Request));
		}

		/// <summary>서버 초기화</summary>
		/// <param name="Request">서버 초기화 요청 객체</param>
		/// <returns>서버 초기화 결과 객체</returns>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("Initialize")]
		public async Task<ActionResult> Initialize([FromBody] RequestServerInitialize Request)
		{
			return Json(await m_dataProvider.Initialize(Request));
		}

		/// <summary>서버 정보를 수정한다.</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <param name="Request">서버 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}")]
		public async Task<ActionResult> UpdateServer([FromRoute] string Id, [FromBody] RequestServer Request)
		{
			return Json(await m_dataProvider.Update(Id, Request));
		}

		/// <summary>서버 상태 수정</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <param name="State">서버 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}/State/{State}")]
		public async Task<ActionResult> UpdateServerState([FromRoute] string Id, [FromRoute] EnumServerState State)
		{
			return Json(await m_dataProvider.UpdateState(Id, State));
		}

		/// <summary>서버 상태 수정</summary>
		/// <param name="Request">서버 상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("State")]
		public async Task<ActionResult> UpdateServerState([FromBody] RequestServerState Request)
		{
			return Json(await m_dataProvider.UpdateState(Request));
		}

		/// <summary>서버 사용 정보 수정</summary>
		/// <param name="Request">서버 사용 정보 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Usage")]
		public async Task<ActionResult> UpdateServerUsage([FromBody] RequestServerUsage Request)
		{
			return Json(await m_dataProvider.UpdateUsage(Request));
		}

		/// <summary>서버 정보를 삭제한다.</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{Id}")]
		public async Task<ActionResult> RemoveServer([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Remove(Id));
		}

		/// <summary>서버 목록을 가져온다.</summary>
		/// <param name="SearchState">검색할 서버 상태 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServer>))]
		[HttpGet]
		public async Task<ActionResult> GetServers(
			EnumServerState? SearchState,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetList(
				SearchState,
				Skip, CountPerPage,
				OrderFields, OrderDirections,
				SearchFields, SearchKeyword
			));
		}

		/// <summary>서버 목록을 가져온다.</summary>
		/// <param name="SearchState">검색할 서버 상태 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServerDetail>))]
		[HttpGet("Details")]
		public async Task<ActionResult> GetServerDetails(
			EnumServerState? SearchState,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetListDetails(
				SearchState,
				Skip, CountPerPage,
				OrderFields, OrderDirections,
				SearchFields, SearchKeyword
			));
		}

		/// <summary>특정 서버 정보를 가져온다.</summary>
		/// <param name="Id">서버 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServerDetail>))]
		[HttpGet("{Id}")]
		public async Task<ActionResult> GetServer([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Get(Id));
		}

		/// <summary>특정 이름의 서버가 존재하는지 확인한다.</summary>
		/// <param name="Name">검색할 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{Name}")]
		public async Task<ActionResult> IsServerNameExist([FromRoute] string Name)
		{
			return Json(await m_dataProvider.IsNameExist(null, Name));
		}

		/// <summary>특정 이름의 서버가 존재하는지 확인한다.</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 서버 아이디</param>
		/// <param name="Name">검색할 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{ExceptId}/{Name}")]
		public async Task<ActionResult> IsServerNameExist([FromRoute] string ExceptId, [FromRoute] string Name)
		{
			return Json(await m_dataProvider.IsNameExist(ExceptId, Name));
		}

		#endregion

		#region 네트워크 인터페이스 관련

		/// <summary>네트워크 인터페이스 정보를 추가한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Request">네트워크 인터페이스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseNetworkInterface>))]
		[HttpPost("{ServerId}/NetworkInterfaces")]
		public async Task<ActionResult> AddNetworkInterface([FromRoute] string ServerId, [FromBody] RequestNetworkInterface Request)
		{
			return Json(await m_networkInterfaceProvider.Add(ServerId, Request));
		}

		/// <summary>네트워크 인터페이스 정보를 수정한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <param name="Request">네트워크 인터페이스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{ServerId}/NetworkInterfaces/{Id}")]
		public async Task<ActionResult> UpdateNetworkInterface([FromRoute] string ServerId, [FromRoute] string Id, [FromBody] RequestNetworkInterface Request)
		{
			return Json(await m_networkInterfaceProvider.Update(ServerId, Id, Request));
		}

		/// <summary>네트워크 인터페이스 상태를 수정한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <param name="State">네트워크 인터페이스 링크 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{ServerId}/NetworkInterfaces/{Id}/LinkStatus/{State}")]
		public async Task<ActionResult> UpdateNetworkInterfaceLinkState([FromRoute] string ServerId, [FromRoute] string Id, [FromRoute] EnumNetworkLinkState State)
		{
			return Json(await m_networkInterfaceProvider.UpdateLinkState(ServerId, Id, State));
		}

		/// <summary>네트워크 인터페이스 상태를 수정한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Request">네트워크 인터페이스 링크 상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("NetworkInterfaces/LinkStatus")]
		public async Task<ActionResult> UpdateNetworkInterfaceLinkState([FromRoute] string ServerId, [FromBody] RequestNetworkInterfaceLinkState Request)
		{
			return Json(await m_networkInterfaceProvider.UpdateLinkState(Request));
		}

		/// <summary>네트워크 인터페이스 사용 정보 수정</summary>
		/// <param name="Request">네트워크 인터페이스 사용 정보 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("NetworkInterfaces/Usage")]
		public async Task<ActionResult> UpdateNetworkInterfaceUsage([FromBody] RequestNetworkInterfaceUsage Request)
		{
			return Json(await m_networkInterfaceProvider.UpdateUsage(Request));
		}

		/// <summary>네트워크 인터페이스 정보를 삭제한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">네트워크 인터페이스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{ServerId}/NetworkInterfaces/{Id}")]
		public async Task<ActionResult> RemoveNetworkInterface([FromRoute] string ServerId, [FromRoute] string Id)
		{
			return Json(await m_networkInterfaceProvider.Remove(ServerId, Id));
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
		[HttpGet("{ServerId}/NetworkInterfaces")]
		public async Task<ActionResult> GetNetworkInterfaces(
			[FromRoute] string ServerId
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			return Json(await m_networkInterfaceProvider.GetList(
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
		[HttpGet("{ServerId}/NetworkInterfaces/{Id}")]
		public async Task<ActionResult> GetNetworkInterface([FromRoute] string ServerId, [FromRoute] string Id)
		{
			return Json(await m_networkInterfaceProvider.Get(ServerId, Id));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Request">특정 이름의 네트워크 인터페이스 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{ServerId}/NetworkInterfaces/Exist")]
		public async Task<ActionResult> IsNetworkInterfaceNameExist([FromRoute] string ServerId, [FromBody] RequestIsNetworkInterfaceNameExist Request)
		{
			return Json(await m_networkInterfaceProvider.IsNameExist(ServerId, null, Request));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="ExceptId">이름 검색 시 제외할 네트워크 인터페이스 아이디</param>
		/// <param name="Request">특정 이름의 네트워크 인터페이스 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{ServerId}/NetworkInterfaces/Exist/{ExceptId}")]
		public async Task<ActionResult> IsNetworkInterfaceNameExist([FromRoute] string ServerId, [FromRoute] string ExceptId, [FromBody] RequestIsNetworkInterfaceNameExist Request)
		{
			return Json(await m_networkInterfaceProvider.IsNameExist(ServerId, ExceptId, Request));
		}

		#endregion

		#region 네트워크 인터페이스 VLAN 관련

		/// <summary>네트워크 인터페이스 VLAN 정보를 추가한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Request">네트워크 인터페이스 VLAN 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseNetworkInterfaceVlan>))]
		[HttpPost("{ServerId}/NetworkInterfaces/{InterfaceId}/Vlans")]
		public async Task<ActionResult> AddNetworkInterfaceVlan([FromRoute] string ServerId, [FromRoute] string InterfaceId, [FromBody] RequestNetworkInterfaceVlan Request)
		{
			return Json(await m_networkInterfaceVlanProvider.Add(ServerId, InterfaceId, Request));
		}

		/// <summary>네트워크 인터페이스 VLAN 정보를 수정한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Id">네트워크 인터페이스 VLAN 아이디</param>
		/// <param name="Request">네트워크 인터페이스 VLAN 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{ServerId}/NetworkInterfaces/{InterfaceId}/Vlans/{Id}")]
		public async Task<ActionResult> UpdateNetworkInterfaceVlan([FromRoute] string ServerId, [FromRoute] string InterfaceId, [FromRoute] string Id, [FromBody] RequestNetworkInterfaceVlan Request)
		{
			return Json(await m_networkInterfaceVlanProvider.Update(ServerId, InterfaceId, Id, Request));
		}

		/// <summary>네트워크 인터페이스 VLAN 정보를 삭제한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Id">네트워크 인터페이스 VLAN 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{ServerId}/NetworkInterfaces/{InterfaceId}/Vlans/{Id}")]
		public async Task<ActionResult> RemoveNetworkInterfaceVlan([FromRoute] string ServerId, [FromRoute] string InterfaceId, [FromRoute] string Id)
		{
			return Json(await m_networkInterfaceVlanProvider.Remove(ServerId, InterfaceId, Id));
		}

		/// <summary>네트워크 인터페이스 VLAN 목록을 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseNetworkInterfaceVlan>))]
		[HttpGet("{ServerId}/NetworkInterfaces/{InterfaceId}/Vlans")]
		public async Task<ActionResult> GetNetworkInterfaceVlans(
			[FromRoute] string ServerId, [FromRoute] string InterfaceId
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			return Json(await m_networkInterfaceVlanProvider.GetList(
				ServerId, InterfaceId
				, Skip, CountPerPage
				, OrderFields, OrderDirections
				, SearchFields, SearchKeyword
			));
		}

		/// <summary>특정 네트워크 인터페이스 VLAN 정보를 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Id">네트워크 인터페이스 VLAN 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseNetworkInterfaceVlan>))]
		[HttpGet("{ServerId}/NetworkInterfaces/{InterfaceId}/Vlans/{Id}")]
		public async Task<ActionResult> GetNetworkInterfaceVlan([FromRoute] string ServerId, [FromRoute] string InterfaceId, [FromRoute] string Id)
		{
			return Json(await m_networkInterfaceVlanProvider.Get(ServerId, InterfaceId, Id));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="Request">특정 VLAN 태그 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{ServerId}/NetworkInterfaces/{InterfaceId}/Vlans/Exist")]
		public async Task<ActionResult> IsNetworkInterfaceVlanTagExist([FromRoute] string ServerId, [FromRoute] string InterfaceId, [FromBody] RequestIsNetworkInterfaceVlanExist Request)
		{
			return Json(await m_networkInterfaceVlanProvider.IsTagExist(ServerId, InterfaceId, null, Request));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="InterfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="ExceptId">이름 검색 시 제외할 네트워크 인터페이스 VLAN 아이디</param>
		/// <param name="Request">특정 VLAN 태그 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{ServerId}/NetworkInterfaces/{InterfaceId}/Vlans/Exist/{ExceptId}")]
		public async Task<ActionResult> IsNetworkInterfaceVlanTagExist([FromRoute] string ServerId, [FromRoute] string InterfaceId, [FromRoute] string ExceptId, [FromBody] RequestIsNetworkInterfaceVlanExist Request)
		{
			return Json(await m_networkInterfaceVlanProvider.IsTagExist(ServerId, InterfaceId, ExceptId, Request));
		}

		/// <summary>서버 타임아웃 임계값을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<long>))]
		[HttpGet("Threshold")]
		public async Task<ActionResult> GetThreshold()
		{
			return Json(await m_dataProvider.GetThreshold());
		}

		/// <summary>서버 타임아웃 임계값을 설정한다.</summary>
		/// <param name="Timeout">임계값 정보 객체(millisecond)</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Threshold")]
		public async Task<ActionResult> SetThreshold([FromBody] long Timeout)
		{
			return Json(await m_dataProvider.SetThreshold(Timeout));
		}
		#endregion
	}
}