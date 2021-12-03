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
import { ColorBlock } from '../models/color-block.model';
import { CellColor } from '../models/cell-color.model';
import { Injectable } from '@angular/core';

// * 프로퍼티별 색상은 표현하지 않고 Colors.Color 값만 표현하기로 하여 주석처리 *
// const isDefaultColorProp = (prop: string, colorProp: string) => prop === colorProp;
// const hasSingleValue = v => typeof v === 'string' && !!v;
// const hasMultipleValue = (v: Partial<{ Value: string }[]>) => Array.isArray(v) && v.length > 0 && v.some(nv => !!nv.Value);
// const hasColorValue = v => hasSingleValue(v) || hasMultipleValue(v);

@Injectable({ providedIn: 'root' })
export class CellColorFactory {
	create(colors: any, colorProp: string, ignoreDetail = false): CellColor {
		if (!this.hasDefaultColor(colors, colorProp)) {
			return this.emptyColor();
		}

		// if (this.useIgnoreDetail(colors, colorProp, ignoreDetail)) {
		//     return this.emptyColor();
		// }

		return this.fromDefaultColor(colors, colorProp);
	}

	createFirst(itemColors: any[] = [], colorProp: string): CellColor {
		let returnCellColor = this.emptyColor();
		itemColors.some(c => {
			const cellColor = this.create(c, colorProp);
			if (cellColor && cellColor.colorItems.length > 0) {
				returnCellColor = cellColor;
				return true;
			}
			return false;
		});
		return returnCellColor;
	}

	// private useIgnoreDetail(colors: any, colorProp: string, ignoreDetail: boolean) {
	//     if (!ignoreDetail) {
	//         return false;
	//     }
	//     return Object.entries(colors)
	//         .filter(([prop, _]) => !isDefaultColorProp(prop, colorProp))
	//         .every(([_, v]) => !hasColorValue(v));
	// }

	private fromDefaultColor(colors: any, colorProp: string): CellColor {
		const color = ColorBlock.create(colors[colorProp], CellColor.OUTPUT_DEFAULT_COLOR_PROP);
		return new CellColor(color);
	}

	// private fromPropsColor(colors: any, defaultProp: string) {
	//     const toColorItem = ([prop, value]) => ColorBlock.create(value, prop);
	//     const colorItems: ColorBlock[] = Object.entries(colors)
	//         .filter(([prop, v]) => !isDefaultColorProp(prop, defaultProp) && hasColorValue(v))
	//         .map(toColorItem)
	//         .reduce((acc, items) => {
	//             return [...acc, ...items];
	//         }, []);
	//     return new CellColor(colorItems);
	// }

	private emptyColor(): CellColor {
		return new CellColor([]);
	}

	private hasDefaultColor(colors: any, colorProp: string): boolean {
		return colors.hasOwnProperty(colorProp) && !!colors[colorProp];
	}

	// private hasPropsColor(colors: any, defaultProp: string): boolean {
	//     return Object.entries(colors)
	//         .filter(([prop]) => !isDefaultColorProp(prop, defaultProp))
	//         .some(([, v]: [string, any]) => hasColorValue(v));
	// }
}
