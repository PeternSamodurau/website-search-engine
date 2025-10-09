package com.example.skillboxsixapp;


import com.example.skillboxsixapp.listener.OrderStatusListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class OrderServiceIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.3")
    );

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "test-group-" + UUID.randomUUID());
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KafkaTemplate<String, OrderStatusEvent> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private OrderStatusListener orderStatusListener;

    @Value("${app.kafka.order-topic}")
    private String orderTopic;

    @Value("${app.kafka.order-status-topic}")
    private String orderStatusTopic;

    private Consumer<String, OrderStatusEvent> testConsumer;

    @BeforeEach
    void setUp() {

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-group-" + UUID.randomUUID(), "true");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, OrderStatusEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new JsonDeserializer<>(OrderStatusEvent.class, objectMapper));
        testConsumer = consumerFactory.createConsumer();
        testConsumer.subscribe(java.util.Collections.singletonList(orderTopic));
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    void whenPostOrder_thenMessageSentToKafka() {
        // GIVEN
        OrderDTO requestDto = new OrderDTO();
        requestDto.setProductName("Test Book");
        requestDto.setQuantity(5);

        // WHEN
        ResponseEntity<OrderDTO> response = restTemplate.postForEntity("/api/orders", requestDto, OrderDTO.class);

        // THEN
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProductName()).isEqualTo("Test Book");

        ConsumerRecord<String, OrderStatusEvent> singleRecord = KafkaTestUtils.getSingleRecord(testConsumer, orderTopic, Duration.ofSeconds(10));

        assertThat(singleRecord).isNotNull();
        OrderStatusEvent receivedEvent = singleRecord.value();
        assertThat(receivedEvent.getStatus()).isEqualTo(Status.CREATED);
        assertThat(receivedEvent.getOrder().getProductName()).isEqualTo("Test Book");
        assertThat(receivedEvent.getOrder().getQuantity()).isEqualTo(5);
    }

    @Test
    void whenMessageSentToStatusTopic_thenListenerIsCalled() {
        // GIVEN
        OrderDTO orderDto = new OrderDTO(UUID.randomUUID(), "Completed Book", 1, LocalDateTime.now());
        OrderStatusEvent eventToSend = new OrderStatusEvent(UUID.randomUUID(), orderDto, Status.COMPLETED, LocalDateTime.now());
        String key = eventToSend.getOrder().getOrderId().toString();

        // WHEN
        kafkaTemplate.send(orderStatusTopic, key, eventToSend);

        // THEN
        ArgumentCaptor<OrderStatusEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusEvent.class);

        verify(orderStatusListener, timeout(10000).times(1))
                .listen(eventCaptor.capture(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong());

        OrderStatusEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(capturedEvent.getOrder().getProductName()).isEqualTo("Completed Book");
        assertThat(capturedEvent.getOrder().getOrderId().toString()).isEqualTo(key);
    }
}