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
	/// <summary> 버킷목록 </summary>
	public partial class BucketList
	{

		public BucketList()
		{
			this.FileCount = 0;
			this.UsedSize = 0;
			OnCreated();
		}

		/// <summary> 버킷명 </summary>
		public virtual string BucketName { get; set; }

		/// <summary> 볼륨명 </summary>
		public virtual string DiskPoolId { get; set; }

		/// <summary> 유저명 </summary>
		public virtual string UserId { get; set; }

		/// <summary> 유저 아이디 </summary>
		public virtual string UserName { get; set; }

		/// <summary> 오브젝트 테이블 명 </summary>
		public virtual string BucketId { get; set; }

		/// <summary> 파일 갯수 </summary>
		public virtual long FileCount { get; set; }

		/// <summary> 버킷 사용량 </summary>
		public virtual long UsedSize { get; set; }

		/// <summary> 권한 정보 </summary>
		public virtual string ACL { get; set; }

		/// <summary> URL 접근주소 </summary>
		public virtual string WEB { get; set; }

		/// <summary> CORS 설정 </summary>
		public virtual string CORS { get; set; }

		/// <summary> 수명주기 설정 </summary>
		public virtual string Lifecycle { get; set; }

		/// <summary> 버전 설정 </summary>
		public virtual string Versioning { get; set; }

		/// <summary> 접근 설정 </summary>
		public virtual string Access { get; set; }

		/// <summary> 태그 목록 </summary>
		public virtual string Tagging { get; set; }

		/// <summary> 암호화 설정 </summary>
		public virtual string Encryption { get; set; }

		/// <summary> 복제 설정 </summary>
		public virtual string Replication { get; set; }

		/// <summary> 로깅 설정 </summary>
		public virtual string Logging { get; set; }

		/// <summary> 알림 설정 </summary>
		public virtual string Notification { get; set; }

		/// <summary> 정책 설정 </summary>
		public virtual string Policy { get; set; }

		/// <summary> 잠금 설정 </summary>
		public virtual string ObjectLock { get; set; }

		/// <summary> 생성 일자 </summary>
		public virtual DateTime CreateTime { get; set; }

		public virtual string MfaDelete { get; set; }

		public bool TagIndexing { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		#endregion
	}

}
