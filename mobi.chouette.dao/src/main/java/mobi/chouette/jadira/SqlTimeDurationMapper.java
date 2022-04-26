package mobi.chouette.jadira;

import java.sql.Time;

import org.jadira.usertype.spi.shared.AbstractTimeColumnMapper;
import java.time.Duration;
import java.time.LocalTime;
import org.threeten.extra.Seconds;
import java.time.format.DateTimeFormatter;

public class SqlTimeDurationMapper extends AbstractTimeColumnMapper<Duration> {

    private static final long serialVersionUID = -5741261927204773374L;

    public static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public Duration fromNonNullValue(Time value) {
        return Duration.ofMillis(value.getTime());
    }

    @Override
    public Duration fromNonNullString(String s) {
        return Duration.ofSeconds(Seconds.between(LocalTime.of(0, 0), LocalTime.parse(s)).getAmount());
    }

    @Override
    public Time toNonNullValue(Duration value) {
        return new Time(value.toMillis());
    }

    @Override
    public String toNonNullString(Duration value) {
        return LOCAL_TIME_FORMATTER.format(LocalTime.ofNanoOfDay(value.toNanos()));
    }
}
