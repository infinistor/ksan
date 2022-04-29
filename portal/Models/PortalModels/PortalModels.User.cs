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
	/// <summary> 사용자 정보 </summary>
	public partial class User
	{

		public User()
		{
			this.IsDeleted = false;
			this.RegAllowedConnectionIps = new List<AllowedConnectionIp>();
			this.UserRoles = new List<UserRole>();
			this.UserActionLogs = new List<UserActionLog>();
			this.UserClaims = new List<UserClaim>();
			this.UserLoginHistories = new List<UserLoginHistory>();
			this.RegServiceGroups = new List<ServiceGroup>();
			this.ModServiceGroups = new List<ServiceGroup>();
			this.RegServices = new List<Service>();
			this.ModServices = new List<Service>();
			this.RegNetworkInterfaceVlans = new List<NetworkInterfaceVlan>();
			this.ModNetworkInterfaceVlans = new List<NetworkInterfaceVlan>();
			this.ModNetworkInterfaces = new List<NetworkInterface>();
			this.ModServers = new List<Server>();
			this.RegNetworkInterfaces = new List<NetworkInterface>();
			OnCreated();
		}

		/// <summary> 사용자 아이디 </summary>
		public virtual Guid Id { get; set; }

		public virtual int AccessFailedCount { get; set; }

		/// <summary> 사번 등 식별 코드 </summary>
		public virtual string Code { get; set; }

		public virtual string ConcurrencyStamp { get; set; }

		/// <summary> 이메일 </summary>
		public virtual string Email { get; set; }

		/// <summary> 이메일 승인여부 </summary>
		public virtual bool EmailConfirmed { get; set; }

		/// <summary> 잠금 활성화 여부 </summary>
		public virtual bool LockoutEnabled { get; set; }

		/// <summary> 잠금 만료 시간 </summary>
		public virtual DateTime? LockoutEnd { get; set; }

		/// <summary> 사용자명 </summary>
		public virtual string Name { get; set; }

		/// <summary> 표준화 된 이메일 </summary>
		public virtual string NormalizedEmail { get; set; }

		/// <summary> 표준화 된 로그인 아이디 </summary>
		public virtual string NormalizedUserName { get; set; }

		/// <summary> 비밀번호 해쉬 </summary>
		public virtual string PasswordHash { get; set; }

		/// <summary> 전화번호 </summary>
		public virtual string PhoneNumber { get; set; }

		/// <summary> 전화번호 확인 여부 </summary>
		public virtual bool PhoneNumberConfirmed { get; set; }

		/// <summary> 보안 지문 </summary>
		public virtual string SecurityStamp { get; set; }

		/// <summary> 2차 인증 활성화 여부 </summary>
		public virtual bool TwoFactorEnabled { get; set; }

		/// <summary> 로그인 아이디 </summary>
		public virtual string LoginId { get; set; }

		/// <summary> 삭제 여부 </summary>
		public virtual bool IsDeleted { get; set; }

		/// <summary> 비밀번호 변경 일시 </summary>
		public virtual DateTime? PasswordChangeDate { get; set; }

		/// <summary> 등록한 접속 아이피 목록 </summary>
		public virtual IList<AllowedConnectionIp> RegAllowedConnectionIps { get; set; }

		/// <summary> 사용자 역할 목록 </summary>
		public virtual IList<UserRole> UserRoles { get; set; }

		/// <summary> 등록한 사용자 행동 로그 목록 </summary>
		public virtual IList<UserActionLog> UserActionLogs { get; set; }

		/// <summary> 사용자 권한 목록 </summary>
		public virtual IList<UserClaim> UserClaims { get; set; }

		/// <summary> 사용자 로그인 내역 </summary>
		public virtual IList<UserLoginHistory> UserLoginHistories { get; set; }

		/// <summary> 등록 서비스 그룹 목록 </summary>
		public virtual IList<ServiceGroup> RegServiceGroups { get; set; }

		/// <summary> 수정 서비스 그룹 목록 </summary>
		public virtual IList<ServiceGroup> ModServiceGroups { get; set; }

		/// <summary> 등록 서비스 목록 </summary>
		public virtual IList<Service> RegServices { get; set; }

		/// <summary> 수정 서비스 목록 </summary>
		public virtual IList<Service> ModServices { get; set; }

		/// <summary> 등록 네트워크 인터페이스 VLAN 목록 </summary>
		public virtual IList<NetworkInterfaceVlan> RegNetworkInterfaceVlans { get; set; }

		/// <summary> 수정 네트워크 인터페이스 VLAN 목록 </summary>
		public virtual IList<NetworkInterfaceVlan> ModNetworkInterfaceVlans { get; set; }

		/// <summary> 수정 네트워크 인터페이스 목록 </summary>
		public virtual IList<NetworkInterface> ModNetworkInterfaces { get; set; }

		/// <summary> 수정 서버 목록 </summary>
		public virtual IList<Server> ModServers { get; set; }

		/// <summary> 등록 네트워크 인터페이스 목록 </summary>
		public virtual IList<NetworkInterface> RegNetworkInterfaces { get; set; }

		#region Extensibility Method Definitions

		partial void OnCreated();

		#endregion
	}

}
