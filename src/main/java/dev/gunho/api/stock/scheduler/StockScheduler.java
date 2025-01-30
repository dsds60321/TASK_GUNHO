package dev.gunho.api.stock.scheduler;

import dev.gunho.api.global.service.RedisService;
import dev.gunho.api.stock.constant.Stock;
import dev.gunho.api.stock.service.StockService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockScheduler {

    private final RedisService redisService;
    private final StockService stockService;

    // SYMBOL 업데이트
    @PostConstruct
    @Scheduled(cron = "0 5 5 * * ?", zone = "Asia/Seoul")
    public void updateSymbol() {
        log.info("Stock Symbol Start");
        stockService.updateSymbol();
    }

    // 미국 주식 시장 종료 15분 후 실행 5시 15분 TOP 10 주식 레디스 설정
    @PostConstruct
    @Scheduled(cron = "0 15 5 * * ?", zone = "Asia/Seoul")
    public void topStockCheck() {
        log.info("Stock End Check Start");
        List<String> stockSymbols = redisService.getRangeList(0, Stock.TOP_STOCK, Stock.STOCK_SYMBOL_LIST);
        stockSymbols.forEach(stockSymbol -> stockService.dailyToJson(stockSymbol, true));
    }

    @Description("매년 미주 통계 집계")
    @PostConstruct
    @Scheduled(cron = "0 30 5 1 1 ?", zone = "Asia/Seoul")
    public void stockProcess() {
        // 연별 집계
        stockService.annualProcessing(Stock.TOP_STOCK);
    }

}
