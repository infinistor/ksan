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
import { Injectable } from '@angular/core';
import { Grid } from '../classes/grid/grid.model';


/**
 * CommonKenoGrid 서비스 클래스
 */
@Injectable({ providedIn: 'root' })
export class CommonKendoGridService {
	/**
	 * TotalCount 로 그리드 데이터를 전체 Init 한다
	 */
	public initializeData(grid: Grid): void {
		let seqCount = 0;
		const updateFlag = grid.apiUpdateFlag;

		// 생성할 더미 데이터를 하나 뽑아온다
		const dummyObject = Object.assign({}, grid.gridView.data[0]);

		// 더미 오브젝트의 속성데이터를 초기화한다
		for (const column of grid.columns) {
			dummyObject[column.field] = '-';
		}

		// 그리드 데이터의 최초 위치부터 받아온 데이터로 업데이트한다
		for (const item of grid.diffItems) {
			const addItem = Object.assign({}, item);
			addItem[updateFlag] = seqCount;
			addItem.isUpdate = true;
			grid.items.push(addItem);

			// 저장한 스킵 시퀀스를 보관한다
			grid.progressSkip.push(seqCount);

			seqCount++;
		}


		// 최초 위치 이후부터 API 에서 받아온 전체 레코드 수 만큼 더미 오브젝트를 생성한다
		for (let i = grid.pageSize; i < grid.totalCount; i++) {
			const dummy = Object.assign({}, dummyObject);
			dummy[updateFlag] = seqCount;
			dummy.isUpdate = false;
			grid.items.push(dummy);
			seqCount++;
		}
	}

	/**
	 * 받아온 데이터를 캐싱 데이터에 업데이트 한다
	 * @param grid
	 */
	public addData(grid: Grid): void{
		let j = 0;
		const requestRange = (grid.skip + grid.pageSize);

		for (let i = grid.skip; i < requestRange; i++) {
			// API 로 부터 받아온 스킵 인덱스를 저장하고 아이템을 저장한다
			if (grid.progressSkip.indexOf(i) === -1) {
				grid.progressSkip.push(i);
			}

			const addItem = Object.assign({}, grid.diffItems[j]);

			if (!grid.items[i].isUpdate) {
				addItem.isUpdate = true;
				grid.items[i] = addItem;
			}

			j++;
		}
	}

	/**
	 * 저장된 Items 와 스킵범위를 확인해서 API 를 호출할지 말지를 판단한다
	 * @param grid
	 */
	public isHaveToCallApi(grid: Grid): boolean {
		let isHaveToCallApi: boolean = false;

		// 요구하는 보여줘야할 범위 데이터 속에 내가 가진 데이터가 있는지 없는지를 판단해본다
		const requestRange = (grid.skip + grid.pageSize);
		for (let i = grid.skip; i < requestRange; i++) {
			// 요청한적이 없는 스킵 인덱스 인경우
			if (grid.progressSkip.indexOf(i) === -1) {
				isHaveToCallApi = true;
				break;
			}
		}

		return isHaveToCallApi;
	}
}
