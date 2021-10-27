package coma.maciej.wojtaczka.timeline.domain;

public interface DomainEventPublisher {

	void publish(DomainEvent<?> domainEvent);
}
