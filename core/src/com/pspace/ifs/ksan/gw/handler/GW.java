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
package com.pspace.ifs.ksan.gw.handler;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import javax.management.RuntimeErrorException;

import com.pspace.ifs.ksan.gw.exception.GWErrorCode;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagers;
// import com.pspace.ifs.ksan.gw.object.osdclient.OSDClientManager;
import com.pspace.ifs.ksan.libs.osd.OSDClientManager;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.utils.GWPortal;
import com.pspace.ifs.ksan.libs.PrintStack;

import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ProxyConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GW {
    private static final Logger logger = LoggerFactory.getLogger(GW.class);
    private Server server;
    private GWHandlerJetty handler;
	
    public GW() {
    }

	public void configure() throws Exception {
		GWPortal.getInstance().getConfig();
		GWPortal.getInstance().getObjManagerConfig();
		GWPortal.getInstance().getS3Users();
		GWPortal.getInstance().getS3Regions();
		GWPortal.getInstance().getDiskPoolsDetails();

		while (!GWPortal.getInstance().isAppliedDiskpools() || !GWPortal.getInstance().isAppliedUsers()) {
			try {
				Thread.sleep(1000);
				GWPortal.getInstance().getDiskPoolsDetails();
			} catch (InterruptedException e) {
				PrintStack.logging(logger, e);
			}
		}
	}

    public void init() throws Exception {
		
		try {
			configure();
		} catch (Exception e) {
			throw new GWException(GWErrorCode.SERVER_ERROR, null);
		}
		
		checkArgument(GWConfig.getInstance().getEndpoint() != null || GWConfig.getInstance().getSecureEndpoint() != null,
				GWConstants.LOG_GW_MUST_ENDPOINT);
		
		if (GWConfig.getInstance().getEndpoint() != null) {
			checkArgument(GWConfig.getInstance().getEndpoint().getPath().isEmpty(),
					GWConstants.LOG_GW_MUST_ENDPOINT_PATH,	GWConfig.getInstance().getEndpoint().getPath());
		}
		
		if (GWConfig.getInstance().getSecureEndpoint() != null) {
			checkArgument(GWConfig.getInstance().getSecureEndpoint().getPath().isEmpty(),
					GWConstants.LOG_GW_MUST_SECURE_ENDPOINT_PATH,
					GWConfig.getInstance().getSecureEndpoint().getPath());
			requireNonNull(GWConfig.getInstance().getKeyStorePath(), GWConstants.LOG_GW_MUST_KEYSTORE_PATH);
			requireNonNull(GWConfig.getInstance().getKeyStorePassword(), GWConstants.LOG_GW_MUST_KEYSTORE_PASSWORD);
		}

		ExecutorThreadPool pool = new ExecutorThreadPool((int)GWConfig.getInstance().getJettyMaxThreads());
		pool.setName(GWConstants.GW);
		server = new Server(pool);

		// if (GWConfig2.getInstance().servicePath() != null && !GWConfig2.getInstance().servicePath().isEmpty()) {
		// 	ContextHandler context = new ContextHandler();
		// 	context.setContextPath(GWConfig2.getInstance().servicePath());
		// }

		// The HTTP configuration object.
		HttpConfiguration httpConfig = new HttpConfiguration();
		// Configure the HTTP support, for example:
		httpConfig.setSendServerVersion(false);
		SecureRequestCustomizer src = new SecureRequestCustomizer();
		src.setSniHostCheck(false);
		httpConfig.addCustomizer(src);

		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);
		HttpCompliance customHttpCompliance = HttpCompliance.from(GWConstants.LOG_GW_RFC7230);
		httpConnectionFactory.getHttpConfiguration().setHttpCompliance(customHttpCompliance);
		UriCompliance customUriCompliance = UriCompliance.from(GWConstants.LOG_GW_RFC3986);
		httpConnectionFactory.getHttpConfiguration().setUriCompliance(customUriCompliance);

		ServerConnector connector;
		if (GWConfig.getInstance().getEndpoint() != null) {
			ProxyConnectionFactory httpProxyConnectionFactory = new ProxyConnectionFactory(httpConnectionFactory.getProtocol());
			connector = new ServerConnector(server, httpProxyConnectionFactory, httpConnectionFactory);
			logger.debug("host={}, port={}", GWConfig.getInstance().getEndpoint().getHost(), GWConfig.getInstance().getEndpoint().getPort());
			connector.setHost(GWConfig.getInstance().getEndpoint().getHost());
			connector.setPort(GWConfig.getInstance().getEndpoint().getPort());
			
			if (GWConfig.getInstance().getJettyMaxIdleTimeout() > 30000) {
				connector.setIdleTimeout(GWConfig.getInstance().getJettyMaxIdleTimeout());
			}
			
			connector.setReuseAddress(true);
			server.addConnector(connector);
		} else {
			logger.info(GWConstants.LOG_GW_ENDPOINT_IS_NULL);
		}

		if (GWConfig.getInstance().getSecureEndpoint() != null) {
			SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
			sslContextFactory.setKeyStorePath(GWConfig.getInstance().getKeyStorePath());
			sslContextFactory.setKeyStorePassword(GWConfig.getInstance().getKeyStorePassword());
			connector = new ServerConnector(server, sslContextFactory, httpConnectionFactory);
			connector.setHost(GWConfig.getInstance().getSecureEndpoint().getHost());
			connector.setPort(GWConfig.getInstance().getSecureEndpoint().getPort());
			if (GWConfig.getInstance().getJettyMaxIdleTimeout() > 30000) {
				connector.setIdleTimeout(GWConfig.getInstance().getJettyMaxIdleTimeout());
			}

			connector.setReuseAddress(true);
			server.addConnector(connector);
		} else {
			logger.info(GWConstants.LOG_GW_SECURE_ENDPOINT_IS_NULL);
		}

		handler = new GWHandlerJetty();
		server.setHandler(handler);

		// try {
		// 	OSDClientManager.getInstance().init((int)GWConfig.getInstance().getOsdPort(), (int)GWConfig.getInstance().getOsdClientCount());
		// } catch (Exception e) {
		// 	PrintStack.logging(logger, e);
		// }

		ObjManagers.getInstance().init();

		if (GWConfig.getInstance().isCacheDiskpath()) {
			GWUtils.initCache(GWConfig.getInstance().getCacheDiskpath());
		}
		// long start = System.currentTimeMillis();
		GWUtils.initDisk();
		// long end = System.currentTimeMillis();
		// logger.error("disk init times : {} ms", end - start);
		
		GWUtils.initEC();
	}

	public void start() throws Exception {
		server.start();
	}
	
	public void join() throws Exception {
		server.join();
	}

	public void stop() throws Exception {
		server.stop();
		// OSDClientManager.getInstance().close();
	}
}
