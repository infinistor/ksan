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
using System.Linq;
using System.Reflection;
using MTLib.Core;

namespace PortalData.Configs
{
	/// <summary>환경 설정 키 및 기본 값 속성 클래스</summary>
	public class KeyAndDefaultValueAttribute : Attribute
	{
		/// <summary>환경 설정 키</summary>
		public string Key { get; set; } = "";

		/// <summary>환경 설정 기본 값</summary>
		public string DefaultValue { get; set; } = "";
	}

	/// <summary>ConfigKeyAndDefaultValueAttribute 객체 확장 클래스</summary>
	public static class ConfigKeyAndDefaultValueAttributeExtension
	{
		/// <summary>특정 객체의 T 타입의 속성 객체를 가져온다.</summary>
		/// <typeparam name="T">속성 객체 타입</typeparam>
		/// <param name="Value">Object 객체</param>
		/// <param name="PropertyName">프로퍼티명</param>
		/// <returns>T 타입의 속성 객체</returns>
		public static T GetAttribute<T>(this object Value, string PropertyName) where T : Attribute
		{
			T Result = default(T);
			try
			{
				if (Value != null)
				{
					MemberInfo MemberInfo = Value.GetType().GetMember(PropertyName).FirstOrDefault();
					if (MemberInfo != null)
						Result = (T)MemberInfo.GetCustomAttributes(typeof(T), false).FirstOrDefault();
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return Result;
		}

		/// <summary>ConfigKeyAndDefaultValueAttribute 객체의 Key 값을 가져온다.</summary>
		/// <param name="Value">ConfigKeyAndDefaultValueAttribute 객체</param>
		/// <returns>Key 값</returns>
		public static string GetKey(this KeyAndDefaultValueAttribute Value)
		{
			string Result = "";
			try
			{
				if (Value != null)
					Result = Value.Key;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return Result;
		}

		/// <summary>ConfigKeyAndDefaultValueAttribute 객체의 기본 값을 가져온다.</summary>
		/// <param name="Value">ConfigKeyAndDefaultValueAttribute 객체</param>
		/// <returns>기본 값</returns>
		public static string GetDefaultValue(this KeyAndDefaultValueAttribute Value)
		{
			string Result = "";
			try
			{
				if (Value != null)
					Result = Value.DefaultValue;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return Result;
		}
	}
}
