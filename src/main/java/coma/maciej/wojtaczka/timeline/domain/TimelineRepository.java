package coma.maciej.wojtaczka.timeline.domain;

import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;

import java.util.List;

public interface TimelineRepository {

	void saveTimelineItems(List<TimelineItem> newTimelineItems);
}
