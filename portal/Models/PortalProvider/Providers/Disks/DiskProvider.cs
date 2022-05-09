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
using Microsoft.EntityFrameworkCore.Storage;
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
		/// <param name="serverId">서버 아이디</param>
		/// <param name="request">디스크 등록 요청 객체</param>
		/// <returns>디스크 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseDiskWithServer>> Add(string serverId, RequestDisk request)
		{
			ResponseData<ResponseDiskWithServer> result = new ResponseData<ResponseDiskWithServer>();
			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 디스크 풀 아이디가 존재하고 유효한 Guid가 아닌 경우
				Guid guidDiskPoolId = Guid.Empty;
				if (!request.DiskPoolId.IsEmpty() && !Guid.TryParse(request.DiskPoolId, out guidDiskPoolId))
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_POOL_ID);

				// 디스크 풀 아이디가 유효한 경우
				if (guidDiskPoolId != Guid.Empty)
				{
					// 해당 디스크 풀 정보를 가져온다.
					DiskPool diskPool = await this.m_dbContext.DiskPools.AsNoTracking()
						.Where(i => i.Id == guidDiskPoolId)
						.FirstOrDefaultAsync();

					// 해당 디스크 풀이 존재하지 않는 경우
					if (diskPool == null)
						return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_THERE_IS_NO_DISK_POOL);
				}

				// after remove
				// 디스크가 이미 마운트되어 있는지 확인 요청
				ResponseData response = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.servers.{serverId}.disks.check_mount", new
				{
					ServerId = serverId,
					request.Path
				}, 10);

				// 실패인 경우
				if (response.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, response.Code, response.Message);

				Guid newId = Guid.NewGuid();
				uint diskNo = (uint)newId.GetHashCode();
				string diskNoString = $"{diskNo:0000000000}";

				// 디스크 아이디 기록 요청
				ResponseData responseWriteDiskId = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.servers.{serverId}.disks.write_disk_id", new
				{
					Id = newId.ToString(),
					ServerId = serverId,
					request.DiskPoolId,
					DiskNo = diskNoString,
					request.Path
				}, 10);

				// 실패인 경우
				if (responseWriteDiskId.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseDiskWithServer>(EnumResponseResult.Error, responseWriteDiskId.Code, responseWriteDiskId.Message);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						Disk newData = new Disk()
						{
							Id = newId,
							DiskPoolId = guidDiskPoolId == Guid.Empty ? null : guidDiskPoolId,
							DiskNo = diskNoString,
							ServerId = guidServerId,
							Path = request.Path,
							State = (EnumDbDiskState)request.State,
							TotalInode = request.TotalInode,
							ReservedInode = request.ReservedInode,
							UsedInode = request.UsedInode,
							TotalSize = request.TotalSize,
							ReservedSize = request.ReservedSize,
							UsedSize = request.UsedSize,
							RwMode = (EnumDbDiskRwMode)request.RwMode
						};
						await m_dbContext.Disks.AddAsync(newData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
						result.Data = (await this.Get(serverId, newData.Id.ToString())).Data;

						// 추가된 디스크 정보 전송
						// SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.added", result.Data);
						// 디스크풀에 추가된 디스크일 경우 추가된 디스크 정보 전송
						if (newData.DiskPoolId != null) SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", result.Data);
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>디스크 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="request">디스크 수정 요청 객체</param>
		/// <returns>디스크 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string serverId, string id, RequestDisk request)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 디스크 풀 아이디가 존재하고 유효한 Guid가 아닌 경우
				Guid guidDiskPoolId = Guid.Empty;
				if (!request.DiskPoolId.IsEmpty() && !Guid.TryParse(request.DiskPoolId, out guidDiskPoolId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_POOL_ID);

				// 디스크 풀 아이디가 유효한 경우
				if (guidDiskPoolId != Guid.Empty)
				{
					// 해당 디스크 풀 정보를 가져온다.
					DiskPool diskPool = await this.m_dbContext.DiskPools.AsNoTracking()
						.Where(i => i.Id == guidDiskPoolId)
						.FirstOrDefaultAsync();

					// 해당 디스크 풀이 존재하지 않는 경우
					if (diskPool == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_THERE_IS_NO_DISK_POOL);
				}

				// 해당 정보를 가져온다.
				Disk exist = await m_dbContext.Disks
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 마운트 경로가 변경되는 경우
				if (exist.Path != request.Path)
				{
					// 디스크가 이미 마운트되어 있는지 확인 요청
					ResponseData response = SendRpcMq(RabbitMqConfiguration.ExchangeName, $"*.servers.{serverId}.disks.check_mount", new
					{
						ServerId = serverId,
						request.Path
					}, 10);

					// 실패인 경우
					if (response.Result != EnumResponseResult.Success)
						return new ResponseData(EnumResponseResult.Error, response.Code, response.Message);
				}

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.DiskPoolId = guidDiskPoolId == Guid.Empty ? null : guidDiskPoolId;
						exist.Path = request.Path;
						exist.State = (EnumDbDiskState)request.State;
						exist.TotalInode = request.TotalInode;
						exist.ReservedInode = request.ReservedInode;
						exist.UsedInode = request.UsedInode;
						exist.TotalSize = request.TotalSize;
						exist.ReservedSize = request.ReservedSize;
						exist.UsedSize = request.UsedSize;
						exist.RwMode = (EnumDbDiskRwMode)request.RwMode;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;

						// 상세 정보를 가져온다.
						ResponseDiskWithServices response = (await this.Get(serverId, id)).Data;

						// // 수정된 디스크 정보 전송
						// SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.updated", response);

						// 디스크풀에 추가된 디스크일 경우 수정된 디스크 정보 전송
						if (exist.DiskPoolId != null) SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", response);
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>디스크 상태 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="state">디스크 상태</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(string serverId, string id, EnumDiskState state)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Disk exist = await m_dbContext.Disks
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.State = (EnumDbDiskState)state;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;

						// // 디스크 상태 수정 전송
						// SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.state", new { exist.Id, exist.ServerId, exist.DiskPoolId, exist.DiskNo, State = (EnumDiskState)exist.State});

						// 디스크풀에 추가된 디스크일 경우 디스크 상태 수정 전송
						if (exist.DiskPoolId != null) SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", new { exist.Id, exist.ServerId, exist.DiskPoolId, exist.DiskNo, State = (EnumDiskState)exist.State});
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>디스크 상태 수정</summary>
		/// <param name="request">상태 수정 요청 객체</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(RequestDiskState request)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 디스크 아이디와 디스크 식별번호가 모두 없는 경우
				if (request.Id.IsEmpty() && request.DiskNo.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_DISK_ID_OR_NO);

				// 디스크 아이디가 존재하고 아이디가 유효하지 않은 경우
				Guid guidDiskId = Guid.Empty;
				if (!request.Id.IsEmpty() && !Guid.TryParse(request.Id, out guidDiskId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(request.ServerId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 해당 정보를 가져온다.
				Disk exist = await m_dbContext.Disks.AsNoTracking()
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && (i.Id == guidDiskId || i.DiskNo == request.DiskNo));

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 서비스 상태 수정
				result = await UpdateState(request.ServerId, exist.Id.ToString(), request.State);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>디스크 크기 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="totalInode">전체 Inode 수</param>
		/// <param name="reservedInode">예약/시스템 Inode 수</param>
		/// <param name="usedInode">사용된 Inode 수</param>
		/// <param name="totalSize">전체 크기</param>
		/// <param name="reservedSize">예약/시스템 크기</param>
		/// <param name="usedSize">사용된 크기</param>
		/// <returns>디스크 크기 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateSize(string serverId, string id, decimal totalInode, decimal reservedInode, decimal usedInode, decimal totalSize, decimal reservedSize, decimal usedSize)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Disk exist = await m_dbContext.Disks
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.TotalInode = totalInode;
						exist.ReservedInode = reservedInode;
						exist.UsedInode = usedInode;
						exist.TotalSize = totalSize;
						exist.ReservedSize = reservedSize;
						exist.UsedSize = usedSize;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>디스크 크기 수정</summary>
		/// <param name="request">디스크 크기 수정 요청 객체</param>
		/// <returns>디스크 크기 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateSize(RequestDiskSize request)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 디스크 아이디와 디스크 식별번호가 모두 없는 경우
				if (request.Id.IsEmpty() && request.DiskNo.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_DISK_ID_OR_NO);

				// 디스크 아이디가 존재하고 아이디가 유효하지 않은 경우
				Guid guidDiskId = Guid.Empty;
				if (!request.Id.IsEmpty() && !Guid.TryParse(request.Id, out guidDiskId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(request.ServerId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 해당 정보를 가져온다.
				Disk exist = await m_dbContext.Disks.AsNoTracking()
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && (i.Id == guidDiskId || i.DiskNo == request.DiskNo));

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 디스크 크기 수정
				result = await UpdateSize(request.ServerId, exist.Id.ToString(), request.TotalInode, request.ReservedInode, request.UsedInode, request.TotalSize, request.ReservedSize, request.UsedSize);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>디스크 읽기/쓰기 모드 수정</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <param name="diskRwMode">디스크 읽기/쓰기 모드</param>
		/// <returns>디스크 읽기/쓰기 모드 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateRwMode(string serverId, string id, EnumDiskRwMode diskRwMode)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Disk exist = await m_dbContext.Disks
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						exist.RwMode = (EnumDbDiskRwMode)diskRwMode;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;

						// // 수정된 R/W 모드 정보 전송
						// SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.rwmode", new ResponseDiskRwMode().CopyValueFrom(exist));
						
						// 디스크풀에 추가된 디스크일 경우 수정된 R/W 모드 정보 전송
						if (exist.DiskPoolId != null) SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", new ResponseDiskRwMode().CopyValueFrom(exist));
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>디스크 읽기/쓰기 모드 수정</summary>
		/// <param name="request">디스크 읽기/쓰기 모드 수정 요청 객체</param>
		/// <returns>디스크 읽기/쓰기 모드 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateRwMode(RequestDiskRwMode request)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 디스크 아이디와 디스크 식별번호가 모두 없는 경우
				if (request.Id.IsEmpty() && request.DiskNo.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_DISK_ID_OR_NO);

				// 디스크 아이디가 존재하고 아이디가 유효하지 않은 경우
				Guid guidDiskId = Guid.Empty;
				if (!request.Id.IsEmpty() && !Guid.TryParse(request.Id, out guidDiskId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(request.ServerId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 해당 정보를 가져온다.
				Disk exist = await m_dbContext.Disks.AsNoTracking()
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && (i.Id == guidDiskId || i.DiskNo == request.DiskNo));

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 디스크 읽기/쓰기 모드 수정
				result = await UpdateRwMode(request.ServerId, exist.Id.ToString(), request.RwMode);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>디스크 삭제</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <returns>디스크 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string serverId, string id)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				Disk exist = await m_dbContext.Disks.AsNoTracking()
					.FirstOrDefaultAsync(i => i.ServerId == guidServerId && i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Success);

				// 연결된 디스크 풀이 존재하는 경우
				if (exist.DiskPool != null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REMOVE_AFTER_REMOVING_DISKPOOL);

				// 연결된 서비스가 존재하는 경우
				if (await m_dbContext.ServiceDisks.AnyAsync(i => i.DiskId == guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REMOVE_AFTER_REMOVING_SERVICE);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 해당 데이터 삭제
						m_dbContext.Disks.Remove(exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;

						// // 삭제된 디스크 정보 전송
						// SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.disks.removed", new ResponseDisk().CopyValueFrom(exist));

						// 디스크풀에 추가된 디스크일 경우 삭제된 디스크 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", new ResponseDisk().CopyValueFrom(exist));
					}
					catch (Exception ex)
					{
						await transaction.RollbackAsync();

						NNException.Log(ex);

						result.Code = Resource.EC_COMMON__EXCEPTION;
						result.Message = Resource.EM_COMMON__EXCEPTION;
					}
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>특정 서버의 디스크 목록을 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="searchStates">검색할 디스크 상태 목록</param>
		/// <param name="searchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>디스크 목록 객체</returns>
		public async Task<ResponseList<ResponseDisk>> GetList(
			string serverId,
			List<EnumDiskState> searchStates, List<EnumDiskRwMode> searchRwModes,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseDisk> result = new ResponseList<ResponseDisk>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseList<ResponseDisk>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseList<ResponseDisk>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("State", "desc");
				AddDefaultOrders("Path", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				// 목록을 가져온다.
				result.Data = await m_dbContext.Disks.AsNoTracking()
					.Where(i => i.ServerId == guidServerId
						&& (
							searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
							|| (searchFields.Contains("diskno") && i.DiskNo.Contains(searchKeyword))
							|| (searchFields.Contains("path") && i.Path.Contains(searchKeyword))
						)
						&& (searchStates == null || searchStates.Count == 0 || searchStates.Select(j => (int)j).Contains((int)i.State))
						&& (searchRwModes == null || searchRwModes.Count == 0 || searchRwModes.Select(j => (int)j).Contains((int)i.RwMode))
					)
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<Disk, ResponseDisk>(skip, countPerPage);

				result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>전체 디스크 목록을 가져온다.</summary>
		/// <param name="searchStates">검색할 디스크 상태 목록</param>
		/// <param name="searchRwModes">검색할 디스크 읽기/쓰기 모드 목록</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>디스크 목록 객체</returns>
		public async Task<ResponseList<ResponseDiskWithServer>> GetList(
			List<EnumDiskState> searchStates, List<EnumDiskRwMode> searchRwModes,
			int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseDiskWithServer> result = new ResponseList<ResponseDiskWithServer>();

			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("State", "desc");
				AddDefaultOrders("Path", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				// 목록을 가져온다.
				result.Data = await m_dbContext.Disks.AsNoTracking()
					.Where(i =>
						(
							searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
							|| (searchFields.Contains("diskno") && i.DiskNo.Contains(searchKeyword))
							|| (searchFields.Contains("path") && i.Path.Contains(searchKeyword))
						)
						&& (searchStates == null || searchStates.Count == 0 || searchStates.Select(j => (int)j).Contains((int)i.State))
						&& (searchRwModes == null || searchRwModes.Count == 0 || searchRwModes.Select(j => (int)j).Contains((int)i.RwMode))
					)
					.Include(i => i.Server)
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<Disk, ResponseDiskWithServer>(skip, countPerPage);

				result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>디스크 정보를 가져온다.</summary>
		/// <param name="serverId">서버 아이디</param>
		/// <param name="id">디스크 아이디</param>
		/// <returns>디스크 정보 객체</returns>
		public async Task<ResponseData<ResponseDiskWithServices>> Get(string serverId, string id)
		{
			ResponseData<ResponseDiskWithServices> result = new ResponseData<ResponseDiskWithServices>();

			try
			{
				// 서버 아이디가 존재하지 않는 경우
				if (serverId.IsEmpty())
					return new ResponseData<ResponseDiskWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(serverId, out Guid guidServerId))
					return new ResponseData<ResponseDiskWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData<ResponseDiskWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				ResponseDiskWithServices exist = await m_dbContext.Disks.AsNoTracking()
					.Where(i => i.ServerId == guidServerId && i.Id == guidId)
					.Include(i => i.Server)
					.Include(i => i.DiskPool)
					.FirstOrDefaultAsync<Disk, ResponseDiskWithServices>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<ResponseDiskWithServices>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 연결된 서비스 목록을 가져온다.
				List<ResponseService> services = await m_dbContext.Services.AsNoTracking()
					.Where(i => i.ServiceDisks.Any(j => j.DiskId == guidId))
					.ToListAsync<Service, ResponseService>();
				if (services != null)
					exist.Services = services;

				result.Data = exist;
				result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}

		/// <summary>DiskNo로 디스크 ID를 가져온다.</summary>
		/// <param name="diskNo">디스크 No</param>
		/// <returns>디스크 아이디 응답 객체</returns>
		public async Task<ResponseData<ResponseDiskId>> Get(string diskNo)
		{
			ResponseData<ResponseDiskId> result = new ResponseData<ResponseDiskId>();

			try
			{
				// 디스크 번호가 존재하지 않는 경우
				if (diskNo.IsEmpty())
					return new ResponseData<ResponseDiskId>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				ResponseDiskId exist = await m_dbContext.Disks.AsNoTracking()
					.Where(i => i.DiskNo == diskNo)
					.FirstOrDefaultAsync<Disk, ResponseDiskId>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<ResponseDiskId>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				result.Data = exist;
				result.Result = EnumResponseResult.Success;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}

			return result;
		}
	}
}