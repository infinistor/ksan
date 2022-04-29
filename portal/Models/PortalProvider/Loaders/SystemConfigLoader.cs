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
using PortalData.Configs;
using PortalData.Responses.Common;
using PortalModels;
using PortalProviderInterface;
using PortalResources;
using Microsoft.EntityFrameworkCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.EntityFramework;
using MTLib.Reflection;

namespace PortalProvider.Loaders
{
	/// <summary>시스템 환경 설정 로더</summary>
	public class SystemConfigLoader : ISystemConfigLoader
	{
		/// <summary>설정 목록</summary>
		private List<Config> m_configs = new List<Config>();

		/// <summary>생성자</summary>
		public SystemConfigLoader()
		{
			try
			{
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}

		/// <summary>DB에서 설정을 로드한다.</summary>
		/// <param name="context">DB 컨텍스트</param>
		/// <returns>해당 설정 데이터</returns>
		public async Task<bool> Load(DbContext context)
		{
			bool result = false;
			try
			{
				if (context != null)
				{
					// 전체 설정을 가져온다.
					QueryResults<Config> configs = await ((PortalModel)context).Configs.AsNoTracking()
																		.OrderByWithDirection(i => i.Key)
																		.CreateListAsync();

					lock (m_configs)
					{
						// 전체 설정이 유효하지 않은 경우
						if (configs == null)
							m_configs = new List<Config>();
						// 전체 설정이 유효한 경우
						else
							m_configs = configs.Items;
					}

					// 화면 표시명 설정을 가져온다.
					ResponseData<ResponseConfig> displayNameConfig = Get("COMMON.USER_DISPLAY_NAME_FORMAT");
					// 화면 노출 이름 설정이 있는 경우
					if (displayNameConfig.Result == EnumResponseResult.Success)
						UserExtension.DisplayNameFormat = displayNameConfig.Data.Value;

					result = true;
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}

		/// <summary>특정 설정 값에 대한 문자열을 가져온다.</summary>
		/// <param name="key">설정 키</param>
		/// <returns>설정 값</returns>
		public string GetValue(string key)
		{
			string result = "";
			ResponseData<ResponseConfig> config;
			try
			{
				// 해당 설정을 가져온다.
				config = this.Get(key);

				// 설정을 가져오는데 성공한 경우
				if (config.Result == EnumResponseResult.Success)
					result = config.Data.Value;
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}

		/// <summary>특정 설정 값을 T 타입으로 변환하여 반환한다.</summary>
		/// <typeparam name="T">변환할 타입</typeparam>
		/// <param name="key">설정 키</param>
		/// <returns>설정 값</returns>
		public T GetValue<T>(string key)
		{
			T result = default(T);
			ResponseData<ResponseConfig> config;
			try
			{
				// 해당 설정을 가져온다.
				config = this.Get(key);

				// 설정을 가져오는데 성공한 경우
				if (config.Result == EnumResponseResult.Success)
				{
					if (typeof(T).IsEnum)
						result = config.Data.Value.ToEnum<T>();
					else
						result = (T)Convert.ChangeType(config.Data.Value, typeof(T));
				}
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}

		/// <summary>특정 설정을 가져온다.</summary>
		/// <param name="key">설정 키</param>
		/// <returns>해당 설정 데이터</returns>
		public ResponseData<ResponseConfig> Get(string key)
		{
			ResponseData<ResponseConfig> result = new ResponseData<ResponseConfig>();
			ResponseConfig item = null;
			try
			{
				// 설정 키가 유효한 경우
				if (!key.IsEmpty())
				{
					lock (m_configs)
					{
						// 해당 설정을 가져온다.
						Config config = m_configs.Where(i => i.Key == key).FirstOrDefault();
						if (config != null)
						{
							item = new ResponseConfig();
							item.CopyValueFrom(config);
						}
					}

					// 해당 설정이 존재하는 경우
					if (item != null)
					{
						result.Data = item;
						result.Result = EnumResponseResult.Success;
					}
					// 해당 설정이 존재하지 않는 경우
					else
					{
						result.Code = Resource.EC_COMMON__NOT_FOUND;
						result.Message = Resource.EM_COMMON__NOT_FOUND;
					}
				}
				// 설정 키가 유효하지 않은 경우
				else
				{
					result.Code = Resource.EC_COMMON__INVALID_REQUEST;
					result.Message = Resource.EM_COMMON__INVALID_REQUEST;
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

		/// <summary>전체 설정을 가져온다.</summary>
		/// <returns>해당 설정 데이터</returns>
		public ResponseList<ResponseConfig> Get()
		{
			ResponseList<ResponseConfig> result = new ResponseList<ResponseConfig>();
			try
			{
				lock (m_configs)
				{
					result.Data = m_configs.CreateList<Config, ResponseConfig>();
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

		/// <summary>주어진 문자열로 시작하는 키를 가지는 설정 목록을 가져온다.</summary>
		/// <param name="keyStartWith">키 시작 문자열</param>
		/// <returns>설정 목록</returns>
		public ResponseList<ResponseConfig> GetStartsWith(string keyStartWith)
		{
			ResponseList<ResponseConfig> result = new ResponseList<ResponseConfig>();
			try
			{
				lock (m_configs)
				{
					result.Data = m_configs.Where(i => i.Key.StartsWith(keyStartWith)).CreateList<Config, ResponseConfig>();
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

		/// <summary>초기화할 아이템을 가져온다.</summary>
		/// <typeparam name="T">설정 객체 타입</typeparam>
		/// <param name="configs">환경 설정 목록</param>
		/// <param name="propertyName">프로퍼티명</param>
		/// <returns>초기화할 아이템 객체</returns>
		public static KeyValuePair<string, string>? GetInitializationItem<T>(ResponseList<ResponseConfig> configs, string propertyName)
		{
			KeyValuePair<string, string>? result = null;
			T dumyConfig = Activator.CreateInstance<T>();
			try
			{
				if (configs.Data.Items.Count(i => i.Key == dumyConfig.GetAttribute<KeyAndDefaultValueAttribute>(propertyName).Key) == 0)
					result = new KeyValuePair<string, string>(dumyConfig.GetAttribute<KeyAndDefaultValueAttribute>(propertyName).Key
																, dumyConfig.GetAttribute<KeyAndDefaultValueAttribute>(propertyName).DefaultValue);
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
			return result;
		}
	}
}
