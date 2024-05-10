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
		/// <param name="Request">디스크 풀 등록 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskPoolWithDisks>))]
		[HttpPost]
		public async Task<ActionResult> Add([FromBody] RequestDiskPool Request)
		{
			return Json(await m_dataProvider.Add(Request));
		}

		/// <summary>디스크 풀 정보를 수정한다.</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <param name="Request">디스크 풀 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}")]
		public async Task<ActionResult> Update([FromRoute] string Id, [FromBody] RequestDiskPool Request)
		{
			return Json(await m_dataProvider.Update(Id, Request));
		}

		/// <summary>디스크 풀 정보를 삭제한다.</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{Id}")]
		public async Task<ActionResult> Remove([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Remove(Id));
		}

		/// <summary>디스크 풀 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDiskPool>))]
		[HttpGet]
		public async Task<ActionResult> GetList(
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = null
		)
		{
			return Json(await m_dataProvider.GetList(
				Skip, CountPerPage
				, OrderFields, OrderDirections
				, SearchFields, SearchKeyword
			));
		}

		/// <summary>디스크 풀 목록을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDiskPoolWithDisks>))]
		[HttpGet("Details")]
		public async Task<ActionResult> GetListDetails()
		{
			return Json(await m_dataProvider.GetListDetails());
		}

		/// <summary>특정 디스크 풀 정보를 가져온다.</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskPoolWithDisks>))]
		[HttpGet("{Id}")]
		public async Task<ActionResult> Get([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Get(Id));
		}

		/// <summary>특정 이름의 디스크 풀이 존재하는지 확인한다.</summary>
		/// <param name="Name">검색할 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{Name}")]
		public async Task<ActionResult> IsNameExist([FromRoute] string Name)
		{
			return Json(await m_dataProvider.IsNameExist(null, Name));
		}

		/// <summary>특정 이름의 디스크 풀이 존재하는지 확인한다.</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 디스크 풀 아이디</param>
		/// <param name="Name">검색할 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<bool>))]
		[HttpPost("Exist/{ExceptId}/{Name}")]
		public async Task<ActionResult> IsNameExist([FromRoute] string ExceptId, [FromRoute] string Name)
		{
			return Json(await m_dataProvider.IsNameExist(ExceptId, Name));
		}

		/// <summary>기본 디스크풀을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskPool>))]
		[HttpGet("Default")]
		public async Task<ActionResult> GetDefault()
		{
			return Json(await m_dataProvider.GetDefault());
		}

		/// <summary>기본 디스크풀을 변경한다.</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Default/{Id}")]
		public async Task<ActionResult> SetDefault([FromRoute] string Id)
		{
			return Json(await m_dataProvider.SetDefault(Id));
		}

		/// <summary>해당 디스크 타입으로 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDisk>))]
		[HttpGet("AvailableDisks")]
		public async Task<ActionResult> GetAvailableDisks(
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = null)
		{
			return Json(await m_dataProvider.GetAvailableDisks(
				Skip, CountPerPage
				, OrderFields, OrderDirections
				, SearchFields, SearchKeyword));
		}

		/// <summary>주어진 디스크 풀 아이디에 해당하는 디스크 풀에 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="Id">디스크 풀 아이디 (null인 경우, 어느 풀에도 속하지 않은 디스크만 검색한다.)</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDisk>))]
		[HttpGet("{Id}/AvailableDisks")]
		public async Task<ActionResult> GetAvailableDisks(
			[FromRoute] string Id
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = null)
		{
			return Json(await m_dataProvider.GetAvailableDisks(
				Id
				, Skip, CountPerPage
				, OrderFields, OrderDirections
				, SearchFields, SearchKeyword));
		}
		
		/// <summary>디스크 풀에 디스크를 할당한다.</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <param name="Request">디스크 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("Disks/{Id}")]
		public async Task<ActionResult> AddDisks([FromRoute] string Id, [FromBody] RequestDisks Request)
		{
			return Json(await m_dataProvider.AddDisks(Id, Request));
		}
		
		/// <summary>디스크 풀에 디스크를 할당한다.</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <param name="Request">디스크 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Disks/{Id}")]
		public async Task<ActionResult> UpdateDisks([FromRoute] string Id, [FromBody] RequestDisks Request)
		{
			return Json(await m_dataProvider.UpdateDisks(Id, Request));
		}
		
		/// <summary>디스크 풀에 디스크를 제거한다.</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <param name="Request">디스크 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("Disks/{Id}")]
		public async Task<ActionResult> RemoveDisks([FromRoute] string Id, [FromBody] RequestDisks Request)
		{
			return Json(await m_dataProvider.RemoveDisks(Id, Request));
		}
	}
}