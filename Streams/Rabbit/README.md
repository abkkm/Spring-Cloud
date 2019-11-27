# RabitMQ

## Sin plugin de gestion

Arrancamos la imagen

```sh
docker run -d --hostname my-rabbit --name some-rabbit rabbitmq:alpine

docker logs some-rabbit
```

```sh
2019-11-27 11:14:32.028 [info] <0.261.0> Running boot step routing_ready defined by app rabbit
2019-11-27 11:14:32.028 [info] <0.261.0> Running boot step pre_flight defined by app rabbit
2019-11-27 11:14:32.028 [info] <0.261.0> Running boot step notify_cluster defined by app rabbit
2019-11-27 11:14:32.028 [info] <0.261.0> Running boot step networking defined by app rabbit
2019-11-27 11:14:32.031 [info] <0.591.0> started TCP listener on [::]:5672
2019-11-27 11:14:32.031 [info] <0.261.0> Running boot step cluster_name defined by app rabbit
2019-11-27 11:14:32.031 [info] <0.261.0> Running boot step direct_client defined by app rabbit
2019-11-27 11:14:32.281 [info] <0.8.0> Server startup complete; 0 plugins started.
```

Esta escuchando en el puerto por defecto, el 5672

## Con plugin de gestion

Arrancamos la imagen:

```sh
docker run -d --hostname my-rabbit --name some-rabbit -p 9080:15672 rabbitmq:management-alpine
```

Podemos abrir el plugin en `http//10.0.75.1:9080/`. Las credenciales son `guest`, `guest`. Si queremos usar otras credenciales:

```sh
docker run -d --hostname my-rabbit --name some-rabbit -p 9080:15672 -e RABBITMQ_DEFAULT_USER=user -e RABBITMQ_DEFAULT_PASS=password rabbitmq:management-alpine
```
