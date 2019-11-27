package com.euge.streams.servicio;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.euge.streams.GreetingsStreams;
import com.euge.streams.modelo.Greetings;

@Component
public class GreetingsListener {

	@StreamListener(GreetingsStreams.INPUT)
	public void handleGreetings(@Payload Greetings greetings) {
		System.out.println("Received greetings: "+greetings);
	}
}
