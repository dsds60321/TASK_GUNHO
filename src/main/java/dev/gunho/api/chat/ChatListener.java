package dev.gunho.api.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gunho.api.chat.dto.ChatDto;
import dev.gunho.api.chat.service.ChatService;
import dev.gunho.api.global.constant.GlobalConstant;
import dev.gunho.api.global.dto.KafkaMessage;
import dev.gunho.api.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatListener {

    private final ChatService chatService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topicPattern = "chat-.*")
    public void send(String message) {
        log.info("Message received: {}", message);
        try {
            KafkaMessage<ChatDto> kafkaMessage = objectMapper.readValue(message, new TypeReference<KafkaMessage<ChatDto>>() {});
            chatService.send(kafkaMessage.getPayload());
        } catch (Exception e) {
            String errorMessage = "ChatListener.send Error: " + e.getMessage();
            log.error(errorMessage);
            redisService.setHash(GlobalConstant.REDIS_ERROR_KEY, "ChatListener.send 오류", errorMessage);
        }
    }

}
