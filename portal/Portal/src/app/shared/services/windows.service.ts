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
import { isPlatformBrowser } from '@angular/common';
import { ClassProvider, FactoryProvider, InjectionToken, PLATFORM_ID } from '@angular/core';

/* Create a new injection token for injecting the window into a component. */
export const WINDOW = new InjectionToken('WindowToken');

/* Define abstract class for obtaining reference to the global window object. */
export abstract class WindowRef {

	get nativeWindow(): Window | object {
		throw new Error('Not implemented.');
	}

}

/* Define class that implements the abstract class and returns the native window object. */
export class BrowserWindowRef extends WindowRef {

	constructor() {
		super();
	}

	get nativeWindow(): Window | object {
		return window;
	}

}

/* Create an factory function that returns the native window object. */
export function windowFactory(browserWindowRef: BrowserWindowRef, platformId: object): Window | object {
	if (isPlatformBrowser(platformId)) {
		return browserWindowRef.nativeWindow;
	}
	return new Object();
}

/* Create a injectable provider for the WindowRef token that uses the BrowserWindowRef class. */
export const browserWindowProvider: ClassProvider = {
	provide: WindowRef,
	useClass: BrowserWindowRef
};

/* Create an injectable provider that uses the windowFactory function for returning the native window object. */
export const windowProvider: FactoryProvider = {
	provide: WINDOW,
	useFactory: windowFactory,
	deps: [ WindowRef, PLATFORM_ID ]
};

/* Create an array of providers. */
export const WINDOW_PROVIDERS = [
	browserWindowProvider,
	windowProvider
];
