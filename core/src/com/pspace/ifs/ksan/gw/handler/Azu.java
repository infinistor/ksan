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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ProxyConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pspace.ifs.ksan.gw.identity.AzuSharedKey;
import com.pspace.ifs.ksan.gw.utils.AzuConfig;
import com.pspace.ifs.ksan.gw.utils.AzuConstants;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.pspace.ifs.ksan.gw.utils.S3UserManager;
import com.pspace.ifs.ksan.libs.PrintStack;

public class Azu {
    private static final Logger logger = LoggerFactory.getLogger(Azu.class);
    private Server server;
    private AzuHandlerJetty handler;

    public Azu() {
    }

    public void configure() {
        AzuConfig.getInstance().configure();
    }

    public void init() {
        logger.info("azu start!");
        configure();

        ExecutorThreadPool pool = new ExecutorThreadPool(100);
		pool.setName("AZU");
		server = new Server(pool);

        // The HTTP configuration object.
		HttpConfiguration httpConfig = new HttpConfiguration();
		// Configure the HTTP support, for example:
		httpConfig.setSendServerVersion(false);

		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);
		HttpCompliance customHttpCompliance = HttpCompliance.from("RFC7230,MULTIPLE_CONTENT_LENGTHS");
		httpConnectionFactory.getHttpConfiguration().setHttpCompliance(customHttpCompliance);
		UriCompliance customUriCompliance = UriCompliance.from("RFC3986,-AMBIGUOUS_PATH_SEPARATOR");
		httpConnectionFactory.getHttpConfiguration().setUriCompliance(customUriCompliance);

        ProxyConnectionFactory httpProxyConnectionFactory = new ProxyConnectionFactory(httpConnectionFactory.getProtocol());
        ServerConnector connector  = new ServerConnector(server, httpProxyConnectionFactory, httpConnectionFactory);
        connector.setHost("0.0.0.0");
        connector.setPort(10000);
        
        connector.setIdleTimeout(60000);
        
        connector.setReuseAddress(true);
        server.addConnector(connector);

        handler = new AzuHandlerJetty();
        server.setHandler(handler);

        // shared key init
        while (S3UserManager.getInstance().getUserSet().isEmpty()) {
            logger.debug("wait s3user manager init ...");
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.error("", e);
            }
        }

        // File sharedKeys = new File(AzuConstants.SHARED_KEY_AUTH_PATH);
        // if (sharedKeys.exists()) {
        //     try (BufferedReader reader = new BufferedReader(new FileReader(sharedKeys))) {
        //         String line = null;
        //         while ((line = reader.readLine()) != null) {
        //             String[] keyInfo = line.split(":");
        //             AzuSharedKey key = new AzuSharedKey(keyInfo[0], keyInfo[1]);
        //             AzuSharedKeyManager.getInstance().addUser(key);
        //         }
        //     } catch (IOException e) {
        //         PrintStack.logging(logger, e);
        //     }
        // }
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