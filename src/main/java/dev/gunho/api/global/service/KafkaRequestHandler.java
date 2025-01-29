package dev.gunho.api.global.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gunho.api.global.dto.KafkaMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaRequestHandler {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "request-topic", groupId = "response-handler-group")
    public void handleRequest(String record) {
        try {
            // 요청 메시지를 KafkaMessage 객체로 역직렬화
            KafkaMessage request = objectMapper.readValue(record, KafkaMessage.class);

            // 요청 처리 로직 작성
            String processedPayload = "Processed on 8081 Server: " + request.getPayload();

            // 응답 메시지 생성
            KafkaMessage response = new KafkaMessage(request.getRequestId(), processedPayload);

            // KafkaMessage를 JSON 형식으로 변환
            String responseJson = objectMapper.writeValueAsString(response);

            // 응답 메시지를 response-topic으로 전송
            kafkaTemplate.send("response-topic", request.getRequestId(), responseJson);

            System.out.println("Processed and responded for Request ID: " + request.getRequestId());
        } catch (JsonProcessingException e) {
            System.err.println("Failed to process message: " + e.getMessage());
        }
    }
}
