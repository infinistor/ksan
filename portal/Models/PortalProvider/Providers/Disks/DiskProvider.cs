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
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Requests.Disks;
using PortalData.Responses.Disks;
using PortalData.Responses.Services;
using PortalModels;
using PortalProvider.Providers.RabbitMq;
using PortalProviderInterface;
using PortalResources;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.EntityFramework;
using MTLib.Reflection;

namespace PortalProvider.Providers.Disks
{
	/// <summary>디스크 데이터 프로바이더 클래스</summary>
	public class DiskProvider : BaseProvider<PortalModel>, IDiskProvider
	{
		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		public DiskProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<DiskProvider> logger
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
		}

		/// <summary>디스크 등록</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Request">디스크 등록 요청 객체</param>
		/// <returns>디스크 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseDiskWithServer>> Add(string ServerId, RequestDisk Request)
		{
			ResponseData<ResponseDiskWithServer> Result = new ResponseData<ResponseDiskWithServer>();
			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 디스크 풀 아이디가 존재하고 유효한 Guid가 아닌 경우
				Guid GuidDiskPoolId = Guid.Empty;
				if (!Request.DiskPoolId.IsEmpty() && !Guid.TryParse(Request.DiskPoolId, out GuidDiskPoolId))
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_POOL_ID);

				// 디스크 풀 아이디가 유효한 경우
				if (GuidDiskPoolId != Guid.Empty)
				{
					// 해당 디스크 풀 정보를 가져온다.
					DiskPool diskPool = await this.m_dbContext.DiskPools.AsNoTracking()
						.Where(i => i.Id == GuidDiskPoolId)
						.FirstOrDefaultAsync();

					// 해당 디스크 풀이 존재하지 않는 경우
					if (diskPool == null)
						return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_THERE_IS_NO_DISK_POOL);
				}

				// after remove
				// 디스크가 이미 마운트되어 있는지 확인 요청
				var Response = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.servers.{ServerId}.disks.check_mount", new
				{
					ServerId = ServerId,
					Request.Path
				}, 10);

				// 실패인 경우
				if (Response.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Response.Code, Response.Message);

				var NewId = Guid.NewGuid();
				var DiskNo = (uint)NewId.GetHashCode();
				var DiskNoString = $"{DiskNo:0000000000}";

				// 디스크 아이디 기록 요청
				var ResponseWriteDiskId = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.servers.{ServerId}.disks.write_disk_id", new
				{
					Id = NewId.ToString(),
					ServerId = ServerId,
					Request.DiskPoolId,
					DiskNo = DiskNoString,
					Request.Path
				}, 10);

				// 실패인 경우
				if (ResponseWriteDiskId.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, ResponseWriteDiskId.Code, ResponseWriteDiskId.Message);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						var NewData = new Disk()
						{
							Id = NewId,
							DiskPoolId = GuidDiskPoolId == Guid.Empty ? null : GuidDiskPoolId,
							DiskNo = DiskNoString,
							ServerId = GuidServerId,
							Path = Request.Path,
							State = (EnumDbDiskState)Request.State,
							TotalInode = Request.TotalInode,
							ReservedInode = Request.ReservedInode,
							UsedInode = Request.UsedInode,
							TotalSize = Request.TotalSize,
							ReservedSize = Request.ReservedSize,
							UsedSize = Request.UsedSize,
							RwMode = (EnumDbDiskRwMode)Request.RwMode
						};
						await m_dbContext.Disks.AddAsync(NewData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Data = (await this.Get(ServerId, NewData.Id.ToString())).Data;

						// 디스크 추가 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.added", Result.Data);

						// 디스크풀에 추가된 디스크일 경우 추가된 디스크 정보 전송
						if (NewData.DiskPoolId != null) SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", Result.Data);
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>디스크 수정</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <param name="Request">디스크 수정 요청 객체</param>
		/// <returns>디스크 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string ServerId, string Id, RequestDisk Request)
		{
			var Result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 디스크 풀 아이디가 존재하고 유효한 Guid가 아닌 경우
				Guid GuidDiskPoolId = Guid.Empty;
				if (!Request.DiskPoolId.IsEmpty() && !Guid.TryParse(Request.DiskPoolId, out GuidDiskPoolId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_POOL_ID);

				// 디스크 풀 아이디가 유효한 경우
				if (GuidDiskPoolId != Guid.Empty)
				{
					// 해당 디스크 풀 정보를 가져온다.
					var DiskPool = await this.m_dbContext.DiskPools.AsNoTracking()
						.Where(i => i.Id == GuidDiskPoolId)
						.FirstOrDefaultAsync();

					// 해당 디스크 풀이 존재하지 않는 경우
					if (DiskPool == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_THERE_IS_NO_DISK_POOL);
				}

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 마운트 경로가 변경되는 경우
				if (Exist.Path != Request.Path)
				{
					// 디스크가 이미 마운트되어 있는지 확인 요청
					var Response = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.servers.{ServerId}.disks.check_mount", new
					{
						ServerId = ServerId,
						Request.Path
					}, 10);

					// 실패인 경우
					if (Response.Result != EnumResponseResult.Success)
						return new ResponseData(EnumResponseResult.Error, Response.Code, Response.Message);
				}

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.DiskPoolId = GuidDiskPoolId == Guid.Empty ? null : GuidDiskPoolId;
						Exist.Path = Request.Path;
						Exist.State = (EnumDbDiskState)Request.State;
						Exist.TotalInode = Request.TotalInode;
						Exist.ReservedInode = Request.ReservedInode;
						Exist.UsedInode = Request.UsedInode;
						Exist.TotalSize = Request.TotalSize;
						Exist.ReservedSize = Request.ReservedSize;
						Exist.UsedSize = Request.UsedSize;
						Exist.RwMode = (EnumDbDiskRwMode)Request.RwMode;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 상세 정보를 가져온다.
						var Response = (await this.Get(ServerId, Id)).Data;

						// 디스크 추가 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.updated", Response);

						// 디스크풀에 추가된 디스크일 경우 수정된 디스크 정보 전송
						if (Exist.DiskPoolId != null) SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", Response);
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>디스크 상태 수정</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <param name="State">디스크 상태</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(string ServerId, string Id, EnumDiskState State)
		{
			var Result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.State = (EnumDbDiskState)State;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						var Message = new { Exist.Id, Exist.ServerId, Exist.DiskPoolId, Exist.DiskNo, State = (EnumDiskState)Exist.State };

						// 디스크 상태 수정 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.state", Message);

						// 디스크풀에 추가된 디스크일 경우 디스크 상태 수정 전송
						if (Exist.DiskPoolId != null) SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", Message);
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>디스크 상태 수정</summary>
		/// <param name="Request">상태 수정 요청 객체</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(RequestDiskState Request)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 디스크 아이디와 디스크 식별번호가 모두 없는 경우
				if (Request.Id.IsEmpty() && Request.DiskNo.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_DISK_ID_OR_NO);

				// 디스크 아이디가 존재하고 아이디가 유효하지 않은 경우
				var GuidDiskId = Guid.Empty;
				if (!Request.Id.IsEmpty() && !Guid.TryParse(Request.Id, out GuidDiskId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(Request.ServerId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.Disks.AsNoTracking()
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && (i.Id == GuidDiskId || i.DiskNo == Request.DiskNo));

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 서비스 상태 수정
				Result = await UpdateState(Request.ServerId, Exist.Id.ToString(), Request.State);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>디스크 사용 정보 수정</summary>
		/// <param name="Request">디스크 사용 정보 수정 요청 객체</param>
		/// <returns>디스크 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(RequestDiskUsage Request)
		{
			var Result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 디스크 아이디와 디스크 식별번호가 모두 없는 경우
				if (Request.Id.IsEmpty() && Request.DiskNo.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_DISK_ID_OR_NO);

				// 디스크 아이디가 존재하고 아이디가 유효하지 않은 경우
				Guid DiskId = Guid.Empty;
				if (!Request.Id.IsEmpty() && !Guid.TryParse(Request.Id, out DiskId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(Request.ServerId, out Guid ServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.ServerId == ServerId && (i.Id == DiskId || i.DiskNo == Request.DiskNo));

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.TotalInode = Request.TotalInode;
						Exist.ReservedInode = Request.ReservedInode;
						Exist.UsedInode = Request.UsedInode;
						Exist.TotalSize = Request.TotalSize;
						Exist.ReservedSize = Request.ReservedSize;
						Exist.UsedSize = Request.UsedSize;
						Exist.Read = Request.Read;
						Exist.Write = Request.Write;

						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 사용정보 추가
						m_dbContext.DiskUsages.Add(new DiskUsage()
						{
							Id = DiskId,
							RegDate = DateTime.Now,
							UsedInode = Request.UsedInode,
							UsedSize = Request.UsedSize,
							Read = Request.Read,
							Write = Request.Write,
						});
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						await Transaction.CommitAsync();

						// 서비스 상태가 변경될 경우
						if (Exist.State != (EnumDbDiskState)Request.State)
							Result = await UpdateState(Request.ServerId, Exist.Id.ToString(), Request.State);

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
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>디스크 읽기/쓰기 모드 수정</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <param name="DiskRwMode">디스크 읽기/쓰기 모드</param>
		/// <returns>디스크 읽기/쓰기 모드 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateRwMode(string ServerId, string Id, EnumDiskRwMode DiskRwMode)
		{
			var Result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.ServerId == GuidServerId && i.Id == GuidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.RwMode = (EnumDbDiskRwMode)DiskRwMode;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 수정된 R/W 모드 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.rwmode", new ResponseDiskRwMode().CopyValueFrom(Exist));

						// 디스크풀에 추가된 디스크일 경우 수정된 R/W 모드 정보 전송
						if (Exist.DiskPoolId != null) SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", new ResponseDiskRwMode().CopyValueFrom(Exist));
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>디스크 읽기/쓰기 모드 수정</summary>
		/// <param name="Request">디스크 읽기/쓰기 모드 수정 요청 객체</param>
		/// <returns>디스크 읽기/쓰기 모드 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateRwMode(RequestDiskRwMode Request)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 디스크 아이디와 디스크 식별번호가 모두 없는 경우
				if (Request.Id.IsEmpty() && Request.DiskNo.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_DISK_ID_OR_NO);

				// 디스크 아이디가 존재하고 아이디가 유효하지 않은 경우
				Guid GuidDiskId = Guid.Empty;
				if (!Request.Id.IsEmpty() && !Guid.TryParse(Request.Id, out GuidDiskId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(Request.ServerId, out Guid GuidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.Disks.AsNoTracking()
					.FirstOrDefaultAsync(i => i.ServerId == GuidServerId && (i.Id == GuidDiskId || i.DiskNo == Request.DiskNo));

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 디스크 읽기/쓰기 모드 수정
				Result = await UpdateRwMode(Request.ServerId, Exist.Id.ToString(), Request.RwMode);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>디스크 삭제</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <returns>디스크 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string ServerId, string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.Disks.AsNoTracking()
					.FirstOrDefaultAsync(i => i.ServerId == GuidServerId && i.Id == GuidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Success);

				// 연결된 디스크 풀이 존재하는 경우
				if (Exist.DiskPool != null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REMOVE_AFTER_REMOVING_DISKPOOL);

				// 연결된 서비스가 존재하는 경우
				if (await m_dbContext.ServiceDisks.AnyAsync(i => i.DiskId == GuidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REMOVE_AFTER_REMOVING_SERVICE);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 해당 데이터 삭제
						m_dbContext.Disks.Remove(Exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 삭제된 디스크 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.removed", new ResponseDisk().CopyValueFrom(Exist));

						// 디스크풀에 추가된 디스크일 경우 삭제된 디스크 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", new ResponseDisk().CopyValueFrom(Exist));
					}
					catch (Exception ex)
					{
						await Transaction.RollbackAsync();

						NNException.Log(ex);

						Result.Code = Resource.EC_COMMON__EXCEPTION;
						Result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				Result.Code = Resource.EC_COMMON__EXCEPTION;
				Result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return Result;
		}

		/// <summary>특정 서버의 디스크 목록을 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="SearchStates">검색할 디스크 상태 목록</param>
		/// <param name="SearchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>디스크 목록 객체</returns>
		public async Task<ResponseList<ResponseDisk>> GetList(
			string ServerId,
			List<EnumDiskState> SearchStates, List<EnumDiskRwMode> SearchRwModes,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Result = new ResponseList<ResponseDisk>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseList<ResponseDisk>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseList<ResponseDisk>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("State", "desc");
				AddDefaultOrders("Path", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.Disks.AsNoTracking()
					.Where(i => i.ServerId == GuidServerId
						&& (
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("diskno") && i.DiskNo.Contains(SearchKeyword))
							|| (SearchFields.Contains("path") && i.Path.Contains(SearchKeyword))
						)
						&& (SearchStates == null || SearchStates.Count == 0 || SearchStates.Select(j => (int)j).Contains((int)i.State))
						&& (SearchRwModes == null || SearchRwModes.Count == 0 || SearchRwModes.Select(j => (int)j).Contains((int)i.RwMode))
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<Disk, ResponseDisk>(Skip, CountPerPage);

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

		/// <summary>전체 디스크 목록을 가져온다.</summary>
		/// <param name="SearchStates">검색할 디스크 상태 목록</param>
		/// <param name="SearchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>디스크 목록 객체</returns>
		public async Task<ResponseList<ResponseDiskWithServer>> GetList(
			List<EnumDiskState> SearchStates, List<EnumDiskRwMode> SearchRwModes,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Result = new ResponseList<ResponseDiskWithServer>();

			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("State", "desc");
				AddDefaultOrders("Path", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.Disks.AsNoTracking()
					.Where(i =>
						(
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("diskno") && i.DiskNo.Contains(SearchKeyword))
							|| (SearchFields.Contains("path") && i.Path.Contains(SearchKeyword))
						)
						&& (SearchStates == null || SearchStates.Count == 0 || SearchStates.Select(j => (int)j).Contains((int)i.State))
						&& (SearchRwModes == null || SearchRwModes.Count == 0 || SearchRwModes.Select(j => (int)j).Contains((int)i.RwMode))
					)
					.Include(i => i.Server)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<Disk, ResponseDiskWithServer>(Skip, CountPerPage);

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

		/// <summary>디스크 정보를 가져온다.</summary>
		/// <param name="ServerId">서버 아이디</param>
		/// <param name="Id">디스크 아이디</param>
		/// <returns>디스크 정보 객체</returns>
		public async Task<ResponseData<ResponseDiskWithServices>> Get(string ServerId, string Id)
		{
			ResponseData<ResponseDiskWithServices> Result = new ResponseData<ResponseDiskWithServices>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData<ResponseDiskWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(ServerId, out Guid GuidServerId))
					return new ResponseData<ResponseDiskWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid GuidId))
					return new ResponseData<ResponseDiskWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				var Exist = await m_dbContext.Disks.AsNoTracking()
					.Where(i => i.ServerId == GuidServerId && i.Id == GuidId)
					.Include(i => i.Server)
					.Include(i => i.DiskPool)
					.FirstOrDefaultAsync<Disk, ResponseDiskWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseDiskWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 연결된 서비스 목록을 가져온다.
				var Services = await m_dbContext.Services.AsNoTracking()
					.Where(i => i.ServiceDisks.Any(j => j.DiskId == GuidId))
					.ToListAsync<Service, ResponseService>();
				if (Services != null)
					Exist.Services = Services;

				Result.Data = Exist;
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

		/// <summary>DiskNo로 디스크 ID를 가져온다.</summary>
		/// <param name="DiskNo">디스크 No</param>
		/// <returns>디스크 아이디 응답 객체</returns>
		public async Task<ResponseData<ResponseDiskId>> Get(string DiskNo)
		{
			ResponseData<ResponseDiskId> Result = new ResponseData<ResponseDiskId>();

			try
			{
				// 디스크 번호가 존재하지 않는 경우
				if (DiskNo.IsEmpty())
					return new ResponseData<ResponseDiskId>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				ResponseDiskId Exist = await m_dbContext.Disks.AsNoTracking()
					.Where(i => i.DiskNo == DiskNo)
					.FirstOrDefaultAsync<Disk, ResponseDiskId>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseDiskId>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				Result.Data = Exist;
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
	}
}