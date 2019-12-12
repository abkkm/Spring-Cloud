See how the Kafka Cluster is setup in the [Spring Stream showcase](../Streams/Kafka/kafka-cluster/README.md)


# Instalar (local)

En este ejercicio vamos a trabajar alternativamente con Kafka y con Rabbit.

Cuando usemos Kafka, el Docker Compose arranca los siguientes productos:

- Spring Cloud Data Flow Server
- Spring Cloud Skipper Server
- MySQL
- Apache Kafka (incluyendo Zookeeper)
- Prometheus
- Grafana

Cuando usemos Rabbit, el Docker Compose arrancara los siguientes productos:

- Spring Cloud Data Flow Server
- Spring Cloud Skipper Server
- MySQL
- RabbitMQ
- Prometheus
- Grafana

En la configuracion de docker, y mas especificamente, en la del dataflow server, especificamos una serie de variables de entornos que sirven para que cuando se desplieguen streams haya una serie de propiedades especificadas por defecto, y especificamente, propiedades relativas a la conectividad con el middleware - Kafka, Rabbit, o los que se usen:

```yml
  dataflow-server:
    image: springcloud/spring-cloud-dataflow-server:${DATAFLOW_VERSION:?DATAFLOW_VERSION is not set!}
    container_name: dataflow-server
    ports:
      - "9393:9393"
    environment:
      - spring.cloud.dataflow.applicationProperties.stream.spring.cloud.stream.kafka.binder.brokers=PLAINTEXT://kafka:9092
      - spring.cloud.dataflow.applicationProperties.stream.spring.cloud.stream.kafka.streams.binder.brokers=PLAINTEXT://kafka:9092
      - spring.cloud.dataflow.applicationProperties.stream.spring.cloud.stream.kafka.binder.zkNodes=zookeeper:2181
      - spring.cloud.dataflow.applicationProperties.stream.spring.cloud.stream.kafka.streams.binder.zkNodes=zookeeper:2181
      - spring.cloud.skipper.client.serverUri=http://skipper-server:7577/api
      - spring.cloud.dataflow.applicationProperties.stream.management.metrics.export.prometheus.enabled=true
      - spring.cloud.dataflow.applicationProperties.stream.spring.cloud.streamapp.security.enabled=false
      - spring.cloud.dataflow.applicationProperties.stream.management.endpoints.web.exposure.include=prometheus,info,health
      - spring.cloud.dataflow.grafana-info.url=http://localhost:3000
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/dataflow
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=rootpw
      - SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.mariadb.jdbc.Driver
```

Estas propiedades toman precedencia sobre cualquiera que pudiera haberse especificado en los `jars`.

Usando Data Flow para crear y desplegar los streams hace una serie de cosas por nosotros:

- Da nombre a los topicos/exchanges/queues que se utilizaran de entrada y salida
- Asegura que sources, producers y sinks sean consistentes, esto es, que los recursos usados como salida en un source sea el que se use de entrada en un producer, por ejemplo
- Configur brokers - Kafka - znodes - Zookeeper - y hosts - RabbitMQ.

De no usar Data Flow para desplegar, tendriamos que hacer esta configuracion manualmente - como hemos hecho en nuestras aplicaciones de ejemplo; Destacar que cuando desplegamos estas aplicaciones de ejemplo "manualmente", los recursos que se usan y la forma de conectarse al middleware es la indicada en el `application.yml`. Si desplegamos con Data Flow, estos recuersos, colas, exchanges y topics, toman otro nombre, el que les "da" Data Flow.

Vemos la configuracion de Docker con Kafka, y con Rabbit

## Instalacion con Kafka

En unix

```sh
export DATAFLOW_VERSION=2.2.1.RELEASE
export SKIPPER_VERSION=2.1.2.RELEASE

docker-compose up -d
```

En windows

```powershell
$Env:DATAFLOW_VERSION="2.2.1.RELEASE"
$Env:SKIPPER_VERSION="2.1.2.RELEASE"

docker-compose up -d
```
## Instalacion con Rabbit

En unix

```sh
export DATAFLOW_VERSION=2.2.1.RELEASE
export SKIPPER_VERSION=2.1.2.RELEASE

docker-compose -f ./docker-compose.yml -f ./docker-compose-rabbitmq.yml up -d
```

En windows

```powershell
$Env:DATAFLOW_VERSION="2.2.1.RELEASE"
$Env:SKIPPER_VERSION="2.1.2.RELEASE"

docker-compose -f ./docker-compose.yml -f ./docker-compose-rabbitmq.yml up -d
```

__Nota__: para parar:

```sh
docker-compose -f ./docker-compose.yml -f ./docker-compose-rabbitmq.yml down
```

# Spring Cloud Data Flow Dashboard

El dashboard puede abrirse [aqui](http://localhost:9393/dashboard/#/apps).

## Other URLs

Ver [detalles](https://docs.spring.io/spring-cloud-dataflow/docs/current/reference/htmlsingle/#api-guide-resources-runtime-information-applications).

El inventario de aplicaciones puede recuperarse en real time del Cloud Data Server utilizando [este endpoint](http://localhost:9393/runtime/apps).

# Arrancar el shell

```sh
docker exec -it dataflow-server java -jar shell.jar
```

# Cambiar Kafka por RabbitMQ

Ver [detalles](https://dataflow.spring.io/docs/installation/local/docker-customize/).

Lo que describo a continuacion sirve para convertir el docker-compose - que esta preconfigurado para Kafka -, en un docker-compose para Rabbit. El resultado es equivalente al que obtendriamos con la alternativa descrita en el aparatod de instalacion.

Borrar

```yml
kafka:
 image: confluentinc/cp-kafka:5.2.1
 ...
zookeeper:
 image: confluentinc/cp-zookeeper:5.2.1
 ....
```

Poner:

```yml
rabbitmq:
 image: rabbitmq:management-alpine
 expose:
   - '5672'
```
## Configuracion del servicio `dataflow-server` - en el docker-compose.yml

Añadir la siguiente entrada en las propiedades de entorno:

```yml
- spring.cloud.dataflow.applicationProperties.stream.spring.rabbitmq.host=rabbitmq
```

Borrar las siguientes entradas:

```yml
depends_on:
 - kafka
```

Añadir:

```yml
depends_on:
 - rabbitmq
```

## Bloque `app-import` - en el docker-compose.yml
Modificar la entrada `app-import` para reemplazar `https://dataflow.spring.io/kafka-maven-latest` con `https://dataflow.spring.io/rabbitmq-maven-latest`

# Cambiar Prometheus por InfluxDB

Para cambiar de Prometheus a InfluxDB, consultar [aqui](https://dataflow.spring.io/docs/installation/local/docker-customize/).

# Crear y ejecutar aplicaciones locales - en el host

Si desarrollamos aplicaciones que queremos ejecutar en local en la maquina que "hostea" docker, tenemos que configurar los volumnes y la configuracion en el docker-compose [ver](https://dataflow.spring.io/docs/installation/local/docker-customize/).