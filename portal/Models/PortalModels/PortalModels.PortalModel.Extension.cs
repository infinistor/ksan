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
using Microsoft.EntityFrameworkCore;
using MTLib.Core;

namespace PortalModels
{

	/// <summary> CSSPLogModel 데이터베이스 클래스 </summary>
	public partial class PortalModel
	{
		/// <summary> 생성 시 호출되는 함수 </summary>
		partial void OnCreated()
		{
		}

		/// <summary> 마이그레이션 수행 </summary>
		public void Migrate()
		{
			try
			{
				// 테이블 생성이 되지 않은 경우, 마이그레이션 수행
				if (!Database.EnsureCreated()) Database.Migrate();

				// Database.Migrate();
			}
			catch (Exception ex)
			{
				NNException.Log(ex);
			}
		}

		#region 테이블 매핑 사용자 정의

		/// <summary> CLAIM_NAMES 테이블에 대한 매핑 사용자 정의 </summary>
		/// <param name="modelBuilder">모델 빌더 객체</param>
		partial void CustomizeClaimNameMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<ClaimName>()
				.HasIndex(i => new { i.ClaimType, i.ClaimValue }).IsUnique();
		}

		/// <summary> USER_ACTION_LOGS 테이블에 대한 매핑 사용자 정의 </summary>
		/// <param name="modelBuilder">모델 빌더 객체</param>
		partial void CustomizeUserActionLogMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<UserActionLog>()
				.HasIndex(i => new { i.LogLevel, i.RegDate, i.UserId }).IsUnique(false);
		}

		/// <summary> SYSTEM_LOGS 테이블에 대한 매핑 사용자 정의 </summary>
		/// <param name="modelBuilder">모델 빌더 객체</param>
		partial void CustomizeSystemLogMapping(ModelBuilder modelBuilder)
		{
			modelBuilder.Entity<SystemLog>()
				.HasIndex(i => new { i.LogId, i.RegDate }).IsUnique(false);
		}
		#endregion
	}
}
