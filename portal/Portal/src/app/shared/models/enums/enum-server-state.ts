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
export enum EnumServerState {
	Unknown = -2,
	Timeout = -1,
	Offline = 0,
	Online = 1,
}

export namespace EnumServerState {
	export function toDisplayName(value: EnumServerState): string {
		switch (value) {
			case EnumServerState.Unknown:
				return 'UL_SERVERS_SERVER_STATE_UNKNOWN';
			case EnumServerState.Timeout:
				return 'UL_SERVERS_SERVER_STATE_TIMEOUT';
			case EnumServerState.Offline:
				return 'UL_SERVERS_SERVER_STATE_OFFLINE';
			case EnumServerState.Online:
				return 'UL_SERVERS_SERVER_STATE_ONLINE';
			default:
				return value;
		}
	}
	export function toDisplayDescription(value: EnumServerState): string {
		switch (value) {
			case EnumServerState.Unknown:
				return 'UL_SERVERS_SERVER_STATE_UNKNOWN';
			case EnumServerState.Timeout:
				return 'UL_SERVERS_SERVER_STATE_TIMEOUT';
			case EnumServerState.Offline:
				return 'UL_SERVERS_SERVER_STATE_OFFLINE';
			case EnumServerState.Online:
				return 'UL_SERVERS_SERVER_STATE_ONLINE';
			default:
				return value;
		}
	}
	export function toDisplayGroupName(_: EnumServerState): string {
		return 'UL_SERVERS_SERVER_STATE';
	}
	export function toDisplayShortName(value: EnumServerState): string {
		switch (value) {
			case EnumServerState.Unknown:
				return 'UL_SERVERS_SERVER_STATE_UNKNOWN';
			case EnumServerState.Timeout:
				return 'UL_SERVERS_SERVER_STATE_TIMEOUT';
			case EnumServerState.Offline:
				return 'UL_SERVERS_SERVER_STATE_OFFLINE';
			case EnumServerState.Online:
				return 'UL_SERVERS_SERVER_STATE_ONLINE';
			default:
				return value;
		}
	}
	export function toDisplayPrompt(value: EnumServerState): string {
		switch (value) {
			case EnumServerState.Unknown:
				return 'UL_SERVERS_SERVER_STATE_UNKNOWN';
			case EnumServerState.Timeout:
				return 'UL_SERVERS_SERVER_STATE_TIMEOUT';
			case EnumServerState.Offline:
				return 'UL_SERVERS_SERVER_STATE_OFFLINE';
			case EnumServerState.Online:
				return 'UL_SERVERS_SERVER_STATE_ONLINE';
			default:
				return value;
		}
	}
}
