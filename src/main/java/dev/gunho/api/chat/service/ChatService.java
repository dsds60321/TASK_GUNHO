package dev.gunho.api.chat.service;

import dev.gunho.api.chat.dto.ChatDto;
import dev.gunho.api.global.service.RedisService;
import dev.gunho.api.global.util.UTIL;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String CHAT_KEY = "CHAT:%s";

    private final RedisService redisService;


    public void send(ChatDto chatDto) {
        String chatRedisKey = String.format(CHAT_KEY, chatDto.getRoomIdx());
        Map<String, String> data = Map.of(
                "userId", chatDto.getUserId(),
                "regDate", UTIL.getCurrentDate(),
                "message", chatDto.getMessage()
        );

        redisService.saveToStream(chatRedisKey, data);
    }
}
