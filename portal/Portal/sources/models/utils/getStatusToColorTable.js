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

/**
 * 서버나 서비스의 상태 정보를 반환한다.
 * @param {any} Status 상태정보
 * @returns 색을 입힌 상태 정보
 */
export function getStatusToColor(Status) {
	var color = "#E63031";
	switch (Status) {
		case "Online":
			color = "#2CCD70";
			break;
		case "Offline":
			color = "#5F5F5F";
			break;
		case "Timeout":
		default:
			color = "#E63031";
	}
	return `<span style="color:${color};"> ${Status}</span>`;
}