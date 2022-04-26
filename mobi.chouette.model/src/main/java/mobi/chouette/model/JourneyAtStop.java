package mobi.chouette.model;

import java.time.LocalTime;

public interface JourneyAtStop {

    void setArrivalTime(LocalTime localTime);

    void setArrivalDayOffset(int intValue);

    void setDepartureTime(LocalTime localTime);

    void setDepartureDayOffset(int intValue);

    LocalTime getArrivalTime();

    LocalTime getDepartureTime();

    int getArrivalDayOffset();

    int getDepartureDayOffset();
}
