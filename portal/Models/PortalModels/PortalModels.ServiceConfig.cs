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

namespace PortalModels
{
	/// <summary> 서비스 설정 정보 </summary>
	public partial class ServiceConfig
	{
		public ServiceConfig()
		{
			this.LastVersion = true;
			OnCreated();
		}

		/// <summary> 서비스 종류 </summary>
		public virtual EnumDbServiceType Type { get; set; }

		/// <summary> 설정 버전 </summary>
		public virtual int Version { get; set; }

		/// <summary> 설정 정보 </summary>
		public virtual string Config { get; set; }

		/// <summary> 추가 일자 </summary>
		public virtual DateTime RegDate { get; set; }

		/// <summary> 최신 버전 여부 </summary>
		public virtual bool LastVersion { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		public override bool Equals(object obj)
		{
			ServiceConfig toCompare = obj as ServiceConfig;
			if (toCompare == null)
			{
				return false;
			}

			if (!Object.Equals(this.Type, toCompare.Type))
				return false;
			if (!Object.Equals(this.Version, toCompare.Version))
				return false;

			return true;
		}

		public override int GetHashCode()
		{
			int hashCode = 13;
			hashCode = (hashCode * 7) + Type.GetHashCode();
			hashCode = (hashCode * 7) + Version.GetHashCode();
			return hashCode;
		}

		#endregion
	}
}
