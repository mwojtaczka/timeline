package coma.maciej.wojtaczka.timeline.messaging;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import coma.maciej.wojtaczka.timeline.domain.DomainEvent;
import coma.maciej.wojtaczka.timeline.domain.model.Announcement;
import coma.maciej.wojtaczka.timeline.persitence.entity.TimelineItemDbEntity;
import coma.maciej.wojtaczka.timeline.utils.KafkaTestListener;
import coma.maciej.wojtaczka.timeline.utils.UserFixture;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom;
import static coma.maciej.wojtaczka.timeline.domain.model.Announcement.DomainEvents.TIMELINES_UPDATED;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@WebAppConfiguration
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@DirtiesContext
class KafkaAnnouncementEventsListenerTest {

	@BeforeAll
	static void startCassandra() throws IOException, InterruptedException {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
		CqlSession session = EmbeddedCassandraServerHelper.getSession();
		new CQLDataLoader(session).load(new ClassPathCQLDataSet("schema.cql"));
	}

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private CassandraOperations cassandraOperations;

	@Autowired
	private UserFixture $;

	@Autowired
	private KafkaTestListener kafkaTestListener;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		kafkaTestListener.reset();
	}

	@Test
	void shouldFanoutAnnouncementToAllFollowers() throws ExecutionException, InterruptedException, JsonProcessingException {
		//given
		kafkaTestListener.listenToTopic(TIMELINES_UPDATED, 2);

		UUID announcerId1 = UUID.fromString("e64864da-e2ab-4e7d-b65c-1a7a7d91d0f1");
		Instant creationTime1 = Instant.parse("2007-12-03T10:15:30.00Z");
		String announcementPublishedEvent1 = getAnnouncementEventJson(announcerId1, creationTime1);

		UUID announcerId2 = UUID.fromString("30eaeffd-132b-4b75-a6ca-4b452ca8f183");
		Instant creationTime2 = Instant.parse("2007-12-03T10:15:40.00Z");
		String announcementPublishedEvent2 = getAnnouncementEventJson(announcerId2, creationTime2);

		UUID followerId1 = UUID.randomUUID();
		UUID followerId2 = UUID.randomUUID();
		UUID followerId3 = UUID.randomUUID();

		$.userWithId(announcerId1)
		 .isFollowedByUserWithId(followerId1)
		 .andAlsoFollowedByUserWithId(followerId2)
		 .andAlsoFollowedByUserWithId(followerId3);

		$.userWithId(announcerId2)
		 .isFollowedByUserWithId(followerId1)
		 .andAlsoFollowedByUserWithId(followerId2);

		//when
		ListenableFuture<SendResult<String, String>> sent1 =
				kafkaTemplate.send(KafkaAnnouncementEventsListener.ANNOUNCEMENT_PUBLISHED, announcementPublishedEvent1);
		ListenableFuture<SendResult<String, String>> sent2 =
				kafkaTemplate.send(KafkaAnnouncementEventsListener.ANNOUNCEMENT_PUBLISHED, announcementPublishedEvent2);
		sent1.get();
		sent2.get();

		//then
		//verify persistence
		waitUntilRecordsFound(5, Set.of(followerId1, followerId2, followerId3));

		Select selectFollowersTimelineItems =
				selectFrom("timeline", "timeline_item").all().whereColumn("owner_id").isEqualTo(bindMarker());

		List<TimelineItemDbEntity> follower1timelineItems = cassandraOperations.select(
				selectFollowersTimelineItems.build(followerId1), TimelineItemDbEntity.class);
		assertThat(follower1timelineItems).hasSize(2);
		assertThat(follower1timelineItems.get(0).getOwnerId()).isEqualTo(followerId1);
		assertThat(follower1timelineItems.get(0).getAnnouncementAuthorId()).isEqualTo(announcerId2);
		assertThat(follower1timelineItems.get(0).getCreationTime()).isEqualTo(creationTime2);
		assertThat(follower1timelineItems.get(1).getOwnerId()).isEqualTo(followerId1);
		assertThat(follower1timelineItems.get(1).getAnnouncementAuthorId()).isEqualTo(announcerId1);
		assertThat(follower1timelineItems.get(1).getCreationTime()).isEqualTo(creationTime1);

		List<TimelineItemDbEntity> follower2timelineItems = cassandraOperations.select(
				selectFollowersTimelineItems.build(followerId2), TimelineItemDbEntity.class);
		assertThat(follower2timelineItems).hasSize(2);
		assertThat(follower2timelineItems.get(0).getOwnerId()).isEqualTo(followerId2);
		assertThat(follower2timelineItems.get(0).getAnnouncementAuthorId()).isEqualTo(announcerId2);
		assertThat(follower2timelineItems.get(0).getCreationTime()).isEqualTo(creationTime2);
		assertThat(follower2timelineItems.get(1).getOwnerId()).isEqualTo(followerId2);
		assertThat(follower2timelineItems.get(1).getAnnouncementAuthorId()).isEqualTo(announcerId1);
		assertThat(follower2timelineItems.get(1).getCreationTime()).isEqualTo(creationTime1);

		List<TimelineItemDbEntity> follower3timelineItems = cassandraOperations.select(
				selectFollowersTimelineItems.build(followerId3), TimelineItemDbEntity.class);
		assertThat(follower3timelineItems).hasSize(1);
		assertThat(follower3timelineItems.get(0).getOwnerId()).isEqualTo(followerId3);
		assertThat(follower3timelineItems.get(0).getAnnouncementAuthorId()).isEqualTo(announcerId1);
		assertThat(follower3timelineItems.get(0).getCreationTime()).isEqualTo(creationTime1);

		//verify event publishing
		assertThat(kafkaTestListener.msgCount(TIMELINES_UPDATED)).isEqualTo(2);
		String capturedEvent1 = kafkaTestListener.receiveContentFromTopic(TIMELINES_UPDATED)
												 .orElseThrow(() -> new RuntimeException("No events"));

		DomainEvent.Envelope<Announcement> event1 = objectMapper.readValue(capturedEvent1, new TypeReference<>() {
		});
		assertThat(event1.getRecipients()).containsExactlyInAnyOrder(followerId1, followerId2, followerId3);
		assertThat(event1.getPayload().getAuthorId()).isEqualTo(announcerId1);

		String capturedEvent2 = kafkaTestListener.receiveContentFromTopic(TIMELINES_UPDATED)
												 .orElseThrow(() -> new RuntimeException("No events"));
		DomainEvent.Envelope<Announcement> event2 = objectMapper.readValue(capturedEvent2, new TypeReference<>() {
		});
		assertThat(event2.getRecipients()).containsExactlyInAnyOrder(followerId1, followerId2);
		assertThat(event2.getPayload().getAuthorId()).isEqualTo(announcerId2);
	}

	private void waitUntilRecordsFound(int recordsCount, Set<UUID> followers) throws InterruptedException {
		LocalDateTime timeout = now().plus(1000, MILLIS);
		while (now().isBefore(timeout)) {
			List<TimelineItemDbEntity> select = cassandraOperations
					.select("select * from timeline.timeline_item;", TimelineItemDbEntity.class)
					.stream()
					.filter(timelineItemDbEntity -> followers.contains(timelineItemDbEntity.getOwnerId()))
					.collect(Collectors.toList());
			if (select.size() == recordsCount) {
				break;
			}
			Thread.sleep(50);
		}
	}

	private String getAnnouncementEventJson(UUID announcementAuthorId, Instant creationTime) {
		return String.format("{\n" +
									 "   \"authorId\":\"%s\",\n" +
									 "   \"content\":\"Hello world\",\n" +
									 "   \"creationTime\":\"%s\",\n" +
									 "   \"comments\":[\n" +
									 "      \n" +
									 "   ]\n" +
									 "}",
							 announcementAuthorId.toString(), creationTime);
	}
}
