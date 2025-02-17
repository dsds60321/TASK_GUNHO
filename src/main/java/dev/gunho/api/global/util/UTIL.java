package dev.gunho.api.global.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UTIL {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getCurrentDate() {
        return LocalDateTime.now().format(DEFAULT_FORMATTER);
    }

}
