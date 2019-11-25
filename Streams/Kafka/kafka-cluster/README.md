## Docker-compose

Exponemos seis servicios:

```yml
services:
  zookeeper-1:
  zookeeper-2:
  zookeeper-3:
  kafka-1:
  kafka-2:
  kafka-3:
...

Los servicios de Kafka dependen de los de zookeeper:

```yml
  kafka-1:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper-1
      - zookeeper-2
      - zookeeper-3
```

La definicion del cluster de Zookeeper es bastante estandard:

```yml
  zookeeper-1:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_SERVER_ID: 1
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
      ZOOKEEPER_INIT_LIMIT: 5
      ZOOKEEPER_SYNC_LIMIT: 2
      ZOOKEEPER_SERVERS: zookeeper-1:2888:3888;zookeeper-2:2888:3888;zookeeper-3:2888:3888
    extra_hosts:
      - "moby:127.0.0.1"
    ports:
      - '2181'
```

- Mapeamos el puerto `2181` pero no especificamos que puerto se usara en el host. Docker asignara uno al azar
- En `ZOOKEEPER_SERVERS` hacemos referencia a cada uno de los nodos usando el nombre del servicio en docker-compose

La definicion de un nodo de Kafka:

```yml
  kafka-1:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper-1
      - zookeeper-2
      - zookeeper-3
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper-1:2181,zookeeper-2:2181,zookeeper-3:2181
      KAFKA_LISTENERS: PLAINTEXT://kafka-1:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-1:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    extra_hosts:
      - "moby:127.0.0.1"
    ports:
      - '29092:29092'
```

- Primero se tiene que crear el ensemble de Zookeeper
- exponemos solo el puerto `29092`, `39092`y `49092` respectivamente
- `KAFKA_ZOOKEEPER_CONNECT` hacemos referencia a cada nodo de Zookeeper usando el nombre del servicio en docker-compose
- Hemos definido dos listeners:
	- PLAINTEXT. Se usa este listener cuando un cliente accede al host `kafka-1` puerto `9092`
	- PLAINTEXT_HOST. Se usa este listener cuando un cliente accede al host `localhost` puerto `29092`. Este listener esta pensado para ser usado cuando llamamos desde el pc que hostea los contenedores de docker. Por este motivo hemos expuesto este puerto en el contenedor
- `KAFKA_ADVERTISED_LISTENERS` tenemos los metadatos que se devolveran al cliente para que este acceda al cluster. Notese como desde `localhost`, esto es, desde el host de Docker, usaremos el puerto `29092`
- La comunicacion interna entre los nodos del cluster usara el listener `PLAINTEXT` tal y como se indica en `KAFKA_INTER_BROKER_LISTENER_NAME`


## Arranca el cluster

Para arrancar el cluster

```sh
docker-compose up -d
```

Ver el estado:

```sh
docker-compose ps

           Name                        Command            State                      Ports
-------------------------------------------------------------------------------------------------------------
kafka-cluster_kafka-1_1       /etc/confluent/docker/run   Up      0.0.0.0:29092->29092/tcp, 9092/tcp
kafka-cluster_kafka-2_1       /etc/confluent/docker/run   Up      0.0.0.0:39092->39092/tcp, 9092/tcp
kafka-cluster_kafka-3_1       /etc/confluent/docker/run   Up      0.0.0.0:49092->49092/tcp, 9092/tcp
kafka-cluster_zookeeper-1_1   /etc/confluent/docker/run   Up      0.0.0.0:32779->2181/tcp, 2888/tcp, 3888/tcp
kafka-cluster_zookeeper-2_1   /etc/confluent/docker/run   Up      0.0.0.0:32777->2181/tcp, 2888/tcp, 3888/tcp
kafka-cluster_zookeeper-3_1   /etc/confluent/docker/run   Up      0.0.0.0:32778->2181/tcp, 2888/tcp, 3888/tcp
```

Comprobamos los logs para ver que todo esta en orden:

```sh
docker-compose logs zookeeper-1 | Select-String -Pattern "binding"

docker-compose logs zookeeper-2 | Select-String -Pattern "binding"

docker-compose logs zookeeper-3 | Select-String -Pattern "binding"

zookeeper-3_1  | [2019-11-25 04:14:34,068] INFO binding to port 0.0.0.0/0.0.0.0:2181
(org.apache.zookeeper.server.NIOServerCnxnFactory)
```

```sh
docker-compose logs kafka-1 | Select-String -Pattern "started"

docker-compose logs kafka-2 | Select-String -Pattern "started"

docker-compose logs kafka-3 | Select-String -Pattern "started"

kafka-3_1      | [2019-11-25 04:25:47,838] INFO [SocketServer brokerId=3] Started 2 acceptor threads for data-plane
(kafka.network.SocketServer)
kafka-3_1      | [2019-11-25 04:25:48,504] INFO [SocketServer brokerId=3] Started data-plane processors for 2 acceptors
(kafka.network.SocketServer)
kafka-3_1      | [2019-11-25 04:25:48,529] INFO [KafkaServer id=3] started (kafka.server.KafkaServer)
```

Para el docker compose:

```sh
docker-compose stop
```

## Comprueba el Cluster

Vamos a comprobar que todo esta operativo. Nos conectamos al nodo de Zookeeper para ver cual es su direccion:

```sh
docker-compose exec zookeeper-3 bash
```

Una vez conectados, vamos a ver cual es nuestra ip:

```sh
ip addr

1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN group default qlen 1
    link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
    inet 127.0.0.1/8 scope host lo
       valid_lft forever preferred_lft forever
2: tunl0@NONE: <NOARP> mtu 1480 qdisc noop state DOWN group default qlen 1
    link/ipip 0.0.0.0 brd 0.0.0.0
3: ip6tnl0@NONE: <NOARP> mtu 1452 qdisc noop state DOWN group default qlen 1
    link/tunnel6 :: brd ::
231: eth0@if232: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default
    link/ether 02:42:c0:a8:30:03 brd ff:ff:ff:ff:ff:ff
    inet 192.168.48.3/20 brd 192.168.63.255 scope global eth0
       valid_lft forever preferred_lft forever
```

Vale, ahora nos conectamos al nodo de Kafka:

```sh
docker-compose exec kafka-3 bash
```

Creamos el topic:

```sh
kafka-topics --create --topic foo --partitions 1 --replication-factor 1 --if-not-exists --zookeeper 192.168.48.3:2181       
```

Comprobamos que el topic se creo correctamente:

```sh
kafka-topics --describe --topic foo --zookeeper 192.168.48.3:2181

Topic:foo       PartitionCount:1        ReplicationFactor:1     Configs:
        Topic: foo      Partition: 0    Leader: 1       Replicas: 1     Isr: 1
```

Enviamos datos al topic:

```sh
seq 42 | kafka-console-producer --request-required-acks 1 --broker-list kafka-3:9092 --topic foo && echo 'Produced 42 messages.'
```

Leemos datos del topic:

```sh
kafka-console-consumer --bootstrap-server kafka-2:9092 --topic foo --from-beginning --max-messages 42
```