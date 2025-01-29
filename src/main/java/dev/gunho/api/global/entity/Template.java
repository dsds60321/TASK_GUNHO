package dev.gunho.api.global.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.context.annotation.Description;

@Description("HTML 템플렛 테이블")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(name = "template")
public class Template {

    @Id
    @Column(name = "idx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idx;

    @Column(nullable = false)
    private String id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 128)
    private String subject;

    @Column(name = "from_email", length = 128)
    private String from;

    @Column(name = "to_email",length = 128)
    private String to;

    private String bcc;

}
