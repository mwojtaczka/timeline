package coma.maciej.wojtaczka.timeline.domain;

import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TimelineRepository {

	void saveTimelineItems(List<TimelineItem> newTimelineItems);

	List<TimelineItem> fetchTimelineItems(UUID userId, int limit);

	List<TimelineItem> fetchTimelineItems(UUID userId, int limit, Instant olderThan);
}
