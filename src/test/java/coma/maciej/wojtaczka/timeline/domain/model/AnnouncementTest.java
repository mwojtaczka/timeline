package coma.maciej.wojtaczka.timeline.domain.model;

import coma.maciej.wojtaczka.timeline.domain.DomainEvent;
import coma.maciej.wojtaczka.timeline.domain.dto.Envelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static java.time.Instant.parse;
import static org.assertj.core.api.Assertions.assertThat;

class AnnouncementTest {

	@Test
	void shouldCreateNewTimelineItemsForTargetUsers() {
		//given
		UUID announcementAuthorId = UUID.randomUUID();
		UUID userId1 = UUID.randomUUID();
		UUID userId2 = UUID.randomUUID();
		UUID userId3 = UUID.randomUUID();
		List<UUID> targets = List.of(userId1, userId2, userId3);

		Announcement announcement = Announcement.builder()
												.authorId(announcementAuthorId)
												.creationTime(parse("2007-12-03T10:15:30.00Z"))
												.build();

		//when
		List<TimelineItem> newTimelineItems = announcement.fanout(targets);

		//then
		assertThat(newTimelineItems).hasSize(3);
		assertThat(newTimelineItems.get(0).getAnnouncementAuthorId()).isEqualTo(announcementAuthorId);
		assertThat(newTimelineItems.get(0).getCreationTime()).isEqualTo(parse("2007-12-03T10:15:30.00Z"));
		assertThat(newTimelineItems.get(0).getOwnerId()).isEqualTo(userId1);
		assertThat(newTimelineItems.get(1).getAnnouncementAuthorId()).isEqualTo(announcementAuthorId);
		assertThat(newTimelineItems.get(1).getCreationTime()).isEqualTo(parse("2007-12-03T10:15:30.00Z"));
		assertThat(newTimelineItems.get(1).getOwnerId()).isEqualTo(userId2);
		assertThat(newTimelineItems.get(2).getAnnouncementAuthorId()).isEqualTo(announcementAuthorId);
		assertThat(newTimelineItems.get(2).getCreationTime()).isEqualTo(parse("2007-12-03T10:15:30.00Z"));
		assertThat(newTimelineItems.get(2).getOwnerId()).isEqualTo(userId3);
	}

	@Test
	void shouldCreateTimelinesUpdatedEvent() {
		//given
		UUID announcementAuthorId = UUID.randomUUID();
		UUID userId1 = UUID.randomUUID();
		UUID userId2 = UUID.randomUUID();
		UUID userId3 = UUID.randomUUID();
		List<UUID> targets = List.of(userId1, userId2, userId3);

		Announcement announcement = Announcement.builder()
												.authorId(announcementAuthorId)
												.creationTime(parse("2007-12-03T10:15:30.00Z"))
												.build();

		//when
		announcement.fanout(targets);

		//then
		List<DomainEvent<?>> domainEvents = announcement.getDomainEvents();
		assertThat(domainEvents).hasSize(1);
		assertThat(domainEvents.get(0).getDestination()).isEqualTo("timelines-updated");
		assertThat(domainEvents.get(0).getPayload()).isEqualTo(new Envelope<>(targets, announcement));
	}

}
