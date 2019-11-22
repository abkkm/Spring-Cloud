package org.circuitbreaker.demo.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

@Configuration
public class CBConfig {
	private static final Logger log = LoggerFactory.getLogger(CBConfig.class);

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplateBuilder().build();
	}

	//Define un customizer de un CB
	@Bean
	public Customizer<Resilience4JCircuitBreakerFactory> defaultCBCustomizer() {
		return factory -> factory.configureDefault(
				id -> new Resilience4JConfigBuilder(id)
				.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
				//La duracion maxima tiene que ser 2 segundos
				.timeLimiterConfig(TimeLimiterConfig.custom()
						.timeoutDuration(Duration.ofSeconds(3)).build())
				.build());
	}

	@Bean
	public Customizer<Resilience4JCircuitBreakerFactory> slowCBCustomizer() {
		return factory -> {
			factory.configure(
					builder -> builder
					//Si durante 8 segundos recibimos al menos dos llamadas, y el 60% fallan. Pasamos a Half State despues de 10 segundos
					.circuitBreakerConfig(CircuitBreakerConfig.custom()
							.slidingWindow(8, 2, SlidingWindowType.TIME_BASED)
							.waitDurationInOpenState(Duration.ofSeconds(10))
							.failureRateThreshold(60)
							.build()
							)
					//La duracion maxima tiene que ser 1.5 segundos
					.timeLimiterConfig(TimeLimiterConfig.custom()
							.timeoutDuration(Duration.ofSeconds(2)).build())
					, "slow");

			factory.addCircuitBreakerCustomizer(circuitBreaker -> circuitBreaker.getEventPublisher()
					.onError(e -> log.error("error"))
					.onSuccess(e -> log.info("success"))
					,"slow");

		};
	}

}
