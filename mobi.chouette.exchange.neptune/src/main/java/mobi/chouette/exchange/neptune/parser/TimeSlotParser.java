package mobi.chouette.exchange.neptune.parser;


import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.XPPUtil;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.ParserUtils;
import mobi.chouette.exchange.neptune.Constant;
import mobi.chouette.exchange.neptune.model.NeptuneObjectFactory;
import mobi.chouette.exchange.neptune.model.TimeSlot;
import mobi.chouette.exchange.neptune.validation.TimeSlotValidator;
import mobi.chouette.exchange.validation.ValidatorFactory;
import mobi.chouette.model.Timeband;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import java.time.LocalDateTime;
import java.time.LocalTime;

import java.time.format.DateTimeFormatter;
import org.xmlpull.v1.XmlPullParser;

@Log4j
public class TimeSlotParser implements Parser, Constant {

	public static final DateTimeFormatter DATE_TIME_FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");
	private static final String CHILD_TAG = "TimeSlot";

	@Override
	public void parse(Context context) throws Exception {
		XmlPullParser xpp = (XmlPullParser) context.get(PARSER);
		Referential referential = (Referential) context.get(REFERENTIAL);
		NeptuneObjectFactory factory =  (NeptuneObjectFactory) context.get(NEPTUNE_OBJECT_FACTORY);

		xpp.require(XmlPullParser.START_TAG, null, CHILD_TAG);
		int columnNumber =  xpp.getColumnNumber();
		int lineNumber =  xpp.getLineNumber();
		
		TimeSlotValidator validator = (TimeSlotValidator) ValidatorFactory.create(TimeSlotValidator.class.getName(), context);
				

		TimeSlot timeSlot = null;
		String objectId = null;
		
		// Create the timabands and journeyFrequencies
		Timeband timeband = null;

		while (xpp.nextTag() == XmlPullParser.START_TAG) {
			if (xpp.getName().equals("objectId")) {				
				objectId = ParserUtils.getText(xpp.nextText());
				timeSlot = factory.getTimeSlot(objectId);
				timeSlot.setFilled(true);
				
				timeband = ObjectFactory.getTimeband(referential, objectId);
				timeband.setFilled(true);

			} else if (xpp.getName().equals("objectVersion")) {
				Long version = ParserUtils.getLong(xpp.nextText());
				timeSlot.setObjectVersion(version);
				timeband.setObjectVersion(version);
			} else if (xpp.getName().equals("creationTime")) {
				LocalDateTime creationTime = ParserUtils.getLocalDateTime(xpp.nextText());
				timeSlot.setCreationTime(creationTime);
				timeband.setCreationTime(creationTime);
			} else if (xpp.getName().equals("creatorId")) {
				String creatorId = ParserUtils.getText(xpp.nextText());
				timeSlot.setCreatorId(creatorId);
				timeband.setCreatorId(creatorId);
			} else if (xpp.getName().equals("beginningSlotTime")) {
				LocalTime beginningSlotTime = ParserUtils.getLocalTime(xpp.nextText());
				timeSlot.setBeginningSlotTime(beginningSlotTime);
				timeband.setStartTime(beginningSlotTime);
			} else if (xpp.getName().equals("endSlotTime")) {
				LocalTime endSlotTime = ParserUtils.getLocalTime(xpp.nextText());
				timeSlot.setEndSlotTime(endSlotTime);
				timeband.setEndTime(endSlotTime);
			} else if (xpp.getName().equals("firstDepartureTimeInSlot")) {
				timeSlot.setFirstDepartureTimeInSlot(ParserUtils.getLocalTime(xpp.nextText()));
			} else if (xpp.getName().equals("lastDepartureTimeInSlot")) {
				timeSlot.setLastDepartureTimeInSlot(ParserUtils.getLocalTime(xpp.nextText()));
			} else {
				XPPUtil.skipSubTree(log, xpp);
			}
		}
		timeband.setName(DATE_TIME_FORMATTER_HH_MM.format(timeband.getStartTime())+"->"+ DATE_TIME_FORMATTER_HH_MM.format(timeband.getEndTime()));
		validator.addLocation(context, timeSlot, lineNumber, columnNumber);
	}

	static {
		ParserFactory.register(TimeSlotParser.class.getName(),
				new ParserFactory() {
					private TimeSlotParser instance = new TimeSlotParser();

					@Override
					protected Parser create() {
						return instance;
					}
				});
	}
}
