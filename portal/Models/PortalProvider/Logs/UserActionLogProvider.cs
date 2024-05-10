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
using System.Security.Claims;
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Responses.Logs;
using PortalModels;
using PortalProviderInterface;
using PortalResources;
using Microsoft.EntityFrameworkCore;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.EntityFramework;

namespace PortalProvider.Logs
{
	/// <summary>사용자 동작 로그 프로바이더</summary>
	public class UserActionLogProvider : IUserActionLogProvider
	{
		/// <summary>디비 컨텍스트</summary>
		protected readonly PortalModel m_dbContext;

		/// <summary>로그인 사용자 정보</summary>
		public ClaimsPrincipal UserClaimsPrincipal { get; set; } = null;

		/// <summary>로그인 사용자 정보</summary>
		public NNApplicationUser LoginUser
		{
			get
			{
				return null;
			}
		}

		/// <summary>로그인 사용자 아이디</summary>
		public Guid? LoginUserId
		{
			get
			{
				return Guid.Empty;
			}
		}

		/// <summary>로그인 사용자명</summary>
		public string LoginUserName
		{
			get
			{
				return null;
			}
		}

		/// <summary>접속한 아이피 주소</summary>
		public string UserIpAddress { get; set; } = "";

		/// <summary>생성자</summary>
		/// <param name="dbContext">DB 컨텍스트</param>
		public UserActionLogProvider(
			PortalModel dbContext)
		{
			m_dbContext = dbContext;
		}

		/// <summary>로그를 등록한다.</summary>
		/// <param name="Level">로그레벨</param>
		/// <param name="User">사용자 정보</param>
		/// <param name="IpAddress">아이피 주소</param>
		/// <param name="MessageFormat">로그 내용 형식</param>
		/// <param name="MessageValues">로그 내용 값</param>
		/// <returns>등록결과</returns>
		public async Task<ResponseData> Add(EnumLogLevel Level, NNApplicationUser User, string IpAddress, string MessageFormat, params object[] MessageValues)
		{
			var Result = new ResponseData();
			try
			{
				// 로그인 사용자 정보 및 메세지가 유효한 경우
				if (User != null && !MessageFormat.IsEmpty())
				{
					// 로그 저장
					await m_dbContext.UserActionLogs.AddAsync(new UserActionLog() { LogLevel = (EnumDbLogLevel)Level, UserId = User.Id, UserName = User.GetDisplayName(), IpAddress = IpAddress, Message = string.Format(MessageFormat, MessageValues), RegDate = DateTime.Now });
					await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();
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

		/// <summary>로그 목록을 반환한다.</summary>
		/// <param name="SearchStartDate">검색 시작 일시</param>
		/// <param name="SearchEndDate">검색 종료 일시</param>
		/// <param name="Levels">로그 레벨 목록</param>
		/// <param name="Skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="CountPerPage">페이지 당 레코드 수</param>
		/// <param name="SearchFields">검색필드목록 (Email, LoginId, Name, Code, Message)</param>
		/// <param name="SearchKeyword">검색어</param>
		/// <returns>로그 목록</returns>
		public async Task<ResponseList<ResponseUserActionLog>> GetLogs(
			DateTime SearchStartDate, DateTime SearchEndDate,
			List<EnumLogLevel> Levels,
			int Skip = 0, int CountPerPage = 100,
			List<string> SearchFields = null, string SearchKeyword = null
		)
		{
			var Result = new ResponseList<ResponseUserActionLog>();
			try
			{
				if (SearchStartDate > DateTime.MinValue && SearchEndDate > DateTime.MinValue)
				{
					// 검색 필드 목록을 모두 소문자로 변환
					if (SearchFields != null)
						SearchFields = SearchFields.ToLower();

					// 로그 목록을 가져온다.
					Result.Data = await m_dbContext.UserActionLogs.AsNoTracking()
						.Where(i =>
							(
								SearchFields == null || SearchFields.Count == 0 || SearchKeyword.IsEmpty()
								 || (SearchFields.Contains("Email") && i.User.Email.Contains(SearchKeyword))
								 || (SearchFields.Contains("LoginId") && i.User.LoginId.Contains(SearchKeyword))
								 || (SearchFields.Contains("Name") && i.User.Name.Contains(SearchKeyword))
								 || (SearchFields.Contains("Code") && i.User.Code.Contains(SearchKeyword))
								 || (SearchFields.Contains("Message") && i.Message.Contains(SearchKeyword))
								 || (SearchFields.Contains("ip") && i.IpAddress.Contains(SearchKeyword))
							)
							&& (Levels == null || Levels.Count == 0 || Levels.Select(j => (int)j).Contains((int)i.LogLevel))
						)
						.Include(i => i.User)
						.Between(i => i.RegDate, SearchStartDate, SearchEndDate)
						.OrderByWithDirection(i => i.RegDate, "desc")
						.CreateListAsync<UserActionLog, ResponseUserActionLog>(Skip, CountPerPage);
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
	}
}
