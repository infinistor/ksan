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
import java.net.URISyntaxException;

import com.pspace.ifs.ksan.gw.db.GWDB;
import com.pspace.ifs.ksan.gw.exception.GWException;
import com.pspace.ifs.ksan.gw.object.objmanager.ObjManagerHelper;
import com.pspace.ifs.ksan.gw.object.osdclient.OSDClientManager;
import com.pspace.ifs.ksan.gw.utils.GWConfig;
import com.pspace.ifs.ksan.gw.utils.GWUtils;
import com.pspace.ifs.ksan.gw.utils.PrintStack;

import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ProxyConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GW {
    private static final Logger logger = LoggerFactory.getLogger(GW.class);
    private GWConfig config;
    private Server server;
    private GWHandlerJetty handler;

    public GW(GWConfig config) {
        this.config = config;
    }

    public void init() throws GWException {
		try {
			config.configure();	
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		
		checkArgument(config.endpoint() != null || config.secureEndpoint() != null,
				"Must provide endpoint or secure-endpoint");
		
		if (config.endpoint() != null) {
			checkArgument(config.endpoint().getPath().isEmpty(),
					"endpoint path must be empty, was: %s",	config.endpoint().getPath());
		}
		
		if (config.secureEndpoint() != null) {
			checkArgument(config.secureEndpoint().getPath().isEmpty(),
					"secure-endpoint path must be empty, was: %s",
					config.secureEndpoint().getPath());
			requireNonNull(config.keyStorePath(), "Must provide keyStorePath with HTTPS endpoint");
			requireNonNull(config.keyStorePassword(), "Must provide keyStorePassword with HTTPS endpoint");
		}

		ExecutorThreadPool pool = new ExecutorThreadPool(config.jettyMaxThreads());
		pool.setName("S3");
		server = new Server(pool);

		// if (config.servicePath() != null && !config.servicePath().isEmpty()) {
		// 	ContextHandler context = new ContextHandler();
		// 	context.setContextPath(config.servicePath());
		// }

		// The HTTP configuration object.
		HttpConfiguration httpConfig = new HttpConfiguration();
		// Configure the HTTP support, for example:
		httpConfig.setSendServerVersion(false);

		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);
		HttpCompliance customHttpCompliance = HttpCompliance.from("RFC7230,MULTIPLE_CONTENT_LENGTHS");
		httpConnectionFactory.getHttpConfiguration().setHttpCompliance(customHttpCompliance);
		UriCompliance customUriCompliance = UriCompliance.from("RFC3986,-AMBIGUOUS_PATH_SEPARATOR");
		httpConnectionFactory.getHttpConfiguration().setUriCompliance(customUriCompliance);
		//httpConnectionFactory.getHttpConfiguration().setUriCompliance(UriCompliance.RFC3986);

		ServerConnector connector;
		if (config.endpoint() != null) {
			ProxyConnectionFactory httpProxyConnectionFactory = new ProxyConnectionFactory(httpConnectionFactory.getProtocol());
			connector = new ServerConnector(server, httpProxyConnectionFactory, httpConnectionFactory);
			connector.setHost(config.endpoint().getHost());
			connector.setPort(config.endpoint().getPort());
			
			// if(config.jettyMaxIdleTimeout() > 30000) {
				connector.setIdleTimeout(config.jettyMaxIdleTimeout());
			// }
			
			connector.setReuseAddress(true);
			server.addConnector(connector);
		} else {
			logger.info("endpoint is null");
		}

		if (config.secureEndpoint() != null) {
			SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
			sslContextFactory.setKeyStorePath(config.keyStorePath());
			sslContextFactory.setKeyStorePassword(config.keyStorePassword());
			connector = new ServerConnector(server, sslContextFactory, httpConnectionFactory);
			connector.setHost(config.secureEndpoint().getHost());
			connector.setPort(config.secureEndpoint().getPort());
			if(config.jettyMaxIdleTimeout() > 30000) {
				connector.setIdleTimeout(config.jettyMaxIdleTimeout());
			}

			connector.setReuseAddress(true);
			server.addConnector(connector);
		} else {
			logger.info("secureEndpoint is null");
		}

		handler = new GWHandlerJetty(config);
		server.setHandler(handler);

		GWDB s3DB = GWUtils.getDBInstance();
		try {
			s3DB.init(config.dbHost(), config.dbPort(), config.database(), config.dbUser(), config.dbPass(), config.dbPoolSize());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}

		try {
			OSDClientManager.getInstance().init(config.osdPort(), config.osdClientCount());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}

		try {
			ObjManagerHelper.getInstance().init(config.objManagerCount());
		} catch (Exception e) {
			PrintStack.logging(logger, e);
		}
	}

	public void start() throws Exception {
		server.start();
	}
	
	public void join() throws Exception {
		server.join();
	}

	public void stop() throws Exception {
		server.stop();
	}
}
