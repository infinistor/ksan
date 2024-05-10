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

namespace PortalData.Requests.Agent
{
	/// <summary>서버 초기화 요청 클래스</summary>
	public class RequestAgentInitialize
	{
		/// <summary> 서버 주소 </summary>
		public string LocalIp { get; set; }

		/// <summary> 서버 아이디 </summary>
		public string ServerId { get; set; }

		/// <summary> 서버 이름 </summary>
		public string ServerName { get; set; }

		/// <summary> Agent 아이디 </summary>
		public string AgentServiceId { get; set; }

		/// <summary> 포탈 주소 </summary>
		public string PortalHost { get; set; }

		/// <summary> 포탈 포트 </summary>
		public int PortalPort { get; set; }

		/// <summary> RabbitMQ 주소 </summary>
		public string MQHost { get; set; }

		/// <summary> RabbitMQ 포트 </summary>
		public int MQPort { get; set; }

		/// <summary> RabbitMQ 유저 </summary>
		public string MQUser { get; set; }

		/// <summary> RabbitMQ 비밀번호 </summary>
		public string MQPassword { get; set; }

		/// <summary> 포탈 API 키 </summary>
		public string PortalApiKey { get; set; }

	}
}