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
using PortalData.Requests.Disks;
using PortalData.Responses.Disks;
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
using PortalData.Responses.Servers;

namespace PortalProvider.Providers.Disks
{
	/// <summary>디스크 풀 데이터 프로바이더 클래스</summary>
	public class DiskPoolProvider : BaseProvider<PortalModel>, IDiskPoolProvider
	{
		/// <summary>디스크 데이터 프로바이더</summary>
		protected readonly IDiskProvider m_diskProvider;

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		/// <param name="configuration">역할 정보</param>
		/// <param name="userManager">사용자 관리자</param>
		/// <param name="systemLogProvider">시스템 로그 프로바이더</param>
		/// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
		/// <param name="serviceScopeFactory">서비스 팩토리</param>
		/// <param name="logger">로거</param>
		/// <param name="diskProvider">디스크 데이터 프로바이더</param>
		public DiskPoolProvider(
			PortalModel dbContext,
			IConfiguration configuration,
			UserManager<NNApplicationUser> userManager,
			ISystemLogProvider systemLogProvider,
			IUserActionLogProvider userActionLogProvider,
			IServiceScopeFactory serviceScopeFactory,
			ILogger<DiskPoolProvider> logger,
			IDiskProvider diskProvider
		)
			: base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
		{
			m_diskProvider = diskProvider;
		}

		/// <summary>디스크 풀 등록</summary>
		/// <param name="Request">디스크 풀 등록 요청 객체</param>
		/// <returns>디스크 풀 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseDiskPoolWithDisks>> Add(RequestDiskPool Request)
		{
			var Result = new ResponseData<ResponseDiskPoolWithDisks>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재할경우
				if (await this.IsNameExist(Request.Name))
					return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_DUPLICATED_NAME);

				// 디스크 GUID 목록
				var DiskIds = new List<Guid>();

				// 모든 디스크 아이디에 대해서 처리
				if (Request.DiskIds != null && Request.DiskIds.Count > 0)
				{
					// 모든 디스크 아이디에 대해서 처리
					foreach (string DiskId in Request.DiskIds)
					{
						if (!Guid.TryParse(DiskId, out Guid GuidDiskId))
							return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_INVALID_DISK_ID);
						DiskIds.Add(GuidDiskId);
					}

					// 주어진 디스크 아이디로 다른 그룹에 속하지 않는 디스크 아이디 수가 요청 개수와 다른 경우
					if (await this.m_dbContext.Disks.AsNoTracking()
						.Where(i => DiskIds.Contains(i.Id) && i.DiskPoolId == null)
						.CountAsync() != DiskIds.Count)
						return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_NOT_AVAILABLE_DISK_ID_USED);
				}

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						DiskPool newData = new DiskPool()
						{
							Id = Guid.NewGuid(),
							Name = Request.Name,
							Description = Request.Description,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now,
							DiskPoolType = (EnumDbDiskPoolType)Request.DiskPoolType,
							ReplicationType = (EnumDbDiskPoolReplicaType)Request.ReplicationType
						};
						await m_dbContext.DiskPools.AddAsync(newData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 모든 디스크 아이디에 대해서 처리
						foreach (Guid DiskId in DiskIds)
						{
							// 해당 디스크 정보를 가져온다.
							var Disk = await m_dbContext.Disks
								.Where(i => i.Id == DiskId)
								.FirstOrDefaultAsync();

							// 디스크 풀 아이디 변경
							Disk.DiskPoolId = newData.Id;
						}
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;
						Result.Data = (await this.Get(newData.Id.ToString())).Data;

						// 추가된 디스크 풀 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.added", Result.Data);
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

		/// <summary>디스크 풀 수정</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <param name="Request">디스크 풀 수정 요청 객체</param>
		/// <returns>디스크 풀 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string Id, RequestDiskPool Request)
		{
			var Result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 이름으로 조회할 경우
				if (!Guid.TryParse(Id, out Guid GuidId))
				{
					var DiskPool = await m_dbContext.DiskPools.AsNoTracking().FirstOrDefaultAsync(i => i.Name == Id);

					//서비스가 존재하지 않는 경우
					if (DiskPool == null)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

					GuidId = DiskPool.Id;
				}

				// 요청이 유효하지 않은 경우
				if (!Request.IsValid())
					return new ResponseData(EnumResponseResult.Error, Request.GetErrorCode(), Request.GetErrorMessage());

				// 동일한 이름이 존재할경우
				if (await this.IsNameExist(Request.Name, GuidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_DUPLICATED_NAME);

				// 디스크 GUID 목록
				var DiskIds = new List<Guid>();

				// 모든 디스크 아이디에 대해서 처리
				if (Request.DiskIds != null && Request.DiskIds.Count > 0)
				{
					// 모든 디스크 아이디에 대해서 처리
					foreach (var DiskId in Request.DiskIds)
					{
						if (!Guid.TryParse(DiskId, out Guid GuidDiskId))
							return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_INVALID_DISK_ID);
						DiskIds.Add(GuidDiskId);
					}

					// 주어진 디스크 아이디로 다른 그룹에 속하지 않는 디스크 아이디 수가 요청 개수와 다른 경우
					if (await this.m_dbContext.Disks.AsNoTracking()
						.Where(i => DiskIds.Contains(i.Id) && (i.DiskPoolId == null || i.DiskPoolId == GuidId))
						.CountAsync() != DiskIds.Count)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_NOT_AVAILABLE_DISK_ID_USED);
				}

				// 해당 정보를 가져온다.
				var Exist = await m_dbContext.DiskPools
					.FirstOrDefaultAsync(i => i.Id == GuidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 이 디스크 풀 아이디를 사용하는 이전 디스크의 디스크 풀 아이디 초기화
						var OldDisks = await m_dbContext.Disks
							.Where(i => i.DiskPoolId == GuidId)
							.ToListAsync();
						if (OldDisks.Count > 0)
						{
							// 모든 디스크 아이디에 대해서 처리
							foreach (Disk oldDisk in OldDisks)
								// 그룹 아이디 변경
								oldDisk.DiskPoolId = null;
							// 데이터가 변경된 경우 저장
							if (m_dbContext.HasChanges())
								await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}

						// 모든 디스크 아이디에 대해서 처리
						foreach (Guid guidDiskId in DiskIds)
						{
							// 해당 디스크 정보를 가져온다.
							var Disk = await m_dbContext.Disks
								.Where(i => i.Id == guidDiskId)
								.FirstOrDefaultAsync();

							// 디스크 풀 아이디 변경
							Disk.DiskPoolId = GuidId;
						}
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 정보를 수정한다.
						Exist.Name = Request.Name;
						Exist.Description = Request.Description;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							Exist.ModId = LoginUserId;
							Exist.ModName = LoginUserName;
							Exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}
						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 상세 정보를 가져온다.
						var Response = (await this.Get(Id)).Data;

						// 수정된 디스크 풀 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", Response);
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

		/// <summary>디스크 풀 삭제</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <returns>디스크 풀 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string Id)
		{
			var Result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				DiskPool Exist = null;

				// 해당 정보를 가져온다.
				// 이름으로 조회할 경우
				if (!Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.DiskPools.FirstOrDefaultAsync(i => i.Name == Id);
				// Id로 조회할경우
				else
					Exist = await m_dbContext.DiskPools.FirstOrDefaultAsync(i => i.Id == GuidId);

				// 해당 정보가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData(Result.Result = EnumResponseResult.Success);

				using (var Transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 해당 디스크 풀에 속한 디스크 목록을 가져온다.
						var Disks = await this.m_dbContext.Disks
							.Where(i => i.DiskPoolId == Exist.Id)
							.ToListAsync();

						// 모든 디스크에 대해서 처리
						foreach (var Disk in Disks)
							Disk.DiskPoolId = null;
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 해당 데이터 삭제
						m_dbContext.DiskPools.Remove(Exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await Transaction.CommitAsync();

						Result.Result = EnumResponseResult.Success;

						// 삭제된 디스크 풀 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.removed", new ResponseDiskPool().CopyValueFrom(Exist));
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

		/// <summary>디스크 풀 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Name, Description, Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>디스크 풀 목록 객체</returns>
		public async Task<ResponseList<ResponseDiskPool>> GetList(
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = ""
		)
		{
			var Result = new ResponseList<ResponseDiskPool>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.DiskPools.AsNoTracking()
					.Where(i =>
						(
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("name") && i.Name.Contains(SearchKeyword))
							|| (SearchFields.Contains("description") && i.Description.Contains(SearchKeyword))
							|| (SearchFields.Contains("path") && i.Disks.Where(j => j.Path.Contains(SearchKeyword)).Any())
						)
					)
					.OrderByWithDirection(OrderFields, OrderDirections)
					.CreateListAsync<DiskPool, ResponseDiskPool>(Skip, CountPerPage);

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

		/// <summary>디스크 풀 목록을 가져온다.</summary>
		/// <returns>디스크 풀 목록 객체</returns>
		public async Task<ResponseList<ResponseDiskPoolDetails>> GetListDetails()
		{
			var Result = new ResponseList<ResponseDiskPoolDetails>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 목록을 가져온다.
				Result.Data = await m_dbContext.DiskPools.AsNoTracking().CreateListAsync<DiskPool, ResponseDiskPoolDetails>();

				for (int i = 0; i < Result.Data.Items.Count; i++)
				{
					var DiskPool = Result.Data.Items[i];
					if (!Guid.TryParse(DiskPool.Id, out Guid DiskPoolId)) continue;
					var Data = await m_dbContext.Servers.AsNoTracking()
						.Include(i => i.Disks.Where(j => j.DiskPoolId == DiskPoolId))
						.Include(i => i.NetworkInterfaces)
						.CreateListAsync<Server, ResponseServerDetail>();

					if (Data.Items.Count != 0)
					{
						var DiskPools = new List<ResponseServerDetail>();
						for (int n = Data.Items.Count - 1; n >= 0; n--)
						{
							if (Data.Items[n].Disks == null) Data.Items.RemoveAt(n);
							else if (Data.Items[n].Disks.Count == 0) Data.Items.RemoveAt(n);
						}
						DiskPool.Servers.AddRange(Data.Items);
					}
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

		/// <summary>특정 디스크 풀 정보를 가져온다.</summary>
		/// <param name="Id">디스크 풀 아이디 / 이름</param>
		/// <returns>디스크 풀 정보 객체</returns>
		public async Task<ResponseData<ResponseDiskPoolWithDisks>> Get(string Id)
		{
			var Result = new ResponseData<ResponseDiskPoolWithDisks>();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty())
					return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				ResponseDiskPoolWithDisks Exist = null;

				// 해당 정보를 가져온다.
				// 이름으로 조회할 경우
				if (!Guid.TryParse(Id, out Guid GuidId))
					Exist = await m_dbContext.DiskPools.AsNoTracking().Where(i => i.Name == Id).Include(i => i.Disks).FirstOrDefaultAsync<DiskPool, ResponseDiskPoolWithDisks>();
				// Id로 조회할경우
				else
					Exist = await m_dbContext.DiskPools.AsNoTracking().Where(i => i.Id == GuidId).Include(i => i.Disks).FirstOrDefaultAsync<DiskPool, ResponseDiskPoolWithDisks>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

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

		/// <summary>특정 이름의 디스크 풀이 존재하는지 확인한다.</summary>
		/// <param name="ExceptId">이름 검색 시 제외할 디스크 풀 아이디</param>
		/// <param name="Name">검색할 이름</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsNameExist(string ExceptId, string Name)
		{
			var Result = new ResponseData<bool>();
			var GuidId = Guid.Empty;

			try
			{
				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				if (!ExceptId.IsEmpty() && !Guid.TryParse(ExceptId, out GuidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EN_DISK_POOLS_INVALID_ID);

				// 요청 객체가 유효하지 않은 경우
				if (Name.IsEmpty())
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_DISK_POOLS_REQUIRE_NAME);

				Result.Data = await IsNameExist(Name, GuidId != Guid.Empty ? GuidId : null);
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

		/// <summary>특정 이름의 디스크 풀이 존재하는지 확인한다.</summary>
		/// <param name="Name">검색할 이름</param>
		/// <param name="ExceptId">이름 검색 시 제외할 디스크 풀 아이디</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<bool> IsNameExist(string Name, Guid? ExceptId = null)
		{
			try
			{
				// 동일한 이름이 존재하는 경우
				return await m_dbContext.DiskPools.AsNoTracking().AnyAsync(i => (ExceptId == null || i.Id != ExceptId) && i.Name == Name);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}

			return false;
		}

		/// <summary>해당 디스크 타입으로 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>참여가 가능한 디스크 목록 객체</returns>
		public async Task<ResponseList<ResponseDisk>> GetAvailableDisks(
			int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "")
		{
			var Result = new ResponseList<ResponseDisk>();

			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Path", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.Disks.AsNoTracking()
					.Where(i =>
						i.DiskPoolId == null
						&& (
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("path") && i.Path.Contains(SearchKeyword))
						)
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

		/// <summary>주어진 디스크 풀 아이디에 해당하는 디스크 풀에 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="Id">디스크 풀 아이디 (null인 경우, 어느 풀에도 속하지 않은 디스크만 검색한다.)</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="OrderFields">정렬필드목록 (Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="OrderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="SearchFields">검색필드 목록 (Path)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>참여가 가능한 디스크 목록 객체</returns>
		public async Task<ResponseList<ResponseDisk>> GetAvailableDisks(
			string Id
			, int Skip = 0, int CountPerPage = 100
			, List<string> OrderFields = null, List<string> OrderDirections = null
			, List<string> SearchFields = null, string SearchKeyword = "")
		{
			var Result = new ResponseList<ResponseDisk>();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (Id.IsEmpty() || !Guid.TryParse(Id, out Guid guidId))
					return new ResponseList<ResponseDisk>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EN_DISK_POOLS_INVALID_ID);

				// 정보를 가져온다.
				var Exist = await m_dbContext.DiskPools.AsNoTracking()
					.Where(i => i.Id == guidId)
					.FirstOrDefaultAsync<DiskPool, ResponseDiskPool>();

				// 해당 데이터가 존재하지 않는 경우
				if (Exist == null)
					return new ResponseList<ResponseDisk>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Path", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref OrderFields, ref OrderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref SearchFields);

				// 목록을 가져온다.
				Result.Data = await m_dbContext.Disks.AsNoTracking()
					.Where(i =>
						(i.DiskPoolId == null || (!Id.IsEmpty() && i.DiskPoolId == guidId))
						&& (
							SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
							|| (SearchFields.Contains("path") && i.Path.Contains(SearchKeyword))
						)
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
	}
}