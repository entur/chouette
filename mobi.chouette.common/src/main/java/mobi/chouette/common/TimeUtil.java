package mobi.chouette.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import org.threeten.extra.Seconds;

import javax.xml.datatype.XMLGregorianCalendar;

public class TimeUtil {

    public static Duration subtract(LocalTime thisDeparture, LocalTime firstDeparture) {
        long seconds;
        // Assuming journeys last no more than 24 hours
        if (firstDeparture.isBefore(thisDeparture)) {
            seconds = Seconds.between(firstDeparture, thisDeparture).getAmount();
        } else {
            seconds = TimeUnit.DAYS.toSeconds(1) - Seconds.between(thisDeparture, firstDeparture).getAmount();
        }

        return Duration.ofSeconds(seconds);
    }

    /**
     * Convert localDateTime to LocalDate, ignoring time.
     *
     * This is a bit shady, but necessary as long as incoming data, while semantically a LocalDate, is represented as xs:dateTime.
     */
    public static java.time.LocalDate toLocalDateIgnoreTime(java.time.LocalDateTime localDateTime) {
        return localDateTime.toLocalDate();
    }

    public static LocalDate toLocalDate(Date date) {
        if(date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return java.util.Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static long toEpochMilliseconds(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static long toEpochMilliseconds(LocalDate localDate) {
        return toEpochMilliseconds(localDate.atStartOfDay());
    }

    public static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    public static LocalDateTime toLocalDateTime(XMLGregorianCalendar calendar) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(calendar.toGregorianCalendar().getTime().getTime()), ZoneId.systemDefault());
    }

    public static LocalDateTime toLocalDateTime(Calendar calendar) {
        return LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
    }

    public static GregorianCalendar toCalendar(LocalDateTime localDateTime) {
        return GregorianCalendar.from(localDateTime.atZone(ZoneId.systemDefault()));
    }

    public static LocalTime toLocalTime(Calendar calendar) {
        return toLocalDateTime(calendar).toLocalTime();
    }

    public static LocalDate toLocalDate(Calendar calendar) {
        return toLocalDateTime(calendar).toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(long instant) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(instant), ZoneId.systemDefault());
    }

    public static LocalTime toLocalTime(long instant) {
        return toLocalDateTime(instant).toLocalTime();
    }

    public static LocalDate toLocalDate(long instant) {
        return toLocalDateTime(instant).toLocalDate();
    }


    public static long toMillisecondsOfDay(LocalTime localTime) {
        return localTime.toNanoOfDay()/1000000;
    }
}
