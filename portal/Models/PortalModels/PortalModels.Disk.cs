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
	/// <summary> 디스크 정보 </summary>
	public partial class Disk
	{

		public Disk()
		{
			this.State = (PortalModels.EnumDbDiskState)Enum.Parse(typeof(PortalModels.EnumDbDiskState), "-2");
			this.TotalInode = 0m;
			this.ReservedInode = 0m;
			this.UsedInode = 0m;
			this.TotalSize = 0m;
			this.ReservedSize = 0m;
			this.UsedSize = 0m;
			this.ServiceDisks = new List<ServiceDisk>();
			OnCreated();
		}

		/// <summary> 디스크 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 디스크 Pool 아이디 </summary>
		public virtual Guid? DiskPoolId { get; set; }

		/// <summary> 디스크 번호 (디스크 아이디의 HashCode) </summary>
		public virtual string DiskNo { get; set; }

		/// <summary> 서버 아이디 </summary>
		public virtual Guid ServerId { get; set; }

		/// <summary> 마운트 경로 </summary>
		public virtual string Path { get; set; }

		/// <summary> 서비스 상태 </summary>
		public virtual EnumDbDiskState State { get; set; }

		/// <summary> 전체 inode 수 </summary>
		public virtual decimal TotalInode { get; set; }

		/// <summary> 예약/시스템 inode 수 </summary>
		public virtual decimal ReservedInode { get; set; }

		/// <summary> 사용된 inode 수 </summary>
		public virtual decimal UsedInode { get; set; }

		/// <summary> 전체 크기 </summary>
		public virtual decimal TotalSize { get; set; }

		/// <summary> 예약/시스템 크기 </summary>
		public virtual decimal ReservedSize { get; set; }

		/// <summary> 사용된 크기 </summary>
		public virtual decimal UsedSize { get; set; }

		/// <summary> 디스크 읽기/쓰기 모드 </summary>
		public virtual EnumDbDiskRwMode RwMode { get; set; }

		/// <summary> 서버 정보 </summary>
		public virtual Server Server { get; set; }

		/// <summary> 서비스 디스크 목록 </summary>
		public virtual IList<ServiceDisk> ServiceDisks { get; set; }

		/// <summary> 디스크 풀 정보 </summary>
		public virtual DiskPool DiskPool { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		#endregion
	}

}
