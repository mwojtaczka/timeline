package coma.maciej.wojtaczka.timeline.utils;

import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnBean(EmbeddedKafkaBroker.class)
public class KafkaTestListener implements DisposableBean {

	private final EmbeddedKafkaBroker broker;

	private Map<String, ConcurrentLinkedQueue<ConsumerRecord<String, String>>> recordsPerTopic;
	private Map<String, CountDownLatch> latchPerTopic;
	private Set<KafkaMessageListenerContainer<String, String>> containers = new HashSet<>();

	public KafkaTestListener(EmbeddedKafkaBroker broker) {
		this.broker = broker;
		recordsPerTopic = new HashMap<>();
		latchPerTopic = new HashMap<>();
	}

	public void listenToTopic(String topic, int expectedMsgCount) {
		recordsPerTopic.put(topic, new ConcurrentLinkedQueue<>());
		latchPerTopic.put(topic, new CountDownLatch(expectedMsgCount));
		setupContainer(topic);
	}

	private void setupContainer(String topic) {
		ContainerProperties containerProperties = new ContainerProperties(topic);
		Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), "false", broker);
		DefaultKafkaConsumerFactory<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProperties);
		var container = new KafkaMessageListenerContainer<>(consumer, containerProperties);
		container.setupMessageListener((MessageListener<String, String>) record -> consume(record, topic));
		container.start();
		containers.add(container);
		ContainerTestUtils.waitForAssignment(container, broker.getPartitionsPerTopic());
	}

	public void reset() {
		recordsPerTopic = new HashMap<>();
		latchPerTopic = new HashMap<>();
		containers.forEach(KafkaMessageListenerContainer::stop);
	}

	void consume(ConsumerRecord<String, String> consumerRecord, String topic) {
		ConcurrentLinkedQueue<ConsumerRecord<String, String>> records = recordsPerTopic.get(topic);
		records.add(consumerRecord);
		latchPerTopic.get(topic).countDown();
	}

	@SneakyThrows
	public int msgCount(String topic) {
		latchPerTopic.get(topic).await(500, TimeUnit.MILLISECONDS);
		return recordsPerTopic.get(topic).size();
	}

	@SneakyThrows
	public Optional<String> receiveContentFromTopic(String topic) {
		latchPerTopic.get(topic).await(500, TimeUnit.MILLISECONDS);
		ConsumerRecord<String, String> msg = recordsPerTopic.get(topic).poll();
		if (msg == null) {
			return Optional.empty();
		}
		return Optional.of(msg.value());
	}

	@SneakyThrows
	public boolean noMoreMessagesOnTopic(String topic, long awaitTimeMillis) {
		Thread.sleep(awaitTimeMillis);
		return recordsPerTopic.get(topic).isEmpty();
	}

	@Override
	public void destroy() {
		containers.forEach(KafkaMessageListenerContainer::stop);
	}
}
