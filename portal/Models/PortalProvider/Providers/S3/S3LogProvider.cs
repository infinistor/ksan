using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.EntityFramework;
using PortalData;
using PortalData.Response.S3Log;
using PortalModels;
using PortalProviderInterface;
using PortalResources;

namespace PortalProvider.Providers.S3
{
	/// <summary>S3 프로바이더 클래스</summary>
	public class S3LogProvider : BaseProvider<PortalModel>, IS3LogProvider
	{

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="logger">로거</param>
		/// <param name="systemInformationProvider">시스템 정보 프로바이더</param>
		public S3LogProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<S3LogProvider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		#region S3
		/// <summary>버킷별 스토리지 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3StorageUsage>> GetStorageUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3StorageUsage>();

			try
			{
				if (string.IsNullOrEmpty(StartDate) || Days < 0)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				var SearchStartDate = Convert.ToDateTime(StartDate);
				var SearchEndDate = SearchStartDate.AddDays(Days);

				Result.Data = await m_dbContext.BucketAssets.AsNoTracking()
					.Where(i => (SearchStartDate <= i.InDate) && (SearchEndDate >= i.InDate) && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						MaxUsage = Items.Max(i => i.MaxUsed),
						MinUsage = Items.Min(i => i.MinUsed),
						AverageUsage = Items.Average(i => i.AvgUsed)
					})
					.CreateListAsync<dynamic, ResponseS3StorageUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3StorageUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception e)
			{
				NNException.Log(e);
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>버킷별 리퀘스트 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3RequestUsage>> GetRequestUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3RequestUsage>();
			try
			{
				if (string.IsNullOrEmpty(StartDate) || Days < 0)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				var SearchStartDate = Convert.ToDateTime(StartDate);
				var SearchEndDate = SearchStartDate.AddDays(Days);

				Result.Data = await m_dbContext.BucketApiAssets.AsNoTracking()
					.Where(i => (SearchStartDate <= i.InDate) && (SearchEndDate >= i.InDate) && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						Get = Items.Sum(j => j.Event.StartsWith("REST.GET") ? j.Count : 0),
						Put = Items.Sum(j => j.Event.StartsWith("REST.PUT") ? j.Count : 0),
						Delete = Items.Sum(j => j.Event.StartsWith("REST.DELETE") ? j.Count : 0),
						Post = Items.Sum(j => j.Event.StartsWith("REST.POST") ? j.Count : 0),
						List = Items.Sum(j => j.Event.StartsWith("REST.LIST") ? j.Count : 0),
						Head = Items.Sum(j => j.Event.StartsWith("REST.HEAD") ? j.Count : 0),
					})
					.CreateListAsync<dynamic, ResponseS3RequestUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3RequestUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception e)
			{
				NNException.Log(e);
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>버킷별 업, 다운로드 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3TransferUsage>> GetTransferUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3TransferUsage>();
			try
			{
				if (string.IsNullOrEmpty(StartDate) || Days < 0)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				var SearchStartDate = Convert.ToDateTime(StartDate);
				var SearchEndDate = SearchStartDate.AddDays(Days);

				Result.Data = await m_dbContext.BucketIoAssets.AsNoTracking()
					.Where(i => (SearchStartDate <= i.InDate) && (SearchEndDate >= i.InDate) && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						UploadUsage = Items.Sum(i => i.Upload),
						DownloadUsage = Items.Sum(i => i.Download),
					})
					.CreateListAsync<dynamic, ResponseS3TransferUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3TransferUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception e)
			{
				NNException.Log(e);
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>버킷별 에러 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3ErrorUsage>> GetErrorUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3ErrorUsage>();

			try
			{
				if (string.IsNullOrEmpty(StartDate) || Days < 0)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				var SearchStartDate = Convert.ToDateTime(StartDate);
				var SearchEndDate = SearchStartDate.AddDays(Days);

				Result.Data = await m_dbContext.BucketErrorAssets.AsNoTracking()
					.Where(i => (SearchStartDate <= i.InDate) && (SearchEndDate >= i.InDate) && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						ClientError = Items.Sum(i => i.ClientErrorCount),
						ServerError = Items.Min(i => i.ServerErrorCount)
					})
					.CreateListAsync<dynamic, ResponseS3ErrorUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3ErrorUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception e)
			{
				NNException.Log(e);
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>현재 버킷의 사용량을 조회한다.</summary>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3BucketUsage>> GetBucket(string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3BucketUsage>();

			try
			{
				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3BucketUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}
		/// <summary>특정 시간동안 발생한 버킷별 요청 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3RequestUsage>> GetRequests(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3RequestUsage>();

			try
			{
				// 시작시간과 종료시간을 DateTime으로 변환한다.
				if (!ConvertToDateTime(StartTime, out DateTime SearchStartTime) || !ConvertToDateTime(EndTime, out DateTime SearchEndTime))
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}
				// 시작시간이 종료시간보다 크면 에러를 리턴한다.
				if (SearchStartTime > SearchEndTime)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				// 요청 횟수를 조회한다.
				Result.Data = await m_dbContext.BucketApiMeters.AsNoTracking()
					.Where(i => i.InDate <= SearchEndTime && i.InDate >= SearchStartTime && i.UserName != "-" && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						Get = Items.Sum(j => j.Event.StartsWith("REST.GET") ? j.Count : 0),
						Put = Items.Sum(j => j.Event.StartsWith("REST.PUT") ? j.Count : 0),
						Delete = Items.Sum(j => j.Event.StartsWith("REST.DELETE") ? j.Count : 0),
						Post = Items.Sum(j => j.Event.StartsWith("REST.POST") ? j.Count : 0),
						List = Items.Sum(j => j.Event.StartsWith("REST.LIST") ? j.Count : 0),
						Head = Items.Sum(j => j.Event.StartsWith("REST.HEAD") ? j.Count : 0),
					})
					.CreateListAsync<dynamic, ResponseS3RequestUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3RequestUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>특정 시간동안 발생한 버킷별 사용량을 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3TransferUsage>> GetTransfers(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3TransferUsage>();

			try
			{
				// 시작시간과 종료시간을 DateTime으로 변환한다.
				if (!ConvertToDateTime(StartTime, out DateTime SearchStartTime) || !ConvertToDateTime(EndTime, out DateTime SearchEndTime))
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}
				// 시작시간이 종료시간보다 크면 에러를 리턴한다.
				if (SearchStartTime > SearchEndTime)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				// 전송량을 조회한다.
				Result.Data = await m_dbContext.BucketIoMeters.AsNoTracking()
					.Where(i => i.InDate <= SearchEndTime && i.InDate >= SearchStartTime && i.UserName != "-" && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						UploadUsage = Items.Sum(i => i.Upload),
						DownloadUsage = Items.Sum(i => i.Download),
					})
					.CreateListAsync<dynamic, ResponseS3TransferUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3TransferUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>특정 시간동안 발생한 버킷별 에러 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3ErrorUsage>> GetErrors(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3ErrorUsage>();

			try
			{
				// 시작시간과 종료시간을 DateTime으로 변환한다.
				if (!ConvertToDateTime(StartTime, out DateTime SearchStartTime) || !ConvertToDateTime(EndTime, out DateTime SearchEndTime))
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}
				// 시작시간이 종료시간보다 크면 에러를 리턴한다.
				if (SearchStartTime > SearchEndTime)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				// 에러 횟수를 조회한다.
				Result.Data = await m_dbContext.BucketErrorMeters.AsNoTracking()
					.Where(i => i.InDate <= SearchEndTime && i.InDate >= SearchStartTime && i.UserName != "-" && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						ClientError = Items.Sum(i => i.ClientErrorCount),
						ServerError = Items.Min(i => i.ServerErrorCount)
					})
					.CreateListAsync<dynamic, ResponseS3ErrorUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3ErrorUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
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
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3RequestUsage>> GetBackendRequestUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3RequestUsage>();
			try
			{
				if (string.IsNullOrEmpty(StartDate) || Days < 0)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				var SearchStartDate = Convert.ToDateTime(StartDate);
				var SearchEndDate = SearchStartDate.AddDays(Days);

				Result.Data = await m_dbContext.BackendApiAssets.AsNoTracking()
					.Where(i => (SearchStartDate <= i.InDate) && (SearchEndDate >= i.InDate) && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						Get = Items.Sum(j => j.Event.StartsWith("BACKEND.GET") ? j.Count : 0),
						Put = Items.Sum(j => j.Event.StartsWith("BACKEND.PUT") ? j.Count : 0),
						Delete = Items.Sum(j => j.Event.StartsWith("BACKEND.DELETE") ? j.Count : 0),
						Post = Items.Sum(j => j.Event.StartsWith("BACKEND.POST") ? j.Count : 0),
						List = Items.Sum(j => j.Event.StartsWith("BACKEND.LIST") ? j.Count : 0),
						Head = Items.Sum(j => j.Event.StartsWith("BACKEND.HEAD") ? j.Count : 0),
					})
					.CreateListAsync<dynamic, ResponseS3RequestUsage>(Skip, CountPerPage);


				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3RequestUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception e)
			{
				NNException.Log(e);
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>버킷별 Backend의 업, 다운로드 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3TransferUsage>> GetBackendTransferUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3TransferUsage>();
			try
			{
				if (string.IsNullOrEmpty(StartDate) || Days < 0)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				var SearchStartDate = Convert.ToDateTime(StartDate);
				var SearchEndDate = SearchStartDate.AddDays(Days);

				Result.Data = await m_dbContext.BackendIoAssets.AsNoTracking()
					.Where(i => (SearchStartDate <= i.InDate) && (SearchEndDate >= i.InDate) && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						UploadUsage = Items.Sum(i => i.Upload),
						DownloadUsage = Items.Sum(i => i.Download),
					})
					.CreateListAsync<dynamic, ResponseS3TransferUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3TransferUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception e)
			{
				NNException.Log(e);
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>버킷별 Backend의 에러 사용량을 조회한다.</summary>
		/// <param name="StartDate">조회 시작일</param>
		/// <param name="Days">조회할 일자</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="UserName">유저명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3ErrorUsage>> GetBackendErrorUsage(string StartDate, int Days, string BucketName = null, string UserName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3ErrorUsage>();

			try
			{
				if (string.IsNullOrEmpty(StartDate) || Days < 0)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				var SearchStartDate = Convert.ToDateTime(StartDate);
				var SearchEndDate = SearchStartDate.AddDays(Days);

				Result.Data = await m_dbContext.BackendErrorAssets.AsNoTracking()
					.Where(i => (SearchStartDate <= i.InDate) && (SearchEndDate >= i.InDate) && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						ClientError = Items.Sum(i => i.ClientErrorCount),
						ServerError = Items.Min(i => i.ServerErrorCount)
					})
					.CreateListAsync<dynamic, ResponseS3ErrorUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3ErrorUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception e)
			{
				NNException.Log(e);
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return Result;
		}

		/// <summary>특정 시간동안 발생한 버킷별 Backend의 요청 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3RequestUsage>> GetBackendRequests(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3RequestUsage>();

			try
			{
				// 시작시간과 종료시간을 DateTime으로 변환한다.
				if (!ConvertToDateTime(StartTime, out DateTime SearchStartTime) || !ConvertToDateTime(EndTime, out DateTime SearchEndTime))
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}
				// 시작시간이 종료시간보다 크면 에러를 리턴한다.
				if (SearchStartTime > SearchEndTime)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				// 요청 횟수를 조회한다.
				Result.Data = await m_dbContext.BackendApiMeters.AsNoTracking()
					.Where(i => i.InDate <= SearchEndTime && i.InDate >= SearchStartTime && i.UserName != "-" && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						Get = Items.Sum(j => j.Event.StartsWith("REST.GET") ? j.Count : 0),
						Put = Items.Sum(j => j.Event.StartsWith("REST.PUT") ? j.Count : 0),
						Delete = Items.Sum(j => j.Event.StartsWith("REST.DELETE") ? j.Count : 0),
						Post = Items.Sum(j => j.Event.StartsWith("REST.POST") ? j.Count : 0),
						List = Items.Sum(j => j.Event.StartsWith("REST.LIST") ? j.Count : 0),
						Head = Items.Sum(j => j.Event.StartsWith("REST.HEAD") ? j.Count : 0),
					})
					.CreateListAsync<dynamic, ResponseS3RequestUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3RequestUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>특정 시간동안 발생한 버킷별 Backend의 사용량을 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3TransferUsage>> GetBackendTransfers(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3TransferUsage>();

			try
			{
				// 시작시간과 종료시간을 DateTime으로 변환한다.
				if (!ConvertToDateTime(StartTime, out DateTime SearchStartTime) || !ConvertToDateTime(EndTime, out DateTime SearchEndTime))
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}
				// 시작시간이 종료시간보다 크면 에러를 리턴한다.
				if (SearchStartTime > SearchEndTime)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				// 전송량을 조회한다.
				Result.Data = await m_dbContext.BackendIoMeters.AsNoTracking()
					.Where(i => i.InDate <= SearchEndTime && i.InDate >= SearchStartTime && i.UserName != "-" && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						UploadUsage = Items.Sum(i => i.Upload),
						DownloadUsage = Items.Sum(i => i.Download),
					})
					.CreateListAsync<dynamic, ResponseS3TransferUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3TransferUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>특정 시간동안 발생한 버킷별 Backend의 에러 횟수를 조회한다.</summary>
		/// <param name="StartTime">시작시간(ex 2023-01-01 01:00:00)</param>
		/// <param name="EndTime">종료시간(ex 2023-01-01 01:01:00)</param>
		/// <param name="UserName">유저명</param>
		/// <param name="BucketName">버킷명</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <returns>결과</returns>
		public async Task<ResponseList<ResponseS3ErrorUsage>> GetBackendErrors(string StartTime, string EndTime, string UserName = null, string BucketName = null, int Skip = 0, int CountPerPage = 100)
		{
			var Result = new ResponseList<ResponseS3ErrorUsage>();

			try
			{
				// 시작시간과 종료시간을 DateTime으로 변환한다.
				if (!ConvertToDateTime(StartTime, out DateTime SearchStartTime) || !ConvertToDateTime(EndTime, out DateTime SearchEndTime))
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}
				// 시작시간이 종료시간보다 크면 에러를 리턴한다.
				if (SearchStartTime > SearchEndTime)
				{
					Result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					Result.Message = Resource.EM_COMMON__INVALID_DATE;
					return Result;
				}

				// 에러 횟수를 조회한다.
				Result.Data = await m_dbContext.BackendErrorMeters.AsNoTracking()
					.Where(i => i.InDate <= SearchEndTime && i.InDate >= SearchStartTime && i.UserName != "-" && (UserName == null || i.UserName == UserName) && (BucketName == null || i.BucketName == BucketName))
					.GroupBy(i => new { i.UserName, i.BucketName }, (Key, Items) => new
					{
						Key.UserName,
						Key.BucketName,
						ClientError = Items.Sum(i => i.ClientErrorCount),
						ServerError = Items.Min(i => i.ServerErrorCount)
					})
					.CreateListAsync<dynamic, ResponseS3ErrorUsage>(Skip, CountPerPage);

				// 결과가 없을 경우 빈 데이터 생성
				Result.Data ??= new QueryResults<ResponseS3ErrorUsage>();
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}
		#endregion

		#region Util

		/// <summary> 날짜를 DateTime으로 변환한다.</summary>
		static public bool ConvertToDateTime(string StrDate, out DateTime Result)
		{
			if (string.IsNullOrEmpty(StrDate))
			{
				Result = DateTime.MinValue;
				return false;
			}
			else if (StrDate.Length == 10)
			{
				var value = 0L;
				if (long.TryParse(StrDate, out value))
				{
					DateTime dt = DateTime.UnixEpoch;
					Result = dt.AddSeconds(value).ToLocalTime();
					return true;
				}
			}
			else if (StrDate.Length == 13)
			{
				var value = 0L;
				if (long.TryParse(StrDate, out value))
				{
					DateTime dt = DateTime.UnixEpoch;
					Result = dt.AddMilliseconds(value).ToLocalTime();
					return true;
				}
			}
			return DateTime.TryParse(StrDate, out Result);
		}
		#endregion
	}
}