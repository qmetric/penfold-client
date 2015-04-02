package com.qmetric.penfold.client.app.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TaskDateTimeFormatter
{
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String print(final LocalDateTime dateTime)
    {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static LocalDateTime parse(final String dateTime)
    {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
    }
}
