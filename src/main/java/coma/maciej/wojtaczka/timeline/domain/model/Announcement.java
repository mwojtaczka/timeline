package coma.maciej.wojtaczka.timeline.domain.model;

import coma.maciej.wojtaczka.timeline.domain.DomainEvent;
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
//TODO: add comments

	public List<TimelineItem> fanout(List<UUID> targets) {

		List<TimelineItem> timelineItems = targets.stream()
												  .map(targetId -> TimelineItem.builder()
																			   .ownerId(targetId)
																			   .announcementAuthorId(authorId)
																			   .creationTime(creationTime)
																			   .build())
												  .collect(Collectors.toList());

		DomainEvent<List<TimelineItem>> timelinesUpdatedEvent = DomainEvents.timelinesUpdated(timelineItems);
		addEventToPublish(timelinesUpdatedEvent);

		return timelineItems;
	}

	public static class DomainEvents {
		public static final String TIMELINES_UPDATED = "timelines-updated";

		static DomainEvent<List<TimelineItem>> timelinesUpdated(List<TimelineItem> newTimelineItems) {
			return new DomainEvent<>(TIMELINES_UPDATED, newTimelineItems);
		}
	}
}
