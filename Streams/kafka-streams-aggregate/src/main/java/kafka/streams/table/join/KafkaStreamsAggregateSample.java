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

import java.util.function.Consumer;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.fasterxml.jackson.databind.ObjectMapper;

import kafka.streams.table.join.modelo.DomainEvent;

@SpringBootApplication
public class KafkaStreamsAggregateSample {

	public static void main(String[] args) {
		SpringApplication.run(KafkaStreamsAggregateSample.class, args);
	}

	public static class KafkaStreamsAggregateSampleApplication {

		@Bean
		public Consumer<KStream<String, DomainEvent>> aggregate() {

			final ObjectMapper mapper = new ObjectMapper();
			final Serde<DomainEvent> domainEventSerde = new JsonSerde<>( DomainEvent.class, mapper );

			return input -> input
					.groupBy(
							(s, domainEvent) -> domainEvent.getBoardUuid(),
							Grouped.with(null, domainEventSerde))
					.aggregate(
							String::new,
							(s, domainEvent, board) -> board.concat(domainEvent.getEventType()),
							Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("test-events-snapshots")
							.withKeySerde(Serdes.String()).
							withValueSerde(Serdes.String())
							);
		}
	}

}
