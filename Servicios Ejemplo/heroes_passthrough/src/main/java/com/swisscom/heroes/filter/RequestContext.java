package com.swisscom.heroes.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class RequestContext {

	private final boolean initialised=false;
	private String cookie="";
	private String csrf="";

	public String getCookie() {
		return cookie;
	}
	public String getCSRF() {
		return csrf;
	}

	public void init(final HttpServletRequest request, final HttpServletResponse response) {
		if (initialised) {
			return;
		}
		final String valorCookie = request.getHeader(Header.COOKIE.getHeaderName());
		if (valorCookie != null) {
			this.cookie= valorCookie ;
		}
		final String valorCabecera = request.getHeader(Header.CSRF.getHeaderName());
		if (valorCabecera != null) {
			this.csrf= valorCabecera ;
		}
	}

}

