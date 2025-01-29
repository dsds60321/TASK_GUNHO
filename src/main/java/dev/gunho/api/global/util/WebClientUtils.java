package dev.gunho.api.global.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebClientUtils {

    private final ObjectMapper objectMapper;

    /**
     * DTO를 MultiValueMap으로 변환
     */
    public MultiValueMap<String, String> convertToMultiValueMap(Object dto) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            Map<String, Object> map = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {});

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof List) {
                    List<?> list = (List<?>) entry.getValue();
                    for (Object obj : list) {
                        params.add(entry.getKey(), obj.toString());
                    }
                } else if (entry.getValue() instanceof Map) {
                    params.add(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
                } else {
                    params.add(entry.getKey(), entry.getValue().toString());
                }
            }
            return params;
        } catch (Exception e) {
            throw new IllegalStateException("Object 변환 중 오류 발생", e);
        }
    }

}
