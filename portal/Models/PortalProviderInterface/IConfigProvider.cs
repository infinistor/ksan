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
using PortalData.Enums;
using PortalData.Requests.Configs;
using PortalData.Responses.Configs;

namespace PortalProviderInterface
{
	/// <summary>서비스 데이터 프로바이더 인터페이스</summary>
	public interface IConfigProvider : IBaseProvider
	{
		/// <summary>서비스의 설정 정보 목록을 가져온다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <returns>설정 정보가 포함된 결과 목록 객체</returns>
		Task<ResponseList<ResponseServiceConfig>> GetConfigList(EnumServiceType ServiceType);

		/// <summary>서비스의 최신 설정 정보를 가져온다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <returns>설정 정보가 포함된 결과 객체</returns>
		Task<ResponseData<ResponseServiceConfig>> GetConfig(EnumServiceType ServiceType);

		/// <summary> 특정 버전의 서비스 설정 정보를 가져온다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Version">서비스 버전</param>
		/// <returns>설정 정보가 포함된 결과 객체</returns>
		Task<ResponseData<ResponseServiceConfig>> GetConfig(EnumServiceType ServiceType, int Version);

		/// <summary>주어진 설정 정보를 저장한다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Config">서비스 설정 정보</param>
		/// <returns>설정 결과 객체</returns>
		Task<ResponseData<ResponseUpdateConfig>> SetConfig(EnumServiceType ServiceType, string Config);

		/// <summary>주어진 설정 정보를 저장한다.</summary>
		/// <param name="Request">서비스 설정 객체</param>
		/// <returns>설정 결과 객체</returns>
		Task<ResponseData<ResponseUpdateConfig>> SetConfig(RequestServiceConfig Request);

		/// <summary>서비스 설정을 특정 버전으로 한다.</summary
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Version">서비스 버전</param>
		/// <returns>설정 결과 객체</returns>
		Task<ResponseData<ResponseUpdateConfig>> SetConfigLastVersion(EnumServiceType ServiceType, int Version);

		/// <summary>서비스의 설정 정보를 제거한다.</summary>
		/// <param name="ServiceType">서비스 타입</param>
		/// <param name="Version">서비스 버전</param>
		/// <returns>삭제 결과 객체</returns>
		Task<ResponseData> RemoveConfig(EnumServiceType ServiceType, int Version);
	}
}