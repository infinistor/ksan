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
using PortalData.Requests.Disks;
using PortalData.Requests.Networks;
using PortalData.Requests.Servers;
using PortalData.Responses.Disks;
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

		/// <summary>디스크 데이터 프로바이더</summary>
		private readonly IDiskProvider m_diskProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		/// <param name="networkInterfaceProvider">네트워크 인터페이스 데이터 프로바이더</param>
		/// <param name="networkInterfaceVlanProvider">네트워크 인터페이스 VLAN 데이터 프로바이더</param>
		/// <param name="diskProvider">디스크 데이터 프로바이더</param>
		public ServersController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<ServersController> logger,
			IServerProvider dataProvider,
			INetworkInterfaceProvider networkInterfaceProvider,
			INetworkInterfaceVlanProvider networkInterfaceVlanProvider,
			IDiskProvider diskProvider
		)
			: base(configuration, userManager, logger, dataProvider, networkInterfaceProvider, networkInterfaceVlanProvider)
		{
			m_dataProvider = dataProvider;
			m_networkInterfaceProvider = networkInterfaceProvider;
			m_networkInterfaceVlanProvider = networkInterfaceVlanProvider;
			m_diskProvider = diskProvider;
		}

		#region 서버 관련

		/// <summary>서버 정보를 추가한다.</summary>
		/// <param name="request">서버 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServer>))]
		[HttpPost]
		public async Task<ActionResult> AddServer([FromBody] RequestServer request)
		{
			return Json(await m_dataProvider.Add(request));
		}

		/// <summary>서버 정보를 수정한다.</summary>
		/// <param name="id">서버 아이디</param>
		/// <param name="request">서버 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}")]
		public async Task<ActionResult> UpdateServer([FromRoute] string id, [FromBody] RequestServer request)
		{
			return Json(await m_dataProvider.Update(id, request));
		}

		/// <summary>서버 상태 수정</summary>
		/// <param name="id">서버 아이디</param>
		/// <param name="state">서버 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}/State/{state}")]
		public async Task<ActionResult> UpdateServerState([FromRoute] string id, [FromRoute] EnumServerState state)
		{
			return Json(await m_dataProvider.UpdateState(id, state));
		}

		/// <summary>서버 상태 수정</summary>
		/// <param name="request">서버 상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("State")]
		public async Task<ActionResult> UpdateServerState([FromBody] RequestServerState request)
		{
			return Json(await m_dataProvider.UpdateState(request));
		}

		/// <summary>서버 사용 정보 수정</summary>
		/// <param name="request">서버 사용 정보 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Usage")]
		public async Task<ActionResult> UpdateServerUsage([FromBody] RequestServerUsage request)
		{
			return Json(await m_dataProvider.UpdateUsage(request));
		}

		/// <summary>서버 정보를 삭제한다.</summary>
		/// <param name="id">서버 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{id}")]
		public async Task<ActionResult> RemoveServer([FromRoute] string id)
		{
			return Json(await m_dataProvider.Remove(id));
		}

		/// <summary>서버 목록을 가져온다.</summary>
		/// <param name="searchStates">검색할 서버 상태 목록</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseServer>))]
		[HttpGet]
		public async Task<ActionResult> GetServers(
			List<EnumServerState> searchStates,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetList(
				searchStates,
				skip, countPerPage,
				orderFields, orderDirections,
				searchFields, searchKeyword
			));
		}

		/// <summary>특정 서버 정보를 가져온다.</summary>
		/// <param name="id">서버 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseServer>))]
		[HttpGet("{id}")]
		public async Task<ActionResult> GetServer([FromRoute] string id)
		{
			return Json(await m_dataProvider.Get(id));
		}

		/// <summary>특정 이름의 서버가 존재하는지 확인한다.</summary>
		/// <param name="request">특정 이름의 서버 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist")]
		public async Task<ActionResult> IsServerNameExist([FromBody] RequestIsServerNameExist request)
		{
			return Json(await m_dataProvider.IsNameExist(null, request));
		}

		/// <summary>특정 이름의 서버가 존재하는지 확인한다.</summary>
		/// <param name="exceptId">이름 검색 시 제외할 서버 아이디</param>
		/// <param name="request">특정 이름의 서버 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{exceptId}")]
		public async Task<ActionResult> IsServerNameExist([FromRoute] string exceptId, [FromBody] RequestIsServerNameExist request)
		{
			return Json(await m_dataProvider.IsNameExist(exceptId, request));
		}

		#endregion

		#region 네트워크 인터페이스 관련

		/// <summary>네트워크 인터페이스 정보를 추가한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="request">네트워크 인터페이스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseNetworkInterface>))]
		[HttpPost("{serverId}/NetworkInterfaces")]
		public async Task<ActionResult> AddNetworkInterface([FromRoute] string serverId, [FromBody] RequestNetworkInterface request)
		{
			return Json(await m_networkInterfaceProvider.Add(serverId, request));
		}

		/// <summary>네트워크 인터페이스 정보를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <param name="request">네트워크 인터페이스 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{serverId}/NetworkInterfaces/{id}")]
		public async Task<ActionResult> UpdateNetworkInterface([FromRoute] string serverId, [FromRoute] string id, [FromBody] RequestNetworkInterface request)
		{
			return Json(await m_networkInterfaceProvider.Update(serverId, id, request));
		}

		/// <summary>네트워크 인터페이스 상태를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <param name="state">네트워크 인터페이스 링크 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{serverId}/NetworkInterfaces/{id}/LinkStatus/{state}")]
		public async Task<ActionResult> UpdateNetworkInterfaceLinkState([FromRoute] string serverId, [FromRoute] string id, [FromRoute] EnumNetworkLinkState state)
		{
			return Json(await m_networkInterfaceProvider.UpdateLinkState(serverId, id, state));
		}

		/// <summary>네트워크 인터페이스 상태를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="request">네트워크 인터페이스 링크 상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("NetworkInterfaces/LinkStatus")]
		public async Task<ActionResult> UpdateNetworkInterfaceLinkState([FromRoute] string serverId, [FromBody] RequestNetworkInterfaceLinkState request)
		{
			return Json(await m_networkInterfaceProvider.UpdateLinkState(request));
		}

		/// <summary>네트워크 인터페이스 사용 정보 수정</summary>
		/// <param name="request">네트워크 인터페이스 사용 정보 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("NetworkInterfaces/Usage")]
		public async Task<ActionResult> UpdateNetworkInterfaceUsage([FromBody] RequestNetworkInterfaceUsage request)
		{
			return Json(await m_networkInterfaceProvider.UpdateUsage(request));
		}

		/// <summary>네트워크 인터페이스 정보를 삭제한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{serverId}/NetworkInterfaces/{id}")]
		public async Task<ActionResult> RemoveNetworkInterface([FromRoute] string serverId, [FromRoute] string id)
		{
			return Json(await m_networkInterfaceProvider.Remove(serverId, id));
		}

		/// <summary>네트워크 인터페이스 목록을 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseNetworkInterface>))]
		[HttpGet("{serverId}/NetworkInterfaces")]
		public async Task<ActionResult> GetNetworkInterfaces(
			[FromRoute] string serverId
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = ""
		)
		{
			return Json(await m_networkInterfaceProvider.GetList(
				serverId
				, skip, countPerPage
				, orderFields, orderDirections
				, searchFields, searchKeyword
			));
		}

		/// <summary>특정 네트워크 인터페이스 정보를 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">네트워크 인터페이스 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseNetworkInterface>))]
		[HttpGet("{serverId}/NetworkInterfaces/{id}")]
		public async Task<ActionResult> GetNetworkInterface([FromRoute] string serverId, [FromRoute] string id)
		{
			return Json(await m_networkInterfaceProvider.Get(serverId, id));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="request">특정 이름의 네트워크 인터페이스 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{serverId}/NetworkInterfaces/Exist")]
		public async Task<ActionResult> IsNetworkInterfaceNameExist([FromRoute] string serverId, [FromBody] RequestIsNetworkInterfaceNameExist request)
		{
			return Json(await m_networkInterfaceProvider.IsNameExist(serverId, null, request));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="exceptId">이름 검색 시 제외할 네트워크 인터페이스 아이디</param>
		/// <param name="request">특정 이름의 네트워크 인터페이스 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{serverId}/NetworkInterfaces/Exist/{exceptId}")]
		public async Task<ActionResult> IsNetworkInterfaceNameExist([FromRoute] string serverId, [FromRoute] string exceptId, [FromBody] RequestIsNetworkInterfaceNameExist request)
		{
			return Json(await m_networkInterfaceProvider.IsNameExist(serverId, exceptId, request));
		}

		#endregion

		#region 네트워크 인터페이스 VLAN 관련

		/// <summary>네트워크 인터페이스 VLAN 정보를 추가한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="request">네트워크 인터페이스 VLAN 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseNetworkInterfaceVlan>))]
		[HttpPost("{serverId}/NetworkInterfaces/{interfaceId}/Vlans")]
		public async Task<ActionResult> AddNetworkInterfaceVlan([FromRoute] string serverId, [FromRoute] string interfaceId, [FromBody] RequestNetworkInterfaceVlan request)
		{
			return Json(await m_networkInterfaceVlanProvider.Add(serverId, interfaceId, request));
		}

		/// <summary>네트워크 인터페이스 VLAN 정보를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="id">네트워크 인터페이스 VLAN 아이디</param>
		/// <param name="request">네트워크 인터페이스 VLAN 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{serverId}/NetworkInterfaces/{interfaceId}/Vlans/{id}")]
		public async Task<ActionResult> UpdateNetworkInterfaceVlan([FromRoute] string serverId, [FromRoute] string interfaceId, [FromRoute] string id, [FromBody] RequestNetworkInterfaceVlan request)
		{
			return Json(await m_networkInterfaceVlanProvider.Update(serverId, interfaceId, id, request));
		}

		/// <summary>네트워크 인터페이스 VLAN 정보를 삭제한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="id">네트워크 인터페이스 VLAN 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{serverId}/NetworkInterfaces/{interfaceId}/Vlans/{id}")]
		public async Task<ActionResult> RemoveNetworkInterfaceVlan([FromRoute] string serverId, [FromRoute] string interfaceId, [FromRoute] string id)
		{
			return Json(await m_networkInterfaceVlanProvider.Remove(serverId, interfaceId, id));
		}

		/// <summary>네트워크 인터페이스 VLAN 목록을 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseNetworkInterfaceVlan>))]
		[HttpGet("{serverId}/NetworkInterfaces/{interfaceId}/Vlans")]
		public async Task<ActionResult> GetNetworkInterfaceVlans(
			[FromRoute] string serverId, [FromRoute] string interfaceId
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = ""
		)
		{
			return Json(await m_networkInterfaceVlanProvider.GetList(
				serverId, interfaceId
				, skip, countPerPage
				, orderFields, orderDirections
				, searchFields, searchKeyword
			));
		}

		/// <summary>특정 네트워크 인터페이스 VLAN 정보를 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="id">네트워크 인터페이스 VLAN 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseNetworkInterfaceVlan>))]
		[HttpGet("{serverId}/NetworkInterfaces/{interfaceId}/Vlans/{id}")]
		public async Task<ActionResult> GetNetworkInterfaceVlan([FromRoute] string serverId, [FromRoute] string interfaceId, [FromRoute] string id)
		{
			return Json(await m_networkInterfaceVlanProvider.Get(serverId, interfaceId, id));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="request">특정 VLAN 태그 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{serverId}/NetworkInterfaces/{interfaceId}/Vlans/Exist")]
		public async Task<ActionResult> IsNetworkInterfaceVlanTagExist([FromRoute] string serverId, [FromRoute] string interfaceId, [FromBody] RequestIsNetworkInterfaceVlanExist request)
		{
			return Json(await m_networkInterfaceVlanProvider.IsTagExist(serverId, interfaceId, null, request));
		}

		/// <summary>특정 이름의 네트워크 인터페이스가 존재하는지 확인한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="interfaceId">네트워크 인터페이스 아이디</param>
		/// <param name="exceptId">이름 검색 시 제외할 네트워크 인터페이스 VLAN 아이디</param>
		/// <param name="request">특정 VLAN 태그 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("{serverId}/NetworkInterfaces/{interfaceId}/Vlans/Exist/{exceptId}")]
		public async Task<ActionResult> IsNetworkInterfaceVlanTagExist([FromRoute] string serverId, [FromRoute] string interfaceId, [FromRoute] string exceptId, [FromBody] RequestIsNetworkInterfaceVlanExist request)
		{
			return Json(await m_networkInterfaceVlanProvider.IsTagExist(serverId, interfaceId, exceptId, request));
		}

		#endregion

		#region 디스크 관련

		/// <summary>디스크 정보를 추가한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="request">디스크 등록 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDisk>))]
		[HttpPost("{serverId}/Disks")]
		public async Task<ActionResult> AddDisk([FromRoute] string serverId, [FromBody] RequestDisk request)
		{
			return Json(await m_diskProvider.Add(serverId, request));
		}

		/// <summary>디스크 정보를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="request">디스크 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{serverId}/Disks/{id}")]
		public async Task<ActionResult> UpdateDisk([FromRoute] string serverId, [FromRoute] string id, [FromBody] RequestDisk request)
		{
			return Json(await m_diskProvider.Update(serverId, id, request));
		}

		/// <summary>디스크 상태를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="state">디스크 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{serverId}/Disks/{id}/State/{state}")]
		public async Task<ActionResult> UpdateDiskState([FromRoute] string serverId, [FromRoute] string id, [FromRoute] EnumDiskState state)
		{
			return Json(await m_diskProvider.UpdateState(serverId, id, state));
		}

		/// <summary>디스크 읽기/쓰기 모드를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="diskRwMode">디스크 읽기/쓰기 모드</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{serverId}/Disks/{id}/RwMode/{diskRwMode}")]
		public async Task<ActionResult> UpdateDiskRwMode([FromRoute] string serverId, [FromRoute] string id, [FromRoute] EnumDiskRwMode diskRwMode)
		{
			return Json(await m_diskProvider.UpdateRwMode(serverId, id, diskRwMode));
		}

		/// <summary>디스크 정보를 삭제한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{serverId}/Disks/{id}")]
		public async Task<ActionResult> RemoveDisk([FromRoute] string serverId, [FromRoute] string id)
		{
			return Json(await m_diskProvider.Remove(serverId, id));
		}

		/// <summary>디스크 상태를 수정한다.</summary>
		/// <param name="request">상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Disks/State")]
		public async Task<ActionResult> UpdateDiskState([FromBody] RequestDiskState request)
		{
			return Json(await m_diskProvider.UpdateState(request));
		}

		/// <summary>디스크 크기를 수정한다.</summary>
		/// <param name="request">디스크 크기 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Disks/Size")]
		public async Task<ActionResult> UpdateDiskSize([FromBody] RequestDiskSize request)
		{
			return Json(await m_diskProvider.UpdateSize(request));
		}

		/// <summary>디스크 읽기/쓰기 모드를 수정한다.</summary>
		/// <param name="request">디스크 읽기/쓰기 모드 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Disks/RwMode")]
		public async Task<ActionResult> UpdateDiskRwMode([FromBody] RequestDiskRwMode request)
		{
			return Json(await m_diskProvider.UpdateRwMode(request));
		}

		/// <summary>디스크 목록을 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="searchStates">검색할 디스크 상태 목록</param>
		/// <param name="searchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDisk>))]
		[HttpGet("{serverId}/Disks")]
		public async Task<ActionResult> GetDisks(
			[FromRoute] string serverId,
			List<EnumDiskState> searchStates, List<EnumDiskRwMode> searchRwModes,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			return Json(await m_diskProvider.GetList(
				serverId,
				searchStates, searchRwModes,
				skip, countPerPage,
				orderFields, orderDirections,
				searchFields, searchKeyword
			));
		}

		/// <summary>특정 디스크 정보를 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskWithServices>))]
		[HttpGet("{serverId}/Disks/{id}")]
		public async Task<ActionResult> GetDisk([FromRoute] string serverId, [FromRoute] string id)
		{
			return Json(await m_diskProvider.Get(serverId, id));
		}

		#endregion
	}
}