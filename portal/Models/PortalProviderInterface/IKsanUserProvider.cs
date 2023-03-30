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
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Ksan;
using PortalData.Responses.Disks;
using PortalData.Responses.Ksan;

namespace PortalProviderInterface
{
	/// <summary>Ksan 사용자 프로바이더 인터페이스</summary>
	public interface IKsanUserProvider : IBaseProvider
	{
		/// <summary>Ksan 사용자를 추가한다.</summary>
		/// <param name="Request">Ksan 사용자 정보</param>
		/// <returns>Ksan 사용자 등록 결과</returns>
		Task<ResponseData<ResponseKsanUser>> Add(RequestKsanUser Request);

		/// <summary>Ksan 사용자를 추가한다.</summary>
		/// <param name="Name">Ksan 사용자 이름</param>
		/// <param name="AccessKey">엑세스키</param>
		/// <param name="SecretKey">시크릿키</param>
		/// <returns>Ksan 사용자 등록 결과</returns>
		Task<ResponseData<ResponseKsanUser>> Add(string Name, string AccessKey, string SecretKey);

		/// <summary>Ksan 사용자를 수정한다.</summary>
		/// <param name="Id">Ksan 사용자 식별자</param>
		/// <param name="Request">Ksan 사용자 정보</param>
		/// <returns>Ksan 사용자 수정 결과</returns>
		Task<ResponseData> Update(string Id, RequestKsanUserUpdate Request);

		/// <summary>Ksan 사용자를 삭제한다.</summary>
		/// <param name="Id">Ksan 사용자 식별자</param>
		/// <returns>Ksan 사용자 삭제 결과</returns>
		Task<ResponseData> Remove(string Id);

		/// <summary>특정 로그인 이메일에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="Email">이메일 주소</param>
		/// <returns>Ksan 사용자 정보 객체</returns>
		Task<ResponseData<ResponseKsanUser>> GetUserByEmail(string Email);

		/// <summary>Ksan 사용자 식별자로 특정 사용자를 가져온다.</summary>
		/// <param name="Id">Ksan 사용자 식별자</param>
		/// <returns>해당 사용자 데이터</returns>
		Task<ResponseData<ResponseKsanUser>> GetUser(string Id);

		/// <summary>전체 사용자를 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드목록 (Id, Email, Name)</param>
		/// <param name="SearchKeyword">검색어 (옵션)</param>
		/// <returns>Ksan 사용자 목록</returns>
		Task<ResponseList<ResponseKsanUser>> GetUsers(int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = "");

		/// <summary>사용자 이름 중복 여부를 검사한다.</summary>
		/// <param name="UserName">사용자 이름</param>
		/// <returns>검사 결과 객체</returns>
		Task<ResponseData> CheckUserNameDuplicated(string UserName);

		/// <summary>이메일 중복 여부를 검사한다.</summary>
		/// <param name="Email">이메일</param>
		/// <returns>검사 결과 객체</returns>
		Task<ResponseData> CheckEmailDuplicated(string Email);

		/// <summary>사용자의 스토리지 클래스 정보를 추가한다.</summary>
		/// <param name="Request">사용자 스토리지 클래스 추가 객체</param>
		/// <returns>스토리지 클래스 추가 결과</returns>
		Task<ResponseData> AddStorageClass(RequestStorageClass Request);


		/// <summary> 사용자의 스토리지 클래스 정보를 변경한다.</summary>
		/// <param name="StorageClassId"> 사용자 스토리지 클래스 아이디</param>
		/// <param name="Request">사용자 스토리지 클래스 변경 객체</param>
		/// <returns> 사용자 스토리지 클래스 삭제 결과 </returns>
		Task<ResponseData> UpdateStorageClass(string StorageClassId, RequestStorageClass Request);

		/// <summary> 사용자의 스토리지 클래스 정보를 삭제한다.</summary>
		/// <param name="StorageClassId"> 사용자 스토리지 클래스 아이디</param>
		/// <returns> 사용자 스토리지 클래스 삭제 결과 </returns>
		Task<ResponseData> RemoveStorageClass(string StorageClassId);

		/// <summary>사용자의 스토리지 클래스 목록을 조회한다.</summary>
		/// <param name="UserId">Ksan 사용자 식별자</param>
		/// <returns>스토리지 클래스 목록 결과</returns>
		Task<ResponseList<ResponseStorageClass>> GetUserStorageClass(string UserId);

		/// <summary>특정 스토리지 클래스을 조회한다.</summary>
		/// <param name="StorageClassId"> 사용자 스토리지 클래스 아이디</param>
		/// <returns>스토리지 클래스 목록 결과</returns>
		Task<ResponseData<ResponseStorageClass>> GetStorageClass(string StorageClassId);

		/// <summary>사용자에게 할당가능한 스토리지 클래스 목록을 조회한다.</summary>
		/// <param name="UserId">Ksan 사용자 식별자</param>
		/// <param name="DiskPoolId"> 디스크풀 식별자</param>
		/// <returns>스토리지 클래스 목록 결과</returns>
		Task<ResponseList<ResponseDiskPool>> GetAvailableStorageClass(string UserId, string DiskPoolId = null);
	}
}
