```java
		@Bean
		public Consumer<KStream<String, DomainEvent>> aggregate() {

			final ObjectMapper mapper = new ObjectMapper();
			final Serde<DomainEvent> domainEventSerde = new JsonSerde<>( DomainEvent.class, mapper );

			return input -> input
					.groupBy(
							(s, domainEvent) -> domainEvent.getBoardUuid(),
							Grouped.with(null, domainEventSerde))
					.aggregate(
							String::new,
							(s, domainEvent, board) -> board.concat(domainEvent.getEventType()),
							Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("test-events-snapshots")
								.withKeySerde(Serdes.String())
								.withValueSerde(Serdes.String())
							);
		}
```

Un bean que retorna un Consumer. Se interpretara como un __Sink__ con un canal llamado __aggregate-in__. El __nombre del canal se deriva del nombre
de la funcion__. En Application.yml especificamos cual es el canal fisico, `bar`:


```yml
spring:
  cloud:
    stream:
      bindings:
        aggregate-in-0:
          destination: bar
```

En nuestro caso estamos usando Kafka, `bar` es un topic de Kafka. Podemos ver la configuracion de Kafka, los brokers, y especialmente el __serdes__ por defecto a utilizar para los key y value:

```yml
spring:
  cloud:
    stream:
      kafka:
        streams:
          binder:
            brokers: 10.0.75.1:29092,10.0.75.1:39092,10.0.75.1:49092
            configuration:
              default:
                key:
                  serde: org.apache.kafka.common.serialization.Serdes$StringSerde
                value:
                  serde: org.apache.kafka.common.serialization.Serdes$StringSerde
              commit.interval.ms: 1000
```

Analizando el bean, el nombre nos permite inferir el nombre del binding:

```java
		@Bean
		public Consumer<KStream<String, DomainEvent>> aggregate() {
```

En este caso estamos usando Kafka Streams. Queremos agregar, pero para agregar primero hay que hacer un groupby. Como no agrupamos por key no usaremos `GroupByKey`, sino `GroupBy`. Agrupamos por el `Uuid`. Como el valor es del tipo EventDomain, y el __serde__ por defecto es String, lo especificamos:

```java
return input -> input
		.groupBy(
				(s, domainEvent) -> domainEvent.getBoardUuid(),
				Grouped.with(null, domainEventSerde))
```

Despues de este paso tenemos un KGroupedStream y podemos pasar a agregar. Los resultados de la agregacion los iremos guardando en un Key-store llamado `test-events-snapshots`.

```java				
		.aggregate(
				String::new, //Inicializa. Este objeto es el que referimos mas adelante como board
				(s, domainEvent, board) -> board.concat(domainEvent.getEventType()), //Agregacion. Tomamos (s, domainEvent) y lo agregamos en board
				Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("test-events-snapshots")
					.withKeySerde(Serdes.String())
					.withValueSerde(Serdes.String() 
				)
				);
```

Finalmente, en el controller lo que vamos a hacer es consultar el key-store que hemos creado antes:

```java
@RestController
public class FooController {

	@Autowired
	private InteractiveQueryService interactiveQueryService;

	@RequestMapping("/events")
	public String events() {

		final ReadOnlyKeyValueStore<String, String> topFiveStore =
				interactiveQueryService.getQueryableStore("test-events-snapshots", QueryableStoreTypes.<String, String>keyValueStore());
		return topFiveStore.get("12345");
	}
}
```

## Producer

El producer lo ejecutamos por separado para generar datos