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

namespace PortalModels
{
    /// <summary>역할(권한그룹) 내 사용자</summary>
    public partial class UserRole {

        public UserRole()
        {
            OnCreated();
        }

        /// <summary>사용자 아이디</summary>
        public virtual Guid UserId { get; set; }

        /// <summary>역할(권한그룹) 아이디</summary>
        public virtual Guid RoleId { get; set; }

        /// <summary>역할 정보</summary>
        public virtual Role Role { get; set; }

        /// <summary>사용자 정보</summary>
        public virtual User User { get; set; }

        #region Extensibility Method Definitions

        partial void OnCreated();

        public override bool Equals(object obj)
        {
          UserRole toCompare = obj as UserRole;
          if (toCompare == null)
          {
            return false;
          }

          if (!Object.Equals(this.UserId, toCompare.UserId))
            return false;
          if (!Object.Equals(this.RoleId, toCompare.RoleId))
            return false;

          return true;
        }

        public override int GetHashCode()
        {
          int hashCode = 13;
          hashCode = (hashCode * 7) + UserId.GetHashCode();
          hashCode = (hashCode * 7) + RoleId.GetHashCode();
          return hashCode;
        }

        #endregion
    }

}
