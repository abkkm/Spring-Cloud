/*
 * Copyright 2012-2019 the original author or authors.
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

package com.example;

import java.util.function.Function;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.cloud.function.context.FunctionalSpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;

//@SpringBootApplication
@SpringBootConfiguration
public class SampleApplication implements ApplicationContextInitializer<GenericApplicationContext> {

	public static void main(String[] args) throws Exception {
		FunctionalSpringApplication.run(SampleApplication.class, args);
	}

	//localhost:8080/uppercase/Eugenio Garcia
	//Son posts: 
	//curl http://localhost:8080/demo -d Eugenio
	//curl http://localhost:8080/ -d Eugenio
	@Bean
	public Function<String, String> uppercase() {
		return value -> value.toUpperCase();
	}

	@Override
	public void initialize(GenericApplicationContext applicationContext) {
		applicationContext.registerBean("demo", FunctionRegistration.class,() -> new FunctionRegistration<>(uppercase())
				.type(FunctionType.from(String.class).to(String.class)));
	}
}