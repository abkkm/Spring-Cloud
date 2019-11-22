package com.euge.vault.service;

import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponseSupport;

import com.euge.vault.service.model.Credentials;

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
