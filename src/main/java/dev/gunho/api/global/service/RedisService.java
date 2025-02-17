package dev.gunho.api.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
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


    /**
     * Redis Stream에 데이터 저장
     * @param streamKey Stream의 Key (채팅방 ID 등)
     * @param messageData 메시지 데이터 (Map 형태로 전달)
     * @return 저장된 RecordId
     */
    public RecordId saveToStream(String streamKey, Map<String, String> messageData) {
        return stringRedisTemplate.opsForStream()
                .add(MapRecord.create(streamKey, messageData));
    }

    /**
     * Redis Stream에서 데이터 읽기
     * @param streamKey Stream의 Key
     * @param lastId 읽기를 시작할 ID (0-0 부터 시작하는 경우 모든 데이터, 또는 마지막 ID 이후 데이터를 읽음)
     * @return 읽은 데이터 리스트
     */
    public List<MapRecord<String, Object, Object>> readFromStream(String streamKey, String lastId) {
        return stringRedisTemplate.opsForStream()
                .read(StreamOffset.create(streamKey, ReadOffset.from(lastId)));
    }


    /**
     * Redis Stream 데이터 읽고 최신 메시지 ID 반환
     * @param streamKey Stream의 Key
     * @param lastId 읽기를 시작할 ID
     * @return 최신 메시지 ID
     */
    public String readAndSaveLastReadId(String streamKey, String lastId) {
        List<MapRecord<String, Object, Object>> records = readFromStream(streamKey, lastId);
        if (!records.isEmpty()) {
            // 마지막 메시지의 ID 저장
            return records.get(records.size() - 1).getId().toString();
        }
        return lastId; // 데이터 없으면 lastId 반환
    }


}
