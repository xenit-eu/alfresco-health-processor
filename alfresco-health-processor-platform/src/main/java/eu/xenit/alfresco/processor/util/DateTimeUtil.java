package eu.xenit.alfresco.processor.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
    public static long xSecondsAgoToMs(long secondsAgo){
        return OffsetDateTime.now().minusSeconds(secondsAgo).toInstant().toEpochMilli();
    }

    public static String fromTheLastXYears(int years) {
        return LocalDate.now().minusYears(years)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String toReadableString(long epochMilli) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault())
                .toString();
    }

    public static long dateToEpochMs(String date) {
        return LocalDate.parse(
                date,
                DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}
