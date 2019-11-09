package com.euge.gw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
@EnableConfigurationProperties(UriConfiguration.class)
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder, UriConfiguration uriConfiguration) {
		final String httpUri = uriConfiguration.getHttpbin();
		return builder.routes()
				.route(p -> p
						.path("/get")
						.filters(f -> f.addRequestHeader("Hello", "World"))
						.uri(httpUri))
				/*There are some differences between this new route configuration and the previous one we created. For one, 
				 * we are using the host predicate instead of the path predicate. This means that as long as the host is hystrix.com 
				 * we will route the request to HTTPBin and wrap that request in a HystrixCommand. We do this by applying a filter to 
				 * the route. The Hystrix filter can be configured using a configuration object. In this example we are just giving 
				 * the HystrixCommand the name mycmd.
				 * */	        
				.route(p -> p
						.host("*.hystrix.com")
						.filters(f -> f.hystrix(config -> config.setName("mycmd").setFallbackUri("forward:/fallback")))
						.uri(httpUri))
				.build();
	}

	@RequestMapping("/fallback")
	public Mono<String> fallback() {
		return Mono.just("fallback");
	}

}
