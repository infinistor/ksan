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
package com.pspace.ifs.ksan.osd;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.osd.DISKPOOLLIST.DISKPOOL.SERVER;
import com.pspace.ifs.ksan.osd.DISKPOOLLIST.DISKPOOL.SERVER.DISK;

public class DoEmptyTrash implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(DoECPriObject.class);
    private static DISKPOOLLIST diskpoolList;

    @Override
    public void run() {
        logger.info(OSDConstants.LOG_DO_EC_PRI_OBJECT_START);
        diskpoolList = OSDUtils.getInstance().getDiskPoolList();

        if (OSDUtils.getInstance().getCacheDisk() != null) {
            recursiveEmptyCache(OSDUtils.getInstance().getCacheDisk());
        }

        for (SERVER server : diskpoolList.getDiskpool().getServers()) {
            if (OSDUtils.getInstance().getLocalIP().equals(server.getIp())) {
                for (DISK disk : server.getDisks()) {
                    String trashDir = disk.getPath() + OSDConstants.SLASH + OSDConstants.TRASH_DIR;
                    empty(trashDir);
                }
            }
        }
    }

    private void recursiveEmptyCache(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                if (files[i].getName().equals(OSDConstants.TRASH_DIR)) {
                    empty(files[i].getAbsolutePath());
                }
            }
        }
    }

    private void empty(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
    }
}