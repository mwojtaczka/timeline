package coma.maciej.wojtaczka.timeline.messaging;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
class KafkaConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate() {
		Map<String, Object> publisherProperties = new HashMap<>();
		publisherProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

		DefaultKafkaProducerFactory<String, String> producerFactory =
				new DefaultKafkaProducerFactory<>(publisherProperties, new StringSerializer(), new StringSerializer());

		return new KafkaTemplate<>(producerFactory);
	}
}
