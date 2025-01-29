package dev.gunho.api.stock.scheduler;

import dev.gunho.api.global.service.RedisService;
import dev.gunho.api.stock.service.StockService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockScheduler {

    private final RedisService redisService;
    private final StockService stockService;
    private final int TOP_STOCK = 10;

    @PostConstruct
    @Scheduled(cron = "0 5 5 * * ?", zone = "Asia/Seoul")
    public void updateSymbol() {
        log.info("Stock Symbol Start");
        stockService.updateSymbol();
    }

    // 미국 주식 시장 종료 15분 후 실행 5시 15분
    @PostConstruct
    @Scheduled(cron = "0 15 5 * * ?", zone = "Asia/Seoul")
    public void topStockCheck() {
        log.info("Stock End Check Start");
        List<String> stockSymbols = redisService.getRangeList(0, TOP_STOCK, "STOCK_SYMBOLS");
        stockSymbols.forEach(stockSymbol -> stockService.daily(stockSymbol, true));
    }

    // HASH에 받은 데이터 재 가공
//    @Scheduled(cron = "0 30 5 * * ?", zone = "Asia/Seoul")
//    public void stockProcess() {
//        stockService.periodProcessing(TOP_STOCK);
//    }

}
