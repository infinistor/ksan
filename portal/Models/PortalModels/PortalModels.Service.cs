﻿/*
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
	/// <summary> 서비스 정보 </summary>
	public partial class Service
	{

		public Service()
		{
			this.HaAction = (PortalModels.EnumDbHaAction)Enum.Parse(typeof(PortalModels.EnumDbHaAction), "-1");
			this.State = (PortalModels.EnumDbServiceState)Enum.Parse(typeof(PortalModels.EnumDbServiceState), "-2");
			this.CpuUsage = 0f;
			this.MemoryTotal = 0m;
			this.MemoryUsed = 0m;
			this.ThreadCount = 0;
			this.Vlans = new List<ServiceNetworkInterfaceVlan>();
			this.ServiceUsages = new List<ServiceUsage>();
			this.ServiceEventLogs = new List<ServiceEventLog>();
			OnCreated();
		}

		/// <summary> 서비스 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 서버 아이디 </summary>
		public virtual Guid ServerId { get; set; }

		/// <summary> 서비스 그룹 아이디 </summary>
		public virtual Guid? GroupId { get; set; }

		/// <summary> 서비스명 </summary>
		public virtual string Name { get; set; }

		/// <summary> 설명 </summary>
		public virtual string Description { get; set; }

		/// <summary> 서비스 타입 </summary>
		public virtual EnumDbServiceType ServiceType { get; set; }

		/// <summary> HA 작업 </summary>
		public virtual EnumDbHaAction HaAction { get; set; }

		/// <summary> 서비스 상태 </summary>
		public virtual EnumDbServiceState State { get; set; }

		/// <summary> CPU 사용률 </summary>
		public virtual float? CpuUsage { get; set; }

		/// <summary> 전체 메모리 크기 </summary>
		public virtual decimal? MemoryTotal { get; set; }

		/// <summary> 사용 메모리 크기 </summary>
		public virtual decimal? MemoryUsed { get; set; }

		/// <summary> 스레드수 </summary>
		public virtual int ThreadCount { get; set; }

		/// <summary> 수정자 아이디 </summary>
		public virtual Guid? ModId { get; set; }

		/// <summary> 수정자명 </summary>
		public virtual string ModName { get; set; }

		/// <summary> 수정일시 </summary>
		public virtual DateTime? ModDate { get; set; }

		/// <summary> VLAN 목록 </summary>
		public virtual IList<ServiceNetworkInterfaceVlan> Vlans { get; set; }

		/// <summary> 서비스 그룹 정보 </summary>
		public virtual ServiceGroup ServiceGroup { get; set; }

		/// <summary> 수정 사용자 정보 </summary>
		public virtual User ModUser { get; set; }

		public virtual IList<ServiceUsage> ServiceUsages { get; set; }

		public virtual Server Server { get; set; }

		public virtual IList<ServiceEventLog> ServiceEventLogs { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		#endregion
	}

}
