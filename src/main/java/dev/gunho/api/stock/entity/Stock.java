package dev.gunho.api.stock.entity;

import dev.gunho.api.global.entity.BaseTimeEntity;
import dev.gunho.api.rule.entity.Rule;
import dev.gunho.api.user.entity.User;
import jakarta.persistence.*;
import jdk.jfr.Description;
import lombok.*;

@Description("주식 테이블")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(name = "stock")
public class Stock extends BaseTimeEntity {

    @Id
    @Column(name = "idx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idx;

    @Column(nullable = false)
    private String symbol;

    private int quantity;

    private String currency;

    private Double averagePrice; // 평단가

    private Double buyPrice; // 매수 목표가

    private Double sellPrice; // 매도 목표가


    // User와의 N:1 관계 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx") // 외래 키
    private User user;

    // Stock과 Rule의 1:N 관계 추가
    @OneToOne(mappedBy = "stock", cascade = CascadeType.REMOVE)
    private Rule rule;

}
