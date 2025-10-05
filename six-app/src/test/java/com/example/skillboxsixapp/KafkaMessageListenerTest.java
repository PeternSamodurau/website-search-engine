package com.example.skillboxsixapp;

import com.example.skillboxsixapp.model.KafkaMessage;
import com.example.skillboxsixapp.service.KafkaMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
public class KafkaMessageListenerTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.3")
    );

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, KafkaMessage> kafkaTemplate;

    @Autowired
    private KafkaMessageService kafkaMessageService;

    @Value("${app.kafka.kafkaMessageTopic}")
    private String topicName;

    @Test
    void whenSendKafkaMessage_thenHandleMessageByListener() {
        KafkaMessage event = new KafkaMessage();
        event.setId(1L);
        event.setMessage("Message from kafka");
        String key = UUID.randomUUID().toString();

        kafkaTemplate.send(topicName, key, event);

        await()
                .pollInterval(Duration.ofSeconds(3))
                .atMost(10, SECONDS)
                .untilAsserted(() -> {
                    Optional<KafkaMessage> mayBeKafkaMessage = kafkaMessageService.getById(1L);
                    assertThat(mayBeKafkaMessage).isPresent();
                    KafkaMessage kafkaMessage = mayBeKafkaMessage.get();
                    assertThat(kafkaMessage.getMessage()).isEqualTo("Message from kafka");
                    assertThat(kafkaMessage.getId()).isEqualTo(1L);
                });
    }
}
