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
	/// <summary> 서비스 사용 디스크 정보 </summary>
	public partial class ServiceDisk
	{

		public ServiceDisk()
		{
			OnCreated();
		}

		/// <summary> 서비스 아이디 </summary>
		public virtual Guid ServiceId { get; set; }

		/// <summary> 디스크 아이디 </summary>
		public virtual Guid DiskId { get; set; }

		/// <summary> 서비스 정보 </summary>
		public virtual Service Service { get; set; }

		/// <summary> 디스크 정보 </summary>
		public virtual Disk Disk { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		public override bool Equals(object obj)
		{
			ServiceDisk toCompare = obj as ServiceDisk;
			if (toCompare == null)
			{
				return false;
			}

			if (!Object.Equals(this.ServiceId, toCompare.ServiceId))
				return false;
			if (!Object.Equals(this.DiskId, toCompare.DiskId))
				return false;

			return true;
		}

		public override int GetHashCode()
		{
			int hashCode = 13;
			hashCode = (hashCode * 7) + ServiceId.GetHashCode();
			hashCode = (hashCode * 7) + DiskId.GetHashCode();
			return hashCode;
		}

		#endregion
	}

}
