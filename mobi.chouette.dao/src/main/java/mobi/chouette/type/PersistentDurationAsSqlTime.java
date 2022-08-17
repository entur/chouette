package mobi.chouette.type;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.TimeTypeDescriptor;

import java.time.Duration;

/**
 * Mapping java.time.Duration to java.sql.Time.
 *
 * Should ideally store Duration as String or no of ms, but keeping 'time without time zone' in db to minimize impact on chouette2.
 *
 */
public class PersistentDurationAsSqlTime extends AbstractSingleColumnStandardBasicType<Duration> {

    public static final PersistentDurationAsSqlTime INSTANCE = new PersistentDurationAsSqlTime();

    public PersistentDurationAsSqlTime() {
        super(TimeTypeDescriptor.INSTANCE, PersistentDurationAsSqlTimeJavaDescriptor.INSTANCE);
    }


    @Override
    public String getName() {
        return "persistent_duration_as_sql_time";
    }
}
