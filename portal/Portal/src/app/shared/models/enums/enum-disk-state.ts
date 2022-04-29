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

export enum EnumDiskState {
	Bad = -2,
	Disable = -1,
	Stop = 0,
	Weak = 1,
	Good = 2,
}

export namespace EnumDiskState {
	export function toDisplayName(value: EnumDiskState): string {
		switch (value) {
			case EnumDiskState.Bad:
				return 'UL_DISKS_DISK_STATE_BAD';
			case EnumDiskState.Disable:
				return 'UL_DISKS_DISK_STATE_DISABLE';
			case EnumDiskState.Stop:
				return 'UL_DISKS_DISK_STATE_STOP';
			case EnumDiskState.Weak:
				return 'UL_DISKS_DISK_STATE_WEAK';
			case EnumDiskState.Good:
				return 'UL_DISKS_DISK_STATE_GOOD';
			default:
				return value;
		}
	}
	export function toDisplayDescription(value: EnumDiskState): string {
		switch (value) {
			case EnumDiskState.Bad:
				return 'UL_DISKS_DISK_STATE_BAD';
			case EnumDiskState.Disable:
				return 'UL_DISKS_DISK_STATE_DISABLE';
			case EnumDiskState.Stop:
				return 'UL_DISKS_DISK_STATE_STOP';
			case EnumDiskState.Weak:
				return 'UL_DISKS_DISK_STATE_WEAK';
			case EnumDiskState.Good:
				return 'UL_DISKS_DISK_STATE_GOOD';
			default:
				return value;
		}
	}
	export function toDisplayGroupName(_: EnumDiskState): string {
		return 'UL_DISKS_DISK_STATE';
	}
	export function toDisplayShortName(value: EnumDiskState): string {
		switch (value) {
			case EnumDiskState.Bad:
				return 'UL_DISKS_DISK_STATE_BAD';
			case EnumDiskState.Disable:
				return 'UL_DISKS_DISK_STATE_DISABLE';
			case EnumDiskState.Stop:
				return 'UL_DISKS_DISK_STATE_STOP';
			case EnumDiskState.Weak:
				return 'UL_DISKS_DISK_STATE_WEAK';
			case EnumDiskState.Good:
				return 'UL_DISKS_DISK_STATE_GOOD';
			default:
				return value;
		}
	}
	export function toDisplayPrompt(value: EnumDiskState): string {
		switch (value) {
			case EnumDiskState.Bad:
				return 'UL_DISKS_DISK_STATE_BAD';
			case EnumDiskState.Disable:
				return 'UL_DISKS_DISK_STATE_DISABLE';
			case EnumDiskState.Stop:
				return 'UL_DISKS_DISK_STATE_STOP';
			case EnumDiskState.Weak:
				return 'UL_DISKS_DISK_STATE_WEAK';
			case EnumDiskState.Good:
				return 'UL_DISKS_DISK_STATE_GOOD';
			default:
				return value;
		}
	}
}
