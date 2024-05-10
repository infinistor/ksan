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
	/// <summary> 서버 정보 </summary>
	public partial class Server
	{

		public Server()
		{
			this.Clock = 0;
			this.State = (PortalModels.EnumDbServerState)Enum.Parse(typeof(PortalModels.EnumDbServerState), "-2");
			this.LoadAverage1M = 0f;
			this.LoadAverage5M = 0f;
			this.LoadAverage15M = 0f;
			this.MemoryTotal = 0m;
			this.MemoryUsed = 0m;
			this.NetworkInterfaces = new List<NetworkInterface>();
			this.Disks = new List<Disk>();
			this.ServerUsages = new List<ServerUsage>();
			this.Services = new List<Service>();
			OnCreated();
		}

		/// <summary> 서버 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 서버명 </summary>
		public virtual string Name { get; set; }

		public string HostName { get; set; }

		/// <summary> 설명 </summary>
		public virtual string Description { get; set; }

		/// <summary> CPU 모델 </summary>
		public virtual string CpuModel { get; set; }

		/// <summary> CPU 클럭 </summary>
		public virtual short Clock { get; set; }

		/// <summary> 서버 상태 </summary>
		public virtual EnumDbServerState State { get; set; }

		/// <summary> Rack 정보 </summary>
		public virtual string Rack { get; set; }

		/// <summary> 1분 Load Average </summary>
		public virtual float? LoadAverage1M { get; set; }

		/// <summary> 5분 Load Average </summary>
		public virtual float? LoadAverage5M { get; set; }

		/// <summary> 15분 Load Average </summary>
		public virtual float? LoadAverage15M { get; set; }

		/// <summary> 전체 메모리 크기 </summary>
		public virtual decimal? MemoryTotal { get; set; }

		/// <summary> 사용 메모리 크기 </summary>
		public virtual decimal? MemoryUsed { get; set; }

		/// <summary> 수정자 아이디 </summary>
		public virtual Guid? ModId { get; set; }

		/// <summary> 수정자명 </summary>
		public virtual string ModName { get; set; }

		/// <summary> 수정일시 </summary>
		public virtual DateTime? ModDate { get; set; }

		/// <summary> 네트워크 인터페이스 목록 </summary>
		public virtual IList<NetworkInterface> NetworkInterfaces { get; set; }

		/// <summary> 수정한 사용자 정보 </summary>
		public virtual User ModUser { get; set; }

		/// <summary> 디스크 목록 </summary>
		public virtual IList<Disk> Disks { get; set; }

		public virtual IList<ServerUsage> ServerUsages { get; set; }

		public virtual IList<Service> Services { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		#endregion
	}

}
