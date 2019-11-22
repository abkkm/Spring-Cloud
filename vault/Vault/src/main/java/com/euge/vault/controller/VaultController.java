package com.euge.vault.controller;

import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.euge.vault.service.CredentialsService;
import com.euge.vault.service.model.Credentials;

@RestController
public class VaultController {

	@Autowired
	CredentialsService serv;

	@GetMapping("/update/{value}")
	public String update(@PathVariable("value") String valor) throws URISyntaxException {
		serv.secureCredentials(new Credentials("euge", valor));
		return "Updated";
	}

	@GetMapping("/get")
	public String get() throws URISyntaxException {
		return serv.accessCredentials().getContrasena();
	}
}
