package dev.gunho.api.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.gunho.api.stock.entity.StockSymbol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockDto {

    private String symbol;
    @JsonProperty("country")
    private String country;
    @JsonProperty("industry")
    private String industry;
    @JsonProperty("marketCap")
    private String marketCap;
    @JsonProperty("name")
    private String name;
    @JsonProperty("sector")
    private String sector;
    @JsonProperty("lastsale")
    private String lastSale;
    @JsonProperty("volume")
    private String volume;

    public StockSymbol mapToEntity(StockDto dto) {
        Double marketCapValue = null;
        if (dto.getMarketCap() != null && !dto.getMarketCap().isEmpty()) {
            marketCapValue = Double.parseDouble(dto.getMarketCap().replaceAll(",", ""));
        }

        return StockSymbol.builder()
                .symbol(dto.getSymbol())
                .country(dto.getCountry())
                .industry(dto.getIndustry())
                .marketCap(BigDecimal.valueOf(marketCapValue))
                .name(dto.getName())
                .sector(dto.getSector())
                .build();
    }

}
