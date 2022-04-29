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
namespace PortalData.Responses.Accounts
{
	/// <summary>권한 응답 클래스</summary>
	public class ResponseClaim
	{
		/// <summary>권한 아이디</summary>
		public int Id { get; set; } = 0;

		/// <summary>권한 타입</summary>
		public string ClaimType { get; set; } = "";

		/// <summary>권한 값</summary>
		public string ClaimValue { get; set; } = "";

		/// <summary>권한 제목</summary>
		public string ClaimTitle { get; set; } = "";

		/// <summary>뎁스</summary>
		public short Depth { get; set; } = 0;

		/// <summary>하위 권한 존재 여부</summary>
		public bool HasChild { get; set; } = false;

		/// <summary>문자열로 변환</summary>
		/// <returns></returns>
		public override string ToString()
		{
			return string.Format("{0} {1}", ClaimType, ClaimValue);
		}
	}
}
