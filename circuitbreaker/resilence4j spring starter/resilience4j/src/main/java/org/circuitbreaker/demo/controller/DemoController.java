package org.circuitbreaker.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.circuitbreaker.demo.service.HttpBinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

	Logger LOG = LoggerFactory.getLogger(DemoController.class);

	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

	private final HttpBinService httpBin;

	public DemoController(CircuitBreakerFactory<?, ?> circuitBreakerFactory, HttpBinService httpBinService) {
		this.circuitBreakerFactory = circuitBreakerFactory;
		this.httpBin = httpBinService;
	}

	@GetMapping("/get")
	public Map<?, ?> get() {
		return httpBin.get();
	}

	//Una llamada de un segundo no falla
	@GetMapping("/delay/{seconds}")
	public Map<?, ?> delay(@PathVariable int seconds) {
		return circuitBreakerFactory.create("slow").run(
				httpBin.delaySuppplier(seconds), 
				t -> {
					LOG.warn("failback", t);
					final Map<String, String> fallback = new HashMap<>();
					fallback.put("hello", "world");
					return fallback;
				});
	}

	//Una llamada de un segundo falla
	@GetMapping("/retraso/{seconds}")
	public Map<?, ?> retraso(@PathVariable int seconds) {
		return circuitBreakerFactory.create("normal").run(
				httpBin.delaySuppplier(seconds), 
				t -> {
					LOG.warn("failback", t);
					final Map<String, String> fallback = new HashMap<>();
					fallback.put("hello", "world");
					return fallback;
				});
	}
}
