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

function polarToCartesian(centerX, centerY, radius, angleInDegrees) {
	const angleInRadians = ((angleInDegrees - 90) * Math.PI) / 180.0;

	return {
		x: centerX + radius * Math.cos(angleInRadians),
		y: centerY + radius * Math.sin(angleInRadians),
	};
}
/**
 * 입력받은 값을 바탕으로 그래프를 그린다.
 * @param {int} x x좌표
 * @param {int} y y좌표
 * @param {int} radius 각도
 * @param {int} percent 크기
 * @returns 
 */
export function drawCurve(x, y, radius, percent) {
	let endAngle = (percent / 100) * 360;
	if (endAngle == 360) endAngle -= 1e-5;
	const start = polarToCartesian(x, y, radius, endAngle);
	const end = polarToCartesian(x, y, radius, 0);

	const largeArcFlag = endAngle <= 180 ? "0" : "1";

	return ["M", start.x, start.y, "A", radius, radius, 0, largeArcFlag, 0, end.x, end.y].join(" ");
}
