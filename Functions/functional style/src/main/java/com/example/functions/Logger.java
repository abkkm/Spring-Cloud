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

package com.example.functions;

import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

/*
 * POST a localhost:8080/logger
 * 
 * Con payload
 * 
{
	"nombre":"Eugenio",
	"apellido":"Garcia"
}
 *
 */
public class Logger implements Consumer<entrada> {
	private static org.slf4j.Logger LOG = LoggerFactory.getLogger(Logger.class);

	@Override
	public void accept(entrada t) {
		LOG.info("hola "+t.nombre+" "+t.apellido);
	}

}

class entrada{
	public String nombre;
	public String apellido;

}
