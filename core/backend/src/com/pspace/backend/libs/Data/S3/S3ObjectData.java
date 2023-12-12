/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License. See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.backend.Libs.Data.S3;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pspace.backend.Libs.S3.Tagging;
import com.pspace.backend.Libs.S3.Tagging.TagSet.Tag;
import com.pspace.ifs.ksan.objmanager.Metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3ObjectData {
	final Logger logger = LoggerFactory.getLogger(S3ObjectData.class);

	public String objectName;
	public String versionId;
	public long size;
	public Collection<Tag> tags;

	public Boolean isTagSet;

	public S3ObjectData(String objectName, String versionId, long size, String strTags) {
		this.objectName = objectName;
		this.versionId = versionId;
		this.size = size;
		setTags(strTags);
	}

	public S3ObjectData(Metadata Data) {
		this.objectName = Data.getPath();
		this.versionId = Data.getVersionId();
		setTags(Data.getTag());
	}

	public Boolean setTags(String strTagging) {
		if (StringUtils.isBlank(strTagging)) {
			isTagSet = false;
			return false;
		}
		try {
			// Tag 언마샬링
			var myTags = new XmlMapper().readValue(strTagging, Tagging.class);
			if (myTags == null)
				return false;
			if (myTags.tags == null)
				return false;
			if (myTags.tags.tags == null)
				return false;
			if (myTags.tags.tags.size() == 0)
				return false;

			for (var MyTag : myTags.tags.tags)
				tags.add(MyTag);
			isTagSet = true;
			return true;
		} catch (Exception e) {
			logger.error("Object info read failed : {}", strTagging, e);
			return false;
		}
	}
}
