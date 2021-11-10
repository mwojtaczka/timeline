package coma.maciej.wojtaczka.timeline.domain.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class AnnouncementId {

	UUID authorId;
	Instant creationTime;
}
