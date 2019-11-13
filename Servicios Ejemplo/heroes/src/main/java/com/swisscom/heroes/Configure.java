package com.swisscom.heroes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.swisscom.hero.datasource.heroesDS;

@Configuration
public class Configure {

	@Bean
	public heroesDS service() {
		return new heroesDS();
	}

}
