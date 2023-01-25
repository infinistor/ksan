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
import { JetView } from "webix-jet";

export default class TopView extends JetView {
	config() {
		return {
			view: "form",
			id: "login_form",

			borderless: true,
			elements: [
				{ align: "center" },
				{
					view: "text",
					width: 300,
					label: "Id",
					name: "LoginId",
					align: "center",
					on: {
						onEnter: function (ev) {
							doLogin(this.getFormView().getValues());
						},
					},
				},
				{
					view: "text",
					width: 300,
					type: "password",
					label: "Password",
					name: "Password",
					align: "center",
					on: {
						onEnter: function (ev) {
							doLogin(this.getFormView().getValues());
						},
					},
				},
				{
					borderless: true,
					css: { "text-align": "center" },
					cols: [
						{
							view: "button",
							value: "Login",
							css: "webix_primary",
							width: 150,
							align: "center",
							click: function () {
								doLogin(this.getFormView().getValues());
							},
						},

						{ view: "button", value: "Cancel", width: 150, align: "center" },
					],
				},
				{ align: "center" },
			],
		};
	}
}

function doLogin(form) {
	webix
		.ajax()
		.headers({ "Content-Type": "application/json" })
		.post("/api/v1/Account/login", JSON.stringify(form))
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Success") {
					var ReturnUrl = webix.storage.session.get("ReturnUrl");
					if (ReturnUrl != null && ReturnUrl != "") location.href = ReturnUrl;
					else location.href = "/";
				} else {
					webix.alert({ title: response.Result, text: response.Message, type: "alert-error" });
				}
			},
			function (error) {
				var response = JSON.parse(error.response);
				webix.message({ text: response.Message, type: "error", expire: 5000 });
			}
		);
}
