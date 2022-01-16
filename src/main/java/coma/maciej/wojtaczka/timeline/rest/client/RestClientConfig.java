package coma.maciej.wojtaczka.timeline.rest.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

	@Bean
	@Qualifier("AnnBrd")
	RestTemplate restTemplateAnnBrd(RestTemplateBuilder builder, @Value("${announcement.board.host:localhost:8081}") String annBrdHost) {

		return builder.rootUri(String.format("http://%s", annBrdHost)) //TODO
					  .build();
	}

	@Bean
	@Qualifier("UsrConn")
	RestTemplate restTemplateUsrConn(RestTemplateBuilder builder, @Value("${user.connector.host:localhost:8081}") String usrConnHost) {

		return builder.rootUri(String.format("http://%s", usrConnHost)) //TODO
					  .build();
	}
}
