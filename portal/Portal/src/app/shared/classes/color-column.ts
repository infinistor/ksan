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
/**
 * 그리드 결과
 */
export interface GridResult {
		data: any[];
		total: number;
}
type RowsData = Array<any> | GridResult;

const DEFAULT_MASTER_COLOR_PROP = 'Colors';
const DEFAULT_COLOR_PROP = 'Color';
const DEFAULT_DETAIL_PROP = 'Items';

export class ColorColumn {
	constructor(
			public colorObjProp = DEFAULT_MASTER_COLOR_PROP,
			public colorProp = DEFAULT_COLOR_PROP,
			public itemsProp = DEFAULT_DETAIL_PROP
	) {

	}

	static create(): ColorColumn {
			return new ColorColumn();
	}

	hasColor(rowsData: RowsData): boolean {
			const rows = Array.isArray(rowsData) ? rowsData : rowsData.data;
			if (!rows || rows.length === 0) {
					return false;
			}
			return rows[0].hasOwnProperty(this.colorObjProp);
	}

	hasItemColor(rowsData: RowsData): boolean {
			const rows = Array.isArray(rowsData) ? rowsData : rowsData.data;
			if (!rows || rows.length === 0 || !rows[0].hasOwnProperty(this.itemsProp)) {
					return false;
			}
			const details = rows[0][this.itemsProp];
			if (!details || !Array.isArray(details) || details.length === 0) {
					return false;
			}

			return details[0].hasOwnProperty(this.colorObjProp);
	}
}

