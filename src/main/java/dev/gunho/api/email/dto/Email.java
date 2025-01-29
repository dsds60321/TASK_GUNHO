package dev.gunho.api.email.dto;

import lombok.Data;

@Data
public class Email {

    private String subject;
    private String to;
    private String from;
    private String bcc;
    private String contents;

}
