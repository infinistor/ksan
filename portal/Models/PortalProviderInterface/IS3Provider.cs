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
using PortalData.Requests.S3;
using PortalData.Response.S3;

namespace PortalProviderInterface
{
	/// <summary>S3 프로바이더 인터페이스</summary>
	public interface IS3Provider : IBaseProvider
	{

		/// <summary>접근 아이피 주소를 등록한다.</summary>
		/// <param name="Request">접근 아이피 주소 정보 요청 객체</param>
		/// <returns>등록 성공 여부</returns>
		Task<ResponseData<ResponseS3AccessIp>> AddAccessIp(RequestS3AccessIp Request);
		// Task<ResponseData> AddAccessIp(RequestS3AccessIp request); //KJW Change

		/// <summary>접근 아이피 주소를 수정한다.</summary>
		/// <param name="AddressId">수정할 주소 아이디</param>
		/// <param name="Request">접근 아이피 주소 정보 요청 객체</param>
		/// <returns>수정 성공 여부</returns>
		Task<ResponseData> UpdateAccessIp(string AddressId, RequestS3AccessIp Request);

		/// <summary>접근 아이피 주소를 삭제한다.</summary>
		/// <param name="AddressId">삭제할 주소 아이디</param>
		/// <returns>삭제 성공 여부</returns>
		Task<ResponseData> RemoveAccessIp(string AddressId);

		//KJW ADD
		/// <summary>접근 아이피 주소를 삭제한다.</summary>
		/// <param name="UserId">유저 아이디</param>
		/// <param name="BucketName">버킷 이름</param>
		/// <returns>삭제 성공 여부</returns>
		Task<ResponseData> RemoveAccessIp(string UserId, string BucketName);

		/// <summary>접근 아이피 주소 상세 정보를 가져온다.</summary>
		/// <param name="AddressId">주소 아이디</param>
		/// <returns>접근 아이피 주소 정보</returns>
		Task<ResponseData<ResponseS3AccessIp>> GetAccessIp(string AddressId);

		/// <summary>접근 아이피 주소 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="UserId">유저 아이디</param>
		/// <param name="BucketName">버킷 이름</param>
		/// <returns>접근 아이피 주소 목록 객체</returns>
		Task<ResponseList<ResponseS3AccessIp>> GetAccessIps(int Skip = 0, int CountPerPage = 100, string UserId = null, string BucketName = null);
	}
}