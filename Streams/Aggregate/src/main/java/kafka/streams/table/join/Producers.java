/*
 * Copyright 2018 the original author or authors.
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

package kafka.streams.table.join;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.fasterxml.jackson.databind.ObjectMapper;

import kafka.streams.table.join.modelo.DomainEvent;

public class Producers {

	private static Serde<DomainEvent> domainEventSerde;

	public static void main(String... args) {

		final ObjectMapper mapper = new ObjectMapper();
		domainEventSerde = new JsonSerde<>(DomainEvent.class, mapper);

		final Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "10.0.75.1:29092");
		props.put(ProducerConfig.RETRIES_CONFIG, 0);
		props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
		props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
		props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, domainEventSerde.serializer().getClass());

		final DomainEvent ddEvent = new DomainEvent();
		ddEvent.setBoardUuid("12345");
		ddEvent.setEventType("thisisanevent");

		final DefaultKafkaProducerFactory<String, DomainEvent> pf = new DefaultKafkaProducerFactory<>(props);
		final KafkaTemplate<String, DomainEvent> template = new KafkaTemplate<>(pf, true);
		template.setDefaultTopic("bar");

		template.sendDefault("", ddEvent);
	}
}
