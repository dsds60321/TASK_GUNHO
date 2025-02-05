package dev.gunho.api.email.service;

import dev.gunho.api.email.dto.Email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {


    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * 텍스트 이메일 전송 메서드
     */
    public void sendTextEmail(Email email) throws MessagingException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email.getTo().toArray(new String[0]));                      // 수신자 이메일
        message.setSubject(email.getSubject());            // 이메일 제목
        message.setText(email.getContents());                  // 이메일 본문
        message.setFrom(email.getFrom()); // 발신자 이메일

        javaMailSender.send(message);
        System.out.println("이메일 전송 성공!");
    }

    public void sendHtmlEmail(Email email) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        helper.setTo(email.getTo().toArray(new String[0]));                      // 수신자 이메일
        helper.setSubject(email.getSubject());            // 이메일 제목
        helper.setText(email.getContents(), true);     // true: HTML 형식으로 내용 전송
        helper.setFrom(email.getFrom()); // 발신자 이메일

        javaMailSender.send(mimeMessage);
        System.out.println("HTML 이메일 전송 성공!");
    }


}
