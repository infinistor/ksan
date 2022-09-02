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
using System.Threading.Tasks;
using PortalData;
using PortalProviderInterface;
using PortalSvr.Services;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using PortalData.Requests.Region;
using Swashbuckle.AspNetCore.Annotations;
using System.Net;

namespace PortalSvr.Controllers.Regions
{
	/// <summary>리전 관련 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class RegionsController : BaseController
	{
		/// <summary>데이터 프로바이더</summary>
		private readonly IRegionProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">리전 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public RegionsController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<RegionsController> logger,
			IRegionProvider dataProvider
			)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>리전 목록을 반환한다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<RequestRegion>))]
		[HttpGet]
		public async Task<ActionResult> Get(
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null
		)
		{
			return Json(await m_dataProvider.GetList(Skip, CountPerPage, OrderFields, OrderDirections));
		}

		/// <summary>리전 식별자로 특정 리전을 가져온다.</summary>
		/// <param name="RegionName">리전 식별자</param>
		/// <returns>리전 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<RequestRegion>))]
		[HttpGet("{RegionName}")]
		public async Task<ActionResult> Get([FromRoute] string RegionName)
		{
			return Json(await m_dataProvider.Get(RegionName));
		}

		/// <summary>리전을 생성한다.</summary>
		/// <param name="Request">리전 정보</param>
		/// <returns>리전 생성 결과</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<RequestRegion>))]
		[HttpPost]
		public async Task<ActionResult> Add([FromBody] RequestRegion Request)
		{
			return Json(await m_dataProvider.Add(Request));
		}

		/// <summary>리전을 생성한다.</summary>
		/// <param name="Request">리전 정보</param>
		/// <returns>리전 생성 결과</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("Sync")]
		public async Task<ActionResult> Sync([FromBody] List<RequestRegionSync> Request)
		{
			return Json(await m_dataProvider.Sync(Request));
		}

		/// <summary>리전을 삭제한다.</summary>
		/// <param name="RegionName">리전 식별자</param>
		/// <returns>리전 삭제 결과</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete]
		public async Task<ActionResult> Remove([FromRoute] string RegionName)
		{
			return Json(await m_dataProvider.Remove(RegionName));
		}
	}
}
