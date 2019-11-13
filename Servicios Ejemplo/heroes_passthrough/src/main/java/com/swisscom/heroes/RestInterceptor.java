package com.swisscom.heroes;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.swisscom.heroes.filter.Header;
import com.swisscom.heroes.filter.RequestContext;

public class RestInterceptor implements ClientHttpRequestInterceptor {
	private static final Logger logger = LoggerFactory.getLogger(RestInterceptor.class);

	private final RequestContext context;

	public RestInterceptor(final RequestContext context) {
		super();
		this.context = context;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		final HttpHeaders headers = request.getHeaders();
		if(!context.getCookie().isEmpty()) {
			logger.info("Forwarding the cookie value "+context.getCookie());
			headers.add(Header.COOKIE.getHeaderName(), context.getCookie());
		}
		if(!context.getCSRF().isEmpty()) {
			logger.info("Forwarding the header value "+context.getCSRF());
			headers.add(Header.CSRF.getHeaderName(), context.getCSRF());
		}
		return execution.execute(request, body);
	}
}