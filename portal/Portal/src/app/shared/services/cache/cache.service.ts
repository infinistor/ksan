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
import { CacheValue } from './cache-value.model';

const DEFAULT_EXPIRES = Number.MAX_VALUE / 1000 / 60 / 60;   // 만료시간 기본 설정

/**
 * 세션 캐시 서비스
 */
@Injectable({ providedIn: 'root' })
export class SessionCacheService {
	/**
	 * 생성자
	 */
	constructor() { }

	/**
	 * 캐시에서 가져오기
	 * @param key
	 */
	public get(key: string): any {
		// 해당 key가 존재할 경우
		if (this.has(key)) {
			const value: CacheValue = JSON.parse(sessionStorage.getItem(key));

			// 만료시간 이내이면 cache 값 return
			if (!this.isExpired(value.expires)) {
				return value.value;
			}
			// 만료시간이 지난 경우 cache에서 해당 key 삭제
			else {
				this.remove(key);
				return null;
			}
		}
		// 해당 key가 존재하지 않을 경우
		else {
			return null;
		}
	}

	/**
	 * 캐시에 저장
	 * @param key
	 * @param value
	 * @param expires
	 */
	public set(key: string, value: any, expires?: number): boolean {
		try {
			const cacheValue: CacheValue = new CacheValue();
			cacheValue.value = value;
			cacheValue.expires = expires ? expires : DEFAULT_EXPIRES;

			sessionStorage.setItem(key, JSON.stringify(cacheValue));
			return true;
		} catch (e) {
			return false;
		}
	}

	/**
	 * 해당 key를 캐시에서 삭제
	 * @param key
	 */
	public remove(key: string): void {
		sessionStorage.removeItem(key);
	}

	/**
	 * 해당 key가 캐시에 존재하는지 확인
	 * @param key 검사할 키 이름
	 */
	public has(key: string): boolean {
		return !!sessionStorage.getItem(key);
	}

	/**
	 * 캐시에서 key와 value가 모두 일치하는 값이 있는지 확인
	 * @param key
	 * @param value
	 */
	public hasValue(key: string, value: string): boolean {
		// 해당 key가 캐시에 존재할 경우
		if (this.has(key)) {
			// 해당 key의 값을 가져온다
			const cacheValue = this.get(key);

			// 값이 있을 경우
			if (cacheValue) {
				// 값에서 찾고자 하는 값이 존재하는지 확인
				if (typeof (cacheValue) === 'string' || typeof (cacheValue) === 'number') {
					return cacheValue === value;
				}
				else {
					return cacheValue.indexOf(value) >= 0;
				}
			}
			// 값이 없는 경우
			else {
				return false;
			}
		}
		// 해당 key가 캐시에 존재하지 않을 경우
		else {
			return false;
		}
	}

	/**
	 * 캐시 모두 삭제
	 */
	public clear(): void {
		sessionStorage.clear();
	}

	/**
	 * 만료 시간 확인
	 * @param expires
	 */
	private isExpired(expires: number): boolean {
		try {
			return (expires * 1000 * 60 * 60 <= Date.now());
		} catch (e) {
			return false;
		}
	}
}

/**
 * 로컬 캐시 서비스
 */
@Injectable({ providedIn: 'root' })
export class LocalCacheService {
	/**
	 * 생성자
	 */
	constructor() { }

	/**
	 * 캐시에서 가져오기
	 * @param key
	 */
	public get(key: string): any {
		// 해당 key가 존재할 경우
		if (this.has(key)) {
			const value: CacheValue = JSON.parse(localStorage.getItem(key));

			// 만료시간 이내이면 cache 값 return
			if (!this.isExpired(value.expires)) {
				return value.value;
			}
			// 만료시간이 지난 경우 cache에서 해당 key 삭제
			else {
				this.remove(key);
				return null;
			}
		}
		// 해당 key가 존재하지 않을 경우
		else {
			return null;
		}
	}

	/**
	 * 캐시에 저장
	 * @param key
	 * @param value
	 * @param expires
	 */
	public set(key: string, value: any, expires?: number): boolean {
		try {
			const cacheValue: CacheValue = new CacheValue();
			cacheValue.value = value;
			cacheValue.expires = expires ? expires : DEFAULT_EXPIRES;

			localStorage.setItem(key, JSON.stringify(cacheValue));
			return true;
		} catch (e) {
			return false;
		}
	}

	/**
	 * 해당 key를 캐시에서 삭제
	 * @param key
	 */
	public remove(key: string): void {
		localStorage.removeItem(key);
	}

	/**
	 * 해당 key가 캐시에 존재하는지 확인
	 * @param key
	 */
	public has(key: string): boolean {
		return !!localStorage.getItem(key);
	}

	/**
	 * 캐시에서 key와 value가 모두 일치하는 값이 있는지 확인
	 * @param key
	 * @param value
	 */
	public hasValue(key: string, value: string): boolean {
		// 해당 key가 캐시에 존재할 경우
		if (this.has(key)) {
			// 해당 key의 값을 가져온다
			const cacheValue = this.get(key);

			// 값이 있을 경우
			if (cacheValue) {
				// 값에서 찾고자 하는 값이 존재하는지 확인
				if (typeof (cacheValue) === 'string' || typeof (cacheValue) === 'number') {
					return cacheValue === value;
				}
				else {
					return cacheValue.indexOf(value) >= 0;
				}
			}
			// 값이 없는 경우
			else {
				return false;
			}
		}
		// 해당 key가 캐시에 존재하지 않을 경우
		else {
			return false;
		}
	}

	/**
	 * 캐시 모두 삭제
	 */
	public clear(): void {
		localStorage.clear();
	}

	/**
	 * 만료 시간 확인
	 * @param expires
	 */
	private isExpired(expires: number): boolean {
		try {
			return (expires * 1000 * 60 * 60 <= Date.now());
		} catch (e) {
			return false;
		}
	}
}
