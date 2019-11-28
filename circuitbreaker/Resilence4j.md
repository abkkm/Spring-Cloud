# Resilence4J

Resilence4j offers a number of components all of them in the area of _resilence_. The pattern to configure the components is the same for all. Here we will use the `CircuitBreaker`, but the same tactic can be used for the rest. The components are:

- CircuitBreaker
- ThreadPoolBulkhead
- RateLimiter
- Retry 

We will detail the steps to configure the components

# 1. Create a Custom Configuration (optional)

Optional step when the default configuration of the components does not cover our needs:

```java
CircuitBreakerConfig  myconf = CircuitBreakerConfig.custom()
	.xxxxxxx
	.xxxxxxx
	.build();
```

# 2. Create a Registry

Next we have to create a Registry. We can create the Registry in two different ways, depending on whether we are going to use the default configuration or one custom - see previous step.

## Using Defaults

Using the defaults:

```java
CircuitBreakerRegistry mycircuitBreakerRegistry  = CircuitBreakerRegistry.ofDefaults();
```

## Custom configuration

Using the custom configuration created earlier:

```java
CircuitBreakerRegistry mycircuitBreakerRegistry = CircuitBreakerRegistry.of(myconf);
```

The configuration __could also be added__ to an existing Registry. Its like adding a new layer on top of the existing Registry configuration:

```java
mycircuitBreakerRegistry.addConfiguration("myExtraConfiguration", myconfig);
```

# 3. Create a CircuitBreaker

Finally we use the Registry to create the component. In this case it is a CircuitBreaker. 

We name it and optionally provide a configuration - that will override any set that the registry may have. Again, we have to options:

- Create the component with the configuration defined in the Registry
- Use a custom configuration "on top" of the configuration defined in the Registry

In both cases we are giving the component a name. This is crucial, because this would be used in the service to select the Component - CircuitBreaker - to use. We can have several Components defined and use one or another depending on our needs.

## Using the registry configuration:

```java
CircuitBreaker myCircuitBreaker1 =  mycircuitBreakerRegistry.circuitBreaker("name1");
```

## Using an custom configuration 

In this example it is a redundant because the configuration we are setting does not add anything "new" to the Registry configuration. It is show here for ilustration purposes:

```java
CircuitBreaker myCircuitBreaker2 = mycircuitBreakerRegistry.circuitBreaker("name2", myconf);
```

# 4. Use the CircuitBreaker

Finally it is time to use the Component - CircuitBreaker in this case. There are two syntaxis:

- Decorating the "service". The component is added as a wrapper that intercepts the requests adding the behaviour defined in the Component
- We use the Component to invoke the service

Both are ways to do the same, wrap around the service with a Component that adds a behaviour to the service - transparently to the service implementation.

## Decorated Supplier

We can create a Decorated Supplier. In this case we create one that returns a String. Two steps have to be follow:
1. Create the decorated supplier - consumer, ...
2. Invoke the decorated supplier - consumer, ...

### Create the Decorated Supplier - Consumer ...

We take any suplier, or lambda, and using the method `decorateCheckedSupplier` of the Component, we create a __decorated version of the function__. When we call this decorated component, we will be wrapping the function with the definition of the Component. In our example the Component is a Circuit Breaker:

```java
CheckedFunction0<String> myDecoratedSupplier = CircuitBreaker.decorateCheckedSupplier(
	myCircuitBreaker1, 
	() -> "This can be any method which returns an String);
```

### Call the Decorated Supplier - Consumer ...

We use `Try.of` to use the Decorated Supplier. This method allow us to chaing functions - in this case a `map`.

```java
Try<String> resultado = Try.of(myDecoratedSupplier)
                .map(value -> value + ". Con un extra");
```

We then evaluate the response. We check if it has completed - `isSuccess()` - and then we get the returned value - `get()`:

```java
assertThat(resultado.isSuccess()).isTrue();
assertThat(resultado.get()).isEqualTo("This can be any method which returns an String. Con un extra");
```

#### Recover

See that we can add a failback using `recover`:

```java
String resultado = Try.ofSupplier(myDecoratedSupplier)
	.map(value -> value + ". Con un extra");
    .recover(throwable -> "Hello from Recovery")
	.get(); 
```

## Use the functional interface directly

If we do not want to decorate our functional interface but call it directly - in this case we call `backendService::doSomething` using the methods available in the Component - `executeSupplier`:

```java
String resultado = myCircuitBreaker1.executeSupplier(backendService::doSomething);
```

# 5. Consume Events (optional)

Each objet has its own events that we can use to monitor changes of state and configuration - for example changes of status in the CircuitBreaker, ...: 

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

# 6. Combine Several Components

We can use more than one Component at a time when calling a given function. For example, lets imagine we want to use a `CircuitBreaker`, `Retry` and `Bulkhead`. We define these components as described above:

```java
CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendService");

Retry retry = Retry.ofDefaults("backendService");

Bulkhead bulkhead = Bulkhead.ofDefaults("backendService");
```

Let´s now see the Supplier we want to decorate:

```java
Supplier<String> supplier = () -> backendService.doSomething(param1, param2)
```

We create the decorated version:

```java
Supplier<String> decoratedSupplier = Decorators.ofSupplier(supplier)
  .withRetry(retry)
  .withCircuitBreaker(circuitBreaker)
  .withBulkhead(bulkhead)
  .decorate();
```

Now that we have the decorated supplier, we can invoke it:

```java
String result = Try.ofSupplier(decoratedSupplier)
					.recover(throwable -> "Hello from Recovery")
					.get();
```