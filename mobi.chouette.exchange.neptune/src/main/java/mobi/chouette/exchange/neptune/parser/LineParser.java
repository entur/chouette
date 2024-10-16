package mobi.chouette.exchange.neptune.parser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.XPPUtil;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.ParserUtils;
import mobi.chouette.exchange.neptune.JsonExtension;
import mobi.chouette.exchange.neptune.model.TransportModeNameEnum;
import mobi.chouette.exchange.neptune.validation.LineValidator;
import mobi.chouette.exchange.validation.ValidatorFactory;
import mobi.chouette.model.Company;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.Route;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import mobi.chouette.model.type.UserNeedEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import org.xmlpull.v1.XmlPullParser;

@Log4j
public class LineParser implements Parser, Constant, JsonExtension {
	private static final String CHILD_TAG = "Line";

	@Override
	public void parse(Context context) throws Exception {

		XmlPullParser xpp = (XmlPullParser) context.get(PARSER);
		Referential referential = (Referential) context.get(REFERENTIAL);

		xpp.require(XmlPullParser.START_TAG, null, CHILD_TAG);
		int columnNumber = xpp.getColumnNumber();
		int lineNumber = xpp.getLineNumber();

		LineValidator validator = (LineValidator) ValidatorFactory.create(LineValidator.class.getName(), context);

		Line line = null;
		String objectId = null;
		while (xpp.nextTag() == XmlPullParser.START_TAG) {

			if (xpp.getName().equals("objectId")) {
				objectId = ParserUtils.getText(xpp.nextText());
				line = ObjectFactory.getLine(referential, objectId);
				line.setFilled(true);
				line.setNetwork(getPtNetwork(referential));
				line.setCompany(getFirstCompany(referential));
			} else if (xpp.getName().equals("objectVersion")) {
				Long version = ParserUtils.getLong(xpp.nextText());
				line.setObjectVersion(version);
			} else if (xpp.getName().equals("creationTime")) {
				LocalDateTime creationTime = ParserUtils.getLocalDateTime(xpp.nextText());
				line.setCreationTime(creationTime);
			} else if (xpp.getName().equals("creatorId")) {
				line.setCreatorId(ParserUtils.getText(xpp.nextText()));
			} else if (xpp.getName().equals("name")) {
				line.setName(ParserUtils.getText(xpp.nextText()));
			} else if (xpp.getName().equals("number")) {
				line.setNumber(ParserUtils.getText(xpp.nextText()));
			} else if (xpp.getName().equals("publishedName")) {
				line.setPublishedName(ParserUtils.getText(xpp.nextText()));
			} else if (xpp.getName().equals("transportModeName")) {
				
				TransportModeNameEnum value = ParserUtils.getEnum(TransportModeNameEnum.class, xpp.nextText());
				convertNeptuneTransportModeToInternalModel(line,value);
			} else if (xpp.getName().equals("lineEnd")) {
				String lineEnd = ParserUtils.getText(xpp.nextText());
				validator.addLineEnd(context, objectId, lineEnd);
			} else if (xpp.getName().equals("routeId")) {
				String routeId = ParserUtils.getText(xpp.nextText());
				validator.addRouteId(context, objectId, routeId);
				Route route = ObjectFactory.getRoute(referential, routeId);
				route.setLine(line);
			} else if (xpp.getName().equals("ptNetworkIdShortcut")) {
				String ptNetworkIdShortcut = ParserUtils.getText(xpp.nextText());
				validator.addPtNetworkIdShortcut(context, objectId, ptNetworkIdShortcut);
			} else if (xpp.getName().equals("registration")) {
				while (xpp.nextTag() == XmlPullParser.START_TAG) {
					if (xpp.getName().equals("registrationNumber")) {
						line.setRegistrationNumber(ParserUtils.getText(xpp.nextText()));
					} else {
						XPPUtil.skipSubTree(log, xpp);
					}
				}
			} else if (xpp.getName().equals("comment")) {
				line.setComment(ParserUtils.getText(xpp.nextText()));
			} else if (xpp.getName().equals("LineExtension")) {

				while (xpp.nextTag() == XmlPullParser.START_TAG) {
					if (xpp.getName().equals("mobilityRestrictedSuitability")) {
						line.setMobilityRestrictedSuitable(ParserUtils.getBoolean(xpp.nextText()));
					} else if (xpp.getName().equals("accessibilitySuitabilityDetails")) {
						List<UserNeedEnum> userNeeds = new ArrayList<UserNeedEnum>();
						while (xpp.nextTag() == XmlPullParser.START_TAG) {
							if (xpp.getName().equals("MobilityNeed") || xpp.getName().equals("PsychosensoryNeed")
									|| xpp.getName().equals("MedicalNeed")) {
								UserNeedEnum userNeed = ParserUtils.getEnum(UserNeedEnum.class, xpp.nextText());
								if (userNeed != null) {
									userNeeds.add(userNeed);
								}
							} else {
								XPPUtil.skipSubTree(log, xpp);
							}
						}
						line.setUserNeeds(userNeeds);
					} else {
						XPPUtil.skipSubTree(log, xpp);
					}
				}

			} else {
				XPPUtil.skipSubTree(log, xpp);
			}
		}
		validator.addLocation(context, line, lineNumber, columnNumber);
	}


	private void convertNeptuneTransportModeToInternalModel(Line line, TransportModeNameEnum value) {
		log.warn("Mapping from Neptune to internal model is probably not correct - someone with Neptune expertise should look at it");
		
		switch(value) {
		case Air:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Air);
			break;
		case    Train: 
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Rail);
			break;
		case    LongDistanceTrain:
		case    LongDistanceTrain_2:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Rail);
			line.setTransportSubModeName(TransportSubModeNameEnum.InterregionalRail);
			break;
		case    RapidTransit:
		case    LocalTrain:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Rail);
			line.setTransportSubModeName(TransportSubModeNameEnum.Local);
			break;
		case    Metro:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Metro);
			break;
		case    Tramway:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Tram);
			break;
		case    Coach:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Coach);
			break;
		case    Bus:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Bus);
			break;
		case    Ferry:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Ferry);
			break;
		case    Waterborne:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Water);
			break;
		case    Trolleybus:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.TrolleyBus);
			break;
		case    Bicycle:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Bicycle);
			break;
		case    Shuttle:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Bus);
			line.setTransportSubModeName(TransportSubModeNameEnum.ShuttleBus);
			break;
		case    Taxi:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Taxi);
			break;
		case    Val:
		case    Other:
		case    PrivateVehicle:
		case    Walk:
			line.setTransportModeName(mobi.chouette.model.type.TransportModeNameEnum.Other);
		}
	}


	private Company getFirstCompany(Referential referential) {
		for (Company company : referential.getCompanies().values()) {
			if (company.isFilled())
				return company;
		}
		return null;
	}

	private Network getPtNetwork(Referential referential) {
		for (Network network : referential.getPtNetworks().values()) {
			if (network.isFilled())
				return network;
		}
		return null;
	}

	static {
		ParserFactory.register(LineParser.class.getName(), new ParserFactory() {
			private LineParser instance = new LineParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}
}
