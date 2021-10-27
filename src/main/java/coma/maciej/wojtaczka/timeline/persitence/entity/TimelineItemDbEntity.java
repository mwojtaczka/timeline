package coma.maciej.wojtaczka.timeline.persitence.entity;

import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Builder
@Value
@Table("timeline_item")
public class TimelineItemDbEntity {

	@PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, name = "owner_id")
	UUID ownerId;
	@PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, name = "announcement_author_id")
	UUID announcementAuthorId;
	@PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING, name = "creation_time")
	Instant creationTime;

	public static TimelineItemDbEntity from(TimelineItem timelineItem) {
		return TimelineItemDbEntity.builder()
								   .ownerId(timelineItem.getOwnerId())
								   .announcementAuthorId(timelineItem.getAnnouncementAuthorId())
								   .creationTime(timelineItem.getCreationTime())
								   .build();
	}
}
