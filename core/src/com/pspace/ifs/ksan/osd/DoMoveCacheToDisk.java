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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pspace.ifs.ksan.osd.utils.OSDConfig;
import com.pspace.ifs.ksan.osd.utils.OSDConstants;
import com.pspace.ifs.ksan.osd.utils.OSDUtils;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.PrintStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoMoveCacheToDisk implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(DoMoveCacheToDisk.class);
    private static volatile boolean isStop = false;
    ExecutorService executorService;
    List<Callable<Void>> tasks;

    public static void stopDoMoveCacheToDisk() {
        isStop = true;
    }

    @Override
    public void run() {
        logger.info(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK, OSDConfig.getInstance().getCacheDiskpath());
        executorService = Executors.newFixedThreadPool(4);
        tasks = new ArrayList<>();
        recursiveMove(OSDConfig.getInstance().getCacheDiskpath());
        try {
            executorService.invokeAll(tasks);
            executorService.shutdown();
            while (!executorService.isTerminated()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            PrintStack.logging(logger, e);
        } finally {
            logger.info(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK_END);
        }
    }
    
    private void recursiveMove(String dirPath) {
        try {
            File dir = new File(dirPath);
            File[] files = dir.listFiles();

            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    if (files[i].getName().equals(Constants.OBJ_DIR)) {
                        // check(files[i].getAbsolutePath());
                        tasks.add(new FindFiles(files[i]));
                        return;
                    } else {
                        recursiveMove(files[i].getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }
    }

    class FindFiles implements Callable<Void> {
        // private volatile boolean isDone = false;
        private File file;
        private long expire = OSDConfig.getInstance().getCacheExpire();
        List<String> fileList;
        ExecutorService executor;
        // Queue<String> queue = new LinkedList<>();

        public FindFiles(File file) {
            this.file = file;
        }

        @Override
        public Void call() throws Exception {
            try {
                logger.info("file path : {}", file.getAbsolutePath());
                executor = Executors.newFixedThreadPool(5);
                fileList = new ArrayList<>();
                // MoveWorker mover = new MoveWorker();
                // mover.start();
                check(file.getAbsolutePath());
                if (fileList.size() > 0) {
                    executor.execute(new MoveWorker(fileList));
                }
                executor.shutdown();
                while (!executor.isTerminated()) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                PrintStack.logging(logger, e);
            } finally {
                logger.info("FindFiles end : {}", file.getAbsolutePath());
            }
            // logger.info("file list size : {}", fileList.size());
            // move();
            return null;
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
                    if (diff >= expire) {
                        fileList.add(files[i].getAbsolutePath());
                        // queue.add(files[i].getAbsolutePath());
                    }
                }
                if (fileList.size() >= 100) {
                    // MoveFiles moveFiles = new MoveFiles(fileList);
                    // executor.execute(moveFiles);
                    executor.execute(new MoveWorker(fileList));
                    fileList = new ArrayList<>();
                }
            }
        }

        class MoveWorker extends Thread {
            List<String> list;
            
            MoveWorker(List<String> list) {
                this.list = list;
            }

            public void run() {
                try {
                    byte[] buffer = new byte[512 * 1024];
                    int len;
                    for (String source : list) {
                        if (isStop) {
                            logger.info("MoveWorker is Stoped. : {}", file.getAbsolutePath());
                            return;
                        }

                        if (source == null || source.isEmpty()) {
                            continue;
                        }

                        String targetPath = source.substring(OSDConfig.getInstance().getCacheDiskpath().length());
                        File file = new File(source);
                        File target = new File(targetPath);
                        
                        if (file.exists()) {
                            if (target.exists()) {
                                if (!target.delete()) {
                                    logger.error(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK_DELETE_TARGET_FAIL, target.getAbsolutePath());
                                } 
                            }
                            try (FileInputStream fis = new FileInputStream(file);
                                 FileOutputStream fos = new FileOutputStream(target)) {
                                while ((len = fis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                                fos.flush();
                                logger.info(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK_RENAME_SUCCESS, file.getAbsolutePath(), targetPath);
                            } catch (IOException e) {
                                logger.error(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK_RENAME_FAIL, file.getAbsolutePath(), targetPath);
                                logger.error(e.getMessage());
                                Files.createSymbolicLink(Paths.get(target.getAbsolutePath()), Paths.get(file.getAbsolutePath()));
                            }
                            // try {
                            //     Files.move(Paths.get(file.getAbsolutePath()), Paths.get(target.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                            //     logger.info(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK_RENAME_SUCCESS, file.getAbsolutePath(), targetPath);
                            // } catch (IOException e) {
                            //     logger.error(OSDConstants.LOG_DO_MOVE_CACHE_TO_DISK_RENAME_FAIL, file.getAbsolutePath(), targetPath);
                            //     logger.error(e.getMessage());
                            // }
                        }
                    }
                } catch (Exception e) {
                    PrintStack.logging(logger, e);
                }
                logger.info("MoveWorker end : {}", file.getAbsolutePath());
            }
        }
    }
}
