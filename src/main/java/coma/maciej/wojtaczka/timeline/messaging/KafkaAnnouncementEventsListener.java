package coma.maciej.wojtaczka.timeline.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import coma.maciej.wojtaczka.timeline.domain.TimelineService;
import coma.maciej.wojtaczka.timeline.domain.model.Announcement;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class KafkaAnnouncementEventsListener {

	private final Logger LOG = LoggerFactory.getLogger(KafkaAnnouncementEventsListener.class);

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

		String announcementJson = consumerRecord.value();

		try {
			Announcement announcement = objectMapper.readValue(announcementJson, Announcement.class);
			timelineService.fanoutAnnouncement(announcement);
		} catch (JsonProcessingException e) {
			LOG.error("Could not parse json", e);
		}
	}
}
