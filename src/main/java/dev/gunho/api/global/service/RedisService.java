package dev.gunho.api.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;

    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public void addAllToList(String key, List<String> values) {
        stringRedisTemplate.opsForList().rightPushAll(key, values);
    }


    // Redis List - 데이터 추가 (맨 뒤에 추가: RPUSH)
    public void addToListRight(String key, String value) {
        stringRedisTemplate.opsForList().rightPush(key, value);
    }

    // Redis List - 전체 데이터 가져오기
    public List<String> getList(String key) {
        return stringRedisTemplate.opsForList().range(key, 0, -1);
    }

    public List<String> getRangeList(int start, int end, String key) {
        return stringRedisTemplate.opsForList().range(key, start, end);
    }

    public void setHash(String hashKey, String key, String value) {
        stringRedisTemplate.opsForHash().put(hashKey, key, value);
    }

    public boolean existsByKey(String symbol) {
        return stringRedisTemplate.hasKey(symbol);
    }

    public Map<Object, Object> getHashEntries(String key) {
        return stringRedisTemplate.opsForHash().entries(key);
    }

    public void deleteHashKey(String hashKey, Object key) {
        stringRedisTemplate.opsForHash().delete(hashKey, key);
    }

    public boolean isHashKeyExists(String hashName, String key) {
        return stringRedisTemplate.opsForHash().hasKey(hashName, key);
    }

    // Hash에 여러 필드-값 쌍 저장
    public void addAllToHash(String hashKey, Map<String, Object> data) {
        stringRedisTemplate.opsForHash().putAll(hashKey, data);
    }

}
