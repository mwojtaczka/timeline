package coma.maciej.wojtaczka.timeline.domain;

import coma.maciej.wojtaczka.timeline.domain.dto.AnnouncementId;
import coma.maciej.wojtaczka.timeline.domain.model.Announcement;
import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class TimelineService {

	public static final int PAGE_LIMIT = 20;
	private final ConnectorService connectorService;
	private final AnnouncementService announcementService;
	private final DomainEventPublisher eventPublisher;
	private final TimelineRepository timelineRepository;

	public TimelineService(ConnectorService connectorService,
						   AnnouncementService announcementService,
						   DomainEventPublisher eventPublisher,
						   TimelineRepository timelineRepository) {
		this.connectorService = connectorService;
		this.announcementService = announcementService;
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

	public List<Announcement> getFreshestTimelineAnnouncements(UUID userId) {

		List<TimelineItem> timelineItems = timelineRepository.fetchTimelineItems(userId, PAGE_LIMIT);

		List<Announcement> timelineAnnouncements = announcementService.convert(timelineItems);
		timelineAnnouncements.sort(Comparator.comparing(Announcement::getCreationTime).reversed());

		return timelineAnnouncements;
	}

	public List<Announcement> getTimelineAnnouncementsOlderThan(UUID userId, AnnouncementId last) {

		List<TimelineItem> timelineItems = timelineRepository.fetchTimelineItems(userId, PAGE_LIMIT, last.getCreationTime())
															 .stream()
															 .filter(filterOut(last))
															 .collect(Collectors.toList());

		List<Announcement> timelineAnnouncements = announcementService.convert(timelineItems);
		timelineAnnouncements.sort(Comparator.comparing(Announcement::getCreationTime).reversed());

		return timelineAnnouncements;
	}

	private Predicate<TimelineItem> filterOut(AnnouncementId last) {
		return timelineItem -> !(timelineItem.getAnnouncementAuthorId().equals(last.getAuthorId()) &&
				timelineItem.getCreationTime().equals(last.getCreationTime()));
	}
}
