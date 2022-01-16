package coma.maciej.wojtaczka.timeline.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import coma.maciej.wojtaczka.timeline.domain.TimelineService;
import coma.maciej.wojtaczka.timeline.domain.model.Announcement;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class KafkaAnnouncementEventsListener {

	public final static String ANNOUNCEMENT_PUBLISHED = "announcement-published";
	private final String GROUP_ID = "timeline";

	private final TimelineService timelineService;
	private final ObjectMapper objectMapper;

	public KafkaAnnouncementEventsListener(TimelineService timelineService, ObjectMapper objectMapper) {
		this.timelineService = timelineService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = ANNOUNCEMENT_PUBLISHED, groupId = GROUP_ID)
	void listenToUserCreated(ConsumerRecord<String, String> consumerRecord) {
		log.debug("{} event received: {}", ANNOUNCEMENT_PUBLISHED, consumerRecord.value());
		String announcementJson = consumerRecord.value();

		try {
			Announcement announcement = objectMapper.readValue(announcementJson, Announcement.class);
			timelineService.fanoutAnnouncement(announcement);
			log.info("Announcement from user: {}, time: {} has been fanout", announcement.getAuthorId(), announcement.getCreationTime());
		} catch (JsonProcessingException e) {
			log.error("Could not parse json", e);
		}
	}
}
