package org.example.service;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.example.dto.EmailRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {
    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"));

    @Autowired
    private KafkaTemplate<String, EmailRequest> kafkaTemplate;

    @MockitoSpyBean
    private EmailConsumer emailConsumer;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(
            new ServerSetup(3025, "127.0.0.1", ServerSetup.PROTOCOL_SMTP)
    );

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.consumer.value-deserializer", () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
        registry.add("spring.kafka.consumer.properties.spring.json.value.default.type", () -> "org.example.dto.EmailRequest");

        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> "3025");
        registry.add("spring.mail.protocol", () -> "smtp");

        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");

        registry.add("spring.mail.username", () -> "");
        registry.add("spring.mail.password", () -> "");

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Test
    void testKafkaFlow() throws Exception {
        EmailRequest request = new EmailRequest("test@test.com", "Test Title", "Test Body");

        kafkaTemplate.send("mail-tasks", request);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(emailConsumer, times(1)).consume(any(EmailRequest.class));
        });
    }

    @Test
    void testEmailDelivery() throws Exception {
        String targetEmail = "client@example.com";
        String subject = "Заголовок из Kafka";
        EmailRequest request = new EmailRequest(targetEmail, subject, "Текст письма");

        kafkaTemplate.send("mail-tasks", request);

        await().atMost(Duration.ofSeconds(10)).until(() -> greenMail.getReceivedMessages().length > 0);

        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];

        assertEquals(targetEmail, receivedMessage.getAllRecipients()[0].toString());
        assertEquals(subject, receivedMessage.getSubject());

        String body = (String) receivedMessage.getContent();
        assertEquals("Текст письма", body.trim());
    }
}