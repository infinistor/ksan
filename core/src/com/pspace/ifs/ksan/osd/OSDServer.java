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

import static com.google.common.io.BaseEncoding.base16;

import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.FileReader;
import org.json.simple.JSONObject;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.pspace.ifs.ksan.osd.DoEmptyTrash;
import com.pspace.ifs.ksan.osd.utils.OSDConfig;
import com.pspace.ifs.ksan.osd.utils.OSDConstants;
import com.pspace.ifs.ksan.osd.utils.OSDUtils;
import com.pspace.ifs.ksan.libs.DiskManager;
import com.pspace.ifs.ksan.libs.KsanUtils;
import com.pspace.ifs.ksan.libs.data.OsdData;
import com.pspace.ifs.ksan.libs.multipart.Part;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.HeartbeatManager;
import com.pspace.ifs.ksan.libs.config.AgentConfig;
import com.pspace.ifs.ksan.libs.osd.OSDClient;
import com.pspace.ifs.ksan.libs.Constants;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;

import de.sfuhrm.openssl4j.OpenSSL4JProvider;
import org.apache.commons.crypto.stream.CtrCryptoInputStream;
import org.apache.commons.crypto.stream.CtrCryptoOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OSDServer {
    private final static Logger logger = LoggerFactory.getLogger(OSDServer.class);
    private volatile static boolean isRunning;
    private volatile static ScheduledExecutorService serviceEmptyTrash = null;
    private volatile static ScheduledExecutorService serviceMoveCacheToDisk = null;
    private volatile static ScheduledExecutorService serviceEC = null;
    private volatile static ExecutorService service = null;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new HookThread());
        OSDServer server = new OSDServer();
        
        try {
            server.start();
        } catch (Exception e) {
            logger.error("OSDServer start error : {}", e.getMessage());
        }
    }

    private static void setSystemConfiguration() {
        System.setProperty(Constants.OSD_CONFIG_KEY, Constants.OSD_CONFIG_DIR);
        System.setProperty(Constants.OSD_PID_KEY, Constants.OSD_PID_DIR);
        System.setProperty(Constants.OSD_SERVICEID_KEY, Constants.OSD_SERVICEID_DIR);
    }

    public static void start() {
        logger.info(OSDConstants.LOG_OSD_SERVER_START);
        setSystemConfiguration();
        KsanUtils.writePID(System.getProperty(Constants.OSD_PID_KEY) + File.separator + Constants.OSD_PID_FILE);
        try {
            OSDPortal.getInstance().getConfig();
            OSDPortal.getInstance().getDiskPoolsDetails();
            while (!OSDPortal.getInstance().isAppliedDiskpools()) {
                Thread.sleep(1000);
                OSDPortal.getInstance().getDiskPoolsDetails();
            }
        } catch (Exception e) {
            PrintStack.logging(logger, e);
            throw new RuntimeException(new RuntimeException());
        }

        try {
            EventObject.getInstance().regist();
        } catch (Exception e) {
            PrintStack.logging(logger, e);
        }

        // startEmptyTrash();
        // startMoveCacheToDisk();
        startECThread();

        int poolSize = OSDConfig.getInstance().getPoolSize();

        service = Executors.newFixedThreadPool(poolSize);

        isRunning = true;
        try (ServerSocket server = new ServerSocket(OSDConfig.getInstance().getPort())) {
            while (isRunning) {
                try {
                    Socket socket = server.accept();
                    logger.info(OSDConstants.LOG_OSD_SERVER_CONNECTED_INFO, socket.getRemoteSocketAddress().toString());
                    service.execute(new Worker(socket));
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public static void startEmptyTrash() {
        if (serviceEmptyTrash != null) {
            serviceEmptyTrash.shutdownNow();
            while (!serviceEmptyTrash.isTerminated()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        } else {
            serviceEmptyTrash = Executors.newSingleThreadScheduledExecutor();
        }
        serviceEmptyTrash.scheduleAtFixedRate(new DoEmptyTrash(), 1000, OSDConfig.getInstance().getTrashCheckInterval(), TimeUnit.MILLISECONDS);
    }

    public static void startMoveCacheToDisk() {
        if (OSDConfig.getInstance().isCacheDiskpath()) {
            if (serviceMoveCacheToDisk != null) {
                serviceMoveCacheToDisk.shutdownNow();
            } else {
                serviceMoveCacheToDisk = Executors.newSingleThreadScheduledExecutor();
            }
            serviceMoveCacheToDisk.scheduleAtFixedRate(new DoMoveCacheToDisk(), 1000, OSDConfig.getInstance().getCacheCheckInterval(), TimeUnit.MILLISECONDS);
        }
    }

    public static void startECThread() {
        if (serviceEC != null) {
            serviceEC.shutdownNow();
        } else {
            serviceEC = Executors.newSingleThreadScheduledExecutor();
        }
        logger.info("start ec thread, interval : {} ms", OSDConfig.getInstance().getECCheckInterval());
        serviceEC.scheduleAtFixedRate(new DoECPriObject(), 1000, OSDConfig.getInstance().getECCheckInterval(), TimeUnit.MILLISECONDS);
    }

    static class HookThread extends Thread {
		private static final Logger logger = LoggerFactory.getLogger(HookThread.class);
		
		@Override
		public void run() {
			// kill -TERM pid
			try {
                logger.info(OSDConstants.HOOK_THREAD_INFO);
                if (service != null) {
                    service.shutdown();
                    while (!service.isTerminated()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                OSDPortal.getInstance().postGWEvent(false);
			} catch (Exception e) {
				PrintStack.logging(logger, e);
			}
            logger.info(OSDConstants.STOP_KSAN_OSD);
		}
	}
}