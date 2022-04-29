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
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;
using PortalProviderInterface;
using PortalSvr.Services;
using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using Swashbuckle.AspNetCore.Annotations;

namespace PortalSvr.Controllers.Accounts
{
	/// <summary>사용자 관련 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class S3UsersController : BaseController
	{
		/// <summary>데이터 프로바이더</summary>
		private readonly IS3UserProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public S3UsersController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<S3UsersController> logger,
			IS3UserProvider dataProvider
			)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>사용자 목록을 반환한다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드목록 (LoginId, Email, Name)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.list")]
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseS3User>))]
		[HttpGet]
		public async Task<ActionResult> Get(int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetUsers(skip, countPerPage, orderFields, orderDirections, searchFields, searchKeyword));
		}

		/// <summary>특정 사용자 정보를 반환한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.view")]
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseS3User>))]
		[HttpGet("{id}")]
		public async Task<ActionResult> Get([FromRoute] string id)
		{
			return Json(await m_dataProvider.GetUserById(id));
		}

		/// <summary>사용자 정보를 등록한다.</summary>
		/// <param name="request">사용자 등록 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.add")]
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseS3User>))]
		[HttpPost]
		public async Task<ActionResult> Post([FromBody] RequestS3User request)
		{
			return Json(await m_dataProvider.Add(request));
		}

		/// <summary>사용자 정보를 수정한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <param name="request">사용자 수정 정보 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.update")]
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{id}")]
		public async Task<ActionResult> Put([FromRoute] string id, [FromBody] RequestS3User request)
		{
			return Json(await m_dataProvider.Update(id, request));
		}

		/// <summary>사용자 정보를 삭제한다.</summary>
		/// <param name="id">사용자 식별자</param>
		/// <returns>결과 JSON 문자열</returns>
		// [ClaimRequirement("Permission", "common.account.users.remove")]
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{id}")]
		public async Task<ActionResult> Delete([FromRoute] string id)
		{
			return Json(await m_dataProvider.Remove(id));
		}
	}
}
