package mobi.chouette.type;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.threeten.extra.Seconds;

import java.sql.Time;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PersistentDurationAsSqlTimeJavaDescriptor extends AbstractTypeDescriptor<Duration> {

    public static final PersistentDurationAsSqlTimeJavaDescriptor INSTANCE =
            new PersistentDurationAsSqlTimeJavaDescriptor();

    public static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public PersistentDurationAsSqlTimeJavaDescriptor() {
        super(Duration.class, ImmutableMutabilityPlan.INSTANCE);
    }


    @Override
    public String toString(Duration value) {
        if (value == null) {
            return "null";
        }
        return LOCAL_TIME_FORMATTER.format(LocalTime.ofNanoOfDay(value.toNanos()));
    }

    @Override
    public Duration fromString(String string) {
        if (string == null) {
            return null;
        }
        return Duration.ofSeconds(Seconds.between(LocalTime.of(0, 0), LocalTime.parse(string)).getAmount());
    }

    @Override
    public <X> X unwrap(Duration value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }

        if (Time.class.isAssignableFrom(type)) {
            return (X) new Time(value.toMillis());
        }

        throw unknownUnwrap(type);
    }

    @Override
    public <X> Duration wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }

        if (value instanceof Time) {
            return Duration.ofMillis(((Time) value).getTime());
        }

        throw unknownWrap(value.getClass());
    }

}