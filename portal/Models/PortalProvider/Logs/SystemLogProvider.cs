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
using PortalData.Responses.Logs;
using PortalModels;
using PortalProviderInterface;
using PortalResources;
using Microsoft.EntityFrameworkCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.EntityFramework;

namespace PortalProvider.Logs
{
	/// <summary>시스템 로그 프로바이더 클래스</summary>
	public class SystemLogProvider : ISystemLogProvider
	{
		/// <summary>디비 컨텍스트</summary>
		protected readonly PortalModel m_dbContext;

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		public SystemLogProvider(PortalModel dbContext)
		{
			m_dbContext = dbContext;
		}

		/// <summary>로그를 등록한다.</summary>
		/// <param name="level">로그레벨</param>
		/// <param name="messageFormat">로그 내용 형식</param>
		/// <param name="messageValues">로그 내용 값</param>
		/// <returns>등록결과</returns>
		public async Task<ResponseData> Add(EnumLogLevel level, string messageFormat, params object[] messageValues)
		{
			ResponseData result = new ResponseData();
			try
			{
				// 메세지가 유효한 경우
				if (!messageFormat.IsEmpty())
				{
					// 로그 저장
					await m_dbContext.SystemLogs.AddAsync(new SystemLog() { LogLevel = (EnumDbLogLevel)level, Message = string.Format(messageFormat, messageValues), RegDate = DateTime.Now });
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
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

		/// <summary>로그 목록을 반환한다.</summary>
		/// <param name="searchStartDate">검색 시작 일시</param>
		/// <param name="searchEndDate">검색 종료 일시</param>
		/// <param name="levels">로그 레벨 목록</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수</param>
		/// <param name="searchFields">검색필드목록 (Email, LoginId, Name, Code, Message)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>로그 목록</returns>
		public async Task<ResponseList<ResponseSystemLog>> GetLogs(
			DateTime searchStartDate, DateTime searchEndDate,
			List<EnumLogLevel> levels = null,
			int skip = 0, int countPerPage = 100,
			List<string> searchFields = null, string searchKeyword = ""
		)
		{
			ResponseList<ResponseSystemLog> result = new ResponseList<ResponseSystemLog>();
			try
			{
				if (searchStartDate > DateTime.MinValue && searchEndDate > DateTime.MinValue)
				{
					// 검색 필드 목록을 모두 소문자로 변환
					if (searchFields != null)
						searchFields = searchFields.ToLower();

					// 로그 목록을 가져온다.
					result.Data = await m_dbContext.SystemLogs.AsNoTracking()
						.Where(i => searchStartDate <= i.RegDate && i.RegDate <= searchEndDate
                             && (
	                             searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
                                 || (searchFields.Contains("message") && i.Message.Contains(searchKeyword))
                             )
                             && (levels == null || levels.Count == 0 || levels.Select(j => (int)j).Contains((int) i.LogLevel))
						)
						.OrderByDescending(i => i.RegDate)
						.CreateListAsync<SystemLog, ResponseSystemLog>(skip, countPerPage);
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
	}
}
