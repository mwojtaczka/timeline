package coma.maciej.wojtaczka.timeline.domain;

import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
public class DomainEvent<T> {

	String destination;
	List<UUID> recipients;
	T payload;

	public DomainEvent(String destination, List<UUID> recipients, T payload) {
		this.destination = destination;
		this.recipients = recipients;
		this.payload = payload;
	}

	public Envelope<T> getEnvelope() {
		return new Envelope<>(recipients, payload);
	}

	@Value
	public static class Envelope<T> {
		List<UUID> recipients;
		T payload;
	}
}
