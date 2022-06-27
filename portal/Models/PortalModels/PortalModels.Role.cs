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
	/// <summary> 역할(권한 그룹) </summary>
	public partial class Role
	{

		public Role()
		{
			this.RoleClaims = new List<RoleClaim>();
			this.AllowedConnectionIps = new List<AllowedConnectionIp>();
			this.UserRoles = new List<UserRole>();
			OnCreated();
		}

		/// <summary> 역할(권한그룹) 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 병행지문 </summary>
		public virtual string ConcurrencyStamp { get; set; }

		/// <summary> 역할(권한 그룹)명 </summary>
		public virtual string Name { get; set; }

		/// <summary> 표준화 된 역할(권한 그룹)명 </summary>
		public virtual string NormalizedName { get; set; }

		/// <summary> 역할 권한 목록 </summary>
		public virtual IList<RoleClaim> RoleClaims { get; set; }

		/// <summary> 접속 허용 아이피 목록 </summary>
		public virtual IList<AllowedConnectionIp> AllowedConnectionIps { get; set; }

		/// <summary> 역할(권한그룹) 내 사용자 목록 </summary>
		public virtual IList<UserRole> UserRoles { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		#endregion
	}

}
