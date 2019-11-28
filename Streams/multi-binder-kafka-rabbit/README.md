Para probar:

## RabitMQ

Arrancamos Rabitt

```sh
docker run -d --hostname my-rabbit --name some-rabbit -p 9080:15672 -p 5672:5672 rabbitmq:management-alpine
```

Podemos abrir el plugin en `http//10.0.75.1:9080/`. Las credenciales son `guest`, `guest`. Si queremos usar otras credenciales:

## Kafka

```sh
docker-compose up -d
```

Nos conectamos:

```sh
docker-compose exec kafka-3 bash
```

Y creamos el productor:

```sh
kafka-console-producer --request-required-acks 1 --broker-list kafka-3:9092 --topic dataIn
```

Empezamos a escribir, cuando escribamos `english` lo contabilizara en el topic `english-counts`, etc.

Podemos ver lo que llega al topic. Nos conectamos y luego abrimos el consumer:

```sh
docker-compose exec kafka-1 bash

kafka-console-consumer --bootstrap-server kafka-2:9092 --topic dataOut --from-beginning
```