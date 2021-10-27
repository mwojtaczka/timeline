package coma.maciej.wojtaczka.timeline.domain;

import coma.maciej.wojtaczka.timeline.domain.model.Announcement;
import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TimelineService {

    private final ConnectorService connectorService;
	private final DomainEventPublisher eventPublisher;
	private final TimelineRepository timelineRepository;

	public TimelineService(ConnectorService connectorService,
						   DomainEventPublisher eventPublisher,
						   TimelineRepository timelineRepository) {
		this.connectorService = connectorService;
		this.eventPublisher = eventPublisher;
		this.timelineRepository = timelineRepository;
	}

	public void fanoutAnnouncement(Announcement announcement) {
		try {
			List<UUID> followersIndices = connectorService.fetchFollowers(announcement.getAuthorId());


			List<TimelineItem> newTimelineItems = announcement.fanout(followersIndices);

			timelineRepository.saveTimelineItems(newTimelineItems);

			announcement.getDomainEvents()
						.forEach(eventPublisher::publish);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
}
