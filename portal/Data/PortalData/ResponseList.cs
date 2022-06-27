﻿/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
using MTLib.CommonData;

namespace PortalData
{
	/// <summary>응답 목록 클래스</summary>
	/// <typeparam name="T">목록 내 타입</typeparam>
	public class ResponseList<T> : CommonResponseListWithAuth<T>
	{
		/// <summary>생성자</summary>
		public ResponseList()
		{

		}

		/// <summary>생성자</summary>
		/// <param name="Result">응답 결과</param>
		/// <param name="Code">응답 코드</param>
		/// <param name="Message">응답 메세지</param>
		/// <param name="IsNeedLogin">로그인 필요 여부</param>
		/// <param name="AccessDenied">권한 없음 여부</param>
		public ResponseList(EnumResponseResult Result, string Code, string Message, bool IsNeedLogin, bool AccessDenied)
			: base(Result, Code, Message, IsNeedLogin, AccessDenied)
		{
		}

		/// <summary>생성자</summary>
		/// <param name="Result">응답 결과</param>
		/// <param name="Code">응답 코드</param>
		/// <param name="Message">응답 메세지</param>
		/// <param name="Data">데이터</param>
		/// <param name="IsNeedLogin">로그인 필요 여부</param>
		/// <param name="AccessDenied">권한 없음 여부</param>
		public ResponseList(EnumResponseResult Result, string Code, string Message, QueryResults<T> Data, bool IsNeedLogin, bool AccessDenied)
			: base(Result, Code, Message, Data, IsNeedLogin, AccessDenied)
		{
		}

		/// <summary>생성자</summary>
		/// <param name="Data">데이터</param>
		public ResponseList(QueryResults<T> Data)
			: this(EnumResponseResult.Success, "", "", Data, false, false)
		{
		}

		/// <summary>생성자</summary>
		/// <param name="Result">응답 결과</param>
		/// <param name="Data">데이터</param>
		public ResponseList(EnumResponseResult Result, QueryResults<T> Data)
			: this(Result, "", "", Data, false, false)
		{
		}

		/// <summary>생성자</summary>
		/// <param name="Result">응답 결과</param>
		/// <param name="Code">응답 코드</param>
		/// <param name="Message">응답 메세지</param>
		public ResponseList(EnumResponseResult Result, string Code, string Message)
			: base(Result, Code, Message, false, false)
		{
		}
	}
}
