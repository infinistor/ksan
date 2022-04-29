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
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;

namespace PortalProviderInterface
{
	/// <summary>S3 사용자 프로바이더 인터페이스</summary>
	public interface IS3UserProvider : IBaseProvider
	{
		/// <summary>S3 사용자를 추가한다.</summary>
		/// <param name="request">S3 사용자 정보</param>
		/// <returns>S3 사용자 등록 결과</returns>
		Task<ResponseData<ResponseS3User>> Add(RequestS3User request);

		/// <summary>S3 사용자를 수정한다.</summary>
		/// <param name="id">S3 사용자 식별자</param>
		/// <param name="request">S3 사용자 정보</param>
		/// <returns>S3 사용자 수정 결과</returns>
		Task<ResponseData> Update(string id, RequestS3User request);

		/// <summary>S3 사용자를 삭제한다.</summary>
		/// <param name="id">S3 사용자 식별자</param>
		/// <returns>S3 사용자 삭제 결과</returns>
		Task<ResponseData> Remove(string id);

		/// <summary>특정 로그인 이메일에 대한 사용자 정보 객체를 가져온다.</summary>
		/// <param name="email">이메일 주소</param>
		/// <returns>S3 사용자 정보 객체</returns>
		Task<ResponseData<ResponseS3User>> GetUserByEmail(string email);

		/// <summary>S3 사용자 식별자로 특정 사용자를 가져온다.</summary>
		/// <param name="id">S3 사용자 식별자</param>
		/// <returns>해당 사용자 데이터</returns>
		Task<ResponseData<ResponseS3User>> GetUserById(string id);

		/// <summary>전체 사용자를 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드목록 (Id, Email, Name)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>S3 사용자 목록</returns>
		Task<ResponseList<ResponseS3User>> GetUsers(int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = "");

		/// <summary>사용자 이름 중복 여부를 검사한다.</summary>
		/// <param name="userName">사용자 이름</param>
		/// <returns>검사 결과 객체</returns>
		Task<ResponseData> CheckUserNameDuplicated(string userName);

		/// <summary>이메일 중복 여부를 검사한다.</summary>
		/// <param name="email">이메일</param>
		/// <returns>검사 결과 객체</returns>
		Task<ResponseData> CheckEmailDuplicated(string email);
	}
}
