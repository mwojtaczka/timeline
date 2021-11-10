package coma.maciej.wojtaczka.timeline.domain;

import coma.maciej.wojtaczka.timeline.domain.model.Announcement;
import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;

import java.util.List;

public interface AnnouncementService {

	List<Announcement> convert(List<TimelineItem> timelineItems);
}
