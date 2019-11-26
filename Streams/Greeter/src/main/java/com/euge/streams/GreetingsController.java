package com.euge.streams;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingsController {
	private final GreetingsService greetingsService;

	public GreetingsController(GreetingsService greetingsService) {
		this.greetingsService = greetingsService;
	}

	@GetMapping("/greetings")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void greetings(@RequestParam("message") String message) {

		final Greetings greetings = new Greetings(System.currentTimeMillis(),message);

		greetingsService.sendGreeting(greetings);
	}
}
