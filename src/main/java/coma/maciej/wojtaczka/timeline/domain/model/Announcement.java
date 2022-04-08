package coma.maciej.wojtaczka.timeline.domain.model;

import coma.maciej.wojtaczka.timeline.domain.DomainEvent;
import coma.maciej.wojtaczka.timeline.domain.dto.Envelope;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class Announcement extends DomainModel {

	private final UUID authorId;
	private final String content;
	private final Instant creationTime;
	private final long commentsCount;

	public List<TimelineItem> fanout(List<UUID> targets) {

		List<TimelineItem> timelineItems = targets.stream()
												  .map(targetId -> TimelineItem.builder()
																			   .ownerId(targetId)
																			   .announcementAuthorId(authorId)
																			   .creationTime(creationTime)
																			   .build())
												  .collect(Collectors.toList());

		DomainEvent<Envelope<Announcement>> timelinesUpdatedEvent = DomainEvents.timelinesUpdated(targets, this);
		addEventToPublish(timelinesUpdatedEvent);

		return timelineItems;
	}

	public static class DomainEvents {
		public static final String TIMELINES_UPDATED = "timelines-updated";

		static DomainEvent<Envelope<Announcement>> timelinesUpdated(List<UUID> recipients, Announcement timelineData) {
			return new DomainEvent<>(TIMELINES_UPDATED, new Envelope<>(recipients, timelineData));
		}
	}
}
