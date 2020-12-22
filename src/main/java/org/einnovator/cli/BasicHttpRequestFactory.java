
/*
 * Copyright (C) 2020 EInnovator (support@einnovator.org) All rights reserved.
 */

package org.einnovator.cli;

import java.io.IOException;
import java.util.Base64;
import java.util.function.Supplier;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Custom {@link HttpComponentsClientHttpRequestFactory} for BASIC authorization and custom connection settings.
 */
public class BasicHttpRequestFactory extends HttpComponentsClientHttpRequestFactory implements InitializingBean, DisposableBean {

	protected Supplier<Credentials> supplier;
	
	public BasicHttpRequestFactory(Supplier<Credentials> supplier) {
		this.supplier = supplier;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		final HttpClientBuilder builder = HttpClients.custom().addInterceptorLast(new HttpRequestInterceptor() {
			@Override
			public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
				Credentials credentials = supplier.get();
				String auth = makeAuthorization(credentials.getUserPrincipal().getName(),credentials.getPassword());
				request.addHeader(HttpHeaders.AUTHORIZATION, auth);
			}
		});
		//builder.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(true).build());
		this.setHttpClient(this.configureHttpClient(builder).build());
	}
	
	protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder) {
		return builder;
	}
	
	
	protected String makeAuthorization(String username, String password){
		byte[] bytes = Base64.getEncoder().encode(new String(username + ":" + password).getBytes());
		String authHeader = "Basic " + new String(bytes);
		return authHeader;
	}
}

