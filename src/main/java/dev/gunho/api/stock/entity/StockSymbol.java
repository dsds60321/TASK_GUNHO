package dev.gunho.api.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.context.annotation.Description;

import java.math.BigDecimal;

@Description("미국 주식 Symbol 코드성 테이블")
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(name = "stock_symbol")
public class StockSymbol {

    @Id
    @Column(name = "symbol")
    private String symbol;

    @Column(name = "sector")
    private String sector;

    @Column(name = "industry")
    private String industry;

    @Column(name = "market_cap", precision = 20, scale = 2)
    private BigDecimal marketCap;

    @Column(name = "last_sale", precision = 20, scale = 2)
    private BigDecimal lastSale;

    private String country;

    private String name;

}