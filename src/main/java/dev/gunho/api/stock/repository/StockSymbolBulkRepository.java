package dev.gunho.api.stock.repository;

import dev.gunho.api.stock.entity.StockSymbol;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class StockSymbolBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void bulkInsert(List<StockSymbol> stockSymbols) {
        String sql = "INSERT INTO stock_symbol (symbol, market_cap, name, country, sector, industry) VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, stockSymbols, 1000, (ps, stockSymbol) -> {
            ps.setString(1, stockSymbol.getSymbol());
            ps.setObject(2, stockSymbol.getMarketCap());
            ps.setString(3, stockSymbol.getName());
            ps.setString(4, stockSymbol.getCountry());
            ps.setString(5, stockSymbol.getSector());
            ps.setString(6, stockSymbol.getIndustry());
        });
    }

    @Transactional
    public void bulkUpdate(List<StockSymbol> stockSymbols) {
        String sql = """
        UPDATE stock_symbol
        SET 
            market_cap = CASE symbol 
                %s 
                ELSE market_cap END,
            name = CASE symbol 
                %s 
                ELSE name END,
            country = CASE symbol 
                %s 
                ELSE country END,
            sector = CASE symbol 
                %s 
                ELSE sector END,
            industry = CASE symbol 
                %s 
                ELSE industry END
        WHERE symbol IN (%s)
    """;

        int batchSize = 50;  // Batch 처리 개수
        for (int i = 0; i < stockSymbols.size(); i += batchSize) {
            List<StockSymbol> batchList = stockSymbols.subList(i, Math.min(i + batchSize, stockSymbols.size()));

            // CASE-WHEN 절 생성
            String marketCapCase = generateCaseWhenClause(batchList, StockSymbol::getMarketCap);
            String nameCase = generateCaseWhenClause(batchList, StockSymbol::getName);
            String countryCase = generateCaseWhenClause(batchList, StockSymbol::getCountry);
            String sectorCase = generateCaseWhenClause(batchList, StockSymbol::getSector);
            String industryCase = generateCaseWhenClause(batchList, StockSymbol::getIndustry);

            // WHERE IN 절 생성
            String whereSymbols = batchList.stream()
                    .map(stock -> "'" + stock.getSymbol() + "'")
                    .collect(Collectors.joining(", "));

            // 최종 SQL 생성
            String formattedSql = String.format(sql, marketCapCase, nameCase, countryCase, sectorCase, industryCase, whereSymbols);

            // 쿼리 실행
            jdbcTemplate.update(formattedSql);
//            jdbcTemplate.batchUpdate(formattedSql);
        }
    }

    private String generateCaseWhenClause(List<StockSymbol> stockSymbols, java.util.function.Function<StockSymbol, ?> valueExtractor) {
        return stockSymbols.stream()
                .map(stock -> {
                    Object value = valueExtractor.apply(stock);
                    if (value == null) {
                        return String.format("WHEN '%s' THEN NULL", stock.getSymbol());
                    } else if (value instanceof String) {
                        return String.format("WHEN '%s' THEN '%s'", stock.getSymbol(), value.toString().replace("'", "''"));
                    } else {
                        return String.format("WHEN '%s' THEN %s", stock.getSymbol(), value.toString());
                    }
                })
                .collect(Collectors.joining(" "));
    }




}
