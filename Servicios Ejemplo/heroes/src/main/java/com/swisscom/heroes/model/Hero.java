package com.swisscom.heroes.model;

import javax.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

@Validated
public final class Hero {

	private final int id;
	private final String name;

	public static Builder builder() {
		return new Builder();
	}

	private Hero(@JsonProperty("id") final int id,@JsonProperty("name") final String name) {
		this.id=id;
		this.name=name;
	}

	@NotNull
	public int getId() {
		return id;
	}

	@NotNull
	public String getName() {
		return name;
	}


	public static class Builder {
		private int id;
		private String name;

		public Builder id(final int id) {
			this.id= id;
			return this;
		}

		public Builder name(final String name) {
			this.name= name;
			return this;
		}

		public Hero createHero() {
			return new Hero(id, name);
		}
	}

}
