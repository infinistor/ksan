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
import {List} from 'linq-collections';

/**
 * enum 으로 부터 가져온 아이템 타입
 */
export type EnumItem = {text: string, name: string, value: string, enable: boolean, isSelectItem: boolean, isAllItem: boolean };

/**
 *
 * @param enumData
 * @param filter
 * @param hiddenValues
 * @param disabledValues
 */
export function enumerate<T = any>(enumData: any, filter: string[] = null, hiddenValues: string[] = null, disabledValues: string[] = null): EnumItem[] {
	let result: EnumItem[] = [];

	// 데이터의 모든 속성에 대해서 처리
	for (const value in enumData) {

		// 소유하고 있는 프로퍼티인 경우
		if (enumData.hasOwnProperty(value)) {

			// 값 저장
			const enumValue = enumData[value];

			// 값이 문자열인 경우 (이름인 경우)
			if (typeof enumValue === 'string') {

				if (filter && !this.filter(enumValue)) {
					continue;
				}

				// 감춤 목록에 존재하는 경우, 스킵
				if (hiddenValues && hiddenValues.findIndex((hiddenValue) => {
					return enumValue === hiddenValue;
				}) >= 0)
					continue;

				let enable = true;

				// 비활성화 목록에 존재하는 경우, 비활성화
				if (disabledValues && disabledValues.findIndex((disabledValue) => enumValue === disabledValue) >= 0) {
					enable = false;
				}

				if (enumData.hasOwnProperty('toDisplayShortName')) {
					result.push({text: enumData.toDisplayShortName(enumValue), name: enumValue, value: enumData[enumValue], enable, isSelectItem: false, isAllItem: false });
				} else {
					result.push({text: enumData[enumValue], name: enumValue, value: enumData[enumValue], enable, isSelectItem: false, isAllItem: false });
				}
			}
		}
	}

	result = new List<EnumItem>(result).orderBy(i => i.value).toArray();

	return result;
}
