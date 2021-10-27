package coma.maciej.wojtaczka.timeline.domain;

import lombok.Value;

@Value
public class DomainEvent<T> {

	String destination;
	T payload;

	public DomainEvent(String destination, T payload) {
		this.destination = destination;
		this.payload = payload;
	}
}
