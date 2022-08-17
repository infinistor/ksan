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
import java.io.IOException;
import java.util.Calendar;

import com.pspace.ifs.ksan.osd.utils.OSDConfig;
import com.pspace.ifs.ksan.osd.utils.OSDConstants;
import com.pspace.ifs.ksan.osd.utils.OSDUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoMoveCacheToDisk implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(DoMoveCacheToDisk.class);

    @Override
    public void run() {
        logger.info(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK);
        recursiveMove(OSDConfig.getInstance().getCacheDisk());
    }
    
    private void recursiveMove(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                if (files[i].getName().equals(OSDConstants.OBJ_DIR)) {
                    check(files[i].getAbsolutePath());
                } else {
                    recursiveMove(files[i].getAbsolutePath());
                }
            }
        }
    }

    private void check(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        long now = Calendar.getInstance().getTimeInMillis();
        
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                check(files[i].getAbsolutePath());
            } else if (files[i].isFile()) {
                long diff = (now - files[i].lastModified());

                if (diff >= OSDConfig.getInstance().getCacheLimitMilliseconds()) {
                    move(files[i]);
                }
            }
        }
    }

    private void move(File file) {
        String targetPath = file.getAbsolutePath().substring(OSDConfig.getInstance().getCacheDisk().length());

        logger.info(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK_TARGET_PATH, targetPath);
        File target = new File(targetPath);
        if (target.exists()) {
            target.delete();
        }

        String command = OSDConstants.DO_MOVE_CACHE_TO_DISK_COMMAND + file.getAbsolutePath() + OSDConstants.SPACE + targetPath;
        logger.info(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK_COMMAND, command);
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            int exitCode = p.waitFor();
            p.destroy();
            logger.info(OSDConstants.LOG_DO_EC_PRI_OBJECT_ZFEC_EXIT_CODE, exitCode);
            file.delete();
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }
    }
}
