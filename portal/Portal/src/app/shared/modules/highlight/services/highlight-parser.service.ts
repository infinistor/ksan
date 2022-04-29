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
import { HighlightColor } from '../models';

export class HighlightParserService {
	private readonly OBJECT_COLOR_PROPERTY = 'Color';

	public parse(field: string, colors: any): HighlightColor {
		if (!colors || !field) {
			return null;
		}

		const propertyColor: string | any[] = colors[field];

		const hasPropertyColorAtLeastOne = Object.keys(colors)
			.filter(prop => prop !== this.OBJECT_COLOR_PROPERTY)
			.some(prop => {
				const color = colors[prop];
				return color && color.length > 0;
			});

		if (hasPropertyColorAtLeastOne && (!propertyColor || propertyColor.length === 0)) {
			return null;
		}

		if (!hasPropertyColorAtLeastOne && !colors[this.OBJECT_COLOR_PROPERTY]) {
			return null;
		}

		if (!hasPropertyColorAtLeastOne) {
			return this.parseObjectColor(colors);
		}

		return typeof propertyColor === 'object' ? this.parseValueColors(propertyColor) : this.parseFieldColor(propertyColor);
	}

	private parseValueColors(propertyColors: string | any[]): HighlightColor {
		if (!(propertyColors && propertyColors.length > 0) || typeof propertyColors !== 'object') {
			return null;
		}

		const highlights: Map<string, string> = new Map();
		propertyColors.map(c => highlights.set(c.Name, c.Value));
		return new HighlightColor({ colorType: 'value', colors: highlights });
	}

	private parseFieldColor(color: string | any[]): HighlightColor {
		if (!(color && color.length > 0) || typeof color !== 'string') {
			return null;
		}
		return new HighlightColor({ colorType: 'field', color });
	}

	private parseObjectColor(colors: any): HighlightColor {
		const color: string = colors[this.OBJECT_COLOR_PROPERTY];

		if (!color) {
			return null;
		}

		return color ? new HighlightColor({ colorType: 'object', color }) : null;
	}
}
