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

namespace PortalSvr.Controllers.Networks
{
	/// <summary>네트워크 인터페이스 컨트롤러</summary>
	[EnableCors("CorsPolicy")]
	[Produces("application/json")]
	[Route("api/v1/[controller]")]
	[ApiKeyAuthorize]
	public class S3LogsController : BaseController
	{
		/// <summary>데이터 프로바이더</summary>
		private readonly IS3LogProvider m_dataProvider;

		/// <summary>생성자</summary>
		/// <param name="configuration">설정 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="logger">로거</param>
		/// <param name="dataProvider">데이터 프로바이더</param>
		public S3LogsController(
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ILogger<S3LogsController> logger,
			IS3LogProvider dataProvider
		)
			: base(configuration, userManager, logger, dataProvider)
		{
			m_dataProvider = dataProvider;
		}

		#region S3
		/// <summary>버킷별 스토리지 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseS3StorageUsage>))]
		[HttpGet("StorageUsage")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetStorageUsage([Required] string StartDate, [Required] int Days, [FromQuery] string BucketName = null, [FromQuery] string UserName = null, [FromQuery] int Skip = 0, [FromQuery] int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetStorageUsage(StartDate, Days, BucketName, UserName, Skip, CountPerPage));
		}

		/// <summary>버킷별 리퀘스트 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseS3RequestUsage>))]
		[HttpGet("RequestUsage")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetRequestUsage([Required] string StartDate, [Required] int Days, [FromQuery] string BucketName = null, [FromQuery] string UserName = null, [FromQuery] int Skip = 0, [FromQuery] int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetRequestUsage(StartDate, Days, BucketName, UserName, Skip, CountPerPage));
		}

		/// <summary>버킷별 업, 다운로드 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseS3TransferUsage>))]
		[HttpGet("TransferUsage")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetTransferUsage([Required] string StartDate, [Required] int Days, [FromQuery] string BucketName = null, [FromQuery] string UserName = null, [FromQuery] int Skip = 0, [FromQuery] int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetTransferUsage(StartDate, Days, BucketName, UserName, Skip, CountPerPage));
		}

		/// <summary>버킷별 에러 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseS3ErrorUsage>))]
		[HttpGet("ErrorUsage")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetErrorUsage([Required] string StartDate, [Required] int Days, [FromQuery] string BucketName = null, [FromQuery] string UserName = null, [FromQuery] int Skip = 0, [FromQuery] int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetErrorUsage(StartDate, Days, BucketName, UserName, Skip, CountPerPage));
		}

		/// <summary>현재 버킷의 사용량을 조회한다.</summary>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseS3BucketUsage>))]
		[HttpGet("Bucket")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetBuckets(string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetBucket(UserName, BucketName, Skip, CountPerPage));
		}

		/// <summary>특정 시간동안 발생한 버킷별 요청 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseS3RequestUsage>))]
		[HttpGet("Request")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetRequests([Required] string StartTime, [Required] string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetRequests(StartTime, EndTime, UserName, BucketName, Skip, CountPerPage));
		}

		/// <summary>특정 시간동안 발생한 버킷별 사용량을 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseS3TransferUsage>))]
		[HttpGet("Transfer")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetTransfers([Required] string StartTime, [Required] string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetTransfers(StartTime, EndTime, UserName, BucketName, Skip, CountPerPage));
		}

		/// <summary>특정 시간동안 발생한 버킷별 에러 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseS3ErrorUsage>))]
		[HttpGet("Error")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetErrors([Required] string StartTime, [Required] string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetErrors(StartTime, EndTime, UserName, BucketName, Skip, CountPerPage));
		}
		#endregion

		#region Backend
		/// <summary>버킷별 Backend의 리퀘스트 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseS3RequestUsage>))]
		[HttpGet("Backend/RequestUsage")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetBackendRequestUsage([Required] string StartDate, [Required] int Days, [FromQuery] string BucketName = null, [FromQuery] string UserName = null, [FromQuery] int Skip = 0, [FromQuery] int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetBackendRequestUsage(StartDate, Days, BucketName, UserName, Skip, CountPerPage));
		}

		/// <summary>버킷별 Backend의 업, 다운로드 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseS3TransferUsage>))]
		[HttpGet("Backend/TransferUsage")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetBackendTransferUsage([Required] string StartDate, [Required] int Days, [FromQuery] string BucketName = null, [FromQuery] string UserName = null, [FromQuery] int Skip = 0, [FromQuery] int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetBackendTransferUsage(StartDate, Days, BucketName, UserName, Skip, CountPerPage));
		}

		/// <summary>버킷별 Backend의 에러 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseS3ErrorUsage>))]
		[HttpGet("Backend/ErrorUsage")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetBackendErrorUsage([Required] string StartDate, [Required] int Days, [FromQuery] string BucketName = null, [FromQuery] string UserName = null, [FromQuery] int Skip = 0, [FromQuery] int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetBackendErrorUsage(StartDate, Days, BucketName, UserName, Skip, CountPerPage));
		}

		/// <summary>특정 시간동안 발생한 버킷별 Backend의 요청 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseS3RequestUsage>))]
		[HttpGet("Backend/Request")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetBackendRequests([Required] string StartTime, [Required] string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetBackendRequests(StartTime, EndTime, UserName, BucketName, Skip, CountPerPage));
		}

		/// <summary>특정 시간동안 발생한 Backend의 버킷별 사용량을 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseS3TransferUsage>))]
		[HttpGet("Backend/Transfer")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetBackendTransfers([Required] string StartTime, [Required] string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetBackendTransfers(StartTime, EndTime, UserName, BucketName, Skip, CountPerPage));
		}

		/// <summary>특정 시간동안 발생한 버킷별 Backend의 에러 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>버킷의 사용량 정보 객체</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseS3ErrorUsage>))]
		[HttpGet("Backend/Error")]
		[ApiKeyAuthorize]
		public async Task<ActionResult> GetBackendErrors([Required] string StartTime, [Required] string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			return Json(await m_dataProvider.GetBackendErrors(StartTime, EndTime, UserName, BucketName, Skip, CountPerPage));
		}
		#endregion
	}
}