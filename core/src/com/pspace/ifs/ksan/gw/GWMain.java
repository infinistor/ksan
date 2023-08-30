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
package com.pspace.ifs.ksan.gw;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.FileReader;

import com.pspace.ifs.ksan.gw.handler.GW;
import com.pspace.ifs.ksan.gw.handler.Azu;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.HeartbeatManager;
import com.pspace.ifs.ksan.libs.PrintStack;
import com.pspace.ifs.ksan.libs.config.AgentConfig;
import com.pspace.ifs.ksan.gw.utils.GWPortal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.constants.platform.RLIMIT;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.util.Platform;

public class GWMain {
	private static GW gw;
	
	private static final Logger logger = LoggerFactory.getLogger(GWMain.class);

	private GWMain() {
		throw new AssertionError(GWConstants.INTENTIONALLY_NOT_IMPLEMENTED);
	}

	public static void main(String[] args) throws Exception {
		POSIX posix = POSIXFactory.getPOSIX();
		System.setErr(createLoggerErrorPrintStream());

		if (Platform.IS_LINUX) {
			posix.setrlimit(RLIMIT.RLIMIT_NOFILE.intValue(), 50000L, 50000L);
		}

		Runtime.getRuntime().addShutdownHook(new HookThread());
		setSystemConfiguration();
		writePID();

		// setting timezone, locale 
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		Locale.setDefault(Locale.KOREA);

		logger.info("GWMain Started.");
		Thread thread = new Thread() {
			@Override
			public void run() {
				Azu azu = new Azu();
				try {
					azu.init();
					azu.start();
					azu.join();
					logger.error("Stop ksan azu ...");
				} catch (IllegalStateException e) {
					logger.error(e.getMessage());
					System.exit(1);
				} catch (Exception e) {
					logger.error(e.getMessage());
					System.exit(1);
				}
			}
		};
		thread.start();

		gw = new GW();

		try {
			gw.init();
			gw.start();
			gw.join();
			logger.error(GWConstants.STOP_KSAN_GW);
		} catch (IllegalStateException e) {
			PrintStack.logging(logger, e);
			System.exit(1);
		} catch (Exception e) {
			PrintStack.logging(logger, e);
			System.exit(1);
		}
	}

	private static PrintStream createLoggerErrorPrintStream() {
		return new PrintStream(System.err) {
			private final StringBuilder builder = new StringBuilder();

			@Override
			public void print(final String string) {
				logger.error(string);
			}

			@Override
			public void write(byte[] buf, int off, int len) {
				for (int i = off; i < len; ++i) {
					char ch = (char) buf[i];
					if (ch == GWConstants.CHAR_NEWLINE) {
						if (builder.length() != 0) {
							print(builder.toString());
							builder.setLength(0);
						}
					} else {
						builder.append(ch);
					}
				}

				logger.error(builder.toString());
			}
		};
	}

	static class HookThread extends Thread {
		private static final Logger logger = LoggerFactory.getLogger(HookThread.class);
		
		@Override
		public void run() {
			// kill -TERM pid
			logger.warn(GWConstants.HOOK_THREAD_INFO);
			try {
				gw.stop();
				GWPortal.getInstance().postGWEvent(false);
			} catch (Exception e) {
				PrintStack.logging(logger, e);
			}
		}
	}

	public static void writePID() {
		
        // File file = new File(GWConstants.PID_PATH);
		File file = new File(System.getProperty(Constants.PID_KEY) + File.separator + Constants.PID_FILE);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8)) {
				Long pid = ProcessHandle.current().pid();

				logger.debug(GWConstants.LOG_GW_PID, pid);
				fw.write(String.valueOf(pid));
				fw.flush();
			} catch (IOException e) {
				logger.error(e.getMessage());
				System.exit(-1);
			}
        } catch (IOException e) {
            logger.error(e.getMessage());
            System.exit(-1);
        }
    }

	private static void setSystemConfiguration() {
		System.setProperty(Constants.SERVICEID_KEY, Constants.SERVICEID_DIR);
		System.setProperty(Constants.CONFIG_KEY, Constants.CONFIG_DIR);
		System.setProperty(Constants.OBJMANAGER_CONFIG_KEY, Constants.OBJMANAGER_CONFIG_DIR);
		System.setProperty(Constants.PID_KEY, Constants.PID_DIR);
		System.setProperty(Constants.AGENT_CONF_KEY, Constants.AGENT_CONF_DIR);
		System.setProperty(Constants.DISKPOOL_CONF_KEY, Constants.DISKPOOL_CONF_DIR);

	} 
}