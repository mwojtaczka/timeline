package coma.maciej.wojtaczka.timeline.rest.client;

import coma.maciej.wojtaczka.timeline.domain.ConnectorService;
import coma.maciej.wojtaczka.timeline.domain.model.UserConnection;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
public class ConnectorServiceRestClient implements ConnectorService {

	public final static String GET_CONNECTIONS_BY_USER = "/v1/connections";

	private final RestTemplate restTemplate;

	public ConnectorServiceRestClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Override
	public List<UUID> fetchFollowers(UUID authorId) {

		String url = UriComponentsBuilder.fromUriString(GET_CONNECTIONS_BY_USER)
										 .queryParam("userId", authorId.toString())
										 .encode().build().toUriString();

		UserConnection[] userConnections = restTemplate.getForObject(
				url,
				UserConnection[].class);

		if (userConnections == null) {
			return List.of();
		}

		return Stream.of(userConnections)
					 .map(UserConnection::getUser2)
					 .collect(toList());
	}
}
