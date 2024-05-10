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
using System.Net;
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
using Swashbuckle.AspNetCore.Annotations;
using PortalData.Response.S3Log;
using System.ComponentModel.DataAnnotations;
using PortalData.Requests.S3;

namespace PortalSvr.Controllers.Networks
{
	/// <summary>네트워크 인터페이스 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class S3Controller : BaseController
	{
		/// <summary>데이터 프로바이더</summary>
		private readonly IS3Provider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public S3Controller(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<S3Controller> logger,
			IS3Provider dataProvider
		)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		/// <summary>버킷 로깅 정보를 설정한다.</summary>
		/// <param name="Request">버킷 로깅 정보</param>
		/// <returns>버킷 로깅 정보</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPost("SetBucketLogging")]
		public async Task<IActionResult> SetBucketLogging([FromBody] RequestS3BucketLogging Request)
		{
			return Json(await m_dataProvider.SetBucketLogging(Request));
		}
	}
}