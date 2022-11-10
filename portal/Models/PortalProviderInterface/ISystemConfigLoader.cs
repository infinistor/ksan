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
using System.Threading.Tasks;
using PortalData;
using PortalData.Responses.Common;
using Microsoft.EntityFrameworkCore;

namespace PortalProviderInterface
{
	/// <summary>시스템 환경 설정 로더 인터페이스</summary>
	public interface ISystemConfigLoader
	{
		/// <summary>DB에서 설정을 로드한다.</summary>
		/// <param name="context">DB 컨텍스트</param>
		/// <returns>로드 결과</returns>
		bool Load(DbContext context);

		/// <summary>특정 설정 값에 대한 문자열을 가져온다.</summary>
		/// <param name="key">설정 키</param>
		/// <returns>설정 값</returns>
		string GetValue(string key);

		/// <summary>특정 설정 값을 T 타입으로 변환하여 반환한다.</summary>
		/// <typeparam name="T">변환할 타입</typeparam>
		/// <param name="key">설정 키</param>
		/// <returns>설정 값</returns>
		T GetValue<T>(string key);

		/// <summary>특정 설정을 가져온다.</summary>
		/// <param name="key">설정 키</param>
		/// <returns>해당 설정 데이터</returns>
		ResponseData<ResponseConfig> Get(string key);

		/// <summary>전체 설정을 가져온다.</summary>
		/// <returns>설정 목록</returns>
		ResponseList<ResponseConfig> Get();

		/// <summary>주어진 문자열로 시작하는 키를 가지는 설정 목록을 가져온다.</summary>
		/// <param name="keyStartWith">키 시작 문자열</param>
		/// <returns>설정 목록</returns>
		ResponseList<ResponseConfig> GetStartsWith(string keyStartWith);
	}
}
