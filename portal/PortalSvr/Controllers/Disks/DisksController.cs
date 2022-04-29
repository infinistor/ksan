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
		/// <param name="serverId">서버 아이디</param>
		/// <param name="request">디스크 등록 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDisk>))]
		[HttpPost("{serverId}")]
		public async Task<ActionResult> Add([FromRoute] string serverId, [FromBody] RequestDisk request)
		{
			return Json(await m_dataProvider.Add(serverId, request));
		}

		/// <summary>디스크 정보를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="request">디스크 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{serverId}/{id}")]
		public async Task<ActionResult> Update([FromRoute] string serverId, [FromRoute] string id, [FromBody] RequestDisk request)
		{
			return Json(await m_dataProvider.Update(serverId, id, request));
		}

		/// <summary>디스크 상태를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="state">디스크 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{serverId}/{id}/State/{state}")]
		public async Task<ActionResult> UpdateState([FromRoute] string serverId, [FromRoute] string id, [FromRoute] EnumDiskState state)
		{
			return Json(await m_dataProvider.UpdateState(serverId, id, state));
		}

		/// <summary>디스크 읽기/쓰기 모드를 수정한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="diskRwMode">디스크 읽기/쓰기 모드</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{serverId}/{id}/RwMode/{diskRwMode}")]
		public async Task<ActionResult> UpdateRwMode([FromRoute] string serverId, [FromRoute] string id, [FromRoute] EnumDiskRwMode diskRwMode)
		{
			return Json(await m_dataProvider.UpdateRwMode(serverId, id, diskRwMode));
		}

		/// <summary>디스크 상태를 수정한다.</summary>
		/// <param name="request">상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("State")]
		public async Task<ActionResult> UpdateState([FromBody] RequestDiskState request)
		{
			return Json(await m_dataProvider.UpdateState(request));
		}

		/// <summary>디스크 크기를 수정한다.</summary>
		/// <param name="request">디스크 크기 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("Size")]
		public async Task<ActionResult> UpdateSize([FromBody] RequestDiskSize request)
		{
			return Json(await m_dataProvider.UpdateSize(request));
		}

		/// <summary>디스크 읽기/쓰기 모드를 수정한다.</summary>
		/// <param name="request">디스크 읽기/쓰기 모드 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("RwMode")]
		public async Task<ActionResult> UpdateRwMode([FromBody] RequestDiskRwMode request)
		{
			return Json(await m_dataProvider.UpdateRwMode(request));
		}

		/// <summary>디스크 정보를 삭제한다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{serverId}/{id}")]
		public async Task<ActionResult> Remove([FromRoute] string serverId, [FromRoute] string id)
		{
			return Json(await m_dataProvider.Remove(serverId, id));
		}

		/// <summary>전체 디스크 목록을 가져온다.</summary>
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
		[HttpGet("")]
		public async Task<ActionResult> Get(
			List<EnumDiskState> searchStates, List<EnumDiskRwMode> searchRwModes,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetList(
				searchStates, searchRwModes,
				skip, countPerPage,
				orderFields, orderDirections,
				searchFields, searchKeyword
			));
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
		[HttpGet("{serverId}")]
		public async Task<ActionResult> Get(
			[FromRoute] string serverId,
			List<EnumDiskState> searchStates, List<EnumDiskRwMode> searchRwModes,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			return Json(await m_dataProvider.GetList(
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
		[HttpGet("{serverId}/{id}")]
		public async Task<ActionResult> Get([FromRoute] string serverId, [FromRoute] string id)
		{
			return Json(await m_dataProvider.Get(serverId, id));
		}

		/// <summary>DiskNo로 디스크 ID를 가져온다.</summary>
		/// <param name="diskNo">디스크 No</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskWithServices>))]
		[HttpGet("Find/{diskNo}")]
		public async Task<ActionResult> Get([FromRoute] string diskNo)
		{
			return Json(await m_dataProvider.Get(diskNo));
		}
	}
}