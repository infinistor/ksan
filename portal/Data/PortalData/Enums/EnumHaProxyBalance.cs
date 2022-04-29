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
namespace PortalData.Enums
{
	/// <summary>HA Proxy Balance 옵션</summary>
	public enum EnumHaProxyBalance
	{
		/// <summary>순차적으로 분배</summary>
		RoundRobin,
		/// <summary>서버에 부여된 가중치에 따라서 분배</summary>
		StaticRr,
		/// <summary>접속수가 가장 적은 서버로 분배</summary>
		LeastConn,
		/// <summary>운영중인 서버의 가중치를 나눠서 접속자 IP 해싱(hashing)해서 분배</summary>
		Source
	}
}