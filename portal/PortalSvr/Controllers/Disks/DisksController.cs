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
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDisk>))]
		[HttpPost("{ServerId}")]
		public async Task<ActionResult> Add([FromRoute] string ServerId, [FromBody] RequestDisk Request)
		{
			return Json(await m_dataProvider.Add(ServerId, Request));
		}

		/// <summary>디스크 정보를 수정한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <param name="Request">디스크 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{ServerId}/{Id}")]
		public async Task<ActionResult> Update([FromRoute] string ServerId, [FromRoute] string Id, [FromBody] RequestDisk Request)
		{
			return Json(await m_dataProvider.Update(ServerId, Id, Request));
		}

		/// <summary>디스크 상태를 수정한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <param name="State">디스크 상태</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{ServerId}/{Id}/State/{State}")]
		public async Task<ActionResult> UpdateState([FromRoute] string ServerId, [FromRoute] string Id, [FromRoute] EnumDiskState State)
		{
			return Json(await m_dataProvider.UpdateState(ServerId, Id, State));
		}

		/// <summary>디스크 읽기/쓰기 모드를 수정한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <param name="DiskRwMode">디스크 읽기/쓰기 모드</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("{ServerId}/{Id}/RwMode/{DiskRwMode}")]
		public async Task<ActionResult> UpdateRwMode([FromRoute] string ServerId, [FromRoute] string Id, [FromRoute] EnumDiskRwMode DiskRwMode)
		{
			return Json(await m_dataProvider.UpdateRwMode(ServerId, Id, DiskRwMode));
		}

		/// <summary>디스크 상태를 수정한다.</summary>
		/// <param name="Request">상태 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("State")]
		public async Task<ActionResult> UpdateState([FromBody] RequestDiskState Request)
		{
			return Json(await m_dataProvider.UpdateState(Request));
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

		/// <summary>디스크 읽기/쓰기 모드를 수정한다.</summary>
		/// <param name="Request">디스크 읽기/쓰기 모드 수정 요청 객체</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpPut("RwMode")]
		public async Task<ActionResult> UpdateRwMode([FromBody] RequestDiskRwMode Request)
		{
			return Json(await m_dataProvider.UpdateRwMode(Request));
		}

		/// <summary>디스크 정보를 삭제한다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData))]
		[HttpDelete("{ServerId}/{Id}")]
		public async Task<ActionResult> Remove([FromRoute] string ServerId, [FromRoute] string Id)
		{
			return Json(await m_dataProvider.Remove(ServerId, Id));
		}

		/// <summary>전체 디스크 목록을 가져온다.</summary>
		/// <param name="SearchStates">검색할 디스크 상태 목록</param>
		/// <param name="SearchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseList<ResponseDisk>))]
		[HttpGet("")]
		public async Task<ActionResult> Get(
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
		/// <param name="OrderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (DiskNo, Path)</param>
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
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskWithServices>))]
		[HttpGet("{ServerId}/{Id}")]
		public async Task<ActionResult> Get([FromRoute] string ServerId, [FromRoute] string Id)
		{
			return Json(await m_dataProvider.Get(ServerId, Id));
		}

		/// <summary>DiskNo로 디스크 ID를 가져온다.</summary>
		/// <param name="DiskNo">디스크 No</param>
		/// <returns>결과 JSON 문자열</returns>
		[SwaggerResponse((int)HttpStatusCode.OK, null, typeof(ResponseData<ResponseDiskWithServices>))]
		[HttpGet("Find/{DiskNo}")]
		public async Task<ActionResult> Get([FromRoute] string DiskNo)
		{
			return Json(await m_dataProvider.Get(DiskNo));
		}
	}
}