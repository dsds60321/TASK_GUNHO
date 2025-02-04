package dev.gunho.api.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.gunho.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jdk.jfr.Description;
import lombok.*;

@Description("user JWT token 테이블")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(name = "auth")
public class Auth extends BaseTimeEntity {

    @Id
    @Column(name = "idx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idx;

    @Column(nullable = false)
    private String accessToken;

    @Column(nullable = false)
    private String refreshToken;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public void updateAccessToken(String newAccessToken) {
        this.accessToken = newAccessToken;
    }

    public void updateRefreshToken(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
    }
}
