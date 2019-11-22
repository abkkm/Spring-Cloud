package com.euge.vault.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties("example")
public class MyConfiguration {

	private String username;

	private String password;

	private String alias;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias= alias;
	}
}
