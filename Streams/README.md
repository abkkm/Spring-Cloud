Arrancamos Docker, y luego arrancamos nuestro cluster de Kafka:

```sh
cd ./Kafka/kafka-cluster

docker-compose up -d

Starting kafka-cluster_zookeeper-3_1 ... done
Starting kafka-cluster_zookeeper-2_1 ... done
Starting kafka-cluster_zookeeper-1_1 ... done
Starting kafka-cluster_kafka-2_1     ... done
Starting kafka-cluster_kafka-3_1     ... done
Starting kafka-cluster_kafka-1_1     ... done
```

Averiguemos la ip de uno de los nodos de Zookeeper:

```sh
docker-compose exec zookeeper-3 bash
```

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
13: eth0@if14: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc noqueue state UP group default
    link/ether 02:42:c0:a8:50:02 brd ff:ff:ff:ff:ff:ff
    inet 192.168.80.2/20 brd 192.168.95.255 scope global eth0
       valid_lft forever preferred_lft forever
```	
	
Creamos el topico:

```sh	 
docker-compose exec kafka-3 bash
```

```sh	   
kafka-topics --create --topic foo --partitions 2 --replication-factor 2 --if-not-exists --zookeeper 192.168.80.2:2181       
kafka-topics --create --topic bar --partitions 2 --replication-factor 2 --if-not-exists --zookeeper 192.168.80.2:2181       
```

```sh
kafka-topics --describe --topic foo --zookeeper 192.168.80.2:2181

Topic:foo       PartitionCount:2        ReplicationFactor:3     Configs:
        Topic: foo      Partition: 0    Leader: 3       Replicas: 3,1,2 Isr: 3,2,1
        Topic: foo      Partition: 1    Leader: 1       Replicas: 1,2,3 Isr: 3,2,1
```		
		
