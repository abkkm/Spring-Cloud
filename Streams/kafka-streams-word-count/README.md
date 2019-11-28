```java
@Bean
public Function<KStream<Bytes, String>, KStream<Bytes, WordCount>> process() {

	return input -> input
			.flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
			.map((key, value) -> new KeyValue<>(value, value))
			.groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
			.windowedBy(TimeWindows.of(Duration.ofMillis(WINDOW_SIZE_MS)))
			.count(Materialized.as("WordCounts-1"))
			.toStream()
			.map((key, value) -> new KeyValue<>(null, new WordCount(key.key(), value, new Date(key.window().start()), new Date(key.window().end()))));
}
```

Se trata de una funcion, asi que tenemos dos canales, uno de entrada y otro de salida. La funcion se llama process, asi que los canales seran:
- process-in-0
- process-out-0

En el yml podemos ver cuales son los topics asociados a cada canal:

```yml
spring.cloud.stream:
  bindings:
    process-in-0:
      destination: words
    process-out-0:
      destination: counts
```

En el yml no hemos tenido que indicar que el binder es Kafka porque en las dependencias solamente hemos incluido Kafka - asi que spring en el classpath solo encontrara Kafka, y este sera el binder por defecto.

Podemos ver en el yml la configuracion de kafka - notese los brokers, y que hemos especificado un serde para key y values - sera usado por defecto cuando no se indique otra cosa:

```yml
spring.cloud.stream:
  kafka:
    streams:
      binder:
        applicationId: hello-word-count-sample
        configuration:
          commit.interval.ms: 100
          default:
            key.serde: org.apache.kafka.common.serialization.Serdes$StringSerde
            value.serde: org.apache.kafka.common.serialization.Serdes$StringSerde
		brokers: 10.0.75.1:29092,10.0.75.1:39092,10.0.75.1:49092            			
```

Volviendo a la definicion de la funcion:

1. Toma un `KStream<Bytes, String>`de entrada - topic words
2. Para generar un `KStream<Bytes, WordCount>` de salida - topic counts
3. Matiene la misma key, pero genera 0, 1 o n registros con cada registro. Toma el value, y genera una lista con cada Word

```java
.flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
```

4. Transforma cada registro, cambiando tanto key como valor - dara lugar a un repartitioning. Pone como key el valor, y mantiene el valor como valor

```java
.map((key, value) -> new KeyValue<>(value, value))
```			

5. Vamos a agregar, asi que tenemos que hacer un groupby antes. En este caso lo hacemos por Key, con lo que la Key se mantiene - y no se hace un repartitioning. Especificamos los serdes aunque esto seria redundante ya que estos son los serdes por defecto

```java
.groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
```			

6. Creamos una ventana de tiempo:

```java
.windowedBy(TimeWindows.of(Duration.ofMillis(WINDOW_SIZE_MS)))
```

7. Contamos. En este caso queremos guardar en un keystore los totales. Llamamos al keystore `WordCounts-1`

```java
.count(Materialized.as("WordCounts-1"))
```

8. El resultado de la agregacion seria una KTable. Lo convertimos en un stream

```java
.toStream()
```

9. Creamos el key pair que vamos a dirigir a la salida, que recordemos era de tipo `KStream<Bytes, WordCount>`. La key sera nula
.map((key, value) -> new KeyValue<>(null, new WordCount(key.key(), value, new Date(key.window().start()), new Date(key.window().end()))));
}


Para probarlo, arrancamos Kafka:

```sh
docker-compose up -d
```

Abrimos una sesion y arrancamos el productor:

```sh
docker-compose exec kafka-1 bash

kafka-console-producer --request-required-acks 1 --broker-list kafka-3:9092 --topic words
```

Abrimos una segunda sesion y arrancamos el consumidor:

```sh
docker-compose exec kafka-2 bash

kafka-console-consumer --bootstrap-server kafka-2:9092 --topic counts --from-beginning
```