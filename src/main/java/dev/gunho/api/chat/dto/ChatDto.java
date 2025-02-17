package dev.gunho.api.chat.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatDto {
    long roomIdx;
    long userIdx;
    String message;
    String userId;
    LocalDateTime regDate;
}
