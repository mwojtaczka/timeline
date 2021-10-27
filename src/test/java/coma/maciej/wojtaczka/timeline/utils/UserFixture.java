package coma.maciej.wojtaczka.timeline.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import coma.maciej.wojtaczka.timeline.domain.model.UserConnection;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static coma.maciej.wojtaczka.timeline.rest.client.ConnectorServiceRestClient.GET_CONNECTIONS_BY_USER;

@Component
public class UserFixture {

	private final WireMockServer mockServer;
	private final ObjectMapper objectMapper;

	public UserFixture(WireMockServer mockServer, ObjectMapper objectMapper) {
		this.mockServer = mockServer;
		this.objectMapper = objectMapper;
	}

	public UserBuilder userWithId(UUID userId) {
		return new UserBuilder(userId);
	}

	public class UserBuilder {
		private final UUID userId;
		private final List<UserConnection> userConnections = new ArrayList<>();

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
			userConnections.add(connection);
			String jsonResponseBody = objectMapper.writeValueAsString(userConnections);

			ResponseDefinitionBuilder response = WireMock.aResponse()
														 .withHeader("Content-type", "application/json")
														 .withBody(jsonResponseBody);

			mockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(GET_CONNECTIONS_BY_USER))
									   .withQueryParam("userId", equalTo(userId.toString()))
									   .willReturn(response));
			return this;
		}

		public UserBuilder andAlsoByUserWithId(UUID connectedUserId) {
			return isFollowedByUserWithId(connectedUserId);
		}
	}
}
