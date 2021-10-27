package coma.maciej.wojtaczka.timeline.rest.client;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

	@Bean
	RestTemplate restTemplate(RestTemplateBuilder builder) {

		return builder.rootUri("http://localhost:8081") //TODO
			   .build();
	}
}
