package com.euge.streams;

public class Greetings {
	private long timestamp;
	private String message;

	public Greetings(long timestamp, String message) {
		super();
		this.timestamp = timestamp;
		this.message = message;
	}

	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "Mensaje recibido: "+message;
	}

}
