package mobi.chouette.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.time.Duration;
import java.time.LocalTime;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TimeUtilTest {

    @Test
    public void testSubtractSameDay() {
        Duration duration = TimeUtil.subtract(LocalTime.of(12, 30, 0), LocalTime.of(10, 5, 30));
        Assert.assertEquals(duration, Duration.ofSeconds(2 * 60 * 60 + 24 * 60 + 30));
    }

    @Test
    public void testSubtractThisDepartureOnNextDay() {
        Duration duration = TimeUtil.subtract(LocalTime.of(00, 10, 0), LocalTime.of(23, 50, 00));
        Assert.assertEquals(duration, Duration.ofSeconds(20 * 60));
    }


    @Test
    public void localDateTimeToLocalDateIgnoresTime() {
        java.time.LocalDate converted = TimeUtil.toLocalDateIgnoreTime(LocalDateTime.of(2018, 3, 20,6,23));

        Assert.assertEquals(converted.getYear(), 2018);
        Assert.assertEquals(converted.getMonthValue(), 3);
        Assert.assertEquals(converted.getDayOfMonth(), 20);
    }
}
