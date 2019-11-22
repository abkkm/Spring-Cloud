Hystrix ha sido una de las primeras librerías en Java disponibles para implementar varios patrones de tolerancia a fallos en los microservicios. Desde hace un tiempo ha pasado a modo mantenimiento en el que ya no se incluyen nuevas características, una de las librerías recomendadas como sustituta es Resilience4j. Resilience4j proporciona las características similares con algunas ventajas adicionales.

Los patrones útiles para aumentar la tolerancia a fallos debido a problemas de red o fallo de alguno de los múltiples servicios son:

Circuit breaker: para dejar de hacer peticiones cuando un servicio invocado está fallando.
Retry: realiza reintentos cuando un servicio ha fallado de forma temporal.
Bulkhead: limita el número de peticiones concurrentes salientes a un servicio para no sobrecargarlo.
Rate limit: limita el número de llamadas que recibe un servicio en un periodo de tiempo.
Cache: intenta obtener un valor de la cache y si no está presente de la función de la que lo recupera.
Time limiter: limita el tiempo de ejecución de una función para no esperar indifinidamente a una respuesta.
Además de la funcionalidad de métricas.
La ventaja de Resilience4j es que todos estos patrones para los microservicios se ejecutan en el mismo hilo que el principal y no en un aparte como en Hystrix. Su uso además no requiere de crear clases específicas para hacer uso de los patrones y pueden emplearse las funciones lambda incorporadas en Java 8.

Este artículo actualiza el ejemplo que usaba Spring Boot y Spring Cloud que implementé en su momento para la serie de artículos sobre Spring Cloud añadiendo una implementación del cliente de un microservicio con Resilience4j.

La configuración de Resilience4j se puede proporcionar mediante código Java, con anotaciones y con la integración para Spring Boot con parámetros en el archivo de configuración de la aplicación. La aplicación además de varias cosas de Spring Cloud para otros artículos de la serie consiste para el de este artículo en un servicio cliente y un servicio servidor que para ser tolerantes a fallos hacen uso de los patrones circuit breaker y time limiter para demostrar su uso.

Resilience4j para implementar los patrones lo que hace es decorar la función objetivo que hace la invocación del servicio. Si se quieren aplicar varios patrones hay que encadenar las decoraciones, por ejemplo, si se quiere limitar el número de llamadas salientes con bulkhead y el patrón circuit breaker hay que aplicar una decoración sobre la otra. En este ejemplo se aplica un time limiter y un circuit breaker usando código Java. La variable get es la que realmente contiene la llamada al servicio.