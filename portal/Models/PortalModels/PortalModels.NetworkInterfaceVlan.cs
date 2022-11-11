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
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Data.Common;
using System.Linq;
using System.Linq.Expressions;

namespace PortalModels
{
	/// <summary> 네트워크 인터페이스 VLAN 설정 </summary>
	public partial class NetworkInterfaceVlan
	{

		public NetworkInterfaceVlan()
		{
			this.ServiceNetworkInterfaceVlans = new List<ServiceNetworkInterfaceVlan>();
			OnCreated();
		}

		/// <summary> 네트워크 인터페이스 VLAN 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 네트워크 인터페이스 아이디 </summary>
		public virtual Guid InterfaceId { get; set; }

		/// <summary> VLAN 태그 </summary>
		public virtual int Tag { get; set; }

		/// <summary> 아이피 주소 </summary>
		public virtual string IpAddress { get; set; }

		/// <summary> 서브넷 마스크 </summary>
		public virtual string SubnetMask { get; set; }

		/// <summary> 게이트웨이 </summary>
		public virtual string Gateway { get; set; }

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

		/// <summary> 등록 사용자 정보 </summary>
		public virtual User RegUser { get; set; }

		/// <summary> 수정 사용자 정보 </summary>
		public virtual User ModUser { get; set; }

		/// <summary> 네트워크 인터페이스 정보 </summary>
		public virtual NetworkInterface NetworkInterface { get; set; }

		/// <summary> 서비스 네트워크 인터페이스 VLAN 목록 </summary>
		public virtual IList<ServiceNetworkInterfaceVlan> ServiceNetworkInterfaceVlans { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		#endregion
	}

}
