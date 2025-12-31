package com.upec.factoryscheduling.common.utils;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static LocalDateTime parseDateTime(String date) {
        if (!StringUtils.hasLength(date)) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(date, formatter);
    }

    public static LocalDateTime parseDateTime(String date, String format) {
        if (!StringUtils.hasLength(date)) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.parse(date, formatter);
    }

    public static LocalDate parseLocalDate(String date) {
        if (!StringUtils.hasLength(date)) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(date.substring(0, 10), formatter);
    }
}
