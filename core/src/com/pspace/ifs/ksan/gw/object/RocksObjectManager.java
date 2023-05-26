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
package com.pspace.ifs.ksan.gw.object;

import java.util.SortedMap;

import com.pspace.ifs.ksan.gw.identity.S3Parameter;
import com.pspace.ifs.ksan.gw.object.S3Range;
import com.pspace.ifs.ksan.objmanager.Metadata;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.gw.object.S3Object;
import com.pspace.ifs.ksan.gw.encryption.S3Encryption;
import com.pspace.ifs.ksan.gw.exception.GWException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocksObjectManager implements IObjectManager {
    private static final Logger logger = LoggerFactory.getLogger(RocksObjectManager.class);

    @Override
    public void getObject(S3Parameter param, Metadata meta, S3Encryption en, S3Range range) throws GWException {
        // 
    }

    @Override
    public S3Object putObject(S3Parameter param, Metadata meta, S3Encryption en) throws GWException {
        // 
        return null;
    }

    @Override
    public S3Object copyObject(S3Parameter param, Metadata srcObjMeta, S3Encryption srcEn, Metadata meta,
            S3Encryption en) throws GWException {
        // 
        return null;
    }

    @Override
    public boolean deleteObject(S3Parameter param, Metadata meta) throws GWException {
        // 
        return false;
    }

    @Override
    public S3Object uploadPart(S3Parameter param, Metadata meta) throws GWException {
        // 
        return null;
    }

    @Override
    public S3Object uploadPartCopy(S3Parameter param, Metadata srcObjMeta, S3Encryption srcEn, S3Range range,
            Metadata meta) throws GWException {
        // 
        return null;
    }
    
    @Override
    public S3Object completeMultipart(S3Parameter param, Metadata meta, S3Encryption en,
            SortedMap<Integer, Part> listPart) throws GWException {
        // 
        return null;
    }

    @Override
    public void abortMultipart(S3Parameter param, Metadata meta, SortedMap<Integer, Part> listPart) throws GWException {
    
    }

    @Override
    public boolean deletePart(S3Parameter param, Metadata meta) throws GWException {
        return true;
    }

    @Override
    public void restoreObject(S3Parameter param, Metadata meta, Metadata restoreMeta) throws GWException {

    }

    @Override
    public void storageMove(S3Parameter param, Metadata meta, Metadata restoreMeta) throws GWException {
        
    }
}
