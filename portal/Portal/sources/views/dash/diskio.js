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
import { sizeToString } from "../../models/utils/sizeToString";

const MY_WRITE_TITLE = "disk_write_title";
const MY_READ_TITLE = "disk_read_title";
const MY_WRITE = "disk_write";
const MY_READ = "disk_read";
var intervalDiskIo;

export default class DiskioView extends JetView {
	config() {
		return {
			type: "abslayout",
			minWidth: 540,
			height: 300,
			borderless: true,
			rows: [
				{
					view: "label",
					label: "<span class='card_title'>Disk I/O</span>",
				},
				{ height: 20 },
				{
					cols: [
						{
							view: "label",
							label: "<span class='io_write'>Write</span>",
							height: 15,
						},
						{
							view: "label",
							label: "<span class='io_read'>Read</span>",
							height: 15,
						},
					],
				},
				{
					cols: [
						{
							id: MY_WRITE_TITLE,
							view: "label",
							label: "<span class='io_in'>0.0</span><span class='io_in_unit'>MB/s</span>",
						},
						{
							id: MY_READ_TITLE,
							view: "label",
							label: "<span class='io_out'>0.0</span><span class='io_out_unit'>MB/s</span>",
						},
					],
				},
				{
					cols: [
						{
							view: "chart",
							id: MY_WRITE,
							minWidth: 300,
							height: 240,
							borderless: true,
							dynamic: true,
							alpha: 0.3,
							border: true,
							type: "splineArea",
							value: "#Write#",
							color: "#B9C6DF",
							padding: {
								left: 10,
								top: 10,
							},
							tooltip: {
								template: (obj) => {
									return "Write : " + sizeToString(obj.Write);
								},
							},
						},
						{
							view: "chart",
							id: MY_READ,
							minWidth: 300,
							height: 240,
							borderless: true,
							dynamic: true,
							alpha: 0.3,
							border: true,
							type: "splineArea",
							value: "#Read#",
							color: "#d3d3d3",
							padding: {
								left: 10,
								top: 10,
							},
							tooltip: {
								template: (obj) => {
									return "Read : " + sizeToString(obj.Read);
								},
							},
						},
					],
				},
			],
		};
	}
	init() {
		startTimer();
	}
}

/**
 * 타이머를 시작한다.
 */
function startTimer() {
	intervalDiskIo = setInterval(function () {
		loadDiskUsages();
	}, 5000);
}

/**
 * 타이머를 종료한다.
 */
function stopTimer() {
	clearInterval(intervalDiskIo);
}

/**
 * 디스크 사용량 정보를 읽어와 합산한다.
 * @returns 디스크 사용량 정보
 */
function loadDiskUsages() {
	return webix
		.ajax()
		.get("/api/v1/Disks?SearchStates=Good")
		.then(
			function (data) {
				var response = data.json();
				if (response.Result == "Error") {
					webix.message({ text: response.Message, type: "error", expire: 5000 });
					stopTimer();
					return "";
				} else {
					var usage = { DateTime: Date.now(), Write: 0.0, Read: 0.0 };
					response.Data.Items.forEach((item) => {
						usage.Write += item.Write;
						usage.Read += item.Read;
					});
					$$(MY_WRITE).add(usage);
					$$(MY_READ).add(usage);
					$$(MY_WRITE_TITLE).setValue(sizeToHtml(usage.Write, "Write"));
					$$(MY_READ_TITLE).setValue(sizeToHtml(usage.Read, "Read"));
					return usage;
				}
			},
			function (error) {
				// var response = JSON.parse(error.response);
				// webix.message({ text: response.Message, type: "error", expire: 5000 });
				stopTimer();
				return "";
			}
		);
}

/**
 * 입력된 byte를 보기쉬운 용량 단위로 변환하여 반환한다.
 * @param {long} bytes 변환할 용량
 * @param {String} type 타입
 * @returns 용량 + 단위
 */
export function sizeToHtml(bytes, type) {
	const thresh = 1000;
	const dp = 1;

	if (Math.abs(bytes) < thresh) {
		if (type == "Write") return `<span class='io_in'>${bytes}</span><span class='io_in_unit'>B/s</span>`;
		else return `<span class='io_out'>${bytes}</span><span class='io_out_unit'>B/s</span>`;
	}

	const units = ["kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"];
	let u = -1;
	const r = 10 ** dp;

	do {
		bytes /= thresh;
		++u;
	} while (Math.round(Math.abs(bytes) * r) / r >= thresh && u < units.length - 1);

	if (type == "Write") return `<span class='io_in'>${bytes.toFixed(dp)}</span><span class='io_in_unit'>${units[u]}/s</span>`;
	else return `<span class='io_out'>${bytes.toFixed(dp)}</span><span class='io_out_unit'>${units[u]}/s</span>`;
}
