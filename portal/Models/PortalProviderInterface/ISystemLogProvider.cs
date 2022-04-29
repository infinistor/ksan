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
using System.Threading.Tasks;
using PortalData;
using PortalData.Enums;
using PortalData.Responses.Logs;

namespace PortalProviderInterface
{
	/// <summary>시스템 로그 프로바이더 인터페이스</summary>
	public interface ISystemLogProvider
	{
		/// <summary>로그를 등록한다.</summary>
		/// <param name="level">로그레벨</param>
		/// <param name="messageFormat">로그 내용 형식</param>
		/// <param name="messageValues">로그 내용 값</param>
		/// <returns>등록결과</returns>
		Task<ResponseData> Add(EnumLogLevel level, string messageFormat, params object[] messageValues);

		/// <summary>로그 목록을 반환한다.</summary>
		/// <param name="searchStartDate">검색 시작 일시</param>
		/// <param name="searchEndDate">검색 종료 일시</param>
		/// <param name="levels">로그 레벨 목록</param>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수</param>
		/// <param name="searchFields">검색필드목록 (Email, LoginId, Name, Code, Message)</param>
		/// <param name="searchKeyword">검색어</param>
		/// <returns>로그 목록</returns>
		Task<ResponseList<ResponseSystemLog>> GetLogs(
			DateTime searchStartDate, DateTime searchEndDate,
			List<EnumLogLevel> levels = null,
			int skip = 0, int countPerPage = 100,
			List<string> searchFields = null, string searchKeyword = ""
		);
	}
}
