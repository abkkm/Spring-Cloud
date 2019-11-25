Se trata de un docker-compose que implementa un Kafka con un solo nodo - y un solo node de Zookeeper. Utiliza la imagen de [Confluence](https://docs.confluent.io/4.0.0/installation/docker/docs/quickstart.html)

## Limpia contenedores

```sh
docker ps --all -q|%{docker rm -f $_}
```

## Arranca

Arrancamos las imagenes:

```sh
docker-compose up -d
```

Ver el estado:

```sh
docker-compose ps

            Name                         Command            State              Ports
------------------------------------------------------------------------------------------------
kafka-single-node_kafka_1       /etc/confluent/docker/run   Up      0.0.0.0:9092->9092/tcp
kafka-single-node_zookeeper_1   /etc/confluent/docker/run   Up      2181/tcp, 2888/tcp, 3888/tcp
```

Comprobamos los logs para ver que todo esta en orden:

```sh
docker-compose logs zookeeper | Select-String -Pattern "binding"

zookeeper_1  | [2019-11-25 01:48:59,723] INFO binding to port 0.0.0.0/0.0.0.0:2181 (org.apache.zookeeper.server.NIOServerCnxnFactory)
```

```sh
docker-compose logs kafka | Select-String -Pattern "started"

kafka_1      | [2019-11-25 01:49:02,707] INFO [SocketServer brokerId=1] Started 2 acceptor threads for data-plane
(kafka.network.SocketServer)
kafka_1      | [2019-11-25 01:49:02,975] DEBUG [ReplicaStateMachine controllerId=1] Started replica state machine with initial state
-> Map() (kafka.controller.ZkReplicaStateMachine)
kafka_1      | [2019-11-25 01:49:02,979] DEBUG [PartitionStateMachine controllerId=1] Started partition state machine with initial
state -> Map() (kafka.controller.ZkPartitionStateMachine)
kafka_1      | [2019-11-25 01:49:03,051] INFO [SocketServer brokerId=1] Started data-plane processors for 2 acceptors
(kafka.network.SocketServer)
kafka_1      | [2019-11-25 01:49:03,068] INFO [KafkaServer id=1] started (kafka.server.KafkaServer)
```

Para el docker compose:

```sh
docker-compose stop
```

## Probar

Vamos a comprobar que todo esta operativo. Nos conectamos al nodo de Zookeeper para ver cual es su direccion:

```sh
docker exec -it kafka-single-node_zookeeper_1 bash
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
219: eth0@if220: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default
    link/ether 02:42:c0:a8:10:02 brd ff:ff:ff:ff:ff:ff
    inet 192.168.16.2/20 brd 192.168.31.255 scope global eth0
       valid_lft forever preferred_lft forever
```

Vale, ahora nos conectamos al nodo de Kafka:

```sh
docker exec -it kafka-single-node_kafka_1 bash 
```

Creamos el topic:

```sh
kafka-topics --create --topic foo --partitions 1 --replication-factor 1 --if-not-exists --zookeeper 192.168.16.2:2181       
```

Comprobamos que el topic se creo correctamente:

```sh
kafka-topics --describe --topic foo --zookeeper 192.168.16.2:2181

Topic:foo       PartitionCount:1        ReplicationFactor:1     Configs:
        Topic: foo      Partition: 0    Leader: 1       Replicas: 1     Isr: 1
```

Enviamos datos al topic:

```sh
seq 42 | kafka-console-producer --request-required-acks 1 --broker-list kafka:29092 --topic foo && echo 'Produced 42 messages.'
```

Leemos datos del topic:

```sh
kafka-console-consumer --bootstrap-server kafka:29092 --topic foo --from-beginning --max-messages 42
```

Equivalente a lo anterior:

```sh 
docker exec kafka-single-node_kafka_1 kafka-topics --describe --topic foo --zookeeper 192.168.16.2:2181

docker exec kafka-single-node_kafka_1 bash -c "seq 42 | kafka-console-producer --request-required-acks 1 --broker-list kafka:29092 --topic foo && echo 'Produced 42 messages.'"

docker exec kafka-single-node_kafka_1 kafka-console-consumer --bootstrap-server kafka:29092 --topic foo --from-beginning --max-messages 42
```
```