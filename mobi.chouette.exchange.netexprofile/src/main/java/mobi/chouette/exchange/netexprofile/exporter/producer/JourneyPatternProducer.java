package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.type.AlightingPossibilityEnum;
import mobi.chouette.model.type.BoardingPossibilityEnum;
import org.rutebanken.netex.model.*;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.netexId;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.*;

public class JourneyPatternProducer extends NetexProducer implements NetexEntityProducer<org.rutebanken.netex.model.JourneyPattern, mobi.chouette.model.JourneyPattern> {

    @Override
    public org.rutebanken.netex.model.JourneyPattern produce(Context context, mobi.chouette.model.JourneyPattern neptuneJourneyPattern) {
        org.rutebanken.netex.model.JourneyPattern netexJourneyPattern = netexFactory.createJourneyPattern();
        netexJourneyPattern.setVersion(neptuneJourneyPattern.getObjectVersion() > 0 ? String.valueOf(neptuneJourneyPattern.getObjectVersion()) : NETEX_DATA_OJBECT_VERSION);

        String journeyPatternId = netexId(neptuneJourneyPattern.objectIdPrefix(), JOURNEY_PATTERN, neptuneJourneyPattern.objectIdSuffix());
        netexJourneyPattern.setId(journeyPatternId);

        if (isSet(neptuneJourneyPattern.getComment())) {
            KeyValueStructure keyValueStruct = netexFactory.createKeyValueStructure()
                    .withKey("Comment")
                    .withValue(neptuneJourneyPattern.getComment());
            netexJourneyPattern.setKeyList(netexFactory.createKeyListStructure().withKeyValue(keyValueStruct));
        }

        if (isSet(neptuneJourneyPattern.getName())) {
            netexJourneyPattern.setName(getMultilingualString(neptuneJourneyPattern.getName()));
        }

        if (isSet(neptuneJourneyPattern.getPublishedName())) {
            netexJourneyPattern.setShortName(getMultilingualString(neptuneJourneyPattern.getPublishedName()));
        }

        if (isSet(neptuneJourneyPattern.getRegistrationNumber())) {
            PrivateCodeStructure privateCodeStruct = netexFactory.createPrivateCodeStructure();
            privateCodeStruct.setValue(neptuneJourneyPattern.getRegistrationNumber());
            netexJourneyPattern.setPrivateCode(privateCodeStruct);
        }

        Route route = neptuneJourneyPattern.getRoute();
        RouteRefStructure routeRefStruct = netexFactory.createRouteRefStructure();
        routeRefStruct.setVersion(route.getObjectVersion() != null ? String.valueOf(route.getObjectVersion()) : NETEX_DATA_OJBECT_VERSION);

        String routeIdRef = netexId(route.objectIdPrefix(), ROUTE, route.objectIdSuffix());
        routeRefStruct.setRef(routeIdRef);

        netexJourneyPattern.setRouteRef(routeRefStruct);

        PointsInJourneyPattern_RelStructure pointsInJourneyPattern = netexFactory.createPointsInJourneyPattern_RelStructure();
        List<StopPoint> stopPoints = neptuneJourneyPattern.getStopPoints();
        stopPoints.sort(Comparator.comparingInt(StopPoint::getPosition));

        for (int i = 0; i < stopPoints.size(); i++) {
            StopPoint stopPoint = stopPoints.get(i);

            if (stopPoint != null) {
                String pointInPatternIdSuffix = stopPoint.objectIdSuffix() + "-" + stopPoint.getPosition();
                String stopPointInJourneyPatternId = netexId(stopPoint.objectIdPrefix(), STOP_POINT_IN_JOURNEY_PATTERN, pointInPatternIdSuffix);

                StopPointInJourneyPattern stopPointInJourneyPattern = netexFactory.createStopPointInJourneyPattern();
                stopPointInJourneyPattern.setVersion(stopPoint.getObjectVersion() > 0 ? String.valueOf(stopPoint.getObjectVersion()) : NETEX_DATA_OJBECT_VERSION);
                stopPointInJourneyPattern.setId(stopPointInJourneyPatternId);

                if (isSet(stopPoint.getContainedInStopArea())) {
                    String stopPointIdSuffix = stopPoint.getContainedInStopArea().objectIdSuffix();
                    String stopPointIdRef = netexId(stopPoint.objectIdPrefix(), SCHEDULED_STOP_POINT, stopPointIdSuffix);

                    ScheduledStopPointRefStructure stopPointRefStruct = netexFactory.createScheduledStopPointRefStructure().withRef(stopPointIdRef);
                    stopPointInJourneyPattern.setScheduledStopPointRef(netexFactory.createScheduledStopPointRef(stopPointRefStruct));
                } else {
                    throw new RuntimeException("StopPoint with id : " + stopPoint.getObjectId() + " is not contained in a StopArea. Cannot produce ScheduledStopPoint reference.");
                }

                BoardingPossibilityEnum forBoarding = stopPoint.getForBoarding();
                AlightingPossibilityEnum forAlighting = stopPoint.getForAlighting();

                if (forBoarding != null && forAlighting != null) {
                    if (forBoarding.equals(BoardingPossibilityEnum.normal) && forAlighting.equals(AlightingPossibilityEnum.forbidden)) {
                        stopPointInJourneyPattern.setForAlighting(false);
                    }
                    if (forAlighting.equals(AlightingPossibilityEnum.normal) && forBoarding.equals(BoardingPossibilityEnum.forbidden)) {
                        stopPointInJourneyPattern.setForBoarding(false);
                    }
                }

                stopPointInJourneyPattern.setOrder(BigInteger.valueOf(i + 1));
                pointsInJourneyPattern.getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().add(stopPointInJourneyPattern);
            }
        }

        netexJourneyPattern.setPointsInSequence(pointsInJourneyPattern);
        return netexJourneyPattern;
    }

}