package org.circuitbreaker.demo;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpBinService {

	private final RestTemplate rest;

	public HttpBinService(RestTemplate rest) {
		this.rest = rest;
	}

	public Map get() {
		return rest.getForObject("https://httpbin.org/get", Map.class);

	}

	public Map delay(int seconds) {
		return rest.getForObject("https://httpbin.org/delay/" + seconds, Map.class);
	}

	public Supplier<Map> delaySuppplier(int seconds) {
		return () -> this.delay(seconds);
	}
}
