package dev.gunho.api.chat.mapper;

import dev.gunho.api.chat.dto.ChatDto;
import dev.gunho.api.chat.dto.ChatPayload;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @Builder(disableBuilder = true))
public interface ChatMapper {

    ChatMapper INSTANCE = Mappers.getMapper(ChatMapper.class);


    ChatPayload toPayload(ChatDto chatDto);
}
