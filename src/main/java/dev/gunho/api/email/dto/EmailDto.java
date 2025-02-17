package dev.gunho.api.email.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmailDto {

    private String subject;
    private List<String> to;
    private String from;
    private String bcc;
    private String contents;

}
