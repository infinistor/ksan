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
	/// <summary>디스크 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class DisksController : BaseController
	{
		/// <summary>디스크 데이터 프로바이더</summary>
		private readonly IDiskProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">디스크 데이터 프로바이더</param>
		public DisksController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<DisksController> logger,
			IDiskProvider dataProvider
		)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>디스크 정보를 추가한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Request">디스크 등록 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskWithServerAndNetwork>))]
		[HttpPost("{ServerId}")]
		public async Task<ActionResult> Add([FromRoute] string ServerId, [FromBody] RequestDisk Request)
		{
			return Json(await m_dataProvider.Add(ServerId, Request));
		}

		/// <summary>디스크 정보를 수정한다.</summary>
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <param name="Request">디스크 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}")]
		public async Task<ActionResult> Update([FromRoute] string Id, [FromBody] RequestDisk Request)
		{
			return Json(await m_dataProvider.Update(Id, Request));
		}

		/// <summary>디스크 상태를 수정한다.</summary>
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <param name="State">디스크 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}/State/{State}")]
		public async Task<ActionResult> UpdateState([FromRoute] string Id, [FromRoute] EnumDiskState State)
		{
			return Json(await m_dataProvider.UpdateState(Id, State));
		}

		/// <summary>디스크 읽기/쓰기 모드를 수정한다.</summary>
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <param name="DiskRwMode">디스크 읽기/쓰기 모드</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{Id}/RwMode/{DiskRwMode}")]
		public async Task<ActionResult> UpdateRwMode([FromRoute] string Id, [FromRoute] EnumDiskRwMode DiskRwMode)
		{
			return Json(await m_dataProvider.UpdateRwMode(Id, DiskRwMode));
		}

		/// <summary>디스크 사용정보를 수정한다.</summary>
		/// <param name="Request">디스크 사용 정보</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Usage")]
		public async Task<ActionResult> UpdateUsage([FromBody] RequestDiskUsage Request)
		{
			return Json(await m_dataProvider.UpdateUsage(Request));
		}

		/// <summary>디스크 정보를 삭제한다.</summary>
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{Id}")]
		public async Task<ActionResult> Remove( [FromRoute] string Id)
		{
			return Json(await m_dataProvider.Remove(Id));
		}

		/// <summary>전체 디스크 목록을 가져온다.</summary>
		/// <param name="SearchStates">검색할 디스크 상태 목록</param>
		/// <param name="SearchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDisk>))]
		[HttpGet]
		public async Task<ActionResult> GetList(
			List<EnumDiskState> SearchStates, List<EnumDiskRwMode> SearchRwModes,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetList(
				SearchStates, SearchRwModes,
				Skip, CountPerPage,
				OrderFields, OrderDirections,
				SearchFields, SearchKeyword
			));
		}

		/// <summary>디스크 목록을 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="SearchStates">검색할 디스크 상태 목록</param>
		/// <param name="SearchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDisk>))]
		[HttpGet("{ServerId}")]
		public async Task<ActionResult> Get(
			[FromRoute] string ServerId,
			List<EnumDiskState> SearchStates, List<EnumDiskRwMode> SearchRwModes,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetList(
				ServerId,
				SearchStates, SearchRwModes,
				Skip, CountPerPage,
				OrderFields, OrderDirections,
				SearchFields, SearchKeyword
			));
		}

		/// <summary>특정 디스크 정보를 가져온다.</summary>
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskWithServerAndNetwork>))]
		[HttpGet("Detail/{Id}")]
		public async Task<ActionResult> Get([FromRoute] string Id)
		{
			return Json(await m_dataProvider.Get(Id));
		}

		/// <summary>디스크 임계값을 가져온다.</summary>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskThreshold>))]
		[HttpGet("Threshold")]
		public async Task<ActionResult> GetThreshold()
		{
			return Json(await m_dataProvider.GetThreshold());
		}

		/// <summary>디스크 임계값을 설정한다.</summary>
		/// <param name="Request">임계값 정보 객체(Byte)</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Threshold")]
		public async Task<ActionResult> SetThreshold([FromBody] RequestDiskThreshold Request)
		{
			return Json(await m_dataProvider.SetThreshold(Request));
		}
	}
}