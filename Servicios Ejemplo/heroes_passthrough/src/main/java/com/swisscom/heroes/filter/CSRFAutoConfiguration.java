package com.swisscom.heroes.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CSRFAutoConfiguration {

    private static final int ORDER = 2;

    @Bean
    @RequestScope
    public RequestContext requestContext() {
        return new RequestContext();
    }

    @Bean
    public FilterRegistrationBean<CSRFFilter> trackingFilterRegistrar(final RequestContext oceRequestContext) {
        final FilterRegistrationBean<CSRFFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CSRFFilter(oceRequestContext));
        registration.setOrder(ORDER);
        return registration;
    }

}
