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
using Microsoft.EntityFrameworkCore.Storage;
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
		/// <param name="request">디스크 풀 등록 요청 객체</param>
		/// <returns>디스크 풀 등록 결과 객체</returns>
		public async Task<ResponseData<ResponseDiskPoolWithDisks>> Add(RequestDiskPool request)
		{
			ResponseData<ResponseDiskPoolWithDisks> result = new ResponseData<ResponseDiskPoolWithDisks>();
			try
			{
				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 동일한 이름이 존재하는지 확인한다.
				ResponseData<bool> responseExist = await this.IsNameExist(null, new RequestIsDiskPoolNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData<ResponseDiskPoolWithDisks>(responseExist.Result, responseExist.Code, responseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_DUPLICATED_NAME);

				// 디스크 GUID 목록
				List<Guid> diskIds = new List<Guid>();

				// 모든 디스크 아이디에 대해서 처리
				if (request.DiskIds != null && request.DiskIds.Count > 0)
				{
					// 모든 디스크 아이디에 대해서 처리
					foreach (string diskId in request.DiskIds)
					{
						if (!Guid.TryParse(diskId, out Guid guidDiskId))
							return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_INVALID_DISK_ID);
						diskIds.Add(guidDiskId);
					}

					// 주어진 디스크 아이디로 다른 그룹에 속하지 않는 디스크 아이디 수가 요청 개수와 다른 경우
					if (await this.m_dbContext.Disks.AsNoTracking()
						.Where(i => diskIds.Contains(i.Id) && i.DiskPoolId == null)
						.CountAsync() != diskIds.Count)
						return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_NOT_AVAILABLE_DISK_ID_USED);
				}

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 정보를 생성한다.
						DiskPool newData = new DiskPool()
						{
							Id = Guid.NewGuid(),
							Name = request.Name,
							Description = request.Description,
							RegId = LoginUserId,
							RegName = LoginUserName,
							RegDate = DateTime.Now,
							ModId = LoginUserId,
							ModName = LoginUserName,
							ModDate = DateTime.Now
						};
						await m_dbContext.DiskPools.AddAsync(newData);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 모든 디스크 아이디에 대해서 처리
						foreach (Guid guidDiskId in diskIds)
						{
							// 해당 디스크 정보를 가져온다.
							Disk disk = await m_dbContext.Disks
								.Where(i => i.Id == guidDiskId)
								.FirstOrDefaultAsync();

							// 디스크 풀 아이디 변경
							disk.DiskPoolId = newData.Id;
						}
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;
						result.Data = (await this.Get(newData.Id.ToString())).Data;

						// 추가된 디스크 풀 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.added", result.Data);
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

		/// <summary>디스크 풀 수정</summary>
		/// <param name="id">디스크 풀 아이디</param>
		/// <param name="request">디스크 풀 수정 요청 객체</param>
		/// <returns>디스크 풀 수정 결과 객체</returns>
		public async Task<ResponseData> Update(string id, RequestDiskPool request)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청이 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 동일한 이름이 존재하는지 확인한다.
				ResponseData<bool> responseExist = await this.IsNameExist(id, new RequestIsDiskPoolNameExist(request.Name));
				// 동일한 이름이 존재하는지 확인하는데 실패한 경우
				if (responseExist.Result != EnumResponseResult.Success)
					return new ResponseData(responseExist.Result, responseExist.Code, responseExist.Message);
				// 동일한 이름이 존재하는 경우
				if (responseExist.Data)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_DUPLICATED_NAME);

				// 디스크 GUID 목록
				List<Guid> diskIds = new List<Guid>();

				// 모든 디스크 아이디에 대해서 처리
				if (request.DiskIds != null && request.DiskIds.Count > 0)
				{
					// 모든 디스크 아이디에 대해서 처리
					foreach (string diskId in request.DiskIds)
					{
						if (!Guid.TryParse(diskId, out Guid guidDiskId))
							return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_INVALID_DISK_ID);
						diskIds.Add(guidDiskId);
					}

					// 주어진 디스크 아이디로 다른 그룹에 속하지 않는 디스크 아이디 수가 요청 개수와 다른 경우
					if (await this.m_dbContext.Disks.AsNoTracking()
						.Where(i => diskIds.Contains(i.Id) && (i.DiskPoolId == null || i.DiskPoolId == guidId))
						.CountAsync() != diskIds.Count)
						return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__DUPLICATED_DATA, Resource.EM_DISK_POOLS_NOT_AVAILABLE_DISK_ID_USED);
				}

				// 해당 정보를 가져온다.
				DiskPool exist = await m_dbContext.DiskPools
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 이 디스크 풀 아이디를 사용하는 이전 디스크의 디스크 풀 아이디 초기화
						List<Disk> oldDisks = await m_dbContext.Disks
							.Where(i => i.DiskPoolId == guidId)
							.ToListAsync();
						if (oldDisks.Count > 0)
						{
							// 모든 디스크 아이디에 대해서 처리
							foreach (Disk oldDisk in oldDisks)
								// 그룹 아이디 변경
								oldDisk.DiskPoolId = null;
							// 데이터가 변경된 경우 저장
							if (m_dbContext.HasChanges())
								await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}

						// 모든 디스크 아이디에 대해서 처리
						foreach (Guid guidDiskId in diskIds)
						{
							// 해당 디스크 정보를 가져온다.
							Disk disk = await m_dbContext.Disks
								.Where(i => i.Id == guidDiskId)
								.FirstOrDefaultAsync();

							// 디스크 풀 아이디 변경
							disk.DiskPoolId = guidId;
						}
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 정보를 수정한다.
						exist.Name = request.Name;
						exist.Description = request.Description;
						// 데이터가 변경된 경우 저장
						if (m_dbContext.HasChanges())
						{
							exist.ModId = LoginUserId;
							exist.ModName = LoginUserName;
							exist.ModDate = DateTime.Now;
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
						}
						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;

						// 상세 정보를 가져온다.
						ResponseDiskPoolWithDisks response = (await this.Get(id)).Data;

						// 수정된 디스크 풀 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.updated", response);
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

		/// <summary>디스크 풀 삭제</summary>
		/// <param name="id">디스크 풀 아이디</param>
		/// <returns>디스크 풀 삭제 결과 객체</returns>
		public async Task<ResponseData> Remove(string id)
		{
			ResponseData result = new ResponseData();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 해당 정보를 가져온다.
				DiskPool exist = await m_dbContext.DiskPools
					.FirstOrDefaultAsync(i => i.Id == guidId);

				// 해당 정보가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData(result.Result = EnumResponseResult.Success);

				using (IDbContextTransaction transaction = await m_dbContext.Database.BeginTransactionAsync())
				{
					try
					{
						// 해당 디스크 풀에 속한 디스크 목록을 가져온다.
						List<Disk> disks = await this.m_dbContext.Disks
							.Where(i => i.DiskPoolId == guidId)
							.ToListAsync();

						// 모든 디스크에 대해서 처리
						foreach (Disk disk in disks)
							disk.DiskPoolId = null;
						if (m_dbContext.HasChanges())
							await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						// 해당 데이터 삭제
						m_dbContext.DiskPools.Remove(exist);
						await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

						await transaction.CommitAsync();

						result.Result = EnumResponseResult.Success;

						// 삭제된 디스크 풀 정보 전송
						SendMq(RabbitMqConfiguration.ExchangeName, "*.servers.diskpools.removed", new ResponseDiskPool().CopyValueFrom(exist));
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

		/// <summary>디스크 풀 목록을 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Name, Description)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (Name, Description, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>디스크 풀 목록 객체</returns>
		public async Task<ResponseList<ResponseDiskPool>> GetList(
			int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseDiskPool> result = new ResponseList<ResponseDiskPool>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				// 목록을 가져온다.
				result.Data = await m_dbContext.DiskPools.AsNoTracking()
					.Where(i =>
						(
							searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
							|| (searchFields.Contains("name") && i.Name.Contains(searchKeyword))
							|| (searchFields.Contains("description") && i.Description.Contains(searchKeyword))
							|| (searchFields.Contains("path") && i.Disks.Where(j => j.Path.Contains(searchKeyword)).Any())
						)
					)
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<DiskPool, ResponseDiskPool>(skip, countPerPage);

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

		/// <summary>디스크 풀 목록을 가져온다.</summary>
		/// <returns>디스크 풀 목록 객체</returns>
		public async Task<ResponseList<ResponseDiskPoolDetails>> GetListDetails()
		{
			ResponseList<ResponseDiskPoolDetails> result = new ResponseList<ResponseDiskPoolDetails>();
			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 목록을 가져온다.
				result.Data = await m_dbContext.DiskPools.AsNoTracking().CreateListAsync<DiskPool, ResponseDiskPoolDetails>();

				for (int i = 0; i < result.Data.Items.Count; i++)
				{
					var DiskPool = result.Data.Items[i];
					if (!Guid.TryParse(DiskPool.Id, out Guid DiskPoolId)) continue;
					var Data = await m_dbContext.Servers.AsNoTracking()
						.Include(i => i.Disks.Where(j => j.DiskPoolId == DiskPoolId))
						.Include(i => i.NetworkInterfaces)
						.ThenInclude(i => i.NetworkInterfaceVlans)
						.CreateListAsync<Server, ResponseServerDetail>();

					if (Data.Items.Count != 0)
					{
						for (int n = Data.Items.Count; n <= 0; n--)
						{
							if (Data.Items[n].Disks.Count == 0)
								Data.Items.RemoveAt(n);
						}
						DiskPool.Servers.AddRange(Data.Items);
					}
				}

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

		/// <summary>특정 디스크 풀 정보를 가져온다.</summary>
		/// <param name="id">디스크 풀 아이디</param>
		/// <returns>디스크 풀 정보 객체</returns>
		public async Task<ResponseData<ResponseDiskPoolWithDisks>> Get(string id)
		{
			ResponseData<ResponseDiskPoolWithDisks> result = new ResponseData<ResponseDiskPoolWithDisks>();
			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				ResponseDiskPoolWithDisks exist = await m_dbContext.DiskPools.AsNoTracking()
					.Where(i => i.Id == guidId)
					.Include(i => i.Disks)
					.FirstOrDefaultAsync<DiskPool, ResponseDiskPoolWithDisks>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseData<ResponseDiskPoolWithDisks>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

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

		/// <summary>특정 이름의 디스크 풀가 존재하는지 확인한다.</summary>
		/// <param name="exceptId">이름 검색 시 제외할 디스크 풀 아이디</param>
		/// <param name="request">특정 이름의 디스크 풀 존재여부 확인 요청 객체</param>
		/// <returns>해당 이름이 존재하는지 여부</returns>
		public async Task<ResponseData<bool>> IsNameExist(string exceptId, RequestIsDiskPoolNameExist request)
		{
			ResponseData<bool> result = new ResponseData<bool>();
			Guid guidId = Guid.Empty;

			try
			{
				// 아이디가 존재하고, 아이디가 유효하지 않은 경우
				if (!exceptId.IsEmpty() && !Guid.TryParse(exceptId, out guidId))
					return new ResponseData<bool>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 요청 객체가 유효하지 않은 경우
				if (!request.IsValid())
					return new ResponseData<bool>(EnumResponseResult.Error, request.GetErrorCode(), request.GetErrorMessage());

				// 동일한 이름이 존재하는 경우
				if (await m_dbContext.DiskPools.AsNoTracking().AnyAsync(i => (exceptId.IsEmpty() || i.Id != guidId) && i.Name == request.Name))
					result.Data = true;
				// 동일한 이름이 존재하지 않는 경우
				else
					result.Data = false;
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

		/// <summary>해당 디스크 타입으로 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>참여가 가능한 디스크 목록 객체</returns>
		public async Task<ResponseList<ResponseDisk>> GetAvailableDisks(
			int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "")
		{
			ResponseList<ResponseDisk> result = new ResponseList<ResponseDisk>();

			try
			{
				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Path", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				// 목록을 가져온다.
				result.Data = await m_dbContext.Disks.AsNoTracking()
					.Where(i =>
						i.DiskPoolId == null
						&& (
							searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
							|| (searchFields.Contains("diskno") && i.DiskNo.Contains(searchKeyword))
							|| (searchFields.Contains("path") && i.Path.Contains(searchKeyword))
						)
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

		/// <summary>주어진 디스크 풀 아이디에 해당하는 디스크 풀에 참여가 가능한 디스크 목록을 가져온다.</summary>
		/// <param name="id">디스크 풀 아이디 (null인 경우, 어느 풀에도 속하지 않은 디스크만 검색한다.)</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (DiskNo, Path, HaAction, State, TotalSize, ReservedSize, UsedSize, RwMode)</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드 목록 (DiskNo, Path)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>참여가 가능한 디스크 목록 객체</returns>
		public async Task<ResponseList<ResponseDisk>> GetAvailableDisks(
			string id
			, int skip = 0, int countPerPage = 100
			, List<string> orderFields = null, List<string> orderDirections = null
			, List<string> searchFields = null, string searchKeyword = "")
		{
			ResponseList<ResponseDisk> result = new ResponseList<ResponseDisk>();

			try
			{
				// 아이디가 유효하지 않은 경우
				if (id.IsEmpty() || !Guid.TryParse(id, out Guid guidId))
					return new ResponseList<ResponseDisk>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_REQUEST, Resource.EM_COMMON__INVALID_REQUEST);

				// 정보를 가져온다.
				ResponseDiskPool exist = await m_dbContext.DiskPools.AsNoTracking()
					.Where(i => i.Id == guidId)
					.FirstOrDefaultAsync<DiskPool, ResponseDiskPool>();

				// 해당 데이터가 존재하지 않는 경우
				if (exist == null)
					return new ResponseList<ResponseDisk>(EnumResponseResult.Error, Resource.EC_COMMON__NOT_FOUND, Resource.EM_COMMON__NOT_FOUND);

				// 기본 정렬 정보 추가
				ClearDefaultOrders();
				AddDefaultOrders("Path", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				// 목록을 가져온다.
				result.Data = await m_dbContext.Disks.AsNoTracking()
					.Where(i =>
						(i.DiskPoolId == null || (!id.IsEmpty() && i.DiskPoolId == guidId))
						&& (
							searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
							|| (searchFields.Contains("diskno") && i.DiskNo.Contains(searchKeyword))
							|| (searchFields.Contains("path") && i.Path.Contains(searchKeyword))
						)
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
	}
}