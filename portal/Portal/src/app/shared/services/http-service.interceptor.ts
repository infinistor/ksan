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
import {HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse, HttpResponse, HttpHeaders} from '@angular/common/http';
import { Router } from '@angular/router';
import {Observable, throwError, of} from 'rxjs';
import {catchError, map, retryWhen, mergeMap, take, delay} from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class HttpServiceInterceptor implements HttpInterceptor {
	constructor(
		private router: Router,
		private authService: AuthService
	) { }

	// http request interceptor
	intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
		let req: HttpRequest<any>;

		// 파일 업로드 경우
		if (request.url.toLowerCase().endsWith('/api/v1/upload')) {
			req = request;
		}
		else {

			let requestHeaders: HttpHeaders = request.headers;

			if (requestHeaders.has('Content-Type'))
				requestHeaders = requestHeaders.set('Content-Type', 'application/json; charset=utf-8');
			else
				requestHeaders = requestHeaders.append('Content-Type', 'application/json; charset=utf-8');

			req = request.clone({
				headers: requestHeaders,
				withCredentials: true
			});
		}

		return next.handle(req)
			.pipe(
				retryWhen(errors => {

					const maxRetries = 3;
					let retryCount = 0;

					return errors
						.pipe(
							delay(2000)
						)
						.pipe(
							mergeMap(error => {
								return error.status < 500 || ++retryCount >= maxRetries ? throwError(error) : of(error);
							}),
							take(maxRetries)
						);
				}),
				map((event: HttpEvent<any>) => {
					if (req.url.toLowerCase().endsWith('/api/v1/upload'))
						return event;
					else if (event instanceof HttpResponse) {
						return event;
					}
				}),
				catchError(err => {
					return this.errorHandler(err);
				})
			);
	}

	// 통신 에러 핸들러
	public errorHandler(err: HttpErrorResponse): Observable<never> {
		if (err instanceof HttpErrorResponse)
		{
			switch (err.status) {
				case 200:
					break;
				case 400:
				case 404:
					this.router.navigate(['/errors/400']);
					break;
				case 401:
					// 로그인 정보 수신하는 부분들을 위해서 로그인 검사 한번 더 호출
					this.authService.checkLogin().subscribe((_) => {
						this.router.navigate(['/account/login']);
					});
					break;
				case 403:
				case 500:
				case 503:
				case 504:
					this.router.navigate(['/errors/' + err.status]);
					break;
				default:
					this.router.navigate(['/account/login']);
					break;
			}
		}
		else {
			this.router.navigate(['/errors']);
			return throwError(err);
		}
	}
}
