package coma.maciej.wojtaczka.timeline.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import coma.maciej.wojtaczka.timeline.domain.model.Announcement;
import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;
import coma.maciej.wojtaczka.timeline.domain.model.UserConnection;
import coma.maciej.wojtaczka.timeline.persitence.entity.TimelineItemDbEntity;
import coma.maciej.wojtaczka.timeline.rest.client.AnnouncementServiceRestClient;
import lombok.SneakyThrows;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static coma.maciej.wojtaczka.timeline.rest.client.AnnouncementServiceRestClient.FETCH_ANNOUNCEMENTS;
import static coma.maciej.wojtaczka.timeline.rest.client.ConnectorServiceRestClient.GET_CONNECTIONS_BY_USER;
import static coma.maciej.wojtaczka.timeline.rest.client.ConnectorServiceRestClient.USERS_ID_PARAM;
import static java.util.Comparator.comparing;

@Component
public class UserFixture {

	private final WireMockServer mockServer;
	private final ObjectMapper objectMapper;
	private final CassandraOperations cassandraOperations;

	public UserFixture(WireMockServer mockServer,
					   ObjectMapper objectMapper,
					   CassandraOperations cassandraOperations) {
		this.mockServer = mockServer;
		this.objectMapper = objectMapper;
		this.cassandraOperations = cassandraOperations;
	}

	public UserBuilder userWithId(UUID userId) {
		return new UserBuilder(userId);
	}

	public class UserBuilder {
		private final UUID userId;
		private final List<UserConnection> followers = new ArrayList<>();
		private final List<FollowedUserContextHolder> followees = new ArrayList<>();
		private final List<TimelineItem> timelineItems = new ArrayList<>();

		public UserBuilder(UUID userId) {
			this.userId = userId;
		}

		@SneakyThrows
		public UserBuilder isFollowedByUserWithId(UUID connectedUserId) {
			UserConnection connection = UserConnection.builder()
													  .user1(userId)
													  .user2(connectedUserId)
													  .creationTime(Instant.parse("2007-12-03T10:15:30.00Z"))
													  .build();
			followers.add(connection);
			String jsonResponseBody = objectMapper.writeValueAsString(followers);

			ResponseDefinitionBuilder response = WireMock.aResponse()
														 .withHeader("Content-type", "application/json")
														 .withBody(jsonResponseBody);

			mockServer.stubFor(get(urlPathEqualTo(GET_CONNECTIONS_BY_USER))
									   .withQueryParam(USERS_ID_PARAM, equalTo(userId.toString()))
									   .willReturn(response));
			return this;
		}

		public UserBuilder andAlsoFollowedByUserWithId(UUID connectedUserId) {
			return isFollowedByUserWithId(connectedUserId);
		}

		public FollowedUserContextHolder followsUserWithId(UUID followeeId) {
			return new FollowedUserContextHolder(followeeId, this);
		}

		@SneakyThrows
		public void isDone() {
			List<AnnouncementServiceRestClient.AnnouncementQuery> queries =
					timelineItems.stream()
								 .peek(entity -> cassandraOperations.insert(TimelineItemDbEntity.from(entity)))
								 .map(AnnouncementServiceRestClient.AnnouncementQuery::from)
								 .sorted(comparing(AnnouncementServiceRestClient.AnnouncementQuery::getCreationTime).reversed())
								 .collect(Collectors.toList());


			List<AnnouncementServiceRestClient.AnnouncementsPerAuthor> responseBody =
					followees.stream()
							 .map(this::toAnnouncementPerAuthor)
							 .collect(Collectors.toList());

			createAllPossibleStubs(queries, responseBody);
		}

		private AnnouncementServiceRestClient.AnnouncementsPerAuthor toAnnouncementPerAuthor(
				FollowedUserContextHolder followedUserContextHolder) {
			return AnnouncementServiceRestClient.AnnouncementsPerAuthor.builder()
																	   .authorId(followedUserContextHolder.announcerId)
																	   .announcements(followedUserContextHolder.userAnnouncements)
																	   .build();
		}

		@SneakyThrows
		private void createAllPossibleStubs(List<AnnouncementServiceRestClient.AnnouncementQuery> queries,
											List<AnnouncementServiceRestClient.AnnouncementsPerAuthor> queriesResponses) {

			for (AnnouncementServiceRestClient.AnnouncementQuery referenceQuery : queries) {
				List<AnnouncementServiceRestClient.AnnouncementQuery> subQueries =
						queries.stream()
							   .filter(q -> isBeforeOrEqual(q.getCreationTime(),
															referenceQuery.getCreationTime()))
							   .limit(20)
							   .collect(Collectors.toList());

				List<AnnouncementServiceRestClient.AnnouncementsPerAuthor> subResponses =
						queriesResponses.stream()
										.flatMap(qr -> qr.getAnnouncements().stream())
										.filter(a -> isBeforeOrEqual(a.getCreationTime()
												, referenceQuery.getCreationTime()))
										.limit(20)
										.collect(Collectors.groupingBy(Announcement::getAuthorId))
										.entrySet().stream()
										.map(entry -> AnnouncementServiceRestClient.AnnouncementsPerAuthor.builder()
																										  .authorId(entry.getKey())
																										  .announcements(entry.getValue())
																										  .build())
										.collect(Collectors.toList());

				ResponseDefinitionBuilder response = WireMock.aResponse()
															 .withHeader("Content-type", "application/json")
															 .withBody(objectMapper.writeValueAsString(subResponses));

				String queriesJson = objectMapper.writeValueAsString(subQueries);
				mockServer.stubFor(post(FETCH_ANNOUNCEMENTS)
										   .withRequestBody(equalToJson(queriesJson, true, false))
										   .willReturn(response));
			}
		}

		private boolean isBeforeOrEqual(Instant i, Instant reference) {
			return i.isBefore(reference) || i.equals(reference);
		}


	}

	public class FollowedUserContextHolder {

		private final UUID announcerId;
		private final List<Announcement> userAnnouncements = new ArrayList<>();
		private final UserBuilder userWhoFollows;

		public FollowedUserContextHolder(UUID id, UserBuilder userWhoFollows) {
			this.announcerId = id;
			this.userWhoFollows = userWhoFollows;
			userWhoFollows.followees.add(this);
		}

		public AnnouncementBuilder whoPublishedAnnouncement() {
			return new AnnouncementBuilder(this, userWhoFollows);
		}

	}

	public static class AnnouncementBuilder {

		private final Announcement.AnnouncementBuilder builder;
		private final FollowedUserContextHolder announcer;
		private final UserBuilder userWhoFollowsTheAnnouncer;

		public AnnouncementBuilder(FollowedUserContextHolder announcer, UserBuilder userWhoFollows) {
			this.announcer = announcer;
			this.userWhoFollowsTheAnnouncer = userWhoFollows;
			this.builder = Announcement.builder()
									   .authorId(announcer.announcerId)
									   .content("Default content")
									   .commentsCount(0)
									   .creationTime(Instant.now());
		}

		public AnnouncementBuilder atTime(Instant instant) {
			builder.creationTime(instant);
			return this;
		}

		public AnnouncementBuilder withContent(String content) {
			builder.content(content);
			return this;
		}

		public AnnouncementBuilder thatHasBeenCommentedTimes(long times) {
			builder.commentsCount(times);
			return this;
		}

		public AnnouncementBuilder andAlsoAnnouncement() {
			build();
			return new AnnouncementBuilder(announcer, userWhoFollowsTheAnnouncer);
		}

		public UserBuilder andTheGivenUser() {
			build();
			return userWhoFollowsTheAnnouncer;
		}

		private void build() {
			Announcement announcement = builder.build();
			announcer.userAnnouncements.add(announcement);
			TimelineItem timelineItem = TimelineItem.builder()
													.ownerId(userWhoFollowsTheAnnouncer.userId)
													.announcementAuthorId(announcement.getAuthorId())
													.creationTime(announcement.getCreationTime())
													.build();
			userWhoFollowsTheAnnouncer.timelineItems.add(timelineItem);
		}
	}
}
