package coma.maciej.wojtaczka.timeline.rest.controller;

import coma.maciej.wojtaczka.timeline.domain.TimelineService;
import coma.maciej.wojtaczka.timeline.domain.dto.AnnouncementId;
import coma.maciej.wojtaczka.timeline.domain.model.Announcement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class TimelineRestController {

	final static String TIMELINES_URL = "/timelines";
	private final TimelineService timelineService;

	public TimelineRestController(TimelineService timelineService) {
		this.timelineService = timelineService;
	}

	@GetMapping(TIMELINES_URL + "/{userId}")
	ResponseEntity<List<Announcement>> fetch(@PathVariable UUID userId,
											 @RequestParam(required = false) Instant announcementCreationTime,
											 @RequestParam(required = false) UUID announcementAuthorID) {

		List<Announcement> announcements;

		if (announcementCreationTime != null && announcementAuthorID != null) {
			AnnouncementId announcementId = AnnouncementId.builder()
														  .authorId(announcementAuthorID)
														  .creationTime(announcementCreationTime)
														  .build();
			announcements = timelineService.getTimelineAnnouncementsOlderThan(userId, announcementId);

			return ResponseEntity.ok(announcements);
		}
		announcements = timelineService.getFreshestTimelineAnnouncements(userId);

		return ResponseEntity.ok(announcements);
	}
}
