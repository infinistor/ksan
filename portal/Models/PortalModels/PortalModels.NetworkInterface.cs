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

namespace PortalModels
{
	/// <summary> 네트워크 인터페이스 정보 </summary>
	public partial class NetworkInterface
	{
		public NetworkInterface()
		{
			this.Dhcp = (PortalModels.EnumDbYesNo)Enum.Parse(typeof(PortalModels.EnumDbYesNo), "0");
			this.LinkState = (PortalModels.EnumDbNetworkLinkState)Enum.Parse(typeof(PortalModels.EnumDbNetworkLinkState), "0");
			this.BandWidth = 0m;
			this.Rx = 0m;
			this.Tx = 0m;
			this.NetworkInterfaceVlans = new List<NetworkInterfaceVlan>();
			this.NetworkInterfaceUsages = new List<NetworkInterfaceUsage>();
			OnCreated();
		}

		/// <summary> 네트워크 인터페이스 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 서버 아이디 </summary>
		public virtual Guid ServerId { get; set; }

		/// <summary> 인터페이스명 </summary>
		public virtual string Name { get; set; }

		/// <summary> 설명 </summary>
		public virtual string Description { get; set; }

		/// <summary> DHCP 사용 여부 </summary>
		public virtual EnumDbYesNo? Dhcp { get; set; }

		/// <summary> 맥주소 </summary>
		public virtual string MacAddress { get; set; }

		/// <summary> 네트워크 연결 상태 </summary>
		public virtual EnumDbNetworkLinkState? LinkState { get; set; }

		/// <summary> 아이피 주소 </summary>
		public virtual string IpAddress { get; set; }

		/// <summary> 서브넷 마스크 </summary>
		public virtual string SubnetMask { get; set; }

		/// <summary> 게이트웨이 </summary>
		public virtual string Gateway { get; set; }

		/// <summary> DNS #1 </summary>
		public virtual string Dns1 { get; set; }

		/// <summary> DNS #2 </summary>
		public virtual string Dns2 { get; set; }

		/// <summary> 네트워크 BandWidth </summary>
		public virtual decimal? BandWidth { get; set; }

		/// <summary> 관리용 인터페이스인지 여부 </summary>
		public virtual bool IsManagement { get; set; }

		/// <summary> 수신 속도 </summary>
		public virtual decimal? Rx { get; set; }

		/// <summary> 송신 속도 </summary>
		public virtual decimal? Tx { get; set; }

		/// <summary> 등록자 아이디 </summary>
		public virtual Guid? RegId { get; set; }

		/// <summary> 등록자명 </summary>
		public virtual string RegName { get; set; }

		/// <summary> 등록일시 </summary>
		public virtual DateTime? RegDate { get; set; }

		/// <summary> 수정자 아이디 </summary>
		public virtual Guid? ModId { get; set; }

		/// <summary> 수정자명 </summary>
		public virtual string ModName { get; set; }

		/// <summary> 수정일시 </summary>
		public virtual DateTime? ModDate { get; set; }

		public virtual Server Server { get; set; }

		public virtual User ModUser { get; set; }

		public virtual IList<NetworkInterfaceVlan> NetworkInterfaceVlans { get; set; }

		public virtual User RegUser { get; set; }

		public virtual IList<NetworkInterfaceUsage> NetworkInterfaceUsages { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		#endregion
	}

}
