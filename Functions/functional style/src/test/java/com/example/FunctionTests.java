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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@FunctionalSpringBootTest
@AutoConfigureWebTestClient
public class FunctionTests {

	@Autowired
	private WebTestClient client;

	@Test
	public void words_using_http() throws Exception {
		client.post().uri("/").body(Mono.just("foo"), String.class).exchange()
		.expectStatus().isOk().expectBody(String.class).isEqualTo("FOO");
	}

	@Test
	public void words_using_http1() throws Exception {
		client.post().uri("/demo").body(Mono.just("foo"), String.class).exchange()
		.expectStatus().isOk().expectBody(String.class).isEqualTo("FOO");
	}

	@Autowired
	private FunctionCatalog catalog;

	/**
	 * 
	 * ATENCION. AUNQUE LA FUNCION LA HEMOS REGISTRADO COMO STRING, AL OBTENERLA DEL CATALOGO SE DEFINE COMO FLUX>STRING>
	 * VER TAMBIEN NOTA EN EL POM
	 * 
	 */
	@Test
	public void words_using_catalogue() throws Exception {
		final Function<Flux<String>, Flux<String>> function = catalog.lookup(Function.class,
				"demo");

		final String resp=function.apply(Flux.just("foo")).blockFirst();

		assertThat(resp).isEqualTo("FOO");
	}

}
