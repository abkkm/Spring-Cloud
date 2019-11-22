# Setup

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

Hemos configurado el `uri` del vault como `https://www.gz.com:8200` que usa https. Tendremos que cargar el certificado de este site, el `www.gz.com` - ahora configuramos el `truststore`.  Por ultimo hemos indicado que la authenticacion es de tipo `token`, y hemos especificado el token a utilizar.

Hay [otras formas](https://cloud.spring.io/spring-cloud-static/spring-cloud-vault/2.2.0.RC2/reference/html/) de autenticacion con vault. Voy a destacar aqui `appid`:

## Autenticacion con `Appid`

### IP

Con `appid` tenemos dos variantes, usando la ip, o la map address. Con la ip la configuracion seria como sigue:

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

El valor de la ip es el hash 256 de la direccion ip:

```sh
echo -n 192.168.99.1 | sha256sum
357f24d38387cffe873d75e444736dbbae6d99facb7b6745a2fd2eaa799d4332  -
```

### MAC

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

La MAC seria la del `network-interface`. La obtenemos como sigue:

```sh
echo -n 0AFEDE1234AC | sha256sum
fadb01a1cd7edfa37ce7f3e0170ff252701d8b74154875c25bc980eca68e6a8b  -
```

# Vault Secrets usados por `spring-cloud-starter-vault-config`

`spring-cloud-starter-vault-config` busca en Vault las propiedades siguiendo una convencion:

- Buscando en `secret`
	- Usando el nombre de la aplicacion
		- Utilizando  el `profile`
	- Buscando en `application` - que seria el lugar donde guardar secretos comunes a todos las apis
		- Utilizando  el `profile`

Esto es, se buscara la propiedad dentro en las siguientes ubicaciones de secretos, y en este orden:

```sh
secret/vault-sample/prueba
secret/vault-sample
secret/application/prueba
secret/application
```

# Actuator

If the application imports the spring-boot-starter-actuator project, the status of the vault server will be available via the /health endpoint.

The vault health indicator can be enabled or disabled through the property management.health.vault.enabled (default to true).

# Propiedades en nuestra Aplicacion

Podemos inyectar los secretos de vault como si se tratara de una configuracion distribuida "mas", como Consul. La aplicacion no tiene una logica especial - solo configurar en `bootstrap.yml` vault. Lo que es el codigo propiamente dicho trata las propiedades de forma "estandard" sin ninguna referencia especial a vault.

En nuestra aplicacione tenemos:

```java
@SpringBootApplication
@EnableConfigurationProperties(MyConfiguration.class)
public class VaultApplication implements CommandLineRunner {
```

Estamos especificando que tenemos propiedades definidas en `MyConfiguration.class`.

```java
@ConfigurationProperties("example")
public class MyConfiguration {

	private String username;

	private String password;

	(. . .)
```

Hemos definido dos propiedades, `example.username` y `example.password`.

Vamos a dar valor a estas propiedades en `vault`. Vamos a crear dos juegos de secretos, uno que usaremos cuando no haya ´profile´ especificado, y otro con el `profile` prueba:

```sh
vault kv put secret/vault-sample/ example.username=demouser example.password=demopassword

vault kv put secret/vault-sample/prueba example.username=prueba-user example.password=prueba-password
```

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

# VaultTemplate

Definimos un modelo que enviaremos mas tarde a Vault. Las propiedades del POJO seran los Keys en vault. En este ejemplo se crearan dos keys, `usuario` y `contrseana`:

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

El servicio utiliza `VaultTemplate` para acceder al Vault:

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

Se especifica el secret y el objeto - el modelo que hemos definido antes:

```java
vaultTemplate.write("secret/data/vault-sample", credentials);
```

Para leer especificamos tambien el secret y recuperamos el modelo:

```java
final VaultResponseSupport<Credentials> response = vaultTemplate.read("secret/data/vault-sample", Credentials.class);
```

Por ejemplo, despues de escribir con el VaultTemplate:

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

Como estamos usando https y ademas con un self-signed certificate, necesitamos usar un __trust store__ en el que poder guardar la CA que emitio el certificado - el propio certificado. Primero creamos el certificado.

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

