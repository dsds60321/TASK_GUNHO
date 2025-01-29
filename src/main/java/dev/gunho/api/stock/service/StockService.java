package dev.gunho.api.stock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gunho.api.global.scheduler.GlobalScheduler;
import dev.gunho.api.global.service.RedisService;
import dev.gunho.api.global.service.WebClientService;
import dev.gunho.api.stock.constant.StockRedisKeys;
import dev.gunho.api.stock.dto.StockSymbolDto;
import dev.gunho.api.stock.entity.StockSymbol;
import dev.gunho.api.stock.repository.StockSymbolBulkRepository;
import dev.gunho.api.stock.repository.StockSymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    @Value("${api.alpha-vantage.host}")
    private String alphaVantageApi;

    @Value("${api.alpha-vantage.key}")
    private String alphaVantageApiKey;

    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final WebClientService webClientService;
    private final StockSymbolRepository stockSymbolRepository;
    private final StockSymbolBulkRepository stockSymbolBulkRepository;

    private static final String NASDAQ_BASE = "https://api.nasdaq.com";
    private static final String SYMBOL_URI = "/api/screener/stocks";


    /**
     * symbol update
     */
    @Transactional
    public void updateSymbol() {
        try {
            redisService.setHash(GlobalScheduler.REDIS_ERROR_KEY, "symbol", "nasdaq");
            // Nasdaq API URL 설정
            LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("tableonly", "false");
            queryParams.add("limit", "25");
            queryParams.add("exchange", "NASDAQ");
            queryParams.add("download", "true");

            log.info("Sending GET request to Nasdaq API: {}", NASDAQ_BASE + SYMBOL_URI);

            String jsonResponse = webClientService.get(
                    NASDAQ_BASE,
                    SYMBOL_URI,
                    queryParams,
                    null,
                    String.class
            );

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new RuntimeException("Nasdaq API 응답이 비어 있습니다.");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode rowsNode = rootNode.path("data").path("rows");

            if (rowsNode == null || !rowsNode.isArray()) {
                throw new RuntimeException("Nasdaq API 응답에서 유효한 'rows' 데이터가 없습니다.");
            }

            List<StockSymbolDto> stockDtos = new ArrayList<>();
            rowsNode.forEach(stockData -> {
                if (stockData.hasNonNull("symbol")) {
                    try {
                        StockSymbolDto stockSymbolDto = objectMapper.treeToValue(stockData, StockSymbolDto.class);
                        stockDtos.add(stockSymbolDto);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to map stock data: {}", stockData, e);
                    }


                } else {
                    log.warn("Encountered stock data without symbol: {}", stockData);
                }
            });

            // json -> entity
            List<StockSymbol> stockEntities = stockDtos.stream()
                    .map(dto -> dto.mapToEntity(dto))
                    .collect(Collectors.toList());

            long count = stockSymbolRepository.count();
            if (count > 0) {
                stockSymbolBulkRepository.bulkUpdate(stockEntities);
            } else {
                stockSymbolBulkRepository.bulkInsert(stockEntities);
            }



        } catch (Exception e) {
            String errorMessage = "StockService.updateSymbol Error: " + e.getMessage();
            log.error(errorMessage, e);
            redisService.setHash(GlobalScheduler.REDIS_ERROR_KEY, "StockService.updateSymbol 오류", errorMessage);
        }

    }

    public void daily(String symbol, boolean isFull) {

        if (!StringUtils.hasLength(symbol)) {
            return;
        }

        if (redisService.existsByKey(symbol)) {
            return;
        }

        log.info("Stock Service Start: symbol={}, isFull={}", symbol, isFull);

        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("function", "TIME_SERIES_DAILY");
        queryParams.add("symbol", symbol);
        queryParams.add("apikey", alphaVantageApiKey);

        if (isFull) {
            queryParams.add("outputsize", "full");
        }

        try {

            String response = webClientService.get(alphaVantageApi, "/query", queryParams, null, String.class);
            if (response == null) {
                log.info("Stock Service symbol={} 데이터가 없습니다.", symbol);
                return;
            }
            // JSON 응답 데이터를 Map으로 파싱
            Map<String, Object> responseData = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
            });

            // "Time Series (Daily)" 데이터 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> timeSeriesDaily = (Map<String, Object>) responseData.get("Time Series (Daily)");
            if (timeSeriesDaily == null) {
                log.error("Time Series (Daily) 데이터가 없습니다.");
                return;
            }

            // Redis에 저장: 날짜별 Key와 데이터 저장
            for (Map.Entry<String, Object> entry : timeSeriesDaily.entrySet()) {
                String date = entry.getKey(); // 날짜 (예: "2025-01-22")
                Object dailyData = entry.getValue(); // 날짜에 해당하는 데이터

                try {
                    // Hash의 Key: 날짜, Value: 날짜별 데이터 직렬화(JSON)
                    String data = objectMapper.writeValueAsString(dailyData);

                    // Redis Hash에 저장
                    redisService.setHash(symbol, date, data);
                } catch (Exception e) {
                    log.error("Redis 저장 중 예외 발생: symbol={}, date={}, error={}", symbol, date, e.getMessage());
                }

            }

            log.info("{} redis 저장", symbol);
        } catch (Exception e) {
            log.error("StockService.getInt Exception: {}", e.getMessage());
        }
    }

    public void periodProcessing(int TOP_STOCK) {
        List<String> symbols = redisService.getRangeList(0, TOP_STOCK, StockRedisKeys.STOCK_SYMBOL_LIST);
        symbols.forEach(symbol -> {
            Map<Object, Object> entries = redisService.getHashEntries(symbol);

        });
    }

}
