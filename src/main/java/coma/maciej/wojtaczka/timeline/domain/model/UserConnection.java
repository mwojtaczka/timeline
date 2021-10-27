package coma.maciej.wojtaczka.timeline.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class UserConnection {

	UUID user1;
	UUID user2;
	Instant creationTime;
}
