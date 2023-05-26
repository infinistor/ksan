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
 * 입력된 byte를 보기쉬운 용량 단위로 변환하여 반환한다.
 * @param {long} bytes 변환할 용량
 * @param {String} type 타입
 * @returns 용량 + 단위
 */
export function sizeToHtml(bytes, type) {
	const thresh = 1000;
	const dp = 1;

	if (Math.abs(bytes) < thresh) {
		if (type == "Write") return `<span class='io_in'>${bytes.toFixed(dp)}</span><span class='io_in_unit'>B/s</span>`;
		else return `<span class='io_out'>${bytes.toFixed(dp)}</span><span class='io_out_unit'>B/s</span>`;
	}

	const units = ["KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"];
	let u = -1;
	const r = 10 ** dp;

	do {
		bytes /= thresh;
		++u;
	} while (Math.round(Math.abs(bytes) * r) / r >= thresh && u < units.length - 1);

	if (type == "Write") return `<span class='io_in'>${bytes.toFixed(dp)}</span><span class='io_in_unit'>${units[u]}/s</span>`;
	else return `<span class='io_out'>${bytes.toFixed(dp)}</span><span class='io_out_unit'>${units[u]}/s</span>`;
}
