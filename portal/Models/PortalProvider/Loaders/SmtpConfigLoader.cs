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
using System.Collections.Generic;
using PortalData;
using PortalData.Configs;
using PortalData.Responses.Common;
using PortalProviderInterface;
using MTLib.AspNetCore;
using MTLib.Core;

namespace PortalProvider.Loaders
{
	/// <summary>Smtp 설정 로더</summary>
	public class SmtpConfigLoader : ISmtpConfigLoader
	{
		/// <summary>환경 설정 로더</summary>
		private ISystemConfigLoader m_configLoader;

		/// <summary>생성자</summary>
		/// <param name="loader">설정로더</param>
		public SmtpConfigLoader(ISystemConfigLoader loader)
		{
			m_configLoader = loader;
		}

		/// <summary>Smtp 관련 설정을 반환한다.</summary>
		/// <returns>Smtp 관련 설정 객체</returns>
		public SmtpConfig GetConfig()
		{
			CSSPSmtpConfig result = new CSSPSmtpConfig();
			try
			{
				result.Host = m_configLoader.GetValue(result.GetAttribute<KeyAndDefaultValueAttribute>(nameof(result.Host)).Key);
				result.Port = m_configLoader.GetValue<int>(result.GetAttribute<KeyAndDefaultValueAttribute>(nameof(result.Port)).Key);
				result.Account = m_configLoader.GetValue(result.GetAttribute<KeyAndDefaultValueAttribute>(nameof(result.Account)).Key);
				result.Password = m_configLoader.GetValue(result.GetAttribute<KeyAndDefaultValueAttribute>(nameof(result.Password)).Key);
				result.UseSsl = m_configLoader.GetValue<bool>(result.GetAttribute<KeyAndDefaultValueAttribute>(nameof(result.UseSsl)).Key);
				result.DefaultSenderEmail = m_configLoader.GetValue(result.GetAttribute<KeyAndDefaultValueAttribute>(nameof(result.DefaultSenderEmail)).Key);
				result.DefaultSenderName = m_configLoader.GetValue(result.GetAttribute<KeyAndDefaultValueAttribute>(nameof(result.DefaultSenderName)).Key);
			}
			catch (System.Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}

		/// <summary>초기화해야할 설정 목록을 가져온다.</summary>
		/// <returns>초기화해야할 설정 목록</returns>
		public List<KeyValuePair<string, string>> GetListForInitialization()
		{
			List<KeyValuePair<string, string>> result = new List<KeyValuePair<string, string>>();
			ResponseList<ResponseConfig> configs;
			KeyValuePair<string, string>? item;
			try
			{
				// SMTP 관련 설정을 가져온다.
				configs = m_configLoader.GetStartsWith("SMTP.");

				// 설정이 유효한 경우
				if (configs != null)
				{
					item = SystemConfigLoader.GetInitializationItem<CSSPSmtpConfig>(configs, "Host");
					if (item != null) result.Add((KeyValuePair<string, string>)item);
					item = SystemConfigLoader.GetInitializationItem<CSSPSmtpConfig>(configs, "Port");
					if (item != null) result.Add((KeyValuePair<string, string>)item);
					item = SystemConfigLoader.GetInitializationItem<CSSPSmtpConfig>(configs, "Account");
					if (item != null) result.Add((KeyValuePair<string, string>)item);
					item = SystemConfigLoader.GetInitializationItem<CSSPSmtpConfig>(configs, "Password");
					if (item != null) result.Add((KeyValuePair<string, string>)item);
					item = SystemConfigLoader.GetInitializationItem<CSSPSmtpConfig>(configs, "UseSsl");
					if (item != null) result.Add((KeyValuePair<string, string>)item);
					item = SystemConfigLoader.GetInitializationItem<CSSPSmtpConfig>(configs, "DefaultSenderEmail");
					if (item != null) result.Add((KeyValuePair<string, string>)item);
					item = SystemConfigLoader.GetInitializationItem<CSSPSmtpConfig>(configs, "DefaultSenderName");
					if (item != null) result.Add((KeyValuePair<string, string>)item);
				}
			}
			catch (System.Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}
	}
}
