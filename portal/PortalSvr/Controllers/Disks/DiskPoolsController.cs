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
using PortalData.Requests.Disks;
using PortalData.Responses.Disks;
using PortalProviderInterface;
using PortalSvr.Services;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using Swashbuckle.AspNetCore.Annotations;

namespace PortalSvr.Controllers.Disks
{
	/// <summary>디스크 풀 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class DiskPoolsController : BaseController
	{

		/// <summary>디스크 풀 데이터 프로바이더</summary>
		private readonly IDiskPoolProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public DiskPoolsController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<DiskPoolsController> logger,
			IDiskPoolProvider dataProvider
		)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>디스크 풀 정보를 추가한다.</summary>
		/// <param name="request">디스크 풀 등록 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskPoolWithDisks>))]
		[HttpPost]
		public async Task<ActionResult> Add([FromBody] RequestDiskPool request)
		{
			return Json(await m_dataProvider.Add(request));
		}

		/// <summary>디스크 풀 정보를 수정한다.</summary>
		/// <param name="id">디스크 풀 아이디</param>
		/// <param name="request">디스크 풀 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}")]
		public async Task<ActionResult> Update([FromRoute] string id, [FromBody] RequestDiskPool request)
		{
			return Json(await m_dataProvider.Update(id, request));
		}

		/// <summary>디스크 풀 정보를 삭제한다.</summary>
		/// <param name="id">디스크 풀 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{id}")]
		public async Task<ActionResult> Remove([FromRoute] string id)
		{
			return Json(await m_dataProvider.Remove(id));
		}

		/// <summary>디스크 풀 목록을 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDiskPool>))]
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
		
		/// <summary>특정 디스크 풀 정보를 가져온다.</summary>
		/// <param name="id">디스크 풀 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskPoolWithDisks>))]
		[HttpGet("{id}")]
		public async Task<ActionResult> Get([FromRoute] string id)
		{
			return Json(await m_dataProvider.Get(id));
		}
		
		/// <summary>특정 이름의 디스크 풀이 존재하는지 확인한다.</summary>
		/// <param name="request">특정 이름의 디스크 풀 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist")]
		public async Task<ActionResult> IsNameExist([FromBody] RequestIsDiskPoolNameExist request)
		{
			return Json(await m_dataProvider.IsNameExist(null, request));
		}
		
		/// <summary>특정 이름의 디스크 풀이 존재하는지 확인한다.</summary>
		/// <param name="exceptId">이름 검색 시 제외할 디스크 풀 아이디</param>
		/// <param name="request">특정 이름의 디스크 풀 존재여부 확인 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{exceptId}")]
		public async Task<ActionResult> IsNameExist([FromRoute] string exceptId, [FromBody] RequestIsDiskPoolNameExist request)
		{
			return Json(await m_dataProvider.IsNameExist(exceptId, request));
		}

		/// <summary>해당 디스크 타입으로 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDisk>))]
		[HttpGet("AvailableDisks")]
		public async Task<ActionResult> GetAvailableDisks(
			int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "")
		{
			return Json(await m_dataProvider.GetAvailableDisks(
				skip, countPerPage
				, orderFields, orderDirections
				, searchFields, searchKeyword));
		}

		/// <summary>주어진 디스크 풀 아이디에 해당하는 디스크 풀에 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="id">디스크 풀 아이디 (null인 경우, 어느 풀에도 속하지 않은 디스크만 검색한다.)</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDisk>))]
		[HttpGet("{id}/AvailableDisks")]
		public async Task<ActionResult> GetAvailableDisks(
			[FromRoute] string id
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "")
		{
			return Json(await m_dataProvider.GetAvailableDisks(
				id
				, skip, countPerPage
				, orderFields, orderDirections
				, searchFields, searchKeyword));
		}
	}
}