package mobi.chouette.model.blueprint;

import mobi.chouette.model.CalendarDay;

import com.tobedevoured.modelcitizen.annotation.Blueprint;
import com.tobedevoured.modelcitizen.annotation.Default;
import java.time.LocalDate;

@Blueprint(CalendarDay.class)
public class CalendarDayBlueprint
{
   @Default
   LocalDate date;

   @Default
   Boolean included = Boolean.TRUE;
}
