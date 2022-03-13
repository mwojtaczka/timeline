package coma.maciej.wojtaczka.timeline.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Builder
@Value
public class TimelineItem {

	UUID ownerId;
	UUID announcementAuthorId;
	Instant creationTime;
}
