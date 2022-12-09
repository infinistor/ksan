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
	/// <summary> ksanGW 로그 </summary>
	public partial class S3Logging
	{

		public S3Logging()
		{
			OnCreated();
		}

		/// <summary> 아이디 </summary>
		public virtual long Id { get; set; }

		/// <summary> 유저명 </summary>
		public virtual string UserName { get; set; }

		/// <summary> 버킷명 </summary>
		public virtual string BucketName { get; set; }

		/// <summary> 발생시각 </summary>
		public virtual DateTime InDate { get; set; }

		/// <summary> 원격 호스트 </summary>
		public virtual string RemoteHost { get; set; }

		public virtual string RequestUser { get; set; }

		/// <summary> 요청 아이디 </summary>
		public virtual string RequestId { get; set; }

		/// <summary> API 명 </summary>
		public virtual string Operation { get; set; }

		/// <summary> 오브젝트명 </summary>
		public virtual string ObjectName { get; set; }

		/// <summary> 요청URI </summary>
		public virtual string RequestUri { get; set; }

		/// <summary> 상태코드 </summary>
		public virtual int StatusCode { get; set; }

		/// <summary> 에러코드 </summary>
		public virtual string ErrorCode { get; set; }

		/// <summary> 응답 크기 </summary>
		public virtual long ResponseLength { get; set; }

		/// <summary> 오브젝트 크기 </summary>
		public virtual long ObjectLength { get; set; }

		/// <summary> 총 소요시간 </summary>
		public virtual long TotalTime { get; set; }

		/// <summary> 요청 크기 </summary>
		public virtual long RequestLength { get; set; }

		/// <summary> 참조 </summary>
		public virtual string Rererer { get; set; }

		/// <summary> 대리인 </summary>
		public virtual string UserAgent { get; set; }

		/// <summary> 버전아이디 </summary>
		public virtual string VersionId { get; set; }

		/// <summary> 호스트아이디 </summary>
		public virtual string HostId { get; set; }

		/// <summary> 사인 </summary>
		public virtual string Sign { get; set; }

		/// <summary> SSL 그륩 </summary>
		public virtual string SslGroup { get; set; }

		/// <summary> 사인 종류 </summary>
		public virtual string SignType { get; set; }

		/// <summary> 엔드포인트 </summary>
		public virtual string Endpoint { get; set; }

		/// <summary> TLS 버전 </summary>
		public virtual string TlsVersion { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		#endregion
	}

}
