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

const REGION_NAME = "label_legion";
const KSAN_AGENT = "label_ksan_agent";
const KSAN_GW = "label_ksan_gw";
const KSAN_OSD = "label_ksan_osd";
const KSAN_LIFECYCLE = "label_ksan_lifecycle";
const KSAN_LOG = "label_ksan_log";
const KSAN_REPLICATION = "label_ksan_replication";
const MY_STATUS = "service_status";

const DEFAULT_PREFIX = "<span class='service_name_default'>ksan</span><span class='service_name'>";
var global_status = true;
export default class OverviewView extends JetView {
	config() {
		return {
			type: "abslayout",
			minWidth: 540,
			height: 300,
			rows: [
				{ id: MY_STATUS, view: "label" },
				{ height: 10 },
				{
					height: 35,
					cols: [
						{ label: "Region", view: "label", width: 100, height: 35, css: "overview_title" },
						{ id: REGION_NAME, view: "label", height: 35 },
					],
				},
				{
					height: 35,
					cols: [
						{ label: "Services", view: "label", width: 100, height: 35, css: "overview_title" },
						{ id: KSAN_AGENT, view: "label" },
					],
				},
				{
					cols: [
						{ width: 100, height: 35 },
						{ id: KSAN_GW, height: 35, view: "label" },
					],
				},
				{
					cols: [
						{ width: 100, height: 35 },
						{ id: KSAN_OSD, height: 35, view: "label" },
					],
				},
				{
					cols: [
						{ width: 100, height: 35 },
						{ id: KSAN_LIFECYCLE, height: 35, view: "label" },
					],
				},
				{
					cols: [
						{ width: 100, height: 35 },
						{ id: KSAN_LOG, height: 35, view: "label" },
					],
				},
				{
					cols: [
						{ width: 100, height: 35 },
						{ id: KSAN_REPLICATION, height: 35, view: "label" },
					],
				},
			],
		};
	}
	init() {
		loadServices();
		loadMainRegion();
	}
}

/**
 * 기본 리전을 조회한다.
 * @returns 기본 리전 정보
 */
function loadMainRegion() {
	webix
		.ajax()
		.get("/api/v1/Regions/Default")
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else $$(REGION_NAME).setValue(`<span class='region_name'>${response.Data.Name}</span>`);
			},
			function (error) {
				if (error.status != 401) {
					var response = JSON.parse(error.response);
					webix.message({ text: response.Message, type: "error", expire: 5000 });
				}
			}
		);
}

/**
 * 서비스 상태를 조회한다.
 * @returns 서비스 정보
 */
function loadServices() {
	webix
		.ajax()
		.get("/api/v1/Services")
		.then(
			function (data) {
				var response = data.json();
				
				if (response.Result == "Error") webix.message({ text: response.Message, type: "error", expire: 5000 });
				else {
					$$(KSAN_AGENT).setValue(getStatus("ksanAgent", response.Data.Items));
					$$(KSAN_GW).setValue(getStatus("ksanGW", response.Data.Items));
					$$(KSAN_OSD).setValue(getStatus("ksanOSD", response.Data.Items));
					$$(KSAN_LIFECYCLE).setValue(getStatus("ksanLifecycleManager", response.Data.Items));
					$$(KSAN_LOG).setValue(getStatus("ksanLogManager", response.Data.Items));
					$$(KSAN_REPLICATION).setValue(getStatus("ksanReplicationManager", response.Data.Items));

					if (global_status == true) $$(MY_STATUS).setValue("<span class='card_title'>System Overview</span> <span class='status_marker healthy'>Healthy</span>");
					else $$(MY_STATUS).setValue("<span class='card_title'>System Overview</span> <span class='status_marker check'>Need to Check</span>");
				}
			},
			function (error) {
				if (error.status != 401) {
					var response = JSON.parse(error.response);
					webix.message({ text: response.Message, type: "error", expire: 5000 });
				}
			}
		);
}

/**
 * 서비스의 상태 정보를 출력한다.
 * @param {string} service_type 서비스 타입
 * @param {json} items 서비스 목록
 * @returns 
 */
function getStatus(service_type, items) {
	var offlineCount = 0;
	var timeoutCount = 0;
	var unknownCount = 0;
	var count = 0;

	items.forEach((item) => {
		if (item.ServiceType == service_type) {
			switch (item.State) {
				case "Offline": offlineCount++; break;
				case "Timeout": timeoutCount++; break;
				case "Unknown": unknownCount++; break;
			}
			count++;
		}
	});

	var result = `${DEFAULT_PREFIX}${service_type.substring(4)} ${count - (offlineCount + timeoutCount + unknownCount)}/${count}</span>`;

	if (offlineCount == 0 && timeoutCount == 0 && unknownCount == 0) result += " <span class='service_status healthy'>(Healthy)</span>";
	else {
		global_status = false;
		result += "<span class='service_status unhealthy'>";
		var prefix = "(";
		if (offlineCount > 0) { result += `${prefix}${offlineCount} Offline`; prefix = ", "; }
		if (timeoutCount > 0) { result += `${prefix}${timeoutCount} Timeout`; prefix = ", "; }
		if (unknownCount > 0) { result += `${prefix}${unknownCount} Unknown`; }
		result += ")</span>";
	}
	return result;
}
