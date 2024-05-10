using System;
using System.Data.Entity;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore.Storage;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.EntityFramework;
using MTLib.NetworkData;
using MTLib.Reflection;
using PortalData;
using PortalData.Requests.S3;
using PortalData.Response.S3;
using PortalModels;
using PortalProviderInterface;
using PortalResources;

namespace PortalProvider.Providers.S3
{
	/// <summary>S3 프로바이더 클래스</summary>
	public class S3Provider : BaseProvider<PortalModel>, IS3Provider
	{

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="logger">로거</param>
		/// <param name="systemInformationProvider">시스템 정보 프로바이더</param>
		public S3Provider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<S3Provider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		/// <summary>버킷에 로깅을 설정한다.</summary>
		/// <param name="Request">버킷 로깅 정보 요청 객체</param>
		/// <returns>설정 성공 여부</returns>
		public async Task<ResponseData> SetBucketLogging(RequestS3BucketLogging Request)
		{
			var Result = new ResponseData();

			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// TODO: 버킷 로깅 설정
				Result.Message = "버킷에 로깅을 설정하였습니다.";
				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>접근 아이피 주소를 등록한다.</summary>
		/// <param name="Request">접근 아이피 주소 정보 요청 객체</param>
		/// <returns>등록 성공 여부 및 정보 반환</returns>
		public async Task<ResponseData<ResponseS3AccessIp>> AddAccessIp(RequestS3AccessIp Request)
		{
			var Result = new ResponseData<ResponseS3AccessIp>();

			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseS3AccessIp>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청한 아이피 주소가 유효하지 않은 경우
				if (!IpAddressValue.TryParse(Request.IpAddress, out IpAddressValue ipAddressValue))
					return new ResponseData<ResponseS3AccessIp>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_IP_ADDRESS, Resource.EM_COMMON__INVALID_IP_ADDRESS);

				using IDbContextTransaction Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					var Item = new S3AccessIp()
					{
						AddressId = Guid.NewGuid().ToString(),
						UserId = Request.UserId,
						BucketName = Request.BucketName,
						StartIpNo = ipAddressValue.Min,
						EndIpNo = ipAddressValue.Max,
						IpAddress = Request.IpAddress,
						ModId = LoginUserId,
						ModName = LoginUserName,
					};

					// 접근 아이피 주소 추가
					await m_dbContext.S3AccessIps.AddAsync(Item);
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					await Transaction.CommitAsync();

					//KJW Add
					Result.Data.CopyValueFrom(Item);
					Result.Result = EnumResponseResult.Success;

				}
				catch (Exception ex)
				{
					await Transaction.RollbackAsync();

					NNException.Log(ex);

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__EXCEPTION;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>접근 아이피 주소를 수정한다.</summary>
		/// <param name="AddressId">수정할 주소 아이디</param>
		/// <param name="Request">접근 아이피 주소 정보 요청 객체</param>
		/// <returns>수정 성공 여부</returns>
		public async Task<ResponseData> UpdateAccessIp(string AddressId, RequestS3AccessIp Request)
		{
			var Result = new ResponseData();

			try
			{
				// 주소 아이디가 유효하지 않은 경우
				if (AddressId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 요청한 아이피 주소가 유효하지 않은 경우
				if (!IpAddressValue.TryParse(Request.IpAddress, out IpAddressValue ipAddressValue))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_IP_ADDRESS, Resource.EM_COMMON__INVALID_IP_ADDRESS);

				// 해당 접근 아이피 주소 객체를 가져온다.
				var s3AccessIp = await m_dbContext.S3AccessIps
					.Where(i => i.AddressId == AddressId)
					.FirstOrDefaultAsync<dynamic, S3AccessIp>();

				// 해당 데이터가 존재하지 않는 경우
				if (s3AccessIp == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using IDbContextTransaction Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					// 접근 아이피 주소 수정
					s3AccessIp.UserId = Request.UserId;
					s3AccessIp.BucketName = Request.BucketName;
					s3AccessIp.StartIpNo = ipAddressValue.Min;
					s3AccessIp.EndIpNo = ipAddressValue.Max;
					s3AccessIp.IpAddress = Request.IpAddress;
					s3AccessIp.ModId = LoginUserId;
					s3AccessIp.ModName = LoginUserName;
					if (m_dbContext.HasChanges())
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					await Transaction.CommitAsync();

					Result.Result = EnumResponseResult.Success;
				}
				catch (Exception ex)
				{
					await Transaction.RollbackAsync();

					NNException.Log(ex);

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__EXCEPTION;
				}

			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>접근 아이피 주소를 삭제한다.</summary>
		/// <param name="AddressId">삭제할 주소 아이디</param>
		/// <returns>삭제 성공 여부</returns>
		public async Task<ResponseData> RemoveAccessIp(string AddressId)
		{
			var Result = new ResponseData();

			try
			{
				// 주소 아이디가 유효하지 않은 경우
				if (AddressId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 접근 아이피 주소 객체를 가져온다.
				var s3AccessIp = await m_dbContext.S3AccessIps.Where(i => i.AddressId == AddressId).FirstOrDefaultAsync<dynamic, S3AccessIp>();

				// 해당 데이터가 존재하지 않는 경우
				if (s3AccessIp == null)
					return new ResponseData(EnumResponseResult.Success);

				using IDbContextTransaction Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					// 접근 아이피 주소 삭제
					m_dbContext.S3AccessIps.Remove(s3AccessIp);
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					await Transaction.CommitAsync();

					Result.Result = EnumResponseResult.Success;
				}
				catch (Exception ex)
				{
					await Transaction.RollbackAsync();

					NNException.Log(ex);

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__EXCEPTION;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>접근 아이피 주소를 삭제한다.</summary>
		/// <param name="UserId">유저 아이디</param>
		/// <param name="BucketName">버킷 이름</param>
		/// <returns>삭제 성공 여부</returns>
		public async Task<ResponseData> RemoveAccessIp(string UserId, string BucketName)
		{
			var Result = new ResponseData();

			try
			{
				var S3AccessIps = await m_dbContext.S3AccessIps.AsNoTracking()
					.Where(i => UserId.IsEmpty() || i.UserId == UserId)
					.Where(i => BucketName.IsEmpty() || i.BucketName == BucketName)
					.CreateListAsync<S3AccessIp>();

				// 해당 데이터가 존재하지 않는 경우
				if (S3AccessIps.Items == null || S3AccessIps.Items.Count == 0)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				using IDbContextTransaction Transaction = await m_dbContext.Database.BeginTransactionAsync();
				try
				{
					// 접근 아이피 주소 삭제
					foreach (var Item in S3AccessIps.Items)
					{
						m_dbContext.S3AccessIps.Remove(Item);

					}
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

					await Transaction.CommitAsync();

					Result.Result = EnumResponseResult.Success;
				}
				catch (Exception ex)
				{
					await Transaction.RollbackAsync();

					NNException.Log(ex);

					Result.Code = Resource.EC_COMMON__EXCEPTION;
					Result.Message = Resource.EM_COMMON__EXCEPTION;
				}

			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>접근 아이피 주소 상세 정보를 가져온다.</summary>
		/// <param name="AddressId">주소 아이디</param>
		/// <returns>접근 아이피 주소 정보</returns>
		public async Task<ResponseData<ResponseS3AccessIp>> GetAccessIp(string AddressId)
		{
			var Result = new ResponseData<ResponseS3AccessIp>();

			try
			{
				// 주소 아이디가 유효하지 않은 경우
				if (AddressId.IsEmpty())
					return new ResponseData<ResponseS3AccessIp>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 접근 아이피 주소 객체를 가져온다.
				Result.Data = await m_dbContext.S3AccessIps.AsNoTracking()
					.Where(i => i.AddressId == AddressId)
					.FirstOrDefaultAsync<S3AccessIp, ResponseS3AccessIp>();

				// 해당 데이터가 존재하지 않는 경우
				if (Result.Data == null)
					return new ResponseData<ResponseS3AccessIp>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				IPAddress ipAddress = ((uint)Result.Data.StartIpNo).ToIPAddress();
				if (ipAddress != null)
					Result.Data.StartIpAddress = ipAddress.ToString();
				ipAddress = ((uint)Result.Data.EndIpNo).ToIPAddress();
				if (ipAddress != null)
					Result.Data.EndIpAddress = ipAddress.ToString();

				Result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Result = EnumResponseResult.Error;
				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>접근 아이피 주소 목록을 가져온다.</summary>
		/// <param name="loginUserId">로그인 사용자 아이디 객체</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="UserId">유저 아이디</param>
		/// <param name="BucketName">버킷 이름</param>
		/// <returns>S3 사용현황 목록 객체</returns>
		public async Task<ResponseList<ResponseS3AccessIp>> GetAccessIps(int Skip = 0, int CountPerPage = 100, string UserId = null, string BucketName = null)
		{
			var result = new ResponseList<ResponseS3AccessIp>();

			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("IpAddress", "asc");

				// 로그 목록을 가져온다.
				result.Data = await m_dbContext.S3AccessIps.AsNoTracking()
					.Where(i => UserId.IsEmpty() || i.UserId == UserId)
					.Where(i => BucketName.IsEmpty() || i.BucketName == BucketName)
					.CreateListAsync<S3AccessIp, ResponseS3AccessIp>(Skip, CountPerPage);

				// 모든 접근 아이피에 대해서 처리
				foreach (ResponseS3AccessIp accessIp in result.Data.Items)
				{
					IPAddress ipAddress = ((uint)accessIp.StartIpNo).ToIPAddress();
					if (ipAddress != null)
						accessIp.StartIpAddress = ipAddress.ToString();
					ipAddress = ((uint)accessIp.EndIpNo).ToIPAddress();
					if (ipAddress != null)
						accessIp.EndIpAddress = ipAddress.ToString();
				}

				result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Result = EnumResponseResult.Error;
				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}
	}
}