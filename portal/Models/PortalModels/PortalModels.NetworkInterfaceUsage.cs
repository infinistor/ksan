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
	/// <summary> 네트워크 인터페이스 사용 정보 </summary>
	public partial class NetworkInterfaceUsage
	{

		public NetworkInterfaceUsage()
		{
			this.BandWidth = 0m;
			this.Rx = 0m;
			this.Tx = 0m;
			OnCreated();
		}

		/// <summary> 네트워크 인터페이스 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 등록일시 </summary>
		public virtual DateTime RegDate { get; set; }

		/// <summary> 네트워크 BandWidth </summary>
		public virtual decimal? BandWidth { get; set; }

		/// <summary> 수신 속도 </summary>
		public virtual decimal? Rx { get; set; }

		/// <summary> 송신 속도 </summary>
		public virtual decimal? Tx { get; set; }

		public virtual NetworkInterface NetworkInterface { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		public override bool Equals(object obj)
		{
			NetworkInterfaceUsage toCompare = obj as NetworkInterfaceUsage;
			if (toCompare == null)
			{
				return false;
			}

			if (!Object.Equals(this.Id, toCompare.Id))
				return false;
			if (!Object.Equals(this.RegDate, toCompare.RegDate))
				return false;

			return true;
		}

		public override int GetHashCode()
		{
			int hashCode = 13;
			hashCode = (hashCode * 7) + Id.GetHashCode();
			hashCode = (hashCode * 7) + RegDate.GetHashCode();
			return hashCode;
		}

		#endregion
	}

}
