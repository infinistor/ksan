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
using System.ComponentModel.DataAnnotations;
using MTLib.Core;

namespace PortalData.ValidationAttributes
{
	/// <summary>해당 문자열이 아이피 주소에 해당하는지에 대한 유효성 검사 속성 클래스</summary>
	[AttributeUsage(AttributeTargets.Property)]
	public class IpAddressAttribute : ValidationAttribute
	{
		/// <summary>유효한지 여부 검사</summary>
		/// <param name="value">검사할 값</param>
		/// <returns>유효한지 여부</returns>
		public override bool IsValid(object value)
		{
			bool Result = false;

			// 값이 문자열인 경우
			if (value == null || value is string)
			{
				if (value == null || ((string)value).IsEmpty() || ((string)value).IsIpAddress())
					Result = true;
			}

			return Result;
		}
	}
}