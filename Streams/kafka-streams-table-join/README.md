```java
@Bean
public BiFunction<KStream<String, Long>, KTable<String, String>, KStream<String, Long>> process() {

	return (userClicksStream, userRegionsTable) -> userClicksStream
			.leftJoin(userRegionsTable,
					(clicks, region) -> new RegionWithClicks(region == null ? "UNKNOWN" : region, clicks),
					Joined.with(Serdes.String(), Serdes.Long(), null))
			.map((user, regionWithClicks) -> new KeyValue<>(regionWithClicks.getRegion(), regionWithClicks.getClicks()))
			.groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
			.reduce((firstClicks, secondClicks) -> firstClicks + secondClicks)
			.toStream();
}
```

Dos cosas interesantes:

- Usamos una `BiFunction`. Esta es una funcion que tiene dos argumentos. Esto significa que tendremos dos streams de entrada y uno de salida
- Los dos argumentos de entrada son un `KStream` y un `KTable`. Con un `KTable` lo que estamos haciendo es crear un changelog a partir del stream. Cada key, value que se recibe es upserteado en la tabla. Si el valor es null, la entrada se elimina de la tabla. A diferencia de lo que sucede con una `GlobalKTable`, la informacion procede de aquellas particiones del topico a las que este conectada nuestra aplicacion. La aplicacion no vera todas las particiones si hay mas de una instancia corriendo
	
	
Si vemos el Application.yml:

```yml
spring.cloud.stream.bindings.process-in-0:
  destination: user-clicks
spring.cloud.stream.bindings.process-in-1:
  destination: user-regions
spring.cloud.stream.bindings.process-out-0:
  destination: output-topic
```

Vemos como el primer argumento de la funcion procede del topico `user-clicks` y que la `KTable` se construye con el topico `user-regions`.