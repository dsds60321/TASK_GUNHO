package dev.gunho.api.global.scheduler;

import dev.gunho.api.email.dto.Email;
import dev.gunho.api.email.service.EmailService;
import dev.gunho.api.global.constant.GlobalConstant;
import dev.gunho.api.global.entity.Template;
import dev.gunho.api.global.repository.TemplateRepository;
import dev.gunho.api.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalScheduler {

    private final TemplateRepository templateRepository;
    private final EmailService emailService;
    private final RedisService redisService;



    // 오류에 대한 메일 서비스
    @Scheduled(cron = "0 */1 * * * *")
    public void errorLog() {
        try {

            boolean existsByKey = redisService.existsByKey(GlobalConstant.REDIS_ERROR_KEY);
            if (!existsByKey) {
                return;
            }

            Template errorTemplate = templateRepository.getById("ERROR");
            String content = errorTemplate != null && StringUtils.hasText(errorTemplate.getContent())
                    ? errorTemplate.getContent()
                    : "제목 : %s \n summary : %s";

            Map<Object, Object> entries = redisService.getHashEntries(GlobalConstant.REDIS_ERROR_KEY);
            if (entries == null || entries.isEmpty()) {
                log.info("No error logs found in Redis.");
                return;
            }

            Email email = new Email();
            email.setSubject("[GUNHO.DEV] 오류 알림");
            email.setTo(List.of("dsds60321@gmail.com"));
            email.setFrom("dsds60321@gunho.dev");

            entries.forEach((key, value) -> {
                try {
                    if (key == null || value == null) {
                        log.warn("Null key or value in Redis entry. Key: {}, Value: {}", key, value);
                        return; // Skip null data
                    }

                    String emailContent = content.replace("{title}", key.toString())
                            .replace("{content}", value.toString());

                    email.setContents(emailContent);

                    emailService.sendHtmlEmail(email);
                    redisService.deleteHashKey(GlobalConstant.REDIS_ERROR_KEY, key);
                    log.info("Processed and removed key: {}", key);

                } catch (Exception e) {
                    log.error("Error processing entry: key={}, value={}, error={}", key, value, e.getMessage(), e);
                }
            });


        } catch (Exception e) {
            log.error("GlobalScheduler error in errorLog: {}", e.getMessage(), e);
        }
    }


}
