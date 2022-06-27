﻿/*
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
	/// <summary> 디스크 사용량 </summary>
	public partial class DiskUsage
	{

		public DiskUsage()
		{
			this.UsedInode = 0m;
			this.UsedSize = 0m;
			this.Read = 0m;
			this.Write = 0m;
			OnCreated();
		}

		/// <summary> 디스크 유세이지 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 생성시간 </summary>
		public virtual DateTime RegDate { get; set; }

		/// <summary> Inode사용량 </summary>
		public virtual decimal UsedInode { get; set; }

		/// <summary> 현재 사용량 </summary>
		public virtual decimal UsedSize { get; set; }

		/// <summary> 디스크 읽기 </summary>
		public virtual decimal Read { get; set; }

		/// <summary> 디스크 쓰기 </summary>
		public virtual decimal Write { get; set; }

		public virtual Disk Disk { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		public override bool Equals(object obj)
		{
			DiskUsage toCompare = obj as DiskUsage;
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
