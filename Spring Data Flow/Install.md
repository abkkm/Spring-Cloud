# Instalar (local)

El Docker Compose arranca los siguientes productos:

- Spring Cloud Data Flow Server
- Spring Cloud Skipper Server
- MySQL
- Apache Kafka (incluyendo Zookeeper)
- Prometheus
- Grafana

## Instalacion

En unix

```sh
export DATAFLOW_VERSION=2.2.1.RELEASE
export SKIPPER_VERSION=2.1.2.RELEASE
docker-compose up -d
```

En wondows

```powershell
$Env:DATAFLOW_VERSION="2.2.1.RELEASE"
$Env:SKIPPER_VERSION="2.1.2.RELEASE"
docker-compose up -d
```

## Spring Cloud Data Flow Dashboard

El dashboard puede abrirse [aqui](http://localhost:9393/dashboard/#/apps).

### Other URLs

Ver [detalles](https://docs.spring.io/spring-cloud-dataflow/docs/current/reference/htmlsingle/#api-guide-resources-runtime-information-applications).

El inventario de aplicaciones puede recuperarse en real time del Cloud Data Server utilizando [este endpoint](http://localhost:9393/runtime/apps).

## Arrancar el shell

```sh
docker exec -it dataflow-server java -jar shell.jar
```

## Cambiar Kafka por RabbitMQ

Ver [detalles](https://dataflow.spring.io/docs/installation/local/docker-customize/).

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
### Configuracion del servicio `dataflow-server` - en el docker-compose.yml

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

### Bloque `app-import` - en el docker-compose.yml
Modificar la entrada `app-import` para reemplazar `https://dataflow.spring.io/kafka-maven-latest` con `https://dataflow.spring.io/rabbitmq-maven-latest`

## Cambiar Prometheus por InfluxDB

Para cambiar de Prometheus a InfluxDB, consultar [aqui](https://dataflow.spring.io/docs/installation/local/docker-customize/).

## Crear y ejecutar aplicaciones locales - en el host

Si desarrollamos aplicaciones que queremos ejecutar en local en la maquina que "hostea" docker, tenemos que configurar los volumnes y la configuracion en el docker-compose [ver](https://dataflow.spring.io/docs/installation/local/docker-customize/).