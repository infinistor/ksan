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
 * 전역 static 확장 메서드
 */
interface ObjectConstructor {
	removeProperties(target: any, source: any): any;
}

/**
 * source 객체에 존재하지 않는 속성을 target 객체에서 삭제한다.
 * @param target 속성을 삭제할 객체
 * @param source 비교할 원본 객체
 */
// tslint:disable-next-line:only-arrow-functions
Object.removeProperties = function(target: any, source: any): any {

	if (target) {

		// 원본 객체의 모든 속성에 대해서 처리
		Object.keys(target).forEach((key) => {
			// 원본 객체
			if (!(key in source))
				delete target[key];
		});
	}
	return target;
};
