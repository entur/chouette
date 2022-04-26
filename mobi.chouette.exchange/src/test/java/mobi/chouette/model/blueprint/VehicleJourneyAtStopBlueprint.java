package mobi.chouette.model.blueprint;

import mobi.chouette.common.TimeUtil;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.VehicleJourneyAtStop;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import com.tobedevoured.modelcitizen.annotation.Mapped;
import java.time.LocalTime;

@Blueprint(VehicleJourneyAtStop.class)
public class VehicleJourneyAtStopBlueprint
{

//   @Mapped
//   VehicleJourney vehicleJourney;

   @Mapped
   StopPoint stopPoint;

   @Default
   LocalTime departureTime = TimeUtil.toLocalTime(2500);

   @Default
   LocalTime arrivalTime = TimeUtil.toLocalTime(2800);

}
