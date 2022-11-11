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
import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as moment from 'moment';

// noinspection JSUnusedGlobalSymbols
@Pipe({
	name: 'dynamicPipe',
	pure: false
})
export class DynamicPipe implements PipeTransform {


	/**
	 * 생성자
	 * @param translate
	 */
	constructor(
		translate: TranslateService,
		// sanitized: DomSanitizer
	) {
		this.translate = translate;
	}

	// 색상 프로퍼티명
	static COLOR_PROPERTY_NAME: string = 'Colors';

	// 번역 서비스
	translate: TranslateService;

	/**
	 * transform 구현체
	 * @param item 데이터 객체
	 * @param modifier 함수명
	 * @param param 파라미터
	 * @param field 값을 가져올 필드명
	 * @param useHighlight 하이라이트를 사용할지 여부
	 */
	transform(item: any, modifier: string, param: any, field?: string, useHighlight?: boolean): any
	{
		// 값이 없을 경우
		if (item == null)
			return '';

		if (typeof useHighlight === 'undefined') {
			useHighlight = true;
		}

		const isHighlight = field && item[DynamicPipe.COLOR_PROPERTY_NAME] && useHighlight;

		const value: any = field ? this.getValue(item, field) : item;

		let result: any;

		// 값이 정확하지 않으면 빈값으로 리턴
		if ((value == null || value === '' || value === '-' || (value === '-' && param === '-')) && modifier !== 'getHaStatus')
			return '';

		// enum 값과 modifier 가 모두 있는 경우
		if (modifier && param) {
			result = this[modifier](value, param);
		}
		// modifier 만 있는 경우
		else if (modifier) {
			result = this[modifier](value);
		}
		// 모두 없는 경우
		else {
			result = value;
		}

		// 하이라이트 설정인 경우
		if (isHighlight)
			result = this.setHighlight(item[DynamicPipe.COLOR_PROPERTY_NAME], field, result);

		return result;
	}

	/**
	 * 아이템에서 해당 컬럼의 값을 추출
	 * @param item 데이터 객체
	 * @param field 필드명
	 */
	getValue(item: any, field: string): any {
		const fields: string[] = field.split('.');
		let value: any = item[fields[0]];

		for (let i = 1; i < fields.length; i++) {
			value = value[fields[i]];
		}

		return value;
	}

	/**
	 * html 형식을 반환한다
	 * @param value 값
	 */
	// trustHtml(value:string): SafeHtml {
	//     return this.sanitized.bypassSecurityTrustHtml(value);
	// }

	/**
	 * 사용자 템플릿을 html 형식으로 반환한다
	 * @param value 값
	 * @param template 템플릿 내용
	 */
	customTemplate(value: string, template: string): string {
		// return this.trustHtml(template.replace('{0}', value));
		return template.replace('{0}', value);
	}

	/**
	 * 주어진 속성들의 값을 여러 줄로 분리(<br>)하여 합친다.
	 * @param value 값
	 * @param targetProperties 합칠 속성명 목록
	 */
	mergeObject(value: any, targetProperties: string[]): string {
		let result = '';

		// 넘어온값이 배열인경우
		if (value instanceof Array) {
			value.forEach((item: any) => {
				// 받아온 오브젝트를 합친다
				for (const property of targetProperties) {
					// 배열이라면
					if (item[property] instanceof Array) {
						result = result + item[property].join('<br>');
					}
					// 오브젝트라면
					else {

						if (typeof item[property] === 'undefined')
							continue;

						if (item[property] !== '') {
							if (item[property].split(',').length > 1) {
								result = result + item[property].split(',').join('<br>') + '<br>';
							} else {
								result = result + item[property] + '<br>';
							}
						}
					}
				}

				if (result.indexOf('undefined') > -1)
					result = '';
			});

		} else {

			// 받아온 오브젝트를 합친다
			for (const property of targetProperties) {
				// 배열이라면
				if (value[property] instanceof Array) {
					result = result + value[property].join('<br>');
				}
				// 오브젝트라면
				else {

					if (typeof (value[property]) !== 'undefined' )
						result = result + value[property].split(',').join('<br>');
				}
			}

			if (result.indexOf('undefined') > -1)
				result = '';
		}

		return result;
	}

	/**
	 * 하이라이트를 설정한다
	 * @param item 데이터 객체
	 * @param field 속성명
	 * @param value 값
	 */
	setHighlight(item: any, field: string, value: string): string {
		const COLOR_PROPERTY_NAME: string = 'Color';
		let result: string = '';
		let hasValue: boolean = false;

		try {

			// color 가 설정된 필드가 있는지 확인
			Object.keys(item).forEach((val) => {
				if ((typeof (item[val]) !== 'object' && item[val] !== null && item[val] !== undefined && item[val] !== '' && val !== COLOR_PROPERTY_NAME) || (typeof (item[val]) === 'object' && item[val].length > 0))
					hasValue = true;
			});

			// 현재 필드에 값이 있을 경우
			if (hasValue && item[field] !== null && item[field] !== undefined && item[field] !== '') {
				// 필드 값이 배열인 경우
				if (typeof (item[field]) === 'object' && item[field].length >= 0) {
					// 현재 값을 split 한다
					const values: string[] = value.split(',');

					// 배열 값에 해당하는 부분을 찾아서 해당 부분만 색을 넣는다
					item[field].forEach((color: any) => {
						const index = values.findIndex(i => i.trim() === color.Name);
						if (index >= 0)
							values[index] = '<span style="color:' + color.Value + '">' + color.Name + '</span>';
					});

					result = values.join(',');
				}
				// 일반 값일 경우
				else
					result = '<span style="color:' + item[field] + '">' + value + '</span>';
			}
			// 모든 값에 color 가 설정되어 있지 않고, color 필드에 값이 있을 경우
			else if (!hasValue && item[COLOR_PROPERTY_NAME] !== '')
				result = '<span style="color:' + item[COLOR_PROPERTY_NAME] + '">' + value + '</span>';
			else
				result = value;
		}
		catch (e) { }

		// return this.trustHtml(result);
		return result;
	}

	/**
	 * 날짜를 포메팅 한다
	 * @param value 값
	 * @param format 형식 문자열 (moment 형식을 따른다.)
	 */
	formatDate(value: any, format: string): string {
		if (!value)
			return '';

		return moment(value).format(format);
	}

	/**
	 * object[targetPropertyName] => 1,2,3,4,....
	 * @param value 값
	 * @param targetPropertyName 대상 필드명
	 */
	toCommaWithValue(value: Array<any>, targetPropertyName: string): string {
		if (typeof value === 'undefined')
			return '';
		if (value.length === 0)
			return '';

		return value.map(x => x[targetPropertyName]).join(',');
	}

	/**
	 * object["1","2","3"] => 1,2,3,4,....
	 * @param value 값 목록
	 */
	toCommaWithStringValue(value: Array<any>): string {
		if (typeof value === 'undefined')
			return '';
		if (value.length === 0)
			return '';

		return value.join(',');
	}


	/**
	 * 객체 배열의 Name 값을 ','로 이어 문자열로 만든다.
	 * @param value 값 목록
	 */
	toCommaWithName(value: Array<any>): string {
		if (typeof value === 'undefined')
			return '';
		if (value.length === 0)
			return '';

		return value.map(i => i.Name).join(',');
	}

	/**
	 * enum 타입과 맞는 DisplayName 을 출력한다
	 * @param value 값
	 * @param enumClass enum 클래스
	 */
	enumTranslate(value: any, enumClass: any): string {

		// 값과 enum 클래스 객체가 유효한 경우
		if (value && enumClass) {
			let translated: string = '';
			if (enumClass.hasOwnProperty('toDisplayShortName')) {
				this.translate.get(enumClass.toDisplayShortName(enumClass[value])).subscribe(res => {
					translated = res;
				});
			} else {
				translated = enumClass[value];
			}
			return translated;
		}
		// 값이나 enum 클래스 객체가 유효하지 않은 경우
		else {
			if (value)
				return value;
			else
				return '';
		}
	}

	/**
	 * 클릭이 가능한 형태로 만든다.
	 * @param value 값
	 */
	enableClick(value: any): string {
		if (!value)
			return '';

		return `<div class='grid-cell-click'>${value}</div>`;
	}

	/**
	 * 천단위 표기를 하고 클릭이 가능한 형태로 만든다.
	 * @param value 값
	 */
	enableClickWithCommaSeparates(value: any): string {
		let result = '';

		if (!value)
			return '';

		const valueArray = value.split(',');
		for (const item of valueArray) {
			result += `<div class='grid-cell-click'>${item}</div>`;
		}

		return result;
	}

	/**
	 * Enum 타입과 맞는 DisplayName 을 출력하고 클릭이 가능한 형태로 만든다.
	 * @param value 값
	 * @param enumClass enum 클래스
	 */
	enableClickWithEnumTranslate(value: any, enumClass: any): string {
		if (typeof value === 'undefined')
			return '';

		if (value === 99)
			return '';

		if (value === null)
			return '';

		let translated: string = '';
		if (enumClass.hasOwnProperty('toDisplayShortName')) {
			this.translate.get(enumClass.toDisplayShortName(value)).subscribe(res => {
				translated = res;
			});
		} else {
			translated = enumClass[value];
		}
		return `<div class='grid-cell-click'>` + translated + '</div>';
	}

	/**
	 * Enum 타입과 맞는 DisplayName 을 ','로 구분하여 멀티 라인으로 출력한다
	 * @param value 값
	 * @param enumClass enum 클래스
	 */
	enumTranslateToMultiline(value: any, enumClass: any): string {
		if (typeof value === 'undefined')
			return '';

		let translated = '';
		let translatedWords = '';
		const split = value.split(',');

		if (value === '')
			return '';

		if (split.length === 0)
			return '';

		for (let i = 0; i < split.length; i++) {
			this.translate.get(enumClass.toDisplayShortName(split[i])).subscribe(res => {
				translated = res;
			});

			translatedWords = (!(i === (split.length - 1))) ? translatedWords + translated : translatedWords + ',' + translated;
		}

		return translatedWords.toMultiLine({ separators: [','], newLine: '<br>'});
	}

	/**
	 * 주어진 값 목록들을 enum 타입과 맞는 DisplayName 으로 변환 후, 콤마로 문자열 연결하여 출력한다
	 * @param values 값 목록
	 * @param enumClass enum 클래스
	 */
	getEnumTranslateFromArray(values: any[], enumClass: any): string {
		if (!values)
			return '';

		if (values.length === 0)
			return '';

		const translated: Array<string> = new Array<string>();

		for (let i = 0; i < values.length; i++) {
			this.translate.get(enumClass.toDisplayShortName(values[i])).subscribe(res => {
				translated.push(res);
			});
		}

		return translated.join(',');
	}

	/**
	 * length 속성을 가지는 객체의 length 값을 출력한다.
	 * @param value length 속성을 가지는 객체
	 */
	length(value: any): string {
		if (!value)
			return '0';

		if (!value.hasOwnProperty('length'))
			return '0';

		return value.length.toString();
	}

	/**
	 * 천단위 콤마
	 * @param num
	 */
	numberWithCommas(num: number): string {
		const parts: string[] = num.toString().split('.');
		parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
		return parts.join('.');
	}

	/**
	 * 크기 출력
	 * @param num 바이트 수
	 */
	sizeOnlyAuto(num: number): string {
		let value: number;

		const ebNum = num / 1024 / 1024 / 1024 / 1024 / 1024 / 1024;
		if (ebNum >= 1) {
			value = ebNum;
		} else {
			const pbNum = num / 1024 / 1024 / 1024 / 1024 / 1024;
			if (pbNum >= 1) {
				value = pbNum;
			} else {
				const tbNum = num / 1024 / 1024 / 1024 / 1024;
				if (tbNum >= 1) {
					value = tbNum;
				} else {
					const gbNum = num / 1024 / 1024 / 1024;
					if (gbNum >= 1) {
						value = gbNum;
					} else {
						const mbNum = num / 1024 / 1024;
						if (mbNum >= 1) {
							value = mbNum;
						} else {
							value = num / 1024;
						}
					}
				}
			}
		}

		const parts: string[] = value.toFixed(1).toString().split('.');
		parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
		return parts.join('.');
	}

	/**
	 * 단위 출력
	 * @param num 바이트 수
	 */
	unitOnlyAuto(num: number): string {
		let unit: string;

		const ebNum = num / 1024 / 1024 / 1024 / 1024 / 1024 / 1024;
		if (ebNum >= 1) {
			unit = 'EB';
		} else {
			const pbNum = num / 1024 / 1024 / 1024 / 1024 / 1024;
			if (pbNum >= 1) {
				unit = 'PB';
			} else {
				const tbNum = num / 1024 / 1024 / 1024 / 1024;
				if (tbNum >= 1) {
					unit = 'TB';
				} else {
					const gbNum = num / 1024 / 1024 / 1024;
					if (gbNum >= 1) {
						unit = 'GB';
					} else {
						const mbNum = num / 1024 / 1024;
						if (mbNum >= 1) {
							unit = 'MB';
						} else {
							unit = 'KB';
						}
					}
				}
			}
		}

		return unit;
	}

	/**
	 * 크기 출력
	 * @param num 바이트 수
	 */
	sizeBpsOnlyAuto(num: number): string {
		let value: number;

		const pbNum = num / 1024 / 1024 / 1024 / 1024 / 1024;
		if (pbNum >= 1) {
			value = pbNum;
		} else {
			const tbNum = num / 1024 / 1024 / 1024 / 1024;
			if (tbNum >= 1) {
				value = tbNum;
			} else {
				const gbNum = num / 1024 / 1024 / 1024;
				if (gbNum >= 1) {
					value = gbNum;
				} else {
					const mbNum = num / 1024 / 1024;
					if (mbNum >= 1) {
						value = mbNum;
					} else {
						value = num / 1024;
					}
				}
			}
		}

		const parts: string[] = value.toFixed(1).toString().split('.');
		parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
		return parts.join('.');
	}

	/**
	 * BPS 단위 출력
	 * @param num 바이트 수
	 */
	unitBpsOnlyAuto(num: number): string {
		let unit: string;

		const pbNum = num / 1024 / 1024 / 1024 / 1024 / 1024;
		if (pbNum >= 1) {
			unit = 'PB/s';
		} else {
			const tbNum = num / 1024 / 1024 / 1024 / 1024;
			if (tbNum >= 1) {
				unit = 'TB/s';
			} else {
				const gbNum = num / 1024 / 1024 / 1024;
				if (gbNum >= 1) {
					unit = 'GB/s';
				} else {
					const mbNum = num / 1024 / 1024;
					if (mbNum >= 1) {
						unit = 'MB/s';
					} else {
						unit = 'KB/s';
					}
				}
			}
		}

		return unit;
	}

	/**
	 * 크기 및 단위까지 출력
	 * @param num 바이트 수
	 */
	sizeAuto(num: number): string {
		let value: number;
		let unit: string;

		const ebNum = num / 1024 / 1024 / 1024 / 1024 / 1024 / 1024;
		if (ebNum >= 1) {
			value = ebNum;
			unit = 'EB';
		}
		else {
			const pbNum = num / 1024 / 1024 / 1024 / 1024 / 1024;
			if (pbNum >= 1) {
				value = pbNum;
				unit = 'PB';
			} else {
				const tbNum = num / 1024 / 1024 / 1024 / 1024;
				if (tbNum >= 1) {
					value = tbNum;
					unit = 'TB';
				} else {
					const gbNum = num / 1024 / 1024 / 1024;
					if (gbNum >= 1) {
						value = gbNum;
						unit = 'GB';
					} else {
						const mbNum = num / 1024 / 1024;
						if (mbNum >= 1) {
							value = mbNum;
							unit = 'MB';
						} else {
							value = num / 1024;
							unit = 'KB';
						}
					}
				}
			}
		}

		const parts: string[] = value.toFixed(1).toString().split('.');
		parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
		return parts.join('.') + ' ' + unit;
	}

	/**
	 * BPS 크기 및 단위까지 출력
	 * @param num 바이트 수
	 */
	bpsAuto(num: number): string {
		let value: number;
		let unit: string;

		const pbNum = num / 1024 / 1024 / 1024 / 1024 / 1024;
		if (pbNum >= 1) {
			value = pbNum;
			unit = 'PBps';
		} else {
			const tbNum = num / 1024 / 1024 / 1024 / 1024;
			if (tbNum >= 1) {
				value = tbNum;
				unit = 'TBps';
			} else {
				const gbNum = num / 1024 / 1024 / 1024;
				if (gbNum >= 1) {
					value = gbNum;
					unit = 'GBps';
				} else {
					const mbNum = num / 1024 / 1024;
					if (mbNum >= 1) {
						value = mbNum;
						unit = 'MBps';
					} else {
						value = num / 1024;
						unit = 'KBps';
					}
				}
			}
		}

		const parts: string[] = value.toFixed(1).toString().split('.');
		parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
		return parts.join('.') + ' ' + unit;
	}

	/**
	 * TB 단위 출력
	 * @param num
	 */
	sizeTB(num: number): string {
		const parts: string[] = (num / 1024 / 1024 / 1024 / 1024).toFixed(1).toString().split('.');
		parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
		return parts.join('.');
	}

	/**
	 * GB 단위 출력
	 * @param num
	 */
	sizeGB(num: number): string {
		const parts: string[] = (num / 1024 / 1024 / 1024).toFixed(1).toString().split('.');
		parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
		return parts.join('.');
	}

	/**
	 * MB 단위 출력
	 * @param num
	 */
	sizeMB(num: number): string {
		const parts: string[] = (num / 1024 / 1024).toFixed(1).toString().split('.');
		parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
		return parts.join('.');
	}

	/**
	 * 퍼센트
	 * @param num
	 */
	percent(num: number): string {
		return num.toFixed(1).toString() + ' %';
	}

	/**
	 * ON/OFF
	 * @param value 값 값
	 */
	onOff(value: boolean): string {
		return value ? 'ON' : 'OFF';
	}

	/**
	 * YES/NO
	 * @param value 값 값
	 */
	yesNo(value: boolean): string {
		return value ? 'YES' : 'NO';
	}

	/**
	 * VLAN ID 출력
	 * @param vlanId VLAN ID 번호
	 */
	vlanId(vlanId: number): string {
		if (vlanId <= 1)
			return '-';
		else
			return this.numberWithCommas(vlanId);
	}

	/**
	 * Interface 를 여러 행으로 출력
	 * @param values 값 문자열 배열
	 */
	toInterfaceMultiLine(values: Array<any>): string {
		let result: string = '';

		if (values.isEmpty())
			return '';

		for (let i = 0; i < values.length; i++) {
			let value = values[i];
			const splitValue = value.split(',');

			if (splitValue && splitValue.length > 1) {
				if (splitValue[1] === '0')
					value = splitValue[0];
				else
					value = splitValue[0] + ',' + splitValue[1];
			}

			result += value;
			if (i !== (values.length - 1))
				result += '<br>';
		}

		return result;
	}

	/**
	 * 문자를 그리드안에서 개행시킨다
	 * @param value 값
	 * @param options 개행 옵션 객체
	 */
	multiline(value: string, options: MultilineOptions): string {
		if (value.isEmpty())
			return '';

		return value.toMultiLine(options);
	}

	/**
	 * 문자를 그리드안에서 개행시킨다
	 * @param value 값 목록
	 * @param newLine 개행 문자로 사용할 문자열
	 */
	multiLineArray(value: Array<any>, newLine: string): string {
		let result: string = '';

		if (value.isEmpty())
			return '';

		for (let i = 0; i < value.length; i++) {
			result += value[i];
			if (i !== (value.length - 1))
				result += newLine;
		}

		return result;
	}

	/**
	 * object[targetPropertyName] => 1</br>2</br>
	 * @param value 값
	 * @param targetPropertyName 대상 필드명
	 * @param newLine 개행 문자로 사용할 문자열
	 */
	multiLineWithPropertyValue(value: Array<any>, targetPropertyName: string, newLine: string): string{
		let result: string = '';

		if (typeof value === 'undefined')
			return '';
		if (value.length === 0)
			return '';

		for (let i = 0; i < value.length; i++) {
			result += value[i][targetPropertyName];
			if (i !== (value.length - 1))
				result += newLine;
		}

		return result;
	}
}
