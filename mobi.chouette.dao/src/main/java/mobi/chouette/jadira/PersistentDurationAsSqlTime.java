package mobi.chouette.jadira;

import java.sql.Time;

import org.jadira.usertype.spi.shared.AbstractSingleColumnUserType;
import java.time.Duration;

/**
 * Mapping java.time.Duration to java.sql.Time.
 *
 * Should ideally store Duration as String or no of ms, but keeping 'time without time zone' in db to minimize impact on chouette2.
 *
 */
public class PersistentDurationAsSqlTime extends AbstractSingleColumnUserType<Duration, Time, SqlTimeDurationMapper> {

    private static final long serialVersionUID = 1L;
}
