Para probar:

Nos conectamos:

```sh
docker-compose exec kafka-3 bash
```

Y creamos el productor:

```sh
kafka-console-producer --request-required-acks 1 --broker-list kafka-3:9092 --topic words
```

Empezamos a escribir, cuando escribamos `english` lo contabilizara en el topic `english-counts`, etc.

Podemos ver lo que llega al topic. Nos conectamos y luego abrimos el consumer:

```sh
docker-compose exec kafka-1 bash

kafka-console-consumer --bootstrap-server kafka-2:9092 --topic english-counts --from-beginning
kafka-console-consumer --bootstrap-server kafka-2:9092 --topic french-counts --from-beginning
kafka-console-consumer --bootstrap-server kafka-2:9092 --topic spanish-counts --from-beginning
```