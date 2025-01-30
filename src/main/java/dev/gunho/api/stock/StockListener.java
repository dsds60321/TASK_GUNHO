package dev.gunho.api.stock;

import dev.gunho.api.global.service.RedisService;
import dev.gunho.api.stock.constant.Stock;
import dev.gunho.api.stock.service.StockService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockListener {

    private final RedisService redisService;
    private final StockService stockService;

    @KafkaListener(topics = "top-topic", groupId = "stock-group")
    public void listen(String message) throws MessagingException {
        List<String> stockSymbols = redisService.getRangeList(0, Stock.TOP_STOCK, Stock.STOCK_SYMBOL_LIST);
        stockSymbols.forEach(stockSymbol -> stockService.dailyToJson(stockSymbol, true));
    }
}
