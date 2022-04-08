package coma.maciej.wojtaczka.timeline.domain.dto;

import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
public class Envelope<T> {
	List<UUID> recipients;
	T payload;
}
