# Introduction

Consul nos ofrece varias capacidades:

- Registro de servicios
- Health
- Configuracion distribuida

El servidor de consul lo ejecutamos en una imagen (no sucede como en Eureka que podemos crear un MiSe con el servidor de Eureka).
El MiSe tiene que tener acceso a un __Agente__ que es quien realmente se conecta con Conul. El puerto usado por defecto para conectar con el agente es el 8500, pero se puede elegir otro:

```yml
spring:
  cloud:
    consul:
      #Donde encontrar Consul
      host: localhost
      port: 8500
```

## Registro de servicios

Para registrar servicios basta con incluir la dependencia. Cada MiSe entonces tiene que "contactar" con Consul - a traves del Agente - para informarle de su presencia.

Hay dos casos de uso:

- Registrar el MiSe. El MiSe pasa a ser registrado en Consul, de modo que puede ser invocado desde otros MiSe
- Discovery. El MiSe no se registra en Consul, pero es capaz de ver que servicios estan registrados en Consul - y llamarlos

El servicio puede usar uno u otro servicio - o ambos:

```yml
spring:
  cloud:
    consul:
      discovery:
        #Cada cuanto tiempo checkear los cambios en Consul
        catalog-services-watch-delay: 1000
        catalog-services-watch-timeout: 1000
        #Habilita el discovery
        enabled: true
        #Habilita el registro
        register: true
```

__Los servicios se registran con un nombre y con una instance id__. El __instance id__ tiene que ser __unico__ por cada instancia del servicio registrado. Por defecto el nombre de instancia se crea usando el service-name concatenado con el puerto del MiSe.

El nombre del servicio es el nombre logico que tenemos que usar en la uri para llamar al MiSe.

Podemos configurar el nombre del servicio y la instancia en el yml:

```yml
spring:
  cloud:
    consul:
      discovery:
        instance-id: ${spring.application.name}
        service-name: mi-prefijo-${spring.application.name}
        hostname: localhost
```

__Notese__ la propiedad `hostname`. Esta propiedad especifica cual es el hostname que se usara para hacer la invocacion al servicio. Si ponemos localhost, la imagen en docker lo resolvera como 127.0.0.1. Esa ip en la imagen no se corresponde con nuestra maquina, sino con la propia imagen. Ver la seccion de Health

### Registrar la Gestion como un MiSe separado

Si especificamos en el yml un puerto para la gestion:

```yml
management:
  server:
    port: 4452
```

El efecto sera que se registra un segundo MiSe que estara a cargo de la gestion.

### Tags

Podemos añadir metadatos al servicio que se visualizaran en Consul como tags:

```yml
spring:
  cloud:
    consul:
      discovery:
        tags:
        - foo=bar
        - nombre=prueba
```

### Watch

Periodicamente se comprueba con Consul que no haya habido cambios en el registro de servicios, y si los hubiera se creara un evento en el MiSe.

```yml
spring:
  cloud:
    consul:
      discovery:
        #Cada cuanto tiempo checkear los cambios en Consul
        catalog-services-watch-delay: 1000
        catalog-services-watch-timeout: 1000
```

## Health

Consul necesita comprobar periodicamente que los servicios registrados siguen activos. Para ello cada servicio tiene que exponer un endpoint - que sera invocado por Consul. El health check de una instancia de Consul se hace por defecto en el end-point `/health`, que por cierto, es tambien la ubicacion por defecto que utiliza `Actuator`.

Podemos habilitar el health check como sigue:

```yml
spring:
  cloud:
    consul:
      discovery:
        register-health-check: true        
		healthCheckInterval: 5s
        hostname: 10.0.75.1
```

__Importantisimo__, como esto ejecutando __Consul en Docker__, cuando Consul invoque a la api de health, si usa localhost no se esta refiriendo a mi maquina sino al propio host de Docker. Con la propiedad `hostname` podemos especificar el host que debe utilziar Docker.
En mi caso la ip que he informado es la que mi maquina tiene asignada en el `bridge`de docker.

La configuracion anterior es equivalente a:

```yml
spring:
  cloud:
    consul:
      discovery:
        register-health-check: true
        health-check-path: ${management.server.servlet.context-path}/health
        healthCheckInterval: 5s
        hostname: 10.0.75.1
```

Si quisieramos configurar otro end-point como health - y no reutilizar el que viene con `Actuator` -, por ejemplo `my-health-check`, tendriamos que configurar:

```yml
spring:
  cloud:
    consul:
      discovery:
        register-health-check: true
        health-check-path: /my-health-check
        healthCheckInterval: 5s
        hostname: 10.0.75.1
```

### Health con un management service

Si especificamos otro puerto para la gestion -en `Actuator` -, el efecto es que se crean dos servicios en Consul para cada MiSe: el propio MiSe, y su vertiente de gestion:

```yml
# Configurar Actuator
management:
  # Esto hace que en Consul se registre un segundo servicio. El Mise quedaria reflejado en dos servicios, uno que actua como el
  # propio MiSe y otro que hace las veces de gestion - health,..
  server:
    port: 4452
```

En este caso tenemos que especificar cual es la ruta de health, especificamente para indicar que el servlet donde se aloja el health - del actuator - es ahora diferente:

```yml
spring:
  cloud:
    consul:
        register-health-check: true
        health-check-path: ${management.server.servlet.context-path}/health
        healthCheckInterval: 5s
        hostname: 10.0.75.1
```

### Headers

Podemos aplicar headers a las peticiones de Health:

```yml
spring:
  cloud:
    consul:
      discovery:
        health-check-headers:
          X-Config-Token: 6442e58b-d1ea-182e-cfa5-cf9cddef0722
```

## Configuracion Distribuida

AL incluir la dependencia `spring-cloud-starter-consul-config` estamos habilitando la configuracion distribuida de Consul. La primera cosa
que tenemos que hacer es asegurar que movemos todas las propiedades que no dependeran de Consul al `bootstrap.yml`.

Las propiedades el MiSe las buscara en una ruta llamada:

```yml
/config/<MiSe Name>/key
```

Por ejemplo, en nuestro caso podriamos ir a la consola de Consul y crear un key pair:

- Key. Especificar como key: `/config/testConsulApp/sample/prop`
- Value. el valor de la propiedad.

Si observamos nuestro bean, vemos que la propiedad que esperamos es `sample.prop`:

```java
@ConfigurationProperties("sample")
@Data
public class SampleProperties {

  private String prop = "default value";

  public void setProp(String prop) {
    this.prop = prop;
  }

  public String getProp() {
    return prop;
  }
}

La configuracion tambien soporta profiles. Si configuramos un profile:

```yml
spring:
  profiles:
    active: dev
```

Por ejemplo, en nuestro caso podriamos ir a la consola de Consul y crear un key pair:

- Key. Especificar como key: `/config/testConsulApp,dev/sample/prop`
- Value. el valor de la propiedad para el `profile` dev.

Si nuestro profile fuera `dev` la propiedad `sample.prop` se tomaria desde este key, en caso contrario desde el key `/config/testConsulApp/sample/prop`

El orden de precedencia seria:

- `/config/testConsulApp,dev/`
- `/config/testConsulApp/`
- `/config/Application,dev/`
- `/config/Application/`

__Notese_ como hay una entrada llamada `Aplication`. Esto nos permitiria crear propiedades por defecto que aplicarian a varios MiSe.

### Actualizar la propiedad

Si cambiasemos en Consul el valor de una propiedad, el valor se refrescara automaticamente en nuestro MiSe. Esto es asi porque las propiedades tienen la anotacion `@RefreshScope`.

## Buscando Servicios

### Service Discovery

Podemos buscar los servicios registrados:

```java
@Autowired
private DiscoveryClient discoveryClient;
```

### Ribbon

Por defecto en la release Greenwich el Load Balancer que se usa es Netflix Ribbon - y el circuit breaker Netflix Hystrix. Si mirasemos las librerias incluidas en el jar veriamos Ribbon, incluso sino lo hemos incluido como dependencia. Esto cambiara con la
siguiente release, Hudson. Esto significa que cuando hacemos:

```java
@Autowired
private LoadBalancerClient loadBalancer;
```

o

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
  return new RestTemplate();
}
```

Lo que estamos usando es Ribbon

### Feign

Podemos usar tambien Feign como load balancer. En este caso tenemos que incluir la dependencia en el pom. Podemos usarlo de forma estandard:

```java
@FeignClient("testConsulApp")
public interface SampleClient {
  @RequestMapping(value = "/choose", method = RequestMethod.GET)
  String choose();
}
```

# Ejecutar Consul

Starts Consul:

```bash
docker-compose up -d
```
