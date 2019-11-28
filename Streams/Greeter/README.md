Demuestra un uso clasico de Spring Cloud Streams

- Definimos las Operaciones
- Implementamos un Bean con cada operacion
- Implementamos la logica de la aplicacion


## Operaciones

Creamos una interface con todas las operaciones:

```java
public interface GreetingsStreams {
	String INPUT = "greetings-in";
	String OUTPUT = "greetings-out";

	@Input(INPUT)
	SubscribableChannel inboundGreetings();

	@Output(OUTPUT)
	MessageChannel outboundGreetings();
}
```

En este caso tenemos dos operaciones, una de entrada y otra de salida. La de entrada es asincrona - callback - pero podria haber sido una operacion polled. En el Application.yml definiremos `greetings-in`y `greetings-out`. En Kafka el primero indica un consumer group llamado `greetings-in` y el segundo un productor con client-id `greetings-out`. En el yml podemos especificar el binder - en este caso no lo hacemos porque en el classpath solo tenemos Kafka, asi que tomara este por defecto; En casos multibinder - ver el ejemplo de Kafka y Rabitt - se tendra que especificar -, el topic:

```yml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: 10.0.75.1:29092,10.0.75.1:39092,10.0.75.1:49092
      bindings:
        greetings-in:
          destination: foo
          contentType: application/json
        greetings-out:
          destination: foo
          contentType: application/json
```

Antes de terminar de hablar de las Operaciones, indicar que por defecto se incluyen tres interfaces:

- __Sink.class__. Implementa un canal de entrada
- __Source.class__. Implementa un canal de salida
- __Producer.class__.  Implementa un canal de entrada y salida

## Implementar las Operaciones

Bastara con que incluyamos la anotacion que sigue:

```java
@EnableBinding(GreetingsStreams.class)
@SpringBootApplication
public class StreamsApplication {
```

Para que spring inyecte una bean que implemente los metodos de nuestra interface. Asi por ejemplo, para enviar datos al canal:

```java
@Service
public class GreetingsService {

	private final GreetingsStreams greetingsStreams;

	public GreetingsService(GreetingsStreams greetingsStreams) {
		this.greetingsStreams = greetingsStreams;
	}

	public void sendGreeting(final Greetings greetings) {

		final MessageChannel messageChannel = greetingsStreams.outboundGreetings();

		messageChannel.send(MessageBuilder
				.withPayload(greetings)
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
				.build());
	}
}
```

En el constructor inyectamos una Bean que implementa nuestra interface:

```java
public GreetingsService(GreetingsStreams greetingsStreams) {
		this.greetingsStreams = greetingsStreams;
	}
```

Para enviar datos ya podemos usar la bean para acceder al canal - y a sus metodos:

```java
final MessageChannel messageChannel = greetingsStreams.outboundGreetings();

messageChannel.send(MessageBuilder
		.withPayload(greetings)
		.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
		.build());
```

Para escuchar:

```java
@StreamListener(GreetingsStreams.INPUT)
public void handleGreetings(@Payload Greetings greetings) {
	System.out.println("Received greetings: "+greetings);
}
```

La anotacion directamente nos extrae los datos. Cada vez que aparezca un key pair por el stream se llamara a este metodo. La opcion de leer usando pooling - sincrona - seria paracida a la de enviar. Usariamos nuestra bean para acceder al canal, llamariamos al metodo `poll`, y si hay datos los recuperariamos. Podemos planificar la frecuencia con la que hacer poll usando @Schedulled