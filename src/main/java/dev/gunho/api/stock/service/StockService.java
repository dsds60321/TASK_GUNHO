package dev.gunho.api.stock.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gunho.api.global.constant.GlobalConstant;
import dev.gunho.api.global.scheduler.GlobalScheduler;
import dev.gunho.api.global.service.RedisService;
import dev.gunho.api.global.service.WebClientService;
import dev.gunho.api.stock.constant.StockConstants;
import dev.gunho.api.stock.dto.StockDto;
import dev.gunho.api.stock.entity.Stock;
import dev.gunho.api.stock.entity.StockSymbol;
import dev.gunho.api.stock.repository.StockRepository;
import dev.gunho.api.stock.repository.StockSymbolBulkRepository;
import dev.gunho.api.stock.repository.StockSymbolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final StockSymbolBulkRepository stockSymbolBulkRepository;
    private final StockSymbolRepository stockSymbolRepository;
    private final StockRepository stockRepository;

    private static final String NASDAQ_BASE = "https://api.nasdaq.com";
    private static final String SYMBOL_URI = "/api/screener/stocks";


    /**
     * symbol update
     */
    @Transactional
    public void updateSymbol() {
        try {
//            List<String> topSymbols = redisService.getRangeList(0, Stock.TOP_STOCK, Stock.STOCK_SYMBOL_LIST);
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

            // TODO : 추후 유저 선택한 주식에 대해 추적할지 검토
            List<StockDto> stockDtos = new ArrayList<>();

            rowsNode.forEach(stockData -> {
                if (stockData.hasNonNull("symbol")) {
                    try {
                        // DB 저장을 위해 데이터 가공
                        StockDto stockDto = objectMapper.treeToValue(stockData, StockDto.class);
                        stockDtos.add(stockDto);

                        // REDIS 저장을 위해 데이터 가공 ==================================================

                        // SYMBOL_LIST
                        redisService.addToListRight(StockConstants.STOCK_SYMBOL_LIST, stockDto.getSymbol());

                        // 전체 SYMBOL PRICE, VOLUME redis
//                        if (topSymbols.contains(stockDto.getSymbol())) {
                            String dailyPriceRedisKey = String.format(StockConstants.STOCK_DAILY_PRICE_SYMBOL, stockDto.getSymbol());
                            String dailyVolumeRedisKey = String.format(StockConstants.STOCK_DAILY_VOLUME_SYMBOL, stockDto.getSymbol());
                            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                            // 달러 표시 제거
                            if (stockDto.getLastSale() != null && stockDto.getLastSale().startsWith("$")) {
                                stockDto.setLastSale(stockDto.getLastSale().replace("$", ""));
                            }

                            // 중복 방지를 위해 hash에 동일 날짜가 존재하지 않는 경우만 추가
                            if (!redisService.isHashKeyExists(dailyPriceRedisKey, currentDate)) {
                                redisService.setHash(dailyPriceRedisKey, currentDate, stockDto.getLastSale());
                            }

                            if (!redisService.isHashKeyExists(dailyVolumeRedisKey, currentDate)) {
                                redisService.setHash(dailyVolumeRedisKey, currentDate, stockDto.getVolume());
                            }
//                        }

                    } catch (JsonProcessingException e) {
                        log.error("Failed to map stock data: {}", stockData, e);
                    }

                } else {
                    log.warn("Encountered stock data without symbol: {}", stockData);
                }
            });

            // AACG 26880109.00
            // **marketCap 기준 내림차순 정렬**
            List<StockDto> sortedStockDtos = stockDtos.stream()
                    .sorted((dto1, dto2) -> {
                        Double cap1 = parseMarketCap(dto1.getMarketCap());
                        Double cap2 = parseMarketCap(dto2.getMarketCap());
                        return Double.compare(cap2, cap1); // 내림차순 정렬
                    })
                    .collect(Collectors.toList());

            // **Redis 리스트 초기화 및 재저장**
            redisService.delete(StockConstants.STOCK_SYMBOL_LIST); // 기존 Redis 리스트 삭제
            redisService.delete(StockConstants.STOCK_SYMBOL_HASH); // 기존 Redis 리스트 삭제

            sortedStockDtos.forEach(stockDto -> {
                redisService.addToListRight(StockConstants.STOCK_SYMBOL_LIST, stockDto.getSymbol());
                redisService.setHash(StockConstants.STOCK_SYMBOL_HASH, stockDto.getSymbol(), stockDto.getName());
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
            redisService.setHash(GlobalConstant.REDIS_ERROR_KEY, "StockService.updateSymbol 오류", errorMessage);
        }

    }

    // 매일 이전 주식 장 확인
    public void dailyToJson(String symbol, boolean isFull) {
        String redisKey = String.format(StockConstants.STOCK_DAILY_JSON_SYMBOL, symbol);
        log.info("Stock Service Start: symbol={}, isFull={}", symbol, isFull);

        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("function", "TIME_SERIES_DAILY");
        queryParams.add("symbol", symbol);
        queryParams.add("apikey", alphaVantageApiKey);

        if (isFull) {
            queryParams.add("outputsize", "full");
        }

        try {
            // API CALL
            String response = webClientService.get(alphaVantageApi, "/query", queryParams, null, String.class);

            if (response == null) {
                log.info("Stock Service symbol={} 데이터가 없습니다.", symbol);
                return;
            }
            // JSON 응답 데이터를 Map으로 파싱
            Map<String, Object> responseData = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});

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
                    // 데이터 변환
                    Map<String, String> transformedData = transformDailyData(dailyData);

                    // Hash의 Key: 날짜, Value: 날짜별 데이터 직렬화(JSON)
                    String data = objectMapper.writeValueAsString(transformedData);

                    // Redis Hash에 저장
                    redisService.setHash(redisKey, date, data);
                } catch (Exception e) {
                    log.error("Redis 저장 중 예외 발생: symbol={}, date={}, error={}", symbol, date, e.getMessage());
                }

            }

            log.info("{} redis 저장", symbol);
        } catch (Exception e) {
            log.error("StockService.getInt Exception: {}", e.getMessage());
        }
    }

    public void annualProcessing(int TOP_STOCK) {
        List<String> symbols = redisService.getRangeList(0, TOP_STOCK, StockConstants.STOCK_SYMBOL_LIST);

        symbols.forEach(symbol -> {
            // SYMBOL 년도별
            Map<String, List<Double>> yearlyCloseValues = new HashMap<>();

            // SYMBOL 데이터 추출
            Map<Object, Object> entries = redisService.getHashEntries(symbol);
            if (entries == null || entries.isEmpty()) {
                return;
            }

            entries.forEach((key, value) -> {

                try {

                    String date = key.toString();
                    String json = value.toString();

                    String year = date.substring(0, 4);
                    Map<String, String> dataMap = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
                    });

                    if (dataMap.containsKey("close")) {
                        double close = Double.parseDouble(dataMap.get("close"));
                        yearlyCloseValues.computeIfAbsent(year, k -> new ArrayList<>()).add(close);
                    }

                } catch (Exception e) {
                    String errorMessage = "StockService.annualProcessing Error: " + e.getMessage();
                    redisService.setHash(GlobalConstant.REDIS_ERROR_KEY, "StockService.annualProcessing 오류", errorMessage);
                    log.error(errorMessage, e);
                }
            });

            String redisKey = String.format(StockConstants.STOCK_ANNUAL_PRICE_SYMBOL, symbol);
            yearlyCloseValues.forEach((year, closeValues) -> {
                double average = closeValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                try {
                    // Redis 저장
                    redisService.setHash(redisKey, year, String.valueOf(average));
                    log.info("연도 {}의 close 평균 {}를 Redis 리스트에 저장했습니다.", year, average);
                } catch (Exception e) {
                    log.error("Redis 리스트 저장 중 오류 발생: year={}, average={}, error={}", year, average, e.getMessage());
                }
            });

        });
    }

    /**
     * 1분 간격 주식 크롤링
     */
    public void crawling() {
        // 등록된 심볼에서만 1분 단위 시장가 조회
        List<Stock> stocks = stockRepository.findAll();
        stocks.forEach(stock -> {
            String url = "https://finance.yahoo.com/quote/%s";
            String symbol = stock.getSymbol();
            url = String.format(url, symbol);
            try {
                // URL에 연결하여 HTML 문서 가져오기
                Document document = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36") // User-Agent 헤더 추가
                        .timeout(5000) // 연결 timeout 설정 (5초)
                        .get();

                // qsp-pre-price 주식 시장 시작전
                // qsp-price 주식 시장 시작
                Element priceElement = document.selectFirst("[data-testid='qsp-pre-price']");
                if (priceElement == null) {
                    priceElement = document.selectFirst("[data-testid='qsp-price']");
                }

                if (priceElement != null) {
                    String preMarketPrice = priceElement.text();
                    String redisPriceKey = String.format(StockConstants.STOCK_DAILY_PRICE_SYMBOL, symbol);
                    redisService.set(redisPriceKey, preMarketPrice);

                    log.info("REDIS PRICE 추가 : {} : price : {}", symbol, preMarketPrice);
                } else {
                    log.info("{} 데이터를 찾을 수 없습니다.", symbol);
                    redisService.setHash(GlobalConstant.REDIS_ERROR_KEY, "크롤링 파싱 실패", "크롤링 확인을 위해 해당 링크를 방문해주세요 : <a location.href='" + url + "'>" + url +"</a>" );
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("크롤링 중 오류 발생: " + e.getMessage());
            }
        });


    }

    /**
     * 미국 주식시장이 열려있는지 확인하는 메서드
     * @return boolean
     */
    private boolean isUsMarketOpen() {
        // 미 동부 표준시 기준: 오전 9:30 - 오후 4:00
        TimeZone estTimeZone = TimeZone.getTimeZone("America/New_York");
        Calendar currentTime = Calendar.getInstance(estTimeZone);

        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        // 주식 시장은 9:30 AM ~ 4:00 PM에 열림
        return (hour > 9 || (hour == 9 && minute >= 30)) && hour < 16;
    }



    /**
     * 숫자와 점을 제거하여 변환
     */
    private Map<String, String> transformDailyData(Object dailyData) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> originalData = objectMapper.convertValue(dailyData, new TypeReference<Map<String, String>>() {});

            Map<String, String> transformedData = new HashMap<>();
            originalData.forEach((key, value) -> {
                String transformedKey = key.replaceFirst("^[\\d]+\\.\\s*", ""); // 숫자+점+공백 제거
                transformedData.put(transformedKey, value);
            });

            log.debug("Original Data: {}", originalData); // 변환 전 데이터 확인
            log.debug("Transformed Data: {}", transformedData); // 변환 후 데이터 확인
            return transformedData;
        } catch (Exception e) {
            log.error("dailyData 변환 중 예외 발생: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    private Double parseMarketCap(String marketCap) {
        try {
            if (marketCap == null || marketCap.isEmpty()) {
                return 0.0;
            }
            // 콤마 제거 후 Double 변환
            return Double.parseDouble(marketCap.replace(",", ""));
        } catch (NumberFormatException e) {
            log.warn("Invalid marketCap format: {}", marketCap, e);
            return 0.0;
        }
    }
}
