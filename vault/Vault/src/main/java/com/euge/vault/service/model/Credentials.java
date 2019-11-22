package com.euge.vault.service.model;

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
