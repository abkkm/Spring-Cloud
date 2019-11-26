# Zookeeper

Para ver en detalle como administrar el Zookeeper acudir [aqui](https://zookeeper.apache.org/doc/r3.4.13/zookeeperAdmin.html)

## ZooKeeper Commands: The Four Letter Words

Podemos ver cual es el estado de un servidor en el cluster de Zookeeper conectandonos con `telnet´ o `nc`. Nos conectamos y luego tecleamos el comando `stat`:

```sh
nc 192.168.48.4 2181

stat
```

Retorna:

```sh
Zookeeper version: 3.4.14-4c25d480e66aadd371de8bd2fd8da255ac140bcf, built on 03/06/2019 16:18 GMT
Clients:
 /192.168.48.5:35518[1](queued=0,recved=221,sent=221)
 /192.168.48.6:40878[1](queued=0,recved=249,sent=249)
 /192.168.48.3:35228[0](queued=0,recved=1,sent=0)

Latency min/avg/max: 0/1/47
Received: 472
Sent: 471
Connections: 3
Outstanding: 0
Zxid: 0x30000013b
Mode: follower
Node count: 140
```

Otra forma de hacer lo mismo:

```sh
echo stat|nc 192.168.48.3 2181|grep Mode
Mode: follower
```

### Otros comandos

|Comando|Descripcion|
|-------|-------|
|conf|New in 3.3.0: Print details about serving configuration.|
|cons|New in 3.3.0: List full connection/session details for all clients connected to this server. Includes information on numbers of packets received/sent, session id, operation latencies, last operation performed, etc...|
|crst|New in 3.3.0: Reset connection/session statistics for all connections.|
|dump|Lists the outstanding sessions and ephemeral nodes. This only works on the leader.|
|envi|Print details about serving environment|
|ruok|Tests if server is running in a non-error state. The server will respond with imok if it is running. Otherwise it will not respond at all. A response of "imok" does not necessarily indicate that the server has joined the quorum, just that the server process is active and bound to the specified client port. Use "stat" for details on state wrt quorum and client connection information.|
|srst|Reset server statistics.|
|srvr|New in 3.3.0: Lists full details for the server.|
|stat|Lists brief details for the server and connected clients.|
|wchs|New in 3.3.0: Lists brief information on watches for the server.|
|wchc|New in 3.3.0: Lists detailed information on watches for the server, by session. This outputs a list of sessions(connections) with associated watches (paths). Note, depending on the number of watches this operation may be expensive (ie impact server performance), use it carefully.|
|wchp|New in 3.3.0: Lists detailed information on watches for the server, by path. This outputs a list of paths (znodes) with associated sessions. Note, depending on the number of watches this operation may be expensive (ie impact server performance), use it carefully.|
|mntr|New in 3.4.0: Outputs a list of variables that could be used for monitoring the health of the cluster.|


```sh
echo mntr | nc localhost 2181

zk_version  3.4.0
zk_avg_latency  0
zk_max_latency  0
zk_min_latency  0
zk_packets_received 70
zk_packets_sent 69
zk_outstanding_requests 0
zk_server_state leader
zk_znode_count   4
zk_watch_count  0
zk_ephemerals_count 0
zk_approximate_data_size    27
zk_followers    4                   - only exposed by the Leader
zk_synced_followers 4               - only exposed by the Leader
zk_pending_syncs    0               - only exposed by the Leader
zk_open_file_descriptor_count 23    - only available on Unix platforms
zk_max_file_descriptor_count 1024   - only available on Unix platforms
zk_fsync_threshold_exceed_count	0
```

## Zookeeper en Kafka

Podemos ver un video muy interesante [aqui](https://www.youtube.com/watch?v=3GVRhDT4jhs).

El cluster de Kafka es masterless, todos los brokers son iguales. Sin embargo, si bien es cierto que no hay un master, no es menos cierto que para definir un cluster se necesita una gestion, un overhead:

- Identificar cuales son los brokers disponibles en el cluster. El cluster Kafka puede tener cientos de brokers, que pueden incorporarse o desactivarse. En todo momento es necesario que esta disponible y que no
- Tareas Administrativas. Necesitamos saber que topics hay creados, que particiones tienen, que broker es el master en cada particion, elegir estos masters, reasignar particiones a brokers cuando algun broker muere, ...

Estas dos actividades se implementan utilizando Zookeeper:

- Cada broker sera un `znode` de tipo `efimero` en Zookeeper
- Todos los brokers activos eligiran quien actuare de `controller`. El `controller` se encargara de monitorizar los brokers, reasignar particiones, selecionar el master de cada particion...
- La informacion de los topics, particiones, etc. se mantiene en Zookeeper

### Consola de Zookeeper
Veamos todo esto en Zookeeper. Nos conectamos a un nodo de Zookeeper:

```sh
docker-compose exec zookeeper-1 bash
```

Abrimos la consola de Zookeeper:

```sh
zookeeper-shell localhost:2181

Connecting to localhost:2181
Welcome to ZooKeeper!
JLine support is disabled

WATCHER::

WatchedEvent state:SyncConnected type:None path:null
```

### Otros comandos de la Consola de Zookeeper

```sh
ZooKeeper -server host:port cmd args
        stat path [watch]
        set path data [version]
        ls path [watch]
        delquota [-n|-b] path
        ls2 path [watch]
        setAcl path acl
        setquota -n|-b val path
        history
        redo cmdno
        printwatches on|off
        delete path [version]
        sync path
        listquota path
        rmr path
        get path [watch]
        create [-s] [-e] path data acl
        addauth scheme auth
        quit
        getAcl path
        close
        connect host:port
```

# Kafka

Nos conectamos a la consola de Zookeeper:

```sh
docker-compose exec zookeeper-1 bash

zookeeper-shell localhost:2181

Connecting to localhost:2181
Welcome to ZooKeeper!
JLine support is disabled

WATCHER::

WatchedEvent state:SyncConnected type:None path:null
```

## Brokers

Podemos ver todas las jerarquias disponibles en el Zookeeper. Cada broker de Kafka se registra en la jerarquia `brokers`:

```sh
ls /

[cluster, controller_epoch, controller, brokers, zookeeper, admin, isr_change_notification, consumers, log_dir_event_notification, latest_producer_id_block, config]
```

Cada broker de Kafka se registra como un `znode` efimero, con un id unico - que hemos especificado en sus propiedades; ver docker-compose, `KAFKA_BROKER_ID`. En nuestro cluster tenemos tres brokers con ids 1, 2 y 3 - usamos el comando `ls`, como si fuera un filesystem:


```sh
ls /brokers

[ids, topics, seqid]
```

```sh
ls /brokers/ids

[1, 2, 3]
```

Estos nodos son efimeros. En el momento que el broker no envie el heartbeat a Zookeeper, el `znode` desaparecera. Notese que el `controller` establece un Watcher con los znodes en la jerarquia brokers, de modo que se entera de cualquier salida o entrada de brokers.

Podemos tambien ver la informacion de un znode con el comando `get`:

```sh
get /brokers/ids/1

{"listener_security_protocol_map":{"PLAINTEXT":"PLAINTEXT","PLAINTEXT_HOST":"PLAINTEXT"},"endpoints":["PLAINTEXT://kafka-1:9092","PLAINTEXT_HOST://localhost:29092"],"jmx_port":-1,"host":"kafka-1","timestamp":"1574676574001","port":9092,"version":4}
cZxid = 0x50000001c
ctime = Mon Nov 25 10:09:34 UTC 2019
mZxid = 0x50000001c
mtime = Mon Nov 25 10:09:34 UTC 2019
pZxid = 0x50000001c
cversion = 0
dataVersion = 1
aclVersion = 0
ephemeralOwner = 0x30004c65e450003
dataLength = 248
numChildren = 0```
```

### Los Brokers son efimeros

Vamos a hacer un experimento. Consultemos desde la consola el broker 1 - notese la opcion `watch`:

```sh
ls /brokers/ids/1 watch
[]
```

Si ahora paramos el broker 1:

```sh
docker-compose stop kafka-1
```

Esto es lo que sucede:

```sh
WATCHER::

WatchedEvent state:SyncConnected type:NodeDeleted path:/brokers/ids/1
```

y ademas podemos ver que:

```sh
ls /brokers/ids

[2, 3]
```

Si ahora lo arrancamos:

```sh
docker-compose start kafka-1
```

```sh
ls /brokers/ids

[1, 2, 3]
```

## Controller

Entre todos los brokers del cluster, uno es elegido como `Controller`. La eleccion es simplre, todos los brokers lo intentan, solo uno lo consigue. Intentarlo significa crear un `znode` efimero en Zookeeper llamado `/controller`. Como solo puede haber uno, todos los brokers recibiran una excepcion de Zookeper salvo uno - el mas rapido. El resto de brokers al recibir la excepcion lo que haran sera crear un `watch` de modo que en el instante que observen que el controler haya muerto - es efimero -, volveran a postularse como candidatos a ser el `controller` del cluster.

Podemos ver el controller:

```sh
get /controller

{"version":1,"brokerid":2,"timestamp":"1574676573544"}
cZxid = 0x500000013
ctime = Mon Nov 25 10:09:33 UTC 2019
mZxid = 0x500000013
mtime = Mon Nov 25 10:09:33 UTC 2019
pZxid = 0x500000013
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x30004c65e450002
dataLength = 54
numChildren = 0
```

Observerse como en este momento es el broker 2 el `controller`. Si lo matasemos:

```sh
docker-compose stop kafka-2
```

Podemos ver como el broker que actua de `controller` ha cambiado:

```sh
get /controller

{"version":1,"brokerid":3,"timestamp":"1574682097737"}
cZxid = 0x500000058
ctime = Mon Nov 25 11:41:37 UTC 2019
mZxid = 0x500000058
mtime = Mon Nov 25 11:41:37 UTC 2019
pZxid = 0x500000058
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x20004d05aac0000
dataLength = 54
numChildren = 0
```

## Alta disponibilidad

Vamos a demostrar la alta disponibilidad en Kafka. Para ello creamos un par de topics, con dos particiones y replication factor de tres. Nos conectamos con uno de los brokers de Kafka:

```sh
docker-compose exec kafka-1 bash
```

Vamos a crear los topics:


```sh
kafka-topics --create --topic foo --partitions 2 --replication-factor 3 --if-not-exists --zookeeper 192.168.80.3:2181

Created topic foo.

kafka-topics --create --topic bar --partitions 2 --replication-factor 3 --if-not-exists --zookeeper 192.168.80.3:2181

Created topic bar.

kafka-topics --create --topic gol --partitions 2 --replication-factor 2 --if-not-exists --zookeeper 192.168.80.3:2181

Created topic gol.
```

Comprobamos que se hayan creado bien:

```sh
kafka-topics --describe --topic foo --zookeeper 192.168.80.3:2181

Topic:foo       PartitionCount:2        ReplicationFactor:3     Configs:
        Topic: foo      Partition: 0    Leader: 3       Replicas: 3,1,2 Isr: 3,1,2
        Topic: foo      Partition: 1    Leader: 1       Replicas: 1,2,3 Isr: 1,2,3
		
kafka-topics --describe --topic bar --zookeeper 192.168.80.3:2181

Topic:bar       PartitionCount:2        ReplicationFactor:3     Configs:
        Topic: bar      Partition: 0    Leader: 3       Replicas: 3,2,1 Isr: 3,2,1
        Topic: bar      Partition: 1    Leader: 1       Replicas: 1,3,2 Isr: 1,3,2

kafka-topics --describe --topic gol --zookeeper 192.168.80.3:2181

opic:gol       PartitionCount:2        ReplicationFactor:2     Configs:
        Topic: gol      Partition: 0    Leader: 3       Replicas: 3,2   Isr: 3,2
        Topic: gol      Partition: 1    Leader: 1       Replicas: 1,3   Isr: 1,3
```

Podemos observar varias cosas aqui:

- foo tiene dos particiones. El lider de la particion 0 es el broker 3, y el de la particion 1 el broker 1
- Podemos ver en que brokers tenemos las copias de cada particion. La particion 0 tiene sus `Replicas`en 3, 1 y 2. La particion 1 tiene sus `Replicas`en 1, 2 y 3.
- Podemos tambien ver si las replicas estan sincronismo. En `Isr` podemos ver que estan In-Synchronism las tres copias de cada particion. La particion 0 tiene en sincronimo la copia guardada en los brokers 3, 2 y 1. La particion 1 tiene en sincronimo la copia guardada en los brokers 1, 3 y 2. 
- Hemos creado tambien un topic, - gol - con un factor de replicacion menor

Vamos a generar algunos datos:

```sh
seq 42 | kafka-console-producer --request-required-acks 1 --broker-list kafka-3:9092 --topic foo && echo 'Produced 42 messages.'
seq 42 | kafka-console-producer --request-required-acks 1 --broker-list kafka-3:9092 --topic bar && echo 'Produced 42 messages.'
seq 42 | kafka-console-producer --request-required-acks 1 --broker-list kafka-3:9092 --topic gol && echo 'Produced 42 messages.'
```

Podemos consumir los datos:

```sh
kafka-console-consumer --bootstrap-server kafka-2:9092 --topic foo --from-beginning --max-messages 42
kafka-console-consumer --bootstrap-server kafka-2:9092 --topic bar --from-beginning --max-messages 42
kafka-console-consumer --bootstrap-server kafka-2:9092 --topic gol --from-beginning --max-messages 42
```

### Paramos un broker

Si parasemos el broker 3, que es el leader de la particion 0 de los topics foo, y bar

```sh
docker-compose stop kafka-3
```

Veamos ahora como quedan los topics:

```sh
kafka-topics --describe --topic foo --zookeeper 192.168.80.3:2181

Topic:foo       PartitionCount:2        ReplicationFactor:3     Configs:
        Topic: foo      Partition: 0    Leader: 1       Replicas: 3,1,2 Isr: 1,2        
		Topic: foo      Partition: 1    Leader: 1 		Replicas: 1,2,3 Isr: 1,2
		
kafka-topics --describe --topic bar --zookeeper 192.168.80.3:2181

Topic:bar       PartitionCount:2        ReplicationFactor:3     Configs:
        Topic: bar      Partition: 0    Leader: 2       Replicas: 3,2,1 Isr: 2,1
        Topic: bar      Partition: 1    Leader: 1       Replicas: 1,3,2 Isr: 1,2

kafka-topics --describe --topic gol --zookeeper 192.168.80.3:2181

Topic:gol       PartitionCount:2        ReplicationFactor:2     Configs:
        Topic: gol      Partition: 0    Leader: 2       Replicas: 3,2   Isr: 2
        Topic: gol      Partition: 1    Leader: 1       Replicas: 1,3   Isr: 1
```

Vemos que:

- El lider de las particion 0 ya no es el 3, pero ha cambiado al 1 y al 2 en foo y en bar respectivamente
- La copia de la particion 3 deja de estar en sincronismo
- Para el topic gol no se buscan otros brokers, por lo que efectivamente estamos funcionando con una replica

Si arrancamos de nuevo Kafka-3:

```sh
docker-compose start kafka-3
```

Veamos ahora como quedan los topics:

```sh
kafka-topics --describe --topic foo --zookeeper 192.168.80.3:2181

Topic:foo       PartitionCount:2        ReplicationFactor:3     Configs:
        Topic: foo      Partition: 0    Leader: 1       Replicas: 3,1,2 Isr: 1,2,3
        Topic: foo      Partition: 1    Leader: 1       Replicas: 1,2,3 Isr: 1,2,3

kafka-topics --describe --topic bar --zookeeper 192.168.80.3:2181

Topic:bar       PartitionCount:2        ReplicationFactor:3     Configs:
        Topic: bar      Partition: 0    Leader: 2       Replicas: 3,2,1 Isr: 2,1,3
        Topic: bar      Partition: 1    Leader: 1       Replicas: 1,3,2 Isr: 1,2,3
```

Los lideres de cada particion no han cambiado, pero observese como el broker 3 vuelve a figurar en la lista de nodos sincronizados.

Una cosa mas. Mientras el nodo estaba parado hemos seguido enviando datos. Una vez se arranca el nodo de vuelta, Kafka sincronizara las particiones en el nodo 3 con el cluster. Podemos verlo aqui:

```sh
root@c5b9f3c4af70:/# kafka-topics --describe --topic gol --zookeeper 192.168.80.3:2181
Topic:gol       PartitionCount:2        ReplicationFactor:2     Configs:
        Topic: gol      Partition: 0    Leader: 2       Replicas: 3,2   Isr: 2
        Topic: gol      Partition: 1    Leader: 1       Replicas: 1,3   Isr: 1

root@c5b9f3c4af70:/# kafka-topics --describe --topic gol --zookeeper 192.168.80.3:2181
Topic:gol       PartitionCount:2        ReplicationFactor:2     Configs:
        Topic: gol      Partition: 0    Leader: 2       Replicas: 3,2   Isr: 2
        Topic: gol      Partition: 1    Leader: 1       Replicas: 1,3   Isr: 1

root@c5b9f3c4af70:/# kafka-topics --describe --topic gol --zookeeper 192.168.80.3:2181
Topic:gol       PartitionCount:2        ReplicationFactor:2     Configs:
        Topic: gol      Partition: 0    Leader: 2       Replicas: 3,2   Isr: 2
        Topic: gol      Partition: 1    Leader: 1       Replicas: 1,3   Isr: 1

root@c5b9f3c4af70:/# kafka-topics --describe --topic gol --zookeeper 192.168.80.3:2181
Topic:gol       PartitionCount:2        ReplicationFactor:2     Configs:
        Topic: gol      Partition: 0    Leader: 2       Replicas: 3,2   Isr: 2,3
        Topic: gol      Partition: 1    Leader: 1       Replicas: 1,3   Isr: 1,3
```

### Zookeeper

Vemos como queda esto reflejado en Zookeeper:

```sh
ls /brokers/topics

[bar, __confluent.support.metrics, __consumer_offsets, foo]
```

```sh
get /brokers/topics/foo

{"version":1,"partitions":{"1":[1,2,3],"0":[3,1,2]}}
cZxid = 0x500000076
ctime = Mon Nov 25 12:25:35 UTC 2019
mZxid = 0x500000076
mtime = Mon Nov 25 12:25:35 UTC 2019
pZxid = 0x500000078
cversion = 1
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 52
numChildren = 1
```

Podemos ver que el topic tiene dos particiones, que cada particion tiene tres copias, en que broker esta cada copia, y cual es el leader `preferido` - el primer broker de cada lista.

