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
	/// <summary> 서비스 사용 정보 </summary>
	public partial class ServiceUsage
	{

		public ServiceUsage()
		{
			this.CpuUsage = 0f;
			this.MemoryTotal = 0m;
			this.MemoryUsed = 0m;
			this.ThreadCount = 0;
			OnCreated();
		}

		/// <summary> 서비스 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 등록일시 </summary>
		public virtual DateTime RegDate { get; set; }

		/// <summary> CPU 사용률 </summary>
		public virtual float? CpuUsage { get; set; }

		/// <summary> 전체 메모리 크기 </summary>
		public virtual decimal? MemoryTotal { get; set; }

		/// <summary> 사용 메모리 크기 </summary>
		public virtual decimal? MemoryUsed { get; set; }

		/// <summary> 스레드수 </summary>
		public virtual int ThreadCount { get; set; }

		public virtual Service Service { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		public override bool Equals(object obj)
		{
			ServiceUsage toCompare = obj as ServiceUsage;
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
