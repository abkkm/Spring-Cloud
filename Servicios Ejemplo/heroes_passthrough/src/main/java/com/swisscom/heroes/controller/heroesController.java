package com.swisscom.heroes.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.swisscom.heroes.model.Hero;

@RestController
@RequestMapping(value = "/")
public class heroesController {

	@Autowired
	RestTemplate servicio;
	//Eureka service name
	private final String url="http://heroes";

	@RequestMapping(value = "/heroes",produces = { "application/json" }, method = RequestMethod.GET)
	public ResponseEntity<Hero[]> getHeroes(){
		final String heroes_url = url+"/heroes";
		final ResponseEntity<Hero[]> resp= servicio.getForEntity(heroes_url, Hero[].class);
		return resp;
	}

	@RequestMapping(value = "/heroes/{id}",produces = { "application/json" }, method = RequestMethod.GET)
	public ResponseEntity<Hero> getHero(@PathVariable("id") Integer id){

		final String heroes_url = url+"/heroes/{id}";
		final Map<String, String> uriParams = new HashMap<>();
		uriParams.put("id", id.toString());

		final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(heroes_url);

		final ResponseEntity<Hero> resp= servicio.getForEntity(builder.buildAndExpand(uriParams).toUri(), Hero.class);
		return resp;
	}

	@RequestMapping(value = "/heroes",produces = { "application/json" }, method = RequestMethod.POST)
	public ResponseEntity<Hero> addHero(@RequestBody Hero hero){
		final String heroes_url = url+"/heroes";
		return servicio.postForEntity(url, hero, Hero.class);
	}

	@RequestMapping(value = "/heroes",produces = { "application/json" }, method = RequestMethod.PUT)
	public ResponseEntity<Void> updateHero(@RequestBody Hero hero){
		final String heroes_url = url+"/heroes";
		final HttpEntity<Hero> data=new HttpEntity<Hero>(hero);
		return servicio.exchange(heroes_url, HttpMethod.PUT,data,Void.class);
	}

	@CrossOrigin(origins = "http://localhost:4200")
	@RequestMapping(value = "/heroes/{id}",produces = { "application/json" },method = RequestMethod.DELETE)
	public ResponseEntity<Void> deleteHero(@PathVariable int id){
		final String heroes_url = url+"/heroes/{id}";
		final Map<String, String> uriParams = new HashMap<>();
		uriParams.put("id", String.valueOf(id));

		final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(heroes_url);

		return servicio.exchange(builder.buildAndExpand(uriParams).toUri(), HttpMethod.DELETE,null,Void.class);
	}

}
