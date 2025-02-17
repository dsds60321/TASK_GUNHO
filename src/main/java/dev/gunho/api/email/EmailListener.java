package dev.gunho.api.email;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gunho.api.email.dto.EmailDto;
import dev.gunho.api.email.service.EmailService;
import dev.gunho.api.global.dto.KafkaMessage;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailListener {

    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "email-topic", groupId = "producer-group")
    public void listen(String message) throws MessagingException {
        try {
            KafkaMessage<EmailDto> kafkaMessage = objectMapper.readValue(message, new TypeReference<KafkaMessage<EmailDto>>() {});
            EmailDto emailDto = kafkaMessage.getPayload();

            emailService.sendHtmlEmail(emailDto);
            log.info("Email sent: {}", emailDto);
        } catch (Exception e) {
            // 포괄적인 예외 처리
            System.err.println("알 수 없는 에러 발생: " + e.getMessage());
        }

    }
}
