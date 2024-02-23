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
using System.Threading.Tasks;
using PortalData;
using PortalData.Response.S3Log;

namespace PortalProviderInterface
{
	/// <summary>S3 프로바이더 인터페이스</summary>
	public interface IS3LogProvider : IBaseProvider
	{
		#region S3
		/// <summary>버킷별 스토리지 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3StorageUsage>> GetStorageUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>버킷별 리퀘스트 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3RequestUsage>> GetRequestUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>버킷별 업, 다운로드 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3TransferUsage>> GetTransferUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>버킷별 에러 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3ErrorUsage>> GetErrorUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>현재 버킷의 사용량을 조회한다.</summary>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3BucketUsage>> GetBucket(string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>특정 시간동안 발생한 버킷별 요청 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3RequestUsage>> GetRequests(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>특정 시간동안 발생한 버킷별 사용량을 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3TransferUsage>> GetTransfers(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>특정 시간동안 발생한 버킷별 에러 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3ErrorUsage>> GetErrors(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100);
		#endregion

		#region Backend
		/// <summary>버킷별 Backend의 리퀘스트 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3RequestUsage>> GetBackendRequestUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>버킷별 Backend의 업, 다운로드 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3TransferUsage>> GetBackendTransferUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>버킷별 Backend의 에러 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3ErrorUsage>> GetBackendErrorUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>특정 시간동안 발생한 버킷별 Backend의 요청 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3RequestUsage>> GetBackendRequests(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>특정 시간동안 발생한 버킷별 Backend의 사용량을 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3TransferUsage>> GetBackendTransfers(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100);

		/// <summary>특정 시간동안 발생한 버킷별 Backend의 에러 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		Task<ResponseList<ResponseS3ErrorUsage>> GetBackendErrors(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100);
		#endregion
	}
}