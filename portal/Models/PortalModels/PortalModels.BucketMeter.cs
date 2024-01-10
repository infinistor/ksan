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
	/// <summary> 버킷 사용량 로그 </summary>
	public partial class BucketMeter
	{

		public BucketMeter()
		{
			this.Used = 0;
			OnCreated();
		}

		/// <summary> 생성 시간 </summary>
		public virtual DateTime InDate { get; set; }

		/// <summary> 유저명 </summary>
		public virtual string UserName { get; set; }

		/// <summary> 버킷명 </summary>
		public virtual string BucketName { get; set; }

		/// <summary> 버킷 사용량 </summary>
		public virtual long Used { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		public override bool Equals(object obj)
		{
			BucketMeter toCompare = obj as BucketMeter;
			if (toCompare == null)
			{
				return false;
			}

			if (!Object.Equals(this.InDate, toCompare.InDate))
				return false;
			if (!Object.Equals(this.UserName, toCompare.UserName))
				return false;
			if (!Object.Equals(this.BucketName, toCompare.BucketName))
				return false;

			return true;
		}

		public override int GetHashCode()
		{
			int hashCode = 13;
			hashCode = (hashCode * 7) + InDate.GetHashCode();
			hashCode = (hashCode * 7) + UserName.GetHashCode();
			hashCode = (hashCode * 7) + BucketName.GetHashCode();
			return hashCode;
		}

		#endregion
	}

}
