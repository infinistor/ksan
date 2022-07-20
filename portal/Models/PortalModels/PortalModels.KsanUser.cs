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
using System.Collections.Generic;

namespace PortalModels
{
	/// <summary> 유저 정보 </summary>
	public partial class KsanUser
	{

		public KsanUser()
		{
			this.UserDiskPools = new List<UserDiskPool>();
			OnCreated();
		}

		/// <summary> 엑세스 키 </summary>
		public virtual string AccessKey { get; set; }

		/// <summary> 비밀 키 </summary>
		public virtual string SecretKey { get; set; }

		/// <summary> 유저 아이디 </summary>
		public virtual Guid Id { get; set; }

		/// <summary> 유저명 </summary>
		public virtual string Name { get; set; }

		/// <summary> 이메일 </summary>
		public virtual string Email { get; set; }

		public virtual IList<UserDiskPool> UserDiskPools { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		public override bool Equals(object obj)
		{
			KsanUser toCompare = obj as KsanUser;
			if (toCompare == null)
			{
				return false;
			}

			if (!Object.Equals(this.AccessKey, toCompare.AccessKey))
				return false;
			if (!Object.Equals(this.SecretKey, toCompare.SecretKey))
				return false;

			return true;
		}

		public override int GetHashCode()
		{
			int hashCode = 13;
			hashCode = (hashCode * 7) + AccessKey.GetHashCode();
			hashCode = (hashCode * 7) + SecretKey.GetHashCode();
			return hashCode;
		}

		#endregion
	}

}
