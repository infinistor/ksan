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
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Requests.Disks;
using PortalData.Responses.Disks;
using PortalModels;
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

namespace PortalProvider.Providers.DiskGuids
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
		/// <param name="ServerId">서버 아이디 / 이름</param>
		/// <param name="Request">디스크 등록 요청 객체</param>
		/// <param name="DiskCheck">디스크 등록 확인 여부</param>
		/// <returns>디스크 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseDiskWithServerAndNetwork>> Add(string ServerId, RequestDisk Request, bool DiskCheck = true)
		{
			ResponseData<ResponseDiskWithServerAndNetwork> Result = new ResponseData<ResponseDiskWithServerAndNetwork>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 서버 아이디가 유효하지 않는 경우
				if (ServerId.IsEmpty())
					return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_SERVER_ID);

				// 서버 정보를 가져온다.
				Server Server = null;

				// 이름으로 조회할 경우
				if (!Guid.TryParse(ServerId, out Guid ServerGuid))
					Server = await m_dbContext.Servers.FirstOrDefaultAsync(i => i.Name == ServerId);
				// Id로 조회할경우
				else
					Server = await m_dbContext.Servers.FirstOrDefaultAsync(i => i.Id == ServerGuid);

				// 해당 정보가 존재하지 않는 경우
				if (Server == null)
					return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 이름이 유효하지 않을경우
				if (Request.Name.IsEmpty())
					return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 동일한 이름이 존재할 경우
				if (await this.IsNameExist(Request.Name))
					return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_COMMON_NAME_ALREADY_EXIST);


				// 디스크풀 정보가 존재할 경우
				Guid DiskPoolGuid = Guid.Empty;
				if (!Request.DiskPoolId.IsEmpty())
				{

					DiskPool Exist = null;
					// Id로 조회할경우
					if (Guid.TryParse(Request.DiskPoolId, out DiskPoolGuid))
						Exist = await m_dbContext.DiskPools.FirstOrDefaultAsync(i => i.Id == DiskPoolGuid);
					// 이름으로 조회할 경우
					else
						Exist = await m_dbContext.DiskPools.FirstOrDefaultAsync(i => i.Name == Request.DiskPoolId);

					// 해당 정보가 존재하지 않는 경우
					if (Exist == null)
						return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_DISK_POOL_DOES_NOT_EXIST);

					DiskPoolGuid = Exist.Id;
				}

				// 디스크 체크가 활성화 되어 있을 경우만 확인
				if (DiskCheck)
				{
					// 디스크가 이미 마운트되어 있는지 확인 요청
					var Response = SendRpcMq($"*.servers.{Server.Id.ToString()}.disks.check_mount", new { ServerId = Server.Id, Request.Path }, 10);

					// 실패인 경우
					if (Response.Result != EnumResponseResult.Success)
						return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, Response.Code, Response.Message);
				}

				// 아이디 생성
				var NewId = Guid.NewGuid();

				// 디스크 체크가 활성화 되어 있을 경우만 확인
				if (DiskCheck)
				{
					// 디스크 아이디 기록 요청
					var ResponseWriteDiskId = SendRpcMq($"*.servers.{Server.Id.ToString()}.disks.write_disk_id", new
					{
						Id = NewId.ToString(),
						ServerId = Server.Id.ToString(),
						RegId = LoginUserId,
						RegName = LoginUserName,
						RegDate = DateTime.Now,
						ModId = LoginUserId,
						ModName = LoginUserName,
						ModDate = DateTime.Now,
						Request.DiskPoolId,
						Request.Name,
						Request.Path,
					}, 10);

					// 실패인 경우
					if (ResponseWriteDiskId.Result != EnumResponseResult.Success)
						return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, ResponseWriteDiskId.Code, ResponseWriteDiskId.Message);
				}

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						var NewData = new Disk()
						{
							Id = NewId,
							DiskPoolId = DiskPoolGuid == Guid.Empty ? null : DiskPoolGuid,
							Name = Request.Name,
							ServerId = Server.Id,
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

						// 디스크 추가 정보 가져오기
						Result = await this.Get(NewData.Id.ToString());

						// 디스크 추가 정보 전송
						SendMq("*.servers.disks.added", Result.Data);
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
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <param name="Request">디스크 수정 요청 객체</param>
		/// <returns>디스크 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string Id, RequestDisk Request)
		{
			var Result = new ResponseData();

			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 서버를 변경할 경우
				Guid ServerGuid = Guid.Empty;
				if (!Request.ServerId.IsEmpty())
				{
					//서버 정보를 가져온다.
					Server Server = null;

					// Guid일 경우
					if (Guid.TryParse(Request.ServerId, out ServerGuid))
						Server = await this.m_dbContext.Servers.AsNoTracking().FirstOrDefaultAsync(i => i.Id == ServerGuid);
					// 이름일 경우
					else
						Server = await m_dbContext.Servers.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Request.ServerId);

					//유효한 서버가 아닐 경우
					if (Server == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

					ServerGuid = Server.Id;
				}

				// 디스크 풀을 변경할 경우
				Guid DiskPoolGuid = Guid.Empty;
				if (!Request.DiskPoolId.IsEmpty())
				{
					// 디스크 풀 정보를 가져온다.
					DiskPool DiskPool = null;
					// Id로 조회할경우
					if (Guid.TryParse(Request.DiskPoolId, out DiskPoolGuid))
						DiskPool = await m_dbContext.DiskPools.AsNoTracking().FirstOrDefaultAsync(i => i.Id == DiskPoolGuid);
					// 이름으로 조회할 경우
					else
						DiskPool = await m_dbContext.DiskPools.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Request.DiskPoolId);

					// 디스크 풀이 존재하지 않는 경우
					if (DiskPool == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_DISK_POOL_DOES_NOT_EXIST);

					DiskPoolGuid = DiskPool.Id;
				}

				// 해당 정보를 가져온다.
				Disk Exist = null;

				// 아이디로 가져올 경우
				if (Guid.TryParse(Id, out Guid DiskGuid))
					Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.Id == DiskGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_DISK_DOES_NOT_EXIST);

				// 동일한 이름이 존재할 경우
				if (await this.IsNameExist(Request.Name, Exist.Id))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_COMMON_NAME_ALREADY_EXIST);

				// 마운트 경로가 변경되는 경우
				if (Exist.Path != Request.Path)
				{
					// 디스크가 이미 마운트되어 있는지 확인 요청
					ResponseData Response;
					if (ServerGuid != Guid.Empty)
						Response = SendRpcMq($"*.servers.{ServerGuid}.disks.check_mount", new { ServerId = ServerGuid, Request.Path }, 10);
					else
						Response = SendRpcMq($"*.servers.{Exist.ServerId}.disks.check_mount", new { ServerId = Exist.ServerId, Request.Path }, 10);

					// 실패인 경우
					if (Response.Result != EnumResponseResult.Success)
						return new ResponseData(EnumResponseResult.Error, Response.Code, Response.Message);
				}

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.DiskPoolId = DiskPoolGuid == Guid.Empty ? null : DiskPoolGuid;
						if (ServerGuid != Guid.Empty) Exist.ServerId = ServerGuid;
						Exist.Name = Request.Name;
						Exist.Path = Request.Path;
						Exist.State = (EnumDbDiskState)Request.State;
						Exist.TotalInode = Request.TotalInode;
						Exist.ReservedInode = Request.ReservedInode;
						Exist.UsedInode = Request.UsedInode;
						Exist.TotalSize = Request.TotalSize;
						Exist.ReservedSize = Request.ReservedSize;
						Exist.UsedSize = Request.UsedSize;
						Exist.RwMode = (EnumDbDiskRwMode)Request.RwMode;
						Exist.ModId = LoginUserId;
						Exist.ModName = LoginUserName;
						Exist.ModDate = DateTime.Now;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 디스크 변경 정보 전송
						SendMq("*.servers.disks.updated", Exist);
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
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <param name="State">디스크 상태</param>
		/// <returns>서버 상태 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateState(string Id, EnumDiskState State)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 해당 정보를 가져온다.
				Disk Exist = null;

				// 아이디로 가져올 경우
				if (Guid.TryParse(Id, out Guid DiskGuid))
					Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.Id == DiskGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_DISK_DOES_NOT_EXIST);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.State = (EnumDbDiskState)State;
						Exist.ModId = LoginUserId != Guid.Empty ? LoginUserId : null;
						Exist.ModName = LoginUserName;
						Exist.ModDate = DateTime.Now;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;


						// 디스크 상태 수정 전송
						var Message = new { Exist.Id, Exist.ServerId, Exist.DiskPoolId, Exist.Name, State = (EnumDiskState)Exist.State };
						SendMq("*.servers.disks.state", Message);
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

		/// <summary>디스크 사용 정보 수정</summary>
		/// <param name="Request">디스크 사용 정보 수정 요청 객체</param>
		/// <returns>디스크 사용 정보 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateUsage(RequestDiskUsage Request)
		{
			var Result = new ResponseData();
			try
			{
				// 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 디스크 아이디가 유효하지 않을 경우
				if (Request.Id.IsEmpty() || !Guid.TryParse(Request.Id, out Guid DiskGuid))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REQUIRE_DISK_ID_OR_NO);

				// 서버 아이디가 유효하지 않은 경우
				if (!Guid.TryParse(Request.ServerId, out Guid ServerGuid))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.ServerId == ServerGuid && i.Id == DiskGuid);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_DISK_DOES_NOT_EXIST);

				// 임계값을 가져온다.
				var Threshold = await GetThreshold();

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
						Exist.ModId = LoginUserId != Guid.Empty ? LoginUserId : null;
						Exist.ModName = LoginUserName;
						Exist.ModDate = DateTime.Now;

						// 사용정보 추가
						m_dbContext.DiskUsages.Add(new DiskUsage()
						{
							Id = DiskGuid,
							RegDate = DateTime.Now,
							UsedInode = Request.UsedInode,
							UsedSize = Request.UsedSize,
							Read = Request.Read,
							Write = Request.Write,
						});
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						await Transaction.CommitAsync();
						Result.Result = EnumResponseResult.Success;

						// 디스크가 Good이고 TotalSize가 0이 아니며, 남은 용량이 ThresholdDiskWeak 사이즈 보다 작고 ReadWrite모드일 경우 디스크의 상태를 Weak로 변경
						if (Exist.State == EnumDbDiskState.Good && Exist.TotalSize > 0 && Exist.TotalSize - Exist.UsedSize < Threshold.Data.ThresholdDiskWeak && Exist.RwMode == EnumDbDiskRwMode.ReadWrite)
						{
							Exist.State = EnumDbDiskState.Weak;
							Exist.RwMode = EnumDbDiskRwMode.ReadOnly;
						}

						// 디스크가 Weak이고 TotalSize가 0이 아니며, 남은 용량이 ThresholdDiskGood 사이즈 보다 크고 ReadOnly모드일 경우 디스크의 상태를 Good로 변경
						if (Exist.State == EnumDbDiskState.Weak && Exist.TotalSize > 0 && Exist.TotalSize - Exist.UsedSize > Threshold.Data.ThresholdDiskGood && Exist.RwMode == EnumDbDiskRwMode.ReadOnly)
						{
							Exist.State = EnumDbDiskState.Good;
							Exist.RwMode = EnumDbDiskRwMode.ReadWrite;
						}

						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
							SendMq("*.servers.disks.state", new { Exist.Id, Exist.ServerId, Exist.DiskPoolId, Exist.Name, State = (EnumDiskState)Exist.State });
							SendMq("*.servers.disks.rwmode", new { Exist.Id, Exist.ServerId, Exist.DiskPoolId, Exist.Name, RwMode = (EnumDiskRwMode)Exist.RwMode });
						}
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
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <param name="DiskRwMode">디스크 읽기/쓰기 모드</param>
		/// <returns>디스크 읽기/쓰기 모드 수정 결과 객체</returns>
		public async Task<ResponseData> UpdateRwMode(string Id, EnumDiskRwMode DiskRwMode)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 해당 정보를 가져온다.
				Disk Exist = null;

				// 아이디로 가져올 경우
				if (Guid.TryParse(Id, out Guid DiskGuid))
					Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.Id == DiskGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Disks.FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_DISK_DOES_NOT_EXIST);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 수정한다.
						Exist.RwMode = (EnumDbDiskRwMode)DiskRwMode;
						Exist.ModId = LoginUserId != Guid.Empty ? LoginUserId : null;
						Exist.ModName = LoginUserName;
						Exist.ModDate = DateTime.Now;

						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 수정된 R/W 모드 정보 전송
						var Message = new { Exist.Id, Exist.ServerId, Exist.DiskPoolId, Exist.Name, RwMode = (EnumDiskRwMode)Exist.RwMode };
						SendMq("*.servers.disks.rwmode", Message);
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

		/// <summary>디스크 삭제</summary>
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <returns>디스크 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 해당 정보를 가져온다.
				Disk Exist = null;

				// 아이디로 가져올 경우
				if (Guid.TryParse(Id, out Guid DiskGuid))
					Exist = await m_dbContext.Disks.AsNoTracking().FirstOrDefaultAsync(i => i.Id == DiskGuid);
				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Disks.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_DISK_DOES_NOT_EXIST);

				// 연결된 디스크 풀이 존재하는 경우
				if (Exist.DiskPool != null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_REMOVE_AFTER_REMOVING_DISKPOOL);

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
						SendMq("*.servers.disks.removed", new { Exist.Id, Exist.ServerId, Exist.DiskPoolId, Exist.Name });
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
		/// <param name="OrderFields">정렬필드목록 (Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Path)</param>
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
				if (ServerId.IsEmpty() || !Guid.TryParse(ServerId, out Guid ServerGuid))
					return new ResponseList<ResponseDisk>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_SERVER_ID);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("State", "desc");
				AddDefaultOrders("Path", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를 초기화한다.
				InitSearchFields(ref SearchFields);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.Disks.AsNoTracking()
					.Where(i => i.ServerId == ServerGuid
						&& (
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("path") && i.Path.Contains(SearchKeyword))
						)
						&& (SearchStates == null || SearchStates.Count == 0 || SearchStates.Select(j => (int)j).Contains((int)i.State))
						&& (SearchRwModes == null || SearchRwModes.Count == 0 || SearchRwModes.Select(j => (int)j).Contains((int)i.RwMode))
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<Disk, ResponseDisk>(Skip, CountPerPage);

				//DiskPool 목록을 가져온다.
				var DiskPools = await m_dbContext.DiskPools.AsNoTracking().ToListAsync();

				// 서버 목록을 가져온다.
				var Servers = await m_dbContext.Servers.AsNoTracking().ToListAsync();

				//서버와 디스크풀 이름을 추가한다.
				foreach (var Disk in Result.Data.Items)
				{
					if (Disk.DiskPoolId != null && Guid.TryParse(Disk.DiskPoolId, out Guid DiskPoolGuid))
						Disk.DiskPoolName = DiskPools.First(i => i.Id == DiskPoolGuid).Name;
					Disk.ServerName = Servers.First(i => i.Id == ServerGuid).Name;
				}

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
		/// <param name="OrderFields">정렬필드목록 (Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode) (기본정렬 State desc, Path asc, HaAction desc)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>디스크 목록 객체</returns>
		public async Task<ResponseList<ResponseDisk>> GetList(
			List<EnumDiskState> SearchStates, List<EnumDiskRwMode> SearchRwModes,
			int Skip = 0, int CountPerPage = 100,
			List<string> OrderFields = null, List<string> OrderDirections = null,
			List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Result = new ResponseList<ResponseDisk>();

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
							|| (SearchFields.Contains("path") && i.Path.Contains(SearchKeyword))
						)
						&& (SearchStates == null || SearchStates.Count == 0 || SearchStates.Select(j => (int)j).Contains((int)i.State))
						&& (SearchRwModes == null || SearchRwModes.Count == 0 || SearchRwModes.Select(j => (int)j).Contains((int)i.RwMode))
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<Disk, ResponseDisk>(Skip, CountPerPage);

				//DiskPool 목록을 가져온다.
				var DiskPools = await m_dbContext.DiskPools.AsNoTracking().ToListAsync();

				// 서버 목록을 가져온다.
				var Servers = await m_dbContext.Servers.AsNoTracking().ToListAsync();

				//서버와 디스크풀 이름을 추가한다.
				foreach (var Disk in Result.Data.Items)
				{
					if (Disk.DiskPoolId != null && Guid.TryParse(Disk.DiskPoolId, out Guid DiskPoolGuid))
						Disk.DiskPoolName = DiskPools.First(i => i.Id == DiskPoolGuid).Name;
					if (Disk.ServerId != null && Guid.TryParse(Disk.ServerId, out Guid ServerGuid))
						Disk.ServerName = Servers.First(i => i.Id == ServerGuid).Name;
				}

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
		/// <param name="Id">디스크 아이디 / 이름</param>
		/// <returns>디스크 정보 객체</returns>
		public async Task<ResponseData<ResponseDiskWithServerAndNetwork>> Get(string Id)
		{
			var Result = new ResponseData<ResponseDiskWithServerAndNetwork>();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISKS_INVALID_DISK_ID);

				// 해당 정보를 가져온다.
				ResponseDiskWithServerAndNetwork Exist = null;

				// 아이디로 가져올 경우
				if (Guid.TryParse(Id, out Guid DiskGuid))
					Exist = await m_dbContext.Disks.AsNoTracking()
					.Where(i => i.Id == DiskGuid)
					.Include(i => i.DiskPool)
					.Include(i => i.Server)
					.ThenInclude(i => i.NetworkInterfaces)
					.FirstOrDefaultAsync<Disk, ResponseDiskWithServerAndNetwork>();

				// 이름으로 조회할 경우
				else
					Exist = await m_dbContext.Disks.AsNoTracking()
					.Where(i => i.Name == Id)
					.Include(i => i.DiskPool)
					.Include(i => i.Server)
					.ThenInclude(i => i.NetworkInterfaces)
					.FirstOrDefaultAsync<Disk, ResponseDiskWithServerAndNetwork>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseDiskWithServerAndNetwork>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_DISK_DOES_NOT_EXIST);

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

		/// <summary>해당 이름이 존재하는지 여부</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 디스크 아이디</param>
		/// <param name="Name">검색할 이름</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<bool> IsNameExist(string Name, Guid? ExceptId = null)
		{
			try
			{
				// 동일한 이름이 존재하는지 확인한다.
				return await m_dbContext.Disks.AsNoTracking().AnyAsync(i => (ExceptId == null || i.Id != ExceptId) && i.Name.Equals(Name));
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}

			return false;
		}

		public static readonly string THRESHOLD_DISK_GOOD = "THRESHOLD.DISK_GOOD";
		public static readonly string THRESHOLD_DISK_WEAK = "THRESHOLD.DISK_WEAK";

		public static readonly long DEFAULT_THRESHOLD_DISK_GOOD = 1000000000;
		public static readonly long DEFAULT_THRESHOLD_DISK_WEAK = 500000000;

		/// <summary>디스크 임계값을 가져온다.</summary>
		/// <returns>임계값 정보 객체</returns>
		public async Task<ResponseData<ResponseDiskThreshold>> GetThreshold()
		{
			var Result = new ResponseData<ResponseDiskThreshold>();

			try
			{
				// 해당 정보를 가져온다.
				var DiskGoodConfig = await m_dbContext.Configs.AsNoTracking().Where(i => i.Key == THRESHOLD_DISK_GOOD).FirstOrDefaultAsync();
				var DiskWeakConfig = await m_dbContext.Configs.AsNoTracking().Where(i => i.Key == THRESHOLD_DISK_WEAK).FirstOrDefaultAsync();

				Result.Data = new ResponseDiskThreshold();

				if (DiskGoodConfig != null && long.TryParse(DiskGoodConfig.Value, out long ThresholdDiskGood)) Result.Data.ThresholdDiskGood = ThresholdDiskGood;
				else Result.Data.ThresholdDiskGood = DEFAULT_THRESHOLD_DISK_GOOD;
				if (DiskWeakConfig != null && long.TryParse(DiskGoodConfig.Value, out long ThresholdDiskWeak)) Result.Data.ThresholdDiskWeak = ThresholdDiskWeak;
				else Result.Data.ThresholdDiskWeak = DEFAULT_THRESHOLD_DISK_WEAK;

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

		/// <summary>디스크 임계값을 설정한다.</summary>
		/// <param name="Request">임계값 정보 객체</param>
		/// <returns>처리 결과</returns>
		public async Task<ResponseData> SetThreshold(RequestDiskThreshold Request)
		{
			var Result = new ResponseData();

			try
			{
				// 요청이 유효하지 않은 경우
				if (Request.ThresholdDiskGood < 1 || Request.ThresholdDiskWeak < 1 || Request.ThresholdDiskGood < Request.ThresholdDiskWeak)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON_THRESHOLD_INVALID);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 해당 정보를 가져온다.
						var DiskGoodConfig = await m_dbContext.Configs.FirstOrDefaultAsync(i => i.Key == THRESHOLD_DISK_GOOD);
						var DiskWeakConfig = await m_dbContext.Configs.FirstOrDefaultAsync(i => i.Key == THRESHOLD_DISK_WEAK);

						// 해당 정보가 존재하지 않는 경우 추가한다.
						if (DiskGoodConfig == null)
							await m_dbContext.Configs.AddAsync(new Config() { Key = THRESHOLD_DISK_GOOD, Value = Request.ThresholdDiskGood.ToString() });
						// 존재할 경우 수정한다.
						else
							DiskGoodConfig.Value = Request.ThresholdDiskGood.ToString();

						// 해당 정보가 존재하지 않는 경우 추가한다.
						if (DiskWeakConfig == null)
							await m_dbContext.Configs.AddAsync(new Config() { Key = THRESHOLD_DISK_WEAK, Value = Request.ThresholdDiskWeak.ToString() });
						// 존재할 경우 수정한다.
						else
							DiskWeakConfig.Value = Request.ThresholdDiskWeak.ToString();

						// 데이터가 변경된 경우 저장
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