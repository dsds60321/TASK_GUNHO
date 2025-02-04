package dev.gunho.api.rule.entity;

import dev.gunho.api.global.entity.BaseTimeEntity;
import dev.gunho.api.stock.entity.Stock;
import dev.gunho.api.user.entity.User;
import jakarta.persistence.*;
import jdk.jfr.Description;
import lombok.*;

import java.math.BigDecimal;

@Description("Rule 테이블")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(name = "rule")
public class Rule extends BaseTimeEntity {

    @Id
    @Column(name = "idx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idx;

    private String id;

    private int buyPrice;
    // 증가
    @Column(precision = 3, scale = 2)
    private BigDecimal buyPercentage;

    // 매도
    @Column(precision = 3, scale = 2)
    private BigDecimal sellPercentage;

    private int sellPrice;

    // Stock와의 N:1 관계 추가
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_idx") // 외래 키
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx") // 외래 키
    private User user;

}
