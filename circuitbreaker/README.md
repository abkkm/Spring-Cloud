The patter to configure is the same for all the components. Here we will use the `CircuitBreaker`. The components are:

- CircuitBreaker
- ThreadPoolBulkhead
- RateLimiter
- Retry 


# 1. Create a Custom Configuration (optional)

```java
CircuitBreakerConfig  myconf = CircuitBreakerConfig.custom()
	.xxxxxxx
	.xxxxxxx
	.build();
```

# 2. Create a Registry

We can create it using the defaults:

```java
CircuitBreakerRegistry mycircuitBreakerRegistry  = CircuitBreakerRegistry.ofDefaults();
```

Or we can use our custom configuration:

```java
CircuitBreakerRegistry mycircuitBreakerRegistry = CircuitBreakerRegistry.of(myconf);
```

We can add a configuration on top of an existing registry:

```java
mycircuitBreakerRegistry.addConfiguration("myExtraConfiguration", myconfig);
```

# 3. Create a CircuitBreaker

We use the registry to create the CircuitBreaker. We name it and optionally provide a configuration - that will override any set that the registry may have:

Using the registry configuration:

```java
CircuitBreaker myCircuitBreaker1 =  mycircuitBreakerRegistry.circuitBreaker("name1");
```

Using an custom configuration - in our case is silly because we are using the same configuration the CircuitBreaker already has:

```java
CircuitBreaker myCircuitBreaker2 = mycircuitBreakerRegistry.circuitBreaker("name2", myconf);
```

# 4. Use the CircuitBreaker

## Decorated Supplier

We can create a Decorated Supplier. In this case we create one that returns a String:

```java
CheckedFunction0<String> myDecoratedSupplier = CircuitBreaker.decorateCheckedSupplier(
	myCircuitBreaker1, 
	() -> "This can be any method which returns an String);
```

We use `Try.of` to use the Decorated Supplier. See that we can chain functions - in this case a `map`.

```java
Try<String> resultado = Try.of(myDecoratedSupplier)
                .map(value -> value + ". Con un extra");
```

We then evaluate the response. We check if it has completed - `isSuccess()` - and then we get the returned value - `get()`:

```java
assertThat(resultado.isSuccess()).isTrue();
assertThat(resultado.get()).isEqualTo("This can be any method which returns an String. Con un extra");
```

### Recover

See that we can add a failback using `recover`:

```java
String resultado = Try.ofSupplier(myDecoratedSupplier)
	.map(value -> value + ". Con un extra");
    .recover(throwable -> "Hello from Recovery")
	.get(); 
```

## Use the functional interface directly

If we do not want to decorate our functional interface but call it directly - in this case we call `backendService::doSomething`:

```java
String resultado = myCircuitBreaker1.executeSupplier(backendService::doSomething);
```

# 5. Consume Events (optional)

Each objet has its own events that we can use to monitor changes of state and configuration: 

```java
mycircuitBreakerRegistry.getEventPublisher()
  .onEntryAdded(entryAddedEvent -> {
		CircuitBreaker addedCircuitBreaker = entryAddedEvent.getAddedEntry();
		LOG.info("CircuitBreaker {} added", addedCircuitBreaker.getName());
  })
  .onEntryRemoved(entryRemovedEvent -> {
		CircuitBreaker removedCircuitBreaker = entryRemovedEvent.getRemovedEntry();
		LOG.info("CircuitBreaker {} removed", removedCircuitBreaker.getName());
  });
```
