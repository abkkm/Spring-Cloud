## Configuration of the Circuit Breaker
We define a Circuit Breaker Registry with a Bean that returns `Resilience4JCircuitBreakerFactory`:

```java
@Bean
public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
	return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
			.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(3)).build())
			.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
			.build());
}
```

### Reactive

In the reactive case use `ReactiveResilience4JCircuitBreakerFactory`instead of `Resilience4JCircuitBreakerFactory`.

## Using the Circuit Breaker
In the controller we inject the Circuit Breaker Registry:

```java
@RestController
public class DemoController {
	private final CircuitBreakerFactory circuitBreakerFactory;
	private final HttpBinService httpBin;

	public DemoController(CircuitBreakerFactory circuitBreakerFactory, HttpBinService httpBinService) {
		this.circuitBreakerFactory = circuitBreakerFactory;
		this.httpBin = httpBinService;
	}
```

Now we are ready to use the Circuit Breaker:

```java
	@GetMapping("/delay/{seconds}")
	public Map delay(@PathVariable int seconds) {
		return circuitBreakerFactory.create("delay").run(httpBin.delaySuppplier(seconds), t -> {
			LOG.warn("delay call failed error", t);
			final Map<String, String> fallback = new HashMap<>();
			fallback.put("hello", "world");
			return fallback;
		});
	}
```

First we create a circuit breaker called "delay", and then we run "something":

```java
return circuitBreakerFactory.create("delay").run(
```

`run() has two arguments:
- what we want to run
- an optional failback, should the run fail

Here we are running:

```java
httpBin.delaySuppplier(seconds)
```

And the failback is logging a message, and returning a _fake_ response. In our case the method we run returns a `Supplier<Map>` so we need to return a `Map` with the failback:

```java
t-> {
		LOG.warn("delay call failed error", t);
		final Map<String, String> fallback = new HashMap<>();
		fallback.put("hello", "world");
		return fallback;
	}
```

