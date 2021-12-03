using System.Collections.Generic;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Requests.Services;
using PortalData.Requests.Services.Configs;
using PortalData.Responses.Services;
using PortalData.Responses.Services.Configs;
using MTLib.CommonData;

namespace PortalProviderInterface
{
	/// <summary>서비스 그룹 데이터 프로바이더 인터페이스</summary>
	public interface IServiceGroupProvider : IBaseProvider
	{
		/// <summary>서비스 그룹 등록</summary>
		/// <param name="request">서비스 그룹 등록 요청 객체</param>
		/// <returns>서비스 그룹 등록 결과 객체</returns>
		Task<ResponseData<ResponseServiceGroupWithServices>> Add(RequestServiceGroup request);
		
		/// <summary>서비스 그룹 수정</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <param name="request">서비스 그룹 수정 요청 객체</param>
		/// <returns>서비스 그룹 수정 결과 객체</returns>
		Task<ResponseData> Update(string id, RequestServiceGroup request);
		
		/// <summary>서비스 그룹 삭제</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 삭제 결과 객체</returns>
		Task<ResponseData> Remove(string id);
		
		/// <summary>서비스 그룹 목록을 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, CpuModel, Clock)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>서비스 그룹 목록 객체</returns>
		Task<ResponseList<ResponseServiceGroup>> GetList(
			int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = ""
		);
		
		/// <summary>서비스 그룹 정보를 가져온다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 정보 객체</returns>
		Task<ResponseData<ResponseServiceGroupWithServices>> Get(string id);

		/// <summary>해당 이름이 존재하는지 여부</summary>
		/// <param name="exceptId">이름 검색 시 제외할 서비스 그룹 아이디</param>
		/// <param name="request">특정 이름의 서비스 그룹 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		Task<ResponseData<bool>> IsNameExist(string exceptId, RequestIsServiceGroupNameExist request);

		/// <summary>해당 서비스 타입으로 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="serviceType">서비스 타입</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>참여가 가능한 서비스 목록 객체</returns>
		Task<ResponseList<ResponseService>> GetAvailableServices(
			EnumServiceType serviceType
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "");

		/// <summary>주어진 서비스 그룹 아이디에 해당하는 서비스 그룹에 참여가 가능한 서비스 목록을 가져온다.</summary>
		/// <param name="id">서비스 그룹 아이디 (null인 경우, 어느 그룹에도 속하지 않은 서비스만 검색한다.)</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, IpAddress)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>참여가 가능한 서비스 목록 객체</returns>
		Task<ResponseList<ResponseService>> GetAvailableServices(
			string id
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "");

		/// <summary>서비스 그룹 시작</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 시작 결과 객체</returns>
		Task<ResponseData> Start(string id);

		/// <summary>서비스 그룹 중지</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 중지 결과 객체</returns>
		Task<ResponseData> Stop(string id);

		/// <summary>서비스 그룹 재시작</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>서비스 그룹 재시작 결과 객체</returns>
		Task<ResponseData> Restart(string id);

		/// <summary>특정 서비스 그룹의 설정 정보를 가져온다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <returns>설정 문자열이 포함된 결과 객체</returns>
		Task<ResponseData<T>> GetConfig<T>(string id) where T : IResponseServiceConfig;

		/// <summary>주어진 설정 정보를 특정 서비스 그룹에 저장한다.</summary>
		/// <param name="id">서비스 그룹 아이디</param>
		/// <param name="config">서비스 설정 객체</param>
		/// <returns>설정 결과 객체</returns>
		Task<ResponseData> SetConfig<T>(string id, T config) where T : CommonRequestData, IRequestServiceConfig;
	}
}