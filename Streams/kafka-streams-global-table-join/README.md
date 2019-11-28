```java
@Bean
public BiFunction<KStream<String, Long>, GlobalKTable<String, String>, KStream<String, Long>> process() {

	return (userClicksStream, userRegionsTable) -> userClicksStream
			.leftJoin(userRegionsTable,
					(name,value) -> name,
					(clicks, region) -> new RegionWithClicks(region == null ? "UNKNOWN" : region, clicks)
					)
			.map((user, regionWithClicks) -> new KeyValue<>(regionWithClicks.getRegion(), regionWithClicks.getClicks()))
			.groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
			.reduce((firstClicks, secondClicks) -> firstClicks + secondClicks)
			.toStream();
}
```

Dos cosas interesantes:

- Usamos una `BiFunction`. Esta es una funcion que tiene dos argumentos. Esto significa que tendremos dos streams de entrada y uno de salida
- Los dos argumentos de entrada son un `KStream` y un `GlobalKTable`. Con un `GlobalKTable` lo que estamos haciendo es dos cosas:
	- Crear un changelog a partir del stream. Cada key, value que se recibe es upserteado en la tabla. Si el valor es null, la entrada se elimina de la tabla
	- Es global. Esto significa que una determinada aplicacion recibira la informacion de todas las particiones del topico
	
	
Si vemos el Application.yml:

```yml
spring.cloud.stream.bindings.process-in-0:
  destination: user-clicks
spring.cloud.stream.bindings.process-in-1:
  destination: user-regions
spring.cloud.stream.bindings.process-out-0:
  destination: output-topic
```

Vemos como el primer argumento de la funcion procede del topico `user-clicks` y que la `GlobalKTable` se construye con el topico `user-regions`.