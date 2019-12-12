Vamos a demostrar como utilizar Spring Streaming. Primero lo usaremos a pelo y en segundo lugar utilizando `Spring Data Flow`.

# Aplicaciones

Tenemos tres aplicaciones

- Source. Canal destino. Envia informacion
- Processor. Recibe informacion, la procesa, y la envia
- Sink. Canal fuete. Recive informacion

## Dependencias

Las tres aplicaciones usan las mismas dependencias. Podemos destacar las siguientes dependencias:

- Usamos __Spring Stream__

```xml
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-stream</artifactId>
		</dependency>
```

- Con el __binder de Kafka__

```xml		
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-stream-binder-kafka</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka</artifactId>
		</dependency>
```

- Incluimos soporte para Controlers, y Actuator

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
```

- Para el __testing__ usamos un helper para __Spring Stream__:

```xml
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-stream-test-support</artifactId>
			<scope>test</scope>
		</dependency>
```

- Y especificamente para el __testing__ del __binder de Kafka__:

```xml		
		<dependency>
			<groupId>org.springframework.kafka</groupId>
			<artifactId>spring-kafka-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>
```

## Conexion

En todos los application.yml especificaremos donde poder conectarnos con Kafka. Esta configuracion la hemos incluido especificamente para cubrir el caso en el que las aplicaciones se ejecutar a pelo, desde el pc local - no desde dentro de docker en una imagen. En la configuracion del kafka hemos "advertised" la ip del host local, que sera la que el adaptador de red le asigne al pc local en docker - en mi caso `10.0.75.1`. Ver [los detalles de la configuracion de Kafka](../Streams/Kafka/Zookeeper-Kafka.md).

```yml
  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: dataflow-kafka
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://:29092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://10.0.75.1:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - '29092:29092'
```

Notese como desde dentro de Docker kafka esta publicado en `kafka:9092`, pero desde el pc que hostea docker, kafka esta publicado en `10.0.75.1:29092`. En las aplicaciones incluiremos la siguiente entrada:

```yml
spring.cloud.stream.kafka.binder.brokers=10.0.75.1:29092
```

## Aplicacion `Source`

La aplicacion se llama `usage-detail-sender-kafka`.

Es una aplicacion Spring Stream que actua como destino `@EnableBinding(Source.class)`. Utiliza Spring Schedulling - `@EnableScheduling`

```java
@EnableScheduling
@EnableBinding(Source.class)
public class UsageDetailSender {
```

Spring inyecta la bean que gestiona el canal de destino:

```java
@Autowired
private Source source;
```

Periodicamente, cada segundo, enviamos informacion al destino - `@Scheduled(fixedDelay = 1000)`:

```java
@Scheduled(fixedDelay = 1000)
public void sendEvents() {
	final UsageDetail usageDetail = new UsageDetail();
	usageDetail.setUserId(this.users[new Random().nextInt(5)]);
	usageDetail.setDuration(new Random().nextInt(300));
	usageDetail.setData(new Random().nextInt(700));

	this.source.output().send(MessageBuilder.withPayload(usageDetail).build());
}
```

Notese como usamos la bean inyectada para enviar los datos:

```java
this.source.output().send(MessageBuilder.withPayload(usageDetail).build());
```

### Resources

Especificamos el nombre del topic donde la aplicacion escribira. No es preciso especificar el binder porque solo se usa Kafka - en el class path. Los datos de conexion con Kafka son los por defecto, por eso no se detallan:

```yml
spring.cloud.stream.bindings.output.destination=usage-detail
```

### Testing

Se inyectan dos beans:

```java
	@Autowired
	private MessageCollector messageCollector;

	@Autowired
	private Source source;
```

Con `MessageCollector` podemos capturar los mensajes que llega a un canal. 

Lo que haremos en nuestro caso de prueba es comprobar que a nuestro topic lleguen datos con una determinada frecuencia

- El canal a inspeccionar es `this.source.output()`
- Hay que inspeccionar cada segundo `poll(1, TimeUnit.SECONDS)`
- Y lo que inspeccionamos es este `MessageCollector`, `this.messageCollector.forChannel`

```java
final Message message = this.messageCollector.forChannel(this.source.output()).poll(1, TimeUnit.SECONDS);

		final String usageDetailJSON = message.getPayload().toString();
		assertTrue(usageDetailJSON.contains("userId"));
		assertTrue(usageDetailJSON.contains("duration"));
		assertTrue(usageDetailJSON.contains("data"));
```

## Aplicacion `Procesor`

La aplicacion se llama `usage-cost-processor-kafka`.

Es una aplicacion Spring Stream que actua como processor `@EnableBinding(Processor.class)`

```java
@EnableBinding(Processor.class)
public class UsageCostProcessor {
```

Anotamos el metodo `processUsageCost` para indicar que su input procede del canal de entrada - `@StreamListener(Processor.INPUT)` -, y su salida se escribe en el canal de salida - `@SendTo(Processor.OUTPUT)`:

```java
@StreamListener(Processor.INPUT)
@SendTo(Processor.OUTPUT)
public UsageCostDetail processUsageCost(UsageDetail usageDetail) {
	UsageCostDetail usageCostDetail = new UsageCostDetail();
	usageCostDetail.setUserId(usageDetail.getUserId());
	usageCostDetail.setCallCost(usageDetail.getDuration() * this.ratePerSecond);
	usageCostDetail.setDataCost(usageDetail.getData() * this.ratePerMB);
	return usageCostDetail;
}
```

### Resources

Especificamos el nombre del topic donde la aplicacion lee - que coincide con el topic donde nuestro `Source` escribio -, y el topic donde escribe:

```yml
spring.cloud.stream.bindings.input.destination=usage-detail
spring.cloud.stream.bindings.output.destination=usage-cost
```

### Testing

Se inyectan dos beans:

```java
	@Autowired
	private Processor processor;

	@Autowired
	private MessageCollector messageCollector;
```

Con `MessageCollector` podemos capturar los mensajes que llega a un canal. Con `processor` tendremos acceso a los canales de entrada y de salida.

Lo que haremos en nuestro caso de prueba es:

1. usar `el propio` procesor para averiguar cual es el canal de entrada, enviar algo a dicho canal - esta es la situacion de partida de nuestra aplicacion; Nuestra aplicacion "empieza" cuando alguien escribe en el canal de entrada -
2. Comprobar que el canal de salida tenemos los datos que nuestro processor debe producir. Para esto usaremos el `MessageCollector` 

```java
@Test
public void testUsageCostProcessor() throws Exception {
	this.processor.input().send(MessageBuilder.withPayload("{\"userId\":\"user3\",\"duration\":101,\"data\":502}").build());

	final Message message = this.messageCollector.forChannel(this.processor.output()).poll(1, TimeUnit.SECONDS);

	assertTrue(message.getPayload().toString().equals("{\"userId\":\"user3\",\"callCost\":10.100000000000001,\"dataCost\":25.1}"));
}
```

Prepara los datos de entrada:

```java
this.processor.input().send(MessageBuilder.withPayload("{\"userId\":\"user3\",\"duration\":101,\"data\":502}").build());
```

Comprueba que se escribe en el canal de salida - donde el processor que estamos testeando tiene que escribir

```java
final Message message = this.messageCollector.forChannel(this.processor.output()).poll(1, TimeUnit.SECONDS);
```

Verificamos que llegue lo que se supone debe llegar:

```java
assertTrue(message.getPayload().toString().equals("{\"userId\":\"user3\",\"callCost\":10.100000000000001,\"dataCost\":25.1}"));
```

## Aplicacion `Sink`

La aplicacion se llama `usage-cost-logger-kafka`.

Es una aplicacion Spring Stream que actua como Sink, `@EnableBinding(Sink.class)`

```java
@EnableBinding(Sink.class)
public class UsageCostLogger {
```

Anotamos el metodo `process` para indicar que su input procede del canal de entrada - `@StreamListener(Processor.INPUT)`.

```java
@StreamListener(Sink.INPUT)
public void process(UsageCostDetail usageCostDetail) {
	logger.info(usageCostDetail.toString());
}
```

### Resources

Especificamos el nombre del topic donde la aplicacion lee - que coincide con el topic donde nuestro `Procesor` escribio.

```yml
spring.cloud.stream.bindings.input.destination=usage-cost
```

### Testing

Se inyectan dos beans:

```java
	@Autowired
	protected Sink sink;

	@Autowired
	protected UsageCostLogger usageCostLogger;
```

`usageCostLogger` es nuestro Sink, nuestra implementacion. Tenemos que comprobar que cuando haya datos en el canal de entrada nuestra implementacion "sea disparada". "Disparada" significa que el metodo `process` de la bean `UsageCostLogger` se invoque, pasando como argumento de entrada la informacion publicada en el canal de entrada.

Vamos a espiar la bean `UsageCostLogger`, y especificamente el metodo `process`. Creamos el espia:

```java
@EnableAutoConfiguration
@EnableBinding(Sink.class)
static class TestConfig {

	// Override `UsageCostLogger` bean for spying.
	@Bean
	@Primary
	public UsageCostLogger usageCostLogger() {
		return spy(new UsageCostLogger());
	}
}
```

Un poco rebuscado - podriamos haber usado la anotacion `@SpyBean` sobre en lugar de `@Autowired`, pero eso es otra historia.

1. Creamos una nueva configuracion especifica para el test:

```java
@EnableAutoConfiguration
@EnableBinding(Sink.class)
static class TestConfig {
```

2. Reemplazamos la bean `usageCostLogger` por esta implementacion:

```java
@Bean
@Primary
public UsageCostLogger usageCostLogger() {
	return spy(new UsageCostLogger());
}
```

3. La implementacion no es otra cosa que el `Spy` de la clase. Esto es, estamos usando la implementacion real de nuestra vean, pero podemos ahora espiarla. Pasamos ahora a la definicion del caso de prueba:

```java
@Test
public void testUsageCostLogger() throws Exception {
	final ArgumentCaptor<UsageCostDetail> captor = ArgumentCaptor.forClass(UsageCostDetail.class);

	this.sink.input().send(MessageBuilder.withPayload("{\"userId\":\"user3\",\"callCost\":10.100000000000001,\"dataCost\":25.1}").build());

	verify(this.usageCostLogger).process(captor.capture());
}
```

4. Preparamos la entrada, enviado datos al canal de entrada:

```java
this.sink.input().send(MessageBuilder.withPayload("{\"userId\":\"user3\",\"callCost\":10.100000000000001,\"dataCost\":25.1}").build());
```

5. Verificamos que el metodo `process` se haya invocado:

```java
final ArgumentCaptor<UsageCostDetail> captor = ArgumentCaptor.forClass(UsageCostDetail.class);
verify(this.usageCostLogger).process(captor.capture());
```

Notese que como process tiene un argumento de entrada debemos definir un captor. El tipo es `UsageCostDetail.class`. Podriamos verificar no solo que el metodo `process` sea invocado, pero tambien el argumento pasado. Por ejemplo:

```java
assertTrue(captor.getValue().getUserId().compareTo("user3")==0);
```

# Ejecutar las aplicaciones

Vamos a ver dos formas de ejecutar las aplicaciones:

- Directamente, como aplicaciones individuales
- Usando Spring Dataflow

En ambos casos arrancamos nuestra configuracion de docker-compose:

```sh
docker-compose up -d
```

## Directamente

Lanzamos los jars

```sh
Start-Job -ScriptBlock {& java -jar usage-detail-sender-kafka-0.0.1-SNAPSHOT.jar}

Start-Job -ScriptBlock {& java -jar usage-cost-processor-kafka-0.0.1-SNAPSHOT.jar}

Start-Job -ScriptBlock {& java -jar usage-cost-logger-kafka-0.0.1-SNAPSHOT.jar}
```

Podemos ver como los tres jobs se estan ejecutando:

```sh
Get-Job

Id     Name            PSJobTypeName   State         HasMoreData     Location             Command
--     ----            -------------   -----         -----------     --------             -------
3      Job3            BackgroundJob   Completed     True            localhost            & java -jar usage-deta...
5      Job5            BackgroundJob   Completed     True            localhost            & java -jar usage-cost...
7      Job7            BackgroundJob   Completed     True            localhost            & java -jar usage-cost...
```

Nos conectamos a Zookeeper:

```sh
docker-compose exec zookeeper bash
```

Abrimos la consola:

```sh
root@08d8fbf75789:/# zookeeper-shell localhost:2181
Connecting to localhost:2181
Welcome to ZooKeeper!
JLine support is disabled

WATCHER::

WatchedEvent state:SyncConnected type:None path:null
```

Vemos que los dos topicos se han creado:

```sh
ls /brokers/topics

[usage-cost, __confluent.support.metrics, __consumer_offsets, usage-detail]
```

Conectemonos ahora al nodo de Kafka para mirar los topicos:

```sh
docker-compose exec kafka bash
```

Veamos el contenido del primer topic:

```sh
kafka-console-consumer --bootstrap-server kafka:9092 --topic usage-detail --from-beginning
```

Y el del segundo topic:

```sh
kafka-console-consumer --bootstrap-server kafka:9092 --topic usage-cost --from-beginning
```

## Usando Spring Data Flow

Ver como hacerlo [aqui](../UsingAppsInDataflow.md).