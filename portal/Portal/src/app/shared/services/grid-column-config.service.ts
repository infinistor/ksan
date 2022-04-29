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
import { Injectable } from '@angular/core';
import { GridColumnsConfig } from '../classes/grid/grid-columns-configs.model';
import { Grid } from '../classes/grid/grid.model';

/**
 * 그리드 설정 관리 서비스
 */
@Injectable({ providedIn: 'root' })
export class GridColumnConfigService {

	// 저장소명
	private STORAGE_KEY = 'grid-columns-config';

	/**
	 * 저장된 그리드 컬럼 설정을 가져온다.
	 * @returns 그리드 컬럼 설정 목록
	 */
	private getConfigsFromStorage(): GridColumnsConfig[] {
		const configs = localStorage.getItem(this.STORAGE_KEY);
		if (configs) {
			return JSON.parse(configs);
		} else {
			return [];
		}
	}

	/**
	 * 그리드 컬럼 설정을 저장한다.
	 * @param configs 저장할 그리드 컬럼 설정
	 */
	private setConfigsToStorage(configs: GridColumnsConfig[]): void {
		const data = JSON.stringify(configs);
		localStorage.setItem(this.STORAGE_KEY, data);
	}

	/**
	 * 그리드 경로에 해당하는 설정을 가져온다.
	 * @param gridPath 그리드 경로
	 * @returns 그리드 컬럼 설정 객체
	 */
	getConfig(gridPath: string): GridColumnsConfig {
		const configs = this.getConfigsFromStorage();

		let config = configs.find(item => item.gridPath === gridPath);
		if (!config) {
			config = { gridPath, hiddenColumnFields: [] };
		}
		return config;
	}

	/**
	 * 그리드 컬럼 설정을 저장한다.
	 * @param config 그리드 컬럼 설정 객체
	 */
	setConfig(config: GridColumnsConfig): void {
		if (!config) {
			return;
		}

		const configs = this.getConfigsFromStorage();

		const found = configs.find(item => item.gridPath === config.gridPath);
		if (found) {
			found.hiddenColumnFields = config.hiddenColumnFields;
		}
		else {
			configs.push({ ...config });
		}

		this.setConfigsToStorage(configs);
	}

	/**
	 * 주어진 그리드에 해당하는 그리드 컬럼 설정을 가져온다.
	 * @param grid 그리드 객체
	 * @returns 그리드 컬럼 설정 객체
	 */
	getConfigByGrid(grid: Grid): GridColumnsConfig {
		const gridPath = grid.getGridPath();
		return this.getConfig(gridPath);
	}

	/**
	 * 해당 그리드의 그리드 컬럼 설정을 저장한다.
	 * @param grid 그리드 객체
	 */
	setConfigByGrid(grid: Grid): void {
		const gridPath = grid.getGridPath();
		const hiddenColumnFields = grid.getHiddenColumnFields();
		this.setConfig({ gridPath, hiddenColumnFields });
	}
}
