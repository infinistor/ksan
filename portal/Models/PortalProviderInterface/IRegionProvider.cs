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
using PortalData.Requests.Region;
using PortalData.Responses.Region;

namespace PortalProviderInterface
{
	/// <summary>리전 프로바이더 인터페이스</summary>
	public interface IRegionProvider : IBaseProvider
	{
		/// <summary>리전을 생성한다.</summary>
		/// <param name="Request">리전 정보</param>
		/// <returns>리전 등록 결과</returns>
		Task<ResponseData<ResponseRegion>> Add(RequestRegion Request);

		/// <summary>리전을 동기화한다.</summary>
		/// <param name="Request">리전 정보 목록</param>
		/// <returns>리전 등록 결과</returns>
		Task<ResponseData> Sync(List<RequestRegionSync> Request);

		/// <summary>리전을 삭제한다.</summary>
		/// <param name="RegionName">리전 식별자</param>
		/// <returns>리전 삭제 결과</returns>
		Task<ResponseData> Remove(string RegionName);

		/// <summary>리전 식별자로 특정 리전을 가져온다.</summary>
		/// <param name="RegionName">리전 식별자</param>
		/// <returns>리전 정보 객체</returns>
		Task<ResponseData<ResponseRegion>> Get(string RegionName);

		/// <summary>리전 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <returns>리전 목록</returns>
		Task<ResponseList<ResponseRegion>> GetList(
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null
		);
	}
}
