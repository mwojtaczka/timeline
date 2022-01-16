package coma.maciej.wojtaczka.timeline.rest.client;

import coma.maciej.wojtaczka.timeline.domain.AnnouncementService;
import coma.maciej.wojtaczka.timeline.domain.model.Announcement;
import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;
import lombok.Builder;
import lombok.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
public class AnnouncementServiceRestClient implements AnnouncementService {

	public final static String FETCH_ANNOUNCEMENTS = "/v1/announcements/fetch";

	private final RestTemplate restTemplate;

	public AnnouncementServiceRestClient(@Qualifier("AnnBrd") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Override
	public List<Announcement> convert(List<TimelineItem> timelineItems) {

		List<AnnouncementQuery> queries = timelineItems.stream()
													   .map(AnnouncementQuery::from)
													   .collect(toList());

		AnnouncementsPerAuthor[] announcements = restTemplate.postForObject(
				FETCH_ANNOUNCEMENTS,
				queries,
				AnnouncementsPerAuthor[].class);

		if (announcements == null) {
			return List.of();
		}

		return Stream.of(announcements)
					 .flatMap(announcement -> announcement.getAnnouncements().stream())
					 .collect(toList());
	}


	@Value
	@Builder
	public static class AnnouncementQuery {

		UUID authorId;
		Instant creationTime;

		public static AnnouncementQuery from(TimelineItem item) {
			return AnnouncementQuery.builder()
									.authorId(item.getAnnouncementAuthorId())
									.creationTime(item.getCreationTime())
									.build();
		}
	}

	@Value
	@Builder
	public static class AnnouncementsPerAuthor {
		UUID authorId;
		List<Announcement> announcements;
	}
}
