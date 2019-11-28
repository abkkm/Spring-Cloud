# Introduccion

Vamos a ver dos casos de uso para Vault:

- Vault como Configuration Store
- Vault como proveedror de secretos

# Vault como Configuration Store

## Definimos las propiedades de la aplicacion

Podemos inyectar los secretos de vault como si se tratara de una configuracion distribuida "mas" - del tipo de `Config Server` o `Consul`. La aplicacion no require una logica especial - solo configurar en `bootstrap.yml` vault. Lo que es el codigo propiamente dicho trata las propiedades de forma "estandard" sin ninguna referencia especial a vault.

Especificamos que `MyConfiguration.class` define propiedades de spring boot:

```java
@SpringBootApplication
@EnableConfigurationProperties(MyConfiguration.class)
public class VaultApplication implements CommandLineRunner {
```

`MyConfiguration.class` es una clase estandard. Aqui estamos definiendo propiedades como `example.username`, `example.password` y `example.alias`.

```java
@ConfigurationProperties("example")
public class MyConfiguration {

	private String username;

	private String password;

	private String alias;
```

Las propiedades las va a buscar primero en Vault, y luego en el Application.yml - bueno, en environment variables, prpiedades de JVM, etc.; El punto es que al incluir el starter vault en las dependencias, se incluye vault en la "ecuacion".

Para demostrar este principio, en vault vamos a guardar - ahora veremos como - `username` y `password`, pero __no__ alias, asi que `alias` lo tomara del yml:

```yml
example:
  password: pass
  # username: egsmartin
  alias: jefe supremo
```

## Creamos las propiedades en Vault

Vamos a dar valor a estas propiedades en `vault`. Vamos a crear dos juegos de secretos, uno que usaremos cuando no haya ´profile´ especificado, y otro con el `profile` prueba - funciona como `Consul`; No en vano ambos son productos de Hashicorp:

```sh
vault kv put secret/vault-sample/ example.username=demouser example.password=demopassword

vault kv put secret/vault-sample/prueba example.username=prueba-user example.password=prueba-password
```

Notese como hemos creado un secreto llamado `secret/vault-sample/` y otro llamado `secret/vault-sample/prueba`. Diseccionemos los secretos:

- ´secret´. Es el path que hemos definido en vault asociado a un kv store - ver en el siguiente apartado como configuramos vault en el `bootstrap.yml`
- `vault-sample` es el nombre de nuestra aplicacion - ver en el siguiente apartado como configuramos vault en el `bootstrap.yml`
- `prueba`. Hace referencia al `profile`. Si nuestra aplicacion tiene este profile buscara en primer lugar la resolucion de las proiedades aqui, y solo sino las encuentra en `secret/vault-sample/`.

Vemos los secretos en vault:

```sh
vault kv get secret/vault-sample/prueba

Key                 Value
---                 -----
example.password    prueba-password
example.username    prueba-user
```

```sh
vault kv get secret/vault-sample                             

========== Data ==========
Key                 Value
---                 -----
example.password    demopassword
example.username    demouser
```

Notese como no hemos configurado `example.alias`.

## Setup de Vault

En las dependencias hemos incluido:

```yml
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

Una vez hemos incluido `spring-cloud-starter-vault-config` la aplicacion va a buscar en el `vault` las propiedades que podamos encontrar - como sucede con `Consul`. La configuracion del `vault` debe especificarse en el `bootstrap.yml`:

```yml
spring:
  application:
    name: vault-sample
  profiles:
    active:
    - prueba
  cloud:
    vault:
      # sets the hostname of the Vault host. The host name will be used for SSL certificate validation
      host: www.gz.com
      port: 8200
      scheme: https
      # configure the Vault endpoint with an URI. Takes precedence over host/port/scheme configuration      
      uri: https://www.gz.com:8200 
      connection-timeout: 5000
      read-timeout: 15000
      # sets the order for the property source
      config:
        order: -10
      authentication: token
      token: s.dremNR6XWrPRj1gQrQwlyrWN
      kv:
        enabled: true
		# Es el valor por defecto
        backend: secret
        # Es el valor por defecto
        profile-separator: '/'
        # Es el valor por defecto
        default-context: application
        # Es el valor por defecto (el nombre de la aplicacion)
        application-name: vault-sample
```

Hemos configurado el `uri` del vault como `https://www.gz.com:8200` que usa __https__. Tendremos que cargar el __certificado de este site__, el `www.gz.com` - mas adelante en este documento definimos como configurar el `truststore`.

```yml
    vault:
      # sets the hostname of the Vault host. The host name will be used for SSL certificate validation
      host: www.gz.com
      port: 8200
      scheme: https
      # configure the Vault endpoint with an URI. Takes precedence over host/port/scheme configuration      
      uri: https://www.gz.com:8200 
      connection-timeout: 5000
      read-timeout: 15000
```

Estamos definiendo que queremos usar un `kv` engine:

```yml
      kv:
        enabled: true
		# Es el valor por defecto
        backend: secret
        # Es el valor por defecto
        profile-separator: '/'
        # Es el valor por defecto
        default-context: application
        # Es el valor por defecto (el nombre de la aplicacion)
        application-name: vault-sample
```

El `kv` engine usa como path `secret`. El nombre de la aplicacion es `vault-sample`, y por defecto el contexto sera `application`. ¿Esto que siginifica?. Pondamos un ejemplo. Supongamos que el `profile` fuera prueba. Los secretos se iran buscando en este orden de precedencia:

1. `/secret/vault-sample/prueba`
2. `/secret/vault-sample/`
3. `/secret/application/prueba`
4. `/secret/application/`

Esto es: 

1. `{backend}/{application-name}/{profile}`
2. `{backend}/{application-name}`
1. `{backend}/{default-context}/{profile}`
1. `{backend}/{default-context}`

Si en alguno de estos paths tenemos los secretos `example.username`, `example.password`, o `example.alias`, la aplicacion los leera de vault.

Por ultimo hemos indicado que la __authenticacion__ es de tipo `token`, y hemos especificado el token a utilizar. 

```yml
      authentication: token
      token: s.dremNR6XWrPRj1gQrQwlyrWN
```

Hay [otras formas](https://cloud.spring.io/spring-cloud-static/spring-cloud-vault/2.2.0.RC2/reference/html/) de autenticacion con vault. Voy a destacar aqui `appid`:

### Autenticacion con `Appid`

Con `appid` tenemos dos variantes:

- Usando la `ip` de la aplicacion - del NIC que usa la aplicacion para llamar a Vault
- Usando la `MAC` address de la aplicacion - del NIC que usa la aplicacion para llamar a Vault

#### Usando la ip

Con la ip la configuracion seria como sigue:

```yml
spring:
  application:
    name: vault-sample
  profiles:
    active:
    - prueba
  cloud:
    vault:
      # sets the hostname of the Vault host. The host name will be used for SSL certificate validation
      host: www.gz.com
      port: 8200
      scheme: https
      # configure the Vault endpoint with an URI. Takes precedence over host/port/scheme configuration      
      uri: https://www.gz.com:8200 
      connection-timeout: 5000
      read-timeout: 15000
      # sets the order for the property source
      config:
        order: -10
      kv:
        enabled: true
      authentication: APPID
      app-id:
        user-id: 357f24d38387cffe873d75e444736dbbae6d99facb7b6745a2fd2eaa799d4332  -
      token: s.dremNR6XWrPRj1gQrQwlyrWN
``` 

Notese que ahora tenemos `authentication: APPID` en lugar de `authentication: token`. El valor de `user-id` es la ip, o mejor dicho, el hash 256 de la direccion ip. Podemos calcular el hash como sigue:

```sh
echo -n 192.168.99.1 | sha256sum
357f24d38387cffe873d75e444736dbbae6d99facb7b6745a2fd2eaa799d4332  -
```

#### MAC

La segunda variante, con la MAC:

```yml
spring:
  application:
    name: vault-sample
  profiles:
    active:
    - prueba
  cloud:
    vault:
      # sets the hostname of the Vault host. The host name will be used for SSL certificate validation
      host: www.gz.com
      port: 8200
      scheme: https
      # configure the Vault endpoint with an URI. Takes precedence over host/port/scheme configuration      
      uri: https://www.gz.com:8200 
      connection-timeout: 5000
      read-timeout: 15000
      # sets the order for the property source
      config:
        order: -10
      kv:
        enabled: true
      authentication: APPID
      app-id:
        user-id: 357f24d38387cffe873d75e444736dbbae6d99facb7b6745a2fd2eaa799d4332  -
        network-interface: vEthernet
      token: s.dremNR6XWrPRj1gQrQwlyrWN
```

El valor de `user-id` es la `MAC` address, o mejor dicho, el hash 256 de la `MAC`. Podemos calcular el hash como sigue:

```sh
echo -n 0AFEDE1234AC | sha256sum
fadb01a1cd7edfa37ce7f3e0170ff252701d8b74154875c25bc980eca68e6a8b  -
```
# Vault como proveedror de secretos

## VaultTemplate

Podemos usar VaultTemplate para utilizar la API de Vault y consultar y actualizar secretos. El uso sigue el patron estandard en estos casos:

- Definimos el modelo
- Utilizamos el template para acceder utilizar la API

### Modelo

Definimos un modelo que enviaremos mas tarde a Vault. Las propiedades del POJO seran los Keys en vault. En este ejemplo se crearan dos keys, `usuario` y `contrasena`:

```java
public class Credentials {

	//Las propiedades de este objeto se convertiran en keys cuando lo enviemos al vault
	private String usuario;
	private String contrasena;

	public Credentials() {

	}

	public Credentials(String username, String password) {
		this.usuario = username;
		this.contrasena = password;
	}

	public String getUsuario() {
		return usuario;
	}

	public String getContrasena() {
		return contrasena;
	}

	@Override
	public String toString() {
		return "Credential [usuario=" + usuario + ", contrasena=" + contrasena + "]";
	}
}
```

### Servicio

El servicio utiliza `VaultTemplate` para acceder al Vault. Aqui podemos ver como podemos tanto leer - `Read` - como escribir - `Write` - secretos:

```java
@Service
public class CredentialsService {

	@Autowired
	private VaultTemplate vaultTemplate;

	public void secureCredentials(Credentials credentials) throws URISyntaxException {
		vaultTemplate.write("secret/data/vault-sample", credentials);
	}

	public Credentials accessCredentials() throws URISyntaxException {
		final VaultResponseSupport<Credentials> response = vaultTemplate.read("secret/data/vault-sample", Credentials.class);
		return response.getData();
	}
}
```

#### Escribir

Se especifica el secret y el objeto - el modelo que hemos definido antes:

```java
vaultTemplate.write("secret/data/vault-sample", credentials);
```

#### Leer

Para leer especificamos tambien el secret y recuperamos el modelo:

```java
final VaultResponseSupport<Credentials> response = vaultTemplate.read("secret/data/vault-sample", Credentials.class);
```

#### Comprobar el "efecto" en el Vault

Despues de escribir con el VaultTemplate:

```sh
vault kv get secret/data/vault-sample/

======= Data =======
Key           Value
---           -----
contrasena    nani
usuario       euge
```

Notese que siempre se recuperan o actualizan todos los kv del secret en bloque. Si hacemos:

```sh
vault kv put secret/data/vault-sample/ foo=bar
```

El secreto se actualiza y se pierden `contrasena` y `usuario`:

```sh
vault kv get secret/data/vault-sample/                            

=== Data ===
Key    Value
---    -----
foo    bar
```

Vemos que hemos perdimos nuestra `contrasena` y `usuario`. Si usamos de nuevo el VaultTemplate y miramos de nuevo:

```sh
vault kv get secret/data/vault-sample/

======= Data =======
Key           Value
---           -----
contrasena    nico
usuario       euge
```

# Trust Store

Como estamos usando https y ademas con un self-signed certificate, necesitamos usar un __trust store__ en el que poder __guardar la CA__ que emitio el certificado - el propio certificado. Primero creamos el certificado.

Para crear el certificado usamos __keytool__ - incluida con el jdk:

```sh
keytool -import -v -trustcacerts -file euge.pem -keypass password -storepass password -keystore clienttruststore.jks
```

Hay que recordar una cosa, que si usamos este metodo no se permiten rutas relativas. Bien especificamos una ruta absoluta, o bien la ruta desde la raiz. En nuestro caso al indicar `-keystore clienttruststore.jks`, el trust store deberiamos colocarlo en la raiz del proyecto, justo donde esta tambien este `readme.md`.

Si el certificado no esta protegido con una contraseña:

```sh
keytool -import -v -trustcacerts -file euge.pem -storepass password -keystore clienttruststore.jks  
```

Aqui hemos incluido nuestro certificado, `euge.pem`, en el trust store, `clienttruststore.jks`. El trust store tiene como contraseña `password`.

Podemos ver el contenido del trust store:

```sh
keytool -v -list -keystore clienttruststore.jks
```

Para usar el trust store hay tres opciones

## Trust Store en la `maquina`

En el los argumentos de la vm añadiriamos:

```sh
-Djavax.net.ssl.trustStore=clienttruststore.jks -Djavax.net.ssl.trustStorePassword=password
```

## Trust Store en el `classpath`

En el `bootstrap.yml` incluimos:

```yml
spring:
  cloud:
    vault:
      ssl:
        trust-store: classpath:/clienttruststore.jks
        trust-store-password: password  
```

## Trust Store en `resources`

En el `bootstrap.yml` incluimos:

```yml
spring:
  cloud:
    vault:
      ssl:
        trust-store: clienttruststore.jks
        trust-store-password: password 
```

# Actuator

If the application imports the spring-boot-starter-actuator project, the status of the vault server will be available via the /health endpoint.

The vault health indicator can be enabled or disabled through the property management.health.vault.enabled (default to true).
