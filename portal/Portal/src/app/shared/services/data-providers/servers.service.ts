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
import {Injectable} from '@angular/core';
import {environment} from '../../../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {ResponseData} from '../../models/response-data.model';
import {catchError, map} from 'rxjs/operators';
import {ResponseServer} from '../../models/responses/servers/response-server';
import {RequestServer} from '../../models/requests/servers/request-server-update.model';


@Injectable()

/**
 * 서버 정보 데이터 프로바이더
 */
export class ServersProvider {
	// 서버 정보 URL
	private SERVERS_URL = `${environment.apiUrl}/Servers`;

	// 생성자
	constructor(
		private httpClient: HttpClient
	) {
	}

	/**
	 * 서버 정보 조회
	 */
	get(id: string): Observable<ResponseData<ResponseServer>> {
		return this.httpClient.get<ResponseData<ResponseServer>>(`${this.SERVERS_URL}/${id}`)
			.pipe(
				map((result) => {
					return Object.assign(new ResponseData<ResponseServer>(), result);
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 서버 정보 등록
	 */
	add(request: RequestServer): Observable<ResponseData> {
		return this.httpClient.post<ResponseData>(`${this.SERVERS_URL}`, request)
			.pipe(
				map((result) => {
					return Object.assign(new ResponseData(), result);
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 서버 정보 수정
	 */
	update(id: string, request: RequestServer): Observable<ResponseData> {
		return this.httpClient.put<ResponseData>(`${this.SERVERS_URL}/${id}`, request)
			.pipe(
				map((result) => {
					return Object.assign(new ResponseData(), result);
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}

	/**
	 * 서버 정보 삭제
	 */
	delete(id: string): Observable<ResponseData> {
		return this.httpClient.delete<ResponseData>(`${this.SERVERS_URL}/${id}`)
			.pipe(
				map((result) => {
					return Object.assign(new ResponseData(), result);
				}),
				catchError((err) => {
					return throwError(err);
				})
			);
	}
}
