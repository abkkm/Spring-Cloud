/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.consul.sample;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableAutoConfiguration
@RestController
@EnableConfigurationProperties
//Habilta Feign
@EnableFeignClients
@Slf4j
public class SampleConsulApplication {

	@Autowired
	private Environment env;

	@Autowired
	private SampleClient sampleClient;


	//Hace referencia al nombre de la aplicacion. Con este nombre se registrara la aplicacion en Consul
	@Value("${spring.application.name:testConsulApp}")
	private String appName;

	public static void main(String[] args) {
		SpringApplication.run(SampleConsulApplication.class, args);
	}

	/*
	 * 
	 * Distributed Configuration
	 * 
	 */

	@Bean
	public SampleProperties sampleProperties() {
		return new SampleProperties();
	}

	//Las propiedades se mapean automaticamente desde la configuracion guardada en Consul
	@RequestMapping("/prop")
	public String prop() {
		return sampleProperties().getProp();
	}

	@RequestMapping("/myenv")
	public String env(@RequestParam("prop") String prop) {
		return this.env.getProperty(prop, "Not Found");
	}



	/*
	 * 
	 * Health Check
	 * 
	 */

	private static Random generador = new Random();

	//Custom Health check endpoint. Mirar el bootstrap.yml para ver como lo hemos configurado
	//@RequestMapping("/my-health-check")
	@GetMapping("/my-health-check")
	public ResponseEntity<String> myCustomCheck() {
		final String message;
		if(generador.nextInt(4)>0) {
			message = "All is fine under the Sun";
			return new ResponseEntity<>(message, HttpStatus.OK);
		}
		else {
			message = "Simulating an error in the health";
			return new ResponseEntity<>(message, HttpStatus.FORBIDDEN);
		}
	}

	/*
	 * 
	 * Registro
	 * 
	 *Se hace automaticamente al specificar la dependencia starter. Podemos eso si personalizar el registro usando propiedades 
	 *en bootstrap.yml y en application.yml. Podemos cambiar los nombres, desdoblar nuestro MiSe en un endpoint de gestion - con 
	 *su propio puerto que sera usado por Consul para determinar la salud, etc; Podemos especificar un puerto diferente para el 
	 *agente de consul - por defecto el 8500. Podemos espeficiar los nombres del servicio y del management service. Ver documentacion 
	 *adjunta
	 *EL health check se hace por defecto en el endpoint /health que justamente es el endpoint que actuator crea para publicitar 
	 *el health del MiSe
	 *
	 */

	//Accede a la informacion de registro. Esta es la informacion que el MiSe envio a Consul para registrarse
	@Autowired
	private Registration registration;

	@RequestMapping("/me")
	public ServiceInstance me() {
		return this.registration;
	}

	/*
	 * 
	 * Discovery Client
	 * 
	 */

	//Con consult se inyecta un discovery client, que nos permitira interrogar a Consul y recuperar todas las instancias de un servicio
	//Esta es una abstraccion, un interface, que es implementado por Eureka, Consul,... En este caso la implementacion que tenemos es la
	//de Consul
	@Autowired
	private DiscoveryClient discoveryClient;

	//Implementamos un endpoint en el controlador que recuperara todas las intancias del servicio
	@RequestMapping("/instances")
	public List<ServiceInstance> instances() {
		return this.discoveryClient.getInstances(this.appName);
	}


	/*
	 * 
	 * Load Balancing con Rest Template
	 * 
	 */

	/*
	 * 
	 * Load Balancing usando el cliente LoadBalancer. Utiliza un estilo similar a Feign, implementando el endpoint como el metodo
	 * de un interface
	 * 
	 */

	//Represents a client-side loadbalancer. La implementacion que usamos en nuestro caso es Netflix-Ribbon. En la release Greenwich
	//se inyecta Netflix-Ribbon, y Netflix-Hystrix por defecto. En los MiSe que demuestran el uso del nuevo Sipring Cloud Loadbalancer
	//en las dependencias debemos excluir explicitamente el uso de Netflix Ribbon y Hystrix para forzar que no se use Netflix
	@Autowired
	private LoadBalancerClient loadBalancer;

	//Demuestra con el cliente del loadbalancer podemos seleccionar una instancia. El loadbalancer es local en el MiSe
	//En este caso recuperamos todo lo que Netflix-Ribbon ha descargado de Consul para este servicio
	@RequestMapping("/")
	public ServiceInstance lb() {
		return this.loadBalancer.choose(this.appName);
	}

	//Demuestra con el cliente del loadbalancer (Netflix-Ribbon) podemos seleccionar una instancia. El loadbalancer es local en el MiSe
	//En este caso recuperamos nos centramos en ver que uri se ha seleccionado el balanceador para este servicio
	@RequestMapping("/choose")
	public String choose() {
		//Usa un metodo del loadbalancer para elegir una de las instancias disponibles
		return this.loadBalancer.choose(this.appName).getUri().toString();
	}

	/*
	 * 
	 * Rest Template Bean con la anotacion @LoadBalanced
	 * 
	 */

	@Autowired
	private RestTemplate restTemplate;

	@Bean
	@LoadBalanced
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	//Usando Netflix-Ribbon
	@RequestMapping("/rest")
	public String rest() {
		return this.restTemplate.getForObject("http://" + this.appName + "/me",
				String.class);
	}

	/*
	 * 
	 * FEIGN
	 * 
	 */

	//Usamos el service Discovery con Feign
	//Esto es estandard, simplemente nuestro endpoint llama a un metodo...
	@RequestMapping("/feign")
	public String feign() {
		return this.sampleClient.choose();
	}

	//El metodo al que llamamos es parte de un interface anotado con Feign. Esto hace que la llamada al metodo se traduzca en 
	//una llamada a un servicio via Get. El servicio llamado sera testConsulApp. testConsulApp es el ID de un servicio registrado en Consul
	//Llamamos al servicio al recurso /choose, y esperamos un String
	//Seria como hacer un GET al endpoint testConsulApp/choose con el RestTemplate
	@FeignClient("testConsulApp")
	public interface SampleClient {

		@RequestMapping(value = "/choose", method = RequestMethod.GET)
		String choose();

	}

}
